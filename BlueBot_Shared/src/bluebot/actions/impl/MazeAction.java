package bluebot.actions.impl;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import algorithms.Dijkstra;
import bluebot.BarcodeExecuter;
import bluebot.DriverException;
import bluebot.actions.Action;
import bluebot.actions.ActionException;
import bluebot.graph.Border;
import bluebot.graph.Direction;
import bluebot.graph.Graph;
import bluebot.graph.Orientation;
import bluebot.graph.Tile;
import bluebot.maze.AbstractPathProvider;
import bluebot.sensors.CalibrationException;
import bluebot.util.Timer;
import bluebot.util.Utils;



/**
 * {@link Action} implementation for the maze exploration algorithm
 */
public class MazeAction extends Action {
	
	private final Graph maze;
	private Direction headDirection,moveDirection;
	private Tile current;
	private int calibParam = 0;
	private int turnCalibCost = 3;
	private int moveCalibCost = 2;
	
	private List<Tile> blackSpots;
	private BarcodeExecuter barcodeExecuter;
//	private MyBarcodeScanner barcodeScanner;
	private ArrayList<Tile> stillCheckForBarcode;
	private final Dijkstra pf;
	private boolean stillExploring=true;
//	private final WhiteLineAction wa;
	private boolean orientateHorizontal=false;
	private boolean orientateVertical=false;
	private int calibLimit = 10;
	
	public MazeAction(){
		this.maze = new Graph();
		this.headDirection=Direction.UP;
		this.moveDirection=Direction.UP;
		this.blackSpots = null;
		this.stillCheckForBarcode = new ArrayList<Tile>();
		this.pf = new Dijkstra(maze);
//		this.wa = new WhiteLineAction();
	}
	/**
	 * Execute the wall following algorithm. Always keep the wall to your right. Till we're back on the start position and all
	 * Start neighbors are explored. This means 'black spots' still remain in the maze. The algorithm detects black spots and will visit the black spots to explore the remaining tiles.
	 * @throws ActionException 
	 * @throws CalibrationException 
	 */
	protected void execute() throws InterruptedException, ActionException, DriverException {
		final Timer timer = new Timer();
		timer.reset();
		
		this.getDriver().resetOrientation();
//		long startTime = System.currentTimeMillis();
		this.initializeRootTile();
		
		// The barcode executor can only be initialized here,
		// because there is no Driver instance
		// to pass to its constructor before this point.
		this.barcodeExecuter = new BarcodeExecuter(getDriver(), maze);
//		this.barcodeScanner = new MyBarcodeScanner();
//		barcodeScanner.start();
		
		final MyPathProvider route = new MyPathProvider();
		for (Tile[] path; (path = route.getPathToNextTile()) != null;) {
			if (isAborted()) {
				return;
			}
			
			// We have located a tile that requires scanning
			// Travel to it using the optimal path
			for (int i = 1; i < path.length; i++) {
				moveTo(path[i]);
			}
			
			final Tile tile = current;
			// Scan the current tile
			checkEfficicientlyTile(tile);
			// Only add the neighbors next to open borders,
			// to avoid having the excess tiles on the outer rim
			maze.addVerticies(tile.getAbsoluteNeighbors());
			
			// Check for a barcode if necessary
			if (tile.canHaveBarcode()) {
				final int barcode = scanBarcode(tile);
				if (barcode > 0) {
					barcodeExecuter.executeBarcode(barcode, tile);
				}
			}
		}
		
		final long timeExploration = timer.read();
		
		Tile a = maze.getCheckpointVertex();
		if (a == null) {
			a = current;
		} else {
			followEfficientlyPath(pf.findShortestPath(current, a));
		}
		
		Tile b = maze.getFinishVertex();
		if (b == null) {
			b = maze.getVertex(0, 0);
		}
		
		timer.reset();
		followEfficientlyPath(pf.findShortestPath(a, b));
		final long timeShortestPath = timer.read();
		
		final String msg = new StringBuilder()
		.append(Utils.formatDuration(timeExploration)).append(" to explore the maze.\n")
		.append(Utils.formatDuration(timeShortestPath)).append(" to reach the finish.")
		.toString();
		getDriver().sendMessage(msg, "Finished");
		
		/*
		do{
			if(isAborted()){
//				barcodeScanner.stop();
				return;
			}
			
			Tile next = this.determineNextTile();
			this.moveTo(next);
			if (next.isExplored()) {
				// Wait until we have stopped moving
				waitForMoving(false);
				// At this point the barcode scanner will start processing information
				// so we give it some time to get the work done before we continue
				try {
					Thread.sleep(100L);
				} finally {
//					barcodeScanner.stop();
				}
			} else {
				// The barcode scanner has enough time to process information
				// while we analyze the current tile
				this.checkEfficicientlyTile(next);
//				barcodeScanner.stop();
			}
			
			if(next.canHaveBarcode()){
				final int barcode =
//						current.getBarCode();
						scanBarcode(next);
				if (barcode > 0) {
					this.barcodeExecuter.executeBarcode(barcode, next);
				}
			}
			
			this.maze.addVerticies(next.getAbsoluteNeighbors());
			
			if(current == this.maze.getRootTile()){
				this.findBlackSpots();
				this.processBlackSpots();
				break;
			}
			
		}while(this.hasUnvisitedNeighbors(this.maze.getRootTile())||this.hasUnvisitedNeighbors(current)||this.graphHasUnvisitedNeighbors());
		
		this.processBarcodes();
		this.stillExploring = false;
		long stopTime = System.currentTimeMillis();
		long duration = stopTime-startTime;
		int seconds = (int) (duration / 1000) % 60 ;
		int minutes = (int) ((duration / (1000*60)) % 60);
		String finishStamp = null;
		if(this.maze.getFinishVertex() != null && this.maze.getCheckpointVertex() != null){
			this.followEfficientlyPath(pf.findShortestPath(current, this.maze.getCheckpointVertex()));
			long startFinish = System.currentTimeMillis();
			this.followEfficientlyPath(pf.findShortestPath(current, this.maze.getFinishVertex()));
			long endFinish = System.currentTimeMillis();
			long finishDuration = endFinish-startFinish;
			int finishseconds = (int) (finishDuration / 1000) % 60 ;
			int finishminutes = (int) ((finishDuration / (1000*60)) % 60);
			finishStamp = (finishminutes<10 ? "0"+finishminutes : finishminutes)+":"+(finishseconds<10 ? "0"+finishseconds : finishseconds);
		}else{
			getDriver().sendError("No finish tile was scanned.");
		}
		StringBuilder str = new StringBuilder();
		str.append("It took "+(minutes<10 ? "0"+minutes : minutes)+":"+(seconds<10 ? "0"+seconds : seconds)+" to explore the maze.");
		if(finishStamp != null){
			str.append("\nIt took "+finishStamp+" to reach the finish tile.");
		}
		getDriver().sendMessage(str.toString(), "Maze explored !");
		*/
	}
	
	@SuppressWarnings("unused")
	private void processBlackSpots() throws InterruptedException, ActionException, DriverException {
		for(Tile t : this.blackSpots){
			List<Tile> path = pf.findShortestPath(current, t);
			if(path!=null){
				this.followEfficientlyPath(pf.findShortestPath(current, t));
				this.checkEfficicientlyTile(this.maze.getVertex(t.getX(), t.getY()));
				if (current.canHaveBarcode()) {
					final int b = scanBarcode(current);
					if(b>0){
						this.barcodeExecuter.executeBarcode(b,current);
					}
				}
			}
		}
		this.findBlackSpots();
		if(this.blackSpots.size()>0){
			Iterator<Tile> iter = blackSpots.iterator();
			while(iter.hasNext()){
				Tile t = iter.next();
				List<Tile> path = pf.findShortestPath(current, t);
				if(path==null){
					iter.remove();
				}
			}
			this.processBlackSpots();
		}
		
	}
	/**
	 * Check for tiles that still need to be checked for barcodes. And process them if necessary.
	 * 
	 * @throws CalibrationException
	 * @throws InterruptedException
	 * @throws ActionException
	 * @throws DriverException
	 */
	@SuppressWarnings("unused")
	private void processBarcodes() throws CalibrationException,
			InterruptedException, ActionException, DriverException {
		if(stillCheckForBarcode.size()>0){
			for(Tile t:stillCheckForBarcode){
				this.followEfficientlyPath(pf.findShortestPath(current,t));;
				final int barcode = scanBarcode(current);
				if (barcode > 0) {
					this.barcodeExecuter.executeBarcode(barcode, current);
				}
				
			}
		}
	}
	
	/**
	 * Move to a given next tile.
	 * 
	 * @param next
	 * @throws InterruptedException
	 * @throws ActionException 
	 * @throws CalibrationException 
	 */
	private void moveTo(Tile next) throws ActionException, DriverException, InterruptedException {
		if(next.equals(current)){
			getDriver().sendError("Tiles are the same");
		}
		if(next.isEastFrom(this.current)){
			this.travelEast();
		}else if(next.isWestFrom(this.current)){
			this.travelWest();
		}else if(next.isNorthFrom(this.current)){
			this.travelNorth();
		}else if(next.isSouthFrom(this.current)){
			this.travelSouth();
		}else{
			getDriver().sendError("[EXCEPTION]-Something strange happend.");
		}
		this.current = next;
		
	}
	
	
	/**
	 * Move forward , every 4 tiles orientate the robot.
	 * 
	 * @throws ActionException
	 * @throws DriverException
	 * @throws InterruptedException
	 */
	private void moveForward() throws ActionException, DriverException, InterruptedException {
		// Start the barcode scanner before we move forward
//		barcodeScanner.start();
		// Allow the barcode scanner some time to kick into action
		Thread.sleep(100L);
		// Move forward 
		if(!stillExploring){
			this.getDriver().moveForward(400F, true);
		}
		else{
			if(this.orientationIsNeeded()){
				if(this.orientateVertical){
					if((moveDirection.equals(Direction.UP)||moveDirection.equals(Direction.DOWN))&&!checkForWall()){
						this.getDriver().moveForward(40F, true);
						executeWhiteLine();
						this.getDriver().moveForward(200F,true);
						this.calibParam=0;
						this.orientateVertical = false;
					}else{
						this.getDriver().moveForward(400F, true);
					}
				}else if(this.orientateHorizontal){
					if((moveDirection.equals(Direction.LEFT)||moveDirection.equals(Direction.RIGHT))&&!checkForWall()){
						this.getDriver().moveForward(40F, true);
						executeWhiteLine();
						this.getDriver().moveForward(200F,true);
						this.calibParam=0;
						this.orientateHorizontal = false;
					}else{
						this.getDriver().moveForward(400F, true);
					}
				}else{
					this.getDriver().moveForward(400F, true);
				}
			}else{
				this.getDriver().moveForward(400F, true);
			}
		}
		this.incrementCalibParam(this.moveCalibCost);
		getDriver().modifyOrientation();
	}
	
	private boolean orientationIsNeeded(){
		if(this.orientateHorizontal||this.orientateVertical)
			return true;
		return false;
	}
	/**
	 * Let the robot travel south.
	 * 
	 * @throws InterruptedException
	 * @throws ActionException 
	 */
	private void travelSouth() throws ActionException, DriverException, InterruptedException {
		switch(moveDirection){
			case DOWN:
				break;
			case LEFT:
				this.getDriver().turnLeft(90F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			case RIGHT:
				this.getDriver().turnRight(90F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			case UP:
				this.getDriver().turnRight(180F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			default:
				break;
			
		}
		
		this.moveDirection = Direction.DOWN;
		this.headDirection = moveDirection;
		this.moveForward();
		
	}
	/**
	 * Let the robot travel west.
	 * 
	 * @throws InterruptedException
	 * @throws ActionException 
	 * @throws CalibrationException 
	 */
	private void travelWest() throws ActionException, DriverException, InterruptedException {
		switch(moveDirection){
			case DOWN:
				this.getDriver().turnRight(90F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			case LEFT:
				
				break;
			case RIGHT:
				this.getDriver().turnRight(180F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			case UP:
				this.getDriver().turnLeft(90F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			default:
				break;
		
		}
		
		this.moveDirection = Direction.LEFT;
		this.headDirection = this.moveDirection;
		this.moveForward();
		
	}
	/**
	 * Let the robot travel north.
	 * 
	 * @throws InterruptedException
	 * @throws ActionException 
	 * @throws CalibrationException 
	 */
	private void travelNorth() throws ActionException, DriverException, InterruptedException {
		switch(moveDirection){
			case DOWN:
				this.getDriver().turnRight(180F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			case LEFT:
				this.getDriver().turnRight(90F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			case RIGHT:
				this.getDriver().turnLeft(90F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			case UP:
				
				break;
			default:
				break;
			
		}
		
		this.moveDirection = Direction.UP;
		this.headDirection = this.moveDirection;
		this.moveForward();
	}
	/**
	 * Let the robot travel east.
	 * 
	 * @throws InterruptedException
	 * @throws ActionException 
	 * @throws CalibrationException 
	 */
	private void travelEast() throws ActionException, DriverException, InterruptedException {
		switch(moveDirection){
			case DOWN:
				this.getDriver().turnLeft(90F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			case LEFT:
				this.getDriver().turnRight(180F,true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			case RIGHT:
				break;
			case UP:
				this.getDriver().turnRight(90F, true);
				this.incrementCalibParam(this.turnCalibCost);
				break;
			default:
				break;
			
		}
		
		
		this.moveDirection = Direction.RIGHT;
		this.headDirection = moveDirection;
		this.moveForward();
		
	}
	/**
	 * Always give right or ahead or left or back. If black spots are detected, give these priority as next tile if they are reachable.
	 * 
	 * @return
	 */
	@SuppressWarnings("unused")
	private Tile determineNextTile() {/*
		if(this.blackSpots != null){
			Iterator<Tile> iter = this.blackSpots.iterator();
			while(iter.hasNext()){
				Tile t = iter.next();
				if(t.isNeighborFrom(current)){
					iter.remove();
					return t;
				}
			}
		}*/
		List<Tile> possibs = new ArrayList<Tile>();
		switch(moveDirection){
			case DOWN:
				if(current.getBorderWest() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX()-1,current.getY()));
				}
				if(current.getBorderSouth() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX(),current.getY()-1));
				}
				if(current.getBorderEast()==Border.OPEN){
					possibs.add(maze.getVertex(current.getX()+1,current.getY()));
				}
				if(current.getBorderNorth() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX(),current.getY()+1));
					
				}
				break;
			case LEFT:
				if(current.getBorderNorth() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX(),current.getY()+1));
					
				}
				if(current.getBorderWest() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX()-1,current.getY()));
				}
				if(current.getBorderSouth() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX(),current.getY()-1));
				}
				if(current.getBorderEast()==Border.OPEN){
					possibs.add(maze.getVertex(current.getX()+1,current.getY()));
				}
				
				break;
			case RIGHT:
				if(current.getBorderSouth() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX(),current.getY()-1));
				}
				if(current.getBorderEast()==Border.OPEN){
					possibs.add(maze.getVertex(current.getX()+1,current.getY()));
				}
				if(current.getBorderNorth() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX(),current.getY()+1));
					
				}
				if(current.getBorderWest() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX()-1,current.getY()));
				}
				break;
			case UP:
				if(current.getBorderEast()==Border.OPEN){
					possibs.add(maze.getVertex(current.getX()+1,current.getY()));
				}
				if(current.getBorderNorth() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX(),current.getY()+1));
					
				}
				if(current.getBorderWest() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX()-1,current.getY()));
				}
				if(current.getBorderSouth() == Border.OPEN){
					possibs.add(maze.getVertex(current.getX(),current.getY()-1));
				}
				
				break;
		}
		for(Tile t : possibs){
			if(!t.isExplored()){
				return t;
			}
		}
		return possibs.get(0);
		
	}
	/**
	 * Check if a wall is present in front of the robot.
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	private Boolean checkForWall() throws InterruptedException{
		Thread.sleep(200L);
		int dist = getDriver().readSensorUltraSonic();
		if(dist < 25){
			return true;
		}else if(dist > 30){
			return false;
		}else{
			getDriver().turnHeadCounterClockWise(5);
			Thread.sleep(200L);
			int dist1 = getDriver().readSensorUltraSonic();
			getDriver().turnHeadClockWise(10);
			Thread.sleep(200L);
			int dist2 = getDriver().readSensorUltraSonic();
			getDriver().turnHeadCounterClockWise(5);
			if(dist1 < 25 || dist2 < 25){
				return true;
			}
			return false;
		}
	}
	/**
	 * Initialize the robot and the root tile it is standing on.
	 * @throws InterruptedException
	 */
	private void initializeRootTile() throws InterruptedException{
		Tile root = new Tile(0,0);
		this.checkEfficicientlyTile(root);
		//Tile root = this.exploreTile(new Tile(0,0));
		this.maze.setRootTile(root);
		this.maze.addVerticies(root.getAbsoluteNeighbors());
		getDriver().sendTile(root);
		this.current = root;
		//this.processUnExploredTiles();
	}
	
	/**
	 * Check if a given tile has unvisited neighbors.
	 * @param t
	 * @return
	 */
	private boolean hasUnvisitedNeighbors(Tile t){
		for(Tile tile : this.maze.getNeighborsFrom(t)){
			if(!tile.isExplored()){
				return true;
			}
		}
		return false;
		
	}
	/**
	 * Check if the graph still has unvisited neighbors left.
	 * @return
	 */
	@SuppressWarnings("unused")
	private boolean graphHasUnvisitedNeighbors(){
		for(Tile t : this.maze.getVerticies()){
			if(t.isExplored()){
				if(hasUnvisitedNeighbors(t)){
					return true;
				}
			}
		}
		
		return false;
	}
	/**
	 * Find black spots in the currently used maze.
	 * 
	 */
	private void findBlackSpots(){
		this.blackSpots = new ArrayList<Tile>();
		for(Tile t : this.maze.getVerticies()){
			if(!t.isExplored()){
				blackSpots.add(t);
			}
		}
		
	}
	/**
	 * Check efficiently a given tile.
	 * 
	 * @param t
	 * @throws InterruptedException
	 */
	private void checkEfficicientlyTile(Tile t) throws InterruptedException{
		for(Direction d : this.getBordersToBeChecked(t)){
			checkBorder(d,t);
		}
	}
	/**
	 * Check the border from a given tile in a given direction.
	 * 
	 * @param d
	 * @param t
	 * @throws InterruptedException
	 */
	private void checkBorder(Direction d, Tile t) throws InterruptedException {
		Tile neighbor = this.getNeighborForGivenDirection(d,t);
		Border flag = Border.OPEN;
		if(wallInDirection(d)){
			flag = Border.CLOSED;
		}
		switch(d){
		case DOWN:
			t.setBorderSouth(flag);
			neighbor.setBorderNorth(flag);
			break;
		case LEFT:
			t.setBorderWest(flag);
			neighbor.setBorderEast(flag);
			break;
		case RIGHT:
			t.setBorderEast(flag);
			neighbor.setBorderWest(flag);
			break;
		case UP:
			t.setBorderNorth(flag);
			neighbor.setBorderSouth(flag);
			break;
		default:
			throw new IllegalStateException("Woops, the world has collapsed.");
		
		}
		getDriver().sendTile(t);
		getDriver().sendTile(neighbor);
		if(neighbor.getBarCode()!=-1 && neighbor.canHaveBarcode()){
			this.stillCheckForBarcode.add(neighbor);
		}
	}
	/**
	 * Ask the neighboring tile in a given direction and given tile.
	 * 
	 * @param d
	 * @param t
	 * @return
	 */
	private Tile getNeighborForGivenDirection(Direction d,Tile t) {
		this.maze.addVerticies(t.getAbsoluteNeighbors());
		for(Tile n : this.maze.getAbsoluteNeighborsFrom(t)){
			switch(d){
				case DOWN:
					if(n.isSouthFrom(t)){
						return n;
					}
					break;
				case LEFT:
					if(n.isWestFrom(t)){
						return n;
					}
					break;
				case RIGHT:
					if(n.isEastFrom(t)){
						return n;
					}
					break;
				case UP:
					if(n.isNorthFrom(t)){
						return n;
					}
					break;
				default:
					break;
			
			}
		}
		
		throw new IllegalStateException("Oh dear, shouldn't happen.");
	}
	/**
	 * Check for a wall in a given direction.
	 * 
	 * @param d
	 * @return true if a wall is detected, false otherwise.
	 * @throws InterruptedException
	 */
	private boolean wallInDirection(Direction d) throws InterruptedException{
		boolean wall;
		switch(this.headDirection){
		case DOWN:
			if(d == Direction.DOWN){
				wall = checkForWall();
				
			}else if(d == Direction.LEFT){
				getDriver().turnHeadClockWise(90);
				wall = checkForWall();
				getDriver().turnHeadCounterClockWise(90);
			}else if(d == Direction.RIGHT){
				getDriver().turnHeadCounterClockWise(90);
				wall = checkForWall();
				getDriver().turnHeadClockWise(90);
			}else{
				getDriver().turnHeadClockWise(180);
				wall = checkForWall();
				getDriver().turnHeadCounterClockWise(180);
			}
			return wall;
		case LEFT:
			if(d == Direction.DOWN){
				getDriver().turnHeadCounterClockWise(90);
				wall = checkForWall();
				getDriver().turnHeadClockWise(90);
			}else if(d == Direction.LEFT){
				wall = checkForWall();
			}else if(d == Direction.RIGHT){
				getDriver().turnHeadClockWise(180);
				wall = checkForWall();
				getDriver().turnHeadCounterClockWise(180);
			}else{
				getDriver().turnHeadClockWise(90);
				wall = checkForWall();
				getDriver().turnHeadCounterClockWise(90);
			}
			return wall;
		case RIGHT:
			if(d == Direction.DOWN){
				
				getDriver().turnHeadClockWise(90);
				wall = checkForWall();
				getDriver().turnHeadCounterClockWise(90);
				
			}else if(d == Direction.LEFT){
				getDriver().turnHeadClockWise(180);
				wall = checkForWall();
				getDriver().turnHeadCounterClockWise(180);
			}else if(d == Direction.RIGHT){
				wall = checkForWall();
			}else{
				getDriver().turnHeadCounterClockWise(90);
				wall = checkForWall();
				getDriver().turnHeadClockWise(90);
			}
			return wall;
		case UP:
			if(d == Direction.DOWN){
				getDriver().turnHeadClockWise(180);
				wall = checkForWall();
				getDriver().turnHeadCounterClockWise(180);
			}else if(d == Direction.LEFT){
				getDriver().turnHeadCounterClockWise(90);
				wall = checkForWall();
				getDriver().turnHeadClockWise(90);
			}else if(d == Direction.RIGHT){
				getDriver().turnHeadClockWise(90);
				wall =checkForWall();
				getDriver().turnHeadCounterClockWise(90);
			}else{
				wall = checkForWall();
			}
			return wall;
		
		}
		throw new IllegalStateException("Should not happen here.");
	}
	/**
	 * Determine which borders still need to be checked for a given tile.
	 * 
	 * @param t
	 * @return list of directions the tile still needs to be checked.
	 */
	private List<Direction> getBordersToBeChecked(Tile t){
		List<Direction> directions = new ArrayList<Direction>();
		if(t.getBorderEast()==Border.UNKNOWN){
			directions.add(Direction.RIGHT);
		}
		if(t.getBorderNorth()==Border.UNKNOWN){
			directions.add(Direction.UP);
		}
		if(t.getBorderSouth() == Border.UNKNOWN){
			directions.add(Direction.DOWN);
		}
		if(t.getBorderWest() == Border.UNKNOWN){
			directions.add(Direction.LEFT);
		}
		
		return directions;
	}
	/**
	 * Scan a given tile for a barcode.
	 * 
	 * @param tile
	 * @return
	 * @throws ActionException
	 * @throws DriverException
	 * @throws InterruptedException
	 */
	private final int scanBarcode(final Tile tile)
			throws ActionException, DriverException, InterruptedException {
		int barcode = tile.getBarCode();
		if (barcode == 0) {
			// The tile has been checked before,
			// and it has no barcode
			return -1;
		}
		if (barcode > 0) {
			// The tile has been checked before,
			// and it has a valid barcode
			return barcode;
		}
		
		final ReadBarcodeAction reader = new ReadBarcodeAction(tile);
		
		final int speed = getDriver().getSpeed();
		reader.execute(getDriver());
		getDriver().setSpeed(speed);
		
		barcode = reader.getBarcode();
		if (barcode <= 0) {
			// Remember to not check this tile again
			tile.setBarCode(0);
			return -1;
		}
		
		tile.setBarCode(barcode);
		getDriver().sendTile(tile);
		return barcode;
	}
	
	private void followEfficientlyPath(List<Tile> path) throws ActionException, DriverException, InterruptedException {
		this.getDriver().setSpeed(100);
		ArrayList<Tile> straightLine = new ArrayList<Tile>();
		for(Tile t : path){
			Tile currentTileInList = null;
			if(straightLine.size()>0){
				currentTileInList = straightLine.get(straightLine.size()-1);
			}else{
				currentTileInList = this.current;
			}
			
			if(this.isOnStraighLine(currentTileInList,t)){
				straightLine.add(t);
			}else{
				if(straightLine.size()>0){
					int distanceForward = (straightLine.size())*400;
					this.getDriver().moveForward(distanceForward,true);
					this.current = straightLine.get(straightLine.size()-1);
				}
				this.moveTo(t);
				straightLine.clear();
				this.current = t;
			}
			
		}
		if(straightLine.size()>0){
			int distanceForward = (straightLine.size())*400;
			this.getDriver().moveForward(distanceForward,true);
			this.current = straightLine.get(straightLine.size()-1);
		}
		
	}
	
	private boolean isOnStraighLine(Tile t1,Tile t2){
		return t2.equals(getNeighborForGivenDirection(this.moveDirection, t1));
	}
	

	public boolean isStillExploring() {
		return stillExploring;
	}

	private void incrementCalibParam(int amount) {
		this.calibParam = this.calibParam+amount;
		if(this.calibParam >= this.calibLimit ){
			if(!this.orientateHorizontal){
				this.orientateHorizontal = true;
			}
			if(!this.orientateVertical){
				this.orientateVertical = true;
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	private final class MyPathProvider extends AbstractPathProvider {
		
		protected Tile getCurrentTile() {
			return current;
		}
		
		protected Orientation getCurrentDirection() {
			switch (moveDirection) {
				case UP:
					return Orientation.NORTH;
				case RIGHT:
					return Orientation.EAST;
				case DOWN:
					return Orientation.SOUTH;
				case LEFT:
					return Orientation.WEST;
				default:
					throw new RuntimeException("Invalid direction:  " + moveDirection);
			}
		}
		
		protected Tile getTile(final int x, final int y) {
			return maze.getVertex(x, y);
		}
		
	}
	
}
package bluebot.actions.impl;


import bluebot.Driver;
import bluebot.Robot;
import bluebot.actions.Action;
import bluebot.actions.ActionException;
import bluebot.graph.Orientation;
import bluebot.graph.Tile;
import bluebot.maze.BarcodeValidator;
import bluebot.sensors.Brightness;
import bluebot.sensors.CalibrationException;



/**
 * Precodition:
 * Robot is positioned in straight tile (with only walls left and right). 
 * Robot is positioned in the center of the tile.
 * 
 * This class checks whether on the current position there is a barcode.
 * If so a barcode reading algorithm is started. 
 * After the reading the barcode it can be retrieved.
 * @author Dieter, Ruben
 *
 */
public class ReadBarcodeAction extends Action {
	private Driver driver;
	private int slow = 16;
	private int barcode =0;
	private Tile currentTile;
	
	/**
	 * 
	 * 
	 * 
	 */
	public ReadBarcodeAction(Tile currentTile) {
		this.currentTile = currentTile;  
	}
	
	@Override
	public void execute(Driver driver) throws ActionException,
			CalibrationException, InterruptedException {
		this.driver = driver;
		
		driver.setSpeed(slow);
		
		if(!driver.readsBlack()){
			int maxDriveDistance = 120;
			//Drive backwards to the first black line.
			driver.moveBackward(maxDriveDistance, false);
			waitForBlack(driver, true);
			if (driver.readsBlack()) {
				driver.stop();
			} else {
				//No barcode in this Tile.
				driver.moveForward(maxDriveDistance, true);
				return;
			}
		}
		//Compensate for case that we are in the barcode.
		while(!driver.readsGray()){
			driver.moveForward(20, true);
		}
		
		driver.moveBackward();
		waitForBlack(driver, true);
		float difference = getPosition(driver);
		driver.moveForward();
		waitForBlack(driver,false);
		difference = Math.abs(difference - getPosition(driver));
		//Position robot in middle of first black line
		driver.moveBackward(difference/2F+10, true);
		if(!driver.readsBlack()){
			//Positioning failed
			throw new ActionException("First black line moved. :0");
		}
		//We are in the middle of the first black line.
		for(int i=6; i>0; i--){ // Read the 6 significant lines.
			driver.moveBackward(20, true);
			if(driver.readsBlack()){
				appendBlackToBarcode();
			} else if(driver.readsWhite()){
				appendWhiteToBarcode();
			}
		}
		//Move robot to center of the ending black line
		driver.moveBackward(20, true);
		if(!driver.readsBlack()){
			//Positioning failed
			throw new ActionException("End black line moved. :0");
		}
		
		//Now validate the read barcode and update the tile.
		barcode = BarcodeValidator.validate(barcode);
		if(barcode != -1){
			currentTile.setBarCode(barcode);
			driver.sendTile(currentTile);
			driver.sendDebug("Barcode found:  " + barcode);
		}
		
		// Return to the middle of the tile.
		//	There's no need to adjust/correct our heading,
		//      since this algorithm never turns the robot.
		//      We could simply drive backwards
		//      until we discover the white line,
		//      and then move forward (200 + |sensor-center|) mm
		driver.moveBackward();
		waitForWhite(driver, true);
		driver.stop();
		driver.moveForward(200 + Robot.OFFSET_SENSOR_LIGHT, true);
	}

	private float getPosition(Driver driver) {
		bluebot.util.Orientation pos = driver.getOrientation();
		Orientation orient = Orientation.forHeading(pos.getHeadingBody());
		switch (orient) {
		case NORTH:
		case SOUTH:
			return pos.getY();
		case EAST:
		case WEST:
			return pos.getX();
		default:
			driver.sendError("Wrong heading, orientation does not exist");
			throw new RuntimeException("Wrong orientation");
		}
	}
	
	
	/**
	 * Waits till a 'black' lightvalue is found. 
	 * @param driver
	 * @param flag
	 * 
	 * @throws CalibrationException if the light sensor has not been calibrated
	 */
	//TODO: make this an action to prevent code duplication.
	private final void waitForBlack(final Driver driver, final boolean flag) throws CalibrationException {
		if (flag) {
			while (!isAborted()
					&& driver.isMoving()
					&& !driver.readsBlack());
		} else {
			while (!isAborted()
					&& driver.isMoving()
					&& driver.readsBlack());
		}
	}

	//TODO: make this an action to prevent code duplication.
	private final void waitForWhite(final Driver driver, final boolean flag) throws CalibrationException {
		if (flag) {
			while (!isAborted()
					&& driver.isMoving()
					&& !driver.readsWhite());
		} else {
			while (!isAborted()
					&& driver.isMoving()
					&& driver.readsWhite());
		}
	}

	/**
	 * This returns the value of the barcode if there is one. 
	 * @return
	 * 		If a barcode is found returns it's value (>0)
	 * 		If no barcode is found or an illegal one is found returns -1.
	 */
	public int getBarcode() {
		return barcode;
	}
	
	/**
	 * Appends a "0" (representing a black line) to the barcode.
	 * @return 
	 */
	private void appendBlackToBarcode(){
		barcode <<= 1;
	}
	
	/**
	 * Appends a "1" (representing a white line) to the barcode.
	 * @return 
	 */
	private void appendWhiteToBarcode(){
		barcode <<= 1;
		barcode |= 1;
	}
}

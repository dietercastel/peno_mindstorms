package bluebot.simulator;


import bluebot.Robot;
import bluebot.util.Orientation;



/**
 * The {@link Robot} implementation for the simulator
 * 
 * @author Dieter Castel, Ruben Feyen, Michiel Ruelens 
 */
@SuppressWarnings("unused")
public class VirtualRobot implements Robot {
	/**
	 * Static that holds the standard travel speed in mm/s. This is the speed we measured in the real NXT robot.
	 */
	public static double STANDARD_TRAVEL_SPEED = 30; //Probably get this value from other class.//TODO: see what this is irl
	/**
	 * Static that holds the standard rotate speed in degrees/s. This is the speed we measured in the real NXT robot.
	 */
	public static double STANDARD_ROTATE_SPEED = 30; //Probably get this value from other class.//TODO: see what this is irl
	/**
	 * Variable holding the travel speed of the robot.
	 */
	private double travelSpeed;
	
	/**
	 * Variable holding the rotate speed of the robot.
	 */
	private double rotateSpeed;
	
	/**
	 * Variable representing the absolute heading of this robot in degrees (0� being North). 
	 */
	private float absoluteHeading;

	/**
	 * Variable representing the horizontal coordinate at the start of the current move in the global coordinate system (Origin being the center of the first tile).
	 */
	private float x;
	
	/**
	 * Variable representing the vertical coordinate at the start of the current in the global coordinate system (Origin being the center of the first tile).
	 */
	private float y;
	
	/**
	 * Variable representing the current action of the simulator.
	 */
	private Action currentAction;
	/**
	 * Variable holding the argument corresponding to the <code>currentAction</code>.
	 */
	private double currentArgument;
	/**
	 * Boolean declaring whether the current action is blocking.
	 */
	private boolean isWaiting;
	/**
	 * Variable holding the start time of the {@link currentAction} in milliseconds.
	 */
	private long timestamp;
	
	/**
	 * Variable holding the time in milliseconds at which the current action will be completed.
	 */
	private long currentActionETA;
	
	//Getters and setters of fields.
	
	private double getRotateSpeed() {
		return rotateSpeed;
	}
	
	private float getAbsoluteHeading() {
		return absoluteHeading;
	}

	private void setAbsoluteHeading(float absoluteHeading) {
		this.absoluteHeading = absoluteHeading;
	}

	private float getX() {
		return x;
	}

	private void setX(float x) {
		this.x = x;
	}

	private float getY() {
		return y;
	}

	private void setY(float y) {
		this.y = y;
	}

	private Action getCurrentAction() {
		return currentAction;
	}

	private void setCurrentAction(Action currentAction) {
		this.currentAction = currentAction;
	}

	private double getCurrentArgument() {
		return currentArgument;
	}

	private void setCurrentArgument(double currentArgument) {
		this.currentArgument = currentArgument;
	}

	private boolean isWaiting() {
		return isWaiting;
	}

	private void setWaiting(boolean isWaiting) {
		this.isWaiting = isWaiting;
	}

	private long getTimestamp() {
		return timestamp;
	}

	private void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	private long getCurrentActionETA() {
		return currentActionETA;
	}

	private void setCurrentActionETA(long currentActionETA) {
		this.currentActionETA = currentActionETA;
	}

	private double getTravelSpeed() {
		return travelSpeed;
	}
	
	//Implementation of abstract methods
	
	/**
	 * Returns whether or not the simulator robot is still moving.
	 */
	public boolean isMoving() {
		return System.currentTimeMillis()<getCurrentActionETA();
	}
	
	public void moveBackward() {
		moveBackward(Float.MAX_VALUE, false);
	}
	
	public void moveBackward(final float distance, final boolean wait) {
		setCurrentAction(Action.TRAVEL);
		setCurrentArgument(-distance);
		initializeMove(wait);	
	}
	
	public void moveForward() {
		moveForward(Float.MAX_VALUE, false);
	}
	
	public void moveForward(final float distance, final boolean wait) {
		setCurrentAction(Action.TRAVEL);
		setCurrentArgument(distance);
		initializeMove(wait);	
	}

	/**
	 * Initializes the fields that are universal to all moves. Sleeps the current thread if needed.
	 * @param wait
	 */
	private void initializeMove(final boolean wait) {
		setWaiting(wait);
		setTimestamp(System.currentTimeMillis());
		calculateCurrentActionETA();
		if(wait == true) {
			try {
				Thread.sleep(System.currentTimeMillis()-getCurrentActionETA());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			setWaiting(false);
		}
	}
	
	public int readSensorLight() {
		// TODO
		return 0;
	}
	
	public int readSensorUltraSonic() {
		// TODO
		return 255;
	}
	
	@Override
	public void setTravelSpeed(double speed) {
		this.travelSpeed = speed;
	}
	
	//TODO:@Override
	public void setRotateSpeed(double speed) {
		this.rotateSpeed = speed;
	}
	
	@Override
	public void stop() {
		// TODO
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void turnLeft() {
		turnLeft(Float.MAX_VALUE,false);
	}
	
	public void turnLeft(final float angle, final boolean wait) {
		setCurrentAction(Action.ROTATE);
		setCurrentArgument(angle);
		initializeMove(wait);		
	}
	
	public void turnRight() {
		turnLeft(Float.MAX_VALUE,false);
	}
	
	public void turnRight(final float angle, final boolean wait) {
		setCurrentAction(Action.ROTATE);
		setCurrentArgument(-angle);
		initializeMove(wait);
	}

	@Override
	public float getAngleIncrement() {
		//TODO: dependant on how real NXT side works Math.abs(...).
		return  getHeading() - getAbsoluteHeading();
	}
	
	//TODO:@Override
	private float getHeading(){
		float result = getAbsoluteHeading();
		if(getCurrentAction() == Action.ROTATE){
			Long elapsedTime = System.currentTimeMillis() - getTimestamp();
			double arg = getCurrentArgument();
			result += (arg/Math.abs(arg)) * elapsedTime*getRotateSpeed();
		}
		return result;
	}
	
//	//TODO:@Override
//	public getX(){
//		
//	}
//	
//	//TODO:@Override
//	public getY(){
//		
//	}
	
	public Orientation getOrientation() {
		// TODO:
		//	This method will provide position & heading information
		//	It replaces the getPosition() method below
		return new Orientation(0F, 0F, 0F);
	}

	//TODO:@Override
	public float[] getPosition(){
		throw new UnsupportedOperationException();
	}
	
//TODO:
/*
 * Idee: 
 * Voor Licht sensor:
 * 
	 * Gebruik afbeelding. (Standaard een wit kruis en de borders) 
	 * Voor barcode genereer op de afbeelding die barcode.
	 * Voor elke x en y coordinaat (voor het gegeven doolhof) return de value van die afbeelding.
	 * 
	 * OF
	 * 
	 * Bereken elke waarde aangezien de plaatsing van het witte kruis (en de randen) altijd bekend is.
	 * 
 * 
 * Voor Sonar:
 * 
 	* Bereken de afstand tot de eerste volgende muur in de huidige heading.
 * 
 * 
 * 
 * 
 * 
 * 
 */
	/**
	 * Calculates the ETA of the current action. If action is infinitely long (e.g. "moveForward()") the ETA is set to Long.MAX_VALUE.
	 */
	private void calculateCurrentActionETA(){
		long currentActionETA;
		double arg = Math.abs(getCurrentArgument());
		if(arg == Float.MAX_VALUE){
			currentActionETA = Long.MAX_VALUE;
		} else {
	 		currentActionETA = getTimestamp();
			switch (getCurrentAction()) {
			case TRAVEL:
				currentActionETA +=  (long) (arg/getTravelSpeed());
				break;
	
			case ROTATE:
				currentActionETA +=  (long) (arg/getRotateSpeed());
				break;
			default:
				break;
			}
		}
		setCurrentActionETA(currentActionETA);	
	}

}
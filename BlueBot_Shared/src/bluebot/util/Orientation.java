package bluebot.util;



/**
 * 
 * @author Ruben Feyen
 */
public class Orientation extends Position {
	
	private float heading;
	
	
	public Orientation() {
		this(0F, 0F, 0F);
	}
	public Orientation(final float x, final float y,
			final float heading) {
		super(x, y);
		this.heading = heading;
	}
	
	
	
	public float getHeading() {
		return heading;
	}
	
}
package bluebot.io;


import bluebot.graph.Tile;
import bluebot.sensors.SensorType;



/**
 * This {@link Translator} implementation provides translation
 * for any server-to-client traffic
 * 
 * @author Ruben Feyen
 */
public class ServerTranslator extends Translator {
	
	public ServerTranslator(final Connection connection) {
		super(connection);
	}
	
	
	
	public void notifySpeed(final int percentage) {
		sendPacket(getPacketFactory().createConfigSpeed(percentage));
	}
	
	public void sendDebug(final String msg) {
		sendPacket(getPacketFactory().createDebug(msg));
	}
	
	public void sendError(final String msg) {
		sendPacket(getPacketFactory().createError(msg));
	}
	
	public void sendMessage(final String msg, final String title) {
		sendPacket(getPacketFactory().createMessage(msg, title));
	}
	
	public void sendMotion(final float x, final float y,
			final float body, final float head) {
		sendPacket(getPacketFactory().createMotion(x, y, body, head));
	}
	
	public void sendSensorValue(final SensorType type, final int value) {
		sendPacket(getPacketFactory().createSensorResponse(type, value));
	}
	
	public void sendTile(final Tile tile) {
		sendPacket(getPacketFactory().createTile(tile));
	}
	
}
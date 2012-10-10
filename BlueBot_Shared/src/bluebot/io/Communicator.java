package bluebot.io;


import java.io.IOException;

import bluebot.io.protocol.Packet;
import bluebot.io.protocol.PacketHandler;



/**
 * 
 * @author Ruben Feyen
 */
public class Communicator implements Runnable {
	
	private Connection connection;
	private PacketHandler handler;
	private Thread thread;
	
	
	public Communicator(final Connection connection, final PacketHandler handler) {
		this.connection = connection;
		this.handler = handler;
	}
	
	
	
	public final void run() {
		try {
			for (Packet packet;;) {
				try {
					System.out.println("Reading packet");
					packet = connection.readPacket();
					System.out.println("Packet # " + packet.getOpcode());
					handler.handlePacket(packet);
				} catch (final IOException e) {
					System.out.println("ERROR");
					e.printStackTrace();
					// TODO: Handle the error
					continue;
				} catch (final NullPointerException e) {
					throw new InterruptedException("DEBUG");
				}
			}
		} catch (final InterruptedException e) {
			// This will only occur when the thread has been requested to stop
			System.out.println("INTERRUPTED");
		}
	}
	
	public synchronized void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		}
	}
	
	public synchronized void stop() {
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
	}
	
}
package bluebot.testing;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTConnector;
import lejos.pc.comm.NXTInfo;

public class BTController extends Thread{
	
	private NXTConnector connection;
	private DataOutputStream dos;
	private DataInputStream dis;
	
	public BTController(){
		connection = null;
	}
	@Override
	public void run(){
		
		
	}
	
	public boolean connect(){
		connection = new NXTConnector();
		System.out.println("Connecting to a bluetooth device.");
		
		boolean connected = connection.connectTo("btspp://");
		
		if (!connected) {
			System.out.println("Failed to connect to a bluetooth device.");
			return false;
		}else{
			dos = new DataOutputStream(connection.getOutputStream());
			dis = new DataInputStream(connection.getInputStream());
			return true;
		}
		
	}
	
	public boolean isConnected(){
		return (dos != null && dis != null && connection != null);
	}
	
	public void disConnect(){
		try{
			connection.close();
			connection = null;
			dis.close();
			dis = null;
			dos.close();
			dos = null;
		}catch(IOException e){
			System.out.println(e.getMessage());
		}
	}
	/**
	 * 
	 * @throws IOException
	 *
	public void sendCommand() throws IOException{
		if(!this.isConnected()){
			throw new IOException("Now bluetooth connection available.");
		}
		try{
			while(true){
			dos.writeChars(c.getVal());
			dos.flush();
		}catch(IOException e){
			Debug.print(e.getMessage());
		}
	}*/
	
	public NXTInfo[] getBricks(){
		connection = new NXTConnector();			
		NXTInfo[] nxtInfo = connection.search(null,null,NXTCommFactory.BLUETOOTH);
		return nxtInfo;
	
	}

}

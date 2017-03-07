package hw2;

import java.net.Socket;

public class Lock implements LeaderListener {

	private String lockingString;
	private Server server;
	private LockListener lockListener;
	Socket socket;
	
	/**
	 * things to expect from server
	 */
	private static final int LOCK_ACQUIRED = 1;
	private static final int WAITING = 2;
	private static final int RELEASE_RECEIVED = 3;
	
	/**
	 * things to send to server
	 */
	private static final int STRING_TO_LOCK_ON = 4;
	private static final int RELEASE = 5;
	private static final int ID = 6;
	
	/**
	 * construct the locking service
	 * @param str
	 * the string you want to lock on
	 */
	
	
	public Lock(String str, LockListener lockListener){
		this.lockingString = str;
		this.lockListener = lockListener;
		
		MasterTracker.getMasterServer(this);
	}
	
	@Override
	public void onLeaderChange(Server server) {
		// TODO Auto-generated method stub
		
		//stop the connection with the current one 
		//start a new connection with the new server
		this.server = server;
		
	}
	
	
	
	private void startConnection(){
		
		String ip = server.ip;
		String port = server.port;
		
		//
	}
	
	
	private void stopConnection(){
		//
	}

}

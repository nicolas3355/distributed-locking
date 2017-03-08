package hw2;

import java.net.Socket;

public abstract class Lock extends Thread implements LeaderListener {

	private String lockingString;
	private Server server;
	Socket socket;
	private MasterTracker masterTracker;
	private ClientState currentState = ClientState.Waiting;
	
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
	
	
	public Lock(String str){
		this.lockingString = str;
	}
	
	@Override
	public void onLeaderChange(Server server) {
		// TODO Auto-generated method stub
		
		//stop the connection with the current one 
		//start a new connection with the new server
		if(this.server != null && this.server.equals(server)) return;
		this.server = server;
		
		if(currentState == ClientState.Waiting){
			//stop current connection 
			//start a new connection
		} else if(currentState == ClientState.Executing){
			// set the new server
			// when back from executing 
			//start a new connection to the server and send a release to there
		} else if(currentState == ClientState.PostExecuting){
			//send release to the new server
		} else if (currentState == ClientState.Finsihed){
			//close any open connection if there is something opened
		}
		
		//check my state did i enter the critical section?
		//before
		//entered ctritical section
		//left ctricial section but not send release
		//sent release and finished
		
		stopConnection();
		startConnection();
		
	}
	
	public void run(){
		masterTracker = MasterTracker.getMasterServer(this);
		server = masterTracker.getLeader();
		if(server == null) currentState = ClientState.Waiting;
	}
	
	private void startConnection(){
		
		String ip = server.ip;
		int port = server.port;
		//open a stay a live connection
		//send a string to lock on
		
		// send lock acuired
		onLockReceived();
		//send release 
		masterTracker.unregisterLeaderChangeListener(this);
		masterTracker = null;
	}
	
	
	private void stopConnection(){
		//
	}
	
	protected abstract void onLockReceived();

}

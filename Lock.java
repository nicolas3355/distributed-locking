package hw2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public abstract class Lock extends Thread implements LeaderListener {

	private String lockingString;
	private Server server;
	private Socket socket;
	private String id;

	private MasterTracker masterTracker;
	private ClientState currentState = ClientState.Waiting;

	/**
	 * things to expect from server
	 */
	private static final String LOCK_ACQUIRED = "1";
	private static final String WAITING = "2";
	private static final String RELEASE_RECEIVED = "3";

	/**
	 * things to send to server
	 */
	private static final String STRING_TO_LOCK_ON = "4";
	private static final String RELEASE = "5";
	private static final String ID = "6";

	/**
	 * construct the locking services
	 * @param str
	 * the string you want to lock on
	 */


	public Lock(String lockingString, String id){
		this.lockingString = lockingString;
	}

	@Override
	public void onLeaderChange(Server server) {
		// TODO Auto-generated method stub

		//stop the connection with the current one 
		//start a new connection with the new server
		if(this.server != null && this.server.equals(server)) return;
		this.server = server;

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
		try {
			socket = new Socket(ip, port);
			socket.setKeepAlive(true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println(ID);
			out.println(id);
			if(currentState == ClientState.Waiting){
				out.println(STRING_TO_LOCK_ON);
				out.println(lockingString);
			}else if(currentState == ClientState.PostExecuting){
				out.println(RELEASE);
			}
			while(true){
				String str = in.readLine();
				if(str == null) continue;

				if(str.contains(WAITING)){
					currentState = ClientState.Waiting;
				}else if(str.contains(LOCK_ACQUIRED)){
					currentState = ClientState.Executing;
					onLockReceived();
					currentState = ClientState.PostExecuting;
					out.println(RELEASE);
				}else if(str.contains(RELEASE_RECEIVED)){
					currentState = ClientState.Finsihed;
					masterTracker.unregisterLeaderChangeListener(this);
					masterTracker = null;
					out.close();
					in.close();
					socket.close();
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void stopConnection(){
		if(socket != null)
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
	}

	protected abstract void onLockReceived();

}

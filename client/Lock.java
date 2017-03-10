package hw2.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import hw2.server.Host;
import hw2.utils.ConfReader;
import hw2.utils.Messages;

public abstract class Lock extends Thread implements LeaderListener {

	private String lockingString;
	private Host server;
	private Socket socket;
	private String id;
	
	private MasterTracker masterTracker;
	private ClientState currentState = ClientState.Waiting;
	public static ConfReader confreader;
	/**
	 * construct the locking services
	 * @param str
	 * the string you want to lock on
	 */


	public Lock(String lockingString, String id){
		if(confreader == null){
			confreader = new ConfReader(id);
			confreader.readConfiguration();
		}
		this.lockingString = lockingString;
		this.id = id;
		System.out.println("intializtion");

	}

	@Override
	public void onLeaderChange(Host server) {
		// TODO Auto-generated method stub

		//stop the connection with the current one 
		//start a new connection with the new server
		if(this.server != null && this.server.equals(server)) return;
		this.server = server;
		System.out.println("leader changed");

		stopConnection();
		startConnection();

	}

	public void run(){
		System.out.println("running");
		masterTracker = MasterTracker.getMasterServer(this);
		System.out.println("trying to reach the leader");

		server = masterTracker.getLeader();
		if(server == null) currentState = ClientState.Waiting;
		startConnection();
	}

	private void startConnection(){

		String ip = server.getIpAddress();
		int port = server.getPort();
		try {
			System.out.println("trying to reach the leader");
			socket = new Socket(ip, port);
			socket.setKeepAlive(true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

			if(currentState == ClientState.Waiting){
				out.println(Messages.STRING_TO_LOCK_ON);
				out.println(lockingString);
			}else if(currentState == ClientState.PostExecuting){
				out.println(Messages.RELEASE);
			}			
			out.println(id);
			while(true){
				String str = in.readLine();
				if(str == null) continue;

				if(str.contains(""+Messages.WAITING)){
					currentState = ClientState.Waiting;
				}else if(str.contains(""+Messages.LOCK_ACQUIRED)){
					System.out.println("lock acquired");
					currentState = ClientState.Executing;
					//start a new thread and pummp heart beat
					Thread thread = sendHeartBeat(out);
					thread.start();
					onLockReceived();
					thread.interrupt();
					//kill that thread and continue
					
					currentState = ClientState.PostExecuting;
					out.println(Messages.RELEASE);
					System.out.println("sent release");
				}else if(str.contains(""+Messages.RELEASE_RECEIVED)){
					System.out.println("received release");
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
			System.out.println("starting election");
			masterTracker.triggerElection();
		} finally {
			stopConnection();
		}
	}


	private Thread sendHeartBeat(final PrintWriter printWriter){
		Thread thread = new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (true){
					
					try {
						Thread.sleep(1000);
						printWriter.println(Messages.HEART_BEAT);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						}					
					
				}
			}
			
		});
		return thread;
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

package hw2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MasterTracker extends Thread{

	private static final int TRIGGER_ELECTION = 7;
	private static final int RECEIVED_LEADER = 8; 
	
	private static MasterTracker masterTracker;
	private Server server;
	private boolean listening = false;
	private ArrayList<LeaderListener> listeners;
	//wait for two seconds
	private static final int timeOutInMilliseconds = 2000;

	private int[] ports = {12,123,31,3234};
	private String[] servers = {"127.0.0.1","127.0.0.1","127.0.0.1","127.0.0.1"};

	private MasterTracker(){}

	public  static MasterTracker getMasterServer(LeaderListener listener){
		if (MasterTracker.masterTracker == null) masterTracker = new MasterTracker();
		if(!masterTracker.listening) masterTracker.run();
		
		masterTracker.listeners.add(listener);
		return masterTracker;
	}

	public Server getLeader(){
		if(server == null) triggerElection();
		return server;
	}
	public void triggerElection(){
		new Thread (){
			public void run(){
				for (int i=0;i<ports.length;i++){
					try {
						Socket socket = new Socket(servers[i],ports[i]);
						PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
						out.println(TRIGGER_ELECTION);
						out.close();
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};

	}

	public void run(){
		while (true){
			try {
				startListening();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}


	@SuppressWarnings("resource")
	private void startListening() throws IOException{
		//listening code

		listening = true;
		ServerSocket listener = new ServerSocket(9898);

		listener.setSoTimeout(timeOutInMilliseconds);
		Socket socket = listener.accept();

		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		//trigger an election to surely get a response
		triggerElection();
		while (true) {
			String input = in.readLine();
			if (input == null || input.equals(".")) {
				break;
			}else if(input.contains("6")) {
				String id = input.split("\n")[1].trim();
				server = Server.getServerFromId(id);
				
				for(LeaderListener leaderListener : listeners){
					leaderListener.onLeaderChange(server);
				}
				out.println(RECEIVED_LEADER);
			}
			
		}
	}
	
	public void unregisterLeaderChangeListener(LeaderListener listener){
		for(LeaderListener leaderListener:listeners){
			if(leaderListener == listener){
				listeners.remove(listener);
			}
		}
		
	}


}

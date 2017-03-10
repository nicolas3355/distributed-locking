package hw2.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import hw2.server.Host;
import hw2.utils.ConfReader;
import hw2.utils.Messages;

public class MasterTracker extends Thread{



	private static MasterTracker masterTracker;
	private Host server;
	private boolean listening = false;
	private List<LeaderListener> listeners;

	private MasterTracker(){
		listeners = Collections.synchronizedList(new ArrayList<>());
	}

	public  static MasterTracker getMasterServer(LeaderListener listener){
		System.out.println("enterd master tracker");
		if (MasterTracker.masterTracker == null) {
			masterTracker = new MasterTracker();
		}
		if(!masterTracker.listening) masterTracker.start();
		
		masterTracker.listeners.add(listener);
		return masterTracker;
	}

	public Host getLeader(){
		if(server == null) server = Lock.confreader.getInitialLeader();
		return server;
	}
	
	public void triggerElection(){
		new Thread (){
			public void run(){
				System.out.println("election");
				Host[] servers = Lock.confreader.getServers();
				for (int i=0;i<servers.length;i++){
					try {
						Socket socket = new Socket(servers[i].getIpAddress(),servers[i].getPort());
						PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
						out.println(Messages.ELECTION_START);
						out.close();
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();

	}

	public void run(){
		while(true){
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
		System.out.println("listenning to the new leader");
		listening = true;
		ServerSocket listener = new ServerSocket(ConfReader.getCurrentHost().getPort());
		Socket socket = listener.accept();

		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

		while (true) {
			String input = in.readLine();
			System.out.println("server is saying "+input);
			if (input == null || input.equals(".")) {
				break;
			}else if(input.contains(Messages.NEW_LEADER+"")) {
				String id = input.split("\n")[1].trim();
				server = Lock.confreader.getServerFromId(Integer.parseInt(id));

				for(LeaderListener leaderListener : listeners){
					leaderListener.onLeaderChange(server);
				}
				out.println("received the leader");
			}

		}
	}

	public void unregisterLeaderChangeListener(LeaderListener listener){
		listeners.remove(listener);
		if(listeners.size() == 0) masterTracker = null;
	}


}

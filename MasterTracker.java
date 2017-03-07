package hw2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MasterTracker extends Thread{
	
	private static MasterTracker masterTracker;
	private Server server;
	private boolean listening = false;
	private ArrayList<LeaderListener> listeners;
	//wait for one millisecond
	private static final int timeOutInMilliseconds = 1000;

	private MasterTracker(){}
	
	public  static MasterTracker getMasterServer(LeaderListener listener){
		if (MasterTracker.masterTracker == null) masterTracker = new MasterTracker();
		if(!masterTracker.listening) masterTracker.run();
		
		masterTracker.listeners.add(listener);
		return masterTracker;
	}
	
	public Server getLeader(){
		if(server == null) System.out.println("synchronous");
		return server; 
	}
	
	public void run(){
		//startListening();
		while (true){
			
		}
	}
	
	private void startListening() throws IOException{
		//listening code
		
		listening = true;
		for(LeaderListener listener : listeners){
			listener.onLeaderChange(server);
		}
		ServerSocket listener = new ServerSocket(9898);
		listener.setSoTimeout(timeOutInMilliseconds);
		Socket socket = listener.accept();
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Get messages from the client, line by line; return them
        // capitalized
        while (true) {
            String input = in.readLine();
            if (input == null || input.equals(".")) {
                break;
            }
            out.println(input.toUpperCase());
        }
	}
	
	
}

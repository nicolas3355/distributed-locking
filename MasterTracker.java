package hw2;

import java.util.ArrayList;

public class MasterTracker {
	private static MasterTracker masterTracker;
	private Server server;
	private boolean listnening;
	private ArrayList<LeaderListener> listeners;
	
	private MasterTracker(){
		
	}
	
	public MasterTracker getMasterServer(LeaderListener listener){
		if (MasterTracker.masterTracker == null) masterTracker = new MasterTracker();
		if(!listnening) startListening();
		
		listeners.add(listener);
		return masterTracker;
	}
	
	public Server getLeader(){
		return server; 
	}
	
	private void startListening(){
		//listening code
		
		listnening = true;
		for(LeaderListener listener : listeners){
			listener.onLeaderChange(server);
		}
		
	}
	
	
}

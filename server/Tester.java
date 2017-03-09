package hw2.server;

import hw2.utils.ConfReader;

public class Tester {

	public static void main(String args[]){
		
		
		ConfReader confreader = new ConfReader(args[0]);
		confreader.readConfiguration();
		int id = Integer.parseInt(args[0]);
		Host server = confreader.getServerFromId(id);
		try {
			new ControlSite(server.getPort(),id,confreader.getServers()
					,confreader.getClients(),confreader.getInitialLeader().getId()).start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

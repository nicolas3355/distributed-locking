package hw2.server;

import hw2.utils.ConfReader;

public class Tester {

	public static void main(String args[]){
		int id = Integer.parseInt(args[0]);
		
		ConfReader confreader = new ConfReader();
		confreader.readConfiguration();
		Server server = confreader.getServerFromId(id);
		try {
			new ControlSite(server.getPort(),id,confreader.getServers()
					,confreader.getClients(),confreader.getInitialLeader().getId()).start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

package hw2.utils;

import java.io.File;
import java.util.Scanner;

import hw2.server.Server;
import hw2.server.Process;

public class ConfReader {

	private Server[] servers;
	private Process[] clients;
	
	public void readConfiguration(){
		Scanner scan;
		try {
			scan = new Scanner(new File("configuration"));
		
		int n = scan.nextInt();
		servers = new Server[n];

		for (int i = 0; i < n; i++)
			servers[i] = new Server(scan.nextInt(), scan.next(), scan.nextInt());

		int m = scan.nextInt();
		clients = new Process[m];

		for (int i = 0; i < m; i++)
			clients[i] = new Process(scan.nextInt(), scan.next(), scan.nextInt());

		scan.close();
		} catch(Exception e){
			System.out.println("error processing configuration file");
			e.printStackTrace();
		}		
	}
	
	public Server[] getServers() {
		return servers;
	}
	
	public Process[] getClients() {
		return clients;
	}
	
	public Server getServerFromId(int id){
		for (int i=0;i<servers.length;i++){
			if(servers[i].getId() == id)
				return servers[i];
		}
		return null;
	}
	
	public Server getInitialLeader(){
		Server leader = servers[0];
		for(int i=0;i<servers.length;i++){
			if(servers[i].getId()>leader.getId()) leader = servers[i];
		}
		return leader;
	}
	
}

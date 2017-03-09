package hw2.utils;

import java.io.File;
import java.util.Scanner;

import hw2.client.Client;
import hw2.server.Host;

public class ConfReader {

	private Host[] servers;
	private Host[] clients;
	private int id;
	private static Host currentHost;

	public ConfReader(String id) {
		// TODO Auto-generated constructor stub
		this.id = Integer.parseInt(id);
	}
	public void readConfiguration(){
		Scanner scan;
		try {
			scan = new Scanner(new File("configuration"));

			int n = scan.nextInt();
			servers = new Host[n];

			for (int i = 0; i < n; i++){
				servers[i] = new Host(scan.nextInt(), scan.next(), scan.nextInt());
				if(servers[i].getId() == id) ConfReader.currentHost = servers[i];
			}

			int m = scan.nextInt();
			clients = new Host[m];

			for (int i = 0; i < m; i++){
				clients[i] = new Host(scan.nextInt(), scan.next(), scan.nextInt());
				if(clients[i].getId() == id) ConfReader.currentHost = clients[i];
			}
			scan.close();
		} catch(Exception e){
			System.out.println("error processing configuration file");
			e.printStackTrace();
		}		
	}

	public Host[] getServers() {
		return servers;
	}

	public Host[] getClients() {
		return clients;
	}

	public Host getServerFromId(int id){
		for (int i=0;i<servers.length;i++){
			if(servers[i].getId() == id)
				return servers[i];
		}
		return null;
	}


	public Host getInitialLeader(){
		Host leader = servers[0];
		for(int i=0;i<servers.length;i++){
			if(servers[i].getId()>leader.getId()) leader = servers[i];
		}
		return leader;
	}
	public static Host getCurrentHost() {
		// TODO Auto-generated method stub
		return currentHost;

	}

}

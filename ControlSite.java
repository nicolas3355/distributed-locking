import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ControlSite {

	public static final int WAIT_MESSAGE = 2;
	public static final int PERMISSION_MESSAGE = 1;
	public static final int RELEASE_RECEIVED = 3;
	public static final int ELECTION_START_MESSAGE = 7;
	public static final int ELECTION_OK_MESSAGE = 8;
	public static final int NEW_LEADER = 9;
	public static final int PROCESS_TIMEOUT_INTERVAL = 3000;
	public static final int CONTROL_SITE_TIMEOUT_INTERVAL = 3000;

	private int listeningPort; // port this server listens on 
	private int controlSiteId; // my id
	private Server[] controlSites; // all control sites/ master processes
	private Client[] clients; // all client processes
	private int leaderId; // id of the leader control site
	private ConcurrentHashMap<String, BlockingQueue<Socket>> criticalSectionsRequests; // critical sections queues

	private boolean runningElection; // leader election in progress

	public ControlSite(int listeningPort, int controlSiteId,
			Server[] controlSites, Client[] clients, int leaderId) {

		this.listeningPort = listeningPort;
		this.controlSiteId = controlSiteId;
		this.controlSites = controlSites;
		this.clients = clients;
		this.leaderId = leaderId;
		this.criticalSectionsRequests = new ConcurrentHashMap();
		this.runningElection = false;
	}

	public void start() throws Exception {
		new ServerListener().run();
	}

	public void electLeader() {

		int okCount = 0;
		for (int i = controlSiteId + 1; i < controlSites.length; i++) { 

			// send leader election message to all control sites with id > my id
			try {

				long startTime = System.currentTimeMillis();
				Socket sock = new Socket(clients[i].getIpAddress(),
						clients[i].getPort());

				PrintWriter pout = new PrintWriter(sock.getOutputStream(), true);
				pout.println(ControlSite.ELECTION_START_MESSAGE);
				pout.flush();

				w: while (System.currentTimeMillis() - startTime < CONTROL_SITE_TIMEOUT_INTERVAL) {

					InputStream in = sock.getInputStream();
					BufferedReader bin = new BufferedReader(
							new InputStreamReader(in));

					if (bin.ready()) {

						String line = bin.readLine();
						int message = Integer.parseInt(line);
						if (message == ControlSite.ELECTION_OK_MESSAGE) {
							okCount++;
							break w;
						}
					}
				}
			} catch (Exception e) {

			}
		}

		if (okCount == 0) {

			leaderId = controlSiteId;
			try {
				for (int i = 0; i < controlSites.length; i++) {

					Socket sock = new Socket(controlSites[i].getIpAddress(),
							controlSites[i].getPort());

					PrintWriter pout = new PrintWriter(sock.getOutputStream(),
							true);
					pout.println(NEW_LEADER);
					pout.println(controlSiteId);
					pout.flush();
					sock.close();
				}

				for (int i = 0; i < clients.length; i++) {

					Socket sock = new Socket(clients[i].getIpAddress(),
							clients[i].getPort());
					PrintWriter pout = new PrintWriter(sock.getOutputStream(),
							true);
					pout.println(NEW_LEADER);
					pout.println(controlSiteId);
					pout.flush();

					sock.close();
				}
			} catch (Exception e) {

			}
		}
	}

	private class ServerListener extends Thread {

		@Override
		public void run() {

			try {

				ServerSocket sock = new ServerSocket(listeningPort);

				while (true) {

					Socket client = sock.accept();
					InputStream in = client.getInputStream();
					BufferedReader bin = new BufferedReader(
							new InputStreamReader(in));
					PrintWriter pout = new PrintWriter(
							client.getOutputStream(), true);

					String line = bin.readLine();
					int message = Integer.parseInt(line);
					if (leaderId == controlSiteId
							&& message == Process.REQUEST_MESSAGE) {

						String requestedCS = bin.readLine();
						new RequestHandler(client, requestedCS).run();
					} else if (message == Process.ELECTION_MESSAGE
							|| message == ControlSite.ELECTION_START_MESSAGE) {

						if (!runningElection) {

							runningElection = true;
							electLeader();
						}

						if (message == ControlSite.ELECTION_START_MESSAGE) {
							pout.println(ELECTION_OK_MESSAGE);
						}
					} else if (message == NEW_LEADER) {

						line = bin.readLine();
						leaderId = Integer.parseInt(line);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private class RequestHandler extends Thread {

		private Socket client;
		private String requestedCS;

		public RequestHandler(Socket client, String requestedCS) {

			this.client = client;
			this.requestedCS = requestedCS;
		}

		public void run() {

			if (criticalSectionsRequests.containsKey(requestedCS))
				try {
					criticalSectionsRequests.get(requestedCS).put(client);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			else {
				criticalSectionsRequests.put(requestedCS,
						new LinkedBlockingQueue());
				try {
					criticalSectionsRequests.get(requestedCS).put(client);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			while (criticalSectionsRequests.get(requestedCS).peek() != client)
				; // wait

			if (criticalSectionsRequests.get(requestedCS).peek() == client) {

				try {

					InputStream in = client.getInputStream();
					BufferedReader bin = new BufferedReader(
							new InputStreamReader(in));
					PrintWriter pout = new PrintWriter(
							client.getOutputStream(), true);

					pout.println(PERMISSION_MESSAGE);
					long startTime = System.currentTimeMillis();

					while (true) {

						if (System.currentTimeMillis() - startTime >= PROCESS_TIMEOUT_INTERVAL) {

							System.out.println("Process crashed");
							criticalSectionsRequests.get(requestedCS).take();
							client.close();
							break;
						} else {

							if (bin.ready()) {

								String line;
								if ((line = bin.readLine()) != null) {

									int message = Integer.parseInt(line);

									if (message == Process.HEART_BEAT_MESSAGE) {
										startTime = System.currentTimeMillis();
									}

									else if (message == Process.RELEASE_MESSAGE) {

										criticalSectionsRequests.get(
												requestedCS).take();
										client.close();
										break;
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
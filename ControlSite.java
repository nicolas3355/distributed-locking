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

	/* requests messages */

	public static final int WAIT = 2;
	public static final int PERMISSION = 1;
	public static final int RELEASE_RECEIVED = 3;

	/* update queue messages */
	public static final int OFFER_QUEUE = 10;
	public static final int POLL_QUEUE = 11;

	/* election messages */
	public static final int ELECTION_START = 7;
	public static final int ELECTION_OK = 8;
	public static final int NEW_LEADER = 9;

	public static final int TIMEOUT_INTERVAL = 3000;

	private int listeningPort; // port this server listens on
	private int controlSiteId; // my id
	private Server[] controlSites; // all control sites/ master processes
	private Client[] clients; // all client processes
	private int leaderId; // id of the leader control site
	private ConcurrentHashMap<String, BlockingQueue<Integer>> criticalSectionsRequests; // critical
																						// sections
																						// queues

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

	public Client getClientById(int id) {

		for (int i = 0; i < clients.length; i++)
			if (clients[i].getId() == id)
				return clients[i];
		return null;
	}

	public void addToQueue(String criticalSectionId, int processId) {

		if (criticalSectionsRequests.containsKey(criticalSectionId))
			try {
				// avoid duplicate entries of the same process
				if (criticalSectionsRequests.get(criticalSectionId).size() > 0)
					if (criticalSectionsRequests.get(criticalSectionId).peek() != processId)
						criticalSectionsRequests.get(criticalSectionId).put(
								processId);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		else {
			criticalSectionsRequests.put(criticalSectionId,
					new LinkedBlockingQueue());
			try {
				criticalSectionsRequests.get(criticalSectionId).put(processId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// master needs to update others when it applies changes to its queue
		if (leaderId == controlSiteId)
			updateOthers(OFFER_QUEUE, criticalSectionId, processId);
	}

	public void removeFromQueue(String criticalSectionId, int processId) {

		try {
			if (criticalSectionsRequests.get(criticalSectionId) != null)
				// make sure you're polling the right process
				if (criticalSectionsRequests.get(criticalSectionId).peek() == processId)
					criticalSectionsRequests.get(criticalSectionId).take();

		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// leader needs to update others when it applies changes to its queue
		if (leaderId == controlSiteId)
			updateOthers(POLL_QUEUE, criticalSectionId, processId);
	}

	public void updateOthers(int required, String criticalSectionId,
			int processId) {

		// reliable sync
		for (int i = 0; i < controlSites.length; i++) {

			// since if I'm the leader then all the process of bigger id's are
			// already dead
			// no need to send to them
			if (controlSites[i].getId() < controlSiteId) {

				try {

					Socket sock = new Socket(controlSites[i].getIpAddress(),
							controlSites[i].getPort());

					PrintWriter pout = new PrintWriter(sock.getOutputStream(),
							true);

					pout.println(required);
					pout.println(processId);
					pout.println(criticalSectionId);
					sock.close();

				} catch (Exception e) {

				}
			} else
				break;
		}
	}

	// called by the leader when it wants to apply changes to its queue
	// and by others when they receive the updates from the leader
	public void updateQueue(int required, String criticalSectionId,
			int processId) {

		if (required == ControlSite.POLL_QUEUE)
			removeFromQueue(criticalSectionId, processId);

		else if (required == ControlSite.OFFER_QUEUE)
			addToQueue(criticalSectionId, processId);
	}

	public void electLeader() {

		int okCount = 0;

		for (int i = controlSites.length - 1; i >= 0; i--) {

			// send leader election message to all control sites with id > my id

			if (controlSites[i].getId() > controlSiteId) {

				try {
					// long startTime = System.currentTimeMillis();
					Socket sock = new Socket(controlSites[i].getIpAddress(),
							controlSites[i].getPort());
					sock.setKeepAlive(true);
					sock.setSoTimeout(TIMEOUT_INTERVAL);

					PrintWriter pout = new PrintWriter(sock.getOutputStream(),
							true);
					pout.println(ControlSite.ELECTION_START);

					w: while (true) {
						// w: while (System.currentTimeMillis() - startTime <
						// CONTROL_SITE_TIMEOUT_INTERVAL) {

						InputStream in = sock.getInputStream();
						BufferedReader bin = new BufferedReader(
								new InputStreamReader(in));

						if (bin.ready()) {

							String line = bin.readLine();
							int message = Integer.parseInt(line);
							if (message == ControlSite.ELECTION_OK) {
								okCount++;
								break w;
							}
						}
					}
				} catch (Exception e) {

				}
			} else
				break; // bully algorithm
		}

		// none of the control sites with bigger ids responded
		// become leader by bully algorithm
		if (okCount == 0) {
			leaderId = controlSiteId;
			try {
				for (int i = 0; i < controlSites.length; i++) {

					// since this control site became leader then all sites with
					// bigger ids are dead
					if (controlSites[i].getId() < controlSiteId) {

						Socket sock = new Socket(
								controlSites[i].getIpAddress(),
								controlSites[i].getPort());

						PrintWriter pout = new PrintWriter(
								sock.getOutputStream(), true);
						pout.println(NEW_LEADER);
						pout.println(controlSiteId);
						pout.flush();
						sock.close();
					} else
						break;
				}

				for (int i = 0; i < clients.length; i++) {

					// inform all clients of the new leader
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

				ServerSocket serverSocket = new ServerSocket(listeningPort);
				serverSocket.setSoTimeout(TIMEOUT_INTERVAL);

				while (true) {

					Socket clientSocket = serverSocket.accept();
					InputStream in = clientSocket.getInputStream();
					BufferedReader bin = new BufferedReader(
							new InputStreamReader(in));
					PrintWriter pout = new PrintWriter(
							clientSocket.getOutputStream(), true);

					String line = bin.readLine();
					int message = Integer.parseInt(line);

					if (leaderId == controlSiteId
							&& message == Process.REQUEST_MESSAGE) {

						line = bin.readLine();
						int processId = Integer.parseInt(line);
						String requestedCS = bin.readLine();

						// new thread to handle this request
						new RequestHandler(processId, clientSocket, requestedCS)
								.run();
					}

					// to synchronize changes in the queues
					// when master adds/polls client i to/from queue a
					// it should inform all other control sites to do the same

					else if (message == ControlSite.OFFER_QUEUE
							|| message == ControlSite.POLL_QUEUE) {

						String criticalSectionId = bin.readLine();
						int processId = Integer.parseInt(bin.readLine());

						updateQueue(message, criticalSectionId, processId);

					}

					// election can either be triggered by process or by another
					// server
					else if (message == Process.ELECTION_MESSAGE
							|| message == ControlSite.ELECTION_START) {

						if (!runningElection) {

							runningElection = true;
							electLeader();
						}

						// if received election message from another server
						if (message == ControlSite.ELECTION_START) {
							pout.println(ELECTION_OK);
						}
					}

					else if (message == NEW_LEADER) {

						line = bin.readLine();
						leaderId = Integer.parseInt(line);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// a new RequestHandler thread is created for each request

	private class RequestHandler extends Thread {

		private int processId; // process this thread is handling
		private Socket clientSocket; // the socket this thread is responding to
		private String requestedCS;

		public RequestHandler(int processId, Socket clientSocket,
				String requestedCS) {

			this.processId = processId;
			this.clientSocket = clientSocket;
			this.requestedCS = requestedCS;
		}

		public void run() {

			// add this process to the queue of processes requesting this
			// critical section
			updateQueue(ControlSite.OFFER_QUEUE, requestedCS, processId);

			while (criticalSectionsRequests.get(requestedCS).peek() != processId)
				; // wait for this process' turn in the requested critical
					// section

			if (criticalSectionsRequests.get(requestedCS).peek() == processId) {

				try {

					InputStream in = clientSocket.getInputStream();
					BufferedReader bin = new BufferedReader(
							new InputStreamReader(in));
					PrintWriter pout = new PrintWriter(
							clientSocket.getOutputStream(), true);

					pout.println(PERMISSION);
					// long startTime = System.currentTimeMillis();

					while (true) {

						/*
						 * if (System.currentTimeMillis() - startTime >=
						 * PROCESS_TIMEOUT_INTERVAL) {
						 * 
						 * System.out.println("Process crashed");
						 * criticalSectionsRequests.get(requestedCS).take();
						 * client.close(); break; } else {
						 */
						if (bin.ready()) {

							String line;
							if ((line = bin.readLine()) != null) {

								int message = Integer.parseInt(line);

								/*
								 * if (message == Process.HEART_BEAT_MESSAGE) {
								 * startTime = System.currentTimeMillis(); }
								 * 
								 * else
								 */if (message == Process.RELEASE_MESSAGE) {

									updateQueue(ControlSite.POLL_QUEUE,
											requestedCS, processId);
									pout.println(ControlSite.RELEASE_RECEIVED);
									clientSocket.close();
									break;
								}
							}
						}
					}
					// }
				} catch (Exception e) {

					// process crashed

					System.out.println("Master noticed that process "
							+ processId + " crashed");
					updateQueue(ControlSite.POLL_QUEUE, requestedCS, processId);
					e.printStackTrace();
				}
			}
		}
	}
}
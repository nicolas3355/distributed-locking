package hw2.client;

import hw2.server.Server;

public interface LeaderListener {
	void onLeaderChange(Server server);
}

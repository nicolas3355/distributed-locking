package hw2.client;

import hw2.server.Host;

public interface LeaderListener {
	void onLeaderChange(Host server);
}

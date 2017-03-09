package hw2.utils;

public class Messages {

	/**
	 * things to expect from server
	 */
	public static final int LOCK_ACQUIRED = 1;
	public static final int WAITING = 2;
	public static final int RELEASE_RECEIVED = 3;

	/**
	 * things to send to server
	 */
	public static final int STRING_TO_LOCK_ON = 4;
	public static final int RELEASE = 5;
	public static final int ID = 6;

	/* update queue messages */
	public static final int OFFER_QUEUE = 10;
	public static final int POLL_QUEUE = 11;

	/* election messages */
	public static final int ELECTION_START = 7;
	public static final int ELECTION_OK = 8;
	public static final int NEW_LEADER = 9;
}
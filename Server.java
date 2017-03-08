package hw2;

public class Server {

	public String ip;
	public int port;

	public Server(String ip, int port){
		this.ip = ip;
		this.port = port;
	}
	
	public static Server getServerFromId(String id){
		return new Server("123123",12);
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		String ip = ((Server)obj).ip;
		int port = ((Server)obj).port;
		
		if(this.ip == null) return false;
		
		return this.ip.equals(ip) && this.port == port;
	}
}

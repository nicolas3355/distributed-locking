package client;

public class Client{

	public static void main(String[] args){
		while (true){
			if(Math.random() < 0.1){
				String id = args[0];
				new Lock("string",id) {
					
					@Override
					protected void onLockReceived() {
						// TODO Auto-generated method stub
						
					}
				}.start(); 
			}
		}
	}
}

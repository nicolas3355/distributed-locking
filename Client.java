package hw2;

public class Client{

	public static void main(String[] args){
		new Lock("string","id") {
			
			@Override
			protected void onLockReceived() {
				// TODO Auto-generated method stub
				
			}
		}.start(); 
	}
}

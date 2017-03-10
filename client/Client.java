package hw2.client;

public class Client{

	public static void main(String[] args) throws InterruptedException{

		String id = args[0];
		Lock lock = new Lock("string",id) {

			@Override
			protected void onLockReceived() {
				// TODO Auto-generated method stub
				System.out.println("i am in the critical section");
			}
		};
		lock.start();

		//lock.join();
	}
}

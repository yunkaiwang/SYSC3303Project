package project;

public class ThreadLog {
	// Thread printing function
	public static void print(String msg) {
		System.out.println("Thread #" + Thread.currentThread().getId() + ": " + msg);
	}
}

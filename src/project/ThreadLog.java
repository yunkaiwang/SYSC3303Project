package project;

public class ThreadLog {
	public static void print(String msg) {
		System.out.println("Thread #" + Thread.currentThread().getId() + ": " + msg);
	}
}

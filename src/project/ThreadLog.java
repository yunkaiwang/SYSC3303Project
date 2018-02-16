package project;

/**
 * This is a helper class for all threads to print information to the console with
 * clearly labeled thread number
 * 
 * @author yunka
 *
 */
public class ThreadLog {
	// Thread printing function
	public static void print(String msg) {
		System.out.println("Thread #" + Thread.currentThread().getId() + ": " + msg);
	}
}

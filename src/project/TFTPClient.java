package project;

import java.util.Scanner;

public class TFTPClient {
	TFTPClient() {
	}
	
	private static void printMenu() {
		System.out.println("Available commands:");
		System.out.println("1. stop - stop the client");
		System.out.println();
	}
	
	private void waitForCommand() {
		Scanner s = new Scanner(System.in);
		while (true) {
			printMenu();
			System.out.print("Command: ");
			String cmdLine = s.nextLine().toLowerCase();
			switch(cmdLine) {
				case "stop":
					System.out.println("Stopping client...Good bye!");
					s.close();
					return;
				default:
					System.out.println("Invalid command, please try again!\n");
			}
		}
	}
	
	public static void main(String[] args) {
		TFTPClient client = new TFTPClient();
		client.waitForCommand();
	}
}

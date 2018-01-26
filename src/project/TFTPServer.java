package project;

import java.util.Scanner;

public class TFTPServer {
	private Mode currentMode;

	TFTPServer() {
		// default mode is quite
		this.currentMode = Mode.QUITE;
	}
	
	private static void printMenu() {
		System.out.println("Available commands:");
		System.out.println("1. help - show the menu");
		System.out.println("2. stop - stop the client");
		System.out.println("3. switch - switch mode");
		System.out.println();
	}

	private void switchMode() {
		switch(this.currentMode) {
		case QUITE:
			this.currentMode = Mode.VERBOSE;
			return;
		case VERBOSE:
			this.currentMode = Mode.QUITE;
			return;
		}
	}
	
	private void waitForCommand() {
		Scanner s = new Scanner(System.in);

		printMenu();
		while (true) {
			System.out.print("Command: ");
			String cmdLine = s.nextLine().toLowerCase();
			switch (cmdLine) {
			case "help":
				printMenu();
				continue;
			case "stop":
				System.out.println("Stopping server...Good bye!");
				s.close();
				return;
			case "switch":
				this.switchMode();
				System.out.println("The mode has been switched to " + this.currentMode + "\n");
				continue;
			default:
				System.out.println("Invalid command, please try again!\n");
			}
		}
	}

	public static void main(String[] args) {
		TFTPServer server = new TFTPServer();
		server.waitForCommand();
	}
}

package project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

/**
 * TFTPHost
 * Common functionalities, attributes that a TFTP host must have
 * 
 * @author yunkai wang
 * 
 */
public abstract class TFTPHost {
	protected String folder; // current directory path
	protected Mode currentMode; // verbose or quite
	
	/**
	 * constructor
	 * 
	 * @param folder
	 * @param mode
	 */
	protected TFTPHost(String folder, Mode mode) {
		this.folder = folder;
		this.currentMode = mode;
	}
	
	/**
	 * getter
	 * 
	 * @return folder
	 */
	protected String getFolder() {
		return folder;
	}
	
	/**
	 * setter
	 * 
	 * @param newFolder
	 */
	protected void setFolder(String newFolder) {
		this.folder = newFolder;
	}
	
	/**
	 * combine file name with current directory path
	 * 
	 * @param filename
	 * @return
	 */
	protected String getFilePath(String filename) {
		return getFolder() + filename;
	}
	
	/**
	 * print current directory
	 */
	protected void printDirectory() {
		System.out.println("Current directory is: " + this.getFolder() + "\n");
	}
	
	/**
	 * Convert an address and port into string for printing
	 * 
	 * @param address
	 * @param port
	 * @return
	 */
	protected static String addressToString(InetAddress address, int port) {
		return address.toString() + ":" + port;
	}
	
	/**
	 * Print current printing mode(verbose/quite)
	 */
	protected void printMode() {
		System.out.println("Current mode is: " + currentMode.mode());
	}
	
	/**
	 * Switch the printing mode(verbose/quite)
	 */
	protected void switchMode() {
		this.currentMode = currentMode.switchMode();
		System.out.println("The mode has been switched to " + this.currentMode + "\n");
	}

	/**
	 * print information stored in TFTPPacket
	 * 
	 * @param info
	 * @param packet
	 * @throws IOException
	 */
	protected void printInformation(String info, TFTPPacket packet) throws IOException {
		System.out.println(info);
		switch (this.currentMode) {
		case QUITE: // don't print detailed information in QUITE mode
			return;
		case VERBOSE: // print detailed information in VERBOSE mode
			System.out.println(packet);
			return;
		}
	}
	
	/**
	 * print list of files under current directory
	 */
	protected void printListFiles() {
		File folder = new File(this.getFolder());
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles)
			System.out.println(file.getName());
		System.out.println();
	}
	
	/**
	 * remove the given file
	 * 
	 * @param filename
	 */
	protected void removeFile(String filename) {
		new File(this.getFilePath(filename)).delete();
	}
	
	/**
	 * create new file with the given file name and file size
	 * 
	 * @param filename
	 * @param fileSize
	 */
	protected void createFile(String filename, int fileSize) {
		File file;
		FileOutputStream fs;
		try {
			file = new File(this.getFilePath(filename));
			if (file.exists()) {
				System.out.println(filename + " already exists, please choose a new file name");
				return;
			} else if (!file.exists()) { // file not exist, we need to create the file
				if (!file.createNewFile()) // failed to create the file
					throw new IOException("Failed to create " + filename);
			}
			fs = new FileOutputStream(file);
			Random rand = new Random();
			// write random bytes to the file
			for (int i = 0; i < fileSize; ++i)
				fs.write(rand.nextInt(fileSize) + 1);
			fs.close();
		} catch (IOException e) { // if an error happens, delete the file
			removeFile(filename);
		}
	}
	
	/**
	 * change current directory path with the given new directory path
	 * 
	 * @param newDirectoryPath
	 */
	protected void switchDirectory(String newDirectoryPath) {
		String prevFolder = this.getFolder(); // previous directory path. used for restoring directory path
		String[] directoryPath = newDirectoryPath.split("/");
		for (String dir : directoryPath) {
			if (dir.length() == 0)
				continue;
			else if (dir.equals("..")) {
				if (this.getFolder().indexOf(File.separator) != -1) {
					this.setFolder(this.getFolder().substring(0,
							this.getFolder().substring(0, this.getFolder().length() - 1).lastIndexOf(File.separator))
							+ File.separator);
				} else {
					System.out.println("New directory path is invalid, please try again.\n");
					this.setFolder(prevFolder); // restore directory path
				}
			} else {
				this.setFolder(this.getFolder() + dir + File.separator);
				File path = new File(this.getFolder());
				if (!path.exists()) {
					System.out.println("New directory path is invalid, please try again.\n");
					this.setFolder(prevFolder); // restore directory path
				}
			}
		}
	}
}

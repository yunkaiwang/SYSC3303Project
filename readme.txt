SYSC 3303 PROJECT

Carleton University Winter 2018 SYSC 3303 Project.

Set up test files:
	Step1: Go to the project directory.
	Step2: Create two folders: client_files, server_files.
	Step3: Create test files in these two folders.

Set up environment:
	Step1: Go to TFTPClient then click run.
	Step2: Go to TFTPServer then click run.
	Step3: Go to TFTPErrorSimulator then click run.

Instruction of using TFTP:
	TFTPClient:
		1. menu   ---> if you want to see the menu, type "menu" then press enter
		2. exit   ---> if you want to stop the client and exit, type "exit" press enter
		3. mode   ---> if you want to see the current mode, type "mode" then press enter
		4. switch ---> if you want to switch mode(verbose, quite, normal, or test), type "switch" then press enter
		5. reset  ---> if you want to reset running mode(test or normal), type "reset" then press enter
		6. read   ---> if you want to read a file from server, type "read test.txt" then press enter
		7. write  ---> if you want to write a file to server, type "write test.txt" then press enter
	TFTPServer:
		1. menu   ---> if you want to see the menu, type "menu" then press enter
		2. exit   ---> if you want to stop the client and exit, type "exit" press enter
		3. mode   ---> if you want to see the current mode, type "mode" then press enter
		4. switch ---> if you want to switch mode(verbose, quite, normal, or test), type "switch" then press enter
	TFTPErrorSimulator:
		For iteration 1, the error simulator doesn't support any command, it will just wait for a new request from
		the client, and forward the request to the server, forward the request from the server to the client, etc.
		
Testing step:
	1. follow the instructions in set up environment to start all the applications
	2. If you want to test normal mode
		2.1 in terminal, type read <filename> to test read request (make sure you have
		<filename> in your server_files folder)
		2.2 in terminal, type write <filename> to test write request (make sure you have
		<filename> in your client_files folder)
		2.3 Type 'mode' to switch the mode
		2.4 Type 'switch' to switch the print mode
	3. If you want to test test mode
		3.1 in terminal, type reset(this will set the running mode to test)
		3.2 same as 2.2
		3.3 same as 2.3
	4. to exit, simply type 'exit'
	5. if you want to change the printing mode in server, in the server terminal, type 'switch'.
	
Explaining the names of files:
	 - Mode.java - enum class for printing mode (verbose/quite)
	 - TFTPAckPacket.java - TFTP ack packet class
	 - TFTPClient.java - TFTP client
	 - TFTPDataPacket.java - TFTP data packet class
	 - TFTPErrorPacket.java - TFTP error packet class (not used in iteration 1)
	 - TFTPErrorSimulator.java - TFTP error simulator
	 - TFTPRequestHandler.java - Request handler thread that will be used by the server
	 - TFTPRequestListner.java - Request listener thread that will be used by the server
	 - TFTPRequestPacket.java - TFTP RRQ/WRQ packet class
	 - TFTPServer.java - TFTP server
	 - ThreadLog.java - Helper class for all different threads to print information
	 - Type.java - TFTP request types(WRQ, RRQ, .. etc)
 
 Breakdown of responsibilities:
 	- Yunkai Wang: most of the coding work
 	- Qingyi Yin: TFTPAckPacket class and all diagrams/documents
 	
 Diagrams:
 	All the diagrams are in the diagram folder.
 	- uml-class-diagram: class diagram of this application
 	- ucm-rrq: UCM for read request
 	- ucm-wrq: UCM for write request
 


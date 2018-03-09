SYSC 3303 PROJECT

Carleton University Winter 2018 SYSC 3303 Project.

Set up test files:  
	Step1: Go to the project directory.  
	Step2: Create test files in these two folders, currently I put some test files in these folders, I put a random.txt in client_files folder, and random.txt and test.txt in server_files folder.  

Set up environment:  
	Step1: Go to TFTPClient then click run.  
	Step2: Go to TFTPServer then click run.  
	Step3: Go to TFTPErrorSimulator then click run.  

Instructions of using our system:  
	TFTPClient:  
		- menu   ---> if you want to see the menu, type "menu" then press enter  
		- exit   ---> if you want to stop the client and exit, type "exit" press enter  
		- mode   ---> if you want to see the current mode, type "mode" then press enter  
		- switch ---> if you want to switch mode(verbose, quite, normal, or test), type "switch" then press enter  
		- reset  ---> if you want to reset running mode(test or normal), type "reset" then press enter  
		- read   ---> if you want to read a file from server, type "read <filename>" then press enter  
		- write  ---> if you want to write a file to server, type "write <filename>" then press enter  
                - la     ---> if you want to see all the files under current directory, type "la" then press enter  
		- pwd/dir---> if you want to see current directory path, type "pwd" or "dir" then press enter  
		- cd     ---> if you want to check the directory path, type "cd <path>" then press enter  
	TFTPServer:  
		- menu   ---> if you want to see the menu, type "menu" then press enter  
		- exit   ---> if you want to stop the client and exit, type "exit" press enter  
		- mode   ---> if you want to see the current mode, type "mode" then press enter  
		- switch ---> if you want to switch mode(verbose, quite, normal, or test), type "switch" then press enter  
		- count  ---> if you want to see current number of running threads, type "count" then press enter  
		- la     ---> if you want to see all the files under current directory, type "la" then press enter  
		- pwd/dir---> if you want to see current directory path, type "pwd" or "dir" then press enter  
		- cd     ---> if you want to check the directory path, type "cd <path>" then press enter  
	TFTPErrorSimulator:  
		For iteration 2, the error simulator doesn't support any command, it will just wait for a new request from the client, and forward the request to the server, forward the request from the server to the client, it will not touch any information stored in the packet.  
		
Testing step:  
	1. follow the instructions in set up environment to start all the applications  
	2. If you want to test normal mode  
		2.1 in terminal, type read <filename> to test read request (make sure you have <filename> in your server_files folder)  
		2.2 in terminal, type write <filename> to test write request (make sure you have <filename> in your client_files folder)  
		2.3 Type 'mode' to switch the mode  
		2.4 Type 'switch' to switch the print mode  
	3. If you want to test test mode  
		3.1 in terminal, type reset(this will set the running mode to test)  
		3.2 same as 2.2  
		3.3 same as 2.3  
	4. to exit, simply type 'exit'  
	5. if you want to change the printing mode in server, in the server terminal, type 'switch'.  
	
Explaining the names of files:  
	- TFTPPacket.java - abstract class for all TFTP packets which defined several common functions  
	- TFTPAckPacket.java - class for TFTPAckPacket  
	- TFTPDataPacket.java - class for TFTPDataPacket  
	- TFTPErrorPacket.java - class for TFTPErrorPacket  
	- TFTPRequestPacket.java - class for TFTP RRQ request for TFTP WRQ request  
	- TFTPErrorType.java - enum class for all possible types of TFTP errors (only 1, 2, 3 and 6 are used in iteration 2)  
	- TFTPErrorException.java - exception that is thrown when a TFTPErrorPacket is received, or a TFTPErrorPacket is sent, used for terminating the connection  
	- TFTPClient.java - TFTP client class, can send RRQ or WRQ to the TFTP server  
	- TFTPServer.java - TFTP server class, it will initialize a TFTP request listener thread which will listen for new request  
	- TFTPRequestListener.java - TFTP request listener class, it's a sub-class of thread, and it will listen to WRQ or RRQ on port 69  
	- TFTPRequestHandler.java - TFTP request handler class, it's a sub-class of thread, and it will handle the WRQ or RRQ received by TFTP request listener thread  
	- TFTPErrorSimulator.java - Used for simulating error, not used in iteration 2, for now it will just receive a packet and forward the packet without touching the packet  
	- Mode.java - enum class for current printing mode (quite or verbose)  
	- Type.java - enum class for all different types of TFTP requests (WRQ, RRQ, DATA, ACK, ERROR)  
	- ThreadLog.java - Helper class for all different threads to print information  

Breakdown of responsibilities:  
 	- Yunkai Wang: most of the coding work  
 	- Qingyi Yin: TFTPAckPacket class and all diagrams/documents  
 	
Tips for testing iteration 2:  
	- if you want to test fileNotFound error, simply send a RRQ with a file that doesn't exist in the server's folder  
	- if you want to test fileAlreadyExist error, simply send a WRQ with a file that already exist in the server's folder  
	- if you want to test AccessViolation for RRQ, change the permission so that the file cannot be read, then send the RRQ with that file  
	- if you want to test AccessViolation for WRQ, change the permission of the server's folder so that it cannot be modified, then send a WRQ with any file that exist in the client's folder  
	- if you want to test diskFull for RRQ  
		- one option is to test on a USB whose memory is almost full, and read a large test file from server  
		- the second option is that in TFTPClient.java, line 405 to line 414 checks if the disk is full by calling the funtion getFreeSpace(), you can add a line "freeSpace = 0" on line 406, which is sending a fake message to the system saying that the disk is full, and a diskFull error will be sent. This is easier to be tested, but be sure to remove the line "freeSpace = 0" after testing, otherwise the WRQ will always fail  
	- if you want to test diskFull for WRQ  
		- same as above, test on a USB, and write a large test file to server  
		- similar to above, in TFTPRequestHandler, line 200 - line 209 also checks if the disk is full by calling the the function getFreeSpace(), to fake a disk full exception, add "freeSpace = 0" on line 201, and the system will always send a DiskFull exception for WRQ. Deleting the line that you just added will make the sytem work again  

Diagrams:  
 	All the diagrams are in the diagram folder.  

	Diagrams from iteration 1
 		- ucm-rrq: UCM for read request  
 		- ucm-wrq: UCM for write request  

	Diagrams for iteration 2
 		- uml: class diagram of this system(updated with new classes)  
		- fileNotFound-RRQ: timing diagram for file not found during RRQ  
		- fileNotFound-WRQ: timing diagram for file not found during WRQ  
		- accessViolation-RRQ: timing diagram for access violation during RRQ  
		- accessViolation-WRQ: timing diagram for access violation during WRQ  
		- diskFull-RRQ: timing diagram for disk full during RRQ  
		- diskFull-WRQ: timing diagram for disk full during WRQ  
		- fileAlreadyExists - timing diagram for file already exists (only happends during WRQ)

        Diagrams from iteration 3
        - wrq-lost-data: timing diagram for lost datapacket during WRQ
        - wrq-lost-ack: timing diagram for lost ackpacket during WRQ
        - wrq-lost-request: timing diagram for lost request during WRQ
        - wrq-delay-data: timing diagram for delay datapacket during WRQ
        - wrq-delay-ack: timing diagram for delay ackpacket during WRQ
        - wrq-duplicate-data: timing diagram for duplicate datapacket during WRQ
        - wrq-duplicate-ack: timing diagram for duplicate ack during WRQ
        - rrq-lost-data: timing diagram for lost datapacket during RRQ
        - rrq-lost-ack: timing diagram for lost ackpacket during RRQ
        - rrq-lost-request: timing diagram for lost request during RRQ
        - rrq-delay-data: timing diagram for delay datapacket during RRQ
        - rrq-delay-ack: timing diagram for delay ackpacket during RRQ
        - rrq-duplicate-data: timing diagram for duplicate datapacket during RRQ
        - rrq-duplicate-ack: timing diagram for duplicate ack during RRQ
        - uml: class diagram of this system(updated with new classes)

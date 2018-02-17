SYSC 3303 PROJECT

Carleton University Winter 2018 SYSC 3303 Project.

Set up test files:  
	Step1: Go to the project directory.  
	Step2: Create two folders: client_files, server_files.  
	Step3: Create test files (ex. a.txt) in these two folders.  

Set up environment:  
	Step1: right click TFTPClient --> Run as --> 1 Java Application.  
	Step2: right click TFTPServer --> Run as --> 1 Java Application.
	Step3: right click TFTPErrorSimulator --> Run as --> 1 Java Application.

Instruction of using TFTP:  
	TFTPClient:  
		- menu   ---> display the menu, type "menu" then press enter  
		- exit   ---> stop the client and exit, type "exit" press enter  
		- mode   ---> display the current mode, type "mode" then press enter  
		- switch ---> switch mode(verbose, quite, normal, or test), type "switch" then press enter  
		- reset  ---> reset running mode(test or normal), type "reset" then press enter  
		- read   ---> read a file from server, type "read <filename>" then press enter  
		- write  ---> write a file to server, type "write <filename>" then press enter  
                - la     ---> display all the files under current directory, type "la" then press enter  
		- pwd/dir---> display current directory path, type "pwd" or "dir" then press enter  
		- cd     ---> check the directory path, type "cd <path>" then press enter  
	TFTPServer:  
		- menu   ---> display the menu, type "menu" then press enter  
		- exit   ---> stop the client and exit, type "exit" press enter  
		- mode   ---> display the current mode, type "mode" then press enter  
		- switch ---> switch mode(verbose, quite, normal, or test), type "switch" then press enter  
		- count  ---> display current number of running threads, type "count" then press enter  
		- la     ---> display all the files under current directory, type "la" then press enter  
		- pwd/dir---> display current directory path, type "pwd" or "dir" then press enter  
		- cd     ---> check the directory path, type "cd <path>" then press enter  
	TFTPErrorSimulator:  
		For iteration 2, the error simulator doesn't support any command, it will just wait for a new request from the client, and forward the request to the server, forward the request from the server to the client, it will not touch any information stored in the packet.  
		
Testing step:  
	1. Follow the instructions in set up environment to start all the applications  
	2. To test in normal mode
		2.1 In client side/terminal, type read <filename> to test read request/RRQ (make sure you have <filename> in your server_files folder)  
		2.2 In client side/terminal, type write <filename> to test write request/WRQ (make sure you have <filename> in your client_files folder) 
		2.3 Type 'mode' to get the current printing mode  
		2.4 Type 'switch' to switch the printing mode (QUITE/VERBOSE)  
	3. To test in test mode  
		3.1 In terminal, type reset(this will set the running mode to test)  
		3.2 Same as 2.2  
		3.3 Same as 2.3  
	4. To exit, type 'exit' 
	5. To change the printing mode in server side/server terminal, type 'switch'.  
	
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
	- Lairu Wu: readme adjustment
 	
	
Tips for testing iteration 2:  
	There are 6 kinds of errors:
	1. client send RRQ, but the file not exist on server (fileNotExist)
 	2. client send RRQ, but the client has no permission to read the file (accessViolation)
 	3. client send RRQ, during the file transfer, client's disk become full (diskFull)
 	4. client send WRQ, but the file already exists on server (fileAlreadyExist)
 	5. client send WRQ, but the client has no permission to write to the folder (accessViolation)
 	6. client send WRQ, during the file transfer, server's disk become full (diskFull)
	
	To test: 
	1. fileNotFound: simply send a RRQ with a file that doesn't exist in the server's folder  
	2. fileAlreadyExist: simply send a WRQ with a file that already exist in the server's folder  
	3. AccessViolation for RRQ: change the permission so that the file cannot be read, then send the RRQ with that file  
	4. AccessViolation for WRQ: change the permission of the server's folder so that it cannot be modified, then send a WRQ with any file that exist in the client's folder  
	5. diskFull for RRQ: 
		1st Option: to test on a USB whose memory is almost full, and read a large test file from server  
		2rd Option: in TFTPClient.java, line 405-414 checks if the disk is full by calling the funtion getFreeSpace(), you can add a line "freeSpace = 0" on line 406, which is sending a fake message to the system saying that the disk is full, and a diskFull error will be sent. This is easier to be tested, but be sure to remove the line "freeSpace = 0" after testing, otherwise the WRQ will always fail  
	6. diskFull for WRQ: 
		1st Option: same as above, to test on a USB, and write a large test file to server  
		2rd Option: similar to above, in TFTPRequestHandler, line 200-209 also checks if the disk is full by calling the the function getFreeSpace(), to fake a disk full exception, add "freeSpace = 0" on line 201, and the system will always send a DiskFull exception for WRQ. Deleting the line that you just added will make the sytem work again  

 Diagrams annotation:  
 	All the diagrams are in the diagram folder.  
 	- uml: class diagram of this system  
 	- ucm-rrq: UCM for read request  
 	- ucm-wrq: UCM for write request  
	- fileNotFound-RRQ: timing diagram for file not found during RRQ  
	- fileNotFound-WRQ: timing diagram for file not found during WRQ  
	- accessViolation-RRQ: timing diagram for access violation during RRQ  
	- accessViolation-WRQ: timing diagram for access violation during WRQ  
	- diskFull-RRQ: timing diagram for disk full during RRQ  
	- diskFull-WRQ: timing diagram for disk full during WRQ  
	- fileAlreadyExists - timing diagram for file already exists (only happends during WRQ)  


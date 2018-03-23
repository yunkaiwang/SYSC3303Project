SYSC 3303 PROJECT

Carleton University Winter 2018 SYSC 3303 Project.

Set up test files:  
	Step1: Go to the project directory.  
	Step2: Create test files in these client_files and server_files folder    

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
                - la/ls  ---> if you want to see all the files under current directory, type "la" then press enter 
		- rm     ---> if you want to remove a file under current directory, type "rm <filename>" then press enter  
		- pwd/dir---> if you want to see current directory path, type "pwd" or "dir" then press enter  
		- cd     ---> if you want to check the directory path, type "cd <path>" then press enter  
	TFTPServer:  
		- menu   ---> if you want to see the menu, type "menu" then press enter  
		- exit   ---> if you want to stop the client and exit, type "exit" press enter  
		- mode   ---> if you want to see the current mode, type "mode" then press enter  
		- switch ---> if you want to switch mode(verbose, quite, normal, or test), type "switch" then press enter  
		- count  ---> if you want to see current number of running threads, type "count" then press enter  
		- la/ls  ---> if you want to see all the files under current directory, type "la" then press enter  
		- rm     ---> if you want to remove a file under current directory, type "rm <filename>" then press enter  
		- pwd/dir---> if you want to see current directory path, type "pwd" or "dir" then press enter  
		- cd     ---> if you want to check the directory path, type "cd <path>" then press enter  
	TFTPErrorSimulator:  
		For project iteration 3, the user will be able to simulate "lost", "delay" or "duplicate" of packet, or perform a normal file transfer. (Note that request packet cannot be delayed or duplicated for project iteration 3, it can only be lost)  
		- menu     ---> if you want to see the menu, type "menu" then press enter  
		- exit     ---> if you want to stop the client and exit, type "exit" press enter  
		- normal   ---> if you want to perform a TFTP file transfer without any error, type "normal" press enter  
		- lose     ---> if you want to perform a TFTP file transfer with packet lose, type "lose" press enter  
		- delay    ---> if you want to perform a TFTP file transfer with packet delay with the a specified delay time, type "delay <millisecond>" press enter  
		- duplicate --> if you want to perform a TFTP file transfer with duplicate packet, type "duplicate" press enter  

		If you entered "lose", "delay" or "duplicate" in previous choice, the program will ask you for input again, where you will be choosing the type of packet and block number to simulate the error:  
		- request   ---> if you want to simulate the error on request packet, type "request" and press enter  
		- data      ---> if you want to simulate the error on data packet, type "data <blkNum>" press enter, the block number is the specified data packet which you want to simulate the error  
		- ack ---> if you want to simulate the error on ack packet, type "ack <blkNum> press enter, the block number is the specified data packet which you want to simulate the error  
		
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
	4. If you want to simulate error(make sure you are in test mode first)  
		4.1 in error simulator's terminal, choose the type of error  
		4.2 if you want to test lost packet, type "lose" and press enter, then select the packet type and press enter  
		4.3 if you want to test delay packet, type "delay" and press enter, then select the packet type and press enter  
		4.4 if you want to test duplicate packet, type "duplicate" and press enter, then select the packet type and press enter  
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
 	Previous interations:  
	- Yunkai Wang: most of the coding work  
 	- Qingyi Yin: TFTPAckPacket class and all diagrams/documents  
 	
	iteration 3:  
	- Yunkai Wang: change error simulator & request handler & client to handler duplicate/delay/lose packet cases, improve the usability of the program  
	- Qingyi Yin: all diagrams/documents  


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

	Old diagrams from iteration 1 & 2  
 		- ucm-rrq: UCM for read request  
 		- ucm-wrq: UCM for write request
 		- uml: class diagram of this system(updated with new classes)  
		- fileNotFound-RRQ: timing diagram for file not found during RRQ  
		- fileNotFound-WRQ: timing diagram for file not found during WRQ  
		- accessViolation-RRQ: timing diagram for access violation during RRQ  
		- accessViolation-WRQ: timing diagram for access violation during WRQ  
		- diskFull-RRQ: timing diagram for disk full during RRQ  
		- diskFull-WRQ: timing diagram for disk full during WRQ  

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
        Diagrams from iteration 4
        - wrq-request-4: timing diagram for write request illegal error code 4
        - wrq-data-4: timing diagram for datapacket illegal durinig WRQ error code 4
        - wrq-ack-4: timing diagram for ack packet illegal during WRQ error code 4
        - rrq-request-4: timing diagram for read request illegal error code 4
        - rrq-data-4: timing diagram for datapacket illegal durinig RRQ error code 4
        - rrq-ack-4: timing diagram for ack packet illegal during RRQ error code 4
        - wrq-data-5: timing diagram for unknown transfer id for datapacket durinig WRQ error code 5
        - wrq-ack-5: timing diagram for unknown transfer id for ackpacket durinig WRQ error code 5
        - rrq-data-5: timing diagram for unknown transfer id for datapacket durinig RRQ error code 5
        - rrq-ack-5: timing diagram for unknown transfer id for ackpacket durinig RRQ error code 5
        

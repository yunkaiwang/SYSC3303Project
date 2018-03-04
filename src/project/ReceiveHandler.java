package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

/**
 * @author Lairu Wu modified on March 4, 2018
 * 
 * this class will handle all receiving packet situations, including error handling: 
 * E1. delay a packet-do nothing, retry receive() until timeout limit run out
 * 		If received the delayed packet, continue
 * E2. lose a packet-ran out timeout limit means the expect packet is lost, throw the exception
 * E3. timeout resend-to avoid the Sorcerer's Apprentice bug, resend the packet when timeout(except ACK)
 * E4. If received ERROR packet, throw TFTPErrorException
 * E5. If received packet type is different from expected type, throw TFTPErrorException
 * E6. block number invalid-for ACK and DATA packet, if received block number > expected 
 *		or received block number < 0, throw TFTPErrorException
 * E7. duplicate packet-for ACK and DATA packet, if received block number < expected 
 *		and received block number >= 0, discard the current packet and wait to receive another packet
 */

public class ReceiveHandler {
	public static final int MAX_SEND_TIMES = 3; // timeout limit
	public static final int DEFAULT_TIMEOUT = 2000; // timeout period
	
	/**
	 * 
	 * @param socket
	 * @param expectedType
	 * @param expectedBlockNum
	 * @param isLastSentACK
	 * @param lastSentPacket
	 * @return
	 * @throws IOException
	 * @throws TFTPErrorException 
	 */
	public static TFTPPacket receiveTFTPPacket(DatagramSocket socket, Type expectedType, 
			int expectedBlockNum, boolean isLastSentACK, DatagramPacket lastSentPacket) 
					throws IOException, TFTPErrorException {
		DatagramPacket receivePacket = null;
		TFTPPacket tftpPacket = null;
		int timeouts = 0;
		
		while (timeouts < ReceiveHandler.MAX_SEND_TIMES) { 
			try {
				receivePacket = new DatagramPacket(new byte[TFTPPacket.MAX_LENGTH],
						TFTPPacket.MAX_LENGTH);
				socket.receive(receivePacket);
				tftpPacket = TFTPPacket.createFromPacket(receivePacket);
				
				//check the received packet is the expected one(E4&E5)
				validReceivedPacket(tftpPacket, expectedType, expectedBlockNum);
				
				//E7. duplicate packet-for ACK and DATA packet, if received block number < expected 
				//and received block number >= 0, 
				//discard the current packet and wait to receive another packet
				if (isDuplicate(tftpPacket, expectedType, expectedBlockNum)) {
					tftpPacket = null;
					timeouts = 0;
					continue;
				} 
				
				break; //leave the while loop if received the correct packet
			} catch (SocketTimeoutException e){ //handle three timeout cases
				timeouts++;
				
				//E2. lose a packet-ran out timeout limit means the expect packet is lost, throw the exception
				if (timeouts >= ReceiveHandler.MAX_SEND_TIMES)  {
					ThreadLog.print("packet lost");
					throw new IOException("packet lost");		
				}
				
				//E3. timeout resend-to avoid the Sorcerer's Apprentice bug, resend the packet when timeout(except ACK)
				ThreadLog.print("Receive timed out " + timeouts + " times. Try it again. ");
				if (!isLastSentACK)    
					socket.send(lastSentPacket); 
				
				//E1. delay a packet-do nothing, retry to receive() until timeout limit run out
				//If received the delayed packet, continue
				continue;
			} catch (TFTPErrorException e) {
				throw e;     //throw the detected error from validReceivedPacket()
			}
		}
		
		return tftpPacket;
	}
	
	private static void validReceivedPacket(TFTPPacket tftpPacket, Type expectedType, 
			int expectedBlockNum) throws TFTPErrorException {
		Type tmpType = tftpPacket.type();
		String tmpMsg = "";
		int tmpBlockNum = -1;
		
		
		//E4.If received ERROR packet, throw TFTPErrorException
		if (tmpType == Type.ERROR) {
			TFTPErrorPacket tmpErrorPacket = (TFTPErrorPacket)tftpPacket;
			tmpMsg = tmpErrorPacket.getErrorMsg();
			ThreadLog.print("received ERROR packet--" + tmpMsg);
			throw new TFTPErrorException(tmpMsg);
		}
		
		
		//E5. If received packet type is different from expected type, throw TFTPErrorException
		if(tmpType != expectedType) {
			tmpMsg = "expected to receive " + expectedType + " packet, but actually received " 
					+ tmpType +"packet";
			ThreadLog.print(tmpMsg);
			throw new TFTPErrorException(tmpMsg);
		}
		
		
		//E6. Block number invalid-for ACK and DATA packet, if received block number > expected 
		//or received block number < 0, throw TFTPErrorException
		if(tmpType == Type.ACK) {
			TFTPAckPacket tmpAckPacket = (TFTPAckPacket)tftpPacket;
			tmpBlockNum = tmpAckPacket.getBlockNumber();
			if (tmpBlockNum > expectedBlockNum || tmpBlockNum < 0) {
				tmpMsg = "invalid block number--expected ACK block number is " + expectedBlockNum
						+ ", but actually received ACK block number is " + tmpBlockNum;
				ThreadLog.print(tmpMsg);			
				throw new TFTPErrorException(tmpMsg);
			}
		} else if(tmpType == Type.DATA) {
			TFTPDataPacket tmpDataPacket = (TFTPDataPacket)tftpPacket;
			tmpBlockNum = tmpDataPacket.getBlockNumber();
			if (tmpBlockNum > expectedBlockNum || tmpBlockNum < 0) {
				tmpMsg = "invalid block number--expected ACK block number is " + expectedBlockNum
						+ ", but actually received ACK block number is " + tmpBlockNum;
				ThreadLog.print(tmpMsg);			
				throw new TFTPErrorException(tmpMsg);
			}
		}
		
	}
	
	
	
	private static boolean isDuplicate(TFTPPacket tftpPacket, Type expectedType, int expectedBlockNum) {
		if((expectedType != Type.ACK) && (expectedType != Type.DATA))
			return false;
		
		Type tmpType = tftpPacket.type();
		String tmpMsg = "";
		int tmpBlockNum = -1;
		
		if(tmpType == Type.ACK) {
			TFTPAckPacket tmpAckPacket = (TFTPAckPacket)tftpPacket;
			tmpBlockNum = tmpAckPacket.getBlockNumber();
			
			//the current blockNum is not the expect blockNum(duplication happens)
			if(tmpBlockNum < expectedBlockNum && tmpBlockNum > 0) { 
				tmpMsg = "received duplicate packet-expected ACK block number is " + expectedBlockNum
						+ ", but actually received ACK block number is " + tmpBlockNum;
				ThreadLog.print(tmpMsg);
				return true;
			}	
		}
		else if(tmpType == Type.DATA) {
			TFTPDataPacket tmpAckPacket = (TFTPDataPacket)tftpPacket;
			tmpBlockNum = tmpAckPacket.getBlockNumber();
			
			//the current blockNum is not the expect blockNum(duplication happens)
			if(tmpBlockNum < expectedBlockNum && tmpBlockNum > 0){
				tmpMsg = "received duplicate packet-expected DATA block number is " + expectedBlockNum
						+ ", but actually received DATA block number is " + tmpBlockNum;
				ThreadLog.print(tmpMsg);
				return true;
			}	
		}
		
		return false;
	}
}

package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * TFTPErrorPacket class, in any of the error cases, an TFTP error packet will
 * be prepared and sent, and the corresponding connection will be terminated.
 *  
 * @author yunkai wang
 */
public class TFTPErrorPacket extends TFTPPacket {
	private static final Type DEFAULT_TYPE = Type.ERROR; // default packet type
	private String errorMsg; // error message
	private TFTPErrorType errorType; // error type

	/**
	 * Constructor
	 * 
	 * @param errorCode
	 * @param address
	 * @param port
	 */
	private TFTPErrorPacket(int errorCode, InetAddress address, int port) {
		super(DEFAULT_TYPE, address, port);
		this.errorType = TFTPErrorType.getErrorType(errorCode);
		this.errorMsg = errorType.defaultErrorMsg();
	}
	
	/**
	 * Constructor without defined error message, using the default error message
	 * 
	 * @param errorCode
	 * @param errorMsg
	 * @param address
	 * @param port
	 */
	private TFTPErrorPacket(int errorCode, String errorMsg, InetAddress address, int port) {
		super(DEFAULT_TYPE, address, port);
		this.errorMsg = errorMsg;
		this.errorType = TFTPErrorType.getErrorType(errorCode);
	}

	/**
	 * Create new file not found error packet without defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createFileNotFoundErrorPacket(InetAddress address, int port) {
		return new TFTPErrorPacket(1, address, port);
	}
	
	/**
	 * Create new file not found error packet with defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createFileNotFoundErrorPacket(String msg, InetAddress address, int port) {
		return new TFTPErrorPacket(1, msg, address, port);
	}
	
	/**
	 * Create new access violation error packet without defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createAccessViolationErrorPacket(InetAddress address, int port) {
		return new TFTPErrorPacket(2, address, port);
	}
	
	/**
	 * Create new access violation error packet with defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createAccessViolationErrorPacket(String msg, InetAddress address, int port) {
		return new TFTPErrorPacket(2, msg, address, port);
	}
	
	/**
	 * Create new disk full error packet without defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createDiskfullErrorPacket(InetAddress address, int port) {
		return new TFTPErrorPacket(3, address, port);
	}
	
	/**
	 * Create new disk full error packet with defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createDiskfullErrorPacket(String msg, InetAddress address, int port) {
		return new TFTPErrorPacket(3, msg, address, port);
	}
	
	/**
	 * Create new illegal operation error packet without defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createIllegalTFTPOperation(InetAddress address, int port) {
		return new TFTPErrorPacket(4, address, port);
	}
	
	/**
	 * Create new illegal operation error packet with defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createIllegalTFTPOperation(String msg, InetAddress address, int port) {
		return new TFTPErrorPacket(4, msg, address, port);
	}
	
	/**
	 * Create new unknown transfer id packet without defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createUnknownTID(InetAddress address, int port) {
		return new TFTPErrorPacket(5, address, port);
	}
	
	/**
	 * Create new unknown transfer id packet with defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createUnknownTID(String msg, InetAddress address, int port) {
		return new TFTPErrorPacket(5, msg, address, port);
	}
	
	/**
	 * Create new file already exists error packet without defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createFileAlreadyExistErrorPacket(InetAddress address, int port) {
		return new TFTPErrorPacket(6, address, port);
	}
	
	/**
	 * Create new file already exists error packet with defined error message
	 * 
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createFileAlreadyExistErrorPacket(String msg, InetAddress address, int port) {
		return new TFTPErrorPacket(6, msg, address, port);
	}
	
	/**
	 * Getter
	 * 
	 * @return errorCode
	 */
	public int getErrorCode() {
		return errorType.getErrorCode();
	}

	/**
	 * Getter
	 * 
	 * @return errorMsg
	 */
	public String getErrorMsg() {
		return errorMsg;
	}

	/**
	 * Create new error packet from datagram packet
	 * 
	 * @param packet
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createFromPacket(DatagramPacket packet) {
		return createFromPacketData(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()), packet.getLength(),
				packet.getAddress(), packet.getPort());
	}

	/**
	 * Create new data packet from datagram packet data
	 * 
	 * @param packetData
	 * @param packetDataLength
	 * @param address
	 * @param port
	 * @return TFTPErrorPacket
	 */
	public static TFTPErrorPacket createFromPacketData(byte[] packetData, int packetDataLength, InetAddress address,
			int port) {
		int OPCODE = ((packetData[0] << 8) & 0xFF00) | (packetData[1] & 0xFF);
		if (!Type.validOPCODE(DEFAULT_TYPE, OPCODE))
			throw new IllegalArgumentException("Invalid OP code");
		int errorCode = ((packetData[2] << 8) & 0xFF00) | (packetData[3] & 0xFF);

		int i = 3;
		StringBuilder filenameBuilder = new StringBuilder();
		while (packetData[++i] != 0 && i < (packetDataLength - 1)) {
			filenameBuilder.append((char) packetData[i]);
		}
		
		String errorMsg = filenameBuilder.toString();
		if (packetData[i] != 0)
			throw new IllegalArgumentException("Invalid packet data");
		
		return new TFTPErrorPacket(errorCode, errorMsg, address, port);
	}

	/**
	 * Convert the block number into byte array of length 2
	 * 
	 * @return byteArray
	 */
	private byte[] errorCode() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		int errorCode = errorType.getErrorCode();
		stream.write(errorCode >> 8);
		stream.write(errorCode);
		return stream.toByteArray();
	}

	/**
	 * Generate the byte array that contains all information
	 * 
	 * @return byteArray
	 * @throws IOException
	 */
	@Override
	protected byte[] generateData() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(DEFAULT_TYPE.OPCODE());
		stream.write(errorCode());
		stream.write(errorMsg.getBytes(), 0, errorMsg.getBytes().length);
		stream.write(0);
		return stream.toByteArray();
	}
	
	/**
	 * toString method
	 */
	@Override
	public String toString() {
		return ("Packet type: " + this.type() + "\nDestination: \n" + 
	            "IP address: " + this.getAddress() + "\nPort: " + this.getPort() +
	            "\nInformation in this packet: \n" + "Error code: " +
	            this.getErrorCode() + "\nError message: " + this.getErrorMsg() + "\n");
	}
}

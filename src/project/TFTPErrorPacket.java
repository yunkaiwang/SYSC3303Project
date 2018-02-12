package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * 
 * please note that for project iteration 1, no TFTP ERROR packet will be
 * prepared/transmitted/ received/handled, so this class is not used in any
 * other classes, and the class doesn't contain all the functions that it needs,
 * we are just defining a few functions that we think are important in this
 * class. More functions will be added in later iterations.
 * 
 * @author yunkai wang
 * Last modified on Feb 11, 2018
 */
public class TFTPErrorPacket extends TFTPPacket {
	private static final Type DEFAULT_TYPE = Type.ERROR; // default packet type
	private String errorMsg; // error message
	private TFTPErrorType errorType; // error type

	/**
	 * Constructor
	 * 
	 * @param errorCode
	 * @param errorMsg
	 * @param address
	 * @param port
	 */
	TFTPErrorPacket(int errorCode, String errorMsg, InetAddress address, int port) {
		super(DEFAULT_TYPE, address, port);
		this.errorMsg = errorMsg;
		this.errorType = TFTPErrorType.getErrorType(errorCode);
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
		while (packetData[++i] != 0 && i < packetDataLength) {
			filenameBuilder.append((char) packetData[i]);
		}
		String errorMsg = filenameBuilder.toString();
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
		ByteArrayOutputStream steam = new ByteArrayOutputStream();
		steam.write(DEFAULT_TYPE.OPCODE());
		steam.write(errorCode());
		steam.write(errorMsg.getBytes(), 0, errorMsg.getBytes().length);
		return steam.toByteArray();
	}
}

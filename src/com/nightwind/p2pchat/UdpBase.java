package com.nightwind.p2pchat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UdpBase {

	protected static final String MSG_REQUIRE_ONLINE_CLIENT = "requireOnlineClient";

	protected static final String MSG_RESPONSE_ONLINE_CLIENT_PREFIX = "online client:\n";

	protected static final String MSG_RESPONSE_HEART_BEAT = "heartBeat";

	protected static final String REGEX_ACK_MATCH = ":ACK=[0-9A-Z]{16}";

	protected int sPort = 4869;

	protected DatagramSocket ds = null; // 连接对象

	protected int bufferLength = 1024;

	static class Client {
		String hostname;
		int port;

		public Client() {
		};

		public Client(String hostname, int port) {
			super();
			this.hostname = hostname;
			this.port = port;
		}

		public String getHostname() {
			return hostname;
		}

		public void setHostname(String hostname) {
			this.hostname = hostname;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Client) {
				Client that = (Client) obj;
				return hostname.equals(that.hostname) && port == that.port;
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return hostname + ":" + port;
		}
	}

	public UdpBase() {
	};

	public UdpBase(int sPort) {
		this.sPort = sPort;
	}

	public int getsPort() {
		return sPort;
	}

	public void setsPort(int sPort) {
		this.sPort = sPort;
	}

	public DatagramSocket getDs() {
		return ds;
	}

	public void setDs(DatagramSocket ds) {
		this.ds = ds;
	}

	public int getBufferLength() {
		return bufferLength;
	}

	public void setBufferLength(int bufferLength) {
		this.bufferLength = bufferLength;
	}

	public void init() throws SocketException {
		if (ds == null) {
			ds = new DatagramSocket(sPort);
		}
	}

	public void send(String msg, String addr, int dPort) throws IOException {
		InetAddress inetaddr = InetAddress.getByName(addr);
		try {
			byte[] data = msg.getBytes("utf-8");
			for (int i = 0; i < data.length; i += bufferLength) {
				int len = Math.min(data.length - i, bufferLength);
				byte[] buf;
				buf = Arrays.copyOfRange(data, i, i + len);
				// System.out.println("debug: send bytes = " +
				// Arrays.toString(data));
				DatagramPacket sendDp; // 发送数据包对象
				sendDp = new DatagramPacket(buf, buf.length, inetaddr, dPort);
				while (ds == null) {
					// wait for dataGramSocket initiation
					Thread.sleep(10);
				}
				ds.send(sendDp);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void send(String msg, Client client) throws IOException {
		send(msg, client.getHostname(), client.getPort());
	}

	/**
	 * send message with message digit
	 * 
	 * @param originMsg
	 * @return [0] = checkMsg, [1] = md5Hex(checkMsg)
	 * @throws IOException
	 */
	// public String[] sendWithMsgDigit(String originMsg, Client client) throws
	// IOException {
	//
	// long time = System.currentTimeMillis();
	// String checkMsg = String.format("%s:%d:%d:%s",
	// client.hostname, client.port, time,
	// originMsg);
	// String md5Hex = Str2MD5Hex(checkMsg);
	// // content message digit
	// String contentMD5 = Str2MD5Hex(md5Hex + ":" + originMsg);
	//
	// send(contentMD5 + ":" + md5Hex + ":" + originMsg,
	// new Client(client.hostname, client.port));
	//
	// return new String[] { checkMsg, md5Hex};
	// }

	/**
	 * send message with message digit
	 * 
	 * @param originMsg
	 * @return [0] = checkMsg, [1] = md5Hex(checkMsg)
	 * @throws IOException
	 */
	public String[] sendWithMsgDigit(String originMsg, Client client)
			throws IOException {
		List<String> ret = new ArrayList<>();
		String msg = new String(originMsg);
		
		// split message (length <= bufferLength - 34) 
		while (msg.length() > 0) {
			String sendMsg = Utils.substringByByteCount(msg, bufferLength - 34);
			
			String[] info = getWrapAndMsgDigit(sendMsg, client);
			String checkMsg = info[0];
			String md5Hex = info[1];
			String wrapMsg = info[2];

			send(wrapMsg, new Client(client.hostname, client.port));
			
			ret.add(checkMsg);
			ret.add(md5Hex);
			
//			msg = msg.replaceFirst(sendMsg, "");
			msg = msg.substring(sendMsg.length());
		}
		
		return ret.toArray(new String[0]);
	}

	/**
	 * wrap message with message digit
	 * 
	 * @param originMsg
	 * @return [0] = checkMsg, [1] = md5Hex(checkMsg), [2] = wrap message
	 * @throws IOException
	 */
	public String[] getWrapAndMsgDigit(String originMsg, Client client) {

		long time = System.currentTimeMillis();
		String checkMsg = String.format("%s:%d:%d:%s", client.hostname,
				client.port, time, originMsg);
		String md5Hex = Str2MD5Hex(checkMsg);
		// content message digit
		String contentMD5 = Str2MD5Hex(md5Hex + ":" + originMsg);
		String wrapMsg = contentMD5 + ":" + md5Hex + ":" + originMsg;

		return new String[] { checkMsg, md5Hex, wrapMsg };
	}

	/**
	 * @return [0] = recvMeassge, [1] = remote host address, [2] = remote port
	 * @throws IOException
	 */
	public String[] receive() throws IOException {
		byte[] buf = new byte[bufferLength];
		DatagramPacket receiveDp; // 接收数据包对象
		receiveDp = new DatagramPacket(buf, buf.length);
		while (ds == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		ds.receive(receiveDp);
		InetAddress addr = receiveDp.getAddress();
		byte[] response = receiveDp.getData();
		// System.out.println("debug: receive bytes: = " +
		// Arrays.toString(response));
		String strResponse = new String(response, 0, receiveDp.getLength(), "utf-8");
		return new String[] { strResponse, addr.getHostAddress(),
				String.valueOf(receiveDp.getPort()) };
	}

	public String receiveMsg() throws IOException {
		String[] response = receive();
		String msg = response[0];
		String addr = response[1];
		String port = response[2];
		return String.format("[%s:%s] %s", msg, addr, port);
	}

	public void close() {
		ds.close();
		ds = null;
	}

	public static String Str2MD5Hex(String str) {
		final String ALGORITHM = "MD5";
		String ret = "";
		try {
			MessageDigest md = MessageDigest.getInstance(ALGORITHM);
			md.update(str.getBytes());
			ret = bytes2HexStr(md.digest()).substring(8, 24);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static String bytes2HexStr(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			String hex = Integer.toHexString(b & 0xff);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			sb.append(hex.toUpperCase());
		}
		return sb.toString();
	}

	protected static String[] getAckMsg(String recvMsg)
			throws IllegalArgumentException {
		String[] msgs;
		String ack;
		String originMsg;
		msgs = recvMsg.split(":");
		String contentMD5 = msgs[0];
		ack = msgs[1];
		if (!ack.matches("[0-9A-Z]{16}")) {
			throw new IllegalArgumentException("ACK error");
		}
		if (!Str2MD5Hex(recvMsg.replaceFirst(contentMD5 + ":", "")).equals(
				contentMD5)) {
			// md5 verification failed
			throw new IllegalArgumentException("md5 verification failed");
		}
		originMsg = recvMsg.replaceFirst(contentMD5 + ":" + ack + ":", "");
		// System.out.println("receive msg verification failed" +
		// e.getMessage());
		return new String[] { ack, originMsg };
	}

	// public static void main(String[] args) {
	// byte[] bytes1 = new byte[] { 114, 101, 113, 117, 105, 114, 101, 79, 110,
	// 108, 105, 110, 101, 67, 108, 105, 101, 110, 116, 0};
	// byte[] bytes2 = new byte[] { 114, 101, 113, 117, 105, 114, 101, 79, 110,
	// 108, 105, 110, 101, 67, 108, 105, 101, 110, 116};
	// System.out.println("bytes1.lentgh = " + bytes1.length +
	// " bytes2.lentgh = " + bytes2.length);
	// String str = new String(bytes1, 0, bytes2.length);
	// System.out.println("equals = " + str.equals(MSG_REQUIRE_ONLINE_CLIENT));
	// }

}

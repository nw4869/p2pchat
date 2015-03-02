package com.nightwind.p2pchat;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class P2PServer extends UdpBase {

	public static final int DEFAULT_REFRESH_PERIOD_SECOND = 30;

	private Map<Client, Long> clientMap = new HashMap<>();

	private boolean stop = true;

	protected boolean verbose;

	/**
	 * refresh online client period
	 */
	protected int refreshPeriodSecond = DEFAULT_REFRESH_PERIOD_SECOND;

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public int getRefreshPeriodSecond() {
		return refreshPeriodSecond;
	}

	public void setRefreshPeriodSecond(int refreshPeriodSecond) {
		this.refreshPeriodSecond = refreshPeriodSecond;
	}

	public P2PServer() {
	}

	public P2PServer(int port) {
		super(port);
	}

	public void start() {
		System.out.println("server starting");
		if (isStop()) {
			stop = false;

			// start receive daemon
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (!isStop()) {
						try {
							String[] recv = receive();
							String recvMsg = recv[0];
							Client client = new Client(recv[1],
									Integer.valueOf(recv[2]));
							if (verbose) {
								System.out.println("client: " + client
										+ " msg: " + recvMsg);
							}

							if (recvMsg.matches(REGEX_ACK_MATCH)) {
								// is ACK
								continue;
							}

							String ack;
							String originMsg;
							try {
								String[] ackMsg = getAckMsg(recvMsg);
								ack = ackMsg[0];
								originMsg = ackMsg[1];
							} catch (ArrayIndexOutOfBoundsException e) {
								System.out.println("msg format error");
								continue;
							} catch (Exception e) {
								System.out.println(e.getMessage());
								continue;
							}

							// response ACK
							send(":ACK=" + ack, client);

							if (!clientMap.containsKey(client)) {
								System.out.println("client: " + client
										+ " is connected!");
							}
							synchronized (clientMap) {
								clientMap.put(client,
										System.currentTimeMillis());
							}

							if (originMsg.equals(MSG_REQUIRE_ONLINE_CLIENT)) {
								responseOnlineClient(client);
							} else {
								// send(MSG_RESPONSE_HEART_BEAT, client);
								// sendWithMsgDigit(MSG_RESPONSE_HEART_BEAT,
								// client);
							}
						} catch (IOException e1) {
							System.err.println("receive error: "
									+ e1.getMessage());
							stop = true;
						}

						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							stop = true;
							e.printStackTrace();
						}
					}

				}
			}).start();

			// refresh online client thread
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (!stop) {
						// sleep
						try {
							Thread.sleep(refreshPeriodSecond * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						System.out.println("refreshClient...");
						refreshClient(refreshPeriodSecond);
					}
				}
			}).start();

		}
	}

	public void responseOnlineClient(Client client) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(MSG_RESPONSE_ONLINE_CLIENT_PREFIX);
		synchronized (clientMap) {

			for (Client onlineClient : clientMap.keySet()) {

				// last heart beat time
				// long timeMillis = clientMap.get(key);
				// Date date = new Date(timeMillis);
				// SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
				// String time = sdf.format(date);

				// sb.append(key + ":" + time + "\n");
				if (onlineClient.equals(client)) {
					sb.append(onlineClient + ":you" + "\n");
				} else {
					sb.append(onlineClient + "\n");
				}
			}
		}

		// System.out.println(sb.toString());
		// send(sb.toString(), client);
		sendWithMsgDigit(sb.toString(), client);
	}

	public void refreshClient(int second) {
		StringBuilder sb = new StringBuilder();
		sb.append(MSG_RESPONSE_ONLINE_CLIENT_PREFIX);
		long expiration = System.currentTimeMillis() - second * 1000;
		synchronized (clientMap) {
			for (Client key : clientMap.keySet()) {
				if (clientMap.get(key) < expiration) {
					clientMap.remove(key);
				} else {
					sb.append(key + "\n");
				}
			}
		}
		System.out.println(sb.toString());
	}

	public void clearClient() {
		synchronized (clientMap) {
			clientMap.clear();
		}
	}

	public static void main(String[] args) {

		P2PServer server;
		// check argument
		if (args.length == 1) {
			int port;
			try {
				port = Integer.valueOf(args[0]);
			} catch (Exception e) {
				System.out.println("bad argument!");
				System.exit(0);
				return;
			}
			server = new P2PServer(port);
		} else {
			server = new P2PServer();
		}
		try {
			server.init();
		} catch (SocketException e1) {
			System.err.println("bind error :" + e1.getMessage());
			System.exit(0);
		}
		// debug
		// server.setVerbose(true);
		server.start();
		Scanner sc = new Scanner(System.in);
		while (sc.hasNext()) {

			try {
				String s = sc.nextLine();
				if (s.equals(":q")) {
					System.out.println("exit...");
					break;
				} else if (s.equals(":cls")) {
					System.out.println("clear client...");
					server.clearClient();
				} else if (s.startsWith(":rc")) {
					int second = DEFAULT_REFRESH_PERIOD_SECOND;
					if (s.matches(":rc \\d{1,10}")) {
						second = Integer.valueOf(s.split(" ")[1]);
					}
					System.out.println("refresh online client ( " + second
							+ " seconds )...");
					server.refreshClient(second);
				} else if (s.equals(":v")) {
					System.out.println("verbose...");
					server.setVerbose(true);
				} else if (s.equals(":nv")) {
					System.out.println("non-verbose...");
					server.setVerbose(false);
				} else {
					System.out.println("bad command");
				}
			} catch (Exception e) {
				System.out.println("bad command");
			}
		}
		System.out.println("end");
		sc.close();
		System.exit(0);
	}
}

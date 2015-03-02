package com.nightwind.p2pchat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class P2PClient extends UdpBase {

	public static String DEFAULT_SERVER_HOSTNAME = "120.24.223.185";

	private boolean stop = true;

	protected Client myClient;

	private List<Client> clientList = new ArrayList<>();

	private String serverHostname = DEFAULT_SERVER_HOSTNAME;

	private int serverPort = 4869;

	protected int timeoutSecond = 3;

	protected int heartBreatPeriodSecond = 10;

	protected long lastHeartBeat;

	protected Set<String> sendMsgSet = new HashSet<>();

	protected Set<String> serverSendMsgSet = new HashSet<>();

	protected boolean serverAvaliable;

	protected boolean verbose;

	protected boolean oneChat;

	protected Client oneChatClient;

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public String getServerHostname() {
		return serverHostname;
	}

	public void setServerHostname(String serverHostname) {
		this.serverHostname = serverHostname;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public List<Client> getClientList() {
		return clientList;
	}

	public void setClientList(List<Client> clientList) {
		this.clientList = clientList;
	}

	public int getHeartBreatPeriodSecond() {
		return heartBreatPeriodSecond;
	}

	public void setHeartBreatPeriodSecond(int heartBreatPeriodSecond) {
		this.heartBreatPeriodSecond = heartBreatPeriodSecond;
	}

	public long getLastHeartBeat() {
		return lastHeartBeat;
	}

	public void setLastHeartBeat(long lastHeartBeat) {
		this.lastHeartBeat = lastHeartBeat;
	}

	public boolean isServerAvaliable() {
		return serverAvaliable;
	}

	public void setServerAvaliable(boolean serverAvaliable) {
		this.serverAvaliable = serverAvaliable;
	}

	public int getTimeoutSecond() {
		return timeoutSecond;
	}

	public void setTimeoutSecond(int timeoutSecond) {
		this.timeoutSecond = timeoutSecond;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public boolean isOneChat() {
		return oneChat;
	}

	public void setOneChat(boolean oneChat) {
		this.oneChat = oneChat;
	}

	public Client getOneChatClient() {
		return oneChatClient;
	}

	public void setOneChatClient(Client oneChatClient) {
		this.oneChatClient = oneChatClient;
	}

	public P2PClient() {
		super();
	};

	public P2PClient(int localPort) {
		super(localPort);
	}

	public P2PClient(String serverHostname, int serverPort)
			throws UnknownHostException {
		super();
		serverHostname = InetAddress.getByName(serverHostname).toString()
				.replaceAll(".{0,100}/", "");
		this.serverHostname = serverHostname;
		this.serverPort = serverPort;
	}

	public P2PClient(String serverHostname, int serverPort, int localPort)
			throws UnknownHostException {
		super(localPort);
		serverHostname = InetAddress.getByName(serverHostname).toString()
				.replaceAll(".{0,100}/", "");
		this.serverHostname = serverHostname;
		this.serverPort = serverPort;
	}

	public void requireOnlineClient() throws IOException {
		// send(MSG_REQUIRE_ONLINE_CLIENT, new Client(serverHostname,
		// serverPort));
		sendWithMsgDigit(MSG_REQUIRE_ONLINE_CLIENT, new Client(serverHostname,
				serverPort));
	}

	public List<Client> refreshOnlineClient(String msg) {
		List<Client> clientsBak = new ArrayList<>();
		clientsBak.addAll(clientList);
		clientList.clear();
		try {
			for (String strClient : msg.split("\n")) {
				String[] clientInfo = strClient.split(":");
				String hostname = clientInfo[0];
				int port = Integer.valueOf(clientInfo[1]);
				if (clientInfo.length == 3 && clientInfo[2].equals("you")) {
					myClient = new Client(hostname, port);
				}
				Client client = new Client(hostname, port);
				clientList.add(client);
			}
		} catch (Exception e) {
			clientList.addAll(clientsBak);
		}
		return clientList;
	}

	protected void printOnlineClient() {
		System.out.println("online client:");
		int i = 1;
		for (Client client : clientList) {
			System.out.println("\t("
					+ i++
					+ ") "
					+ client
					+ (myClient != null && myClient.equals(client) ? " (you)"
							: ""));
		}
	}

	public void start() {
		System.out.println("client starting");
		if (stop) {
			stop = false;

			// start message receiver
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (!stop) {
						try {
							String[] recv = receive();
							String recvMsg = recv[0];
							String hostname = recv[1];
							int port = Integer.valueOf(recv[2]);

							if (verbose) {
								System.out.println("recvMsg: " + hostname + ":"
										+ port + " " + recvMsg);
							}

							// check ACK
							if (recvMsg.matches(REGEX_ACK_MATCH)) {
								String md5Hex = recvMsg.split("=")[1];
								sendMsgSet.remove(md5Hex);
								if (serverSendMsgSet.contains(md5Hex)) {
									serverSendMsgSet.remove(md5Hex);
									if (!serverAvaliable) {
										serverAvaliable = true;
										System.out.println("server connected!");
									}
								}
							} else {
								// is not ACK
								String ack;
								String originMsg;

								try {
									String[] ackMsg = getAckMsg(recvMsg);
									ack = ackMsg[0];
									originMsg = ackMsg[1];
								} catch (Exception e) {
									System.out
											.println("message verification failed: "
													+ e.getMessage());
									continue;
								}

								// response ACK
								send(":ACK=" + ack, hostname, port);

								// handle message
								if (hostname.equals(serverHostname)) {
									// server

									if (!serverAvaliable) {
										System.out.println("server connected!");
									}

									// refresh online client
									if (originMsg
											.startsWith(MSG_RESPONSE_ONLINE_CLIENT_PREFIX)) {
										String clients = originMsg
												.replaceAll(
														MSG_RESPONSE_ONLINE_CLIENT_PREFIX,
														"");
										refreshOnlineClient(clients);
										printOnlineClient();
									}
									lastHeartBeat = System.currentTimeMillis();
									serverAvaliable = true;

								} else {
									// handle other client message

									// print user message
									String out = String.format("[%s:%d]: %s",
											hostname, port, originMsg);
									System.out.println(out);

								}
							}
						} catch (IOException e) {
							System.err.println("receive error: "
									+ e.getMessage());
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

			// start send server heart beat thread
			new Thread(new Runnable() {

				@Override
				public void run() {
					while (!stop) {
						try {
							while (ds == null) {
								Thread.sleep(100);
							}
							String originMsg = MSG_RESPONSE_HEART_BEAT;

							// send message with message digit
							sendWithMsgDigit(originMsg, new Client(
									serverHostname, serverPort));

						} catch (IOException e) {
							System.err.println("send hear beat error: "
									+ e.getMessage());
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						try {
							Thread.sleep(heartBreatPeriodSecond * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}
			}).start();

		}
	}

	/**
	 * start timeout checker
	 * 
	 * @param md5Hex
	 * @param checkMsg
	 */
	private void timeoutChecker(final String md5Hex, final String checkMsg) {

		new Thread(new Runnable() {

			@Override
			public void run() {
				// wait timeout
				try {
					Thread.sleep(timeoutSecond * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// send message info
				String[] info = checkMsg.split(":");
				String hostname = info[0];
				String port = info[1];
				String time = info[2];
				String originMsg = checkMsg.replaceFirst(hostname + ":" + port
						+ ":" + time + ":", "");

				// response timeout
				if (sendMsgSet.contains(md5Hex)) {
					sendMsgSet.remove(md5Hex);

					if (serverSendMsgSet.contains(md5Hex)) {
						// server
						serverSendMsgSet.remove(md5Hex);

						if (originMsg.equals(MSG_RESPONSE_HEART_BEAT)) {
							// heart beat
							long now = System.currentTimeMillis();
							if (now - lastHeartBeat > timeoutSecond * 1000) {
								System.out
										.println("server connect timeout ("
												+ timeoutSecond
												+ " seconds), retry after "
												+ (heartBreatPeriodSecond - timeoutSecond)
												+ " seconds");
								serverAvaliable = false;
							}
						} else if (originMsg
								.startsWith(MSG_RESPONSE_ONLINE_CLIENT_PREFIX)) {
							// require online client
							System.out.println("pull online client time out");
						}
					} else {
						// other client
						System.out.println("failed to send " + hostname + ":"
								+ port + " " + originMsg);
					}
				} else {
					// connect fine, do nothing.
				}

			}

		}).start();
	}

	@Override
	public String[] sendWithMsgDigit(String originMsg, Client client)
			throws IOException {
		String[] checkMsgAndMd5 = super.sendWithMsgDigit(originMsg, client);

		for (int i = 0; i < checkMsgAndMd5.length; i += 2) {
			String checkMsg = checkMsgAndMd5[i];
			String md5Hex = checkMsgAndMd5[i + 1];

			sendMsgSet.add(md5Hex);
			if (client.getHostname().equals(serverHostname)) {
				serverSendMsgSet.add(md5Hex);
			}

			// start timeout checker
			timeoutChecker(md5Hex, checkMsg);
		}

		return checkMsgAndMd5;
	}

	public static void main(String[] args) throws IOException {
		String host = DEFAULT_SERVER_HOSTNAME;
		// String host = "127.0.0.1";
		int remotePort = 4869;
		int localPort = 48690;

		// check argument
		try {
			if (args.length >= 1) {
				host = args[0];
			}
			if (args.length >= 2) {
				remotePort = Integer.valueOf(args[1]);
			}
			if (args.length >= 3) {
				localPort = Integer.valueOf(args[2]);
			}
		} catch (Exception e) {
			System.err.println("bad argument!");
			System.exit(0);
			return;
		}

		P2PClient client = new P2PClient(host, remotePort, localPort);
		client.start();
		// debug
		// client.setVerbose(true);
		try {
			client.init();
		} catch (Exception e) {
			System.err.println("bind error: " + e.getMessage());
			System.exit(0);
		}
		client.requireOnlineClient();
		Scanner sc = new Scanner(System.in);
		while (sc.hasNext()) {
			try {

				String strInput = sc.nextLine();
				String cmd[] = strInput.split(" ");

				if (cmd[0].equals(":q")) {
					if (client.isOneChat()) {
						client.setOneChat(false);
						System.out.println("exit one chat mode...");
					} else {
						System.out.println("exit...");
						break;
					}
				} else if (cmd[0].equals(":oc")) {
					System.out.println("show online client...");
					client.requireOnlineClient();
				} else if (cmd[0].equals(":c")) {
					Client c;
					if (cmd[1].matches(".{1,100}:\\d{1,5}$")) {
						String hostname = cmd[1].split(":")[0];
						String ip = InetAddress.getByName(hostname).toString()
								.replaceAll(".{0,100}/", "");
						int port = Integer.valueOf(cmd[1].split(":")[1]);
						c = new Client(ip, port);
					} else {
						int id = Integer.valueOf(cmd[1]) - 1;
						c = client.getClientList().get(id);
					}
					if (cmd.length == 2) {
						// one chat mode
						client.setOneChat(true);
						client.setOneChatClient(c);
						System.out
								.println("start one chat mode. You can send message directly. Entry command \":q\" to exit");
					} else {
						String msg = strInput.replaceFirst(
								":c " + cmd[1] + " ", "");
						client.sendWithMsgDigit(msg, c);
					}
				} else if (cmd[0].equals(":v")) {
					System.out.println("verbose...");
					client.setVerbose(true);
				} else if (cmd[0].equals(":nv")) {
					System.out.println("non-verbose...");
					client.setVerbose(false);
				} else if (cmd[0].equals(":debug")) {
					String tmp = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789abcdefghijabcdefghijabcdefghij";
					client.sendWithMsgDigit(tmp, new Client("127.0.0.1", 4869));
				} else {
					if (client.isOneChat()) {
						// one chat mode: send message directly.
						client.sendWithMsgDigit(strInput,
								client.getOneChatClient());
					} else {
						System.out.println("bad command!");
					}
				}
			} catch (Exception e) {
				System.out.println("bad command!");
			}
		}
		System.out.println("end");
		client.setStop(true);
		sc.close();
		System.exit(0);
	}
}

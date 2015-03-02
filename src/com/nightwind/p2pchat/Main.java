package com.nightwind.p2pchat;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
//			P2PClient.main(args);
			printHelp();
		} else if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
			printHelp();
		} else {
			String[] newArgs = new String[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				newArgs[i-1] = args[i];
			}
			if (args[0].equals("client")) {
				P2PClient.main(newArgs);
			} else if (args[0].equals("server")) {
				P2PServer.main(newArgs);
			} else {
				System.err.println("bad argument!");
			}
			System.exit(0);
		}
	}

	private static void printHelp() {
		System.out.println("[p2pchat v1.0]\nUsage:\t");
		System.out.println("\tRun as a client:\tp2pchat.jar client [serverHostname serverPort localPort]");
		System.out.println("\tRun as a server:\tp2pchat.jar server [localPort]");
		System.out.println("Options:\n\t-h, --help\tdisplay this help and exit");
		System.out.println("Default:\n\tserverHostname=nw4869.xyz, serverPort=4869, localPort=48690");
	}
	
}

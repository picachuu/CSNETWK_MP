/*
 * Ang, Czarina
 * Herrera, Diego
 * Lim, Jannica
 */

import java.net.*;
import java.util.Scanner;
import java.io.*;

public class MP_Client
{
	private static boolean isValidIPAddress(String ip) {
        return ip.equals("localhost") || ip.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }

    private static boolean isValidPort(String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            return port >= 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

	private static void processCommand(String command, PrintWriter out, BufferedReader in) {
		String[] tokens = command.split(" ");
		String cmd = tokens[0].toLowerCase();
	
		try {
			switch (cmd) {
				case "/leave":
					out.println("LEAVE_COMMAND"); // Replace with the actual server command
					System.exit(0); // Exit the client application
					break;
				case "/register":
					if (tokens.length == 2) {
						out.println("REGISTER_COMMAND " + tokens[1]); // Replace with the actual server command
					} else {
						System.out.println("Usage: /register <handle>");
					}
					break;
				case "/store":
					if (tokens.length == 2) {
						sendFileToServer(tokens[1], out, in); // You need to implement this method
					} else {
						System.out.println("Usage: /store <filename>");
					}
					break;
				case "/dir":
					out.println("DIR_COMMAND"); // Replace with the actual server command
					break;
				case "/get":
					if (tokens.length == 2) {
						out.println("GET_COMMAND " + tokens[1]); // Replace with the actual server command
					} else {
						System.out.println("Usage: /get <filename>");
					}
					break;
				case "/?":
					out.println("HELP_COMMAND"); // Replace with the actual server command
					break;
				default:
					System.out.println("Unknown command. Type /? for help.");
					break;
			}
		} catch (Exception e) {
			System.out.println("An error occurred while processing the command: " + e.getMessage());
		}
	}	

	private static class ReceivedMessagesHandler implements Runnable {
        private BufferedReader in;

        public ReceivedMessagesHandler(BufferedReader in) {
            this.in = in;
        }

        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	public static void main(String[] args)
	{
		Scanner scanner = new Scanner(System.in);
        String sServerAddress = "";
        int nPort = 0;
        boolean validCommand = false;
		System.out.println("Welcome to the File Exchange System!");

        while (!validCommand) {
            System.out.println("Enter command to join (Format: /join <ip> <port>):");
            String joinCommand = scanner.nextLine();
            String[] parts = joinCommand.split(" ");

            if (parts.length == 3 && parts[0].equalsIgnoreCase("/join") && 
                isValidIPAddress(parts[1]) && isValidPort(parts[2])) {
                sServerAddress = parts[1];
                nPort = Integer.parseInt(parts[2]);
                validCommand = true;
            } else {
                System.out.println("\nInvalid command. Please check your format.");
            }
        }

		try(Socket clientEndpoint = new Socket(sServerAddress, nPort);
		PrintWriter out = new PrintWriter(clientEndpoint.getOutputStream(), true);
		BufferedReader in = new BufferedReader(new InputStreamReader(clientEndpoint.getInputStream()))) {

			System.out.println("\nClient: Connecting to server at " + clientEndpoint.getRemoteSocketAddress());
			System.out.println("Connection to the File Exchange Server is successful!");

			new Thread(new ReceivedMessagesHandler(in)).start();

			String userInput;
			while (!(userInput = scanner.nextLine()).equalsIgnoreCase("/leave")) {
				if (userInput.startsWith("/")) {
					processCommand(userInput, out, in);
				} else {
					out.println(userInput);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			scanner.close();
			System.out.println("Connection closed. Thank you!.");
		}
	}
}


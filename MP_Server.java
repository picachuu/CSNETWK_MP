/*
 * Ang, Czarina
 * Herrera, Diego
 * Lim, Jannica
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class MP_Server {
	private static Map<String, Socket> clientList = new HashMap<>();

    public static void main(String[] args) throws IOException {
		int nPort = Integer.parseInt(args[0]);
		System.out.println("Server: Listening on port " + args[0] + "...");
		System.out.println();
		Socket serverEndpoint;

        ServerSocket serverSocket = new ServerSocket(nPort);
        System.out.println("Server started on port: " + nPort);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                
                String clientName = in.readLine();
                clientList.put(clientName, clientSocket);
                System.out.println(clientName + " connected.");

                // Handle client messages
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(clientName + ": " + inputLine);
                    // Add message handling and command processing here
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

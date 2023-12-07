/*
 * Ang, Czarina
 * Herrera, Diego
 * Lim, Jannica
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.text.SimpleDateFormat;

public class MPServer {
    private static List<String> listOfAliases = new CopyOnWriteArrayList<>();
    private static List<Socket> listOfSockets = new CopyOnWriteArrayList<>();
    private static Map<String, Socket> aliasSocketMap = new HashMap<>();
    private static final Object lock = new Object();

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java MPServer <IP address> <port number>");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number.");
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(args[0]))) {
            System.out.println("Server has started.");

            while (true) {
                Socket connectionSocket = serverSocket.accept();
                listOfSockets.add(connectionSocket);

                InetAddress clientAddress = connectionSocket.getInetAddress();
                int clientPort = connectionSocket.getPort();
                System.out.println("Server: Client at " + clientAddress.getHostAddress() + ":" + clientPort + " has connected.");

                Thread clientThread = new Thread(() -> handleClient(connectionSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

private static void handleClient(Socket clientSocket) {
    String clientName = ""; // Default for unregistered client
    try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

        while (true) {
            String query = reader.readLine();
            if (query == null || query.isEmpty()) {
                query = "placeholder";
            }
            String[] parts = query.split(" ");

            switch (parts[0]) {
                case "/register":
                    synchronized (lock) {
                        if (listOfAliases.contains(parts[1])) {
                            writer.println("Error: Registration failed. Handle or alias already exists.");
                        } else {
                            if (!clientName.isEmpty()) {
                                listOfAliases.remove(clientName);
                                aliasSocketMap.remove(clientName);
                            }
                            clientName = parts[1];
                            listOfAliases.add(clientName);
                            aliasSocketMap.put(clientName, clientSocket);
                            writer.println("Welcome, " + parts[1] + "!");
                        }
                    }
                    break;

                case "/store":
                    if (clientName.isEmpty()) {
                        writer.println("Please register first.");
                    } else {
                        storeFile(parts[1], reader, writer); // Handle file storage
                    }
                    break;

                case "/dir":
                    if (clientName.isEmpty()) {
                        writer.println("Please register first.");
                    } else {
                        writer.println(getDirectoryList()); // Send directory list to client
                    }
                    break;

                case "/get":
                    if (clientName.isEmpty()) {
                        writer.println("Please register first.");
                    } else {
                        getFile(parts[1], writer); // Handle file retrieval
                    }
                    break;

                case "/leave":
                    synchronized (lock) {
                        if (!clientName.isEmpty()) {
                            listOfAliases.remove(clientName);
                            aliasSocketMap.remove(clientName);
                        }
                        listOfSockets.remove(clientSocket);
                    }
                    return; // Exit the method to end thread

                case "/broadcast":
                    if (clientName.isEmpty()) {
                        writer.println("Please register first.");
                    } else {
                        String message = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                        broadcastMessage("Broadcast from " + clientName + ": " + message, clientSocket); // Broadcast message
                    }
                    break;
            
                case "/message":
                    if (clientName.isEmpty()) {
                        writer.println("Please register first.");
                    } else if (parts.length < 3) {
                        writer.println("Error: Incorrect usage of /message. Correct format: /message <alias> <message>");
                    } else {
                        String alias = parts[1];
                        String message = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                        sendMessage(alias, message, clientName, writer); // Send direct message
                    }
                    break;
            
                default:
                    writer.println("Error: Command not found.");
                    break;
            }
        }
    } catch (IOException e) {
        System.out.println("Error handling client: " + e.getMessage());
    } finally {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing client socket: " + e.getMessage());
        }
    }
    System.out.println("Client from " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + " has disconnected.");
}

private static void storeFile(String filename, BufferedReader reader, PrintWriter writer) {
    try {
        writer.println("ready"); // Indicate that the server is ready to receive the file
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filename));
        String line;
        while (!(line = reader.readLine()).equals("EOF")) { // Assuming "EOF" marks the end of the file content
            fileWriter.write(line);
            fileWriter.newLine();
        }
        fileWriter.close();
        System.out.println("File " + filename + " has been stored.");
    } catch (IOException e) {
        System.out.println("Error storing file: " + e.getMessage());
    }
}

private static String getDirectoryList() {
    File currentDirectory = new File(".");
    File[] filesList = currentDirectory.listFiles();
    StringBuilder files = new StringBuilder("Server files:\n");
    assert filesList != null;
    for (File file : filesList) {
        if (file.isFile() && !file.getName().equals("MPServer.java")) { // Exclude the server file itself
            files.append(file.getName()).append("\t");
        }
    }
    return files.toString();
}

private static void getFile(String filename, PrintWriter writer) {
    File file = new File(filename);
    if (!file.exists() || file.isDirectory()) {
        writer.println("Error: File not found in server.");
        return;
    }

    try {
        writer.println("1"); // Indicate that the server is ready to send the file
        BufferedReader fileReader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = fileReader.readLine()) != null) {
            writer.println(line);
        }
        writer.println("EOF"); // Indicate end of file content
        fileReader.close();
    } catch (IOException e) {
        System.out.println("Error sending file: " + e.getMessage());
    }
}

private static void broadcastMessage(String message, Socket senderSocket) {
    for (Socket clientSocket : listOfSockets) {
        if (clientSocket != senderSocket) { // Do not send the message back to the sender
            try {
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                writer.println(message);
            } catch (IOException e) {
                System.out.println("Error broadcasting message to a client: " + e.getMessage());
            }
        }
    }
}

private static void sendMessage(String alias, String message, String senderName, PrintWriter senderWriter) {
    Socket receiverSocket = aliasSocketMap.get(alias);
    if (receiverSocket != null) {
        try {
            PrintWriter writer = new PrintWriter(receiverSocket.getOutputStream(), true);
            writer.println("From " + senderName + ": " + message);
        } catch (IOException e) {
            System.out.println("Error sending message to alias " + alias + ": " + e.getMessage());
        }
    } else {
        senderWriter.println("User alias " + alias + " does not exist.");
    }
}

}

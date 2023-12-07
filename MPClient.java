/*
 * Ang, Czarina
 * Herrera, Diego
 * Lim, Jannica
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class MPClient {

    private static boolean isConnected = false;
    private static Socket clientSocket;
    private static Thread listenThread;
    private static volatile boolean exit = false;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome! Type `/?` to see the list of commands");

        while (true) {
            if (isConnected) {
                startListening();
            }

            System.out.print("> ");
            String input = scanner.nextLine();
            String[] cmd = input.split(" ");

            switch (cmd[0]) {
                case "/?":
                    displayCommands();
                    break;
            
                case "/join":
                    handleJoin(cmd);
                    break;
            
                case "/leave":
                    handleLeave(cmd);
                    break;
            
                case "/register":
                    handleRegister(cmd);
                    break;
            
                case "/store":
                    handleStore(cmd);
                    break;
            
                case "/dir":
                    handleDir(cmd);
                    break;
            
                case "/get":
                    handleGet(cmd);
                    break;
            
                case "/shutdown":
                    handleShutdown();
                    break;
            
                case "/broadcast":
                    handleBroadcast(cmd);
                    break;
            
                case "/message":
                    handleMessage(cmd);
                    break;
            
                default:
                    System.out.println("Error: Command not found.");
                    break;
            }            

            if (cmd[0].equals("/shutdown")) {
                if (isConnected) {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }

        scanner.close();
        System.out.println("Goodbye!");
    }

    private static void displayCommands() {
        System.out.println("/join <server_ip_add> <port> \t connect to the server application");
        System.out.println("/leave \t\t\t\t disconnect from the server application");
        System.out.println("/register <handle> \t\t register a unique handle or alias");
        System.out.println("/store <filename> \t\t send file to server");
        System.out.println("/dir \t\t\t\t request directory file list from a server");
        System.out.println("/get <filename> \t\t fetch a file from a server");
        System.out.println("/shutdown \t\t\t exit client application");
        System.out.println("/? \t\t\t\t display list of commands");
        System.out.println("/broadcast <message> \t\t send a message to all connected clients");
        System.out.println("/message <alias> <message> \t send a message to a single connected client");
    }    

    private static void startListening() {
        if (listenThread == null || !listenThread.isAlive()) {
            Runnable listenTask = () -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String message;
                    while (!exit && (message = reader.readLine()) != null) {
                        System.out.println("Message: " + message + "\n> ");
                    }
                } catch (IOException e) {
                    if (!exit) {
                        System.out.println("Error in listening: " + e.getMessage());
                    }
                }
            };
    
            listenThread = new Thread(listenTask);
            listenThread.start();
        }
    }

    private static void handleJoin(String[] cmd) {
        if (cmd.length != 3) {
            System.out.println("Error: Incorrect usage of /join. Correct format: /join <server_ip> <port>");
            return;
        }
    
        if (isConnected) {
            System.out.println("Already connected to a server. Please /leave before joining another server.");
            return;
        }
    
        String serverIp = cmd[1];
        int port;
    
        try {
            port = Integer.parseInt(cmd[2]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid port number.");
            return;
        }
    
        try {
            clientSocket = new Socket(serverIp, port);
            isConnected = true;
            System.out.println("Connection to the File Exchange Server is successful!");
        } catch (IOException e) {
            System.out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
        }
    }

    private static void handleLeave(String[] cmd) {
        if (cmd.length != 1) {
            System.out.println("Error: Incorrect usage of /leave. No additional parameters expected.");
            return;
        }
    
        if (!isConnected) {
            System.out.println("Error: Not connected to any server.");
            return;
        }
    
        try {
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            writer.println("/leave");
            clientSocket.close();
            isConnected = false;
            System.out.println("Disconnected from the server.");
        } catch (IOException e) {
            System.out.println("Error while disconnecting: " + e.getMessage());
        }
    }

    private static void handleRegister(String[] cmd) {
        if (cmd.length != 2) {
            System.out.println("Error: Incorrect usage of /register. Correct format: /register <handle>");
            return;
        }
    
        if (!isConnected) {
            System.out.println("Error: Not connected to any server.");
            return;
        }
    
        try {
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer.println(String.join(" ", cmd));
    
            String response = reader.readLine();
            System.out.println(response);
        } catch (IOException e) {
            System.out.println("Error during registration: " + e.getMessage());
        }
    }

    private static void handleStore(String[] cmd) {
        if (cmd.length != 2) {
            System.out.println("Error: Incorrect usage of /store. Correct format: /store <filename>");
            return;
        }
    
        if (!isConnected) {
            System.out.println("Error: Not connected to any server.");
            return;
        }
    
        String filename = cmd[1];
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Error: File not found.");
            return;
        }
    
        try (BufferedReader fileReader = new BufferedReader(new FileReader(file));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
    
            writer.println("/store " + filename);
            
            String response = reader.readLine();
            if (!"ready".equals(response)) {
                System.out.println(response);
                return;
            }
    
            String line;
            while ((line = fileReader.readLine()) != null) {
                writer.println(line);
            }
            writer.println("EOF"); // Indicate end of file content
    
            System.out.println("File successfully uploaded.");
        } catch (IOException e) {
            System.out.println("Error during file store: " + e.getMessage());
        }
    }

    private static void handleDir(String[] cmd) {
        if (cmd.length != 1) {
            System.out.println("Error: Incorrect usage of /dir. No additional parameters expected.");
            return;
        }
    
        if (!isConnected) {
            System.out.println("Error: Not connected to any server.");
            return;
        }
    
        try {
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    
            writer.println("/dir");
    
            String response = reader.readLine();
            System.out.println("Directory list from server:\n" + response);
        } catch (IOException e) {
            System.out.println("Error during directory fetch: " + e.getMessage());
        }
    }

    private static void handleGet(String[] cmd) {
        if (cmd.length != 2) {
            System.out.println("Error: Incorrect usage of /get. Correct format: /get <filename>");
            return;
        }
    
        if (!isConnected) {
            System.out.println("Error: Not connected to any server.");
            return;
        }
    
        String filename = cmd[1];
    
        try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
    
            writer.println("/get " + filename);
    
            String serverStatus = reader.readLine();
            if (!"1".equals(serverStatus)) {
                System.out.println(serverStatus);
                return;
            }
    
            try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filename))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if ("EOF".equals(line)) {
                        break;
                    }
                    fileWriter.write(line);
                    fileWriter.newLine();
                }
            }
    
            System.out.println("File successfully downloaded.");
        } catch (IOException e) {
            System.out.println("Error during file get: " + e.getMessage());
        }
    }

    private static void handleShutdown() {
        if (isConnected) {
            try {
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                writer.println("/leave");
                clientSocket.close();
                System.out.println("Disconnected from the server.");
            } catch (IOException e) {
                System.out.println("Error while shutting down: " + e.getMessage());
            }
        }
        isConnected = false;
        exit = true; // To stop the listening thread
        System.out.println("Client application is shutting down.");
    }

    private static void handleBroadcast(String[] cmd) {
        if (cmd.length < 2) {
            System.out.println("Error: Incorrect usage of /broadcast. Correct format: /broadcast <message>");
            return;
        }
    
        if (!isConnected) {
            System.out.println("Error: Not connected to any server.");
            return;
        }
    
        try {
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            String message = String.join(" ", cmd);
            writer.println(message);
        } catch (IOException e) {
            System.out.println("Error during broadcasting: " + e.getMessage());
        }
    }

    private static void handleMessage(String[] cmd) {
        if (cmd.length < 3) {
            System.out.println("Error: Incorrect usage of /message. Correct format: /message <alias> <message>");
            return;
        }
    
        if (!isConnected) {
            System.out.println("Error: Not connected to any server.");
            return;
        }
    
        try {
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            String message = String.join(" ", cmd);
            writer.println(message);
        } catch (IOException e) {
            System.out.println("Error during message sending: " + e.getMessage());
        }
    }

    // Add methods for other command handling
}

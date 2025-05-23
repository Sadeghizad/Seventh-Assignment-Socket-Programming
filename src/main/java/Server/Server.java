package Server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.io.IOException;
import Shared.User;

public class Server {
    // Predefined users for authentication
    private static final User[] users = {
            new User("user1", "1234"),
            new User("user2", "1234"),
            new User("user3", "1234"),
            new User("user4", "1234"),
            new User("user5", "1234"),
    };

    // List of currently connected clients
    public static ArrayList<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        final int PORT = 12345;

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static boolean authenticate(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    // Broadcast a message to all connected clients
    public static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {  // Don't send back to sender
                client.sendMessage(message);
            }
        }
    }

    // Remove a client from the list when they disconnect
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client disconnected. Total clients: " + clients.size());
    }
}
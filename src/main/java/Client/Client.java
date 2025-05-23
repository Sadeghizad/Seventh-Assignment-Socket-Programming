package Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class Client {
    private static BufferedReader in;
    private static PrintWriter out;
    private static Socket socket;
    private static String username;

    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("localhost", 12345)) {
            Client.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);

            System.out.println("===== Welcome to CS Music Room =====");

            boolean loggedIn = false;
            while (!loggedIn) {
                System.out.print("Username: ");
                username = scanner.nextLine();
                System.out.print("Password: ");
                String password = scanner.nextLine();

                sendLoginRequest(username, password);

                String response = in.readLine();
                if ("LOGIN_SUCCESS".equals(response)) {
                    loggedIn = true;
                    System.out.println("Login successful!");
                } else {
                    System.out.println("Login failed. Please try again.");
                }
            }

            while (true) {
                printMenu();
                System.out.print("Enter choice: ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1" -> enterChat(scanner);
                    case "2" -> uploadFile(scanner);
                    case "3" -> requestDownload(scanner);
                    case "0" -> {
                        System.out.println("Exiting...");
                        out.println("EXIT");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Enter chat box");
        System.out.println("2. Upload a file");
        System.out.println("3. Download a file");
        System.out.println("0. Exit");
    }

    private static void sendLoginRequest(String username, String password) {
        out.println("LOGIN:" + username + ":" + password);
    }

    private static void enterChat(Scanner scanner) throws IOException {
        System.out.println("You have entered the chat (type /exit to leave)");

        // Start message receiver thread
        Thread receiverThread = new Thread(new ClientReceiver(in));
        receiverThread.start();

        String message_string = "";
        while (!message_string.equalsIgnoreCase("/exit")) {
            message_string = scanner.nextLine();
            if (!message_string.equalsIgnoreCase("/exit")) {
                sendChatMessage(message_string);
            }
        }
        out.println("CHAT_EXIT");
        receiverThread.interrupt();
    }

    private static void sendChatMessage(String message_to_send) throws IOException {
        out.println("MSG:" + message_to_send);
    }

    private static void uploadFile(Scanner scanner) throws IOException {
        Path userDir = Paths.get("D:\\Documents\\IdeaProjects\\Seventh-Assignment-Socket-Programming\\src\\main\\resources\\Client\\" + username);
        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
        }

        File[] files = userDir.toFile().listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No files to upload.");
            return;
        }

        System.out.println("Select a file to upload:");
        for (int i = 0; i < files.length; i++) {
            System.out.println((i + 1) + ". " + files[i].getName());
        }

        System.out.print("Enter file number: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        if (choice < 0 || choice >= files.length) {
            System.out.println("Invalid choice.");
            return;
        }

        File selectedFile = files[choice];
        out.println("UPLOAD:" + selectedFile.getName() + ":" + selectedFile.length());

        // Wait for server acknowledgement
        String response = in.readLine();
        if (!"READY".equals(response)) {
            System.out.println("Server not ready for upload.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(selectedFile);
             OutputStream os = socket.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            System.out.println("File uploaded successfully!");
        }
    }

    private static void requestDownload(Scanner scanner) throws IOException {
        out.println("LIST_FILES");

        // Read list of available files
        List<String> files = new ArrayList<>();
        String line;
        while (!(line = in.readLine()).equals("END_LIST")) {
            files.add(line);
        }

        if (files.isEmpty()) {
            System.out.println("No files available for download.");
            return;
        }

        System.out.println("Available files:");
        for (int i = 0; i < files.size(); i++) {
            System.out.println((i + 1) + ". " + files.get(i));
        }

        System.out.print("Enter file number: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        if (choice < 0 || choice >= files.size()) {
            System.out.println("Invalid choice.");
            return;
        }

        String selectedFile = files.get(choice);
        out.println("DOWNLOAD:" + selectedFile);

        // Create user directory if it doesn't exist
        Path userDir = Paths.get("D:\\Documents\\IdeaProjects\\Seventh-Assignment-Socket-Programming\\src\\main\\resources\\Client\\" + username);
        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
        }

        Path filePath = userDir.resolve(selectedFile);
        try (InputStream is = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("File downloaded successfully to: " + filePath);
        }
    }
}
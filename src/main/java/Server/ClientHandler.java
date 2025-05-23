package Server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            // Authentication phase
            String credentials = in.readLine();
            String[] parts = credentials.split(":");
            if (parts.length == 3 && parts[0].equals("LOGIN")) {
                String username = parts[1];
                String password = parts[2];

                if (Server.authenticate(username, password)) {
                    this.username = username;
                    out.println("LOGIN_SUCCESS");
                    System.out.println(username + " authenticated successfully");
                } else {
                    out.println("LOGIN_FAILED");
                    socket.close();
                    return;
                }
            }

            // Handle client messages
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("CHAT_EXIT")) {
                    break;
                } else if (message.startsWith("MSG:")) {
                    String chatMessage = message.substring(4);
                    System.out.println(username + ": " + chatMessage);
                    Server.broadcastMessage(username + ": " + chatMessage, this);
                } else if (message.startsWith("UPLOAD:")) {
                    handleFileUpload(message);
                } else if (message.equals("LIST_FILES")) {
                    sendFileList();
                } else if (message.startsWith("DOWNLOAD:")) {
                    handleFileDownload(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Error with client " + username + ": " + e.getMessage());
        } finally {
            try {
                Server.removeClient(this);
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        out.println("MSG:" + message);
    }

    private void handleFileUpload(String message) throws IOException {
        String[] parts = message.split(":");
        String filename = parts[1];
        long fileSize = Long.parseLong(parts[2]);

        out.println("READY");

        // Create server directory if it doesn't exist
        Path serverDir = Paths.get("resources/Server");
        if (!Files.exists(serverDir)) {
            Files.createDirectories(serverDir);
        }

        Path filePath = serverDir.resolve(filename);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalRead = 0;
            InputStream is = socket.getInputStream();

            while (totalRead < fileSize && (bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
        }
        System.out.println(username + " uploaded file: " + filename);
    }

    private void sendFileList() throws IOException {
        Path serverDir = Paths.get("resources/Server");
        if (Files.exists(serverDir)) {
            Files.list(serverDir)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .forEach(path -> out.println(path.toString()));
        }
        out.println("END_LIST");
    }

    private void handleFileDownload(String message) throws IOException {
        String filename = message.substring(9);
        Path filePath = Paths.get("resources/Server/" + filename);

        if (!Files.exists(filePath)) {
            out.println("FILE_NOT_FOUND");
            return;
        }

        out.println("FILE_FOUND:" + Files.size(filePath));

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             OutputStream os = socket.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
        System.out.println(username + " downloaded file: " + filename);
    }
}
package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientReceiver implements Runnable {
    private BufferedReader inputReader;

    // Constructor that takes a Socket
    public ClientReceiver(Socket socket) throws IOException {
        this.inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // Constructor that takes an InputStream directly
    public ClientReceiver(InputStream inputStream) {
        this.inputReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    public ClientReceiver(BufferedReader in) {
        this.inputReader=in;
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = inputReader.readLine()) != null) {
                // Print the message to CLI
                System.out.println(message);

                // Check for exit condition if needed
                if (message.equalsIgnoreCase("/exit")) {
                    break;
                }
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.out.println("Connection to server lost: " + e.getMessage());
            }
        } finally {
            try {
                if (inputReader != null) {
                    inputReader.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing input stream: " + e.getMessage());
            }
        }
    }
}
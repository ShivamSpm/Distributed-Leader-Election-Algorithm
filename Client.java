import java.io.*;
import java.net.*;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 1234;

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            
            int clientId = input.readInt();
            System.out.println("Client started with ID: " + clientId);
            
            new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        String serverMessage = input.readUTF();
                        System.out.println(serverMessage); // Print messages from server
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed.");
                }
            }).start();


            try {
                while (!socket.isClosed()) {
                    String aliveMessage = "I am alive " + clientId;
                    output.writeUTF(aliveMessage);
                    output.flush();
                    Thread.sleep(3000);
                }
            } catch (InterruptedException e) {
                System.out.println("Client interrupted.");
            }
        }
    }
}

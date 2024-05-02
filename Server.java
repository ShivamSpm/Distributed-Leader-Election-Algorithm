import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
public class Server {
    private static final int PORT = 1234;
    private ServerSocket serverSocket;
    private final ReentrantLock lock = new ReentrantLock();
    private ConcurrentMap<Integer, Socket> clients = new ConcurrentHashMap<>();
    private AtomicInteger clientIdCounter = new AtomicInteger(0);
    private volatile int currentLeaderId = -1;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.startServer();
    }

    public void startServer() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("Server started. Listening on Port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            int clientId = clientIdCounter.getAndIncrement();
            new Thread(new ClientHandler(clientSocket, clientId)).start();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private int clientId;

        public ClientHandler(Socket clientSocket, int clientId) {
            this.clientSocket = clientSocket;
            this.clientId = clientId;
        }

        public void run() {
            try {
                clients.put(clientId, clientSocket);
                System.out.println("Client " + clientId + " connected.");
                DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
                output.writeInt(clientId);

                DataInputStream input = new DataInputStream(clientSocket.getInputStream());

                try {
                    while (true) {
                        String message = input.readUTF();
                        System.out.println(message);
                        if (message.startsWith("I am alive")) {
                            electLeader(clientId);
                        }
                    }

                } catch (EOFException | SocketException e) {
                    System.out.println("Client " + clientId + " has disconnected.");
                    clients.remove(clientId);

                    if(currentLeaderId == clientId){
                        currentLeaderId = -1;
                    }
                    
                    if(clients.isEmpty()){
                        clientIdCounter = new AtomicInteger(0);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("Client " + clientId + " disconnected.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void electLeader(int newClientId) {
        lock.lock();
        try {
            if (newClientId > currentLeaderId) {
                currentLeaderId = newClientId;
                notifyAllClients("New leader is client " + currentLeaderId);
            }
        } finally {
            lock.unlock();
        }
    }

    private void notifyAllClients(String message) {
        for (Socket socket : clients.values()) {
            try {
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                output.writeUTF(message);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


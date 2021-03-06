import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ChatServer {
    public static final int PORT = 54321;
    private static final ArrayList<ClientConnectionData> clientList = new ArrayList<>();
    static File folder = new File(System.getProperty("user.dir")+"/Music");
    static File[] listOfFiles = folder.listFiles();
    static String[] listOfMusicNames;
    

    public static void main(String[] args) throws Exception {
        listOfMusicNames = new String[listOfFiles.length];
        for (int i = 0; i < listOfFiles.length; i++) {
            listOfMusicNames[i] = listOfFiles[i].getName();
        }
        ExecutorService pool = Executors.newFixedThreadPool(100);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Chat Server started.");
            System.out.println("Local IP: "
                    + Inet4Address.getLocalHost().getHostAddress());
            System.out.println("Local Port: " + serverSocket.getLocalPort());
            
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("Connected to %s:%d on local port %d\n",
                        socket.getInetAddress(), socket.getPort(), socket.getLocalPort());
                    
                    // This code should really be done in the separate thread
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    String name = socket.getInetAddress().getHostName();

                    ClientConnectionData client = new ClientConnectionData(socket, in, out, name);
                    

                    //handle client business in another thread
                    pool.execute(new ServerClientListener(client, clientList));
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }

            }
        } 
    }

}

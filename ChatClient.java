import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static PrintWriter out;
    
    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);
        
        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        userInput.nextLine();

        socket = new Socket(serverip, port);
        socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // start a thread to listen for server messages
        ClientServerListener listener = new ClientServerListener(socketIn,socket);
        Thread t = new Thread(listener);
        t.start();

        System.out.println("Starting chat");
    
        String input = userInput.nextLine().trim();  

        while(! input.toLowerCase().startsWith("/quit")) {
            if (listener.state == 0) {
                String msg = String.format("NAME %s", input); 
                out.println(msg);
                input = userInput.nextLine().trim();
            } else if (listener.state == 1) {
                if(input.startsWith("@")){
                    String name = input.substring(1).split(" ")[0];
                    String usermsg = input.substring(name.length()+1).trim();
                    
                    String msg = String.format("PCHAT %s %s",name, usermsg); 
                    out.println(msg);
                    
                } else if(input.startsWith("!")){
                    String msg = String.format("PLAYMUSIC %s",input.substring(1).trim()); 
                    out.println(msg);
                } else if(input.equals("?")){
                    out.println("GETMUSIC");
                }
                else{
                    String msg = String.format("CHAT %s", input); 
                out.println(msg);
                
                }
                input = userInput.nextLine().trim();
                
            }
            
        }
        out.println("QUIT");
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
        
    }
}

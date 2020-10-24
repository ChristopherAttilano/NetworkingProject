import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static ObjectInputStream socketIn;
    private static ObjectOutputStream out;
    
    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);
        
        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        userInput.nextLine();

        socket = new Socket(serverip, port);
        socketIn = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());

        // start a thread to listen for server messages
        ClientServerListener listener = new ClientServerListener(socketIn,socket);
        Thread t = new Thread(listener);
        t.start();

        System.out.println("Starting chat");
    
        String input = userInput.nextLine().trim();
        Message msg = new Message("","");
        while(! input.toLowerCase().startsWith("/quit")) {
            if (listener.state == 0) {
                // String msg = String.format("NAME %s", input); 
                msg = new Message(msg.SubmitNameHeader, input.trim());
                out.writeObject(msg);
            } else if (listener.state == 1) {
                if(input.startsWith("@")){
                    String[] temp = input.split(" ");
                    ArrayList<String> recipients = new ArrayList<String>();
                    int index = 0;
                    for (int i = 0; i < temp.length; i++) {
                        if(temp[i].startsWith("@"))
                            recipients.add(temp[i].substring(1));
                        else{
                            index = input.indexOf(temp[i]);
                            break;
                        }
                    }
                    msg = new Message(msg.PChatHeader, input.substring(index).trim());
                    msg.setRecipients(recipients);
                    out.writeObject(msg);
                    
                } else if(input.startsWith("!")){
                    msg = new Message(msg.PlayMusicHeader, input.substring(1).trim());
                    out.writeObject(msg);
                } else if(input.equals("?")){
                    msg = new Message(msg.GetMusicHeader, "");
                    out.writeObject(msg);
                }
                else if(input.equals("/whoishere")){
                    System.out.println(listener.peopleInRoom+" are in the chat"); 
                }
                else{
                    msg = new Message(msg.ChatHeader, input);
                    out.writeObject(msg);
                }
            }
            input = userInput.nextLine().trim();
        }
        msg = new Message(msg.QuitHeader, "");
        out.writeObject(msg);
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
        
    }
}

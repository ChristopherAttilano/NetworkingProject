import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;



public class ClientServerListener implements Runnable {
    Socket socket;
    ObjectInputStream  socketIn;
    int state = 0;
    String peopleInRoom = "";
    @Override
    public void run() {
        try {
            Message incoming;
            while(true) {
                incoming = (Message) socketIn.readObject();
                if (incoming.header.equals(incoming.SubmitNameHeader)) {
                    state = 0;
                    System.out.println("Enter your username:");
                }else if (incoming.header.equals(incoming.WelcomeHeader)) {
                    if(state == 0)
                        System.out.println(peopleInRoom+" are in the chat");
                    state = 1;
                    String name = incoming.message;
                    System.out.println(name+" has joined"); 
                }else if(incoming.header.equals(incoming.ChatHeader)){
                    String name = incoming.getSender();
                    String msg = incoming.message.trim();
                    System.out.println(name+":"+msg); 
                }else if (incoming.header.equals(incoming.PChatHeader)) {
                    String name = incoming.getSender();
                    String msg = incoming.message.trim();
                    System.out.println(name+"(private):"+msg); 
                }  else if(incoming.header.equals(incoming.PlayMusicHeader)){
                    try {
                        InputStream  audioSrc = new DataInputStream(socket.getInputStream());
                        InputStream bufferedIn = new BufferedInputStream(audioSrc);
                        AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
                        Clip clip = AudioSystem.getClip(); 
                        clip.open(audioStream); 
                        clip.start();
                    }
                    catch(Exception ex){
                        System.out.println("error "+ex);
                    }
                }else if(incoming.header.equals(incoming.GetMusicHeader)){
                    System.out.println(incoming.message.trim()); 
                }else if(incoming.header.equals(incoming.QuitHeader)){
                    System.out.println(incoming.message.trim()+" has left."); 
                }else if(incoming.header.equals(incoming.GetUsersHeader)){
                    peopleInRoom = incoming.message.trim();
                    // System.out.println(incoming.message.trim()+" are in the chat"); 
                }
                //handle different headers
                //WELCOME
                //CHAT
                //EXIT
            }
        } catch (Exception ex) {
            System.out.println("Exception caught in listener - " + ex);
        } finally{
            System.out.println("Client Listener exiting");
        }
    }

	
    public ClientServerListener(ObjectInputStream socketIn, Socket socket) {
        this.socketIn = socketIn;
        this.socket = socket;
    }
}
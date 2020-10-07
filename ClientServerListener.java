import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.Socket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.TargetDataLine;



public class ClientServerListener implements Runnable {

    BufferedReader socketIn;
    Socket socket;
    int state = 0;

    @Override
    public void run() {
        try {
            String incoming = "";

            while( (incoming = socketIn.readLine()) != null) {
                if (incoming.startsWith("SUBMITNAME")) {
                    state = 0;
                    System.out.println("Please enter a username:");
                }else if (incoming.startsWith("WELCOME")) {
                    state = 1;
                    String name = incoming.substring(7).trim();
                    System.out.println("Welcome "+name); 
                }else if(incoming.startsWith("CHAT")){
                    String name = incoming.substring(4).trim().split(" ")[0];
                    String msg = incoming.substring(4).trim().substring(name.toCharArray().length).trim();
                    System.out.println(name+":"+msg); 
                }else if (incoming.startsWith("PCHAT")) {
                    String name = incoming.substring(5).trim().split(" ")[0];
                    String msg = incoming.substring(5).trim().substring(name.toCharArray().length).trim();
                    System.out.println(name+"(private):"+msg); 
                }  else if(incoming.startsWith("PLAYMUSIC")){
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
                }else if(incoming.startsWith("MUSICNAMES")){
                    System.out.println(incoming.substring(10).trim()); 
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

	
    public ClientServerListener(BufferedReader socketIn, Socket socket) {
        this.socketIn = socketIn;
        this.socket = socket;
	}
}
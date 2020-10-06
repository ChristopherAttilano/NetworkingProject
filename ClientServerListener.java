package day5_bca;

import java.io.BufferedReader;
public class ClientServerListener implements Runnable {

    BufferedReader socketIn;
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

	public ClientServerListener(BufferedReader socketIn) {
        this.socketIn = socketIn;
	}
}
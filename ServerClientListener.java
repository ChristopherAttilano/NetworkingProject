package day5_bca;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

public class ServerClientListener implements Runnable {

    // Maintain data about the client serviced by this thread
    ClientConnectionData client;
    ArrayList<ClientConnectionData> clientList;

    public ServerClientListener(ClientConnectionData client, ArrayList<ClientConnectionData> clientList) {
        this.client = client;
        this.clientList = clientList;
    }

    /**
     * Broadcasts a message to all clients connected to the server.
     */
    public void broadcast(String msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList) {
                    c.getOut().println(msg);
                    // c.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }

    }

    public void broadcast(String msg, ClientConnectionData client) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList) {
                    if (!c.equals(client))
                        c.getOut().println(msg);
                    // c.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    public boolean isUsernameValid(String username) {
        for (ClientConnectionData c : clientList) {
            String temp = "NAME "+c.getUserName();
            if (c.getUserName() != null && temp.equals(username))
                return false;
        }
        return username.matches("NAME [a-zA-Z]+");
    }

    @Override
    public void run() {
        try {
            BufferedReader in = client.getInput();
            // get userName, first message from user
            client.getOut().println("SUBMITNAME");
            String username = in.readLine().trim();
            while (!isUsernameValid(username)) {
                client.getOut().println("SUBMITNAME");
                username = in.readLine().trim();
            }
            client.setUserName(username.substring(5));

            synchronized (clientList) {
                clientList.add(client);
            }

            System.out.println("added client " + client.getName());

            // notify all that client has joined
            broadcast(String.format("WELCOME %s", client.getUserName()));

            String incoming = "";

            while ((incoming = in.readLine()) != null) {
                if (incoming.startsWith("QUIT")) {
                    break;
                } else if (incoming.length() > 0) {
                    String msg = String.format("%s:%s", client.getUserName(), incoming);
                    broadcast(msg, client);
                }
            }
        } catch (Exception ex) {
            if (ex instanceof SocketException) {
                System.out.println("Caught socket ex for " + client.getName());
            } else {
                System.out.println(ex);
                ex.printStackTrace();
            }
        } finally {
            // Remove client from clientList, notify all
            synchronized (clientList) {
                clientList.remove(client);
            }
            System.out.println(client.getName() + " has left.");
            broadcast(String.format("EXIT %s", client.getUserName()));
            try {
                client.getSocket().close();
            } catch (IOException ex) {
            }

        }
    }

}

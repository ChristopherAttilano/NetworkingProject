import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

public class ServerClientListener implements Runnable {

    // Maintain data about the client serviced by this thread
    ClientConnectionData client;
    ArrayList<ClientConnectionData> clientList;
    File folder = new File(System.getProperty("user.dir") + "/Music");
    File[] listOfFiles = folder.listFiles();
    String[] listOfMusicNames;

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

    public void broadcast(String msg, String username) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList) {
                    if (c.getUserName().equals(username))
                        c.getOut().println(msg);
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }

    }

    public boolean isUsernameValid(String username) {
        for (ClientConnectionData c : clientList) {
            String temp = "NAME " + c.getUserName();
            if (c.getUserName() != null && temp.equals(username))
                return false;
        }
        return username.matches("NAME [a-zA-Z]+");
    }

    public void playSong(ClientConnectionData client, String song) {
        if (!Arrays.asList(listOfMusicNames).contains(song)) {
            System.out.println("song");
            System.out.println(song);
            return;
        }
        System.out.println("here");
        try {
            File mousicFile = new File(System.getProperty("user.dir") + "/Music/" + song);
            InputStream in = new FileInputStream(mousicFile);
            DataOutputStream out = new DataOutputStream(client.getSocket().getOutputStream());
            byte[] data = new byte[4096];
            int count;
            client.getOut().println("PLAYMUSIC");
            while ((count = in.read(data)) != -1) {
                out.write(data, 0, count);
            }
            in.close();
        } catch (Exception ex) {

        }
    }

    @Override
    public void run() {
        try {
            listOfMusicNames = new String[listOfFiles.length];
            for (int i = 0; i < listOfFiles.length; i++) {
                listOfMusicNames[i] = listOfFiles[i].getName();
            }
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

            while ((incoming = in.readLine().trim()) != null) {
                if (incoming.startsWith("QUIT")) {
                    break;
                } else if (incoming.startsWith("CHAT") && incoming.length() > 5) {
                    String msg = String.format("CHAT %s %s", client.getUserName(), incoming.substring(4).trim());
                    broadcast(msg, client);
                } else if (incoming.startsWith("PCHAT") && incoming.length() > 6) {
                    String name = incoming.substring(5).trim().split(" ")[0];
                    String incomingmsg = incoming.substring(6).trim().substring(name.length()).trim();
                    String msg = String.format("PCHAT %s %s", client.getUserName(), incomingmsg);
                    broadcast(msg, name);
                } else if (incoming.startsWith("PLAYMUSIC") && incoming.length() > 6) {
                    playSong(client, incoming.substring(9).trim());
                } else if (incoming.startsWith("GETMUSIC")) {
                    broadcast("MUSICNAMES "+String.join(",", listOfMusicNames), client.getUserName());
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

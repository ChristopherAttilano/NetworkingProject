import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
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
    public void broadcast(Message msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList) {
                    c.getOut().writeObject(msg);
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }

    }

    public void broadcast(Message msg, ClientConnectionData client) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList) {
                    if (!c.equals(client))
                        c.getOut().writeObject(msg);
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }
    
    public void broadcastToOne(Message msg, ClientConnectionData client) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList) {
                    if (c.equals(client))
                        c.getOut().writeObject(msg);
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }
    
    

    public void broadcast(Message msg, String username) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList) {
                    if (c.getUserName().equals(username))
                        c.getOut().writeObject(msg);
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }

    }
    private void broadcast(Message msg, ArrayList<String> recipients) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList) {
                    if (recipients.contains(c.getUserName()))
                        c.getOut().writeObject(msg);
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    public boolean isUsernameValid(String username) {
        for (ClientConnectionData c : clientList) {
            String temp = c.getUserName();
            if (c.getUserName() != null && temp.equals(username))
                return false;
        }
        return username.matches("[a-zA-Z]+");
    }

    public void playSong(ClientConnectionData client, String song) {
        if (!Arrays.asList(listOfMusicNames).contains(song)) {
            System.out.println("song");
            System.out.println(song);
            return;
        }
        try {
            File mousicFile = new File(System.getProperty("user.dir") + "/Music/" + song);
            InputStream in = new FileInputStream(mousicFile);
            DataOutputStream out = new DataOutputStream(client.getSocket().getOutputStream());
            byte[] data = new byte[4096];
            int count;
            Message msg = new Message("","");
            msg.setHeader(msg.PlayMusicHeader);

            client.getOut().writeObject(msg);
            while ((count = in.read(data)) != -1) {
                out.write(data, 0, count);
            }
            in.close();
        } catch (Exception ex) {

        }
    }
    private String GetUsers() {
        String temp = "";
        for (int i = 0; i < clientList.size(); i++) {
            temp+=clientList.get(i).getUserName()+", ";   
        }
        return temp;
    }
    @Override
    public void run() {
        try {
            synchronized (clientList) {
                clientList.add(client);
            }
            listOfMusicNames = new String[listOfFiles.length];
            for (int i = 0; i < listOfFiles.length; i++) {
                listOfMusicNames[i] = listOfFiles[i].getName();
            }
            ObjectInputStream in = client.getInput();
            Message incoming = new Message("","");

            broadcastToOne(new Message(incoming.SubmitNameHeader,""), client);

            Message username = (Message) in.readObject();
            while (!isUsernameValid(username.message)) {
                System.out.println(username.message);
                broadcastToOne(new Message(incoming.SubmitNameHeader,""), client);
                username = (Message) in.readObject();
            }
            
            client.setUserName(username.message)    ;

        
            System.out.println("added client " + client.getName());

            // notify all that client has joined
            
            broadcast(new Message(incoming.GetUsersHeader,GetUsers()));

            broadcast(new Message(incoming.WelcomeHeader,client.getUserName()));
            
            while (true) {
                incoming = (Message) in.readObject();
                if (incoming.getHeader().equals(incoming.QuitHeader)) {
                    broadcast(new Message(incoming.GetUsersHeader,GetUsers()));
                    break;
                } else if (incoming.getHeader().equals(incoming.ChatHeader )&& incoming.message.length() > 0) {
                    Message msg= new Message(incoming.ChatHeader,incoming.message);
                    msg.setSender(client.getUserName());
                    // String msg = String.format("CHAT %s %s", client.getUserName(), incoming.substring(4).trim());
                    broadcast(msg, client);
                } else if (incoming.getHeader().equals(incoming.PChatHeader ) && incoming.message.length() > 0) {
                    Message msg= new Message(incoming.PChatHeader,incoming.message);
                    msg.setRecipients(incoming.getRecipients());
                    msg.setSender(client.getUserName());
                    broadcast(msg, incoming.getRecipients());
                } else if (incoming.getHeader().equals(incoming.PlayMusicHeader)) {
                    playSong(client, incoming.message.trim());
                } else if (incoming.getHeader().equals(incoming.GetMusicHeader)) {
                    Message msg= new Message(incoming.GetMusicHeader,String.join(",", listOfMusicNames));
                    broadcast(msg, client.getUserName());
                }else if (incoming.getHeader().equals(incoming.GetUsersHeader)) {
                    broadcast(new Message(incoming.GetUsersHeader,GetUsers()), client.getUserName());
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
            synchronized (clientList) {
                clientList.remove(client);
            }
            System.out.println(client.getName() + " has left.");
            Message incoming = new Message("","");
            broadcast(new Message(incoming.QuitHeader,client.getUserName()));
            broadcast(new Message(incoming.GetUsersHeader,GetUsers()));
            // broadcast(String.format("EXIT %s", client.getUserName()));
            try {
                client.getSocket().close();
            } catch (IOException ex) {
            }

        }
    }

    

    

}

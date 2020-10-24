import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable{
    public static final long serialVersionUID = 1L;
    public String header;
    public final String ChatHeader = "ChatHeader";
    public final String PChatHeader = "PChatHeader";
    public final String QuitHeader = "QuitHeader";
    public final String SubmitNameHeader = "SubmitNameHeader";
    public final String WelcomeHeader = "WelcomeHeader";
    public final String PlayMusicHeader = "PlayMusicHeader";
    public final String GetMusicHeader = "GetMusicHeader";
    public final String GetUsersHeader = "GetUsersHeader";
    
    

    public String message;
    private String sender;
    private ArrayList<String> recipients;

    public Message(String header, String message) {
        this.message = message;
        this.header = header;
    }

    public void setHeader(String header){
        this.header = header;
    }

    public String getHeader() {
        return header;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    public void setRecipients(ArrayList<String> recipients) {
        this.recipients = recipients;
    }
    public ArrayList<String> getRecipients() {
        return recipients;
    }
    public void setSender(String sender) {
        this.sender = sender;
    } 
    public String getSender() {
        return sender;
    }
}
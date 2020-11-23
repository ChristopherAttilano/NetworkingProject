import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
/**
 * For Java 8, javafx is installed with the JRE. You can run this program normally.
 * For Java 9+, you must install JavaFX separately: https://openjfx.io/openjfx-docs/
 * If you set up an environment variable called PATH_TO_FX where JavaFX is installed
 * you can compile this program with:
 *  Mac/Linux:
 *      > javac --module-path $PATH_TO_FX --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *  Windows CMD:
 *      > javac --module-path %PATH_TO_FX% --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *  Windows Powershell:
 *      > javac --module-path $env:PATH_TO_FX --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *
 * Then, run with:
 *
 *  Mac/Linux:
 *      > java --module-path $PATH_TO_FX --add-modules javafx.controls day10_chatgui.ChatGuiClient
 *  Windows CMD:
 *      > java --module-path %PATH_TO_FX% --add-modules javafx.controls day10_chatgui.ChatGuiClient
 *  Windows Powershell:
 *      > java --module-path $env:PATH_TO_FX --add-modules javafx.controls day10_chatgui.ChatGuiClient
 *
 * There are ways to add JavaFX to your to your IDE so the compile and run process is streamlined.
 * That process is a little messy for VSCode; it is easiest to do it via the command line there.
 * However, you should open  Explorer -> Java Projects and add to Referenced Libraries the javafx .jar files
 * to have the syntax coloring and autocomplete work for JavaFX
 */

class ServerInfo {
    public final String serverAddress;
    public final int serverPort;

    public ServerInfo(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
}

public class ChatGuiClient extends Application {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    private Stage stage;
    private TextArea messageArea;
    private TextArea participantsArea;
    private TextArea musicArea;
    private TextField textInput;
    private Button sendButton;

    private ServerInfo serverInfo;
    //volatile keyword makes individual reads/writes of the variable atomic
    // Since username is accessed from multiple threads, atomicity is important
    private volatile String username = "";
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        //If ip and port provided as command line arguments, use them
        List<String> args = getParameters().getUnnamed();
        if (args.size() == 2){
            this.serverInfo = new ServerInfo(args.get(0), Integer.parseInt(args.get(1)));
        }
        else {
            //otherwise, use a Dialog.
            Optional<ServerInfo> info = getServerIpAndPort();
            if (info.isPresent()) {
                this.serverInfo = info.get();
            }
            else{
                Platform.exit();
                return;
            }
        }

        this.stage = primaryStage;
        BorderPane borderPane = new BorderPane();

        messageArea = new TextArea();
        messageArea.setWrapText(true);
        messageArea.setEditable(false);
        borderPane.setRight(messageArea);

        participantsArea = new TextArea();
        participantsArea.setWrapText(true);
        participantsArea.setEditable(false);
        participantsArea.setPrefWidth(150);
        borderPane.setCenter(participantsArea);
        participantsArea.setText("Participants:\n");

        musicArea = new TextArea();
        musicArea.setWrapText(true);
        musicArea.setEditable(false);
        musicArea.setPrefHeight(100);
        borderPane.setTop(musicArea);
        musicArea.setText("Played Song:\nN/A");

        //At first, can't send messages - wait for WELCOME!
        textInput = new TextField();
        textInput.setEditable(false);
        textInput.setOnAction(e -> sendMessage());
        sendButton = new Button("Send");
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> sendMessage());

        HBox hbox = new HBox();
        hbox.getChildren().addAll(new Label("Message: "), textInput, sendButton);
        HBox.setHgrow(textInput, Priority.ALWAYS);
        borderPane.setBottom(hbox);

        Scene scene = new Scene(borderPane, 700, 500);
        stage.setTitle("Chat Client");
        stage.setScene(scene);
        stage.show();

        ServerListener socketListener = new ServerListener();

        //Handle GUI closed event
        stage.setOnCloseRequest(e -> {
            try {
                outputStream.writeObject(new Message("QuitHeader", ""));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            socketListener.appRunning = false;
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        new Thread(socketListener).start();
    }

    private void sendMessage() {
        String message = textInput.getText().trim();
        Message msg;
        boolean privateMessage = false;

        if (message.length() == 0)
            return;

        textInput.clear();
        if(message.startsWith("@")){
            String[] temp = message.split(" ");
            ArrayList<String> recipients = new ArrayList<String>();
            int index = 0;
            for (int i = 0; i < temp.length; i++) {
                if(temp[i].startsWith("@")) {
                    recipients.add(temp[i].substring(1));
                    index += temp[i].length();
                } else{
                    break;
                }
            }
            msg = new Message("PChatHeader", message.substring(index).trim());
            msg.setRecipients(recipients);
            privateMessage = true;
        }  else if(message.startsWith("!")){
            msg = new Message("PlayMusicHeader", message.substring(1).trim());
            musicArea.setText("Played song:\n");
            musicArea.appendText(message.substring(1).trim());
        } else if(message.equals("?")){
            msg = new Message("GetMusicHeader", "");
        } else {
            msg = new Message("ChatHeader", message);
        }
        try {
            outputStream.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (privateMessage) {
            messageArea.appendText(username + " (Private): " + message + "\n");
        } else {
            messageArea.appendText(username + ": " + message + "\n");
        }
    }

    private Optional<ServerInfo> getServerIpAndPort() {
        // In a more polished product, we probably would have the ip /port hardcoded
        // But this a great way to demonstrate making a custom dialog
        // Based on Custom Login Dialog from https://code.makery.ch/blog/javafx-dialogs-official/

        // Create a custom dialog for server ip / port
        Dialog<ServerInfo> getServerDialog = new Dialog<>();
        getServerDialog.setTitle("Enter Server Info");
        getServerDialog.setHeaderText("Enter your server's IP address and port: ");

        // Set the button types.
        ButtonType connectButtonType = new ButtonType("Connect", ButtonData.OK_DONE);
        getServerDialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Create the ip and port labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField ipAddress = new TextField();
        ipAddress.setPromptText("e.g. localhost, 127.0.0.1");
        grid.add(new Label("IP Address:"), 0, 0);
        grid.add(ipAddress, 1, 0);

        TextField port = new TextField();
        port.setPromptText("e.g. 54321");
        grid.add(new Label("Port number:"), 0, 1);
        grid.add(port, 1, 1);


        // Enable/Disable connect button depending on whether a address/port was entered.
        Node connectButton = getServerDialog.getDialogPane().lookupButton(connectButtonType);
        connectButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        ipAddress.textProperty().addListener((observable, oldValue, newValue) -> {
            connectButton.setDisable(newValue.trim().isEmpty());
        });

        port.textProperty().addListener((observable, oldValue, newValue) -> {
            // Only allow numeric values
            if (! newValue.matches("\\d*"))
                port.setText(newValue.replaceAll("[^\\d]", ""));

            connectButton.setDisable(newValue.trim().isEmpty());
        });

        getServerDialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(() -> ipAddress.requestFocus());


        // Convert the result to a ServerInfo object when the login button is clicked.
        getServerDialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new ServerInfo(ipAddress.getText(), Integer.parseInt(port.getText()));
            }
            return null;
        });

        return getServerDialog.showAndWait();
    }

    private String getName(){
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Enter Chat Name");
        nameDialog.setHeaderText("Please enter your username.");
        nameDialog.setContentText("Name: ");

        while(username.equals("")) {
            Optional<String> name = nameDialog.showAndWait();
            if (!name.isPresent() || name.get().trim().equals(""))
                nameDialog.setHeaderText("You must enter a nonempty name: ");
            else if (name.get().trim().contains(" "))
                nameDialog.setHeaderText("The name must have no spaces: ");
            else
                username = name.get().trim();
        }
        return username;
    }

    class ServerListener implements Runnable {

        volatile boolean appRunning = false;

        public void run() {
            try {
                // Set up the socket for the Gui
                socket = new Socket(serverInfo.serverAddress, serverInfo.serverPort);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                inputStream = new ObjectInputStream(socket.getInputStream());
                outputStream = new ObjectOutputStream(socket.getOutputStream());

                appRunning = true;
                //Ask the gui to show the username dialog and update username
                //Send to the server
                Platform.runLater(() -> {
                    String name = getName();
                    Message nameMsg = new Message("SumitNameHeader", name);
                    try {
                        outputStream.writeObject(nameMsg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                //handle all kinds of incoming messages
                Message incoming;
                Message getUsers = new Message("GetUsersHeader", "");
                while (appRunning && (incoming = (Message) inputStream.readObject())!=null) {

                    if (incoming.header.equals(incoming.WelcomeHeader)) {
                        String user = incoming.getMessage();
                        //got welcomed? Now you can send messages!
                        if (user.equals(username)) {
                            Platform.runLater(() -> {
                                stage.setTitle("Chatter - " + username);
                                textInput.setEditable(true);
                                sendButton.setDisable(false);
                                messageArea.appendText("Welcome to the chatroom, " + username + "!\n");

                            });
                        }
                        else {
                            Platform.runLater(() -> {
                                messageArea.appendText(user + " has joined the chatroom.\n");
                            });
                        }
                        outputStream.writeObject(getUsers);

                    } else if (incoming.getHeader().equals(incoming.ChatHeader)) {
                        String user = incoming.getSender();
                        String msg = incoming.getMessage();

                        Platform.runLater(() -> {
                            messageArea.appendText(user + ": " + msg + "\n");
                        });
                    } else if (incoming.getHeader().equals(incoming.PChatHeader)) {
                        String user = incoming.getSender();
                        String msg = incoming.getMessage();
                        if (!user.equals(username)) {
                            Platform.runLater(() -> {
                                messageArea.appendText(user + " (Private): " + msg + "\n");
                            });
                        }
                    } else if (incoming.getHeader().equals(incoming.QuitHeader)) {
                        String user = incoming.getSender();
                        Platform.runLater(() -> {
                            messageArea.appendText(user + "has left the chatroom.\n");
                        });
                        outputStream.writeObject(getUsers);
                    } else if (incoming.getHeader().equals(incoming.GetUsersHeader)) {
                        participantsArea.setText("Participants:\n");
                        String[] users = incoming.getMessage().split(", ");
                        for (int i=0; i<users.length; i++) {
                            participantsArea.appendText(users[i]);
                        }
                    } else if (incoming.getHeader().equals(incoming.GetMusicHeader)) {
                        messageArea.appendText("Available music: " + incoming.getMessage());
                    }
                    Thread.sleep(100);
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (Exception e) {
                if (appRunning)
                    e.printStackTrace();
            }
            finally {
                Platform.runLater(() -> {
                    stage.close();
                });
                try {
                    outputStream.writeObject(new Message("QuitHeader", ""));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (socket != null)
                        socket.close();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
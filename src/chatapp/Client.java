/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chatapp;
import java.io.*;
import java.net.*;

/**
 * Module name: Chat Application's Client
 * File name: Client.java
 *
 * <p>Summary: </p>
 * @author mrtodd
 */
public class Client {
    private final int START = 0;
    private final int RECEIVE = 1;
    private final int DISCONNECT = 3;

    /* Sort of want to remove these... */
    private final String ENDCHAT = "/log";
    private final String PING = "/ping";
    private final String CHANGESERVER = "/server";


    private BufferedReader inFromUser;
    private BufferedReader inFromServer;
    private Socket mySocket;
    private String serverIP = "localhost";
    private DataOutputStream outToClient;
    private int port;

    private String mainServerIP;
    private int mainServerPort;


    public Client(int newPort, String newServer){
        if(inFromUser == null)
            inFromUser = new BufferedReader( new InputStreamReader(System.in));
        mainServerPort = newPort;
        mainServerIP = newServer;
        port = newPort;
        serverIP = newServer;
    }
    
    /* Plan to deprecated */
    public Client(BufferedReader input) throws IOException{
        String choice;
        int portInt = 0;
        if(input == null)
            inFromUser = new BufferedReader( new InputStreamReader(System.in));
        else
            inFromUser = input;
        System.out.print("What port: ");
        choice = inFromUser.readLine();
        boolean test = false;
        while(!test){
            try{
                portInt = Integer.parseInt(choice);
                test = true;
            }
            catch (NumberFormatException e){
                System.out.println("That is not a valid port");
            }
        }
        System.out.print("Server Address: ");
        String server = inFromUser.readLine();
        System.out.println();
        serverIP = server;
        mainServerIP = server;
        mainServerPort = portInt;
        port = portInt;
    }

    
    public int sendMessage(String message) throws IOException{
        int ret = RECEIVE;
        if((message != null) && message.length() > 0){
            if(message.equals(ENDCHAT)){
                ret = DISCONNECT;
                outToClient.writeBytes(message + '\n');
            }
            else if(message.startsWith(CHANGESERVER)){
                String[] split = message.split(" ");
                if(split.length == 3){
                    try{
                        mainServerPort = Integer.parseInt(split[2]);
                        mainServerIP = split[1];
                        ret = DISCONNECT;
                    }
                    catch (NumberFormatException e){
                        System.out.println("Try: " + CHANGESERVER + " <server> [port]");
                    }
                }
                else if(split.length == 2){
                    mainServerIP = split[1];
                    ret = DISCONNECT;
                }
                else
                    System.out.println("Try: " + CHANGESERVER + " <server> [port]");
            }
            else
                outToClient.writeBytes(message + '\n');
        }
        return ret;
    }
    
    /* returns whether done or not */
    public boolean chat(){
        boolean done = false;
        System.out.println("Main Server Running on Port: " + port);
        System.out.println("Client command: " + CHANGESERVER + " <server address> [port]");
        String clientSentence;
        String message = "";
        int count = 1000000;
        int switchInt = 0;

        Boolean test = true;
        while(test){
            count--;
            switch(switchInt){
                case START:
                try{
                    setupSockets();
                    switchInt = RECEIVE;
                }catch(IOException e) {
                    closeConnections();
                    test = false;
                }
                break;
                case RECEIVE:
                    try{
                        if(count == 0){
                            test = ping();
                            count = 1000000;
                        }
                        else{
                            if(inFromServer.ready()){
                                clientSentence = inFromServer.readLine();
                                if((clientSentence != null) && clientSentence.length() > 0){
                                    if(clientSentence.equals(ENDCHAT)){
                                        switchInt = DISCONNECT;
                                        System.out.println(clientSentence);
                                    }
                                    else if(clientSentence.equals(PING));
                                    else
                                        System.out.println(clientSentence);
                                }
                            }
                            if(inFromUser.ready()){
                                message = inFromUser.readLine();
                                sendMessage(message);

                            }
                        }
                    }
                    catch (IOException e){
                        closeConnections();
                        test = false;
                    }
                break;
                case DISCONNECT:
                    closeConnections();
                    //test = false;
                    if(serverIP.equals(mainServerIP) && mainServerPort == port)
                        test = false;
                    else{
                        serverIP = mainServerIP;
                        port = mainServerPort;
                        switchInt = START;
                    }
                break;

            }
        }
        return true;
    }

    private void setupSockets() throws IOException{
        mySocket = new Socket(serverIP, port);
        outToClient = new DataOutputStream(mySocket.getOutputStream());
    }
    
    private void closeConnections() {
        try{
            if(mySocket != null){
                mySocket.close();
                mySocket = null;
            }
        }
        catch(IOException e){
            mySocket = null;
        }
        try{
            if(inFromServer != null){
                inFromServer.close();
                inFromServer = null;
            }
        }
        catch(IOException e){
            inFromServer = null;
        }
        try{
            if(outToClient != null){
                outToClient.close();
                outToClient = null;
            }
        }
        catch(IOException E){
            outToClient = null;
        }
        System.out.println("Chat has ended.");

    }
    
    //CORRECT WITH EMPTY STRING PING
    private boolean ping(){
        boolean ret = true;
        try{
            outToClient.writeBytes(PING + '\n');
        }
        catch(IOException e){
            closeConnections();
            ret = false;
        }
        return ret;
    }
}


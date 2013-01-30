/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chatapp;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Module Name: Chat Application's Main Server
 * File Name: MainServer.java
 *
 * <p>Summary: This is the server that handles incoming new clients. Before sending
 * them to appropriate servers.</p>
 * @author Mark Todd
 */


public class MainServer {
    static int PORTINT;
    private int cCount; //client count 
    static String CHANGENAME = "/name";
    static String CONNECTED = "/online";
    static String DIRECTCHAT = "/chatwith"; //NOT IMPLEMENTED
    static String WHISPER = "/whisper";
    static String COMMANDS = "/commands";
    static String ENDCHAT = "/log";
    static String PING = "/ping"; //replace with empty. Have clients and servers ignore
    static ArrayList<String> clients;
    static HashMap<String, Socket> clientConnect;

    static ServerSocket serverSocket;
    static ArrayList<Announcement> announcements, serverMessages;


    static boolean SHUTDOWN;

    MainServer(int port, String serverName){
        clientConnect = new HashMap<String,Socket>();
        cCount = 0;
        PORTINT = port;
        SHUTDOWN = false;
        serverMessages = new ArrayList<Announcement>();
        announcements = new ArrayList<Announcement>();

        clients = new ArrayList<String>();
        try{
            serverSocket = new ServerSocket(port);
        }
        catch (IOException e){
            System.out.println("Error in server creation: " + e);
            System.exit(1);
        }
        serverMessages.add(new Announcement("" +
                "Welcome to Server: " + serverName + ".", "SERVER"));
        serverMessages.add(new Announcement("" +
                "Check commands by typing '/commands'", "SERVER"));

        threads();
    }

    void addServerMessage(String newMessage){
        serverMessages.add(new Announcement(newMessage, "SERVER"));
    }


    private void threads(){
        while(!SHUTDOWN){
            Socket newS;
            try{
                newS = serverSocket.accept();
                String t = ("anonymous#" + Integer.toString(cCount));
                clients.add(t);
                clientConnect.put(t.toLowerCase(), newS);
                cCount++;
                Thread nextThread = new ThreadMaintain(newS, t);
                nextThread.start();
            } catch (IOException e){
                System.out.println("Error accepting new client: " + e);
                System.exit(1);
            }
        }
    }

}
class Announcement {
    private ArrayList<String> allClients;
    private String senderName, originalName, announce;
    private Boolean isWhisper = false;

    Announcement(String message, String clientName){
        senderName = clientName;
        originalName = clientName;
        announce = message;
        allClients = (ArrayList<String>)MainServer.clients.clone();
    }

    Announcement(String message, String whisperSend, String whisperReceive){
        isWhisper = true;
        allClients = new ArrayList<String>();
        allClients.add(whisperReceive);
        announce = message;
        senderName = whisperSend;
        originalName = whisperSend;
    }

    boolean contains(String name){
        return allClients.contains(name);
    }

    String remove(String name){
        String ret;
        allClients.remove(name);
        if(isWhisper)
            ret = " whispered " + announce;
        else
            ret = ": " + announce;
        if(senderName.equals(name))
            ret = "You" + ret;
        else{
            if(!originalName.equals(senderName))
                ret = "(was " + originalName +")" + ret;
            ret = senderName + ret;
        }
        return ret;
    }

    void addName(String name){
        if(!isWhisper)
            allClients.add(name);
    }

    void changeName(String oldN, String newN){
        if(allClients.contains(oldN)){
            allClients.remove(oldN);
            allClients.add(newN);
        }
        if(senderName.equals(oldN))
            senderName = newN;
    }



}

class ThreadMaintain extends Thread {
    private Socket aSocket;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    private boolean end;
    private ArrayList<String> reports;
    private int timer = 10000000;
    private String clientSentence, clientName;
    
    /*Temp Holders */
    private String tempString, tempString1;
    Announcement temp;

    ThreadMaintain(Socket newS, String name) {
        clientName = name;
        aSocket = newS;
        reports = new ArrayList<String>();
        end = false;
        try{
            inFromClient =
                new BufferedReader(new InputStreamReader(aSocket.getInputStream()));
            outToClient = new DataOutputStream(aSocket.getOutputStream());
            System.out.println("Received connection from: " + aSocket.getInetAddress().toString());
            addToAnnounceList(name);
            MainServer.announcements.add(new Announcement(clientName + " is now connected.", "SERVER"));
        }catch (IOException e){
            System.out.println("Error in creating streams: " + e);
            closeConnections();
        }
    }

    @Override
    public void run() {
        String[] tempStringArray;
        boolean first = true;
        while(!MainServer.SHUTDOWN && !end){
            timer--;
            /* Handles chat from other users */

            allToReport();
            if(first){
                try{
                    sendClientUsers();
                }catch (IOException e){
                    closeConnections();
                }
                first = false;
            }

            try{
                if(timer == 0){
                    end = ping();
                    timer = 10000000;
//                    System.out.println("PINGED");
                }
                else{
                    if(inFromClient.ready()){
                        clientSentence = inFromClient.readLine();
                        if((clientSentence != null) && clientSentence.length() > 0){
                            if(clientSentence.equals(MainServer.ENDCHAT)){
                                System.out.println("Received: " + clientSentence);
                                closeConnections();
                            }
                            else if(clientSentence.equals(MainServer.PING));
                            else if(clientSentence.startsWith(MainServer.CHANGENAME)){
                                if(clientSentence.length() > (MainServer.CHANGENAME.length() + 1)){
                                    tempString = clientSentence.substring(MainServer.CHANGENAME.length() + 1);
                                    tempStringArray = tempString.split(" |\n|\t|\r");
                                    tempString = tempStringArray[0];
                                    if(MainServer.clientConnect.containsKey(tempString.toLowerCase()) || tempString.equals("You")){
                                        outToClient.writeBytes("That name is already on server.\n");
                                    }
                                    else if(tempString.length() > 0){
                                        changeAnnouncementsName(clientName, tempString);
                                        MainServer.clients.remove(clientName);
                                        Socket tS = MainServer.clientConnect.remove(clientName.toLowerCase());
                                        tempString1 = clientName + " changed name to: " + tempString;
                                        clientName = tempString;
                                        MainServer.clientConnect.put(clientName.toLowerCase(), tS);
                                        MainServer.clients.add(clientName);
                                        System.out.println(tempString1);
                                        MainServer.announcements.add(new Announcement(tempString1, "SERVER"));
                                        outToClient.writeBytes("You changed your name to: " + clientName + "\n");
                                    }
                                }
                                else
                                    outToClient.writeBytes("Please include name to change to. \n");
                            }
                            else if(clientSentence.startsWith(MainServer.COMMANDS)){
                                    tempString = "Current available commands: \n";
                                    tempString += "/online| Announces current online members.\n";
                                    tempString += "/name <new name>| changes name in chat.\n";
                                    tempString += "/whisper <user> <message>| sends user private message.\n";
                                    outToClient.writeBytes(tempString);
                            }
                            /*  NOT IMPLEMENTED
                             *else if(clientSentence.startsWith(MainServer.DIRECTCHAT)){
                                if(clientSentence.length() > (MainServer.DIRECTCHAT.length() + 1)){
                                    tempString = clientSentence.substring(MainServer.DIRECTCHAT.length() + 1);
                                    connectClientToClient(tempString);
                                }
                            }*/
                            else if(clientSentence.startsWith(MainServer.WHISPER)){
                                if(clientSentence.length() > (MainServer.WHISPER.length() + 1)){
                                    tempString = clientSentence.substring(MainServer.WHISPER.length() + 1);
                                    whisperClient(tempString);
                                }
                            }
                            else if(clientSentence.startsWith(MainServer.CONNECTED)){
                                sendClientUsers();
                            }
                            else{
                                System.out.println(clientName + ": " + clientSentence);
                                MainServer.announcements.add(new Announcement(clientSentence, clientName));
                            }
                        }
                    }
                    while(reports.size() > 0){
                        int i = 0;
                        outToClient.writeBytes(reports.get(i) + '\n');
                        reports.remove(i);
                    }
                }
            }
            catch (IOException e){
                closeConnections();
            }
        }

    }

//    private void connectClientToClient(String otherClient) throws IOException{
//        System.out.println("What is the string: " + tempString);
//        if(MainServer.clientConnect.containsKey(tempString) && !(tempString.equals(clientName))){
//            Socket otherSocket = MainServer.clientConnect.get(otherClient);
//            outToClient.writeBytes("Connecting to: " + otherClient + "\n");
//            outToClient.writeBytes("*connect* " + otherSocket)
//        }
//    }

    private void whisperClient(String wholeString) throws IOException{
        String[] split = wholeString.split(" ");
        String otherClient = split[0];
        String message = "";
        if(MainServer.clientConnect.containsKey(otherClient.toLowerCase()) && !(otherClient.equals(clientName))){
            for(int i = 1; i < split.length; i++){
                message += " " + split[i];
            }
            MainServer.announcements.add(new Announcement(message, clientName, otherClient));
        }
        else{
            outToClient.writeBytes("Client: " + otherClient + " not connected.\n");
        }

    }

    private void sendClientUsers() throws IOException{
        String output = "Currently connected:";
        ArrayList clients = MainServer.clients;
        for(int i = 0; i < clients.size(); i++){
            if(i%2 == 0)
                output = output + "\n" + clients.get(i);
            else
                output = output + " " + clients.get(i);
        }
        outToClient.writeBytes(output + '\n');
    }

    // NOT YET USED (for server - to - server and logging)
    /*private void printAllAnnouncements(ArrayList<Announcement> t){

        int i = 0;
        while (i < t.size()){
            System.out.println("Next: " + t.get(i).announce);
            i++;
        }
    }*/
    private void allToReport(){
        int i = 0;
        while(i < MainServer.serverMessages.size()){
            if(MainServer.serverMessages.get(i).contains(clientName)){
                String t = MainServer.serverMessages.get(i).remove(clientName);
                reports.add(t);
            }
            i++;
        }
        i = 0;
        while(i < MainServer.announcements.size()){
            try{
                if(MainServer.announcements.get(i).contains(clientName)){
                    String t = MainServer.announcements.get(i).remove(clientName);
                    reports.add(t);
            }
            }catch (NullPointerException e){
                System.out.println("Oops. Found a Null Pointer when trying to" +
                        "report announcements.");
            }
            i++;
        }
    }
    
    private void addToAnnounceList(String n){
        for(int i = 0; i < MainServer.announcements.size(); i++){
            MainServer.announcements.get(i).addName(n);
        }
        for(int i = 0; i < MainServer.serverMessages.size(); i++){
            MainServer.serverMessages.get(i).addName(n);
        }
    }

    private void changeAnnouncementsName(String oldN, String newN){
        for(int i = 0; i < MainServer.announcements.size(); i++){
            MainServer.announcements.get(i).changeName(oldN, newN);
        }
    }

    private boolean ping(){
        boolean ret = false;
        try{
            outToClient.writeBytes(MainServer.PING + '\n');
        }
        catch(IOException e){
            closeConnections();
            ret = true;
        }
        return ret;
    }
    private void closeConnections() {
        System.out.println("Closing connection to: " + aSocket.getInetAddress().toString());
        if(MainServer.clients.contains(clientName)){
            MainServer.clients.remove(clientName);
            if(MainServer.clientConnect.containsKey(clientName.toLowerCase()))
                MainServer.clientConnect.remove(clientName.toLowerCase());
        }
        try{
            if(aSocket != null){
                allToReport(); //clears client from announcement list
                reports.clear();
                aSocket.close();
                aSocket = null;
            }
        }catch(IOException e){
            aSocket = null;
        }
        try{
            if(inFromClient != null){
                inFromClient.close();
                inFromClient = null;
            }
        }catch(IOException e){
            inFromClient = null;
        }
        try{
            if(outToClient != null){
                outToClient.close();
                outToClient = null;
            }
        }catch(IOException e){
            outToClient = null;
        }
        MainServer.announcements.add(new Announcement(clientName + " has disconnected.", "SERVER"));
        end = true;
    }

}


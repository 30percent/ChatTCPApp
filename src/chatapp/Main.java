/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chatapp;
import java.io.*;

/**
 *
 * @author mrtodd
 */
public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        System.out.print("Client or Server: ");
        BufferedReader input;
        String choice;
        boolean test = false;
        int port = 0;
        String serverName;
        Thread.sleep(700);
        input = new BufferedReader( new InputStreamReader(System.in));
        choice = input.readLine();
        if(choice.equalsIgnoreCase("server")){
            System.out.print("What port: ");
            choice = input.readLine();
            while(!test){
                try{
                    port = Integer.parseInt(choice);
                    test = true;
                }
                catch (NumberFormatException e){
                    System.out.println("That is not a valid port");
                    test = false;
                }
            }
            choice = "";
            System.out.print("Name of Server: ");
            while(choice.length() < 1){
                choice = input.readLine();
            }
            serverName = choice;
            MainServer serv = new MainServer(port, serverName);
        }
        else if(choice.equalsIgnoreCase("client")){
            try{
                Client myClient = new Client(input);
                myClient.chat();
            }
            catch (IOException e){
                System.out.println("Error in reading from input.");
                System.exit(1);
            }
        }
    }

}

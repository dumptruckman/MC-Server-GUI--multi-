/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

//import java.net.*;

/**
 *
 * @author dumptruckman
 */
/*
public class MCServerGUIHTTPServer extends Thread {
    public MCServerGUIHTTPServer(int listen_port, MCServerGUIView newGui) {
        gui = newGui;
        port = listen_port;
    }

    public void startHttpServer() {
        this.start();
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            //print/send message to the guiwindow
            System.out.println("Trying to bind to localhost on port " + Integer.toString(port) + "...");
            //make a ServerSocket and bind it to given port,
            serverSocket = new ServerSocket(port);
        } catch (Exception e) { //catch any errors and print errors to gui
            System.out.println("Fatal Error:" + e.getMessage());
            return;
        }
        while (true) {
            System.out.println("Ready, Waiting for requests...");
            try {
                //this call waits/blocks until someone connects to the port we
                //are listening to
                Socket connectionSocket = serverSocket.accept();
                //figure out what ipaddress the client commes from, just for show!
                InetAddress client = connectionSocket.getInetAddress();
                //and print it to gui
                System.out.println(client.getHostName() + " connected to server.");
                //Read the http request from the client from the socket interface
                //into a buffer.
                java.io.BufferedReader input = new java.io.BufferedReader(new java.io.InputStreamReader(connectionSocket.getInputStream()));
                //Prepare a outputstream from us to the client,
                //this will be used sending back our response
                //(header + requested file) to the client.
                java.io.DataOutputStream output = new java.io.DataOutputStream(connectionSocket.getOutputStream());
                //as the name suggest this method handles the http request, see further down.
                //abstraction rules
                //http_handler(input, output);
            }
                catch (Exception e) { //catch any errors, and print them
                System.out.println("\nError:" + e.getMessage());
            }
        } //go back in loop, wait for next request
    }



    private int port;
    private MCServerGUIView gui;
}*/
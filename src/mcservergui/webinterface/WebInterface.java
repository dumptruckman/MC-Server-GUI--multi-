/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.webinterface;

import java.io.*;
import java.net.*;

import mcservergui.gui.GUI;

/**
 *
 * @author dumptruckman
 */
public class WebInterface {

    public WebInterface (int port, GUI gui) {
        this.port = port;
        this.gui = gui;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void start() {
        run = true;
        listener = new Listener();
        listener.start();
    }

    public void stop() {
        run = false;
        listener.interrupt();
    }

    private int port;
    private boolean run;
    private ServerSocket socket;
    private GUI gui;
    private Listener listener;

    private final class Listener extends Thread {
        @Override public void run() {
            try {
                socket = new ServerSocket(port);
                System.out.println("Listening on " + port);
            } catch (IOException ioe) {
                // server failed to start
                return;
            }
            try {
                while (run) {
                    Socket client;
                    try {
                        client = socket.accept();
                    } catch (java.io.IOException e) {
                        /*
                        gui.addTextToConsoleOutput("[MC Server GUI] " + e);
                        gui.addTextToConsoleOutput("[MC Server GUI] Accept failed on port "
                                + port + "!");
                         * 
                         */
                        break;
                    }
                    handleConnection(client);
                }
            } finally {
                try {
                    socket.close();
                } catch (java.io.IOException e) {
                }
            }
        }
    }

    private void handleConnection(Socket client) {
        BufferedReader in = null;
        BufferedWriter out = null;
        System.out.println("Connection accepted");
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

            String data = "";
            String line = "";
            while ((line = in.readLine()) != null) {
                data += line;
            }
            processData(data);
        } catch (IOException ioe) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) { }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) { }
            }
        }
    }

    private void processData(String data) {
        System.out.println(data);
    }
}

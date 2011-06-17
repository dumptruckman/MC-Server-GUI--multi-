/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.webinterface;

import java.io.*;
import java.net.*;
import java.nio.channels.*;

import org.codehaus.jackson.*;

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

    public WebInterface(GUI gui) {
        this.gui = gui;
        port = 42424;
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
        try {
            gui.webLogAdd("Shutting down web interface");
            socket.close();
        } catch (Exception e) {
        }
        run = false;
        listener.interrupt();
        // Because I don't know enough about thread locking!:
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {}
    }

    private int port;
    private boolean run;
    private ServerSocket socket;
    //private ServerSocketChannel socket;
    private GUI gui;
    private Listener listener;

    private final class Listener extends Thread {
        @Override public void run() {
            try {
                socket = new ServerSocket(port);
                //socket = ServerSocketChannel.open();
                //socket.configureBlocking(false);
                //socket.socket().bind(new InetSocketAddress(port));
                //Selector selector = Selector.open();
                //socket.register(selector, SelectionKey.OP_ACCEPT);
                gui.webLogAdd("Listening on port: " + port);
            } catch (IOException ioe) {
                gui.webLogAdd("Failed to listen on port: " + port + "!");
                gui.webLogAdd("Perhaps it is already in use?");
                return;
            }
            try {
                while (run) {
                    Socket client;
                    try {
                        client = socket.accept();
                        handleConnection(client);
                        client.close();
                    } catch (java.io.IOException e) {
                        break;
                    }
                }
            } finally {
                WebInterface.this.stop();
            }
        }
    }

    private void handleConnection(Socket client) {
        //gui.webLogAdd("Connection from " + client.getInetAddress().getHostAddress());
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String data = "";
            String line = "";
            System.out.println("reading..");
            //client.getChannel().configureBlocking(false);
            while (in.ready()) {
                /*line = in.readLine();
                System.out.println(line);
                if (line != null) {
                    System.out.println(line);
                    data += line;
                } else {
                    System.out.println("null");
                    break;
                }*/
                int c;
                while ((c = in.read()) != -1) {
                    data += Character.toString((char)c);
                }
            }
            
            
            
            //while ((line = in.readLine()) != null) {
            //    System.out.println(line);
            //    data += line;
            //}
            //while ((c = in.read()) != -1) {
            //    data += Character.toString((char)c);
            //}
            System.out.println(data);
            System.out.println("responding..");
            String response = processData(client, data);
            System.out.println(response);

            out = new PrintWriter(client.getOutputStream(), true);
            out.write(response);
            out.flush();
        } catch (IOException ioe) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) { }
            }
            if (out != null) {
                out.close();
            }
        }
    }

    private void webLog(Socket client, String message) {
        gui.webLogAdd(client.getInetAddress().getHostAddress() + " " + message);
    }

    private String processData(Socket client, String data) {
        JsonParser jp = null;
        String response = "";
        try {
            jp = new JsonFactory().createJsonParser(data);
            jp.nextToken();
            if (jp.getCurrentToken() != null) {
                jp.nextToken();
                String identifier = jp.getCurrentName();
                jp.nextToken();
                if (identifier.equalsIgnoreCase("Server Control")) {
                    String control = jp.getText();
                    if (control.equalsIgnoreCase("Start")) {
                        webLog(client, "issued a Server Start");
                        gui.startServer();
                        response = new ResponseWriter("Success", "Server Start").getResponse();
                    } else if (control.equalsIgnoreCase("Stop")) {
                        webLog(client, "issued a Server Stop");
                        gui.stopServer();
                        response = new ResponseWriter("Success", "Server Stop").getResponse();
                    } else {
                        webLog(client, " sent an unrecognized Server Control!");
                        response = new ResponseWriter("Error", "No such Server Control").getResponse();
                    }
                } else if (identifier.equalsIgnoreCase("Server Command")) {
                    response = new ResponseWriter("Error", "No such Server Command").getResponse();
                } else {
                    webLog(client, " sent an unrecognized identifier!");
                    response = new ResponseWriter("Error", "Unrecognized Identifier").getResponse();
                }
            }
        } catch (IOException ioe) {
            webLog(client, " sent invalid data!");
            response = new ResponseWriter("Error", "Invalid Data").getResponse();
        } finally {
            if (jp != null) {
                try {
                    jp.close();
                } catch (IOException ioe) { }
            }
            return response;
        }
    }

    class ResponseWriter {
        
        private StringWriter response;
        private JsonGenerator jg;
        
        public ResponseWriter() {
            response = new StringWriter();
            try {
                jg = new JsonFactory().createJsonGenerator(response);
                jg.writeStartObject();
            } catch (IOException ioe) {
                jg = null;
            }
        }

        public ResponseWriter(String s) {
            response = new StringWriter();
            try {
                jg = new JsonFactory().createJsonGenerator(response);
                jg.writeStartObject();
            } catch (IOException ioe) {
                jg = null;
            }
            add(s, "");
        }

        public ResponseWriter(String s1, String s2) {
            response = new StringWriter();
            try {
                jg = new JsonFactory().createJsonGenerator(response);
                jg.writeStartObject();
            } catch (IOException ioe) {
                jg = null;
            }
            add(s1, s2);
        }

        public void add(String s1, String s2) {
            if (jg != null) {
                try {
                    jg.writeStringField(s1, s2);
                } catch (IOException ioe) {
                }
            }
        }

        public String getResponse() {
            if (jg != null) {
                try {
                    jg.writeEndObject();
                    jg.close();
                    return response.toString();
                } catch (IOException ioe) {
                    return "";
                }
            } else {
                return "";
            }
        }
    }

}


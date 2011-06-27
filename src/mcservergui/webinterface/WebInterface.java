/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.webinterface;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

import mcservergui.gui.GUI;
import mcservergui.task.event.EventModel;

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

    /*public void start() {
        run = true;
        
    }*/

    public void stop() {
        try {
            gui.webLogAdd("Shutting down web interface");
            if (serverChannel != null) {
                serverChannel.close();
            }
            //if (socketChannel != null) {
            //    socketChannel.close();
            //}
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
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private SocketChannel sChannel;
    private GUI gui;
    private Listener listener;

    private final class Listener extends Thread {
        @Override public void run() {
            selector = null;
            serverChannel = null;
            // Create a non-blocking server socket and check for connections
            try {
                // Create the selector
                selector = Selector.open();

                // Create a non-blocking server socket channel on port 80
                serverChannel = ServerSocketChannel.open();
                serverChannel.configureBlocking(false);
                serverChannel.socket().bind(new InetSocketAddress(port));
                gui.webLogAdd("Listening on port: " + serverChannel.socket().getLocalPort());
                
                // Register both channels with selector
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            } catch (IOException e) {
                gui.webLogAdd("Failed to listen on port: " + port + "!");
                gui.webLogAdd("Perhaps it is already in use?");
                if (selector != null) {
                    try {
                        selector.close();
                    } catch (IOException ie) {}
                }
                if (serverChannel != null) {
                    try {
                        serverChannel.close();
                    } catch (IOException ie) {}
                }
                return;
            }

            while (run) {
                try {
                    // Wait for an event
                    selector.select();
                } catch (IOException ioe) {
                    System.err.println("Error with selector");
                    break;
                }

                // Get list of selection keys with pending events
                java.util.Iterator it = selector.selectedKeys().iterator();

                // Process each key
                while (it.hasNext()) {
                    // Get the selection key
                    SelectionKey selKey = (SelectionKey)it.next();

                    // Remove it from the list to indicate that it is being processed
                    it.remove();

                    try {
                        processSelectionKey(selKey);
                    } catch (IOException e) {
                        // Handle error with channel and unregister
                        System.err.println("Error processing channel");
                        selKey.cancel();
                    }
                }    
            }
            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException ie) {}
            }
            if (serverChannel != null) {
                try {
                    serverChannel.close();
                } catch (IOException ie) {}
            }
        }
    }

    public void processSelectionKey(SelectionKey selKey) throws IOException {
        if (selKey.isValid() && selKey.isAcceptable()) {
            // Get channel with connection request
            ServerSocketChannel connectedChannel =
                    (ServerSocketChannel)selKey.channel();

            // Accept the connection request.
            sChannel = (SocketChannel) connectedChannel.accept();
            sChannel.configureBlocking(false);

            // If serverSocketChannel is non-blocking, sChannel may be null
            if (sChannel == null) {
                selKey.cancel();
                // There were no pending connection requests; try again later.
            } else {
                // Establish connection and set up for reading
                sChannel.finishConnect();
                sChannel.register(selector, sChannel.validOps());
            }
        }
        // Read data and respond
        if (selKey.isValid() && selKey.isReadable()) {
            // Read from the channel
            String data = readMessage(selKey);

            sChannel = (SocketChannel)selKey.channel();
            // Process the data and send a response
            writeMessage(sChannel, processData(sChannel.socket(), data));

            // Close the channel
            sChannel.close();
            selKey.cancel();
        }
    }

    public String readMessage(SelectionKey key) {
        int nBytes = 0;
        SocketChannel socketConection = (SocketChannel)key.channel();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        String result = "";
        try {
            nBytes = socketConection.read(buf);
            buf.flip();
            Charset charset = Charset.forName("us-ascii");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer charBuffer = decoder.decode(buf);
            result =  charBuffer.toString();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void writeMessage(SocketChannel socket, String message) {
        try {
            int numBytesWritten = socket.write(str_to_bb(message));
        } catch (IOException e) {
            webLog(socket.socket(), "Connection closed unexpectedly!");
        }
    }
    
    public static ByteBuffer str_to_bb(String msg){
        try{
            return Charset.forName("UTF-8").newEncoder()
                    .encode(CharBuffer.wrap(msg));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void webLog(Socket client, String message) {
        gui.webLogAdd(client.getInetAddress().getHostAddress() + " " + message);
    }

    private String processData(Socket client, String json) {
        ObjectMapper mapper = new ObjectMapper();
        String response = "";
        java.util.Map<?,?>root = null;
        // Convert json to map
        try {
            root = mapper.readValue(json, java.util.Map.class);
        } catch (IOException e) {
            webLog(client, " sent invalid data!");
            response = new ResponseWriter("Error", "Invalid Data")
                    .getResponse();
            return response;
        }
        if (root == null) {
            webLog(client, " sent invalid data!");
            response = new ResponseWriter("Error", "Invalid Data")
                    .getResponse();
            return response;
        }
        // Fail if the data does not contain the correct keys
        if (!root.containsKey("Password") || !root.containsKey("Request") ||
                !root.containsKey("Data")) {
            webLog(client, " sent incomplete data!");
            response = new ResponseWriter("Error", "Incomplete Data")
                    .getResponse();
            return response;
        }
        // Fail if the data does not contain the correct password
        if (!root.get("Password").equals(gui.config.web.getPassword())) {
            webLog(client, "did not pass authentication!");
            response = new ResponseWriter("Error", "Authentication Error")
                    .getResponse();
            return response;
        }

        // Handle the request
        if (root.get("Request").equals("Start Server")) {
            gui.startServer();
            webLog(client, "issued a Server Start");
            response = new ResponseWriter("Success", "Server Started").getResponse();
        } else if (root.get("Request").equals("Stop Server")) {
            gui.stopServer();
            webLog(client, "issued a Server Stop");
            response = new ResponseWriter("Success", "Server Stopped").getResponse();
        } else if (root.get("Request").equals("Send Input")) {
            //java.util.List<String> data = getRequestData(jp);
            java.util.List<String> data = (java.util.List<String>)root.get("Data");
            System.out.println(data);
            if (!data.isEmpty()) {
                gui.sendInput(data.get(0));
                webLog(client, "sent command '" + data.get(0) + "'");
                response = new ResponseWriter("Success", "Command sent").getResponse();
            } else {
                response = new ResponseWriter("Error", "Did not specify command").getResponse();
            }
        } else if (root.get("Request").equals("Get Output")) {
            if (!gui.config.web.isDisableGetRequests()) {
                webLog(client, "requested server output");
            }
            response = new ResponseWriter("Success", gui.getConsoleOutput()).getResponse();
        } else if (root.get("Request").equals("Execute Task")) {
            java.util.List<String> data = (java.util.List<String>)root.get("Data");
            System.out.println(data);
            if (!data.isEmpty()) {
                if (gui.startTaskByName(data.get(0))) {
                    webLog(client, "executed task '" + data.get(0) + "'");
                    response = new ResponseWriter("Success", "Command sent").getResponse();
                } else {
                    response = new ResponseWriter("Error", "Invalid task").getResponse();
                }
            }
        } else if (root.get("Request").equals("Get Task Names")) {
            if (!gui.config.web.isDisableGetRequests()) {
                webLog(client, "requested task names");
            }
            List<String> tasknames = new ArrayList<String>();
            Iterator it = gui.config.schedule.getEvents().iterator();
            while (it.hasNext()) {
                EventModel event = (EventModel)it.next();
                tasknames.add(event.getName());
            }
            response = new ResponseWriter("Success", tasknames).getResponse();
        } else {
            webLog(client, "sent an unrecognized request!");
            response = new ResponseWriter("Error", "No such request").getResponse();
        }
        
        return response;
    }

    private java.util.List<String> getRequestData(JsonParser jp) throws IOException {
        java.util.List<String> data = new java.util.ArrayList<String>();
        jp.nextToken();
        jp.nextToken();
        jp.nextToken();
        while (jp.getCurrentToken() != JsonToken.END_ARRAY) {
            data.add(jp.getText());
            jp.nextToken();
        }
        return data;
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
            this();
            /*
            response = new StringWriter();
            try {
                jg = new JsonFactory().createJsonGenerator(response);
                jg.writeStartObject();
            } catch (IOException ioe) {
                jg = null;
            }
             * 
             */
            add(s, "");
        }

        public ResponseWriter(String s1, String s2) {
            this();
            /*
            response = new StringWriter();
            try {
                jg = new JsonFactory().createJsonGenerator(response);
                jg.writeStartObject();
            } catch (IOException ioe) {
                jg = null;
            }*/
            add(s1, s2);
        }

        public ResponseWriter(String name, List<String> values) {
            this();
            add(name, values);
        }

        public void add(String s1, String s2) {
            if (jg != null) {
                try {
                    jg.writeStringField(s1, s2);
                } catch (IOException ioe) {
                }
            }
        }

        public void add(String name, List<String> values) {
            if (jg != null) {
                try {
                    jg.writeArrayFieldStart(name);
                    for (int i = 0; i < values.size(); i++) {
                        jg.writeString(values.get(i));
                    }
                    jg.writeEndArray();
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


/*
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package mcservergui;

import java.net.*;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIProxyServer {

    public MCServerGUIProxyServer() {
        listener = new Listener();
        listener.start();
    }

    public void stop() {
        try {
            socket.close();
        } catch (Exception e) {
        }
        
        listener.interrupt();
    }

    private final class Listener extends Thread {
        @Override public void run() {
            //initialize();

            while (true) {
                //startup();

                //String ip = options.get("ipAddress");
                String ip = "127.0.0.1";
                //int port = options.getInt("port");
                int port = 25566;

                InetAddress address;
                if (ip.equals("0.0.0.0")) {
                    address = null;
                } else {
                    try {
                        address = InetAddress.getByName(ip);
                    } catch (UnknownHostException e) {
                        System.out.println("[SimpleServer] " + e);
                        System.out.println("[SimpleServer] Invalid listening address " + ip);
                        break;
                    }
                }

                try {
                    socket = new ServerSocket(port, 0, address);
                } catch (java.io.IOException e) {
                    System.out.println("[SimpleServer] " + e);
                    System.out.println("[SimpleServer] Could not listen on port " + port
                    + "!\nIs it already in use? Exiting application...");
                    break;
                }

                System.out.println("[SimpleServer] Wrapper listening on "
                        + socket.getInetAddress().getHostAddress() + ":"
                        + socket.getLocalPort() + " (connect here)");
                if (socket.getInetAddress().getHostAddress().equals("0.0.0.0")) {
                    System.out.println("[SimpleServer] Note: 0.0.0.0 means all"
                            + " IP addresses; you want this.");
                }

                try {
                    while (true) {
                        Socket client;
                        try {
                            client = socket.accept();
                        } catch (java.io.IOException e) {
                            System.out.println("[SimpleServer] " + e);
                            System.out.println("[SimpleServer] Accept failed on port "
                                    + port + "!");
                            break;
                        }
                        new MCServerGUIPlayer(client, MCServerGUIProxyServer.this);
                    }
                } finally {
                    try {
                        socket.close();
                    } catch (java.io.IOException e) {
                    }
                }

                MCServerGUIProxyServer.this.stop();
            }

            //cleanup();
        }
    }

    private Listener listener;
    private ServerSocket socket;
}

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

package mcservergui.proxyserver;

import mcservergui.gui.GUI;
import mcservergui.config.Config;
import mcservergui.config.ServerProperties;

import java.net.*;

/**
 *
 * @author dumptruckman
 */
public class ProxyServer {

    public ProxyServer(GUI gui, Config config, ServerProperties serverProps) {
        listener = new Listener();
        run = true;
        startCode = 0;
        //this.addObserver(gui.server);
        listener.start();
        this.gui = gui;
        this.config = config;
        this.serverProps = serverProps;
    }

    public void stop() {
        try {
            socket.close();
        } catch (Exception e) {
        }
        run = false;
        listener.interrupt();
    }

    public int getStartCode() {
        try {
            synchronized (listener) {
                listener.wait();
            }
        } catch (InterruptedException ie) {
            
        }
        return startCode;
    }

    public void kick(String name, String reason) {
        Player player = playerList.findPlayer(name);
        if (player != null) {
            player.kick(reason);
        }
    }

    public void banKick(String name, String msg) {
        if (name != null) {
            gui.sendInput("ban " + name);
            Player p = playerList.findPlayer(name);
            if (p != null) {
                //adminLog("Player " + p.getName() + " was banned:\t " + msg);
                p.kick(msg);
            }
        }
    }

    public String findName(String prefix) {
        Player i = playerList.findPlayer(prefix);
        if (i != null) {
            return i.getName();
        }

        return null;
    }

    public Player findPlayer(String prefix) {
        return playerList.findPlayer(prefix);
    }

    public Player findPlayerExact(String exact) {
        return playerList.findPlayerExact(exact);
    }

    private void kickAllPlayers(String message) {
        for (Player player : playerList.getArray()) {
            player.kick(message);
        }
    }

    private final class Listener extends Thread {
        @Override public void run() {
            //initialize();

            while (run) {
                //startup();
                playerList = new PlayerList();
                gui.setPlayerList(playerList);

                //String ip = options.get("ipAddress");
                String ip = serverProps.getServerIp();
                if (ip.isEmpty()) {
                    ip = "0.0.0.0";
                }
                int port = config.getExtPort();

                synchronized (this) {
                    InetAddress address;
                    if (ip.equals("0.0.0.0")) {
                        address = null;
                    } else {
                        try {
                            address = InetAddress.getByName(ip);
                        } catch (UnknownHostException e) {
                            gui.guiLog(e.toString(), GUI.LogLevel.SEVERE);
                            gui.guiLog("Invalid listening address " + ip + "!"
                                    + "<br>Aborting...", GUI.LogLevel.SEVERE);
                            startCode = -1;
                            this.notifyAll();
                            break;
                        }
                    }

                    try {
                        socket = new ServerSocket(port, 0, address);
                    } catch (java.io.IOException e) {
                        gui.guiLog(e.toString(), GUI.LogLevel.SEVERE);
                        gui.guiLog("Could not listen on port " + port + "!<br>"
                                + "Is it already in use? Aborting...",
                                GUI.LogLevel.SEVERE);
                        startCode = -1;
                        this.notifyAll();
                        break;
                    }

                    gui.guiLog("Listening on " + socket.getInetAddress()
                            .getHostAddress() + ":" + socket.getLocalPort()
                            + " (players connect here)");
                    if (socket.getInetAddress().getHostAddress().equals("0.0.0.0")) {
                        gui.guiLog("0.0.0.0 means all IP addresses; you want "
                                + "this.");
                    }
                    startCode = 1;
                    this.notifyAll();
                }

                try {
                    while (true) {
                        Socket client;
                        try {
                            client = socket.accept();
                        } catch (java.io.IOException e) {
                            gui.guiLog(e.toString(), GUI.LogLevel.WARNING);
                            gui.guiLog("Accept failed on port " + port + "!"
                                    + "  Server likely stopped.",
                                    GUI.LogLevel.WARNING);
                            break;
                        }
                        new Player(client, ProxyServer.this);
                    }
                } finally {
                    try {
                        socket.close();
                    } catch (java.io.IOException e) {
                    }
                }

                //ProxyServer.this.stop();
            }
            ProxyServer.this.stop();
            if (gui.server.isRunning()) {
                gui.stopServer();
            }
        }
    }

    private int startCode;
    private Listener listener;
    private ServerSocket socket;
    public PlayerList playerList;
    public ServerProperties serverProps;
    public GUI gui;
    private Config config;
    private boolean run;
}

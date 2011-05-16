/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Observable;
import java.util.Observer;
import java.io.*;

/**
 *
 * @author Billing Manager
 */
public class MCServerGUIServerReceiver extends Observable {
    public MCServerGUIServerReceiver(BufferedReader newBr) {
        br = newBr;
        timer = new java.util.Timer();
        backgroundWork = new BackgroundWork();
        timer.scheduleAtFixedRate(backgroundWork, 0, 1000);
    }

    public class BackgroundWork extends TimerTask implements Observer {
        public BackgroundWork() {
            serverRunning = true;
        }

        public void run() {
            if (serverRunning) {
                while (!hasChanged()) {
                    receivedFromServer = "";
                    try {
                        while (br.ready()) {
                            try {
                                int newChar = br.read();
                                if (newChar != -1) {
                                    receivedFromServer += (char) newChar;
                                }
                            } catch (IOException e) {
                                System.out.println("ServerReceiver reports BufferedReader IOException while trying to read().");
                            }
                        }
                        receivedFromServer = receivedFromServer.replace("\n>", "");
                        receivedFromServer = receivedFromServer.replace("\r>", "");
                        receivedFromServer = receivedFromServer.replace("\n\r>", "");
                        receivedFromServer = receivedFromServer.replace("\r\n>", "");
                        receivedFromServer = receivedFromServer.replace(">\n", "");
                        receivedFromServer = receivedFromServer.replace(">\r", "");
                        receivedFromServer = receivedFromServer.replace(">\n\r", "");
                        receivedFromServer = receivedFromServer.replace(">\r\n", "");
                        receivedFromServer = receivedFromServer.replace("\n", "");
                        receivedFromServer = receivedFromServer.replace("\r", "");
                        receivedFromServer = receivedFromServer.replace("\n\r", "");
                        receivedFromServer = receivedFromServer.replace("\r\n", "");
                        if ((!receivedFromServer.equals("")) && (!receivedFromServer.equals(">"))) {
                            receivedFromServer += "\n";
                            setChanged();
                            notifyObservers();
                        }
                    } catch (IOException e) {
                        System.out.println("ServerReceiver reports BufferedReader IOException while waiting for ready().  Assuming server ended.");
                        serverRunning = false;
                        break;
                    }
                }
            } else {
                this.cancel();
            }
        }
        
        public void update(Observable o, Object arg) {
            if (arg.equals("serverStopped")) {
                serverRunning = false;
            }
            if (arg.equals("serevrStarted")) {
                serverRunning = true;
            }
        }

        private boolean serverRunning;
    }

    public String get() {
        return receivedFromServer;
    }

    private Timer timer;
    private BufferedReader br;
    private String receivedFromServer;
    public BackgroundWork backgroundWork;
}

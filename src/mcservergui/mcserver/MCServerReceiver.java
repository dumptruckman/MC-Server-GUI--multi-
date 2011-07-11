/*
 * MCServerReceiver.java
 */

package mcservergui.mcserver;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Observable;
import java.util.Observer;
import java.io.*;

/**
 *
 * @author dumptruckman
 */
public class MCServerReceiver extends Observable {
    public MCServerReceiver(BufferedReader newBr) {
        br = newBr;
        timer = new java.util.Timer();
        backgroundWork = new BackgroundWork();
        timer.scheduleAtFixedRate(backgroundWork, 0, 50);
    }

    public class BackgroundWork extends TimerTask implements Observer {
        public BackgroundWork() {
            serverRunning = true;
        }

        @Override public void run() {
            if (serverRunning) {
                receivedFromServer = "";
                try {
                    while ((br.ready()) && (!hasChanged())) {
                        try {
                            receivedFromServer = br.readLine();
                        } catch (IOException e) {
                            System.out.println("ServerReceiver reports BufferedReader IOException while trying to readLine().");
                        }
                        //System.out.println(receivedFromServer); // Testing
                        if ((!receivedFromServer.equals("")) && (!receivedFromServer.equals(">")) && (!receivedFromServer.equals(">>")) && (!receivedFromServer.equals(">>>"))) {
                            receivedFromServer += System.getProperty("line.separator");
                            //receivedFromServer += "\\n";
                            // This part helps me find special characters
                            /*for (int i=0; i < receivedFromServer.length(); i++) {
                                System.out.print(receivedFromServer.codePointAt(i) + " ");
                            }
                            System.out.println();*/
                            setChanged();
                            notifyObservers();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("ServerReceiver reports BufferedReader IOException while waiting for ready().  Assuming server ended.");
                    serverRunning = false;
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

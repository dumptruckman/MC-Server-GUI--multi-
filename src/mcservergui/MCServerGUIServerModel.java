/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import java.io.*;
import javax.swing.SwingUtilities;
import java.util.Observable;
import java.util.Observer;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIServerModel extends Observable implements Observer, java.beans.PropertyChangeListener {
    
    public MCServerGUIServerModel()
    {
        serverRunning = false;
    }

    // Method for building the cmdLine
    public void setCmdLine(String...args) {
        cmdLine = args;
    }

    // Method for starting the server
    public boolean start() {
        try {
            // Run the server
            ProcessBuilder pb = new ProcessBuilder(cmdLine);
            pb.redirectErrorStream(true);
            ps = pb.start();

            // Flag this as started
            serverRunning = true;
            setChanged();
            notifyObservers("serverStarted");

            receivedFromServer = "";

            // Collect necessary streams
            br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            osw = new OutputStreamWriter(ps.getOutputStream());

            serverReceiver = new MCServerGUIServerReceiver(br);
            serverReceiver.addObserver(this);
            addObserver(serverReceiver.backgroundWork);
           
            return true;
        } catch (Exception e) {
            System.out.println("Problem launching server");
            return false;
        }
    }

    public void update(Observable o, Object arg) {
        receivedFromServer = serverReceiver.get();
        this.setChanged();
        notifyObservers("newOutput");
    }

    public void propertyChange(java.beans.PropertyChangeEvent evt) {
        if (evt.getNewValue().equals(false)) {
            serverRunning = false;
            setChanged();
            notifyObservers("serverStopped");
        }
    }

    public String getReceived() {
        return receivedFromServer;
    }

    public boolean isRunning() {
        return serverRunning;
    }

    // Method for sending commands to the server
    public void send(final String string) {
        Runnable serverSender = new Runnable() {
            public void run() {
                try {
                    osw.write(string + "\n");
                    osw.flush();
                    //Following is for testing purposes
                    //DataInputStream dis = new DataInputStream(ps.getInputStream());
                    //System.out.println("from subtask: " + dis.readLine());
                } catch (IOException e) {
                    System.out.println("[GUI] Error sending server data.");
                }
            }
        };
        SwingUtilities.invokeLater(serverSender);
    }

    // Method for stopping server
    public void stop() {
        System.out.println("Request to stop received");
        send("stop");
        MCServerGUIServerStopper serverStopper = new MCServerGUIServerStopper(ps, br, osw);
        serverStopper.addPropertyChangeListener(this);
        serverStopper.execute();
    }

    private Process ps;
    private String[] cmdLine;
    private String receivedFromServer;
    private BufferedReader br;
    private OutputStreamWriter osw;
    private MCServerGUIServerReceiver serverReceiver;
    private boolean serverRunning;
}

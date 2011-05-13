/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import java.io.*;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import java.util.Observable;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIServerModel extends Observable {
    
    public MCServerGUIServerModel()
    {
        serverStarted = false;
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

            // debugging
            System.out.println("Server launched");

            // Flag this as started
            serverStarted = true;
            setChanged();
            notifyObservers("serverStatus");

            serverReceiveString = null;

            // Collect necessary streams
            //isr = new InputStreamReader(ps.getInputStream());
            br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            osw = new OutputStreamWriter(ps.getOutputStream());

            // Start receiving server output
            serverReceiver.execute();
           
            return true;
        } catch (Exception e) {
            System.out.println("Problem launching server");
            return false;
        }
    }

    // Method for receiving from server
    SwingWorker serverReceiver = new SwingWorker<Void, Void>() {
        @Override
        public Void doInBackground() {
            System.out.println("serverReceiver started: serverStarted = " + serverStarted);
            while ((!hasChanged()) && (serverStarted)) {
                try {
                    serverReceiveString = br.readLine() + "\n";
                    if (serverReceiveString.equals(">\n")) {
                        serverReceiveString = null;
                    }
                    setChanged();
                    notifyObservers("newOutput");
                } catch (IOException e) {
                    serverStarted = false;
                    setChanged();
                    notifyObservers("serverStatus");
                    System.out.println("failed to readline()");
                }
            }
            return null;
        }
    };

    public String getReceived() {
        return serverReceiveString;
    }

    public boolean isRunning() {
        System.out.println("Checking isRunning() = " + serverStarted);
        return serverStarted;
    }

    // Method for sending commands to the server
    public void send(final String string) {
        Runnable serverSender = new Runnable() {
            public void run() {
                try {
                    System.out.println("sending " + string);
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

    // Worker to stop the server
    SwingWorker Stop = new SwingWorker<Boolean, Boolean>() {
        @Override
        public Boolean doInBackground() {
            send("stop");                           // Sends the command to shut down the server
            try {
                System.out.println("Stopping server.");
                ps.waitFor();
                return true;
            } catch (InterruptedException e) {
                System.out.println("ps.waitFor() interrupted!");
                return false;
            }
        }

        @Override
        public void done() {
            System.out.println("Stop done()");
            serverReceiver.cancel(true);
            System.out.println("setting serverStarted to false");
            while(!MCServerGUIServerModel.this.serverReceiver.isDone()) {}
            serverStarted = false;
            setChanged();
            notifyObservers("serverStatus");
            
            try {
                // Close the io streams
                br.close();
                osw.close();
            } catch (IOException e) {
                System.out.println("Error stopping read streams");
            }
        }
    };


    private Process ps;
    private boolean serverStarted;
    private String[] cmdLine;
    private String serverReceiveString;
    private BufferedReader br;
    private OutputStreamWriter osw;
}

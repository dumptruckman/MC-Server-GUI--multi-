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

            serverReceiveString = "";

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
                    System.out.println("Failed to readLine().  Process likely terminated");
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

    // Method for stopping server
    public void stop() {
        send("stop");
        processEndThread.execute();
    }

    // Worker thread to wait for process to end and then finish up the stop process
    SwingWorker processEndThread = new SwingWorker<Boolean, Boolean>() {
        @Override
        public Boolean doInBackground() {
            try {
                ps.waitFor();
                return true;
            } catch (InterruptedException e) {
                System.out.println("ps.waitFor() interrupted!");
                return false;
            }
        }

        @Override
        public void done() {
            try {
                if (this.get() == true) {
                    try {
                        // Close the io streams
                        br.close();
                        osw.close();
                    } catch (IOException e) {
                        System.out.println("Error stopping read streams");
                    } finally {
                        serverReceiver.cancel(true);
                    }
                    System.out.println("done() complete");
                } else {
                    System.out.println("Stop failed");
                }
            } catch (ExecutionException e) {
                System.out.println("Execution Exception");
            } catch (InterruptedException e) {
                System.out.println("Interrupted Exception");
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

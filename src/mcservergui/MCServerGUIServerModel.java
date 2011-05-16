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
import java.util.regex.Pattern;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIServerModel extends Observable {
    
    public MCServerGUIServerModel()
    {
        serverStarted = false;
        //receive();
    }

    // Method for building the cmdLine
    public void setCmdLine(String...args) {
        cmdLine = args;
        System.out.println(cmdLine);
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
    public SwingWorker serverReceiver = new SwingWorker<Void, Void>() {
        @Override
        public Void doInBackground() {
            while (true) {
                System.out.println("serverReceiver waiting for server to start");
                while (!isRunning()) {}
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("serverReceiver interrupted while waiting for server to start fully");
                }
                while (isRunning()) {
                    System.out.println("serverReceiver thinks server has started");
                    try {
                        serverReceiveString = br.readLine();
                        System.out.print(serverReceiveString);
                        if (serverReceiveString.equals("null")) {
                            serverStarted = false;
                            break;
                        }
                        if (serverReceiveString.equals(">")) {
                            serverReceiveString = null;
                        }
                        if (serverReceiveString != null) {
                            serverReceiveString += "\n";
                            firePropertyChange("serverReceiveString", "", serverReceiveString);
                        }
                    } catch (IOException e) {
                        serverStarted = false;
                        setChanged();
                        notifyObservers("serverStatus");
                        System.out.println("Failed to readLine().  Process likely terminated.");
                    }
                }
            }
            //return null;
        }
    };
    
    

    public String getReceived() {
        return serverReceiveString;
    }

    public boolean isRunning() {
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
                    System.out.println("Server stopped, setting serverStarted to false");
                    ps = null;
                    serverStarted = false;
                    setChanged();
                    notifyObservers("serverStatus");
                    try {
                        // Close the io streams
                        br.close();
                        br = null;
                        osw.close();
                        osw = null;
                    } catch (IOException e) {
                        System.out.println("Error stopping read streams");
                    } finally {
                        //serverReceiver.cancel(true);
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

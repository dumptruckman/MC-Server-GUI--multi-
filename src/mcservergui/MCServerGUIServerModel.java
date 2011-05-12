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

            serverReceiveString = null;

            // Collect necessary streams
            isr = new InputStreamReader(ps.getInputStream());
            br = new BufferedReader(isr);

            // debugging
            System.out.println("Server launched");
            receive();
            serverStarted = true;
            return true;
        } catch (Exception e) {
            System.out.println("Problem launching server");
            return false;
        }
    }

    // Method for receiving from server
    public void receive() {
        serverReceiver.execute();
    }
    SwingWorker serverReceiver = new SwingWorker<Void, Void>() {
        @Override
        public Void doInBackground() {
            System.out.println("test");
            while (!hasChanged()) {
                try {
                    serverReceiveString = br.readLine() + "\n";
                } catch (IOException e) {
                    System.out.println("failed to readline()");
                }

                setChanged();
                notifyObservers();
            }
            return null;
        }
    };

    public String getReceived() {
        return serverReceiveString;
    }

    // Method for sending commands to the server
    public void send(final String string) {
        Runnable serverSender = new Runnable() {
            public void run() {
                try {
                    OutputStreamWriter osw = new OutputStreamWriter(ps.getOutputStream());
                    osw.write(string);
                    osw.flush();

                    // This Thread.sleep() is suposeduly suspicious.  Some say it should work without it, but it doesn't
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        System.out.println("error sleeping");
                    }

                    osw.close();

                    //Comment out the thread.sleep() and uncomment these and see the issue
                    DataInputStream dis = new DataInputStream(ps.getInputStream());
                    System.out.println("from subtask: " + dis.readLine());
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
                System.out.println("Stopped server succesfully.");
                serverReceiver.cancel(true);
                serverStarted = false;
                return true;
            } catch (InterruptedException e) {
                System.out.println("ps.waitFor() interrupted!");
                return false;
            }
        }

        @Override
        public void done() {
            try {
                    System.out.println("Stop done()");
                    // Close the reading streams
                    isr.close();
                   // isr = null;
                    br.close();
                    //br = null;
                } catch (IOException e) {
                    System.out.println("Error stopping read streams");
                }
        }
    };


    public Process ps;
    private boolean serverStarted;
    private String[] cmdLine;
    private String serverReceiveString;
    //private java.io.InputStream is = null;
    private InputStreamReader isr = null;
    private BufferedReader br = null;
    //private OutputStreamWriter osw;
}

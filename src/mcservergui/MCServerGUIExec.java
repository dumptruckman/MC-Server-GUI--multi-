/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

//import java.util.*;
import java.io.*;
//import java.net.*;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIExec {
    
    public MCServerGUIExec()
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

            // Collect necessary streams
            isr = new InputStreamReader(ps.getInputStream());
            br = new BufferedReader(isr);

            // debugging
            System.out.println("Server launched");
            serverStarted = true;
            return true;
        } catch (Exception e) {
            System.out.println("Problem launching server");
            return false;
        }
    }

    // Method for receiving the output of the server
    public String receive() {
        SwingWorker serverReceiveWorker = new SwingWorker<String, String>() {
            @Override
            public String doInBackground() {
                if (isr != null) {
                    try {
                        if ((isr.ready()) && (br != null)) {
                            try {
                                StringBuilder line = new StringBuilder();
                                System.out.println("About to read");
                                while(br.ready()) {
                                    try {
                                        int character = br.read();
                                        if (character == -1) {
                                        } else {
                                            if ((!Character.toString(Character.toChars(character)[0]).equals(">")) && (!Character.toString(Character.toChars(character)[0]).equals("\n"))) {
                                                line.append(Character.toChars(character));
                                            }
                                        }
                                    } catch (IOException e) {
                                        return("[GUI] Error receiving server data.");
                                    }
                                }
                                System.out.println(line.toString());
                                return line.toString();
                            } catch (IOException e) {
                                return "[GUI] Error receiving server data.";
                            }
                        }
                    } catch (IOException e) {
                        return null;
                    }
                    return null;
                } else {
                    return null;
                }

            }

        };
        serverReceiveWorker.execute();
        try {
            if (serverReceiveWorker.get() != null) {
                return serverReceiveWorker.get().toString();
            } else {
                return null;
            }
        } catch (InterruptedException e) {
            System.out.println("stringReceiver.get().toString() interrupted");
            return null;
        } catch (ExecutionException e) {
            System.out.println("stringReceiver.get().toString() execution exception");
            return null;
        }
        
    }

    // Method for sending commands to the server
    public void send(final String string) {

        SwingWorker serverSendWorker = new SwingWorker<Void, Void>() {

            @Override
            public Void doInBackground() {
                try {
                    OutputStreamWriter osw = new OutputStreamWriter(ps.getOutputStream());
                    osw.write(string + "\n");
                    osw.flush();
                    
                    // This Thread.sleep() is suposeduly suspicious.  Some say it should work without it, but it doesn't
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("error sleeping");
                    }

                    osw.close();

                    //Comment out the thread.sleep() and uncomment these and see the issue
                    //DataInputStream dis = new DataInputStream(ps.getInputStream());
                    //System.out.println("from subtask: " + dis.readLine());
                } catch (IOException e) {
                    System.out.println("[GUI] Error sending server data.");
                } finally {
                    return null;
                }
            }
        };
        serverSendWorker.execute();
    }
    

    // Method for checking if the server is running
    public boolean isRunning() {

        if ((serverStarted) && (ps != null)) {
            try {
                ps.exitValue();
                serverStarted = false;   // Just in case
                return false;
            } catch (IllegalThreadStateException e) {
                return true;
            }
        } else {
            return false;
        }
    }

    // Method for stopping the server
    //public boolean stop() {
    //    serverStopWorker.execute();
    //}

    SwingWorker Stop = new SwingWorker<Boolean, Boolean>() {
        @Override
        public Boolean doInBackground() {
            send("stop");
            try {
                //serverStarted = false;
                System.out.println("[GUI] Stopping server.");
                ps.waitFor();
                try {
                    isr.close();
                    isr = null;
                    br.close();
                    br = null;
                } catch (IOException e) {
                    System.out.println("Error stopping streams");
                }

                //ps = null;
                serverStarted = false;
                System.out.println("[GUI] Stopped server succesfully.");
                return true;
            } catch (InterruptedException e) {
                System.out.println("[GUI] Error stopping the server!");
                return false;
            }
        }
    };


    public Process ps;
    private boolean serverStarted;
    private String[] cmdLine;
    //private java.io.InputStream is = null;
    private InputStreamReader isr = null;
    private BufferedReader br = null;
    //private OutputStreamWriter osw;
}

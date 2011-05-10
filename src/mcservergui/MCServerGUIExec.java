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

        SwingWorker stringReceiver = new SwingWorker<String, Void>() {
            @Override
            public String doInBackground() {
                if (isr != null) {
                    try {
                        if (isr.ready()) {
                            try {
                                StringBuilder line = new StringBuilder();
                                //System.out.println("About to read");
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
                                //System.out.println(line.toString());
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
        stringReceiver.execute();
        try {
            return stringReceiver.get().toString();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
        
    }

    // Method for sending commands to the server
    public void send(final String string) {

        SwingWorker stringSender = new SwingWorker<Void, Void>() {

            @Override
            public Void doInBackground() {
                try {
                    osw = new OutputStreamWriter(ps.getOutputStream());
                    MCServerGUIExec.this.osw.write(string + "\r\n");
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

            @Override
            public void done() {

            }
        };
        stringSender.execute();
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
    public boolean stop() {
        this.send("stop");
        try {
            //serverStarted = false;
            System.out.println("[GUI] Stopping server.");
            ps.waitFor();
            try {
                isr.close();
                br.close();
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


    public Process ps;
    private boolean serverStarted;
    private String[] cmdLine;
    private java.io.InputStream is = null;
    private InputStreamReader isr = null;
    private BufferedReader br = null;
    private OutputStreamWriter osw;
}

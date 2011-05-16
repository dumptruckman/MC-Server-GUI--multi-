/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import java.io.*;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIExec {
    
    public MCServerGUIExec(javax.swing.JTextPane new_consoleOutput)
    {
        serverStarted = false;
        consoleOutput = new_consoleOutput;
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

    // Method for receiving from server
    public void receive() {
        SwingWorker serverReceiver = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
        /*Runnable serverReceiver = new Runnable() {
            public void run() {*/
                // Generally the isr (InputStreamReader) is set to null after Process.waitFor() is finished.
                if (isr != null) {
                    /*try {*/
                        // Generally the br (BufferedReader) is set to null after Process.waitFor() is finished.
                        //if ((isr.ready()) && (br != null)) {
                        if (br != null) {
                            /*try {*/
                                StringBuilder line = new StringBuilder();
                                System.out.println("About to read");
                                boolean doneReading = false;
                                // Reads from the BufferedReader while it's got stuff to share.
                                /*while(br.ready()) {
                                    try {
                                        int character = br.read();
                                        if (character != -1) {
                                            if ((!Character.toString(Character.toChars(character)[0]).equals(">")) && (!Character.toString(Character.toChars(character)[0]).equals("\n"))) {
                                                line.append(Character.toChars(character));
                                            }
                                        }
                                    } catch (IOException e) {
                                        System.out.println("IOException on br.read()");
                                    }
                                }*/
                                while (!doneReading) {
                                    try {
                                        int character = br.read();
                                        if (character != -1) {
                                            if ((!Character.toString(Character.toChars(character)[0]).equals(">")) && (!Character.toString(Character.toChars(character)[0]).equals("\n"))) {
                                                line.append(Character.toChars(character));
                                            }
                                        }
                                    } catch (IOException e) {
                                        System.out.println("IOException on br.read()");
                                    }
                                }
                                System.out.println(line.toString());        // Debug output

                                // Replace a blank console output but add to a non-blank one
                                if (consoleOutput.getText().equals("")) {
                                    //System.out.println("1 time");
                                    consoleOutput.setText(line.toString());
                                }
                                    // If consoleOutput already has data, add to it
                                else {
                                    consoleOutput.setText(consoleOutput.getText() + line.toString());
                                }
                            /*} catch (IOException e) {
                                System.out.println("IOException on br.ready()");
                            }*/
                        }
                    /*} catch (IOException e) {
                        System.out.println("IOException on isr.ready()");
                    }*/
                }
                return null;
            }
        };
        serverReceiver.execute();
    }

    // Method for sending commands to the server
    public void send(final String string) {
        Runnable serverSender = new Runnable() {
            public void run() {
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
                    DataInputStream dis = new DataInputStream(ps.getInputStream());
                    System.out.println("from subtask: " + dis.readLine());
                } catch (IOException e) {
                    System.out.println("[GUI] Error sending server data.");
                }
            }
        };
        SwingUtilities.invokeLater(serverSender);
    }
    

    // Method for checking if the server is running
    /*public boolean isRunning() {

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
    }*/

    // Method for stopping the server
    //public boolean stop() {
    //    serverStopWorker.execute();
    //}

    // Worker to stop the server
    SwingWorker Stop = new SwingWorker<Boolean, Boolean>() {
        @Override
        public Boolean doInBackground() {
            send("stop");                           // Sends the command to shut down the server
            try {
                System.out.println("Stopping server.");
                ps.waitFor();

                serverStarted = false;
                System.out.println("Stopped server succesfully.");
                return true;
            } catch (InterruptedException e) {
                System.out.println("ps.waitFor() interrupted!");
                return false;
            }
        }

        @Override
        public void done() {
            try {
                    // Close the reading streams
                    isr.close();
                    isr = null;
                    br.close();
                    br = null;
                } catch (IOException e) {
                    System.out.println("Error stopping read streams");
                }
        }
    };
    /*
    public void stop() {
        Runnable serverStopper = new Runnable() {
            public void run() {
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
                    //return true;
                } catch (InterruptedException e) {
                    System.out.println("[GUI] Error stopping the server!");
                    //return false;
                }
            }
        };
        SwingUtilities.invokeLater(serverStopper);
    }*/


    public Process ps;
    private boolean serverStarted;
    private String[] cmdLine;
    //private java.io.InputStream is = null;
    private InputStreamReader isr = null;
    private BufferedReader br = null;
    private javax.swing.JTextPane consoleOutput;
    //private OutputStreamWriter osw;
}

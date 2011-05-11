/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

//import java.util.*;
import java.io.*;
//import java.net.*;
import javax.swing.SwingWorker;

/**
 *
 * @author Roton
 */
public class MCServerGUIExec {
    
    public MCServerGUIExec(String...args)
    {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            ps = pb.start();
            
            is = ps.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            //bw = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
            
            System.out.println("Server launched");
        } catch (Exception e) {
            System.out.println("Problem launching server");
        }
    }

    public String receive() {
        try {
            if (isr.ready()) {
                try {
                    StringBuilder line = new StringBuilder();
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
                    return line.toString();
                } catch (IOException e) {
                    return "[GUI] Error receiving server data.";
                }
            }
        } catch (IOException e) {
            return "[GUI] Error receiving server data.";
        }
        return null;
    }

    public void send(String string) {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
            bw.write(string);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            System.out.println("[GUI] Error sending server data.");
        }
    }

    public boolean stop() {
        send("stop");
 
        System.out.println("[GUI] Waiting for process to end.");
        SwingWorker worker = new SwingWorker<Boolean, Integer>() {

            @Override
            public Boolean doInBackground() {
                try {
                    System.out.println("[GUI] Stopping server.");
                    ps.waitFor();
                    System.out.println("[GUI] Stopped server succesfully.");
                    return true;
                } catch (InterruptedException e) {
                    System.out.println("[GUI] Error stopping the server!");
                    return false;
                }
            }

            @Override
            public void done() {

            }
        };
        
    }

    private Process ps;
    private java.io.InputStream is;
    private InputStreamReader isr;
    private BufferedReader br;
    //private BufferedWriter bw;
}

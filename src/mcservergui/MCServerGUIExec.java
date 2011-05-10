/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

//import java.util.*;
import java.io.*;
//import java.net.*;

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
            bw = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
            //os = ps.getOutputStream();
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
                            System.out.println("IOException");
                        }
                    }
                    return line.toString();
                } catch (IOException e) {
                    return "Error receiving console.";
                }
            }
        } catch (IOException e) {
            return "Error receiving console.";
        }
        return null;
    }

    public void send(String string) {
        //string += "\r\n";
        try {
            bw.write(string);
            bw.flush();
            bw.close();
            System.out.println("Sent " + string + " to console.");
        } catch (IOException e) {
            System.out.println("Error sending to console.");
        }
    }

    public void stop() {
        this.send("stop");
        
    }


    //private StringBuffer sb;
    private int lastRead;
    private Process ps;
    private java.io.InputStream is;
    private InputStreamReader isr;
    private BufferedReader br;
    //private java.io.OutputStream os;
    //private BufferedWriter bw;
    private BufferedWriter bw;
    //private OutputStreamWriter bw;
}

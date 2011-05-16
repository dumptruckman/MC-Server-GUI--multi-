/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import java.io.*;
import java.util.Scanner;

/**
 *
 * @author Roton
 */
public class MCServerGUIFileHandler {
    MCServerGUIFileHandler(String aFileName, String aEncoding) {
        fEncoding = aEncoding;
        fFileName = aFileName;
    }


    void write() {
        Writer out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(fFileName), fEncoding);
            out.write(FIXED_TEXT);
        } catch (IOException e) {
            
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                
            }
        }
    }
    
    void read() {
    //log("Reading from file.");
    text = new StringBuilder();
    String NL = System.getProperty("line.separator");
    Scanner scanner = null;
    try {
        scanner = new Scanner(new FileInputStream(fFileName), fEncoding);
        while (scanner.hasNextLine()){
            text.append(scanner.nextLine() + NL);
        }
        realtext = text.toString();
    } catch (IOException e) {
        
    }
    finally{
            if (scanner != null) {
                scanner.close();
            }
    }
    //log("Text read in: " + text);
  }

    private String fFileName;
    private String fEncoding;
    private int fFilePosition;
    private StringBuilder text;
    public String realtext;
    private final String FIXED_TEXT = "But soft! what code in yonder program breaks?";
}

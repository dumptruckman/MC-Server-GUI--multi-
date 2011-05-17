/*
 * MCServerGUIFileHandler.java
 */

package mcservergui;

import java.io.*;
import java.util.Scanner;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIFileHandler {

    public MCServerGUIFileHandler(String aFileName, String aEncoding) {
        fEncoding = aEncoding;
        fFileName = aFileName;
        fFolderName = null;
    }

    public MCServerGUIFileHandler(String aFolderName) {
        fEncoding = null;
        fFileName = null;
        fFolderName = null;
    }

    /**
     * Writes data to the file passed in through the constructor
     *
     * @param data  a string containing the data to be written to the file.
     */
    public void write(String data) {
        Writer out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(fFileName), fEncoding);
            out.write(data);
        } catch (IOException e) {
            System.out.println("There was an error writing data to the file " + fFileName);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                System.out.println("There was an error closing the file " + fFileName);
            }
        }
    }

    /**
     * Reads data from the file passed in through the constructor.
     *
     * Retrieve this data with the getData() method.
     */
    public boolean read() {
        StringBuilder text = new StringBuilder();
        String NL = System.getProperty("line.separator");
        Scanner scanner = null;
        try {
            scanner = new Scanner(new FileInputStream(fFileName), fEncoding);
            System.out.println(fFileName);
            while (scanner.hasNextLine()){
                text.append(scanner.nextLine() + NL);
            }
            realtext = text.toString();
            scanner.close();
            return true;
        } catch (IOException e) {
            System.out.println("There was an error reading the file " + fFileName);
            return false;
        }
    }

    /**
     * This function returns all the data from the file as a String.
     *
     * @return data from file
     */
    public String getData() {
        return realtext;
    }


    private String fFileName;
    private String fEncoding;
    private String realtext;
    private String fFolderName;
}

/*
 * MCServerGUIConfig.java
 */

package mcservergui;

import org.codehaus.jackson.*;
import java.io.*;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIConfig {

    MCServerGUIConfig() {
        _windowTitle = "MC Server GUI";
        cmdLine = new CMDLine();
    }

    public void load() {
        File configFile = new File("guiconfig.json");

        // Check to make sure config file exist and if not, creates it.
        try {
            configFile.createNewFile();
        } catch (IOException e) {
            System.out.println("Error creating file guiconfig.json!");
            return;
        }

        //Read in the config file to string configData.
        StringBuilder temp = new StringBuilder();
        try {
            FileInputStream is = new FileInputStream("guiconfig.json");
            boolean eof = false;
            while(!eof) {
                try {
                    int Char = is.read();
                    if (Char != -1) {
                        //System.out.println(Char);
                        temp.append((char)Char);
                    } else {
                        eof = true;
                    }
                } catch (IOException e) {
                    System.out.println("poop");
                }
            }
            try {
                is.close();
            } catch (IOException e) {
                System.out.println("Error closing FileInputStream!");
            }
        } catch (FileNotFoundException e) {
            System.out.println("guiconfig.json not found!");
            return;
        }
        String configData = temp.toString();

        // Begin parsing json configData
        JsonFactory jf = new JsonFactory();
        try {
            JsonParser jp = jf.createJsonParser(configData);
            jp.nextToken();
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jp.getCurrentName();
                jp.nextToken();
                if ("Window Title".equals(fieldname)) {
                    System.out.println(jp.getText());
                    setWindowTitle(jp.getText());
                } else if ("CMD Line".equals(fieldname)) {
                    while (jp.nextToken() != JsonToken.END_OBJECT) {
                        String cmdlinefield = jp.getCurrentName();
                        jp.nextToken();
                        if ("Java Executable".equals(cmdlinefield)) {
                            cmdLine.setJavaExec(jp.getText());
                        } else if ("Bukkit".equals(cmdlinefield)) {
                            cmdLine.setBukkit(jp.getBooleanValue());
                        } else if ("Xmx".equals(cmdlinefield)) {
                            cmdLine.setXmx(jp.getText());
                        } else if ("Xincgc".equals(cmdlinefield)) {
                            cmdLine.setXincgc(jp.getBooleanValue());
                        }
                    }
                }
            }
            jp.close();
        } catch (IOException e) {
            System.out.println("Error creating JsonParser");
            return;
        }
    }
    
    public void save() {
        System.out.println("Saving...");
        JsonFactory jf = new JsonFactory();
        try {
            JsonGenerator jg = jf.createJsonGenerator(new File("guiconfig.json"), JsonEncoding.UTF8);
            jg.useDefaultPrettyPrinter();
            jg.writeStartObject();
            jg.writeStringField("Window Title", getWindowTitle());
            jg.writeObjectFieldStart("CMD Line");
            jg.writeStringField("Java Executable", cmdLine.getJavaExec());
            jg.writeBooleanField("Bukkit", cmdLine.getBukkit());
            jg.writeStringField("Xmx Memory", cmdLine.getXmx());
            jg.writeBooleanField("Xincgc", cmdLine.getXincgc());
            jg.writeEndObject();
            jg.writeEndObject();
            jg.close();
        } catch (IOException e) {
            System.out.println("Error saving guiconfig.json");
            return;
        }
    }

    
    private String _windowTitle;
    public CMDLine cmdLine;

    public String getWindowTitle() { return _windowTitle; }
    
    public void setWindowTitle(String s) { _windowTitle = s; }

    public class CMDLine {
        public CMDLine () {
            _javaExec = "Java";
            _bukkit = true;
            _xmx = "1024M";
            _xincgc = true;
        }

        private String _javaExec, _xmx;
        private boolean _bukkit, _xincgc;

        public String getJavaExec() { return _javaExec; }
        public String getXmx() { return _xmx; }
        public boolean getBukkit() { return _bukkit; }
        public boolean getXincgc() { return _xincgc; }

        public void setJavaExec(String s) { _javaExec = s; }
        public void setXmx(String s) { _xmx = s; }
        public void setBukkit(boolean b) { _bukkit = b; }
        public void setXincgc(boolean b) { _xincgc = b; }
    }

}

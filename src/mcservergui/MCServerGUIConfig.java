/*
 * MCServerGUIConfig.java
 */

package mcservergui;

import org.codehaus.jackson.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIConfig {

    MCServerGUIConfig() {
        _windowTitle = "MC Server GUI";
        cmdLine = new CMDLine();
    }

    public boolean load() {
        File configFile = new File("guiconfig.json");

        // Check to make sure config file exist and if not, creates it.
        try {
            configFile.createNewFile();
        } catch (IOException e) {
            System.out.println("Error creating file guiconfig.json!");
            return false;
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
                    System.out.println("Error reading in data");
                }
            }
            try {
                is.close();
            } catch (IOException e) {
                System.out.println("Error closing FileInputStream!");
            }
        } catch (FileNotFoundException e) {
            System.out.println("guiconfig.json not found!");
            return false;
        }
        String configData = temp.toString();

        // Begin parsing json configData
        JsonFactory jf = new JsonFactory();
        try {
            JsonParser jp = jf.createJsonParser(configData);
            jp.nextToken();
            if (jp.getCurrentToken() != null) {
                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    String fieldname = jp.getCurrentName();
                    jp.nextToken();
                    // General Config options
                    if ("Window Title".equals(fieldname)) {
                        setWindowTitle(jp.getText());
                    } else if ("CMD Line".equals(fieldname)) {
                        // CMD Line config options
                        while (jp.nextToken() != JsonToken.END_OBJECT) {
                            String cmdlinefield = jp.getCurrentName();
                            jp.nextToken();
                            if ("Java Executable".equals(cmdlinefield)) {
                                cmdLine.setJavaExec(jp.getText());
                            } else if ("Server Jar File".equals(cmdlinefield)) {
                                cmdLine.setServerJar(jp.getText());
                            } else if ("Bukkit".equals(cmdlinefield)) {
                                cmdLine.setBukkit(jp.getBooleanValue());
                            } else if ("Xmx".equals(cmdlinefield)) {
                                cmdLine.setXmx(jp.getText());
                            } else if ("Xincgc".equals(cmdlinefield)) {
                                cmdLine.setXincgc(jp.getBooleanValue());
                            } else if ("Extra Arguments".equals(cmdlinefield)) {
                                cmdLine.setExtraArgs(jp.getText());
                            }
                        }
                    }
                }
            } else {
                return false;
            }
            jp.close();
            return true;
        } catch (IOException e) {
            System.out.println("Error creating JsonParser");
            return false;
        }
    }
    
    public void save() {
        JsonFactory jf = new JsonFactory();
        try {
            JsonGenerator jg = jf.createJsonGenerator(new File("guiconfig.json"), JsonEncoding.UTF8);
            jg.useDefaultPrettyPrinter();
            jg.writeStartObject();
            //General Config Options
            jg.writeStringField("Window Title", getWindowTitle());
            jg.writeObjectFieldStart("CMD Line");
            // CMD Line Config Options
            jg.writeStringField("Java Executable", cmdLine.getJavaExec());
            jg.writeStringField("Server Jar File", cmdLine.getServerJar());
            jg.writeBooleanField("Bukkit", cmdLine.getBukkit());
            jg.writeStringField("Xmx Memory", cmdLine.getXmx());
            jg.writeBooleanField("Xincgc", cmdLine.getXincgc());
            jg.writeStringField("Extra Arguments", cmdLine.getExtraArgs());
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
            _serverJar = "craftbukkit.jar";
        }

        private String _javaExec, _xmx, _serverJar, _extraArgs;
        private boolean _bukkit, _xincgc;

        public String getJavaExec() { return _javaExec; }
        public String getXmx() { return _xmx; }
        public String getServerJar() { return _serverJar; }
        public String getExtraArgs() { return _extraArgs; }
        public boolean getBukkit() { return _bukkit; }
        public boolean getXincgc() { return _xincgc; }

        public void setJavaExec(String s) { _javaExec = s; }
        public void setXmx(String s) { _xmx = s; }
        public void setServerJar(String s) { _serverJar = s; }
        public void setExtraArgs(String s) { _extraArgs = s; }
        public void setBukkit(boolean b) { _bukkit = b; }
        public void setXincgc(boolean b) { _xincgc = b; }

        public List<String> getCmdLine() {
            List<String> cmdLine = new ArrayList<String>();
            
            cmdLine.add(getJavaExec());
            if (getBukkit()) {
                cmdLine.add("-Djline.terminal=jline.UnsupportedTerminal");
            }
            cmdLine.add("-Xmx" + getXmx());
            if (getXincgc()) {
                cmdLine.add("-Xincgc");
            }
            cmdLine.add("-jar");
            cmdLine.add(getServerJar());
            cmdLine.add("nogui");
            if (getBukkit()) {
                cmdLine.add("-d");
                cmdLine.add("\"yyyy-MM-dd HH:mm:ss\"");
            }

            return cmdLine;
        }
    }

}

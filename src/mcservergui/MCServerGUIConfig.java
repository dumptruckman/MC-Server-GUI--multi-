/*
 * MCServerGUIConfig.java
 */

package mcservergui;

import org.json.*;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIConfig {

    public MCServerGUIConfig(String aFileName, String aEncoding) {
        fh = new MCServerGUIFileHandler(aFileName, aEncoding);
        windowTitle = null;
        javaExec = null;
    }

    public boolean load() {
        if (fh.read()) {
            configData = fh.getData();
        } else {
            configData = "{ }";
        }
        JSONObject config;
        // Load the file data into json object
        try {
            config = new JSONObject(configData);
        } catch (JSONException e) {
            System.out.println("JSONObject not formatted correctly");
            return false;
        }

        JSONObject runLine;
        System.out.println("CMD Line".split("^$"));
        runLine = new JSONObject(config, "CMD Line".split(""));

        /*
        // Get config settings or set them to default if missing
        try {
            windowTitle = config.get("Window Title").toString();
        } catch (JSONException e) {
            try {
                config.put("Window Title", "MC Server GUI");
                windowTitle = "MC Server GUI";
            } catch (JSONException exc) {}
        } 
        try {
            javaExec = config.get("Java Executable").toString();
        } catch (JSONException e) {
            try {
                config.put("Java Executable", "java");
                javaExec = "java";
            } catch (JSONException exc) {}
        }*/

        // Write the json object back to a string and save it back to the file
        try {
            configData = config.toString(2);
        } catch (JSONException e) {}
        save();
        return true;
    }

    public void save() {
        fh.write(configData);
    }

    private MCServerGUIFileHandler fh;
    private String configData;
    public String windowTitle;
    public String javaExec;
}

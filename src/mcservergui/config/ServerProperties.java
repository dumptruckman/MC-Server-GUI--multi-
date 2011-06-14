/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.config;

import java.io.*;
import java.util.Observer;
import java.util.Observable;

/**
 *
 * @author dumptruckman
 */
public class ServerProperties implements Observer {

    public ServerProperties() {
        serverProps = new File("./server.properties");
        if (serverProps.exists()) {
            if (!serverProps.canRead()) {
                System.err.println("Error reading server.properties!");
                serverProps = null;
            } else {
                if (!serverProps.canWrite()) {
                    System.err.println("Error writing server.properties!");
                    serverProps = null;
                } else {
                    // Everything is good.
                }
            }
        } else {
            try {
                serverProps.createNewFile();
            } catch (IOException ioe) {
                System.err.println("server.properties is non-existant and could not be created!");
                serverProps = null;
            }
        }

        properties = new java.util.Properties();
        properties.setProperty("allow-flight", "false");
        properties.setProperty("allow-nether", "true");
        properties.setProperty("level-name", "world");
        properties.setProperty("level-seed", "");
        properties.setProperty("max-players", "20");
        properties.setProperty("online-mode", "true");
        properties.setProperty("pvp", "true");
        properties.setProperty("server-ip", "");
        properties.setProperty("server-port", "25566");
        properties.setProperty("spawn-animals", "true");
        properties.setProperty("spawn-monsters", "true");
        properties.setProperty("spawn-protection", "16");
        properties.setProperty("view-distance", "10");
        properties.setProperty("white-list", "false");

        try {
            readProps();
        } catch (IOException ioe) {
            System.err.println("Error reading server.properties.");
        }
        try {
            writeProps(); 
        } catch (IOException ioe) {
            System.err.println("Error updating server.properties!");
        }
        hasChanges = false;
    }

    @Override public void update(Observable o, Object arg) {
        if (arg.equals("serverStopped")) {
            if (hasChanges) {
                try {
                    writeProps();
                    hasChanges = false;
                } catch (IOException ioe) {
                    System.err.println("Error updating server.properties!");
                }
            }
        }
    }

    public void readProps() throws IOException {
        if (serverProps != null) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(serverProps));
                String line = "";

                while ((line = br.readLine()) != null) {
                    if (line.contains("=")) {
                        if (line.split("=").length > 1) {
                            if (properties.containsKey(line.split("=")[0])) {
                                properties.setProperty(line.split("=")[0], line.split("=")[1]);
                            }
                        } else {
                            if (properties.containsKey(line.split("=")[0])) {
                                properties.setProperty(line.split("=")[0], "");
                            }
                        }
                    }
                }
            } catch (FileNotFoundException fnfe) {
                throw new IOException("Could not find the file");
            } catch (IOException ioe) {
                throw new IOException("Error reading from file");
            } finally {
                if (br != null) {
                    br.close();
                }
            }
        } else {
            throw new IOException("server.properties is null!");
        }
    }

   public void writeProps() throws IOException {
        if (serverProps != null) {
            BufferedReader br = null;
            BufferedWriter bw = null;
            try {
                br = new BufferedReader(new FileReader(serverProps));
                java.util.List<String> other = new java.util.ArrayList<String>();
                java.util.Properties loadedprops = new java.util.Properties();
                String line = "";
                // Read data out of the server.propeties file
                while ((line = br.readLine()) != null) {
                    if (line.contains("=")) {
                        if (line.split("=").length > 1) {
                            loadedprops.setProperty(line.split("=")[0], line.split("=")[1]);
                        } else {
                            loadedprops.setProperty(line.split("=")[0], "");
                        }
                    } else {
                        other.add(line);
                    }
                }

                // Updating data with new values
                java.util.Iterator iterator = properties.keySet().iterator();
                while(iterator.hasNext()) {
                    String key = iterator.next().toString();
                    loadedprops.setProperty(key, properties.getProperty(key));
                }

                // Writing data back into server.properties
                bw = new BufferedWriter(new FileWriter(serverProps));
                for (int i = 0; i < other.size(); i++) {
                    bw.write(other.get(i));
                    bw.newLine();
                }
                iterator = loadedprops.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next().toString();
                    bw.write(key + "=" + loadedprops.getProperty(key));
                    bw.newLine();
                }
                hasChanges = false;
            } catch (FileNotFoundException fnfe) {
                throw new IOException("Could not find the file");
            } catch (IOException ioe) {
                throw new IOException("Error reading from file");
            } finally {
                if (br != null) {
                    br.close();
                }
                if (bw != null) {
                    bw.close();
                }
            }
        } else {
            throw new IOException("server.properties is null!");
        }
    }

    public boolean getAllowFlight() {
        if (properties.getProperty("allow-flight").equals("true")) {
            return true;
        }
        return false;
    }

    public boolean getAllowNether() {
        if (properties.getProperty("allow-nether").equals("true")) {
            return true;
        }
        return false;
    }

    public boolean getWhiteList() {
        if (properties.getProperty("white-list").equals("true")) {
            return true;
        }
        return false;
    }

    public int getViewDistance() {
        return Integer.valueOf(properties.getProperty("view-distance"));
    }

    public String getSpawnProtection() {
        return properties.getProperty("spawn-protection");
    }

    public boolean getSpawnAnimals() {
        if (properties.getProperty("spawn-animals").equals("true")) {
            return true;
        }
        return false;
    }

    public boolean getSpawnMonsters() {
        if (properties.getProperty("spawn-monsters").equals("true")) {
            return true;
        }
        return false;
    }

    public String getServerPort() {
        return properties.getProperty("server-port");
    }

    public String getServerIp() {
        return properties.getProperty("server-ip");
    }

    public boolean getPvp() {
        if (properties.getProperty("pvp").equals("true")) {
            return true;
        }
        return false;
    }

    public boolean getOnlineMode() {
        if (properties.getProperty("online-mode").equals("true")) {
            return true;
        }
        return false;
    }

    public int getMaxPlayers() {
        return Integer.valueOf(properties.getProperty("max-players"));
    }

    public String getLevelSeed() {
        return properties.getProperty("level-seed");
    }

    public String getLevelName() {
        return properties.getProperty("level-name");
    }

    public void setWhiteList(boolean whiteList) {
        properties.setProperty("white-list", Boolean.toString(whiteList));
        hasChanges = true;
    }

    public void setViewDistance(int viewDistance) {
        properties.setProperty("view-distance", Integer.toString(viewDistance));
        hasChanges = true;
    }

    public void setSpawnProtection(String spawnProtection) {
        properties.setProperty("spawn-protection", spawnProtection);
        hasChanges = true;
    }

    public void setSpawnMonsters(boolean spawnMonsters) {
        properties.setProperty("spawn-monsters", Boolean.toString(spawnMonsters));
        hasChanges = true;
    }

    public void setSpawnAnimals(boolean spawnAnimals) {
        properties.setProperty("spawn-animals", Boolean.toString(spawnAnimals));
        hasChanges = true;
    }

    public void setServerPort(String serverPort) {
        properties.setProperty("server-port", serverPort);
        hasChanges = true;
    }

    public void setServerIp(String serverIp) {
        properties.setProperty("server-ip", serverIp);
        hasChanges = true;
    }

    public void setPvp(boolean pvp) {
        properties.setProperty("pvp", Boolean.toString(pvp));
        hasChanges = true;
    }

    public void setOnlineMode(boolean onlineMode) {
        properties.setProperty("online-mode", Boolean.toString(onlineMode));
        hasChanges = true;
    }

    public void setMaxPlayers(int maxPlayers) {
        properties.setProperty("max-players", Integer.toString(maxPlayers));
        hasChanges = true;
    }

    public void setLevelSeed(String levelSeed) {
        properties.setProperty("level-seed", levelSeed);
        hasChanges = true;
    }

    public void setLevelName(String levelName) {
        properties.setProperty("level-name", levelName);
        hasChanges = true;
    }

    public void setAllowNether(boolean allowNether) {
        properties.setProperty("allow-nether", Boolean.toString(allowNether));
        hasChanges = true;
    }

    public void setAllowFlight(boolean allowFlight) {
        properties.setProperty("allow-flight", Boolean.toString(allowFlight));
        hasChanges = true;
    }

    private File serverProps;
    private boolean hasChanges;
    private java.util.Properties properties;
}

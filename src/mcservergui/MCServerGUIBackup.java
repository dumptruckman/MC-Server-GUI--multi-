/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import java.util.Observable;
import java.io.*;
import javax.swing.SwingWorker;
//import java.util.zip.*;

/**
 *
 * @author Roton
 */
public class MCServerGUIBackup extends Observable {

    public MCServerGUIBackup(MCServerGUIConfig newConfig) {
        config = newConfig;
        
    }

    public boolean startBackup() {
        System.out.println(config.cmdLine.getServerJar());
        String serverPath = new File(".\\" + config.cmdLine.getServerJar()).getParent();
        System.out.println(serverPath);
        if (serverPath != null) {
            backupWorker.execute();
            return true;
        } else {
            return false;
        }
    }

    private SwingWorker backupWorker = new SwingWorker<Boolean, Integer>() {
        @Override
        public Boolean doInBackground() {
            
            return true;
        }

        @Override
        public void done() {
            try {
                backupSuccess = this.get();
                setChanged();
                notifyObservers("finishedBackup");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (java.util.concurrent.ExecutionException e) {
                e.printStackTrace();
            }
        }
    };

    private MCServerGUIConfig config;
    private boolean backupSuccess;
}

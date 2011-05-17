/*
 * MCServerGUIServerStopper.java
 */

package mcservergui;

import javax.swing.SwingWorker;
import java.io.*;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIServerStopper extends SwingWorker<Boolean, Boolean> {
    public MCServerGUIServerStopper (Process newPs, BufferedReader newBr, OutputStreamWriter newOsw) {
        ps = newPs;
        br = newBr;
        osw = newOsw;
    }

    @Override
    public Boolean doInBackground() {
        try {
            ps.waitFor();
            return true;
        } catch (InterruptedException e) {
            System.out.println("ps.waitFor() interrupted!");
            return false;
        }
    }

    @Override
    public void done() {
        try {
            if (this.get() == true) {
                //ps = null;
                firePropertyChange("serverRunning", true, false);
                try {
                    // Close the io streams
                    br.close();
                    //br = null;
                    osw.close();
                    //osw = null;
                } catch (IOException e) {
                    System.out.println("Error stopping read streams");
                } finally {
                    //serverReceiver.cancel(true);
                }
            } else {
                System.out.println("Stop failed");
            }
        } catch (ExecutionException e) {
            System.out.println("Execution Exception");
        } catch (InterruptedException e) {
            System.out.println("Interrupted Exception");
        }
    }

    private Process ps;
    private BufferedReader br;
    private OutputStreamWriter osw;
}

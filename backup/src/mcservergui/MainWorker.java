/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Roton
 */
public class MainWorker {
    
    public MainWorker() {
        timer = new java.util.Timer();
        timer.schedule(new BackgroundWork(), 100);
    }

    class BackgroundWork extends TimerTask {
        public void run() {
            ServerExec.receive();
        }
    }

    public MCServerGUIExec ServerExec;
    Timer timer;
}

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
public class MCServerGUIMainWorker {

    public MCServerGUIMainWorker() {
        timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new BackgroundWork(), 0, 100);
    }

    class BackgroundWork extends TimerTask {
        public void run() {
        }
    }

    Timer timer;
}

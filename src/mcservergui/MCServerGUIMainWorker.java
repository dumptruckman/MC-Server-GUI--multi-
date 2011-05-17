/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIMainWorker {

    public MCServerGUIMainWorker(MCServerGUIServerModel newServer) {
        timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new BackgroundWork(), 0, 1000);
        Server = newServer;
    }

    class BackgroundWork extends TimerTask {
        public void run() {
        }
    }

    MCServerGUIServerModel Server;
    Timer timer;
}

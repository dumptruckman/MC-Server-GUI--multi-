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

    public MCServerGUIMainWorker(MCServerGUIServerModel newServer, MCServerGUIView newGui) {
        gui = newGui;
        server = newServer; 
        timer = new java.util.Timer();
    }

    public void startMainWorker() {
        timer.scheduleAtFixedRate(new BackgroundWork(), 100, 100);
    }

    class BackgroundWork extends TimerTask {
        public void run() {
            gui.scrollText();
        }
    }

    private MCServerGUIServerModel server;
    private MCServerGUIView gui;
    Timer timer;
}

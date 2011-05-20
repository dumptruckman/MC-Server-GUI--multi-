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
public class MCServerGUIPHPInterface {

    public MCServerGUIPHPInterface(MCServerGUIView newGui) {
        gui = newGui;
        timer = new java.util.Timer();
    }

    public void startPHPInterface() {
        timer.scheduleAtFixedRate(new BackgroundWork(), 0, 500);
    }

    class BackgroundWork extends TimerTask {
        public void run() {
            try {
                runner.waitFor();
            } catch (InterruptedException e) {
                System.out.println("pppp");
            }
        }
    }

    public static final String JAVABRIDGE_PORT="8087";
    static final php.java.bridge.JavaBridgeRunner runner = php.java.bridge.JavaBridgeRunner.getInstance(JAVABRIDGE_PORT);
    private MCServerGUIView gui;
    Timer timer;
}

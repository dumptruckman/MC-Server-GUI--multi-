/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import org.quartz.*;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUITask implements Job {

    @Override public void execute(JobExecutionContext context)
        throws JobExecutionException {
        MCServerGUIEvent event = (MCServerGUIEvent)context.getMergedJobDataMap().get("Event");
        MCServerGUIView gui = (MCServerGUIView)context.getMergedJobDataMap().get("GUI");

        if (!event.getWarningList().isEmpty()) {
            java.util.Collections.sort(event.getWarningList());
            for (int i = 0; i < event.getWarningList().size(); i++) {
                System.out.println("There is a warning.");
                gui.sendInput("say " + event.getWarningList().get(i).getMessage());
                int sleeptime;
                if (i+1 < event.getWarningList().size()) {
                    sleeptime = event.getWarningList().get(i).getTime() -
                            event.getWarningList().get(i+1).getTime();
                } else {
                    sleeptime = event.getWarningList().get(i).getTime();
                }
                try {
                    System.out.println("Sleeping for " + sleeptime);
                    Thread.sleep(sleeptime * 1000);
                } catch (InterruptedException ie) {
                    System.out.println("Warning sleep interrupted");
                }
            }
        }

        System.out.println("Proceeding to task");
        if (event.getTask().equals("Start Server")) {
            gui.startServer();
        } else if (event.getTask().equals("Send Command")) {
            gui.sendInput(event.getParams().get(0));
        } else if (event.getTask().equals("Stop Server")) {
            gui.stopServer();
        } else if (event.getTask().equals("Restart Server")) {
            gui.stopServer();
            System.out.println("Sent stop");
            if (!event.getParams().isEmpty()) {
                try {
                    System.out.println("Sleeping for " + Integer.valueOf(event.getParams().get(0)));
                    Thread.sleep(Integer.valueOf(event.getParams().get(0)) * 1000);
                } catch (InterruptedException ie) {
                    System.out.println("Interrupted while waiting to restart server.");
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                System.out.println("Interrupted while waiting required amount to restart server.");
            }
            gui.startServer();
            System.out.println("Sent start");
        } else if (event.getTask().equals("Backup")) {
            gui.backup();
        }
    }
}

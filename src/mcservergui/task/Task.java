/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.task;

import mcservergui.gui.GUI;
import org.quartz.*;
import java.util.List;
import java.util.ArrayList;
import mcservergui.task.event.EventModel;

/**
 *
 * @author dumptruckman
 */
public class Task implements Job {

    @Override public void execute(JobExecutionContext context)
        throws JobExecutionException {
        EventModel event = (EventModel)context.getMergedJobDataMap().get("Event");
        GUI gui = (GUI)context.getMergedJobDataMap().get("GUI");

        if (!event.getWarningList().isEmpty()) {
            java.util.Collections.sort(event.getWarningList());
            for (int i = 0; i < event.getWarningList().size(); i++) {
                gui.sendInput(event.getWarningList().get(i).getMessage());
                int sleeptime;
                if (i+1 < event.getWarningList().size()) {
                    sleeptime = event.getWarningList().get(i).getTime() -
                            event.getWarningList().get(i+1).getTime();
                } else {
                    sleeptime = event.getWarningList().get(i).getTime();
                }
                try {
                    Thread.sleep(sleeptime * 1000);
                } catch (InterruptedException ie) {
                    System.out.println("Warning sleep interrupted");
                }
            }
        }

        System.out.println("Proceeding to task");
        if (event.getTask().equals("Start Server")) {
            waitForBackupFinish(gui);
            gui.startServer();
        } else if (event.getTask().equals("Send Command")) {
            gui.sendInput(event.getParams().get(0));
        } else if (event.getTask().equals("Stop Server")) {
            waitForBackupFinish(gui);
            gui.stopServer();
        } else if (event.getTask().equals("Restart Server")) {
            waitForBackupFinish(gui);
            if (!event.getParams().isEmpty()) {
                gui.restartServer(Integer.valueOf(event.getParams().get(0)));
            } else {
                gui.restartServer();
            }
        } else if (event.getTask().equals("Backup")) {
            waitWhileRestarting(gui);
            gui.backup();
        } else if (event.getTask().equals("Save Worlds")) {
            waitWhileRestarting(gui);
            waitForBackupFinish(gui);
            gui.sendInput("save-all");
        }
    }

    private void waitForBackupFinish(GUI gui) {
        while (gui.getControlState().equals("BACKUP") || gui.getControlState().equals("!BACKUP")) {
            try {
                System.out.println("Waiting for server to finish backing up.");
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                System.out.println("Interrupted while waiting for server to finish backing up.");
            }
        }
    }
    private void waitWhileRestarting(GUI gui) {
        while (gui.isRestarting()) {
            try {
                System.out.println("Waiting while server is restarting.");
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                System.out.println("Interrupted while waiting for server to restart.");
            }
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import java.util.Date;
import java.util.Calendar;
import org.quartz.*;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUITasks implements JobListener {

    public MCServerGUITasks(MCServerGUIView gui) {
        this.gui = gui;
    }

    @Override public String getName() {
        return "Task Listener";
    }

    @Override public void jobToBeExecuted(JobExecutionContext context) {
        String jobname = context.getJobDetail().getKey().getName();
        if (jobname != null) {
            if(jobname.equals("Stop Server")) {
                System.out.println("Stopping server");
                gui.stopServer();
            }
        }
    }

    @Override public void jobExecutionVetoed(JobExecutionContext context) {
    }

    @Override public void jobWasExecuted(JobExecutionContext context,
            JobExecutionException jobException) {
    }

    public static class GuiTask implements Job {
        @Override public void execute(JobExecutionContext context)
            throws JobExecutionException { }
    }

    MCServerGUIView gui;
    Calendar taskTime;
}

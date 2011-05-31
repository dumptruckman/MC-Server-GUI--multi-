/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import org.quartz.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.JobBuilder.*;
import static org.quartz.DateBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static mcservergui.MCServerGUITask.*;
/**
 *
 * @author dumptruckman
 */
public class MCServerGUIEventScheduler {
    public static boolean scheduleEvent(MCServerGUIEvent event, Scheduler scheduler, MCServerGUIView gui) {
        JobDetail job;
        Trigger trigger;
        job = newJob(MCServerGUITask.class)
                .withIdentity(event.getName())
                .build();
        job.getJobDataMap().put("Event", event);
        job.getJobDataMap().put("GUI", gui);

        try {
            trigger = newTrigger()
                    .forJob(job)
                    .withSchedule(cronSchedule(event.getCronEx()))
                    .build();
            try {
                scheduler.scheduleJob(job, trigger);
                System.out.println(trigger.getNextFireTime());
                return true;
            } catch (SchedulerException se) {
                System.out.println("scheduling exception error");
                return false;
            }
        } catch (java.text.ParseException pe) {
            System.out.println("cron parsing error");
            return false;
        }
    }
}

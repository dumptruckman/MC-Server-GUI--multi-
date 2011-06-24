/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.task.event;

import mcservergui.task.event.EventModel;
import mcservergui.gui.GUI;
import org.quartz.*;
import mcservergui.task.Task;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.JobBuilder.*;
import static org.quartz.DateBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static mcservergui.task.Task.*;
/**
 *
 * @author dumptruckman
 */
public class EventScheduler {
    public static boolean scheduleEvent(EventModel event, Scheduler scheduler, GUI gui) {
        JobDetail job;
        Trigger trigger;
        job = newJob(Task.class)
                .withIdentity(event.getName())
                .withDescription(event.getName())
                .build();
        job.getJobDataMap().put("Event", event);
        job.getJobDataMap().put("GUI", gui);
        //job.getJobDataMap().put("Scheduler", scheduler);

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
    public static boolean scheduleImmediateEvent(EventModel event, Scheduler scheduler, GUI gui) {
        JobDetail job;
        Trigger trigger;
        job = newJob(Task.class)
                .withIdentity(event.getName())
                .build();
        job.getJobDataMap().put("Event", event);
        job.getJobDataMap().put("GUI", gui);
        //job.getJobDataMap().put("Scheduler", scheduler);

        trigger = newTrigger()
                .forJob(job)
                .startNow()
                .build();
        try {
            scheduler.scheduleJob(job, trigger);
            System.out.println(trigger.getNextFireTime());
            return true;
        } catch (SchedulerException se) {
            System.out.println("scheduling exception error");
            return false;
        }
    }
}

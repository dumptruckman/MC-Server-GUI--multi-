/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import mcservergui.mcserver.MCServerModel;
import mcservergui.gui.GUI;
import mcservergui.gui.AboutBox;
import mcservergui.task.event.EventModel;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;
import javax.swing.SwingUtilities;
import org.hyperic.sigar.*;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import static mcservergui.tools.TimeTools.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 *
 * @author dumptruckman
 */
public class MainWorker implements java.util.Observer {

    public MainWorker(GUI newGui, MCServerModel server) {
        gui = newGui;
        timer = new java.util.Timer();
        sigarImpl = new Sigar();
        version = org.jdesktop.application.Application
                .getInstance(mcservergui.Main.class).getContext()
                .getResourceMap(AboutBox.class).getString("Application.version");
        serverPid = 0;
        this.server = server;
        try {
            rxBytes = 0;
            txBytes = 0;
            for (int i = 0; i < sigarImpl.getNetInterfaceList().length; i++) {
                rxBytes += sigarImpl.getNetInterfaceStat(sigarImpl.getNetInterfaceList()[i]).getRxBytes();
                txBytes += sigarImpl.getNetInterfaceStat(sigarImpl.getNetInterfaceList()[i]).getTxBytes();
            }
        } catch (SigarException e) {
        } catch (UnsatisfiedLinkError ule) { }

    }

    @Override public void update(java.util.Observable o, Object arg) {
        if (arg.equals("pid")) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    serverPid = server.getPid();
                }
            });
        } else if (arg.equals("piderror")) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    serverPid = -1;
                }
            });
        }
    }

    public void startMainWorker() {
        timer.scheduleAtFixedRate(new MonitorUpdater(), 0, 1000);
        timer.scheduleAtFixedRate(new ScheduleChecker(), 0, 1000);
        timer.scheduleAtFixedRate(new UpdateChecker(), 0, 3600000);
    }

    class ScheduleChecker extends TimerTask {
        @SuppressWarnings("unchecked")
        @Override public void run() {
            class ListModeler implements Runnable {
                javax.swing.ListModel lm;

                public ListModeler() {
                    super();
                    lm = null;
                }

                @Override public void run() {
                    lm = gui.taskSchedulerList.getModel();
                }

                public javax.swing.ListModel getTaskSchedulerListModel() {
                    return lm;
                }
            }
            ListModeler lm = new ListModeler();
            try {
                SwingUtilities.invokeAndWait(lm);
            } catch (InterruptedException ie) {
                return;
            } catch (java.lang.reflect.InvocationTargetException ite) {
                return;
            }
            if (lm.getTaskSchedulerListModel() == null) {
                return;
            }
            java.util.Set<JobKey> keys = null;
            try {
                if (gui.getScheduler().isShutdown()) {
                    return;
                }
                keys = gui.getScheduler().getJobKeys(GroupMatcher
                        .groupContains("DEFAULT"));
            } catch (SchedulerException se) {
                se.printStackTrace();
            }
            if (keys == null) {
                return;
            }
            long currenttime = Calendar.getInstance().getTime().getTime();
            java.util.Iterator i = gui.config.schedule.getEvents().iterator();
            while (i.hasNext()) {
                EventModel event = (EventModel)i.next();
                if (event.isCustomButton()) {
                    continue;
                }
                java.util.Iterator j = keys.iterator();
                while (j.hasNext()) {
                    JobKey job = (JobKey)j.next();
                    if (!job.getName().equals(event.getName())) {
                        continue;
                    }
                    long comparetime = 0;
                    try {
                         comparetime = gui.getScheduler().getTriggersOfJob(job)
                                .get(0).getNextFireTime().getTime();
                    } catch (SchedulerException se) {
                        se.printStackTrace();
                    }
                    if (comparetime != 0) {
                        String timetilnextjob = daysHoursMinutesSecondsFromSeconds(
                                (int)((comparetime - currenttime)/1000));
                        event.setNextFireTime(timetilnextjob);
                    }
                }
                
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    gui.taskSchedulerList.updateUI();
                }
            });
        }
    }

    class UpdateChecker extends TimerTask {
        @Override public void run() {
            String urltext = "https://raw.github.com/dumptruckman/MC-Server-GUI--multi-/master/VERSION";
            try {
                URL url = new URL(urltext);
                HttpURLConnection huc =  (HttpURLConnection)  url.openConnection();
                huc.setRequestMethod("GET");
                huc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)");
                huc.connect();
                if (!(huc.getResponseCode() == 200)) {
                    return;
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(url
                                .openStream()));
                String inputLine = "";

                if ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals(version) || version.contains("-dev")) {
                    } else {
                        gui.outOfDate(inputLine);
                    }
                }
                in.close();
            } catch (java.net.MalformedURLException e) {

            } catch (IOException ioe) {
                
            }
        }
    }

    class MonitorUpdater extends TimerTask {
        @Override public void run() {
            gui.scrollText();
            try {
                gui.guiCpuUsage.setText(CpuPerc.format(sigarImpl.getProcCpu(
                        sigarImpl.getPid()).getPercent()/sigarImpl.getCpuList()
                        .length));
            } catch (SigarException e) {
                gui.guiCpuUsage.setText("Error");
            } catch (UnsatisfiedLinkError ule) { }

            try {
                if (serverPid > 0) {
                    gui.serverCpuUsage.setText(CpuPerc.format(sigarImpl.getProcCpu(
                            serverPid).getPercent()/sigarImpl.getCpuList().length));
                } else if (serverPid == 0) {
                    gui.serverCpuUsage.setText("N/A");
                } else if (serverPid == -1) {
                    gui.serverCpuUsage.setText("Error");
                }
            } catch (SigarException e) {
                gui.serverCpuUsage.setText("Error");
            } catch (UnsatisfiedLinkError ule) { }
            try {
                gui.guiMemoryUsage.setText(new java.text.DecimalFormat("##,###.##").format(
                        (double)(sigarImpl.getProcMem(sigarImpl.getPid()).getResident())/1024/1000)
                        + " M");
            } catch (SigarException e) {
                gui.guiMemoryUsage.setText("Error");
            } catch (UnsatisfiedLinkError ule) { }
            try {
                if (serverPid > 0) {
                    gui.serverMemoryUsage.setText(new java.text.DecimalFormat("##,###.##").format(
                            (double)(sigarImpl.getProcMem(serverPid).getResident())/1024/1000)
                            + " M"/* / " + new java.text.DecimalFormat("##,###.##").format(
                            (double)(sigarImpl.getProcMem(serverPid).getSize())/1024/1000)*/);
                    //System.out.println(sigarImpl.getProcMem(serverPid).getShare());
                } else if (serverPid == 0) {
                    gui.serverMemoryUsage.setText("N/A");
                } else if (serverPid == -1) {
                    gui.serverMemoryUsage.setText("Error");
                }
            } catch (SigarException e) {
                gui.serverMemoryUsage.setText("Error");
            } catch (UnsatisfiedLinkError ule) { }
            if (gui.useNetStat.isSelected()) {
                try {
                    long rxbytes = 0, txbytes = 0;
                    for (int i = 0; i < sigarImpl.getNetInterfaceList().length; i++) {
                        rxbytes += sigarImpl.getNetInterfaceStat(sigarImpl.getNetInterfaceList()[i]).getRxBytes();
                        txbytes += sigarImpl.getNetInterfaceStat(sigarImpl.getNetInterfaceList()[i]).getTxBytes();
                    }
                    int rxpersec = (int)(rxbytes - rxBytes);
                    int txpersec = (int)(txbytes - txBytes);
                    rxBytes = rxbytes;
                    txBytes = txbytes;
                    if (rxpersec < 1024) {
                        gui.receivingBytes.setText(rxpersec + " B/s");
                    } else if ((rxpersec >= 1024) && (rxpersec < 1048576)) {
                        gui.receivingBytes.setText((rxpersec / 1024) + " KB/s");
                    } else if (rxpersec >= 1048576) {
                        gui.receivingBytes.setText((rxpersec / 1048576) + " MB/s");
                    }
                    if (txpersec < 1024) {
                        gui.transmittingBytes.setText(txpersec + " B/s");
                    } else if ((txpersec >= 1024) && (txpersec < 1048576)) {
                        gui.transmittingBytes.setText((txpersec / 1024) + " KB/s");
                    } else if (txpersec >= 1048576) {
                        gui.transmittingBytes.setText((txpersec / 1048576) + " MB/s");
                    }
                } catch (SigarException e) {
                    System.out.println("Error");
                } catch (UnsatisfiedLinkError ule) { }
            } else {
                gui.receivingBytes.setText("?");
                gui.transmittingBytes.setText("?");
            }
        }
    }

    private GUI gui;
    private Timer timer;
    private Sigar sigarImpl;
    private long[] pids;
    private long rxBytes;
    private long txBytes;
    private SigarProxy sigar;
    private long serverPid;
    private MCServerModel server;
    private String version;
}

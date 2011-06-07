/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import java.util.Timer;
import java.util.TimerTask;
import org.hyperic.sigar.*;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIMainWorker implements java.util.Observer {

    public MCServerGUIMainWorker(MCServerGUIView newGui, MCServerGUIServerModel server) {
        gui = newGui;
        timer = new java.util.Timer();
        sigarImpl = new Sigar();
        //sigar = SigarProxyCache.newInstance(sigarImpl, SigarProxyCache.EXPIRE_DEFAULT);
       // ProcessFinder.
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
            serverPid = server.getPid();
        } else if (arg.equals("piderror")) {
            serverPid = -1;
        }
    }

    public void startMainWorker() {
        timer.scheduleAtFixedRate(new BackgroundWork(), 0, 1000);
    }

    class BackgroundWork extends TimerTask {
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

    private MCServerGUIView gui;
    private Timer timer;
    private Sigar sigarImpl;
    private long[] pids;
    private long rxBytes;
    private long txBytes;
    private SigarProxy sigar;
    private long serverPid;
    private MCServerGUIServerModel server;
}

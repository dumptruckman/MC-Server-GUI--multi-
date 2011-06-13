/*
 * Main.java
 */

package mcservergui;

import mcservergui.config.Config;
import mcservergui.mcserver.MCServerModel;
import mcservergui.gui.GUI;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import java.util.Observer;
import java.util.Observable;
import java.util.EventObject;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;


/**
 * The main class of the application.
 */
public class Main extends SingleFrameApplication implements Application.ExitListener, Observer {

    public Main() {
        config = new Config();

        wantsToQuit = false;
    }
    
    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        try {
            // Grab the Scheduler instance from the Factory
            scheduler =  new org.quartz.impl.StdSchedulerFactory().getScheduler();
            // and start it off
            scheduler.start();
        } catch (SchedulerException se) {
            se.printStackTrace();
        }
        addExitListener(this);
        server = new MCServerModel(config);
        show(gui = new GUI(this, server, config, scheduler));
        server.setGui(gui);
        gui.initConfig();
        server.addObserver(gui);
        server.addObserver(this);
        mainWorker = new MainWorker(gui, server);
        server.addObserver(mainWorker);
        gui.setMainWorker(mainWorker);
        mainWorker.startMainWorker();
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of Main
     */
    public static Main getApplication() {
        return Application.getInstance(Main.class);
    }

    /**
     * Informs the application that the server has stopped so that it may exit
     * @param o
     * @param arg
     */
    @Override public void update(Observable o, Object arg) {
        if ((arg.equals("serverStopped")) && (wantsToQuit)) {
            try {
                scheduler.shutdown();
            } catch (SchedulerException se) {
                se.printStackTrace();
            }
            System.exit(0);
        }
    }
    /**
     * Handles the exiting of the application
     * @param e is the EventObject
     * @return true if allowed to exit, false if not
     */
    @Override public boolean canExit(EventObject e) {
        gui.saveConfig();
        if (server.isRunning()) {
            wantsToQuit = true;
            gui.stopServer();
            return false;
        } else {
            try {
                scheduler.shutdown();
            } catch (SchedulerException se) {
                se.printStackTrace();
            }
            return true;
        }
    }

    /**
     *
     * @param e is the EventObject
     */
    @Override public void willExit(java.util.EventObject e) {
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(Main.class, args);
    }

    private boolean wantsToQuit;
    private GUI gui;
    private MCServerModel server;
    private Config config;
    private MainWorker mainWorker;
    private Scheduler scheduler;
}

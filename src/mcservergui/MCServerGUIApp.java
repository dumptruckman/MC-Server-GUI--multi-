/*
 * MCServerGUIApp.java
 */

package mcservergui;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import java.util.Observer;
import java.util.Observable;
import java.util.EventObject;


/**
 * The main class of the application.
 */
public class MCServerGUIApp extends SingleFrameApplication implements Application.ExitListener, Observer {

    public MCServerGUIApp() {
        config = new MCServerGUIConfig();
        server = new MCServerGUIServerModel(config);
        
        wantsToQuit = false;
    }
    
    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        addExitListener(this);
        show(gui = new MCServerGUIView(this, server, config));
        gui.initConfig();
        server.addObserver(gui);
        server.addObserver(this);
        mainWorker = new MCServerGUIMainWorker(gui);
        gui.setMainWorker(mainWorker);
        mainWorker.startMainWorker();
        phpInterface = new MCServerGUIPHPInterface(gui);
        phpInterface.startPHPInterface();
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
     * @return the instance of MCServerGUIApp
     */
    public static MCServerGUIApp getApplication() {
        return Application.getInstance(MCServerGUIApp.class);
    }

    /**
     * Informs the application that the server has stopped so that it may exit
     * @param o
     * @param arg
     */
    public void update(Observable o, Object arg) {
        if ((arg.equals("serverStopped")) && (wantsToQuit)) {
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
        launch(MCServerGUIApp.class, args);
    }

    private boolean wantsToQuit;
    private MCServerGUIView gui;
    private MCServerGUIServerModel server;
    private MCServerGUIConfig config;
    private MCServerGUIMainWorker mainWorker;
    private MCServerGUIPHPInterface phpInterface;
}

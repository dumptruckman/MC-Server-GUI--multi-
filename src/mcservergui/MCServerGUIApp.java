/*
 * MCServerGUIApp.java
 */

package mcservergui;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;


/**
 * The main class of the application.
 */
public class MCServerGUIApp extends SingleFrameApplication {

    public MCServerGUIApp() {
        Server = new MCServerGUIServerModel();
        
    }

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        GUI = new MCServerGUIView(this, Server);
        show(GUI);
        Server.addObserver(GUI);
        MainWorker = new MCServerGUIMainWorker(Server);
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
        root.addWindowListener(new MCServerGUIWindowListener(GUI));
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of MCServerGUIApp
     */
    public static MCServerGUIApp getApplication() {
        return Application.getInstance(MCServerGUIApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(MCServerGUIApp.class, args);
    }

    private MCServerGUIView GUI;
    private MCServerGUIServerModel Server;
    private MCServerGUIMainWorker MainWorker;
}

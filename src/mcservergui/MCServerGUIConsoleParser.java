/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIConsoleParser {

    public MCServerGUIConsoleParser(MCServerGUIConfig.Display display, MCServerGUIView gui) {
        this.display = display;
        this.gui = gui;
    }

    public String parseText(String text) {
        /*if (text.contains(" [INFO] Forcing save..")) {
            gui.setSaving(true);
        }
        if (text.contains(" [INFO] Save complete.")) {
            gui.setSaving(false);
        }*/
        text = text.replaceAll("<", "&lt;");
        text = text.replaceAll(">", "&gt;");

        return text;
    }

    private MCServerGUIConfig.Display display;
    private MCServerGUIView gui;
}

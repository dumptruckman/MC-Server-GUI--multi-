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

    public MCServerGUIConsoleParser(MCServerGUIConfig.Display display) {
        this.display = display;
    }

    public String parseText(String text) {
        text = text.replaceAll("<", "&lt;");
        text = text.replaceAll(">", "&gt;");

        //text = "<font size=2.5>" + text;

        return text;
    }

    private MCServerGUIConfig.Display display;
}

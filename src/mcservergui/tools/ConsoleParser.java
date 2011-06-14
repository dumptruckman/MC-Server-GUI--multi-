/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.tools;
import mcservergui.config.Config;
import mcservergui.gui.GUI;
import java.util.regex.Pattern;

/**
 *
 * @author dumptruckman
 */
public class ConsoleParser {

    public ConsoleParser(Config.Display display, GUI gui) {
        this.display = display;
        this.gui = gui;
    }

    public String parseText(String text) {
        String time = "";
        if (text.contains("["))  {
            if (text.indexOf("[") != 0) {
                time = text.substring(0, text.indexOf("[") - 1);
            }
            String date = "";
            if (time.contains(" ")) {
                date = time.split("\\s")[0];
                time = time.split("\\s")[1];
            }
            String tag = text.substring(text.indexOf("[") + 1, text.indexOf("]"));
            String message = text.substring(text.indexOf("]") + 2);

            message = message.replaceAll("<", "&lt;");
            message = message.replaceAll(">", "&gt;");
            message = message.replaceAll(System.getProperty("line.separator"), "<br>");
            if (tag.equals("INFO")) {
                tag = "[<font color = " + display.getInfoColor() + ">INFO</font>]";
                /* Player speech highlighting
                if (message.contains("&lt;") && message.contains("&gt;")) {

                } else if (message.contains (":")) {
                    if (!message.split(":")[0].contains(" ")) {
                        message.replaceFirst(message.split(":")[0], "<font color = "
                                + display.getInfoColor() + ">" + message.split(":")[0]
                                + "</font>");
                    }
                }
                */
                if (message.contains("[/")) {  // Possible player join
                    String[] splitmessage = message.split(" \\[/[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}:[\\d]{1,5}\\]");
                    System.out.println(splitmessage.length);
                    if (splitmessage.length > 1) {
                        String username = splitmessage[0];
                    }
                }
                if (message.contains(" lost connection: ")) {
                    //String username =
                }
            } else if (tag.equals("WARNING")) {
                tag = "[<font color = " + display.getWarningColor() + ">WARNING</font>]";
            } else if (tag.equals("SEVERE")) {
                tag = "[<font color = " + display.getSevereColor() + ">SEVERE</font>]";
                message = message.replaceFirst("NullPointerException",
                        "[<font color = " + display.getSevereColor() +
                        ">NullPointerException</font>]");
                message = message.replaceFirst("ConcurrentExecutionException",
                        "[<font color = " + display.getSevereColor() +
                        ">ConcurrentExecutionException</font>]");
            } else if (tag.equals("MC Server GUI")) {
                tag = "[" + tag + "]";
            }

            return  "<font color = \"" + display.getTextColor() + "\" size = "
                    + display.getTextSize() + ">" + date + " " + time + " " + tag
                    + " " + message + "</font>";
        } else {
            text = text.replaceAll("<", "&lt;");
            text = text.replaceAll(">", "&gt;");
            return "<font color = \"" + display.getTextColor() + "\" size = "
                    + display.getTextSize() + ">" + text + "</font>";
        }
    }

    private Config.Display display;
    private GUI gui;
}

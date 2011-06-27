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
        if (text.contains("[") && text.contains("]"))  {
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
                if (gui.config.getProxy() &&
                        message.contains("**** FAILED TO BIND TO PORT!")) {
                    gui.addTextToConsoleOutput(date + " " + time + " [WARNING] "
                            + "Are the Proxy Port and Server Port the same?  "
                            + "Check the Server Config tab!");
                }
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
            } else {
                tag = "[" + tag + "]";
            }
            text = "<font color = \"" + display.getTextColor() + "\" size = "
                    + display.getTextSize() + ">" + date + " " + time + " " + tag
                    + " " + message + "</font>";
        } else {
            text = text.replaceAll("<", "&lt;");
            text = text.replaceAll(">", "&gt;");
            text = "<font color = \"" + display.getTextColor() + "\" size = "
                    + display.getTextSize() + ">" + text + "</font>";
        }

        // Make sure the line has a <br> tag
        if (!text.contains("<br>")) {
            text += "<br>";
        }
        
        // Detect and replace color codes
        // First, ascii escape sequence colors
        int index;
        while ((index = text.indexOf(27)) != -1) {
            String afterescape = text.substring(index);
            int mindex = afterescape.indexOf("m");
            if ((mindex == -1) || (mindex > 4)) {
                // Not a color code
                text = text.replaceFirst(Character.toString((char)27), "");
            } else {
                // Is a color code
                int color = Integer.parseInt(afterescape.substring(2, mindex));
                String replace = "";
                switch (color) {
                    case 30:
                        replace = "<font color=000000>";
                        break;
                    case 31:
                        replace = "<font color=ff0000>";
                        break;
                    case 32:
                        replace = "<font color=00ff00>";
                        break;
                    case 33:
                        replace = "<font color=ffff00>";
                        break;
                    case 34:
                        replace = "<font color=0000ff>";
                        break;
                    case 35:
                        replace = "<font color=8e35ef>";
                        break;
                    case 36:
                        replace = "<font color=00ffff>";
                        break;
                    case 37:
                        replace = "<font color=808080>";
                        break;
                    case 0:
                        replace = "</font>";
                        break;
                }
                text = text.replaceFirst("\\Q" + text.substring(index, index+mindex+1) + "\\E", replace);
            }
        }
        // Second, minecraft colors
        while (((index = text.indexOf(65533)) != -1) || ((index = text.indexOf(167)) != -1)) {
            int color = -1;
            try {
                color = Integer.parseInt(text.substring(index+1, index+2), 16);
            } catch (NumberFormatException nfe) {
                // Not a color code
            }
            if (color != -1) {
                String replace = "";
                switch (color) {
                    case 0:
                        replace = "<font color=000000>";
                        break;
                    case 1:
                        replace = "<font color=0000bf>";
                        break;
                    case 2:
                        replace = "<font color=00bf00>";
                        break;
                    case 3:
                        replace = "<font color=00bfbf>";
                        break;
                    case 4:
                        replace = "<font color=bf0000>";
                        break;
                    case 5:
                        replace = "<font color=bf00bf>";
                        break;
                    case 6:
                        replace = "<font color=bfbf00>";
                        break;
                    case 7:
                        replace = "<font color=bfbfbf>";
                        break;
                    case 8:
                        replace = "<font color=404040>";
                        break;
                    case 9:
                        replace = "<font color=4040ff>";
                        break;
                    case 10:
                        replace = "<font color=40ff40>";
                        break;
                    case 11:
                        replace = "<font color=40ffff>";
                        break;
                    case 12:
                        replace = "<font color=ff4040>";
                        break;
                    case 13:
                        replace = "<font color=ff40ff>";
                        break;
                    case 14:
                        replace = "<font color=ffff40>";
                        break;
                    case 15:
                        replace = "</font>";
                        break;
                    case -1:
                        break;
                }
                text = text.replaceFirst(text.substring(index, index+2), replace);
            } else {
                text = text.replaceFirst(text.substring(index, index), "");
            }
        }
        return text;
    }

    private Config.Display display;
    private GUI gui;
}

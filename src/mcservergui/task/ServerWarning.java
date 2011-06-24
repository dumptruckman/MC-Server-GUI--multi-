/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.task;

import static mcservergui.tools.TimeTools.*;

/**
 *
 * @author dumptruckman
 */
public class ServerWarning implements Comparable<ServerWarning> {

    public ServerWarning() {}
    public ServerWarning(int i, String s) { this(s, i); }
    public ServerWarning(String s, int i) {
        time = i;
        message = s;
    }

    int time;
    String message;

    public int getTime() { return time; }
    public String getMessage() { return message; }

    public void setTime(int i) { time = i; }
    public void setMessage(String s) { message = s; }

    @Override public String toString() {
        String name = "";
        if (message.startsWith("say ")) {
            name = "Message: " + message.substring(4);
        } else {
            name = "Command: " + message;
        }
        name += "<br><font size=2>Time: " + hoursMinutesSecondsFromSeconds(time);
        return name;
    }

    @Override public int compareTo(ServerWarning o) {
        return o.getTime() - this.getTime();
    }
}

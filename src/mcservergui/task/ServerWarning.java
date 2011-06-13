/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.task;

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

    @Override public int compareTo(ServerWarning o) {
        return o.getTime() - this.getTime();
    }
}

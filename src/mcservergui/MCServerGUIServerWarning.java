/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIServerWarning implements Comparable<MCServerGUIServerWarning> {

    public MCServerGUIServerWarning() {}
    public MCServerGUIServerWarning(int i, String s) { this(s, i); }
    public MCServerGUIServerWarning(String s, int i) {
        time = i;
        message = s;
    }

    int time;
    String message;

    public int getTime() { return time; }
    public String getMessage() { return message; }

    public void setTime(int i) { time = i; }
    public void setMessage(String s) { message = s; }

    @Override public int compareTo(MCServerGUIServerWarning o) {
        return o.getTime() - this.getTime();
    }
}

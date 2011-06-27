/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.task.event;

import mcservergui.task.ServerWarning;
import mcservergui.listmodel.GUIListModel;

/**
 *
 * @author dumptruckman
 */
public class EventModel implements Comparable<EventModel> {

    public EventModel() {
        //warningList = new java.util.ArrayList<ServerWarning>();
        warningList = new GUIListModel<ServerWarning>();
        params = new java.util.ArrayList<String>();
        _isCustomButton = false;
        nextFireTime = null;
        name = "";
        task = "";
    }

    private String name, cronEx, task, nextFireTime;
    private java.util.List<String> params;
    //private java.util.List<ServerWarning> warningList;
    private GUIListModel<ServerWarning> warningList;
    private boolean _isCustomButton;

    public String getName() { return name; }
    public String getCronEx() { return cronEx; }
    public String getTask() { return task; }
    public java.util.List<String> getParams() { return params; }
    public GUIListModel<ServerWarning> getWarningList() { return warningList; }
    //public java.util.List<ServerWarning> getWarningList() { return warningList; }
    public boolean isCustomButton() { return _isCustomButton; }

    public void setName(String s) { name = s; }
    public void setCronEx(String s) { cronEx = s; }
    public void setTask(String s) { task = s; }
    public void setParams(java.util.List<String> sl) { params = sl; }
    public void setWarningList(GUIListModel<ServerWarning> l) { warningList = l; }
    //public void setWarningList(java.util.List<ServerWarning> l) { warningList = l; }
    public void setCustomButton(boolean b) { _isCustomButton = b; }

    public String getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(String s) {
        nextFireTime = s;
    }

    @Override public String toString() {
        //return name;
        if (name == null || task == null) {
            return "";
        }
        String string = name + "<br><font size=2>" + task;
        if (nextFireTime != null) {
            string += "  in  " + nextFireTime;
        }
        string += "</font>";
        return string;
    }

    @Override public int compareTo(EventModel e) {
        return this.getName().compareTo(e.getName());
    }
}

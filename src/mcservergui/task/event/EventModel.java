/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.task.event;

import mcservergui.task.ServerWarning;

/**
 *
 * @author dumptruckman
 */
public class EventModel {

    public EventModel() {
        warningList = new java.util.ArrayList<ServerWarning>();
        params = new java.util.ArrayList<String>();
    }

    private String name, cronEx, task;
    private java.util.List<String> params;
    private java.util.List<ServerWarning> warningList;

    public String getName() { return name; }
    public String getCronEx() { return cronEx; }
    public String getTask() { return task; }
    public java.util.List<String> getParams() { return params; }
    public java.util.List<ServerWarning> getWarningList() { return warningList; }

    public void setName(String s) { name = s; }
    public void setCronEx(String s) { cronEx = s; }
    public void setTask(String s) { task = s; }
    public void setParams(java.util.List<String> sl) { params = sl; }
    public void setWarningList(java.util.List<ServerWarning> l) { warningList = l; }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIEvent {

    public MCServerGUIEvent() {
        warningList = new java.util.ArrayList<java.util.List>();
    }

    private String name, cronEx, task;
    private java.util.List<String> params;
    private java.util.List<java.util.List> warningList;

    public String getName() { return name; }
    public String getCronEx() { return cronEx; }
    public String getTask() { return task; }
    public java.util.List<String> getParams() { return params; }
    public java.util.List<java.util.List> getWarningList() { return warningList; }

    public void setName(String s) { name = s; }
    public void setCronEx(String s) { cronEx = s; }
    public void setTask(String s) { task = s; }
    public void setParams(java.util.List<String> sl) { params = sl; }
    public void setWarningList(java.util.List<java.util.List> l) { warningList = l; }
}

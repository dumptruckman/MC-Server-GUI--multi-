/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.tools;

/**
 *
 * @author dumptruckman
 */
public class CronMath {
    public static String subtractSeconds(int s, String cronex) {
        int minute = s / 60;
        int second = s % 60;
        int hour = minute / 60;
        minute = minute % 60;
        //int day = hour / 24;
        //hour = hour % 24;
        String csecond = cronex.split("\\s")[0];
        String cminute = cronex.split("\\s")[1];
        String chour = cronex.split("\\s")[2];
        String cdom = cronex.split("\\s")[3];
        String cmon = cronex.split("\\s")[4];
        String cdow = cronex.split("\\s")[5];
        
        return cronex;
    }
    public static String subtractSeconds(String c, int s) {
        return subtractSeconds(s, c);
    }
}

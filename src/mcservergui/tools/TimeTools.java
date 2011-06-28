/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.tools;

import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 *
 * @author dumptruckman
 */
public class TimeTools {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    public static String hmsFromSeconds(int second) {
        int minute = second / 60;
        second = second % 60;
        int hour = minute / 60;
        minute = minute % 60;
        String time = "";
        if (hour != 0) {
            time += hour + "h ";
        }
        if (minute != 0) {
            time += minute + "m ";
        }
        if (second!= 0) {
            time += second + "s";
        }
        return time;
    }

    public static String hoursMinutesSecondsFromSeconds(int second) {
        int minute = second / 60;
        second = second % 60;
        int hour = minute / 60;
        minute = minute % 60;
        String time = "";
        if (hour != 0) {
            time += hour;
        }
        if (hour == 1) {
            time += " hour ";
        } else if (hour > 1) {
            time += " hours ";
        }
        if (minute != 0) {
            time += minute;
        }
        if (minute == 1) {
            time += " minute ";
        } else if (minute > 1) {
            time += " minutes ";
        }
        if (second!= 0) {
            time += second;
        }
        if (second == 1) {
            time += " second";
        } else if (second > 1) {
            time += " seconds";
        }
        return time;
    }

    public static String daysHoursMinutesSecondsFromSeconds(int second) {
        if (second == 0) {
            return "0 seconds";
        } else {
            int minute = second / 60;
            second = second % 60;
            int hour = minute / 60;
            minute = minute % 60;
            int day = hour / 24;
            hour = hour % 24;
            String time = "";
            if (day != 0) {
                time += day;
            }
            if (day == 1) {
                time += " day ";
            } else if (day > 1) {
                time += " days ";
            }
            if (hour != 0) {
                time += hour;
            }
            if (hour == 1) {
                time += " hour ";
            } else if (hour > 1) {
                time += " hours ";
            }
            if (minute != 0) {
                time += minute;
            }
            if (minute == 1) {
                time += " minute ";
            } else if (minute > 1) {
                time += " minutes ";
            }
            if (second != 0) {
                time += second;
            }
            if (second == 1) {
                time += " second";
            } else if (second > 1) {
                time += " seconds";
            }
            return time;
        }
    }

    public static int secondsFromHms(String hms) {
        int seconds = 0, minutes = 0, hours = 0;
        if (hms.contains("h")) {
            hours = Integer.parseInt(hms.split("h")[0].replaceAll(" ", ""));
            if (hms.contains("m") || hms.contains("s")) {
                hms = hms.split("h")[1];
            }
        }
        if (hms.contains("m")) {
            minutes = Integer.parseInt(hms.split("m")[0].replaceAll(" ", ""));
            if (hms.contains("s")) {
                hms = hms.split("m")[1];
            }
        }
        if (hms.contains("s")) {
            seconds = Integer.parseInt(hms.split("s")[0].replaceAll(" ", ""));
        }
        return (hours * 3600) + (minutes * 60) + seconds;
    }

    public static int secondsFromHoursMinutesSeconds(String hms) {
        int seconds = 0, minutes = 0, hours = 0;
        if (hms.contains("hours")) {
            hours = Integer.parseInt(hms.split("hours")[0].replaceAll(" ", ""));
            if (hms.contains("minutes") || hms.contains("seconds") || hms.contains("minute") || hms.contains("second")) {
                hms = hms.split("hours")[1];
            }
        } else if (hms.contains("hour")) {
            hours = Integer.parseInt(hms.split("hour")[0].replaceAll(" ", ""));
            if (hms.contains("minutes") || hms.contains("seconds") || hms.contains("minute") || hms.contains("second")) {
                hms = hms.split("hour")[1];
            }
        }
        if (hms.contains("minutes")) {
            minutes = Integer.parseInt(hms.split("minutes")[0].replaceAll(" ", ""));
            if (hms.contains("seconds") || hms.contains("second")) {
                hms = hms.split("minutes")[1];
            }
        } else if (hms.contains("minute")) {
            minutes = Integer.parseInt(hms.split("minute")[0].replaceAll(" ", ""));
            if (hms.contains("seconds") || hms.contains("second")) {
                hms = hms.split("minute")[1];
            }
        }
        if (hms.contains("seconds")) {
            seconds = Integer.parseInt(hms.split("seconds")[0].replaceAll(" ", ""));
        } else if (hms.contains("second")) {
            seconds = Integer.parseInt(hms.split("second")[0].replaceAll(" ", ""));
        }
        return (hours * 3600) + (minutes * 60) + seconds;
    }

    public static String getTimeStamp() {
        return new SimpleDateFormat(DATE_FORMAT_NOW)
                .format(Calendar.getInstance().getTime());
    }
}

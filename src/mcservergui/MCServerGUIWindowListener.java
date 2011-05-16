/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author Billing Manager
 */
public class MCServerGUIWindowListener extends WindowAdapter{
    public MCServerGUIWindowListener (MCServerGUIView newView) {
        GUI = newView;
    }
    @Override
    public void windowClosing(WindowEvent e) {
        System.out.println(GUI.getFrame().getDefaultCloseOperation());
        System.out.println("Window Closing");
    }

    MCServerGUIView GUI;
}

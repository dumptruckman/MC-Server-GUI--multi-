/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

/**
 *
 * @author dumptruckman
 */
public class MCServerGUIRegexVerifier extends javax.swing.InputVerifier {
    public MCServerGUIRegexVerifier(String regex) {
        this.regex = regex;
    }

    @Override public boolean verify(javax.swing.JComponent input) {
        javax.swing.JTextField tf = (javax.swing.JTextField) input;
        if (java.util.regex.Pattern.matches(regex, tf.getText())){
            return true;
        } else {
            tf.getActionMap().get("postTip").actionPerformed(
                    new java.awt.event.ActionEvent(
                    tf, java.awt.event.ActionEvent.ACTION_PERFORMED, "postTip"));
            return false;
        }
    }

    String regex;
}

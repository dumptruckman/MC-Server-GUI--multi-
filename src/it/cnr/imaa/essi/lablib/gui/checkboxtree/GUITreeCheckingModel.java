/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.imaa.essi.lablib.gui.checkboxtree;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import mcservergui.gui.GUI;

/**
 *
 * @author Billing Manager
 */
public class GUITreeCheckingModel extends DefaultTreeCheckingModel {

    public GUITreeCheckingModel(TreeModel model, GUI gui) {
        super(model);
        this.checkingMode = new GUITreeCheckingMode(this, gui);
    }
}

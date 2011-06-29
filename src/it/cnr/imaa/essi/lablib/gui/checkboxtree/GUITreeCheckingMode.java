/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.imaa.essi.lablib.gui.checkboxtree;

//import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingMode;

import javax.swing.tree.TreePath;
import javax.swing.SwingUtilities;

import mcservergui.gui.GUI;

import org.jdesktop.application.TaskMonitor;
import org.jdesktop.application.TaskService;
import org.jdesktop.application.Task;

//import it.cnr.imaa.essi.lablib.gui.checkboxtree.DefaultTreeCheckingModel;

/**
 *
 * @author dumptruckman
 */
public class GUITreeCheckingMode extends PropagatePreservingCheckTreeCheckingMode {

    private GUI gui;

    public GUITreeCheckingMode(DefaultTreeCheckingModel model, GUI gui) {
        super(model);
        this.gui = gui;
    }

    @Override
    public void checkPath(TreePath pth) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                gui.setPropagatingChecks(true);
                gui.backupButton.setEnabled(false);
                gui.backupFileChooser.setEnabled(false);
                gui.statusBarJob.setText("Checking Paths");
            }
        });
        
        final TreePath path = pth;
        class CheckTask extends Task {

            public CheckTask(org.jdesktop.application.Application app) {
                super(app);
            }

            @Override protected Void doInBackground() {
                // check is propagated to children
                //GUITreeCheckingMode.this.model.checkSubTree(path);
                GUITreeCheckingMode.this.model.addToCheckedPathsSet(path);
                /*
                if (path.getParentPath() != null) {
                    if (GUITreeCheckingMode.this.model.isPathChecked(path.getParentPath())) {
                        System.out.println("Unchecking parent");
                        GUITreeCheckingMode.this.uncheckPath(path.getParentPath());
                        TreePath[] children = GUITreeCheckingMode.this.model.getChildrenPath(path.getParentPath());
                        for (int i = 0; i < children.length; i++) {
                            GUITreeCheckingMode.this.model.addToCheckedPathsSet(children[i]);
                        }
                        GUITreeCheckingMode.this.uncheckPath(path);
                    }
                }*/
                //if (GUITreeCheckingMode.this.model.getChildrenPath(path).length != 0) {
                    //System.out.println(GUITreeCheckingMode.this.model.hasDifferentChildren(path));
                    /*if (!GUITreeCheckingMode.this.model.hasDifferentChildren(path)) {
                        System.out.println("Needs to uncheck children");
                        TreePath[] children = GUITreeCheckingMode.this.model.getChildrenPath(path);
                        for (int i = 0; i < children.length; i++) {
                            if (GUITreeCheckingMode.this.model.isPathChecked(children[i])) {
                                System.out.println("Unchecking child: " + children[i].getLastPathComponent().toString());
                                GUITreeCheckingMode.this.uncheckPath(children[i]);
                            }
                        }
                    }*/
                TreePath[] children = GUITreeCheckingMode.this.model.getChildrenPath(path);
                for (int i = 0; i < children.length; i++) {
                    GUITreeCheckingMode.this.model.addToCheckedPathsSet(children[i]);
                }
                //}
                /*
                  *
                // check all the ancestors with subtrees checked
                TreePath[] parents = new TreePath[path.getPathCount()];
                parents[0] = path;
                boolean uncheckAll = false;
                boolean greyAll = false;
                for (int i = 1; i < parents.length; i++) {
                    parents[i] = parents[i - 1].getParentPath();
                    if (uncheckAll) {
                        GUITreeCheckingMode.this.model.removeFromCheckedPathsSet(parents[i]);
                        if (greyAll) {
                            GUITreeCheckingMode.this.model.addToGreyedPathsSet(parents[i]);
                        } else {
                            if (GUITreeCheckingMode.this.model.pathHasUncheckedChildren(parents[i])) {
                                GUITreeCheckingMode.this.model.addToGreyedPathsSet(parents[i]);
                                greyAll = true;
                            } else {
                                GUITreeCheckingMode.this.model.removeFromGreyedPathsSet(parents[i]);
                            }
                        }
                    } else {
                        switch (GUITreeCheckingMode.this.model.getChildrenChecking(parents[i])) {
                        case HALF_CHECKED:
                            GUITreeCheckingMode.this.model.removeFromCheckedPathsSet(parents[i]);
                            GUITreeCheckingMode.this.model.addToGreyedPathsSet(parents[i]);
                            uncheckAll = true;
                            greyAll = true;
                            break;
                        case ALL_UNCHECKED:
                            GUITreeCheckingMode.this.model.removeFromCheckedPathsSet(parents[i]);
                            GUITreeCheckingMode.this.model.removeFromGreyedPathsSet(parents[i]);
                            uncheckAll = true;
                            break;
                        case ALL_CHECKED:
                            GUITreeCheckingMode.this.model.addToCheckedPathsSet(parents[i]);
                            GUITreeCheckingMode.this.model.removeFromGreyedPathsSet(parents[i]);
                            break;
                        default:
                        case NO_CHILDREN:
                            System.err.println("This should not happen (PropagatePreservingCheckTreeCheckingMode)");
                            break;
                        }
                    }
                }*/
                return null;
            }

            @Override
            protected void finished() {
                super.finished();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        gui.backupButton.setEnabled(true);
                        gui.statusBarJob.setText("");
                        gui.backupFileChooser.updateUI();
                        gui.backupFileChooser.setEnabled(true);
                        gui.setPropagatingChecks(false);
                        gui.saveBackupPathsToConfig();
                    }
                });
            }
        };
        TaskMonitor taskMonitor = gui.getApplication().getContext().getTaskMonitor();
        TaskService taskService = gui.getApplication().getContext().getTaskService();
        CheckTask checkTask = new CheckTask(gui.getApplication());
        taskMonitor.setForegroundTask(checkTask);
        taskService.execute(checkTask);
    }

    
    @Override
    public void uncheckPath(TreePath pth) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                gui.setPropagatingChecks(true);
                gui.backupButton.setEnabled(false);
                gui.backupFileChooser.setEnabled(false);
                gui.statusBarJob.setText("Un-Checking Paths");
            }
        });
        final TreePath path = pth;
        class UnCheckTask extends Task {

            public UnCheckTask(org.jdesktop.application.Application app) {
                super(app);
            }

            @Override protected Void doInBackground() {
                // uncheck is propagated to children
                //GUITreeCheckingMode.this.model.uncheckSubTree(path);
                GUITreeCheckingMode.this.model.removeFromCheckedPathsSet(path);
                if (path.getParentPath() != null) {
                    GUITreeCheckingMode.this.model.removeFromCheckedPathsSet(path.getParentPath());
                    //TreePath parent = path.getParentPath();
                    //if (!GUITreeCheckingMode.this.model.pathHasCheckedChildren(parent)) {
                
                    //}
                }
                TreePath[] children = GUITreeCheckingMode.this.model.getChildrenPath(path);
                for (int i = 0; i < children.length; i++) {
                    GUITreeCheckingMode.this.model.removeFromCheckedPathsSet(children[i]);
                }
                /*
                //this.model.removeFromCheckedPathsSet(path);
                TreePath parentPath = path;
                // uncheck is propagated to parents, too
                while ((parentPath = parentPath.getParentPath()) != null) {
                    GUITreeCheckingMode.this.model.removeFromCheckedPathsSet(parentPath);
                    GUITreeCheckingMode.this.model.updatePathGreyness(parentPath);
                }
                 * 
                 */
                return null;
            }

            @Override
            protected void finished() {
                super.finished();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        gui.backupButton.setEnabled(true);
                        gui.statusBarJob.setText("");
                        gui.backupFileChooser.updateUI();
                        gui.backupFileChooser.setEnabled(true);
                        gui.setPropagatingChecks(false);
                        gui.saveBackupPathsToConfig();
                    }
                });
            }
        }
        TaskMonitor taskMonitor = gui.getApplication().getContext().getTaskMonitor();
        TaskService taskService = gui.getApplication().getContext().getTaskService();
        UnCheckTask unCheckTask = new UnCheckTask(gui.getApplication());
        taskMonitor.setForegroundTask(unCheckTask);
        taskService.execute(unCheckTask);
    }

    /*
    @Override
    public void updateCheckAfterChildrenRemoved(TreePath path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateCheckAfterStructureChanged(TreePath path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateCheckAfterChildrenInserted(TreePath path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
     * 
     */
}

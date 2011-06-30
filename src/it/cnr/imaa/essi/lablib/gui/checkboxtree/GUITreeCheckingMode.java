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

    public static int activeCheckings;
    public static boolean isChecking;

    private GUI gui;

    public GUITreeCheckingMode(DefaultTreeCheckingModel model, GUI gui) {
        super(model);
        this.gui = gui;
        activeCheckings = 0;
    }

    @Override
    public void checkPath(TreePath pth) {
        final TreePath path = pth;
        activeCheckings += 1;
        System.out.println(path.getLastPathComponent().toString());
        while (isChecking) {
            try {
                Thread.sleep (10);
            } catch (InterruptedException e) {

            }
        }
        isChecking = true;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                //gui.setPropagatingChecks(true);
                gui.setPropagatingChecks(true);
                gui.backupButton.setEnabled(false);
                gui.backupFileChooser.setEnabled(false);
                gui.statusBarJob.setText("Checking Paths");
                TaskMonitor taskMonitor = gui.getApplication().getContext().getTaskMonitor();
                TaskService taskService = gui.getApplication().getContext().getTaskService();
                CheckTask checkTask = new CheckTask(gui.getApplication(), path);
                taskMonitor.setForegroundTask(checkTask);
                taskService.execute(checkTask);
            }
        });
    }

    
    @Override
    public void uncheckPath(TreePath pth) {
        TaskMonitor taskMonitor = gui.getApplication().getContext().getTaskMonitor();
        TaskService taskService = gui.getApplication().getContext().getTaskService();
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
                GUITreeCheckingMode.this.model.uncheckSubTree(path);
                TreePath parentPath = path;
                // uncheck is propagated to parents, too
                while ((parentPath = parentPath.getParentPath()) != null) {
                    GUITreeCheckingMode.this.model.removeFromCheckedPathsSet(parentPath);
                    GUITreeCheckingMode.this.model.updatePathGreyness(parentPath);
                }
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
        
        UnCheckTask unCheckTask = new UnCheckTask(gui.getApplication());
        taskMonitor.setForegroundTask(unCheckTask);
        taskService.execute(unCheckTask);
    }


    class CheckTask extends Task {

        private TreePath path;

        public CheckTask(org.jdesktop.application.Application app, TreePath path) {
            super(app);
            this.path = path;
        }

        @Override protected Void doInBackground() {
            // check is propagated to children
            GUITreeCheckingMode.this.model.checkSubTree(path);
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
            }
            return null;
        }

        @Override
        protected void finished() {
            super.finished();
            activeCheckings -= 1;
            isChecking = false;
            System.out.println(activeCheckings);
            if (GUITreeCheckingMode.this.activeCheckings == 0) {
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
    }
}

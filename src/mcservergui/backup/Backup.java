/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.backup;

import java.util.Observable;
import java.io.*;
import javax.swing.SwingWorker;
import java.util.zip.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLDocument;
import mcservergui.config.Config;
import mcservergui.gui.GUI;
import org.jdesktop.application.Task;

/**
 *
 * @author Roton
 */
public class Backup extends Observable {

    public Backup(GUI gui, javax.swing.JTextPane backupLog) {
        this.config = gui.config;
        task = new BackupTask(gui.getApplication());
        this.backupLog = backupLog;
        nl = System.getProperty("line.separator");
        fs = System.getProperty("file.separator");
        try {
            workingDir = new File(".").getCanonicalPath();
        } catch (IOException ioe) {
            System.out.println("Error retrieving working dir");
        }
    }

    public void startBackup() {
        task.execute();
    }

    public void addTextToBackupLog(String textToAdd) {
        try
        {
            ((HTMLEditorKit)backupLog.getEditorKit())
                    .insertHTML((HTMLDocument)backupLog.getDocument(),
                    backupLog.getDocument().getEndPosition().getOffset()-1,
                    textToAdd,
                    1, 0, null);
        } catch ( Exception e ) {
            System.out.println("Error appending text to console output");
        }
        backupLog.setCaretPosition(backupLog.getDocument().getLength());
    }

    public BackupTask getTask() {
        return task;
    }

    private class BackupTask extends Task {

        public BackupTask(org.jdesktop.application.Application app) {
            super(app);
        }

        @Override protected Boolean doInBackground() {
            message("startMessage");
            if (!config.backups.getPathsToBackup().isEmpty()) {
                // Copy paths to backup
                String now = java.util.Calendar.getInstance().getTime().toString().replaceAll(":", ".");
                File backupfolder = new File(config.backups.getPath() + fs + now);
                if (backupfolder.mkdir()) {
                    addTextToBackupLog("Created backup folder " + backupfolder.toString() + nl);
                } else {
                    addTextToBackupLog("<font color=red>Failed to create backup folder " + backupfolder.toString() + nl);
                    message("errorMessage");
                    return false;
                }
                // Perform the backup
                for (int i = 0 ; i < config.backups.getPathsToBackup().size(); i++) {
                    backup(new File(config.backups.getPathsToBackup().get(i)), backupfolder);
                }
                // Delete the server log if set to do so
                if (config.backups.getClearLog()) {
                    addTextToBackupLog("Deleting server.log...");
                    if (new File("./server.log").delete()) {
                        addTextToBackupLog("<font color=green>Success!");
                    } else {
                        addTextToBackupLog("<font color=red>Failed!");
                    }
                }

                // Compression
                if (config.backups.getZip()) {
                    addTextToBackupLog(nl + "<br>Zipping backup folder to " + backupfolder.getName() + ".zip...");
                    try {
                        ZipOutputStream zipout = new ZipOutputStream(
                                new BufferedOutputStream(
                                new FileOutputStream(
                                config.backups.getPath() + fs + backupfolder.getName() + ".7z")));
                        depth = 0;
                        addDirToZip(backupfolder, zipout);
                        addTextToBackupLog(nl + "Finished compiling " + backupfolder.getName() + ".7z" + nl);
                        try {
                            zipout.close();
                            deleteDir(backupfolder);
                        } catch (IOException e) {
                            addTextToBackupLog(nl + "<font color=green>Successfully created " + backupfolder.getName() + ".zip!" + nl);
                        }
                    } catch (FileNotFoundException e) {
                        addTextToBackupLog("<font color=red>failure! Could not find file to compress!" + nl);
                        message("errorMessage");
                        return true;
                    }
                } else {
                    addTextToBackupLog(nl + "Backup compression disabled...skipping." + nl);
                }
                return true;
            } else {
                addTextToBackupLog("Nothing selected to backup!" + nl);
                return true;
            }
        }

        @Override protected void finished() {
            try {
                backupSuccess = (Boolean)get();
                if (backupSuccess) {
                    addTextToBackupLog(nl + "<font color=green>Backup operation completed succesfully!");
                } else {
                    addTextToBackupLog(nl + "<font color=red>Backup operation encountered an error.  Aborting.");
                }
                setChanged();
                notifyObservers("finishedBackup");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (java.util.concurrent.ExecutionException e) {
                e.printStackTrace();
            }
            super.finished();
        }
    }
   /* private SwingWorker backupWorker = new SwingWorker<Boolean, Integer>() {
        @Override public Boolean doInBackground() {
            if (!config.backups.getPathsToBackup().isEmpty()) {
                // Copy paths to backup
                String now = java.util.Calendar.getInstance().getTime().toString().replaceAll(":", ".");
                File backupfolder = new File(config.backups.getPath() + fs + now);
                if (backupfolder.mkdir()) {
                    addTextToBackupLog("Created backup folder " + backupfolder.toString() + nl);
                } else {
                    addTextToBackupLog("<font color=red>Failed to create backup folder " + backupfolder.toString() + nl);
                    return false;
                }
                // Perform the backup
                for (int i = 0 ; i < config.backups.getPathsToBackup().size(); i++) {
                    backup(new File(config.backups.getPathsToBackup().get(i)), backupfolder);
                }
                // Delete the server log if set to do so
                if (config.backups.getClearLog()) {
                    addTextToBackupLog("Deleting server.log...");
                    if (new File("./server.log").delete()) {
                        addTextToBackupLog("<font color=green>Success!");
                    } else {
                        addTextToBackupLog("<font color=red>Failed!");
                    }
                }

                // Compression
                if (config.backups.getZip()) {
                    addTextToBackupLog(nl + "<br>Zipping backup folder to " + backupfolder.getName() + ".zip...");
                    try {
                        ZipOutputStream zipout = new ZipOutputStream(
                                new BufferedOutputStream(
                                new FileOutputStream(
                                config.backups.getPath() + fs + backupfolder.getName() + ".7z")));
                        depth = 0;
                        addDirToZip(backupfolder, zipout);
                        addTextToBackupLog(nl + "Finished compiling " + backupfolder.getName() + ".7z" + nl);
                        try {
                            zipout.close();
                            deleteDir(backupfolder);
                        } catch (IOException e) {
                            addTextToBackupLog(nl + "<font color=green>Successfully created " + backupfolder.getName() + ".zip!" + nl);
                        }
                    } catch (FileNotFoundException e) {
                        addTextToBackupLog("<font color=red>failure! Could not find file to compress!" + nl);
                        return true;
                    }
                } else {
                    addTextToBackupLog(nl + "Backup compression disabled...skipping." + nl);
                }
                return true;
            } else {
                addTextToBackupLog("Nothing selected to backup!" + nl);
                return true;
            }
        }

        @Override public void done() {
            try {
                backupSuccess = this.get();
                if (backupSuccess) {
                    addTextToBackupLog(nl + "<font color=green>Backup operation completed succesfully!");
                } else {
                    addTextToBackupLog(nl + "<font color=red>Backup operation encountered an error.  Aborting.");
                }
                setChanged();
                notifyObservers("finishedBackup");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (java.util.concurrent.ExecutionException e) {
                e.printStackTrace();
            }
        }
    };*/

    public boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        
        return dir.delete();
    }

    private void addDirToZip(File dirObj, ZipOutputStream out) {
        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                depth++;
                addDirToZip(files[i], out);
                continue;
            }
            try {
                FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
                addTextToBackupLog(nl + "Adding " + files[i].getParent() + fs + files[i].getName() + " to archive...");
                String name = files[i].getName();
                File parent = new File(files[i].getParentFile().getPath());
                for (int j = 0; j < depth; j++) {
                    name = parent.getName() + fs + name;
                    parent = new File(parent.getParentFile().getPath());
                }
                try {
                    out.putNextEntry(new ZipEntry(name));
                    int len;
                    while ((len = in.read(tmpBuf)) > 0) {
                        out.write(tmpBuf, 0, len);
                    }
                    out.closeEntry();
                    in.close();
                    addTextToBackupLog("<font color=green>success!" + nl);
                } catch (IOException e) {
                    addTextToBackupLog("<font color=red>failure! Skipping file." + nl);
                }
            } catch (FileNotFoundException e) {
                addTextToBackupLog(nl + "<font color=red>Failed to add " + files[i].getPath() + " to archive. Skipping...");
            }
        }
        depth--;
    }

    private void backup(File backupfrom, File backupfolder) {
        try {
            if (!backupfrom.getCanonicalPath().equals(backupfolder.getParentFile().getCanonicalPath())) {
                if (backupfrom.exists()) {
                    if (backupfrom.canRead()) {
                        File[] files = backupfrom.listFiles();
                        if (files == null || files.length == 0) {
                            java.util.List<File> backupto = new java.util.ArrayList<File>();
                            backupto.add(new File(backupfolder.getPath() +
                                    backupfrom.getPath().replaceFirst(".", "")));
                            addTextToBackupLog(nl + "Backing up \"" + backupfrom.getPath() + "\"...");

                            // The following segment ensures that all the proper parent files exist
                            int j = 0;
                            while (!backupto.get(j).getPath().equals(backupfolder.getPath())) {
                                backupto.add(backupto.get(j).getParentFile());
                                j++;
                            }
                            j = backupto.size() - 1;
                            while (j != 0) {
                                backupto.get(j).mkdir();
                                j--;
                            }

                            if (backupfrom.isDirectory()) {
                                if (backupto.get(0).mkdir()) {
                                    addTextToBackupLog("<font color=green>success!" + nl);
                                } else {
                                    addTextToBackupLog("<font color=red>failure! Can not create directory! Skipping..." + nl);
                                }
                            } else {
                                if (backupto.get(0).getParentFile().canWrite()) {
                                    FileInputStream from = null;
                                    FileOutputStream to = null;
                                    try {
                                        from = new FileInputStream(backupfrom);
                                        to = new FileOutputStream(backupto.get(0));
                                        byte[] buffer = new byte[4096];
                                        int bytesRead;

                                        while ((bytesRead = from.read(buffer)) != -1) {
                                            to.write(buffer, 0, bytesRead); // write
                                        }
                                        addTextToBackupLog("<font color=green>success!" + nl);
                                    } catch (FileNotFoundException e) {
                                        addTextToBackupLog("<font color=red>failure! Could not find file! Skipping..." + nl);
                                    } catch (IOException e) {
                                        addTextToBackupLog("<font color=red>failure! Error copying file! Skipping..." + nl);
                                    } finally {
                                        if (from != null) {
                                            try {
                                                from.close();
                                            } catch (IOException e) {
                                                addTextToBackupLog("<font color=red>Error closing file stream! Continuing..." + nl);
                                            }
                                        }
                                        if (to != null) {
                                            try {
                                                to.close();
                                            } catch (IOException e) {
                                                addTextToBackupLog("<font color=red>Error closing file stream! Continuing..." + nl);
                                            }
                                        }
                                    }
                                } else {
                                    addTextToBackupLog("<font color=red>failure! Can not write to parent directory!" + nl);
                                }
                            }
                        } else { // Path is directory (AND contains files), recurse through it to backup all files within.
                            for (int i = 0; i < files.length; i++) {
                                backup(files[i], backupfolder);
                            }
                        }
                    } else {
                        addTextToBackupLog("<font color=red>Error copying \"" + backupfrom.getPath() + "\". File is unreadable! Skipping..." + nl);
                    }
                } else {
                    addTextToBackupLog("<font color=red>Error copying \"" + backupfrom.getPath() + "\". File does not exist! Skipping..." + nl);
                }
            } else {
                addTextToBackupLog("<font color=red>You may not backup your backup folder! Skipping..." + nl);
            }
        }catch (IOException ioe) {
            addTextToBackupLog("<font color=red>Error attempting backup!");
            System.err.println("Cannot retrieve canonical backup path");
        }
    }

    private String nl;
    private String fs;
    private String workingDir;
    private int depth;
    private Config config;
    private BackupTask task;
    private javax.swing.JTextPane backupLog;
    private boolean backupSuccess;
}

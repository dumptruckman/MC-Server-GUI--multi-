/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui;

import java.util.Observable;
import java.io.*;
import javax.swing.SwingWorker;
import java.util.zip.*;

/**
 *
 * @author Roton
 */
public class MCServerGUIBackup extends Observable {

    public MCServerGUIBackup(MCServerGUIConfig newConfig, javax.swing.JTextPane newLog) {
        config = newConfig;
        backupLog = newLog;
        nl = System.getProperty("line.separator");
        fs = System.getProperty("file.separator");
    }

    public boolean startBackup() {
        String serverPath = new File(".\\" + config.cmdLine.getServerJar()).getParent();
        if (serverPath != null) {
            backupWorker.execute();
            return true;
        } else {
            addTextToBackupLog("Backup failed.  Perhaps a bad server jar?" + nl);
            return false;
        }
    }

    public void addTextToBackupLog(String textToAdd) {
        try {
            backupLog.getDocument().insertString(backupLog.getDocument().getLength(), textToAdd, null);
            backupLog.setCaretPosition(backupLog.getDocument().getLength());
        } catch (javax.swing.text.BadLocationException e) {
            System.out.println("BadLocationException");
        }
    }

    private SwingWorker backupWorker = new SwingWorker<Boolean, Integer>() {
        @Override
        public Boolean doInBackground() {
            if (!config.backups.getPathsToBackup().isEmpty()) {
                // Copy paths to backup
                String now = java.util.Calendar.getInstance().getTime().toString().replaceAll(":", ".");
                File backupfolder = new File(config.backups.getPath() + fs + now);
                if (backupfolder.mkdir()) {
                    addTextToBackupLog("Created backup folder " + backupfolder.toString() + nl);
                } else {
                    addTextToBackupLog("Failed to create backup folder " + backupfolder.toString() + nl);
                    return false;
                }
                for (int i = 0 ; i < config.backups.getPathsToBackup().size(); i++) {
                    File backupfrom = new File(config.backups.getPathsToBackup().get(i));
                    if (backupfrom.exists()) {
                        if (backupfrom.canRead()) {
                            java.util.List<File> backupto = new java.util.ArrayList<File>();
                            backupto.add(new File(backupfolder.getPath() + 
                                    config.backups.getPathsToBackup().get(i).replaceFirst(".", "")));
                            addTextToBackupLog(nl + "Copying \"" + backupfrom.getPath() + "\" to ");
                            addTextToBackupLog("\"" + backupto.get(0).getParent() + "\"...");
                            
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
                                    addTextToBackupLog("success!" + nl);
                                } else {
                                    addTextToBackupLog("failure! Can not create directory! Skipping..." + nl);
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
                                        addTextToBackupLog("success!" + nl);
                                    } catch (FileNotFoundException e) {
                                        addTextToBackupLog("failure! Could not find file! Skipping..." + nl);
                                    } catch (IOException e) {
                                        addTextToBackupLog("failure! Error copying file! Skipping..." + nl);
                                    } finally {
                                        if (from != null) {
                                            try {
                                                from.close();
                                            } catch (IOException e) {
                                                addTextToBackupLog("Error closing file stream! Continuing..." + nl);
                                            }
                                        }
                                        if (to != null) {
                                            try {
                                                to.close();
                                            } catch (IOException e) {
                                                addTextToBackupLog("Error closing file stream! Continuing..." + nl);
                                            }
                                        }
                                    }
                                } else {
                                    addTextToBackupLog("failure! Can not write to parent directory!" + nl);
                                }
                            }
                        } else {
                            addTextToBackupLog("Error copying \"" + backupfrom.getPath() + "\". File is unreadable! Skipping..." + nl);
                        }
                    } else {
                        addTextToBackupLog("Error copying \"" + backupfrom.getPath() + "\". File does not exist! Skipping..." + nl);
                    }
                }

                // Compression
                if (config.backups.getZip()) {
                    addTextToBackupLog(nl + "Zipping backup folder to " + backupfolder.getName() + ".zip...");
                    try {
                        ZipOutputStream zipout = new ZipOutputStream(
                                new BufferedOutputStream(
                                new FileOutputStream(
                                config.backups.getPath() + fs + backupfolder.getName() + ".zip")));
                        addDirToZip(backupfolder, zipout);
                        try {
                            zipout.close();
                        } catch (IOException e) {
                            addTextToBackupLog(nl + "Successfully created " + backupfolder.getName() + ".zip!" + nl);
                        }
                    } catch (FileNotFoundException e) {
                        addTextToBackupLog("failure! Could not find file to compress!" + nl);
                        return true;
                    }
                } else {
                    addTextToBackupLog("Backup compression disabled...skipping." + nl);
                }
                return true;
            } else {
                addTextToBackupLog("Nothing selected to backup!" + nl);
                return true;
            }
        }

        @Override
        public void done() {
            try {
                backupSuccess = this.get();
                if (backupSuccess) {
                    addTextToBackupLog("Backup operation completed succesfully!");
                } else {
                    addTextToBackupLog("Backup operation encountered an error.  Aborting.");
                }
                setChanged();
                notifyObservers("finishedBackup");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (java.util.concurrent.ExecutionException e) {
                e.printStackTrace();
            }
        }
    };

    private void addDirToZip(File dirObj, ZipOutputStream out) {
        //addTextToBackupLog(nl + "Adding directory " + dirObj.getCanonicalPath() + " to archive...");
        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                addDirToZip(files[i], out);
                continue;
            }
            try {
                FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
                addTextToBackupLog(nl + "Adding " + files[i].getPath() + " to archive...");
                try {
                    out.putNextEntry(new ZipEntry(files[i].getAbsolutePath()));
                    int len;
                    while ((len = in.read(tmpBuf)) > 0) {
                        out.write(tmpBuf, 0, len);
                    }
                    out.closeEntry();
                    in.close();
                    addTextToBackupLog("success!" + nl);
                } catch (IOException e) {
                    addTextToBackupLog("failure! Skipping file." + nl);
                }
            } catch (FileNotFoundException e) {
                addTextToBackupLog(nl + "Failed to add " + files[i].getPath() + " to archive. Skipping...");
            }
        }
    }

    private String nl;
    private String fs;
    private MCServerGUIConfig config;
    private javax.swing.JTextPane backupLog;
    private boolean backupSuccess;
}

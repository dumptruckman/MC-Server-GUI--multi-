/*
 * MCServerGUIView.java
 */

package mcservergui;

import java.io.Reader;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLDocument;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.util.Observer;
import java.util.Observable;
import javax.swing.JFileChooser;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.*;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingModel.CheckingMode;
import org.quartz.*;
import static mcservergui.MCServerGUIEventScheduler.*;

/**
 * The application's main frame.
 */
public class MCServerGUIView extends FrameView implements Observer {
    
    public MCServerGUIView(SingleFrameApplication app, MCServerGUIServerModel newServer, MCServerGUIConfig newConfig, Scheduler scheduler) {
        super(app);

        backupFileSystem = new mcservergui.fileexplorer.FileSystemModel(".");

        initComponents();
        fixComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);
        
        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            @Override public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
        
        sayCheckBox.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK),"sayOn");
        sayCheckBox.getActionMap().put("sayOn", sayToggle);
        consoleInput.getInputMap().put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.SHIFT_MASK),"sayOn");
        consoleInput.getActionMap().put("sayOn", saySend);

        this.scheduler = scheduler;

        taskList = new MCServerGUIListModel();
        taskSchedulerList.setModel(taskList);

        config = newConfig;
        server = newServer;
        getFrame().setTitle(config.getWindowTitle());
        controlSwitcher("OFF");

        inputHistory = new ArrayList<String>();
        inputHistoryIndex = -1;

        //enableSystemTrayIcon();

        parser = new MCServerGUIConsoleParser(config.display);
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = MCServerGUIApp.getApplication().getMainFrame();
            aboutBox = new MCServerGUIAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        MCServerGUIApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        tabber = new javax.swing.JTabbedPane();
        mainWindowTab = new javax.swing.JPanel();
        consoleOutputPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        consoleOutput = new javax.swing.JTextPane();
        playerListPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        consoleInputPanel = new javax.swing.JPanel();
        consoleInput = new javax.swing.JTextField();
        submitButton = new javax.swing.JButton();
        sayCheckBox = new javax.swing.JCheckBox();
        serverControlPanel = new javax.swing.JPanel();
        startstopButton = new javax.swing.JButton();
        saveWorldsButton = new javax.swing.JButton();
        serverInfoPanel = new javax.swing.JPanel();
        guiInfoPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        serverConfigTab = new javax.swing.JPanel();
        serverCmdLinePanel = new javax.swing.JPanel();
        javaExecLabel = new javax.swing.JLabel();
        javaExecField = new javax.swing.JTextField();
        serverJarLabel = new javax.swing.JLabel();
        serverJarField = new javax.swing.JTextField();
        bukkitCheckBox = new javax.swing.JCheckBox();
        javaExecBrowseButton = new javax.swing.JButton();
        serverJarBrowseButton = new javax.swing.JButton();
        xmxMemoryLabel = new javax.swing.JLabel();
        xmxMemoryField = new javax.swing.JTextField();
        xincgcCheckBox = new javax.swing.JCheckBox();
        extraArgsLabel = new javax.swing.JLabel();
        extraArgsField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        cmdLineField = new javax.swing.JTextField();
        customLaunchCheckBox = new javax.swing.JCheckBox();
        saveServerConfigButton = new javax.swing.JButton();
        guiConfigTab = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        windowTitleLabel = new javax.swing.JLabel();
        windowTitleField = new javax.swing.JTextField();
        inputHistoryMaxSizeLabel = new javax.swing.JLabel();
        inputHistoryMaxSizeField = new javax.swing.JTextField();
        saveGuiConfigButton = new javax.swing.JButton();
        backupTab = new javax.swing.JPanel();
        backupButton = new javax.swing.JButton();
        backupSettingsPanel = new javax.swing.JPanel();
        backupPathLabel = new javax.swing.JLabel();
        backupPathField = new javax.swing.JTextField();
        backupPathBrowseButton = new javax.swing.JButton();
        zipBackupCheckBox = new javax.swing.JCheckBox();
        saveBackupControlButton = new javax.swing.JButton();
        backupFileChooserPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        backupFileChooser = new it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree();
        backupStatusPanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        backupStatusLog = new javax.swing.JTextPane();
        backupControlRefreshButton = new javax.swing.JButton();
        restoreTab = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        schedulerTab = new javax.swing.JPanel();
        taskSchedulerPanel = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        taskSchedulerList = new javax.swing.JList();
        taskListAddButton = new javax.swing.JButton();
        taskListEditButton = new javax.swing.JButton();
        taskListRemoveButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        tabber.setName("tabber"); // NOI18N
        tabber.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabberStateChanged(evt);
            }
        });
        tabber.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                tabberKeyTyped(evt);
            }
        });

        mainWindowTab.setName("mainWindowTab"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mcservergui.MCServerGUIApp.class).getContext().getResourceMap(MCServerGUIView.class);
        consoleOutputPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("consoleOutputPanel.border.title"))); // NOI18N
        consoleOutputPanel.setName("consoleOutputPanel"); // NOI18N
        consoleOutputPanel.setPreferredSize(new java.awt.Dimension(220, 204));

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        consoleOutput.setContentType(resourceMap.getString("consoleOutput.contentType")); // NOI18N
        consoleOutput.setEditable(false);
        consoleOutput.setToolTipText(resourceMap.getString("consoleOutput.toolTipText")); // NOI18N
        consoleOutput.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        consoleOutput.setName("consoleOutput"); // NOI18N
        consoleOutput.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                consoleOutputMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                consoleOutputMouseExited(evt);
            }
        });
        consoleOutput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                consoleOutputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                consoleOutputFocusLost(evt);
            }
        });
        consoleOutput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                consoleOutputKeyTyped(evt);
            }
        });
        jScrollPane1.setViewportView(consoleOutput);

        javax.swing.GroupLayout consoleOutputPanelLayout = new javax.swing.GroupLayout(consoleOutputPanel);
        consoleOutputPanel.setLayout(consoleOutputPanelLayout);
        consoleOutputPanelLayout.setHorizontalGroup(
            consoleOutputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE)
        );
        consoleOutputPanelLayout.setVerticalGroup(
            consoleOutputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE)
        );

        playerListPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("playerListPanel.border.title"))); // NOI18N
        playerListPanel.setName("playerListPanel"); // NOI18N
        playerListPanel.setPreferredSize(new java.awt.Dimension(125, 204));

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jList1.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Not yet implemented" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList1.setToolTipText(resourceMap.getString("playerList.toolTipText")); // NOI18N
        jList1.setEnabled(false);
        jList1.setFocusable(false);
        jList1.setName("playerList"); // NOI18N
        jList1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jList1KeyTyped(evt);
            }
        });
        jScrollPane2.setViewportView(jList1);

        javax.swing.GroupLayout playerListPanelLayout = new javax.swing.GroupLayout(playerListPanel);
        playerListPanel.setLayout(playerListPanelLayout);
        playerListPanelLayout.setHorizontalGroup(
            playerListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
        );
        playerListPanelLayout.setVerticalGroup(
            playerListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE)
        );

        consoleInputPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("consoleInputPanel.border.title"))); // NOI18N
        consoleInputPanel.setName("consoleInputPanel"); // NOI18N
        consoleInputPanel.setPreferredSize(new java.awt.Dimension(300, 50));

        consoleInput.setText(resourceMap.getString("consoleInput.text")); // NOI18N
        consoleInput.setToolTipText(resourceMap.getString("consoleInput.toolTipText")); // NOI18N
        consoleInput.setName("consoleInput"); // NOI18N
        consoleInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleInputActionPerformed(evt);
            }
        });
        consoleInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                consoleInputKeyPressed(evt);
            }
        });

        submitButton.setText(resourceMap.getString("submitButton.text")); // NOI18N
        submitButton.setName("submitButton"); // NOI18N
        submitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitButtonActionPerformed(evt);
            }
        });
        submitButton.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                submitButtonKeyTyped(evt);
            }
        });

        sayCheckBox.setText(resourceMap.getString("sayCheckBox.text")); // NOI18N
        sayCheckBox.setToolTipText(resourceMap.getString("sayCheckBox.toolTipText")); // NOI18N
        sayCheckBox.setName("sayCheckBox"); // NOI18N
        sayCheckBox.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                sayCheckBoxKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout consoleInputPanelLayout = new javax.swing.GroupLayout(consoleInputPanel);
        consoleInputPanel.setLayout(consoleInputPanelLayout);
        consoleInputPanelLayout.setHorizontalGroup(
            consoleInputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, consoleInputPanelLayout.createSequentialGroup()
                .addComponent(sayCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(consoleInput, javax.swing.GroupLayout.DEFAULT_SIZE, 404, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(submitButton))
        );
        consoleInputPanelLayout.setVerticalGroup(
            consoleInputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(consoleInputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(consoleInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(submitButton)
                .addComponent(sayCheckBox))
        );

        serverControlPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("serverControlPanel.border.title"))); // NOI18N
        serverControlPanel.setName("serverControlPanel"); // NOI18N

        startstopButton.setText(resourceMap.getString("startstopButton.text")); // NOI18N
        startstopButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        startstopButton.setName("startstopButton"); // NOI18N
        startstopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startstopButtonActionPerformed(evt);
            }
        });

        saveWorldsButton.setText(resourceMap.getString("saveWorldsButton.text")); // NOI18N
        saveWorldsButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        saveWorldsButton.setName("saveWorldsButton"); // NOI18N
        saveWorldsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveWorldsButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout serverControlPanelLayout = new javax.swing.GroupLayout(serverControlPanel);
        serverControlPanel.setLayout(serverControlPanelLayout);
        serverControlPanelLayout.setHorizontalGroup(
            serverControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverControlPanelLayout.createSequentialGroup()
                .addComponent(startstopButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveWorldsButton))
        );
        serverControlPanelLayout.setVerticalGroup(
            serverControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverControlPanelLayout.createSequentialGroup()
                .addGroup(serverControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startstopButton)
                    .addComponent(saveWorldsButton))
                .addContainerGap(58, Short.MAX_VALUE))
        );

        serverInfoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Server Information"));
        serverInfoPanel.setName("serverInfoPanel"); // NOI18N

        javax.swing.GroupLayout serverInfoPanelLayout = new javax.swing.GroupLayout(serverInfoPanel);
        serverInfoPanel.setLayout(serverInfoPanelLayout);
        serverInfoPanelLayout.setHorizontalGroup(
            serverInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 122, Short.MAX_VALUE)
        );
        serverInfoPanelLayout.setVerticalGroup(
            serverInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 81, Short.MAX_VALUE)
        );

        guiInfoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("GUI Information"));
        guiInfoPanel.setName("guiInfoPanel"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        javax.swing.GroupLayout guiInfoPanelLayout = new javax.swing.GroupLayout(guiInfoPanel);
        guiInfoPanel.setLayout(guiInfoPanelLayout);
        guiInfoPanelLayout.setHorizontalGroup(
            guiInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(guiInfoPanelLayout.createSequentialGroup()
                .addComponent(jLabel1)
                .addContainerGap(56, Short.MAX_VALUE))
        );
        guiInfoPanelLayout.setVerticalGroup(
            guiInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(guiInfoPanelLayout.createSequentialGroup()
                .addComponent(jLabel1)
                .addContainerGap(67, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout mainWindowTabLayout = new javax.swing.GroupLayout(mainWindowTab);
        mainWindowTab.setLayout(mainWindowTabLayout);
        mainWindowTabLayout.setHorizontalGroup(
            mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainWindowTabLayout.createSequentialGroup()
                .addGroup(mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainWindowTabLayout.createSequentialGroup()
                        .addComponent(serverControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(serverInfoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(guiInfoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainWindowTabLayout.createSequentialGroup()
                        .addComponent(consoleOutputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 397, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(playerListPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE))
                    .addComponent(consoleInputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 532, Short.MAX_VALUE))
                .addGap(13, 13, 13))
        );
        mainWindowTabLayout.setVerticalGroup(
            mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainWindowTabLayout.createSequentialGroup()
                .addGroup(mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(playerListPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                    .addComponent(consoleOutputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(consoleInputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(serverControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(serverInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(guiInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabber.addTab(resourceMap.getString("mainWindowTab.TabConstraints.tabTitle"), mainWindowTab); // NOI18N

        serverConfigTab.setName("serverConfigTab"); // NOI18N

        serverCmdLinePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("serverCmdLinePanel.border.title"))); // NOI18N
        serverCmdLinePanel.setName("serverCmdLinePanel"); // NOI18N

        javaExecLabel.setText(resourceMap.getString("javaExecLabel.text")); // NOI18N
        javaExecLabel.setName("javaExecLabel"); // NOI18N

        javaExecField.setText(resourceMap.getString("javaExecField.text")); // NOI18N
        javaExecField.setToolTipText(resourceMap.getString("javaExecField.toolTipText")); // NOI18N
        javaExecField.setName("javaExecField"); // NOI18N
        javaExecField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                javaExecFieldActionPerformed(evt);
            }
        });

        serverJarLabel.setText(resourceMap.getString("serverJarLabel.text")); // NOI18N
        serverJarLabel.setName("serverJarLabel"); // NOI18N

        serverJarField.setText(resourceMap.getString("serverJarField.text")); // NOI18N
        serverJarField.setName("serverJarField"); // NOI18N
        serverJarField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverJarFieldActionPerformed(evt);
            }
        });

        bukkitCheckBox.setText(resourceMap.getString("bukkitCheckBox.text")); // NOI18N
        bukkitCheckBox.setName("bukkitCheckBox"); // NOI18N
        bukkitCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bukkitCheckBoxActionPerformed(evt);
            }
        });

        javaExecBrowseButton.setText(resourceMap.getString("javaExecBrowseButton.text")); // NOI18N
        javaExecBrowseButton.setToolTipText(resourceMap.getString("javaExecBrowseButton.toolTipText")); // NOI18N
        javaExecBrowseButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        javaExecBrowseButton.setName("javaExecBrowseButton"); // NOI18N
        javaExecBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                javaExecBrowseButtonActionPerformed(evt);
            }
        });

        serverJarBrowseButton.setText(resourceMap.getString("serverJarBrowseButton.text")); // NOI18N
        serverJarBrowseButton.setToolTipText(resourceMap.getString("serverJarBrowseButton.toolTipText")); // NOI18N
        serverJarBrowseButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        serverJarBrowseButton.setName("serverJarBrowseButton"); // NOI18N
        serverJarBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverJarBrowseButtonActionPerformed(evt);
            }
        });

        xmxMemoryLabel.setText(resourceMap.getString("xmxMemoryLabel.text")); // NOI18N
        xmxMemoryLabel.setName("xmxMemoryLabel"); // NOI18N

        xmxMemoryField.setText(resourceMap.getString("xmxMemoryField.text")); // NOI18N
        xmxMemoryField.setToolTipText(resourceMap.getString("xmxMemoryField.toolTipText")); // NOI18N
        xmxMemoryField.setName("xmxMemoryField"); // NOI18N
        xmxMemoryField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xmxMemoryFieldActionPerformed(evt);
            }
        });

        xincgcCheckBox.setText(resourceMap.getString("xincgcCheckBox.text")); // NOI18N
        xincgcCheckBox.setToolTipText(resourceMap.getString("xincgcCheckBox.toolTipText")); // NOI18N
        xincgcCheckBox.setName("xincgcCheckBox"); // NOI18N
        xincgcCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xincgcCheckBoxActionPerformed(evt);
            }
        });

        extraArgsLabel.setText(resourceMap.getString("extraArgsLabel.text")); // NOI18N
        extraArgsLabel.setName("extraArgsLabel"); // NOI18N

        extraArgsField.setText(resourceMap.getString("extraArgsField.text")); // NOI18N
        extraArgsField.setToolTipText(resourceMap.getString("extraArgsField.toolTipText")); // NOI18N
        extraArgsField.setName("extraArgsField"); // NOI18N
        extraArgsField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                extraArgsFieldActionPerformed(evt);
            }
        });

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        cmdLineField.setEditable(false);
        cmdLineField.setText(resourceMap.getString("cmdLineField.text")); // NOI18N
        cmdLineField.setToolTipText(resourceMap.getString("cmdLineField.toolTipText")); // NOI18N
        cmdLineField.setName("cmdLineField"); // NOI18N
        cmdLineField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdLineFieldActionPerformed(evt);
            }
        });

        customLaunchCheckBox.setText(resourceMap.getString("customLaunchCheckBox.text")); // NOI18N
        customLaunchCheckBox.setToolTipText(resourceMap.getString("customLaunchCheckBox.toolTipText")); // NOI18N
        customLaunchCheckBox.setName("customLaunchCheckBox"); // NOI18N
        customLaunchCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customLaunchCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout serverCmdLinePanelLayout = new javax.swing.GroupLayout(serverCmdLinePanel);
        serverCmdLinePanel.setLayout(serverCmdLinePanelLayout);
        serverCmdLinePanelLayout.setHorizontalGroup(
            serverCmdLinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverCmdLinePanelLayout.createSequentialGroup()
                .addComponent(customLaunchCheckBox)
                .addContainerGap())
            .addGroup(serverCmdLinePanelLayout.createSequentialGroup()
                .addGroup(serverCmdLinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, serverCmdLinePanelLayout.createSequentialGroup()
                        .addComponent(serverJarLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(serverJarField, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, serverCmdLinePanelLayout.createSequentialGroup()
                        .addComponent(javaExecLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(javaExecField, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverCmdLinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(serverJarBrowseButton)
                    .addComponent(javaExecBrowseButton))
                .addGap(119, 119, 119))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, serverCmdLinePanelLayout.createSequentialGroup()
                .addGroup(serverCmdLinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(bukkitCheckBox, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, serverCmdLinePanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(cmdLineField, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, serverCmdLinePanelLayout.createSequentialGroup()
                        .addComponent(extraArgsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(extraArgsField, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, serverCmdLinePanelLayout.createSequentialGroup()
                        .addComponent(xmxMemoryLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(xmxMemoryField, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(xincgcCheckBox)))
                .addGap(125, 125, 125))
        );
        serverCmdLinePanelLayout.setVerticalGroup(
            serverCmdLinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverCmdLinePanelLayout.createSequentialGroup()
                .addGroup(serverCmdLinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(javaExecLabel)
                    .addComponent(javaExecField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(javaExecBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverCmdLinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serverJarLabel)
                    .addComponent(serverJarField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(serverJarBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bukkitCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(serverCmdLinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(xmxMemoryLabel)
                    .addComponent(xmxMemoryField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(xincgcCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(serverCmdLinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(extraArgsLabel)
                    .addComponent(extraArgsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(customLaunchCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmdLineField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        saveServerConfigButton.setText(resourceMap.getString("saveServerConfigButton.text")); // NOI18N
        saveServerConfigButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        saveServerConfigButton.setName("saveServerConfigButton"); // NOI18N
        saveServerConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveServerConfigButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout serverConfigTabLayout = new javax.swing.GroupLayout(serverConfigTab);
        serverConfigTab.setLayout(serverConfigTabLayout);
        serverConfigTabLayout.setHorizontalGroup(
            serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverConfigTabLayout.createSequentialGroup()
                .addGroup(serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(serverCmdLinePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(serverConfigTabLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(saveServerConfigButton)))
                .addContainerGap(293, Short.MAX_VALUE))
        );
        serverConfigTabLayout.setVerticalGroup(
            serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverConfigTabLayout.createSequentialGroup()
                .addComponent(serverCmdLinePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveServerConfigButton)
                .addContainerGap(84, Short.MAX_VALUE))
        );

        tabber.addTab(resourceMap.getString("serverConfigTab.TabConstraints.tabTitle"), serverConfigTab); // NOI18N

        guiConfigTab.setName("guiConfigTab"); // NOI18N

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel4.border.title"))); // NOI18N
        jPanel4.setName("jPanel4"); // NOI18N

        windowTitleLabel.setText(resourceMap.getString("windowTitleLabel.text")); // NOI18N
        windowTitleLabel.setName("windowTitleLabel"); // NOI18N

        windowTitleField.setText(resourceMap.getString("windowTitleField.text")); // NOI18N
        windowTitleField.setName("windowTitleField"); // NOI18N
        windowTitleField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                windowTitleFieldActionPerformed(evt);
            }
        });

        inputHistoryMaxSizeLabel.setText(resourceMap.getString("inputHistoryMaxSizeLabel.text")); // NOI18N
        inputHistoryMaxSizeLabel.setName("inputHistoryMaxSizeLabel"); // NOI18N

        inputHistoryMaxSizeField.setText(resourceMap.getString("inputHistoryMaxSizeField.text")); // NOI18N
        inputHistoryMaxSizeField.setToolTipText(resourceMap.getString("inputHistoryMaxSizeField.toolTipText")); // NOI18N
        inputHistoryMaxSizeField.setInputVerifier(new numberVerifier());
        inputHistoryMaxSizeField.setName("inputHistoryMaxSizeField"); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(windowTitleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(windowTitleField, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(inputHistoryMaxSizeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inputHistoryMaxSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(windowTitleLabel)
                    .addComponent(windowTitleField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(inputHistoryMaxSizeLabel)
                    .addComponent(inputHistoryMaxSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        saveGuiConfigButton.setText(resourceMap.getString("saveGuiConfigButton.text")); // NOI18N
        saveGuiConfigButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        saveGuiConfigButton.setName("saveGuiConfigButton"); // NOI18N
        saveGuiConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveGuiConfigButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout guiConfigTabLayout = new javax.swing.GroupLayout(guiConfigTab);
        guiConfigTab.setLayout(guiConfigTabLayout);
        guiConfigTabLayout.setHorizontalGroup(
            guiConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(guiConfigTabLayout.createSequentialGroup()
                .addGroup(guiConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(guiConfigTabLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(saveGuiConfigButton)))
                .addContainerGap(295, Short.MAX_VALUE))
        );
        guiConfigTabLayout.setVerticalGroup(
            guiConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(guiConfigTabLayout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveGuiConfigButton)
                .addContainerGap(249, Short.MAX_VALUE))
        );

        tabber.addTab(resourceMap.getString("guiConfigTab.TabConstraints.tabTitle"), guiConfigTab); // NOI18N

        backupTab.setName("backupTab"); // NOI18N

        backupButton.setText(resourceMap.getString("backupButton.text")); // NOI18N
        backupButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        backupButton.setName("backupButton"); // NOI18N
        backupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backupButtonActionPerformed(evt);
            }
        });

        backupSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("backupSettingsPanel.border.title"))); // NOI18N
        backupSettingsPanel.setName("backupSettingsPanel"); // NOI18N

        backupPathLabel.setText(resourceMap.getString("backupPathLabel.text")); // NOI18N
        backupPathLabel.setName("backupPathLabel"); // NOI18N

        backupPathField.setText(resourceMap.getString("backupPathField.text")); // NOI18N
        backupPathField.setName("backupPathField"); // NOI18N
        backupPathField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backupPathFieldActionPerformed(evt);
            }
        });

        backupPathBrowseButton.setText(resourceMap.getString("backupPathBrowseButton.text")); // NOI18N
        backupPathBrowseButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        backupPathBrowseButton.setName("backupPathBrowseButton"); // NOI18N
        backupPathBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backupPathBrowseButtonActionPerformed(evt);
            }
        });

        zipBackupCheckBox.setText(resourceMap.getString("zipBackupCheckBox.text")); // NOI18N
        zipBackupCheckBox.setName("zipBackupCheckBox"); // NOI18N
        zipBackupCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zipBackupCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backupSettingsPanelLayout = new javax.swing.GroupLayout(backupSettingsPanel);
        backupSettingsPanel.setLayout(backupSettingsPanelLayout);
        backupSettingsPanelLayout.setHorizontalGroup(
            backupSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backupSettingsPanelLayout.createSequentialGroup()
                .addComponent(backupPathLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(backupPathField, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(backupPathBrowseButton))
            .addGroup(backupSettingsPanelLayout.createSequentialGroup()
                .addComponent(zipBackupCheckBox)
                .addContainerGap())
        );
        backupSettingsPanelLayout.setVerticalGroup(
            backupSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backupSettingsPanelLayout.createSequentialGroup()
                .addGroup(backupSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(backupPathLabel)
                    .addComponent(backupPathField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(backupPathBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(zipBackupCheckBox))
        );

        saveBackupControlButton.setText(resourceMap.getString("saveBackupControlButton.text")); // NOI18N
        saveBackupControlButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        saveBackupControlButton.setName("saveBackupControlButton"); // NOI18N
        saveBackupControlButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveBackupControlButtonActionPerformed(evt);
            }
        });

        backupFileChooserPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("backupFileChooserPanel.border.title"))); // NOI18N
        backupFileChooserPanel.setName("backupFileChooserPanel"); // NOI18N

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        backupFileChooser.setModel(backupFileSystem);
        backupFileChooser.setToolTipText(resourceMap.getString("backupFileChooser.toolTipText")); // NOI18N
        backupFileChooser.setName("backupFileChooser"); // NOI18N
        backupFileChooser.setToggleClickCount(3);
        jScrollPane3.setViewportView(backupFileChooser);

        javax.swing.GroupLayout backupFileChooserPanelLayout = new javax.swing.GroupLayout(backupFileChooserPanel);
        backupFileChooserPanel.setLayout(backupFileChooserPanelLayout);
        backupFileChooserPanelLayout.setHorizontalGroup(
            backupFileChooserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)
        );
        backupFileChooserPanelLayout.setVerticalGroup(
            backupFileChooserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
        );

        backupStatusPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("backupStatusPanel.border.title"))); // NOI18N
        backupStatusPanel.setName("backupStatusPanel"); // NOI18N

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        backupStatusLog.setEditable(false);
        backupStatusLog.setName("backupStatusLog"); // NOI18N
        jScrollPane4.setViewportView(backupStatusLog);

        javax.swing.GroupLayout backupStatusPanelLayout = new javax.swing.GroupLayout(backupStatusPanel);
        backupStatusPanel.setLayout(backupStatusPanelLayout);
        backupStatusPanelLayout.setHorizontalGroup(
            backupStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
        );
        backupStatusPanelLayout.setVerticalGroup(
            backupStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
        );

        backupControlRefreshButton.setText(resourceMap.getString("backupControlRefreshButton.text")); // NOI18N
        backupControlRefreshButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        backupControlRefreshButton.setName("backupControlRefreshButton"); // NOI18N
        backupControlRefreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backupControlRefreshButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backupTabLayout = new javax.swing.GroupLayout(backupTab);
        backupTab.setLayout(backupTabLayout);
        backupTabLayout.setHorizontalGroup(
            backupTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backupTabLayout.createSequentialGroup()
                .addGroup(backupTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(backupFileChooserPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(backupSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backupTabLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(backupControlRefreshButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveBackupControlButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 158, Short.MAX_VALUE)
                        .addComponent(backupButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(backupStatusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        backupTabLayout.setVerticalGroup(
            backupTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backupTabLayout.createSequentialGroup()
                .addGroup(backupTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(backupStatusPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(backupFileChooserPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backupTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(backupControlRefreshButton)
                    .addComponent(backupButton)
                    .addComponent(saveBackupControlButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(backupSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19))
        );

        tabber.addTab(resourceMap.getString("backupTab.TabConstraints.tabTitle"), backupTab); // NOI18N

        restoreTab.setName("restoreTab"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        javax.swing.GroupLayout restoreTabLayout = new javax.swing.GroupLayout(restoreTab);
        restoreTab.setLayout(restoreTabLayout);
        restoreTabLayout.setHorizontalGroup(
            restoreTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(restoreTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addContainerGap(436, Short.MAX_VALUE))
        );
        restoreTabLayout.setVerticalGroup(
            restoreTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(restoreTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addContainerGap(326, Short.MAX_VALUE))
        );

        tabber.addTab(resourceMap.getString("restoreTab.TabConstraints.tabTitle"), restoreTab); // NOI18N

        schedulerTab.setName("schedulerTab"); // NOI18N

        taskSchedulerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("taskSchedulerPanel.border.title"))); // NOI18N
        taskSchedulerPanel.setName("taskSchedulerPanel"); // NOI18N

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        taskSchedulerList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        taskSchedulerList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        taskSchedulerList.setName("taskSchedulerList"); // NOI18N
        jScrollPane5.setViewportView(taskSchedulerList);

        javax.swing.GroupLayout taskSchedulerPanelLayout = new javax.swing.GroupLayout(taskSchedulerPanel);
        taskSchedulerPanel.setLayout(taskSchedulerPanelLayout);
        taskSchedulerPanelLayout.setHorizontalGroup(
            taskSchedulerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
        );
        taskSchedulerPanelLayout.setVerticalGroup(
            taskSchedulerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
        );

        taskListAddButton.setText(resourceMap.getString("taskListAddButton.text")); // NOI18N
        taskListAddButton.setName("taskListAddButton"); // NOI18N
        taskListAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                taskListAddButtonActionPerformed(evt);
            }
        });

        taskListEditButton.setText(resourceMap.getString("taskListEditButton.text")); // NOI18N
        taskListEditButton.setName("taskListEditButton"); // NOI18N
        taskListEditButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                taskListEditButtonActionPerformed(evt);
            }
        });

        taskListRemoveButton.setText(resourceMap.getString("taskListRemoveButton.text")); // NOI18N
        taskListRemoveButton.setName("taskListRemoveButton"); // NOI18N
        taskListRemoveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                taskListRemoveButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout schedulerTabLayout = new javax.swing.GroupLayout(schedulerTab);
        schedulerTab.setLayout(schedulerTabLayout);
        schedulerTabLayout.setHorizontalGroup(
            schedulerTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(schedulerTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(taskListAddButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(taskListEditButton)
                .addGap(18, 18, 18)
                .addComponent(taskListRemoveButton)
                .addContainerGap(338, Short.MAX_VALUE))
            .addComponent(taskSchedulerPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        schedulerTabLayout.setVerticalGroup(
            schedulerTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, schedulerTabLayout.createSequentialGroup()
                .addComponent(taskSchedulerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(schedulerTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(taskListAddButton)
                    .addComponent(taskListEditButton)
                    .addComponent(taskListRemoveButton))
                .addGap(83, 83, 83))
        );

        tabber.addTab(resourceMap.getString("schedulerTab.TabConstraints.tabTitle"), schedulerTab); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabber, javax.swing.GroupLayout.DEFAULT_SIZE, 550, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabber, javax.swing.GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE)
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(mcservergui.MCServerGUIApp.class).getContext().getActionMap(MCServerGUIView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setText(resourceMap.getString("statusMessageLabel.text")); // NOI18N
        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 550, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 455, Short.MAX_VALUE)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addContainerGap(394, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(statusMessageLabel)
                        .addComponent(statusAnimationLabel))
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Fixes the oddities of the component placing that netbeans does.
     */
    public final void fixComponents() {
        javax.swing.GroupLayout mainWindowTabLayout = new javax.swing.GroupLayout(mainWindowTab);
        mainWindowTab.setLayout(mainWindowTabLayout);
        mainWindowTabLayout.setHorizontalGroup(
            mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainWindowTabLayout.createSequentialGroup()
                .addGroup(mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainWindowTabLayout.createSequentialGroup()
                        .addComponent(serverControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(serverInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(guiInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                        //.addComponent(backupControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                    .addGroup(mainWindowTabLayout.createSequentialGroup()
                        .addGroup(mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, true)
                            .addComponent(consoleInputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainWindowTabLayout.createSequentialGroup()
                                .addComponent(consoleOutputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(playerListPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))))
        );
        mainWindowTabLayout.setVerticalGroup(
            mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainWindowTabLayout.createSequentialGroup()
                .addGroup(mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, true)
                    .addComponent(consoleOutputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(playerListPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(consoleInputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(guiInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(serverInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(serverControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    //.addComponent(backupControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        backupFileChooser.getCheckingModel().setCheckingMode(CheckingMode.PROPAGATE_PRESERVING_CHECK);
        taskSchedulerList.setCellRenderer(new TaskSchedulerListCellRenderer());
        consoleOutput.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
        backupStatusLog.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
        consoleOutput.setStyledDocument(new javax.swing.text.html.HTMLDocument());
        backupStatusLog.setStyledDocument(new javax.swing.text.html.HTMLDocument());
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mcservergui.MCServerGUIApp.class).getContext().getResourceMap(MCServerGUIView.class);
        this.getFrame().setIconImage(resourceMap.getImageIcon("imageLabel.icon").getImage());
    }

    private class TaskSchedulerListCellRenderer extends javax.swing.JTextPane implements javax.swing.ListCellRenderer {
        public TaskSchedulerListCellRenderer() {
            setOpaque(true);
            this.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
        }

        @Override public java.awt.Component getListCellRendererComponent(
                javax.swing.JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus)
        {
            setText(value.toString());
            setBackground(isSelected ? java.awt.Color.lightGray : java.awt.Color.white);
            setForeground(isSelected ? java.awt.Color.white : java.awt.Color.black);
            return this;
        }
    }

    private class BackupFileChooserCheckingListener implements TreeCheckingListener {
        @Override public void valueChanged(TreeCheckingEvent e) {
            //Changing how it saves what is checked.
            /*if(e.isCheckedPath()) {
                for (int childrenindex = 0; childrenindex < backupFileSystem.getChildCount(
                        e.getPath().getLastPathComponent()); childrenindex++) {
                    backupFileChooser.addCheckingPath(
                            new javax.swing.tree.TreePath(
                            backupFileSystem.getChild(e.getPath().getLastPathComponent(), childrenindex)));
                }
                addPathToBackup(e.getPath().getLastPathComponent().toString());
            } else {
                for (int childrenindex = 0; childrenindex < backupFileSystem.getChildCount(
                        e.getPath().getLastPathComponent()); childrenindex++) {
                    backupFileChooser.removeCheckingPath(
                            new javax.swing.tree.TreePath(
                            backupFileSystem.getChild(e.getPath().getLastPathComponent(), childrenindex)));
                }
                removePathFromBackup(e.getPath().getLastPathComponent().toString());
            }*/
        }
    }

    /**
     * Action object for the toggling of the sayCheckBox
     */
    javax.swing.Action sayToggle = new javax.swing.AbstractAction() {
        @Override public void actionPerformed(ActionEvent e) {
            if (sayCheckBox.isSelected()) {
                sayCheckBox.setSelected(false);
            } else {
                sayCheckBox.setSelected(true);
            }
        }
    };

    /**
     * Action object for the sending of input with prepended "Say " (caused by shift+enter)
     */
    javax.swing.Action saySend = new javax.swing.AbstractAction() {
        @Override public void actionPerformed(ActionEvent e) {
            sendInput(true);
        }
    };

    private void startstopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startstopButtonActionPerformed
        if (startstopButton.getText().equals("Start")) {
            startServer();
        } else {
            stopServer();
        }
    }//GEN-LAST:event_startstopButtonActionPerformed

    private void submitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitButtonActionPerformed
        sendInput();
    }//GEN-LAST:event_submitButtonActionPerformed

    private void consoleInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleInputActionPerformed
        sendInput();
    }//GEN-LAST:event_consoleInputActionPerformed

    private void windowTitleFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_windowTitleFieldActionPerformed
        config.setWindowTitle(windowTitleField.getText());
        getFrame().setTitle(windowTitleField.getText());
    }//GEN-LAST:event_windowTitleFieldActionPerformed

    private void javaExecBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_javaExecBrowseButtonActionPerformed
        final JFileChooser fc = new JFileChooser();
        int returnVal = fc.showOpenDialog(getFrame());
        cmdLineField.setText(config.cmdLine.parseCmdLine());
    }//GEN-LAST:event_javaExecBrowseButtonActionPerformed

    private void serverJarBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverJarBrowseButtonActionPerformed
        try {
            final JFileChooser fc = new JFileChooser(new File(".").getCanonicalPath());
            int returnVal = fc.showOpenDialog(getFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                serverJarField.setText(fc.getSelectedFile().getName());
                config.cmdLine.setServerJar(fc.getSelectedFile().getName());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        } catch (IOException e) {
            System.out.println("Error retrieving path");
        }
    }//GEN-LAST:event_serverJarBrowseButtonActionPerformed

    private void bukkitCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bukkitCheckBoxActionPerformed
        config.cmdLine.setBukkit(bukkitCheckBox.isSelected());
        cmdLineField.setText(config.cmdLine.parseCmdLine());
        if (bukkitCheckBox.isSelected()) {
            //backupPluginsCheckBox.setEnabled(true);
        } else {
           // backupPluginsCheckBox.setEnabled(false);
        }
    }//GEN-LAST:event_bukkitCheckBoxActionPerformed

    private void xmxMemoryFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xmxMemoryFieldActionPerformed
        config.cmdLine.setXmx(xmxMemoryField.getText());
        cmdLineField.setText(config.cmdLine.parseCmdLine());
    }//GEN-LAST:event_xmxMemoryFieldActionPerformed

    private void xincgcCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xincgcCheckBoxActionPerformed
        config.cmdLine.setXincgc(xincgcCheckBox.isSelected());
        cmdLineField.setText(config.cmdLine.parseCmdLine());
    }//GEN-LAST:event_xincgcCheckBoxActionPerformed

    private void extraArgsFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_extraArgsFieldActionPerformed
        config.cmdLine.setExtraArgs(extraArgsField.getText());
        cmdLineField.setText(config.cmdLine.parseCmdLine());
    }//GEN-LAST:event_extraArgsFieldActionPerformed

    private void serverJarFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverJarFieldActionPerformed
        config.cmdLine.setServerJar(serverJarField.getText());
        cmdLineField.setText(config.cmdLine.parseCmdLine());
    }//GEN-LAST:event_serverJarFieldActionPerformed

    private void javaExecFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_javaExecFieldActionPerformed
        config.cmdLine.setJavaExec(javaExecField.getText());
        cmdLineField.setText(config.cmdLine.parseCmdLine());
    }//GEN-LAST:event_javaExecFieldActionPerformed

    private void saveGuiConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveGuiConfigButtonActionPerformed
        saveConfig();
    }//GEN-LAST:event_saveGuiConfigButtonActionPerformed

    private void saveServerConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveServerConfigButtonActionPerformed
        saveConfig();
    }//GEN-LAST:event_saveServerConfigButtonActionPerformed

    private void consoleOutputMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_consoleOutputMouseEntered
        if ((server.isRunning()) && (!consoleOutput.isFocusOwner())) {
            textScrolling = false;

        }
        mouseInConsoleOutput = true;
    }//GEN-LAST:event_consoleOutputMouseEntered

    private void consoleOutputMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_consoleOutputMouseExited
        int selMin = consoleOutput.getSelectionStart();
        int selMax = consoleOutput.getSelectionEnd();
        if ((!consoleOutput.isFocusOwner()) && (server.isRunning()) && (selMax - selMin == 0)) {
            textScrolling = true;
        }
        mouseInConsoleOutput = false;
    }//GEN-LAST:event_consoleOutputMouseExited

    private void consoleOutputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_consoleOutputFocusGained
        if (server.isRunning()) {
            textScrolling = false;
        }
    }//GEN-LAST:event_consoleOutputFocusGained

    private void consoleOutputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_consoleOutputFocusLost
        int selMin = consoleOutput.getSelectionStart();
        int selMax = consoleOutput.getSelectionEnd();
        if ((selMax - selMin == 0) && (server.isRunning()) && (!mouseInConsoleOutput)) {
            textScrolling = true;
        }
    }//GEN-LAST:event_consoleOutputFocusLost

    private void tabberKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tabberKeyTyped
        if(tabber.getSelectedIndex() == 0) {
            giveInputFocus(evt);
        }
    }//GEN-LAST:event_tabberKeyTyped

    private void jList1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jList1KeyTyped
        giveInputFocus(evt);
    }//GEN-LAST:event_jList1KeyTyped

    private void sayCheckBoxKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sayCheckBoxKeyTyped
        giveInputFocus(evt);
    }//GEN-LAST:event_sayCheckBoxKeyTyped

    private void submitButtonKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_submitButtonKeyTyped
        giveInputFocus(evt);
    }//GEN-LAST:event_submitButtonKeyTyped

    private void consoleOutputKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_consoleOutputKeyTyped
        giveInputFocus(evt);
    }//GEN-LAST:event_consoleOutputKeyTyped

    private void consoleInputKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_consoleInputKeyPressed
        if (!inputHistory.isEmpty()) {
            if (evt.getKeyCode() == 38) {
                inputHistoryIndex++;
                if (inputHistoryIndex > inputHistory.size()) {
                    inputHistoryIndex = 0;
                }
                if (inputHistoryIndex == inputHistory.size()) {
                    consoleInput.setText("");
                } else {
                    consoleInput.setText(inputHistory.get(inputHistoryIndex));
                }
            } else if (evt.getKeyCode() == 40) {
                inputHistoryIndex--;
                if (inputHistoryIndex < 0) {
                    inputHistoryIndex = inputHistory.size();
                }
                if (inputHistoryIndex == inputHistory.size()) {
                    consoleInput.setText("");
                } else {
                    consoleInput.setText(inputHistory.get(inputHistoryIndex));
                }
            }
        }
    }//GEN-LAST:event_consoleInputKeyPressed

    private void customLaunchCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customLaunchCheckBoxActionPerformed
        config.cmdLine.setUseCustomLaunch(customLaunchCheckBox.isSelected());
        cmdLineField.setText(config.cmdLine.parseCmdLine());
        if (customLaunchCheckBox.isSelected()) {
            if (java.util.regex.Pattern.matches("^\\s*$", config.cmdLine.getCustomLaunch())) {
                config.cmdLine.setUseCustomLaunch(false);
                config.cmdLine.setCustomLaunch(config.cmdLine.parseCmdLine());
                config.cmdLine.setUseCustomLaunch(true);
                cmdLineField.setText(config.cmdLine.getCustomLaunch());
            }
            cmdLineField.setEditable(true);
            javaExecField.setEditable(false);
            javaExecBrowseButton.setEnabled(false);
            serverJarField.setEditable(false);
            serverJarBrowseButton.setEnabled(false);
            bukkitCheckBox.setEnabled(false);
            xmxMemoryField.setEditable(false);
            xincgcCheckBox.setEnabled(false);
            extraArgsField.setEditable(false);
        } else {
            if (java.util.regex.Pattern.matches("^\\s*$", config.cmdLine.getCustomLaunch())) {
                config.cmdLine.setCustomLaunch(config.cmdLine.parseCmdLine());
            }
            cmdLineField.setEditable(false);
            javaExecField.setEditable(true);
            javaExecBrowseButton.setEnabled(true);
            serverJarField.setEditable(true);
            serverJarBrowseButton.setEnabled(true);
            bukkitCheckBox.setEnabled(true);
            xmxMemoryField.setEditable(true);
            xincgcCheckBox.setEnabled(true);
            extraArgsField.setEditable(true);
        }
    }//GEN-LAST:event_customLaunchCheckBoxActionPerformed

    private void cmdLineFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdLineFieldActionPerformed
        config.cmdLine.setCustomLaunch(cmdLineField.getText());
        if (java.util.regex.Pattern.matches("^\\s*$", config.cmdLine.getCustomLaunch())) {
            config.cmdLine.setUseCustomLaunch(false);
            config.cmdLine.setCustomLaunch(config.cmdLine.parseCmdLine());
            config.cmdLine.setUseCustomLaunch(true);
            cmdLineField.setText(config.cmdLine.getCustomLaunch());
        }
    }//GEN-LAST:event_cmdLineFieldActionPerformed
    
    private void saveWorldsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveWorldsButtonActionPerformed
        this.sendInput("save-all");
    }//GEN-LAST:event_saveWorldsButtonActionPerformed

    private void saveBackupControlButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveBackupControlButtonActionPerformed
        pathsToBackup.clear();
        try {
            int i = 0;
            while(true) {
                pathsToBackup.add(backupFileChooser.getCheckingRoots()[i].getLastPathComponent().toString());
                i++;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            config.backups.setPathsToBackup(pathsToBackup);
        }
        saveConfig();
    }//GEN-LAST:event_saveBackupControlButtonActionPerformed

    private void backupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupButtonActionPerformed
        config.backups.setPath(backupPathField.getText());
        backup();
    }//GEN-LAST:event_backupButtonActionPerformed

    private void tabberStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabberStateChanged
        if (tabber.getSelectedIndex() == 3) {
            refreshBackupFileChooser();
        }
    }//GEN-LAST:event_tabberStateChanged

    private void backupControlRefreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupControlRefreshButtonActionPerformed
        refreshBackupFileChooser();
    }//GEN-LAST:event_backupControlRefreshButtonActionPerformed

    private void backupPathBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupPathBrowseButtonActionPerformed
        try {
            final JFileChooser fc = new JFileChooser(new File(backupPathField.getText()).getCanonicalPath());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(getFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                backupPathField.setText(fc.getSelectedFile().getPath());
                config.backups.setPath(fc.getSelectedFile().getPath());
            }
        } catch (IOException e) {
            System.err.println("[GUI] Error retrieving program path.");
        }
    }//GEN-LAST:event_backupPathBrowseButtonActionPerformed

    private void backupPathFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupPathFieldActionPerformed
        config.backups.setPath(backupPathField.getText());
    }//GEN-LAST:event_backupPathFieldActionPerformed

    private void zipBackupCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zipBackupCheckBoxActionPerformed
        config.backups.setZip(zipBackupCheckBox.isSelected());
    }//GEN-LAST:event_zipBackupCheckBoxActionPerformed

    private void taskListAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskListAddButtonActionPerformed
        addTaskListEntry();
    }//GEN-LAST:event_taskListAddButtonActionPerformed

    private void taskListEditButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskListEditButtonActionPerformed
        if (getEventIndexFromSelected() != -1) {
            editTaskListEntry(getEventIndexFromSelected());
        }
    }//GEN-LAST:event_taskListEditButtonActionPerformed

    private void taskListRemoveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskListRemoveButtonActionPerformed
        if (getEventIndexFromSelected() != -1) {
            if (javax.swing.JOptionPane.showConfirmDialog(this.getFrame(),
                    "Are you sure you wish to remove this event?\n"
                    + "If it is running it will be interrupted.\n",
                    "Remove scheduled task",
                    javax.swing.JOptionPane.YES_NO_OPTION) == 
                    javax.swing.JOptionPane.YES_OPTION) {
                int index = getEventIndexFromSelected();
                try {
                    scheduler.interrupt(JobKey.jobKey(config.schedule.getEvents()
                            .get(index).getName()));
                    scheduler.deleteJob(JobKey.jobKey(config.schedule.getEvents()
                            .get(index).getName()));
                } catch (SchedulerException se) {
                    System.out.println("Error removing old task");
                }
                config.schedule.getEvents().remove(config.schedule.getEvents()
                        .get(index));
                taskList.removeElement(taskList.getElementAt(index));
                config.save();
            }
        }
    }//GEN-LAST:event_taskListRemoveButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JButton backupButton;
    public javax.swing.JButton backupControlRefreshButton;
    public it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree backupFileChooser;
    public javax.swing.JPanel backupFileChooserPanel;
    public javax.swing.JButton backupPathBrowseButton;
    public javax.swing.JTextField backupPathField;
    public javax.swing.JLabel backupPathLabel;
    public javax.swing.JPanel backupSettingsPanel;
    public javax.swing.JTextPane backupStatusLog;
    public javax.swing.JPanel backupStatusPanel;
    public javax.swing.JPanel backupTab;
    public javax.swing.JCheckBox bukkitCheckBox;
    public javax.swing.JTextField cmdLineField;
    public javax.swing.JTextField consoleInput;
    public javax.swing.JPanel consoleInputPanel;
    public javax.swing.JTextPane consoleOutput;
    public javax.swing.JPanel consoleOutputPanel;
    public javax.swing.JCheckBox customLaunchCheckBox;
    public javax.swing.JTextField extraArgsField;
    public javax.swing.JLabel extraArgsLabel;
    public javax.swing.JPanel guiConfigTab;
    public javax.swing.JPanel guiInfoPanel;
    public javax.swing.JTextField inputHistoryMaxSizeField;
    public javax.swing.JLabel inputHistoryMaxSizeLabel;
    public javax.swing.JLabel jLabel1;
    public javax.swing.JLabel jLabel2;
    public javax.swing.JLabel jLabel3;
    public javax.swing.JList jList1;
    public javax.swing.JPanel jPanel4;
    public javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JScrollPane jScrollPane2;
    public javax.swing.JScrollPane jScrollPane3;
    public javax.swing.JScrollPane jScrollPane4;
    public javax.swing.JScrollPane jScrollPane5;
    public javax.swing.JButton javaExecBrowseButton;
    public javax.swing.JTextField javaExecField;
    public javax.swing.JLabel javaExecLabel;
    public javax.swing.JPanel mainPanel;
    public javax.swing.JPanel mainWindowTab;
    public javax.swing.JMenuBar menuBar;
    public javax.swing.JPanel playerListPanel;
    private javax.swing.JProgressBar progressBar;
    public javax.swing.JPanel restoreTab;
    public javax.swing.JButton saveBackupControlButton;
    public javax.swing.JButton saveGuiConfigButton;
    public javax.swing.JButton saveServerConfigButton;
    public javax.swing.JButton saveWorldsButton;
    public javax.swing.JCheckBox sayCheckBox;
    public javax.swing.JPanel schedulerTab;
    public javax.swing.JPanel serverCmdLinePanel;
    public javax.swing.JPanel serverConfigTab;
    public javax.swing.JPanel serverControlPanel;
    public javax.swing.JPanel serverInfoPanel;
    public javax.swing.JButton serverJarBrowseButton;
    public javax.swing.JTextField serverJarField;
    public javax.swing.JLabel serverJarLabel;
    public javax.swing.JButton startstopButton;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    public javax.swing.JPanel statusPanel;
    public javax.swing.JButton submitButton;
    public javax.swing.JTabbedPane tabber;
    public javax.swing.JButton taskListAddButton;
    public javax.swing.JButton taskListEditButton;
    public javax.swing.JButton taskListRemoveButton;
    public javax.swing.JList taskSchedulerList;
    public javax.swing.JPanel taskSchedulerPanel;
    public javax.swing.JTextField windowTitleField;
    public javax.swing.JLabel windowTitleLabel;
    public javax.swing.JCheckBox xincgcCheckBox;
    public javax.swing.JTextField xmxMemoryField;
    public javax.swing.JLabel xmxMemoryLabel;
    public javax.swing.JCheckBox zipBackupCheckBox;
    // End of variables declaration//GEN-END:variables

    private void enableSystemTrayIcon() {
        java.awt.TrayIcon trayIcon = null;
        if (java.awt.SystemTray.isSupported()) {
            // get the SystemTray instance
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            // load an image
            org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mcservergui.MCServerGUIApp.class).getContext().getResourceMap(MCServerGUIView.class);
            // create a action listener to listen for default action executed on the tray icon
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // execute default action of the application
                    // ...
                }
            };
            // create a popup menu
            java.awt.PopupMenu popup = new java.awt.PopupMenu();
            // create menu item for the default action
            java.awt.MenuItem defaultItem = new java.awt.MenuItem("Show");
            defaultItem.addActionListener(listener);
            popup.add(defaultItem);
            /// ... add other items
            // construct a TrayIcon
            
            trayIcon = new java.awt.TrayIcon(resourceMap.getImageIcon("imageLabel.icon").getImage(), "Test", popup);
            trayIcon.setImageAutoSize(true);
            // set the TrayIcon properties
            trayIcon.addActionListener(listener);
            // ...
            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (java.awt.AWTException e) {
                System.err.println(e);
            }
            
            // ...
        } else {
            // disable tray option in your application or
            // perform other actions
        }
    }

    private int getEventIndexFromSelected() {
        try {
            String taskname = taskList.getElementAt(
                    taskSchedulerList.getSelectedIndex()).toString()
                    .split("<br>")[0];
            for (int i = 0; i < config.schedule.getEvents().size(); i++) {
                if (config.schedule.getEvents().get(i).getName().equals(taskname)) {
                    return i;
                }
            }
            return -1;
        } catch (ArrayIndexOutOfBoundsException e) {
            return -1;
        }
    }

    // My methods
    public void addTaskListEntry() {
        JFrame mainFrame = MCServerGUIApp.getApplication().getMainFrame();
        taskDialog = new MCServerGUITaskDialog(
                mainFrame, taskList, config, scheduler, config.schedule.getEvents(), this);
        taskDialog.setLocationRelativeTo(mainFrame);
        MCServerGUIApp.getApplication().show(taskDialog);
    }

    public void editTaskListEntry(int i) {
        JFrame mainFrame = MCServerGUIApp.getApplication().getMainFrame();
        taskDialog = new MCServerGUITaskDialog(
                mainFrame, taskList, config, scheduler, config.schedule.getEvents(), this,
                config.schedule.getEvents().get(i));
        taskDialog.setLocationRelativeTo(mainFrame);
        MCServerGUIApp.getApplication().show(taskDialog);
    }

    /*private void addPathToBackup(String addPath) {
        if (!pathsToBackup.contains(addPath)) {
            pathsToBackup.add(addPath);
            config.backups.setPathsToBackup(pathsToBackup);
        }
    }

    private void removePathFromBackup(String remPath) {
        if (pathsToBackup.contains(remPath)) {
            pathsToBackup.remove(remPath);
            config.backups.setPathsToBackup(pathsToBackup);
        }
    }*/

    public static javax.swing.tree.TreePath createTreePath(File f) {
        List<File> path = new ArrayList<File>();
        path.add(f);
        while((f = f.getParentFile()) != null) {
            path.add(0,f);
        }
        return new javax.swing.tree.TreePath(path.toArray());
    }

    public javax.swing.tree.TreePath[] createTreePathArray(List<String> paths) {
        javax.swing.tree.TreePath[] treepatharray = new javax.swing.tree.TreePath[paths.size()];
        int i = 0;
        while (i < paths.size()) {
            treepatharray[i] = createTreePath(new File(paths.get(i)));
            i++;
        }
        return treepatharray;
    }

    private void refreshBackupFileChooser() {
        if (!controlState.equals("BACKUP")) {
            backupFileChooser.updateUI();
        }
    }

    public void backup() {
        stateBeforeBackup = controlState;
        controlSwitcher("BACKUP");
        statusBeforeBackup = statusMessageLabel.getText();
        statusMessageLabel.setText("Backing up...");
        if (server.isRunning()) {
            sendInput("save-off");
            sendInput("say Backing up server...");
        }
        new File(config.backups.getPath()).mkdir(); // Creates backup directory if it doesn't exist
        MCServerGUIBackup backup = new MCServerGUIBackup(config, backupStatusLog);
        backupStatusLog.setText("");
        if (backup.startBackup()) {
            backup.addObserver(this);
        } else {
            controlSwitcher("!BACKUP");
        }
    }

    /**
     * Verifies that the text entered is a number before focus is released.  If not, it shows the tooltip.
     */
    class numberVerifier extends javax.swing.InputVerifier {
        @Override public boolean verify(javax.swing.JComponent input) {
            javax.swing.JTextField tf = (javax.swing.JTextField) input;
            if (java.util.regex.Pattern.matches("^\\d{1,4}$", tf.getText())){
                return true;
            } else {
                tf.getActionMap().get("postTip").actionPerformed(new ActionEvent(tf, ActionEvent.ACTION_PERFORMED, "postTip"));
                return false;
            }
        }
    }

    /**
     * Sends whatever is in the console box to the server as is.
     */
    public void sendInput() {
        sendInput(false);
    }

    /**
     * Sends whatever is in the console box to the server.
     * @param shouldSay determines if "say " should be prepended to the text.
     */
    public void sendInput(boolean shouldSay) {
        String stringToSend = consoleInput.getText();
        if (inputHistory.size() >= config.getInputHistoryMaxSize()) {
            inputHistory.remove(inputHistory.size() - 1);
        }
        inputHistory.add(0, stringToSend);
        if ((sayCheckBox.isSelected()) && (!shouldSay)) {
            server.send("say " + stringToSend);
        } else if ((!sayCheckBox.isSelected()) && (shouldSay)) {
            server.send("say " + stringToSend);
        } else {
            server.send(stringToSend);
        }
        consoleInput.setText("");
        inputHistoryIndex = -1;
    }

    /**
     * Sends a String to the server.
     * @param s String to send to the server
     */
    public void sendInput(String s) {
        //if (getControlState().equals("ON")) {
        server.send(s);
        //} else {
        //    System.out.println("Server is not running, cannot send: " + s);
        //}
    }

    /**
     * Determines if keyboard focus should be given to the console input field based on the passed KeyEvent.
     * Basically any alpha-numeric keys will cause focus to be granted.
     * @param evt KeyEvent to be passed in.
     */
    public void giveInputFocus(java.awt.event.KeyEvent evt) {
        if ((evt.getKeyChar() != java.awt.event.KeyEvent.CHAR_UNDEFINED) && (consoleInput.isEnabled()) && ((int)evt.getKeyChar() > 32)) {
            if (consoleInput.requestFocusInWindow()) {
                consoleInput.setText(consoleInput.getText() + evt.getKeyChar());
            }
        }
    }

    /**
     * Defines the MainWorker so the view can have access.
     * @param newMainWorker
     */
    public void setMainWorker(MCServerGUIMainWorker newMainWorker) {
        mainWorker = newMainWorker;
    }

    /**
     * Initializes the config file if necessary and sets all the gui elements to their config'd values
     * Usually this is only called once during the constructor.
     */
    public void initConfig() {
        if (config.load()) {
            consoleOutput.setText("Configuration file loaded succesfully!");
        } else {
            consoleOutput.setText("Configuration file not found or invalid!  Creating new config file with default values.");
        }
        updateGuiWithConfigValues();
        saveConfig();
        initSchedule();
    }

    public void initSchedule() {
        for (int i = 0; i < config.schedule.getEvents().size(); i++) {
            scheduleEvent(config.schedule.getEvents().get(i), scheduler, this);
        }
    }

    public void updateGuiWithConfigValues() {
        zipBackupCheckBox.setSelected(config.backups.getZip());
        pathsToBackup = config.backups.getPathsToBackup();
        backupPathField.setText(config.backups.getPath());
        backupFileChooser.setCheckingPaths(createTreePathArray(pathsToBackup));
        windowTitleField.setText(config.getWindowTitle());
        getFrame().setTitle(windowTitleField.getText());
        javaExecField.setText(config.cmdLine.getJavaExec());
        serverJarField.setText(config.cmdLine.getServerJar());
        bukkitCheckBox.setSelected(config.cmdLine.getBukkit());
        xmxMemoryField.setText(config.cmdLine.getXmx());
        xincgcCheckBox.setSelected(config.cmdLine.getXincgc());
        extraArgsField.setText(config.cmdLine.getExtraArgs());
        customLaunchCheckBox.setSelected(config.cmdLine.getUseCustomLaunch());
        inputHistoryMaxSizeField.setText(Integer.toString(config.getInputHistoryMaxSize()));
        if (!config.cmdLine.getUseCustomLaunch()) {
            if (java.util.regex.Pattern.matches("^\\s*$", config.cmdLine.getCustomLaunch())) {
                config.cmdLine.setCustomLaunch(config.cmdLine.parseCmdLine());
            }
        } else {
            if (java.util.regex.Pattern.matches("^\\s*$", config.cmdLine.getCustomLaunch())) {
                config.cmdLine.setUseCustomLaunch(false);
                config.cmdLine.setCustomLaunch(config.cmdLine.parseCmdLine());
                config.cmdLine.setUseCustomLaunch(true);
            }
            cmdLineField.setEditable(true);
            javaExecField.setEditable(false);
            javaExecBrowseButton.setEnabled(false);
            serverJarField.setEditable(false);
            serverJarBrowseButton.setEnabled(false);
            bukkitCheckBox.setEnabled(false);
            xmxMemoryField.setEditable(false);
            xincgcCheckBox.setEnabled(false);
            extraArgsField.setEditable(false);
        }
        backupFileChooser.addTreeCheckingListener(new BackupFileChooserCheckingListener());
        cmdLineField.setText(config.cmdLine.parseCmdLine());
        for (int i = 0; i < config.schedule.getEvents().size(); i++) {
            taskList.add(config.schedule.getEvents().get(i).getName()
                    + "<br><font size=2>"
                    + config.schedule.getEvents().get(i).getTask());
        }
    }

    /**
     * Saves the config file with any changes made by the user through the gui.
     */
    public void saveConfig() {
        String temp = statusMessageLabel.getText();
        statusMessageLabel.setText("Saving configuration...");
        config.setWindowTitle(windowTitleField.getText());
        getFrame().setTitle(windowTitleField.getText());
        config.setInputHistoryMaxSize(Integer.parseInt(inputHistoryMaxSizeField.getText()));
        config.cmdLine.setXmx(xmxMemoryField.getText());
        config.cmdLine.setExtraArgs(extraArgsField.getText());
        config.cmdLine.setServerJar(serverJarField.getText());
        config.cmdLine.setJavaExec(javaExecField.getText());
        if (config.cmdLine.getUseCustomLaunch()) {
            config.cmdLine.setCustomLaunch(cmdLineField.getText());
            if (java.util.regex.Pattern.matches("^\\s*$", cmdLineField.getText())) {
                config.cmdLine.setUseCustomLaunch(false);
                config.cmdLine.setCustomLaunch(config.cmdLine.parseCmdLine());
                config.cmdLine.setUseCustomLaunch(true);
            } else {
                config.cmdLine.setCustomLaunch(config.cmdLine.parseCmdLine());
            }
            cmdLineField.setText(config.cmdLine.parseCmdLine());
        } else {
            cmdLineField.setText(config.cmdLine.parseCmdLine());
            if (java.util.regex.Pattern.matches("^\\s*$", config.cmdLine.getCustomLaunch())) {
                config.cmdLine.setCustomLaunch(config.cmdLine.parseCmdLine());
            }
        }
        config.backups.setPathsToBackup(pathsToBackup);
        config.backups.setPath(backupPathField.getText());
        config.save();
        statusMessageLabel.setText(temp);
    }

    /**
     * As long as textScrolling is true, this will cause the consoleOutput to be scrolled to the bottom.
     */
    public void scrollText() {
        if (textScrolling) {
            consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
        }
    }

    public boolean isRestarting() { return restarting; }

    public void restartServer() { restartServer(0); }

    public void restartServer(int delay) {
        restarting = true;
        stopServer();
        System.out.println("Sent stop");
        try {
            System.out.println("Sleeping for " + delay);
            Thread.sleep(delay * 1000);
        } catch (InterruptedException ie) {
            System.out.println("Interrupted while waiting to restart server.");
        }
        try {
            while (!getControlState().equals("OFF")) {
                System.out.println("Waiting for server to be stopped");
                Thread.sleep(1000);
            }
        } catch (InterruptedException ie) {
            System.out.println("Interrupted while waiting for server to be shut down completely.");
        }
        startServer();
        System.out.println("Sent start");
        restarting = false;
    }

    /**
     * Starts the Minecraft server and verifies that it started properly.
     */
    public void startServer() {
        if (controlState.equals("OFF")) {
            consoleOutput.setText("");
            server.setCmdLine(config.cmdLine.getCmdLine());
            if (server.start().equals("SUCCESS")) {
            } else if (server.start().equals("ERROR")) {
                consoleOutput.setText("[GUI] Unknown error occured while launching the server.");
            } else if (server.start().equals("INVALIDJAR")) {
                consoleOutput.setText("[GUI] The jar file you specified is not a valid file."
                        + "  Please make corrections on the Server Config tab.");
            }
        }
    }

    /**
     * Tells the Minecraft server to stop.
     */
    public void stopServer() {
        if (controlState.equals("ON")) {
            statusMessageLabel.setText("Stopping server...");
            server.stop();
        }
    }

    /**
     * Adds text to the end of the Console Output box.
     * @param textToAdd String of text to add.
     */
    public void addTextToConsoleOutput(String textToAdd) {
        try
        {
            ((HTMLEditorKit)consoleOutput.getEditorKit())
                    .insertHTML((HTMLDocument)consoleOutput.getDocument(),
                    consoleOutput.getDocument().getEndPosition().getOffset()-1,
                    parser.parseText(textToAdd),
                    1, 0, null);
        } catch ( Exception e ) {
            System.out.println("Error appending text to console output");
        }
    }

    /**
     * Observer update() method.  Called when the ServerModel has messages for the View
     * @param o
     * @param arg Message
     */
    @Override public void update(Observable o, Object arg) {
        if (arg.equals("newOutput")) {
            String newOutput = server.getReceived();
            if ((newOutput != null) && (!newOutput.equals("null\n"))) {
                addTextToConsoleOutput(newOutput);
            }
        }

        if (arg.equals("serverStopped")) {
            controlSwitcher("OFF");
        }
        if (arg.equals("serverStarted")) {
            controlSwitcher("ON");
        }

        if (arg.equals("finishedBackup")) {
            if (server.isRunning()) {
                sendInput("say Server backup complete!");
                sendInput("save-on");
            }
            statusMessageLabel.setText(statusBeforeBackup);
            controlSwitcher("!BACKUP");
        }
    }

    /**
     * Switches GUI components into specific states based on param passed.
     * @param serverState Typically the state of the server. "ON" or "OFF"
     */
    private void controlSwitcher(String serverState) {
        controlState = serverState;
        if (serverState.equals("ON")) {
            // Switch GUI control to "ON" status
            startstopButton.setText("Stop");
            consoleInput.setEnabled(true);
            submitButton.setEnabled(true);
            statusMessageLabel.setText("Server Running");
            textScrolling = true;
            saveWorldsButton.setEnabled(true);
        } else if (serverState.equals("OFF")) {
            // Switch GUI controls to "OFF" status
            startstopButton.setText("Start");
            consoleInput.setEnabled(false);
            submitButton.setEnabled(false);
            statusMessageLabel.setText("Server Stopped");
            textScrolling = false;
            mouseInConsoleOutput = false;
            saveWorldsButton.setEnabled(false);
        } else if (serverState.equals("BADCONFIG")) {
            startstopButton.setEnabled(false);
        } else if (serverState.equals("BACKUP")) {
            startstopButton.setEnabled(false);
            saveWorldsButton.setEnabled(false);
            backupControlRefreshButton.setEnabled(false);
            backupButton.setEnabled(false);
            saveGuiConfigButton.setEnabled(false);
            saveServerConfigButton.setEnabled(false);
            saveBackupControlButton.setEnabled(false);
            backupPathField.setEnabled(false);
            backupPathBrowseButton.setEnabled(false);
            backupFileChooser.setEnabled(false);
        } else if (serverState.equals("!BACKUP")) {
            startstopButton.setEnabled(true);
            saveWorldsButton.setEnabled(true);
            backupControlRefreshButton.setEnabled(true);
            backupButton.setEnabled(true);
            saveGuiConfigButton.setEnabled(true);
            saveServerConfigButton.setEnabled(true);
            saveBackupControlButton.setEnabled(true);
            backupPathField.setEnabled(true);
            backupPathBrowseButton.setEnabled(true);
            backupFileChooser.setEnabled(true);
            controlSwitcher(stateBeforeBackup);
        }
    }

    public String getControlState() {
        return controlState;
    }

    private MCServerGUIServerModel server;
    private MCServerGUIServerReceiver serverReceiver;
    private MCServerGUIMainWorker mainWorker;
    private boolean textScrolling;
    private boolean mouseInConsoleOutput;
    private List<String> inputHistory;
    private List<String> pathsToBackup;
    private int inputHistoryIndex;
    private String controlState;
    private String stateBeforeBackup;
    private String statusBeforeBackup;
    private mcservergui.fileexplorer.FileSystemModel backupFileSystem;
    private MCServerGUIConfig config;
    private boolean badConfig;
    private Scheduler scheduler;
    private MCServerGUIListModel taskList;
    private boolean restarting;
    private MCServerGUIConsoleParser parser;

    //Auto created
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;

    private MCServerGUITaskDialog taskDialog;
}

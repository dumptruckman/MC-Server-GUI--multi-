/*
 * GUI.java
 */
package mcservergui.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLDocument;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingModel.CheckingMode;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.*;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import org.jdesktop.application.TaskService;
import org.quartz.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import mcservergui.backup.Backup;
import mcservergui.config.Config;
import mcservergui.tools.ConsoleParser;
import mcservergui.tools.RegexVerifier;
import mcservergui.mcserver.MCServerModel;
import mcservergui.task.TaskDialog;
import mcservergui.Main;
import mcservergui.config.ServerProperties;
import mcservergui.proxyserver.PlayerList;
import mcservergui.webinterface.WebInterface;
import mcservergui.task.event.EventModel;
import static mcservergui.task.event.EventScheduler.*;
import static mcservergui.tools.TimeTools.*;

/**
 * The application's main frame.
 */
public class GUI extends FrameView implements Observer {
    
    public GUI(SingleFrameApplication app, MCServerModel newServer, Config newConfig, Scheduler scheduler) {
        super(app);

        // Sets model for backup file check box tree
        backupFileSystem = new mcservergui.fileexplorer.FileSystemModel(".");
        // Sets model for player list
        playerListModel = new PlayerList();
        // Initializes the custom Button Combo Boxes
        customButtonBoxModel1 = new javax.swing.DefaultComboBoxModel();
        customButtonBoxModel1.addElement("Edit Tasks");
        customButtonBoxModel2 = new javax.swing.DefaultComboBoxModel();
        customButtonBoxModel2.addElement("Edit Tasks");
        propagatingChecks = false;

        initComponents();
        fixComponents();
        // GUI starts unhidden, indication of that:
        isHidden = false;

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                //serverStatusLabel.setText("");
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
                    //serverStatusLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

        // Sets a control+s hotkey for the say checkbox
        sayCheckBox.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK),"sayOn");
        sayCheckBox.getActionMap().put("sayOn", sayToggle);
        // Sets shift+enter as a hotkey to reverse the say setting.
        consoleInput.getInputMap().put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.SHIFT_MASK),"sayOn");
        consoleInput.getActionMap().put("sayOn", saySend);

        // Grabs scheduler from params
        this.scheduler = scheduler;

        serverProperties = new ServerProperties();
        config = newConfig;
        server = newServer;
        server.addObserver(serverProperties);
        server.setServerProps(serverProperties);
        getFrame().setTitle(config.getWindowTitle());
        controlSwitcher("OFF");

        inputHistory = new ArrayList<String>();
        inputHistoryIndex = -1;

        enableSystemTrayIcon();

        parser = new ConsoleParser(config.display, this);

        webServer = new WebInterface(this);
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = Main.getApplication().getMainFrame();
            aboutBox = new AboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        Main.getApplication().show(aboutBox);
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
        playerList = new javax.swing.JList();
        consoleInputPanel = new javax.swing.JPanel();
        consoleInput = new javax.swing.JTextField();
        submitButton = new javax.swing.JButton();
        sayCheckBox = new javax.swing.JCheckBox();
        serverControlPanel = new javax.swing.JPanel();
        startstopButton = new javax.swing.JButton();
        saveWorldsButton = new javax.swing.JButton();
        customCombo1 = new javax.swing.JComboBox();
        customButton1 = new javax.swing.JButton();
        customCombo2 = new javax.swing.JComboBox();
        customButton2 = new javax.swing.JButton();
        serverInfoPanel = new javax.swing.JPanel();
        serverCpuUsageLabel = new javax.swing.JLabel();
        serverCpuUsage = new javax.swing.JLabel();
        serverMemoryUsageLabel = new javax.swing.JLabel();
        serverMemoryUsage = new javax.swing.JLabel();
        receivingBytesLabel = new javax.swing.JLabel();
        transmittingBytesLabel = new javax.swing.JLabel();
        receivingBytes = new javax.swing.JLabel();
        transmittingBytes = new javax.swing.JLabel();
        guiInfoPanel = new javax.swing.JPanel();
        versionLabel = new javax.swing.JLabel();
        guiCpuUsageLabel = new javax.swing.JLabel();
        guiCpuUsage = new javax.swing.JLabel();
        guiMemoryUsageLabel = new javax.swing.JLabel();
        guiMemoryUsage = new javax.swing.JLabel();
        useNetStat = new javax.swing.JCheckBox();
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
        proxyServerPanel = new javax.swing.JPanel();
        useProxyCheckBox = new javax.swing.JCheckBox();
        extPortLabel = new javax.swing.JLabel();
        extPortField = new javax.swing.JTextField();
        serverPropertiesPanel = new javax.swing.JPanel();
        allowFlightCheckBox = new javax.swing.JCheckBox();
        allowNetherCheckBox = new javax.swing.JCheckBox();
        levelNameLabel = new javax.swing.JLabel();
        levelNameField = new javax.swing.JTextField();
        levelSeedLabel = new javax.swing.JLabel();
        levelSeedField = new javax.swing.JTextField();
        maxPlayersSpinner = new javax.swing.JSpinner();
        maxPlayersLabel = new javax.swing.JLabel();
        onlineModeCheckBox = new javax.swing.JCheckBox();
        jCheckBox1 = new javax.swing.JCheckBox();
        pvpCheckBox = new javax.swing.JCheckBox();
        serverIpLabel = new javax.swing.JLabel();
        serverIpField = new javax.swing.JTextField();
        serverPortLabel = new javax.swing.JLabel();
        serverPortField = new javax.swing.JTextField();
        spawnAnimalsCheckBox = new javax.swing.JCheckBox();
        spawnMonstersCheckBox = new javax.swing.JCheckBox();
        spawnProtectionLabel = new javax.swing.JLabel();
        spawnProtectionField = new javax.swing.JTextField();
        viewDistanceLabel = new javax.swing.JLabel();
        viewDistanceSpinner = new javax.swing.JSpinner();
        whiteListCheckBox = new javax.swing.JCheckBox();
        guiConfigTab = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        windowTitleLabel = new javax.swing.JLabel();
        windowTitleField = new javax.swing.JTextField();
        inputHistoryMaxSizeLabel = new javax.swing.JLabel();
        inputHistoryMaxSizeField = new javax.swing.JTextField();
        startServerOnLaunchCheckBox = new javax.swing.JCheckBox();
        commandPrefixLabel = new javax.swing.JLabel();
        commandPrefixField = new javax.swing.JTextField();
        saveGuiConfigButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        textColorLabel = new javax.swing.JLabel();
        bgColorLabel = new javax.swing.JLabel();
        infoColorLabel = new javax.swing.JLabel();
        warningColorLabel = new javax.swing.JLabel();
        severeColorLabel = new javax.swing.JLabel();
        textColorBox = new javax.swing.JTextField();
        bgColorBox = new javax.swing.JTextField();
        infoColorBox = new javax.swing.JTextField();
        warningColorBox = new javax.swing.JTextField();
        severeColorBox = new javax.swing.JTextField();
        textSizeLabel = new javax.swing.JLabel();
        textSizeField = new javax.swing.JSpinner();
        backupTab = new javax.swing.JPanel();
        backupButton = new javax.swing.JButton();
        backupSettingsPanel = new javax.swing.JPanel();
        backupPathLabel = new javax.swing.JLabel();
        backupPathField = new javax.swing.JTextField();
        backupPathBrowseButton = new javax.swing.JButton();
        zipBackupCheckBox = new javax.swing.JCheckBox();
        clearLogCheckBox = new javax.swing.JCheckBox();
        saveBackupControlButton = new javax.swing.JButton();
        backupFileChooserPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        backupFileChooser = new it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree();
        backupStatusPanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        backupStatusLog = new javax.swing.JTextPane();
        backupControlRefreshButton = new javax.swing.JButton();
        schedulerTab = new javax.swing.JPanel();
        taskSchedulerPanel = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        taskSchedulerList = new javax.swing.JList();
        taskListAddButton = new javax.swing.JButton();
        taskListEditButton = new javax.swing.JButton();
        taskListRemoveButton = new javax.swing.JButton();
        pauseSchedulerButton = new javax.swing.JToggleButton();
        webInterfaceTab = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        webPortLabel = new javax.swing.JLabel();
        webPortField = new javax.swing.JTextField();
        useWebInterfaceCheckBox = new javax.swing.JCheckBox();
        webPasswordLabel = new javax.swing.JLabel();
        webPasswordField = new javax.swing.JPasswordField();
        showWebPasswordButton = new javax.swing.JToggleButton();
        disableGetOutputNotificationsCheckBox = new javax.swing.JCheckBox();
        webLogPanel = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        webLog = new javax.swing.JTextPane();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        hideMenu = new javax.swing.JMenu();
        versionNotifier = new javax.swing.JMenu();
        launchSupportPage = new javax.swing.JMenuItem();
        viewChangeLog = new javax.swing.JMenuItem();
        downloadLatestVersion = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        serverStatusLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        statusBarJob = new javax.swing.JLabel();

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

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mcservergui.Main.class).getContext().getResourceMap(GUI.class);
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
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                consoleOutputMouseClicked(evt);
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
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
        );

        playerListPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("playerListPanel.border.title"))); // NOI18N
        playerListPanel.setName("playerListPanel"); // NOI18N
        playerListPanel.setPreferredSize(new java.awt.Dimension(125, 204));

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        playerList.setModel(playerListModel);
        playerList.setToolTipText(resourceMap.getString("playerList.toolTipText")); // NOI18N
        playerList.setFocusable(false);
        playerList.setName("playerList"); // NOI18N
        playerList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                playerListMouseClicked(evt);
            }
        });
        playerList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                playerListKeyTyped(evt);
            }
        });
        jScrollPane2.setViewportView(playerList);

        javax.swing.GroupLayout playerListPanelLayout = new javax.swing.GroupLayout(playerListPanel);
        playerListPanel.setLayout(playerListPanelLayout);
        playerListPanelLayout.setHorizontalGroup(
            playerListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE)
        );
        playerListPanelLayout.setVerticalGroup(
            playerListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
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
                .addComponent(consoleInput, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
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

        customCombo1.setModel(customButtonBoxModel1);
        customCombo1.setToolTipText(resourceMap.getString("customCombo1.toolTipText")); // NOI18N
        customCombo1.setName("customCombo1"); // NOI18N

        customButton1.setText(resourceMap.getString("customButton1.text")); // NOI18N
        customButton1.setToolTipText(resourceMap.getString("customButton1.toolTipText")); // NOI18N
        customButton1.setMargin(new java.awt.Insets(2, 2, 2, 2));
        customButton1.setName("customButton1"); // NOI18N
        customButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customButton1ActionPerformed(evt);
            }
        });

        customCombo2.setModel(customButtonBoxModel2);
        customCombo2.setToolTipText(resourceMap.getString("customCombo2.toolTipText")); // NOI18N
        customCombo2.setName("customCombo2"); // NOI18N

        customButton2.setText(resourceMap.getString("customButton2.text")); // NOI18N
        customButton2.setToolTipText(resourceMap.getString("customButton2.toolTipText")); // NOI18N
        customButton2.setMargin(new java.awt.Insets(2, 2, 2, 2));
        customButton2.setName("customButton2"); // NOI18N
        customButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customButton2ActionPerformed(evt);
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, serverControlPanelLayout.createSequentialGroup()
                .addGroup(serverControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(customCombo2, 0, 93, Short.MAX_VALUE)
                    .addComponent(customCombo1, 0, 93, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(customButton1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(customButton2, javax.swing.GroupLayout.Alignment.TRAILING)))
        );
        serverControlPanelLayout.setVerticalGroup(
            serverControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverControlPanelLayout.createSequentialGroup()
                .addGroup(serverControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startstopButton)
                    .addComponent(saveWorldsButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(customCombo1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(customButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(customCombo2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(customButton2))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        serverInfoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Server Information"));
        serverInfoPanel.setName("serverInfoPanel"); // NOI18N

        serverCpuUsageLabel.setText(resourceMap.getString("serverCpuUsageLabel.text")); // NOI18N
        serverCpuUsageLabel.setName("serverCpuUsageLabel"); // NOI18N

        serverCpuUsage.setText(resourceMap.getString("serverCpuUsage.text")); // NOI18N
        serverCpuUsage.setName("serverCpuUsage"); // NOI18N

        serverMemoryUsageLabel.setText(resourceMap.getString("serverMemoryUsageLabel.text")); // NOI18N
        serverMemoryUsageLabel.setName("serverMemoryUsageLabel"); // NOI18N

        serverMemoryUsage.setText(resourceMap.getString("serverMemoryUsage.text")); // NOI18N
        serverMemoryUsage.setName("serverMemoryUsage"); // NOI18N

        receivingBytesLabel.setText(resourceMap.getString("receivingBytesLabel.text")); // NOI18N
        receivingBytesLabel.setName("receivingBytesLabel"); // NOI18N

        transmittingBytesLabel.setText(resourceMap.getString("transmittingBytesLabel.text")); // NOI18N
        transmittingBytesLabel.setName("transmittingBytesLabel"); // NOI18N

        receivingBytes.setText(resourceMap.getString("receivingBytes.text")); // NOI18N
        receivingBytes.setName("receivingBytes"); // NOI18N

        transmittingBytes.setText(resourceMap.getString("transmittingBytes.text")); // NOI18N
        transmittingBytes.setName("transmittingBytes"); // NOI18N

        javax.swing.GroupLayout serverInfoPanelLayout = new javax.swing.GroupLayout(serverInfoPanel);
        serverInfoPanel.setLayout(serverInfoPanelLayout);
        serverInfoPanelLayout.setHorizontalGroup(
            serverInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverInfoPanelLayout.createSequentialGroup()
                .addGroup(serverInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(serverInfoPanelLayout.createSequentialGroup()
                        .addComponent(serverCpuUsageLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(serverCpuUsage, javax.swing.GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE))
                    .addGroup(serverInfoPanelLayout.createSequentialGroup()
                        .addComponent(serverMemoryUsageLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(serverMemoryUsage, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE))
                    .addGroup(serverInfoPanelLayout.createSequentialGroup()
                        .addComponent(receivingBytesLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(receivingBytes, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE))
                    .addGroup(serverInfoPanelLayout.createSequentialGroup()
                        .addComponent(transmittingBytesLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(transmittingBytes, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE)))
                .addContainerGap())
        );
        serverInfoPanelLayout.setVerticalGroup(
            serverInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverInfoPanelLayout.createSequentialGroup()
                .addGroup(serverInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serverCpuUsageLabel)
                    .addComponent(serverCpuUsage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serverMemoryUsageLabel)
                    .addComponent(serverMemoryUsage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(receivingBytesLabel)
                    .addComponent(receivingBytes))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(transmittingBytesLabel)
                    .addComponent(transmittingBytes))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        guiInfoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("GUI Information"));
        guiInfoPanel.setName("guiInfoPanel"); // NOI18N

        versionLabel.setText(resourceMap.getString("versionLabel.text")); // NOI18N
        versionLabel.setName("versionLabel"); // NOI18N

        guiCpuUsageLabel.setText(resourceMap.getString("guiCpuUsageLabel.text")); // NOI18N
        guiCpuUsageLabel.setName("guiCpuUsageLabel"); // NOI18N

        guiCpuUsage.setText(resourceMap.getString("guiCpuUsage.text")); // NOI18N
        guiCpuUsage.setName("guiCpuUsage"); // NOI18N

        guiMemoryUsageLabel.setText(resourceMap.getString("guiMemoryUsageLabel.text")); // NOI18N
        guiMemoryUsageLabel.setName("guiMemoryUsageLabel"); // NOI18N

        guiMemoryUsage.setText(resourceMap.getString("guiMemoryUsage.text")); // NOI18N
        guiMemoryUsage.setName("guiMemoryUsage"); // NOI18N

        useNetStat.setText(resourceMap.getString("useNetStat.text")); // NOI18N
        useNetStat.setToolTipText(resourceMap.getString("useNetStat.toolTipText")); // NOI18N
        useNetStat.setName("useNetStat"); // NOI18N

        javax.swing.GroupLayout guiInfoPanelLayout = new javax.swing.GroupLayout(guiInfoPanel);
        guiInfoPanel.setLayout(guiInfoPanelLayout);
        guiInfoPanelLayout.setHorizontalGroup(
            guiInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(guiInfoPanelLayout.createSequentialGroup()
                .addGroup(guiInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(guiInfoPanelLayout.createSequentialGroup()
                        .addComponent(guiCpuUsageLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(guiCpuUsage, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE))
                    .addGroup(guiInfoPanelLayout.createSequentialGroup()
                        .addComponent(guiMemoryUsageLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(guiMemoryUsage, javax.swing.GroupLayout.DEFAULT_SIZE, 55, Short.MAX_VALUE))
                    .addComponent(versionLabel)
                    .addComponent(useNetStat))
                .addContainerGap())
        );
        guiInfoPanelLayout.setVerticalGroup(
            guiInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(guiInfoPanelLayout.createSequentialGroup()
                .addGroup(guiInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(guiCpuUsageLabel)
                    .addComponent(guiCpuUsage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(guiInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(guiMemoryUsageLabel)
                    .addComponent(guiMemoryUsage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(versionLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useNetStat)
                .addContainerGap(19, Short.MAX_VALUE))
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
                        .addComponent(playerListPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE))
                    .addComponent(consoleInputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 498, Short.MAX_VALUE))
                .addGap(46, 46, 46))
        );
        mainWindowTabLayout.setVerticalGroup(
            mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainWindowTabLayout.createSequentialGroup()
                .addGroup(mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(playerListPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
                    .addComponent(consoleOutputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(consoleInputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mainWindowTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(serverControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(guiInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(serverInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabber.addTab(resourceMap.getString("mainWindowTab.TabConstraints.tabTitle"), mainWindowTab); // NOI18N

        serverConfigTab.setName("serverConfigTab"); // NOI18N

        serverCmdLinePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("serverCmdLinePanel.border.title"))); // NOI18N
        serverCmdLinePanel.setMaximumSize(new java.awt.Dimension(252, 239));
        serverCmdLinePanel.setName("serverCmdLinePanel"); // NOI18N
        serverCmdLinePanel.setPreferredSize(new java.awt.Dimension(252, 239));

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
                .addComponent(cmdLineField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        saveServerConfigButton.setText(resourceMap.getString("saveServerConfigButton.text")); // NOI18N
        saveServerConfigButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        saveServerConfigButton.setName("saveServerConfigButton"); // NOI18N
        saveServerConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveServerConfigButtonActionPerformed(evt);
            }
        });

        proxyServerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("proxyServerPanel.border.title"))); // NOI18N
        proxyServerPanel.setMaximumSize(new java.awt.Dimension(252, 50));
        proxyServerPanel.setName("proxyServerPanel"); // NOI18N

        useProxyCheckBox.setText(resourceMap.getString("useProxyCheckBox.text")); // NOI18N
        useProxyCheckBox.setToolTipText(resourceMap.getString("useProxyCheckBox.toolTipText")); // NOI18N
        useProxyCheckBox.setName("useProxyCheckBox"); // NOI18N
        useProxyCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useProxyCheckBoxActionPerformed(evt);
            }
        });

        extPortLabel.setLabelFor(extPortField);
        extPortLabel.setText(resourceMap.getString("extPortLabel.text")); // NOI18N
        extPortLabel.setName("extPortLabel"); // NOI18N

        extPortField.setText(resourceMap.getString("extPortField.text")); // NOI18N
        extPortField.setToolTipText(resourceMap.getString("extPortField.toolTipText")); // NOI18N
        extPortField.setInputVerifier(new RegexVerifier("^(6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)$"));
        extPortField.setName("extPortField"); // NOI18N

        javax.swing.GroupLayout proxyServerPanelLayout = new javax.swing.GroupLayout(proxyServerPanel);
        proxyServerPanel.setLayout(proxyServerPanelLayout);
        proxyServerPanelLayout.setHorizontalGroup(
            proxyServerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxyServerPanelLayout.createSequentialGroup()
                .addComponent(useProxyCheckBox)
                .addGap(18, 18, 18)
                .addComponent(extPortLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(extPortField, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE))
        );
        proxyServerPanelLayout.setVerticalGroup(
            proxyServerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxyServerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(useProxyCheckBox)
                .addComponent(extPortLabel)
                .addComponent(extPortField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        serverPropertiesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("serverPropertiesPanel.border.title"))); // NOI18N
        serverPropertiesPanel.setName("serverPropertiesPanel"); // NOI18N

        allowFlightCheckBox.setText(resourceMap.getString("allowFlightCheckBox.text")); // NOI18N
        allowFlightCheckBox.setName("allowFlightCheckBox"); // NOI18N

        allowNetherCheckBox.setSelected(true);
        allowNetherCheckBox.setText(resourceMap.getString("allowNetherCheckBox.text")); // NOI18N
        allowNetherCheckBox.setName("allowNetherCheckBox"); // NOI18N

        levelNameLabel.setText(resourceMap.getString("levelNameLabel.text")); // NOI18N
        levelNameLabel.setName("levelNameLabel"); // NOI18N

        levelNameField.setText(resourceMap.getString("levelNameField.text")); // NOI18N
        levelNameField.setName("levelNameField"); // NOI18N

        levelSeedLabel.setText(resourceMap.getString("levelSeedLabel.text")); // NOI18N
        levelSeedLabel.setName("levelSeedLabel"); // NOI18N

        levelSeedField.setText(resourceMap.getString("levelSeedField.text")); // NOI18N
        levelSeedField.setName("levelSeedField"); // NOI18N

        maxPlayersSpinner.setModel(new javax.swing.SpinnerNumberModel(20, 0, 100, 1));
        maxPlayersSpinner.setName("maxPlayersSpinner"); // NOI18N

        maxPlayersLabel.setText(resourceMap.getString("maxPlayersLabel.text")); // NOI18N
        maxPlayersLabel.setName("maxPlayersLabel"); // NOI18N

        onlineModeCheckBox.setSelected(true);
        onlineModeCheckBox.setText(resourceMap.getString("onlineModeCheckBox.text")); // NOI18N
        onlineModeCheckBox.setName("onlineModeCheckBox"); // NOI18N

        jCheckBox1.setText(resourceMap.getString("jCheckBox1.text")); // NOI18N
        jCheckBox1.setName("jCheckBox1"); // NOI18N

        pvpCheckBox.setSelected(true);
        pvpCheckBox.setText(resourceMap.getString("pvpCheckBox.text")); // NOI18N
        pvpCheckBox.setName("pvpCheckBox"); // NOI18N

        serverIpLabel.setText(resourceMap.getString("serverIpLabel.text")); // NOI18N
        serverIpLabel.setName("serverIpLabel"); // NOI18N

        serverIpField.setText(resourceMap.getString("serverIpField.text")); // NOI18N
        serverIpField.setName("serverIpField"); // NOI18N

        serverPortLabel.setLabelFor(serverPortField);
        serverPortLabel.setText(resourceMap.getString("serverPortLabel.text")); // NOI18N
        serverPortLabel.setName("serverPortLabel"); // NOI18N

        serverPortField.setText(resourceMap.getString("serverPortField.text")); // NOI18N
        serverPortField.setToolTipText(resourceMap.getString("serverPortField.toolTipText")); // NOI18N
        serverPortField.setInputVerifier(new RegexVerifier("^(6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)$"));
        serverPortField.setName("serverPortField"); // NOI18N

        spawnAnimalsCheckBox.setSelected(true);
        spawnAnimalsCheckBox.setText(resourceMap.getString("spawnAnimalsCheckBox.text")); // NOI18N
        spawnAnimalsCheckBox.setName("spawnAnimalsCheckBox"); // NOI18N

        spawnMonstersCheckBox.setSelected(true);
        spawnMonstersCheckBox.setText(resourceMap.getString("spawnMonstersCheckBox.text")); // NOI18N
        spawnMonstersCheckBox.setName("spawnMonstersCheckBox"); // NOI18N

        spawnProtectionLabel.setText(resourceMap.getString("spawnProtectionLabel.text")); // NOI18N
        spawnProtectionLabel.setName("spawnProtectionLabel"); // NOI18N

        spawnProtectionField.setText(resourceMap.getString("spawnProtectionField.text")); // NOI18N
        spawnProtectionField.setName("spawnProtectionField"); // NOI18N

        viewDistanceLabel.setText(resourceMap.getString("viewDistanceLabel.text")); // NOI18N
        viewDistanceLabel.setName("viewDistanceLabel"); // NOI18N

        viewDistanceSpinner.setModel(new javax.swing.SpinnerNumberModel(10, 3, 15, 1));
        viewDistanceSpinner.setName("viewDistanceSpinner"); // NOI18N

        whiteListCheckBox.setText(resourceMap.getString("whiteListCheckBox.text")); // NOI18N
        whiteListCheckBox.setName("whiteListCheckBox"); // NOI18N

        javax.swing.GroupLayout serverPropertiesPanelLayout = new javax.swing.GroupLayout(serverPropertiesPanel);
        serverPropertiesPanel.setLayout(serverPropertiesPanelLayout);
        serverPropertiesPanelLayout.setHorizontalGroup(
            serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                        .addComponent(maxPlayersLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(maxPlayersSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                        .addComponent(onlineModeCheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(pvpCheckBox))
                    .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, serverPropertiesPanelLayout.createSequentialGroup()
                            .addComponent(levelSeedLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(levelSeedField))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, serverPropertiesPanelLayout.createSequentialGroup()
                            .addComponent(levelNameLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(levelNameField))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, serverPropertiesPanelLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(allowFlightCheckBox)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(allowNetherCheckBox)))
                    .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                        .addComponent(serverPortLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(serverPortField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                        .addComponent(serverIpLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(serverIpField, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                .addComponent(spawnAnimalsCheckBox)
                .addGap(105, 105, 105))
            .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spawnMonstersCheckBox)
                    .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                        .addComponent(spawnProtectionLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spawnProtectionField, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)))
                .addContainerGap(30, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                .addComponent(viewDistanceLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(viewDistanceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(85, 85, 85))
            .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                .addComponent(whiteListCheckBox)
                .addContainerGap())
        );
        serverPropertiesPanelLayout.setVerticalGroup(
            serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverPropertiesPanelLayout.createSequentialGroup()
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(allowFlightCheckBox)
                    .addComponent(allowNetherCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(levelNameLabel)
                    .addComponent(levelNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(levelSeedLabel)
                    .addComponent(levelSeedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(maxPlayersLabel)
                    .addComponent(maxPlayersSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(onlineModeCheckBox)
                    .addComponent(pvpCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serverIpLabel)
                    .addComponent(serverIpField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serverPortLabel)
                    .addComponent(serverPortField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spawnAnimalsCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spawnMonstersCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spawnProtectionLabel)
                    .addComponent(spawnProtectionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverPropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(viewDistanceLabel)
                    .addComponent(viewDistanceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(whiteListCheckBox))
        );

        javax.swing.GroupLayout serverConfigTabLayout = new javax.swing.GroupLayout(serverConfigTab);
        serverConfigTab.setLayout(serverConfigTabLayout);
        serverConfigTabLayout.setHorizontalGroup(
            serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverConfigTabLayout.createSequentialGroup()
                .addGroup(serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(proxyServerPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(serverCmdLinePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE))
                    .addGroup(serverConfigTabLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(saveServerConfigButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(serverPropertiesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(100, Short.MAX_VALUE))
        );
        serverConfigTabLayout.setVerticalGroup(
            serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverConfigTabLayout.createSequentialGroup()
                .addGroup(serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(serverConfigTabLayout.createSequentialGroup()
                        .addComponent(serverCmdLinePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(proxyServerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveServerConfigButton))
                    .addComponent(serverPropertiesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(27, Short.MAX_VALUE))
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
        inputHistoryMaxSizeField.setInputVerifier(new RegexVerifier("\\d{1,4}"));
        inputHistoryMaxSizeField.setName("inputHistoryMaxSizeField"); // NOI18N

        startServerOnLaunchCheckBox.setText(resourceMap.getString("startServerOnLaunchCheckBox.text")); // NOI18N
        startServerOnLaunchCheckBox.setName("startServerOnLaunchCheckBox"); // NOI18N
        startServerOnLaunchCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startServerOnLaunchCheckBoxActionPerformed(evt);
            }
        });

        commandPrefixLabel.setText(resourceMap.getString("commandPrefixLabel.text")); // NOI18N
        commandPrefixLabel.setName("commandPrefixLabel"); // NOI18N

        commandPrefixField.setText(resourceMap.getString("commandPrefixField.text")); // NOI18N
        commandPrefixField.setToolTipText(resourceMap.getString("commandPrefixField.toolTipText")); // NOI18N
        commandPrefixField.setName("commandPrefixField"); // NOI18N
        commandPrefixField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commandPrefixFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(windowTitleLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(windowTitleField, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(inputHistoryMaxSizeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(inputHistoryMaxSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(startServerOnLaunchCheckBox)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(commandPrefixLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(commandPrefixField, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE)))
                .addContainerGap())
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
                    .addComponent(inputHistoryMaxSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(startServerOnLaunchCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(commandPrefixLabel)
                    .addComponent(commandPrefixField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        saveGuiConfigButton.setText(resourceMap.getString("saveGuiConfigButton.text")); // NOI18N
        saveGuiConfigButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        saveGuiConfigButton.setName("saveGuiConfigButton"); // NOI18N
        saveGuiConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveGuiConfigButtonActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel1.border.title"))); // NOI18N
        jPanel1.setName("jPanel1"); // NOI18N

        textColorLabel.setText(resourceMap.getString("textColorLabel.text")); // NOI18N
        textColorLabel.setName("textColorLabel"); // NOI18N

        bgColorLabel.setText(resourceMap.getString("bgColorLabel.text")); // NOI18N
        bgColorLabel.setName("bgColorLabel"); // NOI18N

        infoColorLabel.setText(resourceMap.getString("infoColorLabel.text")); // NOI18N
        infoColorLabel.setName("infoColorLabel"); // NOI18N

        warningColorLabel.setText(resourceMap.getString("warningColorLabel.text")); // NOI18N
        warningColorLabel.setName("warningColorLabel"); // NOI18N

        severeColorLabel.setText(resourceMap.getString("severeColorLabel.text")); // NOI18N
        severeColorLabel.setName("severeColorLabel"); // NOI18N

        textColorBox.setEditable(false);
        textColorBox.setText(resourceMap.getString("textColorBox.text")); // NOI18N
        textColorBox.setToolTipText(resourceMap.getString("textColorBox.toolTipText")); // NOI18N
        textColorBox.setName("textColorBox"); // NOI18N
        textColorBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                textColorBoxMouseClicked(evt);
            }
        });
        textColorBox.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                textColorBoxFocusLost(evt);
            }
        });

        bgColorBox.setEditable(false);
        bgColorBox.setToolTipText(resourceMap.getString("bgColorBox.toolTipText")); // NOI18N
        bgColorBox.setName("bgColorBox"); // NOI18N
        bgColorBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                bgColorBoxMouseClicked(evt);
            }
        });
        bgColorBox.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                bgColorBoxFocusLost(evt);
            }
        });

        infoColorBox.setEditable(false);
        infoColorBox.setToolTipText(resourceMap.getString("infoColorBox.toolTipText")); // NOI18N
        infoColorBox.setName("infoColorBox"); // NOI18N
        infoColorBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                infoColorBoxMouseClicked(evt);
            }
        });
        infoColorBox.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                infoColorBoxFocusLost(evt);
            }
        });

        warningColorBox.setEditable(false);
        warningColorBox.setToolTipText(resourceMap.getString("warningColorBox.toolTipText")); // NOI18N
        warningColorBox.setName("warningColorBox"); // NOI18N
        warningColorBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                warningColorBoxMouseClicked(evt);
            }
        });
        warningColorBox.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                warningColorBoxFocusLost(evt);
            }
        });

        severeColorBox.setEditable(false);
        severeColorBox.setToolTipText(resourceMap.getString("severeColorBox.toolTipText")); // NOI18N
        severeColorBox.setName("severeColorBox"); // NOI18N
        severeColorBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                severeColorBoxMouseClicked(evt);
            }
        });
        severeColorBox.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                severeColorBoxFocusLost(evt);
            }
        });

        textSizeLabel.setText(resourceMap.getString("textSizeLabel.text")); // NOI18N
        textSizeLabel.setName("textSizeLabel"); // NOI18N

        textSizeField.setModel(new javax.swing.SpinnerNumberModel(3, 1, 10, 1));
        textSizeField.setName("textSizeField"); // NOI18N
        textSizeField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                textSizeFieldStateChanged(evt);
            }
        });
        textSizeField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                textSizeFieldPropertyChange(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(warningColorLabel)
                    .addComponent(infoColorLabel)
                    .addComponent(textColorLabel)
                    .addComponent(bgColorLabel)
                    .addComponent(severeColorLabel)
                    .addComponent(textSizeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(textSizeField, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                    .addComponent(textColorBox)
                    .addComponent(bgColorBox)
                    .addComponent(infoColorBox)
                    .addComponent(warningColorBox)
                    .addComponent(severeColorBox))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textColorLabel)
                    .addComponent(textColorBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bgColorLabel)
                    .addComponent(bgColorBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(infoColorLabel)
                    .addComponent(infoColorBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(warningColorLabel)
                    .addComponent(warningColorBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(severeColorLabel)
                    .addComponent(severeColorBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textSizeLabel)
                    .addComponent(textSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(195, Short.MAX_VALUE))
        );
        guiConfigTabLayout.setVerticalGroup(
            guiConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(guiConfigTabLayout.createSequentialGroup()
                .addGroup(guiConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(guiConfigTabLayout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveGuiConfigButton))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(168, Short.MAX_VALUE))
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

        clearLogCheckBox.setText(resourceMap.getString("clearLogCheckBox.text")); // NOI18N
        clearLogCheckBox.setToolTipText(resourceMap.getString("clearLogCheckBox.toolTipText")); // NOI18N
        clearLogCheckBox.setName("clearLogCheckBox"); // NOI18N
        clearLogCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearLogCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backupSettingsPanelLayout = new javax.swing.GroupLayout(backupSettingsPanel);
        backupSettingsPanel.setLayout(backupSettingsPanelLayout);
        backupSettingsPanelLayout.setHorizontalGroup(
            backupSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backupSettingsPanelLayout.createSequentialGroup()
                .addComponent(backupPathLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(backupPathField, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(backupPathBrowseButton))
            .addGroup(backupSettingsPanelLayout.createSequentialGroup()
                .addComponent(zipBackupCheckBox)
                .addGap(18, 18, 18)
                .addComponent(clearLogCheckBox)
                .addGap(125, 125, 125))
        );
        backupSettingsPanelLayout.setVerticalGroup(
            backupSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backupSettingsPanelLayout.createSequentialGroup()
                .addGroup(backupSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(backupPathLabel)
                    .addComponent(backupPathField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(backupPathBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backupSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(zipBackupCheckBox)
                    .addComponent(clearLogCheckBox)))
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
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE)
        );
        backupFileChooserPanelLayout.setVerticalGroup(
            backupFileChooserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
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
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
        );
        backupStatusPanelLayout.setVerticalGroup(
            backupStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 179, Short.MAX_VALUE)
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
                .addGap(44, 44, 44))
        );

        tabber.addTab(resourceMap.getString("backupTab.TabConstraints.tabTitle"), backupTab); // NOI18N

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
        taskSchedulerList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                taskSchedulerListKeyTyped(evt);
            }
        });
        jScrollPane5.setViewportView(taskSchedulerList);

        javax.swing.GroupLayout taskSchedulerPanelLayout = new javax.swing.GroupLayout(taskSchedulerPanel);
        taskSchedulerPanel.setLayout(taskSchedulerPanelLayout);
        taskSchedulerPanelLayout.setHorizontalGroup(
            taskSchedulerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 532, Short.MAX_VALUE)
        );
        taskSchedulerPanelLayout.setVerticalGroup(
            taskSchedulerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 284, Short.MAX_VALUE)
        );

        taskListAddButton.setText(resourceMap.getString("taskListAddButton.text")); // NOI18N
        taskListAddButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        taskListAddButton.setName("taskListAddButton"); // NOI18N
        taskListAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                taskListAddButtonActionPerformed(evt);
            }
        });

        taskListEditButton.setText(resourceMap.getString("taskListEditButton.text")); // NOI18N
        taskListEditButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        taskListEditButton.setName("taskListEditButton"); // NOI18N
        taskListEditButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                taskListEditButtonActionPerformed(evt);
            }
        });

        taskListRemoveButton.setText(resourceMap.getString("taskListRemoveButton.text")); // NOI18N
        taskListRemoveButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        taskListRemoveButton.setName("taskListRemoveButton"); // NOI18N
        taskListRemoveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                taskListRemoveButtonActionPerformed(evt);
            }
        });

        pauseSchedulerButton.setText(resourceMap.getString("pauseSchedulerButton.text")); // NOI18N
        pauseSchedulerButton.setToolTipText(resourceMap.getString("pauseSchedulerButton.toolTipText")); // NOI18N
        pauseSchedulerButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        pauseSchedulerButton.setName("pauseSchedulerButton"); // NOI18N
        pauseSchedulerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseSchedulerButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout schedulerTabLayout = new javax.swing.GroupLayout(schedulerTab);
        schedulerTab.setLayout(schedulerTabLayout);
        schedulerTabLayout.setHorizontalGroup(
            schedulerTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(taskSchedulerPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(schedulerTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(taskListAddButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(taskListEditButton)
                .addGap(18, 18, 18)
                .addComponent(taskListRemoveButton)
                .addGap(18, 18, 18)
                .addComponent(pauseSchedulerButton)
                .addContainerGap(280, Short.MAX_VALUE))
        );
        schedulerTabLayout.setVerticalGroup(
            schedulerTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, schedulerTabLayout.createSequentialGroup()
                .addComponent(taskSchedulerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(schedulerTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pauseSchedulerButton)
                    .addComponent(taskListRemoveButton)
                    .addComponent(taskListEditButton)
                    .addComponent(taskListAddButton))
                .addContainerGap())
        );

        tabber.addTab(resourceMap.getString("schedulerTab.TabConstraints.tabTitle"), schedulerTab); // NOI18N

        webInterfaceTab.setName("webInterfaceTab"); // NOI18N

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel2.border.title"))); // NOI18N
        jPanel2.setName("jPanel2"); // NOI18N

        webPortLabel.setText(resourceMap.getString("webPortLabel.text")); // NOI18N
        webPortLabel.setName("webPortLabel"); // NOI18N

        webPortField.setText(resourceMap.getString("webPortField.text")); // NOI18N
        webPortField.setToolTipText(resourceMap.getString("webPortField.toolTipText")); // NOI18N
        webPortField.setInputVerifier(new RegexVerifier("^(6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)$"));
        webPortField.setName("webPortField"); // NOI18N
        webPortField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                webPortFieldFocusLost(evt);
            }
        });

        useWebInterfaceCheckBox.setText(resourceMap.getString("useWebInterfaceCheckBox.text")); // NOI18N
        useWebInterfaceCheckBox.setToolTipText(resourceMap.getString("useWebInterfaceCheckBox.toolTipText")); // NOI18N
        useWebInterfaceCheckBox.setName("useWebInterfaceCheckBox"); // NOI18N
        useWebInterfaceCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useWebInterfaceCheckBoxActionPerformed(evt);
            }
        });

        webPasswordLabel.setText(resourceMap.getString("webPasswordLabel.text")); // NOI18N
        webPasswordLabel.setName("webPasswordLabel"); // NOI18N

        webPasswordField.setText(resourceMap.getString("webPasswordField.text")); // NOI18N
        webPasswordField.setName("webPasswordField"); // NOI18N
        webPasswordField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                webPasswordFieldFocusLost(evt);
            }
        });

        showWebPasswordButton.setText(resourceMap.getString("showWebPasswordButton.text")); // NOI18N
        showWebPasswordButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        showWebPasswordButton.setName("showWebPasswordButton"); // NOI18N
        showWebPasswordButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showWebPasswordButtonActionPerformed(evt);
            }
        });

        disableGetOutputNotificationsCheckBox.setText(resourceMap.getString("disableGetOutputNotificationsCheckBox.text")); // NOI18N
        disableGetOutputNotificationsCheckBox.setToolTipText(resourceMap.getString("disableGetOutputNotificationsCheckBox.toolTipText")); // NOI18N
        disableGetOutputNotificationsCheckBox.setName("disableGetOutputNotificationsCheckBox"); // NOI18N
        disableGetOutputNotificationsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disableGetOutputNotificationsCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(webPortLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(webPortField, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(useWebInterfaceCheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(webPasswordLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(webPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(showWebPasswordButton, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(disableGetOutputNotificationsCheckBox))
                .addContainerGap(68, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(webPortLabel)
                    .addComponent(webPortField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(useWebInterfaceCheckBox)
                    .addComponent(webPasswordLabel)
                    .addComponent(webPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(showWebPasswordButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disableGetOutputNotificationsCheckBox))
        );

        webLogPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("webLogPanel.border.title"))); // NOI18N
        webLogPanel.setName("webLogPanel"); // NOI18N

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        webLog.setName("webLog"); // NOI18N
        jScrollPane6.setViewportView(webLog);

        javax.swing.GroupLayout webLogPanelLayout = new javax.swing.GroupLayout(webLogPanel);
        webLogPanel.setLayout(webLogPanelLayout);
        webLogPanelLayout.setHorizontalGroup(
            webLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)
        );
        webLogPanelLayout.setVerticalGroup(
            webLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout webInterfaceTabLayout = new javax.swing.GroupLayout(webInterfaceTab);
        webInterfaceTab.setLayout(webInterfaceTabLayout);
        webInterfaceTabLayout.setHorizontalGroup(
            webInterfaceTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, webInterfaceTabLayout.createSequentialGroup()
                .addGroup(webInterfaceTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(webLogPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        webInterfaceTabLayout.setVerticalGroup(
            webInterfaceTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(webInterfaceTabLayout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(webLogPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabber.addTab(resourceMap.getString("webInterfaceTab.TabConstraints.tabTitle"), webInterfaceTab); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabber, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabber, javax.swing.GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE)
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(mcservergui.Main.class).getContext().getActionMap(GUI.class, this);
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

        hideMenu.setText(resourceMap.getString("hideMenu.text")); // NOI18N
        hideMenu.setName("hideMenu"); // NOI18N
        hideMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                hideMenuMouseClicked(evt);
            }
        });
        menuBar.add(hideMenu);

        versionNotifier.setText(resourceMap.getString("versionNotifier.text")); // NOI18N
        versionNotifier.setToolTipText(resourceMap.getString("versionNotifier.toolTipText")); // NOI18N
        versionNotifier.setName("versionNotifier"); // NOI18N

        launchSupportPage.setText(resourceMap.getString("launchSupportPage.text")); // NOI18N
        launchSupportPage.setName("launchSupportPage"); // NOI18N
        launchSupportPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                launchSupportPageActionPerformed(evt);
            }
        });
        versionNotifier.add(launchSupportPage);

        viewChangeLog.setText(resourceMap.getString("viewChangeLog.text")); // NOI18N
        viewChangeLog.setName("viewChangeLog"); // NOI18N
        viewChangeLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewChangeLogActionPerformed(evt);
            }
        });
        versionNotifier.add(viewChangeLog);

        downloadLatestVersion.setText(resourceMap.getString("downloadLatestVersion.text")); // NOI18N
        downloadLatestVersion.setName("downloadLatestVersion"); // NOI18N
        downloadLatestVersion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadLatestVersionActionPerformed(evt);
            }
        });
        versionNotifier.add(downloadLatestVersion);

        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        versionNotifier.add(jMenuItem1);

        menuBar.add(versionNotifier);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        serverStatusLabel.setText(resourceMap.getString("serverStatusLabel.text")); // NOI18N
        serverStatusLabel.setName("serverStatusLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        statusBarJob.setText(resourceMap.getString("statusBarJob.text")); // NOI18N
        statusBarJob.setName("statusBarJob"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(statusAnimationLabel)
                    .addGroup(statusPanelLayout.createSequentialGroup()
                        .addComponent(serverStatusLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 235, Short.MAX_VALUE)
                        .addComponent(statusBarJob, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(serverStatusLabel)
                        .addComponent(statusAnimationLabel)
                        .addComponent(statusBarJob, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
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
        javax.swing.GroupLayout serverConfigTabLayout = new javax.swing.GroupLayout(serverConfigTab);
        serverConfigTab.setLayout(serverConfigTabLayout);
        serverConfigTabLayout.setHorizontalGroup(
            serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverConfigTabLayout.createSequentialGroup()
                .addGroup(serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(serverConfigTabLayout.createSequentialGroup()
                        .addGroup(serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(proxyServerPanel, javax.swing.GroupLayout.Alignment.LEADING, 252, 252, 252)
                            .addComponent(serverCmdLinePanel, javax.swing.GroupLayout.Alignment.LEADING, 252, 252, 252))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(serverPropertiesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(serverConfigTabLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(saveServerConfigButton)))
                .addContainerGap(98, Short.MAX_VALUE))
        );
        serverConfigTabLayout.setVerticalGroup(
            serverConfigTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverConfigTabLayout.createSequentialGroup()
                .addComponent(serverCmdLinePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proxyServerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addComponent(saveServerConfigButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(serverConfigTabLayout.createSequentialGroup()
                .addComponent(serverPropertiesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(57, 57, 57))
        );
        // Sets the checking model for the backup file checkbox tree
        //backupFileChooser.getCheckingModel().setCheckingMode(CheckingMode.PROPAGATE_PRESERVING_CHECK);
        /*
        DefaultTreeCheckingModel checkingModel = new DefaultTreeCheckingModel(backupFileChooser.getModel()) {
            @Override public void setCheckingMode(CheckingMode mode) {
                setPreservingCheckTreeCheckingMode();
            }
            public void setPreservingCheckTreeCheckingMode() {
                this.checkingMode = new GUITreeCheckingMode(this, GUI.this);
            }
        };
         * 
         */
        //checkingModel.setCheckingMode(CheckingMode.SIMPLE);
        //backupFileChooser.setCheckingModel(checkingModel);
        //backupFileChooser.setCheckingModel(new GUITreeCheckingModel(backupFileChooser.getModel(), this));
        //backupFileChooser.getCheckingModel().setCheckingMode(CheckingMode.SIMPLE);
        // Sets html fomratting for some components
        taskSchedulerList.setCellRenderer(new TaskSchedulerListCellRenderer());
        consoleOutput.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
        consoleOutput.setStyledDocument(new javax.swing.text.html.HTMLDocument());
        backupStatusLog.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
        backupStatusLog.setStyledDocument(new javax.swing.text.html.HTMLDocument());
        webLog.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
        webLog.setStyledDocument(new javax.swing.text.html.HTMLDocument());
        // Sets the application icon
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mcservergui.Main.class).getContext().getResourceMap(GUI.class);
        this.getFrame().setIconImage(resourceMap.getImageIcon("imageLabel.icon").getImage());

        //Sets the version number on Main Window tab
        versionNumber = org.jdesktop.application.Application
                .getInstance(mcservergui.Main.class).getContext()
                .getResourceMap(AboutBox.class).getString("Application.version");
        versionLabel.setText("Version " + versionNumber);
    }

    /**
     * Sets the rendering of the task list
     */
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

    /**
     * Action object for the toggling of the sayCheckBox
     */
    javax.swing.Action sayToggle = new javax.swing.AbstractAction() {
        @Override public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    if (sayCheckBox.isSelected()) {
                        sayCheckBox.setSelected(false);
                    } else {
                        sayCheckBox.setSelected(true);
                    }
                }
            });
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (startstopButton.getText().equals("Start")) {
                    startServer();
                } else {
                    stopServer();
                }
            }
        });
    }//GEN-LAST:event_startstopButtonActionPerformed

    private void submitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitButtonActionPerformed
        sendInput();
    }//GEN-LAST:event_submitButtonActionPerformed

    private void consoleInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleInputActionPerformed
        sendInput();
    }//GEN-LAST:event_consoleInputActionPerformed

    private void windowTitleFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_windowTitleFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.setWindowTitle(windowTitleField.getText());
                getFrame().setTitle(windowTitleField.getText());
                trayIcon.setToolTip(windowTitleField.getText());
            }
        });
    }//GEN-LAST:event_windowTitleFieldActionPerformed

    private void javaExecBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_javaExecBrowseButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                final JFileChooser fc = new JFileChooser();
                int returnVal = fc.showOpenDialog(getFrame());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    javaExecField.setText(fc.getSelectedFile().getPath());
                    config.cmdLine.setJavaExec(fc.getSelectedFile().getPath());
                }
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
    }//GEN-LAST:event_javaExecBrowseButtonActionPerformed

    private void serverJarBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverJarBrowseButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    final JFileChooser fc = new JFileChooser(new File(".").getCanonicalPath());
                    int returnVal = fc.showOpenDialog(getFrame());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        config.cmdLine.setServerJar(fc.getSelectedFile().getName());
                        serverJarField.setText(fc.getSelectedFile().getName());
                        cmdLineField.setText(config.cmdLine.parseCmdLine());
                    }
                } catch (IOException e) {
                    System.out.println("Error retrieving path");
                }
            }
        });
    }//GEN-LAST:event_serverJarBrowseButtonActionPerformed

    private void bukkitCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bukkitCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.cmdLine.setBukkit(bukkitCheckBox.isSelected());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
    }//GEN-LAST:event_bukkitCheckBoxActionPerformed

    private void xmxMemoryFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xmxMemoryFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.cmdLine.setXmx(xmxMemoryField.getText());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
    }//GEN-LAST:event_xmxMemoryFieldActionPerformed

    private void xincgcCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xincgcCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.cmdLine.setXincgc(xincgcCheckBox.isSelected());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
        
    }//GEN-LAST:event_xincgcCheckBoxActionPerformed

    private void extraArgsFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_extraArgsFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.cmdLine.setExtraArgs(extraArgsField.getText());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
    }//GEN-LAST:event_extraArgsFieldActionPerformed

    private void serverJarFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverJarFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.cmdLine.setServerJar(serverJarField.getText());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
    }//GEN-LAST:event_serverJarFieldActionPerformed

    private void javaExecFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_javaExecFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.cmdLine.setJavaExec(javaExecField.getText());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
    }//GEN-LAST:event_javaExecFieldActionPerformed

    private void saveGuiConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveGuiConfigButtonActionPerformed
        saveConfig();
    }//GEN-LAST:event_saveGuiConfigButtonActionPerformed

    private void saveServerConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveServerConfigButtonActionPerformed
        saveConfig();
    }//GEN-LAST:event_saveServerConfigButtonActionPerformed

    private void consoleOutputMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_consoleOutputMouseExited
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                int selMin = consoleOutput.getSelectionStart();
                int selMax = consoleOutput.getSelectionEnd();
                if ((server.isRunning()) && (selMax - selMin == 0)) {
                    textScrolling = true;
                }
                mouseInConsoleOutput = false;
            }
        });
    }//GEN-LAST:event_consoleOutputMouseExited

    private void consoleOutputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_consoleOutputFocusGained
        textScrolling = false;
    }//GEN-LAST:event_consoleOutputFocusGained

    private void consoleOutputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_consoleOutputFocusLost
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                int selMin = consoleOutput.getSelectionStart();
                int selMax = consoleOutput.getSelectionEnd();
                if ((selMax - selMin == 0) && (server.isRunning()) && (!mouseInConsoleOutput)) {
                    textScrolling = true;
                }
            }
        });
    }//GEN-LAST:event_consoleOutputFocusLost

    private void tabberKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tabberKeyTyped
        final java.awt.event.KeyEvent event = evt;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if(tabber.getSelectedIndex() == 0) {
                    giveInputFocus(event);
                }
            }
        });
    }//GEN-LAST:event_tabberKeyTyped

    private void playerListKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_playerListKeyTyped
        giveInputFocus(evt);
    }//GEN-LAST:event_playerListKeyTyped

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
        final java.awt.event.KeyEvent event = evt;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (!inputHistory.isEmpty()) {
                    if (event.getKeyCode() == 38) {
                        // Move back through the input history
                        inputHistoryIndex++;
                        if (inputHistoryIndex > inputHistory.size()) {
                            inputHistoryIndex = 0;
                        }
                        if (inputHistoryIndex == inputHistory.size()) {
                            consoleInput.setText("");
                        } else {
                            consoleInput.setText(inputHistory.get(inputHistoryIndex));
                        }
                    } else if (event.getKeyCode() == 40) {
                        // Move forward through the input history
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
            }
        });
    }//GEN-LAST:event_consoleInputKeyPressed

    private void customLaunchCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customLaunchCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
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
                    xmxMemoryField.setEditable(true);
                    xincgcCheckBox.setEnabled(true);
                    extraArgsField.setEditable(true);
                }
            }
        });  
    }//GEN-LAST:event_customLaunchCheckBoxActionPerformed

    private void cmdLineFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdLineFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.cmdLine.setCustomLaunch(cmdLineField.getText());
                if (java.util.regex.Pattern.matches("^\\s*$", config.cmdLine.getCustomLaunch())) {
                    config.cmdLine.setUseCustomLaunch(false);
                    config.cmdLine.setCustomLaunch(config.cmdLine.parseCmdLine());
                    config.cmdLine.setUseCustomLaunch(true);
                    cmdLineField.setText(config.cmdLine.getCustomLaunch());
                }
            }
        });
    }//GEN-LAST:event_cmdLineFieldActionPerformed
    
    private void saveWorldsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveWorldsButtonActionPerformed
        this.sendInput("save-all");
    }//GEN-LAST:event_saveWorldsButtonActionPerformed

    private void saveBackupControlButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveBackupControlButtonActionPerformed
        saveConfig();
    }//GEN-LAST:event_saveBackupControlButtonActionPerformed

    private void backupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.backups.setPath(backupPathField.getText());
                backup();
            }
        });
        
    }//GEN-LAST:event_backupButtonActionPerformed

    private void tabberStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabberStateChanged
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (tabber.getSelectedIndex() == 3) {
                    refreshBackupFileChooser();
                }
            }
        });
    }//GEN-LAST:event_tabberStateChanged

    private void backupControlRefreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupControlRefreshButtonActionPerformed
        refreshBackupFileChooser();
    }//GEN-LAST:event_backupControlRefreshButtonActionPerformed

    private void backupPathBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupPathBrowseButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    File backup = new File(backupPathField.getText());
                    final JFileChooser fc;
                    if (backup.exists()) {
                        fc = new JFileChooser(backup.getCanonicalPath());
                    } else {
                        fc = new JFileChooser(new File(".").getCanonicalPath());
                    }
                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int returnVal = fc.showOpenDialog(getFrame());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        backupPathField.setText(fc.getSelectedFile().getPath());
                        config.backups.setPath(fc.getSelectedFile().getPath());
                    }
                } catch (IOException e) {
                    System.err.println("[MC Server GUI] Error retrieving program path.");
                }
            }
        });
    }//GEN-LAST:event_backupPathBrowseButtonActionPerformed

    private void backupPathFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupPathFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.backups.setPath(backupPathField.getText());
            }
        });
    }//GEN-LAST:event_backupPathFieldActionPerformed

    private void zipBackupCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zipBackupCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.backups.setZip(zipBackupCheckBox.isSelected());
            }
        });
    }//GEN-LAST:event_zipBackupCheckBoxActionPerformed

    private void taskListAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskListAddButtonActionPerformed
        addTaskListEntry();
    }//GEN-LAST:event_taskListAddButtonActionPerformed

    private void taskListEditButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskListEditButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                editTaskListEntry((EventModel)taskSchedulerList.getSelectedValue());
            }
        });
    }//GEN-LAST:event_taskListEditButtonActionPerformed

    private void taskListRemoveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskListRemoveButtonActionPerformed
        removeTaskListEntry();
    }//GEN-LAST:event_taskListRemoveButtonActionPerformed

    public void removeTaskListEntry() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (javax.swing.JOptionPane.showConfirmDialog(GUI.this.getFrame(),
                        "Are you sure you wish to remove this event?\n"
                        + "If it is running it will be interrupted.\n",
                        "Remove scheduled task",
                        javax.swing.JOptionPane.YES_NO_OPTION) ==
                        javax.swing.JOptionPane.YES_OPTION) {

                    EventModel event = (EventModel)taskSchedulerList
                            .getSelectedValue();
                    try {
                        scheduler.interrupt(JobKey.jobKey(event.getName()));
                        scheduler.deleteJob(JobKey.jobKey(event.getName()));
                    } catch (SchedulerException se) {
                        System.out.println("Error removing old task");
                    }
                    customButtonBoxModel1.removeElement(event.getName());
                    customButtonBoxModel2.removeElement(event.getName());
                    config.schedule.getEvents().removeElement(event);
                    config.save();
                }
            }
        });
    }

    private void textSizeFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_textSizeFieldPropertyChange
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.display.setTextSize(Integer.parseInt(textSizeField.getValue().toString()));
            }
        });
    }//GEN-LAST:event_textSizeFieldPropertyChange

    private void textColorBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_textColorBoxMouseClicked
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JFrame mainFrame = Main.getApplication().getMainFrame();
                ColorChooser colorchooser = new ColorChooser(
                        mainFrame, textColorBox);
                colorchooser.setLocationRelativeTo(mainFrame);
                Main.getApplication().show(colorchooser);
            }
        });
    }//GEN-LAST:event_textColorBoxMouseClicked

    private void bgColorBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bgColorBoxMouseClicked
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JFrame mainFrame = Main.getApplication().getMainFrame();
                ColorChooser colorchooser = new ColorChooser(
                        mainFrame, bgColorBox);
                colorchooser.setLocationRelativeTo(mainFrame);
                Main.getApplication().show(colorchooser);
            }
        });
    }//GEN-LAST:event_bgColorBoxMouseClicked

    private void infoColorBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_infoColorBoxMouseClicked
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JFrame mainFrame = Main.getApplication().getMainFrame();
                ColorChooser colorchooser = new ColorChooser(
                        mainFrame, infoColorBox);
                colorchooser.setLocationRelativeTo(mainFrame);
                Main.getApplication().show(colorchooser);
            }
        });
    }//GEN-LAST:event_infoColorBoxMouseClicked

    private void warningColorBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_warningColorBoxMouseClicked
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JFrame mainFrame = Main.getApplication().getMainFrame();
                ColorChooser colorchooser = new ColorChooser(
                        mainFrame, warningColorBox);
                colorchooser.setLocationRelativeTo(mainFrame);
                Main.getApplication().show(colorchooser);
            }
        });
    }//GEN-LAST:event_warningColorBoxMouseClicked

    private void severeColorBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_severeColorBoxMouseClicked
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JFrame mainFrame = Main.getApplication().getMainFrame();
                ColorChooser colorchooser = new ColorChooser(
                        mainFrame, severeColorBox);
                colorchooser.setLocationRelativeTo(mainFrame);
                Main.getApplication().show(colorchooser);
            }
        });
    }//GEN-LAST:event_severeColorBoxMouseClicked

    private void textColorBoxFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_textColorBoxFocusLost
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                String rgb = Integer.toHexString(textColorBox.getBackground().getRGB());
                rgb = rgb.substring(2, rgb.length());
                config.display.setTextColor(rgb);
            }
        });
    }//GEN-LAST:event_textColorBoxFocusLost

    private void bgColorBoxFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_bgColorBoxFocusLost
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                String rgb = Integer.toHexString(bgColorBox.getBackground().getRGB());
                rgb = rgb.substring(2, rgb.length());
                config.display.setBgColor(rgb);
                updateConsoleOutputBgColor();
            }
        });
    }//GEN-LAST:event_bgColorBoxFocusLost

    private void infoColorBoxFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_infoColorBoxFocusLost
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                String rgb = Integer.toHexString(infoColorBox.getBackground().getRGB());
                rgb = rgb.substring(2, rgb.length());
                config.display.setInfoColor(rgb);
            }
        });
    }//GEN-LAST:event_infoColorBoxFocusLost

    private void warningColorBoxFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_warningColorBoxFocusLost
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                String rgb = Integer.toHexString(warningColorBox.getBackground().getRGB());
                rgb = rgb.substring(2, rgb.length());
                config.display.setWarningColor(rgb);
            }
        });
    }//GEN-LAST:event_warningColorBoxFocusLost

    private void severeColorBoxFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_severeColorBoxFocusLost
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                String rgb = Integer.toHexString(severeColorBox.getBackground().getRGB());
                rgb = rgb.substring(2, rgb.length());
                config.display.setSevereColor(rgb);
            }
        });
    }//GEN-LAST:event_severeColorBoxFocusLost

    private void playerListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_playerListMouseClicked
        final java.awt.event.MouseEvent event = evt;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (event.getButton() == event.BUTTON3 && (playerList.getSelectedIndex() > -1)) {
                    javax.swing.JPopupMenu playerListContextMenu = new javax.swing.JPopupMenu();
                    javax.swing.JMenuItem kickMenuItem;
                    kickMenuItem = new javax.swing.JMenuItem("Kick");
                    kickMenuItem.addActionListener(
                            new ActionListener() {
                        @Override public void actionPerformed(ActionEvent ev) {
                            String s = (String)javax.swing.JOptionPane.showInputDialog(
                                    GUI.this.getFrame(), "Add a kick message or just "
                                    + "press enter.", "Kick Player", javax.swing
                                    .JOptionPane.PLAIN_MESSAGE, null, null, "");
                            playerListModel.findPlayer(playerListModel.getElementAt(
                                    playerList.getSelectedIndex())).kick(s);
                        }
                    });
                    javax.swing.JMenuItem banMenuItem;
                    banMenuItem = new javax.swing.JMenuItem("Ban");
                    banMenuItem.addActionListener(
                            new ActionListener() {
                        @Override public void actionPerformed(ActionEvent ev) {
                            String s = (String)javax.swing.JOptionPane.showInputDialog(
                                    GUI.this.getFrame(), "Add a ban message or just "
                                    + "press enter.", "Ban Player", javax.swing
                                    .JOptionPane.PLAIN_MESSAGE, null, null, "Banned!");
                            server.banKick(playerListModel.findPlayer(
                                    playerListModel.getElementAt(
                                    playerList.getSelectedIndex())).getName(), s);
                        }
                    });
                    javax.swing.JMenuItem banIpMenuItem;
                    banIpMenuItem = new javax.swing.JMenuItem("Ban IP");
                    banIpMenuItem.addActionListener(
                            new ActionListener() {
                        @Override public void actionPerformed(ActionEvent ev) {
                            String s = (String)javax.swing.JOptionPane.showInputDialog(
                                    GUI.this.getFrame(), "Add a ban message or just "
                                    + "press enter.", "Ban Player IP Address", javax.swing
                                    .JOptionPane.PLAIN_MESSAGE, null, null, "Banned!");
                            server.banKickIP(playerListModel.findPlayer(
                                    playerListModel.getElementAt(
                                    playerList.getSelectedIndex())).getIPAddress(), s);
                        }
                    });
                    playerListContextMenu.add(kickMenuItem);
                    playerListContextMenu.add(banMenuItem);
                    playerListContextMenu.add(banIpMenuItem);
                    playerListContextMenu.show(event.getComponent(), event.getX(), event.getY());
                }
            }
        });
    }//GEN-LAST:event_playerListMouseClicked

    private void clearLogCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearLogCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.backups.setClearLog(clearLogCheckBox.isSelected());
            }
        });
    }//GEN-LAST:event_clearLogCheckBoxActionPerformed

    private void startServerOnLaunchCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startServerOnLaunchCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.setServerStartOnStartup(startServerOnLaunchCheckBox.isSelected());
            }
        });
    }//GEN-LAST:event_startServerOnLaunchCheckBoxActionPerformed

    private void useWebInterfaceCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useWebInterfaceCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.web.setEnabled(useWebInterfaceCheckBox.isSelected());
                if (useWebInterfaceCheckBox.isSelected()) {
                    config.web.setPassword(String.valueOf(webPasswordField.getPassword()));
                    if (webPortField.getInputVerifier().verify(webPortField)) {
                        config.web.setPort(Integer.valueOf(webPortField.getText()));
                        webServer.setPort(config.web.getPort());
                        webServer.start();
                    } else {
                        webPortField.requestFocusInWindow();
                    }
                } else {
                    webServer.stop();
                }
            }
        });
    }//GEN-LAST:event_useWebInterfaceCheckBoxActionPerformed

    private void webPortFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_webPortFieldFocusLost
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.web.setPort(Integer.valueOf(webPortField.getText()));
            }
        });
    }//GEN-LAST:event_webPortFieldFocusLost

    private void showWebPasswordButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showWebPasswordButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (showWebPasswordButton.isSelected()) {
                    webPasswordField.setEchoChar((char)0);
                } else {
                    webPasswordField.setEchoChar('\u25cf');
                }
            }
        });
    }//GEN-LAST:event_showWebPasswordButtonActionPerformed

    private void webPasswordFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_webPasswordFieldFocusLost
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.web.setPassword(String.valueOf(webPasswordField.getPassword()));
            }
        });
    }//GEN-LAST:event_webPasswordFieldFocusLost

    private void disableGetOutputNotificationsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disableGetOutputNotificationsCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.web.setDisableGetRequests(disableGetOutputNotificationsCheckBox.isSelected());
            }
        });
    }//GEN-LAST:event_disableGetOutputNotificationsCheckBoxActionPerformed

    private void consoleOutputMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_consoleOutputMouseClicked
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if ((server.isRunning()) && (!consoleOutput.isFocusOwner())) {
                    textScrolling = false;
                }
                mouseInConsoleOutput = true;
            }
        });
    }//GEN-LAST:event_consoleOutputMouseClicked

    private void customButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customButton1ActionPerformed
        customButtonAction(customCombo1);
    }//GEN-LAST:event_customButton1ActionPerformed

    private void customButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customButton2ActionPerformed
        customButtonAction(customCombo2);
    }//GEN-LAST:event_customButton2ActionPerformed

    private void useProxyCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useProxyCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.setProxy(useProxyCheckBox.isSelected());
                if (useProxyCheckBox.isSelected()) {
                    playerList.setToolTipText("This shows a list of players connected to the server.  Right click a to pull up the player action menu.");
                } else {
                    playerList.setToolTipText("Player list is currently only supported when using the GUI's Proxy feature.");
                }
            }
        });
    }//GEN-LAST:event_useProxyCheckBoxActionPerformed

    private void hideMenuMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hideMenuMouseClicked
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                GUI.this.getApplication().hide(GUI.this);
                isHidden = true;
            }
        }); 
    }//GEN-LAST:event_hideMenuMouseClicked

    private void textSizeFieldStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_textSizeFieldStateChanged
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.display.setTextSize(Integer.parseInt(textSizeField.getValue().toString()));
            }
        });
    }//GEN-LAST:event_textSizeFieldStateChanged

    private void commandPrefixFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandPrefixFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.setCommandPrefix(commandPrefixField.getText());
            }
        });
    }//GEN-LAST:event_commandPrefixFieldActionPerformed

    private void launchSupportPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launchSupportPageActionPerformed
        String url = "http://forums.bukkit.org/threads/admin-mc-server-gui-8-2-cross-platform-a-gui-wrapper-for-your-server-now-w-remote-ctrl-860.17834/";
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (IOException ioe) {
            System.out.println("Error launching page.");
        }
    }//GEN-LAST:event_launchSupportPageActionPerformed

    private void viewChangeLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewChangeLogActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JFrame mainFrame = Main.getApplication().getMainFrame();
                ChangeLog changeLog = new ChangeLog(mainFrame, versionNumber);
                changeLog.setLocationRelativeTo(mainFrame);
                Main.getApplication().show(changeLog);
            }
        });
    }//GEN-LAST:event_viewChangeLogActionPerformed

    private void downloadLatestVersionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadLatestVersionActionPerformed
        String urltext = "https://raw.github.com/dumptruckman/MC-Server-GUI--multi-/master/VERSION";
        String newVersion= "";
        try {
            URL url = new URL(urltext);
            BufferedReader in = new BufferedReader(new InputStreamReader(url
                    .openStream()));
            String line = "";
            if ((line = in.readLine()) != null) {
                newVersion = line;
            }
            in.close();
            String downloadurl = "https://github.com/downloads/dumptruckman/MC-Server-GUI--multi-/mcservergui-" + newVersion + ".zip";
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(downloadurl));
            } catch (IOException ioe) {
                System.out.println("Error launching page.");
            }
        } catch (java.net.MalformedURLException e) {
        } catch (IOException ioe) {
        }
    }//GEN-LAST:event_downloadLatestVersionActionPerformed

    private void pauseSchedulerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseSchedulerButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (pauseSchedulerButton.isSelected()) {
                    try {
                        scheduler.pauseAll();
                        pauseSchedulerButton.setFont(new java.awt.Font("Tahoma",
                                java.awt.Font.BOLD, 11));
                    } catch (SchedulerException se) {
                        pauseSchedulerButton.setSelected(false);
                    }
                } else {
                    try {
                        scheduler.resumeAll();
                        pauseSchedulerButton.setFont(new java.awt.Font("Tahoma",
                                java.awt.Font.PLAIN, 11));
                    } catch (SchedulerException se) {
                        pauseSchedulerButton.setSelected(true);
                    }
                }
            }
        });
    }//GEN-LAST:event_pauseSchedulerButtonActionPerformed

    private void taskSchedulerListKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_taskSchedulerListKeyTyped
        final java.awt.event.KeyEvent event = evt;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (event.getKeyChar() == java.awt.event.KeyEvent.VK_DELETE) {
                    removeTaskListEntry();
                }
            }
        });
    }//GEN-LAST:event_taskSchedulerListKeyTyped

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=9XY8KKATD26GL";
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (IOException ioe) {
            System.out.println("Error launching page.");
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void customButtonAction(javax.swing.JComboBox box) {
        final javax.swing.JComboBox boxxy = box;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (boxxy.getSelectedItem().toString().equals("Edit Tasks")) {
                    tabber.setSelectedIndex(tabber.indexOfTab("Tasks"));
                } else {
                    startTaskByName(boxxy.getSelectedItem().toString());
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JCheckBox allowFlightCheckBox;
    public javax.swing.JCheckBox allowNetherCheckBox;
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
    public javax.swing.JTextField bgColorBox;
    public javax.swing.JLabel bgColorLabel;
    public javax.swing.JCheckBox bukkitCheckBox;
    public javax.swing.JCheckBox clearLogCheckBox;
    public javax.swing.JTextField cmdLineField;
    public javax.swing.JTextField commandPrefixField;
    public javax.swing.JLabel commandPrefixLabel;
    public javax.swing.JTextField consoleInput;
    public javax.swing.JPanel consoleInputPanel;
    public javax.swing.JTextPane consoleOutput;
    public javax.swing.JPanel consoleOutputPanel;
    public javax.swing.JButton customButton1;
    public javax.swing.JButton customButton2;
    public javax.swing.JComboBox customCombo1;
    public javax.swing.JComboBox customCombo2;
    public javax.swing.JCheckBox customLaunchCheckBox;
    public javax.swing.JCheckBox disableGetOutputNotificationsCheckBox;
    public javax.swing.JMenuItem downloadLatestVersion;
    public javax.swing.JTextField extPortField;
    public javax.swing.JLabel extPortLabel;
    public javax.swing.JTextField extraArgsField;
    public javax.swing.JLabel extraArgsLabel;
    public javax.swing.JPanel guiConfigTab;
    public javax.swing.JLabel guiCpuUsage;
    public javax.swing.JLabel guiCpuUsageLabel;
    public javax.swing.JPanel guiInfoPanel;
    public javax.swing.JLabel guiMemoryUsage;
    public javax.swing.JLabel guiMemoryUsageLabel;
    public javax.swing.JMenu hideMenu;
    public javax.swing.JTextField infoColorBox;
    public javax.swing.JLabel infoColorLabel;
    public javax.swing.JTextField inputHistoryMaxSizeField;
    public javax.swing.JLabel inputHistoryMaxSizeLabel;
    public javax.swing.JCheckBox jCheckBox1;
    public javax.swing.JLabel jLabel2;
    public javax.swing.JMenuItem jMenuItem1;
    public javax.swing.JPanel jPanel1;
    public javax.swing.JPanel jPanel2;
    public javax.swing.JPanel jPanel4;
    public javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JScrollPane jScrollPane2;
    public javax.swing.JScrollPane jScrollPane3;
    public javax.swing.JScrollPane jScrollPane4;
    public javax.swing.JScrollPane jScrollPane5;
    public javax.swing.JScrollPane jScrollPane6;
    public javax.swing.JButton javaExecBrowseButton;
    public javax.swing.JTextField javaExecField;
    public javax.swing.JLabel javaExecLabel;
    public javax.swing.JMenuItem launchSupportPage;
    public javax.swing.JTextField levelNameField;
    public javax.swing.JLabel levelNameLabel;
    public javax.swing.JTextField levelSeedField;
    public javax.swing.JLabel levelSeedLabel;
    public javax.swing.JPanel mainPanel;
    public javax.swing.JPanel mainWindowTab;
    public javax.swing.JLabel maxPlayersLabel;
    public javax.swing.JSpinner maxPlayersSpinner;
    public javax.swing.JMenuBar menuBar;
    public javax.swing.JCheckBox onlineModeCheckBox;
    public javax.swing.JToggleButton pauseSchedulerButton;
    public javax.swing.JList playerList;
    public javax.swing.JPanel playerListPanel;
    private javax.swing.JProgressBar progressBar;
    public javax.swing.JPanel proxyServerPanel;
    public javax.swing.JCheckBox pvpCheckBox;
    public javax.swing.JLabel receivingBytes;
    public javax.swing.JLabel receivingBytesLabel;
    public javax.swing.JButton saveBackupControlButton;
    public javax.swing.JButton saveGuiConfigButton;
    public javax.swing.JButton saveServerConfigButton;
    public javax.swing.JButton saveWorldsButton;
    public javax.swing.JCheckBox sayCheckBox;
    public javax.swing.JPanel schedulerTab;
    public javax.swing.JPanel serverCmdLinePanel;
    public javax.swing.JPanel serverConfigTab;
    public javax.swing.JPanel serverControlPanel;
    public javax.swing.JLabel serverCpuUsage;
    public javax.swing.JLabel serverCpuUsageLabel;
    public javax.swing.JPanel serverInfoPanel;
    public javax.swing.JTextField serverIpField;
    public javax.swing.JLabel serverIpLabel;
    public javax.swing.JButton serverJarBrowseButton;
    public javax.swing.JTextField serverJarField;
    public javax.swing.JLabel serverJarLabel;
    public javax.swing.JLabel serverMemoryUsage;
    public javax.swing.JLabel serverMemoryUsageLabel;
    public javax.swing.JTextField serverPortField;
    public javax.swing.JLabel serverPortLabel;
    public javax.swing.JPanel serverPropertiesPanel;
    private javax.swing.JLabel serverStatusLabel;
    public javax.swing.JTextField severeColorBox;
    public javax.swing.JLabel severeColorLabel;
    public javax.swing.JToggleButton showWebPasswordButton;
    public javax.swing.JCheckBox spawnAnimalsCheckBox;
    public javax.swing.JCheckBox spawnMonstersCheckBox;
    public javax.swing.JTextField spawnProtectionField;
    public javax.swing.JLabel spawnProtectionLabel;
    public javax.swing.JCheckBox startServerOnLaunchCheckBox;
    public javax.swing.JButton startstopButton;
    private javax.swing.JLabel statusAnimationLabel;
    public javax.swing.JLabel statusBarJob;
    public javax.swing.JPanel statusPanel;
    public javax.swing.JButton submitButton;
    public javax.swing.JTabbedPane tabber;
    public javax.swing.JButton taskListAddButton;
    public javax.swing.JButton taskListEditButton;
    public javax.swing.JButton taskListRemoveButton;
    public javax.swing.JList taskSchedulerList;
    public javax.swing.JPanel taskSchedulerPanel;
    public javax.swing.JTextField textColorBox;
    public javax.swing.JLabel textColorLabel;
    public javax.swing.JSpinner textSizeField;
    public javax.swing.JLabel textSizeLabel;
    public javax.swing.JLabel transmittingBytes;
    public javax.swing.JLabel transmittingBytesLabel;
    public javax.swing.JCheckBox useNetStat;
    public javax.swing.JCheckBox useProxyCheckBox;
    public javax.swing.JCheckBox useWebInterfaceCheckBox;
    public javax.swing.JLabel versionLabel;
    public javax.swing.JMenu versionNotifier;
    public javax.swing.JMenuItem viewChangeLog;
    public javax.swing.JLabel viewDistanceLabel;
    public javax.swing.JSpinner viewDistanceSpinner;
    public javax.swing.JTextField warningColorBox;
    public javax.swing.JLabel warningColorLabel;
    public javax.swing.JPanel webInterfaceTab;
    public javax.swing.JTextPane webLog;
    public javax.swing.JPanel webLogPanel;
    public javax.swing.JPasswordField webPasswordField;
    public javax.swing.JLabel webPasswordLabel;
    public javax.swing.JTextField webPortField;
    public javax.swing.JLabel webPortLabel;
    public javax.swing.JCheckBox whiteListCheckBox;
    public javax.swing.JTextField windowTitleField;
    public javax.swing.JLabel windowTitleLabel;
    public javax.swing.JCheckBox xincgcCheckBox;
    public javax.swing.JTextField xmxMemoryField;
    public javax.swing.JLabel xmxMemoryLabel;
    public javax.swing.JCheckBox zipBackupCheckBox;
    // End of variables declaration//GEN-END:variables

    public void outOfDate(String version) {
        final String ver = version;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                versionNotifier.setForeground(Color.red);
                versionNotifier.setText("New version " + ver + " is available!");
            }
        });
    }

    public void setPlayerList(PlayerList playerListModel) {
        final PlayerList pl = playerListModel;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                GUI.this.playerListModel = pl;
                playerList.setModel(pl);
                playerList.updateUI();
            }
        });
    }

    public boolean startTaskByName(String name) {
        EventModel event = getTaskByName(name);
        if (event != null) {
            scheduleImmediateEvent(event, scheduler, this);
            return true;
        }
        System.out.println("Could not find event by that name");
        return false;
    }

    public EventModel getTaskByName(String name) {
        java.util.Iterator it = config.schedule.getEvents().iterator();
        while (it.hasNext()) {
            EventModel event = (EventModel)it.next();
            if (event.getName().equalsIgnoreCase(name)) {
                return event;
            }
        }
        return null;
    }

    private void enableSystemTrayIcon() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                trayIcon = null;
                if (java.awt.SystemTray.isSupported()) {
                    hideMenu.setEnabled(true);
                    hideMenu.setToolTipText("Press this to minimize to tray.");
                    // get the SystemTray instance
                    java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
                    // load an image
                    org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mcservergui.Main.class).getContext().getResourceMap(GUI.class);
                    // create a action listener to listen for default action executed on the tray icon
                    ActionListener listener = new ActionListener() {
                        @Override public void actionPerformed(ActionEvent e) {
                            if (!isHidden) {
                                GUI.this.getApplication().hide(GUI.this);
                                isHidden = true;
                            } else {
                                GUI.this.getApplication().show(GUI.this);
                                isHidden = false;
                            }
                        }
                    };
                    // create a popup menu
                    java.awt.PopupMenu popup = new java.awt.PopupMenu();
                    // create menu item for the default action
                    java.awt.MenuItem defaultItem = new java.awt.MenuItem("Show/Hide");
                    defaultItem.addActionListener(listener);
                    popup.add(defaultItem);

                    trayIcon = new java.awt.TrayIcon(resourceMap.getImageIcon("imageLabel.icon").getImage(), config.getWindowTitle(), popup);
                    trayIcon.setImageAutoSize(true);
                    // set the TrayIcon properties
                    trayIcon.addActionListener(listener);
                    // add the tray image
                    try {
                        tray.add(trayIcon);
                    } catch (java.awt.AWTException e) {
                        System.err.println(e);
                    }
                } else {
                    hideMenu.setEnabled(false);
                    hideMenu.setToolTipText("Your Operating System does not support this action!");
                }
            }
        });
    }

    public void addTaskListEntry() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JFrame mainFrame = Main.getApplication().getMainFrame();
                taskDialog = new TaskDialog(mainFrame, config, scheduler, GUI.this);
                taskDialog.setLocationRelativeTo(mainFrame);
                Main.getApplication().show(taskDialog);
            }
        });
    }

    public void editTaskListEntry(EventModel evt) {
        final EventModel event = evt;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JFrame mainFrame = Main.getApplication().getMainFrame();
                taskDialog = new TaskDialog(mainFrame, config, scheduler, GUI.this,
                        event);
                taskDialog.setLocationRelativeTo(mainFrame);
                Main.getApplication().show(taskDialog);
            }
        });
    }

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
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (!controlState.equals("BACKUP")) {
                    backupFileChooser.updateUI();
                }
            }
        });
    }

    public void backup() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                stateBeforeBackup = controlState;
                controlSwitcher("BACKUP");
                statusBarJob.setText("Backing up:");
                TaskMonitor taskMonitor = getApplication().getContext().getTaskMonitor();
                TaskService taskService = getApplication().getContext().getTaskService();
                if (server.isRunning()) {
                    sendInput("save-off");
                    sendInput("say Backing up server...");
                }
                new File(config.backups.getPath()).mkdir(); // Creates backup directory if it doesn't exist
                Backup backup = new Backup(GUI.this, backupStatusLog);
                taskMonitor.setForegroundTask(backup.getTask());
                backupStatusLog.setText("");
                backup.addObserver(GUI.this);
                taskService.execute(backup.getTask());
            }
        });
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
    public void sendInput(boolean b) {
        final boolean shouldSay = b;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                String stringToSend = consoleInput.getText();
                if (inputHistory.size() >= config.getInputHistoryMaxSize()) {
                    inputHistory.remove(inputHistory.size() - 1);
                }
                inputHistory.add(0, stringToSend);
                if ((sayCheckBox.isSelected()) && (!shouldSay)) {
                    sendInput("say " + stringToSend);
                } else if ((!sayCheckBox.isSelected()) && (shouldSay)) {
                    sendInput("say " + stringToSend);
                } else {
                    sendInput(stringToSend);
                }
                consoleInput.setText("");
                inputHistoryIndex = -1;
            }
        });
    }

    /**
     * Sends a String to the server.
     * @param s String to send to the server
     */
    public void sendInput(String s) {
        server.send(s);
    }

    public void setSaving(boolean b) {
        saving = b;
    }

    public boolean isSaving() {
        return saving;
    }

    /**
     * Determines if keyboard focus should be given to the console input field based on the passed KeyEvent.
     * Basically any alpha-numeric keys will cause focus to be granted.
     * @param evt KeyEvent to be passed in.
     */
    public void giveInputFocus(java.awt.event.KeyEvent event) {
        final java.awt.event.KeyEvent evt = event;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if ((evt.getKeyChar() != java.awt.event.KeyEvent.CHAR_UNDEFINED) && (consoleInput.isEnabled()) && ((int)evt.getKeyChar() > 32)) {
                    if (consoleInput.requestFocusInWindow()) {
                        consoleInput.setText(consoleInput.getText() + evt.getKeyChar());
                    }
                }
            }
        });
    }

    public void startWebServer() {
        webServer.setPort(config.web.getPort());
        webServer.start();
    }

    public void stopWebServer() {
        webServer.stop();
    }

    public String getConsoleOutput(OutputFormat format) {
        String output = getConsoleOutput();
        output = Jsoup.clean(output, Whitelist.none().addTags("br"));
        switch (format) {
            case LINEBREAK:
                //output = output.replaceAll("(<font.*>|</font>)", "");
                break;
            case PLAINTEXTCRLF:
                //output = output.replaceAll("(<font.*>|</font>)", "");
                output = output.replaceAll("<br />", Character.toString((char)13)
                        + Character.toString((char)10));
                break;
            case PLAINTEXTLFCR:
                //output = output.replaceAll("(<font.*>|</font>)", "");
                output = output.replaceAll("<br />", Character.toString((char)10)
                        + Character.toString((char)13));
                break;
            case PLAINTEXTLF:
                //output = output.replaceAll("(<font.*>|</font>)", "");
                output = output.replaceAll("<br />", Character.toString((char)10));
                break;
            case PLAINTEXTCR:
                //output = output.replaceAll("(<font.*>|</font>)", "");
                output = output.replaceAll("<br />", Character.toString((char)13));
                break;
            default:
        }
        return output;
    }

    public String getConsoleOutput() {
        String output = "";
        
        class ConsoleOutput implements Runnable {
            String output;
            
            public ConsoleOutput() {
                super();
                output = "";
            }
            
            @Override public void run() {
                output = consoleOutput.getText();
            }

            public String getOutput() {
                return output;
            }
        }
        
        try {
            ConsoleOutput conOut = new ConsoleOutput();
            SwingUtilities.invokeAndWait(conOut);
            output = conOut.getOutput();
        } catch (InterruptedException ie) {
            output = "[MC Server GUI] Interrupted while retrieving output from GUI!";
        } catch (java.lang.reflect.InvocationTargetException ite) {
            output = "[MC Server GUI] Error retrieving output from GUI!";
        }
        
        output = output.replaceAll("(<html.*>|<body.*>|<head.*>|</head>|</body>|</html>)", "");
        return output;
    }

    /**
     * Initializes the config file if necessary and sets all the gui elements to their config'd values
     * Usually this is only called once during the constructor.
     */
    public void initConfig() {
        setConsoleOutput("");
        if (config.load()) {
            guiLog("Configuration file loaded succesfully!");
        } else {
            guiLog("Configuration file not found or invalid!", LogLevel.WARNING);
            guiLog("Creating new config file with default values.");
        }
        if (config.cmdLine.getServerJar().isEmpty()) {
            if (detectServerJar()) {
                guiLog("Automatically detected server jar file: "
                        + config.cmdLine.getServerJar());
            } else {
                guiLog("Could not locate a server jar file automatically!",
                        LogLevel.WARNING);
            }
        }
        updateGuiWithConfigValues();
        updateGuiWithServerProperties();
        initSchedule();
        
        saveConfig();
        
        if (config.web.isEnabled()) {
            startWebServer();
        }
    }

    public boolean detectServerJar() {
        File dir = new File(".");
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().matches("(\\S*bukkit\\S*.jar|\\S*server\\S*"
                    + ".jar)")) {
                config.cmdLine.setServerJar(files[i].getName());
                return true;
            }
        }
        return false;
    }

    public void initSchedule() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                java.util.Iterator it = config.schedule.getEvents().iterator();
                while (it.hasNext()) {
                    EventModel event = (EventModel)it.next();
                    if (!event.isCustomButton()) {
                        scheduleEvent(event, scheduler, GUI.this);
                    } else {
                        customCombo1.addItem(event.getName());
                        customCombo2.addItem(event.getName());
                    }
                }
                customCombo1.setSelectedItem(config.getCustomButton1());
                customCombo2.setSelectedItem(config.getCustomButton2());
            }
        });
    }

    public void initBackupFileChooser() {
        backupFileChooser.setCheckingModel(new GUITreeCheckingModel(backupFileSystem, GUI.this));
        //backupFileChooser.setCheckingPaths(createTreePathArray(config.backups.getPathsToBackup()));
        //javax.swing.tree.TreePath[] paths = createTreePathArray(config.backups.getPathsToBackup());
        //for (int i = 0; i < paths.length; i++) {
//backupFileChooser.addCheckingPath(paths[i]);
            Thread initBackupPaths = new Thread() {
                @Override
                public void run() {
backupFileChooser.setCheckingPaths(createTreePathArray(config.backups.getPathsToBackup()));
                }
            };
            initBackupPaths.start();

        //}
    }

    public void updateGuiWithServerProperties() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                allowFlightCheckBox.setSelected(serverProperties.getAllowFlight());
                allowNetherCheckBox.setSelected(serverProperties.getAllowNether());
                onlineModeCheckBox.setSelected(serverProperties.getOnlineMode());
                pvpCheckBox.setSelected(serverProperties.getPvp());
                spawnAnimalsCheckBox.setSelected(serverProperties.getSpawnAnimals());
                spawnMonstersCheckBox.setSelected(serverProperties.getSpawnMonsters());
                whiteListCheckBox.setSelected(serverProperties.getWhiteList());
                levelNameField.setText(serverProperties.getLevelName());
                levelSeedField.setText(serverProperties.getLevelSeed());
                serverIpField.setText(serverProperties.getServerIp());
                serverPortField.setText(serverProperties.getServerPort());
                spawnProtectionField.setText(serverProperties.getSpawnProtection());
                maxPlayersSpinner.setValue(serverProperties.getMaxPlayers());
                viewDistanceSpinner.setValue(serverProperties.getViewDistance());
            }
        });
    }

    public void updateGuiWithConfigValues() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                textColorBox.setBackground(java.awt.Color.decode("0x"
                        + config.display.getTextColor()));
                bgColorBox.setBackground(java.awt.Color.decode("0x"
                        + config.display.getBgColor()));
                infoColorBox.setBackground(java.awt.Color.decode("0x"
                        + config.display.getInfoColor()));
                warningColorBox.setBackground(java.awt.Color.decode("0x"
                        + config.display.getWarningColor()));
                severeColorBox.setBackground(java.awt.Color.decode("0x"
                        + config.display.getSevereColor()));
                textSizeField.setValue(config.display.getTextSize());
                webPortField.setText(Integer.toString(config.web.getPort()));
                useWebInterfaceCheckBox.setSelected(config.web.isEnabled());
                webPasswordField.setText(config.web.getPassword());
                disableGetOutputNotificationsCheckBox.setSelected(config.web.isDisableGetRequests());
                useProxyCheckBox.setSelected(config.getProxy());
                extPortField.setText(Integer.toString(config.getExtPort()));
                startServerOnLaunchCheckBox.setSelected(config.getServerStartOnStartup());
                zipBackupCheckBox.setSelected(config.backups.getZip());
                clearLogCheckBox.setSelected(config.backups.getClearLog());
                //pathsToBackup = config.backups.getPathsToBackup();
                backupPathField.setText(config.backups.getPath());
                //backupFileChooser.setCheckingPaths(createTreePathArray(pathsToBackup));
                initBackupFileChooser();
                windowTitleField.setText(config.getWindowTitle());
                commandPrefixField.setText(config.getCommandPrefix());
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
                    xmxMemoryField.setEditable(false);
                    xincgcCheckBox.setEnabled(false);
                    extraArgsField.setEditable(false);
                }
                cmdLineField.setText(config.cmdLine.parseCmdLine());
                taskSchedulerList.setModel(config.schedule.getEvents());
                if (useProxyCheckBox.isSelected()) {
                    playerList.setToolTipText("This shows a list of players connected to the server.  Right click a to pull up the player action menu.");
                } else {
                    playerList.setToolTipText("Player list is currently only supported when using the GUI's Proxy feature.");
                }
            }
        });
    }

    public void saveBackupPathsToConfig() {
        config.backups.getPathsToBackup().clear();
        for (int i = 0; i < backupFileChooser.getCheckingRoots().length; i++) {
            config.backups.getPathsToBackup().add(backupFileChooser.getCheckingRoots()[i].getLastPathComponent().toString());
        }
        //config.backups.setPathsToBackup(pathsToBackup);
    }

    /**
     * Saves the config file with any changes made by the user through the gui.
     */
    public void saveConfig() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                config.setWindowTitle(windowTitleField.getText());
                getFrame().setTitle(windowTitleField.getText());
                if (trayIcon != null) {
                    trayIcon.setToolTip(windowTitleField.getText());
                }
                config.setInputHistoryMaxSize(Integer.parseInt(inputHistoryMaxSizeField.getText()));
                config.setCommandPrefix(commandPrefixField.getText());
                config.setCustomButton1(customCombo1.getSelectedItem().toString());
                config.setCustomButton2(customCombo2.getSelectedItem().toString());
                config.cmdLine.setXmx(xmxMemoryField.getText());
                config.cmdLine.setExtraArgs(extraArgsField.getText());
                config.cmdLine.setServerJar(serverJarField.getText());
                config.cmdLine.setJavaExec(javaExecField.getText());
                config.display.setTextSize(Integer.valueOf(textSizeField.getValue().toString()));
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
                //config.backups.setPathsToBackup(pathsToBackup);
                config.backups.setPath(backupPathField.getText());
                config.setProxy(useProxyCheckBox.isSelected());
                config.setExtPort(Integer.valueOf(extPortField.getText()));

                config.save();
                saveServerProperties();
            }
        });
    }

    public void saveServerProperties() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                serverProperties.setAllowFlight(allowFlightCheckBox.isSelected());
                serverProperties.setAllowNether(allowNetherCheckBox.isSelected());
                serverProperties.setOnlineMode(onlineModeCheckBox.isSelected());
                serverProperties.setPvp(pvpCheckBox.isSelected());
                serverProperties.setSpawnAnimals(spawnAnimalsCheckBox.isSelected());
                serverProperties.setSpawnMonsters(spawnMonstersCheckBox.isSelected());
                serverProperties.setWhiteList(whiteListCheckBox.isSelected());
                serverProperties.setLevelName(levelNameField.getText());
                serverProperties.setLevelSeed(levelSeedField.getText());
                serverProperties.setServerIp(serverIpField.getText());
                serverProperties.setServerPort(serverPortField.getText());
                serverProperties.setSpawnProtection(spawnProtectionField.getText());
                serverProperties.setMaxPlayers(Integer.valueOf(maxPlayersSpinner.getValue().toString()));
                serverProperties.setViewDistance(Integer.valueOf(viewDistanceSpinner.getValue().toString()));
                if (!server.isRunning()) {
                    try {
                        serverProperties.writeProps();
                    } catch (IOException ioe) {

                    }
                }
            }
        });
    }

    /**
     * As long as textScrolling is true, this will cause the consoleOutput to be scrolled to the bottom.
     */
    public void scrollText() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (textScrolling) {
                    consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
                }
                webLog.setCaretPosition(webLog.getDocument().getLength());
            }
        });
    }

    public boolean isRestarting() { return restarting; }

    public void restartServer() { restartServer(0); }

    public void restartServer(int delay) {
        restarting = true;
        restartDelay = delay;
        stopServer();
        System.out.println("Sent stop");
    }

    /**
     * Starts the Minecraft server and verifies that it started properly.
     */
    public void startServer() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (controlState.equals("OFF")) {
                    setConsoleOutput("");
                    server.setCmdLine(config.cmdLine.getCmdLine());
                    if (server.start().equals("SUCCESS")) {
                    } else if (server.start().equals("ERROR")) {
                        
                    } else if (server.start().equals("INVALIDJAR")) {
                        guiLog("The jar file you specified is not a valid file."
                                + "  Please make corrections on the Server "
                                + "Config tab.", LogLevel.WARNING);
                    }
                }
            }
        });
    }

    public void startServer(int delay) {
        final int dly = delay;
        class ServerStartThread extends Thread {
            // This method is called when the thread runs
            @Override public void run() {
                try {
                    System.out.println("sleeping for " + dly);
                    Thread.sleep(dly * 1000);
                } catch (InterruptedException ie) { }
                startServer();
                System.out.println("Sent start");
                restarting = false;
            }
        }
        new ServerStartThread().start();
    }

    /**
     * Tells the Minecraft server to stop.
     */
    public void stopServer() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (controlState.equals("ON")) {
                    server.stop();
                }
            }
        });
    }

    public void guiLog(String message, LogLevel level) {
        // format message
        String text = getTimeStamp() + " ";
        switch (level) {
            case INFO:
                text += "[INFO]";
                break;
            case WARNING:
                text += "[WARNING]";
                break;
            case SEVERE:
                text += "[SEVERE]";
                break;
            default:
                text += "[INFO]";
        }
        message = text + " MC Server GUI: " + message;
        // put message in console output
        addTextToConsoleOutput(message);

        // log message to file
        File logFile = new File("mcservergui.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException ignore) { }
        }
        if (!logFile.canWrite()) {
            return;
        }
        FileWriter logWriter = null;
        try {
            logWriter = new FileWriter(logFile, true);
            logWriter.append(message + System.getProperty("line.separator"));
        } catch (IOException ignore) { }
        finally {
            if (logWriter != null) {
                try {
                    logWriter.close();
                } catch (IOException ignore) { }
            }
        }
    }

    public void guiLog(String message) {
        guiLog(message, LogLevel.INFO);
    }

    /**
     * Adds text to the end of the Console Output box.
     * @param textToAdd String of text to add.
     */
    public void addTextToConsoleOutput(String textToAdd) {
        final String text = textToAdd;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try
                {
                    ((HTMLEditorKit)consoleOutput.getEditorKit())
                            .insertHTML((HTMLDocument)consoleOutput.getDocument(),
                            consoleOutput.getDocument().getEndPosition().getOffset()-1,
                            parser.parseText(text),
                            1, 0, null);
                } catch ( Exception e ) {
                    e.printStackTrace();
                    System.err.println("Error appending text to console output: " + text);
                }
            }
        });
        scrollText();
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void webLogAdd(String textToAdd) {
        final String text = textToAdd;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try
                {
                    String textToAdd = parser.parseText(text);

                    ((HTMLEditorKit)webLog.getEditorKit())
                            .insertHTML((HTMLDocument)webLog.getDocument(),
                            webLog.getDocument().getEndPosition().getOffset()-1,
                            textToAdd, 1, 0, null);
                    //System.out.println(webLog.getText());
                } catch ( Exception e ) {
                    //e.printStackTrace();
                    System.err.println("Error appending text to web interface log: " + text);
                }
                scrollText();
            }
        });
        
    }

    public void setConsoleOutput(String setText) {
        final String text = setText;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                consoleOutput.setText("<body bgcolor = " + config.display.getBgColor()
                        + "><font color = \"" + config.display.getTextColor()
                        + "\" size = " + config.display.getTextSize() + ">" + text);
            }
        });
    }

    public void webLogReplace(String replaceText) {
        final String text = replaceText;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                webLog.setText("<body bgcolor = " + config.display.getBgColor()
                        + "><font color = \"" + config.display.getTextColor()
                        + "\" size = " + config.display.getTextSize() + ">" + text);
            }
        });
    }

    public void updateConsoleOutputBgColor() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                consoleOutput.setText(consoleOutput.getText().replaceFirst(
                        "([\\d,a,b,c,d,e,f,A,B,C,D,E,F]){6}",
                        config.display.getBgColor()));
                webLog.setText(webLog.getText().replaceFirst(
                        "([\\d,a,b,c,d,e,f,A,B,C,D,E,F]){6}",
                        config.display.getBgColor()));
            }
        });
    }

    /**
     * Observer update() method.  Called when the MCServerModel has messages for the View
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
            if (restarting) {
                startServer(restartDelay);
            }
        }
        if (arg.equals("serverStarted")) {
            controlSwitcher("ON");
        }

        if (arg.equals("finishedBackup")) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    if (server.isRunning()) {
                        sendInput("say Server backup complete!");
                        sendInput("save-on");
                    }
                    controlSwitcher("!BACKUP");
                }
            });
        }
    }

    /**
     * Switches GUI components into specific states based on param passed.
     * @param serverState Typically the state of the server. "ON" or "OFF"
     */
    private void controlSwitcher(String newServerState) {
        final String serverState = newServerState;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                controlState = serverState;
                if (serverState.equals("ON")) {
                    // Switch GUI control to "ON" status
                    startstopButton.setText("Stop");
                    consoleInput.setEnabled(true);
                    submitButton.setEnabled(true);
                    serverStatusLabel.setForeground(Color.BLUE);
                    serverStatusLabel.setText("Server UP");
                    textScrolling = true;
                    saveWorldsButton.setEnabled(true);
                    useProxyCheckBox.setEnabled(false);
                } else if (serverState.equals("OFF")) {
                    // Switch GUI controls to "OFF" status
                    startstopButton.setText("Start");
                    consoleInput.setEnabled(false);
                    submitButton.setEnabled(false);
                    serverStatusLabel.setForeground(Color.red);
                    serverStatusLabel.setText("Server DOWN");
                    textScrolling = false;
                    mouseInConsoleOutput = false;
                    saveWorldsButton.setEnabled(false);
                    useProxyCheckBox.setEnabled(true);
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
                    statusBarJob.setText("");
                    controlSwitcher(stateBeforeBackup);
                }
            }
        });
    }

    public boolean isPropagatingChecks() {
        return propagatingChecks;
    }

    public void setPropagatingChecks(boolean b) {
        propagatingChecks = b;
    }

    public String getControlState() {
        return controlState;
    }

    public String getServerStatus() {
        if (server.isRunning()) {
            return "UP";
        } else {
            return "DOWN";
        }
    }

    public MCServerModel server;
    //private MCServerReceiver serverReceiver;
    //private MainWorker mainWorker;
    private boolean textScrolling;
    private boolean mouseInConsoleOutput;
    private List<String> inputHistory;
    //private List<String> pathsToBackup;
    private int inputHistoryIndex;
    private String controlState;
    private String stateBeforeBackup;
    private mcservergui.fileexplorer.FileSystemModel backupFileSystem;
    public Config config;
    //private boolean badConfig;
    private Scheduler scheduler;
    //private GUIListModel taskList;
    public javax.swing.DefaultComboBoxModel customButtonBoxModel1;
    public javax.swing.DefaultComboBoxModel customButtonBoxModel2;
    private boolean restarting;
    private ConsoleParser parser;
    private boolean saving;
    private ServerProperties serverProperties;
    private PlayerList playerListModel;
    private boolean isHidden;
    private java.awt.TrayIcon trayIcon;
    private WebInterface webServer;
    private String versionNumber;
    private int restartDelay;
    private boolean propagatingChecks;

    public static enum LogLevel { INFO, WARNING, SEVERE }
    public static enum OutputFormat { 
        LINEBREAK, PLAINTEXTCRLF, PLAINTEXTLFCR, PLAINTEXTCR, PLAINTEXTLF
    }

    //Auto created
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;

    private TaskDialog taskDialog;
}

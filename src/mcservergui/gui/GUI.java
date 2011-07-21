/*
 * GUI.java
 */
package mcservergui.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
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
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLDocument;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingModel;
import mcservergui.MainWorker;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
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
public class GUI extends FrameView implements Observer, ComponentListener {

    private static final int MIN_WIDTH = 451;
    private static final int MIN_HEIGHT = 521;

    public GUI(Main app,/* MCServerModel newServer, Config newConfig,*/ Scheduler scheduler) {
        super(app);

        config = new Config();
        initConfig();


        getFrame().addComponentListener(this);

        UIManager.put("TitledBorder.border", new BorderUIResource(new EtchedBorder()));
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }

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
             public void actionPerformed(ActionEvent e) {
                //serverStatusLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
             public void actionPerformed(ActionEvent e) {
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
             public void propertyChange(java.beans.PropertyChangeEvent evt) {
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

        server = new MCServerModel(this);
        mainWorker = new MainWorker(this);
        server.addObserver(this);
        server.addObserver(app);
        server.addObserver(mainWorker);
        server.addObserver(serverProperties);
        server.setServerProps(serverProperties);
        mainWorker.startMainWorker();
        getFrame().setTitle(config.getWindowTitle());
        controlSwitcher("OFF");

        inputHistory = new ArrayList<String>();
        inputHistoryIndex = -1;

        enableSystemTrayIcon();

        parser = new ConsoleParser(config.display, this);

        webServer = new WebInterface(this);
        schedulePaused = false;

        updateGuiWithConfigValues();
        updateGuiWithServerProperties();
        initSchedule();

        saveConfig();

        if (config.web.isEnabled()) {
            startWebServer();
        }
    }

    public void componentResized(ComponentEvent e) {
        int width = getFrame().getWidth();
        int height = getFrame().getHeight();
        //we check if either the width
        //or the height are below minimum
        boolean resize = false;
        if (width < MIN_WIDTH) {
            resize = true;
            width = MIN_WIDTH;
        }
        if (height < MIN_HEIGHT) {
            resize = true;
            height = MIN_HEIGHT;
        }
        if (resize) {
            getFrame().setSize(width, height);
        }
    }

    public void componentMoved(ComponentEvent e) {
    }
    public void componentShown(ComponentEvent e) {
    }
    public void componentHidden(ComponentEvent e) {
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
     */
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        tabber = new javax.swing.JTabbedPane();
        mainWindowTab = new javax.swing.JPanel();
        consoleOutputPanel = new javax.swing.JPanel();
        consoleOutScrollPane = new javax.swing.JScrollPane();
        consoleOutput = new javax.swing.JTextPane();
        playerListPanel = new javax.swing.JPanel();
        playerListScrollPane = new javax.swing.JScrollPane();
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
        configurationTab = new javax.swing.JPanel();
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
        customCommandLineLabel = new javax.swing.JLabel();
        cmdLineField = new javax.swing.JTextField();
        customLaunchCheckBox = new javax.swing.JCheckBox();
        saveServerConfigButton = new javax.swing.JButton();
        useProxyCheckBox = new javax.swing.JCheckBox();
        extPortLabel = new javax.swing.JLabel();
        extPortField = new javax.swing.JTextField();
        intPortLabel = new javax.swing.JLabel();
        intPortField = new javax.swing.JTextField();
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
        themeTab = new javax.swing.JPanel();
        guiConfigPanel = new javax.swing.JPanel();
        windowTitleLabel = new javax.swing.JLabel();
        windowTitleField = new javax.swing.JTextField();
        inputHistoryMaxSizeLabel = new javax.swing.JLabel();
        inputHistoryMaxSizeField = new javax.swing.JTextField();
        startServerOnLaunchCheckBox = new javax.swing.JCheckBox();
        commandPrefixLabel = new javax.swing.JLabel();
        commandPrefixField = new javax.swing.JTextField();
        saveGuiConfigButton = new javax.swing.JButton();
        colorizationPanel = new javax.swing.JPanel();
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
        taskSchedulerScrollPane = new javax.swing.JScrollPane();
        taskSchedulerList = new javax.swing.JList();
        taskListAddButton = new javax.swing.JButton();
        taskListEditButton = new javax.swing.JButton();
        taskListRemoveButton = new javax.swing.JButton();
        pauseSchedulerButton = new javax.swing.JToggleButton();
        webInterfaceTab = new javax.swing.JPanel();
        webInterfaceConfigPanel = new javax.swing.JPanel();
        webPortLabel = new javax.swing.JLabel();
        webPortField = new javax.swing.JTextField();
        useWebInterfaceCheckBox = new javax.swing.JCheckBox();
        webPasswordLabel = new javax.swing.JLabel();
        webPasswordField = new javax.swing.JPasswordField();
        showWebPasswordButton = new javax.swing.JToggleButton();
        disableGetOutputNotificationsCheckBox = new javax.swing.JCheckBox();
        webLogPanel = new javax.swing.JPanel();
        webLogScrollPane = new javax.swing.JScrollPane();
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

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mcservergui.Main.class).getContext().getResourceMap(GUI.class);

        mainPanel.setLayout(new MigLayout("fill", "0[]0", "0[]0"));

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
        mainPanel.add(tabber, "grow");
        {
            mainWindowTab.setName("mainWindowTab");
            mainWindowTab.setLayout(new MigLayout("fill", "0[33%]0[33%]0[33%]0", "0[]0[min!]0[min!]0"));
            tabber.addTab(resourceMap.getString("mainWindowTab.TabConstraints.tabTitle"), mainWindowTab);
            {
                consoleOutputPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("consoleOutputPanel.border.title")));
                consoleOutputPanel.setName("consoleOutputPanel");
                consoleOutputPanel.setLayout(new MigLayout("fill", "0[]0", "0[]0"));
                mainWindowTab.add(consoleOutputPanel, "width 80%, grow, span 3, split 2");
                {
                    consoleOutScrollPane.setName("consoleOutScrollPane");
                    consoleOutScrollPane.setViewportView(consoleOutput);
                    consoleOutputPanel.add(consoleOutScrollPane, "grow");
                    {
                        consoleOutput.setContentType(resourceMap.getString("consoleOutput.contentType")); // NOI18N
                        consoleOutput.setEditable(false);
                        consoleOutput.setToolTipText(resourceMap.getString("consoleOutput.toolTipText")); // NOI18N
                        consoleOutput.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
                        consoleOutput.setName("consoleOutput"); // NOI18N
                        consoleOutput.addMouseListener(new java.awt.event.MouseAdapter() {
                            public void mouseClicked(java.awt.event.MouseEvent evt) {
                                consoleOutputMouseClicked(evt);
                            }
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
                    }
                }

                playerListPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("playerListPanel.border.title")));
                playerListPanel.setName("playerListPanel");
                playerListPanel.setLayout(new MigLayout("fill", "0[]0", "0[]0"));
                mainWindowTab.add(playerListPanel, "gapleft -5, width 20%, grow, wrap");
                {
                    playerListScrollPane.setName("playerListScrollPane");
                    playerListScrollPane.setViewportView(playerList);
                    playerListPanel.add(playerListScrollPane, "grow");
                    {
                        playerList.setModel(playerListModel);
                        playerList.setToolTipText(resourceMap.getString("playerList.toolTipText"));
                        playerList.setFocusable(false);
                        playerList.setName("playerList");
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
                    }
                }

                consoleInputPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("consoleInputPanel.border.title")));
                consoleInputPanel.setName("consoleInputPanel");
                consoleInputPanel.setLayout(new MigLayout("fill", "0[]0", "0[]0"));
                mainWindowTab.add(consoleInputPanel, "grow, span 3, wrap");
                {
                    sayCheckBox.setText(resourceMap.getString("sayCheckBox.text")); // NOI18N
                    sayCheckBox.setToolTipText(resourceMap.getString("sayCheckBox.toolTipText")); // NOI18N
                    sayCheckBox.setName("sayCheckBox"); // NOI18N
                    sayCheckBox.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyTyped(java.awt.event.KeyEvent evt) {
                            sayCheckBoxKeyTyped(evt);
                        }
                    });
                    consoleInputPanel.add(sayCheckBox, "growx 0, split 3");

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
                    consoleInputPanel.add(consoleInput, "growx");

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
                    consoleInputPanel.add(submitButton, "growx 0");
                }

                serverControlPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("serverControlPanel.border.title")));
                serverControlPanel.setName("serverControlPanel");
                serverControlPanel.setLayout(new MigLayout("fill", "0[35%]0[55%]0[10%]0", "0[]0"));
                mainWindowTab.add(serverControlPanel, "grow");
                {
                    startstopButton.setText(resourceMap.getString("startstopButton.text"));
                    startstopButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
                    startstopButton.setName("startstopButton");
                    startstopButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            startstopButtonActionPerformed(evt);
                        }
                    });
                    serverControlPanel.add(startstopButton, "growx");

                    saveWorldsButton.setText(resourceMap.getString("saveWorldsButton.text"));
                    saveWorldsButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
                    saveWorldsButton.setName("saveWorldsButton");
                    saveWorldsButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            saveWorldsButtonActionPerformed(evt);
                        }
                    });
                    serverControlPanel.add(saveWorldsButton, "span 2, growx, wrap");

                    customCombo1.setModel(customButtonBoxModel1);
                    customCombo1.setToolTipText(resourceMap.getString("customCombo1.toolTipText"));
                    customCombo1.setName("customCombo1");
                    serverControlPanel.add(customCombo1, "span 2, growx");

                    customButton1.setText(resourceMap.getString("customButton1.text"));
                    customButton1.setToolTipText(resourceMap.getString("customButton1.toolTipText"));
                    customButton1.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(customButton1);
                    customButton1.setMargin(new java.awt.Insets(2, 2, 2, 2));
                    customButton1.setName("customButton1");
                    customButton1.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            customButton1ActionPerformed(evt);
                        }
                    });
                    serverControlPanel.add(customButton1, "right, growx, wrap");

                    customCombo2.setModel(customButtonBoxModel2);
                    customCombo2.setToolTipText(resourceMap.getString("customCombo2.toolTipText"));
                    customCombo2.setName("customCombo2");
                    serverControlPanel.add(customCombo2, "span 2, growx");

                    customButton2.setText(resourceMap.getString("customButton2.text"));
                    customButton2.setToolTipText(resourceMap.getString("customButton2.toolTipText"));
                    customButton2.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(customButton2);
                    customButton2.setMargin(new java.awt.Insets(2, 2, 2, 2));
                    customButton2.setName("customButton2");
                    customButton2.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            customButton2ActionPerformed(evt);
                        }
                    });
                    serverControlPanel.add(customButton2, "right, growx");
                }

                serverInfoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Server Information"));
                serverInfoPanel.setName("serverInfoPanel");
                serverInfoPanel.setLayout(new MigLayout());
                mainWindowTab.add(serverInfoPanel, "grow");
                {
                    serverCpuUsageLabel.setText(resourceMap.getString("serverCpuUsageLabel.text"));
                    serverCpuUsageLabel.setName("serverCpuUsageLabel");
                    serverInfoPanel.add(serverCpuUsageLabel);

                    serverCpuUsage.setText(resourceMap.getString("serverCpuUsage.text"));
                    serverCpuUsage.setName("serverCpuUsage");
                    serverInfoPanel.add(serverCpuUsage, "wrap");

                    serverMemoryUsageLabel.setText(resourceMap.getString("serverMemoryUsageLabel.text"));
                    serverMemoryUsageLabel.setName("serverMemoryUsageLabel");
                    serverInfoPanel.add(serverMemoryUsageLabel);

                    serverMemoryUsage.setText(resourceMap.getString("serverMemoryUsage.text"));
                    serverMemoryUsage.setName("serverMemoryUsage");
                    serverInfoPanel.add(serverMemoryUsage, "wrap");

                    receivingBytesLabel.setText(resourceMap.getString("receivingBytesLabel.text"));
                    receivingBytesLabel.setName("receivingBytesLabel");
                    serverInfoPanel.add(receivingBytesLabel);

                    receivingBytes.setText(resourceMap.getString("receivingBytes.text"));
                    receivingBytes.setName("receivingBytes");
                    serverInfoPanel.add(receivingBytes, "wrap");

                    transmittingBytesLabel.setText(resourceMap.getString("transmittingBytesLabel.text"));
                    transmittingBytesLabel.setName("transmittingBytesLabel");
                    serverInfoPanel.add(transmittingBytesLabel);

                    transmittingBytes.setText(resourceMap.getString("transmittingBytes.text"));
                    transmittingBytes.setName("transmittingBytes");
                    serverInfoPanel.add(transmittingBytes);
                }

                guiInfoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("GUI Information"));
                guiInfoPanel.setName("guiInfoPanel");
                guiInfoPanel.setLayout(new MigLayout());
                mainWindowTab.add(guiInfoPanel, "grow");
                {
                    guiCpuUsageLabel.setText(resourceMap.getString("guiCpuUsageLabel.text"));
                    guiCpuUsageLabel.setName("guiCpuUsageLabel");
                    guiInfoPanel.add(guiCpuUsageLabel, "");

                    guiCpuUsage.setText(resourceMap.getString("guiCpuUsage.text"));
                    guiCpuUsage.setName("guiCpuUsage");
                    guiInfoPanel.add(guiCpuUsage, "wrap");

                    guiMemoryUsageLabel.setText(resourceMap.getString("guiMemoryUsageLabel.text"));
                    guiMemoryUsageLabel.setName("guiMemoryUsageLabel");
                    guiInfoPanel.add(guiMemoryUsageLabel, "");

                    guiMemoryUsage.setText(resourceMap.getString("guiMemoryUsage.text"));
                    guiMemoryUsage.setName("guiMemoryUsage");
                    guiInfoPanel.add(guiMemoryUsage, "wrap");

                    versionLabel.setText(resourceMap.getString("versionLabel.text"));
                    versionLabel.setName("versionLabel");
                    guiInfoPanel.add(versionLabel, "span 2, wrap");

                    useNetStat.setText(resourceMap.getString("useNetStat.text"));
                    useNetStat.setToolTipText(resourceMap.getString("useNetStat.toolTipText"));
                    useNetStat.setName("useNetStat");
                    guiInfoPanel.add(useNetStat, "span 2");
                }
            }

            configurationTab.setName("configurationTab");
            configurationTab.setLayout(new MigLayout("fill", "0[]0", "0[]0[]0[]0[min!]0"));
            tabber.addTab(resourceMap.getString("configurationTab.TabConstraints.tabTitle"), configurationTab);
            {
                serverCmdLinePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("serverCmdLinePanel.border.title"))); // NOI18N
                serverCmdLinePanel.setName("serverCmdLinePanel");
                serverCmdLinePanel.setLayout(new MigLayout("fill", "0[min!]0[]0[min!]0", "0[]0[]0[]0[]0"));
                configurationTab.add(serverCmdLinePanel, "grow, wrap");
                {
                    javaExecLabel.setText(resourceMap.getString("javaExecLabel.text"));
                    javaExecLabel.setName("javaExecLabel");
                    serverCmdLinePanel.add(javaExecLabel, "grow 0");

                    javaExecField.setText(resourceMap.getString("javaExecField.text"));
                    javaExecField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(javaExecField);
                    javaExecField.setToolTipText(resourceMap.getString("javaExecField.toolTipText"));
                    javaExecField.setName("javaExecField");
                    javaExecField.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            javaExecFieldActionPerformed(evt);
                        }
                    });
                    serverCmdLinePanel.add(javaExecField, "growx, growy 0");

                    javaExecBrowseButton.setText(resourceMap.getString("javaExecBrowseButton.text"));
                    javaExecBrowseButton.putClientProperty("JComponent.sizeVariant", "mini");
                    SwingUtilities.updateComponentTreeUI(javaExecBrowseButton);
                    javaExecBrowseButton.setToolTipText(resourceMap.getString("javaExecBrowseButton.toolTipText"));
                    javaExecBrowseButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
                    javaExecBrowseButton.setName("javaExecBrowseButton");
                    javaExecBrowseButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            javaExecBrowseButtonActionPerformed(evt);
                        }
                    });
                    serverCmdLinePanel.add(javaExecBrowseButton, "wrap, growx 0");

                    serverJarLabel.setText(resourceMap.getString("serverJarLabel.text"));
                    serverJarLabel.setName("serverJarLabel");
                    serverCmdLinePanel.add(serverJarLabel, "growx 0");

                    serverJarField.setText(resourceMap.getString("serverJarField.text"));
                    serverJarField.setName("serverJarField");
                    serverJarField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(serverJarField);
                    serverJarField.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            serverJarFieldActionPerformed(evt);
                        }
                    });
                    serverCmdLinePanel.add(serverJarField, "growx, split 2");

                    bukkitCheckBox.setText(resourceMap.getString("bukkitCheckBox.text"));
                    bukkitCheckBox.setName("bukkitCheckBox");
                    bukkitCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            bukkitCheckBoxActionPerformed(evt);
                        }
                    });
                    serverCmdLinePanel.add(bukkitCheckBox, "growx 0");

                    serverJarBrowseButton.setText(resourceMap.getString("serverJarBrowseButton.text"));
                    serverJarBrowseButton.setToolTipText(resourceMap.getString("serverJarBrowseButton.toolTipText"));
                    serverJarBrowseButton.putClientProperty("JComponent.sizeVariant", "mini");
                    SwingUtilities.updateComponentTreeUI(serverJarBrowseButton);
                    serverJarBrowseButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
                    serverJarBrowseButton.setName("serverJarBrowseButton");
                    serverJarBrowseButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            serverJarBrowseButtonActionPerformed(evt);
                        }
                    });
                    serverCmdLinePanel.add(serverJarBrowseButton, "growx 0, wrap");

                    xmxMemoryLabel.setText(resourceMap.getString("xmxMemoryLabel.text"));
                    xmxMemoryLabel.setName("xmxMemoryLabel");
                    serverCmdLinePanel.add(xmxMemoryLabel, "span 3, split 5");

                    xmxMemoryField.setText(resourceMap.getString("xmxMemoryField.text"));
                    xmxMemoryField.setToolTipText(resourceMap.getString("xmxMemoryField.toolTipText"));
                    xmxMemoryField.setName("xmxMemoryField");
                    xmxMemoryField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(xmxMemoryField);
                    xmxMemoryField.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            xmxMemoryFieldActionPerformed(evt);
                        }
                    });
                    serverCmdLinePanel.add(xmxMemoryField, "growx 10");

                    xincgcCheckBox.setText(resourceMap.getString("xincgcCheckBox.text"));
                    xincgcCheckBox.setToolTipText(resourceMap.getString("xincgcCheckBox.toolTipText"));
                    xincgcCheckBox.setName("xincgcCheckBox");
                    xincgcCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            xincgcCheckBoxActionPerformed(evt);
                        }
                    });
                    serverCmdLinePanel.add(xincgcCheckBox, "growx 0");

                    extraArgsLabel.setText(resourceMap.getString("extraArgsLabel.text"));
                    extraArgsLabel.setName("extraArgsLabel");
                    serverCmdLinePanel.add(extraArgsLabel, "gapleft 20, growx 0");

                    extraArgsField.setText(resourceMap.getString("extraArgsField.text"));
                    extraArgsField.setToolTipText(resourceMap.getString("extraArgsField.toolTipText"));
                    extraArgsField.setName("extraArgsField");
                    extraArgsField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(extraArgsField);
                    extraArgsField.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            extraArgsFieldActionPerformed(evt);
                        }
                    });
                    serverCmdLinePanel.add(extraArgsField, "growx, wrap");

                    customCommandLineLabel.setText(resourceMap.getString("customCommandLineLabel.text"));
                    customCommandLineLabel.setName("customCommandLineLabel");
                    serverCmdLinePanel.add(customCommandLineLabel, "growx 0, span 3, split 3");

                    cmdLineField.setEditable(false);
                    cmdLineField.setText(resourceMap.getString("cmdLineField.text"));
                    cmdLineField.setToolTipText(resourceMap.getString("cmdLineField.toolTipText"));
                    cmdLineField.setName("cmdLineField");
                    cmdLineField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(cmdLineField);
                    cmdLineField.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            cmdLineFieldActionPerformed(evt);
                        }
                    });
                    serverCmdLinePanel.add(cmdLineField, "growx");

                    customLaunchCheckBox.setText(resourceMap.getString("customLaunchCheckBox.text"));
                    customLaunchCheckBox.setToolTipText(resourceMap.getString("customLaunchCheckBox.toolTipText"));
                    customLaunchCheckBox.setName("customLaunchCheckBox");
                    customLaunchCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            customLaunchCheckBoxActionPerformed(evt);
                        }
                    });
                    serverCmdLinePanel.add(customLaunchCheckBox, "growx 0");
                }

                serverPropertiesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("serverPropertiesPanel.border.title"))); // NOI18N
                serverPropertiesPanel.setName("serverPropertiesPanel");
                serverPropertiesPanel.setLayout(new MigLayout("fill", "0[]0", "0[]0[]0"));
                configurationTab.add(serverPropertiesPanel, "grow, wrap");
                {
                    allowFlightCheckBox.setText(resourceMap.getString("allowFlightCheckBox.text"));
                    allowFlightCheckBox.setName("allowFlightCheckBox");
                    serverPropertiesPanel.add(allowFlightCheckBox, "growx, split 5");

                    allowNetherCheckBox.setSelected(true);
                    allowNetherCheckBox.setText(resourceMap.getString("allowNetherCheckBox.text"));
                    allowNetherCheckBox.setName("allowNetherCheckBox");
                    serverPropertiesPanel.add(allowNetherCheckBox, "growx");

                    onlineModeCheckBox.setSelected(true);
                    onlineModeCheckBox.setText(resourceMap.getString("onlineModeCheckBox.text"));
                    onlineModeCheckBox.setName("onlineModeCheckBox");
                    serverPropertiesPanel.add(onlineModeCheckBox, "growx");

                    pvpCheckBox.setSelected(true);
                    pvpCheckBox.setText(resourceMap.getString("pvpCheckBox.text"));
                    pvpCheckBox.setName("pvpCheckBox");
                    serverPropertiesPanel.add(pvpCheckBox, "growx");

                    whiteListCheckBox.setText(resourceMap.getString("whiteListCheckBox.text"));
                    whiteListCheckBox.setName("whiteListCheckBox");
                    serverPropertiesPanel.add(whiteListCheckBox, "growx, wrap");

                    spawnProtectionLabel.setText(resourceMap.getString("spawnProtectionLabel.text"));
                    spawnProtectionLabel.setName("spawnProtectionLabel");
                    serverPropertiesPanel.add(spawnProtectionLabel, "growx 0, split 6");

                    spawnProtectionField.setText(resourceMap.getString("spawnProtectionField.text"));
                    spawnProtectionField.setName("spawnProtectionField");
                    spawnProtectionField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(spawnProtectionField);
                    serverPropertiesPanel.add(spawnProtectionField, "growx");

                    viewDistanceLabel.setText(resourceMap.getString("viewDistanceLabel.text"));
                    viewDistanceLabel.setName("viewDistanceLabel");
                    serverPropertiesPanel.add(viewDistanceLabel, "growx 0");

                    viewDistanceSpinner.setModel(new javax.swing.SpinnerNumberModel(10, 3, 15, 1));
                    viewDistanceSpinner.setName("viewDistanceSpinner");
                    viewDistanceSpinner.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(viewDistanceSpinner);
                    serverPropertiesPanel.add(viewDistanceSpinner, "growx");

                    maxPlayersLabel.setText(resourceMap.getString("maxPlayersLabel.text"));
                    maxPlayersLabel.setName("maxPlayersLabel");
                    serverPropertiesPanel.add(maxPlayersLabel, "growx 0");

                    maxPlayersSpinner.setModel(new javax.swing.SpinnerNumberModel(20, 0, 100, 1));
                    maxPlayersSpinner.setName("maxPlayersSpinner");
                    maxPlayersSpinner.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(maxPlayersSpinner);
                    serverPropertiesPanel.add(maxPlayersSpinner, "growx 10, wrap");

                    serverIpLabel.setText(resourceMap.getString("serverIpLabel.text"));
                    serverIpLabel.setName("serverIpLabel");
                    serverPropertiesPanel.add(serverIpLabel, "growx 0, split 6");

                    serverIpField.setText(resourceMap.getString("serverIpField.text"));
                    serverIpField.setName("serverIpField");
                    serverIpField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(serverIpField);
                    serverPropertiesPanel.add(serverIpField, "growx 100");

                    serverPortLabel.setLabelFor(serverPortField);
                    serverPortLabel.setText(resourceMap.getString("serverPortLabel.text"));
                    serverPortLabel.setName("serverPortLabel");
                    serverPropertiesPanel.add(serverPortLabel, "growx 0");

                    serverPortField.setText(resourceMap.getString("serverPortField.text"));
                    serverPortField.setToolTipText(resourceMap.getString("serverPortField.toolTipText"));
                    serverPortField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(serverPortField);
                    serverPortField.setInputVerifier(new RegexVerifier("^(6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)$"));
                    serverPortField.setName("serverPortField");
                    serverPropertiesPanel.add(serverPortField, "growx 20");

                    spawnAnimalsCheckBox.setSelected(true);
                    spawnAnimalsCheckBox.setText(resourceMap.getString("spawnAnimalsCheckBox.text"));
                    spawnAnimalsCheckBox.setName("spawnAnimalsCheckBox");
                    serverPropertiesPanel.add(spawnAnimalsCheckBox);

                    spawnMonstersCheckBox.setSelected(true);
                    spawnMonstersCheckBox.setText(resourceMap.getString("spawnMonstersCheckBox.text"));
                    spawnMonstersCheckBox.setName("spawnMonstersCheckBox");
                    serverPropertiesPanel.add(spawnMonstersCheckBox, "wrap");

                    levelNameLabel.setText(resourceMap.getString("levelNameLabel.text"));
                    levelNameLabel.setName("levelNameLabel");
                    serverPropertiesPanel.add(levelNameLabel, "growx 0, split 4");

                    levelNameField.setText(resourceMap.getString("levelNameField.text"));
                    levelNameField.setName("levelNameField");
                    levelNameField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(levelNameField);
                    serverPropertiesPanel.add(levelNameField, "growx 40");

                    levelSeedLabel.setText(resourceMap.getString("levelSeedLabel.text"));
                    levelSeedLabel.setName("levelSeedLabel");
                    serverPropertiesPanel.add(levelSeedLabel, "growx 0");

                    levelSeedField.setText(resourceMap.getString("levelSeedField.text"));
                    levelSeedField.setName("levelSeedField");
                    levelSeedField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(levelSeedField);
                    serverPropertiesPanel.add(levelSeedField, "growx 100");
                }

                guiConfigPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("guiConfigPanel.border.title")));
                guiConfigPanel.setName("guiConfigPanel");
                guiConfigPanel.setLayout(new MigLayout("fill", "0[]0", "0[]0"));
                configurationTab.add(guiConfigPanel, "grow, wrap");
                {
                    useProxyCheckBox.setText(resourceMap.getString("useProxyCheckBox.text"));
                    useProxyCheckBox.setToolTipText(resourceMap.getString("useProxyCheckBox.toolTipText"));
                    useProxyCheckBox.setName("useProxyCheckBox");
                    useProxyCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            useProxyCheckBoxActionPerformed(evt);
                        }
                    });
                    guiConfigPanel.add(useProxyCheckBox, "growx 0, split 5");

                    extPortLabel.setLabelFor(extPortField);
                    extPortLabel.setText(resourceMap.getString("extPortLabel.text"));
                    extPortLabel.setName("extPortLabel");
                    guiConfigPanel.add(extPortLabel, "gapleft 20, growx 0");

                    extPortField.setText(resourceMap.getString("extPortField.text"));
                    extPortField.setToolTipText(resourceMap.getString("extPortField.toolTipText"));
                    extPortField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(extPortField);
                    extPortField.setInputVerifier(new RegexVerifier("^(6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)$"));
                    extPortField.setName("extPortField");
                    guiConfigPanel.add(extPortField, "growx 100");

                    intPortLabel.setLabelFor(intPortField);
                    intPortLabel.setText(resourceMap.getString("intPortLabel.text"));
                    intPortLabel.setName("intPortLabel");
                    guiConfigPanel.add(intPortLabel, "growx 0");

                    intPortField.setText(resourceMap.getString("intPortField.text"));
                    intPortField.setToolTipText(resourceMap.getString("intPortField.toolTipText"));
                    intPortField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(intPortField);
                    intPortField.setInputVerifier(new RegexVerifier("^(6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)$"));
                    intPortField.setName("intPortField");
                    guiConfigPanel.add(intPortField, "growx 100, wrap");

                    windowTitleLabel.setText(resourceMap.getString("windowTitleLabel.text"));
                    windowTitleLabel.setName("windowTitleLabel");
                    guiConfigPanel.add(windowTitleLabel, "growx 0, split 4");

                    windowTitleField.setText(resourceMap.getString("windowTitleField.text"));
                    windowTitleField.setName("windowTitleField");
                    windowTitleField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(windowTitleField);
                    windowTitleField.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            windowTitleFieldActionPerformed(evt);
                        }
                    });
                    guiConfigPanel.add(windowTitleField, "growx");

                    inputHistoryMaxSizeLabel.setText(resourceMap.getString("inputHistoryMaxSizeLabel.text"));
                    inputHistoryMaxSizeLabel.setName("inputHistoryMaxSizeLabel");
                    guiConfigPanel.add(inputHistoryMaxSizeLabel, "growx 0");

                    inputHistoryMaxSizeField.setText(resourceMap.getString("inputHistoryMaxSizeField.text"));
                    inputHistoryMaxSizeField.setToolTipText(resourceMap.getString("inputHistoryMaxSizeField.toolTipText"));
                    inputHistoryMaxSizeField.setInputVerifier(new RegexVerifier("\\d{1,4}"));
                    inputHistoryMaxSizeField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(inputHistoryMaxSizeField);
                    inputHistoryMaxSizeField.setName("inputHistoryMaxSizeField");
                    guiConfigPanel.add(inputHistoryMaxSizeField, "growx, wrap");

                    startServerOnLaunchCheckBox.setText(resourceMap.getString("startServerOnLaunchCheckBox.text"));
                    startServerOnLaunchCheckBox.setName("startServerOnLaunchCheckBox");
                    startServerOnLaunchCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            startServerOnLaunchCheckBoxActionPerformed(evt);
                        }
                    });
                    guiConfigPanel.add(startServerOnLaunchCheckBox, "growx 0, split 3");

                    commandPrefixLabel.setText(resourceMap.getString("commandPrefixLabel.text"));
                    commandPrefixLabel.setName("commandPrefixLabel");
                    guiConfigPanel.add(commandPrefixLabel, "gapleft 20, growx 0");

                    commandPrefixField.setText(resourceMap.getString("commandPrefixField.text"));
                    commandPrefixField.setToolTipText(resourceMap.getString("commandPrefixField.toolTipText"));
                    commandPrefixField.setName("commandPrefixField");
                    commandPrefixField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(commandPrefixField);
                    commandPrefixField.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            commandPrefixFieldActionPerformed(evt);
                        }
                    });
                    guiConfigPanel.add(commandPrefixField, "growx");
                }

                saveServerConfigButton.setText(resourceMap.getString("saveServerConfigButton.text"));
                saveServerConfigButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
                saveServerConfigButton.setName("saveServerConfigButton");
                saveServerConfigButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        saveServerConfigButtonActionPerformed(evt);
                    }
                });
                configurationTab.add(saveServerConfigButton, "growx");
            }

            backupTab.setName("backupTab");
            backupTab.setLayout(new MigLayout("fill", "0[]0", "0[]0"));
            tabber.addTab(resourceMap.getString("backupTab.TabConstraints.tabTitle"), backupTab);
            {
                //@TODO
                
            }

            schedulerTab.setName("schedulerTab");
            schedulerTab.setLayout(new MigLayout("fill", "5[]10[]30[]50[]5", "0[]0[min!]0[min!]0[min!]0[min!]0"));
            tabber.addTab(resourceMap.getString("schedulerTab.TabConstraints.tabTitle"), schedulerTab);
            {
                taskSchedulerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("taskSchedulerPanel.border.title"))); // NOI18N
                taskSchedulerPanel.setName("taskSchedulerPanel");
                taskSchedulerPanel.setLayout(new MigLayout("fill", "0[]0", "0[]0"));
                schedulerTab.add(taskSchedulerPanel, "grow, wrap, span 4");
                {
                    taskSchedulerScrollPane.setName("taskSchedulerScrollPane");
                    taskSchedulerScrollPane.setViewportView(taskSchedulerList);
                    taskSchedulerPanel.add(taskSchedulerScrollPane, "grow");
                    {
                        taskSchedulerList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                        taskSchedulerList.setName("taskSchedulerList");
                        taskSchedulerList.addKeyListener(new java.awt.event.KeyAdapter() {
                            public void keyTyped(java.awt.event.KeyEvent evt) {
                                taskSchedulerListKeyTyped(evt);
                            }
                        });
                    }
                }

                taskListAddButton.setText(resourceMap.getString("taskListAddButton.text")); // NOI18N
                taskListAddButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
                taskListAddButton.setName("taskListAddButton"); // NOI18N
                taskListAddButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        taskListAddButtonActionPerformed(evt);
                    }
                });
                schedulerTab.add(taskListAddButton, "growx");

                taskListEditButton.setText(resourceMap.getString("taskListEditButton.text")); // NOI18N
                taskListEditButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
                taskListEditButton.setName("taskListEditButton"); // NOI18N
                taskListEditButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        taskListEditButtonActionPerformed(evt);
                    }
                });
                schedulerTab.add(taskListEditButton, "growx");

                taskListRemoveButton.setText(resourceMap.getString("taskListRemoveButton.text"));
                taskListRemoveButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
                taskListRemoveButton.setName("taskListRemoveButton");
                taskListRemoveButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        taskListRemoveButtonActionPerformed(evt);
                    }
                });
                schedulerTab.add(taskListRemoveButton, "growx");

                pauseSchedulerButton.setText(resourceMap.getString("pauseSchedulerButton.text"));
                pauseSchedulerButton.setToolTipText(resourceMap.getString("pauseSchedulerButton.toolTipText"));
                pauseSchedulerButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
                pauseSchedulerButton.setName("pauseSchedulerButton");
                pauseSchedulerButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        pauseSchedulerButtonActionPerformed(evt);
                    }
                });
                schedulerTab.add(pauseSchedulerButton, "growx");
            }

            webInterfaceTab.setName("webInterfaceTab");
            webInterfaceTab.setLayout(new MigLayout("fill", "0[]0", "0[min!]0[]0"));
            tabber.addTab(resourceMap.getString("webInterfaceTab.TabConstraints.tabTitle"), webInterfaceTab);
            {
                //@TODO
                webInterfaceConfigPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("webInterfaceConfigPanel.border.title")));
                webInterfaceConfigPanel.setName("webInterfaceConfigPanel");
                webInterfaceConfigPanel.setLayout(new MigLayout("fill", "0[]0", "0[]0[]0"));
                webInterfaceTab.add(webInterfaceConfigPanel, "grow, wrap");
                {
                    useWebInterfaceCheckBox.setText(resourceMap.getString("useWebInterfaceCheckBox.text"));
                    useWebInterfaceCheckBox.setToolTipText(resourceMap.getString("useWebInterfaceCheckBox.toolTipText"));
                    useWebInterfaceCheckBox.setName("useWebInterfaceCheckBox");
                    useWebInterfaceCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            useWebInterfaceCheckBoxActionPerformed(evt);
                        }
                    });
                    webInterfaceConfigPanel.add(useWebInterfaceCheckBox, "growx 0, split");

                    webPortLabel.setText(resourceMap.getString("webPortLabel.text"));
                    webPortLabel.setName("webPortLabel");
                    webInterfaceConfigPanel.add(webPortLabel, "gapleft 20, growx 0");

                    webPortField.setText(resourceMap.getString("webPortField.text"));
                    webPortField.setToolTipText(resourceMap.getString("webPortField.toolTipText"));
                    webPortField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(webPortField);
                    webPortField.setInputVerifier(new RegexVerifier("^(6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)$"));
                    webPortField.setName("webPortField");
                    webPortField.addFocusListener(new java.awt.event.FocusAdapter() {
                        public void focusLost(java.awt.event.FocusEvent evt) {
                            webPortFieldFocusLost(evt);
                        }
                    });
                    webInterfaceConfigPanel.add(webPortField, "growx 20");

                    webPasswordLabel.setText(resourceMap.getString("webPasswordLabel.text"));
                    webPasswordLabel.setName("webPasswordLabel");
                    webInterfaceConfigPanel.add(webPasswordLabel, "growx 0");

                    webPasswordField.setText(resourceMap.getString("webPasswordField.text"));
                    webPasswordField.setName("webPasswordField");
                    webPasswordField.putClientProperty("JComponent.sizeVariant", "small");
                    SwingUtilities.updateComponentTreeUI(webPasswordField);
                    webPasswordField.addFocusListener(new java.awt.event.FocusAdapter() {
                        public void focusLost(java.awt.event.FocusEvent evt) {
                            webPasswordFieldFocusLost(evt);
                        }
                    });
                    webInterfaceConfigPanel.add(webPasswordField, "growx");

                    showWebPasswordButton.setText(resourceMap.getString("showWebPasswordButton.text"));
                    showWebPasswordButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
                    showWebPasswordButton.setName("showWebPasswordButton");
                    showWebPasswordButton.putClientProperty("JComponent.sizeVariant", "mini");
                    SwingUtilities.updateComponentTreeUI(showWebPasswordButton);
                    showWebPasswordButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            showWebPasswordButtonActionPerformed(evt);
                        }
                    });
                    webInterfaceConfigPanel.add(showWebPasswordButton, "growx 0, wrap");

                    disableGetOutputNotificationsCheckBox.setText(resourceMap.getString("disableGetOutputNotificationsCheckBox.text"));
                    disableGetOutputNotificationsCheckBox.setToolTipText(resourceMap.getString("disableGetOutputNotificationsCheckBox.toolTipText"));
                    disableGetOutputNotificationsCheckBox.setName("disableGetOutputNotificationsCheckBox");
                    disableGetOutputNotificationsCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            disableGetOutputNotificationsCheckBoxActionPerformed(evt);
                        }
                    });
                    webInterfaceConfigPanel.add(disableGetOutputNotificationsCheckBox);
                }

                webLogPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("webLogPanel.border.title")));
                webLogPanel.setName("webLogPanel");
                webLogPanel.setLayout(new MigLayout("fill", "0[]0", "0[]0"));
                webInterfaceTab.add(webLogPanel, "grow");
                {
                    webLogScrollPane.setName("webLogScrollPane");
                    webLogScrollPane.setViewportView(webLog);
                    webLogPanel.add(webLogScrollPane, "grow");
                    webLog.setName("webLog");
                }
            }

            themeTab.setName("themeTab");
            themeTab.setLayout(new MigLayout("fill", "0[]0", "0[]0"));
            tabber.addTab(resourceMap.getString("themeTab.TabConstraints.tabTitle"), themeTab);
            {
                //@TODO
                colorizationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("colorizationPanel.border.title")));
                colorizationPanel.setName("colorizationPanel");

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
            }
        }


        saveGuiConfigButton.setText(resourceMap.getString("saveGuiConfigButton.text")); // NOI18N
        saveGuiConfigButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        saveGuiConfigButton.setName("saveGuiConfigButton"); // NOI18N
        saveGuiConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveGuiConfigButtonActionPerformed(evt);
            }
        });




        backupButton.setText(resourceMap.getString("backupButton.text"));
        backupButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        backupButton.setName("backupButton");
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
        backupFileChooser.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                backupFileChooserMouseClicked(evt);
            }
        });
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
    }

    /**
     * Fixes the oddities of the component placing that netbeans does.
     */
    public final void fixComponents() {
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

         public java.awt.Component getListCellRendererComponent(
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
         public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                 public void run() {
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
         public void actionPerformed(ActionEvent e) {
            sendInput(true);
        }
    };

    private void startstopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startstopButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
                config.setWindowTitle(windowTitleField.getText());
                getFrame().setTitle(windowTitleField.getText());
                trayIcon.setToolTip(windowTitleField.getText());
            }
        });
    }//GEN-LAST:event_windowTitleFieldActionPerformed

    private void javaExecBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_javaExecBrowseButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
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
             public void run() {
                config.cmdLine.setBukkit(bukkitCheckBox.isSelected());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
    }//GEN-LAST:event_bukkitCheckBoxActionPerformed

    private void xmxMemoryFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xmxMemoryFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                config.cmdLine.setXmx(xmxMemoryField.getText());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
    }//GEN-LAST:event_xmxMemoryFieldActionPerformed

    private void xincgcCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xincgcCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                config.cmdLine.setXincgc(xincgcCheckBox.isSelected());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
        
    }//GEN-LAST:event_xincgcCheckBoxActionPerformed

    private void extraArgsFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_extraArgsFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                config.cmdLine.setExtraArgs(extraArgsField.getText());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
    }//GEN-LAST:event_extraArgsFieldActionPerformed

    private void serverJarFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverJarFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                config.cmdLine.setServerJar(serverJarField.getText());
                cmdLineField.setText(config.cmdLine.parseCmdLine());
            }
        });
    }//GEN-LAST:event_serverJarFieldActionPerformed

    private void javaExecFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_javaExecFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
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
             public void run() {
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
             public void run() {
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
             public void run() {
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
             public void run() {
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
             public void run() {
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
             public void run() {
                config.backups.setPath(backupPathField.getText());
                backup();
            }
        });
        
    }//GEN-LAST:event_backupButtonActionPerformed

    private void tabberStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabberStateChanged
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
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
             public void run() {
                config.backups.setPath(backupPathField.getText());
            }
        });
    }//GEN-LAST:event_backupPathFieldActionPerformed

    private void zipBackupCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zipBackupCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                config.backups.setZip(zipBackupCheckBox.isSelected());
            }
        });
    }//GEN-LAST:event_zipBackupCheckBoxActionPerformed

    private void taskListAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskListAddButtonActionPerformed
        addTaskListEntry();
    }//GEN-LAST:event_taskListAddButtonActionPerformed

    private void taskListEditButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskListEditButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                editTaskListEntry((EventModel)taskSchedulerList.getSelectedValue());
            }
        });
    }//GEN-LAST:event_taskListEditButtonActionPerformed

    private void taskListRemoveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskListRemoveButtonActionPerformed
        removeTaskListEntry();
    }//GEN-LAST:event_taskListRemoveButtonActionPerformed

    public void removeTaskListEntry() {
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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

    public void removeTaskByName(String name) {
        final String taskname = name;
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                EventModel event = getTaskByName(taskname);
                if (event == null) return;
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
        });
    }

    private void textSizeFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_textSizeFieldPropertyChange
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                config.display.setTextSize(Integer.parseInt(textSizeField.getValue().toString()));
            }
        });
    }//GEN-LAST:event_textSizeFieldPropertyChange

    private void textColorBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_textColorBoxMouseClicked
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
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
             public void run() {
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
             public void run() {
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
             public void run() {
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
             public void run() {
                String rgb = Integer.toHexString(textColorBox.getBackground().getRGB());
                rgb = rgb.substring(2, rgb.length());
                config.display.setTextColor(rgb);
            }
        });
    }//GEN-LAST:event_textColorBoxFocusLost

    private void bgColorBoxFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_bgColorBoxFocusLost
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                String rgb = Integer.toHexString(bgColorBox.getBackground().getRGB());
                rgb = rgb.substring(2, rgb.length());
                config.display.setBgColor(rgb);
                updateConsoleOutputBgColor();
            }
        });
    }//GEN-LAST:event_bgColorBoxFocusLost

    private void infoColorBoxFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_infoColorBoxFocusLost
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                String rgb = Integer.toHexString(infoColorBox.getBackground().getRGB());
                rgb = rgb.substring(2, rgb.length());
                config.display.setInfoColor(rgb);
            }
        });
    }//GEN-LAST:event_infoColorBoxFocusLost

    private void warningColorBoxFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_warningColorBoxFocusLost
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                String rgb = Integer.toHexString(warningColorBox.getBackground().getRGB());
                rgb = rgb.substring(2, rgb.length());
                config.display.setWarningColor(rgb);
            }
        });
    }//GEN-LAST:event_warningColorBoxFocusLost

    private void severeColorBoxFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_severeColorBoxFocusLost
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                String rgb = Integer.toHexString(severeColorBox.getBackground().getRGB());
                rgb = rgb.substring(2, rgb.length());
                config.display.setSevereColor(rgb);
            }
        });
    }//GEN-LAST:event_severeColorBoxFocusLost

    private void playerListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_playerListMouseClicked
        final java.awt.event.MouseEvent event = evt;
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                if (event.getButton() == event.BUTTON3 && (playerList.getSelectedIndex() > -1)) {
                    javax.swing.JPopupMenu playerListContextMenu = new javax.swing.JPopupMenu();
                    javax.swing.JMenuItem kickMenuItem;
                    kickMenuItem = new javax.swing.JMenuItem("Kick");
                    kickMenuItem.addActionListener(
                            new ActionListener() {
                         public void actionPerformed(ActionEvent ev) {
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
                         public void actionPerformed(ActionEvent ev) {
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
                         public void actionPerformed(ActionEvent ev) {
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
             public void run() {
                config.backups.setClearLog(clearLogCheckBox.isSelected());
            }
        });
    }//GEN-LAST:event_clearLogCheckBoxActionPerformed

    private void startServerOnLaunchCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startServerOnLaunchCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                config.setServerStartOnStartup(startServerOnLaunchCheckBox.isSelected());
            }
        });
    }//GEN-LAST:event_startServerOnLaunchCheckBoxActionPerformed

    private void useWebInterfaceCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useWebInterfaceCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
                config.web.setPort(Integer.valueOf(webPortField.getText()));
            }
        });
    }//GEN-LAST:event_webPortFieldFocusLost

    private void showWebPasswordButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showWebPasswordButtonActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
                config.web.setPassword(String.valueOf(webPasswordField.getPassword()));
            }
        });
    }//GEN-LAST:event_webPasswordFieldFocusLost

    private void disableGetOutputNotificationsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disableGetOutputNotificationsCheckBoxActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                config.web.setDisableGetRequests(disableGetOutputNotificationsCheckBox.isSelected());
            }
        });
    }//GEN-LAST:event_disableGetOutputNotificationsCheckBoxActionPerformed

    private void consoleOutputMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_consoleOutputMouseClicked
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                if (server.isRunning()) {
                    textScrolling = false;
                }
            }
        });
    }

    private void customButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        customButtonAction(customCombo1);
    }

    private void customButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        customButtonAction(customCombo2);
    }

    private void useProxyCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                config.setProxy(useProxyCheckBox.isSelected());
                serverIpField.setEnabled(!useProxyCheckBox.isSelected());
                if (useProxyCheckBox.isSelected()) {
                    playerList.setToolTipText("This shows a list of players connected to the server.  Right click a to pull up the player action menu.");
                } else {
                    playerList.setToolTipText("Player list is currently only supported when using the GUI's Proxy feature.");
                }
            }
        });
    }

    private void hideMenuMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hideMenuMouseClicked
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                GUI.this.getApplication().hide(GUI.this);
                isHidden = true;
            }
        }); 
    }//GEN-LAST:event_hideMenuMouseClicked

    private void textSizeFieldStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_textSizeFieldStateChanged
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                config.display.setTextSize(Integer.parseInt(textSizeField.getValue().toString()));
            }
        });
    }//GEN-LAST:event_textSizeFieldStateChanged

    private void commandPrefixFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandPrefixFieldActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
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
        pauseSchedule();
    }//GEN-LAST:event_pauseSchedulerButtonActionPerformed

    public boolean pauseSchedule() {
        class SchedulePauser implements Runnable {
            public SchedulePauser() {
                super();
            }

             public void run() {
                schedulePaused = !schedulePaused;
                if (schedulePaused) {
                    pauseSchedulerButton.setSelected(true);
                    try {
                        scheduler.pauseAll();
                        pauseSchedulerButton.setFont(new java.awt.Font("Tahoma",
                                java.awt.Font.BOLD, 11));
                    } catch (SchedulerException se) {
                        schedulePaused = !schedulePaused;
                        pauseSchedulerButton.setSelected(false);
                    }
                } else {
                    pauseSchedulerButton.setSelected(false);
                    try {
                        scheduler.resumeAll();
                        pauseSchedulerButton.setFont(new java.awt.Font("Tahoma",
                                java.awt.Font.PLAIN, 11));
                    } catch (SchedulerException se) {
                        schedulePaused = !schedulePaused;
                        pauseSchedulerButton.setSelected(true);
                    }
                }
            }

            public boolean getPaused() {
                return schedulePaused;
            }
        }

        try {
            SchedulePauser pause = new SchedulePauser();
            SwingUtilities.invokeAndWait(pause);
            return pause.getPaused();
        } catch (InterruptedException ie) {
            return schedulePaused;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            return schedulePaused;
        }
    }

    private void taskSchedulerListKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_taskSchedulerListKeyTyped
        final java.awt.event.KeyEvent event = evt;
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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

    private void backupFileChooserMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_backupFileChooserMouseClicked

    }

    private void consoleOutputMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_consoleOutputMouseEntered
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                mouseInConsoleOutput = true;
            }
        });
    }//GEN-LAST:event_consoleOutputMouseEntered

    private void customButtonAction(javax.swing.JComboBox box) {
        final javax.swing.JComboBox boxxy = box;
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
    public javax.swing.JTextField intPortField;
    public javax.swing.JLabel intPortLabel;
    public javax.swing.JTextField extraArgsField;
    public javax.swing.JLabel extraArgsLabel;
    public javax.swing.JPanel themeTab;
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
    public javax.swing.JLabel customCommandLineLabel;
    public javax.swing.JMenuItem jMenuItem1;
    public javax.swing.JPanel colorizationPanel;
    public javax.swing.JPanel webInterfaceConfigPanel;
    public javax.swing.JPanel guiConfigPanel;
    public javax.swing.JScrollPane consoleOutScrollPane;
    public javax.swing.JScrollPane playerListScrollPane;
    public javax.swing.JScrollPane jScrollPane3;
    public javax.swing.JScrollPane jScrollPane4;
    public javax.swing.JScrollPane taskSchedulerScrollPane;
    public javax.swing.JScrollPane webLogScrollPane;
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
    public javax.swing.JPanel configurationTab;
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
             public void run() {
                versionNotifier.setForeground(Color.red);
                versionNotifier.setText("New version " + ver + " is available!");
            }
        });
    }

    public void setPlayerList(PlayerList playerListModel) {
        final PlayerList pl = playerListModel;
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
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
                         public void actionPerformed(ActionEvent e) {
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
             public void run() {
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
             public void run() {
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
             public void run() {
                if (!controlState.equals("BACKUP")) {
                    backupFileChooser.updateUI();
                }
            }
        });
    }

    public void backup() {
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
     * @param b determines if "say " should be prepended to the text.
     */
    public void sendInput(boolean b) {
        final boolean shouldSay = b;
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
     * @param event KeyEvent to be passed in.
     */
    public void giveInputFocus(java.awt.event.KeyEvent event) {
        final java.awt.event.KeyEvent evt = event;
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
            
             public void run() {
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
             public void run() {
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
        //backupFileChooser.setCheckingModel(new GUITreeCheckingModel(backupFileSystem, GUI.this));
        backupFileChooser.getCheckingModel().setCheckingMode(TreeCheckingModel.CheckingMode.SIMPLE);
        //backupFileChooser.setCheckingPaths(createTreePathArray(config.backups.getPathsToBackup()));
        //javax.swing.tree.TreePath[] paths = createTreePathArray(config.backups.getPathsToBackup());
        //for (int i = 0; i < paths.length; i++) {
        //backupFileChooser.addCheckingPath(paths[i]);
        /*
            Thread initBackupPaths = new Thread() {
                
                public void run() {
                    backupFileChooser.setCheckingPaths(createTreePathArray(config.backups.getPathsToBackup()));
                }
            };
            initBackupPaths.start();

         * 
         */
        //}
        backupFileChooser.setCheckingPaths(createTreePathArray(config.backups.getPathsToBackup()));
    }

    public void updateGuiWithServerProperties() {
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
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
                serverIpField.setEnabled(!useProxyCheckBox.isSelected());
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
        //for (int i = 0; i < backupFileChooser.getCheckingRoots().length; i++) {
        //    config.backups.getPathsToBackup().add(backupFileChooser.getCheckingRoots()[i].getLastPathComponent().toString());
        //}
        for (int i = 0; i < backupFileChooser.getCheckingPaths().length; i++) {
            config.backups.getPathsToBackup().add(backupFileChooser.getCheckingPaths()[i].getLastPathComponent().toString());
        }
        //config.backups.setPathsToBackup(pathsToBackup);
    }

    public void saveConfigAction() {
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
        config.backups.setPath(backupPathField.getText());
        saveBackupPathsToConfig();
        config.setProxy(useProxyCheckBox.isSelected());
        config.setExtPort(Integer.valueOf(extPortField.getText()));

        config.save();
        saveServerProperties();
    }
    /**
     * Saves the config file with any changes made by the user through the gui.
     */
    public void saveConfig() {
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                saveConfigAction();
            }
        });
    }

    public void saveServerProperties() {
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
             public void run() {
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
    }

    /**
     * Starts the Minecraft server and verifies that it started properly.
     */
    public void startServer() {
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                if (controlState.equals("OFF")) {
                    setConsoleOutput("");
                    TaskMonitor taskMonitor = getApplication().getContext().getTaskMonitor();
                    if ((taskMonitor.getForegroundTask() != null) && taskMonitor.getForegroundTask().isStarted()) {
                        guiLog("Stopping backup first.");
                        taskMonitor.getForegroundTask().cancel(true);
                    }
                    server.setCmdLine(config.cmdLine.getCmdLine());
                    String start = server.start();
                    if (start.equals("SUCCESS")) {
                    } else if (start.equals("ERROR")) {
                        
                    } else if (start.equals("INVALIDJAR")) {
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
             public void run() {
                try {
                    Thread.sleep(dly * 1000);
                } catch (InterruptedException ie) { }
                startServer();
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
             public void run() {
                TaskMonitor taskMonitor = getApplication().getContext().getTaskMonitor();
                if ((taskMonitor.getForegroundTask() != null) && taskMonitor.getForegroundTask().isStarted()) {
                    guiLog("Stopping backup first.");
                    taskMonitor.getForegroundTask().cancel(true);
                }
                if (server.isRunning()) {
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
             public void run() {
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
             public void run() {
                try
                {
                    String textToAdd = parser.parseText(text);

                    ((HTMLEditorKit)webLog.getEditorKit())
                            .insertHTML((HTMLDocument)webLog.getDocument(),
                            webLog.getDocument().getEndPosition().getOffset()-1,
                            textToAdd, 1, 0, null);
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
             public void run() {
                consoleOutput.setText("<body bgcolor = " + config.display.getBgColor()
                        + "><font color = \"" + config.display.getTextColor()
                        + "\" size = " + config.display.getTextSize() + ">" + text);
            }
        });
    }

    public void webLogReplace(String replaceText) {
        final String text = replaceText;
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                webLog.setText("<body bgcolor = " + config.display.getBgColor()
                        + "><font color = \"" + config.display.getTextColor()
                        + "\" size = " + config.display.getTextSize() + ">" + text);
            }
        });
    }

    public void updateConsoleOutputBgColor() {
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
     public void update(Observable o, Object arg) {
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
                 public void run() {
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
     * @param newServerState Typically the state of the server. "ON" or "OFF"
     */
    private void controlSwitcher(String newServerState) {
        final String serverState = newServerState;
        SwingUtilities.invokeLater(new Runnable() {
             public void run() {
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
                    //startstopButton.setEnabled(false);
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
                    //startstopButton.setEnabled(true);
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
    private boolean schedulePaused;
    private MainWorker mainWorker;

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

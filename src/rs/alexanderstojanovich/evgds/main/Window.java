/*
 * Copyright (C) 2024 Alexander Stojanovich <coas91@rocketmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rs.alexanderstojanovich.evgds.main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import rs.alexanderstojanovich.evgds.helper.ButtonEditor;
import rs.alexanderstojanovich.evgds.helper.ButtonRenderer;
import rs.alexanderstojanovich.evgds.level.LevelContainer;
import static rs.alexanderstojanovich.evgds.main.Game.RESOURCES_DIR;
import static rs.alexanderstojanovich.evgds.main.GameObject.MapLevelSize.HUGE;
import static rs.alexanderstojanovich.evgds.main.GameObject.MapLevelSize.LARGE;
import static rs.alexanderstojanovich.evgds.main.GameObject.MapLevelSize.MEDIUM;
import static rs.alexanderstojanovich.evgds.main.GameObject.MapLevelSize.SMALL;
import rs.alexanderstojanovich.evgds.net.ClientInfo;
import rs.alexanderstojanovich.evgds.net.PlayerInfo;
import rs.alexanderstojanovich.evgds.net.PosInfo;
import rs.alexanderstojanovich.evgds.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich
 */
public class Window extends javax.swing.JFrame {

    public final Configuration config = Configuration.getInstance();

    /**
     * Console refresh timer 5000 milliseconds
     */
    public final Timer conRefreshTimer = new Timer((int) Math.round(1000.0 * config.getConsoleRefreshTimer()), (ActionEvent e) -> refreshConsole());

    public static final Color CON_INFO_COLOR = new Color(0, 191, 255); // Deep sky blue
    public static final Color CON_ERR_COLOR = Color.RED;
    public static final Color CON_WARN_COLOR = new Color(255, 215, 0); // Gold

    /**
     * Pass of logger
     */
    public static int logPass = 0;

    /**
     * Console chunk of lines (provides efficient rendering)
     */
    public static final int CON_CHUNK_LINES_SIZE = 1000;

    /**
     * Max line to store
     */
    public static final int MAX_LINES = 1000000; // one-million

    /**
     * Max line to store half
     */
    public static final int MAX_LINES_HALF = 500000; // half a million

    /**
     * Max line to store quarter
     */
    public static final int MAX_LINES_QUARTER = 250000; // quarter of million

    public static enum BuildType {
        PUBLIC, DEVELOPMENT
    }

    /**
     * Status of the console message (with bitmask values).
     */
    public enum Status {
        INFO(1), WARN(2), ERR(4);

        private final int mask;

        private Status(int mask) {
            this.mask = mask;
        }

        public int getMask() {
            return mask;
        }
    }

    protected int filter = 7;

    /**
     * Message log (contains string text plus status)
     */
    protected final IList<Message> messageLog = new BigList<>(); // Store all messages with their status

    /**
     * Message class to store each log entry with its status
     */
    public static class Message {

        public final String text;
        public final Status status;

        public Message(String text, Status status) {
            this.text = text;
            this.status = status;
        }

        public String getText() {
            return text;
        }

        public Status getStatus() {
            return status;
        }
    }

    public static final BuildType BUILD = BuildType.PUBLIC;

    protected double lastTime = 0.0;
    protected double currTime = 0.0;
    protected long bytesReceived = 0L;
    protected long bytesSent = 0L;

    public final SystemInfo si = new SystemInfo();
    public final HardwareAbstractionLayer hal = si.getHardware();
    public final OperatingSystem os = si.getOperatingSystem();

    public final GameObject gameObject;
    public static final Dimension DIM = Toolkit.getDefaultToolkit().getScreenSize();

    public static final String LICENSE_LOGO_FILE_NAME = "gplv3_logo.png";

    public static final String DAY_NIGHT0 = "daynight0.png";
    public static final String DAY_NIGHT1 = "daynight1.png";
    public static final String DAY_NIGHT2 = "daynight2.png";
    public static final String DAY_NIGHT3 = "daynight3.png";
    public static final String DAY_NIGHT4 = "daynight4.png";
    public static final String DAY_NIGHT5 = "daynight5.png";
    public static final String DAY_NIGHT6 = "daynight6.png";
    public static final String DAY_NIGHT7 = "daynight7.png";

    public static final String LOGOX_FILE_NAME = "app-icon.png";
    public static final String LOGO_FILE_NAME = "app-icon-small.png";

    public final IList<PlayerInfo> playerInfos = new GapList<>();
    public final IList<PosInfo> posInfos = new GapList<>();
    public final IList<ClientInfo> clientInfos = new GapList<>();

    /**
     * Init day/night icons (clock)
     */
    public static final List<Icon> dayNightIcons = initDayNight();

    public final DefaultTableModel playerInfoModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    public final DefaultTableModel posInfoModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    public final DefaultTableModel clientInfoModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == this.getColumnCount() - 1;
        }
    };

    /**
     * World file import (*.dat, *.ndat)
     */
    public final JFileChooser fileImport = new JFileChooser();

    /**
     * World file export (*.dat, *.ndat)
     */
    public final JFileChooser fileExport = new JFileChooser();

    /**
     * Save as log
     */
    public final JFileChooser logSaveAs = new JFileChooser();

    /**
     * Creates new form ServerInterface
     *
     * @param gameObject game object linking everything
     */
    public Window(GameObject gameObject) {
        this.gameObject = gameObject;
        initComponents();
        this.setIconImages(appLogos());
        this.initInfoTables();
        setEnabledComponents(this.panelWorld, false);
        setEnabledComponents(this.panelInfo, false);
        this.cmbLevelSize.setModel(new DefaultComboBoxModel(GameObject.MapLevelSize.values()));
        this.initDialogs();
        this.tboxLocalIP.setText(config.getLocalIP());
        this.spinServerPort.setValue(config.getServerPort());
        initPopUpMenu();
        initConsoleRenderer();
    }

    public void initCenterWindow() {
        this.setLocation(DIM.width / 2 - this.getSize().width / 2, DIM.height / 2 - this.getSize().height / 2);
    }

    /**
     * Write new message on the console.
     *
     * @param msg message text
     * @param status message status {INFO, ERR, WARN}
     */
    public void logMessage(String msg, Status status) {
        Message message = new Message(msg, status);
        messageLog.add(message);
    }

    /**
     * Init popup menu for terminal (JTextArea)
     */
    private void initPopUpMenu() {
        // Add the popup menu to the text area
        this.console.setComponentPopupMenu(this.logPopupMenu);

        // Add mouse listener to show the popup menu
        this.console.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopupMenu(e);
            }

            private void showPopupMenu(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    logPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    /**
     * Console renderer (JList)
     */
    private void initConsoleRenderer() {
        // Custom renderer for log messages
        this.console.setCellRenderer((JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus)
                -> {
            JLabel label = new JLabel(value); // Create a JLabel to display the text

            // Apply color based on message content
            if (value.contains("ERR")) {
                label.setForeground(CON_ERR_COLOR);
            } else if (value.contains("WARN")) {
                label.setForeground(CON_WARN_COLOR);
            } else {
                label.setForeground(CON_INFO_COLOR);
            }

            // Handle selection highlighting
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
                label.setOpaque(true);
            } else {
                label.setBackground(list.getBackground());
                label.setOpaque(false);
            }

            return label; // Return the customized component
        });
    }

    /**
     * Init frame logo icon(s).
     *
     * @return list of image(s).
     */
    private static List<Image> appLogos() {
        List<Image> result = new ArrayList<>();

        URL url_logo = Window.class.getResource(RESOURCES_DIR + LOGO_FILE_NAME);
        URL url_logox = Window.class.getResource(RESOURCES_DIR + LOGOX_FILE_NAME);
        if (url_logo != null && url_logox != null) {
            ImageIcon logo = new ImageIcon(url_logo);
            ImageIcon logox = new ImageIcon(url_logox);

            result.add(logo.getImage());
            result.add(logox.getImage());
        }

        return result;
    }

    private static IList<Icon> initDayNight() {
        IList<Icon> result = new GapList<>();
        for (int i = 0; i < 8; i++) {
            URL url = Window.class.getResource(RESOURCES_DIR + String.format("daynight%d.png", i));
            ImageIcon icon = new ImageIcon(url);
            result.add(icon);
        }

        return result;
    }

    private void initInfoTables() {
        this.playerInfoTbl.setModel(playerInfoModel);
        this.posInfoTbl.setModel(posInfoModel);
        this.clientInfoTbl.setModel(clientInfoModel);

        // Initialize columns for the tables
        playerInfoModel.setColumnIdentifiers(new String[]{"Name", "Texture Model", "Unique ID", "Color", "Weapon"});
        posInfoModel.setColumnIdentifiers(new String[]{"Unique ID", "Position", "Front"});
        clientInfoModel.setColumnIdentifiers(new String[]{"Host Name", "Unique ID", "Time to Live", "Date Assigned", "Request per second", "Kick Client"});
    }

    public static void setEnabledComponents(Component component, boolean enabled) {
        component.setEnabled(enabled);

        if (component instanceof Container) {
            for (Component childComponent : ((Container) component).getComponents()) {
                setEnabledComponents(childComponent, enabled);
            }
        }
    }

    public void upsertPlayerInfo(PlayerInfo[] newPlayerInfos) {
        // Remove rows not in the new data
        for (int i = playerInfoModel.getRowCount() - 1; i >= 0; i--) {
            String uniqueId = (String) playerInfoModel.getValueAt(i, 0);
            boolean exists = false;
            for (PlayerInfo info : newPlayerInfos) {
                if (info.getUniqueId().equals(uniqueId)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                playerInfoModel.removeRow(i);
            }
        }

        // Add or update rows
        for (PlayerInfo info : newPlayerInfos) {
            boolean found = false;
            for (int i = 0; i < playerInfoModel.getRowCount(); i++) {
                if (playerInfoModel.getValueAt(i, 0).equals(info.getUniqueId())) {
                    playerInfoModel.setValueAt(info.name, i, 1);
                    playerInfoModel.setValueAt(info.texModel, i, 2);
                    playerInfoModel.setValueAt(info.color.toString(NumberFormat.getInstance(Locale.US)), i, 3);
                    playerInfoModel.setValueAt(info.weapon, i, 4);
                    found = true;
                    break;
                }
            }
            if (!found) {
                playerInfoModel.addRow(new Object[]{
                    info.name,
                    info.texModel,
                    info.uniqueId,
                    info.color.toString(NumberFormat.getInstance(Locale.US)),
                    info.weapon
                });
            }
        }
    }

    public void upsertPosInfo(PosInfo[] newPosInfos) {
        // Remove rows not in the new data
        for (int i = posInfoModel.getRowCount() - 1; i >= 0; i--) {
            String uniqueId = (String) posInfoModel.getValueAt(i, 0);
            boolean exists = false;
            for (PosInfo info : newPosInfos) {
                if (info.getUniqueId().equals(uniqueId)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                posInfoModel.removeRow(i);
            }
        }

        // Add or update rows
        for (PosInfo info : newPosInfos) {
            boolean found = false;
            for (int i = 0; i < posInfoModel.getRowCount(); i++) {
                if (posInfoModel.getValueAt(i, 0).equals(info.getUniqueId())) {
                    posInfoModel.setValueAt(info.getPos().toString(NumberFormat.getNumberInstance(Locale.US)), i, 1);
                    posInfoModel.setValueAt(info.getFront().toString(NumberFormat.getNumberInstance(Locale.US)), i, 2);
                    found = true;
                    break;
                }
            }
            if (!found) {
                posInfoModel.addRow(new Object[]{
                    info.getUniqueId(),
                    info.getPos().toString(NumberFormat.getNumberInstance(Locale.US)),
                    info.getFront().toString(NumberFormat.getNumberInstance(Locale.US))
                });
            }
        }
    }

    public void upsertClientInfo(ClientInfo[] newClientInfos) {
        final ButtonEditor kickBtnEdit = new ButtonEditor(new JButton("Kick"));
        kickBtnEdit.getButton().addActionListener((ActionEvent e) -> {
            final int srow = this.clientInfoTbl.getSelectedRow();
            String client = this.clientInfoModel.getValueAt(srow, clientInfoModel.findColumn("Unique ID")).toString();
            DSLogger.reportInfo("Kick player: " + client, null);
            logMessage("Kick player: " + client, Status.INFO);
            GameServer.kickPlayer(gameObject.gameServer, client);
        });
        final ButtonRenderer btnKickRend = new ButtonRenderer(kickBtnEdit.getButton());

        TableColumn kickCliCol = this.clientInfoTbl.getColumn("Kick Client");
        kickCliCol.setCellEditor(kickBtnEdit);
        kickCliCol.setCellRenderer(btnKickRend);

        // Remove rows not in the new data
        for (int i = clientInfoModel.getRowCount() - 1; i >= 0; i--) {
            String guid = (String) clientInfoModel.getValueAt(i, clientInfoModel.findColumn("Unique ID"));
            boolean exists = false;
            for (ClientInfo info : newClientInfos) {
                if (info.uniqueId.equals(guid)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                clientInfoModel.removeRow(i);
            }
        }

        // Add or update rows
        for (ClientInfo info : newClientInfos) {
            boolean found = false;
            for (int i = 0; i < clientInfoModel.getRowCount(); i++) {
                if (clientInfoModel.getValueAt(i, 1).equals(info.uniqueId)) {
                    clientInfoModel.setValueAt(info.hostName, i, 0);
                    clientInfoModel.setValueAt(info.uniqueId, i, 1);
                    clientInfoModel.setValueAt(info.timeToLive, i, 2);
                    clientInfoModel.setValueAt(info.dateAssigned, i, 3);
                    clientInfoModel.setValueAt(info.requestPerSecond, i, 4);
                    found = true;
                    break;
                }
            }
            if (!found) {
                clientInfoModel.addRow(new Object[]{info.getHostName(), info.getUniqueId(), info.getTimeToLive(), info.dateAssigned.toString(), info.requestPerSecond});
            }
        }
    }

    /**
     * Remove all rows from a DefaultTableModel.
     *
     * @param rowIndex Index of the row to be removed.
     * @param model DefaultTableModel from which the row will be removed.
     */
    private void removeAllRows(DefaultTableModel model) {
        for (int i = 0; i < model.getRowCount(); i++) {
            model.removeRow(i);
        }
    }

    public JList<String> getConsole() {
        return console;
    }

    public JLabel getGameTimeText() {
        return gameTimeText;
    }

    public JProgressBar getProgBar() {
        return progBar;
    }

    public JButton getBtnErase() {
        return btnErase;
    }

    public JButton getBtnExport() {
        return btnExport;
    }

    public JButton getBtnGenerate() {
        return btnGenerate;
    }

    public JButton getBtnImport() {
        return btnImport;
    }

    public JButton getBtnRestart() {
        return btnRestart;
    }

    public JButton getBtnStart() {
        return btnStart;
    }

    public JButton getBtnStop() {
        return btnStop;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        logPopupMenu = new javax.swing.JPopupMenu();
        logMenuCopy = new javax.swing.JMenuItem();
        logMenuSaveAs = new javax.swing.JMenuItem();
        panelNetwork = new javax.swing.JPanel();
        lblLocalIP = new javax.swing.JLabel();
        tboxLocalIP = new javax.swing.JTextField();
        lblServerPort = new javax.swing.JLabel();
        spinServerPort = new javax.swing.JSpinner();
        btnStart = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        btnRestart = new javax.swing.JButton();
        btnHealth = new javax.swing.JButton();
        panelWorld = new javax.swing.JPanel();
        lblLevelSize = new javax.swing.JLabel();
        cmbLevelSize = new javax.swing.JComboBox<>();
        lblWorldName = new javax.swing.JLabel();
        tboxWorldName = new javax.swing.JTextField();
        lblMapSeed = new javax.swing.JLabel();
        spinMapSeed = new javax.swing.JSpinner();
        lblBlockNum = new javax.swing.JLabel();
        tboxBlockNum = new javax.swing.JTextField();
        btnGenerate = new javax.swing.JButton();
        btnImport = new javax.swing.JButton();
        btnExport = new javax.swing.JButton();
        btnErase = new javax.swing.JButton();
        panelInfo = new javax.swing.JPanel();
        gameTimeText = new javax.swing.JLabel();
        progBar = new javax.swing.JProgressBar();
        tabPaneInfo = new javax.swing.JTabbedPane();
        spClientInfo = new javax.swing.JScrollPane();
        clientInfoTbl = new javax.swing.JTable();
        spPlayerInfo = new javax.swing.JScrollPane();
        playerInfoTbl = new javax.swing.JTable();
        spPosInfo = new javax.swing.JScrollPane();
        posInfoTbl = new javax.swing.JTable();
        panelConsole = new javax.swing.JPanel();
        lblHealthMini = new javax.swing.JLabel();
        togBtnBar = new javax.swing.JPanel();
        btnRefresh = new javax.swing.JButton();
        lblLines = new javax.swing.JLabel();
        spinLineNum = new javax.swing.JSpinner();
        togBtnError = new javax.swing.JToggleButton();
        togBtnWarn = new javax.swing.JToggleButton();
        togBtnInfo = new javax.swing.JToggleButton();
        spConsole = new javax.swing.JScrollPane();
        console = new javax.swing.JList<>();
        mainMenu = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        fileMenuStart = new javax.swing.JMenuItem();
        fileMenuStop = new javax.swing.JMenuItem();
        fileMenuRestart = new javax.swing.JMenuItem();
        fileMenuExit = new javax.swing.JMenuItem();
        fileStatus = new javax.swing.JMenu();
        statusMenuHealth = new javax.swing.JMenuItem();
        fileHelp = new javax.swing.JMenu();
        helpMenuAbout = new javax.swing.JMenuItem();
        helpMenuHowToUse = new javax.swing.JMenuItem();

        logMenuCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/copy-clipboard.png"))); // NOI18N
        logMenuCopy.setText("Copy");
        logMenuCopy.setEnabled(false);
        logMenuCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logMenuCopyActionPerformed(evt);
            }
        });
        logPopupMenu.add(logMenuCopy);

        logMenuSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/save-log.png"))); // NOI18N
        logMenuSaveAs.setText("Save As ...");
        logMenuSaveAs.setEnabled(false);
        logMenuSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logMenuSaveAsActionPerformed(evt);
            }
        });
        logPopupMenu.add(logMenuSaveAs);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Demolition Synergy Server");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setMinimumSize(new java.awt.Dimension(640, 360));
        setName("windowFrame"); // NOI18N
        setPreferredSize(new java.awt.Dimension(640, 360));
        setSize(new java.awt.Dimension(640, 360));
        getContentPane().setLayout(new java.awt.GridLayout(2, 2));

        panelNetwork.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Network", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 10))); // NOI18N
        panelNetwork.setLayout(new java.awt.GridLayout(4, 1));

        lblLocalIP.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblLocalIP.setText("Local IP:");
        panelNetwork.add(lblLocalIP);

        tboxLocalIP.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        tboxLocalIP.setText("127.0.0.1");
        tboxLocalIP.setToolTipText("Local IP, check Network options on OS if unsure (ipconfig on Windows)");
        tboxLocalIP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tboxLocalIPActionPerformed(evt);
            }
        });
        panelNetwork.add(tboxLocalIP);

        lblServerPort.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblServerPort.setText("Server Port:");
        panelNetwork.add(lblServerPort);

        spinServerPort.setModel(new javax.swing.SpinnerNumberModel(13667, 13660, 13669, 1));
        spinServerPort.setToolTipText("Server port used by the server to run on");
        spinServerPort.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinServerPortStateChanged(evt);
            }
        });
        panelNetwork.add(spinServerPort);

        btnStart.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        btnStart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/play.png"))); // NOI18N
        btnStart.setText("Start");
        btnStart.setToolTipText("Start the server specified by Local IP and Server Port");
        btnStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartActionPerformed(evt);
            }
        });
        panelNetwork.add(btnStart);

        btnStop.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        btnStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/stop.png"))); // NOI18N
        btnStop.setText("Stop");
        btnStop.setToolTipText("Stop Server Execution (Shutdown signal)");
        btnStop.setEnabled(false);
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });
        panelNetwork.add(btnStop);

        btnRestart.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        btnRestart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/restart.png"))); // NOI18N
        btnRestart.setText("Restart");
        btnRestart.setToolTipText("Restart server (Start & Stop, all players are kicked, World is perserved and Game Time is reset)");
        btnRestart.setEnabled(false);
        btnRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRestartActionPerformed(evt);
            }
        });
        panelNetwork.add(btnRestart);

        btnHealth.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        btnHealth.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/health.png"))); // NOI18N
        btnHealth.setText("Health");
        btnHealth.setToolTipText("Show server health (CPU Load, Memory Usage etc.)");
        btnHealth.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHealthActionPerformed(evt);
            }
        });
        panelNetwork.add(btnHealth);

        getContentPane().add(panelNetwork);

        panelWorld.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "World", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 10))); // NOI18N
        panelWorld.setLayout(new java.awt.GridLayout(3, 6));

        lblLevelSize.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblLevelSize.setText("Level Size:");
        panelWorld.add(lblLevelSize);

        cmbLevelSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbLevelSizeActionPerformed(evt);
            }
        });
        panelWorld.add(cmbLevelSize);

        lblWorldName.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblWorldName.setText("World Name:");
        panelWorld.add(lblWorldName);

        tboxWorldName.setText("My World");
        tboxWorldName.setToolTipText("World Name");
        tboxWorldName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tboxWorldNameActionPerformed(evt);
            }
        });
        panelWorld.add(tboxWorldName);

        lblMapSeed.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblMapSeed.setText("Seed:");
        panelWorld.add(lblMapSeed);

        spinMapSeed.setModel(new javax.swing.SpinnerNumberModel(305419896L, null, null, 1L));
        spinMapSeed.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinMapSeedStateChanged(evt);
            }
        });
        panelWorld.add(spinMapSeed);

        lblBlockNum.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblBlockNum.setText("Blocks:");
        panelWorld.add(lblBlockNum);

        tboxBlockNum.setEditable(false);
        tboxBlockNum.setText("0");
        tboxBlockNum.setToolTipText("Number of blocks in the level map. (Players won't load Empty World)");
        panelWorld.add(tboxBlockNum);

        btnGenerate.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        btnGenerate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/new.png"))); // NOI18N
        btnGenerate.setText("Generate");
        btnGenerate.setToolTipText("Generate new world using Random Level Generator");
        btnGenerate.setEnabled(false);
        btnGenerate.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnGenerate.setIconTextGap(2);
        btnGenerate.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnGenerate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGenerateActionPerformed(evt);
            }
        });
        panelWorld.add(btnGenerate);

        btnImport.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        btnImport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/import.png"))); // NOI18N
        btnImport.setText("Import");
        btnImport.setToolTipText("Import world from binary file (*.dat, *.ndat)");
        btnImport.setEnabled(false);
        btnImport.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnImport.setIconTextGap(2);
        btnImport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImportActionPerformed(evt);
            }
        });
        panelWorld.add(btnImport);

        btnExport.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        btnExport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/export.png"))); // NOI18N
        btnExport.setText("Export");
        btnExport.setToolTipText("Export world to binary file (*.dat, *.ndat)");
        btnExport.setEnabled(false);
        btnExport.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnExport.setIconTextGap(2);
        btnExport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportActionPerformed(evt);
            }
        });
        panelWorld.add(btnExport);

        btnErase.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        btnErase.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/trash.png"))); // NOI18N
        btnErase.setText("Erase");
        btnErase.setToolTipText("Erase World. World will have to be created or imported again.");
        btnErase.setEnabled(false);
        btnErase.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnErase.setIconTextGap(2);
        btnErase.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnErase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEraseActionPerformed(evt);
            }
        });
        panelWorld.add(btnErase);

        getContentPane().add(panelWorld);

        panelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Info", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 10))); // NOI18N
        panelInfo.setLayout(new java.awt.BorderLayout());

        gameTimeText.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        gameTimeText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gameTimeText.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/daynight0.png"))); // NOI18N
        gameTimeText.setText("Day 1 00:00:00");
        gameTimeText.setToolTipText("");
        gameTimeText.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Game Time", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 10))); // NOI18N
        panelInfo.add(gameTimeText, java.awt.BorderLayout.PAGE_START);

        progBar.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Progress:", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 10))); // NOI18N
        progBar.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        progBar.setStringPainted(true);
        panelInfo.add(progBar, java.awt.BorderLayout.PAGE_END);

        clientInfoTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Host Name", "Unique Id", "Time to Live", "Date Assigned", "Request per second"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Object.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        clientInfoTbl.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        clientInfoTbl.setRowHeight(24);
        clientInfoTbl.setShowGrid(true);
        spClientInfo.setViewportView(clientInfoTbl);

        tabPaneInfo.addTab("Client", new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/monitor_icon.png")), spClientInfo, ""); // NOI18N

        playerInfoTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Texture Model", "Unique Id", "Color", "Weapon"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        playerInfoTbl.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        playerInfoTbl.setRowHeight(24);
        playerInfoTbl.setShowGrid(true);
        spPlayerInfo.setViewportView(playerInfoTbl);

        tabPaneInfo.addTab("Player", new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/player.png")), spPlayerInfo); // NOI18N

        posInfoTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Unique Id", "Position", "Front"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        posInfoTbl.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        posInfoTbl.setRowHeight(24);
        posInfoTbl.setShowGrid(true);
        spPosInfo.setViewportView(posInfoTbl);

        tabPaneInfo.addTab("Position", new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/xyz.png")), spPosInfo); // NOI18N

        panelInfo.add(tabPaneInfo, java.awt.BorderLayout.CENTER);

        getContentPane().add(panelInfo);

        panelConsole.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Console", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 10))); // NOI18N
        panelConsole.setLayout(new java.awt.BorderLayout());

        lblHealthMini.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        lblHealthMini.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/health-mini.png"))); // NOI18N
        lblHealthMini.setText("CPU: 0% RAM: 0 MB In/Out: 0 bytes/0 bytes");
        lblHealthMini.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Health", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 10))); // NOI18N
        panelConsole.add(lblHealthMini, java.awt.BorderLayout.PAGE_END);

        togBtnBar.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Filter", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 10))); // NOI18N
        togBtnBar.setLayout(new java.awt.GridLayout(2, 5));

        btnRefresh.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        btnRefresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/refresh.png"))); // NOI18N
        btnRefresh.setText("Refresh");
        btnRefresh.setToolTipText("Refresh the messages (Console)");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });
        togBtnBar.add(btnRefresh);

        lblLines.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblLines.setText("Preview Lines:");
        togBtnBar.add(lblLines);

        spinLineNum.setModel(new javax.swing.SpinnerNumberModel(10000, 1, 500000, 1));
        spinLineNum.setToolTipText("Top number of lines to preview (from the end).");
        spinLineNum.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinLineNumStateChanged(evt);
            }
        });
        togBtnBar.add(spinLineNum);

        togBtnError.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        togBtnError.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/err.png"))); // NOI18N
        togBtnError.setSelected(true);
        togBtnError.setText("Error");
        togBtnError.setToolTipText("Filter Errors");
        togBtnError.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                togBtnErrorActionPerformed(evt);
            }
        });
        togBtnBar.add(togBtnError);

        togBtnWarn.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        togBtnWarn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/warn.png"))); // NOI18N
        togBtnWarn.setSelected(true);
        togBtnWarn.setText("Warning");
        togBtnWarn.setToolTipText("Filter Warnings");
        togBtnWarn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                togBtnWarnActionPerformed(evt);
            }
        });
        togBtnBar.add(togBtnWarn);

        togBtnInfo.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        togBtnInfo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/info.png"))); // NOI18N
        togBtnInfo.setSelected(true);
        togBtnInfo.setText("Info");
        togBtnInfo.setToolTipText("Filter Information");
        togBtnInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                togBtnInfoActionPerformed(evt);
            }
        });
        togBtnBar.add(togBtnInfo);

        panelConsole.add(togBtnBar, java.awt.BorderLayout.PAGE_START);

        console.setModel(new DefaultListModel<String>()
        );
        spConsole.setViewportView(console);

        panelConsole.add(spConsole, java.awt.BorderLayout.CENTER);

        getContentPane().add(panelConsole);

        fileMenu.setText("Server");

        fileMenuStart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/play.png"))); // NOI18N
        fileMenuStart.setText("Start");
        fileMenuStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileMenuStartActionPerformed(evt);
            }
        });
        fileMenu.add(fileMenuStart);

        fileMenuStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/stop.png"))); // NOI18N
        fileMenuStop.setText("Stop");
        fileMenuStop.setEnabled(false);
        fileMenuStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileMenuStopActionPerformed(evt);
            }
        });
        fileMenu.add(fileMenuStop);

        fileMenuRestart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/restart.png"))); // NOI18N
        fileMenuRestart.setText("Restart");
        fileMenuRestart.setEnabled(false);
        fileMenuRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileMenuRestartActionPerformed(evt);
            }
        });
        fileMenu.add(fileMenuRestart);

        fileMenuExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/exit_icon_small.png"))); // NOI18N
        fileMenuExit.setText("Exit");
        fileMenuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileMenuExitActionPerformed(evt);
            }
        });
        fileMenu.add(fileMenuExit);

        mainMenu.add(fileMenu);

        fileStatus.setText("Status");

        statusMenuHealth.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/health-mini.png"))); // NOI18N
        statusMenuHealth.setText("Health");
        statusMenuHealth.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statusMenuHealthActionPerformed(evt);
            }
        });
        fileStatus.add(statusMenuHealth);

        mainMenu.add(fileStatus);

        fileHelp.setText("Help");

        helpMenuAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/info-about.png"))); // NOI18N
        helpMenuAbout.setText("About");
        helpMenuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMenuAboutActionPerformed(evt);
            }
        });
        fileHelp.add(helpMenuAbout);

        helpMenuHowToUse.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/qmark.png"))); // NOI18N
        helpMenuHowToUse.setText("How to use");
        helpMenuHowToUse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMenuHowToUseActionPerformed(evt);
            }
        });
        fileHelp.add(helpMenuHowToUse);

        mainMenu.add(fileHelp);

        setJMenuBar(mainMenu);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartActionPerformed
        // TODO add your handling code here:
        startServerAndUpdate();
    }//GEN-LAST:event_btnStartActionPerformed

    public void startServerAndUpdate() {
        gameObject.gameServer.setLocalIP(tboxLocalIP.getText());
        gameObject.gameServer.setPort((int) spinServerPort.getValue());

        gameObject.start();
        setEnabledComponents(this.panelWorld, true);
        setEnabledComponents(this.panelInfo, true);
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        btnRestart.setEnabled(true);

        fileMenuStart.setEnabled(false);
        fileMenuStop.setEnabled(true);
        fileMenuRestart.setEnabled(true);

        tboxLocalIP.setEnabled(false);
        spinServerPort.setEnabled(false);

        logMenuCopy.setEnabled(true);
        logMenuSaveAs.setEnabled(true);

        messageLog.clear();

        // Start logging in a (rendered) swing component
        gameObject.WINDOW.conRefreshTimer.start();
    }

    public void stopServerAndUpdate() {
        // TODO add your handling code here:                
        setEnabledComponents(this.panelWorld, false);
        setEnabledComponents(this.panelInfo, false);
        gameObject.gameServer.stopServer();
        gameObject.game.stop();

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnRestart.setEnabled(false);

        fileMenuStart.setEnabled(true);
        fileMenuStop.setEnabled(false);
        fileMenuRestart.setEnabled(false);

        removeAllRows(posInfoModel);
        removeAllRows(clientInfoModel);
        removeAllRows(playerInfoModel);

        tboxLocalIP.setEnabled(true);
        spinServerPort.setEnabled(true);

        logMenuCopy.setEnabled(false);
        logMenuSaveAs.setEnabled(false);

        // Stop logging of (rendered) swing component
        gameObject.WINDOW.conRefreshTimer.stop();
    }

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        stopServerAndUpdate();
    }//GEN-LAST:event_btnStopActionPerformed

    public void restartServerAndUpdate() {
        stopServerAndUpdate();
        Game.setGameTicks(config.getGameTicks());

        startServerAndUpdate();
    }

    private void btnRestartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRestartActionPerformed
        // TODO add your handling code here:
        restartServerAndUpdate();
    }//GEN-LAST:event_btnRestartActionPerformed

    private void fileMenuStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileMenuStartActionPerformed
        // TODO add your handling code here:
        startServerAndUpdate();
    }//GEN-LAST:event_fileMenuStartActionPerformed

    public void generateWorld() {
        // TODO add your handling code here:    
        btnGenerate.setEnabled(false);
        btnImport.setEnabled(false);
        btnExport.setEnabled(false);
        btnErase.setEnabled(false);
        final GameObject.MapLevelSize levelSize = (GameObject.MapLevelSize) cmbLevelSize.getSelectedItem();
        SwingWorker<Boolean, Void> swingWorker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return gameObject.generateRandomLevel(levelSize);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(Window.this, "New world succesfully generated!", "Generate New World", JOptionPane.INFORMATION_MESSAGE);
                        tboxBlockNum.setText(String.valueOf(LevelContainer.AllBlockMap.getPopulation()));
                        btnErase.setEnabled(true);
                    } else {
                        JOptionPane.showMessageDialog(Window.this, "New world generation resulted in failured!", "Generate New World", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                }
                btnGenerate.setEnabled(true);
                btnImport.setEnabled(true);
                btnExport.setEnabled(true);
            }
        };
        swingWorker.execute();
    }

    private void btnGenerateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerateActionPerformed
        generateWorld();
    }//GEN-LAST:event_btnGenerateActionPerformed

    /**
     * Set world size (closed form) {SMALL=25k, MEDIUM=50k, LARGE=100k,
     * HUGE=131070 }
     */
    public void setWorldLevelSize() {
        final int numberOfBlocks;
        final GameObject.MapLevelSize levelSize = (GameObject.MapLevelSize) cmbLevelSize.getSelectedItem();
        switch (levelSize) {
            default:
            case SMALL:
                numberOfBlocks = 25000;
                break;
            case MEDIUM:
                numberOfBlocks = 50000;
                break;
            case LARGE:
                numberOfBlocks = 100000;
                break;
            case HUGE:
                numberOfBlocks = 131070;
                break;
        }
        gameObject.randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
    }

    /**
     * Set world size (closed form) {SMALL=25k, MEDIUM=50k, LARGE=100k,
     * HUGE=131070 }
     *
     * @param newLevelSize world size to set
     */
    public void setWorldLevelSize(String newLevelSize) {
        final int numberOfBlocks;
        final GameObject.MapLevelSize levelSize = GameObject.MapLevelSize.valueOf(newLevelSize.toUpperCase());
        switch (levelSize) {
            default:
            case SMALL:
                numberOfBlocks = 25000;
                break;
            case MEDIUM:
                numberOfBlocks = 50000;
                break;
            case LARGE:
                numberOfBlocks = 100000;
                break;
            case HUGE:
                numberOfBlocks = 131070;
                break;
        }
        this.getCmbLevelSize().setSelectedItem(levelSize);
        gameObject.randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
    }

    private void cmbLevelSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbLevelSizeActionPerformed
        // TODO add your handling code here:
        setWorldLevelSize();
    }//GEN-LAST:event_cmbLevelSizeActionPerformed

    private void spinMapSeedStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinMapSeedStateChanged
        // TODO add your handling code here:
        gameObject.randomLevelGenerator.setSeed((long) this.spinMapSeed.getValue());
    }//GEN-LAST:event_spinMapSeedStateChanged

    private void eraseWorld() {
        if (LevelContainer.AllBlockMap.getPopulation() == 0) {
            JOptionPane.showMessageDialog(Window.this, "World is empty - Please create or import one!", "Erase World", JOptionPane.ERROR_MESSAGE);
        } else {
            gameObject.clearEverything();
            JOptionPane.showMessageDialog(Window.this, "World erased! New world can be created or imported.", "Erase World", JOptionPane.INFORMATION_MESSAGE);
            tboxBlockNum.setText(String.valueOf(LevelContainer.AllBlockMap.getPopulation()));
        }
    }

    private void btnEraseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEraseActionPerformed
        // TODO add your handling code here:
        eraseWorld();
    }//GEN-LAST:event_btnEraseActionPerformed

    // init dialog for opening the files, setting it's filters
    private void initDialogs() {
        FileNameExtensionFilter datFilter = new FileNameExtensionFilter("Old Data Format (*.dat)", "dat");
        FileNameExtensionFilter ndatFilter = new FileNameExtensionFilter("New Data Format (*.ndat)", "ndat");

        fileImport.addChoosableFileFilter(datFilter);
        fileImport.addChoosableFileFilter(ndatFilter);

        fileExport.addChoosableFileFilter(datFilter);
        fileExport.addChoosableFileFilter(ndatFilter);
    }

    /**
     * Import world into Dedicated server. Player(s) which are connecting are
     * gonna download that world. File Dialog is used.
     */
    public void worldImport() {
        int option = fileImport.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            final File file = fileImport.getSelectedFile();

            SwingWorker<Boolean, Void> swingWorker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return gameObject.levelContainer.loadLevelFromFile(file.getAbsolutePath());
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(Window.this, "World sucessfully imported from file!", "Import World", JOptionPane.INFORMATION_MESSAGE);
                            tboxBlockNum.setText(String.valueOf(LevelContainer.AllBlockMap.getPopulation()));
                            btnErase.setEnabled(true);
                        } else {
                            JOptionPane.showMessageDialog(Window.this, "World import resulted in failure!", "Import World", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        DSLogger.reportError(ex.getMessage(), ex);
                    }
                }
            };

            swingWorker.execute();
        }
    }

    /**
     * Import world into Dedicated server. Player(s) which are connecting are
     * gonna download that world.
     *
     * @param fileImport file to import
     * @throws java.lang.Exception if file not found
     */
    public void worldImport(File fileImport) throws Exception {
        if (!fileImport.exists()) {
            throw new Exception("World not imported - file not found!");
        }

        SwingWorker<Boolean, Void> swingWorker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return gameObject.levelContainer.loadLevelFromFile(fileImport.getAbsolutePath());
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(Window.this, "World sucessfully imported from file!", "Import World", JOptionPane.INFORMATION_MESSAGE);
                        tboxBlockNum.setText(String.valueOf(LevelContainer.AllBlockMap.getPopulation()));
                        btnErase.setEnabled(true);
                    } else {
                        JOptionPane.showMessageDialog(Window.this, "World import resulted in failure!", "Import World", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                }
            }
        };

        swingWorker.execute();
    }

    private void worldExport() {
        if (LevelContainer.AllBlockMap.getPopulation() == 0) {
            JOptionPane.showMessageDialog(Window.this, "World is empty - Please create or import one!", "Export World", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int option = fileExport.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            final File file = fileExport.getSelectedFile();

            SwingWorker<Boolean, Void> swingWorker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return gameObject.levelContainer.saveLevelToFile(file.getAbsolutePath());
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(Window.this, "World sucessfully exported from file!", "Export World", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(Window.this, "World export resulted in failure!", "Export World", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        DSLogger.reportError(ex.getMessage(), ex);
                    }
                }
            };

            swingWorker.execute();
        }
    }

    private void btnImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImportActionPerformed
        // TODO add your handling code here:
        worldImport();
    }//GEN-LAST:event_btnImportActionPerformed

    private void btnExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportActionPerformed
        // TODO add your handling code here:
        worldExport();
    }//GEN-LAST:event_btnExportActionPerformed

    private void tboxWorldNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tboxWorldNameActionPerformed
        // TODO add your handling code here:
        gameObject.gameServer.setWorldName(this.tboxWorldName.getText());
    }//GEN-LAST:event_tboxWorldNameActionPerformed

    /**
     * Display 'Health' of the Server as CPU usage, Memory usage and Network
     * usage (Local IP interface). In short notation.
     *
     * @param deltaTime deltaTime between periods
     */
    public void checkHealthMini(double deltaTime) {
        StringBuilder sb = new StringBuilder();

        // Append CPU load
        appendCpuLoad(deltaTime, sb, true);
        sb.append(" ");

        // Append memory usage
        appendMemoryUsage(sb, true);
        sb.append(" ");

        // Append network usage for local IP
        appendNetworkUsage(sb, gameObject.gameServer.localIP, true);

        this.lblHealthMini.setText(sb.toString());
    }

    /**
     * Appends the server status to the provided StringBuilder.
     *
     * @param sb the StringBuilder to append the server status to
     */
    private void appendServerStatus(StringBuilder sb) {
        if (gameObject.gameServer.running && !gameObject.gameServer.shutDownSignal) {
            sb.append("Status: RUNNING");
        } else if (gameObject.gameServer.running && gameObject.gameServer.shutDownSignal) {
            sb.append("Status: PENDING SHUT DOWN");
        } else {
            sb.append("Status: NOT RUNNING");
        }
        sb.append("\n\n");
    }

    /**
     * Appends the CPU load for the current process to the provided
     * StringBuilder.
     *
     * @param deltaTime delta time between polls (updates)
     * @param sb the StringBuilder to append the CPU load to
     * @param shortNotation short notation
     */
    private void appendCpuLoad(double deltaTime, StringBuilder sb, boolean shortNotation) {
        CentralProcessor processor = si.getHardware().getProcessor();
        int cpuNumber = processor.getLogicalProcessorCount();

        OSProcess process = os.getProcess(os.getProcessId());
        if (lastTime == 0L) { // first time, avoid abnormal values
            lastTime = (process.getKernelTime() + process.getUserTime()) / 1000.0; // convert millis to sec
        }

        currTime = (process.getKernelTime() + process.getUserTime()) / 1000.0; // convert millis to sec
        // "on windows i need to divide by the # of cpus, but not on the other two systems" - val235
        double cpuLoad = (100d * (currTime - lastTime) / (deltaTime)) / (os.getFamily().equalsIgnoreCase("windows") ? cpuNumber : 1);
        lastTime = currTime;

        sb.append((shortNotation) ? String.format("CPU: %.1f%%\n", cpuLoad) : String.format("CPU Load: %.2f%%\n", cpuLoad));
    }

    /**
     * Appends the memory usage for the current process to the provided
     * StringBuilder.
     *
     * @param sb the StringBuilder to append the memory usage to
     * @param shortNotation short notation
     */
    private void appendMemoryUsage(StringBuilder sb, boolean shortNotation) {
        OSProcess currentProcess = os.getProcess(os.getProcessId());
        long usedMemory = currentProcess.getResidentSetSize();
        if (shortNotation) {
            sb.append(String.format("RAM: %d MB\n", usedMemory / (1024 * 1024)));
        } else {
            sb.append(String.format("Memory: Used = %d MB\n", usedMemory / (1024 * 1024)));
        }
    }

    /**
     * Appends the network usage to the provided StringBuilder for the specified
     * IP address.
     *
     * @param sb the StringBuilder to append the network usage to
     * @param targetIpAddress the local IP address to check (e.g.,
     * "192.168.1.3")
     * @param shortNotation short notation
     */
    private void appendNetworkUsage(StringBuilder sb, String targetIpAddress, boolean shortNotation) {
        List<NetworkIF> networkIFs = hal.getNetworkIFs(true); // true -> include local interfaces
        for (NetworkIF netIF : networkIFs) {
            if (netIF.getIfOperStatus() == NetworkIF.IfOperStatus.UP) {
                List<String> listOfIPv4 = Arrays.asList(netIF.getIPv4addr());
                if (listOfIPv4.contains(targetIpAddress)) {
                    // for first time
                    if (bytesReceived == 0L) {
                        bytesReceived = netIF.getBytesRecv();
                    }

                    // for first time
                    if (bytesSent == 0L) {
                        bytesSent = netIF.getBytesSent();
                    }

                    if (shortNotation) {
                        sb.append(String.format("In/Out: %d bytes/%d bytes\n",
                                netIF.getBytesRecv() - bytesReceived, netIF.getBytesSent() - bytesSent));
                    } else {
                        sb.append(String.format("Network: %s, Received = %d bytes, Sent = %d bytes\n",
                                netIF.getName(), netIF.getBytesRecv() - bytesReceived, netIF.getBytesSent() - bytesSent));
                    }
                    bytesReceived = netIF.getBytesRecv();
                    bytesSent = netIF.getBytesSent();
                    break;
                }
            }
        }
    }

    /**
     * Method to check and display the server's health status as CPU usage,
     * Memory usage and Network usage (Local IP interface).
     */
    private void checkHealth() {
        StringBuilder sb = new StringBuilder();
        appendServerStatus(sb);
        appendCpuLoad(1.0, sb, false);
        appendMemoryUsage(sb, false);
        appendNetworkUsage(sb, gameObject.gameServer.localIP, false);

        JTextArea textArea = new JTextArea(sb.toString(), 10, 35);
        JScrollPane jsp = new JScrollPane(textArea);
        textArea.setEditable(false);
        JOptionPane.showMessageDialog(null, jsp, "Server Health", JOptionPane.INFORMATION_MESSAGE);
    }

    private void btnHealthActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHealthActionPerformed
        // TODO add your handling code here:      
        checkHealth();
    }//GEN-LAST:event_btnHealthActionPerformed

    private void statusMenuHealthActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statusMenuHealthActionPerformed
        // TODO add your handling code here:
        checkHealth();
    }//GEN-LAST:event_statusMenuHealthActionPerformed

    private void infoAbout() {
        URL icon_url = getClass().getResource(RESOURCES_DIR + LICENSE_LOGO_FILE_NAME);
        if (icon_url != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("VERSION v2.0 (%s BUILD reviewed on 2024-12-15 at 08:45).\n", BUILD.toString()));
            sb.append("This software is free software, \n");
            sb.append("licensed under GNU General Public License (GPL).\n");
            sb.append("\n");
            sb.append("For latest release changelog please check GitHub page. \n");
            sb.append("\n");
            sb.append(String.format("Demolition Synergy Version: %d\n", GameObject.VERSION));
            sb.append("\n");
            sb.append("Copyright © 2024\n");
            sb.append("Alexander \"Ermac\" Stojanovich\n");
            sb.append("\n");
            ImageIcon icon = new ImageIcon(icon_url);
            JTextArea textArea = new JTextArea(sb.toString(), 15, 35);
            JScrollPane jsp = new JScrollPane(textArea);
            textArea.setEditable(false);
            JOptionPane.showMessageDialog(this, jsp, "About", JOptionPane.INFORMATION_MESSAGE, icon);
        }
    }

    /**
     * Copy Log to ClipBoard
     */
    private void copyLog() {
        StringBuilder sb = new StringBuilder();
        ListModel<String> model = console.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            sb.append(model.getElementAt(i));
        }
        String text = sb.toString();
        StringSelection stringSelection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    /**
     * Save as Log
     */
    private void saveAsLog() {
        int option = logSaveAs.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            final File file = logSaveAs.getSelectedFile();

            SwingWorker<Void, Void> swingWorker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        // Write the content of the JTextArea to the file
                        StringBuilder sb = new StringBuilder();
                        ListModel<String> model = console.getModel();
                        for (int i = 0; i < model.getSize(); i++) {
                            sb.append(model.getElementAt(i));
                        }
                        String text = sb.toString();

                        writer.write(text);
                        JOptionPane.showMessageDialog(Window.this, "Log saved successfully!");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(Window.this, "Error saving log: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    return null;
                }
            };

            swingWorker.execute();
        }
    }

    private void helpMenuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuAboutActionPerformed
        // TODO add your handling code here:
        infoAbout();
    }//GEN-LAST:event_helpMenuAboutActionPerformed

    private void helpMenuHowToUseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuHowToUseActionPerformed
        // TODO add your handling code here:
        infoHelp();
    }//GEN-LAST:event_helpMenuHowToUseActionPerformed

    private void fileMenuStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileMenuStopActionPerformed
        // TODO add your handling code here:        
        stopServerAndUpdate();
    }//GEN-LAST:event_fileMenuStopActionPerformed

    private void fileMenuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileMenuExitActionPerformed
        // TODO add your handling code here:
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_fileMenuExitActionPerformed

    private void fileMenuRestartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileMenuRestartActionPerformed
        // TODO add your handling code here:
        restartServerAndUpdate();
    }//GEN-LAST:event_fileMenuRestartActionPerformed

    private void tboxLocalIPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tboxLocalIPActionPerformed
        // TODO add your handling code here:
        gameObject.gameServer.setLocalIP(tboxLocalIP.getText());
    }//GEN-LAST:event_tboxLocalIPActionPerformed

    private void spinServerPortStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinServerPortStateChanged
        // TODO add your handling code here:
        gameObject.gameServer.setPort((int) spinServerPort.getValue());
    }//GEN-LAST:event_spinServerPortStateChanged

    private void logMenuCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logMenuCopyActionPerformed
        // TODO add your handling code here:
        copyLog();
    }//GEN-LAST:event_logMenuCopyActionPerformed

    private void logMenuSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logMenuSaveAsActionPerformed
        // TODO add your handling code here:
        saveAsLog();
    }//GEN-LAST:event_logMenuSaveAsActionPerformed

    private void togBtnErrorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_togBtnErrorActionPerformed
        // TODO add your handling code here:
        if (togBtnError.isSelected()) {
            filter |= Status.ERR.mask;
        } else {
            filter &= ~Status.ERR.mask;
        }
    }//GEN-LAST:event_togBtnErrorActionPerformed

    private void togBtnWarnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_togBtnWarnActionPerformed
        // TODO add your handling code here:
        if (togBtnWarn.isSelected()) {
            filter |= Status.WARN.mask;
        } else {
            filter &= ~Status.WARN.mask;
        }
    }//GEN-LAST:event_togBtnWarnActionPerformed

    private void togBtnInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_togBtnInfoActionPerformed
        if (togBtnInfo.isSelected()) {
            filter |= Status.INFO.mask;
        } else {
            filter &= ~Status.INFO.mask;
        }
    }//GEN-LAST:event_togBtnInfoActionPerformed

    private void spinLineNumStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinLineNumStateChanged
        // TODO add your handling code here:

    }//GEN-LAST:event_spinLineNumStateChanged

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        // TODO add your handling code here:
        btnRefresh.setEnabled(false);
        refreshConsole();
        btnRefresh.setEnabled(true);
    }//GEN-LAST:event_btnRefreshActionPerformed

    /**
     * Append a single message to the console with styling.
     *
     * @param listModel model of the console
     * @param msg the message to append
     */
    private void appendToConsole(DefaultListModel listModel, Message msg) {
        listModel.addElement(msg.status + ":" + msg.text + "\r\n");
    }

    /**
     * Refresh console log in chunks. Works with filters {INFO, WARN, ERR}.
     * Called by a Swing Timer to append messages in manageable chunks.
     */
    public void refreshConsole() {
        final DefaultListModel<String> conListModel = (DefaultListModel<String>) console.getModel();
        if (messageLog.isEmpty()) {
            conListModel.clear();
            logPass = 0; // Reset logPass for future use
            return;
        }

        IList<Message> filteredMessages;
        int lines = (int) this.spinLineNum.getValue();

        // Prevent memory overflow by trimming the logs
        if (messageLog.size() > 2 * lines) {
            messageLog.remove(0, lines);
        }
        if (conListModel.size() > 2 * lines) {
            conListModel.removeRange(0, lines);
        }

        // Filter messages
        filteredMessages = messageLog.immutableList().filter(m -> (m.status.mask & filter) != 0);

        int size = filteredMessages.size();
        if (size == 0) {
            conListModel.clear();
            logPass = 0; // Reset logPass for future use
            return;
        }

        // Calculate the range of messages to display
        int startIndex = Math.max(size - lines, 0);
        final IList<Message> lastNMessages = filteredMessages.getAll(startIndex, size - startIndex);

        // If this is the first pass, clear the model
        if (logPass == 0) {
            conListModel.clear();
        }

        // Append messages in chunks
        int chunkSize = Window.CON_CHUNK_LINES_SIZE;
        int start = logPass * chunkSize;
        int end = Math.min(start + chunkSize, lastNMessages.size());

        lastNMessages.getAll(start, end - start).forEach(msg -> appendToConsole(conListModel, msg));

        // Check if we are done processing all messages
        if (end >= lastNMessages.size()) {
            logPass = 0; // Reset logPass for the next refresh
        } else {
            logPass++; // Prepare for the next chunk
        }
    }

    private void infoHelp() {
        URL icon_url = getClass().getResource(RESOURCES_DIR + LOGOX_FILE_NAME);
        if (icon_url != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("- FOR THE PURPOSE ABOUT THIS PROGRAM, \n");
            sb.append("check About. Make sure that you checked it first.\n");
            sb.append("\n");
            sb.append("- Quick start guide:\n");
            sb.append("\t1. Enter your local IP (Local Area Network) and (local) server port.\n");
            sb.append("\t2. Start the server by click on 'Start' button.\n");
            sb.append("\t3. Generate new world by click 'Generate New' button.\n");
            sb.append("\t4. Alternatively, use 'Import World' to import earlier created level map.\n");
            sb.append("\t5. (Optional) World could be exported with 'Export World' to import earlier created level map.\n");
            sb.append("\n");
            sb.append("[*] Trivia:\n");
            sb.append("- Local IP is your local IP address (obtained from home or office router). Check ipconfig on Windows.\n");
            sb.append("- There is no conceptual difference between 'DSynergy' client/server and dedicated server.\n");
            sb.append("- Dedicated server originated direcly from 'DSynergy' by removing code.\n");
            sb.append("- Level Map formats are offered in: 'Old data level format' (*.dat) and 'New data level format' (*.ndat).\n");
            sb.append("- 'Old data level format' (*.dat) is around for quite a while. 'New data level format' (*.ndat) is recent supporting uncapped level sizes.\n");
            sb.append("\n");
            sb.append("\n");
            ImageIcon icon = new ImageIcon(icon_url);
            JTextArea textArea = new JTextArea(sb.toString(), 15, 35);
            JScrollPane jsp = new JScrollPane(textArea);
            textArea.setEditable(false);
            JOptionPane.showMessageDialog(this, jsp, "How to use", JOptionPane.INFORMATION_MESSAGE, icon);
        }
    }

    /**
     * Call this method to update cycle based on ingame time.
     */
    public void updateDayNightCycle() {
        int index = (GameTime.Now().hours / 3) & 7;
        Icon icon = dayNightIcons.get(index);

        gameTimeText.setIcon(icon);
    }

    public JComboBox<String> getCmbLevelSize() {
        return cmbLevelSize;
    }

    public JSpinner getSpinMapSeed() {
        return spinMapSeed;
    }

    public Timer getConRefreshTimer() {
        return conRefreshTimer;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnErase;
    private javax.swing.JButton btnExport;
    private javax.swing.JButton btnGenerate;
    private javax.swing.JButton btnHealth;
    private javax.swing.JButton btnImport;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnRestart;
    private javax.swing.JButton btnStart;
    private javax.swing.JButton btnStop;
    private javax.swing.JTable clientInfoTbl;
    private javax.swing.JComboBox<String> cmbLevelSize;
    private javax.swing.JList<String> console;
    private javax.swing.JMenu fileHelp;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem fileMenuExit;
    private javax.swing.JMenuItem fileMenuRestart;
    private javax.swing.JMenuItem fileMenuStart;
    private javax.swing.JMenuItem fileMenuStop;
    private javax.swing.JMenu fileStatus;
    private javax.swing.JLabel gameTimeText;
    private javax.swing.JMenuItem helpMenuAbout;
    private javax.swing.JMenuItem helpMenuHowToUse;
    private javax.swing.JLabel lblBlockNum;
    private javax.swing.JLabel lblHealthMini;
    private javax.swing.JLabel lblLevelSize;
    private javax.swing.JLabel lblLines;
    private javax.swing.JLabel lblLocalIP;
    private javax.swing.JLabel lblMapSeed;
    private javax.swing.JLabel lblServerPort;
    private javax.swing.JLabel lblWorldName;
    private javax.swing.JMenuItem logMenuCopy;
    private javax.swing.JMenuItem logMenuSaveAs;
    private javax.swing.JPopupMenu logPopupMenu;
    private javax.swing.JMenuBar mainMenu;
    private javax.swing.JPanel panelConsole;
    private javax.swing.JPanel panelInfo;
    private javax.swing.JPanel panelNetwork;
    private javax.swing.JPanel panelWorld;
    private javax.swing.JTable playerInfoTbl;
    private javax.swing.JTable posInfoTbl;
    private javax.swing.JProgressBar progBar;
    private javax.swing.JScrollPane spClientInfo;
    private javax.swing.JScrollPane spConsole;
    private javax.swing.JScrollPane spPlayerInfo;
    private javax.swing.JScrollPane spPosInfo;
    private javax.swing.JSpinner spinLineNum;
    private javax.swing.JSpinner spinMapSeed;
    private javax.swing.JSpinner spinServerPort;
    private javax.swing.JMenuItem statusMenuHealth;
    private javax.swing.JTabbedPane tabPaneInfo;
    private javax.swing.JTextField tboxBlockNum;
    private javax.swing.JTextField tboxLocalIP;
    private javax.swing.JTextField tboxWorldName;
    private javax.swing.JPanel togBtnBar;
    private javax.swing.JToggleButton togBtnError;
    private javax.swing.JToggleButton togBtnInfo;
    private javax.swing.JToggleButton togBtnWarn;
    // End of variables declaration//GEN-END:variables

}

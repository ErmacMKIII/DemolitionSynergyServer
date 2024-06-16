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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import static rs.alexanderstojanovich.evgds.main.Game.RESOURCES_DIR;
import rs.alexanderstojanovich.evgds.net.ClientInfo;
import rs.alexanderstojanovich.evgds.net.PlayerInfo;
import rs.alexanderstojanovich.evgds.net.PosInfo;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Window extends javax.swing.JFrame {

    public final GameObject gameObject;
    public static final Dimension DIM = Toolkit.getDefaultToolkit().getScreenSize();

    public static final String LOGOX_FILE_NAME = "app-icon.png";
    public static final String LOGO_FILE_NAME = "app-icon-small.png";

    public final Map<String, Integer> playerInfoMap = new LinkedHashMap<>();
    public final Map<String, Integer> posInfoMap = new LinkedHashMap<>();
    public final Map<String, Integer> clientInfoMap = new LinkedHashMap<>();

    public final DefaultTableModel playerInfoModel = new DefaultTableModel();
    public final DefaultTableModel posInfoModel = new DefaultTableModel();
    public final DefaultTableModel clientInfoModel = new DefaultTableModel();

    /**
     * Creates new form ServerIntrface
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
    }

    public void initCenterWindow() {
        this.setLocation(DIM.width / 2 - this.getSize().width / 2, DIM.height / 2 - this.getSize().height / 2);
    }

    public void writeOnConsole(String msg) {
        this.console.append(msg + "\r\n");
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

    private void initInfoTables() {
        this.playerInfoTbl.setModel(playerInfoModel);
        this.posInfoTbl.setModel(posInfoModel);
    }

    public static void setEnabledComponents(Component component, boolean enabled) {
        component.setEnabled(enabled);

        if (component instanceof Container) {
            for (Component childComponent : ((Container) component).getComponents()) {
                setEnabledComponents(childComponent, enabled);
            }
        }
    }

    /**
     * Upserts player information into the table.
     *
     * @param playerInfos Array of playerInfoMap objects to be upserted.
     */
    public void upsertPlayerInfo(PlayerInfo[] playerInfos) {
        for (PlayerInfo playerInfo : playerInfos) {
            if (playerInfoMap.containsKey(playerInfo.getUniqueId())) {
                int rowIndex = playerInfoMap.get(playerInfo.getUniqueId());
                updateRowPlayerInfo(rowIndex, playerInfo);
            } else {
                addRowPlayerInfo(playerInfo);
            }
        }
    }

    /**
     * Adds a new row to the table with player information.
     *
     * @param playerInfo playerInfoMap object containing the data for the new
     * row.
     */
    private void addRowPlayerInfo(PlayerInfo playerInfo) {
        Object[] rowData = {
            playerInfo.getName(),
            playerInfo.getTexModel(),
            playerInfo.getUniqueId(),
            playerInfo.getColor().toString(NumberFormat.getInstance(Locale.US))
        };
        playerInfoModel.addRow(rowData);
        int rowIndex = playerInfoModel.getRowCount() - 1;
        playerInfoMap.put(playerInfo.getUniqueId(), rowIndex);
    }

    /**
     * Updates an existing row in the table with new player information.
     *
     * @param rowIndex Index of the row to be updated.
     * @param playerInfo playerInfoMap object containing the updated data.
     */
    private void updateRowPlayerInfo(int rowIndex, PlayerInfo playerInfo) {
        playerInfoModel.setValueAt(playerInfo.getName(), rowIndex, 0);
        playerInfoModel.setValueAt(playerInfo.getTexModel(), rowIndex, 1);
        playerInfoModel.setValueAt(playerInfo.getUniqueId(), rowIndex, 2);
        playerInfoModel.setValueAt((playerInfo.getColor().toString(NumberFormat.getInstance(Locale.US))), rowIndex, 3);
    }

    /**
     * Upserts position information into the table.
     *
     * @param posInfos Array of posInfoMap objects to be upserted.
     */
    public void upsertPosInfo(PosInfo[] posInfos) {
        for (PosInfo posInfo : posInfos) {
            if (playerInfoMap.containsKey(posInfo.getUniqueId())) {
                int rowIndex = posInfoMap.get(posInfo.getUniqueId());
                updateRowPosInfo(rowIndex, posInfo);
            } else {
                addRowPosInfo(posInfo);
            }
        }
    }

    /**
     * Adds a new row to the table with position information.
     *
     * @param posInfo posInfoMap object containing the data for the new row.
     */
    private void addRowPosInfo(PosInfo posInfo) {
        Object[] rowData = {
            posInfo.getUniqueId(),
            posInfo.getPos().toString(NumberFormat.getNumberInstance(Locale.US)),
            posInfo.getFront().toString(NumberFormat.getNumberInstance(Locale.US)),};

        posInfoModel.addRow(rowData);
        int rowIndex = posInfoModel.getRowCount() - 1;
        playerInfoMap.put(posInfo.getUniqueId(), rowIndex);
    }

    /**
     * Updates an existing row in the table with new position information.
     *
     * @param rowIndex Index of the row to be updated.
     * @param posInfo posInfoMap object containing the updated data.
     */
    private void updateRowPosInfo(int rowIndex, PosInfo posInfo) {
        posInfoModel.setValueAt(posInfo.getUniqueId(), rowIndex, 0);
        posInfoModel.setValueAt((posInfo.getPos()), rowIndex, 1);
        posInfoModel.setValueAt((posInfo.getFront()), rowIndex, 2);
    }

    /**
     * Upserts client information into the table.
     *
     * @param clientInfos Array of clientInfoMap objects to be upserted.
     */
    public void upsertClientInfo(ClientInfo[] clientInfos) {
        for (ClientInfo clientInfo : clientInfos) {
            if (clientInfoMap.containsKey(clientInfo.getUniqueId())) {
                int rowIndex = clientInfoMap.get(clientInfo.getUniqueId());
                updateRowClentInfo(rowIndex, clientInfo);
            } else {
                addRowClientInfo(clientInfo);
            }
        }
    }

    /**
     * Adds a new row to the table with client information.
     *
     * @param clientInfo clientInfoMap object containing the data for the new
     * row.
     */
    private void addRowClientInfo(ClientInfo clientInfo) {
        Object[] rowData = {
            clientInfo.getHostName(),
            clientInfo.getUniqueId(),
            clientInfo.getTimeToLive()
        };
        clientInfoModel.addRow(rowData);
        int rowIndex = clientInfoModel.getRowCount() - 1;
        clientInfoMap.put(clientInfo.getUniqueId(), rowIndex);
    }

    /**
     * Updates an existing row in the table with new client information.
     *
     * @param rowIndex Index of the row to be updated.
     * @param clientInfo clientInfoMap object containing the updated data.
     */
    private void updateRowClentInfo(int rowIndex, ClientInfo clientInfo) {
        clientInfoModel.setValueAt(clientInfo.getHostName(), rowIndex, 0);
        clientInfoModel.setValueAt(clientInfo.getUniqueId(), rowIndex, 1);
        clientInfoModel.setValueAt(clientInfo.getTimeToLive(), rowIndex, 2);
    }

    public JTextArea getConsole() {
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

        panelNetwork = new javax.swing.JPanel();
        lblLocalIP = new javax.swing.JLabel();
        tboxLocalIP = new javax.swing.JTextField();
        lblServerPort = new javax.swing.JLabel();
        spinServerPort = new javax.swing.JSpinner();
        btnStart = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        btnRestart = new javax.swing.JButton();
        panelWorld = new javax.swing.JPanel();
        lblLevelSize = new javax.swing.JLabel();
        cmbLevelSize = new javax.swing.JComboBox<>();
        lblWorldName = new javax.swing.JLabel();
        tboxWorldName = new javax.swing.JTextField();
        lblMapSeed = new javax.swing.JLabel();
        spinMapSeed = new javax.swing.JSpinner();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
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
        spConsole = new javax.swing.JScrollPane();
        console = new javax.swing.JTextArea();
        mainMenu = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Demolition Synergy Server");
        setMinimumSize(new java.awt.Dimension(1024, 576));
        setName("windowFrame"); // NOI18N
        setPreferredSize(new java.awt.Dimension(1280, 720));
        setSize(new java.awt.Dimension(1280, 720));
        getContentPane().setLayout(new java.awt.GridLayout(2, 2));

        panelNetwork.setBorder(javax.swing.BorderFactory.createTitledBorder("Network"));
        panelNetwork.setLayout(new java.awt.GridLayout(4, 1));

        lblLocalIP.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblLocalIP.setText("Local IP:");
        panelNetwork.add(lblLocalIP);

        tboxLocalIP.setText("127.0.0.1");
        panelNetwork.add(tboxLocalIP);

        lblServerPort.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblServerPort.setText("Server Port:");
        panelNetwork.add(lblServerPort);

        spinServerPort.setModel(new javax.swing.SpinnerNumberModel(13667, 13660, 13670, 1));
        panelNetwork.add(spinServerPort);

        btnStart.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        btnStart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/play.png"))); // NOI18N
        btnStart.setText("Start");
        btnStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartActionPerformed(evt);
            }
        });
        panelNetwork.add(btnStart);

        btnStop.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        btnStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/stop.png"))); // NOI18N
        btnStop.setText("Stop");
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
        btnRestart.setEnabled(false);
        btnRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRestartActionPerformed(evt);
            }
        });
        panelNetwork.add(btnRestart);

        getContentPane().add(panelNetwork);

        panelWorld.setBorder(javax.swing.BorderFactory.createTitledBorder("World"));
        panelWorld.setLayout(new java.awt.GridLayout(3, 6));

        lblLevelSize.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblLevelSize.setText("Level Size:");
        panelWorld.add(lblLevelSize);
        panelWorld.add(cmbLevelSize);

        lblWorldName.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblWorldName.setText("World Name:");
        panelWorld.add(lblWorldName);

        tboxWorldName.setText("My World");
        panelWorld.add(tboxWorldName);

        lblMapSeed.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblMapSeed.setText("Seed:");
        panelWorld.add(lblMapSeed);
        panelWorld.add(spinMapSeed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 157, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 108, Short.MAX_VALUE)
        );

        panelWorld.add(jPanel1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 157, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 108, Short.MAX_VALUE)
        );

        panelWorld.add(jPanel2);

        btnGenerate.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        btnGenerate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/new.png"))); // NOI18N
        btnGenerate.setText("Generate New");
        btnGenerate.setEnabled(false);
        panelWorld.add(btnGenerate);

        btnImport.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        btnImport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/import.png"))); // NOI18N
        btnImport.setText("Import World");
        btnImport.setEnabled(false);
        panelWorld.add(btnImport);

        btnExport.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        btnExport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/export.png"))); // NOI18N
        btnExport.setText("Export World");
        btnExport.setEnabled(false);
        panelWorld.add(btnExport);

        btnErase.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        btnErase.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/trash.png"))); // NOI18N
        btnErase.setText("Erase World");
        btnErase.setEnabled(false);
        panelWorld.add(btnErase);

        getContentPane().add(panelWorld);

        panelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder("Info"));
        panelInfo.setLayout(new java.awt.BorderLayout());

        gameTimeText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gameTimeText.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rs/alexanderstojanovich/evgds/resources/day-night-cycle.png"))); // NOI18N
        gameTimeText.setText("Day 1 00:00:00");
        gameTimeText.setBorder(javax.swing.BorderFactory.createTitledBorder("Game Time"));
        panelInfo.add(gameTimeText, java.awt.BorderLayout.PAGE_START);

        progBar.setBorder(javax.swing.BorderFactory.createTitledBorder("Progress:"));
        progBar.setStringPainted(true);
        panelInfo.add(progBar, java.awt.BorderLayout.PAGE_END);

        clientInfoTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Hostname", "PlayerId", "Time to Live"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class
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
        clientInfoTbl.setShowGrid(true);
        spClientInfo.setViewportView(clientInfoTbl);

        tabPaneInfo.addTab("Client", spClientInfo);

        playerInfoTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PlayerId", "PlayerName", "Color", "Texture"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        playerInfoTbl.setShowGrid(true);
        spPlayerInfo.setViewportView(playerInfoTbl);

        tabPaneInfo.addTab("Player Info", spPlayerInfo);

        posInfoTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PlayerId", "Pos", "Front"
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
        posInfoTbl.setShowGrid(true);
        spPosInfo.setViewportView(posInfoTbl);

        tabPaneInfo.addTab("Position", spPosInfo);

        panelInfo.add(tabPaneInfo, java.awt.BorderLayout.CENTER);

        getContentPane().add(panelInfo);

        panelConsole.setBorder(javax.swing.BorderFactory.createTitledBorder("Console"));
        panelConsole.setLayout(new java.awt.BorderLayout());

        console.setEditable(false);
        console.setColumns(20);
        console.setRows(5);
        spConsole.setViewportView(console);

        panelConsole.add(spConsole, java.awt.BorderLayout.CENTER);

        getContentPane().add(panelConsole);

        jMenu1.setText("File");

        jMenuItem1.setText("Exit");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        mainMenu.add(jMenu1);

        jMenu2.setText("Edit");
        mainMenu.add(jMenu2);

        setJMenuBar(mainMenu);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartActionPerformed
        // TODO add your handling code here:
        gameObject.start();
        setEnabledComponents(this.panelWorld, true);
        setEnabledComponents(this.panelInfo, true);
    }//GEN-LAST:event_btnStartActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        // TODO add your handling code here:        
        setEnabledComponents(this.panelWorld, false);
        setEnabledComponents(this.panelInfo, false);
        gameObject.gameServer.stopServer();
        gameObject.clearEverything();
    }//GEN-LAST:event_btnStopActionPerformed

    private void btnRestartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRestartActionPerformed
        // TODO add your handling code here:
        gameObject.gameServer.stopServer();
        gameObject.clearEverything();

        gameObject.start();
    }//GEN-LAST:event_btnRestartActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnErase;
    private javax.swing.JButton btnExport;
    private javax.swing.JButton btnGenerate;
    private javax.swing.JButton btnImport;
    private javax.swing.JButton btnRestart;
    private javax.swing.JButton btnStart;
    private javax.swing.JButton btnStop;
    private javax.swing.JTable clientInfoTbl;
    private javax.swing.JComboBox<String> cmbLevelSize;
    private javax.swing.JTextArea console;
    private javax.swing.JLabel gameTimeText;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel lblLevelSize;
    private javax.swing.JLabel lblLocalIP;
    private javax.swing.JLabel lblMapSeed;
    private javax.swing.JLabel lblServerPort;
    private javax.swing.JLabel lblWorldName;
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
    private javax.swing.JSpinner spinMapSeed;
    private javax.swing.JSpinner spinServerPort;
    private javax.swing.JTabbedPane tabPaneInfo;
    private javax.swing.JTextField tboxLocalIP;
    private javax.swing.JTextField tboxWorldName;
    // End of variables declaration//GEN-END:variables
}

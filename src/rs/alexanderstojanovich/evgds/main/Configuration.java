/* 
 * Copyright (C) 2020 Alexander Stojanovich <coas91@rocketmail.com>
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import rs.alexanderstojanovich.evgds.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Configuration {

//    private int monitor = 0;
//    private int fpsCap = 100;
    private int width = 640;
    private int height = 360;
//    private boolean fullscreen = false;
    private DSLogger.DSLogLevel logLevel = DSLogger.DSLogLevel.ERR;
    private boolean logToFile = false;
    private int blockDynamicSize = 50;
    private int textDynamicSize = 10;
    private int textureSize = 512;
    private float gameTimeMultiplier = 1.0f;
    private int optimizationPasses = 8;
    private double gameTicks = 0.0;
    private int blocksPerRun = 1000;
    private int ticksPerUpdate = Game.TPS_TWO;

    private String localIP = "127.0.0.1"; // local IP address used to host the server on (local) machine
    private String serverIP = ""; // server ip used to connect client to server
    private int serverPort = 13667; // used in conjunction with local IP
    private int clientPort = 13667; // used in conjunction with server IP

    private boolean useBakGuid = false;

    private static final String CONFIG_PATH = "dsynergy.ini";

    private static Configuration instance;

    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    private Configuration() {

    }

    // reads configuration from the .ini file
    public void readConfigFile() {
        File cfg = new File(CONFIG_PATH);
        if (cfg.exists()) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(cfg));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue; // ignore whole line comments
                    }

                    // ignore inline comments
                    if (line.contains("#")) {
                        line = line.substring(0, line.indexOf("#"));
                    }

                    // replace all white space chars with empty string
                    String[] words = line.replaceAll("\\s", "")
                            .trim()
                            .split("=");
                    int number;
                    float val;
                    String str;
                    if (words.length == 2) {
                        switch (words[0].toLowerCase()) {
                            case "monitor":
//                                monitor = Integer.parseInt(words[1]);
//                                break;
//                            case "fpscap":
//                                fpsCap = Integer.parseInt(words[1]);
//                                break;
                            case "width":
                                width = Integer.parseInt(words[1]);
                                break;
                            case "height":
                                height = Integer.parseInt(words[1]);
                                break;
//                            case "fullscreen":
//                                fullscreen = Boolean.parseBoolean(words[1].toLowerCase());
//                                break;
                            case "loglevel":
                                int logLevelInt = Integer.parseInt(words[1].toLowerCase());
                                logLevel = DSLogger.DSLogLevel.values()[logLevelInt];
                                break;
                            case "logtofile":
                                logToFile = Boolean.parseBoolean(words[1].toLowerCase());
                                break;
                            case "blockdynamicsize":
                                number = Integer.parseInt(words[1]);
                                if (number >= 3 && number <= 5000) {
                                    blockDynamicSize = number;
                                }
                                break;
                            case "textdynamicsize":
                                number = Integer.parseInt(words[1]);
                                if (number >= 3 && number <= 1000) {
                                    textDynamicSize = number;
                                }
                                break;
                            case "optimizationpasses":
                                number = Integer.parseInt(words[1]);
                                if (number != 0 && (number & (number - 1)) == 0 && number <= 64) {
                                    optimizationPasses = number;
                                }
                                break;
                            case "texturesize":
                                number = Integer.parseInt(words[1]);
                                // if tex size is a non-zero power of two
                                if (number != 0 && (number & (number - 1)) == 0 && number <= 4096) {
                                    textureSize = number;
                                }
                                break;
                            case "gameticks":
                                val = Float.parseFloat(words[1]);
                                if (val >= 0.0f) {
                                    gameTicks = val;
                                }
                                break;
                            case "gametimemultiplier":
                                val = Float.parseFloat(words[1]);
                                if (val >= 0.0f && val <= 5.0f) {
                                    gameTimeMultiplier = val;
                                }
                                break;
                            case "blocksperrun":
                                number = Integer.parseInt(words[1]);
                                // block per cache loading run
                                if (number > 0 && number <= 25000) {
                                    blocksPerRun = number;
                                }
                                break;
                            case "ticksperupdate":
                                number = Integer.parseInt(words[1]);
                                // block per cache loading run
                                if (number > 0 && number <= 2) {
                                    ticksPerUpdate = number;
                                }
                                break;
                            case "localip":
                                localIP = words[1];
                                break;
                            case "serverip":
                                serverIP = words[1];
                                break;
                            case "serverport":
                                number = Integer.parseInt(words[1]);
                                // server port is constrained on local machine
                                if (number >= 13660 && number <= 13669) {
                                    serverPort = number;
                                }
                                break;
                            case "clientport":
                                number = Integer.parseInt(words[1]);
                                // clent port is free
                                if (number > 0) {
                                    clientPort = number;
                                }
                                break;
                            case "usebakguid":
                                useBakGuid = Boolean.parseBoolean(words[1].toLowerCase());
                                break;
                        }
                    }
                }
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException ex) {
                        DSLogger.reportFatalError(ex.getMessage(), ex);
                    }
                }
            }
        }
    }

    // writes configuration to the .ini file (on game exit)
    public void writeConfigFile() {
        File cfg = new File(CONFIG_PATH);
        if (cfg.exists()) {
            cfg.delete();
        }
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(cfg);
//            pw.println("# Monitor (0 - Window; !=0 available monitors)");
//            pw.println("Monitor = " + monitor);
//            pw.println("# Maximum framerate. Depedends on VSync.");
//            pw.println("FPSCap = " + fpsCap);
            pw.println("Width = " + width);
            pw.println("Height = " + height);
//            pw.println("Fullscreen = " + fullscreen);
//            pw.println("# Maximum Framerate set to refresh rate if is enabled");
            pw.println("# Log Level {ERR=0(default), DEBUG=1, ALL=2}");
            pw.println("LogLevel = " + logLevel.ordinal());
            pw.println("# If true generate log file, otherwise print only to console. Used in conjuction with log level.");
            pw.println("LogToFile = " + logToFile);
            pw.println("BlockDynamicSize = " + blockDynamicSize);
            pw.println("TextDynamicSize = " + textDynamicSize);
            pw.println("OptimizationPasses = " + optimizationPasses);
            pw.println("# Texture size. Must be power of two, non-zero and lesser or equal than 4096.");
            pw.println("TextureSize = " + textureSize);
            pw.println("# Game Ticks (decimal). Must be greater or equal zero");
            pw.println("GameTicks = " + gameTicks);
            pw.println("# Game Time (decimal). Must be metween (0, 5]");
            pw.println("GameTimeMultiplier = " + gameTimeMultiplier);
            pw.println("# Block number load per cache run. Must be between (0, 25000]");
            pw.println("BlocksPerRun = " + blocksPerRun);
            pw.println("# Ticks per update (1 - FLUID, 2 - EFFICIENT)");
            pw.println("TicksPerUpdate = " + ticksPerUpdate);
            pw.println("# Local IP address used to host the server on (local) machine. Used with server port");
            pw.println("LocalIP = " + localIP);
            pw.println("# Preferred game server (local or public IP address) for client to connect. Used in conjuction with client port");
            pw.println("ServerIP = " + serverIP);
            pw.println("# Preferred game server port (to run the server). Must be in range 13660-13669");
            pw.println("ServerPort = " + serverPort);
            pw.println("# Client port set to connect to game server. Varying");
            pw.println("ClientPort = " + clientPort);
            pw.println("# Use backup guid. Testing purposes.");
            pw.println("UseBakGuid = " + useBakGuid);
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

//    public int getMonitor() {
//        return monitor;
//    }
//    public int getFpsCap() {
//        return fpsCap;
//    }
//
//    public void setFpsCap(int fpsCap) {
//        this.fpsCap = fpsCap;
//    }
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

//    public boolean isFullscreen() {
//        return fullscreen;
//    }
//
//    public void setFullscreen(boolean fullscreen) {
//        this.fullscreen = fullscreen;
//    }
    public DSLogger.DSLogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(DSLogger.DSLogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isLogToFile() {
        return logToFile;
    }

    public void setLogToFile(boolean logToFile) {
        this.logToFile = logToFile;
    }

    public int getBlockDynamicSize() {
        return blockDynamicSize;
    }

    public int getTextDynamicSize() {
        return textDynamicSize;
    }

    public int getTextureSize() {
        return textureSize;
    }

    public float getGameTimeMultiplier() {
        return gameTimeMultiplier;
    }

    public double getGameTicks() {
        return gameTicks;
    }

    public int getBlocksPerRun() {
        return blocksPerRun;
    }

    public int getOptimizationPasses() {
        return optimizationPasses;
    }

    public int getTicksPerUpdate() {
        return ticksPerUpdate;
    }

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getClientPort() {
        return clientPort;
    }

    public String getLocalIP() {
        return localIP;
    }

    public void setLocalIP(String localIP) {
        this.localIP = localIP;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public boolean isUseBakGuid() {
        return useBakGuid;
    }

}

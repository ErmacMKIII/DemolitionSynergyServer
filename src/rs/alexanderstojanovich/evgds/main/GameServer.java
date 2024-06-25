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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evgds.level.LevelActors;
import rs.alexanderstojanovich.evgds.net.ClientInfo;
import rs.alexanderstojanovich.evgds.net.DSMachine;
import rs.alexanderstojanovich.evgds.util.DSLogger;

/**
 * Demolition Synergy Game Server
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameServer implements DSMachine, Runnable {

    public static final int TIME_TO_LIVE = 60;
    public static final int FAIL_ATTEMPT_MAX = 10;
    public static final int TOTAL_FAIL_ATTEMPT_MAX = 3000;

    public static int TotalFailedAttempts = 0;

    public final Configuration config = Configuration.getInstance();
    protected String worldName = "My World";
    public static int DEFAULT_PORT = 13667;
    protected String localIP = config.getLocalIP();
    protected int port = config.getServerPort();

    protected static final int MAX_CLIENTS = 16;

    protected DatagramSocket endpoint;
    /**
     * Client list with IPs (or hostnames)
     */
    public final IList<String> clients = new GapList<>();
    protected final GameObject gameObject;

    protected volatile boolean running = false;
    protected boolean shutDownSignal = false;
    protected final int version = GameObject.VERSION;
    protected final int timeout = 120 * 1000; // 2 minutes

    /**
     * Magic bytes of End-of-Stream
     */
    public static final byte[] EOS = {(byte) 0xAB, (byte) 0xCD, (byte) 0x0F, (byte) 0x15}; // 4 Bytes

    /**
     * Server worker
     */
    public final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();

//    /**
//     * Server Task worker (handles heavy tasks)
//     */
//    public final ExecutorService serverTaskExecutor = Executors.newFixedThreadPool(GameServer.MAX_CLIENTS);
    /**
     * Who is Client hostname <==> Player UniqueId
     */
    public final LinkedHashMap<String, String> whoIsMap = new LinkedHashMap<>();

    /**
     * Who is Client hostname <==> Time to live (int)
     */
    public final LinkedHashMap<String, Integer> timeToLiveMap = new LinkedHashMap<>();

    /**
     * Failed hosts with number of attempts
     */
    public final LinkedHashMap<String, Integer> failedAttempts = new LinkedHashMap<>();

    /**
     * Blacklisted hosts with number of attempts
     */
    public final IList<String> blacklist = new GapList<>();

    /**
     * Kick list hosts with number of attempts
     */
    public final IList<String> kicklist = new GapList<>();

    /**
     * Create new game server (UDP protocol based)
     *
     * @param gameObject game object
     */
    public GameServer(GameObject gameObject) {
        this.gameObject = gameObject;
    }

    /**
     * Schedule timer task to check clients (Time-To-Live)
     */
    public final Timer timerClientChk = new Timer("Server Utils");

    /**
     * Create new game server (UDP protocol based)
     *
     * @param gameObject game object
     * @param name world name
     */
    public GameServer(GameObject gameObject, String name) {
        this.gameObject = gameObject;
        this.worldName = name;
    }

    /**
     * Start endpoint.
     */
    public void startServer() {
        this.shutDownSignal = false;

        // Schedule timer task to decrease Time-to-live for clients
        TimerTask task1 = new TimerTask() {
            @Override
            public void run() {
                GapList<String> clientKeys = new GapList<>(timeToLiveMap.keySet());
                clientKeys.forEach((String key) -> {
                    timeToLiveMap.compute(key, (String t, Integer u) -> {
                        if (u == null || u <= 1 || kicklist.contains(key)) {
                            GameServer.this.clients.remove(key);
                            String uniqueId = GameServer.this.whoIsMap.remove(key);
                            GameServer.this.gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + GameServer.this.worldName + " - Player Count: " + (GameServer.this.clients.size()));
                            if (uniqueId != null) {
                                GameServer.performCleanUp(GameServer.this.gameObject, uniqueId, u <= 1);
                                whoIsMap.remove(key);
                            }

                            kicklist.remove(key);

                            return null; // Remove the key from timeToLiveMap
                        } else {
                            return u - 1; // Decrement TTL
                        }
                    });
                    // Log the new TTL value
//                    Integer newTimeToLive = timeToLiveMap.get(key);
//                    DSLogger.reportInfo("TimeToLive=" + newTimeToLive, null);
                });
            }
        };
        timerClientChk.scheduleAtFixedRate(task1, 1000L, 1000L);

        serverExecutor.execute(this);

        DSLogger.reportInfo(String.format("Commencing start of Game Server. Game Server will start on %s:%d", localIP, port), null);
    }

    /**
     * Stop running server endpoint. Server would have to be start again.
     */
    public void stopServer() {
        if (running) {
            // Attempt to disconnect clients            
            this.endpoint.close();

            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE);
            this.shutDownSignal = true;
            whoIsMap.clear();
            timeToLiveMap.clear();
            clients.clear();
        }
    }

    /**
     * Shut down execution service(s). Server is not available anymore.
     */
    public void shutDown() {
        this.serverExecutor.shutdown();
//        this.serverTaskExecutor.shutdown();
        this.timerClientChk.cancel();
    }

    /**
     * Assert that failure has happen and client timed out or is about to be
     * rejected. In other words client will fail the test.
     *
     * @param failedHostName client who is submit to test
     */
    public void assertTstFailure(String failedHostName) {
        TotalFailedAttempts++;
        boolean contains = this.failedAttempts.containsKey(failedHostName);
        if (!contains) {
            this.failedAttempts.put(failedHostName, 1);
        } else {
            Integer failAttemptNum = this.failedAttempts.get(failedHostName);
            failAttemptNum++;

            // Blacklisting (equals ban)
            if (failAttemptNum >= FAIL_ATTEMPT_MAX && !blacklist.contains(failedHostName)) {
                blacklist.add(failedHostName);
                gameObject.WINDOW.writeOnConsole((String.format("Client (%s) is now blacklisted!", failedHostName)));
                DSLogger.reportWarning(String.format("Game Server (%s) is now blacklisted!", failedHostName), null);
            }

            // Too much failed attempts, endpoint is vulnerable .. try to shut down
            if (TotalFailedAttempts >= TOTAL_FAIL_ATTEMPT_MAX) {
                gameObject.WINDOW.writeOnConsole((String.format("Game Server (%s:%d) status critical! Trying to shut down!", this.localIP, this.port)));
                DSLogger.reportWarning(String.format("Game Server (%s:%d) status critical! Trying to shut down!", this.localIP, this.port), null);
                shutDownSignal = true;
            }

            this.failedAttempts.replace(failedHostName, failAttemptNum);
        }
    }

    /**
     * Server loop
     */
    @Override
    public void run() {
        running = true;
        try {
            // Bind the endpoint socket to a 'wildcard' IP address amd given port
            endpoint = new DatagramSocket(port, InetAddress.getByName(localIP));
            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + worldName + " - Player Count: " + (clients.size()));
            DSLogger.reportInfo(String.format("Game Server (%s:%d) started!", this.localIP, this.port), null);
            gameObject.WINDOW.writeOnConsole((String.format("Game Server (%s:%d) started!", this.localIP, this.port)));
        } catch (IOException ex) {
            DSLogger.reportError("Cannot create Game Server!", ex);
            gameObject.WINDOW.writeOnConsole(("Cannot create Game Server!"));
            DSLogger.reportError(ex.getMessage(), ex);
            shutDownSignal = true;
        }
        // Accept incoming connections and handle them
        while (!shutDownSignal) {
            try {
                GameServerProcessor.Result procResult = GameServerProcessor.process(this, endpoint);
                final String msg;
                switch (procResult.status) {
                    case INTERNAL_ERROR:
                        msg = String.format("Server %s %s error!", procResult.client, procResult.message);
                        DSLogger.reportError(msg, null);
                        gameObject.WINDOW.writeOnConsole(msg);
                        break;
                    case CLIENT_ERROR:
                        assertTstFailure(procResult.client);
                        msg = String.format("Client %s %s error!", procResult.client, procResult.message);
                        DSLogger.reportError(msg, null);
                        gameObject.WINDOW.writeOnConsole(msg);
                        if (blacklist.contains(procResult.client)) {
                            DSLogger.reportWarning(msg, null);
                            gameObject.WINDOW.writeOnConsole(msg);
                        }
                        break;
                    default:
                    case OK:
                        timeToLiveMap.replace(procResult.client, GameServer.TIME_TO_LIVE);
                        msg = String.format("OK %s %s", procResult.client, procResult.message);
                        DSLogger.reportInfo(msg, null);
                        gameObject.WINDOW.writeOnConsole(msg);
                        break;
                }
            } catch (Exception ex) {
                DSLogger.reportError("Server error: " + ex.getMessage(), ex);
            }
        }

        if (endpoint != null && !endpoint.isClosed()) {
            endpoint.close(); // Handle exceptions
        }
        shutDownSignal = true;

        clients.clear();
        running = false;
        DSLogger.reportInfo("Game Server finished!", null);
        gameObject.WINDOW.writeOnConsole("Game Server finished!");
    }

    /**
     * Perform clean up after player has disconnected or lost connection.
     *
     * @param gameObject game object
     * @param uniqueId player unique id (which was registered)
     * @param isError was client disconnected with error (timed out)
     */
    public static void performCleanUp(GameObject gameObject, String uniqueId, boolean isError) {
        LevelActors levelActors = gameObject.game.gameObject.levelContainer.levelActors;

        levelActors.otherPlayers.removeIf(ply -> ply.uniqueId.equals(uniqueId));
        DSLogger.reportInfo(String.format(isError ? "Player %s timed out." : "Player %s disconnected.", uniqueId), null);
        gameObject.WINDOW.writeOnConsole(String.format(isError ? "Player %s timed out." : "Player %s disconnected.", uniqueId));
    }

    public ClientInfo[] getClientInfo() {
        ClientInfo[] result = new ClientInfo[clients.size()];
        int index = 0;
        for (String cli : clients) {
            ClientInfo ci = new ClientInfo(cli, whoIsMap.getOrDefault(cli, "N/A"), timeToLiveMap.getOrDefault(cli, -1));
            result[index++] = ci;
        }

        return result;
    }

    public static void kickPlayer(GameServer gameServer, String client) {
        if (gameServer.clients.contains(client) && !gameServer.kicklist.contains(client)) {
            gameServer.kicklist.add(client);
        }
    }

    public String getWorldName() {
        return worldName;
    }

    public int getPort() {
        return port;
    }

    public DatagramSocket getEndpoint() {
        return endpoint;
    }

    public IList<String> getClients() {
        return clients;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public boolean isShutDownSignal() {
        return shutDownSignal;
    }

    public void setShutDownSignal(boolean shutDownSignal) {
        this.shutDownSignal = shutDownSignal;
    }

    @Override
    public MachineType getMachineType() {
        return MachineType.DSSERVER;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setEndpoint(DatagramSocket endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public int getTimeout() {
        return timeout;
    }

    public ExecutorService getServerExecutor() {
        return serverExecutor;
    }

    public LinkedHashMap<String, String> getWhoIsMap() {
        return whoIsMap;
    }

    public LinkedHashMap<String, Integer> getFailedAttempts() {
        return failedAttempts;
    }

    public IList<String> getBlacklist() {
        return blacklist;
    }

    public Timer getTimerClientChk() {
        return timerClientChk;
    }

    public void setLocalIP(String localIP) {
        this.localIP = localIP;
    }

    public String getLocalIP() {
        return localIP;
    }

    public IList<String> getKicklist() {
        return kicklist;
    }

}

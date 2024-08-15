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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.mina.core.session.IoSession;

import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;

import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;

import rs.alexanderstojanovich.evgds.level.LevelActors;
import rs.alexanderstojanovich.evgds.net.ClientInfo;
import rs.alexanderstojanovich.evgds.net.DSMachine;
import rs.alexanderstojanovich.evgds.util.DSLogger;

/**
 * Demolition Synergy Game Server
 *
 * This class represents a UDP-based game server for Demolition Synergy. It
 * manages client connections, game sessions, and server operations.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameServer implements DSMachine, Runnable {

    /**
     * Time-to-live for clients in seconds
     */
    public static final int TIME_TO_LIVE = 120;

    /**
     * Maximum failed attempts allowed per client
     */
    public static final int FAIL_ATTEMPT_MAX = 10;

    /**
     * Total maximum failed attempts allowed for the server
     */
    public static final int TOTAL_FAIL_ATTEMPT_MAX = 3000;

    /**
     * Total failed attempts counter
     */
    public static int TotalFailedAttempts = 0;

    /**
     * Configuration instance
     */
    public final Configuration config = Configuration.getInstance();

    /**
     * Default world name
     */
    protected String worldName = "My World";

    /**
     * Default server port
     */
    public static int DEFAULT_PORT = 13667;

    /**
     * Local IP address
     */
    protected String localIP = config.getLocalIP();

    /**
     * Server port
     */
    protected int port = config.getServerPort();

    /**
     * Maximum number of clients allowed
     */
    protected static final int MAX_CLIENTS = 16;

    /**
     * Endpoint address
     */
    protected InetSocketAddress endpoint;

    /**
     * List of connected clients
     */
    public final IList<ClientInfo> clients = new GapList<>();

    /**
     * Game object instance
     */
    protected final GameObject gameObject;

    /**
     * Flag indicating if the server is running
     */
    protected volatile boolean running = false;

    /**
     * Shutdown signal to stop the server
     */
    protected boolean shutDownSignal = false;

    /**
     * Game version
     */
    protected final int version = GameObject.VERSION;

    /**
     * Timeout duration in milliseconds
     */
    protected final int timeout = 120 * 1000; // 2 minutes

    /**
     * Timeout to close session (await) after client said "GOODBYE" to
     * disconnect
     */
    public static final long GOODBYE_TIMEOUT = 15000L;

    /**
     * Magic bytes of End-of-Stream
     */
    public static final byte[] EOS = {(byte) 0xAB, (byte) 0xCD, (byte) 0x0F, (byte) 0x15}; // 4 Bytes

    /**
     * Executor service for server tasks
     */
    public final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();

    /**
     * List of blacklisted hosts
     */
    public final IList<String> blacklist = new GapList<>();

    /**
     * List of kicked clients
     */
    public final IList<String> kicklist = new GapList<>();

    /**
     * Timer for checking client timeouts
     */
    public final Timer timerClientChk = new Timer("Server Utils");

    /**
     * UDP acceptor and session settings
     */
    protected DatagramAcceptor acceptor;

    /**
     * Constructs a new GameServer instance with a given GameObject.
     *
     * @param gameObject The GameObject instance associated with this server.
     */
    public GameServer(GameObject gameObject) {
        this.gameObject = gameObject;
    }

    /**
     * Constructs a new GameServer instance with a given GameObject and world
     * name.
     *
     * @param gameObject The GameObject instance associated with this server.
     * @param name The name of the game world.
     */
    public GameServer(GameObject gameObject, String name) {
        this.gameObject = gameObject;
        this.worldName = name;
    }

    /**
     * Starts the game server endpoint.
     *
     * This method initializes necessary resources and starts the server loop.
     */
    public void startServer() {
        this.shutDownSignal = false;

        // Schedule timer task to decrease Time-to-live for clients
        TimerTask task1 = new TimerTask() {
            @Override
            public void run() {
                // Decrease time-to-live for each client and remove expired clients
                clients.forEach((ClientInfo client) -> {
                    client.timeToLive--;
                    if (client.timeToLive <= 0 || kicklist.contains(client.uniqueId)) {
                        kicklist.remove(client.uniqueId);
                        performCleanUp(gameObject, client.uniqueId, client.timeToLive <= 0);
                    }
                });
                clients.removeIf(cli -> cli.timeToLive <= 0 || kicklist.contains(cli.uniqueId));

                // Update server window title with current player count
                GameServer.this.gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + GameServer.this.worldName + " - Player Count: " + (GameServer.this.clients.size()));
            }
        };
        timerClientChk.scheduleAtFixedRate(task1, 1000L, 1000L);

        // Start the server main loop in a separate thread
        serverExecutor.execute(this);

        // Log server start information
        DSLogger.reportInfo(String.format("Commencing start of Game Server. Game Server will start on %s:%d", localIP, port), null);
    }

    /**
     * Stops the running game server endpoint.
     *
     * This method kicks all connected players and shuts down the server.
     */
    public void stopServer() {
        if (running) {
            // Kick all players
            clients.forEach(cli -> GameServer.kickPlayer(gameObject.gameServer, cli.uniqueId));

            // Reset server window title
            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE);
            this.shutDownSignal = true;

            // close session(s) & acceptor
            acceptor.setCloseOnDeactivation(true);
            for (IoSession ss : acceptor.getManagedSessions().values()) {
                try {
                    ss.closeNow().await(GameServer.GOODBYE_TIMEOUT);
                } catch (InterruptedException ex) {
                    DSLogger.reportError("Unable to close session!", ex);
                    DSLogger.reportError(ex.getMessage(), ex);
                }
            }
            acceptor.unbind();
            acceptor.dispose();

            // Clear client list and finalize server shutdown
            clients.clear();
            running = false;

            // Log server shutdown completion
            DSLogger.reportInfo("Game Server finished!", null);
            gameObject.WINDOW.writeOnConsole("Game Server finished!");
        }
    }

    /**
     * Shuts down the server completely.
     *
     * This method stops all execution services and cancels the client check
     * timer.
     */
    public void shutDown() {
        this.serverExecutor.shutdown();
        this.timerClientChk.cancel();
    }

    /**
     * Asserts that a failure has occurred and handles client timeouts or
     * rejections.
     *
     * @param failedHostName The hostname of the client that failed the test.
     * @param failedGuid The unique ID of the player associated with the client.
     */
    public void assertTstFailure(String failedHostName, String failedGuid) {
        // Filter clients who failed the test
        ClientInfo filtered = clients.getIf(client -> client.hostName.equals(failedHostName) && client.uniqueId.equals(failedGuid));

        // Blacklist the client if they exceeded maximum failed attempts
        if (filtered != null && ++filtered.failedAttempts >= FAIL_ATTEMPT_MAX && !blacklist.contains(failedHostName)) {
            blacklist.add(failedHostName);
            gameObject.WINDOW.writeOnConsole((String.format("Client (%s) is now blacklisted!", failedHostName)));
            DSLogger.reportWarning(String.format("Game Server (%s) is now blacklisted!", failedHostName), null);
        }

        // Shut down the server if total failed attempts threshold is exceeded
        if (++TotalFailedAttempts >= TOTAL_FAIL_ATTEMPT_MAX) {
            gameObject.WINDOW.writeOnConsole((String.format("Game Server (%s:%d) status critical! Trying to shut down!", this.localIP, this.port)));
            DSLogger.reportWarning(String.format("Game Server (%s:%d) status critical! Trying to shut down!", this.localIP, this.port), null);
            shutDownSignal = true;
        }
    }

    /**
     * Main server loop for handling incoming connections and processing client
     * requests.
     */
    @Override
    public void run() {
        running = true;
        try {
            // Bind the endpoint socket to a specific IP address and port
            endpoint = new InetSocketAddress(InetAddress.getByName(localIP), port);

            // Configure the UDP acceptor and session settings
            acceptor = new NioDatagramAcceptor();
            DatagramSessionConfig sessionConfig = acceptor.getSessionConfig();
            sessionConfig.setReuseAddress(true);

            // Set the handler for incoming messages
            GameServerProcessor processor = new GameServerProcessor(GameServer.this);
            acceptor.setHandler(processor);
            acceptor.bind(endpoint);

            // Update server window title with current player count
            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + worldName + " - Player Count: " + (clients.size()));
            DSLogger.reportInfo(String.format("Game Server (%s:%d) started!", this.localIP, this.port), null);
            gameObject.WINDOW.writeOnConsole((String.format("Game Server (%s:%d) started!", this.localIP, this.port)));
        } catch (IOException ex) {
            // Handle server creation failure
            DSLogger.reportError("Cannot create Game Server!", ex);
            gameObject.WINDOW.writeOnConsole(("Cannot create Game Server!"));
            DSLogger.reportError(ex.getMessage(), ex);
            shutDownSignal = true;
        }
    }

    /**
     * Performs cleanup operations after a player has disconnected or lost
     * connection.
     *
     * @param gameObject The GameObject instance associated with the server.
     * @param uniqueId The unique ID of the player who disconnected.
     * @param isError Indicates if the disconnection was due to an error (timed
     * out).
     */
    public static void performCleanUp(GameObject gameObject, String uniqueId, boolean isError) {
        // Perform cleanup actions, such as removing the player from the game
        LevelActors levelActors = gameObject.game.gameObject.levelContainer.levelActors;
        levelActors.otherPlayers.removeIf(ply -> ply.uniqueId.equals(uniqueId));
        DSLogger.reportInfo(String.format(isError ? "Player %s timed out." : "Player %s disconnected.", uniqueId), null);
        gameObject.WINDOW.writeOnConsole(String.format(isError ? "Player %s timed out." : "Player %s disconnected.", uniqueId));
    }

    // Getters and setters for private fields
    /**
     * Retrieves an array of ClientInfo objects representing connected clients.
     *
     * @return An array of ClientInfo objects.
     */
    public ClientInfo[] getClientInfo() {
        ClientInfo[] result = new ClientInfo[clients.size()];
        clients.toArray(result);
        return result;
    }

    /**
     * Kicks a player from the server by adding them to the kick list.
     *
     * @param gameServer The GameServer instance managing the player.
     * @param playerGuid The unique ID of the player to kick.
     */
    public static void kickPlayer(GameServer gameServer, String playerGuid) {
        if (gameServer.clients.containsIf(client -> client.uniqueId.equals(playerGuid)) && !gameServer.kicklist.contains(playerGuid)) {
            gameServer.kicklist.add(playerGuid);
        }
    }

    // Getters and setters for private fields
    /**
     * Retrieves the name of the game world managed by the server.
     *
     * @return The name of the game world.
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Retrieves the port number on which the server is running.
     *
     * @return The server port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Retrieves the endpoint socket used by the server.
     *
     * @return The DatagramSocket endpoint.
     */
    public InetSocketAddress getEndpoint() {
        return endpoint;
    }

    /**
     * Retrieves the list of connected clients.
     *
     * @return The IList containing ClientInfo objects.
     */
    public IList<ClientInfo> getClients() {
        return clients;
    }

    /**
     * Retrieves the GameObject instance associated with the server.
     *
     * @return The GameObject instance.
     */
    public GameObject getGameObject() {
        return gameObject;
    }

    /**
     * Checks if the server shutdown signal is active.
     *
     * @return True if shutdown signal is active, false otherwise.
     */
    public boolean isShutDownSignal() {
        return shutDownSignal;
    }

    /**
     * Sets the server shutdown signal. All sessions are closed and acceptor is
     * unbound and disposed (resource deallocated).
     *
     * @param shutDownSignal True to activate shutdown signal, false otherwise.
     */
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

    public void setEndpoint(InetSocketAddress endpoint) {
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

    @Override
    public String getGuid() {
        return "*";
    }

}

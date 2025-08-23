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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evgds.level.LevelActors;
import rs.alexanderstojanovich.evgds.net.ClientInfo;
import rs.alexanderstojanovich.evgds.net.DSMachine;
import rs.alexanderstojanovich.evgds.net.DSObject;
import rs.alexanderstojanovich.evgds.net.Response;
import rs.alexanderstojanovich.evgds.net.ResponseIfc;
import rs.alexanderstojanovich.evgds.util.DSLogger;

/**
 * Demolition Synergy Game Server
 *
 * This class represents a UDP-based game server for Demolition Synergy. It
 * manages client connections, game sessions, and server operations.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
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
    public static final int TOTAL_FAIL_ATTEMPT_MAX = 1000;

    /**
     * Total failed attempts counter
     */
    public static int TotalFailedAttempts = 0;

    /**
     * Configuration instance
     */
    public static final Configuration config = Configuration.getInstance();

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
    protected static final int MAX_CLIENTS = config.getMaxClients();

    /**
     * Max total request per second
     */
    public static final int MAX_RPS = 2000; // Max Total Request Per Second

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
     * Server util helper (time to live etc.)
     */
    public ExecutorService serverHelperExecutor;

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
    public Timer timerClientChk;

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

        // Start server helper
        serverHelperExecutor = Executors.newSingleThreadExecutor();

        // Schedule timer task to decrease Time-to-live for clients
        timerClientChk = new Timer("Server Utils");
        TimerTask task1 = new TimerTask() {
            @Override
            public void run() {
                // Decrease time-to-live for each client and remove expired clients
                clients.immutableList().forEach((ClientInfo client) -> {
                    client.timeToLive--;
                    // if client is OK -- not timing out ; not on kicklist ; not abusing request per second
                    boolean timedOut = client.timeToLive <= 0;
                    boolean maxRPSReached = client.requestPerSecond > MAX_RPS;
                    // max reqest per second reached -> kick client
                    if (maxRPSReached) {
                        // issuing kick to the client (guid as data) ~ best effort if has not successful first time
                        // also adds to kick list
                        kickPlayer(client.uniqueId);
                    }

                    // timed out -> just clean up resources
                    if (timedOut) {
                        try {
                            // close session (with the client)
                            client.session.closeOnFlush().await(GameServer.GOODBYE_TIMEOUT);

                            // clean up server from client data
                            GameServer.performCleanUp(GameServer.this.gameObject, client.uniqueId, true);

                            // remove from kicklist he/she timed out
                            kicklist.remove(client.uniqueId);
                        } catch (InterruptedException ex) {
                            DSLogger.reportError(ex.getMessage(), ex);
                        }
                    }

                    // reset request per second (RPS)
                    client.requestPerSecond = 0;
                });

                // Remove kicked and timed out players
                clients.removeIf(cli -> cli.timeToLive <= 0 || kicklist.contains(cli.uniqueId));

                // Update server window title with current player count
                GameServer.this.gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + GameServer.this.worldName + " - Player Count: " + (GameServer.this.clients.size()));
            }
        };
        timerClientChk.scheduleAtFixedRate(task1, 1000L, 1000L);

        // Start the server main loop in a separate thread
        serverHelperExecutor.execute(this);

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
            // Send 'notification' that server ic shutting down..
            this.shutDownSignal = true;
            // Reset server window title
            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE);

            // Kick all players (and close their sessions internally)            
            clients.immutableList().forEach(cli -> kickPlayer(cli.uniqueId));

            // (Close session(s))
            acceptor.getManagedSessions().values().forEach(session -> session.closeNow());

            // Set a shutdown timeout on the acceptor itself
            acceptor.setCloseOnDeactivation(true);
            // Close acceptor without blocking            
            CompletableFuture.runAsync(() -> {
                acceptor.unbind(endpoint);
                acceptor.dispose();
            }
            );

            // Clear client list and finalize server shutdown
            clients.clear();

            // Stop server loop and helpers
            shutDown();
            running = false;

            // Log server shutdown completion
            DSLogger.reportInfo("Game Server finished!", null);
            gameObject.WINDOW.logMessage("Game Server finished!", Window.Status.INFO);
        }
    }

    /**
     * Shuts down the server completely.
     *
     * This method stops all execution services and cancels the client check
     * timer.
     */
    public void shutDown() {
        if (this.timerClientChk != null) {
            this.timerClientChk.cancel();
        }
        if (this.serverHelperExecutor != null) {
            this.serverHelperExecutor.shutdown();
        }
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
        ClientInfo filtered = clients.immutableList().getIf(client -> client.hostName.equals(failedHostName) && client.uniqueId.equals(failedGuid));

        // Blacklist the client if they exceeded maximum failed attempts
        if (filtered != null && ++filtered.failedAttempts >= FAIL_ATTEMPT_MAX && !blacklist.contains(failedHostName)) {
            blacklist.add(failedHostName);
            gameObject.WINDOW.logMessage((String.format("Client (%s) is now blacklisted!", failedHostName)), Window.Status.WARN);
            DSLogger.reportWarning(String.format("Game Server (%s) is now blacklisted!", failedHostName), null);
        }

        // Shut down the server if total failed attempts threshold is exceeded
        if (++TotalFailedAttempts >= TOTAL_FAIL_ATTEMPT_MAX) {
            gameObject.WINDOW.logMessage((String.format("Game Server (%s:%d) status critical! Trying to shut down!", this.localIP, this.port)), Window.Status.ERR);
            DSLogger.reportWarning(String.format("Game Server (%s:%d) status critical! Trying to shut down!", this.localIP, this.port), null);

            stopServer();
            gameObject.game.stop();
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
            gameObject.WINDOW.logMessage((String.format("Game Server (%s:%d) started!", this.localIP, this.port)), Window.Status.INFO);
        } catch (IOException ex) {
            // Handle server creation failure
            DSLogger.reportError("Cannot create Game Server!", ex);
            gameObject.WINDOW.logMessage(("Cannot create Game Server!"), Window.Status.ERR);
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
        gameObject.WINDOW.logMessage(String.format(isError ? "Player %s timed out." : "Player %s disconnected.", uniqueId), isError ? Window.Status.ERR : Window.Status.INFO);
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
     * Issue kick to the client. Client will be force to disconnect and it will
     * be cleaned up.
     *
     * @param playerGuid player guid (16 chars) to be kicked
     */
    public void kickPlayer(String playerGuid) {
        final ClientInfo clientInfo;
        if ((clientInfo = clients.getIf(cli -> cli.uniqueId.equals(playerGuid))) != null) {
            try {
                // issuing kick to the client (guid as data)
                ResponseIfc response = new Response(DSObject.NIL_ID, 0L, ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, "KICK");
                response.send(clientInfo.uniqueId, GameServer.this, clientInfo.session);

                // close session (with the client)
                clientInfo.session.closeOnFlush().await(GameServer.GOODBYE_TIMEOUT);
                // clean up server from client data
                GameServer.performCleanUp(gameObject, clientInfo.uniqueId, false);

                // add to kick list for later removal from client list
                kicklist.addIfAbsent(playerGuid);

                // remove from client list
                clients.removeIf(c -> c.uniqueId.equals(clientInfo.uniqueId));
            } catch (Exception ex) {
                DSLogger.reportError(String.format("Error during kick client %s !", clientInfo.uniqueId), ex);
                DSLogger.reportError(ex.getMessage(), ex);
            }
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

    public ExecutorService getServerHelperExecutor() {
        return serverHelperExecutor;
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

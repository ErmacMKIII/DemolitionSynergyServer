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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evgds.net.DSMachine;
import rs.alexanderstojanovich.evgds.util.DSLogger;

/**
 * DSynergy Game client. With multiplayer capabilities.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Game implements DSMachine {

    private static final Configuration config = Configuration.getInstance();

    public static final int TPS = 80; // TICKS PER SECOND GENERATED
    public static final int TPS_HALF = 40; // HALF OF TPS
    public static final int TPS_QUARTER = 20; // QUARTER OF TPS ~ 250 ms
    public static final int TPS_EIGHTH = 10; // EIGHTH OF TPS (Used for Chunk Operations) ~ 125 ms
    public static final int TPS_SIXTEENTH = 5; // EIGHTH OF TPS 

    public static final int TPS_ONE = 1; // One tick ~ 12.5 ms
    public static final int TPS_TWO = 2; // Two ticks ~ 25 ms (Used for Chunk Optimization) ~ default

    public static final double TICK_TIME = 1.0 / (double) TPS;

    public static final float AMOUNT = 0.0075f;
    private static int ups; // current handleInput per second    

    // if this is reach game will close without exception!
    public static final double CRITICAL_TIME = 30.0;
    public static final int AWAIT_TIME = 10; // 10 Seconds

    public static final String ROOT = "/";
    public static final String CURR = "./";
    public static final String RESOURCES_DIR = "/rs/alexanderstojanovich/evgds/resources/";

    public static final String DATA_ZIP = "dsynergy.zip";

    public static final String SCREENSHOTS = "screenshots";
    public static final String CACHE = "cache";

    public static final String INTRFACE_ENTRY = "intrface/";
    public static final String WEAPON_ENTRY = "weapons/";
    public static final String WORLD_ENTRY = "world/";
    public static final String EFFECTS_ENTRY = "effects/";
    public static final String SOUND_ENTRY = "sound/";
    public static final String CHARACTER_ENTRY = "character/";

    protected static double accumulator = 0.0;
    protected static double gameTicks = 0.0;
    protected final int version = GameObject.VERSION;

    public static enum Mode {
        FREE, SINGLE_PLAYER, MULTIPLAYER_HOST, MULTIPLAYER_JOIN, EDITOR
    };
    private static Mode currentMode = Mode.FREE;

    protected static boolean actionPerformed = false; // movement for all actors (critters)
    protected static boolean jumpPerformed = false; // jump for player
    protected static boolean causingCollision = false; // collision with solid environment (all critters)    
    protected boolean running = false;

    protected static final int DEFAULT_TIMEOUT = 30000; // 30 sec
    protected static final int DEFAULT_EXTENDED_TIMEOUT = 120000; // 2 minutes
    protected static final int DEFAULT_SHORTENED_TIMEOUT = 10000; // 10 seconds

//    protected int faultNum = 0;
//    protected static final int MAX_FAULTS = 3;
    /**
     * Magic bytes of End-of-Stream
     */
    public static final byte[] EOS = {(byte) 0xAB, (byte) 0xCD, (byte) 0x0F, (byte) 0x15}; // 4 Bytes

    /**
     * Connect to server stuff & endpoint
     */
    protected DatagramSocket serverEndpoint;
    protected InetAddress serverInetAddr = null;

    protected int timeout = DEFAULT_TIMEOUT;

    /**
     * Player alleged position on the game server
     */
    public final Vector3f playerServerPos = new Vector3f();

    /**
     * Game Loop Timer. Only updates time and small stuff.
     */
    public Timer gameLoopTimer = new Timer("Game-Loop");

    /**
     * Access to Game Engine.
     */
    public final GameObject gameObject;

    public int weaponIndex = 0;

    /**
     * Construct new game (client) view. Demolition Synergy client.
     *
     * @param gameObject game object
     */
    public Game(GameObject gameObject) {
        this.gameObject = gameObject;
    }

    /**
     * Updates game (client).
     *
     * @param deltaTime time interval between updates
     */
    public void update(double deltaTime) {
        gameObject.update((float) deltaTime);
    }

    /**
     * Starts the main loop. Main loop is called from main method. (From
     * GameObject indirectly)
     *
     * @throws java.lang.Exception if status is critical
     */
    public void go() throws Exception {
        this.running = true;
        Game.setCurrentMode(Mode.FREE);
        ups = 0;
        accumulator = 0.0;

        // gameTicks is progressive only ingame time
        gameTicks = config.getGameTicks();
        //----------------------------------------------------------------------
        // Schedule timer task to emulate game loop
        // Cuz primary task is time elapsing
        long[] time = {System.nanoTime(), 0L}; // lastTime, currTime in Array
        gameLoopTimer = new Timer("Game-Loop");
        TimerTask task0 = new TimerTask() {
            @Override
            public void run() {
                // assign current time
                time[1] = System.nanoTime();
                // retrieve delta time
                double deltaTime = (time[1] - time[0]) / 1E9d;
                // reassign so it is calc again in next interval
                time[0] = time[1];

                accumulator += deltaTime;
                gameTicks += deltaTime * Game.TPS;
                if (gameTicks >= Double.MAX_VALUE) {
                    gameTicks = 0.0;
                }

                // Detecting critical status
                if (ups == 0 && deltaTime > CRITICAL_TIME) {
                    DSLogger.reportFatalError("Game status critical!", null);
                    gameObject.WINDOW.stopServerAndUpdate();
                }

                while (accumulator >= TICK_TIME) {
                    // Update with fixed timestep (environment)
                    update(TICK_TIME);
                    ups++;
                    accumulator -= TICK_TIME;
                }
            }
        };
        gameLoopTimer.scheduleAtFixedRate(task0, (long) (0.5 * TICK_TIME * 1000L), (long) (0.5 * TICK_TIME * 1000L));

        this.running = false;
        DSLogger.reportDebug("Main loop ended.", null);
    }

    /**
     * Creates configuration from settings
     *
     * @param gameObject gameObject (contains binds)
     * @return Configuration cfg
     */
    public static Configuration makeConfig(GameObject gameObject) {
        Configuration cfg = Configuration.getInstance();
        cfg.setWidth(gameObject.WINDOW.getWidth());
        cfg.setHeight(gameObject.WINDOW.getHeight());
//        cfg.setFullscreen(gameObject.WINDOW.isFullscreen());
        cfg.setLocalIP(gameObject.gameServer.localIP);
        cfg.setServerPort(gameObject.gameServer.port);

        return cfg;
    }

    /**
     * Cancel the running game loop for dedicated server
     */
    public void stop() {
        if (gameLoopTimer != null) {
            gameLoopTimer.cancel();
        }
    }

    public static void setGameTicks(double gameTicks) {
        Game.gameTicks = gameTicks;
    }

    public static int getUps() {
        return ups;
    }

    public static void setUps(int ups) {
        Game.ups = ups;
    }

    public static double getAccumulator() {
        return accumulator;
    }

    public static Mode getCurrentMode() {
        return currentMode;
    }

    public static void setCurrentMode(Mode currentMode) {
        Game.currentMode = currentMode;
    }

    public static double getGameTicks() {
        return gameTicks;
    }

    public static boolean isChanged() {
        return actionPerformed;
    }

    public static boolean isActionPerformed() {
        return actionPerformed;
    }

    public static void setActionPerformed(boolean actionPerformed) {
        Game.actionPerformed = actionPerformed;
    }

    public static void setCausingCollision(boolean causingCollision) {
        Game.causingCollision = causingCollision;
    }

    public static boolean isJumpPerformed() {
        return jumpPerformed;
    }

    public static boolean isCausingCollision() {
        return causingCollision;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    @Override
    public MachineType getMachineType() {
        return MachineType.DSCLIENT;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public DatagramSocket getServerEndpoint() {
        return serverEndpoint;
    }

    public void setServerEndpoint(DatagramSocket serverEndpoint) {
        this.serverEndpoint = serverEndpoint;
    }

    public InetAddress getServerInetAddr() {
        return serverInetAddr;
    }

    public int getTimeout() {
        return timeout;
    }

    public Vector3f getPlayerServerPos() {
        return playerServerPos;
    }

    public static Configuration getConfig() {
        return config;
    }

//    public int getPort() {
//        return port;
//    }
    public int getWeaponIndex() {
        return weaponIndex;
    }

    @Override
    public String getGuid() {
        return "*";
    }

}

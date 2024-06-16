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
    public static final int TICKS_PER_UPDATE = config.getTicksPerUpdate(); // (1 - FLUID, 2 - EFFICIENT)

    public static final double TICK_TIME = 1.0 / (double) TPS;

    public static final float AMOUNT = 4.45f;
    public static final float JUMP_STR_AMOUNT = 110f;
    public static final float ANGLE = (float) (Math.PI / 180);

    public static final int FORWARD = 0;
    public static final int BACKWARD = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    private static int ups; // current handleInput per second    
    private static int fpsMax = config.getFpsCap(); // fps max or fps cap  

    // if this is reach game will close without exception!
    public static final double CRITICAL_TIME = 10.0;
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
    private static Mode currentMode = Mode.SINGLE_PLAYER;

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
    protected String serverHostName = config.getServerIP();
    protected InetAddress serverInetAddr = null;

    protected int port = config.getClientPort();
    protected int timeout = DEFAULT_TIMEOUT;
    public static final int BUFF_SIZE = 8192; // read bytes (chunk) buffer size

    /**
     * Player alleged position on the game server
     */
    public final Vector3f playerServerPos = new Vector3f();

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
     */
    public void go() {
        this.running = true;
        Game.setCurrentMode(Mode.FREE);
        ups = 0;

        // gameTicks is progressive only ingame time
        gameTicks = config.getGameTicks();
        double lastTime = System.nanoTime();
        double currTime;
        double deltaTime;

        while (!gameObject.gameServer.shutDownSignal) {
            currTime = System.nanoTime();
            deltaTime = (currTime - lastTime) / 1E9d;
            gameTicks += deltaTime * Game.TPS;
            if (gameTicks >= Double.MAX_VALUE) {
                gameTicks = 0.0;
            }

            accumulator += deltaTime;
            lastTime = currTime;

            // Detecting critical status
            if (ups == 0 && deltaTime > CRITICAL_TIME) {
                DSLogger.reportFatalError("Game status critical!", null);
                gameObject.gameServer.stopServer();
                break;
            }

            while (accumulator >= TICK_TIME) {
                // Update with fixed timestep (environment)
                update(TICK_TIME);
                ups++;
                accumulator -= TICK_TIME;
            }
        }

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
        cfg.setFpsCap(fpsMax);
        cfg.setWidth(gameObject.WINDOW.getWidth());
        cfg.setHeight(gameObject.WINDOW.getHeight());
//        cfg.setFullscreen(gameObject.WINDOW.isFullscreen());
        cfg.setServerIP(gameObject.game.serverHostName);
        cfg.setClientPort(gameObject.game.port);
        cfg.setLocalIP(gameObject.gameServer.localIP);
        cfg.setServerPort(gameObject.gameServer.port);

        return cfg;
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

    public static int getFpsMax() {
        return fpsMax;
    }

    public static void setFpsMax(int fpsMax) {
        Game.fpsMax = fpsMax;
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

    public String getServerHostName() {
        return serverHostName;
    }

    public void setServerHostName(String serverHostName) {
        this.serverHostName = serverHostName;
    }

    public InetAddress getServerInetAddr() {
        return serverInetAddr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public Vector3f getPlayerServerPos() {
        return playerServerPos;
    }

}

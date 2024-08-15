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

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evgds.level.Editor;
import rs.alexanderstojanovich.evgds.level.LevelContainer;
import rs.alexanderstojanovich.evgds.level.RandomLevelGenerator;
import rs.alexanderstojanovich.evgds.resources.Assets;
import rs.alexanderstojanovich.evgds.util.DSLogger;

/**
 * Game Engine composed of Game (Loop), Game Renderer and core components.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public final class GameObject { // is mutual object for {Main, Renderer, Random Level Generator}

    // this class protects levelContainer, waterRenderer & Random Level Generator between the threads
    // game logic is contained in here
    public static enum MapLevelSize {
        SMALL, MEDIUM, LARGE, HUGE
    }

    /**
     * All Game Assets (Models, Textures etc.)
     */
    public final Assets GameAssets = new Assets();

    protected boolean initializedWindow = false;
    protected boolean initializedCore = false;

    private final Configuration cfg = Configuration.getInstance();

    public static final int VERSION = 49;
    public static final String WINDOW_TITLE = String.format("Demolition Synergy - v%s", VERSION);
    // makes default window -> Renderer sets resolution from config

    /**
     * Game GLFW Window
     */
    public final Window WINDOW;

    public final LevelContainer levelContainer;
    public final RandomLevelGenerator randomLevelGenerator;

    public final Game game;
    public final GameServer gameServer;

    /**
     * Max number of attempts to download the level
     */
    public static final int MAX_ATTEMPTS = 3;

    /**
     * Async Task Executor
     */
    public static final ExecutorService SINGLE_THR_EXEC = Executors.newSingleThreadExecutor();
    /**
     * Update/Generate for Level Container Mutex. Responsible for read/write to
     * chunks.
     */
    public static final Lock updateRenderLCLock = new ReentrantLock();

    protected static GameObject instance = null;
    protected boolean chunkOperationPerformed = false;

//    protected Quad splashScreen; // splash screen during initialization
    /**
     * Gets one instance of the GameObject (creates new if not exists).
     *
     * @return single window instance
     * @throws java.lang.Exception if not initializedWindow
     */
    public static GameObject getInstance() throws Exception {
        if (instance == null) {
            throw new Exception("Game Object not initialized!");
        }
        return instance;
    }

    /**
     * Init this game container with core components.
     *
     * @throws java.lang.Exception if water renderer or shadow renderer is
     * improperly configured
     */
    public GameObject() throws Exception {
        final int width = cfg.getWidth();
        final int height = cfg.getHeight();
        // creating the window
        /* Create and display the form */
        WINDOW = new Window(this);
        WINDOW.setSize(width, height);
        WINDOW.setVisible(true);
        WINDOW.initCenterWindow();
        WINDOW.setTitle(WINDOW_TITLE);
        //----------------------------------------------------------------------        

        //----------------------------------------------------------------------        
        //----------------------------------------------------------------------
        initializedWindow = true;
        //----------------------------------------------------------------------
        levelContainer = new LevelContainer(this);
        randomLevelGenerator = new RandomLevelGenerator(this.levelContainer);
        //----------------------------------------------------------------------
        //----------------------------------------------------------------------
        initializedCore = true;
        //----------------------------------------------------------------------        
        //----------------------------------------------------------------------
        // init game loop
        game = new Game(this); // init game with given configuration and game object
        gameServer = new GameServer(this); // create new server from game object
        DSLogger.reportDebug("Interface initialized.", null);
        // game interacts with the whole game container
        instance = this;
    }

    /**
     * Start this game container. Starts server and 'game-server' loop
     */
    public void start() {
        try {
            //----------------------------------------------------------------------
            DSLogger.reportDebug("Initiating Game Server Start.", null);
            DSLogger.reportDebug("Game will start soon.", null);
            //----------------------------------------------------------------------
            gameServer.startServer();
            Callable<Object> gameLoop = Executors.callable(() -> {
                try {
                    game.go();
                } catch (Exception ex) {
                    DSLogger.reportError("Game exited abruptly!", ex);
                    DSLogger.reportError(ex.getMessage(), ex);
                }
            });
            SINGLE_THR_EXEC.submit(gameLoop);
        } catch (Exception ex) {
            DSLogger.reportFatalError("Fatal error in Game Loop!", ex);
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    /**
     * Perform optimization (of chunks). Optimization is collecting all tuples
     * with blocklist from all chunks into one tuple selection.
     */
    public void utilOptimization() {
        if (!isWorking()) {
            levelContainer.optimize();
        }
    }

    // -------------------------------------------------------------------------
    /**
     * Update Game Object stuff, like Environment (call only from main)
     *
     * @param deltaTime deltaTime in ticks
     */
    public void update(float deltaTime) {
        if (!initializedWindow) {
            return;
        }

        // working check avoids locking the monitor
        WINDOW.upsertPosInfo(levelContainer.levelActors.getPosInfo());
        WINDOW.upsertPlayerInfo(levelContainer.levelActors.getPlayerInfo());
        WINDOW.upsertClientInfo(gameServer.getClientInfo());
        GameTime now = GameTime.Now();
        this.WINDOW.getGameTimeText().setText(String.format("Day %d %02d:%02d:%02d", now.days, now.hours, now.minutes, now.seconds));

        if (!isWorking() || this.getLevelContainer().getProgress() == 100.0f) {
            this.levelContainer.setProgress(0.0f);
        }

        this.WINDOW.updateDayNightCycle();

        this.WINDOW.getProgBar().setValue(Math.round(this.levelContainer.getProgress()));
        this.WINDOW.getProgBar().validate();
    }

    /**
     * Pull fullyOptimized tuples to working tuples in Block Environment.
     */
    public void pull() {
        if (isWorking()) {
            return;
        }
        levelContainer.blockEnvironment.pull();
    }

    /**
     * Push working to fullyOptimized tuples tuples in Block Environment.
     */
    public void push() {
        if (isWorking()) {
            return;
        }
        levelContainer.blockEnvironment.push();
    }

    /**
     * Swap working tuples & optimizing tuples in Block Environment. Zero cost
     * operation. Doesn't require synchronized block.
     */
    public void swap() {
        if (isWorking() || levelContainer.blockEnvironment.isOptimizing() || !levelContainer.blockEnvironment.isFullyOptimized()) {
            return;
        }
        levelContainer.blockEnvironment.swap();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    /**
     * Clear Everything. Game will be 'Free'.
     */
    public void clearEverything() {
        Editor.deselect();
        LevelContainer.AllBlockMap.init();
        levelContainer.chunks.clear();
        levelContainer.blockEnvironment.clear();

        Arrays.fill(levelContainer.buffer, (byte) 0x00);
        Arrays.fill(levelContainer.bak_buffer, (byte) 0x00);
        levelContainer.pos = 0;
        levelContainer.bak_pos = 0;

        levelContainer.levelActors.player.setPos(new Vector3f());
        levelContainer.levelActors.player.setRegistered(false);
        levelContainer.levelActors.spectator.setPos(new Vector3f());
        levelContainer.levelActors.npcList.clear();
        levelContainer.levelActors.otherPlayers.clear();
        if (gameServer.isShutDownSignal() || !gameServer.isRunning()) {
            WINDOW.setTitle(GameObject.WINDOW_TITLE);
        } else {
            WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + gameServer.worldName + " - Player Count: " + gameServer.clients.size());
        }
        Game.setCurrentMode(Game.Mode.FREE);
    }

    /**
     * Start new level from editor. Editor by default adds 9 'Doom' blocks at
     * the starting position. Called from concurrent thread.
     */
    public void startNewLevel() {
        levelContainer.startNewLevel();
    }

    /**
     * Load level from external file (which is in root of game dir). Called from
     * concurrent thread.
     *
     * @param fileName file name in filesystem.
     * @return success of operation
     */
    public boolean loadLevelFromFile(String fileName) {
        boolean ok = false;
        this.clearEverything();
        updateRenderLCLock.lock();
        try {
            ok |= levelContainer.loadLevelFromFile(fileName);
        } finally {
            updateRenderLCLock.unlock();
        }

        return ok;
    }

    /**
     * Save level to external file (which is in root of game dir). Called from
     * concurrent thread. Could be shared in opened in another game clients.
     *
     * @param fileName file name in filesystem.
     * @return success of operation
     */
    public boolean saveLevelToFile(String fileName) {
        boolean ok = false;
        updateRenderLCLock.lock();
        try {
            ok |= levelContainer.saveLevelToFile(fileName);
        } finally {
            updateRenderLCLock.unlock();
        }

        return ok;
    }

    /**
     * Start new randomly generated level from editor. New level could be
     * generated and subsequently edited from (game) client. Notice that there
     * is no 'SMALL', 'MEDIUM', 'LARGE', 'HUGE'. It is coded to parameter
     * 'numberOfBlocks'.
     *
     * Called from concurrent thread.
     *
     * @param numberOfBlocks max number of blocks to generate
     * @return success of operation
     */
    public boolean generateRandomLevel(int numberOfBlocks) {
        boolean ok = false;
        this.clearEverything();
        updateRenderLCLock.lock();
        try {
            ok |= levelContainer.generateRandomLevel(randomLevelGenerator, numberOfBlocks);
        } finally {
            updateRenderLCLock.unlock();
        }

        return ok;
    }

    /**
     * Start new randomly generated level from editor. New level could be
     * generated and subsequently edited from (game) client. Notice that there
     * is no 'SMALL', 'MEDIUM', 'LARGE', 'HUGE'. It is coded to parameter
     * 'numberOfBlocks'.
     *
     * Called from concurrent thread.
     *
     * @param levelSize Map Level Size
     * @return success of operation
     */
    public boolean generateRandomLevel(GameObject.MapLevelSize levelSize) {
        boolean ok = false;
        this.clearEverything();
        updateRenderLCLock.lock();
        final int numberOfBlocks;
        try {
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
            ok |= levelContainer.generateRandomLevel(randomLevelGenerator, numberOfBlocks);
            ok |= levelContainer.storeLevelToBufferNewFormat();
        } finally {
            updateRenderLCLock.unlock();
        }

        return ok;
    }

    /**
     * Check if level container is generating/loading/saving level.
     *
     * @return
     */
    public boolean isWorking() {
        return levelContainer.isWorking();
    }
    // -------------------------------------------------------------------------

    /*    
    * Load the window context and destroyes the window.
     */
//    public void destroy() {
//        SINGLE_THR_EXEC.shutdown();
//    }
    // prints general and detailed information about solid and fluid chunks
    public void printInfo() {
//        levelContainer.chunks.printInfo();
    }

    public LevelContainer getLevelContainer() {
        return levelContainer;
    }

    public RandomLevelGenerator getRandomLevelGenerator() {
        return randomLevelGenerator;
    }

    public boolean isInitializedWindow() {
        return initializedWindow;
    }

    public Configuration getCfg() {
        return cfg;
    }

    public Game getGame() {
        return game;
    }

    public boolean isInitializedCore() {
        return initializedCore;
    }

    public GameServer getGameServer() {
        return gameServer;
    }

    public boolean isChunkOperationPerformed() {
        return chunkOperationPerformed;
    }

//    /**
//     * Is server running
//     *
//     * @return is server up & running
//     */
//    public boolean isServerRunning() {
//        return gameServer.running;
//    }
}

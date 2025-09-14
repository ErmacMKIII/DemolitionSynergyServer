/* 
 * Copyright (C) 2020 Alexander Stojanonullch <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evgds.level;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evgds.chunk.Chunk;
import rs.alexanderstojanovich.evgds.chunk.Chunks;
import rs.alexanderstojanovich.evgds.core.Camera;
import rs.alexanderstojanovich.evgds.critter.Critter;
import rs.alexanderstojanovich.evgds.level.GravityEnviroment.Result;
import rs.alexanderstojanovich.evgds.light.LightSource;
import rs.alexanderstojanovich.evgds.light.LightSources;
import rs.alexanderstojanovich.evgds.location.BlockLocation;
import rs.alexanderstojanovich.evgds.location.TexByte;
import rs.alexanderstojanovich.evgds.main.Configuration;
import rs.alexanderstojanovich.evgds.main.Game;
import rs.alexanderstojanovich.evgds.main.GameObject;
import rs.alexanderstojanovich.evgds.main.GameTime;
import rs.alexanderstojanovich.evgds.main.Window;
import rs.alexanderstojanovich.evgds.models.Block;
import rs.alexanderstojanovich.evgds.models.Model;
import rs.alexanderstojanovich.evgds.util.DSLogger;
import rs.alexanderstojanovich.evgds.util.ModelUtils;
import rs.alexanderstojanovich.evgds.weapons.Weapons;

/**
 * World container. Contains everything.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class LevelContainer implements GravityEnviroment {

    // Constants for collision control & gravity handling
    /**
     * Min amount of iteration for collision control or gravity control (inner
     * loop)
     */
    public static final float MIN_AMOUNT = -8.4f;
    /**
     * Max amount of iteration for collision control or gravity control (inner
     * loop)
     */
    public static final float MAX_AMOUNT = 8.4f;
    /**
     * Step amount of iteration for collision control or gravity control (inner
     * loop).
     */
    public static final float STEP_AMOUNT = 0.05f;

    // -------------------------------------------------
    /**
     * World level map format. For save/load use.
     */
    public static enum LevelMapFormat {
        /**
         * Old format. Exists for quite long time.
         */
        DAT,
        /**
         * New Format. Blocks are grouped by texture name.
         */
        NDAT
    }

    public final GameObject gameObject;
    protected final Configuration cfg = Configuration.getInstance();

    /**
     * World Skybox - whole world (except Sun) is contained inside
     */
    public static final Block SKYBOX = new Block("night");

    /**
     * Main source of light. Outside of skybox.
     */
    public static final Model SUN = ModelUtils.readFromObjFile(Game.WORLD_ENTRY, "sun.obj", "suntx");
    public static final Vector4f SUN_COLOR_RGBA = new Vector4f(0.75f, 0.5f, 0.25f, 1.0f); // orange-yellow color
    public static final Vector3f SUN_COLOR_RGB = new Vector3f(0.75f, 0.5f, 0.25f); // orange-yellow color RGB

    public static final float SUN_SCALE = 32.0f;
    public static final float SUN_INTENSITY = (float) (1 << 28); // 268.4M

    public static final LightSource SUNLIGHT
            = new LightSource(SUN.pos, SUN_COLOR_RGB, SUN_INTENSITY);

    public final Chunks chunks = new Chunks();
    public final LightSources lightSources;

    private final IList<Integer> vChnkIdList = new GapList<>();
    private final IList<Integer> iChnkIdList = new GapList<>();

    public static final float BASE = 22.5f;
    public static final float SKYBOX_SCALE = BASE * BASE * BASE;
    public static final float SKYBOX_WIDTH = 2.0f * SKYBOX_SCALE;

    public static final Vector3f NIGHT_SKYBOX_COLOR_RGB = new Vector3f(0.25f, 0.5f, 0.75f); // cool bluish color for SKYBOX
    public static final Vector4f NIGHT_SKYBOX_COLOR = new Vector4f(0.25f, 0.5f, 0.75f, 0.15f); // cool bluish color for SKYBOX

    public static final int MAX_NUM_OF_BLOCKS = 131070;

    protected float progress = 0.0f;

    protected boolean working = false;

    public final LevelActors levelActors;

    // position of all the solid blocks to texture name & neighbors
    /**
     * Position of all the solid blocks to texture name & neighbors
     */
    public static final BlockLocation AllBlockMap = new BlockLocation();

    /**
     * Level Buffer to load or save (world) levels.
     */
    public final LevelBuffer levelBuffer;

    protected static boolean actorInFluid = false;

    protected float lastCycledDayTime = 0.0f;

    protected float fallVelocity = 0.0f;
    protected float jumpVelocity = 0.0f;

    public final Weapons weapons;

    public boolean gravityOn = false;

    public static final int NUM_OF_PASSES_MAX = Configuration.getInstance().getOptimizationPasses();

    /**
     * Update on put neighbours location on tex byte properties
     *
     * @param vector vec3f where location is
     *
     * @return bits property
     */
    private static byte updatePutNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            TexByte locVal = AllBlockMap.getLocation(adjPos);
            if (locVal != null) {
                bits |= mask;
                byte adjBits = locVal.byteValue;
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits |= maskAdj;
                locVal.byteValue = adjBits;
            }
        }
        return bits;
    }

    /**
     * Update on remove neighbours location on tex byte properties
     *
     * @param vector vec3f where location is
     *
     * @return bits property
     */
    private static byte updateRemNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            TexByte locVal = AllBlockMap.getLocation(adjPos);
            if (locVal != null) {
                bits |= mask;
                byte adjBits = locVal.byteValue;
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits &= ~maskAdj & 63;
                locVal.byteValue = adjBits;
            }
        }
        return bits;
    }

    /**
     * Put block into All Block Map.
     *
     * @param block block
     */
    public static void putBlock(Block block) {
        Vector3f pos = block.getPos();
        String str = block.getTexName();
        byte bits = updatePutNeighbors(pos);
        TexByte locVal = new TexByte(block.getPrimaryRGBAColor(), str, bits, block.isSolid(), block.getId());
        AllBlockMap.putLocation(new Vector3f(pos), locVal);
    }

    /**
     * Remove block from All Block Map.
     *
     * @param block block
     */
    public static void removeBlock(Block block) {
        Vector3f pos = block.getPos();
        boolean rem = AllBlockMap.removeLocation(pos);
        if (rem) {
            updateRemNeighbors(pos);
        }
    }

    public LevelContainer(GameObject gameObject) {
        this.gameObject = gameObject;
        this.lightSources = new LightSources();

        this.weapons = new Weapons(this);
        this.levelActors = new LevelActors(this);
        this.levelBuffer = new LevelBuffer(this);

        lightSources.addLight(levelActors.player.light);
        lightSources.addLight(SUNLIGHT);
    }

    public static void printPositionMaps() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("POSITION MAP");
        sb.append("(size = ").append(AllBlockMap.getPopulation()).append(")\n");
        sb.append("---------------------------");
        DSLogger.reportDebug(sb.toString(), null);
    }

    public void printQueues() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("VISIBLE QUEUE\n");
        sb.append(vChnkIdList);
        sb.append("\n");
        sb.append("---------------------------");
        sb.append("\n");
        sb.append("INVISIBLE QUEUE\n");
        sb.append(iChnkIdList);
        sb.append("\n");
        sb.append("---------------------------");
        DSLogger.reportDebug(sb.toString(), null);
    }

    // -------------------------------------------------------------------------    
    // -------------------------------------------------------------------------
    /**
     * Start new editor scene with 9 'Doom' blocks.
     *
     * @return on success
     */
    public boolean startNewLevel() {
        if (working) {
            return false;
        }
        boolean success = false;
        working = true;
        progress = 0.0f;
        levelActors.freeze();

        chunks.clear();
        levelActors.npcList.clear();
        AllBlockMap.init();

        lightSources.retainLights(2);

        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                Block blk = new Block("doom0");
                blk.getPos().x = (4 * i) & 0xFFFFFFFE;
                blk.getPos().y = (4 * j) & 0xFFFFFFFE;
                blk.getPos().z = 3 & 0xFFFFFFFE;

                blk.getPrimaryRGBAColor().x = 0.5f * i + 0.25f;
                blk.getPrimaryRGBAColor().y = 0.5f * j + 0.25f;
                blk.getPrimaryRGBAColor().z = 0.0f;

                chunks.addBlock(blk);

                progress += 100.0f / 9.0f;
            }
        }

        levelActors.configureMainObserver(new Vector3f(10.5f, 0.0f, -4.0f), new Vector3f(Camera.Z_AXIS), new Vector3f(Camera.Y_AXIS), new Vector3f(Camera.X_AXIS));

//        NPC npc = new NPC(critter.getModel());
//        npc.getModel().setPos(new Vector3f(0f, 20f, 0f));
//        levelActors.npcList.add(npc);
        levelActors.unfreeze();
        progress = 100.0f;
        working = false;
        success = true;
        return success;
    }

    /**
     * Generate random level for editor
     *
     * @param randomLevelGenerator random level generator (from game object)
     * @param numberOfBlocks pre-defined number of blocks
     * @return on success
     */
    public boolean generateRandomLevel(RandomLevelGenerator randomLevelGenerator, int numberOfBlocks) {
        if (working) {
            return false;
        }
        working = true;
        levelActors.freeze();

        levelActors.configureMainObserver(new Vector3f(10.5f, Chunk.BOUND >> 3, -4.0f), new Vector3f(Camera.Z_AXIS), new Vector3f(Camera.Y_AXIS), new Vector3f(Camera.X_AXIS));

        boolean success = false;
        progress = 0.0f;

        chunks.clear();

        AllBlockMap.init();

        lightSources.retainLights(2);

        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_BLOCKS) {
            randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
            randomLevelGenerator.generate();
            success = true;
        }

        progress = 100.0f;
        working = false;

        levelActors.unfreeze();
        return success;
    }

    /**
     * Generate random level for single player
     *
     * @param randomLevelGenerator random level generator (from game object)
     * @param numberOfBlocks pre-defined number of blocks
     * @return on success
     * @throws java.lang.Exception if player spawn fails
     */
    public boolean generateSinglePlayerLevel(RandomLevelGenerator randomLevelGenerator, int numberOfBlocks) throws Exception {
        if (working) {
            return false;
        }
        working = true;
        levelActors.freeze();

        boolean success = false;
        progress = 0.0f;

        chunks.clear();

        AllBlockMap.init();

        lightSources.retainLights(2);

        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_BLOCKS) {
            // generate blocks
            randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
            randomLevelGenerator.generate();

            success = true;
        }

        progress = 100.0f;
        working = false;

        levelActors.unfreeze();

        return success;
    }

    /**
     * Generate random level for multiplayer (Host)
     *
     * @param randomLevelGenerator random level generator (from game object)
     * @param numberOfBlocks pre-defined number of blocks
     * @return on success
     * @throws java.lang.Exception if player spawn fails
     */
    public boolean generateMultiPlayerLevel(RandomLevelGenerator randomLevelGenerator, int numberOfBlocks) throws Exception {
        if (working) {
            return false;
        }
        working = true;
        levelActors.freeze();

        boolean success = false;
        progress = 0.0f;

        chunks.clear();

        AllBlockMap.init();

        lightSources.retainLights(2);

        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_BLOCKS) {
            // generate blocks
            randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
            randomLevelGenerator.generate();

            success = true;
        }

        progress = 100.0f;
        working = false;

        levelActors.unfreeze();

        return success;
    }

    /**
     * Applies gravity to the player, making them fall downwards if not
     * supported below. Called on motion. Called in 'update'.
     *
     * @param critter critter affected by gravity (submitted)
     * @param deltaTime The time elapsed since the last handleInput.
     * @return {@code true} if the player is affected by gravity, {@code false}
     * otherwise.
     */
    @Override
    public Result gravityDo(Critter critter, float deltaTime) {
        return Result.NEUTRAL;
    }

    /**
     * Makes the player jump upwards.
     *
     * @param critter The player.
     * @param jumpStrength The amount of upward movement.
     * @return {@code true} if the player successfully jumped, {@code false}
     * otherwise.
     */
    @Override
    public boolean jump(Critter critter, float jumpStrength) {
        return false;
    }

    /**
     * Makes the player push downwards, pressuring the bottom surface (or air)
     *
     * @param critter The player.
     * @param crouchStrength The amount of downward movement.
     * @return was crouch performed by player (was able to)
     */
    @Override
    public boolean crouch(Critter critter, float crouchStrength) {
        return false;
    }

    /**
     * Perform update to the day/night cycle. Sun position & sunlight is
     * updated. Skybox rotates counter-clockwise (from -right to right)
     */
    @Deprecated
    public void update() { // call it externally from the main thread 
        if (!working) { // don't subBufferVertices if working, it may screw up!   
            final float now = (float) GameTime.Now().getTime();
            float dtime = now - lastCycledDayTime;
            lastCycledDayTime = now;

            final float dangle = org.joml.Math.toRadians(dtime * 360.0f / 24.0f);

            SKYBOX.setrY(SKYBOX.getrY() + dangle);
            SUN.pos.rotateZ(dangle);

            final float sunAngle = org.joml.Math.atan2(SUN.pos.y, SUN.pos.x);
            float inten = org.joml.Math.sin(sunAngle);

            if (inten < 0.0f) { // night
                SKYBOX.setTexName("night");
                SKYBOX.setPrimaryRGBAColor(new Vector4f((new Vector3f(NIGHT_SKYBOX_COLOR_RGB)).mul(0.15f), 0.15f));
            } else if (inten >= 0.0f) { // day
                SKYBOX.setTexName("day");
                SKYBOX.setPrimaryRGBAColor(new Vector4f((new Vector3f(NIGHT_SKYBOX_COLOR_RGB)).mul(Math.max(inten, 0.15f)), 0.15f));
            }

            final float sunInten = Math.max(inten, 0.0f);
            SUN.setPrimaryRGBAColor(new Vector4f((new Vector3f(SUN_COLOR_RGB)).mul(sunInten), 1.0f));
            SUNLIGHT.setIntensity(sunInten * SUN_INTENSITY);
            SUNLIGHT.pos.set(SUN.pos);

            // always handleInput sunlight (sun/pos)
            lightSources.updateLight(1, SUNLIGHT);
            lightSources.setModified(1, true); // SUNLIGHT index is always 1

            // handleInput - player light - only in correct mode
            if (Game.getCurrentMode() == Game.Mode.FREE || Game.getCurrentMode() == Game.Mode.EDITOR) {
                levelActors.player.light.setIntensity(0.0f);
            } else {
                levelActors.player.light.setIntensity(LightSource.PLAYER_LIGHT_INTENSITY);
                lightSources.updateLight(0, levelActors.player.light);
            }
            lightSources.setModified(0, true); // Player index is always 0
            // updateEnvironment light blocks set light modified for visible lights
            lightSources.sourceList.forEach(ls -> {
                int chnkId = Chunk.chunkFunc(ls.pos);
                if (vChnkIdList.contains(chnkId)) {
                    lightSources.setModified(ls.pos, true);
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    public void incProgress(float increment) {
        if (progress < 100.0f) {
            progress += increment;
        }
    }

    public Window getMyWindow() {
        return gameObject.WINDOW;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public boolean isWorking() {
        return working;
    }

    public Chunks getChunks() {
        return chunks;
    }

    public IList<Integer> getvChnkIdList() {
        return vChnkIdList;
    }

    public IList<Integer> getiChnkIdList() {
        return iChnkIdList;
    }

    public LevelActors getLevelActors() {
        return levelActors;
    }

    public Configuration getCfg() {
        return cfg;
    }

    public float getLastCycledDayTime() {
        return lastCycledDayTime;
    }

    @Override
    public float getFallVelocity() {
        return fallVelocity;
    }

    public LightSources getLightSources() {
        return lightSources;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public static boolean isActorInFluid() {
        return actorInFluid;
    }

    @Override
    public boolean isGravityOn() {
        return gravityOn;
    }

    public float getJumpVelocity() {
        return jumpVelocity;
    }

    public Weapons getWeapons() {
        return weapons;
    }

}

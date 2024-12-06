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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evgds.chunk.Chunk;
import rs.alexanderstojanovich.evgds.chunk.Chunks;
import rs.alexanderstojanovich.evgds.core.Camera;
import rs.alexanderstojanovich.evgds.critter.Critter;
import rs.alexanderstojanovich.evgds.light.LightSource;
import rs.alexanderstojanovich.evgds.light.LightSources;
import rs.alexanderstojanovich.evgds.location.BlockLocation;
import rs.alexanderstojanovich.evgds.location.TexByte;
import rs.alexanderstojanovich.evgds.main.Configuration;
import rs.alexanderstojanovich.evgds.main.Game;
import rs.alexanderstojanovich.evgds.main.GameObject;
import rs.alexanderstojanovich.evgds.models.Block;
import rs.alexanderstojanovich.evgds.models.Model;
import rs.alexanderstojanovich.evgds.resources.Assets;
import rs.alexanderstojanovich.evgds.util.DSLogger;
import rs.alexanderstojanovich.evgds.util.ModelUtils;
import rs.alexanderstojanovich.evgds.util.VectorFloatUtils;
import rs.alexanderstojanovich.evgds.weapons.Weapons;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LevelContainer implements GravityEnviroment {

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

    public static final Block SKYBOX = new Block("night");

    public static final Model SUN = ModelUtils.readFromObjFile(Game.WORLD_ENTRY, "sun.obj", "suntx");
    public static final Vector4f SUN_COLOR_RGBA = new Vector4f(0.75f, 0.5f, 0.25f, 1.0f); // orange-yellow color
    public static final Vector3f SUN_COLOR_RGB = new Vector3f(0.75f, 0.5f, 0.25f); // orange-yellow color RGB

    public static final float SUN_SCALE = 32.0f;
    public static final float SUN_INTENSITY = (float) (1 << 28); // 268M

    public static final LightSource SUNLIGHT
            = new LightSource(SUN.pos, SUN_COLOR_RGB, SUN_INTENSITY);

    public final Chunks chunks = new Chunks();
    public final BlockEnvironment blockEnvironment;
    public final LightSources lightSources;

    private final IList<Integer> vChnkIdList = new GapList<>();
    private final IList<Integer> iChnkIdList = new GapList<>();

    public final byte[] buffer = new byte[0x1000000]; // 16 MB Buffer
    public int pos = 0;

    public final byte[] bak_buffer = new byte[0x1000000]; // 16 MB BAK Buffer
    public int bak_pos = 0;

    public static final float BASE = 24f;
    public static final float SKYBOX_SCALE = BASE * BASE * BASE;
    public static final float SKYBOX_WIDTH = 2.0f * SKYBOX_SCALE;

    public static final Vector3f SKYBOX_COLOR_RGB = new Vector3f(0.25f, 0.5f, 0.75f); // cool bluish color for SKYBOX
    public static final Vector4f SKYBOX_COLOR = new Vector4f(0.25f, 0.5f, 0.75f, 0.15f); // cool bluish color for SKYBOX

    public static final int MAX_NUM_OF_BLOCKS = 131070;

    private float progress = 0.0f;

    private boolean working = false;

    public final LevelActors levelActors;

    // position of all the solid blocks to texture name & neighbors
    public static final BlockLocation AllBlockMap = new BlockLocation();

    protected static boolean actorInFluid = false;

    protected float lastCycledDayTime = 0.0f;

    protected float fallVelocity = 0.0f;
    protected float jumpVelocity = 0.0f;

    public final Weapons weapons;

    public boolean gravityOn = false;

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

    public static void putBlock(Block block) {
        Vector3f pos = block.getPos();
        String str = block.getTexName();
        byte bits = updatePutNeighbors(pos);
        TexByte locVal = new TexByte(block.getPrimaryRGBAColor(), str, bits, block.isSolid(), block.getId());
        AllBlockMap.putLocation(new Vector3f(pos), locVal);
    }

    public static void removeBlock(Block block) {
        Vector3f pos = block.getPos();
        boolean rem = AllBlockMap.removeLocation(pos);
        if (rem) {
            updateRemNeighbors(pos);
        }
    }

    static {
        // setting SKYBOX             
//        SKYBOX.setPrimaryRGBColor(SKYBOX_COLOR_RGB);
//        SKYBOX.setUVsForSkybox();
//        SKYBOX.setScale(SKYBOX_SCALE);
//        SKYBOX.nullifyNormalsForFace(Block.BOTTOM);
//        SKYBOX.setPrimaryColorAlpha(0.15f);

        SUN.setPrimaryRGBColor(new Vector3f(SUN_COLOR_RGB));
        SUN.pos = new Vector3f(0.0f, -12288.0f, 0.0f);
        SUNLIGHT.pos = SUN.pos;
        SUN.setScale(SUN_SCALE);
        SUN.setPrimaryColorAlpha(1.00f);
    }

    public LevelContainer(GameObject gameObject) {
        this.gameObject = gameObject;
        this.blockEnvironment = new BlockEnvironment(gameObject, chunks);
        this.lightSources = new LightSources();

        this.weapons = new Weapons(this);
        this.levelActors = new LevelActors(this);

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

        blockEnvironment.clear();
//        NPC npc = new NPC(critter.getModel());
//        npc.getModel().setPos(new Vector3f(0f, 20f, 0f));
//        levelActors.npcList.add(npc);
        levelActors.unfreeze();
        progress = 100.0f;
        working = false;
        success = true;

        return success;
    }

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

//        solidChunks.updateSolids(this);
//        fluidChunks.updateFluids(this);
        blockEnvironment.clear();

        progress = 100.0f;
        working = false;

        levelActors.unfreeze();

        return success;
    }

    private static String ensureCorrectExtension(String filename) {
        Pattern pattern = Pattern.compile("\\.(dat|ndat)$");
        if (!pattern.matcher(filename).find()) {
            filename += ".dat"; // Default to .dat if no valid extension is found
        }

        return filename;
    }

    public boolean storeLevelToBufferOldFormat() {
        working = true;
        boolean success = false;
//        if (progress > 0.0f) {
//            return false;
//        }
        progress = 0.0f;
        levelActors.freeze();
        pos = 0;
        buffer[0] = 'D';
        buffer[1] = 'S';
        pos += 2;

        Camera camera = levelActors.mainCamera();
        if (camera == null) {
            return false;
        }

        byte[] campos = VectorFloatUtils.vec3fToByteArray(camera.getPos());
        System.arraycopy(campos, 0, buffer, pos, campos.length);
        pos += campos.length;

        byte[] camfront = VectorFloatUtils.vec3fToByteArray(camera.getFront());
        System.arraycopy(camfront, 0, buffer, pos, camfront.length);
        pos += camfront.length;

        byte[] camup = VectorFloatUtils.vec3fToByteArray(camera.getUp());
        System.arraycopy(camup, 0, buffer, pos, camup.length);
        pos += camup.length;

        byte[] camright = VectorFloatUtils.vec3fToByteArray(camera.getRight());
        System.arraycopy(camup, 0, buffer, pos, camright.length);
        pos += camright.length;

        IList<Vector3f> solidPos = AllBlockMap.getPopulatedLocations(tb -> tb.solid);
        IList<Vector3f> fluidPos = AllBlockMap.getPopulatedLocations(tb -> !tb.solid);

        buffer[pos++] = 'S';
        buffer[pos++] = 'O';
        buffer[pos++] = 'L';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int solidNum = solidPos.size();
        buffer[pos++] = (byte) (solidNum);
        buffer[pos++] = (byte) (solidNum >> 8);

        //----------------------------------------------------------------------
        for (Vector3f sp : solidPos) {
            if (gameObject.gameServer.isShutDownSignal()) {
                break;
            }
            byte[] byteArraySolid = Block.toByteArray(sp, AllBlockMap.getLocation(sp));
            System.arraycopy(byteArraySolid, 0, buffer, pos, 29);
            pos += 29;
            progress += 50.0f / (float) solidPos.size();
        }

        buffer[pos++] = 'F';
        buffer[pos++] = 'L';
        buffer[pos++] = 'U';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int fluidNum = fluidPos.size();
        buffer[pos++] = (byte) (fluidNum);
        buffer[pos++] = (byte) (fluidNum >> 8);

        for (Vector3f fp : fluidPos) {
            if (gameObject.gameServer.isShutDownSignal()) {
                break;
            }
            byte[] byteArrayFluid = Block.toByteArray(fp, AllBlockMap.getLocation(fp));
            System.arraycopy(byteArrayFluid, 0, buffer, pos, 29);
            pos += 29;
            progress += 50.0f / (float) fluidPos.size();
        }

        buffer[pos++] = 'E';
        buffer[pos++] = 'N';
        buffer[pos++] = 'D';

        levelActors.unfreeze();
        progress = 100.0f;

        if (progress == 100.0f && !gameObject.gameServer.isShutDownSignal()) {
            success = true;
        }
        working = false;

        return success;
    }

    public boolean storeLevelToBufferNewFormat() {
        working = true;
        boolean success = false;
//        if (progress > 0.0f) {
//            return false;
//        }
        progress = 0.0f;
        levelActors.freeze();
        pos = 0;
        buffer[0] = 'D';
        buffer[1] = 'S';
        buffer[2] = '2';
        pos += 3;

        Camera camera = levelActors.mainCamera();
        if (camera == null) {
            return false;
        }

        byte[] campos = VectorFloatUtils.vec3fToByteArray(camera.getPos());
        System.arraycopy(campos, 0, buffer, pos, campos.length);
        pos += campos.length;

        byte[] camfront = VectorFloatUtils.vec3fToByteArray(camera.getFront());
        System.arraycopy(camfront, 0, buffer, pos, camfront.length);
        pos += camfront.length;

        byte[] camup = VectorFloatUtils.vec3fToByteArray(camera.getUp());
        System.arraycopy(camup, 0, buffer, pos, camup.length);
        pos += camup.length;

        byte[] camright = VectorFloatUtils.vec3fToByteArray(camera.getRight());
        System.arraycopy(camright, 0, buffer, pos, camright.length);
        pos += camright.length;

        int allBlkSize = AllBlockMap.locationProperties.size();

        buffer[pos++] = 'B';
        buffer[pos++] = 'L';
        buffer[pos++] = 'K';
        buffer[pos++] = 'S';

        // Store the total number of blocks
        buffer[pos++] = (byte) (allBlkSize);
        buffer[pos++] = (byte) (allBlkSize >> 8);
        buffer[pos++] = (byte) (allBlkSize >> 16);
        buffer[pos++] = (byte) (allBlkSize >> 24);

        for (String texName : Assets.TEX_WORLD) {
            IList<Vector3f> blkPos = AllBlockMap.getPopulatedLocations(tb -> tb.texName.equals(texName));
            int count = blkPos.size();
            byte[] texNameBytes = texName.getBytes(Charset.forName("US-ASCII"));
            for (int i = 0; i < 5; i++) {
                buffer[pos++] = texNameBytes[i];
            }
            buffer[pos++] = (byte) (count);
            buffer[pos++] = (byte) (count >> 8);
            buffer[pos++] = (byte) (count >> 16);
            buffer[pos++] = (byte) (count >> 24);
            for (Vector3f p : blkPos) {
                if (gameObject.gameServer.isShutDownSignal()) {
                    break;
                }
                byte[] byteArray = Block.toByteArray(p, AllBlockMap.getLocation(p));
                System.arraycopy(byteArray, 5, buffer, pos, 24);
                pos += 29;
                progress += 100.0f / (float) allBlkSize;
            }
        }

        buffer[pos++] = 'E';
        buffer[pos++] = 'N';
        buffer[pos++] = 'D';

        levelActors.unfreeze();
        progress = 100.0f;

        if (progress == 100.0f && !gameObject.gameServer.isShutDownSignal()) {
            success = true;
        }
        working = false;
        return success;
    }

    public boolean loadLevelFromBufferOldFormat() {
        working = true;
        boolean success = false;
//        if (progress > 0.0f) {
//            return false;
//        }
        progress = 0.0f;
        levelActors.freeze();
        pos = 0;
        if (buffer[0] == 'D' && buffer[1] == 'S') {
            chunks.clear();

            AllBlockMap.init();

            lightSources.retainLights(2);

            pos += 2;
            byte[] posArr = new byte[12];
            System.arraycopy(buffer, pos, posArr, 0, posArr.length);
            Vector3f campos = VectorFloatUtils.vec3fFromByteArray(posArr);
            pos += posArr.length;

            byte[] frontArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, frontArr.length);
            Vector3f camfront = VectorFloatUtils.vec3fFromByteArray(frontArr);
            pos += frontArr.length;

            byte[] upArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, upArr.length);
            Vector3f camup = VectorFloatUtils.vec3fFromByteArray(upArr);
            pos += upArr.length;

            byte[] rightArr = new byte[12];
            System.arraycopy(buffer, pos, rightArr, 0, rightArr.length);
            Vector3f camright = VectorFloatUtils.vec3fFromByteArray(rightArr);
            pos += rightArr.length;

            levelActors.configureMainObserver(campos, camfront, camup, camright);

            char[] solid = new char[5];
            for (int i = 0; i < solid.length; i++) {
                solid[i] = (char) buffer[pos++];
            }
            String strSolid = String.valueOf(solid);

            if (strSolid.equals("SOLID")) {
                int solidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                pos += 2;
                for (int i = 0; i < solidNum && !gameObject.gameServer.isShutDownSignal(); i++) {
                    byte[] byteArraySolid = new byte[29];
                    System.arraycopy(buffer, pos, byteArraySolid, 0, 29);
                    Block solidBlock = Block.fromByteArray(byteArraySolid, true);
                    chunks.addBlock(solidBlock);
                    pos += 29;
                    progress += 50.0f / solidNum;
                }

//                solidChunks.updateSolids();
                char[] fluid = new char[5];
                for (int i = 0; i < fluid.length; i++) {
                    fluid[i] = (char) buffer[pos++];
                }
                String strFluid = String.valueOf(fluid);

                if (strFluid.equals("FLUID")) {
                    int fluidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                    pos += 2;
                    for (int i = 0; i < fluidNum && !gameObject.gameServer.isShutDownSignal(); i++) {
                        byte[] byteArrayFluid = new byte[29];
                        System.arraycopy(buffer, pos, byteArrayFluid, 0, 29);
                        Block fluidBlock = Block.fromByteArray(byteArrayFluid, false);
                        chunks.addBlock(fluidBlock);
                        pos += 29;
                        progress += 50.0f / fluidNum;
                    }

//                    fluidChunks.updateFluids();
                    char[] end = new char[3];
                    for (int i = 0; i < end.length; i++) {
                        end[i] = (char) buffer[pos++];
                    }
                    String strEnd = String.valueOf(end);
                    if (strEnd.equals("END")) {
                        success = true;
                    }
                }

            }

        }
        levelActors.unfreeze();
        blockEnvironment.clear();

        progress = 100.0f;
        working = false;

        return success;
    }

    public boolean loadLevelFromBufferNewFormat() throws UnsupportedEncodingException {
        working = true;
        boolean success = false;
//        if (progress > 0.0f) {
//            return false;
//        }
        progress = 0.0f;
        levelActors.freeze();
        pos = 0;

        // Check the initial format identifiers
        if (buffer[pos++] == 'D' && buffer[pos++] == 'S' && buffer[pos++] == '2') {
            AllBlockMap.init();
            lightSources.retainLights(2);

            byte[] posArr = new byte[12];
            System.arraycopy(buffer, pos, posArr, 0, posArr.length);
            Vector3f campos = VectorFloatUtils.vec3fFromByteArray(posArr);
            pos += posArr.length;

            byte[] frontArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, frontArr.length);
            Vector3f camfront = VectorFloatUtils.vec3fFromByteArray(frontArr);
            pos += frontArr.length;

            byte[] upArr = new byte[12];
            System.arraycopy(buffer, pos, upArr, 0, upArr.length);
            Vector3f camup = VectorFloatUtils.vec3fFromByteArray(upArr);
            pos += upArr.length;

            byte[] rightArr = new byte[12];
            System.arraycopy(buffer, pos, rightArr, 0, rightArr.length);
            Vector3f camright = VectorFloatUtils.vec3fFromByteArray(rightArr);
            pos += rightArr.length;

            levelActors.configureMainObserver(campos, camfront, camup, camright);

            if (buffer[pos++] == 'B' && buffer[pos++] == 'L' && buffer[pos++] == 'K' && buffer[pos++] == 'S') {
                // Read the total number of blocks
                int totalBlocks = (buffer[pos++] & 0xFF) | ((buffer[pos++] & 0xFF) << 8)
                        | ((buffer[pos++] & 0xFF) << 16) | ((buffer[pos++] & 0xFF) << 24);

                if (totalBlocks <= 0) {
                    throw new IllegalStateException("No blocks to process!");
                }

                while (true) {
                    char[] texNameChars = new char[5];
                    for (int i = 0; i < texNameChars.length; i++) {
                        texNameChars[i] = (char) buffer[pos++];
                    }
                    String texName = new String(texNameChars);

                    int count = (buffer[pos++] & 0xFF) | ((buffer[pos++] & 0xFF) << 8)
                            | ((buffer[pos++] & 0xFF) << 16) | ((buffer[pos++] & 0xFF) << 24);

                    for (int i = 0; i < count && !gameObject.gameServer.isShutDownSignal(); i++) {
                        byte[] byteArrayBlock = new byte[29];
                        System.arraycopy(texName.getBytes("US-ASCII"), 0, byteArrayBlock, 0, 5);
                        System.arraycopy(buffer, pos, byteArrayBlock, 5, 24);
                        Block block = Block.fromByteArray(byteArrayBlock, !texName.equals("water"));
                        chunks.addBlock(block);
                        pos += 29;
                        progress += 100.0f / (float) totalBlocks;
                    }

                    if (buffer[pos] == 'E' && buffer[pos + 1] == 'N' && buffer[pos + 2] == 'D') {
                        pos += 3;
                        success = true;
                        break;
                    }
                }
            }
        }

        levelActors.unfreeze();
        blockEnvironment.clear();

        progress = 100.0f;
        working = false;

        return success;
    }

    public boolean saveLevelToFile(String filename) {
        if (working) {
            return false;
        }
        boolean success = false;
        ensureCorrectExtension(filename);

        BufferedOutputStream bos = null;
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }

        if (filename.endsWith(".dat")) {
            success |= storeLevelToBufferOldFormat(); // saves level to buffer first
        } else if (filename.endsWith(".ndat")) {
            success |= storeLevelToBufferNewFormat(); // saves level to buffer first
        }

        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(buffer, 0, pos); // save buffer to file at pos mark
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (bos != null) {
            try {
                bos.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
        return success;
    }

    public boolean loadLevelFromFile(String filename) {
        if (working) {
            return false;
        }
        boolean success = false;
        if (filename.isEmpty()) {
            return false;
        }
        ensureCorrectExtension(filename);

        File file = new File(filename);
        BufferedInputStream bis = null;
        if (!file.exists()) {
            return false; // this prevents further fail
        }
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(buffer);
            if (filename.endsWith(".dat")) {
                success |= loadLevelFromBufferOldFormat();
            } else if (filename.endsWith(".ndat")) {
                success |= loadLevelFromBufferNewFormat();
            }

        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (bis != null) {
            try {
                bis.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
        return success;
    }

    public boolean isActorInFluidChk() {
        boolean yea = false;
        Vector3f camPos = levelActors.mainActor().getPos();

        Vector3f obsCamPosAlign = new Vector3f(
                Math.round(camPos.x + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.y + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.z + 0.5f) & 0xFFFFFFFE
        );

        yea = AllBlockMap.isLocationPopulated(obsCamPosAlign, false);

        if (!yea) {
            for (int j = 0; j <= 13; j++) {
                Vector3f adjPos = Block.getAdjacentPos(camPos, j, 2.0f);
                Vector3f adjAlign = new Vector3f(
                        Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                );

                boolean fluidOnLoc = AllBlockMap.isLocationPopulated(adjAlign, false);

                if (fluidOnLoc) {
                    yea = Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, camPos);
                    if (yea) {
                        break;
                    }
                }
            }
        }

        return yea;
    }

    public static boolean isActorInFluidChk(LevelContainer lc) {
        boolean yea = false;
        Vector3f camPos = lc.levelActors.mainActor().getPos();

        Vector3f obsCamPosAlign = new Vector3f(
                Math.round(camPos.x + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.y + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.z + 0.5f) & 0xFFFFFFFE
        );

        yea = AllBlockMap.isLocationPopulated(obsCamPosAlign, false);

        if (!yea) {
            for (int j = 0; j <= 13; j++) {
                Vector3f adjPos = Block.getAdjacentPos(camPos, j, 2.0f);
                Vector3f adjAlign = new Vector3f(
                        Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                );

                boolean fluidOnLoc = AllBlockMap.isLocationPopulated(adjAlign, false);

                if (fluidOnLoc) {
                    yea = Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, camPos);
                    if (yea) {
                        break;
                    }
                }
            }
        }

        return yea;
    }

    public static void updateActorInFluid(LevelContainer lc) {
        actorInFluid = isActorInFluidChk(lc);
    }

    /**
     * Applies gravity to the player, making them fall downwards if not
     * supported below.
     *
     * @param deltaTime The time elapsed since the last handleInput.
     * @return {@code true} if the player is falling, {@code false} otherwise.
     */
    @Override
    public boolean gravityDo(float deltaTime) {
        return false;
    }

    /**
     * Makes the player jump upwards.
     *
     * @param critter The player.
     * @param jumpStrength The amount of upward movement.
     * @param deltaTime The time elapsed since the last handleInput.
     * @return {@code true} if the player successfully jumped, {@code false}
     * otherwise.
     */
    @Override
    public boolean jump(Critter critter, float jumpStrength, float deltaTime) {
        return false;
    }

    /**
     * Makes the player push downwards, pressuring the bottom surface (or air)
     *
     * @param critter The player.
     * @param amountYNeg The amount of downward movement.
     * @param deltaTime The time elapsed since the last handleInput.
     * @return was crouch performed by player (was able to)
     */
    @Override
    public boolean crouch(Critter critter, float amountYNeg, float deltaTime) {
        return false;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    public void incProgress(float increment) {
        if (progress < 100.0f) {
            progress += increment;
        }
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

    public byte[] getBuffer() {
        return buffer;
    }

    public int getPos() {
        return pos;
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

    public BlockEnvironment getBlockEnvironment() {
        return blockEnvironment;
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

/*
 * Copyright (C) 2020 Aleksandar Stojanovic <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evgds.chunk;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.joml.Vector3f;
import rs.alexanderstojanovich.evgds.models.Block;
import rs.alexanderstojanovich.evgds.resources.Assets;
import rs.alexanderstojanovich.evgds.util.ModelUtils;

/**
 * List of world blocks with same face enabled bits and same texture layer.
 * Designated for instanced rendering with texture array (sampler2DArray).
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Tuple extends Series {

    public static final int VEC2_SIZE = 2;
    public static final int VEC3_SIZE = 3;
    public static final int VEC4_SIZE = 4;
    public static final int MAT4_SIZE = 16;

    /**
     * Maps texture name -> layer index in the GL_TEXTURE_2D_ARRAY.
     * Populated once at startup from Assets.TEX_WORLD ordering.
     */
    public static final Map<String, Integer> TEX_LAYER_MAP = new HashMap<>();

    /**
     * Layer index for "water" texture (used for isSolid check).
     * Set during TEX_LAYER_MAP initialization.
     */
    public static int WATER_LAYER_INDEX = -1;

    static {
        // Build layer map from Assets.TEX_WORLD ordering
        // Assets.TEX_WORLD must be ordered consistently (e.g. ["grass","stone","water",...])
        String[] texWorld = Assets.TEX_WORLD;
        for (int i = 0; i < texWorld.length; i++) {
            TEX_LAYER_MAP.put(texWorld[i], i);
            if (texWorld[i].equals("water")) {
                WATER_LAYER_INDEX = i;
            }
        }
    }

    protected int vec4Vbo = 0; // color
    protected static FloatBuffer vec4FloatColorBuff = null;

    protected int mat4Vbo = 0; // model matrix [col0, col1, col2, col3]
    protected static FloatBuffer mat4FloatModelBuff = null;

    protected final String name;

    protected final int facesNum;
    protected final int faceEnBits;

    /**
     * Layer index into GL_TEXTURE_2D_ARRAY. Encoded as UV.z per vertex.
     */
    protected final int layerIndex;

    /**
     * Tuple comparator sorting tuples by (String) name.
     */
    public static final Comparator<Tuple> TUPLE_COMP = (Tuple o1, Tuple o2) -> o1.getName().compareTo(o2.getName());
    /**
     * Unique string name for this tuple, derived from texture name and face enabled bits.
     * Used for hashing and equality checks.
     */
    public final String texName;

    /**
     * Construct new tuple by definition texName x face-enabled-bits.
     * Layer index is resolved from TEX_LAYER_MAP.
     *
     * @param texName    texture name
     * @param faceEnBits face enabled bits
     */
    public Tuple(String texName, int faceEnBits) {
        this.texName = texName;
        this.faceEnBits = faceEnBits;
        this.layerIndex = TEX_LAYER_MAP.getOrDefault(texName, 0);
        this.name = String.format("%s%02d", texName, faceEnBits);

        int numberOfOnes = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            if ((faceEnBits & (1 << j)) != 0) {
                numberOfOnes++;
            }
        }

        this.facesNum = numberOfOnes;
        this.verticesNum = 4 * numberOfOnes;
        this.indicesNum = 6 * numberOfOnes;
    }

    /**
     * Copy constructor. Creates a new tuple with the same properties and a copy of the block list.
     *
     * @param original to copy properties from
     */
    public Tuple(Tuple original) {
        this.texName = original.texName;
        this.faceEnBits = original.faceEnBits;
        this.layerIndex = original.layerIndex;
        this.name = String.format("%s%02d", original.texName(), original.faceBits());
        this.facesNum = original.facesNum;
        this.verticesNum = original.verticesNum;
        this.indicesNum = original.indicesNum;

        this.blockList.clear();
        this.blockList.addAll(original.blockList);
    }

    /**
     * Gets Block from the tuple block list. Complexity is O(log(n)+k).
     *
     * @param pos Vector3f position of the block
     * @return block if found (null if not found)
     */
    public Block getBlock(Vector3f pos) {
        String key = ModelUtils.blockSpecsToUniqueString(this.texName(), pos);

        int left = 0;
        int right = this.blockList.size() - 1;
        int startIndex = -1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            int cmp = Block.UNIQUE_BLOCK_CMP.compare(blockList.get(mid), blockList.get(mid)); // placeholder
            String midKey = ModelUtils.blockSpecsToUniqueString(this.texName(), blockList.get(mid).getPos());
            int c = midKey.compareTo(key);
            if (c < 0) left = mid + 1;
            else if (c > 0) right = mid - 1;
            else { startIndex = mid; right = mid - 1; }
        }

        left = 0; right = this.blockList.size() - 1;
        int endIndex = -1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            String midKey = ModelUtils.blockSpecsToUniqueString(this.texName(), blockList.get(mid).getPos());
            int c = midKey.compareTo(key);
            if (c < 0) left = mid + 1;
            else if (c > 0) right = mid - 1;
            else { endIndex = mid; left = mid + 1; }
        }

        if (startIndex != -1 && endIndex != -1) {
            for (int i = startIndex; i <= endIndex; i++) {
                Block b = blockList.get(i);
                if (b.getPos().equals(pos)) return b;
            }
        }

        return null;
    }

    /**
     * Gets Block from the tuple block list by id. Complexity is O(log(n)+k).
     *
     * @param pos   Vector3f position of the block
     * @param blkId block unique id
     * @return block if found (null if not found)
     */
    public Block getBlock(Vector3f pos, String blkId) {
        String key = blkId;

        int left = 0;
        int right = this.blockList.size() - 1;
        int startIndex = -1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            String midKey = ModelUtils.blockSpecsToUniqueString(this.texName(), blockList.get(mid).getPos());
            int c = midKey.compareTo(key);
            if (c < 0) left = mid + 1;
            else if (c > 0) right = mid - 1;
            else { startIndex = mid; right = mid - 1; }
        }

        left = 0; right = this.blockList.size() - 1;
        int endIndex = -1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            String midKey = ModelUtils.blockSpecsToUniqueString(this.texName(), blockList.get(mid).getPos());
            int c = midKey.compareTo(key);
            if (c < 0) left = mid + 1;
            else if (c > 0) right = mid - 1;
            else { endIndex = mid; left = mid + 1; }
        }

        if (startIndex != -1 && endIndex != -1) {
            for (int i = startIndex; i <= endIndex; i++) {
                Block b = blockList.get(i);
                if (b.getPos().equals(pos)) return b;
            }
        }

        return null;
    }

    /**
     * Buffer vertices with vec3 UV (u, v, layerIndex).
     * UV layout (location=2) is now vec3: xy=atlas UV, z=texture array layer.
     * Vertex.SIZE must be updated to 9 floats (pos3 + normal3 + uv3).
     */
    /**
     * Buffer vertices with vec3 UV (u, v, layerIndex).
     * UV layout (location=2) is now vec3: xy=atlas UV, z=texture array layer.
     * Writes 9 floats per vertex: pos(3) + normal(3) + uv(2) + layer(1).
     */
    @Override
    public boolean bufferVertices() {
        return true;
    }

    /**
     * SubBuffer vertex data prior rendering. And after at least one vertex
     * data buffering.
     *
     * @return if vertex data was successfully buffered
     */
    @Override
    public boolean subBufferVertices() {
        return true;
    }

    /**
     * Buffer VEC4 colors - instanced rendering
     *
     * @return buffered success
     */
    protected boolean bufferColors() {
        return true;
    }

    /**
     * Buffer Model MAT4 [col0, col1, col2, col3]
     *
     * @return buffered success
     */
    protected boolean bufferModelMatrices() {
        return true;
    }

    @Override
    public void bufferAll() {
        buffered = bufferVertices() && bufferColors() && bufferModelMatrices() && bufferIndices();
    }

    @Override
    public void animate() {

    }

    @Override
    public void prepare(boolean cameraInFluid) {

    }

    /**
     * Heavy operation to make underwater ambient if camera in fluid.
     *
     * @param camFront      camera front vec3
     * @param cameraInFluid boolean condition if camera is in fluid
     */
    public void prepare(Vector3f camFront, boolean cameraInFluid) {

    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + this.indicesNum;
        hash = 97 * hash + this.verticesNum;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Tuple other = (Tuple) obj;
        if (this.indicesNum != other.indicesNum) return false;
        if (this.verticesNum != other.verticesNum) return false;
        return Objects.equals(this.name, other.name);
    }

    /**
     * Returns the texture name associated with this tuple.
     */
    public String texName() {
        return texName;
    }

    /**
     * Returns the face enabled bits for this tuple.
     */
    public int faceBits() {
        return faceEnBits;
    }

    /**
     * Returns true if this tuple is solid (not water/fluid).
     * Uses layerIndex comparison instead of string comparison.
     */
    public boolean isSolid() {
        return layerIndex != WATER_LAYER_INDEX;
    }

    public int getLayerIndex() {
        return layerIndex;
    }

    public int getVec4Vbo() {
        return vec4Vbo;
    }

    public int getMat4Vbo() {
        return mat4Vbo;
    }

    public String getName() {
        return name;
    }

    public int getIbo() {
        return ibo;
    }

    public FloatBuffer getVec4FloatColorBuff() {
        return vec4FloatColorBuff;
    }

    public FloatBuffer getMat4FloatModelBuff() {
        return mat4FloatModelBuff;
    }

    public IntBuffer getIntBuff() {
        return intBuff;
    }

    @Override
    public String toString() {
        return "Tuple{" + "name=" + name + ", layer=" + layerIndex + '}';
    }

    public int getVerticesNum() {
        return verticesNum;
    }

    public int getFacesNum() {
        return facesNum;
    }
}

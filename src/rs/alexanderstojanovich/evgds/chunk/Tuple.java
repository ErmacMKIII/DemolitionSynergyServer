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
package rs.alexanderstojanovich.evgds.chunk;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.Objects;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evgds.models.Block;
import rs.alexanderstojanovich.evgds.util.ModelUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Tuple extends Series {
    // tuple is distinct rendering object for instanced rendering
    // all blocks in the tuple have the same properties, 
    // like model matrices, color and texture name, and enabled faces in 6-bit represenation
    // iboMap is not used here

    public static final int VEC2_SIZE = 2;
    public static final int VEC3_SIZE = 3;
    public static final int VEC4_SIZE = 4;
    public static final int MAT4_SIZE = 16;

    protected int vec4Vbo = 0; // color
    protected static FloatBuffer vec4FloatColorBuff = null;

    protected int mat4Vbo = 0; // model matrix [col0, col1, col2, col3]
    protected static FloatBuffer mat4FloatModelBuff = null;

    protected final String name;

    protected final int facesNum;
    protected final int faceEnBits;
    protected final String texName;

    /**
     * Tuple comparator sorting tuples by (String) name.
     */
    public static final Comparator<Tuple> TUPLE_COMP = (Tuple o1, Tuple o2) -> o1.getName().compareTo(o2.getName());

    /**
     * Construct new tuple by definition texName x face-enabled-bits
     *
     * @param texName texture name
     * @param faceEnBits face enabled bits
     */
    public Tuple(String texName, int faceEnBits) {
        this.texName = texName;
        this.faceEnBits = faceEnBits;
        this.name = String.format("%s%02d", texName, faceEnBits);

        int numberOfOnes = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            if ((faceEnBits & mask) != 0) {
                numberOfOnes++;
            }
        }

        this.facesNum = numberOfOnes;
        this.verticesNum = 4 * numberOfOnes; // affects buffering of vertices
        this.indicesNum = 6 * numberOfOnes; // affect buffering of indices
    }

    /**
     * Construct new tuple by definition texName x face-enabled-bits from
     * original tuple.
     *
     * @param original to copy properties from
     */
    public Tuple(Tuple original) {
        this.texName = original.texName;
        this.faceEnBits = original.faceEnBits;
        this.name = String.format("%s%02d", original.texName(), original.faceBits());
        this.facesNum = original.facesNum;
        this.verticesNum = original.verticesNum; // affects buffering of vertices
        this.indicesNum = original.indicesNum; // affect buffering of indices

        this.blockList.clear();
        this.blockList.addAll(original.blockList);
    }

    /**
     * Gets Block from the tuple block list (duplicates may exist but in very
     * low quantity). Complexity is O(log(n)+k).
     *
     * @param pos Vector3f position of the block
     * @return block if found (null if not found)
     */
    public Block getBlock(Vector3f pos) {
        Integer key = ModelUtils.blockSpecsToUniqueInt(isSolid(), this.texName(), this.faceBits(), pos);

        int left = 0;
        int right = this.blockList.size() - 1;
        int startIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = this.blockList.get(mid);
            Integer candInt = candidate.getId();
            int res = candInt.compareTo(key);
            if (res < 0) {
                left = mid + 1;
            } else if (res == 0) {
                startIndex = mid;
                right = mid - 1;
            } else {
                right = mid - 1;
            }
        }

        left = 0;
        right = this.blockList.size() - 1;
        int endIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = this.blockList.get(mid);
            Integer candInt = candidate.getId();
            int res = candInt.compareTo(key);
            if (res < 0) {
                left = mid + 1;
            } else if (res == 0) {
                endIndex = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        if (startIndex != -1 && endIndex != -1) {
            for (int i = startIndex; i <= endIndex; i++) {
                Block blk = this.blockList.get(i);
                if (blk.pos.equals(pos)) {
                    return blk;
                }
            }
        }

        return null;
    }

    /**
     * Gets Block from the tuple block list (duplicates may exist but in very
     * low quantity). Complexity is O(log(n)+k). Faster than method with
     * supplied Vec3f position.
     *
     * @param pos Vector3f position of the block
     * @param blkId block unique id
     * @return block if found (null if not found)
     */
    public Block getBlock(Vector3f pos, int blkId) {
        Integer key = blkId;

        int left = 0;
        int right = this.blockList.size() - 1;
        int startIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = this.blockList.get(mid);
            Integer candInt = candidate.getId();
            int res = candInt.compareTo(key);
            if (res < 0) {
                left = mid + 1;
            } else if (res == 0) {
                startIndex = mid;
                right = mid - 1;
            } else {
                right = mid - 1;
            }
        }

        left = 0;
        right = this.blockList.size() - 1;
        int endIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = this.blockList.get(mid);
            Integer candInt = candidate.getId();
            int res = candInt.compareTo(key);
            if (res < 0) {
                left = mid + 1;
            } else if (res == 0) {
                endIndex = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        if (startIndex != -1 && endIndex != -1) {
            for (int i = startIndex; i <= endIndex; i++) {
                Block blk = this.blockList.get(i);
                if (blk.pos.equals(pos)) {
                    return blk;
                }
            }
        }

        return null;
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tuple other = (Tuple) obj;
        if (this.indicesNum != other.indicesNum) {
            return false;
        }
        if (this.verticesNum != other.verticesNum) {
            return false;
        }
        return Objects.equals(this.name, other.name);
    }

    public String texName() {
        return texName;
    }

    public int faceBits() {
        return faceEnBits;
    }

    public boolean isSolid() {
        return !texName().equals("water");
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
        return "Tuple{" + "name=" + name + '}';
    }

    public int getVerticesNum() {
        return verticesNum;
    }

    public int getFacesNum() {
        return facesNum;
    }

}

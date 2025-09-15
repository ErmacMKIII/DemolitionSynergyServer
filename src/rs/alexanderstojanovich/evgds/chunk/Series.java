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
import org.magicwerk.brownies.collections.IList;
import org.magicwerk.brownies.collections.Key1List;
import rs.alexanderstojanovich.evgds.main.Configuration;
import rs.alexanderstojanovich.evgds.models.Block;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Series { // mutual class for both solid blocks and fluid blocks with improved rendering

    public static final int DYNAMIC_INCREMENT = Configuration.getInstance().getBlockDynamicSize();

    /**
     * List of (environment) blocks with access keys to chunk id
     */
    public final Key1List<Block, Integer> blockList = new Key1List.Builder<Block, Integer>()
            .withKey1Map(blk -> Chunk.chunkFunc(blk.pos))
            .withKey1Sort(true)
            .build();
    protected int bigVbo = 0;
    // array with offsets in the big float buffer
    // this is maximum amount of blocks of the type game can hold
    // --------------blkIndex---ibo-----------------------------
    protected boolean buffered = false;

//    protected static int dynamicSize = DYNAMIC_INCREMENT;
    protected static FloatBuffer bigFloatBuff = null;

    protected static IntBuffer intBuff;
    protected int ibo = 0;

    protected int indicesNum; // not used (unless in Tuples)
    protected int verticesNum; // not used (unless in Tuples)

    public Series() {
        verticesNum = Block.VERTEX_COUNT;
        indicesNum = Block.INDICES_COUNT;
    }

    public static int checkSize(int bitValue) {
        // Initialize a counter for counting ones
        int onesCount = 0;

        // Iterate until the number becomes 0
        while (bitValue != 0) {
            // Use bitwise AND operation to check if the least significant bit is 1
            // If it is, increment the counter
            onesCount += bitValue & 1;
            // Right shift the number by 1 bit to process the next bit
            bitValue >>= 1;
        }

        return onesCount * 6;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public IList<Block> getBlockList() {
        return blockList;
    }

    public int getBigVbo() {
        return bigVbo;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

}

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
package rs.alexanderstojanovich.evgds.level;

import java.util.HashMap;
import java.util.Map;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evgds.chunk.Chunks;
import rs.alexanderstojanovich.evgds.chunk.Tuple;
import rs.alexanderstojanovich.evgds.core.Camera;
import rs.alexanderstojanovich.evgds.main.Configuration;
import rs.alexanderstojanovich.evgds.main.GameObject;
import rs.alexanderstojanovich.evgds.models.Block;
import rs.alexanderstojanovich.evgds.resources.Assets;

/**
 * Module with blocks from all the chunks. Effectively ready for rendering after
 * optimization.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class BlockEnvironment {

    public static final int LIGHT_MASK = 0x01;
    public static final int WATER_MASK = 0x02;
    public static final int SHADOW_MASK = 0x04;

    public final GameObject gameObject;

    /**
     * Working tuples (from update)
     */
    protected volatile IList<Tuple> workingTuples = new GapList<>();
    /**
     * Optimizes tuples (from render)
     */
    protected volatile IList<Tuple> optimizedTuples = new GapList<>();
    /**
     * Lookup table for faster tuple access (avoiding multiple filters)
     */
    protected final Map<String, Map<Integer, Tuple>> tupleLookup = new HashMap<>();

    /**
     * Modified tuples (from update/render). Meaning from update they are
     * modified and need to be pushed to render.
     */
//    public final IList<Tuple> modifiedTuples = new GapList<>();
    protected volatile boolean optimizing = false;
    protected final Chunks chunks;

    protected int lastFaceBits = 0; // starting from one, cuz zero is not rendered
    public static final int NUM_OF_PASSES_MAX = Configuration.getInstance().getOptimizationPasses();
    public final IList<String> modifiedWorkingTupleNames = new GapList<>();

    public BlockEnvironment(GameObject gameObject, Chunks chunks) {
        this.gameObject = gameObject;
        this.chunks = chunks;
    }

    /**
     * Optimization for tuples from all the chunks. The world is built
     * incrementally and consists of two passes. Also includes modifications to
     * work with Tuple Buffer Object (TBO). Modified tuples are pushed to the
     * optimized stream. (Chat GPT)
     *
     * @param vqueue visible chunkId queue
     * @param camera in-game camera
     */
    public void optimizeByControl(IList<Integer> vqueue, Camera camera) {
        optimizing = true;

        // Determine lastFaceBits mask
        final int mask0 = Block.getVisibleFaceBitsFast(camera.getFront(), LevelContainer.actorInFluid ? 0f : 45f);
        workingTuples.removeIf(ot -> (ot.faceBits() & mask0) == 0);

        int lastFaceBitsCopy = lastFaceBits;

        // Create a lookup table for faster tuple access (avoiding multiple filters)
        tupleLookup.clear();
        for (Tuple t : workingTuples) {
            tupleLookup
                    .computeIfAbsent(t.texName(), k -> new HashMap<>())
                    .put(t.faceBits(), t);
        }

        for (String tex : Assets.TEX_WORLD) {
            for (int j = 0; j < NUM_OF_PASSES_MAX; j++) {
                final int faceBits = (++lastFaceBitsCopy) & 63;
                if ((faceBits & (mask0 & 63)) != 0) {
                    // PASS 1: Fetch or Create Tuple
                    Tuple optmTuple = tupleLookup
                            .computeIfAbsent(tex, k -> new HashMap<>())
                            .computeIfAbsent(faceBits, fb -> {
                                Tuple newTuple = new Tuple(tex, fb);
                                workingTuples.add(newTuple);
                                return newTuple;
                            });

                    // PASS 2: Process Chunks and Fill Tuples
                    final IList<Block> selectedBlockList = chunks.getFilteredBlockList(tex, faceBits, vqueue);
                    if (selectedBlockList != null) {
                        boolean modified = optmTuple.blockList.addAll(
                                selectedBlockList.filter(blk -> blk.getTexName().equals(tex) && blk.getFaceBits() == faceBits
                                && camera.doesSeeEff(blk, 30f) && !optmTuple.blockList.contains(blk))
                        );

                        if (modified) {
                            modifiedWorkingTupleNames.addIfAbsent(optmTuple.getName());
                        }
                    }
                }
            }
        }

        lastFaceBits += NUM_OF_PASSES_MAX;
        lastFaceBits &= 63;

        if (lastFaceBits == 0) {
            workingTuples.sort(Tuple.TUPLE_COMP);

            // Only process modified tuples
            workingTuples
                    .filter(wt -> modifiedWorkingTupleNames.contains(wt.getName()))
                    .forEach(wt -> {
                        wt.blockList.sort(Block.UNIQUE_BLOCK_CMP);
                        wt.setBuffered(false);
                    });

            // Remove empty tuples
            workingTuples.removeIf(wt -> wt.blockList.isEmpty());

            modifiedWorkingTupleNames.clear();
            swap();
        }

        optimizing = false;
    }

    /**
     * Push changes. Push working tuples to optimizing tuples. By coping each
     * optimizing to working.
     */
    public void push() {
        optimizedTuples = workingTuples.copy();
    }

    /**
     * Swap working tuples with optimized tuples. What was built by optimization
     * could be rendered (drawn). Called from Game Renderer.
     */
    public void swap() {
        IList<Tuple> temp = optimizedTuples;
        optimizedTuples = workingTuples;
        workingTuples = temp;
    }

    /**
     * Pull from recent. Pull optimized tuples to working tuples. By coping each
     * working to optimized.
     */
    public void pull() {
        workingTuples.addAll(optimizedTuples.filter(ot -> !workingTuples.contains(ot)));
        workingTuples.sort(Tuple.TUPLE_COMP);
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    /**
     * Clear working & optimization tuples
     */
    public void clear() {
        workingTuples.clear();
        optimizedTuples.clear();
    }

    public IList<Tuple> getOptimizedTuples() {
        return optimizedTuples;
    }

    public boolean isOptimizing() {
        return optimizing;
    }

    public Chunks getChunks() {
        return chunks;
    }

    public int getBitPos() {
        return lastFaceBits;
    }

    public int getLastFaceBits() {
        return lastFaceBits;
    }

    public IList<Tuple> getWorkingTuples() {
        return workingTuples;
    }

    public IList<String> getModifiedWorkingTupleNames() {
        return modifiedWorkingTupleNames;
    }

}

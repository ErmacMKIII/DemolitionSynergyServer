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

import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evgds.chunk.Chunk;
import rs.alexanderstojanovich.evgds.chunk.Chunks;
import rs.alexanderstojanovich.evgds.chunk.Tuple;
import rs.alexanderstojanovich.evgds.core.Camera;
import rs.alexanderstojanovich.evgds.light.LightSources;
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
     * Modified tuples (from update/render). Meaning from update they are
     * modified and need to be pushed to render.
     */
//    public final IList<Tuple> modifiedTuples = new GapList<>();
    protected volatile boolean optimizing = false;
    protected volatile boolean fullyOptimized = false;
    protected final Chunks chunks;
    protected int texProcIndex = 0;

    protected int lastFaceBits = 0; // starting from one, cuz zero is not rendered
    public static final int NUM_OF_PASSES_MAX = Configuration.getInstance().getOptimizationPasses();
    public final IList<String> modifiedWorkingTupleNames = new GapList<>();

    public BlockEnvironment(GameObject gameObject, Chunks chunks) {
        this.gameObject = gameObject;
        this.chunks = chunks;
    }

    /**
     * Basic version of optimization for tuples from all the chunks. (Deprecated
     * as clear/new operations are used constantly)
     *
     * @param queue visible chunkId queue
     */
    @Deprecated
    public void optimize(IList<Integer> queue) {
        optimizedTuples.clear();
        int faceBits = 1; // starting from one, cuz zero is not rendered               
        while (faceBits <= 63) {
            for (String tex : Assets.TEX_WORLD) {
                Tuple optmTuple = null;
                for (int chunkId : queue) {
                    Chunk chunk = chunks.getChunk(chunkId);
                    if (chunk != null) {
                        Tuple tuple = chunk.getTuple(tex, faceBits);
                        if (tuple != null) {
                            if (optmTuple == null) {
                                optmTuple = new Tuple(tex, faceBits);
                            }
                            optmTuple.blockList.addAll(tuple.blockList);
                        }
                    }
                }

                if (optmTuple != null) {
                    optimizedTuples.add(optmTuple);
                    optimizedTuples.sort(Tuple.TUPLE_COMP);
                }
            }
            faceBits++;
        }

    }

    /**
     * Improved version of optimization for tuples from all the chunks. World is
     * being built incrementally. Consist of two passes.
     *
     * @param vqueue visible chunkId queue
     * @param camera ingame camera
     */
    public void optimizeFast(IList<Integer> vqueue, Camera camera) {
        // determine lastFaceBits mask
        final int mask0 = Block.getVisibleFaceBitsFast(camera.getFront(), LevelContainer.actorInFluid ? 0f : 45f);
        optimizedTuples.removeIf(ot -> (ot.faceBits() & mask0) == 0);// some removals are made

        // determine texture type to process - split
        if (texProcIndex++ == Assets.TEX_WORLD.length - 1) {
            texProcIndex = 0;
        }

        final String tex = Assets.TEX_WORLD[texProcIndex];

        // PASS 1 : CREATE TUPLES
        int lastFaceBitsCopy = lastFaceBits;
        for (int j = 0; j < NUM_OF_PASSES_MAX; j++) {
            // assign last value & increment to next value with limit to 63
            final int faceBits = (++lastFaceBitsCopy) & 63;
            if ((faceBits & (mask0 & 63)) != 0) {
                final Tuple optmTuple = optimizedTuples.getIf(ot -> ot.texName().equals(tex) && ot.faceBits() == faceBits);
                if (optmTuple == null) {
                    optimizedTuples.add(new Tuple(tex, faceBits));
                    // sort so it remains ordered
                    optimizedTuples.sort(Tuple.TUPLE_COMP);
                }
            }
        }

        // PASS 2 : FILL TUPLES
        lastFaceBitsCopy = lastFaceBits;
        for (int j = 0; j < NUM_OF_PASSES_MAX; j++) {
            // assign last value & increment to next value with limit to 63
            final int faceBits = (++lastFaceBitsCopy) & 63;
            if ((faceBits & (mask0 & 63)) != 0) {
                chunks.chunkList.forEach(chnk -> { // for all chunks
                    if (vqueue.contains(chnk.id)) { // visible ones && not cached!
                        // select correlated tuples
                        final IList<Tuple> selectedTuples = chnk.tupleList.filter(t -> t.texName().equals(tex) && t.faceBits() == faceBits);
                        // for each selected tuple
                        selectedTuples.forEach(st -> {
                            final Tuple optmTuple = optimizedTuples.getIf(ot -> ot.texName().equals(tex) && ot.faceBits() == faceBits);
                            boolean modified = false;
                            // if fullyOptimized doesn't exist
                            for (Block blk : st.blockList) {
                                // take into consideration if could be seen by camera (impr. method)
                                if (camera.doesSeeEff(blk)) {
                                    // add absent blocks
                                    modified |= optmTuple.blockList.addIfAbsent(blk);
                                }
                            }
                            if (modified) {
                                // sort so it does remains ordered
                                optmTuple.blockList.sort(Block.UNIQUE_BLOCK_CMP);
                                // sets TBO to unbuffer if modified
                                optmTuple.setBuffered(false);
                            }
                        });
                    }
                });
            }
        }

        // move forward (with increment)
        lastFaceBits += NUM_OF_PASSES_MAX;

        // Remove empty optimization tuples
        optimizedTuples.removeIf(ot -> ot.blockList.isEmpty());

        // if last bits is processed start from beginning next time
        if (lastFaceBits == 64) {
            lastFaceBits = 0;
        }

        // if full circle with all textures & facebits has been completed
        if (texProcIndex == 0 && lastFaceBits == 0) {
            fullyOptimized = true;
        }

    }

    /**
     * Improved version of optimization for tuples from all the chunks. The
     * world is built incrementally and consists of two passes. Also includes
     * modifications to work with Tuple Buffer Object (TBO). Modified tuples are
     * pushed to the optimized stream. (Chat GPT)
     *
     * @param vqueue visible chunkId queue
     * @param camera in-game camera
     */
    public void optimizeByControl(IList<Integer> vqueue, Camera camera) {
        optimizing = true;

        // Determine lastFaceBits mask
        final int mask0 = Block.getVisibleFaceBitsFast(camera.getFront(), LevelContainer.actorInFluid ? 0f : 45f);
        workingTuples.removeIf(ot -> (ot.faceBits() & mask0) == 0);

        final String tex = Assets.TEX_WORLD[texProcIndex];
        int lastFaceBitsCopy = lastFaceBits;

        for (int j = 0; j < NUM_OF_PASSES_MAX; j++) {
            final int faceBits = (++lastFaceBitsCopy) & 63;
            if ((faceBits & (mask0 & 63)) != 0) {
                // PASS 1: Create Tuples
                Tuple optmTuple = workingTuples
                        .filter(ot -> ot != null && ot.texName().equals(tex) && ot.faceBits() == faceBits)
                        .getFirstOrNull();
                if (optmTuple == null) {
                    optmTuple = new Tuple(tex, faceBits);
                    workingTuples.add(optmTuple);
                }
                optmTuple.blockList.clear(); // cleaning!

                // PASS 2: Fill Tuples
                synchronized (chunks) {
                    chunks.chunkList
                            .filter(chnk -> vqueue.contains(chnk.id) && Chunk.doesSeeChunk(chnk.id, camera, 5f))
                            .forEach(chnk -> {
                                final Tuple workTuple = workingTuples
                                        .filter(ot -> ot != null && ot.texName().equals(tex) && ot.faceBits() == faceBits)
                                        .getFirstOrNull();
                                final IList<Tuple> selectedTuples = chnk.tupleList
                                        .filter(t -> t != null && t.texName().equals(tex) && t.faceBits() == faceBits);

                                if (workTuple != null) {
                                    selectedTuples.forEach(st -> {
                                        boolean modified = workTuple.blockList.addAll(
                                                st.blockList.filter(blk -> blk != null && camera.doesSeeEff(blk, 75f) && !workTuple.blockList.contains(blk))
                                        );
                                        if (modified) {
                                            modifiedWorkingTupleNames.addIfAbsent(workTuple.getName());
                                        }
                                    });
                                }
                            });
                }
            }
        }

        lastFaceBits += NUM_OF_PASSES_MAX;

        if (texProcIndex++ == Assets.TEX_WORLD.length - 1) {
            texProcIndex = 0;
        }

        workingTuples.removeIf(wt -> wt.blockList.isEmpty());

        if (lastFaceBits == 64) {
            lastFaceBits = 0;
        }

        if (texProcIndex == 0 && lastFaceBits == 0) {
            workingTuples.sort(Tuple.TUPLE_COMP);
            workingTuples
                    .filter(wt -> modifiedWorkingTupleNames.contains(wt.getName()))
                    .forEach(wt -> {
                        wt.blockList.sort(Block.UNIQUE_BLOCK_CMP);
                        wt.setBuffered(false);
                    });
            modifiedWorkingTupleNames.clear();

            fullyOptimized = true;
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

        fullyOptimized = false;
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

    public int getTexProcIndex() {
        return texProcIndex;
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

    public boolean isFullyOptimized() {
        return fullyOptimized;
    }

    public IList<String> getModifiedWorkingTupleNames() {
        return modifiedWorkingTupleNames;
    }

}

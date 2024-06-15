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
package rs.alexanderstojanovich.evgds.level;

import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evgds.chunk.Chunk;
import rs.alexanderstojanovich.evgds.chunk.Tuple;
import rs.alexanderstojanovich.evgds.core.Camera;
import static rs.alexanderstojanovich.evgds.level.LevelContainer.AllBlockMap;
import rs.alexanderstojanovich.evgds.location.TexByte;
import static rs.alexanderstojanovich.evgds.main.GameObject.UPDATE_RENDER_IFC_MUTEX;
import static rs.alexanderstojanovich.evgds.main.GameObject.updateRenderLCLock;
import rs.alexanderstojanovich.evgds.models.Block;
import rs.alexanderstojanovich.evgds.resources.Assets;
import rs.alexanderstojanovich.evgds.util.GlobalColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Editor {

    private static Block selectedNew = null;
    private static int blockColorNum = 0;

    private static Block selectedCurr = null;

    private static int texValue = 0; // value about which texture to use

    protected static final Block Decal = new Block("decal", new Vector3f(), GlobalColors.GREEN_RGBA, true);
    protected static boolean DecalActive = false;

    public static void selectNew(LevelContainer lc) {
        deselect();
        if (selectedNew == null) {
            selectedNew = new Block("crate");
        }
        selectTexture(lc.gameObject.GameAssets);
        // fetching..
        Camera camera = lc.levelActors.mainCamera();
        Vector3f pos = camera.getPos();
        Vector3f front = camera.getFront();

        final float skyboxWidth = LevelContainer.SKYBOX_WIDTH;
        // initial calculation (make it dependant to point player looking at)
        // and make it follows player camera        
        selectedNew.getPos().x = (Math.round(8.0f * front.x) + Math.round(pos.x) & 0xFFFFFFFE) % Math.round(skyboxWidth + 1);
        selectedNew.getPos().y = (Math.round(8.0f * front.y) + Math.round(pos.y) & 0xFFFFFFFE) % Math.round(skyboxWidth + 1);
        selectedNew.getPos().z = (Math.round(8.0f * front.z) + Math.round(pos.z) & 0xFFFFFFFE) % Math.round(skyboxWidth + 1);

        if (!cannotPlace(lc)) {
            Decal.pos = selectedNew.pos;
            Decal.setScale(1.05f);
            Decal.setPrimaryRGBAColor(GlobalColors.GREEN_RGBA);
            DecalActive = true;
        }
    }

    public static void selectCurrSolid(LevelContainer lc) {
        deselect();
        Vector3f cameraPos = lc.levelActors.mainCamera().getPos();
        Vector3f cameraFront = lc.levelActors.mainCamera().getFront();

        final int face = Block.getRayTraceSingleFaceFast(cameraFront, 45f);
        final int mask = 1 << face;

        final float stepAmount = 0.125f;
        Vector3f temp = new Vector3f();
        // iterater through faces
        SCAN:
        // detect blocks
        for (float amount = 0.0f; amount <= Chunk.VISION; amount += stepAmount) { // double amount precision
            // possible block location
            Vector3f adjPos = cameraPos.fma(amount, cameraFront, temp);
            Vector3f adjPosAlign = new Vector3f(
                    Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                    Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                    Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
            );

            // detect ray intersection
            TexByte locVal = AllBlockMap.getLocation(adjPosAlign);
            if (locVal != null && locVal.solid && Block.intersectsRay(adjPosAlign, cameraFront, cameraPos)) {
                Chunk chunk = lc.chunks.getChunk(Chunk.chunkFunc(adjPosAlign));
                if (chunk != null) {
                    Tuple tuple = chunk.getTuple(locVal.texName, (int) ((~locVal.byteValue & 63) | mask));
                    if (tuple != null) {
                        Block blk = tuple.getBlock(adjPosAlign);
                        if (blk != null) {
                            selectedCurr = blk;
                        } else {
                            // heavy search on chunk boundaries
                            blk = tuple.blockList.getIf(blk0 -> blk0.pos.equals(adjPosAlign));
                            selectedCurr = blk;
                        }
                        break SCAN;
                    } else {
                        // heavy search on chunk boundaries
                        Block blk = chunk.getBlockList().getIf(blk0 -> blk0.pos.equals(adjPosAlign));
                        selectedCurr = blk;
                        break SCAN;
                    }
                } else {
                    // heavy search on chunk boundaries
                    Block blk = lc.chunks.getTotalList().getIf(blk0 -> blk0.pos.equals(adjPosAlign));
                    selectedCurr = blk;
                    break SCAN;
                }
            }
        }
        if (selectedCurr != null) {
            Decal.pos = selectedCurr.pos;
            Decal.setScale(1.05f);
            Decal.setPrimaryRGBAColor(GlobalColors.YELLOW_RGBA);
            DecalActive = true;
        }
    }

    public static void selectCurrFluid(LevelContainer lc) {
        deselect();
        Vector3f cameraPos = lc.levelActors.mainCamera().getPos();
        Vector3f cameraFront = lc.levelActors.mainCamera().getFront();

        final int face = Block.getRayTraceSingleFaceFast(cameraFront, 45f);
        final int mask = 1 << face;

        final float stepAmount = 0.125f;
        Vector3f temp = new Vector3f();
        // iterater through faces
        SCAN:
        // detect blocks
        for (float amount = 0.0f; amount <= Chunk.VISION; amount += stepAmount) { // double amount precision
            // possible block location
            Vector3f adjPos = cameraPos.fma(amount, cameraFront, temp);
            Vector3f adjPosAlign = new Vector3f(
                    Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                    Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                    Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
            );

            // detect ray intersection
            TexByte locVal = AllBlockMap.getLocation(adjPosAlign);
            if (locVal != null && !locVal.solid && Block.intersectsRay(adjPosAlign, cameraFront, cameraPos)) {
                Chunk chunk = lc.chunks.getChunk(Chunk.chunkFunc(adjPosAlign));
                if (chunk != null) {
                    Tuple tuple = chunk.getTuple(locVal.texName, (int) ((~locVal.byteValue & 63) | mask));
                    if (tuple != null) {
                        Block blk = tuple.getBlock(adjPosAlign);
                        if (blk != null) {
                            selectedCurr = blk;
                        } else {
                            // heavy search on chunk boundaries
                            blk = tuple.blockList.getIf(blk0 -> blk0.pos.equals(adjPosAlign));
                            selectedCurr = blk;
                        }
                        break SCAN;
                    } else {
                        // heavy search on chunk boundaries
                        Block blk = chunk.getBlockList().getIf(blk0 -> blk0.pos.equals(adjPosAlign));
                        selectedCurr = blk;
                        break SCAN;
                    }
                } else {
                    // heavy search on chunk boundaries
                    Block blk = lc.chunks.getTotalList().getIf(blk0 -> blk0.pos.equals(adjPosAlign));
                    selectedCurr = blk;
                    break SCAN;
                }
            }
        }

        if (selectedCurr != null) {
            Decal.pos = selectedCurr.pos;
            Decal.setScale(1.05f);
            Decal.setPrimaryRGBAColor(GlobalColors.YELLOW_RGBA);
            DecalActive = true;
        }
    }

    public static void deselect() {
        selectedNew = selectedCurr = null;
        DecalActive = false;
    }

    public static void selectAdjacentSolid(LevelContainer lc, int position) {
        deselect();
        selectCurrSolid(lc);
        if (selectedCurr != null) {
            if (selectedNew == null) {
                selectedNew = new Block("crate");
            }
            selectTexture(lc.gameObject.GameAssets);
            selectedNew.getPos().x = selectedCurr.getPos().x;
            selectedNew.getPos().y = selectedCurr.getPos().y;
            selectedNew.getPos().z = selectedCurr.getPos().z;

            switch (position) {
                case Block.LEFT:
                    selectedNew.getPos().x -= selectedCurr.getWidth() / 2.0f + selectedNew.getWidth() / 2.0f;
                    break;
                case Block.RIGHT:
                    selectedNew.getPos().x += selectedCurr.getWidth() / 2.0f + selectedNew.getWidth() / 2.0f;
                    break;
                case Block.BOTTOM:
                    selectedNew.getPos().y -= selectedCurr.getHeight() / 2.0f + selectedNew.getHeight() / 2.0f;
                    break;
                case Block.TOP:
                    selectedNew.getPos().y += selectedCurr.getHeight() / 2.0f + selectedNew.getHeight() / 2.0f;
                    break;
                case Block.BACK:
                    selectedNew.getPos().z -= selectedCurr.getDepth() / 2.0f + selectedNew.getDepth() / 2.0f;
                    break;
                case Block.FRONT:
                    selectedNew.getPos().z += selectedCurr.getDepth() / 2.0f + selectedNew.getDepth() / 2.0f;
                    break;
                default:
                    break;
            }

            if (!cannotPlace(lc)) {
                Decal.pos = selectedNew.pos;
                Decal.setScale(1.05f);
                Decal.setPrimaryRGBAColor(GlobalColors.BLUE_RGBA);
                DecalActive = true;
            }
        }
    }

    public static void selectAdjacentFluid(LevelContainer lc, int position) {
        deselect();
        selectCurrFluid(lc);
        if (selectedCurr != null) {
            if (selectedNew == null) {
                selectedNew = new Block("crate");
            }
            selectTexture(lc.gameObject.GameAssets);
            selectedNew.getPos().x = selectedCurr.getPos().x;
            selectedNew.getPos().y = selectedCurr.getPos().y;
            selectedNew.getPos().z = selectedCurr.getPos().z;

            switch (position) {
                case Block.LEFT:
                    selectedNew.getPos().x -= selectedCurr.getWidth() / 2.0f + selectedNew.getWidth() / 2.0f;
                    break;
                case Block.RIGHT:
                    selectedNew.getPos().x += selectedCurr.getWidth() / 2.0f + selectedNew.getWidth() / 2.0f;
                    break;
                case Block.BOTTOM:
                    selectedNew.getPos().y -= selectedCurr.getHeight() / 2.0f + selectedNew.getHeight() / 2.0f;
                    break;
                case Block.TOP:
                    selectedNew.getPos().y += selectedCurr.getHeight() / 2.0f + selectedNew.getHeight() / 2.0f;
                    break;
                case Block.BACK:
                    selectedNew.getPos().z -= selectedCurr.getDepth() / 2.0f + selectedNew.getDepth() / 2.0f;
                    break;
                case Block.FRONT:
                    selectedNew.getPos().z += selectedCurr.getDepth() / 2.0f + selectedNew.getDepth() / 2.0f;
                    break;
                default:
                    break;
            }

            if (!cannotPlace(lc)) {
                Decal.pos = selectedNew.pos;
                Decal.setScale(1.05f);
                Decal.setPrimaryRGBAColor(GlobalColors.BLUE_RGBA);
                DecalActive = true;
            }
        }
    }

    private static boolean cannotPlace(LevelContainer lc) {
        boolean cant = false;
        boolean placeOccupied = LevelContainer.AllBlockMap.isLocationPopulated(selectedNew.pos);
        //----------------------------------------------------------------------
        boolean intersects = false;
        int currChunkId = Chunk.chunkFunc(selectedNew.getPos());
        Chunk currSolidChunk = lc.chunks.getChunk(currChunkId);
        if (currSolidChunk != null) {
            for (Block solidBlock : currSolidChunk.getBlockList()) {
                intersects = selectedNew.intersectsExactly(solidBlock);
                if (intersects) {
                    break;
                }
            }
        }
        //----------------------------------------------------------------------
        boolean leavesSkybox = !LevelContainer.SKYBOX.intersectsEqually(selectedNew);
        if (selectedNew.isSolid()) {
            cant = placeOccupied || intersects || leavesSkybox;
        }
        if (cant) {
            Decal.pos = selectedNew.pos;
            Decal.setScale(1.05f);
            Decal.setPrimaryRGBAColor(GlobalColors.RED_RGBA);
            DecalActive = true;
        }
        return cant;
    }

    public static void add(LevelContainer lc) {
        if (selectedNew != null) {
            if (!cannotPlace(lc) && !lc.levelActors.mainCamera().intersects(selectedNew)) {
                updateRenderLCLock.lock();
                try {
                    lc.chunks.addBlock(selectedNew);
                } finally {
                    updateRenderLCLock.unlock();
                }
                selectedNew = new Block(Assets.TEX_WORLD[texValue]);
            }
        }
        deselect();
    }

    public static void remove(LevelContainer lc) {
        if (selectedCurr != null) {
            synchronized (UPDATE_RENDER_IFC_MUTEX) {
                updateRenderLCLock.lock();
                try {
                    lc.chunks.removeBlock(selectedCurr);
                } finally {
                    updateRenderLCLock.unlock();
                }
            }
        }
        deselect();
    }

    private static void selectTexture(Assets assets) {
        if (selectedNew != null) {
            String texName = assets.TEX_WORLD[texValue];
            selectedNew.setTexNameWithDeepCopy(texName);
        }
    }

    public static void selectPrevTexture(Assets assets) {
        if (selectedNew != null) {
            if (texValue > 0) {
                texValue--;
                selectTexture(assets);
            }
        }
    }

    public static void selectNextTexture(Assets assets) {
        if (selectedNew != null) {
            if (texValue < assets.TEX_WORLD.length - 1) {
                texValue++;
                selectTexture(assets);
            }
        }
    }

    public static void cycleBlockColor() {
        if (selectedNew != null) {
            GlobalColors.ColorName[] values = GlobalColors.ColorName.values();
            selectedNew.setPrimaryRGBAColor(new Vector4f(GlobalColors.getRGBAColorOrDefault(values[++blockColorNum % values.length])));
        }
    }

    public static Block getSelectedNew() {
        return selectedNew;
    }

    public static Block getSelectedCurr() {
        return selectedCurr;
    }

    public static int getBlockColorNum() {
        return blockColorNum;
    }

    public static int getTexValue() {
        return texValue;
    }

    public static Block getDecal() {
        return Decal;
    }

    public static boolean isDecalActive() {
        return DecalActive;
    }

}

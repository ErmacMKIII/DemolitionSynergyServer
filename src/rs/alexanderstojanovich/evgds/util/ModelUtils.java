/*
 * Copyright (C) 2023 coas9
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
package rs.alexanderstojanovich.evgds.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evgds.chunk.Chunk;
import rs.alexanderstojanovich.evgds.main.Game;
import rs.alexanderstojanovich.evgds.models.Material;
import rs.alexanderstojanovich.evgds.models.Mesh;
import rs.alexanderstojanovich.evgds.models.Model;
import rs.alexanderstojanovich.evgds.models.Vertex;
import rs.alexanderstojanovich.evgds.texture.Texture;

public class ModelUtils {

    /**
     * Read the obj and constructs the mode. Texture is assigned from the cache
     * by tex name.
     *
     * @param dirEntry directory entry for the model & texture
     * @param fileName obj filename
     * @param texName texName (in the cache)
     * @return new Model
     */
    public static Model readFromObjFile(String dirEntry, String fileName, String texName) {
        File extern = new File(dirEntry + fileName);
        File archive = new File(Game.DATA_ZIP);
        ZipFile zipFile = null;
        InputStream objInput = null;
        if (extern.exists()) {
            try {
                objInput = new FileInputStream(extern);
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else if (archive.exists()) {
            try {
                zipFile = new ZipFile(archive);
                for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                    if (zipEntry.getName().equals(dirEntry + fileName)) {
                        objInput = zipFile.getInputStream(zipEntry);
                        break;
                    }
                }
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else {
            DSLogger.reportError("Cannot find zip archive " + Game.DATA_ZIP + " or relevant ingame files!", null);
        }
        //----------------------------------------------------------------------
        if (objInput == null) {
            DSLogger.reportError("Cannot find resource " + dirEntry + fileName + "!", null);
            return null;
        }

        Model result = new Model(fileName, texName);
        Mesh mesh = new Mesh();
        BufferedReader br = new BufferedReader(new InputStreamReader(objInput));
        IList<Vector2f> uvs = new GapList<>();
        IList<Vector3f> normals = new GapList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] things = line.split(" ");
                if (things[0].equals("v")) {
                    Vector3f pos = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    Vertex vertex = new Vertex(pos);
                    mesh.vertices.add(vertex);
                } else if (things[0].equals("vt")) {
                    Vector2f uv = new Vector2f(Float.parseFloat(things[1]), 1.0f - Float.parseFloat(things[2]));
                    uvs.add(uv);
                } else if (things[0].equals("vn")) {
                    Vector3f normal = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    normals.add(normal);
                } else if (things[0].equals("f")) {
                    String[] subThings = {things[1], things[2], things[3]};
                    for (String subThing : subThings) {
                        String[] data = subThing.split("/");
                        int index = Integer.parseInt(data[0]) - 1;
                        mesh.indices.add(index);
                        Vertex vertex = mesh.vertices.get(index);
                        if (data.length >= 2 && !data[1].isEmpty()) {
                            vertex.setUv(uvs.get(Integer.parseInt(data[1]) - 1));
                        }
                        if (data.length >= 3 && !data[2].isEmpty()) {
                            mesh.vertices.get(index).setNormal(normals.get(Integer.parseInt(data[2]) - 1));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            try {
                objInput.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                    DSLogger.reportFatalError(ex.getMessage(), ex);
                }
            }
        }

        Material material = new Material(Texture.getOrDefault(texName));
        material.setColor(new Vector4f(GlobalColors.WHITE_RGBA));
        result.materials.add(material);

        result.meshes.add(mesh);
        result.calcDimsPub();

        return result;
    }

    /**
     * Read the obj and constructs the mode.Texture is assigned from the cache
     * by tex name.
     *
     * @param dirEntry directory entry for the model & texture
     * @param fileName obj filename
     * @param texName texName (in the cache)
     * @param gridSize grid size of texture atlas (non-zero, positive number)
     * @param reverseFaces reverse face (for face culling)
     * @return new Model
     */
    public static Model readFromObjFile(String dirEntry, String fileName, String texName, int gridSize, boolean reverseFaces) {
        File extern = new File(dirEntry + fileName);
        File archive = new File(Game.DATA_ZIP);
        ZipFile zipFile = null;
        InputStream objInput = null;
        if (extern.exists()) {
            try {
                objInput = new FileInputStream(extern);
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else if (archive.exists()) {
            try {
                zipFile = new ZipFile(archive);
                for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                    if (zipEntry.getName().equals(dirEntry + fileName)) {
                        objInput = zipFile.getInputStream(zipEntry);
                        break;
                    }
                }
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else {
            DSLogger.reportError("Cannot find zip archive " + Game.DATA_ZIP + " or relevant ingame files!", null);
        }
        //----------------------------------------------------------------------
        if (objInput == null) {
            DSLogger.reportError("Cannot find resource " + dirEntry + fileName + "!", null);
            return null;
        }

        int texIndex = Texture.getOrDefaultIndex(texName);
        int row = texIndex / gridSize;
        int col = texIndex % gridSize;
        final float oneOver = 1.0f / (float) gridSize;

        Model result = new Model(fileName, texName);
        Mesh mesh = new Mesh();
        BufferedReader br = new BufferedReader(new InputStreamReader(objInput));
        IList<Vector2f> uvs = new GapList<>();
        IList<Vector3f> normals = new GapList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] things = line.split(" ");
                if (things[0].equals("v")) {
                    Vector3f pos = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    Vertex vertex = new Vertex(pos);
                    mesh.vertices.addLast(vertex);
                } else if (things[0].equals("vt")) {
                    Vector2f uv = new Vector2f(Float.parseFloat(things[1]), 1.0f - Float.parseFloat(things[2]));
                    if (texIndex != -1) {
                        uv.x = (uv.x + row) * oneOver;
                        uv.y = (uv.y + col) * oneOver;
                    }
                    uvs.add(uv);
                } else if (things[0].equals("vn")) {
                    Vector3f normal = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    normals.add(normal);
                } else if (things[0].equals("f")) {
                    String[] subThings = {things[1], things[2], things[3]};
                    for (String subThing : subThings) {
                        String[] data = subThing.split("/");
                        int index = Integer.parseInt(data[0]) - 1;
                        mesh.indices.add(index);
                        Vertex vertex = mesh.vertices.get(index);
                        if (data.length >= 2 && !data[1].isEmpty()) {
                            vertex.setUv(uvs.get(Integer.parseInt(data[1]) - 1));
                        }
                        if (data.length >= 3 && !data[2].isEmpty()) {
                            mesh.vertices.get(index).setNormal(normals.get(Integer.parseInt(data[2]) - 1));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            try {
                objInput.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                    DSLogger.reportFatalError(ex.getMessage(), ex);
                }
            }
        }

        if (reverseFaces) {
            for (int index = 0; index < mesh.indices.size() - 3; index += 3) {
                mesh.indices.reverse(index, 3);
            }
        }

        Material material = new Material(Texture.getOrDefault(texName));
        material.setColor(new Vector4f(GlobalColors.WHITE_RGBA));
        result.materials.add(material);

        result.meshes.add(mesh);
        result.calcDimsPub();

        return result;
    }

    /**
     * Read the complex obj and constructs the model. Texture is assigned from
     * the atlas by tex name. Detects "g" lines to determine if the model is a
     * gun or player and adjusts texture coordinates accordingly. Extension is
     * OBJX.
     *
     * @param dirEntry directory entry for the model & texture
     * @param fileName obj filename
     * @param texNames texNames (atlas) matching player & gun for example
     * @param gridSize grid size of texture atlas (non-zero, positive number).
     * @param reverseFaces reverse face (for face culling)
     *
     * @return new Model
     */
    public static Model readFromObjFile(String dirEntry, String fileName, String[] texNames, int gridSize, boolean reverseFaces) {
        // For Mesh (Geometry) 
        final Pattern pattern = Pattern.compile("\\b[a-zA-Z0-9\\-_]+_Mesh\\b");

        File extern = new File(dirEntry + fileName);
        File archive = new File(Game.DATA_ZIP);
        ZipFile zipFile = null;
        InputStream objInput = null;
        if (extern.exists()) {
            try {
                objInput = new FileInputStream(extern);
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else if (archive.exists()) {
            try {
                zipFile = new ZipFile(archive);
                for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                    if (zipEntry.getName().equals(dirEntry + fileName)) {
                        objInput = zipFile.getInputStream(zipEntry);
                        break;
                    }
                }
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else {
            DSLogger.reportError("Cannot find zip archive " + Game.DATA_ZIP + " or relevant ingame files!", null);
        }
        //--------------------------------------------------------------------------
        if (objInput == null) {
            DSLogger.reportError("Cannot find resource " + dirEntry + fileName + "!", null);
            return null;
        }

        int globlIndex = -1;
        int texIndex = -1;
        final float oneOver = 1.0f / (float) gridSize;

        Model result = new Model(fileName, texNames[0]);
        Mesh mesh = new Mesh();
        BufferedReader br = new BufferedReader(new InputStreamReader(objInput));
        IList<Vector2f> uvs = new GapList<>();
        IList<Vector3f> normals = new GapList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] things = line.trim().split("\\s+");
                if (things.length == 0) {
                    continue; // Skip empty lines
                }
                if (things[0].equals("v")) {
                    if (things.length < 4) {
                        continue; // Ensure there are enough coordinates
                    }
                    Vector3f pos = new Vector3f(
                            Float.parseFloat(things[1]),
                            Float.parseFloat(things[2]),
                            Float.parseFloat(things[3])
                    );
                    Vertex vertex = new Vertex(pos);
                    mesh.vertices.addLast(vertex);
                } else if (things[0].equals("vt")) {
                    if (things.length < 3) {
                        continue; // Ensure there are enough UV coordinates
                    }
                    Vector2f uv = new Vector2f(
                            Float.parseFloat(things[1]),
                            1.0f - Float.parseFloat(things[2]) // Flip Y-axis if necessary
                    );

                    texIndex = Texture.getOrDefaultIndex(texNames[globlIndex]);
                    int row = texIndex / gridSize;
                    int col = texIndex % gridSize;

                    if (texIndex != -1) {
                        uv.x = (uv.x + row) * oneOver;
                        uv.y = (uv.y + col) * oneOver;
                    }
                    uvs.add(uv);
                } else if (things[0].equals("vn")) {
                    if (things.length < 4) {
                        continue; // Ensure there are enough normal coordinates
                    }
                    Vector3f normal = new Vector3f(
                            Float.parseFloat(things[1]),
                            Float.parseFloat(things[2]),
                            Float.parseFloat(things[3])
                    );
                    normals.add(normal);
                } else if (things[0].equals("g")) {
                    // Detect group name
                    if (things.length >= 2 && pattern.asPredicate().test(things[1])) {
                        globlIndex++;
                    }
                } else if (things[0].equals("f")) {
                    if (things.length < 4) {
                        continue; // Ensure it's a triangular face
                    }
                    String[] subThings = {things[1], things[2], things[3]};
                    for (String subThing : subThings) {
                        String[] data = subThing.split("/");
                        int vertexIndex = Integer.parseInt(data[0]) - 1;
                        mesh.indices.add(vertexIndex);
                        Vertex vertex = mesh.vertices.get(vertexIndex);
                        if (data.length >= 2 && !data[1].isEmpty()) {
                            vertex.setUv(uvs.get(Integer.parseInt(data[1]) - 1));
                        }
                        if (data.length >= 3 && !data[2].isEmpty()) {
                            Vector3f normal = normals.get(Integer.parseInt(data[2]) - 1);
                            mesh.vertices.get(vertexIndex).setNormal(normal);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            try {
                objInput.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                    DSLogger.reportFatalError(ex.getMessage(), ex);
                }
            }
        }

        if (reverseFaces) {
            for (int index = 0; index < mesh.indices.size() - 2; index += 3) {
                // Ensure we have at least three indices to form a triangle
                if (index + 2 < mesh.indices.size()) {
                    // Swap the second and third indices to reverse the face
                    int temp = mesh.indices.get(index + 1);
                    mesh.indices.set(index + 1, mesh.indices.get(index + 2));
                    mesh.indices.set(index + 2, temp);
                }
            }
        }

        Material material = new Material(Texture.getOrDefault(texNames[0]));
        material.setColor(new Vector4f(GlobalColors.WHITE_RGBA));
        result.materials.add(material);

        result.meshes.add(mesh);
        result.calcDimsPub();

        return result;
    }

    /**
     * Convert block specs {solid, texName, VEC3} to unique int (hashcode).
     *
     * @param solid is block solid
     * @param texName texName[5] string,
     * @param facebits
     * @param pos float3(x,y,z) vector
     *
     * @return unique int
     */
    public static int blockSpecsToUniqueInt(boolean solid, String texName, int facebits, Vector3f pos) {
        // Convert boolean solid to integer (0 for false, 1 for true)
        int a = solid ? 1 : 0;

        // Texture index
        int b = Texture.getOrDefaultIndex(texName);

        // Use Chunk function for the position
        int c = Chunk.chunkFunc(pos);

        // Scale and convert position components to integers
        int x = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int y = Math.round((pos.y + Chunk.BOUND) / 2.0f);
        int z = Math.round((pos.z + Chunk.BOUND) / 2.0f);

        // Combine position components into a single value
        int posHash = x * 73856093 ^ y * 19349663 ^ z * 83492791; // Use large prime numbers

        // Combine all components into a single unique integer
        int result = Short.MAX_VALUE | ((a * 31) ^ (b * 17) ^ (c * 13) ^ (facebits * 7) ^ posHash);

        return result;
    }
}

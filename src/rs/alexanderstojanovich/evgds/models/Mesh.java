/*
 * Copyright (C) 2023 coas91@rocketmail.com
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
package rs.alexanderstojanovich.evgds.models;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Mesh {

    public final IList<Vertex> vertices = new GapList<>();
    public final IList<Integer> indices = new GapList<>(); // refers which vertex we want to use when

    protected static FloatBuffer fb;
    protected static IntBuffer ib;

    protected int vbo = 0; // vertex buffer object
    protected int ibo = 0; // index buffer object  

    protected boolean buffered = false;

    public void calcNormals() {
        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            Vector3f v1 = vertices.get(i1).getPos().sub(vertices.get(i0).getPos());
            Vector3f v2 = vertices.get(i2).getPos().sub(vertices.get(i0).getPos());

            Vector3f normal = v1.cross(v2).normalize();
            vertices.get(i0).setNormal(vertices.get(i0).getNormal().add(normal));
            vertices.get(i1).setNormal(vertices.get(i1).getNormal().add(normal));
            vertices.get(i2).setNormal(vertices.get(i2).getNormal().add(normal));
        }

        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).setNormal(vertices.get(i).getNormal().normalize());
        }
    }

    public void nullifyNormals() {
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).getNormal().zero();
        }
        buffered = false;
    }

    public void negateNormals() {
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).getNormal().negate();
        }
        buffered = false;
    }

    /**
     * Swaps the UV coordinates of the vertices in each triangle.
     *
     * This method iterates through the indices list in steps of three,
     * corresponding to the vertices of each triangle. For each triangle, it
     * swaps the UV coordinates of the vertices such that: - Vertex A gets the
     * UV of Vertex C - Vertex B gets the UV of Vertex A - Vertex C gets the UV
     * of Vertex B
     *
     * This operation is useful for modifying the texture mapping of the mesh.
     * After performing the swap, the `buffered` flag is set to false,
     * indicating that the vertex buffer needs to be updated.
     */
    public void triangSwap() {
        // Get the size of the indices list
        int size = indices.size();

        // Loop through each triangle (step by 3)
        for (int i = 0; i < size; i += 3) {
            // Get the indices for the vertices of the current triangle
            int indexA = indices.get(i);
            int indexB = indices.get(i + 1);
            int indexC = indices.get(i + 2);

            // Get the vertices corresponding to these indices
            Vertex a = vertices.get(indexA);
            Vertex b = vertices.get(indexB);
            Vertex c = vertices.get(indexC);

            // Get the UV coordinates of these vertices
            Vector2f uvA = a.getUv();
            Vector2f uvB = b.getUv();
            Vector2f uvC = c.getUv();

            // Swap the UV coordinates
            c.setUv(uvB); // C gets UV of B
            b.setUv(uvA); // B gets UV of A
            a.setUv(uvC); // A gets UV of C
        }
        // Mark the vertex buffer as needing an update
        buffered = false;
    }

    public static void triangSwap(List<Vertex> vertices, List<Integer> indices) {
        for (int i = 0; i < indices.size(); i += 3) {
            Vertex a = vertices.get(indices.get(i));
            Vertex b = vertices.get(indices.get(i + 1));
            Vertex c = vertices.get(indices.get(i + 2));
            Vector2f temp = c.getUv();
            c.setUv(b.getUv());
            b.setUv(a.getUv());
            a.setUv(temp);
        }
    }

    public IList<Vertex> getVertices() {
        return vertices;
    }

    public IList<Integer> getIndices() {
        return indices;
    }

}

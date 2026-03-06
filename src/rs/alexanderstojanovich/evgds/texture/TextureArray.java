package rs.alexanderstojanovich.evgds.texture;

import rs.alexanderstojanovich.evgds.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * TextureArray class responsible for loading and buffering GL_TEXTURE_2D_ARRAY textures.
 * Texture data is stored in BufferedImage array and can be buffered to OpenGL when needed.
 * World textures are loaded into a GL_TEXTURE_2D_ARRAY for efficient sampling in the shader.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class TextureArray implements TextureIfc {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Expected size (width and height) for regular textures in pixels. */
    public static final int TEX_SIZE = 128;

    /** Expected size (width and height) for each tile in the texture array. */
    public static final int ARRAY_TILE_SIZE = 64;

    /** Empty/blank texture used as a placeholder (e.g. for water). */
    public static final TextureArray EMPTY = new TextureArray("", new String[0], Texture.Format.RGBA8, TEX_SIZE);

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** Texture format flag. */
    protected Texture.Format texFmt = Texture.Format.NONE;

    /** Original image data array — one entry per layer. */
    private final BufferedImage[] images;

    /** Texture names (aliases) — one per layer, derived from fileNames. */
    private final String[] texNames;

    /** Original file names — one per layer. */
    private final String[] fileNames;

    /** GL texture ID after buffering to GPU. */
    private int textureID = 0;

    /** Whether texture has been buffered to GPU. */
    private boolean buffered = false;

    /** Expected size of the texture (width and height in pixels). */
    private final int texSize;

    /** Sub-directory used to locate texture files. */
    private String texDir = "";

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates TextureArray from the given file names using default TEX_SIZE.
     *
     * @param subDir    directory or entry where files are located
     * @param fileNames array of image file names (empty array for EMPTY placeholder)
     * @param texFmt    colorRGBA/depth format flag
     */
    public TextureArray(String subDir, String[] fileNames, Texture.Format texFmt) {
        this(subDir, fileNames, texFmt, TEX_SIZE);
    }

    /**
     * Creates TextureArray from the given file names with explicit texSize.
     *
     * @param subDir    directory or entry where files are located
     * @param fileNames array of image file names (empty array for EMPTY placeholder)
     * @param texFmt    colorRGBA/depth format flag
     * @param texSize   expected size of each texture tile (width and height in pixels)
     */
    public TextureArray(String subDir, String[] fileNames, Texture.Format texFmt, int texSize) {
        this.texFmt = texFmt;
        this.texDir = subDir;
        this.texSize = texSize;
        this.fileNames = fileNames;

        if (fileNames == null || fileNames.length == 0) {
            this.images   = new BufferedImage[0];
            this.texNames = new String[0];
        } else {
            this.images   = new BufferedImage[fileNames.length];
            this.texNames = new String[fileNames.length];
            for (int i = 0; i < fileNames.length; i++) {
                final String fn = fileNames[i];
                this.images[i]   = (fn == null || fn.isEmpty())
                        ? null
                        : ImageUtils.loadImage(subDir, fn);
                this.texNames[i] = (fn == null || fn.isEmpty() || !fn.contains("."))
                        ? "EMPTY"
                        : fn.substring(0, fn.lastIndexOf('.'));
                Texture.TEX_STORE.put(this.texNames[i], new TexValue(this, -1, 1));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Buffering — instance
    // -----------------------------------------------------------------------

    /**
     * Buffers texture data to OpenGL as a GL_TEXTURE_2D_ARRAY.
     * Uses the pre-loaded {@link #images} array for layer data.
     */
    @Override
    public void bufferAll() {
        buffered = true;
    }

    // -----------------------------------------------------------------------
    // Buffering — static helpers
    // -----------------------------------------------------------------------

    /**
     * Re-buffers a TextureArray using a pre-built ByteBuffer per layer.
     * The buffer must contain all layers packed sequentially
     * (each layer: {@code ARRAY_TILE_SIZE * ARRAY_TILE_SIZE * 4} bytes).
     *
     * @param textureArray target TextureArray
     * @param imgDatBuff   packed image data (nullable — falls back to stored images)
     */
    public static void bufferAll(TextureArray textureArray, ByteBuffer imgDatBuff) {
        textureArray.buffered = true;
    }

    /** Buffers the EMPTY texture (used for water etc.). */
    public static void bufferAllTextures() {
        EMPTY.bufferAll();
    }

    // -----------------------------------------------------------------------
    // Internal OpenGL loader — from stored images
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Internal OpenGL loader — from pre-built ByteBuffer
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Shared texture parameter setup
    // -----------------------------------------------------------------------

    private void applyArrayTextureParams() {

    }

    // -----------------------------------------------------------------------
    // TextureIfc — bind overloads
    // -----------------------------------------------------------------------


    // -----------------------------------------------------------------------
    // Unbind (static)
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // TextureIfc — release
    // -----------------------------------------------------------------------


    // -----------------------------------------------------------------------
    // TextureIfc — getters
    // -----------------------------------------------------------------------

    @Override
    public int getTextureID() { return textureID; }

    @Override
    public boolean isBuffered() { return buffered; }

    /**
     * Returns the first texture name, or "EMPTY" if none.
     * Use {@link #getTexNames()} for the full array.
     */
    @Override
    public String getTexName() {
        return (texNames != null && texNames.length > 0) ? texNames[0] : "EMPTY";
    }

    /** @return all texture names (one per layer) */
    public String[] getTexNames() { return texNames; }

    /** @return all file names (one per layer) */
    public String[] getFileNames() { return fileNames; }

    /** @return all loaded images (one per layer) */
    public BufferedImage[] getImages() { return images; }

    @Override
    public Texture.Format getTexFmt() { return texFmt; }
}

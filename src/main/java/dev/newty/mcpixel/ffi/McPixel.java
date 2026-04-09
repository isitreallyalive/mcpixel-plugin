package dev.newty.mcpixel.ffi;

public class McPixel {
    public static native long newArt(byte[] image, Configuration config);
    public static native void freeArt(long ptr);
    public static native int[] artDimensions(long ptr);
    public static native Texture[] artBlocks(long ptr);
}

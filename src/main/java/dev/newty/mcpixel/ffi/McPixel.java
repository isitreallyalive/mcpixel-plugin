package dev.newty.mcpixel.ffi;

public class McPixel {
    static {
        NativeLoader.load();
    }

    public static native long newArt(byte[] image);
    public static native void freeArt(long ptr);
    public static native int[] artDimensions(long ptr);
    public static native BlockPair[] artBlocks(long ptr);
}

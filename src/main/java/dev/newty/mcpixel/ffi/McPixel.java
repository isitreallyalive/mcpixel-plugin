package dev.newty.mcpixel.ffi;

public class McPixel {
    static {
        NativeLoader.load();
    }

    public static native long newPixelArt(byte[] image);
    public static native void freePixelArt(long ptr);
}

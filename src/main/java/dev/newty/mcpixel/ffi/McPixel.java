package dev.newty.mcpixel.ffi;

public class McPixel {
    static {
        NativeLoader.load();
    }

    public static native void helloWorld();
}

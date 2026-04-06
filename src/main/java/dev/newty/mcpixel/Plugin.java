package dev.newty.mcpixel;

import dev.newty.mcpixel.ffi.NativeLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Plugin extends JavaPlugin {
    @Override
    public void onEnable() {
        NativeLoader.load();
        Objects.requireNonNull(this.getCommand("pixel")).setExecutor(new PixelCommand());
//
//        try {
//            URL url = URI.create("https://newty.dev/_astro/bert.CW14V6sB.webp").toURL();
//            byte[] data = url.openStream().readAllBytes();
//            long art = McPixel.newArt(data);
//            BlockPair[] blocks = McPixel.artBlocks(art);
//            System.out.println(Arrays.deepToString(blocks));
//            int[] dimensions = McPixel.artDimensions(art);
//            System.out.println(Arrays.toString(dimensions));
//            McPixel.freeArt(art);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

//    @Override
//    public void onDisable() {
//    }
}

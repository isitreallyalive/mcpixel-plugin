package dev.newty.mcpixel;

import dev.newty.mcpixel.ffi.BlockPair;
import dev.newty.mcpixel.ffi.McPixel;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

public final class Plugin extends JavaPlugin {
    @Override
    public void onEnable() {
        try {
            URL url = URI.create("https://newty.dev/_astro/bert.CW14V6sB.webp").toURL();
            byte[] data = url.openStream().readAllBytes();
            long art = McPixel.newArt(data);
            BlockPair[] blocks = McPixel.artBlocks(art);
            System.out.println(Arrays.deepToString(blocks));
            int[] dimensions = McPixel.artDimensions(art);
            System.out.println(Arrays.toString(dimensions));
            McPixel.freeArt(art);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    @Override
//    public void onDisable() {
//    }
}

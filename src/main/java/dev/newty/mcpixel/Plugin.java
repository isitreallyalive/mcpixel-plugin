package dev.newty.mcpixel;

import dev.newty.mcpixel.ffi.McPixel;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

public final class Plugin extends JavaPlugin {
    @Override
    public void onEnable() {
        try {
            URL url = URI.create("https://newty.dev/_astro/bert.CW14V6sB.webp").toURL();
            byte[] data = url.openStream().readAllBytes();
            long art = McPixel.newPixelArt(data);
            System.out.println(art);
            McPixel.freePixelArt(art);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    @Override
//    public void onDisable() {
//    }
}

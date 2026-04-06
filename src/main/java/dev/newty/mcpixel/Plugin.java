package dev.newty.mcpixel;

import dev.newty.mcpixel.ffi.McPixel;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {
    @Override
    public void onEnable() {
        McPixel.helloWorld();
    }

//    @Override
//    public void onDisable() {
//    }
}

package dev.newty.mcpixel;

import dev.newty.mcpixel.ffi.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class McPixel extends JavaPlugin {
    @Override
    public void onEnable() {
        NativeLoader.load();
        mcpixel_h.hello_world();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

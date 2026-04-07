package dev.newty.mcpixel;

import co.aikar.commands.BukkitCommandManager;
import dev.newty.mcpixel.ffi.NativeLoader;
import org.bukkit.plugin.java.JavaPlugin;


public final class Plugin extends JavaPlugin {
    @Override
    public void onEnable() {
        NativeLoader.load();

        BukkitCommandManager manager = new BukkitCommandManager(this);
        manager.registerCommand(new PixelCommand());
    }
}

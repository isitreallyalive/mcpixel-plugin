package dev.newty.mcpixel;

import dev.newty.mcpixel.ffi.NativeLoader;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public final class Plugin extends JavaPlugin {
    private static Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this;

        // setup
        NativeLoader.load();
        LegacyPaperCommandManager<CommandSender> manager = new LegacyPaperCommandManager<>(this, ExecutionCoordinator.simpleCoordinator(), SenderMapper.identity());

        // enable brigadier if possible
        if (manager.hasBrigadierManager()) {
            manager.registerBrigadier();
        }

        // register commands
        manager.command(PixelCommand.build(manager));
    }

    public static Plugin getPlugin() {
        return plugin;
    }
}

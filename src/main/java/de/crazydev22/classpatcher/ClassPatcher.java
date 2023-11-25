package de.crazydev22.classpatcher;

import de.crazydev22.classpatcher.api.ClassInstrumentation;
import de.crazydev22.classpatcher.api.PatchTransformer;
import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ClassPatcher extends JavaPlugin {
    private final PatchTransformer transformer;

    public ClassPatcher() {
        transformer = ClassInstrumentation.getPatchTransformer();
        loadPatches();
        getLogger().info("Loaded Patches!");
    }

    public void loadPatches() {
        File data = getDataFolder();
        data.mkdirs();
        transformer.loadPatches(data)
                .forEach(transformer::put);
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!sender.hasPermission("classpatcher.reload"))
            return false;
        loadPatches();
        sender.sendMessage("Reloaded Pathes!");
        return true;
    }
}

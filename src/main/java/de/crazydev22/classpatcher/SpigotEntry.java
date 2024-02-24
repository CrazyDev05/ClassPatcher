package de.crazydev22.classpatcher;

import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class SpigotEntry extends JavaPlugin {
	private final ClassPatcher patcher;

	public SpigotEntry() {
		this.patcher = new ClassPatcher(getDataFolder());
	}

	@Override
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
		if (!sender.hasPermission("classpatcher.reload"))
			return false;
		patcher.loadPatches();
		sender.sendMessage("Reloaded Pathes!");
		return true;
	}
}

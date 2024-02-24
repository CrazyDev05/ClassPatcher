package de.crazydev22.classpatcher;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

@SuppressWarnings("unused")
public class BungeeEntry extends Plugin {
	private final ClassPatcher patcher;

	public BungeeEntry() {
		this.patcher = new ClassPatcher(getDataFolder());
	}

	@Override
	public void onEnable() {
		getProxy().getPluginManager().registerCommand(this, new BungeeCommand());
	}

	private class BungeeCommand extends Command {

		public BungeeCommand() {
			super("cprl", "classpatcher.reload");
		}

		@Override
		public void execute(CommandSender sender, String[] args) {
			if (!sender.hasPermission("classpatcher.reload"))
				return;
			patcher.loadPatches();
			sender.sendMessage(TextComponent.fromLegacyText("Reloaded Pathes!"));
		}
	}
}

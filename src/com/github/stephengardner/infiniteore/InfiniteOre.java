package com.github.stephengardner.infiniteore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * InfiniteOre - a Bukkit plugin that allows players to place and control mob
 * spawners.
 * 
 * @author Stephen Gardner <StephenBGardner@gmail.com>
 * 
 *         InfiniteOre is free software: you can redistribute it and/or modify
 *         it under the terms of the GNU General Public License as published by
 *         the Free Software Foundation, either version 3 of the License, or (at
 *         your option) any later version.
 * 
 *         InfiniteOre is distributed in the hope that it will be useful, but
 *         WITHOUT ANY WARRANTY; without even the implied warranty of
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *         General Public License for more details.
 * 
 *         You should have received a copy of the GNU General Public License
 *         along with InfiniteOre. If not, see <http://www.gnu.org/licenses/>.
 */

public class InfiniteOre extends JavaPlugin implements Listener, CommandExecutor {

	private HashMap<String, Ore> oreMap;
	private HashMap<String, Integer> tasks;
	private Boolean info;

	File oreFile;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		getCommand("io").setExecutor(this);
		getServer().getPluginManager().registerEvents(this, this);

		oreMap = new HashMap<String, Ore>();
		tasks = new HashMap<String, Integer>();
		oreFile = new File(getDataFolder(), "ore.db");

		if (!oreFile.exists()) {
			try {
				oreFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}

			saveOre();
		}

		loadOre();
		info = getConfig().getBoolean("InfiniteOre.Info", true);
	}

	@Override
	public void onDisable() {
		for (String key : tasks.keySet()) {
			getServer().getScheduler().cancelTask(tasks.get(key));
			recreateOre(oreMap.get(key));
		}

		tasks.clear();
		oreMap.clear();
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		String key = getKey(e.getBlock());

		if (!oreMap.containsKey(key)) {
			return;
		}

		final Ore ore = oreMap.get(key);

		tasks.put(key, getServer().getScheduler().runTaskLater(this, new Runnable() {

			@Override
			public void run() {
				recreateOre(ore);
			}

		}, ore.getRespawnInterval() * 20L).getTaskId());
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK || !info) {
			return;
		}

		if (!oreMap.containsKey(getKey(e.getClickedBlock()))) {
			return;
		}

		Ore ore = oreMap.get(getKey(e.getClickedBlock()));
		e.getPlayer().sendMessage(String.format("This %s is set to respawn every %d seconds.", getTypeName(ore.getBlockType()), ore.getRespawnInterval()));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("io")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command is not available from Console.");
				return true;
			}

			Player p = getServer().getPlayer(sender.getName());
			Block block = p.getTargetBlock(null, 5);
			String key = getKey(block);

			if (block == null || block.getType() == Material.AIR) {
				sender.sendMessage("No block targeted.");
				return true;
			}

			if (args.length < 1) {
				if (!oreMap.containsKey(key)) {
					sender.sendMessage("This block is not set to respawn.");
					return true;
				}

				if (tasks.containsKey(key)) {
					getServer().getScheduler().cancelTask(tasks.get(key));
				}

				oreMap.remove(key);
				sender.sendMessage("This block will no longer respawn.");
				saveOre();
				return true;
			}

			int seconds;

			try {
				seconds = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				sender.sendMessage("Invalid respawn time. Respawn time must be in seconds.");
				return true;
			}

			Ore ore;

			if (oreMap.containsKey(key)) {
				ore = oreMap.get(key);
			} else {
				ore = new Ore(block);
				oreMap.put(key, ore);
			}

			ore.setRespawnInterval(seconds);
			ore.setBlockType(block.getType());
			sender.sendMessage(String.format("Ore set to respawn in %d seconds.", seconds));
			saveOre();
		}

		return true;
	}

	public String getKey(Block block) {
		String world = block.getWorld().getUID().toString();
		int locX = block.getLocation().getBlockX();
		int locY = block.getLocation().getBlockY();
		int locZ = block.getLocation().getBlockZ();

		return (world + locX + locY + locZ);
	}

	public void recreateOre(Ore ore) {
		Block block = getServer().getWorld(ore.getWorld()).getBlockAt(ore.getLocX(), ore.getLocY(), ore.getLocZ());
		block.setType(ore.getBlockType());

		int task = tasks.get(getKey(block));
		tasks.remove(task);
	}

	public String getTypeName(Material type) {
		String name = type.toString().replace("_", " ").toLowerCase();
		StringBuilder sb = new StringBuilder(name);
		int i = 0;

		do {
			sb.replace(i, i + 1, sb.substring(i, i + 1).toUpperCase());
			i = sb.indexOf(" ", i) + 1;
		} while (i > 0 && i < sb.length());

		return sb.toString();
	}

	public void saveOre() {
		try {
			FileOutputStream fos = new FileOutputStream(oreFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(oreMap);
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadOre() {
		try {
			FileInputStream fis = new FileInputStream(oreFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<?, ?> uncasted = (HashMap<?, ?>) ois.readObject();
			ois.close();

			for (Object key : uncasted.keySet()) {
				if (key instanceof String) {
					Object value = uncasted.get(key);

					if (value instanceof Ore) {
						oreMap.put((String) key, (Ore) value);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}

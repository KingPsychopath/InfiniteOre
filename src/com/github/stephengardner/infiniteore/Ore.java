package com.github.stephengardner.infiniteore;

import java.io.Serializable;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class Ore implements Serializable {

	private static final long serialVersionUID = -1555828101552635635L;

	private Material blockType;
	private int respawnInterval, timer;
	private UUID world;
	private int locX;
	private int locY;
	private int locZ;

	public Ore(Block block) {
		this.blockType = block.getType();
		this.world = block.getWorld().getUID();
		this.locX = block.getLocation().getBlockX();
		this.locY = block.getLocation().getBlockY();
		this.locZ = block.getLocation().getBlockZ();
	}

	public Material getBlockType() {
		return blockType;
	}

	public void setBlockType(Material blockType) {
		this.blockType = blockType;
	}

	public UUID getWorld() {
		return world;
	}

	public int getLocX() {
		return locX;
	}

	public int getLocY() {
		return locY;
	}

	public int getLocZ() {
		return locZ;
	}

	public int getRespawnInterval() {
		return respawnInterval;
	}

	public void setRespawnInterval(int respawnInterval) {
		this.respawnInterval = respawnInterval;
	}

	public int getTimer() {
		return timer;
	}

	public void setTimer(int timer) {
		this.timer = timer;
	}

}

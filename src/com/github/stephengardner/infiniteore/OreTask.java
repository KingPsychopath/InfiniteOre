package com.github.stephengardner.infiniteore;

public class OreTask extends Thread {

	private InfiniteOre io;

	public OreTask(InfiniteOre io) {
		this.io = io;
	}

	@Override
	public void run() {
		while (true) {
			for (Ore ore : io.getOreMap().values()) {
				if (ore.getTimer() == 0) {
					io.recreateOre(ore);
					ore.setTimer(-1);
				} else if (ore.getTimer() > -1) {
					ore.setTimer(ore.getTimer() - 1);
				}
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}

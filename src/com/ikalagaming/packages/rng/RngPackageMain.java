package com.ikalagaming.packages.rng;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import com.ikalagaming.event.Listener;
import com.ikalagaming.packages.Package;
import com.ikalagaming.packages.PackageManager;

/**
 * The main interface for the rng package. Seeds the {@link Generator} using
 * {@link SecureRandom} so it should be fairly secure/hard to predict.
 *
 * @author Ches Burks
 *
 */
public class RngPackageMain implements Package, Listener {

	private Generator gen;
	private final double version = 0.1;
	private final String packageName = "rng";
	private boolean enabled = false;

	/**
	 * Returns a random {@link Boolean boolean} if the package is enabled.
	 * Returns false if it is not enabled.
	 *
	 * @return a boolean
	 */
	public boolean getBoolean() {
		if (!PackageManager.getInstance().isEnabled(this)) {
			return false;
		}
		return this.gen.getBoolean();
	}

	/**
	 * If the package is not enabled, returns false. If it is enabled, returns a
	 * {@link Boolean boolean} with a given probability of being true. The
	 * probability is a float from 0.0f to 1.0f, with 0 being no chance of
	 * returning true and 1 being a 100% chance of returning true.
	 *
	 * @param probablilty The chance of returning true
	 * @return a boolean
	 */
	public boolean getBoolean(float probablilty) {
		if (!this.enabled) {
			return false;
		}
		return this.gen.getBoolean(probablilty);
	}

	/**
	 * If the package is not enabled, returns 0.
	 *
	 * Returns a random {@link Float float}.
	 *
	 * @return a random float
	 */
	public float getFloat() {
		if (!this.enabled) {
			return 0;
		}
		return this.gen.getFloat();
	}

	/**
	 *
	 * Returns a random integer if the package is enabled. If it is not enabled,
	 * returns 0.
	 *
	 * @return a random int
	 */
	public int getInt() {
		if (!this.enabled) {
			return 0;
		}
		return this.gen.getInt();
	}

	/**
	 * If the package is not enabled, returns 0. Returns a random
	 * {@link Integer int} between the given values, inclusive. <br>
	 * For example: a call {@code getIntBetween(2,6)} will return either
	 * {@code 2, 3, 4, 5 or 6}.
	 *
	 * @param min The minimum number
	 * @param max The maximum number
	 * @return a random integer
	 */
	public int getIntBetween(int min, int max) {
		if (!this.enabled) {
			return 0;
		}
		return this.gen.getIntBetween(min, max);
	}

	@Override
	public Set<Listener> getListeners() {
		return new HashSet<>();
	}

	@Override
	public String getName() {
		return this.packageName;
	}

	@Override
	public double getVersion() {
		return this.version;
	}

	@Override
	public boolean onDisable() {
		this.gen = null;
		this.enabled = false;
		return true;
	}

	@Override
	public boolean onEnable() {
		SecureRandom sec = new SecureRandom();
		byte[] sbuf = sec.generateSeed(8);
		ByteBuffer bb = ByteBuffer.wrap(sbuf);
		long seed = bb.getLong();
		this.gen = new Generator((int) seed);
		this.enabled = true;
		return true;
	}

	@Override
	public boolean onLoad() {
		return true;
	}

	@Override
	public boolean onUnload() {
		return true;
	}

}

package com.dreamynomad.colorcam;

import android.support.v7.graphics.Palette;
import android.util.LruCache;

import java.util.List;

/**
 * Created by Eric on 11/28/2014.
 */
public class PaletteLruCache extends LruCache<String, List<Palette.Swatch>> {

	private static final int DEFAULT_CACHE_SIZE = 200;

	private static PaletteLruCache sInstance;

	/**
	 * @param maxSize for caches that do not override {@link #sizeOf}, this is
	 *                the maximum number of entries in the cache. For all other caches,
	 *                this is the maximum sum of the sizes of the entries in this cache.
	 */
	private PaletteLruCache(int maxSize) {
		super(maxSize);
	}

	public static PaletteLruCache getInstance() {
		if (sInstance == null) {
			sInstance = new PaletteLruCache(DEFAULT_CACHE_SIZE);
		}

		return sInstance;
	}
}

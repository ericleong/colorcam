package com.dreamynomad.colorcam;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.LruCache;

/**
 * Simple least-recently-used {@link android.graphics.Bitmap} cache, sized in bytes.
 * <p/>
 * Created by Eric on 11/27/2014.
 */
public class BitmapLruCache extends LruCache<String, Bitmap> {

	private static final int DEFAULT_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 4);

	private static BitmapLruCache sInstance;

	/**
	 * @param maxSize for caches that do not override {@link #sizeOf}, this is
	 *                the maximum number of entries in the cache. For all other caches,
	 *                this is the maximum sum of the sizes of the entries in this cache.
	 */
	private BitmapLruCache(int maxSize) {
		super(maxSize);
	}

	public static BitmapLruCache getInstance() {
		if (sInstance == null) {
			sInstance = new BitmapLruCache(DEFAULT_CACHE_SIZE);
		}

		return sInstance;
	}

	@Override
	protected int sizeOf(String key, Bitmap value) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			return value.getAllocationByteCount();
		} else {
			return value.getByteCount();
		}
	}
}

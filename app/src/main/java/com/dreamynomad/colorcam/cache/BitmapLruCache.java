package com.dreamynomad.colorcam.cache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.LruCache;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Simple least-recently-used {@link android.graphics.Bitmap} cache, sized in bytes.
 * <p/>
 * Created by Eric on 11/27/2014.
 */
public class BitmapLruCache extends LruCache<String, Bitmap> {

	private static final int DEFAULT_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 4);

	private static BitmapLruCache sInstance;

	private Set<SoftReference<Bitmap>> mReusableBitmaps;

	/**
	 * @param maxSize for caches that do not override {@link #sizeOf}, this is
	 *                the maximum number of entries in the cache. For all other caches,
	 *                this is the maximum sum of the sizes of the entries in this cache.
	 */
	private BitmapLruCache(int maxSize) {
		super(maxSize);
		mReusableBitmaps = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
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

	@Override
	protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
		// We're running on Honeycomb or later, so add the bitmap
		// to a SoftReference set for possible use with inBitmap later.
		mReusableBitmaps.add(new SoftReference<>(oldValue));
	}

	// This method iterates through the reusable bitmaps, looking for one
	// to use for inBitmap:
	public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
		Bitmap bitmap = null;

		if (mReusableBitmaps != null && !mReusableBitmaps.isEmpty()) {
			synchronized (mReusableBitmaps) {
				final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
				Bitmap item;

				while (iterator.hasNext()) {
					item = iterator.next().get();

					if (null != item && item.isMutable()) {
						// Check to see it the item can be used for inBitmap.
						if (canUseForInBitmap(item, options)) {
							bitmap = item;

							// Remove from reusable set so it can't be used again.
							iterator.remove();
							break;
						}
					} else {
						// Remove from the set if the reference has been cleared.
						iterator.remove();
					}
				}
			}
		}
		return bitmap;
	}

	private static boolean canUseForInBitmap(
			Bitmap candidate, BitmapFactory.Options targetOptions) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			// From Android 4.4 (KitKat) onward we can re-use if the byte size of
			// the new bitmap is smaller than the reusable bitmap candidate
			// allocation byte count.
			int width = targetOptions.outWidth / targetOptions.inSampleSize;
			int height = targetOptions.outHeight / targetOptions.inSampleSize;
			int byteCount = width * height * getBytesPerPixel(candidate.getConfig());

			return byteCount <= candidate.getAllocationByteCount();
		}

		// On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
		return candidate.getWidth() == targetOptions.outWidth
				&& candidate.getHeight() == targetOptions.outHeight
				&& targetOptions.inSampleSize == 1;
	}

	/**
	 * A helper function to return the byte usage per pixel of a bitmap based on its configuration.
	 */
	private static int getBytesPerPixel(Bitmap.Config config) {
		if (config == Bitmap.Config.ARGB_8888) {
			return 4;
		} else if (config == Bitmap.Config.RGB_565) {
			return 2;
		} else if (config == Bitmap.Config.ARGB_4444) {
			return 2;
		} else if (config == Bitmap.Config.ALPHA_8) {
			return 1;
		}
		return 1;
	}
}

package com.dreamynomad.colorcam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.support.v7.graphics.Palette;
import android.util.Log;

import com.dreamynomad.colorcam.cache.BitmapLruCache;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Miscellaneous utilities.
 * <p/>
 * Created by Eric on 11/23/2014.
 */
public class GalleryUtils {

	private static final int DEFAULT_MAX_BITMAP_SIZE = 2048;

	private static final String TAG = GalleryUtils.class.getSimpleName();

	/**
	 * Maximum OpenGL texture size.
	 */
	private static final int sMaxBitmapSize;

	static {
		// Get maximum bitmap size from OpenGL
		// http://stackoverflow.com/questions/15313807/android-maximum-allowed-width-height-of-bitmap/26823209#26823209

		// Get EGL Display
		EGL10 egl = (EGL10) EGLContext.getEGL();
		EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

		// Initialise
		int[] version = new int[2];
		egl.eglInitialize(display, version);

		// Query total number of configurations
		int[] totalConfigurations = new int[1];
		egl.eglGetConfigs(display, null, 0, totalConfigurations);

		// Query actual list configurations
		EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
		egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

		int[] textureSize = new int[1];
		int maximumTextureSize = 0;

		// Iterate through all the configurations to located the maximum texture size
		for (int i = 0; i < totalConfigurations[0]; i++) {
			// Only need to check for width since opengl textures are always squared
			egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

			// Keep track of the maximum texture size
			if (maximumTextureSize < textureSize[0]) {
				maximumTextureSize = textureSize[0];
			}
		}

		// Release
		egl.eglTerminate(display);

		// Return largest texture size found, or default
		sMaxBitmapSize = Math.max(maximumTextureSize, DEFAULT_MAX_BITMAP_SIZE);
	}

	// http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	public static Bitmap decodeSampledBitmapFromResource(
			String pathName, int reqWidth, int reqHeight) {
		return decodeSampledBitmapFromResource(pathName, reqWidth, reqHeight, false);
	}

	// http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	public static Bitmap decodeSampledBitmapFromResource(
			String pathName, int reqWidth, int reqHeight, boolean inMutable) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(pathName, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		options.inMutable = inMutable;
		return BitmapFactory.decodeFile(pathName, options);
	}

	// http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	public static Bitmap decodeSampledBitmapFromResource(
			InputStream inputStream, int reqWidth, int reqHeight) {
		return decodeSampledBitmapFromResource(inputStream, reqWidth, reqHeight, false, null);
	}

	// http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	public static Bitmap decodeSampledBitmapFromResource(
			InputStream inputStream, int reqWidth, int reqHeight, boolean inMutable,
			BitmapLruCache cache) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(inputStream, null, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Use preexisting bitmap if possible.
		if (cache != null) {
			addInBitmapOptions(options, cache);
		}

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		options.inMutable = inMutable;
		return BitmapFactory.decodeStream(inputStream, null, options);
	}

	// http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	public static Bitmap decodeSampledBitmapFromResource(
			FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {
		return decodeSampledBitmapFromResource(fileDescriptor, reqWidth, reqHeight, false);
	}

	// http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	public static Bitmap decodeSampledBitmapFromResource(
			FileDescriptor fileDescriptor, int reqWidth, int reqHeight, boolean inMutable) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		options.inMutable = inMutable;
		return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
	}

	/**
	 * @param options   contains the raw height and width of the image
	 * @param reqWidth  the desired width
	 * @param reqHeight the desired height
	 * @return the value of {@link android.graphics.BitmapFactory.Options#inSampleSize}
	 * that ensures the resulting bitmap is larger than the desired width and height
	 */
	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			// Make sure bitmap can be rendered by ImageView by checking dimensions
			while (((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth)
					|| (height / inSampleSize) > sMaxBitmapSize
					|| (width / inSampleSize) > sMaxBitmapSize) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	/**
	 * @return a comparator that sorts by HSL
	 */
	public static Comparator<Palette.Swatch> getSwatchComparator() {
		return new Comparator<Palette.Swatch>() {
			@Override
			public int compare(Palette.Swatch hsl1, Palette.Swatch hsl2) {
				int hue = (int) Math.signum(hsl1.getHsl()[0] - hsl2.getHsl()[0]);

				if (hue != 0) {
					return hue;
				} else {
					int saturation = (int) Math.signum(hsl1.getHsl()[1] - hsl2.getHsl()[1]);

					if (saturation != 0) {
						return saturation;
					} else {
						// try luminance
						return (int) Math.signum(hsl1.getHsl()[2] - hsl2.getHsl()[2]);
					}
				}
			}
		};
	}

	/* Maximum pixels size for created bitmap. */
	private static final int MAX_NUM_PIXELS_THUMBNAIL = 512 * 384;
	private static final int UNCONSTRAINED = -1;

	/**
	 * Constant used to indicate the dimension of mini thumbnail.
	 */
	public static final int TARGET_SIZE_MINI_THUMBNAIL = 320;

	/**
	 * Attempts to add an unused bitmap from the cache to reuse.
	 *
	 * @param options the bitmap options to modify
	 * @param cache   the cache to reuse bitmaps from
	 */
	public static void addInBitmapOptions(BitmapFactory.Options options,
	                                      BitmapLruCache cache) {
		// inBitmap only works with mutable bitmaps, so force the decoder to
		// return mutable bitmaps.
		options.inMutable = true;

		if (cache != null) {
			// Try to find a bitmap to use for inBitmap.
			Bitmap inBitmap = cache.getBitmapFromReusableSet(options);

			if (inBitmap != null) {
				// If a suitable bitmap has been found, set it as the value of
				// inBitmap.
				options.inBitmap = inBitmap;
			}
		}
	}

	/*
	 * Compute the sample size as a function of minSideLength
     * and maxNumOfPixels.
     * minSideLength is used to specify that minimal width or height of a
     * bitmap.
     * maxNumOfPixels is used to specify the maximal size in pixels that is
     * tolerable in terms of memory usage.
     *
     * The function returns a sample size based on the constraints.
     * Both size and minSideLength can be passed in as IImage.UNCONSTRAINED,
     * which indicates no care of the corresponding constraint.
     * The functions prefers returning a sample size that
     * generates a smaller bitmap, unless minSideLength = IImage.UNCONSTRAINED.
     *
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way.
     * For example, BitmapFactory downsamples an image by 2 even though the
     * request is 3. So we round up the sample size to avoid OOM.
     */
	private static int computeSampleSize(BitmapFactory.Options options,
	                                     int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options,
	                                            int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 :
				(int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == UNCONSTRAINED) ? 128 :
				(int) Math.min(Math.floor(w / minSideLength),
						Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if ((maxNumOfPixels == UNCONSTRAINED) &&
				(minSideLength == UNCONSTRAINED)) {
			return 1;
		} else if (minSideLength == UNCONSTRAINED) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}

	/**
	 * @param filePath the path to the raw image file
	 * @return the potential thumbnail size from {@link MediaStore.Images.Thumbnails#getThumbnail(android.content.ContentResolver, long, int, android.graphics.BitmapFactory.Options)} getThumbnail}
	 */
	public static BitmapFactory.Options getThumbnailSize(String filePath) {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(filePath);
			FileDescriptor fd = stream.getFD();
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 1;
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFileDescriptor(fd, null, options);
			if (options.mCancel || options.outWidth == -1
					|| options.outHeight == -1) {
				return null;
			}
			options.inSampleSize = computeSampleSize(
					options, TARGET_SIZE_MINI_THUMBNAIL, MAX_NUM_PIXELS_THUMBNAIL);
			options.inJustDecodeBounds = false;

			return options;
		} catch (IOException ex) {
			Log.e(TAG, "", ex);
		} catch (OutOfMemoryError oom) {
			Log.e(TAG, "Unable to decode file " + filePath + ". OutOfMemoryError.", oom);
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException ex) {
				Log.e(TAG, "", ex);
			}
		}

		return null;
	}
}

package com.dreamynomad.colorcam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.graphics.Palette;

import java.io.FileDescriptor;
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
		return decodeSampledBitmapFromResource(inputStream, reqWidth, reqHeight, false);
	}

	// http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	public static Bitmap decodeSampledBitmapFromResource(
			InputStream inputStream, int reqWidth, int reqHeight, boolean inMutable) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(inputStream, null, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

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
}

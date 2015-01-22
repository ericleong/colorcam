package com.dreamynomad.colorcam;

import android.animation.ObjectAnimator;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Displays images with their palettes.
 * <p/>
 * Created by Eric on 11/23/2014.
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

	private static final String TAG = GalleryAdapter.class.getSimpleName();

	private Cursor mCursor;

	private OnItemClickListener mOnItemClickListener;

	/**
	 * Called when the user clicks on an item.
	 */
	public static interface OnItemClickListener {
		public void onItemClicked(ViewHolder viewHolder);
	}

	private static Comparator<Palette.Swatch> mSwatchComparator = GalleryUtils.getSwatchComparator();

	/**
	 * Manages a item, which contains an image and its palette.
	 */
	public static class ViewHolder extends RecyclerView.ViewHolder {

		public String mPath;
		public long mId;
		public int[] mColors;

		public int mNumColors;

		public ImageView mImageView;
		public View[] mColorViews;

		public ImageTask mImageTask;
		public AsyncTask mPaletteTask;

		public ViewHolder(final ViewGroup viewGroup) {
			super(viewGroup);

			mImageView = (ImageView) viewGroup.findViewById(R.id.image);

			mColorViews = new View[6];
			mColorViews[0] = viewGroup.findViewById(R.id.color_0);
			mColorViews[1] = viewGroup.findViewById(R.id.color_1);
			mColorViews[2] = viewGroup.findViewById(R.id.color_2);
			mColorViews[3] = viewGroup.findViewById(R.id.color_3);
			mColorViews[4] = viewGroup.findViewById(R.id.color_4);
			mColorViews[5] = viewGroup.findViewById(R.id.color_5);

			mColors = new int[6];
		}
	}

	/**
	 * Set thumbnail bitmap for an image.
	 */
	private static class ImageTask extends AsyncTask<String, Void, Bitmap> {

		private int position;
		private ViewHolder viewHolder;
		private String pathName;
		private long imageId;

		public ImageTask(int position, long imageId, ViewHolder viewHolder) {
			this.position = position;
			this.imageId = imageId;
			this.viewHolder = viewHolder;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			pathName = params[0];

			if (!TextUtils.isEmpty(pathName)) {
				BitmapLruCache cache = BitmapLruCache.getInstance();

				if (cache.get(pathName) == null) {
					Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
							App.getContext().getContentResolver(), imageId,
							MediaStore.Images.Thumbnails.MINI_KIND, null);
					if (cache != null && bitmap != null) {
						cache.put(pathName, bitmap);
					}
					return bitmap;
				} else {
					return cache.get(pathName);
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (this.position == viewHolder.getPosition() && bitmap != null) {
				// faster to use image alpha than setAlpha()
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					viewHolder.mImageView.setImageAlpha(0);
					ObjectAnimator fade =
							ObjectAnimator.ofInt(viewHolder.mImageView, "imageAlpha", 255);
					fade.start();
				} else {
					viewHolder.mImageView.setAlpha(0.0f);
					viewHolder.mImageView.animate().alpha(1.0f);
				}
				viewHolder.mImageView.setImageBitmap(bitmap);

				PaletteLruCache colorCache = PaletteLruCache.getInstance();

				if (colorCache.get(pathName) == null) {
					viewHolder.mPaletteTask =
							new PaletteImageTask(position, viewHolder)
									.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pathName);
				} else {
					setPalette(viewHolder, colorCache.get(pathName));
				}
			}
		}
	}

	/**
	 * Generates a higher resolution image to use for palette generation.
	 */
	private static class PaletteImageTask extends AsyncTask<String, Void, Bitmap> {

		private int position;
		private ViewHolder viewHolder;
		private String pathName;

		private PaletteImageTask(int position, ViewHolder viewHolder) {
			this.position = position;
			this.viewHolder = viewHolder;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			int size = App.getContext().getResources()
					.getDimensionPixelSize(R.dimen.item_gallery_image_height);

			this.pathName = params[0];

			if (this.pathName != null) {
				return GalleryUtils.decodeSampledBitmapFromResource(pathName, size, size);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (position == viewHolder.getPosition() && bitmap != null) {
				viewHolder.mPaletteTask =
						Palette.generateAsync(bitmap, viewHolder.mColorViews.length,
								new PaletteListener(position, viewHolder, pathName));
			}
		}
	}

	/**
	 * @param viewHolder the view holder to set the colors of
	 * @param swatches   the palette colors
	 */
	private static void setPalette(ViewHolder viewHolder, List<Palette.Swatch> swatches) {
		if (viewHolder != null) {
			viewHolder.mNumColors = swatches.size();

			for (int j = 0; j < viewHolder.mColorViews.length; j++) {
				if (j < viewHolder.mNumColors) {
					viewHolder.mColors[j] = swatches.get(j).getRgb();

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						ObjectAnimator fade = ObjectAnimator.ofArgb(viewHolder.mColorViews[j],
								"backgroundColor",
								viewHolder.mColors[j] & 0xFFFFFF,
								(0xFF) << 24 | viewHolder.mColors[j] & 0xFFFFFF);
						fade.start();
					} else {
						viewHolder.mColorViews[j].setBackgroundColor(viewHolder.mColors[j]);
						viewHolder.mColorViews[j].setAlpha(0.0f);
						viewHolder.mColorViews[j].animate().alpha(1.0f);
					}
					viewHolder.mColorViews[j].setVisibility(View.VISIBLE);
				} else {
					viewHolder.mColorViews[j].setVisibility(View.GONE);
				}
			}
		}
	}

	/**
	 * Sets the palette of an image once it is ready.
	 */
	private static class PaletteListener implements Palette.PaletteAsyncListener {

		private int position;
		private ViewHolder viewHolder;
		private String pathName;

		public PaletteListener(int position, ViewHolder viewHolder, String pathName) {
			this.position = position;
			this.viewHolder = viewHolder;
			this.pathName = pathName;
		}

		@Override
		public void onGenerated(Palette palette) {
			if (position == viewHolder.getPosition() && palette != null) {
				// make a copy to sort and store
				List<Palette.Swatch> swatches = new ArrayList<>(palette.getSwatches());
				Collections.sort(swatches, mSwatchComparator);

				// put the swatches into the cache
				PaletteLruCache colorCache = PaletteLruCache.getInstance();
				colorCache.put(pathName, swatches);

				setPalette(viewHolder, swatches);
			}
		}
	}

	public GalleryAdapter(Cursor cursor) {
		mCursor = cursor;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View root = LayoutInflater.from(viewGroup.getContext())
				.inflate(R.layout.item_gallery, viewGroup, false);

		final ViewHolder viewHolder = new ViewHolder((ViewGroup) root);
		viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mOnItemClickListener != null) {
					mOnItemClickListener.onItemClicked(viewHolder);
				}
			}
		});

		return viewHolder;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int i) {

		// Cancel tasks
		if (viewHolder.mImageTask != null && !viewHolder.mImageTask.isCancelled() &&
				viewHolder.mImageTask.getStatus() != AsyncTask.Status.FINISHED) {
			viewHolder.mImageTask.cancel(true);
		}

		if (viewHolder.mPaletteTask != null && !viewHolder.mPaletteTask.isCancelled() &&
				viewHolder.mPaletteTask.getStatus() != AsyncTask.Status.FINISHED) {
			viewHolder.mPaletteTask.cancel(true);
		}

		// Reset image and colors
		viewHolder.mImageView.setImageBitmap(null);
		for (int j = 0; j < viewHolder.mColorViews.length; j++) {
			viewHolder.mColorViews[j].setVisibility(View.GONE);
		}

		viewHolder.mPath = null;
		viewHolder.mId = -1;
		viewHolder.mNumColors = 0;

		if (mCursor != null) {
			mCursor.moveToPosition(i);

			int idIdx = mCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
			long id = mCursor.getLong(idIdx);

			int dataIdx = mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			String path = mCursor.getString(dataIdx);

			if (!TextUtils.isEmpty(path)) {
				viewHolder.mPath = path;
				viewHolder.mId = id;
				viewHolder.mImageTask = new ImageTask(i, id, viewHolder);
				viewHolder.mImageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path);
			} else {
				Log.e(TAG, "Data column is null!");
			}
		} else {
			Log.e(TAG, "Cursor is null!");
		}
	}

	@Override
	public int getItemCount() {
		if (mCursor != null) {
			return mCursor.getCount();
		}

		return 0;
	}

	public void changeCursor(Cursor cursor) {
		mCursor = cursor;
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		mOnItemClickListener = listener;
	}
}

package com.dreamynomad.colorcam;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class ImageActivity extends Activity {

	private static final String TAG = ImageActivity.class.getSimpleName();

	public static final String EXTRA_IMAGE_PATH = "com.dreamynomad.colorcam.image_path";
	public static final String EXTRA_IMAGE_URI = "com.dreamynomad.colorcam.image_uri";
	public static final String EXTRA_COLORS = "com.dreamynomad.colorcam.colors";

	private static final int NUM_COLORS = 6;

	private String mPath;
	private Uri mUri;
	private int[] mColors;

	private ImageView mImageView;
	private View mSwatches;

	private View.OnTouchListener mImageTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (mImageView != null && mSwatches != null) {
				float y = event.getY();
				float center = mImageView.getY() + mImageView.getMeasuredHeight() / 2;

				float[] matrix = new float[9];
				mImageView.getImageMatrix().getValues(matrix);
				float imageHeight = matrix[Matrix.MSCALE_Y]
						* mImageView.getDrawable().getIntrinsicHeight();

				if (y - mSwatches.getMeasuredHeight() / 2 < center - imageHeight / 2) {
					y = center - imageHeight / 2 + mSwatches.getMeasuredHeight() / 2;
				} else if (y + mSwatches.getMeasuredHeight() / 2 >
						center + imageHeight / 2) {
					y = center + imageHeight / 2 - mSwatches.getMeasuredHeight() / 2;
				}

				mSwatches.setY(y - mSwatches.getMeasuredHeight() / 2);
			}

			return true;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

		Intent intent = getIntent();

		String action = intent.getAction();
		String type = intent.getType();

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);

		int min = Math.min(Math.min(size.x, size.y), 2048);

		setContentView(R.layout.activity_image);

		mImageView = (ImageView) findViewById(R.id.full_image);
		mSwatches = findViewById(R.id.swatches);

		if (mImageView != null) {
			mImageView.setOnTouchListener(mImageTouchListener);
		}

		if (intent.hasExtra(EXTRA_IMAGE_PATH)) {
			mPath = intent.getStringExtra(EXTRA_IMAGE_PATH);
			Bitmap bitmap = GalleryUtils.decodeSampledBitmapFromResource(mPath, min, min);

			if (bitmap != null) {
				if (mImageView != null) {
					mImageView.setImageBitmap(bitmap);
				}

				if (intent.hasExtra(EXTRA_COLORS)) {
					int[] colors = intent.getIntArrayExtra(EXTRA_COLORS);

					if (colors != null && colors.length > 0) {
						mColors = colors;
						setColors(colors, getSwatchesWidth(bitmap));
					} else {
						new ColorTask().execute(bitmap);
					}
				} else {
					new ColorTask().execute(bitmap);
				}
			} else {
				finish();
			}
		} else if (intent.hasExtra(EXTRA_IMAGE_URI)) {
			Uri imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI);

			if (imageUri != null) {
				if (mSwatches != null) {
					mSwatches.setVisibility(View.GONE);
				}

				mUri = imageUri;
				new ImageTask(min).execute(imageUri);
			} else {
				finish();
			}
		} else if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
			Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

			if (imageUri != null) {
				if (mSwatches != null) {
					mSwatches.setVisibility(View.GONE);
				}

				mUri = imageUri;
				new ImageTask(min).execute(imageUri);
			} else {
				finish();
			}
		}
	}

	private Bitmap loadUri(Uri imageUri, int min, boolean inMutable) {
		try {
			Bitmap bitmap;
			ParcelFileDescriptor parcelFileDescriptor =
					getContentResolver().openFileDescriptor(imageUri, "r");
			FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

			if (min < 0) {
				// Load entire bitmap. Actual size limit is 32k, but it is unlikely to be reached.
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inMutable = inMutable;
				bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
			} else {
				bitmap = GalleryUtils.decodeSampledBitmapFromResource(
						fileDescriptor, min, min, inMutable);
			}

			parcelFileDescriptor.close();

			return bitmap;
		} catch (IOException e) {
			Log.e(TAG, "Could not load file from: " + imageUri.toString(), e);
		}

		return null;
	}

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public int getSwatchesWidth(Bitmap bitmap) {
		if (bitmap != null) {
			Rect rectangle = new Rect();
			Window window = getWindow();
			window.getDecorView().getWindowVisibleDisplayFrame(rectangle);

			int actionBarHeight = 0;

			// Calculate ActionBar height
			TypedValue tv = new TypedValue();
			if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
				actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
			}

			int height = rectangle.height() - actionBarHeight - getStatusBarHeight();

			if ((float) bitmap.getWidth() / bitmap.getHeight() >
					(float) rectangle.width() / height) {
				return rectangle.width();
			} else {
				return bitmap.getWidth() * height / bitmap.getHeight();
			}
		}

		return 0;
	}

	private class ImageTask extends AsyncTask<Uri, Void, Bitmap> {

		private int min;

		private ImageTask(int min) {
			this.min = min;
		}

		@Override
		protected Bitmap doInBackground(Uri... params) {
			Uri imageUri = params[0];

			return loadUri(imageUri, min, false);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				if (mImageView != null) {
					mImageView.setImageBitmap(bitmap);
				}

				new ColorTask().execute(bitmap);
			} else {
				finish();
			}
		}
	}

	private class ColorTask extends AsyncTask<Bitmap, Void, int[]> {

		private Bitmap bitmap;

		@Override
		protected void onPreExecute() {
			if (mSwatches != null) {
				mSwatches.setVisibility(View.GONE);
			}
		}

		@Override
		protected int[] doInBackground(Bitmap... params) {
			bitmap = params[0];

			Palette palette = Palette.generate(bitmap, NUM_COLORS);

			List<Palette.Swatch> swatches = new ArrayList<>(palette.getSwatches());
			Collections.sort(swatches, GalleryUtils.getSwatchComparator());

			int[] colors = new int[swatches.size()];

			for (int i = 0; i < colors.length; i++) {
				colors[i] = swatches.get(i).getRgb();
			}

			return colors;
		}

		@Override
		protected void onPostExecute(int[] colors) {
			mColors = colors;

			setColors(colors, getSwatchesWidth(bitmap));

			if (mSwatches != null) {
				mSwatches.setVisibility(View.VISIBLE);
			}
		}
	}

	private void setColors(int[] colors, int width) {
		if (mSwatches != null && width > 0) {
			FrameLayout.LayoutParams params =
					(FrameLayout.LayoutParams) mSwatches.getLayoutParams();
			params.width = width;
		}

		View[] colorViews = new View[NUM_COLORS];
		colorViews[0] = findViewById(R.id.color_0);
		colorViews[1] = findViewById(R.id.color_1);
		colorViews[2] = findViewById(R.id.color_2);
		colorViews[3] = findViewById(R.id.color_3);
		colorViews[4] = findViewById(R.id.color_4);
		colorViews[5] = findViewById(R.id.color_5);

		int diameter = width / 8;
		int margin = (width - colors.length * diameter) / colors.length / 2;

		LinearLayout.LayoutParams params =
				new LinearLayout.LayoutParams(diameter, diameter);
		params.setMargins(margin, margin, margin, margin);

		for (int i = 0; i < colorViews.length; i++) {
			if (colorViews[i] != null) {
				if (i < colors.length) {
					Drawable circle = getResources().getDrawable(R.drawable.circle);
					circle.setColorFilter(colors[i], PorterDuff.Mode.SRC_ATOP);
					colorViews[i].setBackgroundDrawable(circle);

					colorViews[i].setLayoutParams(params);
				} else {
					colorViews[i].setVisibility(View.GONE);
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_image, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		if (id == R.id.action_save) {
			saveImage();
			return true;
		} else if (id == R.id.action_share) {
			shareImage();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private File createFile() {
		Bitmap bitmap = null;

		if (mPath != null) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inMutable = true;
			bitmap = BitmapFactory.decodeFile(mPath, options);
		} else if (mUri != null) {
			bitmap = loadUri(mUri, -1, true);
		}

		if (bitmap == null || mColors == null) {
			return null;
		}

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL);
		Canvas canvas = new Canvas(bitmap);

		int diameter = bitmap.getWidth() / 8;
		int margin = (bitmap.getWidth() - mColors.length * diameter) / mColors.length / 2;

		// determine where the view is on the image
		float[] matrix = new float[9];
		mImageView.getImageMatrix().getValues(matrix);
		float imageHeight = matrix[Matrix.MSCALE_Y]
				* mImageView.getDrawable().getIntrinsicHeight();

		float viewY = mSwatches.getY() -
				(mImageView.getY() + mImageView.getMeasuredHeight() / 2 - imageHeight / 2) +
				mSwatches.getMeasuredHeight() / 2;

		// convert into fraction so that it is in right place on the bitmap
		float fractionY = viewY / imageHeight;

		for (int i = 0; i < mColors.length; i++) {
			paint.setColor(mColors[i]);

			canvas.drawCircle((2 * i + 1) * margin + i * diameter + diameter / 2,
					bitmap.getHeight() * fractionY, diameter / 2, paint);
		}

		File file = getOutputMediaFile(getResources().getString(R.string.app_name), ".jpg");

		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file);

			bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);

			fileOutputStream.flush();
			fileOutputStream.close();

			return file;
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Error creating file", e);
			Toast.makeText(getApplicationContext(), "Save Failed", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Log.e(TAG, "Error creating file", e);
			Toast.makeText(getApplicationContext(), "Save Failed", Toast.LENGTH_SHORT).show();
		}

		return null;
	}

	private class SaveTask extends AsyncTask<Void, Void, File> {

		private ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(ImageActivity.this);
			progressDialog.setMessage("Saving File");
			progressDialog.setIndeterminate(true);
			progressDialog.show();
		}

		@Override
		protected File doInBackground(Void... params) {
			return createFile();
		}

		@Override
		protected void onPostExecute(File file) {
			if (file != null) {
				Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				mediaScanIntent.setData(Uri.fromFile(file));
				ImageActivity.this.sendBroadcast(mediaScanIntent);
			}

			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}
	}

	private void saveImage() {
		new SaveTask().execute();
	}

	private class ShareTask extends AsyncTask<Void, Void, File> {

		private ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(ImageActivity.this);
			progressDialog.setMessage("Saving File");
			progressDialog.setIndeterminate(true);
			progressDialog.show();
		}

		@Override
		protected File doInBackground(Void... params) {
			return createFile();
		}

		@Override
		protected void onPostExecute(File file) {
			if (file != null) {
				Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				mediaScanIntent.setData(Uri.fromFile(file));
				ImageActivity.this.sendBroadcast(mediaScanIntent);

				// Create the new Intent using the 'Send' action.
				Intent share = new Intent(Intent.ACTION_SEND);

				// Set the MIME type
				share.setType("image/jpeg");

				Uri uri = Uri.fromFile(file);

				// Add the URI and the caption to the Intent.
				share.putExtra(Intent.EXTRA_STREAM, uri);

				// Broadcast the Intent.
				startActivity(Intent.createChooser(share,
						getResources().getString(R.string.share_to)));
			}

			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}
	}

	private void shareImage() {
		new ShareTask().execute();
	}

	public static File getOutputMediaFile(String subdir, String extension) {

		// TODO: you should check that the SDCard is mounted using
		// Environment.getExternalStorageState() before doing this.
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), subdir);

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
			Log.d(TAG, "failed to create directory");
			return null;
		}

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
				"IMG_" + timeStamp  + extension);

		return mediaFile;
	}
}

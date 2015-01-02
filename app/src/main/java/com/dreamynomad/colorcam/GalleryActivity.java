package com.dreamynomad.colorcam;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import java.util.Arrays;

/**
 * Allows user to browse images on the device.
 */
public class GalleryActivity extends Activity
		implements LoaderManager.LoaderCallbacks<Cursor>, GalleryAdapter.OnItemClickListener {

	private static final int GALLERY_LOADER = 1;

	private static final int RESULT_GALLERY = 100;
	private static final int RESULT_GALLERY_KITKAT = 101;

	private RecyclerView mGallery;
	private GalleryAdapter mAdapter;
	private RecyclerView.LayoutManager mLayoutManager;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

		setContentView(R.layout.activity_gallery);

		mGallery = (RecyclerView) findViewById(R.id.gallery);
		mGallery.setHasFixedSize(true);

		int spanCount;

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			spanCount = 3;
		} else {
			spanCount = 2;
		}

		mLayoutManager = new GridLayoutManager(this, spanCount);
		mGallery.setLayoutManager(mLayoutManager);

		getLoaderManager().initLoader(GALLERY_LOADER, null, this);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			finishAfterTransition();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		if (id == GALLERY_LOADER) {
			return new CursorLoader(this,
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA},
					null, null, MediaStore.Images.Media.DATE_TAKEN + " DESC");
		}

		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (mAdapter == null) {
			mAdapter = new GalleryAdapter(data);
			mAdapter.setOnItemClickListener(this);
			mGallery.setAdapter(mAdapter);
		} else {
			mAdapter.changeCursor(data);
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}

	@Override
	public void onItemClicked(GalleryAdapter.ViewHolder viewHolder) {
		if (viewHolder != null && viewHolder.mPath != null) {
			Intent intent = new Intent(this, ImageActivity.class);

			intent.putExtra(ImageActivity.EXTRA_IMAGE_ID, viewHolder.mId);
			intent.putExtra(ImageActivity.EXTRA_IMAGE_PATH, viewHolder.mPath);

			int[] colors = Arrays.copyOf(viewHolder.mColors, viewHolder.mNumColors);
			intent.putExtra(ImageActivity.EXTRA_COLORS, colors);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				viewHolder.mImageView.setTransitionName("edit");

				for (int i = 0; i < viewHolder.mNumColors; i++) {
					viewHolder.mColorViews[i].setTransitionName("color_" + i);
				}

				Pair[] pairs = new Pair[viewHolder.mNumColors + 1];
				pairs[0] = new Pair<View, String>(viewHolder.mImageView, "edit");
				for (int i = 1; i < viewHolder.mNumColors + 1; i++) {
					pairs[i] = new Pair<>(viewHolder.mColorViews[i - 1], "color_" + (i - 1));
				}

				// create the transition animation - the images in the layouts
				// of both activities are defined with android:transitionName="robot"
				ActivityOptions options = ActivityOptions
						.makeSceneTransitionAnimation(this, pairs);
				// start the new activity
				startActivity(intent, options.toBundle());
			} else {
				startActivity(intent);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_gallery, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_import) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/jpeg");
				Intent chooserIntent = Intent.createChooser(intent,
						getResources().getString(R.string.choose_image));
				startActivityForResult(chooserIntent, RESULT_GALLERY);
			} else {
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("image/jpeg");
				startActivityForResult(intent, RESULT_GALLERY_KITKAT);
			}

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			if (data != null && data.getData() != null) {
				Intent intent = new Intent(this, ImageActivity.class);
				intent.putExtra(ImageActivity.EXTRA_IMAGE_URI, data.getData());

				startActivity(intent);
			}
		}
	}
}

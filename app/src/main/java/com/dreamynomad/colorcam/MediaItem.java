package com.dreamynomad.colorcam;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a simple item from the device {@link android.provider.MediaStore}.
 *
 * Created by Eric on 1/21/2015.
 */
public class MediaItem implements Parcelable {

	private final long mId;
	private final String mPath;

	public MediaItem(long id, String path) {
		this.mId = id;
		this.mPath = path;
	}

	public MediaItem(Parcel in) {
		mId = in.readInt();
		mPath = in.readString();
	}

	public long getId() {
		return mId;
	}

	public String getPath() {
		return mPath;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mId);
		dest.writeString(mPath);
	}

	public static final Parcelable.Creator<MediaItem> CREATOR =
			new Parcelable.Creator<MediaItem>() {

		public MediaItem createFromParcel(Parcel in) {
			return new MediaItem(in);
		}

		public MediaItem[] newArray(int size) {
			return new MediaItem[size];
		}
	};
}

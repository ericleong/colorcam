package com.dreamynomad.colorcam;

/**
 * Represents a simple item from
 *
 * Created by Eric on 1/21/2015.
 */
public class MediaItem {

	private final long mId;
	private final String mPath;

	public MediaItem(long id, String path) {
		this.mId = id;
		this.mPath = path;
	}

	public long getId() {
		return mId;
	}

	public String getPath() {
		return mPath;
	}
}

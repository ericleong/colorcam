package com.dreamynomad.colorcam;

import android.app.Application;
import android.content.Context;

/**
 * Created by Eric on 11/28/2014.
 */
public class App extends Application {

	private static Context sContext;

	@Override
	public void onCreate() {
		super.onCreate();

		sContext = getApplicationContext();
	}

	public static Context getContext() {
		return sContext;
	}
}

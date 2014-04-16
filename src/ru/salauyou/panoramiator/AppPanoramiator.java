package ru.salauyou.panoramiator;

import android.app.Application;
import android.util.Log;

public class AppPanoramiator extends Application {
	@Override
	public void onCreate(){
		super.onCreate();
		Log.d("debug", "Application created");
		Controller.getInstance().runController(getApplicationContext());
		Controller.getInstance().getImageContainer().setQty(10);
	}
}

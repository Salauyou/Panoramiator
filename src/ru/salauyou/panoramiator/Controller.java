package ru.salauyou.panoramiator;

import android.content.Context;
import android.util.Log;

/**
 * Controller is a Singleton class, its task is to provide access to GeolocService and ImageContainer globally
 * After creation, main activity needs to call runController() method with its context passed within it
 */

public class Controller {
	
	private static volatile Controller instance;
	private C_GeolocService geolocService;
	private C_ImageContainer imageContainer;
	
	private Controller(){
		// private constructor	
		Log.println(Log.DEBUG, "panoramiator", "Controller created");
	}
	
	public void runController(Context context){
		// create and run geolocService using context for LocationManager
		if (geolocService == null){
			geolocService = new C_GeolocService(context);
			Log.println(Log.DEBUG, "panoramiator", "Geoloc created");
		}
		// create and run imageContainer
		if (imageContainer == null){
			imageContainer = new C_ImageContainer();
			Log.println(Log.DEBUG, "panoramiator", "Image Container created");
			// add imageContainer as a Geoloc listener
			geolocService.addListener(imageContainer);
			
		}
	}
	
	public static Controller getInstance() {
		if (instance == null){
			instance = new Controller();
		}
		return instance;
	}
	
	public C_GeolocService getGeolocService() {
		return geolocService == null ? null : geolocService;
	}
	
	public C_ImageContainer getImageContainer() {
		return imageContainer == null ? null : imageContainer;
	}
	
}
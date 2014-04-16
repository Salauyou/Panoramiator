package ru.salauyou.panoramiator;

import android.content.Context;
import android.util.Log;

/**
 * Controller is a Singleton class, its task is to provide access to GeolocService and ImageContainer globally
 * After creation, main activity needs to call runController() method with its context passed within it
 */

public class Controller {
	
	private static volatile Controller instance;
	private GeolocService geolocService;
	private ImageContainer imageContainer;
	
	private Controller(){
		// private constructor	
		Log.d("debug", "Controller created");
	}
	
	/**
	 * Run Controller
	 * 
	 * @param context	Context of activity or application
	 */
	public void runController(Context context){
		
		// create and run geolocService using context for LocationManager
		if (geolocService == null){
			geolocService = new GeolocService(context);
			Log.d("debug", "Geoloc created");
		}
		// create and run imageContainer
		if (imageContainer == null){
			imageContainer = new ImageContainer();
			Log.d("debug", "Image Container created");
			geolocService.addListener(imageContainer);
		}
	}
	
	/**
	 * Get instance
	 * 
	 * @return	Instance of class
	 */
	public static Controller getInstance() {
		if (instance == null){
			instance = new Controller();
		}
		return instance;
	}
	
	/**
	 * Get geolocation service object
	 * 
	 * @return	Geolocation service object
	 */
	public GeolocService getGeolocService() {
		return geolocService == null ? null : geolocService;
	}
	
	/**
	 * Get image container
	 * @return	Image container
	 */
	public ImageContainer getImageContainer() {
		return imageContainer == null ? null : imageContainer;
	}
	
}

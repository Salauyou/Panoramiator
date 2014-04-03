package ru.salauyou.panoramiator;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

/**
 * GeolocService provides objects access to device location and its changes, based on Network and GPS providers.
 * To register/unregister listener, addListener()/removeListener() method should be used.
 * Listeners should implement I_GeolocListener interface to receive location changes.
 */

public class C_GeolocService {

	static public final int STATUS_DISABLED = 0;
	static public final int STATUS_LASTKNOWN = 1;
	static public final int STATUS_NETWORK = 2;
	static public final int STATUS_GPS = 3;
	static public final float DISTANCE_UPDATE_GPS = 10;		// distance (meters) in which GPS location should be updated
	static public final float DISTANCE_UPDATE_NETWORK = 40;	// same for network location
	static public final long PERIOD_UPDATE_GPS = 15;		// period (seconds) in which GPS location should be updated
	static public final long PERIOD_UPDATE_NETWORK = 60;	// same for network location
	
	protected List<I_GeolocListener> listeners;
	protected Location location;	// stores current location
	protected int locationStatus;	// stores current location status
	
	boolean isGpsAvailable = false;
	
	
	/**
	 * Public constructor
	 * 
	 * @param	context		The context of activity or application
	 */
	public C_GeolocService(Context context){
		// initialize location object
		location = new Location(LocationManager.GPS_PROVIDER);
		locationStatus = STATUS_DISABLED;
		
		// create and implement location listener
		LocationListener listener = new LocationListener(){

			@Override
			public void onLocationChanged(Location locationNew) {
				
				try {
					if (locationNew.getProvider().equals(LocationManager.GPS_PROVIDER)){
						isGpsAvailable = true;
					}
					
					//   1 check if last location was obtained not from GPS nor Network provider 
					// If yes, skip other conditions and update.
					//   2 check if GPS is avaliable and new location was received from GPS
					//   3 check is GPS is unavalable
					// If 2 or 3, need to follow at least one of accuracy conditions:
					//   4 if new location has an accuracy and current doesn't
					//   5 if new location is more accurate than current
					//   6 if distance between new location and current one is greater than sum of their accuracy values
					// If at least one of 4, 5, 6 is followed, update location
					if (locationStatus == STATUS_DISABLED || locationStatus == STATUS_LASTKNOWN  // 1
						||  ((isGpsAvailable && locationNew.getProvider().equals(LocationManager.GPS_PROVIDER)) // 2 
							  || !isGpsAvailable    // 3 
							) && ((locationNew.hasAccuracy() && !location.hasAccuracy())  // 4
							       || (locationNew.hasAccuracy() && location.hasAccuracy() && (locationNew.getAccuracy() < location.getAccuracy())) // 5
							       || (locationNew.hasAccuracy() && location.hasAccuracy() && (locationNew.distanceTo(location) > locationNew.getAccuracy() + location.getAccuracy())) // 6
							     )
						)     
					{
						// if location was updated, store new location...
						location = locationNew;
						if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
							locationStatus = STATUS_GPS;
						} else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
							locationStatus = STATUS_NETWORK;
						}
						//... and send it to listeners
						for (I_GeolocListener listener : listeners){
							try {
								listener.locationUpdate(location.getLongitude(), location.getLatitude(), locationStatus);
							} catch (Throwable e) { }
						}
						// log
						Log.println(Log.DEBUG, "panoramiator", "Location updated: " + location.getProvider());
					}
				} catch (Throwable e) { }
			}

			@Override
			public void onProviderDisabled(String provider) {
				if (provider.equals(LocationManager.GPS_PROVIDER)){
					isGpsAvailable = false;
				}
			}

			@Override
			public void onProviderEnabled(String provider) {

			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				if (provider.equals(LocationManager.GPS_PROVIDER) 
				    && (status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE)){
					isGpsAvailable = false;
				}
			}		
		}; 
			
		// register in Location Manager and add GPS and Network location providers
		try {
			LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, PERIOD_UPDATE_GPS*1000, DISTANCE_UPDATE_GPS, listener);
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, PERIOD_UPDATE_NETWORK*1000, DISTANCE_UPDATE_NETWORK, listener);
			if (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null){
				location.set(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
				locationStatus = STATUS_LASTKNOWN;
			} else if (locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null) {
				location.set(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
				locationStatus = STATUS_LASTKNOWN;
			}
		} catch (Throwable e) { };
		
		// initialize listeners list
		listeners = new ArrayList<I_GeolocListener>();
	}
	
	/**
	 * Get current location
	 * 
	 * @return	current location
	 */
	public Location getLocation(){
		return location;
	}
	
	/**
	 * Get current status of location
	 * 
	 * @return	Status and provider of current location. Selected from STATUS_DISABLED, STATUS_UNKNOWN, STATUS_GPS, STATUS_NETWORK
	 */
	public int getLocationStatus(){
		return locationStatus;
	}
	
	/**
	 * Add listener to receive location updates. Current location will be send right after adding
	 * 
	 * @param listener	Must implement I_GeolocListener interface
	 */
	public void addListener(I_GeolocListener listener){
		if (!listeners.contains(listener)) {
			listeners.add(listener);
			// sending last known location to new listener
			try {
				listener.locationUpdate(location.getLongitude(), location.getLatitude(), locationStatus);
			} catch (Throwable e) {}
		}
	}
	
	/**
	 * Remove listener if it is attached.
	 * 
	 * @param listener	Must implement I_GeolocListener interface
	 */
	/* removing listener from the list of location update listeners */
	public void removeListener(I_GeolocListener listener){
		try {
			listeners.remove(listeners.indexOf(listener));
		} catch (Throwable e){}
	}
}

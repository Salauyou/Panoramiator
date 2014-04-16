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
 * To subscribe/unsubscribe listener, use {@code addListener()} and {@code removeListener()}.
 * Listener(s) must implement {@code GeolocService.Listener} to receive location updates.
 */

public class GeolocService {

	/**
	 * Possible location status
	 */
	public enum Status{
		DISABLED("N/A"), LASTKNOWN("Last known"), NETWORK("Network"), GPS("GPS");
		
		private String msg;
		private Status(String msg){
			this.msg = msg;
		}
		
		@Override
		public String toString(){
			return this.msg;
		}
	}
	
	/**
	 * Public interface to send location changes
	 */
	public interface Listener {

		/**
		 * Callback to be called when location changes
		 * 
		 * @param longitude
		 * @param latitude
		 * @param status	Status or provider of the location
		 */
		void locationUpdate(double longitude, double latitude, Status status);
		
	}
	
	static public final float DISTANCE_UPDATE_GPS = 10;		// distance (meters) in which GPS location should be updated
	static public final float DISTANCE_UPDATE_NETWORK = 40;	// same for network location
	static public final long PERIOD_UPDATE_GPS = 15;		// period (seconds) in which GPS location should be updated
	static public final long PERIOD_UPDATE_NETWORK = 60;	// same for network location
	
	protected List<Listener> listeners;
	protected Location location;	
	protected Status locationStatus;	
	
	boolean isGpsAvailable = false;
	
	
	/**
	 * Public constructor
	 * 
	 * @param	context		The context of activity or application
	 */
	public GeolocService(Context context){
		
		listeners = new ArrayList<Listener>();
		
		location = new Location(LocationManager.GPS_PROVIDER);
		locationStatus = Status.DISABLED;
	
		LocationListener listener = new LocationListener(){

			@Override
			public void onLocationChanged(Location locationNew) {

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
				if (locationStatus == Status.DISABLED || locationStatus == Status.LASTKNOWN  // 1
					||  ((isGpsAvailable && locationNew.getProvider().equals(LocationManager.GPS_PROVIDER)) // 2 
						  || !isGpsAvailable    // 3 
						) && ((locationNew.hasAccuracy() && !location.hasAccuracy())  // 4
						       || (locationNew.hasAccuracy() && location.hasAccuracy() && (locationNew.getAccuracy() < location.getAccuracy())) // 5
						       || (locationNew.hasAccuracy() && location.hasAccuracy() && (locationNew.distanceTo(location) > locationNew.getAccuracy() + location.getAccuracy())) // 6
						     )
					)     
				{
					
					location = locationNew;
					if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
						locationStatus = Status.GPS;
					} else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
						locationStatus = Status.NETWORK;
					}
					// send new coordinates to listeners
					for (Listener listener : listeners){
						if (listener != null){
                            listener.locationUpdate(location.getLongitude(), location.getLatitude(), locationStatus);
						}
					}
					Log.d("debug", "New location sent: " + locationStatus);
				}
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
			
		// subscribe listener to both GPS and network location providers
		
		LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		if (locationManager != null){
			try {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, PERIOD_UPDATE_GPS*1000, DISTANCE_UPDATE_GPS, listener);
			} catch (IllegalArgumentException e) {
				// GPS provider is inaccessible 
			}
			try {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, PERIOD_UPDATE_NETWORK*1000, DISTANCE_UPDATE_NETWORK, listener);
			} catch (IllegalArgumentException e) {
				// network provider is inaccessible	
			}
			if (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null){
				location.set(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
				locationStatus = Status.LASTKNOWN;
			} else if (locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null) {
				location.set(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
				locationStatus = Status.LASTKNOWN;
			}
		}
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
	 * @return	Status and provider of current location. Selected from {@code Status} enum
	 */
	public Status getLocationStatus(){
		return locationStatus;
	}
	
	/**
	 * Add listener to receive location updates. Current location will be send immediately
	 * 
	 * @param listener	Must implement {@code GeolocService.Listener}
	 */
	public void addListener(Listener listener){
		if (!listeners.contains(listener)) {
			listeners.add(listener);
			listener.locationUpdate(location.getLongitude(), location.getLatitude(), locationStatus);
			Log.d("debug", "Current location sent: " + locationStatus);
		}
	}
	
	/**
	 * Remove listener if it is attached.
	 * 
	 * @param listener	Must implement {@code GeolocService.Listener}
	 */
	public void removeListener(Listener listener){
		int listenerPosition = listeners.indexOf(listener);
		if (listenerPosition != -1){
			listeners.remove(listeners.indexOf(listener));
		}
	}
}

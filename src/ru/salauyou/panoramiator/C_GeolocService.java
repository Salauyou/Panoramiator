package ru.salauyou.panoramiator;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * GeolocService provides objects access to device location and its changes, based on Network and GPS providers.
 * To register/unregister listener, addListener()/removeListener() method should be used.
 * Listeners should implement I_GeolocListener interface to receive location changes.
 */

public class C_GeolocService implements LocationListener {

	static public final int STATUS_DISABLED = 0;
	static public final int STATUS_LASTKNOWN = 1;
	static public final int STATUS_NETWORK = 2;
	static public final int STATUS_GPS = 3;
	static public final float DISTANCE_UPDATE_GPS = 10;		// distance (meters) in which GPS location should be updated
	static public final float DISTANCE_UPDATE_NETWORK = 40;	// same for network location
	static public final long PERIOD_UPDATE_GPS = 15;		// period (seconds) in which GPS location should be updated
	static public final long PERIOD_UPDATE_NETWORK = 60;	// same for network location
	
	private List<I_GeolocListener> listeners;
	private Location location;	// stores current location
	private int locationStatus;	// stores current location status
	
	private LocationManager locationManager;
	
	
	
	/* =========== constructor ==================== */
	public C_GeolocService(Context context){
		// initialize location object
		location = new Location(LocationManager.GPS_PROVIDER);
		locationStatus = STATUS_DISABLED;
		
		// register in Location Manager and add GPS and Network location providers
		try {
			locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, PERIOD_UPDATE_GPS*1000, DISTANCE_UPDATE_GPS, this);
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, PERIOD_UPDATE_NETWORK*1000, DISTANCE_UPDATE_NETWORK, this);
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
	
	/* ---------- method to get current location -------------- */
	public Location getLocation(){
		return location;
	}
	
	/* ---------- method to get current location status ---------- */
	public int getLocationStatus(){
		return locationStatus;
	}
	
	/* =========== working with Geoloc Listeners ================ */
	
	/* adding listener to the list of location update listeners */
	public void addListener(I_GeolocListener listener){
		if (!listeners.contains(listener)) {
			listeners.add(listener);
			// sending last known location to new listener
			try {
				listener.locationUpdate(location.getLongitude(), location.getLatitude(), locationStatus);
			} catch (Throwable e) {}
		}
	}
	
	/* removing listener from the list of location update listeners */
	public void removeListener(I_GeolocListener listener){
		try {
			listeners.remove(listeners.indexOf(listener));
		} catch (Throwable e){}
	}
	
	
	/* =========== LocationListener interface implementation ============ */
	@Override
	public void onLocationChanged(Location locationNew) {
		// check whether new location is more accurate
		// or new location locates far enough from current
		try {
			if (locationStatus == STATUS_DISABLED || locationStatus == STATUS_LASTKNOWN 
					|| (locationNew.hasAccuracy() && !location.hasAccuracy()) 
					|| (locationNew.hasAccuracy() && location.hasAccuracy() && (locationNew.getAccuracy() < location.getAccuracy()))
					|| (locationNew.hasAccuracy() && (locationNew.distanceTo(location) > locationNew.getAccuracy()))
					) 
			{
				// if location was updated, store new location...
				location.set(locationNew);
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
				Log.println(Log.DEBUG, "panoramiator", "Location updated");
			}
		} catch (Throwable e) { }
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}
	
}

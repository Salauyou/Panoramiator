package ru.salauyou.panoramiator;

public interface I_GeolocListener {

	/* callback on location update */
	void locationUpdate(double longitude, double latitude, int provider);
	
}

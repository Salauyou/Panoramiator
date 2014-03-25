package ru.salauyou.panoramiator;

public interface I_GeolocListener {

	/* callback when location was updated */
	void locationUpdate(double longitude, double latitude, int provider);
	
}

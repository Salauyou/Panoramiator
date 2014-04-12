package ru.salauyou.panoramiator;

import java.util.ArrayList;

import android.util.Log;

/** 
 * ImageContainer class provides access to Panoramio photos that were taken near current location.
 */

public class C_ImageContainer implements C_GeolocService.Listener, C_ImageListUpdater.Receiver{

	private ArrayList<C_Image> images;
	private int qtyNeeded;		// quantity of images needed for the slideshow
	private int lastRequested;		// index of last requested image in slide show
	private int idUpdater;			// current callback id of ImageListUpdater
	private double _longitude;	// current geolocation coordinates
	private double _latitude;
	
	/**
	 * Default constructor
	 */
	public C_ImageContainer(){
		lastRequested = -1;
		images = new ArrayList<C_Image>();
		Controller.getInstance().getGeolocService().addListener(this);
	}
	
	/**
	 * Get next image with ready bitmap, providing circullar search within the container
	 * 
	 * @return	
	 */
	public C_Image getNext(){
		
		if (images.size() == 0){
			return null;
		}
		if (lastRequested == -1){
			// at the first call, set lastRequested to the last element of the list,
			// so the next element will be 0
			lastRequested = images.size() - 1;
		}
		int next = lastRequested + 1;
		int imageFound = -1;
		// looking for next ready image in the list circulary
		while(imageFound < 0 && next != lastRequested) {
			if (next >= images.size()){
				next = 0;
			}
			if (images.get(next).isReady()){
				imageFound = next;
			} else {
				images.get(next).startDownload();
			}
			next++;
		}
		if (imageFound >= 0){
			Log.println(Log.DEBUG, "panoramiator", "Requested next image: " + imageFound);
			// if ready image was found, return it and move cursor to it
			lastRequested = imageFound;
			return images.get(imageFound);
		} else {
			// else, set cursor to the start position
			Log.println(Log.DEBUG, "panoramiator", "Requested next image: " + imageFound);
			lastRequested = -1;
			return null;
		}
	}
		
	/**
	 * Get current image
	 * 
	 * @return
	 */
	public C_Image getCurrent(){
		if (lastRequested >= 0){
			Log.println(Log.DEBUG, "panoramiator", "Requested curren image: " + lastRequested);
			return images.get(lastRequested);
		} else {
			Log.println(Log.DEBUG, "panoramiator", "Requested curren image: " + lastRequested);
			return null;
		}
	}
	
	/**
	 * Set internal cursor position to start of the container
	 */
	public void reset(){
		lastRequested = -1;
	}

	/**
	 * Get quantity of images that was asked to download
	 * 
	 * @return
	 */
	public int getQty(){
		return qtyNeeded;
	}
	
	
	/**
	 * Get actual quantity of images in the container, both ready and non-ready
	 * 
	 * @return
	 */
	public int getQtyActual(){
		return images.size();
	}
	
	/**
	 * Get quantity of images that have ready bitmap
	 * 
	 * @return
	 */
	public int getQtyReady(){
		int qtyReady = 0;
		for (C_Image image : images){
			if (image.isReady()){
				qtyReady++;
			}
		}
		return qtyReady;
	}
	
    /**
     * Update desired quantity of images
     * 
     * @param qty
     */
	public void setQty(final int qty){
		if (qty != qtyNeeded) {
			if (qty > 0 && Controller.getInstance().getGeolocService().getLocationStatus()!= C_GeolocService.Status.DISABLED){
				qtyNeeded = qty;
				if (qty > images.size()){
					// if new quantity greater than existing, uploade new image list
					new C_ImageListUpdater().getImagesPanoramio(this, ++idUpdater, _longitude, _latitude, qtyNeeded);
				} else if (qty < images.size()){
					// if new quantity less then existing, just rearrange image list
					images = C_ImageListUpdater.getImagesNearestSorted(images, _longitude, _latitude, qtyNeeded);
				}
			} else {
				images.clear();
			}
		}
	}
	
	/**
	 * C_GeolocService.Status interface implementation
	 */
	@Override
	public void locationUpdate(double longitude, double latitude, C_GeolocService.Status provider){
		_longitude = longitude;	//update location
		_latitude = latitude;
		// create and start imageListUpdater
		if (provider != C_GeolocService.Status.DISABLED){
			new C_ImageListUpdater().getImagesPanoramio(this, ++idUpdater, _longitude, _latitude, qtyNeeded);
		} 
	}
	
	/**
	 * C_ImageListUpdater.Receiver interface implementation
	 */
	@Override 
	public void receiveImageList(ArrayList<C_Image> imagesReceived, int id){
		if (imagesReceived != null && id == idUpdater ){
			// create new image list
			ArrayList<C_Image> imagesNew = new ArrayList<C_Image>();
			boolean foundExisting;
			for (C_Image imageReceived : imagesReceived){    // iteration within received images
				foundExisting = false;
				for (C_Image imageExisting : images){        // iteration within existing images
					if (imageExisting.getUrl().equals(imageReceived.getUrl())){  
						// if image with the same url was found within existing images
						// add it into new image list
						imagesNew.add(imageExisting);
						foundExisting = true;
						break;
					}
				}
				// if received image url was not found within existing images,
				// push it into new image list
				if (!foundExisting){
					imagesNew.add(imageReceived);
				}
			}
			images = imagesNew;		// replace images list with newly created list						
		}
	}
}

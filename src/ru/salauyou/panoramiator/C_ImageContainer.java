package ru.salauyou.panoramiator;

import java.util.ArrayList;
import android.util.Log;

/** 
 * ImageContainer provides access to Panoramio photos that were taken near current location.
 */

public class C_ImageContainer implements I_GeolocListener, I_ImageListReceiver{

	private ArrayList<C_Image> images;
	private int qtyNeeded;		// quantity of images needed for the slideshow
	private int lastRequested;		// index of last requested image in slide show
	private int idUpdater;			// current callback id of ImageListUpdater
	private double _longitude;	// current geolocation coordinates
	private double _latitude;
	
	/* ========= constructor ======  */
	public C_ImageContainer(){
		lastRequested = -1;
		images = new ArrayList<C_Image>();
		Controller.getInstance().getGeolocService().addListener(this);
	}
	
	/* gets next ready image */
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
		
	/* get current image */
	public C_Image getCurrent(){
		if (lastRequested >= 0){
			Log.println(Log.DEBUG, "panoramiator", "Requested curren image: " + lastRequested);
			return images.get(lastRequested);
		} else {
			Log.println(Log.DEBUG, "panoramiator", "Requested curren image: " + lastRequested);
			return null;
		}
	}
	
	/* set cursor position to start of the list */
	public void reset(){
		lastRequested = -1;
	}

	/* get quantity of images desired to download */
	public int getQty(){
		return qtyNeeded;
	}
	
	/* get actual quantity of images, ready and non-ready */
	public int getQtyActual(){
		return images.size();
	}
	
	/* get quantity of images that is already downloaded */
	public int getQtyReady(){
		int qtyReady = 0;
		for (C_Image image : images){
			if (image.isReady()){
				qtyReady++;
			}
		}
		return qtyReady;
	}
	
	/* ============ updates quantity of images for the slideshow ========= */
	public void setQty(final int qty){
		if (qty != qtyNeeded) {
			if (qty > 0 && Controller.getInstance().getGeolocService().getLocationStatus()!= C_GeolocService.STATUS_DISABLED){
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
	
	/* ===== method(s) to implement I_GeolocListener interface ======== */
	@Override
	public void locationUpdate(double longitude, double latitude, int provider){
		_longitude = longitude;	//update location
		_latitude = latitude;
		// create and start imageListUpdater
		if (provider != C_GeolocService.STATUS_DISABLED){
			new C_ImageListUpdater().getImagesPanoramio(this, ++idUpdater, _longitude, _latitude, qtyNeeded);
		} 
	}
	
	/* ===== method(s) to implement I_ImageListReceiver ================= */
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

package ru.salauyou.panoramiator;

import java.util.ArrayList;
import java.util.List;

import ru.salauyou.slideshowswipe.SlideShowSwipe;
import android.graphics.Bitmap;
import android.util.Log;

/** 
 * ImageContainer class provides access to Panoramio photos that were taken near current location.
 */

public class ImageContainer implements GeolocService.Listener, ImageListUpdater.Receiver, SlideShowSwipe.BitmapContainer{

	private List<Image> images;
	private int qtyNeeded;		// quantity of images needed for the slideshow
	private int idUpdater;			// current callback id of ImageListUpdater
	private double _longitude;	// current geolocation coordinates
	private double _latitude;
	
	private Image imageCurrent, imagePrec;
	private int indexCurrent, indexPrec;
	
	
	
	/**
	 * Default constructor
	 */
	public ImageContainer(){
		images = new ArrayList<Image>();
		Controller.getInstance().getGeolocService().addListener(this);
	}

	
		
	/**
	 * Get current image
	 * 
	 * @return
	 */
	public Image getImageCurrent(){
		Log.println(Log.DEBUG, "panoramiator", "Requested current image: " + indexPrec);
		if (imageCurrent == null){
			if (images != null && images.size() > 0){
				if (images.get(0).isReady()){
					imageCurrent = images.get(0);
					indexCurrent = 0;
					return imageCurrent;
				} else {
					images.get(0).startDownload();
					return null;
				}
			} else {
				return null;
			}
		} else {
			return imageCurrent;
		}
	}
	
	
	
	/**
	 * Set internal cursor position to start of the container
	 */
	public void reset(){
		indexCurrent = indexPrec = 0;
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
		for (Image image : images){
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
			if (qty > 0 && Controller.getInstance().getGeolocService().getLocationStatus()!= GeolocService.Status.DISABLED){
				qtyNeeded = qty;
				if (qty > images.size()){
					// if new quantity greater than existing, uploade new image list
					new ImageListUpdater().getImagesPanoramio(this, ++idUpdater, _longitude, _latitude, qtyNeeded);
				} else if (qty < images.size()){
					// if new quantity less then existing, just rearrange image list
					images = ImageListUpdater.getImagesNearestSorted(images, _longitude, _latitude, qtyNeeded);
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
	public void locationUpdate(double longitude, double latitude, GeolocService.Status provider){
		_longitude = longitude;	//update location
		_latitude = latitude;
		// create and start imageListUpdater
		if (provider != GeolocService.Status.DISABLED){
			new ImageListUpdater().getImagesPanoramio(this, ++idUpdater, _longitude, _latitude, qtyNeeded);
		} 
	}
	
	
	
	/**
	 * C_ImageListUpdater.Receiver interface implementation
	 */
	@Override 
	public void receiveImageList(List<Image> imagesReceived, int id){
		if (imagesReceived != null && id == idUpdater ){
			// create new image list
			List<Image> imagesNew = new ArrayList<Image>();
			boolean foundExisting;
			for (Image imageReceived : imagesReceived){    // iteration within received images
				foundExisting = false;
				for (Image imageExisting : images){        // iteration within existing images
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

	
	/**
	 * SlideShowSwipe.BitmapContainer implementation
	 */
	@Override
	public Bitmap getBitmapNext() {
		if (images.size() == 0){
			return null;
		}
		
		int imageFound = -1;
		
		// looking for next ready image in the list circularily
		for (int i = indexCurrent + 1; imageFound < 0 && i != indexCurrent; i++) {
			if (i >= images.size()){
				i = 0;
			}
			if (images.get(i).isReady()){
				imageFound = i;
			} else {
				images.get(i).startDownload();
			}
		}
		
		// if ready image was found, return it and move cursor to it
		if (imageFound >= 0){
			Log.println(Log.DEBUG, "debug", "Requested next image: " + imageFound);
			indexPrec = indexCurrent;
			indexCurrent = imageFound;
			imagePrec = imageCurrent;
			imageCurrent = images.get(imageFound);
			return images.get(imageFound).getBitmap();
		} else {
			// else, set cursor to the start position
			Log.println(Log.DEBUG, "debug", "Requested next image: not found");
			indexPrec = 0;
			return null;
		}
	}

	
	
	@Override
	public Bitmap getBitmapPrevious() {
		if (images.size() == 0){
			return null;
		}
		
		int imageFound = -1;
		
		for (int i = indexCurrent - 1; imageFound < 0 && i != indexCurrent; i--) {
			if (i < 0){
				i = images.size() - 1;
			}
			if (images.get(i).isReady()){
				imageFound = i;
			} else {
				images.get(i).startDownload();
			}
		}
		if (imageFound >= 0){
			Log.println(Log.DEBUG, "debug", "Requested previous image: " + imageFound);
			indexPrec = indexCurrent;
			indexCurrent = imageFound;
			imagePrec = imageCurrent;
			imageCurrent = images.get(imageFound);
			return images.get(imageFound).getBitmap();
		} else {
			// else, set cursor to the start position
			Log.println(Log.DEBUG, "debug", "Requested previous image: not found");
			indexPrec = 0;
			return null;
		}
	}

	
	
	@Override
	public Bitmap getBitmapCurrent() {
		return getImageCurrent() == null ? null : getImageCurrent().getBitmap();
	}

	
	
	@Override
	public void undoGetBitmap() {
		imageCurrent = imagePrec;
		indexCurrent = indexPrec;
	}
}

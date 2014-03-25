package ru.salauyou.panoramiator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;
import android.location.LocationManager;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * ImageListUpdater discovers Panoramio pictures around specified location.
 * Via I_ImageListReceiver, it returns specified number of pictures nearest to given location,
 * sorted by date they were uploaded onto Panoramio (newest first).
 */

public class C_ImageListUpdater {

	// making Panoramio search variables static to make them accessible across instances
	private static volatile double _latitudeDelta = 0.01; // ~ 1 km
	private final static double DELTA_MIN = 0.0001; // ~ 10 meters 
	private static volatile double _deltaL = DELTA_MIN;
	private static volatile double _deltaR = _latitudeDelta;
	
	// property to store current 'id' for getImagesPanoramio method
	// we'll use it when server is requested, to check whether last called 'id' is equal to current
	private static volatile int idGetImagesPanoramio; 

	ArrayList<C_Image> images = new ArrayList<C_Image>();
	
	/* Method to download and process image list from Panoramio.
	 * Runs in separate thread, returns result via I_ImageListReceiver.
	 */
	public void getImagesPanoramio(final I_ImageListReceiver receiver, final int id, final double longitude, final double latitude, final int qty){
				
		// set current 'id' to the last called to prevent needless IO methods calls
		idGetImagesPanoramio = id;
		
		// proposition: '2*qty' is enough to accurately get needed 'qty' of images around given location
		// if 'qty' is small, take fixed 'qtyToReceive' to proceed Panoramio search faster
		final int qtyToReceive = (qty <= 15) ? 30 : (qty * 2); 
							
		// create a handler to send image list when ready
		final Handler handlerGetImages = new Handler() {		
			@Override
			public void handleMessage(Message msg){
				if (images.size() > 0){
					receiver.receiveImageList(images, id);
				}
			}
		};
			
		// create a thread to perform download and processing
		Thread threadGetImages = new Thread() {
			
			/* method to return json from requested URI */
			JSONObject getJson(final String uri) throws Throwable {
				// check if method uses current 'id'
				if (id != idGetImagesPanoramio){
					// if not, skip processing and throw an exception to stop the whole thread
					Log.println(Log.DEBUG, "panoramiator", "Panoramio request skipped due to unproper ID");
					throw new Throwable();
				}
				Log.println(Log.DEBUG, "panoramiator", "Panoramio request executed");
				JSONObject response = null;
				AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Android");
				try {
					HttpResponse httpResponse = httpClient.execute(new HttpGet(uri));
					// if everything is OK with response
					if (httpResponse.getStatusLine().getStatusCode() == 200){
						HttpEntity httpEntity = httpResponse.getEntity();
						// create response as JSON returned from Panoramio
						response = new JSONObject(EntityUtils.toString(httpEntity));
					}
					httpClient.close();
				} catch (Throwable e){
					httpClient.close();
					throw e;
				} 
				if (response == null){
					throw new Throwable();
				} else {
					return response;
				}
			}
			
			
			/* method to return if there are more than 'qtyRequired' images in Panoramio around current location */
			boolean panoramioHasMore(final double _delta) throws Throwable {
				try {
					// get delta for longitude to be similar in meters to delta for latitude
					double _deltaLongitude = getLongitudeDelta(latitude, _delta);
					String httpGetUri = "http://www.panoramio.com/map/get_panoramas.php?set=full"
							+ "&from=" + String.valueOf(qtyToReceive)
							+ "&to=" + String.valueOf(qtyToReceive) 
							+ "&minx=" + String.valueOf(longitude - _deltaLongitude) 
							+ "&miny=" + String.valueOf(latitude - _delta)
							+ "&maxx=" + String.valueOf(longitude + _deltaLongitude)
							+ "&maxy=" + String.valueOf(latitude + _delta)
							+ "&size=medium&mapfilter=false";
					// receive json from panoramio
					JSONObject response = getJson(httpGetUri);
					if (response.has("has_more")){
						if (response.get("has_more").toString().equals("true")){
							return true;
						} else {
							return false;
						}
					} else {
						throw new Exception();
					}
				} catch (Throwable e) {
					throw e;
				}
			};
			
			/* method to get images from Panoramio */
			void panoramioGetImages(final double _delta) throws Throwable{
				// get delta for longitude to be similar in meters to delta for latitude
				double _deltaLongitude = getLongitudeDelta(latitude, _delta);
				String httpGetUri = "http://www.panoramio.com/map/get_panoramas.php?set=full"
						+ "&from=0&to=" + String.valueOf(qtyToReceive) 
						+ "&minx=" + String.valueOf(longitude - _deltaLongitude) 
						+ "&miny=" + String.valueOf(latitude - _delta)
						+ "&maxx=" + String.valueOf(longitude + _deltaLongitude)
						+ "&maxy=" + String.valueOf(latitude + _delta)
						+ "&size=medium&mapfilter=false";
				try {
					Log.println(Log.DEBUG, "panoramiator", httpGetUri);
					// receive json
					JSONObject response = getJson(httpGetUri);
					if (response.has("photos")){
						JSONArray imagesJson = response.getJSONArray("photos");
						// there is no built-in iterator in JSON Array, so iterate on it using 'index'
						int imagesQty = imagesJson.length();
						for (int index = 0; index < imagesQty; index++){
							// extract next image object...
							JSONObject imageJson = (JSONObject)imagesJson.get(index);
							// ... and add it into 'images' list
							images.add(new C_Image(new SimpleDateFormat("dd MMMM yyyy", Locale.US).parse(imageJson.getString("upload_date")),
									imageJson.getString("photo_file_url"), imageJson.getString("photo_url"),
									imageJson.getString("owner_name"), imageJson.getString("photo_title"),
									imageJson.getDouble("longitude"), imageJson.getDouble("latitude")));
						}
					} else {
						throw new Exception();
					}
				} catch (Throwable e){
					throw e;
				}
			
			}
			
			@Override
			public void run(){
				// copy static deltas into local fields
				double delta = _latitudeDelta;
				double deltaR = _deltaR;
				double deltaL = _deltaL;
				
				try {	
					// start binary search of 'delta' that will give ~'qtyRequired' images on Panoramio
					// adjust left delta
					while (panoramioHasMore(deltaL)){
						deltaL *= 0.5;
					}
					// adjust right delta
					while (!panoramioHasMore(deltaR)){
						deltaR *= 2.0;
					}
					
					
					// proceed binary search
					delta = (deltaR - deltaL)/2.0;
					while ((deltaR - deltaL)>= DELTA_MIN){
						if (panoramioHasMore(delta)){
							deltaR = delta;
						} else {
							deltaL = delta;
						}
						delta = (deltaR + deltaL)/2.0;
					}
					
					
					/*
					// proceed golden section search
					if ((deltaR - deltaL)>=DELTA_MIN){
						double delta1 = (1-0.618)*(deltaR - deltaL) + deltaL;
						double delta2 = 0.618*(deltaR - deltaL) + deltaL;
						boolean hm1 = panoramioHasMore(delta1);
						boolean hm2 = panoramioHasMore(delta2);
						while ((deltaR - deltaL)>= DELTA_MIN){
							if(!hm1 && hm2){
								deltaL = delta1;
								deltaR = delta2;
								delta1 = (1-0.618)*(deltaR - deltaL) + deltaL;
								delta2 = 0.618*(deltaR - deltaL) + deltaL;
								if ((deltaR - deltaL)>=DELTA_MIN){
									hm1 = panoramioHasMore(delta1);
									hm2 = panoramioHasMore(delta2);
								}
							} else if (!hm1 && !hm2) {
								deltaL = delta1;
								delta1 = delta2;
								delta2 = 0.618*(deltaR - deltaL) + deltaL;
								if ((deltaR - deltaL)>=DELTA_MIN){
									hm2 = panoramioHasMore(delta2);
								}
							} else if (hm1 && hm2){
								deltaR = delta2;
								delta2 = delta1;
								delta1 = (1-0.618)*(deltaR - deltaL) + deltaL;
								if ((deltaR - deltaL)>=DELTA_MIN){
									hm1 = panoramioHasMore(delta1);
								}
							} else {
								throw new Exception();
							}
						}
					}
					*/
					
					// set static delta fields for further use
					delta = deltaR;
					_latitudeDelta = delta;
					_deltaR = deltaR;
					_deltaL = deltaL;
					
					//  get images list from Panoramio
					panoramioGetImages(delta);
					// and sort them
					images = C_ImageListUpdater.getImagesNearestSorted(images, longitude, latitude, qty);
					
				} catch (Throwable e) {	
					images.clear();
				}
				finally {
					handlerGetImages.sendMessage(new Message());
				}
			}
		};
		
		// start the thread	
		threadGetImages.start();
	}
	
	
	/* method to return needed quantity of images, nearest to given location and sorted by upload date, from the given list */
	static public ArrayList<C_Image> getImagesNearestSorted(ArrayList<C_Image> imagesInput, final double longitude, final double latitude, final int qty){
		
		// custom comparator for upload date sorting
		Comparator<C_Image> comparatorDate = new Comparator<C_Image>(){
			@Override
			public int compare(C_Image image1, C_Image image2){
				if (image1.getDate().equals(image2.getDate())){
					return 0;
				} else { 
					return (image1.getDate().before(image2.getDate())) ? 1 : -1; 
				}
			}
		};
	
		// custom comparator for distance sorting
		Comparator<C_Image> comparatorDistance = new Comparator<C_Image>(){
			@Override
			public int compare(C_Image image1, C_Image image2){
				Location location1 = new Location(LocationManager.GPS_PROVIDER);
				Location location2 = new Location(LocationManager.GPS_PROVIDER);
				Location location = new Location(LocationManager.GPS_PROVIDER);
				location1.setLatitude(image1.getLatitude());
				location1.setLongitude(image1.getLongitude());
				location2.setLatitude(image2.getLatitude());
				location2.setLongitude(image2.getLongitude());
				location.setLatitude(latitude);
				location.setLongitude(longitude);
				// calculate difference of distances between current location and image locations
				double difference = location.distanceTo(location1) - location.distanceTo(location2);
				return difference == 0 ? 0 : (difference > 0 ? 1 : -1); 
			}
		};
		
		// if received list bigger than needed, sort images by location and get first 'qty' images
		if (imagesInput.size() > qty){
			Collections.sort(imagesInput, comparatorDistance);
			imagesInput.subList(qty, imagesInput.size()).clear();
		}
		// then sort images by upload date
		Collections.sort(imagesInput, comparatorDate);
		return imagesInput;
	}
	
	/* return latitude delta that is appx the same in meters as given longitude delta */
	static private double getLongitudeDelta(final double latitude, final double latDelta){
		return latDelta / Math.cos(latitude/180.0 * Math.PI);
	}
}

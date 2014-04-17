package ru.salauyou.panoramiator;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Type class to store image info and bitmap. When created by constructor, it doesn't start
 * downloading bitmap automatically. Use {@code startDownload()} to obtain bitmap; {@code isReady()} to check
 * whether bitmap is ready.
 */
public class Image {
	
	static private ExecutorService downloader;
	static private final int MAX_DOWNLOAD_THREADS = 5;
	
	final private Date date;
	final private String url;
	final private double longitude;
	final private double latitude;
	final private String link;
	final private String author;
	final private String title;
	
	private volatile Bitmap bitmap;
	private volatile boolean bitmapReady = false;
	private volatile boolean bitmapRequested = false;

	
	/**
	 * Public constructor
	 * 
	 * @param date
	 * @param url		URL of image that will be used to download bitmap
	 * @param link		link to web page describing the image
	 * @param author
	 * @param title
	 * @param longitude
	 * @param latitude
	 */
	public Image(Date date, String url, String link, String author, String title, double longitude, double latitude){
		this.date = date;
		this.url = url;
		this.longitude = longitude;
		this.latitude = latitude;
		this.link = link;
		this.author = author;
		this.title = title;
	}
	
	/**
	 * Get bitmap of the image
	 * 
	 * @return	bitmap, or null if it not yet downloaded
	 */
	public Bitmap getBitmap(){
		return bitmapReady ? bitmap : null;
	}
	
	/**
	 * Get date of the image
	 * 
	 * @return
	 */
	public Date getDate(){
		return date;
	}
	
	/**
	 * Get URL of the image
	 * 
	 * @return
	 */
	public String getUrl(){
		return url;
	}
	
	/**
	 * Get author of the image
	 * 
	 * @return
	 */
	public String getAuthor(){
		return author;
	}
	
	/**
	 * Get link to web page describing the image
	 * 
	 * @return
	 */
	public String getLink(){
		return link;
	}
	
	/**
	 * Get title of the image
	 * 
	 * @return
	 */
	public String getTitle(){
		return title;
	}
	
	/**
	 * Get bitmap status of the image
	 * 
	 * @return	true, if bitmap is downloaded yet, false otherwise
	 */
	public boolean isReady(){
		return bitmapReady ? true : false;
	}
	
	/**
	 * Get longitude of geopoint image was taken
	 * 
	 * @return
	 */
	public double getLongitude(){
		return longitude;
	}
	
	/** 
	 * Get latitude of geopoint image was taken
	 * 
	 * @return
	 */
	public double getLatitude(){
		return latitude;
	}
	
	/**
	 * Start download of bitmap 
	 */
	public void startDownload(){
		if (!bitmapRequested){
			if (downloader == null || downloader.isShutdown()){
				downloader = Executors.newFixedThreadPool(MAX_DOWNLOAD_THREADS);
			} 
			downloader.execute(new Runnable(){
				@Override
				public void run(){
					try {
						URL urlConnection = new URL(url);
						bitmap = BitmapFactory.decodeStream(urlConnection.openConnection().getInputStream());
						if (bitmap != null){  
							bitmapReady = true;
						} else {
							bitmapRequested = false;
						}
					} catch (IOException e) { 	
						Log.d("debug", "Image by url:" + url + " cannot be downloaded");
						bitmap = null;
						bitmapRequested = false;			
					} 
				}
			});
			bitmapRequested = true;
		}
	}
}

package ru.salauyou.panoramiator;

import java.util.Date;

import android.graphics.Bitmap;

public class C_Image implements I_BitmapReceiver {
	private Bitmap _bitmap;
	final private Date _date;
	final private String _url;
	final private double _longitude;
	final private double _latitude;
	final private String _link;
	final private String _author;
	final private String _title;
	private boolean bitmapRequested = false;

	
	public C_Image(Date date, String url, String link, String author, String title, double longitude, double latitude){
		_date = date;
		_url = url;
		_longitude = longitude;
		_latitude = latitude;
		_link = link;
		_author = author;
		_title = title;
	}
	
	public Bitmap getBitmap(){
		return _bitmap == null ? null : _bitmap;
	}
	
	public Date getDate(){
		return _date;
	}
	
	public String getUrl(){
		return _url;
	}
	
	public String getAuthor(){
		return _author;
	}
	
	public String getLink(){
		return _link;
	}
	
	public String getTitle(){
		return _title;
	}
	
	public boolean isReady(){
		return _bitmap == null ? false : true;
	}
	
	public double getLongitude(){
		return _longitude;
	}
	
	public double getLatitude(){
		return _latitude;
	}
	
	public void startDownload(){
		if (!bitmapRequested){
			new C_ImageDownloader(this, _url);
			bitmapRequested = true;
		}
	}
	
	/* =========== method(s) to implement I_BitmapReceiver ================ */
	@Override
	public void receiveBitmap(Bitmap bitmap){
		try {
			if (bitmap != null){
				_bitmap = bitmap;
			} else {
				bitmapRequested = false;
			}
		} catch (Throwable e) { 
			_bitmap = null;
			bitmapRequested = false;
		}
	}

}

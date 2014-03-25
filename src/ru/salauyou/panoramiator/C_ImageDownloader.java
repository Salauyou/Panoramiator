package ru.salauyou.panoramiator;

import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class C_ImageDownloader {

	volatile private Bitmap bitmap;
	private I_BitmapReceiver _receiver;
	private String _url;
		
	// handler to send bitmap
	final private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			try {
				_receiver.receiveBitmap(bitmap);
			} catch (Throwable e) { }
		}
	};
	
	// thread to download bitmap
	private Thread threadImageDownloader = new Thread(){
		@Override
		public void run(){
			try {
				Log.println(Log.DEBUG, "panoramiator", "Image request: "+ _url);
				URL urlConnection = new URL(_url);
				bitmap = BitmapFactory.decodeStream(urlConnection.openConnection().getInputStream());
				
			} catch (Throwable e) { 
				bitmap = null;
			} 
			handler.sendMessage(Message.obtain());
		}
	};
	
	// constructor
	public C_ImageDownloader(I_BitmapReceiver receiver, String url){
		_receiver = receiver;
		_url = url;
		threadImageDownloader.start();
	}
}

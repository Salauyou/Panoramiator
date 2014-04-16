package ru.salauyou.panoramiator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

public class ActivitySlideShow extends Activity {

	ImageSwitcher imageSwitcherSlideShow;
	private int imageNumber = 0;
	SchedulerSlideShow schedulerSlideShow;
	ProgressBar progressBar;
	View layoutInfo;
	Button buttonInfo;
	Animation animLayoutInfoIn, animLayoutInfoOut, animButtonInfoIn, animButtonInfoOut;
	boolean viewInfoOpened = false;
	ImageView imageButtonInfo;
	TextView textViewInfoAuthor, textViewInfoDate, textViewInfoLink, textViewStatus, textViewInfoHeader;
	View decorView;
	ViewGroup layoutBackground;
	int slideShowPeriod = 1500;
	boolean slideShowStarted = false;
	Image image;
	
	// ----- handler to catch slide show scheduler messages ----- //
	final Handler handlerSlideShow = new Handler(){
		@Override
		public void handleMessage(Message msg){	
			// show next image
			slideShowNext();
		}
	};
	
	// ----- thread class to schedule the slide show -------- //
	class SchedulerSlideShow extends Thread {
		
		static public final int SLIDESHOW_NEXT = 0;
		static public final int SLIDESHOW_PAUSE = 1;
		private volatile boolean paused = true;
		private final Object pauseObject = new Object();
		
		
		@Override
		public void run(){
			try {
				while(true){
					Thread.sleep(slideShowPeriod);
					while (paused) {
						synchronized(pauseObject) {
							pauseObject.wait();
						}
					}
					handlerSlideShow.sendMessage(Message.obtain());
				}
			} catch (InterruptedException e) { }
		}
		
		// pause method
		public void pause(){
			paused = true;
		}
		
		// resume method
		public void unPause(){
			paused = false;
			synchronized(pauseObject) {
				pauseObject.notify();
			}
		}
		
		// return if the scheduler is paused
		public boolean isPaused(){
			return paused;
		}
	};
	
	
	// ----- onCreate -----------//
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_slideshow);
		
		// get all needed views
		layoutBackground = (ViewGroup)findViewById(R.id.layoutBackground);
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		layoutInfo = (View)findViewById(R.id.layoutInfo);
		buttonInfo = (Button)findViewById(R.id.buttonInfo);
		imageButtonInfo = (ImageView)findViewById(R.id.imageButtonInfo);
		textViewInfoHeader = (TextView)findViewById(R.id.layoutInfoHeader);
		textViewInfoAuthor = (TextView)findViewById(R.id.layoutInfoAuthor);
		textViewInfoDate = (TextView)findViewById(R.id.layoutInfoDate);
		textViewInfoLink = (TextView)findViewById(R.id.layoutInfoLink);
		textViewStatus = (TextView)findViewById(R.id.textViewStatus);
		imageSwitcherSlideShow = (ImageSwitcher)findViewById(R.id.imageSwitcherSlideShow);	
		decorView = getWindow().getDecorView();
		
		// set up image factory to slide show view
		imageSwitcherSlideShow.setFactory(new ViewFactory(){
			@Override
			public View makeView(){
				ImageView imageView = new ImageView(getApplicationContext());
				imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
				imageView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				return imageView;
			}
		}); 
		
		// load animators
		animLayoutInfoIn = AnimationUtils.loadAnimation(this, R.anim.view_info_in);
		animLayoutInfoOut = AnimationUtils.loadAnimation(this, R.anim.view_info_out);
		animButtonInfoIn = AnimationUtils.loadAnimation(this, R.anim.button_info_in);
		animButtonInfoOut = AnimationUtils.loadAnimation(this, R.anim.button_info_out);
		
		// set listener to info view out animation
		animLayoutInfoOut.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation arg0) {
				layoutInfo.setVisibility(View.INVISIBLE);
			}
			@Override
			public void onAnimationRepeat(Animation arg0) { }

			@Override
			public void onAnimationStart(Animation arg0) { 
				layoutInfo.setVisibility(View.INVISIBLE);
			}
		});
		
		
		// set up animators
		Animation animationSlideShowIn = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in);
		Animation animationSlideShowOut = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_out);
		imageSwitcherSlideShow.setInAnimation(animationSlideShowIn);
		imageSwitcherSlideShow.setOutAnimation(animationSlideShowOut);
		
		// proceed intent that started activity
		Intent intent = getIntent();
		if (intent != null && intent.hasExtra("slideShowPeriod")){
			slideShowPeriod = intent.getIntExtra("slideShowPeriod", slideShowPeriod);
		}
		
		// restore info view state
		// if slide show period was saved on bundle, restore it
		if (savedInstanceState != null && savedInstanceState.containsKey("slideShowPeriod")){
			slideShowPeriod = savedInstanceState.getInt("slideShowPeriod");
		}
		
		// discover if slide show was started
		if (Controller.getInstance().getImageContainer().getCurrent() != null){
			slideShowStarted = true;
		}
		
		// if info view was visible, make info button rotated and put text into text views
		if (savedInstanceState != null && savedInstanceState.containsKey("viewInfoOpened") && savedInstanceState.getBoolean("viewInfoOpened")){
			imageButtonInfo.startAnimation(animButtonInfoIn);
			viewInfoOpened = true;
			fillViewInfo();
		} else {
			// if info view was not visible and slide show was not paused, hide info button 
			if (savedInstanceState == null || !savedInstanceState.containsKey("schedulerSlideShowPaused") || !savedInstanceState.getBoolean("schedulerSlideShowPaused")){
				buttonInfo.setVisibility(View.INVISIBLE);
				imageButtonInfo.setVisibility(View.INVISIBLE);
			}
			// hide info view
			layoutInfo.setVisibility(View.INVISIBLE);
		}
		
		// put current image onto slideshow
		if (savedInstanceState != null && savedInstanceState.containsKey("imageNumber")){
			imageNumber = savedInstanceState.getInt("imageNumber");
			if (Controller.getInstance().getImageContainer().getCurrent()!= null){
				imageSwitcherSlideShow.setImageDrawable(new BitmapDrawable(getResources(), Controller.getInstance().getImageContainer().getCurrent().getBitmap()));
				progressBar.setVisibility(View.INVISIBLE);
				textViewStatus.setVisibility(View.INVISIBLE);
			} 
		}
		
		// create and start scheduler thread
		schedulerSlideShow = new SchedulerSlideShow();
		schedulerSlideShow.start();
		
		// run scheduler if it was unpaused
		if (savedInstanceState == null || !savedInstanceState.containsKey("schedulerSlideShowPaused") 
				|| !savedInstanceState.getBoolean("schedulerSlideShowPaused") || !slideShowStarted){
			slideShowToggle(imageSwitcherSlideShow);
		}
		
		// log
		Log.println(Log.DEBUG, "panoramiator", "Slide Show Activity created");
	}
	
	//-------- pause slide show activity -----------//
	protected void onPause(){
		if (!schedulerSlideShow.isPaused()){
			if (slideShowStarted){
				slideShowToggle(imageSwitcherSlideShow);
			} else {
				schedulerSlideShow.pause();
			}
		}
		super.onPause();
	}
	
	//--------- save slide show state on activity pause ------------//
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  super.onSaveInstanceState(savedInstanceState);
	  savedInstanceState.putBoolean("schedulerSlideShowPaused", schedulerSlideShow.isPaused());
	  savedInstanceState.putInt("imageNumber", imageNumber);
	  savedInstanceState.putBoolean("viewInfoOpened", viewInfoOpened);
	  savedInstanceState.putInt("slideShowPeriod", slideShowPeriod);
	}
	
	//------- touching the slide show view toggles slide show -------- //
	public void slideShowToggle(View view){
		if (!schedulerSlideShow.isPaused() && slideShowStarted){
			// if not paused, do pause, except if slide show not started yet
			schedulerSlideShow.pause();
			// show info button
			buttonInfo.setVisibility(View.VISIBLE);
			imageButtonInfo.setVisibility(View.VISIBLE);
			// undim status and navigation bar
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			// cancel going sleep prevention
			imageSwitcherSlideShow.setKeepScreenOn(false);
		} else if (schedulerSlideShow.isPaused() || !slideShowStarted) {
			// if paused, do resume
			if (viewInfoOpened){
				// animatedly close info view
				viewInfoToggle(view);
			}
			schedulerSlideShow.unPause();
			// hide info button
			buttonInfo.setVisibility(View.INVISIBLE);
			imageButtonInfo.setVisibility(View.INVISIBLE);
			// hide layout info
			layoutInfo.setVisibility(View.INVISIBLE);
			// dim status bar and navigation bar
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
			// prevent from going sleep
			imageSwitcherSlideShow.setKeepScreenOn(true);
		}
	}
	
	

	// ----------- next slide -------//
	public void slideShowNext(){
		image = Controller.getInstance().getImageContainer().getNext();
		if (image != null) {
			// mark that slide show started
			slideShowStarted = true;
			imageSwitcherSlideShow.setImageDrawable(new BitmapDrawable(getResources(), image.getBitmap()));
		
			// hide progress bar
			if (progressBar.getVisibility() == View.VISIBLE){
				progressBar.setVisibility(View.INVISIBLE);
				textViewStatus.setVisibility(View.INVISIBLE);
			}
			// dim status bar and navigation bar
			// this is duplicated to proceed after device awake
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		}
	}
	
	
	// ---------- info button click ------------- //
	public void viewInfoToggle(View view){
		if (!viewInfoOpened){
			layoutInfo.startAnimation(animLayoutInfoIn);
			imageButtonInfo.startAnimation(animButtonInfoIn);
			layoutInfo.setVisibility(View.VISIBLE);
			viewInfoOpened = true;
			fillViewInfo();
		} else {
			layoutInfo.startAnimation(animLayoutInfoOut);
			imageButtonInfo.startAnimation(animButtonInfoOut);
			viewInfoOpened = false;
		}
	}
	
	// ------------ info view click ------------//
	public void viewInfoClick(View view){
		// empty
		// just to not let click on info view run the slideshow
	}
	
	// ---------- filling info layout with info about current image -------//
	public void fillViewInfo(){
		Image imageCurrent = Controller.getInstance().getImageContainer().getCurrent();
		if (imageCurrent != null){
			if (imageCurrent.getTitle().equals("")){
				textViewInfoHeader.setVisibility(View.GONE);
			} else {
				textViewInfoHeader.setVisibility(View.VISIBLE);
				textViewInfoHeader.setText(imageCurrent.getTitle());
			}
			textViewInfoAuthor.setText(imageCurrent.getAuthor());
			textViewInfoDate.setText(DateFormat.format("d MMMM yyyy", imageCurrent.getDate()));
			textViewInfoLink.setText(imageCurrent.getLink());
		}	
	}
		

}

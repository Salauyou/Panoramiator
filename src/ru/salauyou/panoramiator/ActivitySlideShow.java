package ru.salauyou.panoramiator;

import ru.salauyou.slideshowswipe.SlideShowSwipe;
import ru.salauyou.slideshowswipe.SlideShowSwipe.State;
import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ActivitySlideShow extends Activity implements SlideShowSwipe.OnStateChangeListener {

	SlideShowSwipe slideShow;
	
	ProgressBar progressBar;
	View layoutInfo;
	Button buttonInfo;
	Animation animLayoutInfoIn, animLayoutInfoOut, animButtonInfoIn, animButtonInfoOut, animButtonInfoOutAndDisappear;
	ImageView imageButtonInfo;
	TextView textViewInfoAuthor, textViewInfoDate, textViewInfoLink, textViewStatus, textViewInfoHeader;
	View decorView;
	
	int slideShowPeriod = 1500;

	ActivitySlideShow self = this;
	

	/**
	 * {@code onCreate()} override
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_slideshow);
		
	
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		layoutInfo = (View)findViewById(R.id.layoutInfo);
		buttonInfo = (Button)findViewById(R.id.buttonInfo);
		imageButtonInfo = (ImageView)findViewById(R.id.imageButtonInfo);
		textViewInfoHeader = (TextView)findViewById(R.id.layoutInfoHeader);
		textViewInfoAuthor = (TextView)findViewById(R.id.layoutInfoAuthor);
		textViewInfoDate = (TextView)findViewById(R.id.layoutInfoDate);
		textViewInfoLink = (TextView)findViewById(R.id.layoutInfoLink);
		textViewStatus = (TextView)findViewById(R.id.textViewStatus);
		slideShow = (SlideShowSwipe)findViewById(R.id.slideShow);	
		decorView = getWindow().getDecorView();

		
		animLayoutInfoIn = AnimationUtils.loadAnimation(this, R.anim.view_info_in);
		animLayoutInfoOut = AnimationUtils.loadAnimation(this, R.anim.view_info_out);
		animButtonInfoIn = AnimationUtils.loadAnimation(this, R.anim.button_info_in);
		animButtonInfoOut = AnimationUtils.loadAnimation(this, R.anim.button_info_out);
		animButtonInfoOutAndDisappear = AnimationUtils.loadAnimation(this, R.anim.button_info_out);
		

		animLayoutInfoOut.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation arg0) {
				layoutInfo.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationRepeat(Animation arg0) { }

			@Override
			public void onAnimationStart(Animation arg0) { }
		});

		
		animLayoutInfoIn.setAnimationListener(new AnimationListener(){
			@Override
			public void onAnimationEnd(Animation arg0) { }
			
			@Override
			public void onAnimationRepeat(Animation arg0) { }

			@Override
			public void onAnimationStart(Animation arg0) { 
				decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
				slideShow.setKeepScreenOn(false);
				layoutInfo.setVisibility(View.VISIBLE);
				fillViewInfo();
			}
		});
		
		animButtonInfoOutAndDisappear.setAnimationListener(new AnimationListener(){
			@Override
			public void onAnimationEnd(Animation arg0) {
				buttonInfo.setVisibility(View.GONE);
				imageButtonInfo.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation arg0) { }

			@Override
			public void onAnimationStart(Animation arg0) { }
			
		});
		
		
		buttonInfo.setVisibility(View.GONE);
		imageButtonInfo.setVisibility(View.GONE);
		layoutInfo.setVisibility(View.GONE);
		
		
		
		if (getIntent() != null && getIntent().hasExtra("slideShowPeriod"))
				slideShowPeriod = getIntent().getIntExtra("slideShowPeriod", slideShowPeriod);

		
		// restore values from saved instance state
		if (savedInstanceState != null){
			if (savedInstanceState.containsKey("slideShowPeriod"))
				slideShowPeriod = savedInstanceState.getInt("slideShowPeriod");
				
			if (savedInstanceState.containsKey("slideShowPaused") && savedInstanceState.getBoolean("slideShowPaused")){
				buttonInfo.setVisibility(View.VISIBLE);
				imageButtonInfo.setVisibility(View.VISIBLE);
			}
			
			if (savedInstanceState.containsKey("viewInfoOpened") && savedInstanceState.getBoolean("viewInfoOpened")){
				layoutInfo.setVisibility(View.VISIBLE);
				imageButtonInfo.startAnimation(animButtonInfoIn);
				fillViewInfo();
			}
		}

		// attach bitmap container and start slide show demonstration
		slideShow.setBitmapContainer(Controller.getInstance().getImageContainer())
			.setSlideShowPeriod(slideShowPeriod)
			.setSlideShowTransition(500)
			.setOnStateChangeListener(this);
			
		// check if slide show is already started
		if (Controller.getInstance().getImageContainer().getBitmapCurrent() != null 
				&& savedInstanceState != null && savedInstanceState.containsKey("slideShowPaused")){
			slideShow.restoreCurrent(savedInstanceState.getBoolean("slideShowPaused"));
			progressBar.setVisibility(View.GONE);
			textViewStatus.setVisibility(View.GONE);
		} else
			slideShow.startSlideShow();	
	
				
		// log
		Log.println(Log.DEBUG, "panoramiator", "Slide Show Activity created");
	}
	
	
	
	//--------- save slide show state on activity pause ------------//
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean("viewInfoOpened", layoutInfo.getVisibility() == View.VISIBLE ? true : false);
		savedInstanceState.putInt("slideShowPeriod", slideShowPeriod);
		savedInstanceState.putBoolean("slideShowPaused", slideShow.getState() == State.SLIDESHOW_PAUSED ? true : false);
	}
	

	
	// ---------- info button click ------------- //
	public void viewInfoToggle(View view){
		if (layoutInfo.getVisibility() != View.VISIBLE){
			layoutInfo.startAnimation(animLayoutInfoIn);
			imageButtonInfo.startAnimation(animButtonInfoIn);
			layoutInfo.setVisibility(View.VISIBLE);
			fillViewInfo();
		} else {
			layoutInfo.startAnimation(animLayoutInfoOut);
			imageButtonInfo.startAnimation(animButtonInfoOut);
		}
	}
	
	// ------------ info view click ------------//
	public void viewInfoClick(View view){
		// empty
		// just to not let click on info view run the slideshow
	}
	
	// ---------- filling info layout with info about current image -------//
	public void fillViewInfo(){
		Image imageCurrent = Controller.getInstance().getImageContainer().getImageCurrent();
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


	/* ============ SlideShowSwipe.OnStateChangeListener implementation  =========== */
	
	@Override
	public void onStateChange(State state) {
		if (state == State.SLIDESHOW_STARTED){
			if (layoutInfo.getVisibility() == View.VISIBLE){
				layoutInfo.startAnimation(animLayoutInfoOut);
				imageButtonInfo.startAnimation(animButtonInfoOutAndDisappear);
			} else {
				imageButtonInfo.setVisibility(View.GONE);
				buttonInfo.setVisibility(View.GONE);
			}
			slideShow.setKeepScreenOn(true);
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		} else if (state == State.SLIDESHOW_PAUSED){
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			slideShow.setKeepScreenOn(false);
			buttonInfo.setVisibility(View.VISIBLE);
			imageButtonInfo.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onCurrentBitmapChange() {
		if (layoutInfo.getVisibility() == View.VISIBLE)
			fillViewInfo();
		if (progressBar.getVisibility() == View.VISIBLE){
			progressBar.setVisibility(View.GONE);
			textViewStatus.setVisibility(View.GONE);
		}
	}
		

}

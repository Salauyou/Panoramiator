package ru.salauyou.panoramiator;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.NumberPicker;

public class ActivityStart extends Activity implements I_GeolocListener {

	
	NumberPicker numberPickerQty, numberPickerPeriod;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// check if there is enough space for UI
		DisplayMetrics displayMetrics = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		// if not, rotate screen back to portrait
		if (displayMetrics.heightPixels / displayMetrics.density < 420){
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		// create layout
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		// define number pickers
		numberPickerQty = (NumberPicker)findViewById(R.id.numberPickerQty);
		numberPickerQty.setMaxValue(50);
		numberPickerQty.setMinValue(2);
		if (savedInstanceState != null && savedInstanceState.containsKey("numberPickerQty")){
			numberPickerQty.setValue(savedInstanceState.getInt("numberPickerQty"));
		} else {
			numberPickerQty.setValue(10);
		}
		numberPickerPeriod = (NumberPicker)findViewById(R.id.numberPickerPeriod);
		numberPickerPeriod.setMinValue(5);
		numberPickerPeriod.setMaxValue(20);
		numberPickerPeriod.setDisplayedValues(new String[]{"0,5", "0,6", "0,7", "0,8", "0,9", "1", "1,1", "1,2", "1,3", "1,4", "1,5", "1,6", "1,7", "1,8", "1,9", "2"});
		if (savedInstanceState != null && savedInstanceState.containsKey("numberPickerPeriod")){
			numberPickerPeriod.setValue(savedInstanceState.getInt("numberPickerPeriod"));
		} else {
			numberPickerPeriod.setValue(10);
		}        
		
		// log
		Log.println(Log.DEBUG, "panoramiator", "Start Activity created");
		/*
		Controller.getInstance().getGeolocService().addListener(this);
		Controller.getInstance().getImageContainer().setQty(25);*/
	}


	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
	}
	
	//--------- save number pickers state on activity pause ------------//
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt("numberPickerQty", numberPickerQty.getValue());
		savedInstanceState.putInt("numberPickerPeriod", numberPickerPeriod.getValue());
	}
	
	
	//-------------- starting the slide show ------------------//
	public void slideShowStart(View view){
		Intent intentToSlideShow = new Intent(this, ActivitySlideShow.class);
		intentToSlideShow.putExtra("slideShowPeriod", numberPickerPeriod.getValue() * 100);
		Controller.getInstance().getImageContainer().reset();
		Controller.getInstance().getImageContainer().setQty(numberPickerQty.getValue());
		this.startActivity(intentToSlideShow);
	}
	
	//======== method(s) to implement I_GeolocListener ============
	@Override
	public void locationUpdate(double longitude, double latitude, int provider){
		// empty
	}

}

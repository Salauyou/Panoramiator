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

public class ActivityStart extends Activity {

	
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
		numberPickerPeriod.setMinValue(1);
		numberPickerPeriod.setMaxValue(10);
		numberPickerPeriod.setDisplayedValues(new String[]{"0,5", "1", "1,5", "2", "2,5", "3", "3,5", "4", "4,5", "5"});
		if (savedInstanceState != null && savedInstanceState.containsKey("numberPickerPeriod")){
			numberPickerPeriod.setValue(savedInstanceState.getInt("numberPickerPeriod"));
		} else {
			numberPickerPeriod.setValue(4);
		}        
		
		// log
		Log.println(Log.DEBUG, "panoramiator", "Start Activity created");
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
		intentToSlideShow.putExtra("slideShowPeriod", numberPickerPeriod.getValue() * 500);
		Controller.getInstance().getImageContainer().reset();
		Controller.getInstance().getImageContainer().setQty(numberPickerQty.getValue());
		this.startActivity(intentToSlideShow);
	}
	
}

package com.android.camera.ui;

import java.util.ArrayList;
import java.util.List;

import com.android.camera.CameraSettings;
import com.android.camera.ListPreference;
import com.android.camera2.R;

import android.content.Context;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class YixiRadioGroup extends RadioGroup implements YixiInLineSettingRadioButton.RadioListener{
	List<RadioButton> mRadioButtons;
	RadioButton mRadioButton;
	ListPreference normPreference;
	boolean isCheckedRadioIn = false;
	ListPreference mPreference;
	String strValueOn;
	String strValueOff;
	public YixiRadioGroup(Context context) {
		super(context);
		strValueOn = getResources().getString(R.string.setting_on_value);
		strValueOff = getResources().getString(R.string.setting_off_value);
		// TODO Auto-generated constructor stub
	}
	
	private void getRadioButtonsInstance(){
		if(mRadioButtons != null){
			//Log.i("lilei", "~~ getRadioButtonsInstance() 111");
		}else{
			mRadioButtons = new ArrayList<RadioButton>();
			for(int i=0;i<getChildCount();i++){
				YixiInLineSettingRadioButton radioLayout = (YixiInLineSettingRadioButton)getChildAt(i);
				mRadioButton = radioLayout.getRadioButton();
				if(mRadioButton != null){
					mRadioButtons.add(mRadioButton);
					mPreference = (ListPreference) mRadioButton.getTag();
					if(mPreference.getKey().equals(CameraSettings.KEY_CAMERA_NOMAL)){
						normPreference = mPreference;
					}
				}else{
					Log.i("lilei", "warn getRadios() i:"+i+" is null");
				}
			}
		}
	}
	
	private void checkRadio(String key){
		getRadioButtonsInstance();
		Log.i("lilei", "~~~ checkRadio() key:"+key+" mRadioButtons.size():"+(mRadioButtons == null?"null":" "+mRadioButtons.size()));
		for(int i = 0;i<mRadioButtons.size();i++){
			mRadioButton = mRadioButtons.get(i);
			mPreference = (ListPreference) mRadioButton.getTag();
			//Log.i("lilei", "~~~ checkRadio() 22 i:"+i+" mPreference:"+(mPreference==null?"null":mPreference.getKey()));
			if(mPreference != null){
				if(mPreference.getKey().equals(key)){
					mRadioButton.setChecked(true);
					//pano mode view do not have set more button
					if(!CameraSettings.KEY_CAMERA_PANORAMA.equals(key)){
						isCheckedRadioIn = true;
						mPreference.setValue(strValueOn);
					}else{
						mPreference.setValue(strValueOff);
					}
				}else{
					mRadioButton.setChecked(false);
					mPreference.setValue(strValueOff);
				}
			}
		}
		if(!isCheckedRadioIn){
			Log.i("~~~ lilei", "checkRadio() checked Radio is not In");
			normPreference.setValue(strValueOn);
		}
	}

	@Override
	public void onRadiochecked(String key) {
		// TODO Auto-generated method stub
		checkRadio(key);
	}

}

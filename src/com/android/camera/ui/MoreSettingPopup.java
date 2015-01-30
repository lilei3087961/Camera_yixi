/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.ui;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.android.camera.CameraActivity;
import com.android.camera.CameraSettings;
import com.android.camera.ListPreference;
import com.android.camera.PreferenceGroup;
import com.android.camera2.R;

/* A popup window that contains several camera settings. */
public class MoreSettingPopup extends AbstractSettingPopup
        implements InLineSettingItem.Listener,
        AdapterView.OnItemClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "MoreSettingPopup";

    private Listener mListener;
    private ArrayList<ListPreference> mListItem = new ArrayList<ListPreference>();
    private ArrayList<ListPreference> mListItemRadioGroup = new ArrayList<ListPreference>(); 
    // Keep track of which setting items are disabled
    // e.g. White balance will be disabled when scene mode is set to non-auto
    private boolean[] mEnabled;

    static public interface Listener {
        public void onSettingChanged(ListPreference pref);
        public void onPreferenceClicked(ListPreference pref);
    }

    private class MoreSettingAdapter extends ArrayAdapter<ListPreference> {
        LayoutInflater mInflater;
        String mOnString;
        String mOffString;
        MoreSettingAdapter() {
            super(MoreSettingPopup.this.getContext(), 0, mListItem);
            Context context = getContext();
            mInflater = LayoutInflater.from(context);
            mOnString = context.getString(R.string.setting_on);
            mOffString = context.getString(R.string.setting_off);
        }

        private int getSettingLayoutId(ListPreference pref) {

            if (isOnOffPreference(pref)) {
            	//modify by lilei begin
            	if(Arrays.asList(YixiSetMoreButton.radioGroup).contains(pref.getKey())){
            		if(CameraActivity.YIXI_LOG_ON)
            			Log.i("lilei", "key:"+pref.getKey());
            		return R.layout.yixi_in_line_setting_radio_button;
            	}
                //return R.layout.in_line_setting_check_box;
            	return R.layout.yixi_in_line_setting_switch;
                //modify by lilei end
            }
            return R.layout.in_line_setting_menu;
        }

        private boolean isOnOffPreference(ListPreference pref) {
            CharSequence[] entries = pref.getEntries();
            if (entries.length != 2) return false;
            String str1 = entries[0].toString();
            String str2 = entries[1].toString();
//            if(pref.getKey().equals(CameraSettings.KEY_CAMERA_NOMAL)){
//            	Log.i("lilei", "~~is key:"+pref.getKey() +" index:"+Arrays.binarySearch(YixiSetMoreButton.radioGroup, pref.getKey()));
//            }
//            if(Arrays.binarySearch(YixiSetMoreButton.radioGroup, pref.getKey())>0){
//            	Log.i("lilei", "~~is key:"+pref.getKey()+" str1:"+str1+" str2:"+str2+" mOnString:"
//            			+mOnString+" mOffString:"+mOffString);
//            }
            return ((str1.equals(mOnString) && str2.equals(mOffString)) ||
                    (str1.equals(mOffString) && str2.equals(mOnString)));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListPreference pref = mListItem.get(position);
            //add for mika by lilei begin
            if(convertView != null){
            	//Log.i("lilei", "##MoreSettingAdapter convertView != null pref.getKey():"+pref.getKey()+" convertView:"+convertView);
            	return convertView;
            }
            if(pref.getKey().equals(CameraSettings.KEY_CAMERA_NOMAL)){
            	YixiRadioGroup mRadioGroup = null;//(RadioGroup)convertView;
            	mRadioGroup = new YixiRadioGroup(getContext());
            	//Log.i("lilei", "00 mListItemRadioGroup.size() :"+mListItemRadioGroup.size());
            	for(int i=0;i<mListItemRadioGroup.size();i++){
            		ListPreference prefRadio = mListItemRadioGroup.get(i);
            		 int viewLayoutId = getSettingLayoutId(prefRadio);
            		 YixiInLineSettingRadioButton view = null;//(InLineSettingItem)convertView;
                     view = (YixiInLineSettingRadioButton)
                             mInflater.inflate(viewLayoutId, parent, false);

                     view.initialize(prefRadio); // no init for restore one
                     view.setSettingChangedListener(MoreSettingPopup.this);
                     view.setRadioListener(mRadioGroup);
                     if (position >= 0 && position < mEnabled.length) {
                         view.setEnabled(mEnabled[position]);
                     } else {
                         Log.w(TAG, " ~~~ Invalid input: enabled list length, " + mEnabled.length
                                 + " position " + position);
                     }
                     //Log.i("lilei", "11 i:"+i+" key:"+prefRadio.getKey()+" view:"+view);
                     mRadioGroup.addView(view);
            	}
            	convertView = mRadioGroup;
            	//convertView.setBackgroundColor(R.color.red);
            	return convertView;
            }else{
            //add for mika by lilei end
	            int viewLayoutId = getSettingLayoutId(pref);
	            InLineSettingItem view = null;//(InLineSettingItem)convertView;
	            view = (InLineSettingItem)
	                    mInflater.inflate(viewLayoutId, parent, false);
	
	            view.initialize(pref); // no init for restore one
	            view.setSettingChangedListener(MoreSettingPopup.this);
	            if (position >= 0 && position < mEnabled.length) {
	                view.setEnabled(mEnabled[position]);
	            } else {
	                Log.w(TAG, "Invalid input: enabled list length, " + mEnabled.length
	                        + " position " + position);
	            }
	            convertView = view;
	            //convertView.setBackgroundColor(R.color.red);
	            return convertView;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            if (position >= 0 && position < mEnabled.length) {
                return mEnabled[position];
            }
            return true;
        }
    }

    public void setSettingChangedListener(Listener listener) {
        mListener = listener;
    }

    public MoreSettingPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(PreferenceGroup group, String[] keys) {
        // Prepare the setting items.
        for (int i = 0; i < keys.length; ++i) {
            ListPreference pref = group.findPreference(keys[i]);
            if(keys[i].equals(CameraSettings.KEY_VIDEO_QUALITY) || keys[i].equals(CameraSettings.KEY_VIDEO_DURATION)){
            	if(CameraActivity.YIXI_LOG_ON)
            		Log.i("lilei", "keys:"+keys[i]+" pref:"+pref);
            }
            //modify by lilei begin
            //KEY_CAMERA_NOMAL should be added to mListItem and mListItemRadioGroup
            //if (pref != null) mListItem.add(pref);
            if (pref != null){
            	if(pref.getKey().equals(CameraSettings.KEY_CAMERA_NOMAL)){
            		mListItem.add(pref);
            	}
	            if(Arrays.asList(YixiSetMoreButton.radioGroup).contains(pref.getKey())){
	            	mListItemRadioGroup.add(pref);
	            }else{
	            	mListItem.add(pref);
	            }
            }
            //modify by lilei end
        }

        ArrayAdapter<ListPreference> mListItemAdapter = new MoreSettingAdapter();
        ((ListView) mSettingList).setAdapter(mListItemAdapter);
        ((ListView) mSettingList).setOnItemClickListener(this);
        ((ListView) mSettingList).setSelector(android.R.color.transparent);
        // Initialize mEnabled
        mEnabled = new boolean[mListItem.size()];
        for (int i = 0; i < mEnabled.length; i++) {
            mEnabled[i] = true;
        }
    }

    // When preferences are disabled, we will display them grayed out. Users
    // will not be able to change the disabled preferences, but they can still see
    // the current value of the preferences
    public void setPreferenceEnabled(String key, boolean enable) {
        int count = mEnabled == null ? 0 : mEnabled.length;
        for (int j = 0; j < count; j++) {
            ListPreference pref = mListItem.get(j);
            if (pref != null && key.equals(pref.getKey())) {
                mEnabled[j] = enable;
                break;
            }
        }
    }

    public void onSettingChanged(ListPreference pref) {
        if (mListener != null) {
            mListener.onSettingChanged(pref);
        }
    }

    // Scene mode can override other camera settings (ex: flash mode).
    public void overrideSettings(final String ... keyvalues) {
        int count = mEnabled == null ? 0 : mEnabled.length;
        for (int i = 0; i < keyvalues.length; i += 2) {
            String key = keyvalues[i];
            String value = keyvalues[i + 1];
            for (int j = 0; j < count; j++) {
                ListPreference pref = mListItem.get(j);
                if (pref != null && key.equals(pref.getKey())) {
                    // Change preference
                    if (value != null) pref.setValue(value);
                    // If the preference is overridden, disable the preference
                    boolean enable = value == null;
                    mEnabled[j] = enable;
                    if (mSettingList.getChildCount() > j) {
                        mSettingList.getChildAt(j).setEnabled(enable);
                    }
                }
            }
        }
        reloadPreference();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
    	Log.i("lilei", "onItemClick position:"+position+" mListener != null:"+(mListener != null));
        if (mListener != null) {
            ListPreference pref = mListItem.get(position);
            mListener.onPreferenceClicked(pref);
        }
    }

    @Override
    public void reloadPreference() {
        int count = mSettingList.getChildCount();
        for (int i = 0; i < count; i++) {
            ListPreference pref = mListItem.get(i);
            if (pref != null) {
                InLineSettingItem settingItem =
                        (InLineSettingItem) mSettingList.getChildAt(i);
                settingItem.reloadPreference();
            }
        }
    }
}

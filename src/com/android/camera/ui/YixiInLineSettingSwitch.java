/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.Switch;
import com.android.camera.ListPreference;
import com.android.camera2.R;

/* A check box setting control which turns on/off the setting. */
public class YixiInLineSettingSwitch extends InLineSettingItem {
	private Switch mSwitch;
    OnCheckedChangeListener mCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean desiredState) {
        	Log.i("lilei", "mCheckedChangeListener key:"+mPreference.getKey()+" desiredState:"+desiredState);
            changeIndex(desiredState ? 1 : 0);
        }
    };

    public YixiInLineSettingSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSwitch = (Switch)findViewById(R.id.setting_switch);
        mSwitch.setOnCheckedChangeListener(mCheckedChangeListener);
    }

    @Override
    public void initialize(ListPreference preference) {
        super.initialize(preference);
        // Add content descriptions for the increment and decrement buttons.       
    }
    @Override
    protected void updateView() {
    	mSwitch.setOnCheckedChangeListener(null);
    	//Log.i("lilei", "YixiInLineSettingRadioButton updateView() mTitle:"+mTitle+" mIndex:"+mIndex);
        if (mOverrideValue == null) {
        	mSwitch.setChecked(mIndex == 1);
        } else {
            int index = mPreference.findIndexOfValue(mOverrideValue);
            mSwitch.setChecked(index == 1);
        }
        mSwitch.setOnCheckedChangeListener(mCheckedChangeListener);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.getText().add(mPreference.getTitle());
        return true;
    }

    @Override
    public void setEnabled(boolean enable) {
        if (mTitle != null) mTitle.setEnabled(enable);
        if (mSwitch != null) mSwitch.setEnabled(enable);
    }
}

package com.android.camera.ui;


import com.android.camera.CameraActivity;
import com.android.camera.CameraSettings;
import com.android.camera.IconListPreference;













import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.android.camera.CameraPreference.OnPreferenceChangedListener;
import com.android.camera2.R;

import android.widget.TextView;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

public class YixiColorEffectButton extends RotateImageView implements YixiColorEffectSettingPopup.Listener{
    AbstractSettingPopup mPopupView;
    private Animation mFadeIn, mFadeOut;
    private Handler mHandler = new MainHandler();
    private final int MSG_DISMISS_POPUP = 0;
    private IconListPreference mPreference;
    private FrameLayout mRootView;
    private FrameLayout cameraRoot;
	PopupWindow mPopupWindow;
	private int mDegree = -1;
    protected OnPreferenceChangedListener mListener;
    private String  mOverrideValue = null;
    protected ViewGroup mPopupArrowContainer;

    public YixiColorEffectButton(Context context, AttributeSet attrs){
        super(context, attrs);
        mFadeIn = AnimationUtils.loadAnimation(context, R.anim.yixi_setting_popup_grow_fade_in);
        mFadeOut = AnimationUtils.loadAnimation(context, R.anim.yixi_setting_popup_shrink_fade_out);
    }
    public void setListener(OnPreferenceChangedListener listener,IconListPreference colorIconPref,FrameLayout root) {
        mListener = listener;
        mPreference = colorIconPref;
        mRootView = root;
    }
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DISMISS_POPUP:
                    dismissPopup();
                    break;
            }
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) return false;

        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN ) { //&& !isOverridden()
            if (mPopupWindow == null || ! mPopupWindow.isShowing()) {
            	if(CameraActivity.YIXI_LOG_ON)
            		Log.i("lilei","onTouchEvent 1111 YixiColorEffectButton showPopup()");
                showPopup();
                //PopupManager.getInstance(getContext()).notifyShowPopup(this);
            } else {
            	if(CameraActivity.YIXI_LOG_ON)
            		Log.i("lilei","onTouchEvent 2222 YixiColorEffectButton dismissPopup()");
                dismissPopup();
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            dismissPopup();
            return true;
        }
        return false;
    }
    @Override
    public void dismissPop(){
        dismissPopup();
    }
    public YixiColorEffectSettingPopup.Listener getColorEffectSetListener(){
        return this;
    }
    public AbstractSettingPopup getcolorPop(){
        return mPopupView;
    }
    @Override
    public void onSettingChanged() {
        //reloadPreference();
        // Dismiss later so the activated state can be updated before dismiss.
        //dismissPopupDelayed();
        if (mListener != null) {
        	if(CameraActivity.YIXI_LOG_ON)
        		Log.i("lilei", "YixiColorEffectButton.onSettingChanged()");
            mListener.onSharedPreferenceChanged();
        }
    }

    @Override
    public void setOrientation(int degree, boolean animation) {
    	if(CameraActivity.YIXI_LOG_ON)
    		Log.i("lilei", "YixiColorEffectButton.setOrientation degree:"+degree+" animation:"+animation);
        super.setOrientation(degree, animation);
        if (mPopupWindow != null && mPopupWindow.isShowing()){
//            if(degree == 180)
//                degree = -180;
            //mPopupView.setOrientation(degree, true);
            dismissPopup();
            showPopup();
        }
    }
    private void showPopup_del() {
        setPressed(true);
        setBackgroundResource(Color.TRANSPARENT);
        mHandler.removeMessages(MSG_DISMISS_POPUP);
        if (mPopupView == null) 
            initializePopup();
        if (mPopupView == null) return ;

        mPopupView.clearAnimation();
        mPopupView.startAnimation(mFadeIn);
        
        if(mPopupArrowContainer != null) {
            //updateArrowImagePos();
            mPopupArrowContainer.setVisibility(View.VISIBLE);
        }
    }
    private void showPopup(){
    	 if (mPopupView == null)
             initializePopup();
    	 mPopupView.setVisibility(View.VISIBLE);
    	 mPopupView.clearAnimation();
    	 mPopupView.startAnimation(mFadeIn);
    	if(mPopupWindow == null){
            mPopupWindow = new PopupWindow(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mPopupWindow.setOutsideTouchable(false);
            mPopupWindow.setFocusable(true);
        }
    	mDegree = getDegree();
    	mPopupView.setOrientation(mDegree, false);
    	mPopupWindow.setContentView(mPopupView);
    	//mPopupWindow.showAsDropDown(this, 0, -300);
    	 switch (mDegree) {
			case 0:
		    	mPopupWindow.showAsDropDown(this, 0, -300);
				break;
			case 90:
				mPopupWindow.showAtLocation(getRootView(), Gravity.LEFT, 0, 0);
				break;
			case 180:
		    	mPopupWindow.showAsDropDown(this, 0, -300);
				break;
			case 270:
				mPopupWindow.showAtLocation(getRootView(), Gravity.LEFT, 0, 0);
				break;
			default:
				break;
		}
    }
    public boolean dismissPopup(){
    	if(mPopupWindow != null && mPopupWindow.isShowing()){
    		mPopupWindow.dismiss();
    	}
    	return false;
    }
    public boolean dismissPopup_del() {
        setPressed(false);
        mHandler.removeMessages(MSG_DISMISS_POPUP);
        if (mPopupView != null && mPopupView.getVisibility() == View.VISIBLE) {
        	mPopupView.setVisibility(View.GONE);
            return true;
        }
        return false;
    }
    private void dismissPopupDelayed() {
        if (!mHandler.hasMessages(MSG_DISMISS_POPUP)) {
            mHandler.sendEmptyMessage(MSG_DISMISS_POPUP);
        }
    }
    private void initializePopup() {
        if (CameraSettings.KEY_COLOR_EFFECT.equals(mPreference.getKey())){
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE); //mRootView
            YixiColorEffectSettingPopup colorEffect = (YixiColorEffectSettingPopup) inflater.inflate(R.layout.yixi_color_setting_effect_popup, mRootView, false);
            colorEffect.initialize(mPreference);
            colorEffect.setSettingChangedListener(this);
            colorEffect.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // TODO Auto-generated method stub
                    return false;
                }
            });
            mPopupView = colorEffect;
        }

        FrameLayout.LayoutParams rlparams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT);
        rlparams.gravity = Gravity.BOTTOM;
        rlparams.bottomMargin = 120;
        rlparams.topMargin = 66;
        //mRootView.addView(mPopupView,rlparams);
        //((ViewGroup)mRootView).addView(mPopupView,rlparams);
    }
}

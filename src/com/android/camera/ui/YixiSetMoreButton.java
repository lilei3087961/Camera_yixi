package com.android.camera.ui;


import com.android.camera.CameraActivity;
import com.android.camera.CameraHolder;
import com.android.camera.CameraSettings;
import com.android.camera.IconListPreference;
import com.android.camera.ListPreference;
import com.android.camera.PreferenceGroup;

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
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import com.android.camera.CameraPreference.OnPreferenceChangedListener;
import com.android.camera2.R;
import com.android.camera2.R.string;

import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera.Parameters;

public class YixiSetMoreButton extends RotateImageView implements  MoreSettingPopup.Listener,ListPrefSettingPopup.Listener{
    private Animation mFadeIn, mFadeOut;
    private static final int SHOW_POP_LEVEL_1 = 1;
    private static final int SHOW_POP_LEVEL_2 = 2;
    private Handler mHandler = new MainHandler();
    private final int MSG_DISMISS_POPUP = 0;
    private IconListPreference mPreference;
    private PreferenceGroup mPreferenceGroupCamera;
    private PreferenceGroup mPreferenceGroupVideo;
    private FrameLayout mRootView;
    private FrameLayout cameraRoot;
    static final int INVALID_DEGREE = -1;
    private int mDegree = INVALID_DEGREE;
    private boolean mDismissAll = false;
    private boolean mPrefChanged = false;
    private boolean isPop2Rotated = false;
    private boolean isLand = false;
    private YixiTabHost tabHost = null;
    private YixiTabHost tabHostLand = null;
    private AbstractSettingPopup mPopViewLevel2;
	PopupWindow mPopupLevel1;
	PopupWindow mPopupLevel2;
    int mCameraId;
    CameraActivity mActivity;
    Parameters mInitialParams;
    protected OnPreferenceChangedListener mListener;
    private String  mOverrideValue = null;
	String strValueOn;
	String strHeadCamera;
	String strHeadVideo;
	String strHeadCommon;
	static final String TAG_CAMERA="camera";
	static final String TAG_VIDEO="video";
	static final String TAG_COMMON="common";
	String mTag = null;
    private String[] mOtherKeys1;
    private String[] mOtherKeys2;
    private String[] mOtherKeys3;
    public static String[] radioGroup = new String[]{
        CameraSettings.KEY_CAMERA_NOMAL,
        CameraSettings.KEY_CAMERA_PANORAMA,
        CameraSettings.KEY_LONGSHOT,
        CameraSettings.KEY_ZSL,
        CameraSettings.KEY_CAMERA_HDR
    };
    public YixiSetMoreButton(Context context, AttributeSet attrs){
        super(context, attrs);
        mFadeIn = AnimationUtils.loadAnimation(context, R.anim.yixi_setting_popup_grow_fade_in);
        mFadeOut = AnimationUtils.loadAnimation(context, R.anim.yixi_setting_popup_shrink_fade_out);
        strValueOn = getResources().getString(R.string.setting_on_value);
        strHeadCamera = getResources().getString(R.string.tab_head_camera);
        strHeadVideo = getResources().getString(R.string.tab_head_video);
        strHeadCommon = getResources().getString(R.string.tab_head_common);
    }
    public void setListener(OnPreferenceChangedListener listener,int cameraId,
    		CameraActivity activity,Parameters initialParams,FrameLayout root){
    	mListener = listener;
    	mCameraId = cameraId;
    	mActivity = activity;
    	mInitialParams = initialParams;
    	mRootView = root;
    	CameraSettings settings = new CameraSettings(mActivity, mInitialParams,mCameraId, 
    			CameraHolder.instance().getCameraInfo());
    	mPreferenceGroupCamera = settings.getPreferenceGroup(R.xml.camera_preferences);
    	mPreferenceGroupVideo = settings.getPreferenceGroup(R.xml.video_preferences);
    }
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DISMISS_POPUP:
                	dismissPopupLevel1();
                    break;
            }
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) return false;
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN ) { //&& !isOverridden()
            if (mPopupLevel1 == null || !mPopupLevel1.isShowing()) {
            	if(CameraActivity.YIXI_LOG_ON)
            		Log.i("lilei","onTouchEvent 1111 set More showPopup(SHOW_POP_LEVEL_1)");
            	showPopupLevel1();
                //PopupManager.getInstance(getContext()).notifyShowPopup(this);
            } else {
            	if(CameraActivity.YIXI_LOG_ON)
            		Log.i("lilei","onTouchEvent 2222 set More dismissPopupAll()");
                dismissPopupAll();
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            Log.i("lilei","onTouchEvent 3333 ");
            dismissPopupAll();
            return true;
        }
        return false;
    }
    public YixiColorEffectSettingPopup.Listener getColorEffectSetListener(){
        return null;
    }
    public AbstractSettingPopup getcolorPop(){
        return null;
    }
    //@Override
    public void onSettingChanged() {
        //reloadPreference();
        // Dismiss later so the activated state can be updated before dismiss.
        //dismissPopupDelayed();
        if (mListener != null) {
            Log.i("lilei", "YixiColorEffectButton.onSettingChanged()");
            mListener.onSharedPreferenceChanged();
        }
    }

    @Override
    public void setOrientation(int degree, boolean animation) {
        Log.i("lilei", "~~~00 no YixiColorEffectButton.setOrientation degree:"+degree+" animation:"+animation);
        super.setOrientation(degree, animation);
        mDegree = degree;
        if(mPopupLevel1 != null && mPopupLevel1.isShowing()){
	        dismissPopupLevel1();
	        showPopupLevel1();
        }
        if(mPopupLevel2 != null && mPopupLevel2.isShowing()){
        	isPop2Rotated = true;
        	dismissPopupAll();
        	showPopupLevel2();
        }
    }

	private void showPopupLevel1() {
        setPressed(true);
        setBackgroundResource(Color.TRANSPARENT);
        mHandler.removeMessages(MSG_DISMISS_POPUP);
        mDegree = getDegree();
        if(mDegree == 90 || mDegree == 270){
        	isLand =  true;
        }else{
        	isLand =  false;
        }

        if ((!isLand && tabHost == null) || (isLand && tabHostLand == null)){
            initializePopup();
        }
        if(isLand && tabHostLand == null){
        	Log.i("lilei", "!! error tabHostLand == null");
        	return;
        }else if(!isLand && tabHost == null){
        	Log.i("lilei", "!! error tabHost == null");
        	return;
        }

        if(mPopupLevel1 == null){
        	mPopupLevel1 = new PopupWindow(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        	mPopupLevel1.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        	mPopupLevel1.setOutsideTouchable(true);
        	mPopupLevel1.setOnDismissListener(new PopupWindow.OnDismissListener() {
				@Override
				public void onDismiss() {
					// TODO Auto-generated method stub
					Log.i("lilei", "mPopupLevel1.onDismiss() ");
				}
			});
        	mPopupLevel1.setFocusable(true);
        }
        if(isLand){
        	tabHostLand.setOrientation(mDegree, false);
        	mPopupLevel1.setContentView(tabHostLand);
        }else{
        	tabHost.setOrientation(mDegree, false);
        	mPopupLevel1.setContentView(tabHost);
        }
        Log.i("lilei", "### showPopup()  mDegree:"+mDegree+" isLand:"+isLand);
        switch (mDegree) {
			case 0:
				mPopupLevel1.showAsDropDown(this);
				break;
			case 90:
				mPopupLevel1.showAtLocation(getRootView(),Gravity.LEFT ,0,0);  // y -120
				break;
			case 180:
				mPopupLevel1.showAsDropDown(this);
				break;
			case 270:
				mPopupLevel1.showAtLocation(getRootView(),Gravity.RIGHT ,0,0);
				break;
			default:
				break;
		}
    }
    private void showPopupLevel2() {
    	if(mPopupLevel2 == null){
    		mPopupLevel2 = new PopupWindow(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    		mPopupLevel2.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    		mPopupLevel2.setOutsideTouchable(true);
    		mPopupLevel2.setOnDismissListener(new PopupWindow.OnDismissListener() {
				@Override
				public void onDismiss() {
					// TODO Auto-generated method stub
					Log.i("lilei", "mPopupLevel2 onDismiss()");
					if(mPrefChanged || isPop2Rotated){
						mPrefChanged = false;
						isPop2Rotated = false;
					}else{
						showPopupLevel1();
					}
				}
			});
    		mPopupLevel2.setFocusable(true);
    	}
    	mPopViewLevel2.setVisibility(View.VISIBLE);
       	mDegree = getDegree();
    	Log.i("lilei", "###*** showPopupSecondLevel()  mDegree:"+mDegree);
    	mPopViewLevel2.setOrientation(mDegree, false);
    	mPopupLevel2.setContentView(mPopViewLevel2);
    	//mPopupLevel2.showAsDropDown(this);
    	//mPopupLevel2.showAsDropDown(this, 5, 0);
    	switch (mDegree) {
			case 0:
				mPopupLevel2.showAsDropDown(this);
				break;
			case 90:
				mPopupLevel2.showAtLocation(getRootView(),Gravity.LEFT ,0,0);
				break;
			case 180:
				mPopupLevel2.showAsDropDown(this);
				break;
			case 270:
				mPopupLevel2.showAtLocation(getRootView(),Gravity.RIGHT ,0,0);
				break;
			default:
				break;
    	}
    }
    public void dismissPopup() {
        setPressed(false);
        mHandler.removeMessages(MSG_DISMISS_POPUP);
        //if(CameraActivity.YIXI_LOG_ON)
        if (mPopupLevel1 != null && mPopupLevel1.isShowing()) {
        	mPopupLevel1.dismiss();
        }
        if(mDismissAll && mPopupLevel2 != null && mPopupLevel2.isShowing()){
        	mPopupLevel2.dismiss();
        }
    }
    private void dismissPopupAll() {
    	dismissPopupLevel1();
    	dismissPopupLevel2();
    }
    private void dismissPopupLevel1(){
        if (mPopupLevel1 != null && mPopupLevel1.isShowing()) {
        	mPopupLevel1.dismiss();
        }
    }
    private void dismissPopupLevel2(){
    	 if(mPopupLevel2 != null && mPopupLevel2.isShowing()){
         	mPopupLevel2.dismiss();
         }
    }
    public void clearAll(){
    	tabHost = null;
    	mPopViewLevel2 = null;
    	mPopupLevel1 = null;
    	mPopupLevel2 = null;
    }
    private void dismissPopupDelayed() {
        if (!mHandler.hasMessages(MSG_DISMISS_POPUP)) {
            mHandler.sendEmptyMessage(MSG_DISMISS_POPUP);
        }
    }

    private void initializePopup() {
    	mOtherKeys1 = new String[] {
                CameraSettings.KEY_CAMERA_NOMAL,
                CameraSettings.KEY_CAMERA_PANORAMA,
                CameraSettings.KEY_LONGSHOT,
                CameraSettings.KEY_ZSL,
                CameraSettings.KEY_CAMERA_HDR,
                CameraSettings.KEY_REDEYE_REDUCTION,
                CameraSettings.KEY_TIMER,
                CameraSettings.KEY_PICTURE_SIZE

        };
        mOtherKeys2 = new String[] {
                CameraSettings.KEY_VIDEO_QUALITY,
                CameraSettings.KEY_VIDEO_DURATION,
                CameraSettings.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL
        };
        mOtherKeys3 = new String[] {
                CameraSettings.KEY_RECORD_LOCATION,
                CameraSettings.KEY_CAMERA_SAVEPATH,
                CameraSettings.KEY_WHITE_BALANCE
        };
    	LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	mDegree = getDegree();
    	if(mDegree == 90 || mDegree == 270){ //for land
    		View rootViewTabHost = inflater.inflate(R.layout.yixi_set_more_tabhost_land,null);
        	MoreSettingPopup popup1 = (MoreSettingPopup)rootViewTabHost.findViewById(R.id.tab_camera);
        	popup1.setSettingChangedListener(this);
        	popup1.initialize(mPreferenceGroupCamera, mOtherKeys1);
        	MoreSettingPopup popup2 = (MoreSettingPopup)rootViewTabHost.findViewById(R.id.tab_video);
        	popup2.setSettingChangedListener(this);
        	popup2.initialize(mPreferenceGroupVideo, mOtherKeys2);
        	MoreSettingPopup popup3 = (MoreSettingPopup)rootViewTabHost.findViewById(R.id.tab_common);
        	popup3.setSettingChangedListener(this);
        	popup3.initialize(mPreferenceGroupCamera, mOtherKeys3);
        	
        	tabHostLand = (YixiTabHost)rootViewTabHost.findViewById(R.id.tabhost_set_more);
        	tabHostLand.setup();
        	//tabHost = new TabHost(getContext());
        	tabHostLand.addTab(tabHostLand.newTabSpec(TAG_CAMERA)
        			.setContent(R.id.tab_camera)
        			.setIndicator(strHeadCamera,getResources().getDrawable(R.drawable.yixi_effect_no)));
        	tabHostLand.addTab(tabHostLand.newTabSpec(TAG_VIDEO)
        			.setContent(R.id.tab_video)
        			.setIndicator(strHeadVideo,getResources().getDrawable(R.drawable.yixi_effect_no)));
        	tabHostLand.addTab(tabHostLand.newTabSpec(TAG_COMMON)
        			.setContent(R.id.tab_common)
        			.setIndicator(strHeadCommon,getResources().getDrawable(R.drawable.yixi_effect_no)));
        	tabHostLand.setOnTabChangedListener(new OnTabChangeListener() {
    			@Override
    			public void onTabChanged(String arg0) {
    				// TODO Auto-generated method stub
    				Log.i("lilei", "tabHostLand.onTabChanged() arg0:"+arg0);
    				mTag = arg0;
    			}
    		});
        	if(mTag == null && mActivity.getCurrentModuleIndex() == ModuleSwitcher.VIDEO_MODULE_INDEX){  //for video
        		mTag = TAG_VIDEO;
        	}else if(mTag == null && mActivity.getCurrentModuleIndex() == ModuleSwitcher.PHOTO_MODULE_INDEX){
        		mTag = TAG_VIDEO;
        	}
        	tabHostLand.setCurrentTabByTag(mTag);
    	}else{//for vertical
    		View rootViewTabHost = inflater.inflate(R.layout.yixi_set_more_tabhost,null);
        	MoreSettingPopup popup1 = (MoreSettingPopup)rootViewTabHost.findViewById(R.id.tab_camera);
        	popup1.setSettingChangedListener(this);
        	popup1.initialize(mPreferenceGroupCamera, mOtherKeys1);
        	MoreSettingPopup popup2 = (MoreSettingPopup)rootViewTabHost.findViewById(R.id.tab_video);
        	popup2.setSettingChangedListener(this);
        	popup2.initialize(mPreferenceGroupVideo, mOtherKeys2);
        	MoreSettingPopup popup3 = (MoreSettingPopup)rootViewTabHost.findViewById(R.id.tab_common);
        	popup3.setSettingChangedListener(this);
        	popup3.initialize(mPreferenceGroupCamera, mOtherKeys3);
        	
        	tabHost = (YixiTabHost)rootViewTabHost.findViewById(R.id.tabhost_set_more);
        	tabHost.setup();
        	//tabHost = new TabHost(getContext());
        	tabHost.addTab(tabHost.newTabSpec(TAG_CAMERA)
        			.setContent(R.id.tab_camera)
        			.setIndicator(strHeadCamera,getResources().getDrawable(R.drawable.yixi_effect_no)));
        	tabHost.addTab(tabHost.newTabSpec(TAG_VIDEO)
        			.setContent(R.id.tab_video)
        			.setIndicator(strHeadVideo,getResources().getDrawable(R.drawable.yixi_effect_no)));
        	tabHost.addTab(tabHost.newTabSpec(TAG_COMMON)
        			.setContent(R.id.tab_common)
        			.setIndicator(strHeadCommon,getResources().getDrawable(R.drawable.yixi_effect_no)));
        	tabHost.setOnTabChangedListener(new OnTabChangeListener() {
    			@Override
    			public void onTabChanged(String arg0) {
    				// TODO Auto-generated method stub
    				Log.i("lilei", "tabHost.onTabChanged() arg0:"+arg0);
    				mTag = arg0;
    			}
    		});
        	if(mTag == null && mActivity.getCurrentModuleIndex() == ModuleSwitcher.VIDEO_MODULE_INDEX){  //for video
        		mTag = TAG_VIDEO;
        	}else if(mTag == null && mActivity.getCurrentModuleIndex() == ModuleSwitcher.PHOTO_MODULE_INDEX){
        		mTag = TAG_VIDEO;
        	}
    		tabHost.setCurrentTabByTag(mTag);
    	}
        Log.i("lilei","initializePopup() mTag:"+mTag+" mDegree:"+mDegree+" tabHost:"+tabHost+" tabHostLand:"+tabHostLand);
    }
    /*
     * yixicamera note:
     * seetting changed
     * when radio is checked,this function will be called twice,wo shuld only do the checked item.
    */
	@Override
	public void onSettingChanged(ListPreference pref) {
		// TODO Auto-generated method stub
		//Log.i("lilei", "###YixiSetMoreButton.onSettingChanged key:"+pref.getKey()+" pref:"+pref.getValue());
		if(Arrays.asList(YixiSetMoreButton.radioGroup).contains(pref.getKey()) && !pref.getValue().equals(strValueOn)){
			//Log.i("lilei", "###YixiSetMoreButton.onSettingChanged 00 key:"+pref.getKey()+" pref:"+pref.getValue());
			return;
		}
		if(pref.getKey().equals(CameraSettings.KEY_CAMERA_PANORAMA)){
			//Log.i("lilei", "###YixiSetMoreButton.onSettingChanged 1.5 key:"+pref.getKey()+" pref:"+pref.getValue());
			dismissPopupLevel1();
			mActivity.onModuleSelected(ModuleSwitcher.WIDE_ANGLE_PANO_MODULE_INDEX);
			return;
		}
		if((mTag == TAG_CAMERA && mActivity.getCurrentModuleIndex() == ModuleSwitcher.VIDEO_MODULE_INDEX)
		  ||(mTag == TAG_VIDEO &&mActivity.getCurrentModuleIndex() == ModuleSwitcher.PHOTO_MODULE_INDEX)){
			Log.i("lilei", "!!!mTag is:"+mTag+" but ModuleIndex is:"+mActivity.getCurrentModuleIndex()+" return");
			return;
		}
		mListener.onSharedPreferenceChanged();
		if(CameraActivity.YIXI_LOG_ON)
			Log.i("lilei", "~~~ first level seetting change:"+pref.getKey()+" value:"+pref.getValue());
	}
    /*
     * yixicamera note:
     * first level seetting item clicked
    */
	@Override
	public void onPreferenceClicked(ListPreference pref) {
		// TODO Auto-generated method stub
		LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        ListPrefSettingPopup basic = (ListPrefSettingPopup) inflater.inflate(
                R.layout.list_pref_setting_popup, null, false);
        basic.initialize(pref);
        basic.setSettingChangedListener(this);
        dismissPopupLevel1();
        if(CameraActivity.YIXI_LOG_ON)
        	Log.i("lilei", "###  key:"+pref.getKey()+" value:"+pref.getValue());
        if(mDegree != INVALID_DEGREE){
        	basic.setOrientation(mDegree, false);
        }
    	mPopViewLevel2 = basic;
    	showPopupLevel2();
	}
    /*
     * yixicamera note:
     * second level seetting changed
     * when second level setting change,should reinitialize first level pop
     * ex: set picture size, and so on.
    */
	@Override
	public void onListPrefChanged(ListPreference pref) {
		// TODO Auto-generated method stub
		mPrefChanged = true;
		onSettingChanged(pref);
		Log.i("lilei", "onListPrefChanged");
		dismissPopupAll();
		initializePopup();
		showPopupLevel1();
	}
}

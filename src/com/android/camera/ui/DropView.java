package com.android.camera.ui;

import com.android.camera.CameraActivity;
import com.android.camera.ui.ModuleSwitcher.ModuleSwitchListener;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import com.android.camera.ListPreference ;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.android.camera.PreferenceGroup;

import com.android.camera.CameraPreference.OnPreferenceChangedListener;
import com.android.camera.ui.ModuleSwitcher;
import com.android.camera.CameraHolder;
import com.android.camera.CameraSettings ;
import com.android.camera2.R;

public class DropView extends RelativeLayout implements View.OnClickListener{
	
	private View mDropLayoutView;
	private int mDropHeight;
	private ImageView mFlashView ;
	private ImageView mPhotoView ;
	private ImageView mSwitchView ;
	private ImageView mIndiactorView ;
	private CameraActivity mActivity ;
	private Context mContext ;
	private ModuleSwitchListener mListener;
	
	private int STATUS_SHOWING = 0 ;
	private int STATUS_HIDEING = 1 ;
	private int viewStatus = STATUS_SHOWING; 
	private View viewParent ;
	private int moduleIndex ;
	private int mCameraId = 0;
	private OnPreferenceChangedListener mOnPreferenceChangedListener ;
	private PreferenceGroup mPreferenceGroup ;
	
	private String mFlashKey ;
	private final String FLASH_DISABLE = "disable" ;

	public DropView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		mContext = context ;
	}
	
	public DropView(Context context, AttributeSet attrs) {
		super(context);
		// TODO Auto-generated constructor stub
		mContext = context ;
	}

	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		mFlashView = (ImageView)findViewById(R.id.flash_view);
		mFlashView.setOnClickListener(this) ;
		mPhotoView = (ImageView)findViewById(R.id.photo_view);
		mPhotoView.setOnClickListener(this) ;
		mSwitchView = (ImageView)findViewById(R.id.switch_view);
		mSwitchView.setOnClickListener(this) ;
		if(getCameraCount() <= 1) {
			mSwitchView.setVisibility(View.GONE);
		}
		mDropLayoutView = findViewById(R.id.drop_view);
		mIndiactorView = (ImageView)findViewById(R.id.indiactor_view);
		mIndiactorView.setOnClickListener(this) ;
	}
	
	public void setActivity(CameraActivity activity){
		mActivity = activity ;
	}
	
	public void hideView(){
		mFlashView.setVisibility(View.GONE);
		mPhotoView.setVisibility(View.GONE);
		mSwitchView.setVisibility(View.GONE);
		mIndiactorView.setImageResource(R.drawable.yixi_image_to_show);
	}
	
	public void showView(){
		mFlashView.setVisibility(View.VISIBLE);
		mPhotoView.setVisibility(View.VISIBLE);
		mSwitchView.setVisibility(View.VISIBLE);
		mIndiactorView.setImageResource(R.drawable.yixi_image_to_hide);
	}

	public void setOnModuleSwitcherListener(ModuleSwitchListener listener){
		mListener = listener ;
	}
	
	public void setOnPreferenceChangedListener(OnPreferenceChangedListener listener){
		mOnPreferenceChangedListener = listener ;
	}
	
	public void setIndex(int index){
		moduleIndex = index ;
		if(moduleIndex == ModuleSwitcher.VIDEO_MODULE_INDEX) {
			mPhotoView.setImageResource(R.drawable.ic_switch_camera);
			mFlashKey = CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE;
		} else if(moduleIndex == ModuleSwitcher.PHOTO_MODULE_INDEX){
			mPhotoView.setImageResource(R.drawable.ic_switch_video);
			mFlashKey = CameraSettings.KEY_FLASH_MODE ;
		}
	}
	
	public void setPreferenceGroup(PreferenceGroup group){
		mPreferenceGroup = group ;
		updateFlashUi(getCurrentMode());
	}
	
	public void hideCameraSwitch(){
		mSwitchView.setVisibility(View.GONE);
	}
	
	public void setCameraId(int cameraId){
		mCameraId = cameraId ;
		CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId] ;
		if(info.facing == CameraInfo.CAMERA_FACING_FRONT) {
			mSwitchView.setImageResource(R.drawable.ic_switch_back);
		} else {
			mSwitchView.setImageResource(R.drawable.ic_switch_front);
		}
	}
	
	private void switchCamera(){
		int numOfCamera = getCameraCount();
		mCameraId = (mCameraId + 1) % numOfCamera ;
		mOnPreferenceChangedListener.onCameraPickerClicked(mCameraId);
		setCameraId(mCameraId);
		updateFlashUi(getCurrentMode());
	}
	
    private int getCameraCount() {
        return android.hardware.Camera.getNumberOfCameras();
    }
	
	private void switchModule() {
		int index ;
		if(mListener != null) {
			if(moduleIndex == ModuleSwitcher.VIDEO_MODULE_INDEX) {
				index = ModuleSwitcher.PHOTO_MODULE_INDEX ;
			} else {
				index = ModuleSwitcher.VIDEO_MODULE_INDEX ;
			}
			mListener.onModuleSelected(index);
		}
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v == mFlashView) {
			switchFlashMode();
		} else if(v == mPhotoView){
			switchModule();
		} else if(v == mSwitchView){
			switchCamera();
		} else if(v == mIndiactorView){
			startAnimation(isNeedHide());
			changeViewStatus();
		}
	}
	
	private boolean isNeedHide(){
		if(viewStatus == STATUS_SHOWING) {
			return true ;
		}
		return false ;
	}
	
	private void changeViewStatus(){
		if(viewStatus == STATUS_SHOWING) {
			viewStatus = STATUS_HIDEING ;
		} else {
			viewStatus = STATUS_SHOWING ;
		}
	}
	
	private float getAnimationDistance_del(boolean isHide){
		mDropHeight = mDropLayoutView.getHeight()>0?mDropLayoutView.getHeight():mDropHeight;
		int len = mDropHeight;

		Log.i("lilei", "getAnimationDistance() mDropLayoutView.getHeight():"+mDropLayoutView.getHeight()
				+" mSwitchView.getVisibility():"+mSwitchView.getVisibility());
		float distance ;
		if(isHide) {
			if(mSwitchView.getVisibility() == View.GONE) {
				distance = - ((float) (2/3*len)) ;
			}else {
				distance = - ((float) (1/4*len)) ;
			}			
		} else {
			if(mSwitchView.getVisibility() == View.GONE) {
				distance = - ((float) (2*len)) ;
			}else {
				distance = - ((float) (3*len)) ;
			}
		}
		return distance;
	}
	private float getAnimationDistance(boolean isHide){
		mDropHeight = mDropLayoutView.getHeight()>0?mDropLayoutView.getHeight():mDropHeight;
		Log.i("lilei", "getAnimationDistance() mDropHeight:"+mDropHeight);
		float distance ;
		if(isHide) {
			distance = (float) (-0.75*mDropHeight);
		}else{
			distance = -3*mDropHeight;
		}
		return distance;
	}
	public void startAnimation(final boolean isHideView){		
		int len = getHeight(); 
		TranslateAnimation anima  ;
		if(isHideView) {
			anima = new TranslateAnimation(0, 0,0, 100) ;
		}else {
			anima = new TranslateAnimation(0, 0, getAnimationDistance(isHideView) ,0) ;
		}
		anima.setDuration(800);
		Log.i("lilei","startAnimation isHideView:"+isHideView+" getAnimationDistance:"+getAnimationDistance(isHideView));
		startAnimation(anima);
		anima.start();
		anima.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				mIndiactorView.setClickable(false);
				Log.i("lilei","startAnimation() onAnimationStart");
				if(!isHideView) {
					showView();
				}
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				Log.i("lilei","startAnimation() onAnimationEnd");
				if(isHideView) {
					hideView();	
				}
				mIndiactorView.setClickable(true);
			}
		}) ;
	}
	
	private void switchFlashMode(){
		String nextMode = getNextFlashMode(getCurrentMode()) ;
		setFlashMode(nextMode);
		updateFlashUi(nextMode);
		mOnPreferenceChangedListener.onSharedPreferenceChanged();
	}
	
	private void setFlashMode(String  mode){
		if(mode.equals(FLASH_DISABLE))
			return ;
		ListPreference pref = mPreferenceGroup.findPreference(mFlashKey);
		pref.setValue(mode);
	}
	
	private String getCurrentMode(){
		ListPreference pref = mPreferenceGroup.findPreference(mFlashKey);
		if(pref == null) {
			return FLASH_DISABLE ;
		}
		return pref.getValue() ;
	}
	
	private String getNextFlashMode(String currentMode){
		String nextMode = "";
		if(currentMode.equals(Parameters.FLASH_MODE_OFF)) {
			nextMode = Parameters.FLASH_MODE_AUTO ;
		} else 	if(currentMode.equals(Parameters.FLASH_MODE_ON)) {
			nextMode = Parameters.FLASH_MODE_OFF ;
		} else 	if(currentMode.equals(Parameters.FLASH_MODE_AUTO)) {
			nextMode = Parameters.FLASH_MODE_ON ;
		} else {
			nextMode = FLASH_DISABLE ;
		}
		return nextMode ;
	}
	
	private void updateFlashUi(String mode){
		Log.e("aa", "dxh  updateFlashUi  mode=" +mode);
		mFlashView.setEnabled(true);
		if(mode.equals(Parameters.FLASH_MODE_OFF)) {
			mFlashView.setImageResource(R.drawable.ic_flash_off_holo_light);
		} else 	if(mode.equals(Parameters.FLASH_MODE_ON)) {
			mFlashView.setImageResource(R.drawable.ic_flash_on_holo_light);
		} else 	if(mode.equals(Parameters.FLASH_MODE_AUTO)) {
			mFlashView.setImageResource(R.drawable.ic_flash_auto_holo_light);
		}else {
			mFlashView.setImageResource(R.drawable.ic_flash_off_holo_light);
			mFlashView.setEnabled(false);
		}
	}
}

/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.camera;

import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.camera.CameraPreference.OnPreferenceChangedListener;
import com.android.camera.data.LocalMediaData;
import com.android.camera.data.LocalMediaData.PhotoData;
import com.android.camera.ui.AbstractSettingPopup;
import com.android.camera.ui.CameraControls;
import com.android.camera.ui.CameraRootView;
import com.android.camera.ui.ModuleSwitcher;
import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.RenderOverlay;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.YixiCameraControls;
import com.android.camera.ui.YixiSetMoreButton;
import com.android.camera.ui.YixiShutterButton;
import com.android.camera.PauseButton.OnPauseButtonListener;
import com.android.camera.ui.ZoomRenderer;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.android.camera.ui.DropView ;

import java.util.List;

public class VideoUI implements PieRenderer.PieListener,
        PreviewGestures.SingleTapListener,
        CameraRootView.MyDisplayListener,
        SurfaceTextureListener, SurfaceHolder.Callback,
        PauseButton.OnPauseButtonListener {
    private static final String TAG = "CAM_VideoUI";
    private static final int UPDATE_TRANSFORM_MATRIX = 1;
    // module fields
    private CameraActivity mActivity;
    private View mRootView;
    private TextureView mTextureView;
    // An review image having same size as preview. It is displayed when
    // recording is stopped in capture intent.
    private ImageView mReviewImage;
    private View mReviewCancelButton;
    private View mReviewDoneButton;
    private View mReviewPlayButton;
    //modify by lilei begin
    //private ShutterButton mShutterButton;
    //private CameraControls mCameraControls;
    //private View mPreviewThumb;
    //private VideoController mController;
    //private LinearLayout mLabelsLinearLayout;
    private YixiShutterButton mShutterButton;
    private YixiCameraControls mCameraControls;
    private RotateImageView mPreviewThumb;
    private YixiSetMoreButton mYixiSetMoreButton;
    private View mSetMoreView;
    private LoadLatestImageTask mLoadLatestImageTask;
    private YixiVideoController mController;
    private TextView mRecordingIndicator;
    private RelativeLayout mLabelsLinearLayout;
    //modify by lilei end
    private PauseButton mPauseButton;
    private ModuleSwitcher mSwitcher;
    private TextView mRecordingTimeView;
    private View mTimeLapseLabel;
    private RenderOverlay mRenderOverlay;
    private PieRenderer mPieRenderer;
    private VideoMenu mVideoMenu;
    private SettingsPopup mPopup;
    private ZoomRenderer mZoomRenderer;
    private PreviewGestures mGestures;
    private View mMenuButton;
    private OnScreenIndicators mOnScreenIndicators;
    private RotateLayout mRecordingTimeRect;
    private boolean mRecordingStarted = false;
    private SurfaceTexture mSurfaceTexture;
    private int mZoomMax;
    private List<Integer> mZoomRatios;
    private View mFlashOverlay;
    private boolean mOrientationResize;
    private boolean mPrevOrientationResize;
    private boolean mIsTimeLapse = false;

    private View mPreviewCover;
    private SurfaceView mSurfaceView = null;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private float mSurfaceTextureUncroppedWidth;
    private float mSurfaceTextureUncroppedHeight;
    private float mAspectRatio = 4f / 3f;
    private boolean mAspectRatioResize;
    private Matrix mMatrix = null;
    private final YixiAnimationManager mAnimationManager;  //modify by lilei
    private DropView mDropView ;
    private int mCameraId ;
    //add by lilei begin
    public interface DismissListener{
        public void dismissPop();
    }
	private void initSetMoreView(){
		mSetMoreView = (mActivity.getLayoutInflater().inflate(R.layout.yixi_set_more_view, null));
		if(mSetMoreView != null){
			((ViewGroup)mRootView).addView(mSetMoreView);
			mYixiSetMoreButton = (YixiSetMoreButton)mSetMoreView.findViewById(R.id.set_more_button);
		}
	}
	public void setListener(Camera.Parameters params, OnPreferenceChangedListener listener){
		mYixiSetMoreButton.setListener(listener,mCameraId, mActivity, params,null);
	}
    //add by lilei end
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TRANSFORM_MATRIX:
                    setTransformMatrix(mPreviewWidth, mPreviewHeight);
                    break;
                default:
                    break;
            }
        }
    };
    private OnLayoutChangeListener mLayoutListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            int width = right - left;
            int height = bottom - top;
            // Full-screen screennail
            int w = width;
            int h = height;
            //modify for AP-520 by lilei begin
            int orientation = CameraUtil.getDisplayRotation(mActivity);
            if(orientation != 180){
                orientation = 0;
            }
            mRecordingTimeRect.setOrientation(orientation, false); //add for AP-483 by lilei
            //modify for AP-520 by lilei end
            if (CameraUtil.getDisplayRotation(mActivity) % 180 != 0) {
                w = height;
                h = width;
            }
            if (mPreviewWidth != width || mPreviewHeight != height
                    || (mOrientationResize != mPrevOrientationResize)
                    || (mAspectRatioResize)) {
                mPreviewWidth = width;
                mPreviewHeight = height;
                onScreenSizeChanged(width, height, w, h);
                mAspectRatioResize = false;
            }
        }
    };

    public void showPreviewCover() {
        mPreviewCover.setVisibility(View.VISIBLE);
    }

    private class SettingsPopup extends PopupWindow {
        public SettingsPopup(View popup) {
            super(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setOutsideTouchable(true);
            setFocusable(true);
            popup.setVisibility(View.VISIBLE);
            setContentView(popup);
            showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        }

        public void dismiss(boolean topLevelOnly) {
            super.dismiss();
            popupDismissed();
            showUI();
            mVideoMenu.popupDismissed(topLevelOnly);

            // Switch back into fullscreen/lights-out mode after popup
            // is dimissed.
            mActivity.setSystemBarsVisibility(false);
        }

        @Override
        public void dismiss() {
            // Called by Framework when touch outside the popup or hit back key
            dismiss(true);
        }
    }
  //add by lilei begin
    private String getLatestImagePath(){
        String[] CAMERA_PATH = { Storage.DIRECTORY + "/%" ,SDCard.instance().getDirectory() + "/%"};
        Cursor c = mActivity.getContentResolver().query(
                LocalMediaData.PhotoData.CONTENT_URI,
                LocalMediaData.PhotoData.QUERY_PROJECTION,
                MediaStore.Images.Media.DATA + " like ? or " +
                MediaStore.Images.Media.DATA + " like ? ", CAMERA_PATH,
                LocalMediaData.PhotoData.QUERY_ORDER);
        String path = null;
        if (c != null && c.moveToFirst()) {
            path = c.getString(PhotoData.COL_DATA);
            Log.i("lilei", "getLatestImagePath() c.getCount():"+c.getCount()+" path:"+path);
        }
        if (c != null) {
            c.close();
        }
        return path;
    }
    private class  LoadLatestImageTask extends AsyncTask<Void, Void,Bitmap>{
        @Override
        protected Bitmap doInBackground(Void... params) {
//          LocalData mLocalData = mActivity.getFixedFirstDataAdapter().getLocalData(1);
//          String imgPath = mLocalData==null?"":mLocalData.getPath();
            Bitmap sImageBitmap = null;
            String imgPath = getLatestImagePath();
            if(imgPath != null){
                  Bitmap imageBitmap = BitmapFactory.decodeFile(imgPath);
                  if(imageBitmap == null){
                      Log.i("lilei","warn get latest image is null!maybe raw, path:"+imgPath);
                  }
                  if(imageBitmap != null && imageBitmap.getWidth() >0){
	                  Matrix matrix = new Matrix();
	                  float scaleWidth = (float)mPreviewThumb.getLayoutParams().width / imageBitmap.getWidth();
	                  float scaleHeight = (float)mPreviewThumb.getLayoutParams().height / imageBitmap.getHeight();
	                  int height = mPreviewThumb.getLayoutParams().height;
	                  matrix.postScale(scaleWidth,scaleHeight);
	                  sImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix,false);
	                  //mPreviewThumb.setImageBitmap(sImageBitmap);
              }
            }
            return sImageBitmap;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null)
                mPreviewThumb.setImageBitmap(bitmap);
            mPreviewThumb.setVisibility(View.VISIBLE);
        }
    }
    //add by lilei end
    public VideoUI(CameraActivity activity, YixiVideoController controller, View parent) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;
        //modify by lilei begin
        //mActivity.getLayoutInflater().inflate(R.layout.video_module, (ViewGroup) mRootView, true);
        mActivity.getLayoutInflater().inflate(R.layout.yixi_video_module, (ViewGroup) mRootView, true);
        initSetMoreView();
        //modify by lilei end
        initDropView();
        mPreviewCover = mRootView.findViewById(R.id.preview_cover);
        mTextureView = (TextureView) mRootView.findViewById(R.id.preview_content);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.addOnLayoutChangeListener(mLayoutListener);
        mFlashOverlay = mRootView.findViewById(R.id.flash_overlay);
        //modify by lilei begin
        mShutterButton = (YixiShutterButton) mRootView.findViewById(R.id.shutter_button);
        mSwitcher = (ModuleSwitcher) mRootView.findViewById(R.id.camera_switcher);
        if(mSwitcher != null){
	        mSwitcher.setCurrentIndex(ModuleSwitcher.VIDEO_MODULE_INDEX);
	        mSwitcher.setSwitchListener(mActivity);
        }
        mAnimationManager = new YixiAnimationManager();
        //modify by lilei end
        initializeMiscControls();
        initializeControlByIntent();
        initializeOverlay();
        initializePauseButton();
        mOrientationResize = false;
        mPrevOrientationResize = false;
    }

    public void setCameraId(int cameraId){
    	mCameraId = cameraId;
    }
    
    private void initDropView(){
    	mDropView = (DropView)(mActivity.getLayoutInflater().inflate(R.layout.drop_view, null));
    	((ViewGroup)mRootView).addView(mDropView);
    	mDropView.setOnModuleSwitcherListener(mActivity);
    	mDropView.setIndex(ModuleSwitcher.VIDEO_MODULE_INDEX);
    	mDropView.setCameraId(mCameraId);
    	mDropView.setOnPreferenceChangedListener((OnPreferenceChangedListener)mController);
    }
    //add by lilei begin

    //add by lilei end
    public void cameraOrientationPreviewResize(boolean orientation){
       mPrevOrientationResize = mOrientationResize;
       mOrientationResize = orientation;
    }

    public void initializeSurfaceView() {
        mSurfaceView = new SurfaceView(mActivity);
        ((ViewGroup) mRootView).addView(mSurfaceView, 0);
        mSurfaceView.getHolder().addCallback(this);
    }

    private void initializeControlByIntent() {
        //modify by lilei begin
    	if(mMenuButton != null){
	        mMenuButton = mRootView.findViewById(R.id.menu);
	        mMenuButton.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View v) {
	                if (mPieRenderer != null) {
	                    mPieRenderer.showInCenter();
	                }
	            }
	        });
    	}
        //mCameraControls = (CameraControls) mRootView.findViewById(R.id.camera_controls);
        mCameraControls = (YixiCameraControls) mRootView.findViewById(R.id.yixi_camera_controls);
        if(mRootView.findViewById(R.id.on_screen_indicators) != null){
	        mOnScreenIndicators = new OnScreenIndicators(mActivity,
	                mRootView.findViewById(R.id.on_screen_indicators));
	        mOnScreenIndicators.resetToDefault();
        }
        //modify by lilei end
        if (mController.isVideoCaptureIntent()) {
            hideSwitcher();
            mActivity.getLayoutInflater().inflate(R.layout.review_module_control,
                    (ViewGroup) mCameraControls);
            // Cannot use RotateImageView for "done" and "cancel" button because
            // the tablet layout uses RotateLayout, which cannot be cast to
            // RotateImageView.
            mReviewDoneButton = mRootView.findViewById(R.id.btn_done);
            mReviewCancelButton = mRootView.findViewById(R.id.btn_cancel);
            mReviewPlayButton = mRootView.findViewById(R.id.btn_play);
            mReviewCancelButton.setVisibility(View.VISIBLE);
            mReviewDoneButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onReviewDoneClicked(v);
                }
            });
            mReviewCancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onReviewCancelClicked(v);
                }
            });
            mReviewPlayButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onReviewPlayClicked(v);
                }
            });
        }
    }

    public void setPreviewSize(int width, int height) {
        if (width == 0 || height == 0) {
            Log.w(TAG, "Preview size should not be 0.");
            return;
        }
        float ratio;
        if (width > height) {
            ratio = (float) width / height;
        } else {
            ratio = (float) height / width;
        }
        if (mOrientationResize &&
                mActivity.getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_PORTRAIT) {
            ratio = 1 / ratio;
        }

        if (ratio != mAspectRatio){
            mAspectRatioResize = true;
            mAspectRatio = ratio;
        }
        mHandler.sendEmptyMessage(UPDATE_TRANSFORM_MATRIX);
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    public void onScreenSizeChanged(int width, int height, int previewWidth, int previewHeight) {
        setTransformMatrix(width, height);
    }

    private void setTransformMatrix(int width, int height) {
        mMatrix = mTextureView.getTransform(mMatrix);
        int orientation = CameraUtil.getDisplayRotation(mActivity);
        float scaleX = 1f, scaleY = 1f;
        float scaledTextureWidth, scaledTextureHeight;
        if (mOrientationResize){
            if (width/mAspectRatio > height){
                scaledTextureHeight = height;
                scaledTextureWidth = (int)(height * mAspectRatio + 0.5f);
            } else {
                scaledTextureWidth = width;
                scaledTextureHeight = (int)(width / mAspectRatio + 0.5f);
            }
        } else {
            if (width > height) {
                scaledTextureWidth = Math.max(width, height * mAspectRatio);
                scaledTextureHeight = Math.max(height, width / mAspectRatio);
            } else {
                scaledTextureWidth = Math.max(width, height / mAspectRatio);
                scaledTextureHeight = Math.max(height, width * mAspectRatio);
            }
        }

        if (mSurfaceTextureUncroppedWidth != scaledTextureWidth ||
                mSurfaceTextureUncroppedHeight != scaledTextureHeight) {
            mSurfaceTextureUncroppedWidth = scaledTextureWidth;
            mSurfaceTextureUncroppedHeight = scaledTextureHeight;
        }
        scaleX = scaledTextureWidth / width;
        scaleY = scaledTextureHeight / height;
        mMatrix.setScale(scaleX, scaleY, (float) width / 2, (float) height / 2);
        mTextureView.setTransform(mMatrix);

        if (mSurfaceView != null && mSurfaceView.getVisibility() == View.VISIBLE) {
            LayoutParams lp = (LayoutParams) mSurfaceView.getLayoutParams();
            lp.width = (int) mSurfaceTextureUncroppedWidth;
            lp.height = (int) mSurfaceTextureUncroppedHeight;
            lp.gravity = Gravity.CENTER;
            mSurfaceView.requestLayout();
        }
    }

    /**
     * Starts a flash animation
     */
    public void animateFlash() {
        mAnimationManager.startFlashAnimation(mFlashOverlay);
    }

    /**
     * Starts a capture animation
     */
    public void animateCapture() {
        Bitmap bitmap = null;
        if (mTextureView != null) {
            bitmap = mTextureView.getBitmap((int) mSurfaceTextureUncroppedWidth / 2,
                    (int) mSurfaceTextureUncroppedHeight / 2);
        }
        animateCapture(bitmap);
    }

    /**
     * Starts a capture animation
     * @param bitmap the captured image that we shrink and slide in the animation
     */
    public void animateCapture(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "No valid bitmap for capture animation.");
            return;
        }
        ((ImageView) mPreviewThumb).setImageBitmap(bitmap);
        mAnimationManager.startCaptureAnimation(mPreviewThumb);
    }

    /**
     * Cancels on-going animations
     */
    public void cancelAnimations() {
        mAnimationManager.cancelAnimations();
    }

    public void hideUI() {
        mCameraControls.setVisibility(View.INVISIBLE);
        //modify by lilei begin
        if(mSwitcher != null)
        	mSwitcher.closePopup();
        //modify by lilei begin
    }

    public void showUI() {
        mCameraControls.setVisibility(View.VISIBLE);
    }

    public boolean arePreviewControlsVisible() {
        return (mCameraControls.getVisibility() == View.VISIBLE);
    }

    public void hideSwitcher() {
    	//modify by lilei begin
    	if(mSwitcher != null){
	        mSwitcher.closePopup();
	        mSwitcher.setVisibility(View.INVISIBLE);
    	}
    	//modify by lilei end
    }

    public void showSwitcher() {
    	//modify by lilei begin
    	if(mSwitcher != null)
    		mSwitcher.setVisibility(View.VISIBLE);
        //modify by lilei end
    }

    public boolean collapseCameraControls() {
        boolean ret = false;
        if (mPopup != null) {
            dismissPopup(false);
            ret = true;
        }
        return ret;
    }

    public boolean removeTopLevelPopup() {
        if (mPopup != null) {
            dismissPopup(true);
            return true;
        }
        return false;
    }

    public void enableCameraControls(boolean enable) {
        if (mGestures != null) {
            mGestures.setZoomOnly(!enable);
        }
        if (mPieRenderer != null && mPieRenderer.showsItems()) {
            mPieRenderer.hide();
        }
    }

    public void initDisplayChangeListener() {
        ((CameraRootView) mRootView).setDisplayChangeListener(this);
    }

    public void removeDisplayChangeListener() {
        ((CameraRootView) mRootView).removeDisplayChangeListener();
    }

    public void overrideSettings(final String... keyvalues) {
        mVideoMenu.overrideSettings(keyvalues);
    }

    public void setOrientationIndicator(int orientation, boolean animation) {
        // We change the orientation of the linearlayout only for phone UI
        // because when in portrait the width is not enough.
/*    	if (mLabelsLinearLayout != null) {
            if (((orientation / 90) & 1) == 0) {
                mLabelsLinearLayout.setOrientation(LinearLayout.VERTICAL);
            } else {
                mLabelsLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            }
        }   */
    	Log.i("lilei", "setOrientationIndicator() orientation:"+orientation+"animation:"+animation);
        mRecordingTimeRect.setOrientation(orientation, animation); //modify for AP-520 by lilei
    }
    public void setOrientationIndicator2(int orientation, boolean animation) {
        //add by lilei begin
        Rotatable[] indicators = {mPreviewThumb,mShutterButton};//mSwitcher,mColorEffectButton.getcolorPop()
        for (Rotatable indicator : indicators) {
            if (indicator != null) indicator.setOrientation(orientation, animation);
        }
        //add by lilei end
    }
    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    public void hideSurfaceView() {
        mSurfaceView.setVisibility(View.GONE);
        mTextureView.setVisibility(View.VISIBLE);
        setTransformMatrix(mPreviewWidth, mPreviewHeight);
    }

    public void showSurfaceView() {
        mSurfaceView.setVisibility(View.VISIBLE);
        mTextureView.setVisibility(View.GONE);
        setTransformMatrix(mPreviewWidth, mPreviewHeight);
    }

    private void initializeOverlay() {
        mRenderOverlay = (RenderOverlay) mRootView.findViewById(R.id.render_overlay);
        if (mPieRenderer == null) {
            mPieRenderer = new PieRenderer(mActivity);
            mVideoMenu = new VideoMenu(mActivity, this, mPieRenderer);
            mPieRenderer.setPieListener(this);
        }
        mRenderOverlay.addRenderer(mPieRenderer);
        if (mZoomRenderer == null) {
            mZoomRenderer = new ZoomRenderer(mActivity);
        }
        mRenderOverlay.addRenderer(mZoomRenderer);
        if (mGestures == null) {
            mGestures = new PreviewGestures(mActivity, this, mZoomRenderer, mPieRenderer);
            mRenderOverlay.setGestures(mGestures);
        }
        mGestures.setRenderOverlay(mRenderOverlay);
        //modify by lilei begin
        mPreviewThumb = (RotateImageView)mRootView.findViewById(R.id.preview_thumb);
        //modify by lilei end
        mPreviewThumb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do not allow navigation to filmstrip during video recording
                if (!mRecordingStarted) {
                    mActivity.gotoGallery();
                }
            }
        });
        //modify by lilei begin
        if(mLoadLatestImageTask == null)
            mLoadLatestImageTask = new LoadLatestImageTask();
        mLoadLatestImageTask.execute();
        //modify by lilei end
    }

    public void setPrefChangedListener(OnPreferenceChangedListener listener) {
        mVideoMenu.setListener(listener);
    }

    private void initializeMiscControls() {
        mReviewImage = (ImageView) mRootView.findViewById(R.id.review_image);
        mShutterButton.setImageResource(R.drawable.yixi_btn_new_shutter_video);
        mShutterButton.setOnShutterButtonListener(mController);
        mShutterButton.setVisibility(View.VISIBLE);
        mShutterButton.requestFocus();
        mShutterButton.enableTouch(true);
        mRecordingTimeView = (TextView) mRootView.findViewById(R.id.recording_time);
        //add by lilei begin 
        mRecordingIndicator = (TextView) mRootView.findViewById(R.id.recording_indicator);
        //add by lilei end
        mRecordingTimeRect = (RotateLayout) mRootView.findViewById(R.id.recording_time_rect);
        mTimeLapseLabel = mRootView.findViewById(R.id.time_lapse_label);
        // The R.id.labels can only be found in phone layout.
        // That is, mLabelsLinearLayout should be null in tablet layout.
        mLabelsLinearLayout = (RelativeLayout) mRootView.findViewById(R.id.labels);
    }

    private void initializePauseButton() {
        mPauseButton = (PauseButton) mRootView.findViewById(R.id.video_pause);
        mPauseButton.setOnPauseButtonListener(this);
    }

    public void updateOnScreenIndicators(Parameters param, ComboPreferences prefs) {
      if(mOnScreenIndicators != null){
	      mOnScreenIndicators.updateFlashOnScreenIndicator(param.getFlashMode());
	      boolean location = RecordLocationPreference.get(
	              prefs, mActivity.getContentResolver());
	      mOnScreenIndicators.updateLocationIndicator(location);
      }
    }

    public void setAspectRatio(double ratio) {
        if (mOrientationResize &&
                mActivity.getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_PORTRAIT) {
            ratio = 1 / ratio;
        }

        if (ratio != mAspectRatio){
            mAspectRatioResize = true;
            mAspectRatio = (float)ratio;
        }
        mHandler.sendEmptyMessage(UPDATE_TRANSFORM_MATRIX);

    }

    public void showTimeLapseUI(boolean enable) {
        if (mTimeLapseLabel != null) {
            mTimeLapseLabel.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
        mIsTimeLapse = enable;
    }

    private void openMenu() {
        if (mPieRenderer != null) {
            mPieRenderer.showInCenter();
        }
    }

    public void dismissPopup(boolean topLevelOnly) {
        // In review mode, we do not want to bring up the camera UI
        if (mController.isInReviewMode()) return;
        if (mPopup != null) {
            mPopup.dismiss(topLevelOnly);
        }
    }

    private void popupDismissed() {
        mPopup = null;
    }

    public void showPopup(AbstractSettingPopup popup) {
        hideUI();

        if (mPopup != null) {
            mPopup.dismiss(false);
        }
        mPopup = new SettingsPopup(popup);
    }

    public void onShowSwitcherPopup() {
        hidePieRenderer();
    }

    public boolean hidePieRenderer() {
        if (mPieRenderer != null && mPieRenderer.showsItems()) {
            mPieRenderer.hide();
            return true;
        }
        return false;
    }

    // disable preview gestures after shutter is pressed
    public void setShutterPressed(boolean pressed) {
        if (mGestures == null) return;
        mGestures.setEnabled(!pressed);
    }

    public void enableShutter(boolean enable) {
        if (mShutterButton != null) {
            mShutterButton.setEnabled(enable);
        }
    }

    // PieListener
    @Override
    public void onPieOpened(int centerX, int centerY) {
        setSwipingEnabled(false);
        // Close module selection menu when pie menu is opened.
        //modify by lilei begin
        if(mSwitcher != null)
        	mSwitcher.closePopup();
        //modify by lilei end
    }

    @Override
    public void onPieClosed() {
        setSwipingEnabled(true);
    }

    public void setSwipingEnabled(boolean enable) {
        mActivity.setSwipingEnabled(enable);
    }

    public void showPreviewBorder(boolean enable) {
       // TODO: mPreviewFrameLayout.showBorder(enable);
    }

    // SingleTapListener
    // Preview area is touched. Take a picture.
    @Override
    public void onSingleTapUp(View view, int x, int y) {
        mController.onSingleTapUp(view, x, y);
    }

    public void showRecordingUI(boolean recording) {
        mRecordingStarted = recording;
        //modify by lilei begin
        if(mMenuButton != null)
        	mMenuButton.setVisibility(recording ? View.GONE : View.VISIBLE);
        if(mOnScreenIndicators != null)
        	mOnScreenIndicators.setVisibility(recording ? View.GONE : View.VISIBLE);
        mYixiSetMoreButton.setVisibility(recording ? View.GONE : View.VISIBLE);
        //modify by lilei end
        mDropView.setVisibility(recording ? View.GONE : View.VISIBLE);
        if (recording) {
            mShutterButton.setImageResource(R.drawable.yixi_btn_shutter_video_recording);
            hideSwitcher();
            mRecordingTimeView.setText("");
            mRecordingTimeView.setVisibility(View.VISIBLE);
            mRecordingIndicator.setVisibility(View.VISIBLE);  //add by lilei
            mPauseButton.setVisibility(mIsTimeLapse ? View.GONE : View.VISIBLE);
        } else {
            mShutterButton.setImageResource(R.drawable.yixi_btn_new_shutter_video);
            if (!mController.isVideoCaptureIntent()) {
                showSwitcher();
            }
            mRecordingTimeView.setVisibility(View.GONE);
            //add by lilei begin
            if(mRecordingIndicator.getVisibility() == View.VISIBLE)
            	mRecordingIndicator.setVisibility(View.GONE);
            //add by lilei end
            mPauseButton.setVisibility(View.GONE);
        }
    }

    public void showReviewImage(Bitmap bitmap) {
        mReviewImage.setImageBitmap(bitmap);
        mReviewImage.setVisibility(View.VISIBLE);
    }

    public void showReviewControls() {
        CameraUtil.fadeOut(mShutterButton);
        CameraUtil.fadeIn(mReviewDoneButton);
        CameraUtil.fadeIn(mReviewPlayButton);
        mReviewImage.setVisibility(View.VISIBLE);
        //modify by lilei begin
        if(mMenuButton != null)
        	mMenuButton.setVisibility(View.GONE);
        if(mOnScreenIndicators != null)
        	mOnScreenIndicators.setVisibility(View.GONE);
        //modify by lilei end
    }

    public void hideReviewUI() {
        mReviewImage.setVisibility(View.GONE);
        mShutterButton.setEnabled(true);
        //modify by lilei begin
        if(mMenuButton != null)
        	mMenuButton.setVisibility(View.VISIBLE);
        if(mOnScreenIndicators != null)
        	mOnScreenIndicators.setVisibility(View.VISIBLE);
        //modify by lilei end
        CameraUtil.fadeOut(mReviewDoneButton);
        CameraUtil.fadeOut(mReviewPlayButton);
        CameraUtil.fadeIn(mShutterButton);
    }

    private void setShowMenu(boolean show) {
        if (mController.isVideoCaptureIntent())
            return;
        //modify by lilei begin
        if (mOnScreenIndicators != null) {
            mOnScreenIndicators.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (mMenuButton != null) {
            mMenuButton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        //modify by lilei end
    }

    public void onPreviewFocusChanged(boolean previewFocused) {
        if (previewFocused) {
            showUI();
        } else {
            hideUI();
        }
        if (mGestures != null) {
            mGestures.setEnabled(previewFocused);
        }
        if (mRenderOverlay != null) {
            // this can not happen in capture mode
            mRenderOverlay.setVisibility(previewFocused ? View.VISIBLE : View.GONE);
        }
        setShowMenu(previewFocused);
    }

    public void initializePopup(PreferenceGroup pref) {
        mVideoMenu.initialize(pref);
    }
	public void initDropPreference(PreferenceGroup pref){
    	mDropView.setPreferenceGroup(pref);
    }

    public void initializeZoom(Parameters param) {
        if (param == null || !param.isZoomSupported()) {
            mGestures.setZoomEnabled(false);
            return;
        }
        mGestures.setZoomEnabled(true);
        mZoomMax = param.getMaxZoom();
        mZoomRatios = param.getZoomRatios();
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        mZoomRenderer.setZoomMax(mZoomMax);
        mZoomRenderer.setZoom(param.getZoom());
        mZoomRenderer.setZoomValue(mZoomRatios.get(param.getZoom()));
        mZoomRenderer.setOnZoomChangeListener(new ZoomChangeListener());
    }

    public void clickShutter() {
        mShutterButton.performClick();
    }

    public void pressShutter(boolean pressed) {
        mShutterButton.setPressed(pressed);
    }

    public View getShutterButton() {
        return mShutterButton;
    }

    public void setRecordingTime(String text) {
        mRecordingTimeView.setText(text);
    }

    public void setRecordingTimeTextColor(int color) {
        mRecordingTimeView.setTextColor(color);
    }

    public boolean isVisible() {
        return mCameraControls.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onDisplayChanged() {
        mCameraControls.checkLayoutFlip();
        mController.updateCameraOrientation();
    }

    private class ZoomChangeListener implements ZoomRenderer.OnZoomChangedListener {
        @Override
        public void onZoomValueChanged(int index) {
            int newZoom = mController.onZoomChanged(index);
            if (mZoomRenderer != null) {
                mZoomRenderer.setZoomValue(mZoomRatios.get(newZoom));
            }
        }

        @Override
        public void onZoomStart() {
            if (mPieRenderer != null) {
                if (!mRecordingStarted) mPieRenderer.hide();
                mPieRenderer.setBlockFocus(true);
            }
        }

        @Override
        public void onZoomEnd() {
            if (mPieRenderer != null) {
                mPieRenderer.setBlockFocus(false);
            }
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    // SurfaceTexture callbacks
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurfaceTexture = surface;
        mController.onPreviewUIReady();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurfaceTexture = null;
        mController.onPreviewUIDestroyed();
        Log.d(TAG, "surfaceTexture is destroyed");
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Make sure preview cover is hidden if preview data is available.
        if (mPreviewCover.getVisibility() != View.GONE) {
            mPreviewCover.setVisibility(View.GONE);
        }
    }

    // SurfaceHolder callbacks
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG, "Surface changed. width=" + width + ". height=" + height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "Surface created");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "Surface destroyed");
        mController.stopPreview();
    }

     @Override
    public void onButtonPause() {
         //add by lilei begin
         if(mRecordingIndicator.getVisibility() == View.VISIBLE)
         	mRecordingIndicator.setVisibility(View.GONE);
         //add by lilei end
        mRecordingTimeView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.yixi_ic_recording_indicator, 0, 0, 0);
                mController.onButtonPause();
    }

    @Override
    public void onButtonContinue() {
        //add by lilei begin
        mRecordingIndicator.setVisibility(View.VISIBLE);
        //add by lilei end
        mRecordingTimeView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.yixi_ic_recording_indicator, 0, 0, 0);
        mController.onButtonContinue();
    }

    public void resetPauseButton() {
        //add by lilei begin
        mRecordingIndicator.setVisibility(View.VISIBLE);
        //add by lilei end
        mRecordingTimeView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.yixi_ic_recording_indicator, 0, 0, 0);
        mPauseButton.setPaused(false);
    }

    public void setPreference(String key, String value) {
        mVideoMenu.setPreference(key, value);
    }
}

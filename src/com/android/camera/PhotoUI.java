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


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.android.camera.CameraPreference.OnPreferenceChangedListener;
import com.android.camera.FocusOverlayManager.FocusUI;
import com.android.camera.SurfaceTextureRenderer.FrameDrawer;
import com.android.camera.data.LocalData;
import com.android.camera.data.LocalMediaData;
import com.android.camera.data.LocalMediaData.PhotoData;
import com.android.camera.ui.AbstractSettingPopup;
import com.android.camera.ui.CameraControls;
import com.android.camera.ui.CameraRootView;
import com.android.camera.ui.CountDownView;
import com.android.camera.ui.CountDownView.OnCountDownFinishedListener;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.FocusIndicator;
import com.android.camera.ui.ModuleSwitcher;
import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.PieRenderer.PieListener;
import com.android.camera.ui.RenderOverlay;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.YixiCameraControls;
import com.android.camera.ui.YixiColorEffectButton;
import com.android.camera.ui.YixiColorEffectSettingPopup;
import com.android.camera.ui.YixiSetMoreButton;
import com.android.camera.ui.YixiShutterButton;
import com.android.camera.ui.ZoomRenderer;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.android.camera.ui.DropView ;

import java.util.List;

public class PhotoUI implements PieListener,
    PreviewGestures.SingleTapListener,
    FocusUI, TextureView.SurfaceTextureListener,
    LocationManager.Listener, CameraRootView.MyDisplayListener,
    CameraManager.CameraFaceDetectionCallback {

    private static final String TAG = "CAM_UI";
    private int mDownSampleFactor = 4;
    private CameraActivity mActivity;
    private PreviewGestures mGestures;

    private View mRootView;
    private SurfaceTexture mSurfaceTexture;

    private PopupWindow mPopup;
    private CountDownView mCountDownView;

    private FaceView mFaceView;
    private RenderOverlay mRenderOverlay;
    private View mReviewCancelButton;
    private View mReviewDoneButton;
    private View mReviewRetakeButton;
    private ImageView mReviewImage;
    private DecodeImageForReview mDecodeTaskForReview = null;

    private View mMenuButton;
    private PhotoMenu mMenu;
    //modify by lilei begin
    private final YixiAnimationManager mAnimationManager; //modify by lilei
    //private PhotoController mController;
    //private ImageView mPreviewThumb;
    //private CameraControls mCameraControls;
    //private ShutterButton mShutterButton;
    private YixiPhotoController mController;
    private RotateImageView mPreviewThumb;
    private YixiShutterButton mShutterButton;
    private YixiCameraControls mCameraControls;
    private ModuleSwitcher mSwitcher;
    private YixiColorEffectButton mColorEffectButton;
    private FrameLayout mColorEffectPopFrame;
    private YixiSetMoreButton mYixiSetMoreButton;
    private View mSetMoreView;
    private LoadLatestImageTask mLoadLatestImageTask;
    //modify by lilei end
    private AlertDialog mLocationDialog;

    // Small indicators which show the camera settings in the viewfinder.
    private OnScreenIndicators mOnScreenIndicators;

    private PieRenderer mPieRenderer;
    private ZoomRenderer mZoomRenderer;
    private Toast mNotSelectableToast;

    private int mZoomMax;
    private List<Integer> mZoomRatios;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    public boolean mMenuInitialized = false;
    private float mSurfaceTextureUncroppedWidth;
    private float mSurfaceTextureUncroppedHeight;

    private View mFlashOverlay;

    private SurfaceTextureSizeChangedListener mSurfaceTextureSizeListener;
    private TextureView mTextureView;
    private Matrix mMatrix = null;
    private float mAspectRatio = 4f / 3f;
    private boolean mAspectRatioResize;

    private boolean mOrientationResize;
    private boolean mPrevOrientationResize;
    private View mPreviewCover;
    private final Object mSurfaceTextureLock = new Object();
	private DropView mDropView ;
    private int mCameraId ;
    //add by lilei begin
    public interface DismissListener{
        public void dismissPop();
    }
    //add by lilei end
	
    public interface SurfaceTextureSizeChangedListener {
        public void onSurfaceTextureSizeChanged(int uncroppedWidth, int uncroppedHeight);
    }

    private OnLayoutChangeListener mLayoutListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            int width = right - left;
            int height = bottom - top;
            if (mPreviewWidth != width || mPreviewHeight != height
                    || (mOrientationResize != mPrevOrientationResize)
                    || mAspectRatioResize) {
                mPreviewWidth = width;
                mPreviewHeight = height;
                setTransformMatrix(width, height);
                mController.onScreenSizeChanged((int) mSurfaceTextureUncroppedWidth,
                        (int) mSurfaceTextureUncroppedHeight);
                mAspectRatioResize = false;
            }
        }
    };

    private class DecodeTask extends AsyncTask<Void, Void, Bitmap> {
        private final byte [] mData;
        private int mOrientation;
        private boolean mMirror;

        public DecodeTask(byte[] data, int orientation, boolean mirror) {
            mData = data;
            mOrientation = orientation;
            mMirror = mirror;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            // Decode image in background.
            Bitmap bitmap = CameraUtil.downSample(mData, mDownSampleFactor);
            if ((mOrientation != 0 || mMirror) && (bitmap != null)) {
                Matrix m = new Matrix();
                if (mMirror) {
                    // Flip horizontally
                    m.setScale(-1f, 1f);
                }
                m.preRotate(mOrientation);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m,
                        false);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPreviewThumb.setImageBitmap(bitmap);
            mAnimationManager.startCaptureAnimation(mPreviewThumb);
        }
    }

    private class DecodeImageForReview extends DecodeTask {
        public DecodeImageForReview(byte[] data, int orientation, boolean mirror) {
            super(data, orientation, mirror);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                return;
            }
            mReviewImage.setImageBitmap(bitmap);
            mReviewImage.setVisibility(View.VISIBLE);
            mDecodeTaskForReview = null;
        }
    }

    public PhotoUI(CameraActivity activity, YixiPhotoController controller, View parent) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;
        //modify by lilei begin
//        mActivity.getLayoutInflater().inflate(R.layout.photo_module,
//                (ViewGroup) mRootView, true);
        mActivity.getLayoutInflater().inflate(R.layout.yixi_photo_module,
                (ViewGroup) mRootView, true);
        initSetMoreView();
        //modify by lilei end
		initDropView();
        mRenderOverlay = (RenderOverlay) mRootView.findViewById(R.id.render_overlay);
        mFlashOverlay = mRootView.findViewById(R.id.flash_overlay);
        mPreviewCover = mRootView.findViewById(R.id.preview_cover);
        // display the view
        mTextureView = (TextureView) mRootView.findViewById(R.id.preview_content);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.addOnLayoutChangeListener(mLayoutListener);
        initIndicators();

        //modify by lilei begin
        mShutterButton = (YixiShutterButton) mRootView.findViewById(R.id.shutter_button);
        mColorEffectButton = (YixiColorEffectButton) mRootView.findViewById(R.id.btn_color_effect);
        mColorEffectPopFrame = (FrameLayout)mRootView.findViewById(R.id.color_effect_pop_frame);
        mSwitcher = (ModuleSwitcher) mRootView.findViewById(R.id.camera_switcher);
        if(mSwitcher != null){
            mSwitcher.setCurrentIndex(ModuleSwitcher.PHOTO_MODULE_INDEX);
            mSwitcher.setSwitchListener(mActivity);
        }
        //mCameraControls = (CameraControls) mRootView.findViewById(R.id.camera_controls);
        mCameraControls = (YixiCameraControls) mRootView.findViewById(R.id.yixi_camera_controls);
        mMenuButton = mRootView.findViewById(R.id.menu);
        mAnimationManager = new YixiAnimationManager();
        //modify by lilei end
        ViewStub faceViewStub = (ViewStub) mRootView
                .findViewById(R.id.face_view_stub);
        if (faceViewStub != null) {
            faceViewStub.inflate();
            mFaceView = (FaceView) mRootView.findViewById(R.id.face_view);
            setSurfaceTextureSizeChangedListener(mFaceView);
        }

        mOrientationResize = false;
        mPrevOrientationResize = false;
    }
    //add by lilei begin
    public void setOrientationIndicator(int orientation, boolean animation) {
        Rotatable[] indicators = {mPreviewThumb,mShutterButton,mColorEffectButton,mYixiSetMoreButton};//mSwitcher,mColorEffectButton.getcolorPop()
         for (Rotatable indicator : indicators) {
             if (indicator != null) indicator.setOrientation(orientation, animation);
         }
    }
	private void initDropView(){
    	mDropView = (DropView)(mActivity.getLayoutInflater().inflate(R.layout.drop_view, null));
    	((ViewGroup)mRootView).addView(mDropView);
    	mDropView.setOnModuleSwitcherListener(mActivity);
    	mDropView.setIndex(ModuleSwitcher.PHOTO_MODULE_INDEX);
    	mDropView.setCameraId(mCameraId);
    	mDropView.setOnPreferenceChangedListener((OnPreferenceChangedListener)mController);
    }
	private void initSetMoreView(){
		mSetMoreView = (mActivity.getLayoutInflater().inflate(R.layout.yixi_set_more_view, null));
		if(mSetMoreView != null){
			((ViewGroup)mRootView).addView(mSetMoreView);
			mYixiSetMoreButton = (YixiSetMoreButton)mSetMoreView.findViewById(R.id.set_more_button);
			
		}
	}
	public void setCameraId(int cameraId){
    	mCameraId = cameraId;
    }
    public YixiColorEffectSettingPopup.Listener getColorEffectSetListener(){
        return mColorEffectButton.getColorEffectSetListener();
    }
    //add by lilei end
    public void setDownFactor(int factor) {
        mDownSampleFactor = factor;
    }

     public void cameraOrientationPreviewResize(boolean orientation){
        mPrevOrientationResize = mOrientationResize;
        mOrientationResize = orientation;
     }

    public void setAspectRatio(float ratio) {
        if (ratio <= 0.0) throw new IllegalArgumentException();

        if (mOrientationResize &&
                mActivity.getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_PORTRAIT) {
            ratio = 1 / ratio;
        }

        Log.d(TAG,"setAspectRatio() ratio["+ratio+"] mAspectRatio["+mAspectRatio+"]");
        mAspectRatio = ratio;
        mAspectRatioResize = true;
        mTextureView.requestLayout();
    }

    public void setSurfaceTextureSizeChangedListener(SurfaceTextureSizeChangedListener listener) {
        mSurfaceTextureSizeListener = listener;
    }

    private void setTransformMatrix(int width, int height) {
        mMatrix = mTextureView.getTransform(mMatrix);
        float scaleX = 1f, scaleY = 1f;
        float scaledTextureWidth, scaledTextureHeight;
        if (mOrientationResize){
            scaledTextureWidth = height * mAspectRatio;
            if(scaledTextureWidth > width){
                scaledTextureWidth = width;
                scaledTextureHeight = scaledTextureWidth / mAspectRatio;
            } else {
                scaledTextureHeight = height;
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
            if (mSurfaceTextureSizeListener != null) {
                mSurfaceTextureSizeListener.onSurfaceTextureSizeChanged(
                        (int) mSurfaceTextureUncroppedWidth, (int) mSurfaceTextureUncroppedHeight);
            }
        }
        scaleX = scaledTextureWidth / width;
        scaleY = scaledTextureHeight / height;
        mMatrix.setScale(scaleX, scaleY, (float) width / 2, (float) height / 2);
        mTextureView.setTransform(mMatrix);

        // Calculate the new preview rectangle.
        RectF previewRect = new RectF(0, 0, width, height);
        mMatrix.mapRect(previewRect);
        mController.onPreviewRectChanged(CameraUtil.rectFToRect(previewRect));
    }

    protected Object getSurfaceTextureLock() {
        return mSurfaceTextureLock;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        synchronized (mSurfaceTextureLock) {
            Log.v(TAG, "SurfaceTexture ready.");
            mSurfaceTexture = surface;
            mController.onPreviewUIReady();
            // Workaround for b/11168275, see b/10981460 for more details
            if (mPreviewWidth != 0 && mPreviewHeight != 0) {
                // Re-apply transform matrix for new surface texture
                setTransformMatrix(mPreviewWidth, mPreviewHeight);
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        synchronized (mSurfaceTextureLock) {
            mSurfaceTexture = null;
            mController.onPreviewUIDestroyed();
            Log.w(TAG, "SurfaceTexture destroyed");
            return true;
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Make sure preview cover is hidden if preview data is available.
        if (mPreviewCover.getVisibility() != View.GONE) {
            mPreviewCover.setVisibility(View.GONE);
        }
    }

    public View getRootView() {
        return mRootView;
    }

    private void initIndicators() {
        //modify by lilei begin
        if(mRootView.findViewById(R.id.on_screen_indicators) != null){
            mOnScreenIndicators = new OnScreenIndicators(mActivity,
                    mRootView.findViewById(R.id.on_screen_indicators));
        }
        //modify by lilei end
    }

    public void onCameraOpened(PreferenceGroup prefGroup, ComboPreferences prefs,
            Camera.Parameters params, OnPreferenceChangedListener listener) {
        if (mPieRenderer == null) {
            mPieRenderer = new PieRenderer(mActivity);
            mPieRenderer.setPieListener(this);
            mRenderOverlay.addRenderer(mPieRenderer);
        }

        if (mMenu == null) {
            mMenu = new PhotoMenu(mActivity, this, mPieRenderer);
            mMenu.setListener(listener);
        }
        //add by lilei begin
        int orientation =mActivity.getResources().getConfiguration().orientation;
        if(mColorEffectButton != null){
            IconListPreference colorIconPref = (IconListPreference)prefGroup.findPreference(CameraSettings.KEY_COLOR_EFFECT);
            FrameLayout cameraRooot = (FrameLayout)mActivity.findViewById(R.id.camera_layout_root);
            mColorEffectButton.setListener(listener,colorIconPref,mColorEffectPopFrame);
            //mColorEffectButton.setOrientation(orientation, false);
        }
        if(mYixiSetMoreButton != null){
        	mYixiSetMoreButton.setListener(listener,mCameraId, mActivity, params,mColorEffectPopFrame);
        }
        //add by lilei end
        mMenu.initialize(prefGroup);
        mDropView.setPreferenceGroup(prefGroup);
        mMenuInitialized = true;

        if (mZoomRenderer == null) {
            mZoomRenderer = new ZoomRenderer(mActivity);
            mRenderOverlay.addRenderer(mZoomRenderer);
        }

        if (mGestures == null) {
            // this will handle gesture disambiguation and dispatching
            mGestures = new PreviewGestures(mActivity, this, mZoomRenderer, mPieRenderer);
            mRenderOverlay.setGestures(mGestures);
        }
        mGestures.setZoomEnabled(params.isZoomSupported());
        mGestures.setRenderOverlay(mRenderOverlay);
        mRenderOverlay.requestLayout();

        initializeZoom(params);
        updateOnScreenIndicators(params, prefGroup, prefs);
    }

    public void animateCapture(final byte[] jpegData, int orientation, boolean mirror) {
        // Decode jpeg byte array and then animate the jpeg
        DecodeTask task = new DecodeTask(jpegData, orientation, mirror);
        task.execute();
    }

    private void openMenu() {
        if (mPieRenderer != null) {
            // If autofocus is not finished, cancel autofocus so that the
            // subsequent touch can be handled by PreviewGestures
            if (mController.getCameraState() == PhotoController.FOCUSING) {
                    mController.cancelAutoFocus();
            }
            mPieRenderer.showInCenter();
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
    public void initializeControlByIntent() {
        //modify by lilei begin
        //mPreviewThumb = (ImageView) mRootView.findViewById(R.id.preview_thumb);
        mPreviewThumb = (RotateImageView) mRootView.findViewById(R.id.preview_thumb);
        mPreviewThumb.setVisibility(View.INVISIBLE);
        if(mLoadLatestImageTask == null)
            mLoadLatestImageTask = new LoadLatestImageTask();
        mLoadLatestImageTask.execute();
        //modify by lilei end
        mPreviewThumb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.gotoGallery();
            }
        });
        //modify by lilei begin
        mMenuButton = mRootView.findViewById(R.id.menu);
        if(mMenuButton != null){
            mMenuButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    openMenu();
                }
            });
        }
        //modify by lilei end
        if (mController.isImageCaptureIntent()) {
            hideSwitcher();
            //modify by lilei begin
            //ViewGroup cameraControls = (ViewGroup) mRootView.findViewById(R.id.camera_controls);
            ViewGroup cameraControls = (ViewGroup) mRootView.findViewById(R.id.yixi_camera_controls);
            //modify by lilei end
            mActivity.getLayoutInflater().inflate(R.layout.review_module_control, cameraControls);

            mReviewDoneButton = mRootView.findViewById(R.id.btn_done);
            mReviewCancelButton = mRootView.findViewById(R.id.btn_cancel);
            mReviewRetakeButton = mRootView.findViewById(R.id.btn_retake);
            mReviewImage = (ImageView) mRootView.findViewById(R.id.review_image);
            mReviewCancelButton.setVisibility(View.VISIBLE);

            mReviewDoneButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onCaptureDone();
                }
            });
            mReviewCancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onCaptureCancelled();
                }
            });

            mReviewRetakeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onCaptureRetake();
                }
            });
        }
    }

    public void hideUI() {
        mCameraControls.setVisibility(View.INVISIBLE);
        //modify by lilei begin
        if(mSwitcher != null){
            mSwitcher.closePopup();
        }
        //modify by lilei end
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
        if(mSwitcher != null){
            mSwitcher.setVisibility(View.VISIBLE);
        }
        //modify by lilei end
    }
	public void setDropViewEnabled(boolean enable){
    	mDropView.setEnabled(enable);
    }
    // called from onResume but only the first time
    public  void initializeFirstTime() {
        // Initialize shutter button.
        mShutterButton.setImageResource(R.drawable.yixi_btn_new_shutter);
        mShutterButton.setOnShutterButtonListener(mController);
        mShutterButton.setVisibility(View.VISIBLE);
    }

    // called from onResume every other time
    public void initializeSecondTime(Camera.Parameters params) {
        initializeZoom(params);
        if (mController.isImageCaptureIntent()) {
            hidePostCaptureAlert();
        }
        if (mMenu != null) {
            mMenu.reloadPreferences();
        }
    }

    public void showLocationDialog() {
        mLocationDialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.remember_location_title)
                .setMessage(R.string.remember_location_prompt)
                .setPositiveButton(R.string.remember_location_yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int arg1) {
                                mController.enableRecordingLocation(true);
                                mLocationDialog = null;
                            }
                        })
                .setNegativeButton(R.string.remember_location_no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int arg1) {
                                dialog.cancel();
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mController.enableRecordingLocation(false);
                        mLocationDialog = null;
                    }
                })
                .show();
    }

    public void initializeZoom(Camera.Parameters params) {
        if ((params == null) || !params.isZoomSupported()
                || (mZoomRenderer == null)) return;
        mZoomMax = params.getMaxZoom();
        mZoomRatios = params.getZoomRatios();
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        if (mZoomRenderer != null) {
            mZoomRenderer.setZoomMax(mZoomMax);
            mZoomRenderer.setZoom(params.getZoom());
            mZoomRenderer.setZoomValue(mZoomRatios.get(params.getZoom()));
            mZoomRenderer.setOnZoomChangeListener(new ZoomChangeListener());
        }
    }

    @Override
    public void showGpsOnScreenIndicator(boolean hasSignal) { }

    @Override
    public void hideGpsOnScreenIndicator() { }

    public void overrideSettings(final String ... keyvalues) {
        if (mMenu == null) return;
        mMenu.overrideSettings(keyvalues);
    }

    public void updateOnScreenIndicators(Camera.Parameters params,
            PreferenceGroup group, ComboPreferences prefs) {
        if (params == null || group == null) return;
        //modify by lilei begin
        if(mOnScreenIndicators != null){
                mOnScreenIndicators.updateSceneOnScreenIndicator(params.getSceneMode());
                mOnScreenIndicators.updateExposureOnScreenIndicator(params,
                        CameraSettings.readExposure(prefs));
                mOnScreenIndicators.updateFlashOnScreenIndicator(params.getFlashMode());
                int wbIndex = -1;
                String wb = Camera.Parameters.WHITE_BALANCE_AUTO;
                if (Camera.Parameters.SCENE_MODE_AUTO.equals(params.getSceneMode())) {
                    wb = params.getWhiteBalance();
                }
                ListPreference pref = group.findPreference(CameraSettings.KEY_WHITE_BALANCE);
                if (pref != null) {
                    wbIndex = pref.findIndexOfValue(wb);
                }
                // make sure the correct value was found
                // otherwise use auto index
                mOnScreenIndicators.updateWBIndicator(wbIndex < 0 ? 2 : wbIndex);
                boolean location = RecordLocationPreference.get(
                        prefs, mActivity.getContentResolver());
                mOnScreenIndicators.updateLocationIndicator(location);
        }
        //modify by lilei end
    }

    public void setCameraState(int state) {
    }

    public void animateFlash() {
        mAnimationManager.startFlashAnimation(mFlashOverlay);
    }

    public void enableGestures(boolean enable) {
        if (mGestures != null) {
            mGestures.setEnabled(enable);
        }
    }

    // forward from preview gestures to controller
    @Override
    public void onSingleTapUp(View view, int x, int y) {
        mController.onSingleTapUp(view, x, y);
    }

    public boolean onBackPressed() {
        if (mPieRenderer != null && mPieRenderer.showsItems()) {
            mPieRenderer.hide();
            return true;
        }
        // In image capture mode, back button should:
        // 1) if there is any popup, dismiss them, 2) otherwise, get out of
        // image capture
        if (mController.isImageCaptureIntent()) {
            mController.onCaptureCancelled();
            return true;
        } else if (!mController.isCameraIdle()) {
            // ignore backs while we're taking a picture
            return true;
        } else {
            return false;
        }
    }

    public void onPreviewFocusChanged(boolean previewFocused) {
        if (previewFocused) {
            showUI();
        } else {
            hideUI();
        }
        if (mFaceView != null) {
            mFaceView.setBlockDraw(!previewFocused);
        }
        if (mGestures != null) {
            mGestures.setEnabled(previewFocused);
        }
        if (mRenderOverlay != null) {
            // this can not happen in capture mode
            mRenderOverlay.setVisibility(previewFocused ? View.VISIBLE : View.GONE);
        }
        if (mPieRenderer != null) {
            mPieRenderer.setBlockFocus(!previewFocused);
        }
        setShowMenu(previewFocused);
        if (!previewFocused && mCountDownView != null) mCountDownView.cancelCountDown();
    }

    public void showPopup(AbstractSettingPopup popup) {
        hideUI();

        if (mPopup == null) {
            mPopup = new PopupWindow(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mPopup.setOutsideTouchable(true);
            mPopup.setFocusable(true);
            mPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    mPopup = null;
                    mMenu.popupDismissed(mDismissAll);
                    mDismissAll = false;
                    showUI();

                    // Switch back into fullscreen/lights-out mode after popup
                    // is dimissed.
                    mActivity.setSystemBarsVisibility(false);
                }
            });
        }
        popup.setVisibility(View.VISIBLE);
        mPopup.setContentView(popup);
        mPopup.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
    }

    public void dismissPopup() {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }

    private boolean mDismissAll = false;
    public void dismissAllPopup() {
        mDismissAll = true;
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }

    public void onShowSwitcherPopup() {
        if (mPieRenderer != null && mPieRenderer.showsItems()) {
            mPieRenderer.hide();
        }
    }

    private void setShowMenu(boolean show) {
        if (mOnScreenIndicators != null) {
            mOnScreenIndicators.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (mMenuButton != null) {
            mMenuButton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public boolean collapseCameraControls() {
        // TODO: Mode switcher should behave like a popup and should hide itself when there
        // is a touch outside of it.
        //modify by lilei begin
        if(mSwitcher != null){
            mSwitcher.closePopup();
        }
        //modify by lilei end
        // Remove all the popups/dialog boxes
        boolean ret = false;
        if (mPopup != null) {
            dismissAllPopup();
            ret = true;
        }
        onShowSwitcherPopup();
        return ret;
    }

    protected void showCapturedImageForReview(byte[] jpegData, int orientation, boolean mirror) {
        mDecodeTaskForReview = new DecodeImageForReview(jpegData, orientation, mirror);
        mDecodeTaskForReview.execute();
        //modify by lilei begin
        if(mOnScreenIndicators != null){
            mOnScreenIndicators.setVisibility(View.GONE);
        }
        if(mMenuButton != null){
            mMenuButton.setVisibility(View.GONE);
        }
        //modify by lilei end
        CameraUtil.fadeIn(mReviewDoneButton);
        mShutterButton.setVisibility(View.INVISIBLE);
        CameraUtil.fadeIn(mReviewRetakeButton);
        hideSwitcher();  //add for AP-109 by lilei 
        pauseFaceDetection();
    }

    protected void hidePostCaptureAlert() {
        if (mDecodeTaskForReview != null) {
            mDecodeTaskForReview.cancel(true);
        }
        mReviewImage.setVisibility(View.GONE);
        //modify by lilei begin
        if(mOnScreenIndicators != null){
            mOnScreenIndicators.setVisibility(View.VISIBLE);
        }
        if(mMenuButton != null){
            mMenuButton.setVisibility(View.VISIBLE);
        }
        //modify by lilei end
        CameraUtil.fadeOut(mReviewDoneButton);
        mShutterButton.setVisibility(View.VISIBLE);
        CameraUtil.fadeOut(mReviewRetakeButton);
        resumeFaceDetection();
    }

    public void setDisplayOrientation(int orientation) {
        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(orientation);
        }
    }

    // shutter button handling

    public boolean isShutterPressed() {
        return mShutterButton.isPressed();
    }

    /**
     * Enables or disables the shutter button.
     */
    public void enableShutter(boolean enabled) {
        if (mShutterButton != null) {
            mShutterButton.setEnabled(enabled);
        }
    }

    public void pressShutterButton() {
        if (mShutterButton.isInTouchMode()) {
            mShutterButton.requestFocusFromTouch();
        } else {
            mShutterButton.requestFocus();
        }
        mShutterButton.setPressed(true);
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
                mPieRenderer.hide();
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

    @Override
    public void onPieOpened(int centerX, int centerY) {
        setSwipingEnabled(false);
        if (mFaceView != null) {
            mFaceView.setBlockDraw(true);
        }
        // Close module selection menu when pie menu is opened.
        //modify by lilei begin
        if(mSwitcher != null){
            mSwitcher.closePopup();
        }
        //modify by lilei end
    }

    @Override
    public void onPieClosed() {
        setSwipingEnabled(true);
        if (mFaceView != null) {
            mFaceView.setBlockDraw(false);
        }
    }

    public void setSwipingEnabled(boolean enable) {
        mActivity.setSwipingEnabled(enable);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    // Countdown timer

    private void initializeCountDown() {
        mActivity.getLayoutInflater().inflate(R.layout.count_down_to_capture,
                (ViewGroup) mRootView, true);
        mCountDownView = (CountDownView) (mRootView.findViewById(R.id.count_down_to_capture));
        mCountDownView.setCountDownFinishedListener((OnCountDownFinishedListener) mController);
        mCountDownView.bringToFront();
    }

    public boolean isCountingDown() {
        return mCountDownView != null && mCountDownView.isCountingDown();
    }

    public void cancelCountDown() {
        if (mCountDownView == null) return;
        mCountDownView.cancelCountDown();
    }

    public void startCountDown(int sec, boolean playSound) {
        if (mCountDownView == null) initializeCountDown();
        mCountDownView.startCountDown(sec, playSound);
    }

    public void showPreferencesToast() {
        if (mNotSelectableToast == null) {
            String str = mActivity.getResources().getString(R.string.not_selectable_in_scene_mode);
            mNotSelectableToast = Toast.makeText(mActivity, str, Toast.LENGTH_SHORT);
        }
        mNotSelectableToast.show();
    }

    public void showPreviewCover() {
        mPreviewCover.setVisibility(View.VISIBLE);
    }

    public void onPause() {
        cancelCountDown();

        // Clear UI.
        collapseCameraControls();
        if (mFaceView != null) mFaceView.clear();

        if (mLocationDialog != null && mLocationDialog.isShowing()) {
            mLocationDialog.dismiss();
        }
        mLocationDialog = null;
    }

    public void initDisplayChangeListener() {
        ((CameraRootView) mRootView).setDisplayChangeListener(this);
    }

    public void removeDisplayChangeListener() {
        ((CameraRootView) mRootView).removeDisplayChangeListener();
    }

    // focus UI implementation

    private FocusIndicator getFocusIndicator() {
        return (mFaceView != null && mFaceView.faceExists()) ? mFaceView : mPieRenderer;
    }

    @Override
    public boolean hasFaces() {
        return (mFaceView != null && mFaceView.faceExists());
    }

    public void clearFaces() {
        if (mFaceView != null) mFaceView.clear();
    }

    @Override
    public void clearFocus() {
        FocusIndicator indicator = getFocusIndicator();
        if (indicator != null) indicator.clear();
    }

    @Override
    public void setFocusPosition(int x, int y) {
        mPieRenderer.setFocus(x, y);
    }

    @Override
    public void onFocusStarted() {
        getFocusIndicator().showStart();
    }

    @Override
    public void onFocusSucceeded(boolean timeout) {
        getFocusIndicator().showSuccess(timeout);
    }

    @Override
    public void onFocusFailed(boolean timeout) {
        getFocusIndicator().showFail(timeout);
    }

    @Override
    public void pauseFaceDetection() {
        if (mFaceView != null) mFaceView.pause();
    }

    @Override
    public void resumeFaceDetection() {
        if (mFaceView != null) mFaceView.resume();
    }

    public void onStartFaceDetection(int orientation, boolean mirror) {
        mFaceView.clear();
        mFaceView.setVisibility(View.VISIBLE);
        mFaceView.setDisplayOrientation(orientation);
        mFaceView.setMirror(mirror);
        mFaceView.resume();
    }

    @Override
    public void onFaceDetection(Face[] faces, CameraManager.CameraProxy camera) {
        mFaceView.setFaces(faces);
    }

    @Override
    public void onDisplayChanged() {
        Log.d(TAG, "Device flip detected.");
        mCameraControls.checkLayoutFlip();
        mController.updateCameraOrientation();
    }

    public void setPreference(String key, String value) {
        mMenu.setPreference(key, value);
    }

}

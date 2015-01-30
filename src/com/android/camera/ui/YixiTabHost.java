package com.android.camera.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;

public class YixiTabHost extends TabHost {
	private int mOrientation;
    private Matrix mMatrix = new Matrix();
    protected View mChild;

	public YixiTabHost(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
    public YixiTabHost(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public void setOrientation(int orientation, boolean animation) {
	        orientation = orientation % 360;
	        if (mOrientation == orientation) return;
	        mOrientation = orientation;
	        requestLayout();
	}

	public int getOrientation() {
	        return mOrientation;
	}
	 @Override
	    protected void onFinishInflate() {
	        mChild = getChildAt(0);
	        mChild.setPivotX(0);
	        mChild.setPivotY(0);
	    }

	    @Override
	    protected void onLayout(
	            boolean change, int left, int top, int right, int bottom) {
	        int width = right - left;
	        int height = bottom - top;
	        Log.i("lilei","*** YixiTabHost.onLayout() mOrientation:"+mOrientation+" width:"+width
	        		+" height:"+height);
	        switch (mOrientation) {
	            case 0:
	                mChild.layout(0, 0, width, height);
	                break;
	            case 180:  //add for AP-483 by lilei 
	            	mChild.layout(0, 0, width, height);
	                break;
	            case 90:
	            case 270:
	                mChild.layout(0, 0, height, width);
	                break;
	        }
	    }

	    @Override
	    protected void onMeasure(int widthSpec, int heightSpec) {
	        int w = 0, h = 0;
	        switch(mOrientation) {
	            case 0:
	            case 180:
	                measureChild(mChild, widthSpec, heightSpec);
	                w = mChild.getMeasuredWidth();
	                h = mChild.getMeasuredHeight();
	                break;
	            case 90:
	            case 270:
	                measureChild(mChild, heightSpec, widthSpec);
	                w = mChild.getMeasuredHeight();
	                h = mChild.getMeasuredWidth();
	                break;
	        }
	        setMeasuredDimension(w, h);

	        switch (mOrientation) {
	            case 0:
	                mChild.setTranslationX(0);
	                mChild.setTranslationY(0);
	                break;
	            case 90:
	                mChild.setTranslationX(0);
	                mChild.setTranslationY(h);
	                break;
	            case 180:
	                mChild.setTranslationX(w);
	                mChild.setTranslationY(h);
	                break;
	            case 270:
	                mChild.setTranslationX(w);
	                mChild.setTranslationY(0);
	                break;
	        }
	        mChild.setRotation(-mOrientation);
	    }

	    @Override
	    public boolean shouldDelayChildPressedState() {
	        return false;
	    }
}

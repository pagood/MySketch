package com.ms.xiaoyu.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.ms.xiaoyu.mysketch.R;
import com.ms.xiaoyu.util.TimedPoint;
import com.ms.xiaoyu.util.ControlTimedPoints;
import com.ms.xiaoyu.util.Bezier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Pad extends View {
	private List<TimedPoint> mPoints;
	private Paint mPaint;
	private Path mPath;
	private Bitmap mSignatureBitmap;
	private Canvas mSignatureBitmapCanvas;
	private final float ORI = 10.7f;
	private float width = ORI;
	private Rect dirtyRect;
	public Pad(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		
		init();
	}
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(w, h, oldw, oldh);
		mSignatureBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		mSignatureBitmap.eraseColor(getResources().getColor(R.color.white));
		mSignatureBitmapCanvas = new Canvas(mSignatureBitmap);
	}
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		canvas.drawBitmap(mSignatureBitmap,0,0, mPaint);
	}
	public void init(){
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(5);
		mPaint.setColor(getResources().getColor(R.color.black));
		mPoints = new ArrayList<TimedPoint>();
		mPath = new Path();
		dirtyRect = new Rect();

	}
	@Override
	public boolean onTouchEvent(MotionEvent event){
		float eventX = event.getX();
        float eventY = event.getY();
		switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
				addPoint(new TimedPoint(eventX, eventY));
				dirtyRect = new Rect((int)eventX,(int)eventY,(int)eventX,(int)eventY);
				invalidate(dirtyRect);
				break;
			case MotionEvent.ACTION_MOVE:
				width = event.getPressure() * ORI;
				
				addPoint(new TimedPoint(eventX, eventY));
				//dirty area to improve the ability
				dirtyRect.left = (int)(Math.min(dirtyRect.left, eventX) - width / 2);
				dirtyRect.right = (int)(Math.max((int) dirtyRect.right, (int) eventX) + 1 + width/2);
				dirtyRect.top = (int)(Math.min((int) dirtyRect.top, (int) eventY) - width/2);
				dirtyRect.bottom = (int)(Math.max((int) dirtyRect.bottom, (int) eventY) + 1 + width/2);
				invalidate(dirtyRect);
				
				break;
			case MotionEvent.ACTION_UP:
				width = ORI;
				mPoints.clear();
			
		}	
		return true;
	}
	private void addPoint(TimedPoint newPoint) {
        mPoints.add(newPoint);
        if (mPoints.size() > 2) {
            // To reduce the initial lag make it work with 3 mPoints
            // by copying the first point to the beginning.
            if (mPoints.size() == 3) mPoints.add(0, mPoints.get(0));

            ControlTimedPoints tmp = calculateCurveControlPoints(mPoints.get(0), mPoints.get(1), mPoints.get(2));
            TimedPoint c2 = tmp.c2;
            tmp = calculateCurveControlPoints(mPoints.get(1), mPoints.get(2), mPoints.get(3));
            TimedPoint c3 = tmp.c1;
            Bezier curve = new Bezier(mPoints.get(1), c2, c3, mPoints.get(2));

            TimedPoint startPoint = curve.startPoint;
            TimedPoint endPoint = curve.endPoint;

            addBezier(curve);


            // Remove the first element from the list,
            // so that we always have no more than 4 mPoints in mPoints array.
            mPoints.remove(0);
        }
    }

    private void addBezier(Bezier curve) {
        float originalWidth = mPaint.getStrokeWidth();
        float drawSteps = (float) Math.floor(curve.length());

        for (int i = 0; i < drawSteps; i++) {
            // Calculate the Bezier (x, y) coordinate for this step.
            float t = ((float) i) / drawSteps;
            float tt = t * t;
            float ttt = tt * t;
            float u = 1 - t;
            float uu = u * u;
            float uuu = uu * u;

            float x = uuu * curve.startPoint.x;
            x += 3 * uu * t * curve.control1.x;
            x += 3 * u * tt * curve.control2.x;
            x += ttt * curve.endPoint.x;

            float y = uuu * curve.startPoint.y;
            y += 3 * uu * t * curve.control1.y;
            y += 3 * u * tt * curve.control2.y;
            y += ttt * curve.endPoint.y;
            mPaint.setStrokeWidth(width);
            // Set the incremental stroke width and draw.
            mSignatureBitmapCanvas.drawPoint(x, y, mPaint);
        }

//        mPaint.setStrokeWidth(originalWidth);
    }

    private ControlTimedPoints calculateCurveControlPoints(TimedPoint s1, TimedPoint s2, TimedPoint s3) {
        float dx1 = s1.x - s2.x;
        float dy1 = s1.y - s2.y;
        float dx2 = s2.x - s3.x;
        float dy2 = s2.y - s3.y;

        TimedPoint m1 = new TimedPoint((s1.x + s2.x) / 2.0f, (s1.y + s2.y) / 2.0f);
        TimedPoint m2 = new TimedPoint((s2.x + s3.x) / 2.0f, (s2.y + s3.y) / 2.0f);

        float l1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
        float l2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);

        float dxm = (m1.x - m2.x);
        float dym = (m1.y - m2.y);
        float k = l2 / (l1 + l2);
        TimedPoint cm = new TimedPoint(m2.x + dxm * k, m2.y + dym * k);

        float tx = s2.x - cm.x;
        float ty = s2.y - cm.y;

        return new ControlTimedPoints(new TimedPoint(m1.x + tx, m1.y + ty), new TimedPoint(m2.x + tx, m2.y + ty));
    }
    public void clearPad(){
    	mSignatureBitmap.eraseColor(getResources().getColor(R.color.white));
    	invalidate();
    }
    public void setColor(int color){
    	mPaint.setColor(getResources().getColor(color));
    }
    public int getColor(){
    	return mPaint.getColor();
    }
    public InputStream getInputStream(){
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	mSignatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
    	InputStream isBm = new ByteArrayInputStream(baos.toByteArray());
    	return isBm;
    }
    
    
}

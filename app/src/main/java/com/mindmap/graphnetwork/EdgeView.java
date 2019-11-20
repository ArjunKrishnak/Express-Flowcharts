package com.mindmap.graphnetwork;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


public class EdgeView extends View {

    int mEdgeColor,mEdgeStrokeWidth;
    private static final int DEFAULT_STROKE_WIDTH = 12,DEFAULT_EDGE_COLOR = Color.RED;
    private static final float TOUCH_TOLERANCE = 4;

    Path mPath,mCursorPath;
    Paint mPaint,mCursorPaint;
    private static final float CURSOR_RADIUS = 30,CURSOR_STROKE_WIDTH = 4f;

    float mStartX,mStartY,mEndX,mEndY,mCurrX, mCurrY;

    public EdgeView(Context context) {
        super(context);
        init(null);
    }

    public EdgeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public EdgeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public EdgeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(@Nullable AttributeSet set){
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPath = new Path();
        mCursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCursorPaint.setStyle(Paint.Style.STROKE);
        mCursorPaint.setStrokeJoin(Paint.Join.MITER);
        mCursorPaint.setStrokeWidth(CURSOR_STROKE_WIDTH);
        mCursorPath = new Path();
        mPaint.setColor(DEFAULT_EDGE_COLOR);
        mPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        mCursorPaint.setColor(DEFAULT_EDGE_COLOR);

        if(set == null){
            return;
        }

        TypedArray typedarray = getContext().obtainStyledAttributes(set, R.styleable.Graph);
        mEdgeColor = typedarray.getColor(R.styleable.Graph_EdgeColor, DEFAULT_EDGE_COLOR);
        mEdgeStrokeWidth = typedarray.getColor(R.styleable.Graph_EdgeColor, DEFAULT_STROKE_WIDTH);
        mPaint.setColor(mEdgeColor);
        mPaint.setStrokeWidth(mEdgeStrokeWidth);
        mCursorPaint.setColor(mEdgeColor);
        typedarray.recycle();

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        mStartX = 300;mStartY = 100;mCurrX=400;mCurrY=200;
        mPath.moveTo(mStartX, mStartY);
        mPath.quadTo(mStartX, mStartY, mCurrX,mCurrY);
        canvas.drawPath( mPath,  mPaint);
        canvas.drawPath(mCursorPath,mCursorPaint);
    }

    private void touch_start(float x, float y) {
        mStartX = x;
        mStartY = y;
        mCurrX = mStartX;
        mCurrY = mStartY;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mCurrX);
        float dy = Math.abs(y - mCurrY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mCurrX = x;
            mCurrY = y;
            mCursorPath.reset();
            mPath.reset();
            mCursorPath.addCircle(mCurrX, mCurrY, CURSOR_RADIUS, Path.Direction.CW);
        }
    }

    private void touch_up() {
        mCursorPath.reset();
        mPath.reset();
        mEndX = mCurrX;
        mEndY =  mCurrY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }
}

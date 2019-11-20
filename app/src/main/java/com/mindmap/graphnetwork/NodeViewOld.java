package com.mindmap.graphnetwork;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class NodeViewOld extends View {
    Paint mPaint;
    int mNodeColor;
    Context mContext;
    private static final int DEFAULT_NODE_COLOR = Color.BLUE;

    public NodeViewOld(Context context) {
        super(context);
        mContext = context;
        init(null);
    }

    public NodeViewOld(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(attrs);
    }

    public NodeViewOld(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public NodeViewOld(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        init(attrs);
    }


    private void init(@Nullable AttributeSet set){
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(DEFAULT_NODE_COLOR);

        if(set == null){
            return;
        }

        TypedArray typedarray = getContext().obtainStyledAttributes(set, R.styleable.Graph);
        mNodeColor = typedarray.getColor(R.styleable.Graph_NodeColor, DEFAULT_NODE_COLOR);
        mPaint.setColor(mNodeColor);
        typedarray.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        mRect.left = 0;
//        mRect.right = getWidth();
//        mRect.top = 0;
//        mRect.bottom = getHeight();
//
//        canvas.drawRect(mRect, mPaint);
//        super.onDraw(canvas);
        int x = getWidth();
        int y = getHeight();
        int radius;
        radius = 100;
        mPaint.setStyle( Paint.Style.FILL);
//        mPaint.setColor( Color.RED);
//        canvas.drawPaint(paint);
        // Use Color.parseColor to define HTML colors
//        mPaint.setAntiAlias(true);
//
//        mPaint.setColor(Color.parseColor("#CD5C5C"));
        canvas.drawCircle(x / 2, y / 2, radius, mPaint);

    }

    public void highlightConnectetd(){

    }

    public void openNode(){
    }

    public EdgeView AddEdge(){
        EdgeView edgeView = new EdgeView(mContext);
        return edgeView;
    }

}

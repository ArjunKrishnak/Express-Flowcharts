package com.mindmap.graphnetwork;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import org.json.JSONObject;


public class Edge implements MindMapDrawable{

    int mEdgeColor,mEdgeStrokeWidth;
    private static final int DEFAULT_STROKE_WIDTH = 12,DEFAULT_EDGE_COLOR = Color.RED;

    private Path mPath,mStartCursorPath,mEndCursorPath;
    private Paint mPaint,mCursorPaint;
    private static final float CURSOR_RADIUS = 30,CURSOR_STROKE_WIDTH = 4f;
    private boolean mEditable;
    private Node mFromNode;
    private Node mToNode;
    private MainView mParentView;
    private float mCurrentScale = 1f;


    private float mStartX,mStartY,mEndX,mEndY;

    public DrawableType type(){
        return DrawableType.EDGE;
    }

    Edge(float startX,float startY,float endX,float endY,MainView parent){
        setStart(startX,startY);
        setEnd(endX,endY);

        init(parent);
    }
    public void setFromNode(Node fromNode){
        mFromNode = fromNode;
    }
    public void setToNode(Node toNode){
        mToNode = toNode;
    }


    public void setStart(float startX,float startY) {
        mStartX = startX;
        mStartY = startY;
    }
    public void setEnd(float endX,float endY) {
        mEndX = endX;
        mEndY = endY;
    }
    public void MakeEditable(boolean state){
        mEditable = state;
    }

    private void init(MainView parent){
        mParentView = parent;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPath = new Path();
        mCursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCursorPaint.setStyle(Paint.Style.STROKE);
        mCursorPaint.setStrokeJoin(Paint.Join.MITER);
        mCursorPaint.setStrokeWidth(CURSOR_STROKE_WIDTH);
        mStartCursorPath = new Path();
        mEndCursorPath = new Path();
        mPaint.setColor(DEFAULT_EDGE_COLOR);
        mCurrentScale = mParentView.mScaleFactor;
        mEdgeStrokeWidth = (int)(DEFAULT_STROKE_WIDTH*mCurrentScale);
        mPaint.setStrokeWidth(mEdgeStrokeWidth);
        mCursorPaint.setColor(DEFAULT_EDGE_COLOR);
        mEditable = true;
    }

    public void draw(Canvas canvas) {
        mPath.reset();
        mPath.moveTo(mStartX, mStartY);
        mPath.quadTo(mStartX, mStartY, mEndX,mEndY);
        canvas.drawPath( mPath,  mPaint);

        if(mEditable) {
            mStartCursorPath.reset();
            mStartCursorPath.addCircle( mStartX, mStartY, CURSOR_RADIUS, Path.Direction.CW );
            canvas.drawPath( mStartCursorPath, mCursorPaint );

            mEndCursorPath.reset();
            mEndCursorPath.addCircle( mEndX, mEndY, CURSOR_RADIUS, Path.Direction.CW );
            canvas.drawPath( mEndCursorPath, mCursorPaint );
        }
    }

    boolean fromNode(Node node){
       return mFromNode==node;
    }
    boolean toNode(Node node){
        return mToNode==node;
    }

    public boolean contains(float x, float y){
//        if(Editable){
//            if((x-mStartX)*(x-mStartX) + (y-mStartY)*(y-mStartY) <= 5*CURSOR_RADIUS*CURSOR_RADIUS)
//                return true;
//            if((x-mEndX)*(x-mEndX) + (y-mEndY)*(y-mEndY) <= 5*CURSOR_RADIUS*CURSOR_RADIUS)
//                return true;
//            return false;
//        }
        //comparing perpendicular distance from the line
        if(mStartX==mEndX) {
            if (x - mStartX <= 5*DEFAULT_STROKE_WIDTH)
                return true;
            return false;
        }
        float slope = (mEndY - mStartY)/(mEndX - mStartX);
        if((slope*x - y + mStartY-slope*mStartX)/Math.sqrt(1+slope*slope)<=5*DEFAULT_STROKE_WIDTH)
            return true;
        return false;
    }

    public JSONObject toJson(){
        return null;
    }

    @Override
    public boolean onScreen(float width, float height) {
        return true;
//        RectF boundPath;
//        float slope;
//        if(mStartX==mEndX)
//            slope = 100;
//        else
//            slope = (mEndY - mStartY)/(mEndX - mStartX);
//        if(slope>=1) {
//            if (mStartX < mEndX)
//                boundPath = new RectF( mStartX - DEFAULT_STROKE_WIDTH, mStartY, mEndX + DEFAULT_STROKE_WIDTH, mEndY );
//            else
//                boundPath = new RectF( mStartX + DEFAULT_STROKE_WIDTH, mStartY, mEndX - DEFAULT_STROKE_WIDTH, mEndY );
//        }
//        else {
//            if (mStartY < mEndY)
//                boundPath = new RectF( mStartX, mStartY - DEFAULT_STROKE_WIDTH, mEndX, mEndY+DEFAULT_STROKE_WIDTH );
//            else
//                boundPath = new RectF( mStartX, mStartY + DEFAULT_STROKE_WIDTH, mEndX, mEndY-DEFAULT_STROKE_WIDTH );
//        }
//        RectF boundView = new RectF(0,0,width,height);
//        return boundPath.intersect(boundView);

    }

    @Override
    public void move(float shiftX, float shiftY) {
        mStartX = mStartX+shiftX;
        mEndX = mEndX+shiftX;
        mStartY = mStartY+ shiftY;
        mEndY = mEndY+shiftY;
    }

    @Override
    public void scale(float scale) {
        float scaleFactor = scale/mCurrentScale;
        mStartX*=scaleFactor;
        mEndX*=scaleFactor;
        mStartY*=scaleFactor;
        mEndY*=scaleFactor;
        mCurrentScale = scale;
        mEdgeStrokeWidth = (int)(mEdgeStrokeWidth*scaleFactor);
        mPaint.setStrokeWidth(mEdgeStrokeWidth);
    }
}

package com.mindmap.graphnetwork;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public class Node implements MindMapDrawable{
    private static final String TAG = "Node";

    //Saved in Json
    int mNodeColor;
    private float mX,mY,mR;
    private String mId;
    //Not Saved in Json
    Paint mPaint;
    Path mPath;
    private static final int DEFAULT_NODE_COLOR = Color.BLUE;
    private static final float DEFAULT_NODE_RADIUS = 100;
    //Node state variables
    private MainView mParentView;
    private float mCurrentScale = 1f;

    @Override
    public String getId(){
        return mId;
    }

    @Override
    public DrawableType type(){
        return DrawableType.NODE;
    }

    public Node(float x,float y,float r,String id,MainView parent){
        this(x,y,parent);
        this.mId = id;
        this.mR = r;
    }


    public Node(float x,float y,MainView parentView) {
        mId = JsonHelper.getUniqueID();
        mPath = new Path();
        setParentView(parentView);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mNodeColor = DEFAULT_NODE_COLOR;
        mPaint.setColor(mNodeColor);
        mCurrentScale = parentView.mScaleFactor;
        mR = mCurrentScale*DEFAULT_NODE_RADIUS;
        set(x,y);
    }

    public void set(float x, float y) {
        mX = x;
        mY = y;
        mPath.reset();
        mPath.addCircle( mX, mY, mR, Path.Direction.CW );
    }

    @Override
    public boolean contains(float x, float y) {
        if((x-mX)*(x-mX) + (y-mY)*(y-mY) <= mR*mR)
            return true;
        return false;
    }

    @Override
    public void draw(Canvas canvas){
        mPath.reset();
        mPath.addCircle( mX, mY, mR, Path.Direction.CW );
        canvas.drawPath(mPath, mPaint);
    }

    @Override
    public void scale(float scale){
        float scaleFactor = scale/mCurrentScale;
        mX = mX*scaleFactor;
        mY = mY*scaleFactor;
        mR = mR*scaleFactor;
        mCurrentScale = scale;
    }


    public void setParentView(MainView parentView){
        mParentView = parentView;
    }

    public void highlightConnectetd(){

    }

    public void openNode(){
    }

    public float[] centre(){
        float[] centreXY = {mX,mY};
        return centreXY;
    }


    @Override
    public boolean onScreen(float width, float height ) {
        return true;
//        final RectF boundCircle  = new RectF();
//        mPath.computeBounds(boundCircle, true);
//        final RectF boundView = new RectF(0,0,width,height);
//        return boundCircle.intersect(boundView);
    }

    @Override
    public void move(float shiftX, float shiftY) {
        mX=mX+shiftX;
        mY=mY+shiftY;
    }

    @Override
    /**
     * @return a JSON representation of this Node
     */
    public JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put( JsonHelper.ITEM_TYPE_KEY, getClass().getName()); // TODO change this to enum
            obj.put( JsonHelper.NodeSchema.NODE_CENTRE_X_KEY, this.mX);
            obj.put( JsonHelper.NodeSchema.NODE_CENTRE_Y_KEY, this.mY);
            obj.put( JsonHelper.NodeSchema.NODE_RADIUS_KEY, this.mR);
            obj.put( JsonHelper.ITEM_ID_KEY, this.getId());
            return obj;

        } catch (Exception e) {
            Log.e(TAG, "toJson: ", e);
            return null;
        }
    }

    /**
     * get a UmlNoteNode from a JSONObject
     *
     * @param obj JSONObject representation of a UmlNoteNode
     * @return a Node of the given JSONObject
     */
    public static Node fromJson(JSONObject obj,MainView view) {
        try {

            return new Node((float) obj.getDouble( JsonHelper.NodeSchema.NODE_CENTRE_X_KEY),
                            (float) obj.getDouble( JsonHelper.NodeSchema.NODE_CENTRE_Y_KEY),
                            (float)obj.getDouble( JsonHelper.NodeSchema.NODE_RADIUS_KEY),
                            obj.getString( JsonHelper.ITEM_ID_KEY),view);
        } catch (Exception e) {
            Log.e(TAG, "fromJson: ", e);
            return null;
        }
    }

}

package com.mindmap.graphnetwork;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;

public class Node implements MindMapDrawable{
    Paint mPaint;
    Path mPath;
    int mNodeColor;
    private static final int DEFAULT_NODE_COLOR = Color.BLUE;
    private static final float DEFAULT_NODE_RADIUS = 100;
    private float mX,mY,mR;
    private MainView mParentView;
    private float mCurrentScale = 1f;
    private static final String TAG = "Node";
    private String mId;

    @Override
    public String getId(){
        return mId;
    }

//    public Node(Context context) {
//       super(context);
//    }
    @Override
    public DrawableType type(){
        return DrawableType.NODE;
    }

    public Node(float x,float y,float r,String id){
        this(x,y,null);
        this.mId = id;
        //this.mR = r; //TODO add radius here
    }


    public Node(float x,float y,MainView parentView) {
        mId = FileHelper.getUniqueID();
        mPath = new Path();
        setParentView(parentView);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mNodeColor = DEFAULT_NODE_COLOR;
        mPaint.setColor(mNodeColor);
        if(parentView!=null)//TODO delete mParentView?
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

//    @Override
//    protected void onDraw(Canvas canvas){
//        super.onDraw(canvas);
//        canvas.drawCircle(mX, mY, mR, mPaint);
//    }

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

            obj.put(FileHelper.ITEM_TYPE_KEY, getClass().getName());
            obj.put(FileHelper.LOC_X_KEY, this.mX);
            obj.put(FileHelper.LOC_Y_KEY, this.mY);
            obj.put(FileHelper.ITEM_ID_KEY, this.getId());

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
    public static Node fromJson(JSONObject obj) {
        try {
//            return new Node(obj.getString("cdn_text"), (float) obj.getDouble(FileHelper.LOC_X_KEY),
//                    (float) obj.getDouble(FileHelper.LOC_Y_KEY));
            return new Node((float) obj.getDouble(FileHelper.LOC_X_KEY),(float) obj.getDouble(FileHelper.LOC_Y_KEY),DEFAULT_NODE_RADIUS,
                            (String) obj.getString(FileHelper.ITEM_ID_KEY));
        } catch (Exception e) {
            Log.e(TAG, "fromJson: ", e);
            return null;
        }
    }

}

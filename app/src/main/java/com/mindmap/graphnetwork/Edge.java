package com.mindmap.graphnetwork;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import org.json.JSONObject;

import java.util.List;


public class Edge implements MindMapDrawable{

    private static final String TAG = "Edge";

    //Saved in Json
    private String mId;
    int mEdgeColor,mEdgeStrokeWidth;
    private Node mFromNode;
    private Node mToNode;
    //Not Saved in Json
    private static final int DEFAULT_STROKE_WIDTH = 12,DEFAULT_EDGE_COLOR = Color.RED;
    private static final float DEFAULT_CURSOR_RADIUS = 30, DEFAULT_CURSOR_STROKE_WIDTH = 4f;
    private Path mPath,mStartCursorPath,mEndCursorPath;
    private Paint mPaint,mCursorPaint;
    //Edge state variables
    private boolean mEditable;
    private MainView mParentView;
    private float mCurrentScale = 1f;
    private float mStartX,mStartY,mEndX,mEndY;

    public DrawableType type(){
        return DrawableType.EDGE;
    }

    //Called from MainView
    Edge(float startX,float startY,float endX,float endY,MainView parent){
        setStart(startX,startY);
        setEnd(endX,endY);
        init(parent);
        editable(true);
        mId = JsonHelper.getUniqueID();
    }

    //Called while decoding Json
    Edge(Node fromNode,Node toNode,String Id,MainView parent){
        float[] startXY = (fromNode).centre();
        float[] endXY = (toNode).centre();
        setStart(startXY[0],startXY[1]);
        setEnd(endXY[0],endXY[1]);
        setFromNode(fromNode);
        setToNode(toNode);
        init( parent );
        editable(false);
        mId = Id;
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

    public void editable(boolean state){
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
        mCursorPaint.setStrokeWidth( DEFAULT_CURSOR_STROKE_WIDTH );
        mStartCursorPath = new Path();
        mEndCursorPath = new Path();
        mPaint.setColor(DEFAULT_EDGE_COLOR);
        mCurrentScale = mParentView.mScaleFactor;
        mEdgeStrokeWidth = (int)(DEFAULT_STROKE_WIDTH*mCurrentScale);
        mPaint.setStrokeWidth(mEdgeStrokeWidth);
        mCursorPaint.setColor(DEFAULT_EDGE_COLOR);
    }

    public void draw(Canvas canvas) {
        mPath.reset();
        mPath.moveTo(mStartX, mStartY);
        mPath.quadTo(mStartX, mStartY, mEndX,mEndY);
        canvas.drawPath( mPath,  mPaint);

        if(mEditable) {
            mStartCursorPath.reset();
            mStartCursorPath.addCircle( mStartX, mStartY, DEFAULT_CURSOR_RADIUS, Path.Direction.CW );
            canvas.drawPath( mStartCursorPath, mCursorPaint );

            mEndCursorPath.reset();
            mEndCursorPath.addCircle( mEndX, mEndY, DEFAULT_CURSOR_RADIUS, Path.Direction.CW );
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
//            if((x-mStartX)*(x-mStartX) + (y-mStartY)*(y-mStartY) <= 5*DEFAULT_CURSOR_RADIUS*DEFAULT_CURSOR_RADIUS)
//                return true;
//            if((x-mEndX)*(x-mEndX) + (y-mEndY)*(y-mEndY) <= 5*DEFAULT_CURSOR_RADIUS*DEFAULT_CURSOR_RADIUS)
//                return true;
//            return false;
//        }

        //comparing perpendicular distance from the line
        if(mStartX==mEndX) {
            if (x - mStartX <= 5*mEdgeStrokeWidth)
                return true;
            return false;
        }
        float slope = (mEndY - mStartY)/(mEndX - mStartX);
        if((slope*x - y + mStartY-slope*mStartX)/Math.sqrt(1+slope*slope)<=5*mEdgeStrokeWidth)
            return true;
        return false;
    }

    @Override
    public boolean onScreen(float width, float height) {

        RectF boundPath;
        float slope;
        if(mStartX==mEndX)
            slope = 100;
        else
            slope = (mEndY - mStartY)/(mEndX - mStartX);
        if(slope>=1) {
            if (mStartX < mEndX)
                boundPath = new RectF( mStartX - mEdgeStrokeWidth, mStartY, mEndX + mEdgeStrokeWidth, mEndY );
            else
                boundPath = new RectF( mStartX + mEdgeStrokeWidth, mStartY, mEndX - mEdgeStrokeWidth, mEndY );
        }
        else {
            if (mStartY < mEndY)
                boundPath = new RectF( mStartX, mStartY - mEdgeStrokeWidth, mEndX, mEndY+mEdgeStrokeWidth );
            else
                boundPath = new RectF( mStartX, mStartY + mEdgeStrokeWidth, mEndX, mEndY-mEdgeStrokeWidth );
        }
        RectF boundView = new RectF(0,0,width,height);
        return boundPath.intersect(boundView);

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

    @Override
    public String getId(){
        return mId;
    }

    /**
     * Json representation of this Edge
     * @return a JSONObject containing all the information needed to save and load
     */

    @Override
    public JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put( JsonHelper.ITEM_TYPE_KEY, getClass().getName());
            obj.put( JsonHelper.EdgeSchema.EDGE_START_NODE_KEY, this.mFromNode.getId());
            obj.put( JsonHelper.EdgeSchema.EDGE_END_NODE_KEY, this.mToNode.getId());
            obj.put( JsonHelper.EdgeSchema.EDGE_STROKE_WIDTH_KEY, this.mEdgeStrokeWidth);
            obj.put( JsonHelper.ITEM_ID_KEY, this.getId());
            return obj;

        } catch (Exception e) {
            Log.e(TAG, "toJson: ", e);
            return null;
        }
    }

    /**
     * Get a UmlBentArrow from a saved JSONObject representing a UmlBentArrow
     *
     * @param jsonObject     representing a UmlBentArrow
     * @param alldrDrawables all possible shapes this UmlBentArrow can point to/from
     * @return a new UmlBentArrow
     */
    public static Edge fromJson(JSONObject jsonObject, List<MindMapDrawable> alldrDrawables,MainView view) {
        try {

            Node startNode = null;
            Node endNode = null;
            String startShapeId = jsonObject.getString(JsonHelper.EdgeSchema.EDGE_START_NODE_KEY);
            String endShapeId = jsonObject.getString(JsonHelper.EdgeSchema.EDGE_END_NODE_KEY);

            for (MindMapDrawable shape : alldrDrawables) {
                if (shape instanceof Node) {
                    if (shape.getId().equals(startShapeId)) startNode = (Node) shape;
                    if (shape.getId().equals(endShapeId)) endNode = (Node) shape;
                }
            }

            //return a new arrow if we found both the items
            return (startNode == null || endNode == null) ? null
                    : new Edge(startNode, endNode,jsonObject.getString( JsonHelper.ITEM_ID_KEY),view);
        } catch (Exception e) {
            Log.e(TAG, "fromJson: ", e);
            return null;
        }
    }


}

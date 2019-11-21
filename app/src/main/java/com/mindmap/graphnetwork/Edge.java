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
    private String mId;


    private float mStartX,mStartY,mEndX,mEndY;

    public DrawableType type(){
        return DrawableType.EDGE;
    }

    Edge(float startX,float startY,float endX,float endY,MainView parent){
        setStart(startX,startY);
        setEnd(endX,endY);
        init(parent);
    }

    Edge(Node fromNode,Node toNode,String Id){
        float[] startXY = (fromNode).centre();
        float[] endXY = (toNode).centre();
        setStart(startXY[0],startXY[1]);
        setEnd(endXY[0],endXY[1]);
        setFromNode(fromNode);
        setToNode(toNode);
        init( null );
        MakeEditable(false);
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
    public void MakeEditable(boolean state){
        mEditable = state;
    }

    private void init(MainView parent){
        mId = FileHelper.getUniqueID();
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
        if(mParentView!=null) //TODO delete mParentView?
            mCurrentScale = mParentView.mScaleFactor;
        mEdgeStrokeWidth = (int)(DEFAULT_STROKE_WIDTH*mCurrentScale);
        mPaint.setStrokeWidth(mEdgeStrokeWidth);
        mCursorPaint.setColor(DEFAULT_EDGE_COLOR);
        mEditable = true; //TODO take it out of init
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

    @Override
    public String getId(){
        return mId;
    }

    /**
     * Json representation of this UmlBentArrow
     *
     * @return a JSONObject containing all the information needed to save and load
     */

    @Override
    public JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();

            obj.put(FileHelper.ITEM_TYPE_KEY, getClass().getName());
            obj.put("cd_arrow_start", this.mFromNode.getId());
            obj.put("cd_arrow_end", this.mToNode.getId());
            obj.put(FileHelper.ITEM_ID_KEY, this.getId());
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
    public static Edge fromJson(JSONObject jsonObject,
                                        List<MindMapDrawable> alldrDrawables) {
        try {


            Node startNode = null;
            Node endNode = null;
            String startShapeId = jsonObject.getString("cd_arrow_start");
            String endShapeId = jsonObject.getString("cd_arrow_end");

            for (MindMapDrawable shape : alldrDrawables) {
                if (shape instanceof Node) {
                    if (shape.getId().equals(startShapeId)) startNode = (Node) shape;
                    if (shape.getId().equals(endShapeId)) endNode = (Node) shape;
                }
            }

            //return a new arrow if we found both the items
            return (startNode == null || endNode == null) ? null
                    : new Edge(startNode, endNode,jsonObject.getString(FileHelper.ITEM_ID_KEY) );
        } catch (Exception e) {
            Log.e(TAG, "fromJson: ", e);
            return null;
        }
    }


}

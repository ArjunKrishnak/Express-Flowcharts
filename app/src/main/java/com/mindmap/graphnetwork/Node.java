package com.mindmap.graphnetwork;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import org.json.JSONObject;
enum NodeShape { CIRCLE, SQUARE };
public class Node implements MindMapDrawable{
    private static final String TAG = "Node";

    //Saved in Json
    int mNodeColorID;
    private float mX,mY,mR;
    private String mId;
    private String mTitle = "";
    private String mDescription  = "";
    private NodeShape mShape = NodeShape.CIRCLE;
    private float mTextSize;
    //Not Saved in Json
    Paint mPaint;
    Path mPath;
    Paint mTitlePaint;
    private static final int DEFAULT_NODE_COLOR = Color.BLUE;
    public static final float DEFAULT_NODE_RADIUS = 100;//making it available for MainActivity for new node creation
    private static final int DEFAULT_TITLE_COLOR = Color.BLACK;
    private static final int DEFAULT_TEXT_SIZE = 30;
    //Node state variables
    private MainView mParentView;
    private float mCurrentScale = 1f;

    public void setShape(NodeShape shape) {
        this.mShape = shape;
    }

    public NodeShape getShape() {
        return mShape;
    }

    @Override
    public void setColorID(int colorID) {
        this.mNodeColorID = colorID;
    }

    @Override
    public int getColorID() {
        return this.mNodeColorID;
    }

    public float getR(){
        return mR;
    }

    @Override
    public String getTitle(){
        return mTitle;
    }

    @Override
    public String getDescription(){
        if(mDescription==null)
            return "";
        return mDescription;
    }

    String reduceText(String title){
        Rect boundTitle = new Rect();
        int length = title.length();
        mTitlePaint.getTextBounds(title, 0, length, boundTitle);

        while(boundTitle.width()>2*mR) {
            int factor =  (int)(boundTitle.width()/(2*mR));
            if(factor>2)
                length = (int)((2*length)/factor);
            else
                length = length-1;
            mTitlePaint.getTextBounds( title, 0, length, boundTitle );
        }
        if(length==title.length())
            return title;
        return title.substring(0,length-3)+"...";
    }

    @Override
    public void setTitle(String title){
        mTitle = title;
    }

    @Override
    public void setDescription(String description){
        if(description==null)
            mDescription = "";
        else
            mDescription = description;
    }

    @Override
    public String getId(){
        return mId;
    }

    @Override
    public DrawableType type(){
        return DrawableType.NODE;
    }

    public Node(float x,float y,float r,String id,MainView parent,String title,String description,int colorID,NodeShape shape){
        this(x,y,parent,title,description,shape);
        this.mId = id;
        this.mR = r;
        setColorID(colorID);
    }

    public Node(float x,float y,MainView parentView,String title,String description,NodeShape shape){
        this(x,y,parentView,title);
        setDescription(description);
        setShape(shape);
    }

    public Node(float x,float y,MainView parentView,String title) {
        mId = JsonHelper.getUniqueID();
        mPath = new Path();
        setParentView(parentView);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mNodeColorID = DEFAULT_NODE_COLOR;
        mPaint.setColor( mNodeColorID );
        mCurrentScale = parentView.mScaleFactor;
        mR = mCurrentScale*DEFAULT_NODE_RADIUS;
        set(x,y);
        mTitlePaint= new Paint();
        mTitlePaint.setColor( DEFAULT_TITLE_COLOR );
        mTextSize = DEFAULT_TEXT_SIZE*mCurrentScale;
        mTitlePaint.setTextSize(mTextSize);
        mTitlePaint.setTextAlign(Paint.Align.CENTER);
        setTitle(title);
    }

    public void set(float x, float y) {
        mX = x;
        mY = y;
        mPath.reset();
        mPath.addCircle( mX, mY, mR, Path.Direction.CW );
    }

    public float distance(PointF p1,PointF p2){
        return Math.abs( (float)Math.sqrt( (p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y) ) );
    }

    @Override
    public boolean contains(float x, float y) {
        if(mShape==NodeShape.CIRCLE) {
            if ((x - mX) * (x - mX) + (y - mY) * (y - mY) <= mR * mR)
                return true;
        }
        else if(mShape==NodeShape.SQUARE){

            PointF currP = new PointF(x,y);
            PointF p1 = new PointF(mX-mR,mY-mR);
            PointF p2 = new PointF(mX-mR,mY+mR);
            PointF p3 = new PointF(mX+mR,mY-mR);
            PointF p4 = new PointF(mX+mR,mY+mR);

            if( distance(p1,currP) < 2*mR && distance(p2,currP) < 2*mR &&
                distance(p3,currP) < 2*mR && distance(p4,currP) < 2*mR )
                return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas){
        mPath.reset();
        if(mShape == NodeShape.CIRCLE)
            mPath.addCircle( mX, mY, mR, Path.Direction.CW );
        else if(mShape == NodeShape.SQUARE)
            mPath.addRect( mX-mR, mY-mR, mX+mR,mY+mR, Path.Direction.CW );
        mPaint.setColor(mNodeColorID);
        canvas.drawPath(mPath, mPaint);
        canvas.drawText( reduceText(mTitle), mX, mY, mTitlePaint);
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


    public float[] centre(){
        float[] centreXY = {mX,mY};
        return centreXY;
    }


    @Override
    public boolean onScreen(float width, float height ) {
        final RectF boundCircle  = new RectF(mX-mR,mY-mR,mX+mR,mY+mR);
        final RectF boundView = new RectF(0,0,width,height);
        return boundCircle.intersect(boundView);
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
            obj.put( JsonHelper.NodeSchema.NODE_TITLE_KEY,this.mTitle);
            obj.put( JsonHelper.NodeSchema.NODE_DESCRIPTION_KEY,this.mDescription);
            obj.put( JsonHelper.NodeSchema.NODE_COLOR_KEY,this.mNodeColorID );
            obj.put( JsonHelper.NodeSchema.NODE_SHAPE_KEY,this.mShape.toString() );
            return obj;

        } catch (Exception e) {
            Log.e(TAG, "toJson: ", e);
            return null;
        }
    }

    private static NodeShape shapeStringToEnum(String string){
        if(string.equals(NodeShape.CIRCLE.toString()))
            return NodeShape.CIRCLE;
        return NodeShape.SQUARE;
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
                            obj.getString( JsonHelper.ITEM_ID_KEY),view,
                            obj.getString( JsonHelper.NodeSchema.NODE_TITLE_KEY),
                            obj.getString( JsonHelper.NodeSchema.NODE_DESCRIPTION_KEY ),
                            obj.getInt( JsonHelper.NodeSchema.NODE_COLOR_KEY ),
                            shapeStringToEnum(obj.getString( JsonHelper.NodeSchema.NODE_SHAPE_KEY)));
        } catch (Exception e) {
            Log.e(TAG, "fromJson: ", e);
            return null;
        }
    }

}

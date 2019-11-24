package com.mindmap.graphnetwork;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import org.json.JSONObject;

public class Node implements MindMapDrawable{
    private static final String TAG = "Node";

    //Saved in Json
    int mNodeColorID;
    private float mX,mY,mR;
    private String mId;
    private String mTitle = "";
    private String mDescription  = "";
    //Not Saved in Json
    Paint mPaint;
    Path mPath;
    Paint mTitlePaint;
    private static final int DEFAULT_NODE_COLOR = Color.BLUE;
    public static final float DEFAULT_NODE_RADIUS = 100;//making it available for MainActivity for new node creation
    private static final int DEFAULT_TITLE_COLOR = Color.RED;
    //Node state variables
    private MainView mParentView;
    private float mCurrentScale = 1f;

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

    @Override
    public void setTitle(String title){
        mTitle = title;
        Rect boundTitle = new Rect();
        mTitlePaint.getTextBounds(title, 0, title.length(), boundTitle);
        //Expand the node as required by the title
        if(2*mR<boundTitle.width())
            mR = boundTitle.width()/2;
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

    public Node(float x,float y,float r,String id,MainView parent,String title,String description,int colorID){
        this(x,y,parent,title,description);
        this.mId = id;
        this.mR = r;
        setColorID(colorID);
    }

    public Node(float x,float y,MainView parentView,String title,String description){
        this(x,y,parentView,title);
        setDescription(description);
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
        mTitlePaint.setTextSize(30);
        mTitlePaint.setTextAlign(Paint.Align.CENTER);
        setTitle(title);
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
        mPaint.setColor(mNodeColorID);
        canvas.drawPath(mPath, mPaint);
        canvas.drawText(mTitle, mX, mY, mTitlePaint);
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
                            obj.getString( JsonHelper.ITEM_ID_KEY),view,
                            obj.getString( JsonHelper.NodeSchema.NODE_TITLE_KEY),
                            obj.getString( JsonHelper.NodeSchema.NODE_DESCRIPTION_KEY ),
                            obj.getInt( JsonHelper.NodeSchema.NODE_COLOR_KEY ));
        } catch (Exception e) {
            Log.e(TAG, "fromJson: ", e);
            return null;
        }
    }

}

package com.mindmap.expressFlowchart;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

enum ViewTask{
    MOVE_NODE,SHOW_CONNECTED,
    MOVE_EDGE,
    DETAILS_WINDOW,
    PAN_SCREEN,ZOOM_SCREEN,
    IDLE;
}
enum DrawableType{
   NODE,EDGE;
}

public class MainView extends View implements View.OnClickListener,View.OnLongClickListener {

    //MainView state variables
    private MindMapDrawable mClicked = null; //null means none are clicked
    private MindMapDrawable mLongClicked = null; //null means none are long clicked
    int mClickCount = 0; //variable for counting two successive up-down events
    private long mClickEnd = 0; // click end time
    Edge mEdge = null; //temporarily required for drawing drawing ege
    ViewTask mViewTask; //Which task is currently executing
    private float mDownX,mDownY; //press down X,Y
    private boolean savePending = false; //used for saving file
    boolean mClickedNodeSelected = false;
    boolean mExporting = false;

    //currently seleced preferences from details window
    private int mColorId = Color.BLUE; //selected color from details_window for either edge or node
    private NodeShape mNodeShape = NodeShape.CIRCLE;
    private ArrowShape mArrowShape = ArrowShape.NONE;
    private float mNodeRadius = Node.DEFAULT_NODE_RADIUS;
    private float mEdgeStrokeWidth = Edge.DEFAULT_STROKE_WIDTH;

    //remembering last seleced preferences from details window
    private NodeShape lastSelectedNodeShape = NodeShape.CIRCLE;
    private int lastSelectedNodeColor = Color.BLUE;
    private int lastSelectedEdgeColor = Color.BLACK;
    private ArrowShape lastSelectedArrowShape = ArrowShape.NONE;
    private float lastSelectedNodeRadius = Node.DEFAULT_NODE_RADIUS;
    private float lastSelectedStrokeWidth = Edge.DEFAULT_STROKE_WIDTH;

    //Saved in Json
    private ArrayList<MindMapDrawable> mAllViewDrawables;
    float mScaleFactor = 1f;

    //Panning
    //coordinates to shift the Drawables by
    public float mSwipeDownX=0f;
    public float mSwipeDownY=0f;
    public float mShiftX=0f;
    public float mShiftY=0f;

    //Scaling
    ScaleGestureDetector mScaleDetector;
    float mScaleFocusX = 0f;
    float mScaleFocusY = 0f;
    float MIN_SCALE = 0.5f;
    float MAX_SCALE = 4f;
    float mChangeInscale = 1f;
    MotionEvent mEvent;

    //Not Saved in Json
    //constant for defining the maximum total time duration between the first click and second click that can be considered as double-click
    private static final long MAX_DOUBLE_CLICK_DURATION = 500;
    private Context mContext = null;
    Point mNewButtonXY = new Point( 0,0 );

    public void setNewButtonXY(Point newButtonXY){
        if(mNewButtonXY==null)
            mNewButtonXY = new Point(0,0);
        mNewButtonXY = newButtonXY;
    }

    /**
     * Removes all items and "clears the working space"
     * THIS SHOULD ONLY BE CALLED AFTER USER'S CONSENT
     */
    public void resetSpace(){
        resetSpace(1.0f);
        Toast.makeText(mContext,R.string.new_working_area, Toast.LENGTH_SHORT ).show();
    }
    public void resetSpace(float scale) {
        mAllViewDrawables.clear();
        mClicked = null;
        mLongClicked = null;
        mEdge = null;
        savePending = false;
        mScaleFactor = scale;
        postInvalidate();

    }

    /**
     * @return true if there is a change pending to be saved, false otherwise
     */
    public boolean getSavePending() {
        return savePending;
    }

    /**
     * @param savePending new boolean whether change is pending
     */
    public void setSavePending(boolean savePending) {
        this.savePending = savePending;
    }

    public boolean isEmpty() {
        return this.mAllViewDrawables.isEmpty();
    }

    /**
     * @return a JSONObject that contains all the information of this editor
     */
    public JSONObject toJson() {
        try {
            JSONArray arr = new JSONArray();
            for (MindMapDrawable drawable : mAllViewDrawables)
                arr.put(drawable.toJson());

            JSONObject obj = new JSONObject();
            obj.put( FileHelper.SCALE_KEY, this.mScaleFactor);
            obj.put( FileHelper.ITEMS_KEY, arr);
            return obj;

        } catch (Exception e) {
            Toast.makeText(mContext, R.string.save_error, Toast.LENGTH_LONG).show();
            return null;
        }
    }


    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if(mEvent.getPointerCount() != 2)
                return false; //fix for immediate drag recognized as scale
            mViewTask = ViewTask.ZOOM_SCREEN;
            return true;
        }
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScaleFactor = mScaleFactor;
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max( MIN_SCALE,Math.min( MAX_SCALE,mScaleFactor));
            mChangeInscale = mScaleFactor/oldScaleFactor;
            if(oldScaleFactor == mScaleFactor) // only need to scale otherwise
                return true;
            mScaleFocusX = detector.getFocusX();
            mScaleFocusY = detector.getFocusY();
            scaleDrawables(mScaleFocusX,mScaleFocusY,true);
            return true;
        }
        @Override
        public void onScaleEnd(ScaleGestureDetector detector){
            mViewTask = ViewTask.IDLE;
        }
    }


    public MainView(Context context) {
        super(context);
        init(context);
    }

    public MainView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context,attrs,defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context,attrs,defStyleAttr,defStyleRes);
        init(context);
    }

    void init(Context context){
        this.setClickable(true);
        this.setOnClickListener(this);
        this.setOnLongClickListener(this);
        mAllViewDrawables = new ArrayList<>();
        mContext = context;
        mViewTask = ViewTask.IDLE;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    /**
     * implementation things to draw
     * @param canvas to draw items
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (MindMapDrawable drawable : mAllViewDrawables) {
            if (drawable.onScreen( getWidth(), getHeight() ) || mExporting)
                    drawable.draw(canvas);
        }

    }

    public void addDrawable(MindMapDrawable drawable) {
        mAllViewDrawables.add(drawable);
        savePending = true;
        postInvalidate();
    }

    public void addNode(float x,float y,String title,String description,NodeShape shape,int color,float mNodeRadius){
        Node node = new Node(x,y,this,title, description,shape);
        node.set( x,y);
        node.setColorID(color);
        if(mNodeRadius==Node.NODE_RADIUS_WARP_TEXT)
            node.setR(mNodeRadius);
        addDrawable(node);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        ((MainActivity)mContext).closeOptions();
        if(mViewTask == ViewTask.DETAILS_WINDOW) {
            return true;
        }
        mEvent = event;
        mScaleDetector.onTouchEvent(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mViewTask = ViewTask.PAN_SCREEN; //take precedence over all other actions
                mSwipeDownX = event.getX(0); //take first finger as reference by default getX() is getX(0)
                mSwipeDownY = event.getY(0);
                break;
            case MotionEvent.ACTION_DOWN:
                if(mViewTask ==  ViewTask.PAN_SCREEN)
                    break;
                mDownX = event.getX();
                mDownY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getX();
                float moveY = event.getY();

                if(mViewTask == ViewTask.PAN_SCREEN){
                    mShiftX = moveX-mSwipeDownX;
                    mShiftY = moveY-mSwipeDownY;
                    mSwipeDownX = moveX;
                    mSwipeDownY = moveY;
                    moveDrawables(true);
                    return true;
                }

                if(mClicked==null)
                    mClickedNodeSelected = false;
                else
                    mClickedNodeSelected = mClickedNodeSelected || mClicked.contains( mDownX, mDownY );

                if(!mClickedNodeSelected) {
                    return true;
                }

                if(mViewTask == ViewTask.MOVE_NODE) {
                    Node selectedNode = (Node) mClicked;
                    moveNode(selectedNode,moveX,moveY);
                    savePending = true;
                    postInvalidate();
                    return true;
                }
                else if(mViewTask==ViewTask.MOVE_EDGE){
                    mEdge.setEnd(moveX,moveY);
                    savePending = true;
                    postInvalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if(mViewTask == ViewTask.PAN_SCREEN){
                    mViewTask = ViewTask.IDLE;
                    return true;
                }
            case MotionEvent.ACTION_UP:
                if(mViewTask == ViewTask.PAN_SCREEN){
                    mViewTask = ViewTask.IDLE;
                    return true;
                }
                if(mViewTask == ViewTask.MOVE_NODE) {
                    mViewTask = ViewTask.IDLE;
                    mClickedNodeSelected = false;
                    return true;
                }
                if(mViewTask == ViewTask.MOVE_EDGE){
                    float upX = event.getX();
                    float upY = event.getY();
                    Node toNode =  (Node)findItem(upX, upY,DrawableType.NODE);
                    if(toNode == null || toNode == mEdge.getFromNode()){
                        if( mAllViewDrawables.size() > 0 )
                            mAllViewDrawables.remove( mAllViewDrawables.size() - 1 );
                    }
                    else {
                        mEdge.setToNode(toNode);
                        float[] centreXY = (toNode).centre();
                        mEdge.setEnd(centreXY[0],centreXY[1]);
                        mEdge.editable( false );
                    }
                    mEdge = null;
                    mClickedNodeSelected = false;
                    mViewTask = ViewTask.IDLE;
                    savePending = true;
                    postInvalidate();
                    return true;
                }
                break;
        }

//        postInvalidate();
        return true;
    }


    @Override
    public void onClick(View view){
        //Double CLick
        if(mClicked!=null){
           MindMapDrawable  pClicked = mClicked;
            //search in only correct viewable type
           mClicked = findItem(mDownX, mDownY,pClicked.type());
           if(pClicked == mClicked && System.currentTimeMillis()-mClickEnd < MAX_DOUBLE_CLICK_DURATION){
               mClickCount = 2;
               mClickEnd = System.currentTimeMillis();
               if(mClicked instanceof Node) {
                   if(mViewTask == ViewTask.IDLE) {
                       float[] centreXY = ((Node)mClicked).centre();
                       mEdge = new Edge( centreXY[0], centreXY[1], centreXY[0], centreXY[1],this );
                       mEdge.setFromNode((Node)mClicked);
                       mEdge.setArrowShape(lastSelectedArrowShape);
                       mEdge.setColorID(lastSelectedEdgeColor);
                       mEdge.setEdgeStrokeWidth(lastSelectedStrokeWidth);
                       mAllViewDrawables.add( mEdge );
                       mViewTask = ViewTask.MOVE_EDGE;
                       savePending = true;
                       postInvalidate();
                   }
               }
               return;
           }
        }

        //Single Click
        //search for node first
        mClicked = findItem(mDownX, mDownY,DrawableType.NODE);
        if(mClicked==null)
            mClicked = findItem(mDownX, mDownY,DrawableType.EDGE);
        if(mClicked!=null) {

            if(mClicked instanceof Node) {
                if( mViewTask == ViewTask.IDLE)
                    mViewTask = ViewTask.MOVE_NODE;
            }

            mClickCount = 1;
            mClickEnd = System.currentTimeMillis();
            return;
        }
        if(mViewTask != ViewTask.MOVE_NODE && mViewTask != ViewTask.MOVE_EDGE)
            mViewTask = ViewTask.IDLE;
        return;
    }


    @Override
    public boolean onLongClick(View view) {
        mLongClicked = findItem(mDownX, mDownY,DrawableType.NODE);
        if(mLongClicked==null)
            mLongClicked = findItem(mDownX, mDownY,DrawableType.EDGE);

        if( mViewTask == ViewTask.IDLE) {
            mViewTask = ViewTask.DETAILS_WINDOW;
            showDetailsWindow();
        }

        return true;
    }

    public void showDetailsWindow(){

        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View detailsLayout = layoutInflater.inflate(R.layout.details_window,null);
        final AlertDialog.Builder detailsAlertBuilder = new AlertDialog.Builder( mContext );
        detailsAlertBuilder.setView(detailsLayout);
        final AlertDialog detailsAlertDialog = detailsAlertBuilder.create();

        final TextView TitleTextView = detailsLayout.findViewById(R.id.title_text_view);
        if (mLongClicked==null)
            TitleTextView.setText( R.string.new_node );
        else {
            if(mLongClicked instanceof Node)
                TitleTextView.setText( R.string.edit_node );
            else
                TitleTextView.setText( R.string.edit_edge );
        }

        //if we're editing a drawable, populate the dialog with the current contents
        final EditText nameEditText = detailsLayout.findViewById(R.id.name_edit_text );
        final EditText descriptionEditText = detailsLayout.findViewById(R.id.description_edit_text );

        if (mLongClicked!=null) {
            nameEditText.setText( (mLongClicked).getTitle() );
            descriptionEditText.setText( (mLongClicked).getDescription() );
            mColorId = mLongClicked.getColorID();
            if(mLongClicked instanceof Node) {
                mNodeShape = ((Node) mLongClicked).getShape();
                mNodeRadius = ((Node) mLongClicked).getR();
            }
            else if(mLongClicked instanceof Edge) {
                mArrowShape = ((Edge) mLongClicked).getArrowShape();
                mEdgeStrokeWidth = ((Edge) mLongClicked).getEdgeStrokeWidth();
            }
        }


        //Populate with recently used node preferences
        if(mLongClicked == null){
            mColorId = lastSelectedNodeColor;
            mNodeShape = lastSelectedNodeShape;
            mNodeRadius=lastSelectedNodeRadius;
        }

        final ImageButton setButton = detailsLayout.findViewById(R.id.set_button );

        setButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(mLongClicked==null) {
                    addNode( mDownX, mDownY, nameEditText.getText().toString(), descriptionEditText.getText().toString(), mNodeShape, mColorId,mNodeRadius );
                    lastSelectedNodeColor = mColorId;
                    lastSelectedNodeShape = mNodeShape;
                    lastSelectedNodeRadius = mNodeRadius;
                }
                else{
                    (mLongClicked).setTitle( nameEditText.getText().toString() );
                    (mLongClicked).setDescription( descriptionEditText.getText().toString() );
                    (mLongClicked).setColorID(mColorId);

                    if(mLongClicked instanceof Node) {
                        ((Node)mLongClicked).setShape(mNodeShape);
                        ((Node)mLongClicked).setR(mNodeRadius);
                        lastSelectedNodeColor = mColorId;
                        lastSelectedNodeShape = mNodeShape;
                        lastSelectedNodeRadius = mNodeRadius;
                    }
                    else {
                        ((Edge) mLongClicked).setArrowShape(mArrowShape);
                        ((Edge) mLongClicked).setEdgeStrokeWidth(mEdgeStrokeWidth);
                        lastSelectedEdgeColor = mColorId;
                        lastSelectedArrowShape = mArrowShape;
                        lastSelectedStrokeWidth = mEdgeStrokeWidth;
                    }

                }
                detailsAlertDialog.dismiss();
                postInvalidate();
            }
        });

        if(mLongClicked!=null) { //only add delete button if we are editing
            final ImageButton deleteNodeButton = new ImageButton( mContext );
            deleteNodeButton.setBackgroundColor(Color.TRANSPARENT);
            deleteNodeButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            deleteNodeButton.setImageDrawable(getResources().getDrawable(R.drawable.delete));
            deleteNodeButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteItem( mLongClicked );
                    detailsAlertDialog.dismiss();
                }
            } );

            final LinearLayout buttonLinearLayout = detailsLayout.findViewById( R.id.button_linear_layout );
            buttonLinearLayout.addView( deleteNodeButton );
        }

        final ImageButton closeButton = detailsLayout.findViewById(R.id.close_button );
        closeButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detailsAlertDialog.dismiss();
            }
        });

        final ViewGroup row1 = detailsLayout.findViewById( R.id.color_palette_row_1 );
        final ViewGroup row2 = detailsLayout.findViewById( R.id.color_palette_row_2 );
        for (int i = 0; i < row1.getChildCount(); i++) {
            final View child = row1.getChildAt(i);
            if(mColorId == ((ColorDrawable) child.getBackground()).getColor()){
                if(mColorId == Color.BLACK || mColorId == Color.RED || mColorId ==Color.BLUE
                        || mColorId == -65409 || mColorId == -65281 || mColorId == -8453889)
                    ((ImageView)child).setImageDrawable(getResources().getDrawable(R.drawable.radio_white));
                else
                    ((ImageView)child).setImageDrawable(getResources().getDrawable(R.drawable.radio_black));
            }
            child.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mColorId = ((ColorDrawable) view.getBackground()).getColor();
                    for (int m = 0; m < row1.getChildCount(); m++) {
                        ((ImageView)row1.getChildAt(m)).setImageDrawable(getResources().getDrawable(R.drawable.color_box ));
                    }
                    for (int m = 0; m < row2.getChildCount(); m++) {
                        ((ImageView)row2.getChildAt(m)).setImageDrawable(getResources().getDrawable(R.drawable.color_box ));
                    }
                    if(mColorId == Color.BLACK || mColorId == Color.RED || mColorId ==Color.BLUE
                            || mColorId == -65409 || mColorId == -65281 || mColorId == -8453889)
                        ((ImageView)view).setImageDrawable(getResources().getDrawable(R.drawable.radio_white));
                    else
                        ((ImageView)view).setImageDrawable(getResources().getDrawable(R.drawable.radio_black));
                }
            });
        }
        for (int i = 0; i < row2.getChildCount(); i++) {
            final View child = row2.getChildAt(i);
            if(mColorId == ((ColorDrawable) child.getBackground()).getColor()){
                if(mColorId == Color.BLACK || mColorId == Color.RED || mColorId ==Color.BLUE
                        || mColorId == -65409 || mColorId == -65281 || mColorId == -8453889)
                    ((ImageView)child).setImageDrawable(getResources().getDrawable(R.drawable.radio_white));
                else
                    ((ImageView)child).setImageDrawable(getResources().getDrawable(R.drawable.radio_black));
            }
            child.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mColorId = ((ColorDrawable) view.getBackground()).getColor();
                    for (int m = 0; m < row1.getChildCount(); m++) {
                        ((ImageView)row1.getChildAt(m)).setImageDrawable(getResources().getDrawable(R.drawable.color_box ));
                    }
                    for (int m = 0; m < row2.getChildCount(); m++) {
                        ((ImageView)row2.getChildAt(m)).setImageDrawable(getResources().getDrawable(R.drawable.color_box ));
                    }
                    if(mColorId == Color.BLACK || mColorId == Color.RED || mColorId ==Color.BLUE
                            || mColorId == -65409 || mColorId == -65281 || mColorId == -8453889)
                        ((ImageView)view).setImageDrawable(getResources().getDrawable(R.drawable.radio_white));
                    else
                        ((ImageView)view).setImageDrawable(getResources().getDrawable(R.drawable.radio_black));
                }
            });
        }


        //Shape layout
        final LinearLayout shapeLinearLayout = detailsLayout.findViewById( R.id.shape_linear_layout );

        if(mLongClicked instanceof Edge) {
            final ImageButton leftArrowOnlyButton = new ImageButton( mContext );
            final ImageButton rightArrowOnlyButton = new ImageButton( mContext );
            final ImageButton doubleArrowButton = new ImageButton( mContext );
            final ImageButton noArrowButton = new ImageButton( mContext );

            leftArrowOnlyButton.setBackgroundColor(Color.TRANSPARENT);
            rightArrowOnlyButton.setBackgroundColor(Color.TRANSPARENT);
            doubleArrowButton.setBackgroundColor(Color.TRANSPARENT);
            noArrowButton.setBackgroundColor(Color.TRANSPARENT);

            switch (mArrowShape){
                case START:
                    if (mDownX > ((Edge) mLongClicked).getStartXY().x)
                        leftArrowOnlyButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    else
                        rightArrowOnlyButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    break;
                case END:
                    if (mDownX < ((Edge) mLongClicked).getStartXY().x)
                        leftArrowOnlyButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    else
                        rightArrowOnlyButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    break;
                case DOUBLE:
                    doubleArrowButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    break;
                case NONE:
                    noArrowButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    break;
            }

            leftArrowOnlyButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            leftArrowOnlyButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.left_arrow ));
            leftArrowOnlyButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mDownX > ((Edge) mLongClicked).getStartXY().x) {
                        mArrowShape = ArrowShape.START;
                    }
                    else {
                        mArrowShape = ArrowShape.END;
                    }
                    leftArrowOnlyButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    rightArrowOnlyButton.setBackgroundColor(Color.TRANSPARENT);
                    doubleArrowButton.setBackgroundColor(Color.TRANSPARENT);
                    noArrowButton.setBackgroundColor(Color.TRANSPARENT);
                }
            } );
            shapeLinearLayout.addView( leftArrowOnlyButton );

            rightArrowOnlyButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            rightArrowOnlyButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.right_arrow));
            rightArrowOnlyButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mDownX < ((Edge) mLongClicked).getStartXY().x)
                        mArrowShape = ArrowShape.START;
                    else
                        mArrowShape = ArrowShape.END;
                    leftArrowOnlyButton.setBackgroundColor(Color.TRANSPARENT);
                    rightArrowOnlyButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    doubleArrowButton.setBackgroundColor(Color.TRANSPARENT);
                    noArrowButton.setBackgroundColor(Color.TRANSPARENT);
                }
            } );
            shapeLinearLayout.addView( rightArrowOnlyButton );

            doubleArrowButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            doubleArrowButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.double_headed_arrow));
            doubleArrowButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mArrowShape = ArrowShape.DOUBLE;
                    leftArrowOnlyButton.setBackgroundColor(Color.TRANSPARENT);
                    rightArrowOnlyButton.setBackgroundColor(Color.TRANSPARENT);
                    doubleArrowButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    noArrowButton.setBackgroundColor(Color.TRANSPARENT);
                }
            } );
            shapeLinearLayout.addView( doubleArrowButton );

            noArrowButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            noArrowButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.none_arrow));
            noArrowButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mArrowShape = ArrowShape.NONE;
                    leftArrowOnlyButton.setBackgroundColor(Color.TRANSPARENT);
                    rightArrowOnlyButton.setBackgroundColor(Color.TRANSPARENT);
                    doubleArrowButton.setBackgroundColor(Color.TRANSPARENT);
                    noArrowButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                }
            } );
            shapeLinearLayout.addView( noArrowButton );

            ArrayList<String> spinnerArray = new ArrayList<String>();
            spinnerArray.add(getResources().getString(R.string.edgehalf ));
            spinnerArray.add(getResources().getString(R.string.edgecurrent ));
            spinnerArray.add(getResources().getString(R.string.edgedouble ));
            spinnerArray.add(getResources().getString(R.string.size ));
            Spinner spinner = new Spinner(mContext);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item,spinnerArray) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {

                    View v = super.getView(position, convertView, parent);
                    if (position == getCount()) {
                        ((TextView)v.findViewById(android.R.id.text1)).setText("");
                        ((TextView)v.findViewById(android.R.id.text1)).setHint(getItem(getCount()));
                    }

                    return v;
                }

                @Override
                public int getCount() {
                    return super.getCount()-1; // you dont display last item. It is used as hint.
                }

            };

            spinner.setAdapter(adapter);
            spinner.setSelection(adapter.getCount());
            spinner.setOnItemSelectedListener( new Spinner.OnItemSelectedListener(){
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                    switch (position){
                        case 0:
                            mEdgeStrokeWidth /= 2;
                            break;
                        case 1:
                            mEdgeStrokeWidth *= 1;
                            break;
                        case 2:
                            mEdgeStrokeWidth *= 2;
                            break;
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            } );
            shapeLinearLayout.addView(spinner);
        }
        else{
            final ImageButton circleShapeButton = new ImageButton( mContext );
            final ImageButton squareShapeButton = new ImageButton( mContext );
            final ImageButton diamondShapeButton = new ImageButton( mContext );
            circleShapeButton.setBackgroundColor(Color.TRANSPARENT);
            squareShapeButton.setBackgroundColor(Color.TRANSPARENT);
            diamondShapeButton.setBackgroundColor(Color.TRANSPARENT);
            switch (mNodeShape){
                case CIRCLE:
                    circleShapeButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    break;
                case SQUARE:
                    squareShapeButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    break;
                case DIAMOND:
                    diamondShapeButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    break;
            }

            circleShapeButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            circleShapeButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.circle));
            circleShapeButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mNodeShape = NodeShape.CIRCLE;
                    circleShapeButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    squareShapeButton.setBackgroundColor(Color.TRANSPARENT);
                    diamondShapeButton.setBackgroundColor(Color.TRANSPARENT);
                }
            } );
            shapeLinearLayout.addView( circleShapeButton );

            squareShapeButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            squareShapeButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.square ));
            squareShapeButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mNodeShape = NodeShape.SQUARE;
                    circleShapeButton.setBackgroundColor(Color.TRANSPARENT);
                    squareShapeButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                    diamondShapeButton.setBackgroundColor(Color.TRANSPARENT);
                }
            } );
            shapeLinearLayout.addView( squareShapeButton );

            diamondShapeButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            diamondShapeButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.diamond ));
            diamondShapeButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mNodeShape = NodeShape.DIAMOND;
                    circleShapeButton.setBackgroundColor(Color.TRANSPARENT);
                    squareShapeButton.setBackgroundColor(Color.TRANSPARENT);
                    diamondShapeButton.setBackgroundColor(getResources().getColor(R.color.transparent50grey ));
                }
            } );

            shapeLinearLayout.addView( diamondShapeButton );

            ArrayList<String> spinnerArray = new ArrayList<String>();
            spinnerArray.add(getResources().getString(R.string.nodehalf ));
            spinnerArray.add(getResources().getString(R.string.nodecurrent ));
            spinnerArray.add(getResources().getString(R.string.nodedouble ));
            spinnerArray.add(getResources().getString(R.string.nodewrapText ));
            spinnerArray.add(getResources().getString(R.string.size ));

            Spinner spinner = new Spinner(mContext);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item,spinnerArray) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {

                    View v = super.getView(position, convertView, parent);
                    if (position == getCount()) {
                        ((TextView)v.findViewById(android.R.id.text1)).setText("");
                        ((TextView)v.findViewById(android.R.id.text1)).setHint(getItem(getCount()));
                    }

                    return v;
                }

                @Override
                public int getCount() {
                    return super.getCount()-1; // you dont display last item. It is used as hint.
                }

            };

            spinner.setAdapter(adapter);
            spinner.setSelection(adapter.getCount());

            spinner.setOnItemSelectedListener( new Spinner.OnItemSelectedListener(){
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                    switch (position){
                        case 0:
                            mNodeRadius /= 2;
                            break;
                        case 1:
                            mNodeRadius *= 1;
                            break;
                        case 2:
                            mNodeRadius *= 2;
                            break;
                        case 3:
                            mNodeRadius = Node.NODE_RADIUS_WARP_TEXT;
                            break;
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            } );
            shapeLinearLayout.addView(spinner);
        }

        detailsAlertDialog.show();
        detailsAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
            @Override
            public void onDismiss(DialogInterface dialog) {
                mLongClicked = null;
                mViewTask = ViewTask.IDLE;
            }
        } );
    }

    public MindMapDrawable findItem(float x, float y){
        return findItem(x,y,null);
    }

    public MindMapDrawable findItem(float x, float y, DrawableType type) {
        for (MindMapDrawable drawable : mAllViewDrawables)
            if (drawable.contains( (int)(x), (int)(y) ) && ((type == null)||(type == drawable.type())))
                return drawable;
        return null;
    }

    public void deleteItem(MindMapDrawable item) {
        if (item != null) {
            if (item instanceof Node) {
                Iterator<MindMapDrawable> iterator = mAllViewDrawables.iterator();
                //check all the items and see if there's an arrow pointing to the item we will remove
                while (iterator.hasNext()) {
                    MindMapDrawable drawable = iterator.next();
                    if (drawable instanceof Edge) {
                        Edge edge = (Edge) drawable;
                        if (edge.fromNode((Node)item)||edge.toNode((Node)item))
                            iterator.remove();
                    }
                }
            }
            mAllViewDrawables.remove(item);
        }
        savePending = true;
        postInvalidate();
    }

    public void moveNode(Node node,float moveX,float moveY) {
        Iterator<MindMapDrawable> iterator = mAllViewDrawables.iterator();
        while (iterator.hasNext()) {
            MindMapDrawable drawable = iterator.next();
            if (drawable instanceof Edge) {
                Edge edge = (Edge) drawable;
                if (edge.fromNode(node))
                    edge.setStart(moveX,moveY);
                if (edge.toNode(node))
                    edge.setEnd(moveX,moveY);
            }
        }
        node.set( moveX, moveY );
    }

    public void moveDrawables(boolean postInvalidate){
        for (MindMapDrawable drawable : mAllViewDrawables) {
            drawable.move(mShiftX,mShiftY);
        }
        savePending = true;
        if(postInvalidate)
            postInvalidate();
    }

    public void scaleDrawables(float focusX,float focusY,boolean postInvaclidate){
        for (MindMapDrawable drawable : mAllViewDrawables) {
            drawable.scale(mScaleFactor);
        }
        float scaledFocusX =  focusX*mChangeInscale;
        float scaledFocusY =  focusY*mChangeInscale;
        mShiftX = focusX - scaledFocusX;
        mShiftY = focusY - scaledFocusY;
        moveDrawables(postInvaclidate);
//        savePending = true;
//        postInvalidate();
    }

    /**
     * @return an ArrayList contianing all the drawables
     */
    public ArrayList<MindMapDrawable> getAllClassDrawables() {
        return mAllViewDrawables;
    }


    /**
     * @return a Bitmap object containing this View's items
     */
    public Bitmap getBitmap() {
        mExporting = true;
        float oldChnageInScale = mChangeInscale;
        if(mScaleFactor!=1) {
            mChangeInscale = 1 / mScaleFactor;
            scaleDrawables( getWidth(), getHeight(),false );
        }
        PointF topLeft = new PointF(0,0), bottomRight = new PointF( getWidth(),getHeight() );
        for (MindMapDrawable drawable : mAllViewDrawables) {
            if (drawable instanceof Node) {
                PointF p = ((Node) drawable).getXY();
                float r = ((Node) drawable).getR();
                if (topLeft.x > p.x - 2 * r)
                    topLeft.x = p.x - 2 * r;
                if (topLeft.y > p.y - 2 * r)
                    topLeft.y = p.y - 2 * r;
                if (bottomRight.x < p.x + 2 * r)
                    bottomRight.x = p.x + 2 * r;
                if (bottomRight.y < p.y + 2 * r)
                    bottomRight.y = p.y + 2 * r;
            }
        }
        Bitmap result = Bitmap.createBitmap((int)(bottomRight.x - topLeft.x), (int)(bottomRight.y - topLeft.y),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.WHITE);

        for (MindMapDrawable drawable : mAllViewDrawables){
            drawable.draw(canvas,topLeft);
        }

        if(mScaleFactor!=1) {
            mChangeInscale = mScaleFactor;
            scaleDrawables( getWidth(), getHeight(),false );
            mChangeInscale = oldChnageInScale;
        }

        mExporting = false;
        return result;
    }



}

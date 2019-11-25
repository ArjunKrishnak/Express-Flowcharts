package com.mindmap.graphnetwork;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

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

    private static final String TAG = "MainView";

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
    private int mColorId = Color.BLUE;//selected color from details_window
    private NodeShape mShape = NodeShape.CIRCLE;//selected shape from details_window

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
    float MIN_SCALE = 0.25f;
    float MAX_SCALE = 4f;
    float mChangeInscale = 1f;//TODO get rid of this?
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
    }
    public void resetSpace(float scale) {
        //TODO check if any resetting is missed.
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
            obj.put(JsonHelper.SCALE_KEY, this.mScaleFactor);
            obj.put(JsonHelper.ITEMS_KEY, arr);
            return obj;

        } catch (Exception e) {
            Toast.makeText(mContext, "Save error", Toast.LENGTH_LONG).show();
            Log.e(TAG,"Save error",e);
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
            scaleDrawables(mScaleFocusX,mScaleFocusY);
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
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setColor( Color.RED );
//        canvas.drawCircle(-100,-100,500,paint);
        for (MindMapDrawable drawable : mAllViewDrawables) {
            if (drawable.onScreen( getWidth(), getHeight() ))
                    drawable.draw(canvas);
        }

    }

    public void addDrawable(MindMapDrawable drawable) {
        mAllViewDrawables.add(drawable);
        savePending = true;
        postInvalidate();
    }

    public void addNode(float x,float y,String title,String description,NodeShape shape){
//        Node node = new Node(mContext);
//        node.initNode(x,y,this);
        Node node = new Node(x,y,this,title, description,shape);
        node.set( x,y-node.getR() );//To make it display above the new node button
        node.setColorID(mColorId);
        addDrawable(node);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
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
                    moveDrawables();
                    return true;
                }

                if(mClicked==null)
                    mClickedNodeSelected = false;
                else
                    mClickedNodeSelected = mClickedNodeSelected || mClicked.contains( mDownX, mDownY );

                if(!mClickedNodeSelected) {
                    Toast.makeText( mContext, "nothing to move", Toast.LENGTH_SHORT ).show();
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
                    if(toNode == null){
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
                       mAllViewDrawables.add( mEdge );
                       mViewTask = ViewTask.MOVE_EDGE;
                       savePending = true;
                       postInvalidate();
                   }
               }
//               Toast.makeText( mContext, "Double Clicked  node", Toast.LENGTH_SHORT ).show();
               return;
           }
        }

        //Single Click
        //search for node first
        mClicked = findItem(mDownX, mDownY,DrawableType.NODE);
        if(mClicked==null)
            mClicked = findItem(mDownX, mDownY,DrawableType.EDGE);
        if(mClicked!=null) {
//            Toast.makeText( mContext, "Single Clicked  node", Toast.LENGTH_SHORT ).show();

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

        //if we're editing a drawable, populate the dialog with the current contents
        final TextView TitleTextView = detailsLayout.findViewById(R.id.title_text_view);
        if (mLongClicked==null)
            TitleTextView.setText( R.string.new_node );
        else {
            if(mLongClicked instanceof Node)
                TitleTextView.setText( R.string.edit_node );
            else
                TitleTextView.setText( R.string.edit_edge );
        }

        final EditText nameEditText = detailsLayout.findViewById(R.id.name_edit_text );
        if (mLongClicked!=null)
            nameEditText.setText(( mLongClicked).getTitle());
        final EditText descriptionEditText = detailsLayout.findViewById(R.id.description_edit_text );
        if (mLongClicked!=null)
            descriptionEditText.setText((mLongClicked).getDescription());
        final ImageButton setButton = detailsLayout.findViewById(R.id.set_button );

        setButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mLongClicked==null)
                    addNode(mDownX,mDownY,nameEditText.getText().toString(),descriptionEditText.getText().toString(),mShape);
                else{
                    (mLongClicked).setTitle( nameEditText.getText().toString() );
                    (mLongClicked).setDescription( descriptionEditText.getText().toString() );
                    (mLongClicked).setColorID(mColorId);
                }
                detailsAlertDialog.dismiss();
                postInvalidate();
            }
        });

        if(mLongClicked!=null) { //only add delete button if we are editing
            final ImageButton deleteNodeButton = new ImageButton( mContext );
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

        detailsLayout.findViewById(R.id.imageview_color_palette_pink).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });
        detailsLayout.findViewById(R.id.imageview_color_palette_orange).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });

        detailsLayout.findViewById(R.id.imageview_color_palette_magenta).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });

        detailsLayout.findViewById(R.id.imageview_color_palette_grey).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });

        detailsLayout.findViewById(R.id.imageview_color_palette_green).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });

        detailsLayout.findViewById(R.id.imageview_color_palette_cyan).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });

        detailsLayout.findViewById(R.id.imageview_color_palette_chartreuse).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });
        detailsLayout.findViewById(R.id.imageview_color_palette_blue).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });
        detailsLayout.findViewById(R.id.imageview_color_palette_black).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });
        detailsLayout.findViewById(R.id.imageview_color_palette_sky_blue).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });
        detailsLayout.findViewById(R.id.imageview_color_palette_yellow).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });
        detailsLayout.findViewById(R.id.imageview_color_palette_spring_green).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });
        detailsLayout.findViewById(R.id.imageview_color_palette_red).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });
        detailsLayout.findViewById(R.id.imageview_color_palette_purple).setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mColorId = ((ColorDrawable) view.getBackground()).getColor();
            } });

        //Shape layout
        final LinearLayout shapeLinearLayout = detailsLayout.findViewById( R.id.shape_linear_layout );

        if(mLongClicked instanceof Edge) {
            final ImageButton leftArrowOnlyButton = new ImageButton( mContext );
            leftArrowOnlyButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            leftArrowOnlyButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.left_arrow ));
            leftArrowOnlyButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mDownX > ((Edge) mLongClicked).getStartXY().x)
                        ((Edge) mLongClicked).setArrowShape( ArrowShape.START );
                    else
                        ((Edge) mLongClicked).setArrowShape( ArrowShape.END );
                }
            } );
            shapeLinearLayout.addView( leftArrowOnlyButton );

            final ImageButton rightArrowOnlyButton = new ImageButton( mContext );
            rightArrowOnlyButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            rightArrowOnlyButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.right_arrow));
            rightArrowOnlyButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mDownX < ((Edge) mLongClicked).getStartXY().x)
                        ((Edge) mLongClicked).setArrowShape( ArrowShape.START );
                    else
                        ((Edge) mLongClicked).setArrowShape( ArrowShape.END );
                }
            } );
            shapeLinearLayout.addView( rightArrowOnlyButton );

            final ImageButton doubleArrowButton = new ImageButton( mContext );
            doubleArrowButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            doubleArrowButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.double_headed_arrow));
            doubleArrowButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((Edge) mLongClicked).setArrowShape( ArrowShape.DOUBLE );
                }
            } );
            shapeLinearLayout.addView( doubleArrowButton );

            final ImageButton noArrowButton = new ImageButton( mContext );
            noArrowButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            noArrowButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.none_arrow));
            noArrowButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((Edge) mLongClicked).setArrowShape( ArrowShape.NONE );
                }
            } );
            shapeLinearLayout.addView( noArrowButton );
        }
        else{
            final ImageButton circleShapeButton = new ImageButton( mContext );
            circleShapeButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            circleShapeButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.circle));
            circleShapeButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mLongClicked == null)
                        mShape = NodeShape.CIRCLE;
                    else
                        ((Node)mLongClicked).setShape(NodeShape.CIRCLE);
                }
            } );
            shapeLinearLayout.addView( circleShapeButton );

            final ImageButton squareShapeButton = new ImageButton( mContext );
            squareShapeButton.setLayoutParams( new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.MATCH_PARENT, 1 ) );
            squareShapeButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.square ));
            squareShapeButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mLongClicked == null)
                        mShape = NodeShape.SQUARE;
                    else
                        ((Node)mLongClicked).setShape(NodeShape.SQUARE);
                }
            } );
            shapeLinearLayout.addView( squareShapeButton );
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

    public void moveDrawables(){
        for (MindMapDrawable drawable : mAllViewDrawables) {
            drawable.move(mShiftX,mShiftY);
        }
        savePending = true;
        postInvalidate();
    }

    public void scaleDrawables(float focusX,float focusY){
        for (MindMapDrawable drawable : mAllViewDrawables) {
            drawable.scale(mScaleFactor);
        }
        float scaledFocusX =  focusX*mChangeInscale;
        float scaledFocusY =  focusY*mChangeInscale;
        mShiftX = focusX - scaledFocusX;
        mShiftY = focusY - scaledFocusY;
        moveDrawables();
//        savePending = true;
//        postInvalidate();
    }

    /**
     * @return an ArrayList contianing all the drawables
     */
    public ArrayList<MindMapDrawable> getAllClassDrawables() {
        return mAllViewDrawables;
    }
    

}

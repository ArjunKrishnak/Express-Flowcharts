package com.mindmap.graphnetwork;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private MainView mMainView;
    private float mButtonX,mButtonY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );
        mMainView = findViewById(R.id.MainViewID);
        Button addNodeButton = findViewById( R.id.AddNodeButton );
        addNodeButton.setOnTouchListener( new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // save the X,Ycoordinates
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mButtonX = event.getX();
                    mButtonY = event.getY();
                }
                return false;
            }
        } );
        addNodeButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mMainView.addNode(mButtonX,mButtonY);
            }
        } );

        Button zoomButton = findViewById( R.id.ZoomButton );
        zoomButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mMainView.zoom(2);
            }
        } );

        Button zoomOutButton = findViewById( R.id.zoomOutButton );
        zoomOutButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mMainView.zoom(0.5f);
            }
        } );
    }





}

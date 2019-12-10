package com.mindmap.expressFlowchart;

import android.graphics.Canvas;
import android.graphics.PointF;

import org.json.JSONObject;

/**
 * Any item that can be selected in a MindMapGraph
 */
public interface MindMapDrawable {
    DrawableType type();

    /**
     * Draw the item in a given Canvas
     * @param c Canvas on which to draw the MindMapDrawable
     */
    void draw(Canvas c);
    void draw(Canvas c, PointF reference);

    /**
     * returns if the item is in vieport of screen
     * @param height,width of the screen
     */
    boolean onScreen(float width ,float height);

    /**
     * move the drawable as the screen moves
     */
    void move(float shiftX,float shiftY);

    /**
     * scale the drawable as the screen moves
     */
    void scale(float mScaleFactor);

    /**
     * Check if the given points lie inside the UmlDrawable
     * @param x coordinate of point
     * @param y coordinate of point
     * @return true if this MindMapDrawable contains the point, false otherwise
     */
    boolean contains(float x, float y);

    /**
     * @return a JSONObject representation of this MindMapDrawable
     */
    JSONObject toJson();

    /**
     * @return a Unique String Id of this MindMapDrawable
     */
    String getId();

    String getTitle();
    void setTitle(String title);

    String getDescription();
    void setDescription(String title);

    int getColorID();
    void setColorID(int colorID);

}


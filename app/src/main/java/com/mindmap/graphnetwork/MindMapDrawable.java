package com.mindmap.graphnetwork;

import android.graphics.Canvas;
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
}


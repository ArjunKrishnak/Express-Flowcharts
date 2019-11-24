package com.mindmap.graphnetwork;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

/**
 * Use to manage Files, i.e. saving and loading
 */
public final class JsonHelper {
    private static final String TAG = "JsonHelper";

    /** Location for all saved data*/
    public final File MIND_MAP_FOLDER;
    /** Location for all saved photos */
    public final File PICTURES_FOLDER;
    /** Extension for saved files */
    public static final String EXTENSION = ".map";
    /** Extension used for images */
    public static final String IMG_EXTENSION = ".png";

    /** key used to get an items */
    public static final String ITEMS_KEY = "items";
    /** key used to get an items */
    public static final String SCALE_KEY = "scale";
    /** key used to get an item type */
    public static final String ITEM_TYPE_KEY = "drawable_type";
    /** key used to get an item id */
    public static final String ITEM_ID_KEY = "drawable_id";

    public class NodeSchema{
        NodeSchema(){};
        public static final String NODE_CENTRE_X_KEY = "node_centre_x";
        public static final String NODE_CENTRE_Y_KEY = "node_centre_y";
        public static final String NODE_RADIUS_KEY = "node_radius";
        public static final String NODE_TITLE_KEY = "node_title";
        public static final String NODE_DESCRIPTION_KEY = "node_description";
        public static final String NODE_COLOR_KEY = "node_color";
        public static final String NODE_SHAPE_KEY = "node_shape";
    }

    public class EdgeSchema{
        EdgeSchema(){};
        public static final String EDGE_START_NODE_KEY = "edge_start_node";
        public static final String EDGE_END_NODE_KEY = "edge_end_node";
        public static final String EDGE_STROKE_WIDTH_KEY = "edge_stroke_width";
        public static final String EDGE_TITLE_KEY = "edge_title";
        public static final String EDGE_DESCRIPTION_KEY = "edge_description";
        public static final String EDGE_COLOR_KEY = "edge_color";
        public static final String EDGE_ARROW_TYPE_KEY = "edge_arrow_type";
    }

    //TODO why was this private ctor in vdroid
    public JsonHelper(Context context) {
        MIND_MAP_FOLDER = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/mindmap/");
        PICTURES_FOLDER = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/pictures/");
        if (!MIND_MAP_FOLDER.exists())
            if (!MIND_MAP_FOLDER.mkdir())
                Log.e(TAG, "could not create MIND_MAP_FOLDER directory ["
                        + MIND_MAP_FOLDER.getAbsolutePath() + "]");

        if (!PICTURES_FOLDER.exists()) {
            Log.w(TAG, "initialize: Pictures directory did not already exist. Attempting to create it now");
            if (!PICTURES_FOLDER.mkdir())
                Log.e(TAG, "initialize: could not create pictures directory ["
                + PICTURES_FOLDER.getAbsolutePath() + "]");
        }
    }

    /**
     * Saves the given contents to the given location
     * Will overwrite if the file already exists
     *
     * @param obj contents to sv
     * @param f   location to fileSave the object
     * @param ctx used to throw toast
     * @return true if fileSave was successful, false otherwise
     */
    public static boolean writeFile(Object obj, File f, Context ctx) {
        if (obj instanceof JSONObject)
            return writeJsonFile((JSONObject) obj, f, ctx);
        else if (obj instanceof Bitmap)
            return writeImageFile((Bitmap) obj, f, ctx);
        else {
            Log.w(TAG, "writeFile: not designed to handle [" + obj.getClass().getSimpleName() + "]");
            return false;
        }
    }

    /**
     * Writes an image file at the given location
     * Will overwrite if it already exists
     *
     * @param image           to be written
     * @param destinationFile where to write the given image
     * @param ctx Context of the application
     * @return true if the file was written successfully, false otherwise
     */
    public static boolean writeImageFile(Bitmap image, File destinationFile, Context ctx) {
        FileOutputStream out = null;
        try {
            if (destinationFile.exists()) destinationFile.delete();
            out = new FileOutputStream(destinationFile);
            image.compress( Bitmap.CompressFormat.PNG, 100, out); // PNG is lossless --> 100 is ignored
            Toast.makeText(ctx, R.string.save_successful, Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "writeImageFile: Error:", e);
            Toast.makeText(ctx, R.string.save_error, Toast.LENGTH_SHORT).show();
            return false;
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                //do nothing
            }
        }
    }

    /**
     * Writes an JSONObject at the given location
     * Will overwrite if it already exists
     *
     * @param jObj            to be written
     * @param destinationFile where to write the given JSONObject
     * @param ctx Context of the application
     * @return true if the file was written successfully, false otherwise
     */
    public static boolean writeJsonFile(JSONObject jObj, File destinationFile, Context ctx) {
        try {
            destinationFile.createNewFile();
            PrintWriter out = new PrintWriter(destinationFile);
            out.print(jObj.toString());
            out.close();
            Toast.makeText(ctx, R.string.save_successful, Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "writeFile: ", e);
            Toast.makeText(ctx, R.string.save_error, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Return the JSONObject contents of a given File
     *
     * @param f   File where the JSONObject is located
     * @param ctx used to throw a toast
     * @return JSONObject contents of a given file
     */
    public static JSONObject getJsonFromFile(File f, Context ctx) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            return new JSONObject(reader.readLine());
        } catch (Exception e) {
            Toast.makeText(ctx, R.string.load_error, Toast.LENGTH_LONG).show();
            Log.e(TAG, "loadItem: ", e);
            return null;
        }
    }

    public static String getUniqueID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

}

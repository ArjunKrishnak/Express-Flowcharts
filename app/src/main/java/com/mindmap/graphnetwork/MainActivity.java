package com.mindmap.graphnetwork;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;

public class MainActivity extends AppCompatActivity {
    private MainView mMainView;
    private float mButtonX,mButtonY;
    private static final String TAG = "MainActivity";
    private FileHelper mFileHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );
        mMainView = findViewById(R.id.MainViewID);
        mFileHelper = new FileHelper(this);

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

        Button saveButton = findViewById( R.id.save_button );
        saveButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFile();
            }
        } );

        Button loadButton = findViewById( R.id.load_button );
        loadButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFile();
            }
        } );
    }

    /**
     * Warns the user that the file exists and prompts the user to overwrite the file
     *
     * @param toWrite           contents the user wants to fileSave
     * @param destFile          where the user wants to fileSave the contents
     * @param updateSavePending update the editorView's savePending boolean,
     */
    private void warnOverwrite(final Object toWrite, final File destFile, final boolean updateSavePending) {
        AlertDialog.Builder overwriteWarning = new AlertDialog.Builder(this);
        overwriteWarning.setTitle(R.string.overwrite_dialog_title);
        overwriteWarning.setMessage(R.string.overwrite_dialog_body);
        overwriteWarning.setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (FileHelper.writeFile(toWrite, destFile, getApplicationContext()))
                    if (updateSavePending)
                        mMainView.setSavePending(false); //only call setSavePending if we're told to change it
            }
        });
        overwriteWarning.setNegativeButton(R.string.no_str, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); //do nothing
            }
        });
        overwriteWarning.show();
    }

    /**
     * check if the desired file exists before saving
     * if the file does exist, then warn the user about overwriting the file before saving
     *
     * @param obj      contents to fileSave
     * @param destFile location to fileSave the JSONObject
     */
    public void checkAndSaveJson(final JSONObject obj, final File destFile) {
        try {
//            currentFile = destFile;

            if (!destFile.exists())
                FileHelper.writeFile(obj, destFile, this);
            else //the file already exists, warn the user that it will be overwritten
                warnOverwrite(obj, destFile, true);
        } catch (Exception e) {
            Toast.makeText(this, R.string.save_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "saveJsonToDisk: ", e);
        }
    }


    /**
     * helper method for checkAndSaveJson
     * Can pass a String instead of a File
     *
     * @param obj      contents to fileSave
     * @param fileName name for the File
     */
    public void checkAndSaveJson(final JSONObject obj, String fileName) {
        checkAndSaveJson(obj, new File(mFileHelper.MIND_MAP_FOLDER.getAbsolutePath() + "/"
                + fileName + FileHelper.EXTENSION));
    }


    /**
     * Allow the user to pick a name for the file they're saving
     */
    public void saveFile() {
        if (mMainView.isEmpty()) {
            Toast.makeText( getApplicationContext(), "nothing to save", Toast.LENGTH_SHORT ).show(); //if the current working area is empty, warn the user and do not fileSave anything
            return;
        }
        else {
            final JSONObject obj = mMainView.toJson();
            AlertDialog.Builder saveFileBuilder = new AlertDialog.Builder(this);
            final EditText fileNameEditText = new EditText(this);
            fileNameEditText.setHint(R.string.file_name_hint);
            fileNameEditText.setSingleLine();
            saveFileBuilder.setTitle(R.string.save_as);
            saveFileBuilder.setView(fileNameEditText);
            saveFileBuilder.setPositiveButton(R.string.done_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //check if the file exists and warn the user if it does
                    checkAndSaveJson(obj, fileNameEditText.getText().toString());
                    mMainView.setSavePending(false); //once we've saved, we don't have changes pending
                }
            });
            saveFileBuilder.setNegativeButton(R.string.cancel_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            saveFileBuilder.show();
        }
    }

    /**
     * Lists available items by calling listItems if there are no changes pending
     */
    public void loadFile() {
        if (mMainView.getSavePending()) { // if we have changes pending then warn the user
            AlertDialog.Builder changesPendingBuilder = new AlertDialog.Builder(this);
            changesPendingBuilder.setTitle(R.string.changes_pending_dialog_title);
            changesPendingBuilder.setMessage(R.string.changes_pending_dialog_body);
            changesPendingBuilder.setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listItems(); //list the items, the pending changes will be lost
                }
            });
            changesPendingBuilder.setNegativeButton(R.string.no_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss(); // just go back to editing
                }
            });
            changesPendingBuilder.show();
        } else
            listItems(); // we don't have changes pending, just list the items
    }

    /**
     * List the available items and allows the user to pick one to load
     */
    public void listItems() {
        try {
            //get all the .map files from the directory
            final File MindMapFiles[] = mFileHelper.MIND_MAP_FOLDER.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(FileHelper.EXTENSION.toLowerCase());
                }
            });

            //if there are files to list, show them and ask the user to pick one
            if (MindMapFiles != null && MindMapFiles.length > 0) {
                final String fileNames[] = new String[MindMapFiles.length];
                for (int i = 0; i < MindMapFiles.length; i++)
                    fileNames[i] = MindMapFiles[i].getName().substring(0, MindMapFiles[i].getName().length()
                            - FileHelper.EXTENSION.length());
                AlertDialog.Builder listBuilder = new AlertDialog.Builder(this);

                listBuilder.setItems(fileNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadFromFile(MindMapFiles[which]); //when one is selected, fileLoad it
                    }
                });
                listBuilder.setNegativeButton(R.string.cancel_str, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); //the user hit "cancel"
                    }
                });
                listBuilder.setTitle(R.string.pick_file_title);
                listBuilder.show(); //show this dialog
            } else {
                //there are no violet files on the device, let the user know
                AlertDialog.Builder noFilesAlert = new AlertDialog.Builder(this);
                noFilesAlert.setTitle(R.string.no_mindmap_items_dialog_title);
                noFilesAlert.setMessage(R.string.no_mindmap_items_dialog_body);
                noFilesAlert.setPositiveButton(R.string.ok_str, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                noFilesAlert.show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.load_list_error, Toast.LENGTH_LONG).show();
            Log.e(TAG, "listLoadableItems: ", e);
        }
    }

    /**
     * loads a saved state from the given File
     * Logs and shows a Toast when an exception is encountered
     *
     * @param f File with saved state to fileLoad
     */
    private void loadFromFile(File f) {
        JSONObject obj = FileHelper.getJsonFromFile(f, this); //create a JSONObject from the File
        mMainView.resetSpace(); //clear the view
        //currentFile = f; //this is referenced later on in saveAs
        try {
            //get a JSONArray with all the items
            JSONArray arr = obj.getJSONArray(mMainView.ITEMS_KEY);

            //for each item, add it to the view
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString(FileHelper.ITEM_TYPE_KEY).equals(Node.class.getName()))
                    mMainView.addDrawable(Node.fromJson(arr.getJSONObject(i)));
                else if (arr.getJSONObject(i).getString(FileHelper.ITEM_TYPE_KEY).equals(Edge.class.getName()))
                    mMainView.addDrawable(Edge.fromJson(arr.getJSONObject(i),mMainView.getAllClassDrawables()));
            }

            mMainView.setSavePending(false); //when we fileLoad, there are no more saves pending

        } catch (Exception e) {
            Log.e(TAG, "loadFromJSON: ", e);
            Toast.makeText(this, R.string.load_error, Toast.LENGTH_LONG).show();
        }
    }


}

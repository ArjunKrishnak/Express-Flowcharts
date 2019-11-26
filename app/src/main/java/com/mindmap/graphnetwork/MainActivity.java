package com.mindmap.graphnetwork;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;

public class MainActivity extends AppCompatActivity {
    private MainView mMainView;
    private Point mButtonXY;
    private static final String TAG = "MainActivity";
    private JsonHelper mFileHelper;
    private File mCurrentFile = null;
    PopupWindow mColorPallettePopupWindow;

    /** returns left top location of a view **/
    private Point getViewLocation(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0] ;
        int y = location[1] ;
        return new Point(x, y);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        // Hide the status bar.
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        // Hide action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        setContentView( R.layout.activity_main );
        mMainView = findViewById(R.id.MainViewID);
        mFileHelper = new JsonHelper(this);

        ImageButton optionsButton =findViewById(R.id.options_button );
        optionsButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileDialog();
            }
        } );
    }

    /**
     * Shows a dialog, lets the user choose file options
     */
    private void fileDialog() {
        AlertDialog.Builder fileDialogBuilder = new AlertDialog.Builder(this);
        fileDialogBuilder.setTitle(R.string.file);
        fileDialogBuilder.setItems(R.array.file_options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        newWorkingArea();
                        break;
                    case 1:
                        loadFile();
                        break;
                    case 2:
                        saveFile();
                        break;
                    case 3:
                        //exportPrompt(); //TODO add export functionality
                        break;
                    case 4:
                        listItems(true);
                        break;
                    default:
                        break;
                }
            }
        });
        fileDialogBuilder.setNegativeButton(R.string.cancel_str, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        fileDialogBuilder.show();
    }

    /**
     * Prompt the user if they want to reset the working area
     */
    private void newWorkingArea() {
        //only show the dialog if it makes sense to do so
        if (!mMainView.isEmpty() && mMainView.getSavePending()) {

            AlertDialog.Builder resetAreaDialog = new AlertDialog.Builder(this);
            resetAreaDialog.setTitle(R.string.new_class_diagram_dialog_title);
            //the message is dependent on if unsaved changes are present
            resetAreaDialog.setMessage(mMainView.getSavePending() ? R.string.changes_pending_dialog_body
                    : R.string.new_class_diagram_dialod_body);
            resetAreaDialog.setNegativeButton(R.string.no_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss(); //do nothing, the user hit "no"
                }
            });
            resetAreaDialog.setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //reset the working area
                    mMainView.setSavePending(false);
                    mMainView.resetSpace();
                    mCurrentFile = null;
                }
            });
            resetAreaDialog.show();
        }
        else{
            mMainView.setSavePending(false);
            mMainView.resetSpace();
        }
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
                if (JsonHelper.writeFile(toWrite, destFile, getApplicationContext()))
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
            mCurrentFile = destFile;
            if (!destFile.exists())
                JsonHelper.writeFile(obj, destFile, this);
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
                + fileName + JsonHelper.EXTENSION));
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
    public void deleteFile(File file) {
        if(file!=null){
            if (file.exists()) {
                if (file.delete()) {
                    Toast.makeText( this,"File Deleted",Toast.LENGTH_SHORT ).show();
                } else {
                    Toast.makeText( this,"File Not Deleted",Toast.LENGTH_SHORT ).show();
                }
            }
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
                    listItems(false); //list the items, the pending changes will be lost
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
            listItems(false); // we don't have changes pending, just list the items
    }

    /**
     * List the available items and allows the user to pick one to load
     * delete argument if true means list called by delete else called by load
     */
    public void listItems(final boolean delete) {
        try {
            //get all the .map files from the directory
            final File MindMapFiles[] = mFileHelper.MIND_MAP_FOLDER.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith( JsonHelper.EXTENSION.toLowerCase());
                }
            });

            //if there are files to list, show them and ask the user to pick one
            if (MindMapFiles != null && MindMapFiles.length > 0) {
                final String fileNames[] = new String[MindMapFiles.length];
                for (int i = 0; i < MindMapFiles.length; i++)
                    fileNames[i] = MindMapFiles[i].getName().substring(0, MindMapFiles[i].getName().length()
                            - JsonHelper.EXTENSION.length());
                AlertDialog.Builder listBuilder = new AlertDialog.Builder(this);

                listBuilder.setItems(fileNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(delete){
                            deleteFile(MindMapFiles[which]);
                        }
                        else {
                            loadFromFile( MindMapFiles[which] ); //when one is selected, fileLoad it
                        }
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
        JSONObject obj = JsonHelper.getJsonFromFile(f, this); //create a JSONObject from the File
        mCurrentFile = f; //this is referenced later on in saveAs and deleteFile

        //currentFile = f; //this is referenced later on in saveAs
        try {
            mMainView.resetSpace((float)obj.getDouble(JsonHelper.SCALE_KEY)); //clear the view
            //get a JSONArray with all the items
            JSONArray arr = obj.getJSONArray(JsonHelper.ITEMS_KEY);

            //for each item, add it to the view
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString( JsonHelper.ITEM_TYPE_KEY).equals(Node.class.getName()))
                    mMainView.addDrawable(Node.fromJson(arr.getJSONObject(i),mMainView));
                else if (arr.getJSONObject(i).getString( JsonHelper.ITEM_TYPE_KEY).equals(Edge.class.getName()))
                    mMainView.addDrawable(Edge.fromJson(arr.getJSONObject(i),mMainView.getAllClassDrawables(),mMainView));
            }

            mMainView.setSavePending(false); //when we fileLoad, there are no more saves pending

        } catch (Exception e) {
            Log.e(TAG, "loadFromJSON: ", e);
            Toast.makeText(this, R.string.load_error, Toast.LENGTH_LONG).show();
        }
    }


}

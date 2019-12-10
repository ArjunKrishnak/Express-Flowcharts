package com.mindmap.expressFlowchart;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    private MainView mMainView;
    private FileHelper mFileHelper;
    private File mCurrentFile = null;
    private InterstitialAd mInterstitialAd;
    private boolean mAdCalledByNew;
    ViewGroup mRoot;
    View mOptions;
    private String CHANNEL_ID = "Foreground export service channel";


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

        createNotificationChannel();

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService( Context.LAYOUT_INFLATER_SERVICE);
        setContentView( R.layout.activity_main);
        mMainView = findViewById(R.id.MainViewID);
        mRoot = findViewById(R.id.RootConstraintView);
        mFileHelper = new FileHelper(this);

        mOptions = layoutInflater.inflate(R.layout.options,null);
        mOptions.setVisibility(View.GONE);
        mRoot.addView(mOptions);

        InitOptions();

        ImageButton optionsButton =findViewById(R.id.options_button );
        optionsButton.setOnClickListener( new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              if(mOptions.getVisibility()==View.VISIBLE)
                  closeOptions();
              else
                  openOptions();
          }
        } );

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.admob_interstitial_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                if(mAdCalledByNew) {
                    newWorkingArea();
                    closeOptions();
                }
                else{
                    exportFile();
                    closeOptions();
                }
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });

        SharedPreferences pref = this.getApplicationContext().getSharedPreferences(getString(R.string.preference_file_name) , Activity.MODE_PRIVATE );
        SharedPreferences.Editor editor = pref.edit();
        boolean launching_app_first_time = pref.getBoolean(getString(R.string.launching_time),true);
        if(launching_app_first_time) {
            editor.putBoolean( getString(R.string.launching_time), false );
            editor.apply();
            help(true);
        }
    }

    public void openOptions()
    {
        mOptions.setVisibility(View.VISIBLE);
    }
    public void closeOptions()
    {
        mOptions.setVisibility(View.GONE);
    }


    /**
     * initialize listeners for options menu
     */
    private void InitOptions(){
        LinearLayout linearlayoutMenuAbout = mOptions.findViewById(R.id.linearlayout_menu_about);
        LinearLayout linearlayoutMenuHelp = mOptions.findViewById(R.id.linearlayout_menu_help);
        LinearLayout linearlayoutMenuNew = mOptions.findViewById(R.id.linearlayout_menu_new_project);
        LinearLayout linearlayoutMenuOpen = mOptions.findViewById(R.id.linearlayout_menu_open);
        LinearLayout linearlayoutMenuSave = mOptions.findViewById(R.id.linearlayout_menu_save);
        LinearLayout linearlayoutMenuExport = mOptions.findViewById(R.id.linearlayout_menu_export);
        LinearLayout linearlayoutMenuDelete = mOptions.findViewById(R.id.linearlayout_menu_delete);

        linearlayoutMenuAbout.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                about();
                closeOptions();
            }
        } );
        linearlayoutMenuHelp.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                help(false);
                closeOptions();
            }
        } );
        linearlayoutMenuNew.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInterstitialAd.isLoaded()) {
                    mAdCalledByNew = true;
                    mInterstitialAd.show();
                }
                else {
                    newWorkingArea();
                    closeOptions();
                }
            }
        } );
        linearlayoutMenuOpen.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFile();
                closeOptions();
            }
        } );
        linearlayoutMenuSave.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFile();
                closeOptions();
            }
        } );
        linearlayoutMenuExport.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInterstitialAd.isLoaded()) {
                    mAdCalledByNew = false;
                    mInterstitialAd.show();
                }
                else {
                    exportFile();
                    closeOptions();
                }
            }
        } );
        linearlayoutMenuDelete.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listItems(true);
                closeOptions();
            }
        } );

    }

    /**
     * Shows the about dialog.
     */
    public void about()
    {

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View aboutLayout = layoutInflater.inflate(R.layout.about,null);
        final AlertDialog.Builder aboutDialogBuilder = new AlertDialog.Builder( this );
        aboutDialogBuilder.setView(aboutLayout);
        aboutDialogBuilder.setNegativeButton(R.string.cancel_str, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        aboutDialogBuilder.create().show();
    }

    /**
     * Shows the help dialog.
     */
    public void help(boolean calledAtLaunchTime)
    {

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View helpLayout = layoutInflater.inflate(R.layout.help,null);
        final AlertDialog.Builder helpDialogBuilder = new AlertDialog.Builder( this );
        helpDialogBuilder.setView(helpLayout);
        helpDialogBuilder.setNegativeButton(R.string.cancel_str, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        if(calledAtLaunchTime) {
            ImageView imageview_help_title = helpLayout.findViewById(R.id.imageview_help_title);
            imageview_help_title.setImageDrawable(getResources().getDrawable(R.drawable.welcome_icon));
            TextView textview_help_title = helpLayout.findViewById(R.id.textview_help_title);
            textview_help_title.setText(getString(R.string.welcome_text));
        }
        helpDialogBuilder.create().show();
    }

    /**
     * Prompt the user for a filename to use when saving the working area as an image
     */
    public void exportFile() {
        if (!mMainView.isEmpty()) { //continue if it's not empty
            AlertDialog.Builder exportImgDialogBuilder = new AlertDialog.Builder(this);
            exportImgDialogBuilder.setTitle(R.string.export_dialog_title);
            final EditText fileNameEditText = new EditText(this);
            fileNameEditText.setHint(R.string.file_name_hint);
            fileNameEditText.setSingleLine();
            //if we are currently working on a file, set the text to that file's name
            if (this.mCurrentFile != null)
                fileNameEditText.setText(mCurrentFile.getName().substring(0,
                        mCurrentFile.getName().length() - mFileHelper.EXTENSION.length()));
            fileNameEditText.selectAll();
            exportImgDialogBuilder.setView(fileNameEditText);
            exportImgDialogBuilder.setPositiveButton(R.string.ok_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //overrided later
                }
            });
            exportImgDialogBuilder.setNegativeButton(R.string.cancel_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            final AlertDialog exportImgDialog = exportImgDialogBuilder.create();
            exportImgDialog.show();

            exportImgDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ("".equals(fileNameEditText.getText().toString().trim())) {
                        fileNameEditText.setError(getString( R.string.file_name_required ));
                        return;
                    }
                    exportImg(fileNameEditText.getText().toString());
                    exportImgDialog.dismiss();
                }
            });


        } else //the working area is empty, let the user know & do nothing else
            Toast.makeText( getApplicationContext(), R.string.nothing_to_export, Toast.LENGTH_SHORT ).show();
    }

    private static final int PERMISSION_REQUEST_CODE = 200;
    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale( this, WRITE_EXTERNAL_STORAGE )) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder( this );
            alertBuilder.setCancelable( true );
            alertBuilder.setTitle( getString( R.string.permission_necessary_title ) );
            alertBuilder.setMessage( R.string.permission_necessary_message);
            alertBuilder.setPositiveButton( android.R.string.yes, new DialogInterface.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions( MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE );
                }
            } );
            AlertDialog alert = alertBuilder.create();
            alert.show();
        } else {
            ActivityCompat.requestPermissions( MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE );
        }

    }


    /**
     * exports the current editorView as an image
     * If the file already exists, warns the user about overwriting
     *
     * @param fileName to be used for the image
     */
    private void exportImg(String fileName) {
        Bitmap img = mMainView.getBitmap();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            File destFile = new File( mFileHelper.PICTURES_FOLDER, fileName + FileHelper.IMG_EXTENSION );
            if (destFile.exists()) //because we're saving this file as an image, we don't want to update the editorView's
                warnOverwrite( img, destFile, false );
            else {
                FileHelper.writeFile( img, destFile, this );
                Toast.makeText( this,R.string.file_saved_in + destFile.getAbsolutePath(), Toast.LENGTH_SHORT ).show();
            }
        }
        else {
            if (!checkPermission())
                requestPermission();
            if (!checkPermission()) {
                Toast.makeText( this, R.string.permission_necessary_message, Toast.LENGTH_SHORT ).show();
                return;
            }
            String ImagePath = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    img,
                    fileName,
                    ""
            );
            Toast.makeText( this, R.string.successfully_exported, Toast.LENGTH_SHORT ).show();
        }
    }

    /**
     * Prompt the user if they want to reset the working area
     */
    private void newWorkingArea() {
        //only show the dialog if it makes sense to do so
        if (!mMainView.isEmpty() && mMainView.getSavePending()) {

            AlertDialog.Builder resetAreaDialog = new AlertDialog.Builder(this);
            resetAreaDialog.setTitle(R.string.new_diagram_dialog_title );
            //the message is dependent on if unsaved changes are present
            resetAreaDialog.setMessage(mMainView.getSavePending() ? R.string.changes_pending_dialog_body
                    : R.string.new_diagram_dialod_body );
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
                if (FileHelper.writeFile(toWrite, destFile, getApplicationContext())) {
                    if (updateSavePending)
                        mMainView.setSavePending( false ); //only call setSavePending if we're told to change it
                    if(toWrite instanceof Bitmap)
                        Toast.makeText(getApplicationContext(),R.string.file_saved_in + destFile.getAbsolutePath(), Toast.LENGTH_SHORT ).show();
                    else
                        Toast.makeText(getApplicationContext(),R.string.file_saved_successfully, Toast.LENGTH_SHORT ).show();
                }
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
                FileHelper.writeFile(obj, destFile, this);
            else //the file already exists, warn the user that it will be overwritten
                warnOverwrite(obj, destFile, true);
        } catch (Exception e) {
            Toast.makeText(this, R.string.save_error, Toast.LENGTH_SHORT).show();
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
                   //overriden later
                }
            });
            saveFileBuilder.setNegativeButton(R.string.cancel_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            final AlertDialog saveFileDialog = saveFileBuilder.create();
            saveFileDialog.show();

            saveFileDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ("".equals(fileNameEditText.getText().toString().trim())) {
                        fileNameEditText.setError(getString( R.string.file_name_required ));
                        return;
                    }
                    //check if the file exists and warn the user if it does
                    checkAndSaveJson(obj, fileNameEditText.getText().toString());
                    mMainView.setSavePending(false); //once we've saved, we don't have changes pending
                    saveFileDialog.dismiss();
                }
            });
        }
    }

    /**
     * Lists available items by calling listItems if there are no changes pending
     */
    public void deleteFile(File file) {
        if(file!=null){
            if (file.exists()) {
                if (file.delete()) {
                    Toast.makeText( this,R.string.file_deleted,Toast.LENGTH_SHORT ).show();
                } else {
                    Toast.makeText( this,R.string.file_not_deleted,Toast.LENGTH_SHORT ).show();
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
                    return name.toLowerCase().endsWith( FileHelper.EXTENSION.toLowerCase());
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
        }
    }

    /**
     * loads a saved state from the given File
     * shows a Toast when an exception is encountered
     *
     * @param f File with saved state to fileLoad
     */
    private void loadFromFile(File f) {
        JSONObject obj = FileHelper.getJsonFromFile(f, this); //create a JSONObject from the File
        mCurrentFile = f; //this is referenced later on in saveAs and deleteFile

        //currentFile = f; //this is referenced later on in saveAs
        try {
            mMainView.resetSpace((float)obj.getDouble( FileHelper.SCALE_KEY)); //clear the view
            //get a JSONArray with all the items
            JSONArray arr = obj.getJSONArray( FileHelper.ITEMS_KEY);

            //for each item, add it to the view
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString( FileHelper.ITEM_TYPE_KEY).equals(Node.class.getName()))
                    mMainView.addDrawable(Node.fromJson(arr.getJSONObject(i),mMainView));
                else if (arr.getJSONObject(i).getString( FileHelper.ITEM_TYPE_KEY).equals(Edge.class.getName()))
                    mMainView.addDrawable(Edge.fromJson(arr.getJSONObject(i),mMainView.getAllClassDrawables(),mMainView));
            }

            mMainView.setSavePending(false); //when we fileLoad, there are no more saves pending

        } catch (Exception e) {
            Toast.makeText(this, R.string.load_error, Toast.LENGTH_LONG).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Export service channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}

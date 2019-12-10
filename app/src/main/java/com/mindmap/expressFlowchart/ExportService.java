//package com.mindmap.expressFlowchart;
//
//import android.annotation.TargetApi;
//import android.app.AlertDialog;
//import android.app.Notification;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.os.Build;
//import android.os.IBinder;
//import android.provider.MediaStore;
//import android.widget.Toast;
//
//import androidx.annotation.Nullable;
//import androidx.core.app.ActivityCompat;
//import androidx.core.app.NotificationCompat;
//import androidx.core.content.ContextCompat;
//
//import java.io.File;
//
//import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
//
//public class ExportService extends Service {
//
//    public ExportService() {
//    }
//
//
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        // Let it continue running until it is stopped.
//        String CHANNEL_ID = intent.getStringExtra("channel_id");
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this,
//                0, notificationIntent, 0);
//
//        Notification notification = new NotificationCompat.Builder(this,CHANNEL_ID )
//                .setContentTitle(getString( R.string.export_notification_title ))
//                .setContentText("Exporting in progress")
//                .setSmallIcon( R.drawable.welcome_icon)
//                .setContentIntent(pendingIntent)
//                .build();
//
//        startForeground(1, notification);
//
//        Bitmap img = mMainView.getBitmap();
//        String fileName = intent.getStringExtra("file_name");
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
//            File destFile = new File(fileName);
//            FileHelper.writeImageFile( img, destFile, this);
//        }
//        else {
//            String ImagePath = MediaStore.Images.Media.insertImage(
//                    getContentResolver(),
//                    img,
//                    fileName,
//                    ""
//            );
//        }
//
//        return START_NOT_STICKY;
//    }
//}

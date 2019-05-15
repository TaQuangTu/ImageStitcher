package tan.examlple.com.javacoban.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import tan.examlple.com.javacoban.R;
import tan.examlple.com.javacoban.permission.RuntimePermissionHelper;

public class ResultActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSION_WRITE_EXTERNAL = 0;
    private static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL = 1;
    PhotoView photoView;
    Button btnBack, btnSave;
    RuntimePermissionHelper permissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        mapViews();
        setImageViewFromIntent();
        setOnViewClick();

        //init permission helper
        permissionHelper = new RuntimePermissionHelper(this);
    }

    protected void mapViews() {
        photoView = findViewById(R.id.photoViewResult);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setImageViewFromIntent() {
        Bitmap bmp = null;
        String filename = getIntent().getStringExtra("image");
        try {
            FileInputStream is = this.openFileInput(filename);
            bmp = BitmapFactory.decodeStream(is);
            photoView.setImageBitmap(bmp);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setOnViewClick() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT<23){
                    //just do work without checking permission
                    storeImage(((BitmapDrawable) photoView.getDrawable()).getBitmap());
                    return;
                }
                //if android version is greater than 22
                if (permissionHelper.permissionAlreadyGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        storeImage(((BitmapDrawable) photoView.getDrawable()).getBitmap());
                } else {
                    permissionHelper.requestPermisstion(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_PERMISSION_READ_EXTERNAL);
                }

            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResultActivity.this.onBackPressed();
            }
        });
    }

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Toast.makeText(this, "Saving fails", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();
            fos.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        Log.d("ac", "getOutputMediaFile: "+Environment.getExternalStorageState());
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + "ImageSticher"
                + "/Images");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName = "MI_" + timeStamp + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION_READ_EXTERNAL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //continue working
                btnSave.performClick();
            } else {
                //permission denied

            }
        }
    }
}

package tan.examlple.com.javacoban.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.FileInputStream;

import tan.examlple.com.javacoban.R;
import tan.examlple.com.javacoban.permission.RuntimePermissionHelper;
import tan.examlple.com.javacoban.storage.BitmapStorageHelper;

public class ResultActivity extends AppCompatActivity implements BitmapStorageHelper.BitmapSavingListener {
    private static final int REQUEST_CODE_PERMISSION_WRITE_EXTERNAL = 0;
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
                if (Build.VERSION.SDK_INT < 23) {
                    //just do work without checking permission
                    Toast.makeText(ResultActivity.this, "Saving...", Toast.LENGTH_SHORT).show();
                    BitmapStorageHelper.getInstance().storagePhotoViewByNewThread(ResultActivity.this, photoView);
                    return;
                }
                //if android version is greater than 22
                if (permissionHelper.permissionAlreadyGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(ResultActivity.this, "Saving...", Toast.LENGTH_SHORT).show();
                    BitmapStorageHelper.getInstance().storagePhotoViewByNewThread(ResultActivity.this, photoView);
                } else {
                    permissionHelper.requestPermisstion(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_CODE_PERMISSION_WRITE_EXTERNAL);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION_WRITE_EXTERNAL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //continue working
                btnSave.performClick();
            } else {
                //permission denied
            }
        }
    }

    @Override
    public void onSaveStatusChange(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

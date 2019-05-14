package tan.examlple.com.javacoban.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import tan.examlple.com.javacoban.R;
import tan.examlple.com.javacoban.dialog.DialogWaiting;
import tan.examlple.com.javacoban.imageprocess.ImageProcessThread;
import tan.examlple.com.javacoban.imageprocess.ImageProcessThread.ImageProcessingListener;

public class MainActivity extends AppCompatActivity implements ImageProcessingListener {

    private static int PICK_IMAGE_1 = 1;
    private static int PICK_IMAGE_2 = 2;

    public static String STR_BITMAP_RESULT = "BITMAP_RESULT";
    public static String STR_BUNDLE = "BUNDLE";
    private DialogWaiting dialogWaiting;
    private Button btnStitch, btnBack;
    private ImageView imv1;
    private ImageView imv2, imvResult;
    private ImageProcessThread processThread;
    private ProgressBar progressBar;
    private ConstraintLayout cstImageContainer;
    private ConstraintLayout cstResultContainer;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    //TODO: openCV code goes there
                    processThread = new ImageProcessThread(MainActivity.this, dialogWaiting);
                    processThread.execute(imv1, imv2);
                    Log.i("OpenCV", "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapViews();
        initDailogWaiting();
        setOnClick();
    }

    private void mapViews() {
        imv1 = findViewById(R.id.imvFirstImage);
        imv2 = findViewById(R.id.imvSecondImage);
        btnStitch = findViewById(R.id.btnStitch);
        btnBack = findViewById(R.id.btnBack);
        imvResult = findViewById(R.id.imvResult);
        progressBar = findViewById(R.id.progressBarWaiting);
        cstResultContainer = findViewById(R.id.cstResultContainer);
        cstImageContainer = findViewById(R.id.cstImageContainer);
    }

    private void setOnClick() {
        btnStitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!OpenCVLoader.initDebug()) {
                    Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, MainActivity.this, mLoaderCallback);
                } else {
                    Log.d("OpenCV", "OpenCV library found inside package. Using it!");
                    mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                }
            }
        });
        imv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImageFromGallery(PICK_IMAGE_1);
            }
        });
        imv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImageFromGallery(PICK_IMAGE_2);
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToStart();
            }
        });
        imvResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                cstResultContainer.setVisibility(View.GONE);
                btnBack.setVisibility(View.GONE);
                Bitmap bitmapResult = ((BitmapDrawable) imvResult.getDrawable()).getBitmap();

                try {
                    //Write file
                    String filename = "bitmap.png";
                    FileOutputStream stream = MainActivity.this.openFileOutput(filename, Context.MODE_PRIVATE);
                    bitmapResult.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    //Cleanup
                    stream.close();
                    bitmapResult.recycle();

                    //Pop intent
                    Intent in1 = new Intent(MainActivity.this, ResultActivity.class);
                    in1.putExtra("image", filename);
                    startActivity(in1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void setUIAfterRun(Bitmap result) {
        //TODO: open dialog
        dialogWaiting.dismiss();
        cstImageContainer.setVisibility(View.GONE);
        cstResultContainer.setVisibility(View.VISIBLE);
        imvResult.setImageBitmap(result);
        btnStitch.setVisibility(View.GONE);
        btnBack.setVisibility(View.VISIBLE);
    }

    @Override
    public void setUIBeforeRun() {
        //TODO: turn of dialog
        dialogWaiting.show();
    }

    private void initDailogWaiting() {
        dialogWaiting = new DialogWaiting(this);
    }

    private void chooseImageFromGallery(int possition) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        int targetImageCode = PICK_IMAGE_1;
        if (possition == 2) targetImageCode = PICK_IMAGE_2;
        startActivityForResult(photoPickerIntent, targetImageCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                if (requestCode == PICK_IMAGE_1) imv1.setImageBitmap(selectedImage);
                if (requestCode == PICK_IMAGE_2) imv2.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(MainActivity.this, "You haven't picked Image", Toast.LENGTH_LONG).show();
        }
    }

    public void onResume() {
        super.onResume();
        btnStitch.setVisibility(View.VISIBLE);
        cstImageContainer.setVisibility(View.VISIBLE);

        cstResultContainer.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        btnBack.setVisibility(View.GONE);

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        }
    }

    private void backToStart() {
        btnBack.setVisibility(View.GONE);
        btnStitch.setVisibility(View.VISIBLE);
        findViewById(R.id.cstImageContainer).setVisibility(View.VISIBLE);
        findViewById(R.id.cstResultContainer).setVisibility(View.GONE);
    }

    public void stopMatching() {
        processThread.cancel(false);
    }


}

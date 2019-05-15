package tan.examlple.com.javacoban.activity;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import tan.examlple.com.javacoban.R;
import tan.examlple.com.javacoban.dialog.DialogWaiting;
import tan.examlple.com.javacoban.fragment.ImageHorizontalListFragment;
import tan.examlple.com.javacoban.imageprocess.ImageProcessThread;
import tan.examlple.com.javacoban.imageprocess.ImageProcessThread.ImageProcessingListener;
import tan.examlple.com.javacoban.permission.RuntimePermissionHelper;

public class MainActivity extends AppCompatActivity implements ImageProcessingListener {

    private static final int PICK_IMAGE_MULTIPLE = 2222;
    private static int PICK_IMAGE = 1;
    private static int REQUEST_CAMERA = 2000;
    private static int REQUEST_WRITE_EXTERNAL_STORAGE = 3000;
    private static int REQUEST_READ_EXTERNAL_STORAGE = 4000;
    private static int TAKE_IMAGE1 = 1;
    private static int TAKE_IMAGE2 = 2;


    public static String STR_BITMAP_RESULT = "BITMAP_RESULT";
    public static String STR_BUNDLE = "BUNDLE";


    private DialogWaiting dialogWaiting;
    private Button btnStitch, btnBack;
    private ImageView imvResult;
    private ImageProcessThread processThread;
    private ProgressBar progressBar;
    private ConstraintLayout cstImageContainer;
    private ConstraintLayout cstResultContainer;
    private LinearLayout lnCameraContainer;

    private ImageHorizontalListFragment horizontalListFragment;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    //TODO: openCV code goes there
                    processThread = new ImageProcessThread(MainActivity.this, dialogWaiting);
                    //TODO: wrap all images
                    processThread.setDataBeforeRun(horizontalListFragment.getBitmapArray());
                    processThread.execute();
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
        btnStitch = findViewById(R.id.btnStitch);
        btnBack = findViewById(R.id.btnBack);
        imvResult = findViewById(R.id.imvResult);
        progressBar = findViewById(R.id.progressBarWaiting);
        cstResultContainer = findViewById(R.id.cstResultContainer);
        lnCameraContainer = findViewById(R.id.lnCameraContainer);
        cstImageContainer = findViewById(R.id.cstImageContainer);
        horizontalListFragment = (ImageHorizontalListFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentImageList);
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
                    if (horizontalListFragment.getBitmapArray().size() > 2) {
                        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                    } else {
                        Toast.makeText(MainActivity.this, "You must choose 2 image at least!", Toast.LENGTH_SHORT).show();
                    }
                }
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
              /*  progressBar.setVisibility(View.VISIBLE);
                cstResultContainer.setVisibility(View.GONE);
                btnBack.setVisibility(View.GONE);*/
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
        lnCameraContainer.setVisibility(View.GONE);
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

    public void chooseImageFromGallery(int possition) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, possition);
    }

    public void chooseMutipleImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_MULTIPLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == TAKE_IMAGE1) {

            }
            if (requestCode == TAKE_IMAGE2) {

            }
            if (requestCode == PICK_IMAGE_MULTIPLE) {
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                ArrayList<String> imagesEncodedList = new ArrayList<String>();
                if (data.getData() != null) {

                    Uri mImageUri = data.getData();

                    // Get the cursor
                    Cursor cursor = getContentResolver().query(mImageUri,
                            filePathColumn, null, null, null);
                    // Move to first row
                    cursor.moveToFirst();
                    cursor.close();

                    ArrayList<Uri> mArrayUri = new ArrayList<Uri>();
                    mArrayUri.add(mImageUri);
                    for (int i = 0; i < mArrayUri.size(); i++) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mArrayUri.get(i));
                            horizontalListFragment.addBitmap(bitmap, i);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                } else {
                    if (data.getClipData() != null) {
                        ClipData mClipData = data.getClipData();
                        ArrayList<Uri> mArrayUri = new ArrayList<Uri>();
                        for (int i = 0; i < mClipData.getItemCount(); i++) {

                            ClipData.Item item = mClipData.getItemAt(i);
                            Uri uri = item.getUri();
                            mArrayUri.add(uri);
                            // Get the cursor
                            Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
                            // Move to first row
                            cursor.moveToFirst();

                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            String imageEncoded = cursor.getString(columnIndex);
                            imagesEncodedList.add(imageEncoded);
                            cursor.close();
                            for (int j = 0; j < mArrayUri.size(); j++) {
                                try {
                                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mArrayUri.get(j));
                                    horizontalListFragment.addBitmap(bitmap, j);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                        Log.v("LOG_TAG", "Selected Images" + mArrayUri.size());
                    }
                }
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
        //request all permissions needed
        requestAllNeededPermissions();
    }

    private void backToStart() {
        btnBack.setVisibility(View.GONE);
        btnStitch.setVisibility(View.VISIBLE);
        cstImageContainer.setVisibility(View.VISIBLE);
        cstResultContainer.setVisibility(View.GONE);
        lnCameraContainer.setVisibility(View.VISIBLE);
    }

    public void stopMatching() {
        processThread.cancel(false);
    }

    private void requestAllNeededPermissions() {
        RuntimePermissionHelper permissionHelper = new RuntimePermissionHelper(this);
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        int numberOfAcceptedPermission = 0;
        if (permissionHelper.permissionAlreadyGranted(Manifest.permission.READ_EXTERNAL_STORAGE) == false) {
            permissionHelper.requestPermisstion(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            numberOfAcceptedPermission++;
        }
        if (permissionHelper.permissionAlreadyGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) == false) {
            permissionHelper.requestPermisstion(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            numberOfAcceptedPermission++;
        }
        if (permissionHelper.permissionAlreadyGranted(Manifest.permission.CAMERA) == false) {
            permissionHelper.requestPermisstion(Manifest.permission.CAMERA, REQUEST_CAMERA);
        } else {
            numberOfAcceptedPermission++;
        }
        //continue request if one of the permission is not accepted, this a hack :))
        if (numberOfAcceptedPermission < 3) requestAllNeededPermissions();
    }

}

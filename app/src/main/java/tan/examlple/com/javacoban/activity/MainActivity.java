package tan.examlple.com.javacoban.activity;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import tan.examlple.com.javacoban.R;
import tan.examlple.com.javacoban.camera.CameraRequestHelper;
import tan.examlple.com.javacoban.dialog.DialogWaiting;
import tan.examlple.com.javacoban.fragment.ImageHorizontalListFragment;
import tan.examlple.com.javacoban.imageprocess.ImageProcessThread;
import tan.examlple.com.javacoban.imageprocess.ImageProcessThread.ImageProcessingListener;
import tan.examlple.com.javacoban.permission.RuntimePermissionHelper;

public class MainActivity extends AppCompatActivity implements ImageProcessingListener {

    private static final int PICK_IMAGE_MULTIPLE = 2222;
    private static int REQUEST_CAMERA = 2000;
    private static int REQUEST_WRITE_EXTERNAL_STORAGE = 3000;
    private static int REQUEST_READ_EXTERNAL_STORAGE = 4000;
    private static int REQUEST_TAKE_IMAGE1_FROM_CAMERA = 11;
    private static int REQUEST_TAKE_IMAGE2_FROM_CAMERA = 12;
    private String pathOfImage1, pathOfImage2;

    private DialogWaiting dialogWaiting;
    private Button btnStitch, btnBack;
    private ImageView imvResult, imvCamera;
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
                    processThread = new ImageProcessThread(MainActivity.this);
                    processThread.setDataBeforeRun(horizontalListFragment.getBitmapArray());
                    processThread.setmPercentageListener(dialogWaiting);
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
        imvCamera = findViewById(R.id.imvCamera);
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
                progressBar.setVisibility(View.VISIBLE);
                cstResultContainer.setVisibility(View.GONE);
                btnBack.setVisibility(View.GONE);

                //create new thread to storage image and get its filename back
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmapResult = ((BitmapDrawable) imvResult.getDrawable()).getBitmap();
                        try {
                            //Write file
                            final String filename = "bitmap.png";
                            FileOutputStream stream = MainActivity.this.openFileOutput(filename, Context.MODE_PRIVATE);
                            bitmapResult.compress(Bitmap.CompressFormat.PNG, 100, stream);

                            //Cleanup
                            stream.close();
                            bitmapResult.recycle();

                            //after having saved the bitmap, start new activity
                            Intent in1 = new Intent(MainActivity.this, ResultActivity.class);
                            in1.putExtra("image", filename);
                            startActivity(in1);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        imvCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                horizontalListFragment.clearBitmapArrays();
                pathOfImage1 = CameraRequestHelper.openCamera(MainActivity.this, REQUEST_TAKE_IMAGE1_FROM_CAMERA);
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
        startActivityForResult(Intent.createChooser(intent, "Select multiple picture"), PICK_IMAGE_MULTIPLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //it is a code request for replace image, it's also the position of recycler view
            if (requestCode != PICK_IMAGE_MULTIPLE && requestCode != REQUEST_TAKE_IMAGE1_FROM_CAMERA && requestCode != REQUEST_TAKE_IMAGE2_FROM_CAMERA) {
                final Uri imageUri = data.getData();
                final InputStream imageStream;
                try {
                    imageStream = getContentResolver().openInputStream(imageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    this.horizontalListFragment.addBitmap(selectedImage, requestCode);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            //if result come from camera
            else if (requestCode == REQUEST_TAKE_IMAGE1_FROM_CAMERA || requestCode == REQUEST_TAKE_IMAGE2_FROM_CAMERA) {
                if (requestCode == REQUEST_TAKE_IMAGE1_FROM_CAMERA) {
                    if (pathOfImage1 != null) {
                        Log.d("ssss", "onActivityResult: image1 not null");
                        final Bitmap bitmap1 = BitmapFactory.decodeFile(pathOfImage1);
                        horizontalListFragment.addBitmap(bitmap1, 0);
                        Log.d("ssss", "onActivityResult: image1 added");
                        pathOfImage2 = CameraRequestHelper.openCamera(MainActivity.this, REQUEST_TAKE_IMAGE2_FROM_CAMERA);
                    }
                } else if (requestCode == REQUEST_TAKE_IMAGE2_FROM_CAMERA) {
                    if (pathOfImage2 != null) {
                        Log.d("ssss", "onActivityResult: image2 not null");
                        final Bitmap bitmap2 = BitmapFactory.decodeFile(pathOfImage2);
                        horizontalListFragment.addBitmap(bitmap2, 1);
                        Log.d("ssss", "onActivityResult: image2 added");
                    }
                }
            }
            //if images come from gallery
            if (requestCode == PICK_IMAGE_MULTIPLE) {
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                ArrayList<String> imagesEncodedList = new ArrayList<>();
                if (data.getData() != null) {

                    Uri mImageUri = data.getData();

                    // Get the cursor
                    Cursor cursor = getContentResolver().query(mImageUri,
                            filePathColumn, null, null, null);
                    // Move to first row
                    cursor.moveToFirst();
                    cursor.close();

                    ArrayList<Uri> mArrayUri = new ArrayList<>();
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
                        ArrayList<Uri> mArrayUri = new ArrayList<>();
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
        lnCameraContainer.setVisibility(View.VISIBLE);
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
        imvCamera.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_activity,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemMenuId = item.getItemId();
        if(itemMenuId==R.id.menu_item_help){
            Intent intentNewActivity = new Intent(this,HelpActivity.class);
            startActivity(intentNewActivity);
        }
        if(itemMenuId==R.id.menu_item_stiched_images){
            Toast.makeText(this, "Go to: Internal Storage -> Android -> data -> ImageSticher -> Files", Toast.LENGTH_SHORT).show();
        }
        if(itemMenuId==R.id.menu_item_email){

        }
        return super.onOptionsItemSelected(item);
    }

    public void stopMatching() {
        processThread.cancel(false);
    }

    private void requestAllNeededPermissions() {
        RuntimePermissionHelper permissionHelper = new RuntimePermissionHelper(this);
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        if (permissionHelper.permissionAlreadyGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) == false) {
            permissionHelper.requestPermisstion(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        if (permissionHelper.permissionAlreadyGranted(Manifest.permission.CAMERA) == false) {
            permissionHelper.requestPermisstion(Manifest.permission.CAMERA, REQUEST_CAMERA);
        }
    }
}

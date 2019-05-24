package tan.examlple.com.javacoban.storage;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Environment;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BitmapStorageHelper {

    public interface BitmapSavingListener{
        void onSaveStatusChange(String message);
    }

    private static BitmapStorageHelper instance;

    private BitmapStorageHelper() {
    }

    public static BitmapStorageHelper getInstance() {
        if (instance == null) instance = new BitmapStorageHelper();
        return instance;
    }

    private File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + "ImageStitcher"
                + "/Files");
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

    public boolean storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            return false;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public void storagePhotoViewByNewThread(BitmapSavingListener bitmapSavingListener,PhotoView photoView) {
        StorageThread storageThread = new StorageThread(bitmapSavingListener);
        storageThread.execute(photoView);
    }
    class StorageThread extends AsyncTask<PhotoView, Void, Boolean>{
        private BitmapSavingListener bitmapSavingListener;

        public StorageThread(BitmapSavingListener listener){
            this.bitmapSavingListener = listener;
        }
        @Override
        protected Boolean doInBackground(PhotoView... photoViews) {
            return BitmapStorageHelper.getInstance().storeImage(((BitmapDrawable) photoViews[0].getDrawable()).getBitmap());
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            String storageMessage;
            if(aBoolean==true){
                storageMessage = "Saving success!";
            }
            else{
                storageMessage = "Saving fails!";
            }
            bitmapSavingListener.onSaveStatusChange(storageMessage);
        }
    }
}

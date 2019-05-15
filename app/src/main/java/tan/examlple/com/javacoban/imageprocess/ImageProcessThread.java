package tan.examlple.com.javacoban.imageprocess;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.util.ArrayList;

import static tan.examlple.com.javacoban.imageprocess.ImageStitcher.*;

public class ImageProcessThread extends AsyncTask<Void,ImageView,Bitmap> {

    private ImageProcessingListener imageProcessingListener;
    private ProcessingListener percentageListener;
    private ArrayList<Bitmap> bitmapArrayList;
    public void setDataBeforeRun(ArrayList<Bitmap> bitmapArray) {
        bitmapArrayList = bitmapArray;
    }

    public interface ImageProcessingListener
    {
         void setUIAfterRun(Bitmap result);
         void setUIBeforeRun();
    }
    public ImageProcessThread(ImageProcessingListener imageProcessingListener, ProcessingListener percentageListener){
        this.imageProcessingListener = imageProcessingListener;
        this.percentageListener = percentageListener;
    }
    @Override
    protected void onPreExecute() {
        imageProcessingListener.setUIBeforeRun();
    }
    @Override
    protected Bitmap doInBackground(Void... voids) {
        ImageStitcher imageStitcher = getInstance();
        imageStitcher.setPersentageListener(percentageListener);
        //stitch two first bitmap
        Bitmap result = imageStitcher.stitch(bitmapArrayList.get(0),bitmapArrayList.get(1));
        //stitch all except last image cause it is just an image insertion icon
        for(int i=2;i<bitmapArrayList.size()-1;i++){
            result = imageStitcher.stitch(result,bitmapArrayList.get(i));
        }
        
        return result;
    }
    @Override
    protected void onPostExecute(Bitmap result) {
        imageProcessingListener.setUIAfterRun(result);
    }
}

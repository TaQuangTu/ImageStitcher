package tan.examlple.com.javacoban.imageprocess;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

public class ImageProcessThread extends AsyncTask<ImageView,ImageView,Bitmap> {

    private ImageProcessingListener imageProcessingListener;
    public interface ImageProcessingListener
    {
         void setUIAfterRun(Bitmap result);
         void setUIBeforeRun();
    }
    public ImageProcessThread(ImageProcessingListener imageProcessingListener){
        this.imageProcessingListener = imageProcessingListener;
    }
    @Override
    protected void onPreExecute() {
        imageProcessingListener.setUIBeforeRun();
    }
    @Override
    protected Bitmap doInBackground(ImageView... imageViews) {
        //get two image in parameters
        ImageView imv1 = imageViews[0];
        ImageView imv2 = imageViews[1];
        Bitmap result = ImageStitcher.getInstance().stitch(imv1,imv2);
        return result;
    }
    @Override
    protected void onPostExecute(Bitmap result) {
        imageProcessingListener.setUIAfterRun(result);
    }
}

package tan.examlple.com.javacoban.imageprocess;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ImageStitcher {
    public interface ProcessingListener{
        int onProcess(int percentage);
    }
    private ProcessingListener listener;
    public void setPersentageListener(ProcessingListener processingListener){
        this.listener = processingListener;

    }
    private static String TAG = "ImageSticher";
    private static ImageStitcher imageStitcher;
    private FeatureDetector detector;

    public static ImageStitcher getInstance() {
        if (imageStitcher == null) imageStitcher = new ImageStitcher();
        return imageStitcher;
    }

    private ImageStitcher() {
        detector = FeatureDetector.create(FeatureDetector.ORB);
        ProcessingListener defaultListener = new ProcessingListener() {
            @Override
            public int onProcess(int percentage) {
                return 0;
            }
        };
        this.listener=defaultListener;
    }
    private Bitmap getBitmap(ImageView imv) {
        Bitmap bm = (((BitmapDrawable) imv.getDrawable()).getBitmap()).copy(Bitmap.Config.ARGB_8888, false);
        //TODO: uncomment below line to avoid calculation too long
       //bm = getReduceResolutionBitmap(bm);
        return bm;
    }
    private Bitmap getReduceResolutionBitmap(Bitmap bitmap){
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap,(int)(bitmap.getWidth()*0.8),(int)(bitmap.getHeight()*0.8),true);
        return resizedBitmap;
    }
    private Mat getMat(Bitmap image) {
        Mat mat = new Mat();
        Utils.bitmapToMat(image, mat);
        return mat;
    }


    public Bitmap stitch(Bitmap imv1, Bitmap imv2) {

        //Step 1: create ORB feature detector, ORB descriptor extractor, descriptor matcher
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        listener.onProcess(5+new Random().nextInt(10));

        //step 2: read two images to Mat format for next calculation, declare some necessary variables
        Mat mat1 = getMat(imv1);
        Mat mat2 = getMat(imv2);
        MatOfKeyPoint matOfKeyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint matOfKeyPoint2 = new MatOfKeyPoint();

        //step 3: detect keypoints for two the images and store the found keypoints to matOfKeypoints
        detector.detect(mat1, matOfKeyPoint1);
        detector.detect(mat2, matOfKeyPoint2);
        listener.onProcess(15+new Random().nextInt(5));

        //step 4: extract keypoint descriptors and store them to descriptor1,2
        Mat descriptor1 = new Mat();
        Mat descriptor2 = new Mat();
        descriptorExtractor.compute(mat1, matOfKeyPoint1, descriptor1);
        descriptorExtractor.compute(mat2, matOfKeyPoint2, descriptor2);
        listener.onProcess(20+new Random().nextInt(10));
        //step 5: match descriptors in two the sets and store matches into a list
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descriptor1, descriptor2, knnMatches, 2); //the fourth parameter specifies matcher to find 2 best matches for each keypoint
        listener.onProcess(30+new Random().nextInt(10));

        //step 6: Filter matches using the Lowe's ratio test
        float ratioThresh = 0.75f;
        List<DMatch> listOfGoodMatches = new ArrayList<>();
        for (int i = 0; i < knnMatches.size(); i++) {
            if (knnMatches.get(i).rows() > 1) {
                DMatch[] matches = knnMatches.get(i).toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }
        listener.onProcess(40+new Random().nextInt(10));
        //Localize the object
        List<Point> points1 = new ArrayList<>();
        List<Point> points2 = new ArrayList<>();
        List<KeyPoint> listOfKeypoints1 = matOfKeyPoint1.toList(); //get keypoint reached from step 3
        List<KeyPoint> listOfKeypoints2 = matOfKeyPoint2.toList();
        listener.onProcess(50+new Random().nextInt(5));

        for (int i = 0; i < listOfGoodMatches.size(); i++) {
            //-- Get the keypoints from the good matches
            points1.add(listOfKeypoints1.get(listOfGoodMatches.get(i).queryIdx).pt);
            points2.add(listOfKeypoints2.get(listOfGoodMatches.get(i).trainIdx).pt);
        }
        listener.onProcess(55+new Random().nextInt(2));

        MatOfPoint2f objMat = new MatOfPoint2f(), sceneMat = new MatOfPoint2f();
        objMat.fromList(points1);
        sceneMat.fromList(points2);
        double ransacReprojThreshold = 3;//or 2=3.0
        listener.onProcess(57+new Random().nextInt(3));

        //TODO: swap sceneMat and objMat
        Mat homography = Calib3d.findHomography(objMat,sceneMat, Calib3d.RANSAC,ransacReprojThreshold);
        listener.onProcess(60+new Random().nextInt(3));

        //log to debug
       /* for(int i=0;i<points1.size();i++){
            Point p1 =  points1.get(i);
            Point p2 = points2.get(i);
            Log.d(TAG, ""+(int)p1.x+","+(int)p1.y+"<-->"+(int)p2.x+","+(int)p2.y);
        }
        Log.d(TAG, "homo:=============================");
        Log.d(TAG, "homosize: "+homography.height()+" "+homography.width());
        for(int i=0;i<3;i++){
            Log.d(TAG, ""+homography.get(i,0)[0]+" "+homography.get(i,1)[0]+" "+homography.get(i,2)[0]);
            Log.d(TAG, "size: "+homography.get(i,0).length);
        }
        */
        //stitch images

        Bitmap res =imv2; //default value for result
        if (homography == null || homography.height() < 3 || homography.width() < 3) {
            Log.d(TAG, "stitch: can not stitch the images");
        } else { //stitch
            res = stitchThroughHomography(imv1, imv2, homography);
            return res;
        }
        //TODO: return new bitmap that matched from two the image
        return res;
    }

    private Point getReflectPoint(int x, int y, Double[][] homography) {
        double newX = 0, newY = 0;
        newX = (homography[0][0] * x + homography[0][1] * y + homography[0][2]);
        newY = (homography[1][0] * x + homography[1][1] * y + homography[1][2]);
        return new Point(newX, newY);
    }

    private Bitmap stitchThroughHomography(Bitmap bm1, Bitmap bm2, Mat homography) {
        int minX = 99999999, minY =9999999;
        int maxX = -1, maxY = -1;
        int percentage = 70;
        final int DETAL_Y = 1000, MAX_HEIGHT = 8000;
        final int DETAL_X = 1000, MAX_WIDTH = 8000;
        //create a bitmap that big enough
        Bitmap res = Bitmap.createBitmap(MAX_WIDTH, MAX_HEIGHT, Bitmap.Config.ARGB_8888);
        //first, copy bitmap1
        for (int i = 0; i < bm2.getWidth(); i++) {
            for (int j = 0; j < bm2.getHeight(); j++) {
                int newX = i+DETAL_X, newY = j+DETAL_Y;
                if (newX >= 0 && newX < MAX_WIDTH && newY >= 0 && newY < MAX_HEIGHT){
                    if(newX<minX) minX = newX;
                    if(newX>maxX) maxX = newX;
                    if(newY<minY) minY = newY;
                    if(newY>maxY) maxY = newY;
                    res.setPixel(newX, newY, bm2.getPixel(i, j));
                }

            }
            listener.onProcess((percentage+(int)((double)i/bm2.getWidth()*20)));
        }
        //next, reflect all pixels from right to left
        //create array of homography for quicker calculation and copy values from homography mat
        Double homoAsArray[][] = new Double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                homoAsArray[i][j] = homography.get(i, j)[0];
            }
        }
        percentage=90;
        for (int i = 0; i < bm1.getWidth(); i++) {
            for (int j = 0; j < bm1.getHeight(); j++) {
                Point relectPoint = getReflectPoint(i, j, homoAsArray);
                int newX = (int) (relectPoint.x+DETAL_X);
                int newY = (int) (relectPoint.y+DETAL_Y);
                if (newX >= 0 && newX < MAX_WIDTH && newY >= 0 && newY < MAX_HEIGHT){
                    if(newX>=DETAL_X&&newX<=bm2.getWidth()+DETAL_X&&newY>=DETAL_Y&&newY<=bm2.getHeight()+DETAL_Y) continue;
                    else {
                        res.setPixel(newX, newY, bm1.getPixel(i, j));
                        if(newX<minX) minX = newX;
                        if(newX>maxX) maxX = newX;
                        if(newY<minY) minY = newY;
                        if(newY>maxY) maxY = newY;
                    }
                }
            }
            listener.onProcess((percentage+(int)((double)i/bm1.getWidth()*10)));
        }
        res = Bitmap.createBitmap(res,minX,minY,maxX-minX,maxY-minY);
        if(res.getWidth()>4000||res.getHeight()>4000) res = Bitmap.createScaledBitmap(res,(int)(res.getWidth()*0.8), (int)(res.getHeight()*0.8),true);
        Log.d(TAG, "stitchThroughHomography: minmax"+minX+" "+minY+ " "+maxX+" "+maxY);

        //some pixels is not setted color cause transformation by Homography, we need to set color to it
        //fillBlackPixels(res);
        return res;
    }
    //some black pixels need to be filled with avarage value of neighboor pixel values
    private void fillBlackPixels(Bitmap bm){
        int width = bm.getWidth();
        int height = bm.getHeight();
        for(int i=1;i<width-1;i++){
            for(int j=1;j<height-1;j++){
                if(bm.getPixel(i,j)==0)//unsetted color
                {
                    bm.setPixel(i,j,avarageAround(bm,i,j));
                }
            }
        }
    }
    /**this function assume that x,y is not a point that laid on edge or corner.
     * If the point(x,y) is not like that, bitmap out of bound will be threw*/
    private int avarageAround(Bitmap bm, int x, int y){
        int sum = 0;
        sum+=bm.getPixel(x-1,y-1);
        sum+=bm.getPixel(x,y-1);
        sum+=bm.getPixel(x+1,y-1);
        sum+=bm.getPixel(x+1,y);
        sum+=bm.getPixel(x+1,y+1);
        sum+=bm.getPixel(x,y+1);
        sum+=bm.getPixel(x-1,y+1);
        sum+=bm.getPixel(x-1,y);
        return sum/8;
    }
}

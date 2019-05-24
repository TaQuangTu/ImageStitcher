package tan.examlple.com.javacoban.imageprocess;

import android.graphics.Bitmap;
import android.util.Log;

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

import tan.examlple.com.javacoban.math.RandomHelper;

public class ImageStitcher {

    //this listener is used to show percentage of stitching processing to class that implements it
    public interface PercentageListener {
        int onPercentageChange(int percentage);
    }

    private PercentageListener percentageListener;
    private static String TAG = "ImageSticher";
    private static ImageStitcher imageStitcher;
    private FeatureDetector detector;
    private DescriptorMatcher matcher;
    private DescriptorExtractor descriptorExtractor;

    //private constructor to avoid creating new instance outside this class
    private ImageStitcher() {
        //Step 1: create ORB feature detector, ORB descriptor extractor, descriptor matcher
        detector = FeatureDetector.create(FeatureDetector.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        //we need a default percentage listener and this listener do nothing
        percentageListener = new PercentageListener() {
            @Override
            public int onPercentageChange(int percentage) {
                //do nothing
                return 0;
            }
        };
    }

    public static ImageStitcher getInstance() {
        if (imageStitcher == null) imageStitcher = new ImageStitcher();
        return imageStitcher;
    }

    public void setPercentageListener(PercentageListener listener) {
        if (listener != null) {
            this.percentageListener = listener;
        }
        //else, continue using default listener
    }

    /*  private Bitmap getBitmap(ImageView imv) {
          Bitmap bm = (((BitmapDrawable) imv.getDrawable()).getBitmap()).copy(Bitmap.Config.ARGB_8888, false);
          //TODO: uncomment below line to avoid calculation too long
         //bm = getReduceResolutionBitmap(bm);
          return bm;
      }
      private Bitmap getReduceResolutionBitmap(Bitmap bitmap){
          Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap,(int)(bitmap.getWidth()*0.8),(int)(bitmap.getHeight()*0.8),true);
          return resizedBitmap;
      }*/
    private Mat getMat(Bitmap image) {
        Mat mat = new Mat();
        Utils.bitmapToMat(image, mat);
        return mat;
    }

    private Bitmap stitch(Bitmap imv1, Bitmap imv2, int beginPercent, int endPercent) { //two last args are used for listener

        int range = endPercent - beginPercent;
        //step 1: see constructor
        //step 2: read two images to Mat format for next calculation, declare some necessary variables

        Mat mat1 = getMat(imv1);
        Mat mat2 = getMat(imv2);
        MatOfKeyPoint matOfKeyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint matOfKeyPoint2 = new MatOfKeyPoint();

        //step 3: detect keypoints for two the images and store the found keypoints to matOfKeypoints
        detector.detect(mat1, matOfKeyPoint1);

        detector.detect(mat2, matOfKeyPoint2);


        //step 4: extract keypoint descriptors and store them to descriptor1,2
        Mat descriptor1 = new Mat();
        Mat descriptor2 = new Mat();
        descriptorExtractor.compute(mat1, matOfKeyPoint1, descriptor1);
        descriptorExtractor.compute(mat2, matOfKeyPoint2, descriptor2);

        //step 5: match descriptors in two the sets and store matches into a list
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descriptor1, descriptor2, knnMatches, 2); //the fourth parameter specifies matcher to find 2 best matches for each keypoint

        //step 6: Filter matches using the Lowe's ratio test
        float ratioThresh = 0.75f;
        List<DMatch> listOfGoodMatches = new ArrayList<>();
        int sizeOfMatchingPair = knnMatches.size();
        for (int i = 0; i < sizeOfMatchingPair; i++) {
            if (knnMatches.get(i).rows() > 1) {
                DMatch[] matches = knnMatches.get(i).toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }

        //Localize the object
        List<Point> points1 = new ArrayList<>();
        List<Point> points2 = new ArrayList<>();
        List<KeyPoint> listOfKeypoints1 = matOfKeyPoint1.toList(); //get keypoint reached from step 3
        List<KeyPoint> listOfKeypoints2 = matOfKeyPoint2.toList();

        for (int i = 0; i < listOfGoodMatches.size(); i++) {
            //-- Get the keypoints from the good matches
            points1.add(listOfKeypoints1.get(listOfGoodMatches.get(i).queryIdx).pt);
            points2.add(listOfKeypoints2.get(listOfGoodMatches.get(i).trainIdx).pt);
        }

        MatOfPoint2f objMat = new MatOfPoint2f(), sceneMat = new MatOfPoint2f();
        objMat.fromList(points1);
        sceneMat.fromList(points2);
        double ransacReprojThreshold = 3;//or 2=3.0

        Bitmap res = imv1; //default value for result
        if (objMat == null || sceneMat == null || objMat.toArray().length < 3 || sceneMat.toArray().length < 3) {
            Log.d(TAG, "stitch: 2 images have no same point");
            return res; //no match pair found
        }
        //TODO: swap sceneMat and objMat if result is not as expected
        Mat homography = Calib3d.findHomography(objMat, sceneMat, Calib3d.RANSAC, ransacReprojThreshold);
        if (homography == null || homography.height() < 3 || homography.width() < 3) {
            Log.d(TAG, "stitch: can not stitch the images");
        } else { //stitch
            res = stitchThroughHomography(imv1, imv2, homography, beginPercent, endPercent);
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

    private Bitmap stitchThroughHomography(Bitmap bm1, Bitmap bm2, Mat homography, int beginPercent, int endPercent) {
        int range = endPercent - beginPercent;
        int minX = 99999999, minY = 9999999;
        int maxX = -1, maxY = -1;
        final int DETAL_Y = 2000, MAX_HEIGHT = 9000;
        final int DETAL_X = 2000, MAX_WIDTH = 5000;
        //create a bitmap that big enough
        Bitmap res = Bitmap.createBitmap(MAX_WIDTH, MAX_HEIGHT, Bitmap.Config.ARGB_8888);
        //first, copy bitmap1
        int width1 = bm1.getWidth(), width2 = bm2.getWidth();
        int height1 = bm1.getHeight(), height2 = bm2.getHeight();
        for (int i = 0; i < width2; i++) {
            for (int j = 0; j < height2; j++) {
                int newX = i + DETAL_X, newY = j + DETAL_Y;
                if (newX >= 0 && newX < MAX_WIDTH && newY >= 0 && newY < MAX_HEIGHT) {
                    if (newX < minX) minX = newX;
                    if (newX > maxX) maxX = newX;
                    if (newY < minY) minY = newY;
                    if (newY > maxY) maxY = newY;
                    res.setPixel(newX, newY, bm2.getPixel(i, j));
                }
            }
            percentageListener.onPercentageChange(beginPercent + (int) (((double) i / (width2 + width1)) * range));
        }
        //next, reflect all pixels from right to left
        //create array of homography for quicker calculation and copy values from homography mat
        Double homoAsArray[][] = new Double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                homoAsArray[i][j] = homography.get(i, j)[0];
            }
        }
        for (int i = 0; i < width1; i++) {
            for (int j = 0; j < height1; j++) {
                Point relectPoint = getReflectPoint(i, j, homoAsArray);
                int newX = (int) (relectPoint.x + DETAL_X);
                int newY = (int) (relectPoint.y + DETAL_Y);
                if (newX >= 0 && newX < MAX_WIDTH && newY >= 0 && newY < MAX_HEIGHT) {
                    res.setPixel(newX, newY, bm1.getPixel(i, j));
                    if (newX < minX) minX = newX;
                    if (newX > maxX) maxX = newX;
                    if (newY < minY) minY = newY;
                    if (newY > maxY) maxY = newY;
                }
            }
            percentageListener.onPercentageChange(beginPercent + (int) ((((double) i + width2) / (width2 + width1)) * range));
        }
        res = Bitmap.createBitmap(res, minX, minY, maxX - minX, maxY - minY);
        //scale result bitmap if need
       /* if (res.getWidth() > 4000 || res.getHeight() > 4000)
            res = Bitmap.createScaledBitmap(res, (int) (res.getWidth() * 0.8), (int) (res.getHeight() * 0.8), true);*/
        percentageListener.onPercentageChange(beginPercent + range);
        Log.d(TAG, "stitchThroughHomography: minmax" + minX + " " + minY + " " + maxX + " " + maxY);

        return res;
    }

    public Bitmap stichMultipleBitmap(ArrayList<Bitmap> bitmaps) {

        int numBitmaps = bitmaps.size(); //-->we have "numBitmaps - 1" images (exclude the last item cause it is an icon)
        int turn = 0; //turn of stitching
        int beginPercent = 0;
        int percentOfEachTurn = 100 / (bitmaps.size() - 2);
        boolean takenBitmaps[] = new boolean[numBitmaps - 1];
        for (int i = 0; i < takenBitmaps.length; i++) takenBitmaps[i] = false;

        Bitmap res = bitmaps.get(0);
        takenBitmaps[0] = true;

        int consecutiveUnStitchableTurn = 0;
        while (hasUnStitchedImage(takenBitmaps) == true && consecutiveUnStitchableTurn <= 4) {
            int nextImageIndex = getUnStitchImage(takenBitmaps);
            Log.d(TAG, "stichMultipleBitmap: nextindex" + nextImageIndex);
            Bitmap temp = imageStitcher.stitch(bitmaps.get(nextImageIndex), res, beginPercent, (turn + 1) * percentOfEachTurn);
            if (temp != bitmaps.get(nextImageIndex)) {  //stitch success
                res = temp;
                takenBitmaps[nextImageIndex] = true;
                beginPercent = (++turn) * percentOfEachTurn;
                consecutiveUnStitchableTurn = 0;
            } else {
                consecutiveUnStitchableTurn++;
            }
        }
        percentageListener.onPercentageChange(0); //back to 0 for the next turn
        return res;
    }

    private boolean hasUnStitchedImage(boolean[] images) {
        for (int i = 0; i < images.length; i++) {
            if (images[i] == false) return true;
        }
        return false;
    }

    private int getUnStitchImage(boolean[] images) {
        RandomHelper randomHelper = RandomHelper.getInstance();
        while (true) {
            int randomIndex = randomHelper.nextIntInRange(0, images.length);
            if (images[randomIndex] == false) return randomIndex;
        }
    }
}

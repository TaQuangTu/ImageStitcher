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

public class ImageStitcher {
    private static String TAG = "ImageSticher";
    private static ImageStitcher imageStitcher;
    private FeatureDetector detector;

    public static ImageStitcher getInstance() {
        if (imageStitcher == null) imageStitcher = new ImageStitcher();
        return imageStitcher;
    }

    private ImageStitcher() {
        detector = FeatureDetector.create(FeatureDetector.ORB);
    }

    private Bitmap getReduceResolutionBitmap(Bitmap bm) {
        //scale bitmap (otherwise the program crashes due to memory lack)
        int MAX_DIM = 400;
        int w = MAX_DIM, h = MAX_DIM;  //default values
        if (bm.getWidth() >= bm.getHeight() && bm.getWidth() > MAX_DIM) {
            w = MAX_DIM;
            h = bm.getHeight() * MAX_DIM / bm.getWidth();
        } else if (bm.getWidth() <= bm.getHeight() && bm.getHeight() > MAX_DIM) {
            h = MAX_DIM;
            w = bm.getWidth() * MAX_DIM / bm.getHeight();
        }
        bm = Bitmap.createScaledBitmap(bm, w, h, false);
        return bm;
    }

    private Bitmap getBitmap(ImageView imv) {
        Bitmap bm = (((BitmapDrawable) imv.getDrawable()).getBitmap()).copy(Bitmap.Config.ARGB_8888, false);
        //TODO: uncomment below line to avoid calculation too long
        // bm = getReduceResolutionBitmap(bm);
        return bm;
    }

    /**
     * get bitmap without resolution reduction
     */
    private Bitmap getOriginBitmap(ImageView imv) {
        Bitmap bm = (((BitmapDrawable) imv.getDrawable()).getBitmap()).copy(Bitmap.Config.ARGB_8888, false);
        return bm;
    }

    private Mat getMat(ImageView image) {
        Mat mat = new Mat();
        Utils.bitmapToMat(getBitmap(image), mat);
        return mat;
    }

    /*private Pair<List<Point>, List<Point>> getPairOfMatchingPoints(List<KeyPoint> keyPoints1, List<KeyPoint> keyPoints2, Mat descriptor1, Mat descriptor2) {
        //TODO: increase MAX_KEYPOINT to get more accuracy result (ideal <= 500)
        final int MAX_KEYPOINT = 150; //for more quicker calculation
        ArrayList<Point> points1 = new ArrayList<>();
        ArrayList<Point> points2 = new ArrayList<>();

        int numOfKeypoint1 = descriptor1.rows();
        int numOfKeypoint2 = descriptor2.rows();
        for (int i = 0; i < numOfKeypoint1 && i < MAX_KEYPOINT; i++) {
            int matchingIndex = 0;
            double minDistance = Double.MAX_VALUE;
            for (int j = 0; j < numOfKeypoint2; j++) {
                double distance = getDistance(descriptor1.row(i), descriptor2.row(j));
                if (minDistance > distance) {
                    minDistance = distance;
                    matchingIndex = j;
                }
            }
            Point first = new Point(keyPoints1.get(i).pt.x, keyPoints1.get(i).pt.y);
            Point second = new Point(keyPoints2.get(matchingIndex).pt.x, keyPoints2.get(matchingIndex).pt.y);
            points1.add(first);
            points2.add(second);
        }
        return new Pair<List<Point>, List<Point>>(points1, points2);
    }*/

    /*private double getDistance(Mat descriptor1, Mat descriptor2) {
        //this method knows that both the descriptors's height = 1 and width = 32
        double distance = 0;
        for (int i = 0; i < 32; i++) {
            distance += (Math.abs(descriptor1.get(0, i)[0] - descriptor2.get(0, i)[0]));
        }
        return distance;
    }*/

    public Bitmap stitch(ImageView imv1, ImageView imv2) {

        //Step 1: create ORB feature detector, ORB descriptor extractor, descriptor matcher
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);

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
        for (int i = 0; i < knnMatches.size(); i++) {
            if (knnMatches.get(i).rows() > 1) {
                DMatch[] matches = knnMatches.get(i).toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(listOfGoodMatches);

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
        //TODO: swap sceneMat and objMat
        Mat homography = Calib3d.findHomography(sceneMat,objMat, Calib3d.RANSAC, ransacReprojThreshold);

        //stitch images

        Bitmap res = getBitmap(imv1); //default value for result
        if (homography == null || homography.height() < 3 || homography.width() < 3) {
            Log.d(TAG, "stitch: can not stitch the images");
        } else { //stitch
            Log.d(TAG, "homosize: "+homography.height()+" "+homography.width());
            for(int i=0;i<3;i++){
                Log.d(TAG, "homog: "+homography.get(i,0)[0]+" "+homography.get(i,1)[0]+" "+homography.get(i,2)[0]);
            }
            res = stitchThroughHomography(getBitmap(imv1), getBitmap(imv2), homography);
            return res;
        }
        //TODO: return new bitmap that matched from two the image
        return res;
    }

    private Point getReflectPoint(int x, int y, Double[][] homography) {
        int newX = 0, newY = 0;
        newX = (int) (homography[0][0] * x + homography[0][1] * y + homography[0][2]);
        newY = (int) (homography[1][0] * x + homography[1][1] * y + homography[1][2]);
        return new Point(newX, newY);
    }

    private Bitmap stitchThroughHomography(Bitmap bm1, Bitmap bm2, Mat homography) {
        final int DETAL_Y = 250, MAX_HEIGHT = 4000;
        final int DETAL_X = 250, MAX_WIDTH = 4000;
        //create a bitmap that big enough
        Bitmap res = Bitmap.createBitmap(MAX_WIDTH, MAX_HEIGHT, Bitmap.Config.ARGB_8888);
        //first, copy bitmap1
        for (int i = 0; i < bm1.getWidth(); i++) {
            for (int j = 0; j < bm1.getHeight(); j++) {
                int newX = i+DETAL_X, newY = j+DETAL_Y;
                if (newX >= 0 && newX < MAX_WIDTH && newY >= 0 && newY < MAX_HEIGHT)
                    res.setPixel(newX, newY, bm1.getPixel(i, j));
            }
        }
        //next, reflect all pixels from right to left
        //create array of homography for quicker calculation and copy values from homography mat
        Double homoAsArray[][] = new Double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                homoAsArray[i][j] = homography.get(i, j)[0];
            }
        }
        for (int i = 0; i < bm2.getWidth(); i++) {
            for (int j = 0; j < bm2.getHeight(); j++) {
                Point relectPoint = getReflectPoint(i, j, homoAsArray);
                int newX = (int) relectPoint.x+DETAL_X;
                int newY = (int) relectPoint.y+DETAL_Y;
                if (newX >= 0 && newX < MAX_WIDTH && newY >= 0 && newY < MAX_HEIGHT)
                    res.setPixel(newX, newY, bm2.getPixel(i, j));
            }
        }
        return res;
    }
}

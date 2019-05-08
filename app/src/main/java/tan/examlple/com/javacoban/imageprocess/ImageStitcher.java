package tan.examlple.com.javacoban.imageprocess;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
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
        int MAX_DIM = 300;
        int w = 300, h = 300;  //default values
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
        bm = getReduceResolutionBitmap(bm);
        return bm;
    }

    private Bitmap getOriginBitmap(ImageView imv) {
        Bitmap bm = (((BitmapDrawable) imv.getDrawable()).getBitmap()).copy(Bitmap.Config.ARGB_8888, false);
        return bm;
    }

    private Mat getMat(ImageView image) {
        Mat mat  = new Mat();
        Utils.bitmapToMat(getBitmap(image), mat);
        return mat;
    }

    private Pair<List<Point>, List<Point>> getPairOfMatchingPoints(List<KeyPoint> keyPoints1, List<KeyPoint> keyPoints2, Mat descriptor1, Mat descriptor2) {
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
    }

    private double getDistance(Mat descriptor1, Mat descriptor2) {
        //this method know that both the descriptors's height = 1 and width = 32
        double distance = 0;
        for (int i = 0; i < 32; i++) {
            distance += (Math.abs(descriptor1.get(0, i)[0] - descriptor2.get(0, i)[0]));
        }
        return distance;
    }

    public Bitmap stitch(ImageView imv1, ImageView imv2) {
        //create feature detector, descriptor extractor
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);

        Mat mat1 = getMat(imv1), descriptor1 = new Mat();
        Mat mat2 = getMat(imv2), descriptor2 = new Mat();
        MatOfKeyPoint matOfKeyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint matOfKeyPoint2 = new MatOfKeyPoint();

        //detect keypoints for two the images
        detector.detect(mat1, matOfKeyPoint1);
        detector.detect(mat2, matOfKeyPoint2);

        //get list of keypoints for easier calculation
        List<KeyPoint> keyPoints1 = matOfKeyPoint1.toList();
        List<KeyPoint> keyPoints2 = matOfKeyPoint2.toList();


        //extract keypoint descriptors
        descriptorExtractor.compute(mat1, matOfKeyPoint1, descriptor1);
        descriptorExtractor.compute(mat2, matOfKeyPoint2, descriptor2);

        //match these keypoints (find the best match for each keypoint)
        Pair<List<Point>, List<Point>> matchingPair = getPairOfMatchingPoints(keyPoints1, keyPoints2, descriptor1, descriptor2);

        //get two separate list of matching points (the i'th point at listOfPoints1 is matching with the i'th point listOfPoints2)
        List<Point> points1 = matchingPair.first;
        List<Point> points2 = matchingPair.second;

        Log.d(TAG, "found keypoint number" + points1.size());
        Log.d(TAG, "found keypoint number" + points2.size());

        //find homography on the matching keypoints
        MatOfPoint2f matOfPoint1 = new MatOfPoint2f();
        matOfPoint1.fromList(points1);
        MatOfPoint2f matOfPoint2 = new MatOfPoint2f();
        matOfPoint2.fromList(points2);
        //TODO: swap matOfPoint1 and matOfPoints2 when result is not expected
        Mat homography = Calib3d.findHomography(matOfPoint1, matOfPoint2, Calib3d.RANSAC, 10);
        Log.d(TAG, "homography: " + homography.height() + " " + homography.height());

        //now we have homography, now we match two the ORIGINAL bitmaps
        //bitmap1 = H*bitmap2
        Bitmap bitmap1 = getBitmap(imv1);
        Bitmap bitmap2 = getBitmap(imv2);
        int resultWidth = bitmap1.getWidth() + bitmap2.getWidth();
        int resultHeight = bitmap1.getHeight() + bitmap2.getHeight();
        Bitmap resultBitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
        writeResultIntoNewBitmap(bitmap1, bitmap2, resultBitmap, homography);

        return resultBitmap;
    }

    private Point getReflectPoint(int x, int y, Double[][] homography) {
        int newX = 0, newY = 0;
        newX = (int) (homography[0][0] * x + homography[0][1] * y + homography[0][2]);
        newY = (int) (homography[1][0] * x + homography[1][1] * y + homography[1][2]);
        return new Point(newX, newY);
    }

    private void writeResultIntoNewBitmap(Bitmap bitmap1, Bitmap bitmap2, Bitmap result, Mat homography) {
        //TODO: recheck homography if need (3*3 expected)
        //create array of homography for quicker calculation and copy values from homography mat
        Double homoAsArray[][] = new Double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                homoAsArray[i][j] = homography.get(i, j)[0];
            }
        }

        int width1 = bitmap1.getWidth();
        int width2 = bitmap2.getWidth();
        int height1 = bitmap1.getHeight();
        int height2 = bitmap2.getHeight();
        //first, copy bitmap1 and paste into result
        for (int i = 0; i < width1; i++) {
            for (int j = 0; j < height1; j++) {
                result.setPixel(i, j, bitmap1.getPixel(i, j));
            }
        }
        //next, transform second bitmap pixels to result by Homography
        for (int i = 0; i < width2; i++) {
            for (int j = 0; j < height2; j++) {
                Point reflectPoint = getReflectPoint(i, j, homoAsArray);
                int reflectX = (int) reflectPoint.x;
                int reflectY = (int) reflectPoint.y;
                //make sure new x,y is inside the result bitmap
                if (reflectX < width1 + width2 && reflectY < height1 + height1 && reflectX > 0 && reflectY > 0)
                    result.setPixel(reflectX, reflectY, bitmap2.getPixel(i, j));
            }
        }
    }
}

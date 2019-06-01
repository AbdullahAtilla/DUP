package com.dup.tdup;

import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageProcessor
{
    private final String TAG = "C-ImageProcessor";
    //Constructor
    public ImageProcessor(){}

    public Bitmap extractOutfit(Bitmap bmp, int sensitivity_level, int[] colorCode)
    {
        //Convert bitmap to mat to process
        Mat mat = new Mat();
        Utils.bitmapToMat(bmp, mat);

        //Gaussian blur
        Imgproc.GaussianBlur(mat, mat, new Size(5,5), 0);
        //convert from bgr to hsv
        Mat hsv = new Mat();
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV);

        //define range of color in HSV
        int sensitivity = sensitivity_level;
        if(colorCode.length != 3)
            {Log.d(TAG, "colorCode is must contain 3 elements! ! !");return bmp;}
        int code_1 = colorCode[0];
        int code_2 = colorCode[1];
        int code_3 = colorCode[2];
        Scalar lower_range = new Scalar(0,0,255-sensitivity);
        Scalar upper_range = new Scalar(255,sensitivity,255);
        //threshold the HSV image to get only taget colors
        Mat mask = new Mat();
        Core.inRange(hsv, lower_range, upper_range, mask);
        //bitwise-and mask & original image
        Mat res = new Mat();
        Core.bitwise_and(mat, mat, res, mask);
        //create an inverted mask to segment out the outfit
        Mat mask_2 = new Mat();
        Core.bitwise_not(mask, mask_2);
        //Segmenting the outfit out the frame
        Mat result = new Mat();
        Core.bitwise_and(mat, mat, result,mask_2);

        //Crop only the outfit area
        result = cropAOI(result);

        //convert mat to bitmap
        Bitmap result_bmp = Bitmap.createBitmap(result.cols(), result.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, result_bmp);
        return result_bmp;
    }//end extractOutfit


    //This function crop the largest contour in given Mat
    //and returns cropped Mat
    private Mat cropAOI(Mat mat)
    {
        double largest_area = 0;
        int largest_contour_index = 0;
        Rect bounding_rect = new Rect();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat thresh = new Mat();

        Imgproc.cvtColor(mat, thresh, Imgproc.COLOR_BGR2GRAY);//grayscale
        Imgproc.threshold(thresh, thresh,125,255, Imgproc.THRESH_BINARY);//threshold

        //find contours
        Imgproc.findContours(thresh, contours, thresh, Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);

        for(int contour_index = 0; contour_index < contours.size(); contour_index++)
        {
            double contour_area = Imgproc.contourArea(contours.get(contour_index));
            if(contour_area > largest_area)
            {
                largest_area = contour_area;
                largest_contour_index = contour_index;
                bounding_rect = Imgproc.boundingRect(contours.get(contour_index));
            }//end if statement
        }//end for loop

        Mat result = mat.submat(bounding_rect);
        return result;
    }//end cropAOI
}//end class

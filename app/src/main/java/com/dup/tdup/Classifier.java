package com.dup.tdup;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Classifier
{
    //Constant variables
    private final int DIM_BATCH_SIZE = 1;
    private final int DIM_PIXEL_SIZE = 3;
    private final int outputW = 96;
    private final int outputH = 96;
    protected final int imageSizeX = 192;
    protected final int imageSizeY = 192;
    private final String modelPath = "model.tflite";
    private final int numBytesPerChannel = 4;
    private final String TAG = "C-CLASSIFIER: ";
    private final int LABEL_LENGTH = 14;

    //Other variables
    private Interpreter tflite = null;
    private ByteBuffer imgData = null;
    private Mat mMat = null;

    private int[] intValues = new int[imageSizeX*imageSizeY];
    protected float[][] mPrintPointArray = null;
    private float[][][][] heatmapArray = new float[1][outputW][outputH][14];

    //Constructor
    public Classifier(Activity activity) throws IOException
    {
        this.tflite = new Interpreter(loadModelFile(activity));
        this.imgData = ByteBuffer.allocateDirect(DIM_BATCH_SIZE
                *DIM_PIXEL_SIZE
                *imageSizeX
                *imageSizeY
                *numBytesPerChannel);
        this.imgData.order(ByteOrder.nativeOrder());
        Log.d(TAG," classifier has created.");
    }//End constructor


    //Load model file
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException
    {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        Log.d(TAG, " model file loaded. . .");
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }//End loadModelFile


    public void classifyFrame(Bitmap bitmap)
    {
        if(tflite == null)
        {
            Log.d(TAG, " tflite model is null !");
            return;
        }//end if statement

        bitmapToByteBuffer(bitmap);
        runInference();
        Log.d(TAG, " frame classified. . .");
    }

    //Store bitmap data into byteBuffer
    private void bitmapToByteBuffer(Bitmap bitmap)
    {
        if(imgData == null){return;} //do nothing

        imgData.rewind();
        bitmap.getPixels(intValues, 0,
                bitmap.getWidth(),
                0,0,
                bitmap.getWidth(),
                bitmap.getHeight());

        int pixel = 0; //as counter
        for(int i = 0; i < imageSizeX; i++)
        {
            for(int j= 0; j < imageSizeY; j++)
            {
                int value = intValues[pixel++];
                imgData.putFloat((value) & 0xFF); //B
                imgData.putFloat((value >> 8)  & 0xFF); //G;
                imgData.putFloat((value >> 16) & 0xFF); //R
            }
        }
        Log.d(TAG, " bitmap stored into a bytebuffer. . .");
    }//end bitmapToByteBuffer


    private void runInference()
    {
        tflite.run(imgData, heatmapArray);

        if(mPrintPointArray == null){mPrintPointArray = new float[2][14];}
        if (mMat == null){mMat = new Mat(outputW, outputH, CvType.CV_32F);}

        float[] tempArray = new float[outputW*outputH];
        float[] outTempArray = new float[outputW*outputH];

        for(int i = 0; i < LABEL_LENGTH; i++)
        {
            int index = 0;
            for(int x = 0; x < outputW; x++)
            {
                for(int y = 0; y < outputH; y++)
                {
                    tempArray[index] = heatmapArray[0][y][x][i];
                    index++;
                }//end for loop
            }//end for loop

            mMat.put(0,0, tempArray);
            Imgproc.GaussianBlur(mMat, mMat, new Size(5 , 5),0,0);
            mMat.get(0,0,outTempArray);

            float xMax = 0f;
            float yMax = 0f;
            float vMax = 0f;

            for(int x = 0; x < outputW; x++)
            {
                for(int y = 0; y < outputH; y++)
                {
                    float value = get(x, y, outTempArray);
                    if(value > vMax)
                    {
                        vMax = value;
                        xMax = x;
                        yMax = y;
                    }//end if statement
                }//end for loop
            }//end for loop

            mPrintPointArray[0][i] = xMax;
            mPrintPointArray[1][i] = yMax;
        }//end for-loop
    }//end runInference

    private float get(int x, int y, float[] arr)
    {
        if(x < 0 || y < 0 || x >= outputW || y >= outputH){return -1f;}
        else{return arr[x*outputW+y];}
    }//end get


    public void close()
    {
        tflite.close();
        tflite = null;
        Log.d(TAG, " tensorflow model has closed..");
    }//end close
}//END Classifier
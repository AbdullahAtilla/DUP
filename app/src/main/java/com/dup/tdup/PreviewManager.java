package com.dup.tdup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


/*
* This class handles with camera preview to place outfit on user.
* It periodically classifies the frame
* during camera preview. According to the result of classification,
* detects certain points on users body and place the outfit on user
* by using these points.
* */
public class PreviewManager extends Fragment implements FragmentCompat.OnRequestPermissionsResultCallback
{
    private final String TAG = "C-PREVIEWMANAGER: "; //log TAG
    private final Object lock = new Object();
    private boolean runClassifier = false;

    private AutoFitTextureView autoFitTextureView = null;
    private AutoFitFrameLayout autoFitFrameLayout = null;
    private DrawView drawView = null; //to place outfit on user

    private Classifier classifier = null;
    private ImageReader imageReader = null;

    private Size previewSize = null;
    private String cameraId = null;

    private static int MAX_PREVIEW_WIDTH = 1920;
    private static int MAX_PREVIEW_HEIGHT = 1080;

    private Handler backgroundHandler = null;
    private HandlerThread backgroundThread = null;
    private static String HANDLE_THREAD_NAME = "CameraBackground";

    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private CameraCaptureSession captureSession = null;
    private CameraDevice cameraDevice = null;
    private CaptureRequest.Builder previewRequestBuilder = null;
    private CaptureRequest previewRequest = null;


    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener()
    {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height)
        {
            try {openCamera(width, height); Log.d(TAG, " camera has opened. . .");}
            catch(CameraAccessException e){Log.d(TAG, " camera can not opened ![STL]");}
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height)
        {configureTransform(width, height);}
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface){return true;}
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface){}
    };//end surfaceTextureListener


    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback()
    {
        @Override
        public void onOpened(@NonNull CameraDevice camera)
        {
            cameraOpenCloseLock.release();
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera)
        {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error)
        {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
            Activity activity = getActivity();
            activity.finish();
        }
    };//end stateCallback

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback()
    {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult){}

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result){}
    };

    //Create camera preview session
    private void createCameraPreviewSession()
    {
        try
        {
            SurfaceTexture texture = autoFitTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(
                    Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback()
                    {
                        @Override
                        public void onConfigured(CameraCaptureSession session)
                        {
                            if(cameraDevice == null){return;}
                            captureSession = session;
                            try
                            {
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(previewRequest,
                                                                    captureCallback,
                                                                    backgroundHandler);
                            }catch(CameraAccessException e){}
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {}
                    },
                    null);//End cameraDevice.createCaptureSession
            Log.d(TAG, " camera preview has started. . .");
        }catch(CameraAccessException e){Log.d(TAG, " preview session can not be created! [Camera Access Exception]");}//End catch block
    }//End createCameraPreviewSession




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {return inflater.inflate(R.layout.activity_fit_preview, container, false);}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        autoFitFrameLayout = (AutoFitFrameLayout) view.findViewById(R.id.autofitFrameLayout_fit_preview);
        autoFitTextureView = (AutoFitTextureView) view.findViewById(R.id.autoFitTextureView_fit_preview);
        drawView = (DrawView) view.findViewById(R.id.drawView_fit_preview);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        try
        {
            classifier = new Classifier(getActivity());
            if(drawView != null)
            {drawView.setImgSize(classifier.imageSizeX, classifier.imageSizeY);}
            Log.d(TAG, " activity has been created successfully. . .");
        }
        catch(IOException e){Log.d(TAG, " activity can not be created![IOException]", e);}
        startBackgroundThread();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        startBackgroundThread();
        if(autoFitTextureView.isAvailable())
        {
            try
            {openCamera(autoFitTextureView.getWidth(),autoFitTextureView.getHeight());}
            catch (CameraAccessException e)
            {Log.d(TAG, " camera can not be opened [CameraAccessException]");}
        }
        else
        {
            autoFitTextureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause()
    {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onDestroy()
    {
        classifier.close();
        super.onDestroy();
    }


    //Classification
    ///////////////////////////7///////////////////////
    private Runnable periodicClassify = new Runnable()
    {
        @Override
        public void run()
        {
            synchronized(lock)
            {
                if(runClassifier)
                {
                    classifyFrame();
                }
            }
            backgroundHandler.post(periodicClassify);
        }
    };


    private void classifyFrame()
    {
        if (classifier == null || getActivity() == null || cameraDevice == null)
        {Log.d(TAG, " frame can not be classified due to null element(s) !");return;}

        else
            {
                Bitmap bmp = autoFitTextureView.getBitmap(classifier.imageSizeX, classifier.imageSizeY);
                classifier.classifyFrame(bmp);
                bmp.recycle();
                drawView.setDrawPoint(classifier.mPrintPointArray, 0.5f);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run(){drawView.invalidate();}});

                Log.d(TAG, " frame classified. . .");
            }
    }

    //THREADS
///////////////////////////    ///////////////////////////
    private void startBackgroundThread()
    {
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        synchronized (lock){runClassifier = true;}
        backgroundHandler.post(periodicClassify);
        Log.d(TAG, " background thread has started. . .");
    }

    private void stopBackgroundThread()
    {
        backgroundThread.quitSafely();
        try
        {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
            synchronized(lock){runClassifier = false;}
            Log.d(TAG, " background thread has successfully stopped. . .");
        }catch(InterruptedException e){Log.d(TAG, " background thread can not stop successfully !");}
    }


    //CAMERA MANIPULATION
///////////////////////////    ///////////////////////////
    private void setUpCameraOutputs(int width, int height)
    {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try
        {
            for(String cameraId : manager.getCameraIdList())
            {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT){continue;}

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if(map == null){continue;}

                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);

                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;

                switch (displayRotation)
                {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (sensorOrientation == 90 || sensorOrientation == 270)
                        {swappedDimensions = true;} break;

                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (sensorOrientation == 0 || sensorOrientation == 180)
                        {swappedDimensions = true;} break;
                    default: Log.d(TAG, " display rotation is invalid ! ["+displayRotation+"]");
                }//End switch

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if(swappedDimensions)
                {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if(maxPreviewWidth > MAX_PREVIEW_WIDTH){maxPreviewWidth = MAX_PREVIEW_WIDTH;}
                if(maxPreviewHeight > MAX_PREVIEW_HEIGHT){maxPreviewHeight = MAX_PREVIEW_HEIGHT;}

                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth,
                        rotatedPreviewHeight,
                        maxPreviewWidth,
                        maxPreviewHeight,
                        largest);

                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                    autoFitFrameLayout.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                    autoFitTextureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                    drawView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                }else
                {
                    autoFitFrameLayout.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                    autoFitTextureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                    drawView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                }

                this.cameraId = cameraId;
                Log.d(TAG, " setUpCameraOutputs has successfull. . .");
                return;
            }
        }//End try
        catch(CameraAccessException e)
        {Log.d(TAG, " setUpCameraOutputs has not successfull due to CameraAccessException!");}
        catch(NullPointerException e)
        {Log.d(TAG, " setUpCameraOutputs has not successfull due to NullPointerException!");}
    }//End setUpCameraOutputs


    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) throws CameraAccessException
    {
        //permissionManager.getPermissions(getActivity());//get required permissions
        setUpCameraOutputs(width,height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try
        {
            if(!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
                {throw new RuntimeException("Time out waiting to lock camera opening.");}
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        }catch (CameraAccessException e){}
         catch (InterruptedException e)
         {throw new RuntimeException("Interrupted while trying to lock camera opening.", e);}
        Log.d(TAG, " camera has opened successfully. . .");
    }

    private void closeCamera()
    {
        try
        {
            cameraOpenCloseLock.acquire();
            if(captureSession != null)
            {
                captureSession.close();
                captureSession = null;
            }
            if(cameraDevice != null)
            {
                cameraDevice.close();
                cameraDevice = null;
            }
            if(imageReader != null)
            {
                imageReader.close();
                imageReader = null;
            }
            Log.d(TAG, " camera has closed successfully...");
        }catch(InterruptedException e)
        {throw new RuntimeException("Interrupted while trying to lock camera closing.", e);}
        finally{cameraOpenCloseLock.release();}
    }

    private Size chooseOptimalSize(Size[] choices,
                                   int textureViewWidth,
                                   int textureViewHeight,
                                   int maxWidth,
                                   int maxHeight,
                                   Size aspectRatio)
    {
        ArrayList<Size> bigEnough = new ArrayList<Size>();
        ArrayList<Size> notBigEnough = new ArrayList<Size>();

        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for(Size option : choices)
        {
            if(option.getWidth() <= maxWidth
                    && option.getHeight() <= maxHeight
                    && option.getHeight() == option.getWidth() * h / w)
            {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight)
                {bigEnough.add(option);}
                else{notBigEnough.add(option);}
            }//end if
        }//end for loop

        if(bigEnough.size() > 0)
        {
            Log.d(TAG, " optimal size has found. . .[b]");
            return Collections.min(bigEnough, new CompareSizesByArea());}
        if(notBigEnough.size() > 0)
        {
            Log.d(TAG, " optimal size has found. . . [nb]");
            return Collections.max(notBigEnough, new CompareSizesByArea());}
        else
            {
                Log.d(TAG, " optimal size can not found !");
                return choices[0];
            }
    }//End chooseOptimalSize



    private class CompareSizesByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size o1, Size o2)
        {
            return Long.signum((long) (o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight()));
        }
    }//End CompareSizesByAreaClass



    private void configureTransform(int viewWidth, int viewHeight)
    {
        Activity activity = getActivity();
        if(autoFitTextureView == null || previewSize == null || activity == null)
        {return;}

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0f, 0f, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0f,0f, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
        {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            int scale = Math.max(viewHeight / previewSize.getHeight(),
                    viewWidth / previewSize.getWidth());

            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate((90 * (rotation - 2)), centerX, centerY);
        }else if(rotation == Surface.ROTATION_180)
        {
            matrix.postRotate(180f, centerX, centerY);
        }

        autoFitTextureView.setTransform(matrix);
    }//End configureTransform

    //create new PreviewManager instance
    public static PreviewManager newInstance(){return new PreviewManager();}
}//End Class

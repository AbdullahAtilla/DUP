package com.dup.tdup;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class FitPreviewActivity extends Activity
{

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch(status)
            {
                case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION: return;
                case LoaderCallbackInterface.INIT_FAILED: return;
                case LoaderCallbackInterface.INSTALL_CANCELED: return;
                case LoaderCallbackInterface.MARKET_ERROR: return;
                default: super.onManagerConnected(status);
            }
        }
    };//End mLoaderCallback

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fit_preview);

        if(savedInstanceState == null)
        {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.FrameContainer_fit_preview, PreviewManager.newInstance())
                    .commit();
        }
    }//end onCreate

    @Override
    public void onResume()
    {
        super.onResume();
        if(OpenCVLoader.initDebug())
        {
            Log.i("Test Model -- ", "OpenCV initialize success");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{Log.i("Test Model -- ", "OpenCV initialize failed");}
    }//End onResume
}//end class

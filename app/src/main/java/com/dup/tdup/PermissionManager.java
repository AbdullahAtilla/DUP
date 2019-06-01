package com.dup.tdup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

@SuppressLint("Registered")
public class PermissionManager extends AppCompatActivity
{
    private Activity act = null;
    private boolean allPermissionsGranted = false;
    private String[] permissionList = {Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final int REQUEST_CODE = 999;
    public PermissionManager(Activity act){this.act = act;}


    private boolean checkPermissions()
    {
        for(String perm:permissionList)
        {
            if(ContextCompat.checkSelfPermission(act, perm) != PackageManager.PERMISSION_GRANTED)
            {
                allPermissionsGranted = false;
                return false;
            }
            else{allPermissionsGranted = true;}
        }//end for loop
        return allPermissionsGranted;
    }//end checkPermissions

    public void requestPerms()
    {
        if(checkPermissions()) return;
        else
            {ActivityCompat.requestPermissions(act, permissionList,REQUEST_CODE);}
    }//end requestPerms
}//end class

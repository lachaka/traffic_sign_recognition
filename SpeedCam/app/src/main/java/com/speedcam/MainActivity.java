package com.speedcam;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION_CODE = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkIfAlreadyHavePermissions()) {
            Toast.makeText(MainActivity.this,
                    R.string.grantedPermissions, Toast.LENGTH_SHORT).show();
            switchActivity();
        } else {
            ActivityCompat.requestPermissions(this, 
                new String[] {
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_PERMISSION_CODE);
        }
    }

    private boolean checkIfAlreadyHavePermissions() {
        int cameraPermission = ContextCompat
                .checkSelfPermission(this, Manifest.permission.CAMERA);
        int locationPermission = ContextCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                locationPermission == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permissions have now been granted.");
                    switchActivity();
                } else {
                    Log.i(TAG, "Permissions have now been DENIED.");
                    finishAndRemoveTask ();
                }
                return;
            }
        }
    }

    private void switchActivity() {
        startActivity(new Intent(this, HomeActivity.class));
    }
}

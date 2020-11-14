package com.gev.hw1;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.widget.Toast;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import android.graphics.Bitmap;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import android.os.Bundle;

import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    String name = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if(!report.areAllPermissionsGranted()){
                            Toast.makeText(MainActivity.this, "You need to grant all permission to use this app features", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                })
                .check();

        Button startButton = (Button)findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cvIntent = new Intent(MainActivity.this, OpenCvCamera.class);
                startActivityForResult(cvIntent, 1);
            }
        });

        Button scannerButton = (Button)findViewById(R.id.document_button);
        scannerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cvIntent = new Intent(MainActivity.this, Processing.class);
                cvIntent.putExtra("name", name);
                startActivity(cvIntent);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null)
        {
            return;
        }
        name = data.getStringExtra("name");
    }
    }
package com.bixolon.printer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


public class MainActivity extends AppCompatActivity {

    //@RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        restartService();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    public boolean isPermissionGranted() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ) {

                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.BLUETOOTH_SCAN,
                                                                             Manifest.permission.BLUETOOTH_CONNECT }, 202);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 201);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager())
        {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allPermissionGranted = true;

        for(int i = 0; i < grantResults.length; i++){
            if (grantResults[i] == PackageManager.PERMISSION_DENIED){
                allPermissionGranted = false;
                break;
            }
        }

        if (allPermissionGranted) {
            runService();
        }

    }

    public void runService(){
        Data data = new Data.Builder()
                .putBoolean("startServiceOnly", true)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BXLPrintWorker.class)
                .setInputData(data).addTag("PrintService").build();

        WorkManager.getInstance(this).enqueue(request);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){

            Data data = new Data.Builder()
                    .putBoolean("startServiceOnly", true)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BXLPrintWorker.class)
                    .setInputData(data).addTag("PrintService").build();

            WorkManager.getInstance(this).enqueue(request);
        }
        else
        {
            Intent intent = new Intent(getBaseContext(), BXLPrintService.class);
            intent.putExtra("startServiceOnly", true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            }
            else {
                startService(intent);
            }
        }*/
    }
    public void  restartService(){
        if (isPermissionGranted()){
            runService();
        }
    }

    public void restartService(View view) {
        restartService();
    }


}

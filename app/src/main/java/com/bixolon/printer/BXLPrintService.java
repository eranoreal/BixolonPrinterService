package com.bixolon.printer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.bixolon.printer.PrinterControl.BixolonPrinter;
import com.bxl.config.editor.BXLConfigLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;


public class BXLPrintService extends Service {
    public BXLPrintService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return  null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if(Build.VERSION.SDK_INT >= 24)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

        Toast.makeText(this, "Printer service running.", Toast.LENGTH_LONG).show();


    }

    static BixolonPrinter bixolonPrinter = null;

    static String printerAddress = "", printerModel = "";
    static String intentPrinterAddress= "", intentPrinterModel= "";

    static Bitmap lastBitmap;
    static String lastBitmapPath = "";
    static boolean printerBusy;
    static  List<ArrayList<String>> lineQue;

    private Boolean openPrinter(String printerModel, String printerAddress){

        bixolonPrinter =  new BixolonPrinter(this);
        lineQue = new ArrayList<ArrayList<String>>();

        return bixolonPrinter.printerOpen(BXLConfigLoader.DEVICE_BUS_BLUETOOTH, printerModel,
                                                        printerAddress,true);
    }

    private void closePrinter(){
        bixolonPrinter.printerClose();
        bixolonPrinter = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        if(intent.getBooleanExtra("startServiceOnly", false))
        {
            if(printerAddress == null || printerAddress.equals("") || !openPrinter(printerModel, printerAddress)){
                Toast.makeText(this, "Waiting for printer connection.", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(this, "Currently connected to ." + printerModel +
                        "("+ printerAddress + ")", Toast.LENGTH_LONG).show();
            }

            return START_NOT_STICKY;
        }

        boolean forceConnect = intent.getBooleanExtra("forceConnect", false);
        boolean forceDisconnect = intent.getBooleanExtra("forceDisconnect", false);

        intentPrinterAddress = intent.getStringExtra("printerAddress");
        intentPrinterModel = intent.getStringExtra("printerModel");

        if(!forceConnect && forceDisconnect){
            if (bixolonPrinter != null) {
                closePrinter();
            }
            return START_NOT_STICKY;
        }

        if (forceConnect) {
            if (bixolonPrinter != null) {
                closePrinter();
            }
            if(!openPrinter(intentPrinterModel, intentPrinterAddress)) {
                Toast.makeText(this, "Printer not found.", Toast.LENGTH_LONG).show();
                closePrinter();
            }
            else {
                Toast.makeText(this, "Printer connected successfully.", Toast.LENGTH_LONG).show();
            }
        }
        else {

            final List<String> lines = new ArrayList<>(intent.getStringArrayListExtra("lines"));

            Thread thread = new Thread(new Runnable() {
                public void run()
                {
                    printerBusy = true;
                    final Gson gson = new GsonBuilder()
                            .registerTypeAdapter(Line.class, new LineDeserializer())
                            .create();

                    if(Build.VERSION.SDK_INT >= 30)
                    {
                        for (int i = 0; i < lines.size(); i++) {
                            print(gson.fromJson(lines.get(i), Line.class));

                            try {
                                Thread.sleep(15);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {

                        bixolonPrinter.beginTransactionPrint();
                        for (int i = 0; i < lines.size(); i++) {
                            print(gson.fromJson(lines.get(i), Line.class));
                        }
                        bixolonPrinter.endTransactionPrint();

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    printerBusy =false;


                }
            });

            if (bixolonPrinter == null) {
                if (!openPrinter(intentPrinterModel, intentPrinterAddress)) {
                    Toast.makeText(this, "Printer not found.", Toast.LENGTH_LONG).show();
                    bixolonPrinter = null;
                }
                else {
                    if(!printerBusy) {
                        thread.start();
                    }
                    else {
                        Toast.makeText(this, "Printer still printing, please wait.", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                if (!printerAddress.equals("") && !printerAddress.equals(intentPrinterAddress)) {
                    closePrinter();

                    if (!openPrinter(intentPrinterModel, intentPrinterAddress)) {
                        Toast.makeText(this, "Printer not found.", Toast.LENGTH_LONG).show();
                    }
                    else {
                        if(!printerBusy) {
                            thread.start();
                        }
                        else {
                            Toast.makeText(this, "Printer still printing, please wait.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                else{
                    if(!printerBusy) {
                        thread.start();
                    }
                    else {
                        Toast.makeText(this, "Printer still printing, please wait.", Toast.LENGTH_LONG).show();
                    }

                }
            }
        }

        printerAddress = intentPrinterAddress;
        printerModel = intentPrinterModel;

        return START_NOT_STICKY;

    }

    public void print(Line line){

        switch (line.type) {
            case "text":
                if (line.value.isEmpty()) {
                    bixolonPrinter.printText("\n", line.alignment, line.attribute, line.textsizewidth);
                } else {
                    bixolonPrinter.printText(line.value, line.alignment, line.attribute, line.textsizewidth);
                }

                break;
            case "images":

                boolean reUsedBitmap = lastBitmapPath.equals(line.value);

                if (!reUsedBitmap) {
                    lastBitmap = BitmapFactory.decodeFile(line.value);
                    lastBitmapPath = line.value;
                }

                bixolonPrinter.printImage(lastBitmap, line.textsizewidth, line.alignment, 50, 1, 1);

                break;
            case "barcodes":
                bixolonPrinter.printBarcode(line.value, line.symbology, line.textsizewidth, line.textsizeheight, line.alignment, line.textposition);

                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Printer service stopped.", Toast.LENGTH_LONG).show();
        //stopSelf();
        //stopping the player when service is destroyed

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.bixolon.printer";
        String channelName = "Bixolon Printer Service is running.";
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Bixolon Printer Service is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }


}

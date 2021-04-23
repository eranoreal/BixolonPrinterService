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
    static String lastBitmapPath;

    private Boolean openPrinter(String printerModel, String printerAddress){

        bixolonPrinter =  new BixolonPrinter(this);

        return bixolonPrinter.printerOpen(BXLConfigLoader.DEVICE_BUS_BLUETOOTH, printerModel, printerAddress,true);
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
        intentPrinterAddress = intent.getStringExtra("printerAddress");
        intentPrinterModel = intent.getStringExtra("printerModel");

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
            if (bixolonPrinter == null) {
                if (!openPrinter(intentPrinterModel, intentPrinterAddress)) {
                    Toast.makeText(this, "Printer not found.", Toast.LENGTH_LONG).show();
                    bixolonPrinter = null;
                }
            } else {
                if (!printerAddress.equals("") && !printerAddress.equals(intentPrinterAddress)) {
                    closePrinter();
                    if (!openPrinter(intentPrinterModel, intentPrinterAddress)) {
                        Toast.makeText(this, "Printer not found.", Toast.LENGTH_LONG).show();
                    }
                }
            }

            final List<String> lines = new ArrayList<>(intent.getStringArrayListExtra("lines"));

            final Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Line.class, new LineDeserializer())
                    .create();

            Thread thread = new Thread() {
                public void run() {
                    bixolonPrinter.beginTransactionPrint();

                    for (int i = 0; i < lines.size(); i++) {

                        Line line = gson.fromJson(lines.get(i), Line.class);

                        if (line.type.equals("text")) {
                            if (line.value.isEmpty()) {
                                if (!bixolonPrinter.printText("\n", line.alignment, line.attribute, line.textsizewidth)) {

                                }
                            } else {
                                if (!bixolonPrinter.printText(line.value, line.alignment, line.attribute, line.textsizewidth)) {

                                }
                            }

                        } else if (line.type.equals("images")) {

                            if (lastBitmapPath != line.value) {
                                lastBitmap = BitmapFactory.decodeFile(line.value);
                            }

                            if (!bixolonPrinter.printImage(lastBitmap, line.textsizewidth, line.alignment, 50, 0, 0)) {

                            }

                        } else if (line.type.equals("barcodes")) {
                            if (!bixolonPrinter.printBarcode(line.value, line.symbology, line.textsizewidth, line.textsizeheight, line.alignment, line.textposition)) {

                            }
                        }
                    }

                    bixolonPrinter.endTransactionPrint();
                }
            };
            thread.start();
        }

        this.printerAddress = intentPrinterAddress;
        this.printerModel = intentPrinterModel;

        return START_NOT_STICKY;

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
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Bixolon Printer Service is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }


}

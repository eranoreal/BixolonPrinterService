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
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.bixolon.printer.PrinterControl.BixolonPrinter;
import com.bxl.config.editor.BXLConfigLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BXLPrintService extends Service {
    private static BixolonPrinter bixolonPrinter = null;
    private static String printerAddress = "";
    private static String printerModel = "";
    private static String intentPrinterAddress = "";
    private static String intentPrinterModel = "";
    private static Bitmap lastBitmap;
    private static String lastBitmapPath = "";
    private static boolean printerBusy;

    public BXLPrintService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

        Toast.makeText(this, "Printer service running.", Toast.LENGTH_LONG).show();
    }

    private Boolean openPrinter(String printerModel, String printerAddress) {

        if(bixolonPrinter != null)
            return true;

        bixolonPrinter = new BixolonPrinter(this);
        return bixolonPrinter.printerOpen(BXLConfigLoader.DEVICE_BUS_BLUETOOTH, printerModel, printerAddress, true);
    }

    private void closePrinter() {
        if (bixolonPrinter != null) {
            bixolonPrinter.printerClose();
            bixolonPrinter = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra("startServiceOnly", false)) {
            if (printerAddress == null || printerAddress.isEmpty() || !openPrinter(printerModel, printerAddress)) {
                Toast.makeText(this, "Waiting for printer connection.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Currently connected to " + printerModel + " (" + printerAddress + ")", Toast.LENGTH_LONG).show();
            }
            return START_NOT_STICKY;
        }

        boolean forceConnect = intent.getBooleanExtra("forceConnect", false);
        boolean forceDisconnect = intent.getBooleanExtra("forceDisconnect", false);
        intentPrinterAddress = intent.getStringExtra("printerAddress");
        intentPrinterModel = intent.getStringExtra("printerModel");

        if (forceDisconnect){
            closePrinter();
            Toast.makeText(this, "Printer disconnected.", Toast.LENGTH_LONG).show();
        }
        else if (forceConnect) {

            closePrinter();

            if (!openPrinter(intentPrinterModel, intentPrinterAddress)) {
                Toast.makeText(this, "Printer not found.", Toast.LENGTH_LONG).show();
                closePrinter();
            } else {
                Toast.makeText(this, "Printer connected successfully.", Toast.LENGTH_LONG).show();
            }
        } else {
            final List<String> lines = new ArrayList<>(intent.getStringArrayListExtra("lines"));

            if (bixolonPrinter == null) {
                if (!openPrinter(intentPrinterModel, intentPrinterAddress)) {
                    Toast.makeText(this, "Printer not found.", Toast.LENGTH_LONG).show();
                    bixolonPrinter = null;
                } else {
                    if (!printerBusy) {
                        printLines(lines);
                    } else {
                        Toast.makeText(this, "Printer is busy.", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                if (!printerAddress.isEmpty() && !printerAddress.equals(intentPrinterAddress)) {
                    closePrinter();

                    if (!openPrinter(intentPrinterModel, intentPrinterAddress)) {
                        Toast.makeText(this, "Printer not found.", Toast.LENGTH_LONG).show();
                    } else {
                        if (!printerBusy) {
                            printLines(lines);
                        } else {
                            Toast.makeText(this, "Printer is busy.", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    if (!printerBusy) {
                        printLines(lines);
                    } else {
                        Toast.makeText(this, "Printer is busy.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

        printerAddress = intentPrinterAddress;
        printerModel = intentPrinterModel;

        return START_NOT_STICKY;
    }

    private void printLines(List<String> lines) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new LongOperation(lines));
    }

    private final class LongOperation implements Runnable {
        private List<String> lines;
        LongOperation(List<String> lines) {
            this.lines = lines;
        }

        @Override
        public void run() {
            printerBusy = true;
            final Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Line.class, new LineDeserializer())
                    .create();

            bixolonPrinter.beginTransactionPrint();

            for (String lineJson : lines) {
                Line line = gson.fromJson(lineJson, Line.class);
                print(line);
            }

            bixolonPrinter.endTransactionPrint();
            printerBusy = false;
        }
    }

    public void print(Line line) {
        switch (line.Type) {
            case "text":
                String value = line.Value.isEmpty() ? "\n" : line.Value;
                bixolonPrinter.printText(value, line.Alignment, line.Attribute, line.TextSizeWidth);
                break;
            case "images":
                boolean reUsedBitmap = lastBitmapPath.equals(line.Value);

                if (!reUsedBitmap) {
                    lastBitmap = BitmapFactory.decodeFile(line.Value);
                    lastBitmapPath = line.Value;
                }

                bixolonPrinter.printImage(lastBitmap, line.TextSizeWidth, line.Alignment, 50, 1, 1);
                break;
            case "barcodes":
                bixolonPrinter.printBarcode(line.Value, line.Symbology, line.TextSizeWidth, line.TextSizeHeight, line.Alignment, line.TextPosition);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closePrinter();
        Toast.makeText(this, "Printer service stopped.", Toast.LENGTH_LONG).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = getString(R.string.packageName);
        String channelName = getString(R.string.bixolonServiceRunning);
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle(getString(R.string.bixolonServiceRunning))
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }
}

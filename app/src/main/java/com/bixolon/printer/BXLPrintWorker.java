package com.bixolon.printer;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.bixolon.printer.PrinterControl.BixolonPrinter;
import com.bxl.config.editor.BXLConfigLoader;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Base64;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;

public class BXLPrintWorker extends Worker {

    private static BixolonPrinter bixolonPrinter = null;

    private static String printerAddress = "";
    private static String printerModel = "";
    private static String intentPrinterAddress = "";
    private static String intentPrinterModel = "";

    private static Bitmap lastBitmap;
    private static String lastBitmapPath = "";
    private static boolean printerBusy;

    private Context context;

    public BXLPrintWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();

        if (inputData.getBoolean("startServiceOnly", false)) {
            if (!openPrinter(printerModel, printerAddress)) {
                showMessage("Waiting for printer connection.");
            } else {
                showMessage("Currently connected to: " + printerModel + "(" + printerAddress + ")");
            }
        } else if (inputData.getBoolean("forceDisconnect", false)) {
            if (closePrinter()) {
                showMessage("Currently connected printer disconnected.");
            }
        } else {
            boolean forceConnect = inputData.getBoolean("forceConnect", false);
            boolean connectOnly = inputData.getBoolean("connectOnly", false);

            intentPrinterAddress = inputData.getString("printerAddress");
            intentPrinterModel = inputData.getString("printerModel");

            if (forceConnect) {
                if (bixolonPrinter != null) {
                    closePrinter();
                }

                if (openPrinter(intentPrinterModel, intentPrinterAddress)) {
                    showMessage("Printer connected successfully.");
                }

                if (connectOnly) {
                    return Result.success();
                }
            }

            if (printerBusy) {
                showMessage("Printer still printing, please wait.");
            } else {
                final String linesJson = inputData.getString("lines");

                if (bixolonPrinter == null) {
                    if (openPrinter(intentPrinterModel, intentPrinterAddress)) {
                        executePrintLines(linesJson);
                    }
                } else {
                    if (!isNullOrWhitespace(printerAddress) && !printerAddress.equals(intentPrinterAddress)) {
                        closePrinter();

                        if (openPrinter(intentPrinterModel, intentPrinterAddress)) {
                            executePrintLines(linesJson);
                        }
                    } else {
                        executePrintLines(linesJson);
                    }
                }
            }

            printerAddress = intentPrinterAddress;
            printerModel = intentPrinterModel;
        }

        return Result.success();
    }

    private boolean openPrinter(String newPrinterModel, String newPrinterAddress) {

        if (isNullOrWhitespace(newPrinterAddress))
            return false;

        if(bixolonPrinter != null && printerAddress == newPrinterAddress)
            return true;

        bixolonPrinter = new BixolonPrinter(getApplicationContext());

        boolean isOpened = bixolonPrinter.printerOpen(BXLConfigLoader.DEVICE_BUS_BLUETOOTH, newPrinterModel,
                newPrinterAddress, true);

        if (!isOpened) {
            showMessage("Printer not found.");
            closePrinter();
        }

        return isOpened;
    }

    private boolean closePrinter() {
        boolean isDisconnected = false;

        if (bixolonPrinter != null) {
            bixolonPrinter.printerClose();
            bixolonPrinter = null;
            printerAddress = null;
            isDisconnected = true;
        }

        return isDisconnected;
    }

    private void executePrintLines(String linesJson) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                printLines(linesJson);
            }
        });

        try {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void printLines(String linesJson) {
        Type listType = new TypeToken<ArrayList<Line>>() {
        }.getType();
        ArrayList<Line> lines = null;

        try {
            String decompressJson = decompress(linesJson);
            lines = new Gson().fromJson(decompressJson, listType);
        } catch (Exception e) {
            e.printStackTrace();
        }

        printerBusy = true;

        bixolonPrinter.beginTransactionPrint();

        for (int i = 0; i < lines.size(); i++) {
            if(!print(lines.get(i))){
                showMessage("Printer not in a printing condition.");
                break;
            }
        }

        bixolonPrinter.endTransactionPrint();

        printerBusy = false;
    }

    private boolean print(Line line) {

        switch (line.Type)
        {
            case "text":
                if (line.Value.isEmpty()) {
                  return bixolonPrinter.printText("\n", line.Alignment, line.Attribute, line.TextSizeWidth);
                } else {
                   return bixolonPrinter.printText(line.Value, line.Alignment, line.Attribute, line.TextSizeWidth);
                }
            case "images":
                boolean reUsedBitmap = lastBitmapPath.equals(line.Value);

                if (!reUsedBitmap) {
                    lastBitmap = BitmapFactory.decodeFile(line.Value);
                    lastBitmapPath = line.Value;
                }

                return bixolonPrinter.printImage(lastBitmap, line.TextSizeWidth, line.Alignment, 50, 1, 1);

            case "barcodes":
                   return  bixolonPrinter.printBarcode(line.Value, line.Symbology, line.TextSizeWidth, line.TextSizeHeight,
                           line.Alignment, line.TextPosition);
            default:
                return false;
         }

    }

    private String decompress(String compressedText) throws Exception {
        byte[] compressed = compressedText.getBytes("UTF8");
        compressed = Base64.decodeBase64(compressed);
        byte[] buffer = new byte[compressed.length - 4];
        buffer = copyForDecompression(compressed, buffer, 4, 0);
        final int BUFFER_SIZE = 32;
        ByteArrayInputStream is = new ByteArrayInputStream(buffer);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        StringBuilder string = new StringBuilder();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            string.append(new String(data, 0, bytesRead));
        }
        gis.close();
        is.close();
        return string.toString();
    }

    private byte[] copyForDecompression(byte[] b1, byte[] b2, int srcOffset, int dstOffset) {
        for (int i = 0; i < b2.length && i < b1.length; i++) {
            b2[i] = b1[i + 4];
        }
        return b2;
    }

    private boolean isNullOrWhitespace(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void showMessage(String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        }, 50);
    }
}

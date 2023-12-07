package com.bixolon.printer;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class BXLContentProvider extends ContentProvider {

    private static final String PROVIDER_NAME = "com.bixolon.printer.BXLContentProvider";
    private static final String URL = "content://" + PROVIDER_NAME + "/print";

    static final int RESTART_PRINTER = 101;
    static final int CONNECT_PRINTER = 102;
    static final int DISCONNECT_PRINTER = 103;
    static final int PRINT_PRINTER = 104;

    private static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "restart/", RESTART_PRINTER);
        uriMatcher.addURI(PROVIDER_NAME, "connect/*/*", CONNECT_PRINTER);
        uriMatcher.addURI(PROVIDER_NAME, "disconnect", DISCONNECT_PRINTER);
        uriMatcher.addURI(PROVIDER_NAME, "print/*/*", PRINT_PRINTER);
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
      return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        Data data = null;
        switch (uriMatcher.match(uri)){
            case RESTART_PRINTER:
                data = new Data.Builder()
                        .putBoolean("startServiceOnly", true)
                        .build();

                break;
            case CONNECT_PRINTER:
                data = new Data.Builder()
                        .putString("printerModel", uri.getPathSegments().get(1))
                        .putString("printerAddress", uri.getPathSegments().get(2))
                        .putBoolean("connectOnly", true)
                        .putBoolean("forceConnect", true)
                        .build();

                break;
            case DISCONNECT_PRINTER:
                data = new Data.Builder()
                    .putBoolean("forceDisconnect", true)
                    .build();
                break;
            case PRINT_PRINTER:
                data = new Data.Builder()
                        .putString("printerModel", uri.getPathSegments().get(1))
                        .putString("printerAddress", uri.getPathSegments().get(2))
                        .putString("lines", values.getAsString("lines"))
                        .putBoolean("forceConnect", values.getAsBoolean("forceConnect"))
                        .build();

                break;
            default:
                break;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BXLPrintWorker.class)
                .setInputData(data).addTag("PrintService").build();

        WorkManager.getInstance(getContext()).enqueue(request);

        return 1;
    }
}

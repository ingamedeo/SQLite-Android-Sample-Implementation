package com.ingamedeo.databasetest.db;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class ContentProviderDb extends ContentProvider {
    private DatabaseHelper dbHelper ;
    private SQLiteDatabase db; //db Object
    public static final String AUTHORITY = "com.ingamedeo.databasetest.contentprovider";//specific for our our app, will be specified in maninfed
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        db = dbHelper.getWritableDatabase(); //Get database - write mode
        return true;
    }

    @Override
    public int delete(Uri uri, String where, String[] args) {
        String table = getTableName(uri);
        int del = db.delete(table, where, args);
        getContext().getContentResolver().notifyChange(uri, null); //Notify what happened here ;)
        return del;
    }

    @Override
    public String getType(Uri arg0) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        String table = getTableName(uri);
        long value = db.insert(table, null, initialValues);
        getContext().getContentResolver().notifyChange(uri, null); //Notify what happened here ;)
        return Uri.withAppendedPath(CONTENT_URI, String.valueOf(value));
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String table =getTableName(uri);
        Cursor c = db.query(table,  projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri); //Registering an Observer
        return c;
        //
    }

    @Override
    public int update(Uri uri, ContentValues values, String whereClause,
                      String[] whereArgs) {
        /* We get a URI like: content://com.example.app.provider/table1/3 */
        String id = uri.getLastPathSegment();
        String table = uri.getPathSegments().get(0);
        whereClause =  UsersTable._ID + "=" + id;
        int upd = db.update(table, values, whereClause, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null); //Notify what happened here ;)
        return upd;
    }

    public static String getTableName(Uri uri){
        String value = uri.getPath();
        value = value.replace("/", "");//we need to remove '/'
        return value;
    }
}

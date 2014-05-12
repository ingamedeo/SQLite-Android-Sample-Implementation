package com.ingamedeo.databasetest.db;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by ingamedeo on 25/04/14.
 */

/* This class belongs to the old implementation and has to be removed! */

public class DbAdapter {

    private Context c; //Context
    private Uri contentUri;

    /* Constructor for DbAdapter */
    public DbAdapter(Context context) {
        this.c = context; //Set context
        contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, UsersTable.TABLE_NAME); //Build Up my Content Provider URI
    }

    private ContentValues createContentValues(String name) { //Creating ContentValues resolved by a ContentResolver (ContentProvider)
        ContentValues values = new ContentValues(); //Create new ContentValues Obj
        values.put(UsersTable.NAME, name); //Put name
        return values; //Return values
    }

    public long createUser(String name) { //Insert a new user in our database
        ContentValues createValues = createContentValues(name);
        Uri newUri = c.getContentResolver().insert(contentUri, createValues); //Insert!
        return ContentUris.parseId(newUri); //Get operation ID
    /* Inserts new record, Return -1 if error, or new record ID if successful */
    }

    public boolean updateUser(long UserID, String name) { //Update a User in our database
        ContentValues updateValues = createContentValues(name);
        //Uri uri = ContentUris.withAppendedId(ContentProviderDb.CONTENT_URI, UserID);
        /*
        * Note the call to ContentUris.withAppendedId().
        * This is a helper method to create an id-based URI from a directory-based one.
        * You use it all the time since content providers only provide constants for directory-based URIs.
        * So whenever you want to access a specific object you should use this method.
         */
        Uri uri = ContentUris.withAppendedId(contentUri, UserID);
        return c.getContentResolver().update(uri, updateValues, null, null) > 0; //Parameters: Table Name, Values to update, WHERE Clause, Strings which replace ?s in WHERE clause (Optional - Only if you put ?s in the WHERE clause)
        /* Updates database record, Returns true if the number of rows affected is > 0, if num of rows = 0 returns false, no row with specified identifier found */
    }

    public boolean deleteUser(long UserID) { //Delete a User from our database
        //If WHERE clause = 1, all rows will be deleted and their number will be returned
        return c.getContentResolver().delete(contentUri, UsersTable._ID + "=" + UserID, null) > 0; //Parameters: Table Name, WHERE clause, Strings which replace ?s in WHERE clause (Optional - Only if you put ?s in the WHERE clause)
        /* Deletes a user from the database, returns True if the number of rows deleted is > 0 (Returns exactly the number of rows), returns False (0 without > 0) if no rows that match specified pattern are found */
    }

    public void deleteMultipleUsers(long[] UserIDs) { //Delete multiple Users from our database
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (long UserID : UserIDs) {
            ops.add(ContentProviderOperation.newDelete(contentUri)
                    .withSelection(UsersTable._ID + "=" + UserID, null)
                    .withYieldAllowed(true)

                    /*
                    .withYieldAllowed(true) EXPLANATION

                    The flip side of using batched operations is that a large batch may lock up the database for a long time preventing other applications from accessing data and potentially causing ANRs ("Application Not Responding" dialogs.)
                    To avoid such lockups of the database, make sure to insert "yield points" in the batch.
                    A yield point indicates to the content provider that before executing the next operation it can commit the changes that have already been made, yield to other requests, open another transaction and continue processing operations.
                    A yield point will not automatically commit the transaction, but only if there is another request waiting on the database.
                    Normally a sync adapter should insert a yield point at the beginning of each raw contact operation sequence in the batch.
                     */

                    .build());
        }

        try {
            c.getContentResolver().applyBatch(ContentProviderDb.AUTHORITY, ops); //Apply all operations
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }

    }
}

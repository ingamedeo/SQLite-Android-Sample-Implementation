package com.ingamedeo.databasetest.db;

import android.provider.BaseColumns;

/**
 * Created by ingamedeo on 25/04/14.
 */

public interface UsersTable extends BaseColumns { //This extends BaseColumns implements "String	_ID	The unique ID for a row."

        /*

    TABLE STRUCTURE

    ----------------
         Users
    ----------------
    _id    ---
    name   ---

     */

    String TABLE_NAME = "Users"; //Table Name
    String NAME = "name"; //Name Column

    String[] COLUMNS = new String[] {_ID, NAME};
}

/*

Author: Amedeo Arch (ingamedeo) <ingamedeo[at]gmail[dot]com>

License: YOU CAN DO WHAT THE FUCK YOU WANT!

References:

- http://www.grokkingandroid.com/using-loaders-in-android/
- http://www.grokkingandroid.com/android-tutorial-using-content-providers/
- http://www.grokkingandroid.com/better-performance-with-contentprovideroperation/
- Official Documentation
- http://developer.android.com/reference/android/content/ContentProvider.html#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
- http://blog.oestrich.org/2013/03/using-android-uri-matcher/
- http://www.vogella.com/tutorials/AndroidSQLite/article.html
- http://stackoverflow.com/questions/5376860/how-to-get-string-from-selected-item-of-simplecursoradapter

 *** OLD WAY *** (Don't use them! - Written in Italian)

- http://www.html.it/articoli/la-gestione-dei-database-in-android-1/
- http://www.anddev.it/index.php?topic=856.0

 */


package com.ingamedeo.databasetest;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.ingamedeo.databasetest.db.ContentProviderDb;
import com.ingamedeo.databasetest.db.DbAdapter;
import com.ingamedeo.databasetest.db.UsersTable;

import java.util.HashMap;
import java.util.Set;

//Implements LoaderCallbacks ;)
public class MainActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    //TextView result;
    EditText inputbox;
    Button addbutton;
    ListView listView;
    private SelectionAdapter adapter;
    private int LOADER_ID = 1; //This our Loader ID. It Has to be unique!
    private Uri contentUri;
    DbAdapter dbAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI Components Declaration
        inputbox = (EditText) findViewById(R.id.inputbox);
        addbutton = (Button) findViewById(R.id.addbutton);
        listView = (ListView) findViewById(R.id.listView);

        //Init ListView Adapter
        adapter = new SelectionAdapter(
                        getApplicationContext(), //Context
                        android.R.layout.simple_list_item_2, //List Layout
                        null, //We have no cursor yet..set it to null
                        new String[] {UsersTable.NAME, UsersTable._ID}, //Columns to fill our ListView
                        new int[] { android.R.id.text1, android.R.id.text2 }, //Where to put our data? This is an android internal reference to android.R.layout.simple_list_item_1
                        0); //0 Here! instead of FLAG_REGISTER_CONTENT_OBSERVER since we are using a CursorLoader with our CursorAdapter (since the CursorLoader registers the ContentObserver for us)

        //Set Adapter to ListView
        listView.setAdapter(adapter);

        //Set ChoiceMode (WARNING: Look in XML file... ListView is ChoiceMode enabled)
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            //nr is the number of selected items..will be shown on the Top of the Action Mode screen.
            private int nr = 0;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                if (checked) { //If we check sth new...
                    nr++; //One more item is checked, increment nr
                    adapter.setNewSelection(position, checked); //Add Item to selection
                } else {
                    nr--;
                    adapter.removeSelection(position); //Remove Item from Selection
                }
                mode.setTitle(nr + " selected");

                mode.invalidate(); //Invalidate our ActionMode
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                //nr is inizialized.. Let's see what happens
                nr = 0;

                //Inflate Menu
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.actionmenu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                MenuItem item = menu.findItem(R.id.item_edit);
                if (nr == 1) {
                    item.setVisible(true);
                } else {
                    item.setVisible(false);
                }
                return true; //True if updated
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Set<Integer> checked = adapter.getCurrentCheckedPosition(); //This is a keySet() of all selected items
                long[] itemids = new long[checked.size()]; //Now we have to get itemids...
                String[] itemnames = new String[checked.size()];
                int cont = 0; //This will increase every time we loop
                for (int el : checked) { //For each element in chacked (el is a position)
                    Cursor cursor = (Cursor) adapter.getItem(el); //Get the item associated with that position
                    itemids[cont] = cursor.getLong(cursor.getColumnIndex(UsersTable._ID)); //Get Item ID (which is different from the position!) and add it to an array
                    itemnames[cont] = cursor.getString(cursor.getColumnIndex(UsersTable.NAME));
                    cont++; //Increase cont by 1
                }
                switch (item.getItemId()) {
                    case R.id.item_delete:
                        //dbAdapter.deleteUser(Long.valueOf(id)); //We are not using the method anymore (Since MultiSelection is enabled!)
                        dbAdapter.deleteMultipleUsers(itemids);
                        nr = 0;
                        adapter.clearSelection();
                        //Close ActionMode
                        mode.finish();
                        return true;
                    case R.id.item_edit:
                        showDialog(itemnames[0], String.valueOf(itemids[0]));
                        nr = 0;
                        adapter.clearSelection();
                        //Close ActionMode
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                adapter.clearSelection();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int position, long arg3) {
                // TODO Auto-generated method stub

                //Check 1st Item when LongClicked
                listView.setItemChecked(position, !adapter.isPositionChecked(position));
                return false; //True > No further processing
            }
        });

        dbAdapter = new DbAdapter(this); //New dpAdapter instance, used for everything except query()

        //Build up contentUri
        contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, UsersTable.TABLE_NAME);

        //Init our loader
        getLoaderManager().restartLoader(LOADER_ID, null, this);
        //getLoaderManager().initLoader(LOADER_ID, null, this);

        /* Why do you call restartLoader instead of initLoader?
        *
        * I've found out that restart does exactly the same thing as init if our loader doesn't exist yet
        */

        addbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!inputbox.getText().toString().trim().isEmpty()) {

                    long id = dbAdapter.createUser(inputbox.getText().toString());
                    Log.i("log_tag", String.valueOf(id));
                    //Clear EditText
                    inputbox.setText("");

                    //Not needed!
                    //getLoaderManager().restartLoader(LOADER_ID, null, MainActivity.this);

                    //Hide Virtual Keyboard after adding a new element
                    InputMethodManager imm = (InputMethodManager)getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(inputbox.getWindowToken(), 0);
                }
            }
        });
    }

    public void showDialog(String name, final String id) {
        final EditText textbox = new EditText(MainActivity.this);
        textbox.setSingleLine(); //Single Line
        textbox.setImeOptions(EditorInfo.IME_ACTION_DONE);
        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(20, 0, 30, 0); //Set up Layout Margins
        layout.addView(textbox, params); //Add Edittext to this layout
        textbox.setText(name);
        new AlertDialog.Builder(this)
                .setTitle("Update this Item")
                .setMessage("Edit Below:")
                .setView(layout) //set View to LinearLayout
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        update(textbox.getText().toString(), Long.valueOf(id)); //Update
                        //Hide Virtual Keyboard after editing an element
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(textbox.getWindowToken(), 0);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    //Update Record
    private boolean update(String name, long id) {
        return dbAdapter.updateUser(id, name);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Starting LoaderCallback implementation */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(
                this,
                contentUri, //Uri
                UsersTable.COLUMNS, //Columns to be returned back
                null, //Select like WHERE
                null, //Selection args, used to replace ? in WHERE statement
                null); //SortMode
        return loader; //We are done here..return our Loader Object!
    }

    /* Returns a Cursor Object which contains the result set in read/write mode */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor!=null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(UsersTable.NAME));

                Log.i("Name: ", name);
            }

            //Load New Data Into our Adapter
            adapter.swapCursor(cursor);
        } else {
            adapter.swapCursor(null); //Reset cursor to null, means our cursor is empty..no records in our database
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null); //Clean up - Adapter data is now null
    }



    //Adapter Def.
    private class SelectionAdapter extends SimpleCursorAdapter {

        //This will contain which Items are selected
        private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

        //Constructor for SelectionAdapter
        public SelectionAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }


        public void setNewSelection(int position, boolean value) {
            mSelection.put(position, value); //Add new Item to selection
            notifyDataSetChanged(); //Notify
        }

        public boolean isPositionChecked(int position) {
            Boolean result = mSelection.get(position); //Check if selected
            return result == null ? false : result;
        }

        public Set<Integer> getCurrentCheckedPosition() {
            return mSelection.keySet();
        }

        public void removeSelection(int position) {
            mSelection.remove(position); //Remove Selection
            notifyDataSetChanged();
        }

        public void clearSelection() { //Clear Everything
            mSelection = new HashMap<Integer, Boolean>();
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);//let the adapter handle setting up the row views
            v.setBackgroundColor(getResources().getColor(android.R.color.transparent)); //default color

            if (mSelection.get(position) != null) { //If this item is selected
                v.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));// this is a selected position so make it blue
            }
            return v;
        }
    }
}
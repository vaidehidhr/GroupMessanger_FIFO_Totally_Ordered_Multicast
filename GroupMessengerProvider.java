package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */

public class GroupMessengerProvider extends ContentProvider {

    static final String TAG = GroupMessengerProvider.class.getSimpleName();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        // My code
        String arg = values.toString();
        String temp[] = arg.split("=");
        String filename = temp[2];
        String filecontents = temp[1].replace(" key","");
        FileOutputStream outputStream;
        Context context = getContext();
        // https://developer.android.com/training/data-storage/files#java
        try {
            outputStream =context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(filecontents.getBytes());
            outputStream.close();
        }
        catch(Exception e) {
            Log.e(TAG, "File write failed");
        }

        // My code ends

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */

        // My code
        String columnnames[] = new String[] {"key","value"};
        MatrixCursor cursor = new MatrixCursor(columnnames);
        String file_list[]=getContext().fileList();         // https://developer.android.com/reference/android/content/Context.html
        int i = 0;
        while (i<file_list.length) {
            if (file_list[i].equals(selection)) {
                FileInputStream temp;
                Context context = getContext();
                try {
                    //https://stackoverflow.com/questions/9095610/android-fileinputstream-read-txt-file-to-string
                    temp = context.openFileInput(file_list[i]);
                    StringBuffer fileContent = new StringBuffer("");
                    byte[] buffer = new byte[1024];
                    int n;
                    while ((n = temp.read(buffer)) != -1) {
                        fileContent.append(new String(buffer, 0, n));
                    }//https://stackoverflow.com/questions/9095610/android-fileinputstream-read-txt-file-to-string ends
                    cursor.addRow(new Object[] {file_list[i], fileContent});    // https://developer.android.com/reference/android/database/MatrixCursor
                    break;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
                i++;
        }
        //cursor.setNotificationUri(getContext().getContentResolver(),uri);     //https://developer.android.com/reference/android/database/Cursor
        // My code ends

        Log.v("query", selection);
        return cursor;
    }
}

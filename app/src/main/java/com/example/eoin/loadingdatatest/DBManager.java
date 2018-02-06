package com.example.eoin.loadingdatatest;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Eoin on 07/11/2017.
 */

/*
Database Manager class used to manage interaction with SQLite Database
Largely adapted from labs and examples given in class
 */

public class DBManager {
    public static final String nodeId = "nodeID";
    public static final String latitude = "lat";
    public static final String longitude = "lon";

    public static final String wayID = "wayID";
    public static final String nd = "nd";

    private static final String DATABASE_NAME = "routeData";
    private static final String DATABASE_TABLE_NODE = "NodeList";
    private static final String DATABASE_TABLE_WAY = "WayList";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE_NODE =
            "create table if not exists NodeList (id integer primary key autoincrement, " +
                    "nodeID long not null, " +
                    "lon text not null, " +
                    "lat text not null);";
    private static final String DATABASE_CREATE_WAY =
            "create table if not exists WayList (id integer primary key autoincrement, " +
                    "wayID long not null, " +
                    "nd long not null, " +
                        "foreign key  (nd) references NodeList(nodeID));";

    private final Context context;
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;
    private ContentValues args = new ContentValues();

    //Constructor
    public DBManager(Context ctx) {
        this.context = ctx;

        DBHelper = new DatabaseHelper(context);
    }//End DBManager

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }//End DatabaseHelper

        //If table does not exist, then it is created
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE_NODE);
            db.execSQL(DATABASE_CREATE_WAY);
        }//End onCreate

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }//End onUpgrade
    }//End DatabaseHelper

    //Opens database so it can be accessed
    public DBManager open() throws SQLException {
        db = DBHelper.getWritableDatabase();
        return this;
    }//end open()

    //Creates table in database if it does not already exist
    public void createTable() {
        db.execSQL(DATABASE_CREATE_NODE);
        db.execSQL(DATABASE_CREATE_WAY);
    }//End createTable

    //Drops table from database
    public void dropTable(String tableName) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
    }//End dropTable

    //Closes database
    public void close() {
        DBHelper.close();
    }//end Close

    //Gets the the table size (ie number of rows)
    public int getTableSize(String tableName) {
        int numRows = (int)DatabaseUtils.queryNumEntries(db, tableName);
        return numRows;
    }//End getTableSize

    //Inserts a new Node into the table
    public long insertNode(long nId, String lon, String lat) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(nodeId, nId);
        initialValues.put(longitude, lon);
        initialValues.put(latitude, lat);
        return db.insert(DATABASE_TABLE_NODE, null, initialValues);
    }//End insertNode

    //Inserts a new Way into the table
    public long insertWay(long wId, long nId) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(wayID, wId);
        initialValues.put(nd, nId);
        return db.insert(DATABASE_TABLE_WAY, null, initialValues);
    }//End insertWay

    //Deletes Node from table, using the id as a key
    public boolean deleteNodeById(long nid) {
        return db.delete(DATABASE_TABLE_NODE, nodeId + "=" + "'" + nid + "'", null) > 0;
    }//deleteNodeById

    //Gets all book Latitudes and Longitudes
    public Cursor getAllNodes() {
        return db.query(DATABASE_TABLE_NODE, new String[] {
                        nodeId,
                        latitude,
                        longitude
                },
                null, null, null, null, null);
    }//End getAllNodes

    //Returns a single row
    public String[] getNode(long nid) throws SQLException {
        Cursor mCursor =
                db.query(true, DATABASE_TABLE_NODE, new String[] {
                                nodeId,
                                longitude,
                                latitude,
                        },
                        nodeId + " = " +  nid, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }//End if
        String retLon = mCursor.getString(1);
        String retLat = mCursor.getString(2);
        String[] array = {retLon, retLat};
        return array;
    }//End getNode()

    public Cursor getWayById(long wid) throws SQLException {
        return db.query(true, DATABASE_TABLE_WAY, new String[] {
                        wayID,
                        nd,
                },
                        wayID + " = " + wid, null, null, null, null, null );
    }//End getWay()

    public long getWayId(long nid) throws SQLException {
        Cursor mCursor= db.query(true, DATABASE_TABLE_WAY, new String[] {
                        wayID,
                        nd,
                },
                nd + " = " + nid, null, null, null, null, null );
        if (mCursor != null) {
            mCursor.moveToFirst();
        }//End if
        return Long.parseLong(mCursor.getString(0));
    }

    public long getNewWayId(long nid, long wId) throws SQLException {
        Cursor mCursor= db.query(true, DATABASE_TABLE_WAY, new String[] {
                        wayID,
                        nd,
                },
                nd + " = " + nid + " AND " + wayID + " != " + wId, null, null, null, null, null );
        if (mCursor != null) {
            mCursor.moveToFirst();
        }//End if
        return Long.parseLong(mCursor.getString(0));
    }


}//End DBManager
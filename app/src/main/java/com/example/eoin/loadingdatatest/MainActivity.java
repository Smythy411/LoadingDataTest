package com.example.eoin.loadingdatatest;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.opencsv.CSVReader;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.MapQuestRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    //Map view
    MapView map;
    private GoogleApiClient mGoogleApiClient ;
    private LocationRequest mLocationRequest;

    private double routeLength;

    DBManager db;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);

        db = new DBManager(ctx);
        db.open();

        buildGoogleApiClient();

        /*File handling. Database is set up so for the moment it is unneeded.
        FileHandler fh = new FileHandler("UseCaseNodes.csv", "UseCaseWays.osm", this);
        fh.openCSVFile();
        fh.parseXml();
        */

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        //Enables zoom
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        ArrayList<GeoPoint> wayPoints = generateRoute(282735941);

        //In order to execute the network operations, this task can not be done on the main thread.
        //The waypoints are passed to this thread in order to map a route.
        new UpdateRoadTask().execute(wayPoints);

    }//End OnCreate()

    public ArrayList<GeoPoint> generateRoute(long startPoint) {
        //An ArrayList to hold the nodes of the route
        ArrayList<GeoPoint> wayPoints = new ArrayList<>();

        Log.d("StartPoint: ", String.valueOf(startPoint));
        String tempNode[] = db.getNode(startPoint);
        String tempEndNode[];

        GeoPoint startLocation = new GeoPoint(Double.parseDouble(tempNode[0]), Double.parseDouble(tempNode[1]));

        //Sets the inital zoom level and starting location
        IMapController mapController = map.getController();
        mapController.setZoom(17);
        mapController.setCenter(startLocation);

        //Simple marker for the starting node of the route
        Marker startMarker = new Marker(map);
        startMarker.setPosition(startLocation);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
        startMarker.setTitle("Start point");

        wayPoints.add(startLocation);

        int i = 0;
        Long wayId = db.getWayId(startPoint);
        while(i  <= 2) {
            Cursor mCursor = db.getWayById(wayId);
            if (mCursor != null) {
                mCursor.moveToFirst();
            }//End if

            long tempId= wayId;
            while (mCursor.moveToNext()) {
                tempNode = db.getNode(Long.parseLong(mCursor.getString(1)));
                GeoPoint tempPoint = new GeoPoint(Double.parseDouble(tempNode[0]), Double.parseDouble(tempNode[1]));
                wayPoints.add(tempPoint);
            }//end while
            if (tempId == wayId) {
                if (mCursor.moveToLast()) {
                    wayId = db.getNewWayId(Long.parseLong(mCursor.getString(1)), tempId);
                    Log.d("tempId: ", String.valueOf(tempId));
                }
            } else {
                wayId = tempId;
            }
            i++;
        }

        //End location
        GeoPoint endPoint = wayPoints.get(wayPoints.size() - 1) ;
        wayPoints.add(endPoint);

        //Marker for the end of the route
        Marker endMarker = new Marker(map);
        endMarker.setPosition(endPoint);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(endMarker);
        endMarker.setTitle("End point");

        map.invalidate();

        return wayPoints;
    }//End generateRoute()

    /**
     * Async task to get the road in a separate thread.
     * Credit to https://stackoverflow.com/questions/21213224/roadmanager-for-osmdroid-error
     */
    private class UpdateRoadTask extends AsyncTask<Object, Void, Road> {

        protected Road doInBackground(Object... params) {
            @SuppressWarnings("unchecked")
            ArrayList<GeoPoint> waypoints = (ArrayList<GeoPoint>)params[0];
            RoadManager roadManager = new MapQuestRoadManager("4EVl90eNYFzKk3Cp0RMvMLsyzC1PXYL1");


            return roadManager.getRoad(waypoints);
        }//End doInBackground()
        @Override
        protected void onPostExecute(Road result) {
            Road road = result;
            // showing distance and duration of the road
            Toast.makeText(MainActivity.this, "distance="+road.mLength, Toast.LENGTH_SHORT).show();
            Toast.makeText(MainActivity.this, "duration="+road.mDuration, Toast.LENGTH_SHORT).show();

            routeLength = road.mLength;

            if(road.mStatus != Road.STATUS_OK)
                Toast.makeText(MainActivity.this, "Error when loading the road - status="+road.mStatus, Toast.LENGTH_SHORT).show();
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road);

            map.getOverlay().clear();
            map.getOverlays().add(roadOverlay);
            map.invalidate();

            //Adding visible icons for each node in route
            //Being able to handle these nodes is very important.
            Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
            for (int i=0; i<road.mNodes.size(); i++){
                RoadNode node = road.mNodes.get(i);
                Marker nodeMarker = new Marker(map);
                nodeMarker.setPosition(node.mLocation);
                nodeMarker.setIcon(nodeIcon);
                nodeMarker.setTitle("Step "+i);
                nodeMarker.setSnippet(node.mInstructions);
                nodeMarker.setSubDescription(Road.getLengthDurationText(MainActivity.this, node.mLength, node.mDuration));
                map.getOverlays().add(nodeMarker);
            }//End for
        }//End onPostExecute()
    }//End UpdateRoadTask()

    protected void onStart() {

        //LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        //boolean enabled = service
          //      .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // check if enabled and if not send user to the GPS settings
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                //if (!enabled) {
                //Simple AlertBox to ask the user to enable their location.
                final AlertDialog.Builder enableLocation = new AlertDialog.Builder(MainActivity.this);
                enableLocation.setTitle("This application requires permission to access your location." +
                        "Would you like to enable your location?");

                //If user wishes to continue with their action
                enableLocation.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                MY_PERMISSIONS_REQUEST_LOCATION);
                    }//End onClick
                });// End Positive Button
                //If user does not wish to continue with their action
                enableLocation.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setResult(Activity.RESULT_CANCELED);
                    }//End onClick
                });//End Negative Button
                enableLocation.show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
        mGoogleApiClient.connect();
        super.onStart();
    }//End onStart()

    @Override

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }//End onStop()

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }//End onResume()

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }//End builfGoogleApiClient()

    @Override
    public void onConnected(Bundle connectionHint) {
        try {

            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(1000); // Update location every second
            //FusedLocationApi is deprecated, however it is recommended
            //To keep using it for the time being by the documentation.

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);

            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            if (mLastLocation != null) {
                Log.i(TAG, "In OnConnected and notNull" );
                double mLatitude = mLastLocation.getLatitude();
                double mLongitude = mLastLocation.getLongitude();

                TextView textViewCurrentLocation = (TextView) findViewById(R.id.textViewCurrentLocation);
                textViewCurrentLocation.setText("Location: " + String.valueOf(mLatitude) + " " + String.valueOf(mLongitude));
            }//End if
        } catch (SecurityException e) {
            Log.i(TAG, "Error creating location service: " + e.getMessage() );
        }//End try catch
    }//End onConnected()

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }//End onConnectionSuspended()

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }//End if
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        /*
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
        */
    }//End onConnectionFailed()

    public void onLocationChanged(Location location) {
        //TextView textViewCurrentLocation = (TextView) findViewById(R.id.textViewCurrentLocation);
        //textViewCurrentLocation.setText("Location received: " + location.toString());
    }//End onLocationChanged()
}//End MainActivity
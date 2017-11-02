package com.example.eoin.loadingdatatest;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

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

import java.util.ArrayList;

import static android.content.ContentValues.TAG;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    //Map view
    MapView map;
    private GoogleApiClient mGoogleApiClient ;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);

        buildGoogleApiClient();

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        //Enables zoom
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        //Sets the inital zoom level and starting location
        IMapController mapController = map.getController();
        mapController.setZoom(17);
        GeoPoint myAddress = new GeoPoint(53.2795432, -6.3469185);
        mapController.setCenter(myAddress);

        //Simple marker for the starting node of the route
        Marker startMarker = new Marker(map);
        startMarker.setPosition(myAddress);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
        startMarker.setTitle("Start point");

        //An ArrayList to hold the nodes of the route
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(myAddress);
        //End location
        GeoPoint endPoint = new GeoPoint(53.2851255, -6.3374528);
        waypoints.add(endPoint);

        //Marker for the end of the route
        Marker endMarker = new Marker(map);
        endMarker.setPosition(endPoint);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(endMarker);
        endMarker.setTitle("End point");

        map.invalidate();

        //In order to execute the network operations, this task can not be done on the main thread.
        //The waypoints are passed to this thread in order to map a route.
        new UpdateRoadTask().execute(waypoints);

        TextView textViewCurrentLocation = (TextView) findViewById(R.id.textViewCurrentLocation);

    }//End OnCreate()

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

            if(road.mStatus != Road.STATUS_OK)
                Toast.makeText(MainActivity.this, "Error when loading the road - status="+road.mStatus, Toast.LENGTH_SHORT).show();
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road);

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

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                double mLatitude = mLastLocation.getLatitude();
                double mLongitude = mLastLocation.getLongitude();
            }
        } catch (SecurityException e) {
            Log.i(TAG, "Error creating location service: " + e.getMessage() );
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
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
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }//End onResume()
}//End MainActivity

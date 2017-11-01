package com.example.eoin.loadingdatatest;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;


public class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);

        //Map view
        MapView map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        //Enables zoom
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        //Sets the inital zoom level and starting location
        IMapController mapController = map.getController();
        mapController.setZoom(17);
        GeoPoint myAddress = new GeoPoint(53.279635, -6.346731);
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

        /*
        //Constructing the route
        RoadManager roadManager = new OSRMRoadManager(this);
        Road road = roadManager.getRoad(waypoints);
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);

        map.getOverlays().add(roadOverlay);
        */

        TextView textViewCurrentLocation = (TextView) findViewById(R.id.textViewCurrentLocation);

    }

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }
}

package com.sueselbeck.heredemo;

import android.app.Activity;
import android.os.Bundle;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.IconCategory;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapContainer;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapLabeledMarker;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.search.Category;
import com.here.android.mpa.search.CategoryFilter;
import com.here.android.mpa.search.DiscoveryResultPage;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.ExploreRequest;
import com.here.android.mpa.search.PlaceLink;
import com.here.android.mpa.search.ResultListener;

import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class BasicMapActivity extends Activity {

    // map embedded in the map fragment
    private Map map = null;

    // map fragment embedded in this activity
    private MapFragment mapFragment = null;
    private MapContainer placesContainer = null;

    private PositioningManager posManager;
    private boolean paused;

    private MapMarker selectedMapMarker = null;
    private MapRoute mapRoute = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapFragment = (MapFragment)getFragmentManager().findFragmentById(
                R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    mapFragment.getMapGesture().addOnGestureListener(listener);
                    onMapFragmentInitializationCompleted();
                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment: " + error);
                }
            }
        });
    }

    // Resume positioning listener on wake up
    public void onResume() {
        super.onResume();
        paused = false;
        if (posManager != null) {
            posManager.start(
                    PositioningManager.LocationMethod.GPS_NETWORK);
        }
    }

    // To pause positioning listener
    public void onPause() {
        if (posManager != null) {
            posManager.stop();
        }
        super.onPause();
        paused = true;
    }

    // To remove the positioning listener
    public void onDestroy() {
        if (posManager != null) {
            // Cleanup
            posManager.removeListener(
                    positionListener);
        }
        map = null;
        super.onDestroy();
    }

    private void onMapFragmentInitializationCompleted() {
        // retrieve a reference of the map from the map fragment
        map = mapFragment.getMap();

        // start the position manager
        posManager = PositioningManager.getInstance();
        PositioningManager.getInstance().addListener(new WeakReference<>(positionListener));
        posManager.start(PositioningManager.LocationMethod.GPS_NETWORK);

        placesContainer = new MapContainer();
        map.addMapObject(placesContainer);

        // Display position indicator
        map.getPositionIndicator().setVisible(true);

        // Set the map center coordinate to the current position
        map.setCenter(posManager.getPosition().getCoordinate(),
                Map.Animation.NONE);
        map.setZoomLevel(14);
    }

    // Create a gesture listener and add it to the MapFragment
    MapGesture.OnGestureListener listener =
            new MapGesture.OnGestureListener.OnGestureListenerAdapter() {
                @Override
                public boolean onMapObjectsSelected(List<ViewObject> objects) {
                    // There are various types of map objects, but we only want
                    // to handle the MapMarkers we have added
                    for (ViewObject viewObj : objects) {
                        if (viewObj.getBaseType() == ViewObject.Type.USER_OBJECT) {
                            if (((MapObject)viewObj).getType() == MapObject.Type.MARKER) {

                                selectedMapMarker = ((MapMarker) viewObj);

                                // Create the RoutePlan and add two waypoints
                                RoutePlan routePlan = new RoutePlan();
                                // Use our current position as the first waypoint
                                routePlan.addWaypoint(
                                        new GeoCoordinate(posManager.getPosition().getCoordinate()));
                                // Use the marker's position as the second waypoint
                                routePlan.addWaypoint(
                                        new GeoCoordinate(((MapMarker) viewObj).getCoordinate()));

                                // Create RouteOptions and set to fastest & pedestrian mode
                                RouteOptions routeOptions = new RouteOptions();
                                routeOptions.setTransportMode(RouteOptions.TransportMode.PEDESTRIAN);
                                routeOptions.setRouteType(RouteOptions.Type.FASTEST);
                                routePlan.setRouteOptions(routeOptions);

                                // Create a RouteManager and calculate the route
                                RouteManager rm = new RouteManager();
                                rm.calculateRoute(routePlan, new RouteListener());

                                // Remove all other markers from the map
                                for (MapObject mapObject : placesContainer.getAllMapObjects()) {
                                    if (!mapObject.equals(viewObj)) {
                                        placesContainer.removeMapObject(mapObject);
                                    }
                                }

                                // If user has tapped multiple markers, just display one route
                                break;
                            }
                        }
                    }
                    // return false to allow the map to handle this callback also
                    return false;
                }
            };

    private class RouteListener implements RouteManager.Listener {

        public void onProgress(int percentage) {
            // You can use this to display the progress of the route calculation
        }

        public void onCalculateRouteFinished(RouteManager.Error error, List<RouteResult> routeResult) {
            // If the route was calculated successfully
            if (error == RouteManager.Error.NONE) {
                // Render the route on the map
                mapRoute = new MapRoute(routeResult.get(0).getRoute());
                map.addMapObject(mapRoute);
                int routeLength = routeResult.get(0).getRoute().getLength();
                float steps = 2 * (routeLength / 0.8f);
                String title = selectedMapMarker.getTitle();
                title = String.format(Locale.ENGLISH, "It's %d steps to %s and back!",
                        ((int)steps), title);
                selectedMapMarker.setTitle(title);
                selectedMapMarker.showInfoBubble();
            }
            else {
                // Display a message indicating route calculation failure
            }
        }
    }

    // Define positioning listener
    private PositioningManager.OnPositionChangedListener positionListener = new
            PositioningManager.OnPositionChangedListener() {

                public void onPositionUpdated(PositioningManager.LocationMethod method,
                                              GeoPosition position, boolean isMapMatched) {
                    // set the center only when the app is in the foreground
                    // to reduce CPU consumption
                    if (!paused) {
                        map.setCenter(position.getCoordinate(),
                                Map.Animation.LINEAR);
                    }
                }

                public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                 PositioningManager.LocationStatus status) {
                }
            };

    public void findPlaces(View view) {

        // clear the map
        if (mapRoute != null) {
            map.removeMapObject(mapRoute);
            mapRoute = null;
        }
        placesContainer.removeAllMapObjects();

        // collect data to set options
        GeoCoordinate myLocation = posManager.getPosition().getCoordinate();
        EditText editSteps = (EditText)findViewById(R.id.editSteps);
        int noOfSteps;
        try {
            noOfSteps = Integer.valueOf(editSteps.getText().toString());
        } catch (NumberFormatException e) {
            // if input is not a number set to default of 2500
            noOfSteps = 2500;
        }
        float range = ((noOfSteps * 0.762f) / 2);

        // create an exploreRequest and set options
        ExploreRequest request = new ExploreRequest();
        request.setSearchArea(myLocation, (int)range);
        request.setCollectionSize(50);
        request.setCategoryFilter(new CategoryFilter().add(Category.Global.SIGHTS_MUSEUMS));

        try {
            ErrorCode error = request.execute(new SearchRequestListener());
            if( error != ErrorCode.NONE ) {
                // Handle request error
            }
        } catch (IllegalArgumentException ex) {
            // Handle invalid create search request parameters
        }
    }

    // search request listener
    class SearchRequestListener implements ResultListener<DiscoveryResultPage> {

        @Override
        public void onCompleted(DiscoveryResultPage data, ErrorCode error) {
            if (error != ErrorCode.NONE) {
                // Handle error
            } else {
                // results can be of different types
                // we are only interested in PlaceLinks
                List<PlaceLink> results = data.getPlaceLinks();
                if (results.size() > 0) {
                    for (PlaceLink result : results) {

                        EditText editSteps = (EditText)findViewById(R.id.editSteps);
                        //TODO: make the result a global variable
                        int noOfSteps;
                        try {
                            noOfSteps = Integer.valueOf(editSteps.getText().toString());
                        } catch (NumberFormatException e) {
                            // if input is not a number set to default of 2500
                            noOfSteps = 2500;
                        }
                        float range = ((noOfSteps * 0.762f) / 2);

                        // get all results that are far away enough to be a good candidate
                        if (result.getDistance() < range && result.getDistance() > (range * 0.7f)) {
                            GeoCoordinate c = result.getPosition();
                            MapLabeledMarker labeledMarker = new MapLabeledMarker(c);
                            labeledMarker.setIcon(IconCategory.SPORT_OUTDOOR);
                            labeledMarker.setLabelText("eng", result.getTitle());

                            com.here.android.mpa.common.Image img =
                                    new com.here.android.mpa.common.Image();
                            try {
                                img.setImageAsset("pin.png");
                            } catch (IOException e) {
                                // handle exception
                            }
                            //MapMarker marker = new MapMarker(c, img);
                            MapMarker marker = new MapMarker();
                            marker.setCoordinate(c);
                            marker.setTitle(result.getTitle());

                        placesContainer.addMapObject(marker);
                    }
                }

            } else {
                    // handle empty result case
                }
            }
        }
    }

}


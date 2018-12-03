package aot.cs491.com.aot_ar;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.shawnlin.numberpicker.NumberPicker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import aot.cs491.com.aot_ar.aothttpapi.AOTNode;
import aot.cs491.com.aot_ar.aothttpapi.AOTObservation;
import aot.cs491.com.aot_ar.aothttpapi.AOTSensorType;
import aot.cs491.com.aot_ar.aothttpapi.AOTService;
import aot.cs491.com.aot_ar.utils.DisposablesManager;
import aot.cs491.com.aot_ar.utils.Utils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.LocationNode;
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, DatePickerDialog.OnDateSetListener, NumberPicker.OnValueChangeListener, NumberPicker.OnScrollListener
{

    private FusedLocationProviderClient mFusedLocationClient;

    String menuOptionSelected;
    double longitude;
    double latitude;
    int distance;
    Date apiStartDate;
    Date apiEndDate;
    Date filterStartDate;
    Date filterEndDate;
    boolean useImperialUnits;

    CoordinatorLayout coordinatorLayout;
    Button dateButton;
    DatePickerDialog datePickerDialog;
    NumberPicker timePicker;
    FloatingActionButton refreshButton;

    private boolean isInitial = true;
    private boolean isTimePickerScrolling = false;
    private boolean installRequested;
    private boolean hasFinishedLoading = false;
    private boolean markersAdded=false;

    private Snackbar loadingMessageSnackbar = null;
    private Snackbar progressViewSnackbar;
    private ArSceneView arSceneView;

    // Our ARCore-Location scene
    private LocationScene locationScene;
    public static final String TAG = MainActivity.class.getSimpleName();

    List <AOTNode> nodes = null;
    List <CompletableFuture<ViewRenderable>> exampleLayouts;
    List <ViewRenderable> exampleLayoutRenderables;
    List <LocationMarker> locationMarkers;
    List <LocationMarkerCustom> locationMarkersCustom;

    public void initializeAndCallAPI()
    {
        showProgressView("Finding nearby nodes ...");

        AtomicBoolean everythingIsDone = new AtomicBoolean(false);
        AtomicBoolean handlerCalled = new AtomicBoolean(false);

        DisposablesManager.add(
                AOTService.fetchObservationsFromNearbyNodes(longitude, latitude, distance, apiStartDate, apiEndDate)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(disposable -> Log.i(TAG, "Fetching nearby nodes..."))
                        .subscribe(node -> {
                                    Log.i(TAG, "node emitted: " + node.toString());
                                    nodes.add(node);

                                    exampleLayouts.add(
                                            ViewRenderable.builder()
                                                    .setView(this,R.layout.outer_layout)
                                                    .build());

                                    if (node.getObservations() != null) {
                                        Log.i(TAG, "node has: " + node.getObservations().size() + " observations from " + node.getSensors().size() + " sensors");

                                        if (!node.getObservations().isEmpty()) {
                                            filterAndAggregateObservations(node, aotNode -> {
                                                if(everythingIsDone.get() && !handlerCalled.get()) {
                                                    handlerCalled.set(true);
                                                    handleCompleteableFutures();
                                                }
                                                return null;
                                            });
                                        }
                                    }
                                },
                                throwable -> {
                                    Log.e(TAG, "Error while fetching nearby nodes:", throwable);
                                    hideProgressView();
                                },
                                () -> {
                                    Log.i(TAG, "Finished fetching nearby nodes");
                                    everythingIsDone.set(true);
                                }
                        )
        );
    }

    private void filterAndAggregateObservations(AOTNode node, Function<AOTNode, Void> onComplete) {
        List<Observable<List<AOTObservation>>> filterCalls = new ArrayList<>();
        for(AOTSensorType aSensorType: AOTSensorType.values()) {
            filterCalls.add(AOTService.filterObservations(node.getObservations(), aSensorType, filterStartDate, filterEndDate).toObservable());
        }
        DisposablesManager.add(
                Observable.fromIterable(filterCalls)
                        .flatMap(listSingle -> listSingle)
                        .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "avg").toObservable())
                        .doOnSubscribe(disposable -> node.getAggregatedObservations().clear())
                        .subscribe(aotObservation -> {
                                    if(aotObservation.getSensorPath() != null) {
                                        node.getAggregatedObservations().put(aotObservation.getSensorType(), aotObservation);
                                    }
                                    else {
                                        node.getAggregatedObservations().put(aotObservation.getSensorType(), null);
                                    }
                                },
                                throwable -> Log.e(TAG, "Error while filtering/aggregating observations:", throwable),
                                () -> onComplete.apply(node)
                        )
        );
    }

    public void distanceRefreshed()
    {
        showProgressView("Determining your location ...");
        nodes = new ArrayList<>();
        exampleLayouts = new ArrayList<>();
        exampleLayoutRenderables = new ArrayList<ViewRenderable>();
        locationMarkers = new ArrayList<LocationMarker>();
        locationMarkersCustom = new ArrayList<LocationMarkerCustom>();
        locationScene=null;
        hasFinishedLoading = false;
        markersAdded = false;


        try {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                                latitude=location.getLatitude();
                                longitude=location.getLongitude();
                            }
                            else
                            {
                                longitude = -87.662111;
                                latitude = 41.871629;
                            }
                            initializeAndCallAPI();
                        }
                    });
        }
        catch(SecurityException se)
        {
            Toast.makeText(
                    this, "Location permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Revert to default theme which was changed for showing launch screen
        setTheme(R.style.AppTheme_NoActionBar);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arSceneView = findViewById(R.id.ar_scene_view);
        menuOptionSelected="weather";

        coordinatorLayout = findViewById(R.id.coordinator);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> refresh());

        // Initialize date picker
        Calendar calendar = Calendar.getInstance();

        datePickerDialog = new DatePickerDialog(this);
        datePickerDialog.setOnDateSetListener(this);
        datePickerDialog.getDatePicker().setMinDate(Utils.stringToLocalDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).getTime());
        dateButton = findViewById(R.id.dateButton);
        dateButton.setOnClickListener(v -> {
            if(markersAdded) {
                datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
                datePickerDialog.show();
            }
            else {
                refresh();
            }
        });
        timePicker = findViewById(R.id.timePicker);
        timePicker.setOnValueChangedListener(this);
        timePicker.setOnScrollListener(this);

        // Set default date and time
        onDateSet(datePickerDialog.getDatePicker(), calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        setTime(calendar.get(Calendar.HOUR_OF_DAY), false);
        timePicker.setValue(calendar.get(Calendar.HOUR_OF_DAY));
        isInitial = false;

        distance = PreferenceManager.getDefaultSharedPreferences(this).getInt(getResources().getString(R.string.settings_key_distanceThreshold), 2000);
        useImperialUnits = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.settings_key_useImperialUnits), false);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {
                            if (!hasFinishedLoading) {
                                return;
                            }

                            if (locationScene == null) {
                                // If our locationScene object hasn't been setup yet, this is a good time to do it
                                // We know that here, the AR components have been initiated.
                                locationScene = new LocationScene(this, this, arSceneView);

                                // Now lets create our location markers.
                                // First, a layout
                                for (int i=0; i<nodes.size(); i++)
                                {
//                                    locationMarkersCustom.add(createLocationMarkerCustom(nodes.get(i).getLocation().lon(),
//                                            nodes.get(i).getLocation().lat(), exampleLayoutRenderables.get(i),nodes.get(i)));
                                    locationMarkers.add(createLocationMarker(nodes.get(i).getLocation().lon(),
                                            nodes.get(i).getLocation().lat(), i));
                                }

                                for(int i=0; i<nodes.size(); i++)
                                {
                                    setRenderEvent(locationMarkers.get(i), exampleLayoutRenderables.get(i), nodes.get(i),i);
                                    setInnerLayoutValues(exampleLayoutRenderables.get(i), nodes.get(i),i);

                                }

                                // Adding the marker
                                for(int i=0; i< nodes.size();i++) {
                                    locationScene.mLocationMarkers.add(locationMarkers.get(i));

                                }
                                markersAdded=true;
                                hideProgressView();
                            }

                            Frame frame = arSceneView.getArFrame();
                            if (frame == null) {
                                return;
                            }

                            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                                return;
                            }

                            if (locationScene != null) {
                                locationScene.processFrame(frame);
                            }

//                            if (loadingMessageSnackbar != null) {
//                                for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
//                                    if (plane.getTrackingState() == TrackingState.TRACKING) {
//                                        hideLoadingMessage();
//                                    }
//                                }
//                            }
                        });


        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.
        ARLocationPermissionHelper.requestPermission(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        for(int i=0; i<nodes.size();i++)
        {
            View eView = exampleLayoutRenderables.get(i).getView();
            View vgraph= eView.findViewById(R.id.graph_layout_id);
            vgraph.setVisibility(LinearLayout.GONE);
            eView.findViewById(R.id.aggregate_layout_id).setVisibility(LinearLayout.GONE);
        }
        if (id == R.id.nav_light) {
            menuOptionSelected="light";
            if(markersAdded)
            {
                setInnerLayoutValuesFromMenu();
            }

        } else if (id == R.id.nav_pollution) {
            menuOptionSelected="airquality";
            if(markersAdded)
            {
                setInnerLayoutValuesFromMenu();
            }

        } else if (id == R.id.nav_weather) {
            menuOptionSelected="weather";
            if(markersAdded)
            {
                setInnerLayoutValuesFromMenu();
            }

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private Node getExampleView(int i) {
        Node base = new Node();
        base.setRenderable(exampleLayoutRenderables.get(i));
        Context c = this;
        // Add  listeners etc here
        View eView = exampleLayoutRenderables.get(i).getView();
        TextView textViewData1 = eView.findViewById(R.id.textView_temp);
        TextView textViewData2 = eView.findViewById(R.id.textView_pres);
        TextView textViewData3 = eView.findViewById(R.id.textView_hum);
        TextView textViewDistance = eView.findViewById(R.id.textView_dist1);
        //View compLayout = findViewById(R.id.comp_layout_id);
        //TextView compLayoutText1 = compLayout.findViewById(R.id.comptext1);


        eView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // TODO Auto-generated method stub
                //compLayout.setVisibility(LinearLayout.VISIBLE);
//                for(int i=0; i<nodes.size();i++)
//                {
//                    View eView = exampleLayoutRenderables.get(i).getView();
//                    View vgraph= eView.findViewById(R.id.graph_layout_id);
//                    vgraph.setVisibility(LinearLayout.GONE);
//
//                }
                return false;
            }
        });

        textViewDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout l = (LinearLayout) v.getParent().getParent().getParent().getParent();
                l.findViewById(R.id.graph_layout_id).setVisibility(LinearLayout.GONE);
                l.findViewById(R.id.aggregate_layout_id).setVisibility(LinearLayout.GONE);
            }
        });

        textViewData1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout l=(LinearLayout) v.getParent().getParent().getParent().getParent().getParent();
                l.findViewById(R.id.graph_layout_id).setVisibility(LinearLayout.VISIBLE);
                l.findViewById(R.id.aggregate_layout_id).setVisibility(LinearLayout.VISIBLE);

                TextView min =l.findViewById(R.id.textViewMinVal);
                TextView med =l.findViewById(R.id.textViewMedVal);
                TextView max =l.findViewById(R.id.textViewMaxVal);
                TextView units =l.findViewById(R.id.textViewArrgegateUnits);
                min.setText("No Data");
                med.setText("No Data");
                max.setText("No Data");
                units.setText("");

                AOTSensorType sensorType=AOTSensorType.TEMPERATURE;
                if(menuOptionSelected=="weather") {
                     sensorType = AOTSensorType.TEMPERATURE;
                    units.setText(sensorType.getUnit(useImperialUnits));
                }
                else if(menuOptionSelected=="light")
                {
                     sensorType = AOTSensorType.LIGHT_INTENSITY;
                    units.setText(sensorType.getUnit(useImperialUnits));
                }
                else if(menuOptionSelected=="airquality")
                {
                    sensorType = AOTSensorType.CARBON_MONOXIDE;
                    units.setText(sensorType.getUnit(useImperialUnits));
                }


                    DisposablesManager.add(
                            AOTService.filterObservations(nodes.get(i).getObservations(), sensorType, apiStartDate, apiEndDate)
                                    .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "min"))
                                    .subscribe(aotObservation -> {

                                        if (aotObservation != null && aotObservation.getSensorPath() != null) {
                                            min.setText(aotObservation.getValue(useImperialUnits).toString());
                                        }
                                    })
                    );

                    DisposablesManager.add(
                            AOTService.filterObservations(nodes.get(i).getObservations(), sensorType, apiStartDate, apiEndDate)
                                    .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "median"))
                                    .subscribe(aotObservation -> {
                                        if (aotObservation != null && aotObservation.getSensorPath() != null) {
                                            med.setText(aotObservation.getValue(useImperialUnits).toString());
                                        }
                                    })
                    );
                    DisposablesManager.add(
                            AOTService.filterObservations(nodes.get(i).getObservations(), sensorType, apiStartDate, apiEndDate)
                                    .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "max"))
                                    .subscribe(aotObservation -> {
                                        if (aotObservation != null && aotObservation.getSensorPath() != null) {
                                            max.setText(aotObservation.getValue(useImperialUnits).toString());
                                        }
                                    })
                    );


                GraphView graph = (GraphView) l.findViewById(R.id.graph1);
                graph.removeAllSeries();
                ArrayList <DataPoint> dataPointsList = new ArrayList<DataPoint>();
                for (AOTObservation a:nodes.get(i).getObservations())
                {
                    if(menuOptionSelected.equals("weather")) {
                        if (a.getSensorType().equals(AOTSensorType.TEMPERATURE)) {
                            dataPointsList.add(new DataPoint(a.getTimestamp(), a.getValue(useImperialUnits)));
                        }
                    }
                    else if (menuOptionSelected.equals("light"))
                    {
                        if (a.getSensorType().equals(AOTSensorType.LIGHT_INTENSITY)) {
                            dataPointsList.add(new DataPoint(a.getTimestamp(), a.getValue(useImperialUnits)));
                        }
                    }
                    else if (menuOptionSelected.equals("airquality"))
                    {
                        if (a.getSensorType().equals(AOTSensorType.CARBON_MONOXIDE)) {
                            dataPointsList.add(new DataPoint(a.getTimestamp(), a.getValue(useImperialUnits)));
                        }
                    }

                }
                DataPoint da[]=  dataPointsList.toArray(new DataPoint[dataPointsList.size()]);
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(da);
                graph.addSeries(series);
                styleGraph(graph, series);
            }
        });

        textViewData2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout l=(LinearLayout) v.getParent().getParent().getParent().getParent().getParent();
                l.findViewById(R.id.graph_layout_id).setVisibility(LinearLayout.VISIBLE);
                l.findViewById(R.id.aggregate_layout_id).setVisibility(LinearLayout.VISIBLE);

                TextView min =l.findViewById(R.id.textViewMinVal);
                TextView med =l.findViewById(R.id.textViewMedVal);
                TextView max =l.findViewById(R.id.textViewMaxVal);
                TextView units =l.findViewById(R.id.textViewArrgegateUnits);
                min.setText("No Data");
                med.setText("No Data");
                max.setText("No Data");
                units.setText("");

                AOTSensorType sensorType=AOTSensorType.PRESSURE;
                if(menuOptionSelected=="weather") {
                    sensorType = AOTSensorType.PRESSURE;
                    units.setText(sensorType.getUnit(useImperialUnits));
                }
                else if(menuOptionSelected=="light")
                {
                    sensorType = AOTSensorType.INFRA_RED_LIGHT;
                    units.setText(sensorType.getUnit(useImperialUnits));
                }
                else if(menuOptionSelected=="airquality")
                {
                    sensorType = AOTSensorType.SULPHUR_DIOXIDE;
                    units.setText(sensorType.getUnit(useImperialUnits));
                }


                DisposablesManager.add(
                        AOTService.filterObservations(nodes.get(i).getObservations(), sensorType, apiStartDate, apiEndDate)
                                .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "min"))
                                .subscribe(aotObservation -> {

                                    if (aotObservation != null && aotObservation.getSensorPath() != null) {
                                        min.setText(aotObservation.getValue(useImperialUnits).toString());
                                    }
                                })
                );

                DisposablesManager.add(
                        AOTService.filterObservations(nodes.get(i).getObservations(), sensorType, apiStartDate, apiEndDate)
                                .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "median"))
                                .subscribe(aotObservation -> {
                                    if (aotObservation != null && aotObservation.getSensorPath() != null) {
                                        med.setText(aotObservation.getValue(useImperialUnits).toString());
                                    }
                                })
                );
                DisposablesManager.add(
                        AOTService.filterObservations(nodes.get(i).getObservations(), sensorType, apiStartDate, apiEndDate)
                                .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "max"))
                                .subscribe(aotObservation -> {
                                    if (aotObservation != null && aotObservation.getSensorPath() != null) {
                                        max.setText(aotObservation.getValue(useImperialUnits).toString());
                                    }
                                })
                );

                GraphView graph = (GraphView) l.findViewById(R.id.graph1);
                graph.removeAllSeries();
                ArrayList <DataPoint> dataPointsList = new ArrayList<DataPoint>();
                for (AOTObservation a:nodes.get(i).getObservations())
                {
                    if(menuOptionSelected.equals("weather")) {
                        if (a.getSensorType().equals(AOTSensorType.PRESSURE)) {
                            dataPointsList.add(new DataPoint(a.getTimestamp(), a.getValue(useImperialUnits)));
                        }
                    }
                    else if (menuOptionSelected.equals("light"))
                    {
                        if (a.getSensorType().equals(AOTSensorType.INFRA_RED_LIGHT)) {
                            dataPointsList.add(new DataPoint(a.getTimestamp(), a.getValue(useImperialUnits)));
                        }
                    }
                    else if (menuOptionSelected.equals("airquality"))
                    {
                        if (a.getSensorType().equals(AOTSensorType.SULPHUR_DIOXIDE)) {
                            dataPointsList.add(new DataPoint(a.getTimestamp(), a.getValue(useImperialUnits)));
                        }
                    }

                }
                DataPoint da[]=  dataPointsList.toArray(new DataPoint[dataPointsList.size()]);
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(da);
                graph.addSeries(series);
                styleGraph(graph, series);
            }
        });

        textViewData3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout l=(LinearLayout) v.getParent().getParent().getParent().getParent().getParent();
//                if (l.findViewById(R.id.graph_layout_id).getVisibility() == View.GONE) {
                    l.findViewById(R.id.graph_layout_id).setVisibility(LinearLayout.VISIBLE);
                    l.findViewById(R.id.aggregate_layout_id).setVisibility(LinearLayout.VISIBLE);
//                } else if (l.findViewById(R.id.graph_layout_id).getVisibility() == View.VISIBLE){
//                    l.findViewById(R.id.graph_layout_id).setVisibility(LinearLayout.GONE);
//                }

                TextView min =l.findViewById(R.id.textViewMinVal);
                TextView med =l.findViewById(R.id.textViewMedVal);
                TextView max =l.findViewById(R.id.textViewMaxVal);
                TextView units =l.findViewById(R.id.textViewArrgegateUnits);

                min.setText("No Data");
                med.setText("No Data");
                max.setText("No Data");
                units.setText("");

                AOTSensorType sensorType=AOTSensorType.HUMIDITY;
                if(menuOptionSelected=="weather") {
                    sensorType = AOTSensorType.HUMIDITY;
                    units.setText(sensorType.getUnit(useImperialUnits));
                }
                else if(menuOptionSelected=="light")
                {
                    sensorType = AOTSensorType.ULTRA_VIOLET_LIGHT;
                    units.setText(sensorType.getUnit(useImperialUnits));
                }
                else if(menuOptionSelected=="airquality")
                {
                    sensorType = AOTSensorType.NITROGEN_DIOXIDE;
                    units.setText(sensorType.getUnit(useImperialUnits));
                }


                DisposablesManager.add(
                        AOTService.filterObservations(nodes.get(i).getObservations(), sensorType, apiStartDate, apiEndDate)
                                .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "min"))
                                .subscribe(aotObservation -> {

                                    if (aotObservation != null && aotObservation.getSensorPath() != null) {
                                        min.setText(aotObservation.getValue(useImperialUnits).toString());
                                    }
                                })
                );

                DisposablesManager.add(
                        AOTService.filterObservations(nodes.get(i).getObservations(), sensorType, apiStartDate, apiEndDate)
                                .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "median"))
                                .subscribe(aotObservation -> {
                                    if (aotObservation != null && aotObservation.getSensorPath() != null) {
                                        med.setText(aotObservation.getValue(useImperialUnits).toString());
                                    }
                                })
                );
                DisposablesManager.add(
                        AOTService.filterObservations(nodes.get(i).getObservations(), sensorType, apiStartDate, apiEndDate)
                                .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "max"))
                                .subscribe(aotObservation -> {
                                    if (aotObservation != null && aotObservation.getSensorPath() != null) {
                                        max.setText(aotObservation.getValue(useImperialUnits).toString());
                                    }
                                })
                );

                GraphView graph = (GraphView) l.findViewById(R.id.graph1);
                graph.removeAllSeries();
                ArrayList <DataPoint> dataPointsList = new ArrayList<DataPoint>();
                for (AOTObservation a:nodes.get(i).getObservations())
                {
                    if(menuOptionSelected.equals("weather")) {
                        if (a.getSensorType().equals(AOTSensorType.HUMIDITY)) {
                            dataPointsList.add(new DataPoint(a.getTimestamp(), a.getValue(useImperialUnits)));
                        }
                    }
                    else if (menuOptionSelected.equals("light"))
                    {
                        if (a.getSensorType().equals(AOTSensorType.ULTRA_VIOLET_LIGHT)) {
                            dataPointsList.add(new DataPoint(a.getTimestamp(), a.getValue(useImperialUnits)));
                        }
                    }
                    else if (menuOptionSelected.equals("airquality"))
                    {
                        if (a.getSensorType().equals(AOTSensorType.NITROGEN_DIOXIDE)) {
                            dataPointsList.add(new DataPoint(a.getTimestamp(), a.getValue(useImperialUnits)));
                        }
                    }

                }
                DataPoint da[]=  dataPointsList.toArray(new DataPoint[dataPointsList.size()]);
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(da);
                graph.addSeries(series);
                styleGraph(graph, series);
            }
        });


        return base;
    }

    private void styleGraph(GraphView graph, LineGraphSeries series) {
        graph.setTitle("TREND");
        graph.setTitleColor(getColor(R.color.colorAccentDark));
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(MainActivity.this));
        graph.getGridLabelRenderer().setNumHorizontalLabels(4);
        graph.getViewport().setMinX(apiStartDate.getTime());
        graph.getViewport().setMaxX(apiEndDate.getTime());
        graph.getViewport().setXAxisBoundsManual(true);

        if(series != null) {
            series.setColor(getColor(R.color.colorAccent));
        }
    }

    /**
     * Make sure we call locationScene.resume();
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                DemoUtils.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            DemoUtils.displayError(this, "Unable to get camera", ex);
            finish();
            return;
        }

        Integer newDistance = PreferenceManager.getDefaultSharedPreferences(this).getInt(getResources().getString(R.string.settings_key_distanceThreshold), 2000);
        boolean distanceChanged = newDistance != distance;

        Boolean newUseImperialUnits = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.settings_key_useImperialUnits), false);
        boolean unitsChanged = newUseImperialUnits != useImperialUnits;

        distance = newDistance;
        useImperialUnits = newUseImperialUnits;

        if (distanceChanged) {
            refresh();
        } else if (unitsChanged) {
            setTime(timePicker.getValue(), true);
        }
    }

    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    public void onPause() {
        super.onPause();

        if (locationScene != null) {
            locationScene.pause();
        }

        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
        else
        {
            distanceRefreshed();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    // View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    //| View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void showLoadingMessage() {
        if (loadingMessageSnackbar != null && loadingMessageSnackbar.isShownOrQueued()) {
            return;
        }

        loadingMessageSnackbar =
                Snackbar.make(
                        coordinatorLayout,
                        R.string.plane_finding,
                        Snackbar.LENGTH_INDEFINITE);
        loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        loadingMessageSnackbar.show();
    }

    private void hideLoadingMessage() {
        if (loadingMessageSnackbar == null) {
            return;
        }

        loadingMessageSnackbar.dismiss();
        loadingMessageSnackbar = null;
    }

    private void handleCompleteableFutures()
    {
        showProgressView("Rendering nodes ...");
        CompletableFuture.allOf(exampleLayouts.toArray(new CompletableFuture[exampleLayouts.size()]))
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().

                            if (throwable != null) {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }

                            try {

                                for(int i=0; i< nodes.size(); i++) {
                                    exampleLayoutRenderables.add(exampleLayouts.get(i).get());
                                }

                                hasFinishedLoading = true;

                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }

                            return null;
                        });

    }


    public LocationMarker createLocationMarker(double longitude, double latitude, int i)
    {
        return new LocationMarker(
                longitude,
                latitude,
                getExampleView(i)
                //getExampleView1(vr)
        );
    }

    public void setRenderEvent(LocationMarker lm, ViewRenderable vr, AOTNode aotN, int i) {
        lm.setRenderEvent(new LocationNodeRender() {
            @Override
            public void render(LocationNode node) {
                View eView = vr.getView();
                TextView value2 = eView.findViewById(R.id.textView_dist1);
                Float distanceInKilometers = node.getDistance() / 1000f;
                Float distanceValue = useImperialUnits ? Utils.kilometersToMiles(distanceInKilometers) : distanceInKilometers;
                value2.setText(Utils.round(distanceValue).toString() + (useImperialUnits ? " mi" : " km"));

            }
        });
    }


    public void setRenderEventCustom(LocationMarkerCustom lm, ViewRenderable vr)
    {
        lm.setRenderEvent(new LocationNodeRender() {
            @Override
            public void render(LocationNode node) {
                //View eView = exampleLayoutRenderables.get(0).getView();
                View eView = vr.getView();
                TextView distanceTextView = eView.findViewById(R.id.textView_dist1);
                TextView nameView = eView.findViewById(R.id.textView_loc1);
                distanceTextView.setText(node.getDistance() + "M");
                nameView.setText(lm.getAotNode().getAddress());
            }
        });
    }


    public void setInnerLayoutValues(ViewRenderable vr, AOTNode aotN, int i)
    {
        View eView = vr.getView();
        TextView nameView = eView.findViewById(R.id.textView_loc1);
        TextView value1l = eView.findViewById(R.id.textView_T);
        TextView value3l = eView.findViewById(R.id.textView_P);
        TextView value4l = eView.findViewById(R.id.textView_H);

        TextView value1 = eView.findViewById(R.id.textView_temp);
        TextView value3 = eView.findViewById(R.id.textView_pres);
        TextView value4 = eView.findViewById(R.id.textView_hum);

        TextView value1u = eView.findViewById(R.id.textView_tempu);
        TextView value3u = eView.findViewById(R.id.textView_presu);
        TextView value4u = eView.findViewById(R.id.textView_humu);
        nameView.setText(aotN.getAddress());
        AOTObservation observation;
        if(menuOptionSelected=="weather") {

            value1l.setBackgroundResource(R.drawable.temperature);
            value1l.setText(null);
            value3l.setBackgroundResource(R.drawable.pressure);
            value3l.setText(null);
            value4l.setBackgroundResource(R.drawable.humidity);
            value4l.setText(null);

            observation = aotN.getAggregatedObservations().get(AOTSensorType.TEMPERATURE);
            if(observation !=null) {
                //value1.setText(observation.getValue(useImperialUnits).toString() + observation.getUnits(useImperialUnits));

                value1.setText(observation.getValue(useImperialUnits).toString());
                value1u.setText(observation.getUnits(useImperialUnits));
            }
            else {
                value1.setText("No Data");
                value1u.setText("");
            }

            observation = aotN.getAggregatedObservations().get(AOTSensorType.PRESSURE);
            if(observation !=null) {

                value3.setText(observation.getValue(useImperialUnits).toString());
                value3u.setText(observation.getUnits(useImperialUnits));
            }
            else {
                value3.setText("No Data");
                value3u.setText("");
            }

            observation = aotN.getAggregatedObservations().get(AOTSensorType.HUMIDITY);
            if(observation !=null) {
                value4.setText(observation.getValue(useImperialUnits).toString());
                value4u.setText(observation.getUnits(useImperialUnits));
            }
            else {
                value4.setText("No Data");
                value4u.setText("");
            }

        }
        if(menuOptionSelected=="light") {
            value1l.setBackgroundResource(0);
            value1l.setText("Light");
            value3l.setBackgroundResource(0);
            value3l.setText("IR");
            value4l.setBackgroundResource(0);
            value4l.setText("UV");

            observation = aotN.getAggregatedObservations().get(AOTSensorType.LIGHT_INTENSITY);
            if(observation !=null) {

                value1.setText(observation.getValue(useImperialUnits).toString());
                value1u.setText(observation.getUnits(useImperialUnits));
            }
            else {
                value1.setText("No Data");
                value1u.setText("");
            }

            observation = aotN.getAggregatedObservations().get(AOTSensorType.INFRA_RED_LIGHT);
            if(observation !=null) {
                value3.setText(observation.getValue(useImperialUnits).toString());
                value3u.setText(observation.getUnits(useImperialUnits));
            }
            else {
                value3.setText("No Data");
                value3u.setText("");
            }

            observation = aotN.getAggregatedObservations().get(AOTSensorType.ULTRA_VIOLET_LIGHT);
            if(observation !=null) {
                value4.setText(observation.getValue(useImperialUnits).toString());
                value4u.setText(observation.getUnits(useImperialUnits));
            }
            else {
                value4.setText("No Data");
                value4u.setText("");
            }

        }
        if(menuOptionSelected=="airquality") {

            value1l.setBackgroundResource(0);
            value1l.setText("CO");
            value3l.setBackgroundResource(0);
            value3l.setText("SO2");
            value4l.setBackgroundResource(0);
            value4l.setText("NO2");


            observation = aotN.getAggregatedObservations().get(AOTSensorType.CARBON_MONOXIDE);
            if(observation !=null) {
                value1.setText(observation.getValue(useImperialUnits).toString());
                value1u.setText(observation.getUnits(useImperialUnits));
            }
            else {
                value1.setText("No Data");
                value1u.setText("");
            }

            observation = aotN.getAggregatedObservations().get(AOTSensorType.SULPHUR_DIOXIDE);
            if(observation !=null) {
                value3.setText(observation.getValue(useImperialUnits).toString());
                value3u.setText(observation.getUnits(useImperialUnits));
            }
            else {
                value3.setText("No Data");
                value3u.setText("");
            }

            observation = aotN.getAggregatedObservations().get(AOTSensorType.NITROGEN_DIOXIDE);
            if(observation !=null) {
                value4.setText(observation.getValue(useImperialUnits).toString());
                value4u.setText(observation.getUnits(useImperialUnits));
            }
            else {
                value4.setText("No Data");
                value4u.setText("");
            }
        }
    }
    public void setInnerLayoutValuesFromMenu()
    {
        for(int i=0; i<nodes.size(); i++) {

            setInnerLayoutValues(exampleLayoutRenderables.get(i), nodes.get(i), i);
        }

    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        apiStartDate = Utils.stringToLocalDate(year, month + 1, dayOfMonth);
        apiEndDate = Utils.setTimeForLocalDate(23, 59, 59, apiStartDate);
        dateButton.setText(Utils.dateToString(apiStartDate, "EEE, MMM d, ''yy", TimeZone.getDefault()));
        setTime(timePicker.getValue(), false);

        if(!isInitial) {
            refresh();
        }
    }

    private void refresh() {
        if (markersAdded) {
            locationScene.clearMarkers();
            distanceRefreshed();
        } else {
            Toast.makeText(
                    this, "Models Not Loaded. Please wait to fetch new data.", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void setTime(int hour, boolean shouldRefresh) {
        filterStartDate = Utils.setTimeForLocalDate(hour, 0, 0, apiStartDate);
        filterEndDate = Utils.setTimeForLocalDate(hour, 59, 59, apiStartDate);
        if(shouldRefresh) {
            if (markersAdded) {
                for (AOTNode node : nodes) {
                    filterAndAggregateObservations(node, aotNode -> {
                        int i = nodes.indexOf(aotNode);
                        setInnerLayoutValues(exampleLayoutRenderables.get(i), aotNode, i);
                        return null;
                    });
                }
            } else {
                Toast.makeText(
                        this, "Models Not Loaded.", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    @Override
    public void onScrollStateChange(NumberPicker view, int scrollState) {
        if(scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
            setTime(view.getValue(), true);
            isTimePickerScrolling = false;
            Log.d(TAG, "State changed " + view.getValue());
        }
        else {
            isTimePickerScrolling = true;
        }
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        if(!isTimePickerScrolling) {
            setTime(newVal, true);
            Log.d(TAG, "Value changed " +newVal);
        }
    }

    private void showProgressView(String message) {
        if(progressViewSnackbar == null) {
            coordinatorLayout = findViewById(R.id.coordinator);
            progressViewSnackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_INDEFINITE);
            ViewGroup contentLay = (ViewGroup) progressViewSnackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text).getParent();
            ProgressBar item = new ProgressBar(contentLay.getContext());
            contentLay.addView(item,0);
        }
        else {
            progressViewSnackbar.setText(message);
        }

        if (!progressViewSnackbar.isShownOrQueued()) {
            progressViewSnackbar.show();
            Log.d(TAG, "Showing progress view");
        }
    }

    private void hideProgressView() {
        if(progressViewSnackbar != null && progressViewSnackbar.isShownOrQueued()) {
            progressViewSnackbar.dismiss();
            progressViewSnackbar = null;
            Log.d(TAG, "Hiding progress view");
        }
    }
}

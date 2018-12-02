package aot.cs491.com.aot_ar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.jjoe64.graphview.GraphView;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.LocationNode;
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, DatePickerDialog.OnDateSetListener, NumberPicker.OnValueChangeListener
{

    int count1 =0;
    private FusedLocationProviderClient mFusedLocationClient;
    Location loc;

    String menuOptionSelected;
    double longitude;
    double latitude;
    int distance;
    Date apiStartDate;
    Date apiEndDate;
    Date filterStartDate;
    Date filterEndDate;
    AOTSensorType sensorType;
    boolean useImperialUnits;

    static final int DIALOG_ID = 0;

    Button dateButton;
    DatePickerDialog datePickerDialog;
    NumberPicker timePicker;

    private boolean installRequested;
    private boolean hasFinishedLoading = false;
    private boolean markersAdded=false;

    private Snackbar loadingMessageSnackbar = null;
    private ArSceneView arSceneView;
    // Renderables for this example
    CompletableFuture<ViewRenderable> exampleLayout;
    CompletableFuture<ViewRenderable> exampleLayout1;
    private ModelRenderable andyRenderable;
    private ViewRenderable exampleLayoutRenderable;
    private ViewRenderable exampleLayoutRenderable1;
    // Our ARCore-Location scene
    private LocationScene locationScene;
    TextView helloWorldLabel;
    public static final String TAG = MainActivity.class.getSimpleName();

    List <AOTNode> nodes = null;
    List <CompletableFuture<ViewRenderable>> exampleLayouts;
    List <ViewRenderable> exampleLayoutRenderables;
    List <LocationMarker> locationMarkers;
    List <LocationMarkerCustom> locationMarkersCustom;

    public void initializeAndCallAPI()
    {
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
                                            filterAndAggregateObservations(node);
                                        }
                                    }
                                    else {
                                        helloWorldLabel.setText("\nNode: " + node.toString());
                                        helloWorldLabel.append("\n" + sensorType.name() + ": No data available");
                                        helloWorldLabel.append("\n");
                                    }
                                    helloWorldLabel.append("\n");
                                },
                                throwable -> Log.e(TAG, "Error while fetching nearby nodes:", throwable),
                                () -> {
                                    Log.i(TAG, "Finished fetching nearby nodes");
                                    handleCompleteableFutures();
                                }
                        )
        );
    }

    private void filterAndAggregateObservations(AOTNode node) {
        List<Observable<List<AOTObservation>>> filterCalls = new ArrayList<>();
        for(AOTSensorType aSensorType: AOTSensorType.values()) {
            filterCalls.add(AOTService.filterObservations(node.getObservations(), aSensorType, filterStartDate, filterEndDate).toObservable());
        }
        DisposablesManager.add(
                Observable.fromIterable(filterCalls)
                        .flatMap(listSingle -> listSingle)
                        .flatMap(aotObservations -> AOTService.aggregateObservations(aotObservations, "avg").toObservable())
//                                                            .subscribe(aotObservations -> {
//                                                                DisposablesManager.add(
//                                                                    AOTService.aggregateObservations(aotObservations, "avg")
                        .subscribe(aotObservation -> {
                                    if(aotObservation.getSensorPath() != null) {
                                        node.getAggregatedObservations().put(aotObservation.getSensorType(), aotObservation);
                                    }
                                    else {
                                        node.getAggregatedObservations().put(aotObservation.getSensorType(), null);
                                    }
                                },
                                throwable -> Log.e(TAG, "Error while filtering/aggregating observations:", throwable)
                        )
        );
//                                                            })
//                                            );
    }

    public void distanceRefreshed()
    {

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

        helloWorldLabel = findViewById(R.id.textTime);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize date picker
        Calendar calendar = Calendar.getInstance();

        datePickerDialog = new DatePickerDialog(this);
        datePickerDialog.setOnDateSetListener(this);
        dateButton = findViewById(R.id.dateButton);
        dateButton.setOnClickListener(v -> datePickerDialog.show());
        timePicker = findViewById(R.id.timePicker);
        timePicker.setOnValueChangedListener(this);

        // Set default date and time
        onDateSet(null, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        onValueChange(null, 0, calendar.get(Calendar.HOUR_OF_DAY));
        timePicker.setValue(calendar.get(Calendar.HOUR_OF_DAY));

        distance = PreferenceManager.getDefaultSharedPreferences(this).getInt("distanceThreshold", 2000);
        useImperialUnits = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("useImperialUnits", false);

        sensorType = AOTSensorType.TEMPERATURE;



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
                            Log.i(TAG, "Finished loading models.");
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

                            if (loadingMessageSnackbar != null) {
                                for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                                    if (plane.getTrackingState() == TrackingState.TRACKING) {
                                        hideLoadingMessage();
                                    }
                                }
                            }
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

        if (id == R.id.nav_light) {
            Log.i("MainActivity:: MEnu","Light Selected");
//            menuOptionSelected="light";
//            if(locationScene!=null)
//            {
//                setInnerLayoutValuesFromMenu();
//            }

        } else if (id == R.id.nav_pollution) {
            Log.i("MainActivity:: MEnu","Pollution Selected");
//            menuOptionSelected="airquality";
//            if(locationScene!=null)
//            {
//                setInnerLayoutValuesFromMenu();
//            }

        } else if (id == R.id.nav_weather) {
//            menuOptionSelected="weather";
            Log.i("MainActivity:: MEnu","Weather Selected");
//            if(locationScene!=null)
//            {
//                setInnerLayoutValuesFromMenu();
//            }

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Example node of a layout
     *
     * @return
     */

    private Node getExampleView1(ViewRenderable vr) {
        Node base = new Node();
        base.setRenderable(vr);
        Context c = this;
        // Add  listeners etc here
        View eView = vr.getView();
        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched.", Toast.LENGTH_LONG)
                    .show();
            return false;
        });

        return base;
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
        //View compLayout = findViewById(R.id.comp_layout_id);
        //TextView compLayoutText1 = compLayout.findViewById(R.id.comptext1);


        eView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // TODO Auto-generated method stub
                //compLayout.setVisibility(LinearLayout.VISIBLE);
                for(int i=0; i<nodes.size();i++)
                {
                    View eView = exampleLayoutRenderables.get(i).getView();
                    View vgraph= eView.findViewById(R.id.graph_layout_id);
                    vgraph.setVisibility(LinearLayout.GONE);

                }
                v.findViewById(R.id.graph_layout_id).setVisibility(LinearLayout.VISIBLE);
                GraphView graph = (GraphView) v.findViewById(R.id.graph1);
                graph.removeAllSeries();
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[]{
                        new DataPoint(0, 1),
                        new DataPoint(1, 5),
                        new DataPoint(2, 3),
                        new DataPoint(3, 5),
                        new DataPoint(4, 17),
                        new DataPoint(5, 5),
                        new DataPoint(6, 5),
                        new DataPoint(7, 23),
                        new DataPoint(8, 18),
                        new DataPoint(9, 5),
                        new DataPoint(10, 5),
                        new DataPoint(11, 7),
                        new DataPoint(12, 13),
                        new DataPoint(13, 5),
                        new DataPoint(14, 3),
                        new DataPoint(15, 23),
                        new DataPoint(16, 5),
                        new DataPoint(17, 2),
                        new DataPoint(18, 5),
                        new DataPoint(19, 2),
                        new DataPoint(20, 2),
                        new DataPoint(21, 5),
                        new DataPoint(22, 4),
                        new DataPoint(23, 6),
                });
                graph.addSeries(series);


                Toast.makeText(
                        c, "Location Marker Long Pressed.", Toast.LENGTH_LONG)
                        .show();
                return false;
            }
        });

        textViewData1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout l=(LinearLayout) v.getParent().getParent().getParent();
                l.findViewById(R.id.graph_layout_id).setVisibility(LinearLayout.VISIBLE);
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

                Toast.makeText(
                    c, "Temp marker touched." + i, Toast.LENGTH_LONG)
                    .show();
                 return ;
            }
        });

        textViewData2.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Pres marker touched.", Toast.LENGTH_LONG)
                    .show();
            return false;
        });
        textViewData3.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Hum marker touched.", Toast.LENGTH_LONG)
                    .show();
            return false;
        });

        return base;
    }


    /***
     * Example Node of a 3D model
     *
     * @return
     */
    private Node getAndy() {
        Node base = new Node();
        base.setRenderable(andyRenderable);
        Context c = this;
        base.setOnTapListener((v, event) -> {
            Toast.makeText(
                    c, "Andy touched.", Toast.LENGTH_LONG)
                    .show();
        });
        return base;
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

        if (arSceneView.getSession() != null) {
            showLoadingMessage();
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
                        MainActivity.this.findViewById(android.R.id.content),
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

    public LocationMarkerCustom createLocationMarkerCustom(double longitude, double latitude, ViewRenderable vr, AOTNode aotNode )
    {
        return new LocationMarkerCustom(
                longitude,
                latitude,
                getExampleView1(vr),aotNode
        );
    }

    public void setRenderEvent(LocationMarker lm, ViewRenderable vr, AOTNode aotN, int i) {
        lm.setRenderEvent(new LocationNodeRender() {
            @Override
            public void render(LocationNode node) {
                View eView = vr.getView();
                TextView value2 = eView.findViewById(R.id.textView_dist1);
                value2.setText(node.getDistance() + "M");

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
        TextView value1 = eView.findViewById(R.id.textView_temp);
        TextView value3 = eView.findViewById(R.id.textView_pres);
        TextView value4 = eView.findViewById(R.id.textView_hum);
        nameView.setText(aotN.getAddress());
        AOTObservation observation;
        if(menuOptionSelected=="weather") {

            observation = aotN.getAggregatedObservations().get(AOTSensorType.TEMPERATURE);
            if(observation !=null)
                value1.setText(observation.getValue(useImperialUnits).toString() + observation.getUnits(useImperialUnits));
            else
                value1.setText("NF");

            observation = aotN.getAggregatedObservations().get(AOTSensorType.PRESSURE);
            if(observation !=null)
                value3.setText(observation.getValue(useImperialUnits).toString() + observation.getUnits(useImperialUnits));
            else
                value3.setText("NF");

            observation = aotN.getAggregatedObservations().get(AOTSensorType.HUMIDITY);
            if(observation !=null)
                value4.setText(observation.getValue(useImperialUnits).toString() + observation.getUnits(useImperialUnits));
            else
                value4.setText("NF");

        }
        if(menuOptionSelected=="light") {

            observation = aotN.getAggregatedObservations().get(AOTSensorType.LIGHT_INTENSITY);
            if(observation !=null)
                value1.setText(observation.getValue(useImperialUnits).toString() + observation.getUnits(useImperialUnits));
            else
                value1.setText("NF");

            observation = aotN.getAggregatedObservations().get(AOTSensorType.INFRA_RED_LIGHT);
            if(observation !=null)
                value3.setText(observation.getValue(useImperialUnits).toString() + observation.getUnits(useImperialUnits));
            else
                value3.setText("NF");

            observation = aotN.getAggregatedObservations().get(AOTSensorType.ULTRA_VIOLET_LIGHT);
            if(observation !=null)
                value4.setText(observation.getValue(useImperialUnits).toString() + observation.getUnits(useImperialUnits));
            else
                value4.setText("NF");

        }
        if(menuOptionSelected=="airquality") {

            observation = aotN.getAggregatedObservations().get(AOTSensorType.CARBON_MONOXIDE);
            if(observation !=null)
                value1.setText(observation.getValue(useImperialUnits).toString() + observation.getUnits(useImperialUnits));
            else
                value1.setText("NF");

            observation = aotN.getAggregatedObservations().get(AOTSensorType.SULPHUR_DIOXIDE);
            if(observation !=null)
                value3.setText(observation.getValue(useImperialUnits).toString() + observation.getUnits(useImperialUnits));
            else
                value3.setText("NF");

            observation = aotN.getAggregatedObservations().get(AOTSensorType.NITROGEN_DIOXIDE);
            if(observation !=null)
                value4.setText(observation.getValue(useImperialUnits).toString() + observation.getUnits(useImperialUnits));
            else
                value4.setText("NF");
        }
    }
    public void setInnerLayoutValuesFromMenu()
    {
        for(int i=0; i<nodes.size(); i++)
        {
            setInnerLayoutValues(exampleLayoutRenderables.get(i), nodes.get(i),i);
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        apiStartDate = Utils.stringToLocalDate(year, month + 1, dayOfMonth);
        apiEndDate = Utils.setTimeForLocalDate(23, 59, 59, apiStartDate);
        dateButton.setText(Utils.dateToString(apiStartDate, "EEE, MMM d, ''yy", TimeZone.getDefault()));
        onValueChange(timePicker,0,timePicker.getValue());

        if(markersAdded) {
            locationScene.clearMarkers();
            distanceRefreshed();
        }
        else
        {
            Toast.makeText(
                    this, "Models Not Loaded. Please wait to fetch new data.", Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        filterStartDate = Utils.setTimeForLocalDate(newVal, 0, 0, apiStartDate);
        filterEndDate = Utils.setTimeForLocalDate(newVal, 59, 59, apiStartDate);
        if(markersAdded)
        {
            for(AOTNode node : nodes) {
                filterAndAggregateObservations(node);
            }
            for(int i=0; i<nodes.size(); i++)
            {
                setInnerLayoutValues(exampleLayoutRenderables.get(i), nodes.get(i),i);
            }
        }
        else
        {
            Toast.makeText(
                    this, "Models Not Loaded.", Toast.LENGTH_LONG)
                    .show();
        }
        // TODO: Trigger filter and aggregation
    }
}

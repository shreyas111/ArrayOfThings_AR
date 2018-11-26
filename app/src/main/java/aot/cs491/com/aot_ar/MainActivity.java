package aot.cs491.com.aot_ar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import aot.cs491.com.aot_ar.aothttpapi.AOTNode;
import aot.cs491.com.aot_ar.aothttpapi.AOTObservation;
import aot.cs491.com.aot_ar.aothttpapi.AOTService;
import aot.cs491.com.aot_ar.utils.DisposablesManager;
import aot.cs491.com.aot_ar.utils.Utils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.LocationNode;
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener
{

    int count1 =0;

    static final int DIALOG_ID = 0;
    FloatingActionButton fab;
    FloatingActionButton fab1;

    //TextView mDate;
    //TextView mTime;

    private boolean installRequested;
    private boolean hasFinishedLoading = false;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arSceneView = findViewById(R.id.ar_scene_view);

        helloWorldLabel = findViewById(R.id.textTime);

        Date parsedDate = Utils.stripTimeFromLocalDate(new Date());
        double longitude = -87.662111;
        double latitude = 41.871629;
        int distance = 2000;

        String sensorType = "Temperature";
        Date filterStartDate = parsedDate;
        Date filterEndDate = null;



        nodes = new ArrayList<>();
        exampleLayouts = new ArrayList<>();
        exampleLayoutRenderables = new ArrayList<ViewRenderable>();
        locationMarkers = new ArrayList<LocationMarker>();
        locationMarkersCustom = new ArrayList<LocationMarkerCustom>();

        DisposablesManager.add(
                AOTService.fetchObservationsFromNearbyNodes(longitude, latitude, distance, parsedDate)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(disposable -> Log.i(TAG, "Fetching nearby nodes..."))
                        .subscribe(node -> {
                                    Log.i(TAG, "node emitted: " + node.toString());
                                    nodes.add(node);
                                    helloWorldLabel.setText("\nNode: " + node.toString());

//                                    if (exampleLayouts== null)
//                                    {
//                                        exampleLayouts = new ArrayList<>();
//                                    }


                                    //if(count1<=4) {
                                    exampleLayouts.add(
                                            ViewRenderable.builder()
                                                    .setView(this,R.layout.inner_layout)
                                                    .build());
                                    //}
//                                    exampleLayout =
//                                            ViewRenderable.builder()
//                                                    .setView(this, R.layout.example_layout)
//                                                    .build();
//                                    exampleLayout1 =
//                                            ViewRenderable.builder()
//                                                    .setView(this, R.layout.example_layout)
//                                                    .build();

                                    if (node.getObservations() != null) {
                                        Log.i(TAG, "node has: " + node.getObservations().size() + " observations from " + node.getSensors().size() + " sensors");


                                        if (!node.getObservations().isEmpty()) {
                                            List<AOTObservation> temperatureData = AOTService.filterObservations(node.getObservations(), sensorType, filterStartDate, filterEndDate);
                                            AOTObservation aggregatedObservation = AOTService.aggregateObservations(temperatureData, "avg");
                                            helloWorldLabel.append("\n" + sensorType + ": " + aggregatedObservation.getValue());
                                        }



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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //mDate= findViewById(R.id.textDate);
        //mTime= findViewById(R.id.textTime);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab1 = (FloatingActionButton) findViewById(R.id.fab1);

        fab.setOnClickListener(view -> {
            Snackbar snackbar = Snackbar.make(MainActivity.this.findViewById(android.R.id.content), "Select a Date", Snackbar.LENGTH_LONG);
            snackbar.getView().setBackgroundColor(0xbf323232);
            snackbar.show();

            Calendar now = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    MainActivity.this,
                    MainActivity.this,
                    now.get(Calendar.YEAR), // Initial year selection
                    now.get(Calendar.MONTH), // Initial month selection
                    now.get(Calendar.DAY_OF_MONTH) // Inital day selection
            );
            dialog.show();
        });

        fab1.setOnClickListener(view -> {
            Snackbar snackbar = Snackbar.make(MainActivity.this.findViewById(android.R.id.content), "Select a Time", Snackbar.LENGTH_LONG);
            snackbar.getView().setBackgroundColor(0xbf323232);
            snackbar.show();

            Calendar now = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(
                    MainActivity.this,
                    MainActivity.this,
                    now.get(Calendar.HOUR_OF_DAY), // Initial year selection
                    now.get(Calendar.MINUTE), // Initial month selection
                    false
                    );
            dialog.show();
        });



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        // Build a renderable from a 2D View.
//         exampleLayout =
//                ViewRenderable.builder()
//                        .setView(this, R.layout.example_layout)
//                        .build();

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
//        CompletableFuture<ModelRenderable> andy = ModelRenderable.builder()
//                .setSource(this, R.raw.andy)
//                .build();

        //handleCompleteableFutures();


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
                                //locationMarkers= new ArrayList<LocationMarker>();

                                //for (int i=0; i<2;i++)
                                //{
//                                    locationMarkers.add(new LocationMarker(
//                                        -87.664400,
//                                        41.871910,
//                                        getExampleView(0)));

//                                locationMarkers.add(new LocationMarker(
//                                        -87.654076,
//                                        41.86946,
//                                        getExampleView(1)));


                                //}
                                // Now lets create our location markers.
                                // First, a layout
                                for (int i=0; i<nodes.size(); i++)
                                {
//                                    locationMarkersCustom.add(createLocationMarkerCustom(nodes.get(i).getLocation().lon(),
//                                            nodes.get(i).getLocation().lat(), exampleLayoutRenderables.get(i),nodes.get(i)));
                                    locationMarkers.add(createLocationMarker(nodes.get(i).getLocation().lon(),
                                            nodes.get(i).getLocation().lat(), exampleLayoutRenderables.get(i)));
                                }

//                                locationMarkers.add(createLocationMarker(-87.664400,
//                                        41.871910, exampleLayoutRenderables.get(0)));
//                                locationMarkers.add(createLocationMarker(-87.654608,
//                                        41.869678, exampleLayoutRenderables.get(1)));
                                //LocationMarker layoutLocationMarker = createLocationMarker(-87.664400,
                                //        41.871910, exampleLayoutRenderables.get(0));

//                                        new LocationMarker(
//                                        -87.664400,
//                                        41.871910,
//                                        getExampleView1(exampleLayoutRenderables.get(0))
//                                );
//                                LocationMarker layoutLocationMarker1 = createLocationMarker(-87.654608,
//                                        41.869678, exampleLayoutRenderables.get(1));

//                                LocationMarker layoutLocationMarker1 = new LocationMarker(
//                                        -87.654608,
//                                        41.869678,
//                                        getExampleView1(exampleLayoutRenderables.get(1))
//                                );
                                for(int i=0; i<nodes.size(); i++)
                                {
                                    setRenderEvent(locationMarkers.get(i), exampleLayoutRenderables.get(i), nodes.get(i));
                                }

                                // An example "onRender" event, called every frame
                                // Updates the layout with the markers distance
//                                locationMarkers.get(0).setRenderEvent(new LocationNodeRender() {
//                                    @Override
//                                    public void render(LocationNode node) {
//                                        //View eView = exampleLayoutRenderables.get(0).getView();
//                                        View eView = exampleLayoutRenderables.get(0).getView();
//                                        TextView distanceTextView = eView.findViewById(R.id.textView2);
//                                        distanceTextView.setText(node.getDistance() + "M");
//                                    }
//                                });

//                                locationMarkers.get(1).setRenderEvent(new LocationNodeRender() {
//                                    @Override
//                                    public void render(LocationNode node) {
//                                        //View eView = exampleLayoutRenderables.get(0).getView();
//                                        View eView = exampleLayoutRenderables.get(1).getView();
//                                        TextView distanceTextView = eView.findViewById(R.id.textView2);
//                                        distanceTextView.setText(node.getDistance() + "M");
//                                    }
//                                });

//                                locationMarkers.get(0).setRenderEvent(new LocationNodeRender() {
//                                    @Override
//                                    public void render(LocationNode node) {
//                                        //View eView = exampleLayoutRenderables.get(0).getView();
//                                        View eView = exampleLayoutRenderables.get(0).getView();
//                                        TextView distanceTextView = eView.findViewById(R.id.textView2);
//                                        distanceTextView.setText(node.getDistance() + "M");
//                                    }
//                                });

//                                locationMarkers.get(1).setRenderEvent(new LocationNodeRender() {
//                                    @Override
//                                    public void render(LocationNode node) {
//                                        //View eView = exampleLayoutRenderables.get(0).getView();
//                                        View eView = exampleLayoutRenderables.get(1).getView();
//                                        TextView distanceTextView = eView.findViewById(R.id.textView2);
//                                        distanceTextView.setText(node.getDistance() + "M");
//                                    }
//                                });
                                // Adding the marker
                                for(int i=0; i< nodes.size();i++) {
                                    locationScene.mLocationMarkers.add(locationMarkers.get(i));

                                }
                                //locationScene.mLocationMarkers.add(locationMarkers.get(1));
                                //locationScene.mLocationMarkers.add(locationMarkers.get(2));
                                //locationScene.mLocationMarkers.add(locationMarkers.get(3));
                                //locationScene.mLocationMarkers.add(locationMarkers.get(0));
                                //locationScene.mLocationMarkers.add(locationMarkers.get(1));

                                // Adding a simple location marker of a 3D model
//                                locationScene.mLocationMarkers.add(
//                                        new LocationMarker(
//                                                -0.119677,
//                                                51.478494,
//                                                getAndy()));
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
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        String time = "You picked the following time: "+hourOfDay+"h "+minute +"m";
        Snackbar snackbar = Snackbar.make(MainActivity.this.findViewById(android.R.id.content), time, Snackbar.LENGTH_LONG);
        snackbar.getView().setBackgroundColor(0xbf323232);
        snackbar.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        String date = "You picked the following date: "+dayOfMonth+"/"+(monthOfYear+1)+"/"+year;
        Snackbar snackbar = Snackbar.make(MainActivity.this.findViewById(android.R.id.content), date, Snackbar.LENGTH_LONG);
        snackbar.getView().setBackgroundColor(0xbf323232);
        snackbar.show();
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

        } else if (id == R.id.nav_pollution) {

        } else if (id == R.id.nav_weather) {

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
    private Node getExampleView() {
        Node base = new Node();
        base.setRenderable(exampleLayoutRenderable);
        Context c = this;
        // Add  listeners etc here
        View eView = exampleLayoutRenderable.getView();
        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched.", Toast.LENGTH_LONG)
                    .show();
            return false;
        });

        return base;
    }
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
        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched.", Toast.LENGTH_LONG)
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
                //CompletableFuture.allOf(
                //        exampleLayouts.get(0),
                //        exampleLayouts.get(1),
                //        exampleLayouts.get(2),
                //        exampleLayouts.get(3))
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
//                                if(exampleLayoutRenderables== null) {
//                                    exampleLayoutRenderables = new ArrayList<ViewRenderable>();
//                                }
//                                for (CompletableFuture<ViewRenderable> a : exampleLayouts) {
//                                    exampleLayoutRenderables.add(a.get());
//                                }
                                for(int i=0; i< nodes.size(); i++) {
                                    exampleLayoutRenderables.add(exampleLayouts.get(i).get());
                                }
                                //exampleLayoutRenderable = exampleLayout.get();
                                //exampleLayoutRenderable1 = exampleLayout1.get();
                                //exampleLayoutRenderables.add(exampleLayouts.get(0).get());
                                //exampleLayoutRenderables.add(exampleLayouts.get(1).get());

                                //andyRenderable = andy.get();
                                hasFinishedLoading = true;

                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }

                            return null;
                        });

    }

    public LocationMarker createLocationMarker(double longitude, double latitude, ViewRenderable vr )
    {
        return new LocationMarker(
                longitude,
                latitude,
                getExampleView1(vr)
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

    public void setRenderEvent(LocationMarker lm, ViewRenderable vr, AOTNode aotN)
    {
        lm.setRenderEvent(new LocationNodeRender() {
            @Override
            public void render(LocationNode node) {
                //View eView = exampleLayoutRenderables.get(0).getView();
                View eView = vr.getView();
                TextView distanceTextView = eView.findViewById(R.id.textView_dist1);
                TextView nameView = eView.findViewById(R.id.textView_loc1);
                distanceTextView.setText(node.getDistance() + "M");
                nameView.setText(aotN.getAddress());

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
}

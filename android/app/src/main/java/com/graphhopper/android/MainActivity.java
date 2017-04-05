package com.graphhopper.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/*
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.eddystone.Eddystone;*/
import com.estimote.sdk.SystemRequirementsChecker;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.util.Constants;
import com.graphhopper.util.Helper;
import com.graphhopper.util.Parameters.Algorithms;
import com.graphhopper.util.Parameters.Routing;
import com.graphhopper.util.PointList;
import com.graphhopper.util.ProgressListener;
import com.graphhopper.util.StopWatch;


import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.Tile;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.layers.vector.PathLayer;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static android.widget.LinearLayout.*;

public class MainActivity extends Activity implements BeaconConsumer, RangeNotifier {
    private static final int NEW_MENU_ID = Menu.FIRST + 1;
    private MapView mapView;
    private Activity application;

    private TextView infoText1;
    private TextView infoText2;

    private String closestBeacon;
    private double closestLon;
    private double closestLat;
    private double closestDistance;
    private int closestId;

    private long firstContactTime =Long.MAX_VALUE;


    private GraphHopper hopper;
    private GeoPoint start;
    private GeoPoint end;
    private Spinner localSpinner;
    private Button localButton;
    private Spinner remoteSpinner;
    private Button remoteButton;
    private volatile boolean prepareInProgress = false;
    private volatile boolean shortestPathRunning = false;
    private String currentArea = "bamberg";
    private String fileListURL = "http://download2.graphhopper.com/public/maps/" + Constants.getMajorVersion() + "/";
    private String prefixURL = fileListURL;
    private String downloadURL;
    private File mapsFolder;
    private ItemizedLayer<MarkerItem> itemizedLayer;
    private PathLayer pathLayer;
    private double lat;
    private double lon;


    private int userType =1;
    private LinearLayout parkingLayout;

    private boolean hasGeneralParkingInformation=false;

    private String requestUrl;
    private boolean hasParkingInformation=false;
    private boolean isParking =false;
    private List<ParkingSlot> parkingSlotList =new LinkedList<>();



    //TODO
    public static final String TAG = "monitor";
    private BeaconManager beaconManager;
 //   private Region region;
    private String scanId;
    private int available;


    protected boolean onLongPress(GeoPoint p) {
        if (!isReady())
            return false;

        if (shortestPathRunning) {
            logUser("Calculation still in progress");
            return false;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Tile.SIZE = Tile.calculateTileSize(getResources().getDisplayMetrics().scaledDensity);
        mapView = new MapView(this);

        final EditText input = new EditText(this);
        input.setText(currentArea);
        boolean greaterOrEqKitkat = Build.VERSION.SDK_INT >= 19;
        if (greaterOrEqKitkat) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                logUser("GraphHopper is not usable without an external storage!");
                return;
            }
            mapsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "/graphhopper/maps/");
        } else
            mapsFolder = new File(Environment.getExternalStorageDirectory(), "/graphhopper/maps/");

        if (!mapsFolder.exists())
            mapsFolder.mkdirs();




        parkingLayout =new LinearLayout(this);
        parkingLayout.setOrientation(VERTICAL);

        LinearLayout.LayoutParams LLParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        parkingLayout.setLayoutParams(LLParams);

        initFiles("bamberg");

        application =this;


        infoText1 =new TextView(this);
        infoText2 =new TextView(this);

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void showNotification(String title, String message) {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0,
                new Intent[] { notifyIntent }, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        SystemRequirementsChecker.checkWithDefaultDialogs(this);

        // beaconManager Register proceding
        beaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect the main Eddystone-UID frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // Detect the telemetry Eddystone-TLM frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        beaconManager.bind(this);



    }

    @Override
    protected void onPause() {
        super.onPause();

        //onPause proceeding
        beaconManager.unbind(this);

        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hopper != null)
            hopper.close();

        hopper = null;
        // necessary?
        System.gc();

        // Cleanup VTM
        mapView.map().destroy();

     }

    boolean isReady() {
        // only return true if already loaded
        if (hopper != null)
            return true;

        if (prepareInProgress) {
            logUser("Preparation still in progress");
            return false;
        }
        logUser("Prepare finished but hopper not ready. This happens when there was an error while loading the files");
        return false;
    }

    private void initFiles(String area) {
        prepareInProgress = true;
        currentArea = area;
        downloadingFiles();

    }


    void downloadingFiles() {
        final File areaFolder = new File(mapsFolder, currentArea + "-gh");
        if (downloadURL == null || areaFolder.exists()) {


                    loadMap(areaFolder);

        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Downloading and uncompressing " + downloadURL);
        dialog.setIndeterminate(false);
        dialog.setMax(100);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.show();

        new GHAsyncTask<Void, Integer, Object>() {
            protected Object saveDoInBackground(Void... _ignore)
                    throws Exception {
                String localFolder = Helper.pruneFileEnd(AndroidHelper.getFileName(downloadURL));
                localFolder = new File(mapsFolder, localFolder + "-gh").getAbsolutePath();
                log("downloading & unzipping " + downloadURL + " to " + localFolder);
                AndroidDownloader downloader = new AndroidDownloader();
                downloader.setTimeout(30000);
                downloader.downloadAndUnzip(downloadURL, localFolder,
                        new ProgressListener() {
                            @Override
                            public void update(long val) {
                                publishProgress((int) val);
                            }
                        });
                return null;
            }

            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                dialog.setProgress(values[0]);
            }

            protected void onPostExecute(Object _ignore) {
                dialog.dismiss();
                if (hasError()) {
                    String str = "An error happened while retrieving maps:" + getErrorMessage();
                    log(str, getError());
                    logUser(str);
                } else {
                    loadMap(areaFolder);
                }
            }
        }.execute();
    }

    void loadMap(File areaFolder) {
        logUser("loading map");

        // Long press receiver
        mapView.map().layers().add(new LongPressLayer(mapView.map()));

        // Map file source
        MapFileTileSource tileSource = new MapFileTileSource();
        Log.d(TAG,new File(areaFolder, currentArea + ".map").getAbsolutePath());
        tileSource.setMapFile(new File(areaFolder, currentArea + ".map").getAbsolutePath());
        VectorTileLayer l = mapView.map().setBaseMap(tileSource);
        mapView.map().setTheme(VtmThemes.DEFAULT);
        mapView.map().layers().add(new BuildingLayer(mapView.map(), l));
        mapView.map().layers().add(new LabelLayer(mapView.map(), l));

        // Markers layer
        itemizedLayer = new ItemizedLayer<>(mapView.map(), (MarkerSymbol) null);
        mapView.map().layers().add(itemizedLayer);

        // Map position
        GeoPoint mapCenter = tileSource.getMapInfo().boundingBox.getCenterPoint();
        mapView.map().setMapPosition(mapCenter.getLatitude(), mapCenter.getLongitude(), 1 << 18);


       // setContentView(mapView);
        loadGraphStorage();




    }

    void loadGraphStorage() {
        logUser("loading graph (" + Constants.VERSION + ") ... ");
        new GHAsyncTask<Void, Void, Path>() {
            protected Path saveDoInBackground(Void... v) throws Exception {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                tmpHopp.load(new File(mapsFolder, currentArea).getAbsolutePath() + "-gh");
                log("found graph " + tmpHopp.getGraphHopperStorage().toString() + ", nodes:" + tmpHopp.getGraphHopperStorage().getNodes());
                hopper = tmpHopp;
                return null;
            }

            protected void onPostExecute(Path o) {
                if (hasError()) {
                    logUser("An error happened while creating graph:"
                            + getErrorMessage());
                } else {
                    logUser("Finished loading graph. Press long to define where to start and end the route.");
                }

                finishPrepare();
            }
        }.execute();
    }

    private void finishPrepare() {
        prepareInProgress = false;
    }

    private PathLayer createPathLayer(PathWrapper response) {
        Style style = Style.builder()
                .generalization(Style.GENERALIZATION_SMALL)
                .strokeColor(0x9900cc33)
                .strokeWidth(4 * getResources().getDisplayMetrics().density)
                .build();
        PathLayer pathLayer = new PathLayer(mapView.map(), style);
        List<GeoPoint> geoPoints = new ArrayList<>();
        PointList pointList = response.getPoints();
        for (int i = 0; i < pointList.getSize(); i++)
            geoPoints.add(new GeoPoint(pointList.getLatitude(i), pointList.getLongitude(i)));
        pathLayer.setPoints(geoPoints);
        return pathLayer;
    }

    @SuppressWarnings("deprecation")
    private MarkerItem createMarkerItem(GeoPoint p, int resource) {
        Drawable drawable = getResources().getDrawable(resource);
        Bitmap bitmap = AndroidGraphics.drawableToBitmap(drawable);
        MarkerSymbol markerSymbol = new MarkerSymbol(bitmap, 0.5f, 1);
        MarkerItem markerItem = new MarkerItem("", "", p);
        markerItem.setMarker(markerSymbol);
        return markerItem;
    }

    public void calcPath(final double fromLat, final double fromLon,
                         final double toLat, final double toLon) {

        log("calculating path ...");
        new AsyncTask<Void, Void, PathWrapper>() {
            float time;

            protected PathWrapper doInBackground(Void... v) {
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).
                        setAlgorithm(Algorithms.DIJKSTRA_BI);
                req.getHints().
                        put(Routing.INSTRUCTIONS, "false");
                GHResponse resp = hopper.route(req);
                time = sw.stop().getSeconds();
                return resp.getBest();
            }

            protected void onPostExecute(PathWrapper resp) {
                if (!resp.hasErrors()) {
                    log("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
                            + toLon + " found path with distance:" + resp.getDistance()
                            / 1000f + ", nodes:" + resp.getPoints().getSize() + ", time:"
                            + time + " " + resp.getDebugInfo());
                    logUser("the route is " + (int) (resp.getDistance() / 100) / 10f
                            + "km long, time:" + resp.getTime() / 60000f + "min, debug:" + time);

                    pathLayer = createPathLayer(resp);
                    mapView.map().layers().add(pathLayer);
                    mapView.map().updateMap(true);
                } else {
                    logUser("Error:" + resp.getErrors());
                }
                shortestPathRunning = false;
            }
        }.execute();
    }

    private void log(String str) {
        Log.i("GH", str);
    }

    private void log(String str, Throwable t) {
        Log.i("GH", str, t);
    }

    private void logUser(String str) {
        log(str);
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, NEW_MENU_ID, 0, "Google");
        // menu.add(0, NEW_MENU_ID + 1, 0, "Other");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case NEW_MENU_ID:
                if (start == null || end == null) {
                    logUser("tap screen to set start and end of route");
                    break;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                // get rid of the dialog
                intent.setClassName("com.google.android.apps.maps",
                        "com.google.android.maps.MapsActivity");
                intent.setData(Uri.parse("http://maps.google.com/maps?saddr="
                        + start.getLatitude() + "," + start.getLongitude() + "&daddr="
                        + end.getLatitude() + "," + end.getLongitude()));
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onBeaconServiceConnect() {
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        beaconManager.addRangeNotifier(this);
    }

    String identificationHelperMethodPutToTrash(Identifier tellMe)
    {
        if(tellMe.toString().equals("0x6e78426f6333"))
            return "WXBS";
        if(tellMe.toString().equals("0x6d7574494b6d"))
            return "rgvB";


        return "-do not know-";
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {


        for (Beacon beacon: collection) {

            // TODO CODE FROM LIBRARY! reasonable?
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                Log.d(TAG, /*"I see a beacon transmitting namespace id: "+namespaceId+*/
                        identificationHelperMethodPutToTrash(instanceId)+
                        " approximately "+beacon.getDistance()+" meters away.");



               evaluateBeacon(instanceId.toString(),beacon.getDistance());

                // Do we have telemetry data?
                if (beacon.getExtraDataFields().size() > 0) {
                    long telemetryVersion = beacon.getExtraDataFields().get(0);
                    long batteryMilliVolts = beacon.getExtraDataFields().get(1);
                    long pduCount = beacon.getExtraDataFields().get(3);
                    long uptime = beacon.getExtraDataFields().get(4);

                    Log.d(TAG, "The above beacon is sending telemetry version "+telemetryVersion+
                            ", has been up for : "+uptime+" seconds"+
                            ", has a battery level of "+batteryMilliVolts+" mV"+
                            ", and has transmitted "+pduCount+" advertisements.");

                }


            }
        }
    }

    /**
     * Evaluate a found Beacon with List of beacons
     * @param beaconIdentifier (beaconidentification
     * @param distance from the beacon (measured, uncertain)
     */
    private void evaluateBeacon(final String beaconIdentifier, final double distance) {

        // TODO if we find a "parkingSlotList list" ->proceede informations still mockup
        if (beaconIdentifier.toString().equals("0x6d7574494b6d") && distance < 0.5) {
            if (!hasGeneralParkingInformation) {
                //todo if we find a parkinglist
                //new HttpGetRequest().execute("http://10.4.0.75/showtable.php");
                parkingSlotList = ParkingSlot.getParkingSpace();
                lat = 49.901673749;
                lon = 10.8693892;

                hasGeneralParkingInformation = true;

                //TODO delete?
                //final File areaFolder = new File(mapsFolder, currentArea + "-gh");
                //Log.d(TAG, areaFolder.getAbsolutePath());

                application.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mapView != null) {
                            available = 0;
                            for (ParkingSlot parkingSlot : parkingSlotList) {
                                GeoPoint p = new GeoPoint(parkingSlot.getLat(), parkingSlot.getLon());
                                if (parkingSlot.isOccupied() == false) {


                                    if (parkingSlot.getType() == 1)
                                        itemizedLayer.addItem(createMarkerItem(p, R.drawable.parking));
                                    if (parkingSlot.getType() == 2)
                                        itemizedLayer.addItem(createMarkerItem(p, R.drawable.wheelchair));
                                    if (parkingSlot.getType() == 3)
                                        itemizedLayer.addItem(createMarkerItem(p, R.drawable.parkcharge));

                                    if (closestBeacon == null) {
                                        closestBeacon = parkingSlot.getBeacon();
                                        closestLon = parkingSlot.getLon();
                                        closestLat = parkingSlot.getLat();
                                        closestId = parkingSlot.getNumber();
                                        closestDistance = Math.sqrt(Math.pow(lat - parkingSlot.getLat(), 2) + Math.pow(lon - parkingSlot.getLon(), 2));
                                    } else {

                                        double otherDistance = Math.sqrt(Math.pow(lat - parkingSlot.getLat(), 2) + Math.pow(lon - parkingSlot.getLon(), 2));
                                        if (otherDistance < closestDistance) {
                                            closestDistance = otherDistance;
                                            closestBeacon = parkingSlot.getBeacon();
                                            closestLon = parkingSlot.getLon();
                                            closestLat = parkingSlot.getLat();
                                            closestId=parkingSlot.getNumber();
                                        }
                                    }


                                    if (parkingSlot.getType() == userType)
                                        available++;
                                    else if (userType == 2)
                                        available++;

                                    if(parkingSlot.getType()==1 && userType ==3)
                                        available++;
                                } else
                                    itemizedLayer.addItem(createMarkerItem(p, R.drawable.noparking));
                            }


                            infoText1.setText(available + " Parking Spaces available for you");
                            infoText1.setTextSize(20);
                            int CELL_PADDING = 10;
                            infoText1.setPadding(CELL_PADDING, CELL_PADDING, CELL_PADDING, CELL_PADDING);
                            infoText1.setGravity(Gravity.CENTER);
                            infoText2.setPadding(CELL_PADDING, CELL_PADDING, CELL_PADDING, CELL_PADDING);
                            infoText2.setGravity(Gravity.CENTER);
                            infoText2.setText("");
                            infoText2.setTextSize(20);
                            application.setContentView(parkingLayout);
                            parkingLayout.addView(infoText1);
                            parkingLayout.addView(infoText2);
                            parkingLayout.addView(mapView);
                            mapView.map().setMapPosition(lat, lon, 1 << 17);
                            Toast.makeText(application, "Welcome in the parkingSlotList Space of Erba. We show you the fastest way to the next parkingSlotList space", Toast.LENGTH_SHORT).show();

                            if (closestBeacon != null) {
                                //calcPath(lon,lat,closestLon,closestLat);

                                shortestPathRunning = true;

                                application.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        calcPath(lat, lon, closestLat,
                                                closestLon);
                                        GeoPoint p1=new GeoPoint(lat,lon);
                                        itemizedLayer.addItem(createMarkerItem(p1, R.drawable.marker_icon_green));
                                        GeoPoint p2=new GeoPoint(closestLat,closestLon);
                                        itemizedLayer.addItem(createMarkerItem(p2, R.drawable.marker_icon_red));
                                    }
                                });
                            }
                        } else
                            hasGeneralParkingInformation = false;
                    }


                });

            }
        }

        //check if we are close to a parkingslot
            for (ParkingSlot slot : parkingSlotList) {

                if (slot.getBeacon().equals(beaconIdentifier.toString())) {
                    Log.d(TAG, "Parking Attempt!!!");


                    if(firstContactTime !=Long.MAX_VALUE && isParking ==false && System.currentTimeMillis()- firstContactTime >5000 && distance < 1.0) {
                        isParking = true;


                        application.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                setContentView(R.layout.pay);
                            }
                        });
                        Log.d(TAG, "PARKED!!!");

                    }

                        if (firstContactTime == Long.MAX_VALUE && distance < 1.0) {
                            firstContactTime = System.currentTimeMillis();
                            Log.d(TAG, "Time set");
                        }


                        if (firstContactTime < Long.MAX_VALUE && distance > 1.1) {
                            application.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {


                                    if (!isParking) {
                                        Log.d(TAG, "was not PARKED!!!");
                                        infoText1.setText(available + " Parking Spaces available for you");
                                    }
                            else {
                                Log.d(TAG, "left PARKING!!!");
                                isParking = false;
                                Log.d(TAG, "Time released");
                                        int costs=(int)(firstContactTime -System.currentTimeMillis())/4000;
                                        if(costs<1)
                                            costs=1;
                                        infoText1.setText("YOU PAID: "+costs+ " €");
                                        new HttpGetRequest().execute("http://10.4.0.75/book.php?id="+closestId+"&parkingSlotList=0&userID=Flo");
                                            }

                            firstContactTime = Long.MAX_VALUE;


                                }
                            });
                            Log.d(TAG, "PARKED!!!");




                        }

                        application.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (closestBeacon.equals(beaconIdentifier.toString()))
                                    infoText2.setText("Parking Space in " + round(distance, 2) + " m");
                                else
                                    infoText2.setText("closest Parking Space in " + round(distance, 2) + " m");
                            }
                        });





                    if (!hasParkingInformation) {
                        hasParkingInformation = true;


                    }
                }
            }



    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public void beginParkingByConfirmation(View view){
        //ENTER database
        application.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoText1.setText(" PARKED");
                setContentView(parkingLayout);

                new HttpGetRequest().execute("http://10.4.0.75/book.php?id="+closestId+"&parkingSlotList=1&userID=Flo");
                Log.d(TAG,closestId+" ////////////////////////////////");

            }
        });


    }

    public void denyOfParkingSlotEntering(View view){
        //LEAVE database
        application.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isParking =false;
                firstContactTime =Long.MAX_VALUE;

                infoText1.setText(available + " Parking Spaces available for you");

                // Go back to Map Layout
                setContentView(parkingLayout);

            }
        });
    }


    class LongPressLayer extends Layer implements GestureListener {

        LongPressLayer(org.oscim.map.Map map) {
            super(map);
        }

        @Override
        public boolean onGesture(Gesture g, MotionEvent e) {
            if (g instanceof Gesture.LongPress) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                return onLongPress(p);
            }
            return false;
        }
    }




}

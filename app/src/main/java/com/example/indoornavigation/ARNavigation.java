package com.example.indoornavigation;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.indoornavigation.mapping.Location;
import com.example.indoornavigation.mapping.LocationFactory;
import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ARNavigation extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = ARNavigation.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final double CHECK_FOR_ARRIVAL = 4.0;
    private static final double WRONG_DIRECTION = 4.0;

    private ArrayList<Integer> shortestPath = new ArrayList<>();
    private ArrayList<Integer> coordsOfDestinationId;
    private ArrayList<Integer> coordsOfDestination;

    private CloudAnchorFragment mARFragment;
    private ModelRenderable mObjRenderable;
    private AnchorNode mAnchorNode = null;
    private Node arrow;

    // private float currentDegree = 0f;
    private SensorManager sensorManager;

    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    //beacon Service
    private FirebaseFirestore fStore;
    StorageReference storageReference;
    CollectionReference notebookRef;
    private ListenerRegistration notebookListener;
    List<Double> distance437List, distance574List, distance570List= new ArrayList<>();
    double distance437=0, distance570=0, distance574 = 0;
    public double[] distanceVector = {0,0,0}, calculatedPosition = {0,0,0};
//    double[][] positions = new double[][]{{390, 182},{664,218},{929,181}};
    double[][] positions = new double[][]{{39.61830,20.83864},{39.61840,20.83862},{39.61852,20.83861}};
   // public BeaconService beaconService;
    //private boolean mUserRequestedInstall = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        ArrayList<Integer> coordsOfCurrentPos = getIntent().getIntegerArrayListExtra(DestinationActivity.COORDS_OF_CURRENT_POS);
        coordsOfDestinationId = getIntent().getIntegerArrayListExtra(DestinationActivity.COORDS_OF_DESTINATION_ID);
        ArrayList<Integer> coordsOfEntrance = getIntent().getIntegerArrayListExtra(DestinationActivity.COORDS_OF_ENTRANCE);
        String selectDestinationFrom = DestinationActivity.SELECT_DESTINATION_FROM;
        String selectLocationFrom = CurrentLocationActivity.SELECT_LOCATION_FROM;

        if (selectDestinationFrom.equals("fromId") && selectLocationFrom.equals("fromQRCode")) {
            Path path = new Path(coordsOfEntrance.get(0), coordsOfDestinationId.get(0));
            shortestPath = path.createPath();
        }
        if (selectDestinationFrom.equals("fromId") && selectLocationFrom.equals("fromMenu")) {
            Path path = new Path(coordsOfCurrentPos.get(0), coordsOfDestinationId.get(0));
            shortestPath = path.createPath();
        }
        if (selectDestinationFrom.equals("fromMenu") && selectLocationFrom.equals("fromQRCode")) {
            String from = getIntent().getExtras().getString(DestinationActivity.FROM);
            LocationFactory locationFactory = new LocationFactory();
            Location point = locationFactory.getLocation("DESTINATION", from);
            coordsOfDestination = point.getCoordinates();
            Path path = new Path(coordsOfEntrance.get(0), coordsOfDestination.get(0));
            shortestPath = path.createPath();
        }
        if (selectDestinationFrom.equals("fromMenu") && selectLocationFrom.equals("fromMenu")) {
            String from = getIntent().getExtras().getString(DestinationActivity.FROM);
            LocationFactory locationFactory = new LocationFactory();
            Location point = locationFactory.getLocation("DESTINATION", from);
            coordsOfDestination = point.getCoordinates();
            Path path = new Path(coordsOfCurrentPos.get(0), coordsOfDestination.get(0));
            shortestPath = path.createPath();
        }

        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        notebookRef = fStore.collection("beaconInfo");

        // access in the device's sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        setContentView(R.layout.activity_arnavigation);
        setARFragment();
    }

    private void setARFragment() {
        mARFragment = (CloudAnchorFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().

        create3dModel();
        mARFragment.getArSceneView().getScene().addOnUpdateListener(this::onSceneUpdate);

    }

    private void create3dModel() {
        ModelRenderable.builder()
                .setSource(this, Uri.parse("model.sfb"))
                .build()
                .thenAccept(renderable -> mObjRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast = Toast.makeText(this, "Unable to load obj renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        WorkRequest beaconWorker = new OneTimeWorkRequest.Builder(BeaconService.class).build();
        WorkManager.getInstance(getApplicationContext()).enqueue(beaconWorker);
    }

    @Override
    protected void onStop() {
        super.onStop();
        notebookListener.remove();
    }


    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this);
    }

    /**
     * Called when sensor values have changed
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

//        // get the angle around the z-axis rotated
//        float degreez = Math.round(event.values[0]);
//        // get the angle around the x-axis rotated
//        float degreex = Math.round(event.values[1]);
//        // get the angle around the y-axis rotated
//        float degreey = Math.round(event.values[2]);
//        ConstantsVariables.sensor = degreez;
//        //tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");
//
//        // create a rotation animation (reverse turn degree degrees)
//        RotateAnimation ra = new RotateAnimation(
//                currentDegree,
//                -degreez,
//                Animation.RELATIVE_TO_SELF, 0.5f,
//                Animation.RELATIVE_TO_SELF,
//                0.5f);
//
//        // how long the animation will take place
//        ra.setDuration(210);
//
//        // set the animation after the end of the reservation status
//        ra.setFillAfter(true);
//
//        // Start the animation
//        //image.startAnimation(ra);
//        //Log.i(TAG, String.valueOf(ra));
//        currentDegree = -degreez;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        }
        updateOrientationAngles();

    }


    /** Compute the three orientation angles based on the most recent readings from
     * the device's accelerometer and magnetometer.
     */
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        // "orientationAngles" now has up-to-date information.
    }


    /**
     * Called when the accuracy of a sensor has changed
     * @param sensor
     * @param accuracy
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }


    private void onSceneUpdate(FrameTime frameTime) {

        // Let the fragment update its state first.
        mARFragment.onUpdate(frameTime);

        // If there is no frame then don't process anything.
        if (mARFragment.getArSceneView().getArFrame() == null) {
            return;
        }

        // If ARCore is not tracking yet, then don't process anything.
        if (mARFragment.getArSceneView().getArFrame().getCamera().getTrackingState() != TrackingState.TRACKING) {
            return;
        }

        // Place the anchor 1m in front of the camera if anchorNode is null.
        if (this.mAnchorNode == null) {
            addModelToScene();
        }
//        else{
//            directUser();
//        }
    }

    /**
     * It called firstly to initialize the arrow in the correct position
     */
    private void addModelToScene() {
        Session session = mARFragment.getArSceneView().getSession();
        Pose pos = Objects.requireNonNull(mARFragment.getArSceneView().getArFrame()).getCamera().getPose().compose(Pose.makeTranslation(0, 0, -1));
        assert session != null;
        Anchor mAnchor;
        mAnchor = session.createAnchor(pos);
        mAnchorNode = new AnchorNode(mAnchor);
        mAnchorNode.setParent(mARFragment.getArSceneView().getScene());

        // Create the arrow node and add it to the anchor.
        arrow = new Node();
        Quaternion rotation1 = Quaternion.axisAngle(new Vector3(1.3f, 0.5f, 0.0f), 90); // rotate X axis 90 degrees
        Quaternion rotation2 = Quaternion.axisAngle(new Vector3(0.5f, -1.5f, 1.0f), 90); // rotate Y axis 90 degrees
        arrow.setLocalRotation(Quaternion.multiply(rotation1, rotation2));
        arrow.setParent(mAnchorNode);
        //arrow.setRenderable(mObjRenderable);

        updateLocationUser();
    }


    private void updateLocationUser() {
//        notebookListener = notebookRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
//            @Override
//            public void onEvent(@Nullable QuerySnapshot documentSnapshots, @Nullable FirebaseFirestoreException error) {
//                Log.i(TAG,"mphkeee");
//                if (error != null) {
//                    Toast.makeText(ARNavigation.this, "Error while loading!", Toast.LENGTH_SHORT).show();
//
//                } else {
//                    for (DocumentChange documentChange : documentSnapshots.getDocumentChanges()) {
//                        Long MinorType = (Long) documentChange.getDocument().getData().get("Minor");
//
//                        if (MinorType == ConstantsVariables.minor_437) {
//                            distance437List = (List<Double>) documentChange.getDocument().getData().get("Distance(m)");
//                            distance437 = distance437List.get(distance437List.size() - 1);
//                            distanceVector[0] = distance437 / 100000; //km to meters
//
//                        } else if (MinorType == ConstantsVariables.minor_570) {
//                            distance570List = (List<Double>) documentChange.getDocument().getData().get("Distance(m)");
//                            distance570 = distance570List.get(distance570List.size() - 1);
//                            distanceVector[1] = distance570 / 100000;
//                        } else if (MinorType == ConstantsVariables.minor_574) {
//                            distance574List = (List<Double>) documentChange.getDocument().getData().get("Distance(m)");
//                            distance574 = distance574List.get(distance574List.size() - 1);
//                            distanceVector[2] = distance574 / 100000;
//                        }
//                    }
//                }
//                Log.i(TAG, String.valueOf(distanceVector.length));
//                Log.i(TAG, "Distances: " + distance437 + distance570 + distance574);
//
//                //Trilateration
//                NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distanceVector), new LevenbergMarquardtOptimizer());
//                LeastSquaresOptimizer.Optimum optimum = solver.solve();
//                calculatedPosition = optimum.getPoint().toArray();
//
//                Log.d(TAG, String.valueOf(calculatedPosition[0]));
//                Log.d(TAG, String.valueOf(calculatedPosition[1]));
//                directUser();
//            }
//        });
//        double lat = Array.getDouble(calculatedPosition, 0);
//        double lon = Array.getDouble(calculatedPosition, 1);
//        LatLng position;
//        position = new LatLng(lat, lon);

        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
        List<BeaconInfo> beaconList = db.getAllBeacons();

        for (BeaconInfo beaconInfo : beaconList) {
            int MinorType = beaconInfo.getMinor();

            if (MinorType == ConstantsVariables.minor_437) {
                distance437 =  beaconInfo.getDistance();
                distanceVector[0] = distance437 / 100000; //km to meters

            } else if (MinorType == ConstantsVariables.minor_570) {
                distance570 = beaconInfo.getDistance();
                distanceVector[1] = distance570 / 100000;

            } else if (MinorType == ConstantsVariables.minor_574) {
                distance574 = beaconInfo.getDistance();
                distanceVector[2] = distance574 / 100000;
            }
        }
        Log.i(TAG, String.valueOf(distanceVector.length));
        Log.i(TAG, "Distances: " + distance437 + distance570 + distance574);

        //Trilateration
        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distanceVector), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();
        calculatedPosition = optimum.getPoint().toArray();

        directUser();
    }


    private void directUser() {
//
        Vector3 cameraPos = mARFragment.getArSceneView().getScene().getCamera().getWorldPosition();
        Vector3 cameraForward = mARFragment.getArSceneView().getScene().getCamera().getForward();
        Vector3 position = Vector3.add(cameraPos, cameraForward.scaled((float)Math.round(orientationAngles[2] * 10f) / 10f));
        Quaternion rotation = arrow.getLocalRotation();
//        Vector3 scale =  arrow.getWorldScale();
        arrow.setLocalPosition(position);
//        arrow.setLocalRotation(rotation);
//        arrow.setLocalScale(scale);
//        arrow.setParent(mAnchorNode);
//        arrow.setRenderable(mObjRenderable);


//            arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f), 45.0f)); // back
//            arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
//            arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left


        // ΜΠΛΕΚΑΣ ------ ΒΛΑΧΟΣ
        if (shortestPath.get(0) == 1 && shortestPath.get(1) == 2) {

            // forward - right
//            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(2);  // ΕΔΩ ΑΠΟΘΗΚΕΥΩ ΤΙΣ ΣΥΝΤΕΤΑΓΜΕΝΕΣ ΠΟΥ ΘΑ ΤΟΠΟΘΕΤΕΙΤΑΙ ΤΟ OBJECT. ΤΙΣ ΠΗΡΑ ΚΑΝΟΝΤΑΣ MAPPING ΣΤΗΝ ΕΙΚΟΝΑ.
//            for (int i = 0; i < 2; i++) {
//                pointsToBePlacedObject.add(new ArrayList<Integer>());
//            }
//            pointsToBePlacedObject.get(0).add(0);
//            pointsToBePlacedObject.get(0).add(0);
//            pointsToBePlacedObject.get(1).add(1);
//            pointsToBePlacedObject.get(1).add(1);

            // ΔΥΟ ΕΜΦΑΝΙΣΕΙΣ ΤΟΥ ΒΕΛΟΣ(ΕΥΘΕΙΑ, ΔΕΞΙΑ)
//            for (int i = 0; i < 2; i++) {
//
//                if (i == 0) {
//                    arrow.setLocalRotation(rotation); //forward
//                }
//                else {
//                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
//                }
//
//                arrow.setRenderable(mObjRenderable);

//                double[] currentPosition = userLocation.calculatedPosition;
//                while (previousUserLocation == currentPosition) { // αυτο γίνεται ώστε η επόμενη εμφανιση του object να γίνει αφού κινειθεί ο χρήστης, καθώς διαφορετικά ισώς να εμαφανίζονται διαδοχικά(γρηγορα) τα βέλοι, κάτι το οποίο δεν θέλουμε
//                    // wait
//                    currentPosition = userLocation.calculatedPosition;
//                }
//                if (Math.abs(currentPosition[0] - pointsToBePlacedObject.get(i).get(0)) > WRONG_DIRECTION && Math.abs(currentPosition[1] - pointsToBePlacedObject.get(i).get(1)) > WRONG_DIRECTION) {
//                    // ΕΜΦΑΝΙΣΕ ΚΑΤΙ ΠΟΥ ΝΑ ΤΟΥ ΕΞΗΓΕΙ ΟΤΙ ΚΙΝΕΙΤΑΙ ΛΑΘΟΣ
//                }
 //           }

//            while(calculatedPosition[0] < 500) {
//                arrow.setLocalRotation(rotation); //forward
//                arrow.setRenderable(mObjRenderable);
//
//            }





//            if (DestinationActivity.SELECT_DESTINATION_FROM.equals("fromMenu")) { // ΣΗΜΕΙΩΣΗ : ΟΙ ΣΥΝΤΕΤΑΓΜΕΝΕΣ ΠΟΥ ΑΠΟΘΗΚΕΥΟΥΝ ΤΑ coordsOfDestination/coordsOfDestinationId ΘΑ ΑΛΛΑΞΟΥΝ ΩΣΤΕ ΝΑ ΑΠΟΘΗΚΕΥΟΥΝ ΜΟΝΟ ΕΝΑ ΣΗΜΕΙΟ, ΑΥΤΟ ΕΞΩ ΑΚΡΙΒΩΣ ΑΠΟ ΤΗΝ ΠΟΡΤΑ ΤΟΥ ΠΡΟΟΡΙΣΜΟΥ ΚΑΙ ΟΧΙ ΠΟΛΥΓΩΝΟ ΟΠΩΣ ΑΠΟΘΗΚΕΥΟΥΝ ΤΩΡΑ. ΣΥΜΦΩΝΕΙΣ?
//                if (Math.abs(coordsOfDestination.get(1) - currentPosition[0]) < CHECK_FOR_ARRIVAL && Math.abs(coordsOfDestination.get(2) - currentPosition[1]) < CHECK_FOR_ARRIVAL) {
//                    // ARObject pin point
//                }
//            }
//            else {
//                if (Math.abs(coordsOfDestinationId.get(1) - currentPosition[0]) < CHECK_FOR_ARRIVAL && Math.abs(coordsOfDestinationId.get(2) - currentPosition[1]) < CHECK_FOR_ARRIVAL) {
//                    // ARObject pin point
//                }
//            }
        }
        else if (shortestPath.get(0) == 2 && shortestPath.get(1) == 1) {

            // left - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(390);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(370);
            pointsToBePlacedObject.get(1).add(229);
            for (int i = 0; i < 2; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 0) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);

            }

        }
        else if (shortestPath.get(0) == 1 && shortestPath.get(1) == 3) {

            // forward - forward - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(370);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(390);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(416);
            pointsToBePlacedObject.get(2).add(229);
            for (int i = 0; i < 3; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                arrow.setLocalRotation(rotation); //forward
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 3 && shortestPath.get(1) == 1) {

            // forward - forward - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(416);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(390);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(370);
            pointsToBePlacedObject.get(2).add(229);
            for (int i = 0; i < 3; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                arrow.setLocalRotation(rotation); //forward
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 1 && shortestPath.get(1) == 4) {

            // forward - left - forward - right - forward - .. forward - right - forward - right - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(19);
            for (int i = 0; i < 19; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(370);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(390);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            pointsToBePlacedObject.get(18).add(908);
            pointsToBePlacedObject.get(18).add(229);
            for (int i = 0; i < 19; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 1) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else if (i == 3 || i == 15 || i == 17) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 4 && shortestPath.get(1) == 1) {

            // forward - left - forward - left - forward - .. forward - left - forward - right - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(19);
            for (int i = 0; i < 19; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(370);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(390);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            pointsToBePlacedObject.get(18).add(908);
            pointsToBePlacedObject.get(18).add(229);
            for (int i = 18; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 1) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else if (i == 3 || i == 15 || i == 17) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }


        else if (shortestPath.get(0) == 1 && shortestPath.get(1) == 5) {

            // forward - left - forward - right - forward - .. forward - right - forward - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(18);
            for (int i = 0; i < 18; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(370);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(390);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            for (int i = 0; i < 18; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 1) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else if (i == 3 || i == 15 ) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }


        }
        else if (shortestPath.get(0) == 5 && shortestPath.get(1) == 1) {

            // forward - forward - left - forward - .. forward - left - forward - right - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(18);
            for (int i = 0; i < 18; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(370);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(390);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            for (int i = 17; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 1) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else if (i == 3 || i == 15 ) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 1 && shortestPath.get(1) == 6) {

            // forward - left - forward - right - forward - .. forward - right - forward - left - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(19);
            for (int i = 0; i < 19; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(370);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(390);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            pointsToBePlacedObject.get(18).add(908);
            pointsToBePlacedObject.get(18).add(229);
            for (int i = 0; i < 19; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 1 || i == 17) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else if (i == 3 || i == 15) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 6 && shortestPath.get(1) == 1) {

            // forward - right - forward - left - forward - .. forward - left - forward - right - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(19);
            for (int i = 0; i < 19; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(370);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(390);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            pointsToBePlacedObject.get(18).add(908);
            pointsToBePlacedObject.get(18).add(229);
            for (int i = 18; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 1 || i == 17) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else if (i == 3 || i == 15) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }


        }
        else if (shortestPath.get(0) == 2 && shortestPath.get(1) == 3) {

            // right - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(392);
            pointsToBePlacedObject.get(0).add(228);
            pointsToBePlacedObject.get(1).add(413);
            pointsToBePlacedObject.get(1).add(229);
            for (int i = 0; i < 2; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 0) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);

            }

        }
        else if (shortestPath.get(0) == 3 && shortestPath.get(1) == 2) {

            // forward - left
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(392);
            pointsToBePlacedObject.get(0).add(228);
            pointsToBePlacedObject.get(1).add(413);
            pointsToBePlacedObject.get(1).add(229);
            for (int i = 1; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 0) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);

            }

        }
        else if (shortestPath.get(0) == 2 && shortestPath.get(1) == 4) {

            // forward - forward - right - forward - .. forward - right - forward - right - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(18);
            for (int i = 0; i < 18; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(390);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(391);
            pointsToBePlacedObject.get(1).add(218);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(199);
            pointsToBePlacedObject.get(3).add(426);
            pointsToBePlacedObject.get(3).add(200);
            pointsToBePlacedObject.get(4).add(463);
            pointsToBePlacedObject.get(4).add(201);
            pointsToBePlacedObject.get(5).add(511);
            pointsToBePlacedObject.get(5).add(199);
            pointsToBePlacedObject.get(6).add(550);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(600);
            pointsToBePlacedObject.get(7).add(197);
            pointsToBePlacedObject.get(8).add(653);
            pointsToBePlacedObject.get(8).add(194);
            pointsToBePlacedObject.get(9).add(690);
            pointsToBePlacedObject.get(9).add(191);
            pointsToBePlacedObject.get(10).add(740);
            pointsToBePlacedObject.get(10).add(194);
            pointsToBePlacedObject.get(11).add(786);
            pointsToBePlacedObject.get(11).add(195);
            pointsToBePlacedObject.get(12).add(831);
            pointsToBePlacedObject.get(12).add(199);
            pointsToBePlacedObject.get(13).add(889);
            pointsToBePlacedObject.get(13).add(202);
            pointsToBePlacedObject.get(14).add(928);
            pointsToBePlacedObject.get(14).add(199);
            pointsToBePlacedObject.get(15).add(929);
            pointsToBePlacedObject.get(15).add(217);
            pointsToBePlacedObject.get(16).add(928);
            pointsToBePlacedObject.get(16).add(229);
            pointsToBePlacedObject.get(17).add(908);
            pointsToBePlacedObject.get(17).add(229);
            for (int i = 0; i < 18; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 2 || i == 14 || i == 16) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }



        }
        else if (shortestPath.get(0) == 4 && shortestPath.get(1) == 2) {

            // forward - left - forward - left - forward -  .. forward - left - forward  - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(18);
            for (int i = 0; i < 18; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(390);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(391);
            pointsToBePlacedObject.get(1).add(218);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(199);
            pointsToBePlacedObject.get(3).add(426);
            pointsToBePlacedObject.get(3).add(200);
            pointsToBePlacedObject.get(4).add(463);
            pointsToBePlacedObject.get(4).add(201);
            pointsToBePlacedObject.get(5).add(511);
            pointsToBePlacedObject.get(5).add(199);
            pointsToBePlacedObject.get(6).add(550);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(600);
            pointsToBePlacedObject.get(7).add(197);
            pointsToBePlacedObject.get(8).add(653);
            pointsToBePlacedObject.get(8).add(194);
            pointsToBePlacedObject.get(9).add(690);
            pointsToBePlacedObject.get(9).add(191);
            pointsToBePlacedObject.get(10).add(740);
            pointsToBePlacedObject.get(10).add(194);
            pointsToBePlacedObject.get(11).add(786);
            pointsToBePlacedObject.get(11).add(195);
            pointsToBePlacedObject.get(12).add(831);
            pointsToBePlacedObject.get(12).add(199);
            pointsToBePlacedObject.get(13).add(889);
            pointsToBePlacedObject.get(13).add(202);
            pointsToBePlacedObject.get(14).add(928);
            pointsToBePlacedObject.get(14).add(199);
            pointsToBePlacedObject.get(15).add(929);
            pointsToBePlacedObject.get(15).add(217);
            pointsToBePlacedObject.get(16).add(928);
            pointsToBePlacedObject.get(16).add(229);
            pointsToBePlacedObject.get(17).add(908);
            pointsToBePlacedObject.get(17).add(229);
            for (int i = 17; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 2 || i == 14 || i == 16) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 2 && shortestPath.get(1) == 5) {

            // forward - forward - right - forward - .. forward - right - forward - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(17);
            for (int i = 0; i < 17; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(390);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(391);
            pointsToBePlacedObject.get(1).add(218);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(199);
            pointsToBePlacedObject.get(3).add(426);
            pointsToBePlacedObject.get(3).add(200);
            pointsToBePlacedObject.get(4).add(463);
            pointsToBePlacedObject.get(4).add(201);
            pointsToBePlacedObject.get(5).add(511);
            pointsToBePlacedObject.get(5).add(199);
            pointsToBePlacedObject.get(6).add(550);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(600);
            pointsToBePlacedObject.get(7).add(197);
            pointsToBePlacedObject.get(8).add(653);
            pointsToBePlacedObject.get(8).add(194);
            pointsToBePlacedObject.get(9).add(690);
            pointsToBePlacedObject.get(9).add(191);
            pointsToBePlacedObject.get(10).add(740);
            pointsToBePlacedObject.get(10).add(194);
            pointsToBePlacedObject.get(11).add(786);
            pointsToBePlacedObject.get(11).add(195);
            pointsToBePlacedObject.get(12).add(831);
            pointsToBePlacedObject.get(12).add(199);
            pointsToBePlacedObject.get(13).add(889);
            pointsToBePlacedObject.get(13).add(202);
            pointsToBePlacedObject.get(14).add(928);
            pointsToBePlacedObject.get(14).add(199);
            pointsToBePlacedObject.get(15).add(929);
            pointsToBePlacedObject.get(15).add(217);
            pointsToBePlacedObject.get(16).add(928);
            pointsToBePlacedObject.get(16).add(229);
            for (int i = 0; i < 17; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 2 || i == 14 ) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }


        }
        else if (shortestPath.get(0) == 5 && shortestPath.get(1) == 2) {

            // forward - forward - left - forward - .. forward - left - forward - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(17);
            for (int i = 0; i < 17; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(390);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(391);
            pointsToBePlacedObject.get(1).add(218);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(199);
            pointsToBePlacedObject.get(3).add(426);
            pointsToBePlacedObject.get(3).add(200);
            pointsToBePlacedObject.get(4).add(463);
            pointsToBePlacedObject.get(4).add(201);
            pointsToBePlacedObject.get(5).add(511);
            pointsToBePlacedObject.get(5).add(199);
            pointsToBePlacedObject.get(6).add(550);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(600);
            pointsToBePlacedObject.get(7).add(197);
            pointsToBePlacedObject.get(8).add(653);
            pointsToBePlacedObject.get(8).add(194);
            pointsToBePlacedObject.get(9).add(690);
            pointsToBePlacedObject.get(9).add(191);
            pointsToBePlacedObject.get(10).add(740);
            pointsToBePlacedObject.get(10).add(194);
            pointsToBePlacedObject.get(11).add(786);
            pointsToBePlacedObject.get(11).add(195);
            pointsToBePlacedObject.get(12).add(831);
            pointsToBePlacedObject.get(12).add(199);
            pointsToBePlacedObject.get(13).add(889);
            pointsToBePlacedObject.get(13).add(202);
            pointsToBePlacedObject.get(14).add(928);
            pointsToBePlacedObject.get(14).add(199);
            pointsToBePlacedObject.get(15).add(929);
            pointsToBePlacedObject.get(15).add(217);
            pointsToBePlacedObject.get(16).add(928);
            pointsToBePlacedObject.get(16).add(229);
            for (int i = 16; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 2 || i == 14 ) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 2 && shortestPath.get(1) == 6) {

            // forward - forward - right - forward - .. forward - right - forward - left - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(18);
            for (int i = 0; i < 18; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(390);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(391);
            pointsToBePlacedObject.get(1).add(218);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(199);
            pointsToBePlacedObject.get(3).add(426);
            pointsToBePlacedObject.get(3).add(200);
            pointsToBePlacedObject.get(4).add(463);
            pointsToBePlacedObject.get(4).add(201);
            pointsToBePlacedObject.get(5).add(511);
            pointsToBePlacedObject.get(5).add(199);
            pointsToBePlacedObject.get(6).add(550);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(600);
            pointsToBePlacedObject.get(7).add(197);
            pointsToBePlacedObject.get(8).add(653);
            pointsToBePlacedObject.get(8).add(194);
            pointsToBePlacedObject.get(9).add(690);
            pointsToBePlacedObject.get(9).add(191);
            pointsToBePlacedObject.get(10).add(740);
            pointsToBePlacedObject.get(10).add(194);
            pointsToBePlacedObject.get(11).add(786);
            pointsToBePlacedObject.get(11).add(195);
            pointsToBePlacedObject.get(12).add(831);
            pointsToBePlacedObject.get(12).add(199);
            pointsToBePlacedObject.get(13).add(889);
            pointsToBePlacedObject.get(13).add(202);
            pointsToBePlacedObject.get(14).add(928);
            pointsToBePlacedObject.get(14).add(199);
            pointsToBePlacedObject.get(15).add(929);
            pointsToBePlacedObject.get(15).add(217);
            pointsToBePlacedObject.get(16).add(928);
            pointsToBePlacedObject.get(16).add(229);
            pointsToBePlacedObject.get(17).add(908);
            pointsToBePlacedObject.get(17).add(229);
            for (int i = 0; i < 18; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 16) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else if (i == 2 || i == 14) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 6 && shortestPath.get(1) == 2) {

            // forward - right - forward - left - forward - .. forward - left - forward - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(18);
            for (int i = 0; i < 18; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(390);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(391);
            pointsToBePlacedObject.get(1).add(218);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(199);
            pointsToBePlacedObject.get(3).add(426);
            pointsToBePlacedObject.get(3).add(200);
            pointsToBePlacedObject.get(4).add(463);
            pointsToBePlacedObject.get(4).add(201);
            pointsToBePlacedObject.get(5).add(511);
            pointsToBePlacedObject.get(5).add(199);
            pointsToBePlacedObject.get(6).add(550);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(600);
            pointsToBePlacedObject.get(7).add(197);
            pointsToBePlacedObject.get(8).add(653);
            pointsToBePlacedObject.get(8).add(194);
            pointsToBePlacedObject.get(9).add(690);
            pointsToBePlacedObject.get(9).add(191);
            pointsToBePlacedObject.get(10).add(740);
            pointsToBePlacedObject.get(10).add(194);
            pointsToBePlacedObject.get(11).add(786);
            pointsToBePlacedObject.get(11).add(195);
            pointsToBePlacedObject.get(12).add(831);
            pointsToBePlacedObject.get(12).add(199);
            pointsToBePlacedObject.get(13).add(889);
            pointsToBePlacedObject.get(13).add(202);
            pointsToBePlacedObject.get(14).add(928);
            pointsToBePlacedObject.get(14).add(199);
            pointsToBePlacedObject.get(15).add(929);
            pointsToBePlacedObject.get(15).add(217);
            pointsToBePlacedObject.get(16).add(928);
            pointsToBePlacedObject.get(16).add(229);
            pointsToBePlacedObject.get(17).add(908);
            pointsToBePlacedObject.get(17).add(229);
            for (int i = 17; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 16) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else if (i == 2 || i == 14) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 3 && shortestPath.get(1) == 4) {

            // forward - right - forward - right - forward - .. forward - right - forward - right - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(19);
            for (int i = 0; i < 19; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(392);
            pointsToBePlacedObject.get(0).add(228);
            pointsToBePlacedObject.get(1).add(413);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            pointsToBePlacedObject.get(18).add(908);
            pointsToBePlacedObject.get(18).add(229);
            for (int i = 0; i < 19; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 1 || i == 3 || i == 15 || i == 17) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 4 && shortestPath.get(1) == 3) {

            // forward - left - forward - left - forward - .. forward - left - forward - left - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(19);
            for (int i = 0; i < 19; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(392);
            pointsToBePlacedObject.get(0).add(228);
            pointsToBePlacedObject.get(1).add(413);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            pointsToBePlacedObject.get(18).add(908);
            pointsToBePlacedObject.get(18).add(229);
            for (int i = 18; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 1 || i == 3 || i == 15 || i == 17) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 3 && shortestPath.get(1) == 5) {

            // forward - right - forward - right - forward - .. forward - right - forward - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(18);
            for (int i = 0; i < 18; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(392);
            pointsToBePlacedObject.get(0).add(228);
            pointsToBePlacedObject.get(1).add(413);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            for (int i = 0; i < 18; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 1 || i == 3 || i == 15) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 5 && shortestPath.get(1) == 3) {

            // forward - forward - left - forward - .. forward - left - forward - left - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(18);
            for (int i = 0; i < 18; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(392);
            pointsToBePlacedObject.get(0).add(228);
            pointsToBePlacedObject.get(1).add(413);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            for (int i = 17 ; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 1 || i == 3 || i == 15) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 3 && shortestPath.get(1) == 6) {

            // forward - right - forward - right - forward - .. forward - right - forward - left - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(19);
            for (int i = 0; i < 19; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(413);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(390);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            pointsToBePlacedObject.get(18).add(908);
            pointsToBePlacedObject.get(18).add(229);
            for (int i = 0; i < 19; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 17) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else if (i == 1 || i == 3 || i == 15) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 6 && shortestPath.get(1) == 3) {

            // forward - right - forward - left - forward - .. forward - left - forward - left - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(19);
            for (int i = 0; i < 19; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(413);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(390);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(391);
            pointsToBePlacedObject.get(2).add(218);
            pointsToBePlacedObject.get(3).add(391);
            pointsToBePlacedObject.get(3).add(199);
            pointsToBePlacedObject.get(4).add(426);
            pointsToBePlacedObject.get(4).add(200);
            pointsToBePlacedObject.get(5).add(463);
            pointsToBePlacedObject.get(5).add(201);
            pointsToBePlacedObject.get(6).add(511);
            pointsToBePlacedObject.get(6).add(199);
            pointsToBePlacedObject.get(7).add(550);
            pointsToBePlacedObject.get(7).add(199);
            pointsToBePlacedObject.get(8).add(600);
            pointsToBePlacedObject.get(8).add(197);
            pointsToBePlacedObject.get(9).add(653);
            pointsToBePlacedObject.get(9).add(194);
            pointsToBePlacedObject.get(10).add(690);
            pointsToBePlacedObject.get(10).add(191);
            pointsToBePlacedObject.get(11).add(740);
            pointsToBePlacedObject.get(11).add(194);
            pointsToBePlacedObject.get(12).add(786);
            pointsToBePlacedObject.get(12).add(195);
            pointsToBePlacedObject.get(13).add(831);
            pointsToBePlacedObject.get(13).add(199);
            pointsToBePlacedObject.get(14).add(889);
            pointsToBePlacedObject.get(14).add(202);
            pointsToBePlacedObject.get(15).add(928);
            pointsToBePlacedObject.get(15).add(199);
            pointsToBePlacedObject.get(16).add(929);
            pointsToBePlacedObject.get(16).add(217);
            pointsToBePlacedObject.get(17).add(928);
            pointsToBePlacedObject.get(17).add(229);
            pointsToBePlacedObject.get(18).add(908);
            pointsToBePlacedObject.get(18).add(229);
            for (int i = 18; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 17) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else if (i == 1 || i == 3 || i == 15) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else if (shortestPath.get(0) == 4 && shortestPath.get(1) == 5) {

            // forward - right
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(909);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(928);
            pointsToBePlacedObject.get(1).add(229);
            for (int i = 0; i < 2; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 0) {
                    arrow.setLocalRotation(rotation); //forward
                }
                else {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);

            }

        }
        else if (shortestPath.get(0) == 5 && shortestPath.get(1) == 4) {

            // left - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(909);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(928);
            pointsToBePlacedObject.get(1).add(229);
            for (int i = 1; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 0) {
                    arrow.setLocalRotation(rotation); //forward
                }
                else {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);

            }

        }
        else if (shortestPath.get(0) == 4 && shortestPath.get(1) == 6) {

            // forward - forward - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(909);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(928);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(955);
            pointsToBePlacedObject.get(2).add(229);
            for (int i = 0; i < 3; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                arrow.setLocalRotation(rotation); //forward
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }


        }
        else if (shortestPath.get(0) == 6 && shortestPath.get(1) == 4) {

            // forward - forward - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(909);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(928);
            pointsToBePlacedObject.get(1).add(229);
            pointsToBePlacedObject.get(2).add(955);
            pointsToBePlacedObject.get(2).add(229);
            for (int i = 2; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                arrow.setLocalRotation(rotation); //forward
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }


        }
        else if (shortestPath.get(0) == 5 && shortestPath.get(1) == 6) {

            // right - forward
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(928);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(955);
            pointsToBePlacedObject.get(1).add(229);
            for (int i = 0; i < 2; i++) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 0) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }
        else {   // 6-5 path

            // forward - left
            ArrayList<ArrayList<Integer>> pointsToBePlacedObject = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                pointsToBePlacedObject.add(new ArrayList<Integer>());
            }
            pointsToBePlacedObject.get(0).add(928);
            pointsToBePlacedObject.get(0).add(229);
            pointsToBePlacedObject.get(1).add(955);
            pointsToBePlacedObject.get(1).add(229);
            for (int i = 1; i >= 0; i--) {
                arrow.setLocalPosition(new Vector3(pointsToBePlacedObject.get(i).get(0),pointsToBePlacedObject.get(i).get(1), (float)Math.round(orientationAngles[2] * 10f) / 10f));
                if (i == 0) {
                    arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
                }
                else {
                    arrow.setLocalRotation(rotation); //forward
                }
                arrow.setParent(mAnchorNode);
                arrow.setRenderable(mObjRenderable);
            }

        }

    }



    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {

        String openGlVersionString =
                ((ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        return true;
    }

}
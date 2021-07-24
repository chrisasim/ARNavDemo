package com.example.arnavdemo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.arnavdemo.mapping.Location;
import com.example.arnavdemo.mapping.LocationFactory;
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

import java.util.ArrayList;

public class ARNavigation extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArrayList<Integer> shortestPath = new ArrayList<>();

    private CloudAnchorFragment mARFragment;
    private ModelRenderable mObjRenderable;
    private Anchor mAnchor = null;
    private AnchorNode mAnchorNode = null;
    private Node arrow;

   // private float currentDegree = 0f;
    private SensorManager sensorManager;

    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        ArrayList<Integer> coordsOfCurrentPos = getIntent().getIntegerArrayListExtra(DestinationActivity.COORDS_OF_CURRENT_POS);
        ArrayList<Integer> coordsOfDestinationId = getIntent().getIntegerArrayListExtra(DestinationActivity.COORDS_OF_DESTINATION_ID);
        ArrayList<Integer> coordsOfEntrance = getIntent().getIntegerArrayListExtra(DestinationActivity.COORDS_OF_ENTRANCE);
        String selectDestinationFrom = DestinationActivity.SELECT_DESTINATION_FROM;
        String selectLocationFrom = CurrentLocationActivity.SELECT_LOCATION_FROM;

        if (selectDestinationFrom == "fromId" && selectLocationFrom == "fromQRCode") {
            Path path = new Path(coordsOfEntrance.get(0), coordsOfDestinationId.get(0));
            shortestPath = path.createPath();
        }
        if (selectDestinationFrom == "fromId" && selectLocationFrom == "fromMenu") {
            Path path = new Path(coordsOfCurrentPos.get(0), coordsOfDestinationId.get(0));
            shortestPath = path.createPath();
        }
        if (selectDestinationFrom == "fromMenu" && selectLocationFrom == "fromQRCode") {
            String from = getIntent().getExtras().getString(DestinationActivity.FROM);
            LocationFactory locationFactory = new LocationFactory();
            Location point = locationFactory.getLocation("DESTINATION", from);
            ArrayList<Integer> coordsOfDestination = point.getCoordinates();
            Path path = new Path(coordsOfEntrance.get(0), coordsOfDestination.get(0));
            shortestPath = path.createPath();
        }
        if (selectDestinationFrom == "fromMenu" && selectLocationFrom == "fromMenu") {
            String from = getIntent().getExtras().getString(DestinationActivity.FROM);
            LocationFactory locationFactory = new LocationFactory();
            Location point = locationFactory.getLocation("DESTINATION", from);
            ArrayList<Integer> coordsOfDestination = point.getCoordinates();
            Path path = new Path(coordsOfCurrentPos.get(0), coordsOfDestination.get(0));
            shortestPath = path.createPath();
        }

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
        else{
            Vector3 cameraPos = mARFragment.getArSceneView().getScene().getCamera().getWorldPosition();
            Vector3 cameraForward = mARFragment.getArSceneView().getScene().getCamera().getForward();
            Vector3 position = Vector3.add(cameraPos, cameraForward.scaled(orientationAngles[2]));
            Quaternion rotation = arrow.getLocalRotation();
            Vector3 scale =  arrow.getWorldScale();
            arrow.setLocalPosition(position);
            arrow.setLocalRotation(rotation);
            arrow.setLocalScale(scale);
            arrow.setParent(mAnchorNode);
            arrow.setRenderable(mObjRenderable);


//            arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f), 45.0f)); // back
//            arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, 2.0f, 0.0f), 45.0f)); // right
//            arrow.setWorldRotation(Quaternion.axisAngle(new Vector3(0.0f, -2.3f, 0.0f), 45.0f)); //left
        }
    }

    /**
     * It called firstly to initialize the arrow in the correct position
     */
    private void addModelToScene() {
        Session session = mARFragment.getArSceneView().getSession();
        Pose pos = mARFragment.getArSceneView().getArFrame().getCamera().getPose().compose(Pose.makeTranslation(0, 0, -1));
        mAnchor = session.createAnchor(pos);
        mAnchorNode = new AnchorNode(mAnchor);
        mAnchorNode.setParent(mARFragment.getArSceneView().getScene());

        // Create the arrow node and add it to the anchor.
        arrow = new Node();
        Quaternion rotation1 = Quaternion.axisAngle(new Vector3(1.3f, 0.5f, 0.0f), 90); // rotate X axis 90 degrees
        Quaternion rotation2 = Quaternion.axisAngle(new Vector3(0.5f, -1.5f, 1.0f), 90); // rotate Y axis 90 degrees
        arrow.setLocalRotation(Quaternion.multiply(rotation1, rotation2));
        arrow.setParent(mAnchorNode);
        arrow.setRenderable(mObjRenderable);
    }


    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
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

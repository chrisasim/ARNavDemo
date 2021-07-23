package com.example.arnavdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.GeomagneticField;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

import com.example.arnavdemo.mapping.Location;
import com.example.arnavdemo.mapping.LocationFactory;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.Arrays;

public class ARNavigation extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private ArrayList<Integer> shortestPath = new ArrayList<>();

    private CloudAnchorFragment mARFragment;
    //private ArFragment mARFragment;

    private ModelRenderable mObjRenderable;
    private Anchor mAnchor = null;
    private TransformableNode mARObject = null;
    private AnchorNode mAnchorNode = null;

   // private float currentDegree = 0f;
    private SensorManager sensorManager;
    private Sensor mRotationVectorSensor;
    private final float[] mRotationMatrix = new float[16];
    private float[] mOrientValues = new float[3];

  //  private final float[] accelerometerReading = new float[3];
   // private final float[] magnetometerReading = new float[3];
//
//    private float[] rotationMatrix = new float[9];
//    private final float[] orientationAngles = new float[3];
//    private float[] outRotationMatrix;


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

        Log.i(TAG, String.valueOf(shortestPath));

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

        /*
        Node node = new Node();
        node.setParent(mARFragment.getArSceneView().getScene());
        node.setRenderable(mObjRenderable);
        mARFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (mObjRenderable == null) {
                        return;
                    }
                    // Create the Anchor.
                    mAnchor = hitResult.createAnchor();
                    mAnchorNode = new AnchorNode(mAnchor);
                    mAnchorNode.setParent(mARFragment.getArSceneView().getScene());
                    // Create the transformable object and add it to the anchor.
                    mARObject = new TransformableNode(mARFragment.getTransformationSystem());
                    // Set the min and max scales of the ScaleController.
                    // Default min is 0.75, default max is 1.75.
                    mARObject.getScaleController().setMinScale(0.1f);
                    mARObject.getScaleController().setMaxScale(2.0f);
                    // Set the local scale of the node BEFORE setting its parent
                    mARObject.setLocalScale(new Vector3(0.2f, 0.2f, 0.2f));
                    mARObject.setParent(mAnchorNode);
                    mARObject.setRenderable(mObjRenderable);
                    mARObject.select();
                });
                */
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
//        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
//        if (accelerometer != null) {
//            sensorManager.registerListener(this, accelerometer,
//                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
//        }
//        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        if (magneticField != null) {
//            sensorManager.registerListener(this, magneticField,
//                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
//        }

        // find the rotation-vector sensor
        mRotationVectorSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR);
        if (mRotationVectorSensor != null) {
            sensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
//        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            System.arraycopy(event.values, 0, accelerometerReading,
//                    0, accelerometerReading.length);
//            calculateAccMagOrientation();
//        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//            System.arraycopy(event.values, 0, magnetometerReading,
//                    0, magnetometerReading.length);
//        }

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(
                    mRotationMatrix , event.values);
            SensorManager.getOrientation(mRotationMatrix, mOrientValues);
            for(int i=0; i<3; i++)
                mOrientValues[i]=(float)
                        Math.toDegrees(mOrientValues[i])+180.0f;//orientation in degrees
        }
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


//    public void calculateAccMagOrientation() {
//        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading))
//            SensorManager.getOrientation(rotationMatrix, orientationAngles);
//        else { // Most chances are that there are no magnet datas
//            double gx, gy, gz;
//            gx = accelerometerReading[0] / 9.81f;
//            gy = accelerometerReading[1] / 9.81f;
//            gz = accelerometerReading[2] / 9.81f;
//            // http://theccontinuum.com/2012/09/24/arduino-imu-pitch-roll-from-accelerometer/
//            float pitch = (float) -Math.atan(gy / Math.sqrt(gx * gx + gz * gz));
//            float roll = (float) -Math.atan(gx / Math.sqrt(gy * gy + gz * gz));
//            float azimuth = 0; // Impossible to guess
//
//            orientationAngles[0] = azimuth;
//            orientationAngles[1] = pitch;
//            orientationAngles[2] = roll;
//            rotationMatrix = getRotationMatrixFromOrientation(orientationAngles);
//        }
//
//        // Create the arrow node and add it to the anchor.
//        Node arrow = new Node();
//        Quaternion rotation1 = Quaternion.axisAngle(new Vector3(1.3f, 0.5f, 0.0f), 90); // rotate X axis 90 degrees
//        Quaternion rotation2 = Quaternion.axisAngle(new Vector3(0.5f, -1.5f, 1.0f), 90); // rotate Y axis 90 degrees
//        arrow.setLocalRotation(Quaternion.multiply(rotation1, rotation2));
//        arrow.setParent(mAnchorNode);
//        arrow.setRenderable(mObjRenderable);
//    }
//    public static float[] getRotationMatrixFromOrientation(float[] o) {
//        float[] xM = new float[9];
//        float[] yM = new float[9];
//        float[] zM = new float[9];
//
//        float sinX = (float) Math.sin(o[1]);
//        float cosX = (float) Math.cos(o[1]);
//        float sinY = (float) Math.sin(o[2]);
//        float cosY = (float) Math.cos(o[2]);
//        float sinZ = (float) Math.sin(o[0]);
//        float cosZ = (float) Math.cos(o[0]);
//
//        // rotation about x-axis (pitch)
//        xM[0] = 1.0f;xM[1] = 0.0f;xM[2] = 0.0f;
//        xM[3] = 0.0f;xM[4] = cosX;xM[5] = sinX;
//        xM[6] = 0.0f;xM[7] =-sinX;xM[8] = cosX;
//
//        // rotation about y-axis (roll)
//        yM[0] = cosY;yM[1] = 0.0f;yM[2] = sinY;
//        yM[3] = 0.0f;yM[4] = 1.0f;yM[5] = 0.0f;
//        yM[6] =-sinY;yM[7] = 0.0f;yM[8] = cosY;
//
//        // rotation about z-axis (azimuth)
//        zM[0] = cosZ;zM[1] = sinZ;zM[2] = 0.0f;
//        zM[3] =-sinZ;zM[4] = cosZ;zM[5] = 0.0f;
//        zM[6] = 0.0f;zM[7] = 0.0f;zM[8] = 1.0f;
//
//        // rotation order is y, x, z (roll, pitch, azimuth)
//        float[] resultMatrix = matrixMultiplication(xM, yM);
//        resultMatrix = matrixMultiplication(zM, resultMatrix);
//        return resultMatrix;
//    }
//
//
//    public static float[] matrixMultiplication(float[] A, float[] B) {
//        float[] result = new float[9];
//
//        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
//        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
//        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
//
//        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
//        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
//        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
//
//        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
//        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
//        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
//
//        return result;
//    }



    private void onSceneUpdate(FrameTime frameTime) {

        Log.i(TAG, Arrays.toString(mOrientValues));
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
            //Get the direction of the mobile device according to Z-axis
           // Log.i(TAG, String.valueOf(ConstantsVariables.sensor));
            addModelToScene();
        }
    }

    private void addModelToScene() {
        Session session = mARFragment.getArSceneView().getSession();
        Pose pos = mARFragment.getArSceneView().getArFrame().getCamera().getPose().compose(Pose.makeTranslation(0, 0, -1));
        Anchor anchor = session.createAnchor(pos);
        mAnchorNode = new AnchorNode(anchor);
        mAnchorNode.setParent(mARFragment.getArSceneView().getScene());

        // Create the arrow node and add it to the anchor.
        Node arrow = new Node();
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

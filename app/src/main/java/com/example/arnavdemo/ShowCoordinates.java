package com.example.arnavdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
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

import static com.google.ar.core.ArCoreApk.InstallStatus.INSTALL_REQUESTED;

public class ShowCoordinates extends AppCompatActivity {

//    private TextView polygon;
//    private TextView point;
//    private TextView url;

    //beacon Service
    private FirebaseFirestore fStore;
    StorageReference storageReference;
    public static final String TAG = "Location";
    private CollectionReference notebookRef = fStore.collection("beaconInfo");
    private ListenerRegistration notebookListener;
    List<Double> distance437List, distance574List, distance570List= new ArrayList<>();
    double distance437=0, distance570=0, distance574 = 0;
    double[] distanceVector ={0,0,0}, calculatedPosition;
    double[][] positions = new double[][]{{390, 182},{664,218},{929,181}};
    public BeaconService beaconService;
    private boolean mUserRequestedInstall = true;
//    private Object CameraPermissionHelper;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_coordinates);
        onResume();

//        polygon = findViewById(R.id.txtPolygonCoords);
//        point = findViewById(R.id.txtPointCoords);
//        url = findViewById(R.id.txtUrl);

        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();


//        polygon.setText("Your destination keeps in polygon with coords: " + String.valueOf(getIntent().getIntegerArrayListExtra("polygonCoords")));
//        point.setText("Your current location is found: " + String.valueOf(getIntent().getIntegerArrayListExtra("pointCoords")));
//        url.setText("Your current location from qr code is found: " + getIntent().getStringExtra("urlCoords"));

    }

    @Override
    protected void onStart() {
        super.onStart();
        WorkRequest beaconWorker = new OneTimeWorkRequest.Builder(BeaconService.class).build();
        WorkManager.getInstance(getApplicationContext()).enqueue(beaconWorker);
        updateLocationUser();
    }

    @Override
    protected void onStop() {
        super.onStop();
        notebookListener.remove();
    }

    private void updateLocationUser() {
        notebookListener = notebookRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot documentSnapshots, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(ShowCoordinates.this, "Error while loading!", Toast.LENGTH_SHORT).show();

                } else {
                    for (DocumentChange documentChange : documentSnapshots.getDocumentChanges()) {
                        Long MinorType = (Long) documentChange.getDocument().getData().get("Minor");

                        if (MinorType == ConstantsVariables.minor_437) {
                            distance437List = (List<Double>) documentChange.getDocument().getData().get("Distance(m)");
                            distance437 = distance437List.get(distance437List.size() - 1);
                            distanceVector[0] = distance437 / 100000; //km to meters

                        } else if (MinorType == ConstantsVariables.minor_570) {
                            distance570List = (List<Double>) documentChange.getDocument().getData().get("Distance(m)");
                            distance570 = distance570List.get(distance570List.size() - 1);
                            distanceVector[1] = distance570 / 100000;
                        } else if (MinorType == ConstantsVariables.minor_574) {
                            distance574List = (List<Double>) documentChange.getDocument().getData().get("Distance(m)");
                            distance574 = distance574List.get(distance574List.size() - 1);
                            distanceVector[2] = distance574 / 100000;
                        }
                    }
                }
                Log.i(TAG, String.valueOf(distanceVector.length));
                Log.i(TAG, "Distances: " + distance437 + distance570 + distance574);

                //Trilateration
                NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distanceVector), new LevenbergMarquardtOptimizer());
                LeastSquaresOptimizer.Optimum optimum = solver.solve();
                calculatedPosition = optimum.getPoint().toArray();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permission to operate.
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }


        // Make sure Google Play Services for AR is installed and up to date.
        try {
            Session mSession = null;
            if (mSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    case INSTALLED:
                        // Success, create the AR session.
                        mSession = new Session(this);
                        break;
                    case INSTALL_REQUESTED:
                        // Ensures next invocation of requestInstall() will either return
                        // INSTALLED or throw an exception.
                        mUserRequestedInstall = false;
                        return;
                }
            }
        } catch (UnavailableUserDeclinedInstallationException | UnavailableDeviceNotCompatibleException e) {
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "TODO: handle exception " + e, Toast.LENGTH_LONG)
                    .show();
            return;
        } catch (UnavailableArcoreNotInstalledException e) {
            e.printStackTrace();
        } catch (UnavailableSdkTooOldException e) {
            e.printStackTrace();
        } catch (UnavailableApkTooOldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
}
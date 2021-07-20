package com.example.arnavdemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.type.LatLng;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.maps.model.*;


public class UserLocation extends AppCompatActivity {


    //beacon Service
    private FirebaseFirestore fStore;
    StorageReference storageReference;
    public static final String TAG = UserLocation.class.getSimpleName();
    CollectionReference notebookRef;
    private ListenerRegistration notebookListener;
    List<Double> distance437List, distance574List, distance570List= new ArrayList<>();
    double distance437=0, distance570=0, distance574 = 0;
    double[] distanceVector ={0,0,0}, calculatedPosition;
    double[][] positions = new double[][]{{390, 182},{664,218},{929,181}};
    public BeaconService beaconService;
    private boolean mUserRequestedInstall = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_coordinates);
        //onResume();
        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        notebookRef = fStore.collection("beaconInfo");


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
                    Toast.makeText(UserLocation.this, "Error while loading!", Toast.LENGTH_SHORT).show();

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
//        double lat = Array.getDouble(calculatedPosition, 0);
//        double lon = Array.getDouble(calculatedPosition, 1);
//        LatLng position;
//        position = new LatLng(lat, lon);
    }

}

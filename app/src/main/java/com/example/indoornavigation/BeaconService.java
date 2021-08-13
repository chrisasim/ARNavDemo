package com.example.indoornavigation;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BeaconService extends Worker {

    public static BluetoothAdapter mBluetoothAdapter;
    public BluetoothLeScanner mBluetoothLeScanner;
    public ScanSettings mScanSettings;
    public float Distance, DistanceSecondMethod;
    public static final String TAG = "Beacon Service";
    private static final double KALMAN_R = 0.125d;
    private static final double KALMAN_Q = 0.5d;
    private final KalmanFilter  kalmanFilter;



    public BeaconService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        kalmanFilter = new KalmanFilter(KALMAN_R, KALMAN_Q);

    }


    DatabaseHandler db = new DatabaseHandler(getApplicationContext());

    protected ScanCallback mScanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            final BluetoothDevice bluetoothDevice = result.getDevice();
            ScanRecord scanRecord = result.getScanRecord();
            assert scanRecord != null;
            List<ADStructure> structures = ADPayloadParser.getInstance().parse(scanRecord.getBytes());
            ConstantsVariables.activity_started_bluetooth = true;
            String deviceName = scanRecord.getDeviceName();

            if (deviceName != null) {
                for (ADStructure structure : structures) {
                    if (structure instanceof IBeacon) {
                        final IBeacon iBeacon = (IBeacon) structure;
                        //Distance = (float) calculateDistance(result.getRssi());
                        Distance = (float) calculateDistance(applyKalmanFilterToRssi(result.getRssi()));
                        //DistanceSecondMethod = (float) calculateDistanceSecondMethod(result.getRssi());
                        DistanceSecondMethod = (float) calculateDistanceSecondMethod(applyKalmanFilterToRssi(result.getRssi()));
                        if (db.getCount() == 0 || !db.checkIfSpecificMinorIsInDB(iBeacon.getMinor())) {
                            db.addBeacon(new BeaconInfo(iBeacon.getMinor(), Distance, result.getRssi()));
                        }
                        else {
                            BeaconInfo beaconInfo = db.getBeaconInfo(iBeacon.getMinor());
                            beaconInfo.setMinor(iBeacon.getMinor());
                            beaconInfo.setDistance(Distance);
                            beaconInfo.setRssi(result.getRssi());
                            db.updateBeacon(beaconInfo);
                        }

//                        final DocumentReference docref = FirebaseFirestore.getInstance().collection("beaconInfo").document(bluetoothDevice.getAddress());
//                        docref.get().addOnCompleteListener(task -> {
//                            Log.i(TAG, String.valueOf(task.getException()));
//                            if (task.isSuccessful()) {
//                                Log.i(TAG, "iffffff");
//                                DocumentSnapshot documentSnapshot = task.getResult();
//                                Distance = (float) calculateDistance(result.getRssi());
//                                DistanceSecondMethod = (float) calculateDistanceSecondMethod(result.getRssi());
//                                if (documentSnapshot != null && documentSnapshot.exists()) {
//                                    docref.update("RSSI", FieldValue.arrayUnion(result.getRssi()));
//                                    docref.update("Distance(m)", FieldValue.arrayUnion(Distance));
//                                } else {
//                                    Log.i(TAG, "elseee");
//                                    Map<String, Object> data = new HashMap<>();
//                                    data.put("Protocol", "iBeacon");
//                                    data.put("UUID", ConstantsVariables.uuid);
//                                    data.put("Major", iBeacon.getMajor());
//                                    data.put("Minor", iBeacon.getMinor());
//                                    data.put("MAC", bluetoothDevice.getAddress());
//                                    data.put("Device Name", bluetoothDevice.getName());
//                                    data.put("Class", bluetoothDevice.getBluetoothClass());
//                                    data.put("TxPower", result.getTxPower());
//                                    data.put("RSSI", FieldValue.arrayUnion(result.getRssi()));
//                                    data.put("Distance(m)", FieldValue.arrayUnion(Distance));
//                                    docref.set(data).addOnSuccessListener(new OnSuccessListener<Void>() {
//                                        @Override
//                                        public void onSuccess(Void aVoid) {
//                                            Log.d(TAG, "user profile is created for ");
//                                        }
//                                    }).addOnFailureListener(new OnFailureListener() {
//                                        @Override
//                                        public void onFailure(@NonNull Exception e) {
//                                            Log.d(TAG, "onFailure" + e.toString());
//                                        }
//                                    });
//                                }
//                            }
//                        });
                    }
                }
            }
        }
    };

    @NonNull
    @Override
    public Result doWork() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            System.out.println("Bluetooth not supported");
        } else {
            System.out.println("Bluetooth initialized");
        }
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            setScanSettings();
            mBluetoothLeScanner.startScan(null, mScanSettings, mScanCallback);
        }
        return Result.success();
    }
    
        // Method Apply Filter
    private double applyKalmanFilterToRssi(double rssi){
        double filterrssi = kalmanFilter.applyFilter(rssi);
        return filterrssi;
    }

    private void setScanSettings() {
        ScanSettings.Builder mBuilder = new ScanSettings.Builder();
        mBuilder.setReportDelay(0);
        mBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        mScanSettings = mBuilder.build();
    }

    public double calculateDistance(double rssi) {
        int txPower = -56;
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }
        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

    public double calculateDistanceSecondMethod(double rssi) {
        int txPower = -56;
        if (rssi==0) {
            return -1.0;
        }
        double exponent = (txPower-rssi)/(10*2);
        return Math.pow(10, exponent);
    }

}

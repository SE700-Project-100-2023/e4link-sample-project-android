package com.empatica.sample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;
import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApiCallbackProvider;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
import com.polar.sdk.api.errors.PolarInvalidArgument;
import com.polar.sdk.api.model.PolarDeviceInfo;
import com.polar.sdk.api.model.PolarHrBroadcastData;
import com.polar.sdk.api.model.PolarHrData;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;


public class MainActivity extends AppCompatActivity implements EmpaDataDelegate, EmpaStatusDelegate {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;


    private static final String EMPATICA_API_KEY = BuildConfig.EMPATICA_API_KEY;


    private EmpaDeviceManager deviceManager = null;

    private TextView accel_xLabel;

    private TextView accel_yLabel;

    private TextView accel_zLabel;

    private TextView bvpLabel;

    private TextView edaLabel;

    private TextView ibiLabel;

    private TextView temperatureLabel;

    private TextView batteryLabel;

    private TextView statusLabel;

    private TextView deviceNameLabel;

    private TextView polarConnectionStatusLabel;

    private TextView polarHrLabel;

    private TextView polarHrRrLabel;

    private LinearLayout dataCnt;

    private PolarBleApi polarApi;

    private Disposable polarBroadcastDisposable;
    private Disposable polarScanDisposable;
    private Disposable polarHrDisposable;
    private Boolean isPolarDeviceConnected = false;
    private String polarDeviceId = "C5040528";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

         polarApi = PolarBleApiDefaultImpl.defaultImplementation(getApplicationContext(), new HashSet<PolarBleApi.PolarBleSdkFeature>(Arrays.asList(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
        )));

        setContentView(R.layout.activity_main);

        // Initialize vars that reference UI components
        statusLabel = (TextView) findViewById(R.id.status);

        dataCnt = (LinearLayout) findViewById(R.id.dataArea);

        accel_xLabel = (TextView) findViewById(R.id.accel_x);

        accel_yLabel = (TextView) findViewById(R.id.accel_y);

        accel_zLabel = (TextView) findViewById(R.id.accel_z);

        bvpLabel = (TextView) findViewById(R.id.bvp);

        edaLabel = (TextView) findViewById(R.id.eda);

        ibiLabel = (TextView) findViewById(R.id.ibi);

        temperatureLabel = (TextView) findViewById(R.id.temperature);

        batteryLabel = (TextView) findViewById(R.id.battery);

        deviceNameLabel = (TextView) findViewById(R.id.deviceName);

        polarConnectionStatusLabel = (TextView) findViewById(R.id.polar_connection_status_label);

        polarHrLabel = (TextView) findViewById(R.id.polar_hr_label);

        polarHrRrLabel = (TextView) findViewById(R.id.polar_hr_rr_label);

        final Button disconnectButton = findViewById(R.id.disconnectButton);

        final Button polarBroadcastButton = findViewById(R.id.polarBroadcastButton);;
        final Button polarConnectButton = findViewById(R.id.polarConnectButton);;
        final Button polarScanButton = findViewById(R.id.polarScanButton);;
        final Button polarHrButton = findViewById(R.id.polarHrButton);;

        disconnectButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (deviceManager != null) {

                    deviceManager.disconnect();
                }
            }
        });

        polarBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // https://github.com/polarofficial/polar-ble-sdk/blob/master/examples/example-android/androidBleSdkTestApp/app/src/main/java/com/polar/androidblesdk/MainActivity.kt
                // Line 194
                if (MainActivity.this.polarBroadcastDisposable == null || MainActivity.this.polarBroadcastDisposable.isDisposed()) {
                    toggleButtonDown(polarBroadcastButton);
                    polarBroadcastDisposable = polarApi.startListenForPolarHrBroadcasts(null)
                            .subscribe(new Consumer<PolarHrBroadcastData>() {
                                @Override
                                public void accept(PolarHrBroadcastData polarHrBroadcastData) throws Throwable {
                                    Log.d(TAG, "HR BROADCAST " + polarHrBroadcastData.getPolarDeviceInfo().getDeviceId() + ";  HR " + polarHrBroadcastData.getHr() + ";  BATT " + polarHrBroadcastData.getBatteryStatus());
                                    updateLabel(polarHrLabel, "[" + polarHrBroadcastData.getPolarDeviceInfo().getDeviceId() + "] HR BROADCAST: " + polarHrBroadcastData.getHr());
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Throwable {
                                    toggleButtonUp(polarBroadcastButton);
                                    Log.e(TAG, "Broadcast listener failed. Reason " + throwable.getMessage());
                                }
                            }, new Action() {
                                @Override
                                public void run() throws Throwable {
                                    Log.d(TAG, "Polar Broadcast complete");
                                }
                            });
                } else {
                    toggleButtonUp(polarBroadcastButton);
                    polarBroadcastDisposable.dispose();
                }
            }
        });

        polarConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // https://github.com/polarofficial/polar-ble-sdk/blob/master/examples/example-android/androidBleSdkTestApp/app/src/main/java/com/polar/androidblesdk/MainActivity.kt
                // Line 215
                try {
                    if (isPolarDeviceConnected) {
                        polarApi.disconnectFromDevice(polarDeviceId);
                        updateLabel(polarConnectionStatusLabel, "Disconnected from: " + polarDeviceId);
                    } else {
                        polarApi.connectToDevice(polarDeviceId);
                        updateLabel(polarConnectionStatusLabel, "Connected to: " + polarDeviceId);
                    }
                } catch (PolarInvalidArgument polarInvalidArgument) {
                    String attemptType = isPolarDeviceConnected ? "disconnect" : "connect";
                    Log.e(TAG, "Failed to " + attemptType + ". Reason: " + polarInvalidArgument.getMessage());
                    updateLabel(polarConnectionStatusLabel, "Failed to " + attemptType + ". Reason: " + polarInvalidArgument.getMessage());
                }
            }
        });

        polarScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // https://github.com/polarofficial/polar-ble-sdk/blob/master/examples/example-android/androidBleSdkTestApp/app/src/main/java/com/polar/androidblesdk/MainActivity.kt
                // Line 243
                if (polarScanDisposable == null || polarScanDisposable.isDisposed()) {
                    toggleButtonDown(polarScanButton);
                    polarScanDisposable = polarApi.searchForDevice()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<PolarDeviceInfo>() {
                                @Override
                                public void accept(PolarDeviceInfo polarDeviceInfo) throws Throwable {
                                    Log.d(TAG, "polar device found id: " + polarDeviceInfo.getDeviceId() + " address: " + polarDeviceInfo.getAddress() + " rssi: " + polarDeviceInfo.getRssi() + " name: " + polarDeviceInfo.getName() + " isConnectable: " + polarDeviceInfo.isConnectable());
                                    updateLabel(polarConnectionStatusLabel, "polar device found id: " + polarDeviceInfo.getDeviceId() + " address: " + polarDeviceInfo.getAddress() + " rssi: " + polarDeviceInfo.getRssi() + " name: " + polarDeviceInfo.getName() + " isConnectable: " + polarDeviceInfo.isConnectable());
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Throwable {
                                    toggleButtonUp(polarScanButton);
                                    Log.e(TAG, "Polar device scan failed. Reason " + throwable.getMessage());
                                    updateLabel(polarConnectionStatusLabel, "Polar device scan failed. Reason " + throwable.getMessage());
                                }
                            }, new Action() {
                                @Override
                                public void run() throws Throwable {
                                    Log.d(TAG, "Polar Scan for Devices complete");
                                }
                            });
                } else {
                    toggleButtonUp(polarScanButton);
                    polarScanDisposable.dispose();
                }
            }
        });

        polarHrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // https://github.com/polarofficial/polar-ble-sdk/blob/master/examples/example-android/androidBleSdkTestApp/app/src/main/java/com/polar/androidblesdk/MainActivity.kt
                // Line 268
                if (polarHrDisposable == null || polarHrDisposable.isDisposed()) {
                    toggleButtonDown(polarHrButton);
                    polarHrDisposable = polarApi.startHrStreaming(polarDeviceId)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<PolarHrData>() {
                                @Override
                                public void accept(PolarHrData polarHrData) throws Throwable {
                                    for (PolarHrData.PolarHrSample sample : polarHrData.getSamples()) {
                                        Log.d(TAG, "HR     bpm: " + sample.getHr() + "  rr (ms): " + sample.getRrsMs() + "  rrAvailable: " + sample.getRrAvailable() + "  contactStatus: " + sample.getContactStatus() + "  contactStatusSupported: " + sample.getContactStatusSupported());
                                        updateLabel(polarHrLabel, "HR: " + sample.getHr());
                                        updateLabel(polarHrRrLabel, "RR (available: " + sample.getRrAvailable() + "): " + sample.getRrsMs() + " ms");
                                    }
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Throwable {
                                    toggleButtonUp(polarHrButton);
                                    Log.e(TAG, "Polar HR Stream Failed. Reason " + throwable.getMessage());
                                    updateLabel(polarHrLabel, "HR: not available (some errors were encountered)");
                                    updateLabel(polarHrRrLabel, "RR: not available (some errors were encountered)");
                                }
                            }, new Action() {
                                @Override
                                public void run() throws Throwable {
                                    Log.d(TAG, "Polar HR Stream complete");
                                }
                            });
                } else {
                    toggleButtonUp(polarHrButton);
                    polarHrDisposable.dispose();
                }
            }
        });

        polarApi.setApiCallback(new PolarBleApiCallbackProvider() {
            @Override
            public void blePowerStateChanged(boolean b) {
                Log.d(TAG, b ? "BLE Power (for Polar): On" : "BLE Power (for Polar): Off");
            }

            @Override
            public void deviceConnected(@NonNull PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "POLAR DEVICE CONNECTED: " + polarDeviceInfo.getDeviceId());
                polarDeviceId = polarDeviceInfo.getDeviceId();
                isPolarDeviceConnected = true;
                toggleButtonDown(polarConnectButton);
            }

            @Override
            public void deviceConnecting(@NonNull PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "CONNECTING to POLAR DEVICE: " + polarDeviceInfo.getDeviceId());
            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "POLAR DEVICE DISCONNECTED: " + polarDeviceInfo.getDeviceId());
                isPolarDeviceConnected = false;
                toggleButtonUp(polarConnectButton);
            }

            @Override
            public void bleSdkFeatureReady(@NonNull String s, @NonNull PolarBleApi.PolarBleSdkFeature polarBleSdkFeature) {

            }

            @Override
            public void streamingFeaturesReady(@NonNull String s, @NonNull Set<? extends PolarBleApi.PolarDeviceDataType> set) {

            }

            @Override
            public void sdkModeFeatureAvailable(@NonNull String s) {

            }

            @Override
            public void hrFeatureReady(@NonNull String s) {
                Log.d(TAG, "POLAR HR FEATURE READINESS: " + s);
            }

            @Override
            public void disInformationReceived(@NonNull String s, @NonNull UUID uuid, @NonNull String value) {
                Log.d(TAG, "POLAR DIS INFO uuid: " + uuid.toString() + "  value: " + value);
            }

            @Override
            public void batteryLevelReceived(@NonNull String s, int i) {
                Log.d(TAG, "POLAR BATTERY LEVEL (" + s + "): " + Integer.toString(i));
            }

            @Override
            public void hrNotificationReceived(@NonNull String s, @NonNull PolarHrData.PolarHrSample polarHrSample) {

            }

            @Override
            public void polarFtpFeatureReady(@NonNull String s) {

            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            initEmpaticaDeviceManager();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_ACCESS_COARSE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, yay!
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        initEmpaticaDeviceManager();
                    }
                } else {
                    // Permission denied, boo!
                    final boolean needRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                    new AlertDialog.Builder(this)
                            .setTitle("Permission required")
                            .setMessage("Without this permission bluetooth low energy devices cannot be found, allow it in order to connect to the device.")
                            .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // try again
                                    if (needRationale) {
                                        // the "never ask again" flash is not set, try again with permission request
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            initEmpaticaDeviceManager();
                                        }
                                    } else {
                                        // the "never ask again" flag is set so the permission requests is disabled, try open app settings to enable the permission
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    }
                                }
                            })
                            .setNegativeButton("Exit application", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // without permission exit is the only way
                                    finish();
                                }
                            })
                            .show();
                }
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void initEmpaticaDeviceManager() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BT);
        }
        // Android 6 (API level 23) now require ACCESS_COARSE_LOCATION permission to use BLE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
        } else {

            if (TextUtils.isEmpty(EMPATICA_API_KEY)) {
                new AlertDialog.Builder(this)
                        .setTitle("Warning")
                        .setMessage("Please insert your API KEY")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // without permission exit is the only way
                                finish();
                            }
                        })
                        .show();
                return;
            }

            // Create a new EmpaDeviceManager. MainActivity is both its data and status delegate.
            deviceManager = new EmpaDeviceManager(getApplicationContext(), this, this);

            // Initialize the Device Manager using your API key. You need to have Internet access at this point.
            deviceManager.authenticateWithAPIKey(EMPATICA_API_KEY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceManager != null) {
            deviceManager.cleanUp();
        }
        if (polarApi != null) {
            polarApi.shutDown();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (deviceManager != null) {
            deviceManager.stopScanning();
        }
    }

    @Override
    public void didDiscoverDevice(EmpaticaDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
        // Check if the discovered device can be used with your API key. If allowed is always false,
        // the device is not linked with your API key. Please check your developer area at
        // https://www.empatica.com/connect/developer.php

        Log.i(TAG, "didDiscoverDevice" + deviceName + "allowed: " + allowed);

        if (allowed) {
            // Stop scanning. The first allowed device will do.
            deviceManager.stopScanning();
            try {
                // Connect to the device
                deviceManager.connectDevice(bluetoothDevice);
                updateLabel(deviceNameLabel, "To: " + deviceName);
            } catch (ConnectionNotAllowedException e) {
                // This should happen only if you try to connect when allowed == false.
                Toast.makeText(MainActivity.this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "didDiscoverDevice" + deviceName + "allowed: " + allowed + " - ConnectionNotAllowedException", e);
            }
        }
    }

    @Override
    public void didFailedScanning(int errorCode) {
        
        /*
         A system error occurred while scanning.
         @see https://developer.android.com/reference/android/bluetooth/le/ScanCallback
        */
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                Log.e(TAG, "Scan failed: a BLE scan with the same settings is already started by the app");
                break;
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                Log.e(TAG, "Scan failed: app cannot be registered");
                break;
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                Log.e(TAG, "Scan failed: power optimized scan feature is not supported");
                break;
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                Log.e(TAG, "Scan failed: internal error");
                break;
            default:
                Log.e(TAG, "Scan failed with unknown error (errorCode=" + errorCode + ")");
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void didRequestEnableBluetooth() {
        // Request the user to enable Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_ENABLE_BT);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BT);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        } else {
            return;
        }
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    public void bluetoothStateChanged() {
        // E4link detected a bluetooth adapter change
        // Check bluetooth adapter and update your UI accordingly.
        boolean isBluetoothOn = BluetoothAdapter.getDefaultAdapter().isEnabled();
        Log.i(TAG, "Bluetooth State Changed: " + isBluetoothOn);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The user chose not to enable Bluetooth
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // You should deal with this
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void didUpdateSensorStatus(@EmpaSensorStatus int status, EmpaSensorType type) {

        didUpdateOnWristStatus(status);
    }

    @Override
    public void didUpdateStatus(EmpaStatus status) {
        // Update the UI
        updateLabel(statusLabel, status.name());

        // The device manager is ready for use
        if (status == EmpaStatus.READY) {
            updateLabel(statusLabel, status.name() + " - Turn on your device");
            // Start scanning
            deviceManager.startScanning();
            // The device manager has established a connection

            hide();

        } else if (status == EmpaStatus.CONNECTED) {

            show();
            // The device manager disconnected from a device
        } else if (status == EmpaStatus.DISCONNECTED) {

            updateLabel(deviceNameLabel, "");

            hide();
        }
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        updateLabel(accel_xLabel, "" + x);
        updateLabel(accel_yLabel, "" + y);
        updateLabel(accel_zLabel, "" + z);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        updateLabel(bvpLabel, "" + bvp);
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        updateLabel(batteryLabel, String.format("%.0f %%", battery * 100));
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        updateLabel(edaLabel, "" + gsr + " @ " + getDateTime(timestamp) + " - " + timestamp );
    }

    public static String getDateTime(double epochTime) {
        // Convert the epoch time to milliseconds (Java Date API works with milliseconds)
        long epochMillis = (long) (epochTime * 1000);

        // Create a Date object from the epoch milliseconds
        Date date = new Date(epochMillis);

        // Set the timezone to NZST (New Zealand Standard Time)
        TimeZone nzstTimeZone = TimeZone.getTimeZone("Pacific/Auckland");

        // Create a SimpleDateFormat with the desired format and set the timezone
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        sdf.setTimeZone(nzstTimeZone);

        // Format the date to NZST
        String nzstFormattedDate = sdf.format(date);

        return nzstFormattedDate;
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        updateLabel(ibiLabel, "" + ibi);
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        updateLabel(temperatureLabel, "" + temp);
    }

    // Update a label with some text, making sure this is run in the UI thread
    private void updateLabel(final TextView label, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                label.setText(text);
            }
        });
    }

    @Override
    public void didReceiveTag(double timestamp) {

    }

    @Override
    public void didEstablishConnection() {

        show();
    }

    @Override
    public void didUpdateOnWristStatus(@EmpaSensorStatus final int status) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (status == EmpaSensorStatus.ON_WRIST) {

                    ((TextView) findViewById(R.id.wrist_status_label)).setText("ON WRIST");
                }
                else {

                    ((TextView) findViewById(R.id.wrist_status_label)).setText("NOT ON WRIST");
                }
            }
        });
    }

    void toggleButtonDown(Button button) {
        toggleButton(button, true);
    }

    void toggleButtonUp(Button button) {
        toggleButton(button, false);
    }

    void toggleButton(Button button, Boolean isDown) {
        Drawable buttonDrawable = button.getBackground();
        if (isDown) {
            DrawableCompat.setTint(buttonDrawable, getColor(androidx.appcompat.R.color.primary_material_dark));
        } else {
            DrawableCompat.setTint(buttonDrawable, getColor(androidx.appcompat.R.color.primary_material_light));
        }
    }

    void show() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                dataCnt.setVisibility(View.VISIBLE);
            }
        });
    }

    void hide() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                dataCnt.setVisibility(View.INVISIBLE);
            }
        });
    }
}

package com.alphadraco.bleconfigurator;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.alphadraco.bleconfigurator.ui.main.deviceConfigFragment;
import com.alphadraco.bleconfigurator.ui.main.deviceParametersFragment;
import com.alphadraco.bleconfigurator.ui.main.devicesListFragment;
import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import com.alphadraco.bleconfigurator.ui.main.SectionsPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public devicesListFragment listFragment;
    public deviceConfigFragment configFragment;
    public deviceParametersFragment parametersFragment;

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;

    public ArrayAdapter deviceAdapter;
    final List<String> devices = new ArrayList<String>();

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    public MultiServiceIODevice device;

    final List<BluetoothDevice> bluetoothDevices = new ArrayList<BluetoothDevice>();
    public int selectedDevice = -1;

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // devices.add(result.getDevice().getName());
            if ((result != null) && (result.getDevice() != null) && (result.getDevice().getName() != null)) { //  && result.getDevice().getName().equals("Geiger")) {
                String name = result.getDevice().getName() + "\n" + result.getDevice().getAddress();
                for (String entry: devices) if (entry.equals(name)) return; // Already in list
                deviceAdapter.add(name);
                bluetoothDevices.add(result.getDevice());
            }
        }
    };

    public boolean hasBlePermissions() {
        if (
                (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                        (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                        (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED))
            return false;
        return true;
    }

    public boolean connectBLE() {
        if (selectedDevice >= 0) {
            btScanner.stopScan(leScanCallback);
            device = new MultiServiceIODevice(this, bluetoothDevices.get(selectedDevice));
            return true;
        }
        return  false;
    }

    public String getConnectString() {
        if (device == null)
            return "Not Connected";
        else
            return device.getConnectString();
    }

    public void configUpdateOccured() {
        if (configFragment != null)
            configFragment.configUpdateOccured();
        /*if (settingsFragment != null)
            settingsFragment.configUpateOccured();*/
    }

    public void infoUpdateOccured() {
        if (listFragment != null)
            listFragment.infoUpdateOccured();
        /*if (settingsFragment != null)
            settingsFragment.infoUpdateOccured();*/
    }

    private int backCounter = 0;

    @Override
    public void onBackPressed() {
        if (backCounter > 0) {
            finish();
            super.onBackPressed();
            return;
        }
        backCounter++;
        Toast.makeText(this, "Press the back button once again to close the application.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        deviceAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_single_choice, devices);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (!hasBlePermissions()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        btScanner.startScan(leScanCallback);


        final Handler hl = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                hl.postDelayed(this, 1000);
                if (device != null)
                    device.tick();
            }
        };
        hl.postDelayed(r, 1000);
    }

    public void updateParameterFragment() {
        if (parametersFragment == null) return;
        parametersFragment.updateParameterFragment();
    }
}
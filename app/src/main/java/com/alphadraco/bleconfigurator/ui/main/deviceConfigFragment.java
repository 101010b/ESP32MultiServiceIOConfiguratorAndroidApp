package com.alphadraco.bleconfigurator.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.alphadraco.bleconfigurator.MainActivity;
import com.alphadraco.bleconfigurator.MultiServiceIODevice;
import com.alphadraco.bleconfigurator.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

/**
 * A placeholder fragment containing a simple view.
 */
public class deviceConfigFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    MainActivity activity;

    public static deviceConfigFragment newInstance(int index) {
        deviceConfigFragment fragment = new deviceConfigFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    public void configUpdateOccured() {
        if (activity == null) return;
        if (activity.device == null) return;
        wlanSSID.setText(activity.device.wlanSSID);
        mqttServer.setText(activity.device.mqttServer);
        mqttPort.setText(String.format("%d",activity.device.mqttPort));
        mqttUser.setText(activity.device.mqttUser);
    }

    TextView password;
    Button unlock;
    Button trySet;
    TextView wlanSSID;
    TextView wlanPWD;
    TextView mqttServer;
    TextView mqttPort;
    TextView mqttPwd;
    TextView mqttUser;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_config, container, false);
        activity = (MainActivity)getActivity();
        activity.configFragment = this;

        password = v.findViewById(R.id.devicePassword);
        unlock = v.findViewById(R.id.unlock);
        trySet = v.findViewById(R.id.setPassword);
        wlanSSID = v.findViewById(R.id.wlanSSID);
        wlanPWD = v.findViewById(R.id.wlanPwd);
        mqttServer = v.findViewById(R.id.MQTTServer);
        mqttPort = v.findViewById(R.id.MQTTServerPort);
        mqttUser = v.findViewById(R.id.MQTTUser);
        mqttPwd = v.findViewById(R.id.MQTTPassword);

        unlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) return;
                if (activity.device == null) return;
                activity.device.tryDevicePwd(password.getText().toString());
            }
        });

        trySet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) return;
                if (activity.device == null) return;
                activity.device.trySetDevicePwd(password.getText().toString());
            }
        });

        wlanSSID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) return;
                if (activity.device == null) return;
                activity.device.wlanSSID = wlanSSID.getText().toString();
                activity.device.updateConfig(MultiServiceIODevice.ConfigData.WLANSSID);
            }
        });

        wlanPWD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) return;
                if (activity.device == null) return;
                activity.device.wlanPWD = wlanPWD.getText().toString();
                activity.device.updateConfig(MultiServiceIODevice.ConfigData.WLANPWD);
            }
        });

        mqttServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) return;
                if (activity.device == null) return;
                activity.device.mqttServer = mqttServer.getText().toString();
                activity.device.updateConfig(MultiServiceIODevice.ConfigData.MQTTSERVER);
            }
        });

        mqttPort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) return;
                if (activity.device == null) return;
                try {
                    int port = Integer.decode(mqttPort.getText().toString());
                    activity.device.mqttPort = port;
                    activity.device.updateConfig(MultiServiceIODevice.ConfigData.MQTTPORT);
                } catch (Exception e) {
                }
            }
        });

        mqttUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) return;
                if (activity.device == null) return;
                activity.device.mqttUser = mqttUser.getText().toString();
                activity.device.updateConfig(MultiServiceIODevice.ConfigData.MQTTUSER);
            }
        });

        mqttPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) return;
                if (activity.device == null) return;
                activity.device.mqttPwd = mqttPwd.getText().toString();
                activity.device.updateConfig(MultiServiceIODevice.ConfigData.MQTTPWD);
            }
        });

        return v;
    }
}
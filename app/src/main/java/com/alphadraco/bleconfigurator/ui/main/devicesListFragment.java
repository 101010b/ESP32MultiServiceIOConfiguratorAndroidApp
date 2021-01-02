package com.alphadraco.bleconfigurator.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.alphadraco.bleconfigurator.MainActivity;
import com.alphadraco.bleconfigurator.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class devicesListFragment extends Fragment {

    public MainActivity activity;

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    private ListView deviceList;
    private Button connectButton;

    public static devicesListFragment newInstance(int index) {
        devicesListFragment fragment = new devicesListFragment();
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

    private TextView connectInfo;
    private String connectString = "";

    public void infoUpdateOccured() {
        String newConnectString = "Not Connected";
        if ((activity != null) && (activity.device != null))
            newConnectString = activity.device.getConnectString();
        if (!newConnectString.equals(connectString)) {
            if (connectInfo != null) connectInfo.setText(newConnectString);
            connectString = newConnectString;
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_devices, container, false);
        activity = (MainActivity)getActivity();
        activity.listFragment = this;

        deviceList = v.findViewById(R.id.devices);
        // deviceAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_single_choice, devices);
        deviceList.setAdapter(((MainActivity)getActivity()).deviceAdapter);
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                activity.selectedDevice = i;
            }
        });
        deviceList.setItemsCanFocus(true);;
        deviceList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        connectButton = v.findViewById(R.id.connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity.connectBLE()) {
                    deviceList.setEnabled(false);
                    connectButton.setEnabled(false);
                }
            }
        });

        connectInfo = v.findViewById(R.id.deviceInfo);
        connectInfo.setText(activity.getConnectString());
        if (activity.device != null) {
            deviceList.setEnabled(false);
            connectButton.setEnabled(false);
        }
        // return root
        return v;
    }
}
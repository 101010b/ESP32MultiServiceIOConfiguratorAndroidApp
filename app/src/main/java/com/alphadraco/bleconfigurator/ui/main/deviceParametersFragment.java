package com.alphadraco.bleconfigurator.ui.main;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.alphadraco.bleconfigurator.ColorEntry;
import com.alphadraco.bleconfigurator.ConfigVariable;
import com.alphadraco.bleconfigurator.MainActivity;
import com.alphadraco.bleconfigurator.MultiServiceIODevice;
import com.alphadraco.bleconfigurator.R;
import com.google.android.material.slider.Slider;
import com.google.android.material.transition.MaterialContainerTransform;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class deviceParametersFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;
    private LinearLayout parameterList = null;

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static deviceParametersFragment newInstance(int index) {
        deviceParametersFragment fragment = new deviceParametersFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    public MainActivity activity;
    private int padtop = 0;
    private int padbottom = 0;
    private int padleft = 0;
    private int padright = 0;

    public void updateVariable(ConfigVariable cfg) {
        activity.device.updateVariable(cfg);
    }

    void addBooleanParameter(ConfigVariable cfg) {
        Switch sw = new Switch(activity.getApplicationContext());
        sw.setText(cfg.name);
        sw.setChecked((cfg.lvalue == 0)?false:true);
        sw.setTextOff("Off");
        sw.setTextOn("On");
        sw.setTag(cfg);
        sw.setTextColor(Color.BLACK);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                ((ConfigVariable)compoundButton.getTag()).lvalue = (b)?1:0;
                updateVariable((ConfigVariable)compoundButton.getTag());
            }
        });
        sw.setPadding(padleft, padtop, padright, padbottom);
        parameterList.addView(sw);
    }

    void addIntegerParameter(ConfigVariable cfg) {
        TextView nv = new TextView(activity.getApplicationContext());
        nv.setTextColor(Color.BLACK);
        nv.setText(String.format("%s (%d <= X <= %d)", cfg.name, cfg.lmin, cfg.lmax));
        SeekBar sk = new SeekBar(activity.getApplicationContext());
        sk.setMin((int)0);
        sk.setMax((int)(cfg.lmax-cfg.lmin));
        sk.setProgress((int)(cfg.lvalue-cfg.lmin));
        sk.setTag(cfg);
        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                ConfigVariable cfg = ((ConfigVariable)seekBar.getTag());
                cfg.lvalue = cfg.lmin + i;
                updateVariable(cfg);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        nv.setPadding(padleft,padtop,padright,0);
        sk.setPadding(padleft*2, 0, padright*2, padbottom);
        parameterList.addView(nv);
        parameterList.addView(sk);
        return;
    }

    void addFloatParameter(ConfigVariable cfg) {
        TextView nv = new TextView(activity.getApplicationContext());
        nv.setTextColor(Color.BLACK);
        nv.setText(String.format("%s (%f <= X <= %f)", cfg.name, cfg.fmin, cfg.fmax));
        EditText et = new EditText(activity.getApplicationContext());
        et.setEms(10);
        et.setText(String.format("%f",cfg.fvalue));
        nv.setPadding(padleft,padtop,padright,0);
        et.setPadding(padleft, 0, padright, padbottom);
        et.setTag(cfg);
        et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ConfigVariable cfg = (ConfigVariable)view.getTag();
                    float f = Float.valueOf(((EditText) view).getText().toString());
                    if (f < cfg.fmin) f = cfg.fmin;
                    if (f > cfg.fmax) f = cfg.fmax;
                    cfg.fvalue = f;
                    updateVariable(cfg);
                }
                catch (Exception e) {
                }
            }
        });
        parameterList.addView(nv);
        parameterList.addView(et);
    }

    void addStringParameter(ConfigVariable cfg) {
        TextView nv = new TextView(activity.getApplicationContext());
        nv.setTextColor(Color.BLACK);
        nv.setText(cfg.name);
        EditText et = new EditText(activity.getApplicationContext());
        et.setEms(10);
        et.setText(cfg.svalue);
        nv.setPadding(padleft,padtop,padright,0);
        et.setPadding(padleft, 0, padright, padbottom);
        et.setTag(cfg);
        et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConfigVariable cfg = (ConfigVariable)view.getTag();
                cfg.svalue = ((EditText)view).getText().toString();
                updateVariable(cfg);
            }
        });
        parameterList.addView(nv);
        parameterList.addView(et);
    }

    void addColorParameter(ConfigVariable cfg) {
        ColorEntry ce = new ColorEntry(activity.getApplicationContext());
        ce.setSelectColor((int)cfg.lvalue);
        ce.setTitleText(cfg.name);
        parameterList.addView(ce);
        ViewGroup.LayoutParams params = ce.getLayoutParams();
        int bh = dpToPx(24);
        params.height = bh+padtop+padbottom;
        ce.setPadding(padleft,padtop,padright,padbottom);
        ce.setDisplayDimension(bh/2);
        ce.setTag(cfg);
        ce.setOnColorChangeListener(new ColorEntry.OnColorChangeListener() {
            @Override
            public void onColorChange(ColorEntry ce, int newColor) {
                ConfigVariable cfg = (ConfigVariable) ce.getTag();
                cfg.lvalue = newColor;
                updateVariable(cfg);
            }
        });
        ce.setLayoutParams(params);
    }

    public void updateParameterFragment() {
        if ((activity == null) || (activity.device == null) || (activity.device.config == null))
            return;
        if (parameterList == null) return;
        parameterList.removeAllViews();
        padtop = padbottom = padleft = padright = dpToPx(6);
        for (int i=0;i<activity.device.config.size();i++) {
            switch (activity.device.config.get(i).vartype) {
                case ui32:
                    if ((activity.device.config.get(i).lmin == 0) &&
                            (activity.device.config.get(i).lmax == 1)) {
                        // Is a boolean status
                        addBooleanParameter(activity.device.config.get(i));
                    } else if ((activity.device.config.get(i).lmin == 0) &&
                            (activity.device.config.get(i).lmax == 16777215L)) {
                        // Is a Color Value
                        addColorParameter(activity.device.config.get(i));
                    } else {
                        // Is a number
                        addIntegerParameter(activity.device.config.get(i));
                    }
                    break;
                case i32:
                    addIntegerParameter(activity.device.config.get(i));
                    break;
                case f32:
                    addFloatParameter(activity.device.config.get(i));
                    break;
                case str:
                    addStringParameter(activity.device.config.get(i));
                    break;
            }
        }
    }

    public void infoUpdateOccured() {
        /*if ((geigerOnButton != null) && (activity != null) && (activity.geigerDevice != null))
            geigerOnButton.setChecked(activity.geigerDevice.geigerON);
        if ((speakerOnButton != null) && (activity != null) && (activity.geigerDevice != null))
            speakerOnButton.setChecked(activity.geigerDevice.speakerON);*/
    }

    public void configUpateOccured() {
        /*if ((wlanOnButton != null) && (activity != null) && (activity.geigerDevice != null))
            wlanOnButton.setChecked(activity.geigerDevice.wlanON);;*/
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

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_parameters, container, false);

        activity = (MainActivity)getActivity();
        activity.parametersFragment = this;

        parameterList = v.findViewById(R.id.parameterList);

        updateParameterFragment();

        infoUpdateOccured();
        configUpateOccured();

        return v;
    }
}
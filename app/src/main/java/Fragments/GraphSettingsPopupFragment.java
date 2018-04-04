package Fragments;

import android.app.Activity;
import android.support.v4.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mattmellor.smartshoegrapher.R;

/**
 * Created by haripriyamehta on 4/3/17.
 */

public class GraphSettingsPopupFragment extends DialogFragment {
//Right now blank

    //Look at InputUserSettingsPopupFragment for inspiration
    //DialogFragment -> meant to be popups (google it or try to look at code of InputUserSettingsPopupFragment/WirelessPairingActivity
    private OnDataPassGraphSettings dataPassHandle;
    private EditText graphtitleEditText;
    private EditText xaxisEditText;
    private EditText yaxisEditText;
    private EditText xscaleEditText;
    private EditText yscaleEditText;
    private String graphtitle = "Sensor Values vs. Number of Samples";
    private String xaxis = "Number of Samples";
    private String yaxis = "Sample of Values";
    private int xscale = 100000;
    private int yscale = 5000;
    private boolean applythisPressed = false;

    //TODO: In the done button listener the method to call with just be 'dismiss()'
    public static GraphSettingsPopupFragment newInstance() {
        return new GraphSettingsPopupFragment();
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity a;
        if (context instanceof Activity) {
            a = (Activity) context;
            dataPassHandle = (GraphSettingsPopupFragment.OnDataPassGraphSettings) a;
            Log.d("HP!", "Made it here");
        }
        //Use dataPassHandle like dataPassHandle.methodName(inputs...);
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View frag = inflater.inflate(R.layout.graph_settings_popup, container, false);
        getDialog().setTitle("Graph Settings");
        Button applybutton = (Button) frag.findViewById(R.id.apply_button);
        graphtitleEditText = (EditText) frag.findViewById(R.id.graph_title);
        xaxisEditText = (EditText) frag.findViewById(R.id.x_title);
        yaxisEditText = (EditText) frag.findViewById(R.id.y_title);
        xscaleEditText = (EditText) frag.findViewById(R.id.xscale_title);
        yscaleEditText = (EditText) frag.findViewById(R.id.yscale_title);

        graphtitleEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applythisPressed = false;
            }
        });

        xaxisEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applythisPressed = false;
            }
        });

        yaxisEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applythisPressed = false;
            }
        });
        xscaleEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applythisPressed = false;
            }
        });
        yscaleEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applythisPressed = false;
            }
        });

        applybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return frag;

    }



    public interface OnDataPassGraphSettings {
        void onDataPassGraphSettings(String graphtitle, String xaxis, String yaxis, int xscale, int yscale);
    }

    public void passDataToMainActivity() {
        dataPassHandle.onDataPassGraphSettings(graphtitle, xaxis, yaxis, xscale, yscale);
    }
}


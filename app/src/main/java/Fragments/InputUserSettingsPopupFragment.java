package Fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mattmellor.smartshoegrapher.R;
import com.mattmellor.smartshoegrapher.UdpClient;

/**
 * Created by Matthew on 12/6/2016.
 * Holds the data about the UDP Connection and contains the wiring
 * Created as a popup to work
 * This is the current UserInputSettings Popup Fragment it is to replace UdpSettingsFragment
 */

public class InputUserSettingsPopupFragment extends DialogFragment {


    private EditText hostnameEditText;
    private EditText localPortEditText;
    private EditText remotePortEditText;

    private String hostname = "footsensor2.dynamic-dns.net";
    private int localPort = 8080; //5006
    private int remotePort = 8080; //2391
    private String unverifiedHostname;
    private String unverifiedRemotePort;
    private String unverifiedLocalPort;
    private final String defaultHostname = "footsensor2.dynamic-dns.net";
    private final int defaultRemotePort = 8080; //2391
    private final int defaultLocalPort = 8080; //5006
    private boolean applyPressed = false;
    private boolean dataIsVerified = false;

    private OnDataPass dataPassHandle; //
    private Handler activityHandler;

    public InputUserSettingsPopupFragment(){
        //Blank on purpose
    }

    public static InputUserSettingsPopupFragment newInstance(){
        return new InputUserSettingsPopupFragment();
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        Activity a;
        if(context instanceof Activity){
            a = (Activity) context;
            dataPassHandle = (OnDataPass) a;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        final View frag = inflater.inflate(R.layout.input_user_udp_settings_popup, container, false);
        getDialog().setTitle("UDP Settings");

        Button ping = (Button) frag.findViewById(R.id.ping_popup);
        Button apply = (Button) frag.findViewById(R.id.apply_button_popup);
        Button reset = (Button) frag.findViewById(R.id.reset_button_popup);
        Button closeButton = (Button) frag.findViewById(R.id.close_button_popup);
        Button connectButton = (Button) frag.findViewById(R.id.connect_button_popup);
        hostnameEditText = (EditText) frag.findViewById(R.id.remote_hostname_popup);
        localPortEditText = (EditText) frag.findViewById(R.id.local_port_popup);
        remotePortEditText = (EditText) frag.findViewById(R.id.remote_port_popup);


        hostnameEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPressed = false;
            }
        });

        remotePortEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPressed = false;
            }
        });

        localPortEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPressed = false;
            }
        });

        //Don't need to communicate with the main thread/UI for this
        ping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(applyPressed) {
                    onClickPing();
                }
                else{
                    Context context = getActivity();
                    CharSequence text = "Apply Settings";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }
        });

        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unverifiedHostname = hostnameEditText.getText().toString();
                unverifiedRemotePort = remotePortEditText.getText().toString();
                unverifiedLocalPort = localPortEditText.getText().toString();
                boolean validParameters = true;

                if(portValid(unverifiedLocalPort) && !localPortAlreadyUsed(unverifiedLocalPort)){
                    localPort = convertStringToInt(unverifiedLocalPort);
                    Log.d("MATT!", "Local Port Valid");
                }
                else{
                    validParameters = false;
                    localPortEditText.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.vibrate));
                    Log.d("MATT!", "Local Port invalid");
                    if(localPortAlreadyUsed(unverifiedLocalPort)){
                        Context context = getActivity();
                        CharSequence text = "Local Port Already Used";
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                    }
                }

                if(portValid(unverifiedRemotePort)){
                    remotePort = convertStringToInt(unverifiedRemotePort);
                    Log.d("MATT!", "Remote Port Valid");
                }
                else{
                    Log.d("MATT!", "remotePort Invalid");
                    remotePortEditText.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.vibrate));
                    validParameters = false;
                }
                if(hostnameValid(unverifiedHostname)){
                    hostname = unverifiedHostname;
                }
                else{
                    hostnameEditText.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.vibrate));
                }
                if(validParameters) {
                    dataIsVerified = true;
                    applyPressed = true;
                }
            }
        });

        //closeButton is the Done Button
        closeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dataIsVerified && applyPressed){
                    dataIsVerified = false;
                    passDataToActivity();
                    Log.d("MATT!", "Sent Data to WirelessPairingActivity");
                }
                else{
                    Context context = getActivity();
                    CharSequence text = "Apply Settings";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hostnameEditText.setText(defaultHostname);
                localPortEditText.setText(R.string.local_port_example);
                remotePortEditText.setText(R.string.remote_port_example);
                hostname = defaultHostname;
                localPort = defaultLocalPort;
                remotePort = defaultRemotePort;
                applyPressed = false;
            }
        });
        return frag;
    }

    /**
     * Starts a thread to ping the server
     */
    public void onClickPing(){
        //set the activity handler
        UdpClient client = new UdpClient(hostname,remotePort,localPort,45);
        UdpClient.UdpServerAcknowledge udpPinger = client.new UdpServerAcknowledge(this.activityHandler);
        udpPinger.start();
    }

    /**
     * Interface methods implemented by wireless pairing activity to allow for communication between the activity
     * and the fragment
     * We only pass verified input
     */
    public interface OnDataPass{
        void onDataPassUdpSettings(String hostname,int localPort, int remotePort);
        boolean isLocalPortUsed(String localPort);
    }

    /**
     * This method passes the inputed sensor data to WirelessPairingActivity
     * Data passed has to have been verified as a proper hostname, remote port, local port
     */
    public void passDataToActivity(){
        dataPassHandle.onDataPassUdpSettings(hostname,localPort,remotePort);
    }

    /**
     *
     * @param handler of the WirelessPairingActivity
     */
    public void setActivityHandler(Handler handler){
        this.activityHandler = handler;
    }

    public boolean localPortAlreadyUsed(String unverifiedLocalPort){
        return dataPassHandle.isLocalPortUsed(unverifiedLocalPort);
    }

    //-------------Helper Functions-------------
    /**
     * @param value string
     * @return true if string is a valid remote port or local port number
     */
    public int convertStringToInt(String value) {
        return Integer.parseInt(value);
    }

    /**
     *
     * @param port
     *          representing the local or remote port
     * @return boolean
     *         true if a valid port number 0 - 65535
     *         false if not a valid port number
     */
    public boolean portValid(String port){
        return port.matches("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$");
    }

    /**
     *
     * @param hostname to be tested
     * @return true if hostname is a valid hostname or IP Address according to RFC 1123
     */
    public boolean hostnameValid(String hostname){
        String validIpAddressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
        String validHostnameRegex = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
        return (hostname.matches(validIpAddressRegex) || hostname.matches(validHostnameRegex));
    }

}

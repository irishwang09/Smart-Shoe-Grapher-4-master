package com.mattmellor.smartshoegrapher;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.GridView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.scichart.data.model.DoubleRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Handler;

import Fragments.GraphFragment;
import Fragments.GraphFragmentLocal;
import Fragments.GraphSettingsPopupFragment;
import Fragments.InputUserSettingsPopupFragment;
import SciChartUserClasses.SciChartBuilder;
import UserDataDataBase.UDPDataBaseHelper;
import UserDataDataBase.UDPDatabaseContract;


public class MainActivity extends AppCompatActivity implements GraphSettingsPopupFragment.OnDataPassGraphSettings{

    private String hostname;
    private int remotePort;
    private int localPort;
    private String graphtitle;
    private String xaxis;
    private String yaxis;
    private int xscale;
    private int yscale;
    public static volatile boolean isRunning;

    private GraphFragment graphFragment;
    private SettingsCardAdapter mAdapter;
    private UDPDataBaseHelper mDbHelper;
    private boolean currentlyGraphing = false;
    private SQLiteDatabase db;
    private Handler mainActivityHandler;

    public static boolean mode = true;
    public static TextView serialWindow;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SciChartBuilder.init(this); //This is important for GraphFragment to initialize it
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //set the layout of the activity
        graphFragment = (GraphFragment) getSupportFragmentManager().findFragmentById(R.id.graph_fragment);
        serialWindow = (TextView) findViewById(R.id.SerialWindow);
        //graphFragmentLocal = (GraphFragmentLocal) getSupportFragmentManager().findFragmentById(R.id.graph_fragment_local);
        /*ArrayList<String> hostnames = new ArrayList<>();
        ArrayList<Integer> localPorts = new ArrayList<>();
        ArrayList<Integer> remotePorts = new ArrayList<>();

        //Read from the UserUDPSettings database if it exists
        mDbHelper = UDPDataBaseHelper.getInstance(getApplicationContext());
        db = mDbHelper.getWritableDatabase(); //Creates a new database if one doesn't exist
        ArrayList<ArrayList<String>> pastSensors = readUDPSettingsFromDataBase();
        if(pastSensors != null && !pastSensors.isEmpty()){
            Log.d("MATT!", "Reading old sensors in onCreate of MainActivity");
            for(ArrayList<String> sensorData: pastSensors){
                String verifiedHostname = sensorData.get(0);
                String verfLocalPort = sensorData.get(1);
                int verifiedLocalPort = Integer.parseInt(verfLocalPort);
                int verifiedRemotePort = Integer.parseInt(sensorData.get(2));
                hostnames.add(verifiedHostname);
                localPorts.add(verifiedLocalPort);
                remotePorts.add(verifiedRemotePort);
            }
            onDataPassUdpSettings(hostnames.get(0), localPorts.get(0), remotePorts.get(0)); //TODO: Change this... it is hardcoded
        }*/

        //Create a Scrolling list for the start stop Sensor Pairing and Graph Settings buttons
        if (currentlyGraphing == false)
             isRunning = true;
        else
             isRunning = false;
        RecyclerView recyclerSettingsCardsList = (RecyclerView) findViewById(R.id.recycler_view_settings_cards_list);
        recyclerSettingsCardsList.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerSettingsCardsList.setLayoutManager(
                new GridLayoutManager(recyclerSettingsCardsList.getContext(),4));
        ArrayList<String> settingCardTitles = new ArrayList<>(Arrays.asList("Start/Stop", "\tSensor\n\tPairing", "\t\tGraph\n\tSettings", "\tReset\n\tGraph"));
        mAdapter = new MainActivity.SettingsCardAdapter(settingCardTitles);
        recyclerSettingsCardsList.setAdapter(mAdapter); //Adapter is what we use to manage add/remove views

        //TODO: Initialize the MainAcitivity Handler here

    }

    @Override
    protected void onDestroy(){
        //mDbHelper.close();
        db.close();
        super.onDestroy();
    }
    public static void setText(String txt)
    {
        serialWindow.setText(txt);
    }
    public static String getText() { return serialWindow.getText().toString(); }
    //Passes Data from main activity to graphfragment
    private void onDataPassUdpSettings(String verifiedHostname, int verifiedLocalPort, int verifiedRemotePort) {
        this.hostname = verifiedHostname;
        this.localPort = verifiedLocalPort;
        this.remotePort = verifiedRemotePort;
        graphFragment.updateHostname(hostname);
        graphFragment.updateLocalPort(localPort);
        graphFragment.updateRemotePort(remotePort);
        Log.d("MATT!", "Passed to GF Verified Host: " + verifiedHostname);
        Log.d("MATT!", "Passed to GF Verified Local Port : " + verifiedLocalPort);
        Log.d("MATT!", "Passed to GF Verified Remote Port : " + verifiedRemotePort);
    }

    //This is attached to the start/stop button in the SettingsCardAdapter
    private void startGraphing() {
        Log.d("MATT!", "Start Graphing");
        graphFragment.startGraphing();
    }
    private void stopGraphing() {
        Log.d("MATT!", "Stop Graphing"); //TODO: This needs debugging after changes for multiple pairing ESPs
        graphFragment.stopGraphing();
    }
    private void resetGraph() {
        graphFragment.resetGraph();
    }

    //-------------Code for RecyclerView-----------

    //This is used to create the underlying code for the list of buttons created in SettingsCardHolder
    private class SettingsCardAdapter extends RecyclerView.Adapter<SettingCardHolder>{
        //dataSet will just contain 3 entries: Start/Stop, Sensor Pairing, Graph Settings
        private ArrayList<String> mdataSet;

        private SettingsCardAdapter(ArrayList<String> dataSet){
            mdataSet = dataSet;
        }

        //Create new views
        @Override
        public MainActivity.SettingCardHolder onCreateViewHolder(ViewGroup parent, int viewType){
            //This is called whenever a new instance of ViewHolder is created
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View v = layoutInflater.inflate(R.layout.setting_card_view,parent,false);
            return new MainActivity.SettingCardHolder(v);
        }

        // Replace the contents of a view (invoked by layout manager)
        @Override
        public void onBindViewHolder(MainActivity.SettingCardHolder ph, int position){
            //Called whenever the SO binds the view with the data...in otherwords the
            //data is shown in the UI
            String title = mdataSet.get(position);
            ph.cardTitle.setText(mdataSet.get(position));
            ph.setOnClickListenerHolder(title);
        }

        @Override
        public int getItemCount(){
            return mdataSet.size();
        }
    }

    private class SettingCardHolder extends RecyclerView.ViewHolder{

        private TextView cardTitle;
        private View item_view;

        private SettingCardHolder (View itemView){  //This must be called at least once per item...
            super(itemView);
            item_view = itemView;
            cardTitle = (TextView) itemView.findViewById(R.id.setting_card_title);
        }

        //OnClick button listener will bring up the Wireless Pairing Activity to get user UDP Setting Data
        //which could be done by

        private void setOnClickListenerHolder(String cardTitle){
            if(cardTitle.equals("\tSensor\n\tPairing")){
                item_view.setOnClickListener(startSensorPairingListener);
            }
            else if(cardTitle.equals("Start/Stop") || cardTitle.equals("Stop")){
                item_view.setOnClickListener(startStopButtonListener);
            }
            else if (cardTitle.equals("\tReset\n\tGraph")){
                item_view.setOnClickListener(resetGraphListener);
            }
            else{  //Equals the button for starting the Graph Settings Button
                item_view.setOnClickListener(graphSettingsButtonListener);
            }
        }
    }
    private View.OnClickListener startSensorPairingListener = new View.OnClickListener(){

        public void onClick(View v){
            Intent intent = new Intent(getApplicationContext(), WirelessPairingActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener startStopButtonListener = new View.OnClickListener(){

        public void onClick(View v){
            if(currentlyGraphing)
            {
                stopGraphing();  //TODO: This needs to be improved
                currentlyGraphing = false;
            }
            else
            {
                currentlyGraphing = true;
                startGraphing();
            }
        }
    };
    private View.OnClickListener resetGraphListener = new View.OnClickListener(){
        public void onClick(View v){
            resetGraph();
        }
    };
    //Button listener to get data from the user on the size of the graph that they want
    private View.OnClickListener graphSettingsButtonListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            //TODO: Implement this to make graphSettings button 'Click'
            //TODO: create the fragment
            FragmentManager fm = getSupportFragmentManager();
            GraphSettingsPopupFragment settingsFragment = GraphSettingsPopupFragment.newInstance();
            settingsFragment.show(fm, "MATT!");
            //It is probably something like settingsFragment.onAttach()
            Log.d("HP!", "is this making it here");
            //This is where we get the graphSettings onClick()
        }
    };


    private ArrayList<ArrayList<String>> readUDPSettingsFromDataBase(){
        //These are the columns we are
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        //Set the query cursor to get the whole table -> thus all of the nulls
        Cursor cursor = db.query(UDPDatabaseContract.UdpDataEntry.TABLE_NAME, null, null, null, null, null, null);
        cursor.moveToFirst(); //Moves the cursor to the first row
        int numRows = cursor.getCount();
        String remoteHostname;
        String localPort;
        String remotePort;
        for(int rowNumber = 0; rowNumber < numRows; rowNumber++){ //Loop through each row and get the column values
            remoteHostname = cursor.getString(0);
            localPort = cursor.getString(1);
            remotePort = cursor.getString(2);
            ArrayList<String> sensorSettings = new ArrayList<>(Arrays.asList(remoteHostname, localPort, remotePort));
            data.add(sensorSettings);
            cursor.moveToNext(); //Move to
        }
        cursor.close();
        return data;
    }

    public void onDataPassGraphSettings(String graphtitle, String xaxis, String yaxis, int xscale, int yscale) {

    }
}

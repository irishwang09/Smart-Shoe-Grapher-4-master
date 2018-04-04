package com.mattmellor.smartshoegrapher;


import android.app.ActionBar;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import Fragments.InputUserSettingsPopupFragment;
import UserDataDataBase.UDPDataBaseHelper;
import UserDataDataBase.UDPDatabaseContract;

/**
 * Created by Matthew on 10/20/2016.
 * This is a public class to allow for connecting to multiple different remote
 * Wifi -UDP Servers
 */

public class WirelessPairingActivity extends AppCompatActivity implements InputUserSettingsPopupFragment.OnDataPass{

    //Fields
    //DataBase Access
    private SQLiteDatabase db;
    private UDPDataBaseHelper mDbHelper;
    private PairingListAdapter mAdapter;
    private ArrayList<String> connected_host_names;
    private ArrayList<String> used_local_ports;
    //ArrayList<String> settingCardTitles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.wireless_pairing_layout);
        connected_host_names = new ArrayList<>();
        used_local_ports = new ArrayList<>();

        //Recycler Pairing List (Scrollable List)
        //Used to dynamically add the pairedSensor views to Recycler List
        //TODO: FIX THIS TO WORK WITH TABLELAYOUT
        RecyclerView recycPairingList = (RecyclerView) findViewById(R.id.pairing_fragment_container);
        recycPairingList.setHasFixedSize(true);
        recycPairingList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PairingListAdapter(new ArrayList<ArrayList<String>>());
        recycPairingList.setAdapter(mAdapter); //Adapter is what we use to manage add/remove views
        //Get a Database
        //Goal here is to read the database if it exists
        mDbHelper = UDPDataBaseHelper.getInstance(getApplicationContext());
        db = mDbHelper.getWritableDatabase(); //Creates a new database if one doesn't exist
        ArrayList<ArrayList<String>> pastSensors = readUDPSettingsFromDataBase();
        //If there are sensors already in the database...add them to the recyclerList
        if(pastSensors != null && !pastSensors.isEmpty()){
            Log.d("MATT!", "Reading old sensors in onCreate");
            for(ArrayList<String> sensorData: pastSensors){
                String verifiedHostname = sensorData.get(0);
                String verfLocalPort = sensorData.get(1);
                int verifiedLocalPort = Integer.parseInt(verfLocalPort);
                int verifiedRemotePort = Integer.parseInt(sensorData.get(2));
                addUDPSensorToConnectedList(verifiedHostname, verifiedLocalPort,verifiedRemotePort);
                connected_host_names.add(verifiedHostname);
                used_local_ports.add(verfLocalPort);
            }
        }

        //Top Level Code to get a new Sensor from the user
        //This is button wiring for add new Sensor
        ImageButton addRemoteSensor = (ImageButton) findViewById(R.id.add_remote_sensor_pairing);
        addRemoteSensor.setOnClickListener(addRemoteSensorListener);
        ImageButton addLocalSensor = (ImageButton) findViewById(R.id.add_local_sensor_pairing);
        addLocalSensor.setOnClickListener(addLocalSensorListener);
    }

    /**
     * Handler to receive messages from the InputUserSettingsPopupFragment
     */
    private android.os.Handler mHandler = new android.os.Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            //Gets the task from the incoming Message object
            String aResponse = msg.getData().getString("message");
            if (aResponse != null) {
                if (aResponse.equals("success")) {
                    Context context = getBaseContext();
                    CharSequence text = "Server Active: Reply Received";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                    Log.d("MATT!", "Succesful Ping");
                }
                else {
                    Context context = getBaseContext();
                    CharSequence text = "No Reply";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                    Log.d("MATT!", "Unsucessful Ping");
                }
            }
        }
    };

    //Listener that brings up the UDPSettingsPopup for Sensor Adding
    private View.OnClickListener addRemoteSensorListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Bring up the UDPSettingsFragment to allow user input of new
            //sensors
            FragmentManager fm = getSupportFragmentManager();
            InputUserSettingsPopupFragment settingsFragment = InputUserSettingsPopupFragment.newInstance();
            settingsFragment.setActivityHandler(mHandler);
            settingsFragment.show(fm, "MATT!");
            WifiManager wifiManager = (WifiManager)getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);
            try
            {
                Method disableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                Boolean result = (Boolean) disableWifi.invoke(wifiManager, null, false);
            }
            catch (Exception e)
            {

            }
            MainActivity.mode = true;
        }
    };
    //Listerner that enables mobile hotspot
    private View.OnClickListener addLocalSensorListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Bring up the UDPSettingsFragment to allow user input of new
            //sensors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                while (!Settings.System.canWrite(getBaseContext().getApplicationContext()))
                {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getBaseContext().getApplicationContext().getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(intent, 0);
                }
            }
            enableHotspot();
            MainActivity.mode = false;
            int counter = 0;
            while (getClientList().size() == 0)
            {
                if (counter > 50) {break; }
            }
            ArrayList<String> connectedLocalSensors = getClientList();
            for (String ipAdd : connectedLocalSensors)
            {
                addUDPSensorToConnectedList(ipAdd, 0, 8080);
            }

            /*TableLayout connectedSensorsLayout = (TableLayout) findViewById(R.id.csTableLayout);
            int counter = 0;

            ArrayList<String> sensorCardTitles = getClientList();
            for (String ipAdd : sensorCardTitles)
            {
                TextView textView = new TextView(getBaseContext().getApplicationContext());
                textView.setText(ipAdd);
                textView.setTextColor(0xff000000);
                TableRow newRow = new TableRow(getBaseContext().getApplicationContext());
                newRow.addView(textView);
                TableLayout.LayoutParams layout = new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);
                connectedSensorsLayout.addView(newRow, layout);
            }*/
        }
    };

    //====================================MOBILE HOTSPOT HELPER METHODS======================================

    private ArrayList<String> getClientList() {
        ArrayList<String> clientList = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] clientInfo = line.split(" +");
                String mac = clientInfo[3];
                if (mac.matches("..:..:..:..:..:..")) { // To make sure its not the title
                    clientList.add(clientInfo[0]);
                }
            }
        } catch (java.io.IOException aE) {
            aE.printStackTrace();
            return null;
        }
        return clientList;
    }
    private void enableHotspot() {
        boolean result = false;
        WifiManager wifiManager = (WifiManager)getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(false);
        }
        ConnectivityManager cman = (ConnectivityManager) getBaseContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        try
        {
            wifiManager.setWifiEnabled(false);
            Method enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            String ssid  =   "test2";//your SSID
            String pass  =   "12345678";// your Password
            WifiConfiguration  myConfig =  new WifiConfiguration();
            myConfig.SSID = ssid;
            myConfig.preSharedKey  = pass ;
            myConfig.status = WifiConfiguration.Status.ENABLED;
            myConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            myConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            myConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            result = (Boolean) enableWifi.invoke(wifiManager, myConfig, true);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            result = false;
        }
    }

//=======================================ONDATAPASS IMPLEMENTATION==============================================

    public void onDataPassUdpSettings(String verifiedHostname, int verifiedLocalPort, int verifiedRemotePort) {
        //Send the Data to the DataBase
        //Check if data is already in the list
        //Repeated Local Ports not allowed in the InputUserSettingsPopupFragment
        if(!connected_host_names.contains(verifiedHostname)) {
            //Add the verifiedSensor to the list of Connected Sensors
            addUDPSensorToConnectedList(verifiedHostname, verifiedLocalPort, verifiedRemotePort);
            connected_host_names.add(verifiedHostname);
            used_local_ports.add(""+verifiedLocalPort);
            //Add sensorToDataBase
            addUDPSettingsToDataBase(verifiedHostname,""+verifiedLocalPort, ""+verifiedRemotePort);
            Log.d("MATT!", "Passed Data/Connected");
        }
        else{
            Context context = getBaseContext();
            CharSequence text = "Already Connected";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            Log.d("MATT!", "Already Connected");
        }
    }

    public boolean isLocalPortUsed(String localPort){
        for(String usedLocalPort: used_local_ports){
            if(usedLocalPort.equals(localPort))return true;
        }
        return false;
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        mDbHelper.close();
        db.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.wireless_pairing_action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.home_button) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //===================================PAIRED SENSORS RECYCLER VIEW===============================================

    public void addUDPSensorToConnectedList(String verifiedHostname, int verifiedLocalPort, int verifiedRemotePort){
        String verifiedLocalPortString = "" + verifiedLocalPort;
        String verifiedRemotePortString = "" + verifiedRemotePort;
        ArrayList<String> dataToAdd = new ArrayList<>(Arrays.asList(verifiedHostname,verifiedLocalPortString, verifiedRemotePortString));
        mAdapter.addDataSet(dataToAdd);
    }

    private void removeUDPSensorFromConnectedList(String hostname){
        mAdapter.removeDataSet(hostname);
        int index = 0;
        for(String host: connected_host_names){
            if(host.equals(hostname)) break;
            index++;
        }
        connected_host_names.remove(index); //Remove the requested hostname from the connectedSensorList
        used_local_ports.remove(index);
    }

    private class PairingHolder extends RecyclerView.ViewHolder {

        private TextView remotePort;
        private TextView localPort;
        private TextView remoteHost;
        private ImageButton removeButton;
        private Button pingButton;

        private PairingHolder (View itemView){  //This must be called at least once per item...
            super(itemView);
            remotePort = (TextView) itemView.findViewById(R.id.paired_sensor_remote_port_text_view);
            localPort = (TextView) itemView.findViewById(R.id.paired_sensor_local_port_text_view);
            remoteHost = (TextView) itemView.findViewById(R.id.remote_hostname_textview);
            removeButton = (ImageButton) itemView.findViewById(R.id.remove_pairing_image); //Press this button to remove the view
            pingButton = (Button) itemView.findViewById(R.id.ping_connected_pair);
            removeButton.setOnClickListener(removeButtonListener);
            pingButton.setOnClickListener(pingButtonListener);
        }


        private View.OnClickListener pingButtonListener = new View.OnClickListener(){

            public void onClick(View v){
                int localPortVal = Integer.parseInt(localPort.getText().toString());
                int remotePortVal = Integer.parseInt(remotePort.getText().toString());
                String remoteHostVal = remoteHost.getText().toString();
                UdpClient client = new UdpClient(remoteHostVal,remotePortVal,localPortVal,1);
                UdpClient.UdpServerAcknowledge udpPinger = client.new UdpServerAcknowledge(mHandler);
                udpPinger.start(); //Runs on a seperate thread then closes when done.
            }

        };

        private View.OnClickListener removeButtonListener = new View.OnClickListener(){

            public void onClick(View v){
                String host = remoteHost.getText().toString();
                removeUDPSensorFromConnectedList(host); //Remove the sensor in list by IDing the hostname
                deleteSingleUDPDataSetting(host); //Remove sensor from database
            }

        };

    }

    private class PairingListAdapter extends RecyclerView.Adapter<PairingHolder>{

        private ArrayList<ArrayList<String>> mdataSet; //Each entry is an array of ['hostname', 'localport', 'remoteport']

        private PairingListAdapter(ArrayList<ArrayList<String>> dataSet){
            mdataSet = dataSet;
        }

        //Create new views
        @Override
        public PairingHolder onCreateViewHolder(ViewGroup parent, int viewType){
            //This is called whenever a new instance of ViewHolder is created
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View v = layoutInflater.inflate(R.layout.paired_sensor_fragment,parent,false);
            return new PairingHolder(v);
        }

        // Replace the contents of a view (invoked by layout manager)
        @Override
        public void onBindViewHolder(PairingHolder ph, int position){
            //Called whenever the SO binds the view with the data...in otherwords the
            //data is shown in the UI
            ph.remotePort.setText(mdataSet.get(position).get(2));
            ph.localPort.setText(mdataSet.get(position).get(1));
            ph.remoteHost.setText(mdataSet.get(position).get(0));
        }

        @Override
        public int getItemCount(){
            return mdataSet.size();
        }

        private void addDataSet(ArrayList<String> dataToAdd){
            mdataSet.add(dataToAdd);
            notifyItemInserted(getItemCount()-1); //Tell layout manager we have an update
        }

        private void removeDataSet(String hostname){
            int position = 1000;
            int index = 0;
            for(ArrayList sensorData: mdataSet){
                if(sensorData.get(0).equals(hostname)) position = index;
                index++;
            }
            if(position != 1000) {
                mdataSet.remove(position);
                notifyItemRemoved(position); //Tell layout manager we have an update
            }
        }

    }


    //======================================DATABASE MANIPULATION=================================================

    private void addUDPSettingsToDataBase(String IPAddress, String localPort, String remotePort){
        //Adding a row to the database
        ContentValues row_value = new ContentValues();
        row_value.put(UDPDatabaseContract.UdpDataEntry.COLUMN_NAME_IP_HOST, IPAddress);
        row_value.put(UDPDatabaseContract.UdpDataEntry.COLUMN_NAME_LOCAL_PORT, localPort);
        row_value.put(UDPDatabaseContract.UdpDataEntry.COLUMN_NAME_REMOTE_PORT, remotePort);
        db.insert(UDPDatabaseContract.UdpDataEntry.TABLE_NAME, null, row_value);
        Log.d("MATT!", "Added UDPSensor to DATABASE");
    }

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

    private void deleteSingleUDPDataSetting(String hostname){
        String selection = UDPDatabaseContract.UdpDataEntry. COLUMN_NAME_IP_HOST + " LIKE ?";
        String[] selectionArgs = {hostname}; //Matches the hostname string
        //Deletes all rows with columns that have values that equal the variable hostname
        db.delete(UDPDatabaseContract.UdpDataEntry.TABLE_NAME, selection, selectionArgs);
        Log.d("MATT!", "Deleted Sensor from DataBase");
    }
}

package Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mattmellor.smartshoegrapher.MainActivity;
import com.mattmellor.smartshoegrapher.R;
import com.mattmellor.smartshoegrapher.UdpClient;
import com.scichart.charting.model.dataSeries.IXyDataSeries;
import com.scichart.charting.modifiers.LegendModifier;
import com.scichart.charting.modifiers.SourceMode;
import com.scichart.charting.visuals.SciChartSurface;
import com.scichart.charting.visuals.axes.NumericAxis;
import com.scichart.charting.visuals.renderableSeries.FastLineRenderableSeries;
import com.scichart.core.annotations.Orientation;
import com.scichart.core.framework.UpdateSuspender;
import com.scichart.drawing.common.BrushStyle;
import com.scichart.drawing.common.FontStyle;
import com.scichart.drawing.common.SolidBrushStyle;
import com.scichart.drawing.utility.ColorUtil;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import SciChartUserClasses.SciChartBuilder;


/**
 * Created by Matthew on 8/15/2016.
 * Fragment to hold a single graph and its underlying UDP Data Collection
 * Real Time Graphing is implemented in this class
 * TODO: This class needs to be changed to handle 24 sensors instead of 6
 * Which will be quite difficult
 */

public class GraphFragmentLocal extends Fragment {

    //UDP Settings
    private UdpClient client;//This is the client that gives up all the the data
    private UdpClient client1;
    private UdpClient client2;
    private String hostname;  //hostname specifying the
    private int remotePort;
    private int localPort;
    private boolean applyPressed = false;


    //Allows Communication With Other Threads Outside GraphFragment class
    private Handler handler;

    private boolean listenerExists = false;
    private boolean startAlreadyPressed = false;
    private int xBound = 10_000;
    private int yBound = 5000;
    private int validDataLength = 80;
    private String graphtitle = "Sensor Values vs. Number of Samples";
    private String xaxis = "Number of Samples";
    private String yaxis = "Sensor Values";
    private int xscale = 100000;
    private int yscale = 5000;

    private SciChartSurface plotSurface;
    private GraphDataSource dataSource; // has a handler to receive data
    protected final SciChartBuilder sciChartBuilder = SciChartBuilder.instance();

   // private ArrayList<>

    private final IXyDataSeries<Integer, Integer> dataSeriesSensor1 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor2 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor3 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor4 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor5 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor6 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor7 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor8 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor9 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor10 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor11 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor12 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor13 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor14 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor15 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
    private final IXyDataSeries<Integer, Integer> dataSeriesSensor16 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();

//    private final IXyDataSeries<Integer, Integer> dataSeriesSensor7 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
//    private final IXyDataSeries<Integer, Integer> dataSeriesSensor8 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
//    private final IXyDataSeries<Integer, Integer> dataSeriesSensor9 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
//    private final IXyDataSeries<Integer, Integer> dataSeriesSensor10 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
//    private final IXyDataSeries<Integer, Integer> dataSeriesSensor11 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();
//    private final IXyDataSeries<Integer, Integer> dataSeriesSensor12 = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class).build();

    private ArrayList<IXyDataSeries<Integer,Integer>> dataSeriesList = new ArrayList<>(Arrays.asList(dataSeriesSensor1,dataSeriesSensor2,
            dataSeriesSensor3, dataSeriesSensor4, dataSeriesSensor5, dataSeriesSensor6, dataSeriesSensor7, dataSeriesSensor8, dataSeriesSensor9, dataSeriesSensor10,
            dataSeriesSensor11, dataSeriesSensor12, dataSeriesSensor13, dataSeriesSensor14, dataSeriesSensor15, dataSeriesSensor16));

    private int xCounter = 0;
    private int xCounter2 = 0;
    private ArrayList<String[]> firstList = new ArrayList<>();
    private ArrayList<String[]> secondList = new ArrayList<>();

    @Override //inflate the fragment view in the mainActivity view
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View frag = inflater.inflate(R.layout.graph_fragment, container, false);

        plotSurface = (SciChartSurface) frag.findViewById(R.id.dynamic_plot);

        //Ensure that the license is added & SciChart Runs
        try{
            plotSurface.setRuntimeLicenseKey(
                    "<LicenseContract>\n" +
                            "<Customer>MIT</Customer>\n" +
                            "<OrderId>EDUCATIONAL-USE-0016</OrderId>\n" +
                            "<LicenseCount>5</LicenseCount>\n" +
                            " <IsTrialLicense>false</IsTrialLicense>\n" +
                            "<SupportExpires>05/31/2017 00:00:00</SupportExpires>\n\n " +
                            " <ProductCode>SC-ANDROID-2D-PRO</ProductCode>\n\n" +
                            " <KeyCode>57a1d37ef5811a3a3b905505a94bf08ba741a706b8768fdf434c05eb7eb2f5b58dc39039e24ff0c0e00b4385838e9ac44154fd7013b2836e7891a2281fe154a3b9915757a401e0978bc1624be61e2a53abc19a3af1f3fb11bdda0c794d1fa7bbad9acc094d884ed540cb3b841926710daa5ee7b433bb77b1d2fd317e8c499fd9db7e38973b4853351c22bc41c49cf4b5b5dc3b1c78d298313be1b071d649229f</KeyCode>\n" +
                            "</LicenseContract>" );
        }catch (Exception e){
            e.printStackTrace();
        }

        UpdateSuspender.using(plotSurface, new Runnable() {
            @Override
            public void run() {
                final NumericAxis xAxis = sciChartBuilder.newNumericAxis().withVisibleRange(0,xscale).build();
                xAxis.setAxisTitle(xaxis); //TODO: This is how you can the xAxisTitle
                final NumericAxis yAxis = sciChartBuilder.newNumericAxis().withVisibleRange(0,yscale).build();
                yAxis.setAxisTitle(yaxis); //TODO: This is how you can change the yAxisTitle

                FontStyle labelStylex = new FontStyle(20, ColorUtil.Green);
                FontStyle axisStylex = new FontStyle(50, ColorUtil.Green);
                xAxis.setTickLabelStyle(labelStylex);
                xAxis.setTitleStyle(axisStylex);

                FontStyle labelStyley = new FontStyle(20, ColorUtil.Yellow);
                FontStyle axisStyley = new FontStyle(50, ColorUtil.Yellow);
                yAxis.setTickLabelStyle(labelStyley);
                yAxis.setTitleStyle(axisStyley);

                BrushStyle bandStyle = new SolidBrushStyle(0x22279B27);
                xAxis.setAxisBandsStyle(bandStyle);
                yAxis.setAxisBandsStyle(bandStyle);



                LegendModifier legendModifier = new LegendModifier(getActivity());
                legendModifier.setShowLegend(true);
                legendModifier.setGetLegendDataFor(SourceMode.AllVisibleSeries);
                legendModifier.setOrientation(Orientation.HORIZONTAL);
                 /*String labelAnnotation = new TextAnnotation(); Still working on it*/
                //These are wrappers for the series we added the data to...It contains the formatting
                final FastLineRenderableSeries rs1 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor1).withStrokeStyle(ColorUtil.argb(0xFF, 0x40, 0x83, 0xB7)).build(); //Light Blue Color
                final FastLineRenderableSeries rs2 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor2).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0xA5, 0x00)).build(); //Light Pink Color
                final FastLineRenderableSeries rs3 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor3).withStrokeStyle(ColorUtil.argb(0xFF, 0xE1, 0x32, 0x19)).build(); //Orange Red Color
                final FastLineRenderableSeries rs4 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor4).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0xFF, 0xFF)).build(); //White color
                final FastLineRenderableSeries rs5 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor5).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0xFF, 0x99)).build(); //Light Yellow color
                final FastLineRenderableSeries rs6 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor6).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0x99, 0x33)).build(); //Light Orange color
                final FastLineRenderableSeries rs7 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor7).withStrokeStyle(ColorUtil.argb(0xFF, 0x40, 0x83, 0xB7)).build(); //Light Blue Color
                final FastLineRenderableSeries rs8 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor8).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0xA5, 0x00)).build(); //Light Pink Color
                final FastLineRenderableSeries rs9 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor9).withStrokeStyle(ColorUtil.argb(0xFF, 0xE1, 0x32, 0x19)).build(); //Orange Red Color
                final FastLineRenderableSeries rs10 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor10).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0xFF, 0xFF)).build(); //White color
                final FastLineRenderableSeries rs11 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor11).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0xFF, 0x99)).build(); //Light Yellow color
                final FastLineRenderableSeries rs12 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor12).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0x99, 0x33)).build(); //Light Orange color
                final FastLineRenderableSeries rs13 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor13).withStrokeStyle(ColorUtil.argb(0xFF, 0xE1, 0x32, 0x19)).build(); //Orange Red Color
                final FastLineRenderableSeries rs14 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor14).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0xFF, 0xFF)).build(); //White color
                final FastLineRenderableSeries rs15 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor15).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0xFF, 0x99)).build(); //Light Yellow color
                final FastLineRenderableSeries rs16 = sciChartBuilder.newLineSeries().withDataSeries(dataSeriesSensor16).withStrokeStyle(ColorUtil.argb(0xFF, 0xFF, 0x99, 0x33)).build(); //Light Orange color


                Collections.addAll(plotSurface.getXAxes(), xAxis);
                Collections.addAll(plotSurface.getYAxes(), yAxis);
                Collections.addAll(plotSurface.getRenderableSeries(), rs1, rs2, rs3, rs4, rs5, rs6,rs7, rs8, rs9, rs10, rs11, rs12, rs13, rs14, rs15, rs16);
            }
        });

        dataSource = new GraphDataSource(); //Run the data receiving & handling on a separate thread
        dataSource.start(); //Starts the thread running/open to receive data

        return frag; //have to return fragment at the end of onCreateView
    }

    //-------------Get Data, Manipulate Data & Notify PlotUpdater-----------
    public class GraphDataSource extends Thread{
        int port = 8080;
        byte[] buffer;
        DatagramPacket packet;
        DatagramSocket socket;
        String recievedData;

        public void run(){

            Looper.prepare();
            //Get the data from the UDP Data Class when its available
            buffer = new byte[2048];
            packet = new DatagramPacket(buffer, buffer.length);
            handler = new Handler(){
                public void handleMessage(Message msg){
                    try {
                        socket = new DatagramSocket(port);
                    } catch (SocketException e) {
                        //message.setText("EXCEPTION THROWN: could not create new DatagramSocket");
                        Log.e("EXCEPTION THROWN", "could not create new DatagramSocket");
                    }
                    try {
                        socket.receive(packet);
                    } catch (IOException e) {
                        Log.e("EXCEPTION THROWN", "could not receive packet");
                    }
                    recievedData = new String(buffer, 0, packet.getLength());
                    socket.close();
                    String[] dataSplit = splitStringEvery(recievedData, 4);
                    if (dataValid(dataSplit)) {
                        final ArrayList<Integer> orderedData = new ArrayList<>();
                        for (int i = 0; i < dataSplit.length; i += 5) {
                            orderedData.add(Integer.parseInt(dataSplit[i]));
                            orderedData.add(Integer.parseInt(dataSplit[i + 1]));
                            orderedData.add(Integer.parseInt(dataSplit[i + 2]));
                            orderedData.add(Integer.parseInt(dataSplit[i + 3]));
                        }
                        UpdateSuspender.using(plotSurface, new Runnable() {    //This updater graphs the values
                            @Override
                            public void run() {
                                addDataToSeries(orderedData); //Adding the data to the graph and drawing it
                            }
                            //TODO: Try adding UdpateSuspender somewhere else so that all 12 sensors are added at once
                            //TODO: ^continue above s.t. addDataToSeries(data) has data that is all of the data (12 series)
                        });
                    }
                }
            };
            Looper.loop();
        }
        private String[] splitStringEvery(String s, int interval) {
            int arrayLength = (int) Math.ceil(((s.length() / (double)interval)));
            String[] result = new String[arrayLength];

            int j = 0;
            int lastIndex = result.length - 1;
            for (int i = 0; i < lastIndex; i++) {
                result[i] = s.substring(j, j + interval);
                j += interval;
            } //Add the last bit
            result[lastIndex] = s.substring(j);
            return result;
            //https://stackoverflow.com/questions/12295711/split-a-string-at-every-nth-position
        }
        private boolean dataValid(String[] array)
        {
            int counter = 0;
            boolean validity = true;
            for (int i = 0; i < array.length; i+=5)
            {
                int checksum = (Integer.parseInt(array[i])+Integer.parseInt(array[i+1])*3+Integer.parseInt(array[i+2])*5+Integer.parseInt(array[i+3])*7)%6;
                if (Integer.parseInt(array[i+4]) != checksum)
                {
                    validity = false;
                    break;
                }
            }
            return validity;
        }

        /**
         *
         * @param data string of the udp data
         * @return true if the data isn't corrupted..aka the correct length
         */
        private boolean dataValid(String data) {
            return ((data.length() == validDataLength)); //TODO make more robust
        }

        private void addDataToSeries(ArrayList<Integer> data){
            //Add x_1, y_1 set to IXySeries
            dataSeriesList.get(0).append(data.get(0), data.get(1));
            dataSeriesList.get(1).append(data.get(2), data.get(3));
            dataSeriesList.get(2).append(data.get(4), data.get(5));
            dataSeriesList.get(3).append(data.get(6), data.get(7));
            dataSeriesList.get(4).append(data.get(8), data.get(9));
            dataSeriesList.get(5).append(data.get(10), data.get(11));
            dataSeriesList.get(6).append(data.get(12), data.get(13));
            dataSeriesList.get(7).append(data.get(14), data.get(15));
            dataSeriesList.get(8).append(data.get(16), data.get(17));
            dataSeriesList.get(9).append(data.get(18), data.get(19));
            dataSeriesList.get(10).append(data.get(20), data.get(21));
            dataSeriesList.get(11).append(data.get(22), data.get(23));
            dataSeriesList.get(12).append(data.get(24), data.get(25));
            dataSeriesList.get(13).append(data.get(26), data.get(27));
            dataSeriesList.get(14).append(data.get(28), data.get(29));
            dataSeriesList.get(15).append(data.get(30), data.get(31));
        }
    }

    //---------------GraphFragment methods---------------

    /**
     * If there isn't already a data listener create one
     * and start listening to data. Data listener notifies the UI thread
     * each time it has a new data packet full of valid data
     * UI thread then graphes it (not implemented)
     */
    public void startGraphing(){
          //if(!startAlreadyPressed) {
          if (!listenerExists) {
              Log.d("MATT!", "Creating connection... pressed start graphing");
              resetGraph();
              listenerExists = true;
              client1 = new UdpClient("footsensor2.dynamic-dns.net", 2391,5007,1);
              client1.setStreamData(true);
              UdpClient.UdpDataListener listener1 = client1.new UdpDataListener(handler, "ANDREW");
              listener1.start();
              //Log.d("MATT!", "Trying Client 2");
              //UdpClient client2 = new UdpClient("footsensor2.dynamic-dns.net", 2392, 5010, 45);
              //client2.setStreamData(true);
              //UdpClient.UdpDataListener listener2 = client2.new UdpDataListener(handler, "MATT");
              //listener2.start();

//                  client = new UdpClient(hostname, remotePort, localPort, 45); //Creates the client with the Updated hostname, remotePort, localPort
//                  client.setStreamData(true);
//                  UdpClient.UdpDataListener listener = client.new UdpDataListener(handler); //Handler has been waiting in the background for data(Since onCreateView)..It is the handler in GraphDataSource
//                  listener.start(); //TODO: Make sure to close listener thread
          }
              //startAlreadyPressed = true;
          //}
    }

    private void resetGraph(){
        xCounter = 0;
        UpdateSuspender.using(plotSurface, new Runnable() {
            @Override
            public void run() {
                for(IXyDataSeries<Integer, Integer> dataSeriesSensor : dataSeriesList){
                    dataSeriesSensor.clear();
                }
            }
        });
    }

    /**
     * Tell the data listener to stop listening to data
     */
    public void stopGraphing(){
        Log.d("MATT!", "Stop Graphing In Graph Fragment");
        if (listenerExists) {
            client1.setStreamData(false);
            listenerExists = false;
        }
        //startAlreadyPressed = false;
    }

    /**
     * Update the remote port
     * @param remotePort of udp server
     */
    public void updateRemotePort(int remotePort){
        this.remotePort = remotePort;
    }

    /**
     * Update the local port
     * @param localPort of udp server
     */
    public void updateLocalPort(int localPort){
        this.localPort = localPort;
    }

    /**
     * Update the hostname
     * @param hostname of udp server
     */
    public void updateHostname(String hostname){
        this.hostname = hostname;
    }

    /* This is to update x-axis,y-axis and title*/
    public void updatexBound(int xBound) { this.xscale= xBound;}

    public void updateyBound(int yBound) { this.yscale= yBound;}

    public void updatetitle(String title) { this.graphtitle= title;}

    public void updatexaxistitle(String title) { this.xaxis= title;}

    public void updateyaxistitle(String title) { this.yaxis= title;}


}

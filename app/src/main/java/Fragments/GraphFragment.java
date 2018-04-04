package Fragments;

import android.content.Context;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.mattmellor.smartshoegrapher.MainActivity;
import com.mattmellor.smartshoegrapher.R;
import com.mattmellor.smartshoegrapher.UdpClient;
import com.scichart.charting.model.dataSeries.IXyDataSeries;
import com.scichart.charting.modifiers.LegendModifier;
import com.scichart.charting.modifiers.SourceMode;
import com.scichart.charting.visuals.SciChartSurface;
import com.scichart.charting.visuals.axes.AutoRange;
import com.scichart.charting.visuals.axes.NumericAxis;
import com.scichart.charting.visuals.annotations.TextAnnotation;
import com.scichart.charting.visuals.renderableSeries.FastLineRenderableSeries;
import com.scichart.charting.visuals.renderableSeries.IRenderableSeries;
import com.scichart.core.annotations.Orientation;
import com.scichart.core.framework.UpdateSuspender;
import com.scichart.data.model.DoubleRange;
import com.scichart.data.model.ISciList;
import com.scichart.drawing.common.BrushStyle;
import com.scichart.drawing.common.FontStyle;
import com.scichart.drawing.common.PenLineCap;
import com.scichart.drawing.common.PenStyle;
import com.scichart.drawing.common.SolidBrushStyle;
import com.scichart.drawing.utility.ColorUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import SciChartUserClasses.SciChartBuilder;


/**
 * Created by Matthew on 8/15/2016.
 * Fragment to hold a single graph and its underlying UDP Data Collection
 * Real Time Graphing is implemented in this class
 * TODO: This class needs to be changed to handle 24 sensors instead of 6
 * Which will be quite difficult
 */

public class GraphFragment extends Fragment {

    //UDP Settings
    private UdpClient client;//This is the client that gives up all the the data;
    private UdpClient client2;
    private UdpClient client1;
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
    private int xscale = 1000;
    private int yscale = 1500;

    private SciChartSurface plotSurface;
    private GraphDataSource dataSource; // has a handler to receive data
    protected final SciChartBuilder sciChartBuilder = SciChartBuilder.instance();

   // creates the 16 graphing lines
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
    private NumericAxis xAxis = null;

    private ArrayList<IXyDataSeries<Integer,Integer>> dataSeriesList = new ArrayList<>(Arrays.asList(dataSeriesSensor1,dataSeriesSensor2,
            dataSeriesSensor3, dataSeriesSensor4, dataSeriesSensor5, dataSeriesSensor6, dataSeriesSensor7, dataSeriesSensor8, dataSeriesSensor9, dataSeriesSensor10,
            dataSeriesSensor11, dataSeriesSensor12, dataSeriesSensor13, dataSeriesSensor14, dataSeriesSensor15, dataSeriesSensor16));

    private int xCounter = 0;

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
        } catch (Exception e){
            e.printStackTrace();
        }
        initChart();
        return frag; //have to return fragment at the end of onCreateView
    }
    private void initChart()
    {
        UpdateSuspender.using(plotSurface, new Runnable() {
            @Override
            public void run() {
                xAxis = sciChartBuilder.newNumericAxis().withAutoRangeMode(AutoRange.Never).build();
                xAxis.setAxisTitle(xaxis); //TODO: This is how you can the xAxisTitle
                final NumericAxis yAxis = sciChartBuilder.newNumericAxis().withVisibleRange(0,yscale).withAutoRangeMode(AutoRange.Always).build();
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

                //ties the 16 graphing lines to the graph
                Collections.addAll(plotSurface.getXAxes(), xAxis);
                Collections.addAll(plotSurface.getYAxes(), yAxis);
                Collections.addAll(plotSurface.getRenderableSeries(), rs1, rs2, rs3, rs4, rs5, rs6, rs7, rs8, rs9, rs10, rs11, rs12, rs13, rs14, rs15, rs16);
            }
        });

        dataSource = new GraphDataSource(); //Run the data receiving & handling on a separate thread]
        //starts listening for data being received by a handler
        dataSource.start();
    }
    //-------------Get Data, Manipulate Data & Notify PlotUpdater-----------
    public class GraphDataSource extends Thread{
        public void run(){
            Looper.prepare();
            {
       //Get the data from the UDP Data Class when its available
                handler = new Handler() {
                    public void handleMessage(Message msg) {
                        if (MainActivity.isRunning){
                            //data is receieved in a long string of numbers
                            String recievedData = (String) msg.obj;
                            //split data every fourth char because that is the format used by
                            //the ESP to convert to 12 bit ints, then populates an String[]
//                            String[] dataSplit = splitStringEvery(recievedData, 4);
                            String[] dataSplit = recievedData.split(",");
//                            String[] finalDataSplit = new String[16];
//                            int fdsIndex = 0;
//                            for (int i = 0; i < 4; i++) {
//                                String padData = dataSplit[i];
//                                String[] newData = splitStringEvery(padData, 4);
//                                for (int j = 0; j < 4; j++) {
//                                    finalDataSplit[fdsIndex] = newData[j];
//                                    fdsIndex++;
//                                }
//                            }
//                            dataSplit = finalDataSplit;
                             //check for errors by comparing the checksum with received data
                            boolean valid = dataValidLocal(dataSplit);
                            if (true) {
                                final ArrayList<Integer> orderedData = new ArrayList<>();
                                int xval = xCounter + 1;
                                StringBuilder sb = new StringBuilder();
                                //populates Arraylist<Int> with dadta from datasplit, separately
                                //by the x value that they correspond with. This format is
                                //important for SciChart.
                                //below is for graphing every datapoint
                                for (int i = 0; i < dataSplit.length; i += 4) {
                                    orderedData.add(xval);
                                    if (Integer.parseInt(dataSplit[i]) > 3000) {
                                        int badNumber = Integer.parseInt(dataSplit[i]);
                                    }
                                    orderedData.add(Integer.parseInt(dataSplit[i]));
                                    orderedData.add(xval);
                                    if (Integer.parseInt(dataSplit[i+1]) > 3000) {
                                        int badNumber = Integer.parseInt(dataSplit[i]);
                                    }
                                    orderedData.add(Integer.parseInt(dataSplit[i + 1]));
                                    orderedData.add(xval);
                                    if (Integer.parseInt(dataSplit[i+2]) > 3000) {
                                        int badNumber = Integer.parseInt(dataSplit[i]);
                                    }
                                    orderedData.add(Integer.parseInt(dataSplit[i + 2]));
                                    orderedData.add(xval);
                                    if (Integer.parseInt(dataSplit[i+3]) > 3000) {
                                        int badNumber = Integer.parseInt(dataSplit[i]);
                                    }
                                    orderedData.add(Integer.parseInt(dataSplit[i + 3]));
                                    xval++;
                                }
                                //below is for graphing every fourth datapoint
                                /*orderedData.add(xval);
                                orderedData.add(Integer.parseInt(dataSplit[0]));
                                orderedData.add(xval);
                                orderedData.add(Integer.parseInt(dataSplit[1]));
                                orderedData.add(xval);
                                orderedData.add(Integer.parseInt(dataSplit[2]));
                                orderedData.add(xval);
                                orderedData.add(Integer.parseInt(dataSplit[3]));*/
                                xval++;
                                xCounter = xval;
                                UpdateSuspender.using(plotSurface, new Runnable() {    //This updater graphs the values
                                    @Override
                                    public void run() {
                                        addDataToSeriesLocal(orderedData); //Adding the data to the graph and drawing it
                                    }
                                });
                                //shift xAxis to show most recent data
                                xAxis.animateVisibleRangeTo(new DoubleRange((double) xCounter-3000, (double) xCounter), (long) 500);

                            }
                        }
                    }
                };
            }
            Looper.loop();
        }

        private String[] splitStringEvery(String s, int interval) {
//            int arrayLength = (int) Math.ceil(((s.length() / (double)interval)));
            String[] result = new String[4];
            int j = 0;
//            int lastIndex = result.length - 1;
            for (int i = 0; i < 4; i++) {
                result[i] = s.substring(j, j + interval);
                j += interval;
            }
            return result;
            //https://stackoverflow.com/questions/12295711/split-a-string-at-every-nth-position
        }
        private boolean dataValidLocal(String[] array)
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
        private void addDataToSeriesLocal(ArrayList<Integer> data){
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
        private boolean dataValidRemote(String data){
            return ((data.length() == validDataLength)); //TODO make more robust
        }
        private ArrayList<ArrayList<Integer>> spliceDataIntoPointsSets(String[] dataSplit){
            ArrayList<ArrayList<Integer>> orderedData = new ArrayList<>();
            ArrayList<Integer> x1 = new ArrayList<>();
            ArrayList<Integer> x2 = new ArrayList<>();
            ArrayList<Integer> x3 = new ArrayList<>();
            ArrayList<Integer> x4 = new ArrayList<>();
            ArrayList<Integer> x5 = new ArrayList<>();
            ArrayList<Integer> x6 = new ArrayList<>();
            ArrayList<Integer> x7 = new ArrayList<>();
            ArrayList<Integer> x8 = new ArrayList<>();
            ArrayList<Integer> x9 = new ArrayList<>();
            ArrayList<Integer> x10 = new ArrayList<>();
            ArrayList<Integer> x11 = new ArrayList<>();
            ArrayList<Integer> x12 = new ArrayList<>();
            ArrayList<Integer> x13 = new ArrayList<>();
            ArrayList<Integer> x14 = new ArrayList<>();
            ArrayList<Integer> x15 = new ArrayList<>();
            ArrayList<Integer> x16 = new ArrayList<>();

            ArrayList<Integer> y1 = new ArrayList<>();
            ArrayList<Integer> y2 = new ArrayList<>();
            ArrayList<Integer> y3 = new ArrayList<>();
            ArrayList<Integer> y4 = new ArrayList<>();
            ArrayList<Integer> y5 = new ArrayList<>();
            ArrayList<Integer> y6 = new ArrayList<>();
            ArrayList<Integer> y7 = new ArrayList<>();
            ArrayList<Integer> y8 = new ArrayList<>();
            ArrayList<Integer> y9 = new ArrayList<>();
            ArrayList<Integer> y10 = new ArrayList<>();
            ArrayList<Integer> y11 = new ArrayList<>();
            ArrayList<Integer> y12 = new ArrayList<>();
            ArrayList<Integer> y13 = new ArrayList<>();
            ArrayList<Integer> y14 = new ArrayList<>();
            ArrayList<Integer> y15 = new ArrayList<>();
            ArrayList<Integer> y16 = new ArrayList<>();

            int xval = xCounter;
            if(xCounter == 0){
                dataSeriesList.get(0).clear();
                dataSeriesList.get(1).clear();
                dataSeriesList.get(2).clear();
                dataSeriesList.get(3).clear();
                dataSeriesList.get(4).clear();
                dataSeriesList.get(5).clear();
                dataSeriesList.get(6).clear();
                dataSeriesList.get(7).clear();
                dataSeriesList.get(8).clear();
                dataSeriesList.get(9).clear();
                dataSeriesList.get(10).clear();
                dataSeriesList.get(11).clear();
                dataSeriesList.get(12).clear();
                dataSeriesList.get(13).clear();
                dataSeriesList.get(14).clear();
                dataSeriesList.get(15).clear();
            }

            int dataLength = dataSplit.length;
            int i = 1;
            int num = 0;
            while (i < dataLength){
                if(i%4==0){ //todo: change this????
                    xval++;
                }
                if(xval == xscale){ //If we are at xBound... break out of adding data
                    xval = 0;
                    xCounter = 0;
                    break;
                }
                Log.d("MATT", dataSplit[i]); //TODO: Investigate this.. some kind of timing issue:
                num = Integer.parseInt(dataSplit[i]); //TODO: Investigate closing thread of clients
                switch(i){
                    case 1:
                        x1.add(xval);
                        y1.add(num);
                        break;
                    case 2:
                        x2.add(xval);
                        y2.add(num);
                        break;
                    case 3:
                        x3.add(xval);
                        y3.add(num);
                        break;
                    case 4:
                        x4.add(xval);
                        y4.add(num);
                        break;
                    case 5:
                        x5.add(xval);
                        y5.add(num);
                        break;
                    case 6:
                        x6.add(xval);
                        y6.add(num);
                        break;
                    case 7:
                        x7.add(xval);
                        y7.add(num);
                        break;
                    case 8:
                        x8.add(xval);
                        y8.add(num);
                        break;
                    case 9:
                        x9.add(xval);
                        y9.add(num);
                        break;
                    case 10:
                        x10.add(xval);
                        y10.add(num);
                        break;
                    case 11:
                        x11.add(xval);
                        y11.add(num);
                        break;
                    case 12:
                        x12.add(xval);
                        y12.add(num);
                        break;
                    case 13:
                        x13.add(xval);
                        y13.add(num);
                        break;
                    case 14:
                        x14.add(xval);
                        y14.add(num);
                        break;
                    case 15:
                        x15.add(xval);
                        y15.add(num);
                        break;
                    case 16:
                        x16.add(xval);
                        y16.add(num);
                        break;
                }
                i++;
            }
            xCounter = xval;
            orderedData.add(x1);
            orderedData.add(y1);
            orderedData.add(x2);
            orderedData.add(y2);
            orderedData.add(x3);
            orderedData.add(y3);
            orderedData.add(x4);
            orderedData.add(y4);
            orderedData.add(x5);
            orderedData.add(y5);
            orderedData.add(x6);
            orderedData.add(y6);
            orderedData.add(x7);
            orderedData.add(y7);
            orderedData.add(x8);
            orderedData.add(y8);
            orderedData.add(x9);
            orderedData.add(y9);
            orderedData.add(x10);
            orderedData.add(y10);
            orderedData.add(x11);
            orderedData.add(y11);
            orderedData.add(x12);
            orderedData.add(y12);
            orderedData.add(x13);
            orderedData.add(y13);
            orderedData.add(x14);
            orderedData.add(y14);
            orderedData.add(x15);
            orderedData.add(y15);
            orderedData.add(x16);
            orderedData.add(y16);
            return orderedData;
        }
        private void addDataToSeriesRemote(ArrayList<ArrayList<Integer>> data){
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
        MainActivity.isRunning = true;
        if (!listenerExists) {
            Log.d("MATT!", "Creating connection... pressed start graphing");
            //resetGraph();
            listenerExists = true;
            client1 = new UdpClient("footpad.duckdns.org",5013,5013,45); //2391,5007,1 //TODO: CHANGE THIS TO TAKE IN DATA!!!
            client1.setStreamData(true);
            UdpClient.UdpDataListener listener1 = client1.new UdpDataListener(handler, "ANDREW");
            listener1.start();
        }
    }
    public void resetGraph(){
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
    public void stopGraphing(){
        Log.d("MATT!", "Stop Graphing In Graph Fragment");
        if (listenerExists) {
            client1.setStreamData(false);
            listenerExists = false;
        }
        MainActivity.isRunning = false;
    }
    public void updateRemotePort(int remotePort){
        this.remotePort = remotePort;
    }
    public void updateLocalPort(int localPort){
        this.localPort = localPort;
    }
    public void updateHostname(String hostname){
        this.hostname = hostname;
    }
    public void updatexBound(int xBound) { this.xscale= xBound;}
    public void updateyBound(int yBound) { this.yscale= yBound;}
    public void updatetitle(String title) { this.graphtitle= title;}
    public void updatexaxistitle(String title) { this.xaxis= title;}
    public void updateyaxistitle(String title) { this.yaxis= title;}
}

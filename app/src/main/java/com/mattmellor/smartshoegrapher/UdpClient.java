package com.mattmellor.smartshoegrapher;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.scichart.core.framework.UpdateSuspender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;

import static com.mattmellor.smartshoegrapher.MainActivity.mode;

/**
 * Created by Matt Mellor on 8/5/2016.
 * Class that represents a udp wifi client
 * That will accept incoming datagrams(packets)
 * Outer class contains the information about the Udp connection
 *
 * Inner classes are responsible for
 *      1. Pinging the server
 *      2. Reading data from the server
 *
 * Reasoning for this formatting is to avoid having the UI Main thread
 * touch the UDP threads (Avoids errors)
 * Additionally the Android Won't Allow UDP Connection on the Main Thread
 *
 * Benefits of this approach
 *      1. Scalability
 *          Since each instance of UdpClient will have a separate instance of
 *          threads that ping the server and read the data, code should be scalable to
 *          allow for multiple udp connections at once.
 */

public class UdpClient  {

    private DatagramSocket receiveSocket;
    private String serverAddress; //Ip address/hostname
    private int remoteServerPort;
    private int localPort;
    private int bufferLength = 82;
    private int dataSetsPerPacket; // statically determined on esp side
    public DatagramPacket rcvdPacket;
    private boolean streamData = true;

    /**
     * @param ipAddress : String representing the ip Address/hostname of the remote server
     * @param remoteServerPort : int representing the value of the remote port of the server
     * @param localPort: int representing the value of the local port of the server
     * @param dataSetsPerPacket: int representing the number of data sets per packet
     */
    public UdpClient(String ipAddress, int remoteServerPort, int localPort, int dataSetsPerPacket){
        this.remoteServerPort = remoteServerPort;
        this.serverAddress = ipAddress;
        this.dataSetsPerPacket = dataSetsPerPacket;
        this.localPort = localPort;
    }

    /**
     * Inner class for sending a message to the remote server
     * This is a separate class because all networking actions can't occur
     * on the main UI thread
     */
    public class UdpServerAcknowledge extends Thread{

        private Handler handler;

        public UdpServerAcknowledge(Handler handler){
            this.handler = handler;
        }

        public void run(){
            acknowledgeServer();
        }

        private void acknowledgeServer() {
            DatagramSocket pingSocket = null;
            DatagramSocket rplySocket = null;
            String mess = "Ping";
            InetAddress address;
            DatagramPacket packet;
            DatagramPacket rPacket;
            byte[] buf = new byte[8];
            boolean fail = false;
            int port = 5013;
            try {
                //create a new socket to send "Ping" to port 5013 in IP address as defined by DDNS
                pingSocket = new DatagramSocket(port);
                pingSocket.setReuseAddress(true);
                address = InetAddress.getByName("footpad.duckdns.org");
                //address = InetAddress.getByName("18.111.32.1");
                packet = new DatagramPacket(mess.getBytes(), mess.length(), address, port);
                pingSocket.send(packet);
                Log.d("MATT!", "About to wait to receive packet");
                rPacket = new DatagramPacket(buf, buf.length);
//                listens for "rply" on port 5003
                rplySocket = new DatagramSocket(5003);
                rplySocket.setSoTimeout(5000);
                rplySocket.receive(rPacket);
                String received = new String(rPacket.getData(), 0, rPacket.getLength());

                if (received.length() > 0){
                    Log.d("MATT!", "Successful Response from server");
                    //notify a handler that the ping was a success. Handler will open a Toast indicating as such
                    threadMsg("success");
                }
            }catch(SocketTimeoutException e){
                //Send a message to the fragment
                Log.d("MATT!", "TimeoutException in ping");
                fail = true;
            }catch (SocketException e){
                e.printStackTrace();
                Log.e("MATT!", "socket exception");
                fail = true;
            }catch(UnknownHostException e){
                e.printStackTrace();
                Log.e("MATT!", "unknown host exception in ping test");
                fail = true;
            }catch(IOException e) {
                e.printStackTrace();
                Log.e("MATT!", "IOException");
                fail = true;
            }catch(Exception e){
                Log.e("MATT!", "General exception");
                e.printStackTrace();
                fail = true;
            }finally {
                if(fail){
                    threadMsg("fail");
                }
                if(pingSocket != null){
                    pingSocket.close();
                }
                if (rplySocket != null){
                    rplySocket.close();
                }
            }
        }

        private void threadMsg(String msg) {
            if (!msg.equals(null) && !msg.equals("")) {
                Message msgObj = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("message", msg);
                msgObj.setData(b);
                handler.sendMessage(msgObj);
            }
        }
    }

    public class UdpDataListener extends Thread {

        private Handler mhandler;
        private String clientID;

        public UdpDataListener(Handler handler, String clientID){
            this.mhandler = handler; //This will be used to pass data to the Graph Fragment
            this.clientID = clientID;
            //The handler specified here is the handler from the GraphDataSource inner class of GraphFragment
        }

        public void run(){
            if (mode) //if using WAN method (mode = true means WAN)
            {
                DatagramSocket senderSocket = null;
                DatagramSocket listenerSocket = null;
                try {
                    String mess = "Android Data Receiver";
                    senderSocket = new DatagramSocket(localPort);
                    InetAddress address = InetAddress.getByName("footpad.duckdns.org");
                    DatagramPacket packet = new DatagramPacket(mess.getBytes(), mess.length(), address, localPort);
                    senderSocket.send(packet);
                    senderSocket.close();
                }
                catch (Exception e)
                {
                    Log.e("IRIS", "exception thrown");
                }
                while(true) {
                    byte[] buffer = new byte[2048];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        listenerSocket = new DatagramSocket(5003);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                    String recievedData;
                    try {
                        listenerSocket.receive(packet);
                    } catch (IOException e) {
                        Log.e("EXCEPTION THROWN", "could not receive packet");
                    }
                    recievedData = new String(buffer, 0, packet.getLength());
                    threadMsgLocal(recievedData);
                    listenerSocket.close();
                }

            }
            //if running LAN mode
            else while (true) localThread();
        }

        private void localThread()
        {
            while (streamData)
            {
                //keeps listening for long strings of data coming into port 5003
                int port = 5003;
                byte[] buffer = new byte[80]; //16 numbers per pad, 4 pads = 64 numbers + 4 commas
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);;
                DatagramSocket socket = null;
                String recievedData;
                try {
                    socket = new DatagramSocket(port);
                    socket.receive(packet);
                    recievedData = new String(buffer, 0, buffer.length);
                    boolean badData = false;
                    for (int i = 0; i < buffer.length; i++) {
                        if (buffer[i] == 0)
                        {
                            badData = true;
                            break;
                        }
                    }
                    if (badData == false)
                    {
                        socket.close();
                        //sends long string of data to a handler (see GraphFragment to see how its handled)
                        threadMsgLocal(recievedData);
                    }
                }
                catch (SocketException e) {
                    //message.setText("EXCEPTION THROWN: could not create new DatagramSocket");
                    Log.e("EXCEPTION THROWN", "could not create new DatagramSocket");
                }
//                try {
//                    socket.receive(packet);
//                }
                catch (IOException e) {
                    Log.e("EXCEPTION THROWN", "could not receive packet");
                }
//                recievedData = new String(buffer, 0, buffer.length);
//                boolean badData = false;
//                for (int i = 0; i < buffer.length; i++) {
//                    if (buffer[i] == 0)
//                    {
//                        badData = true;
//                        break;
//                    }
//                }
//                if (badData == false)
//                {
//                    socket.close();
//                    //sends long string of data to a handler (see GraphFragment to see how its handled)
//                    threadMsgLocal(recievedData);
//                }
            }

        }
        private void threadMsgLocal(String s)
        {
            Message msg = Message.obtain(); // Creates an new Message instance
            msg.obj = s; // Put the string into Message, into "obj" field.
            msg.setTarget(mhandler); // Set the Handler
            try
            {
                msg.sendToTarget();
            }
            catch (Exception e)
            {
                Log.e("IRIS!", "exception thrown");
            }
        }
    }


    //---------------Getter and Setter Methods()------------------


    /**
     *
     * @param streamData
     */
    public void setStreamData(boolean streamData) {
        this.streamData = streamData;
    }
}

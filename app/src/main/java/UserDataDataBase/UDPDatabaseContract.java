package UserDataDataBase;

import android.provider.BaseColumns;

/**
 * Created by Matthew on 11/3/2016.
 * This class is a SQLite database Contract class
 * It is meant to specify the layout(Schema) of the database
 */

public final class UDPDatabaseContract {

    //To avoid accidentally instantiating this class
    private UDPDatabaseContract(){};


    /* Inner class that defines the table contents
    *  We want a 3 column X row table. The 3 columns are for UDP data
    *  representing remote host, local port, and remote port*/
    public static class UdpDataEntry implements BaseColumns{
        public static final String TABLE_NAME = "UserUDPSensors";
        public static final String COLUMN_NAME_IP_HOST = "IP_HOSTNAME";
        public static final String COLUMN_NAME_LOCAL_PORT = "LOCAL_PORT";
        public static final String COLUMN_NAME_REMOTE_PORT = "REMOTE_PORT";

        //SQL format for CREATE TABLE
        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        UdpDataEntry.COLUMN_NAME_IP_HOST + " TEXT," +
                        UdpDataEntry.COLUMN_NAME_LOCAL_PORT + " TEXT," +
                        UdpDataEntry.COLUMN_NAME_REMOTE_PORT + " TEXT" + ")";

        //SQL format for DROPing a table (Deleting a table)
        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + UdpDataEntry.TABLE_NAME;
    }



}

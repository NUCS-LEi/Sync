package org.synclibrary.sync;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.os.Environment;

public class DatabaseDictionary {
	//set the phone local database name here:
	public final static String STAT_DATABASE_NAME = "sync_db";
	//set the project package name here:
	public static String PACKAGE_NAME = "org.sync.test";
	//database DB filename is same as database name
	public static String internalDBFile = STAT_DATABASE_NAME;
	//set the export path on SD card here:
	public static String externalDBPath = PACKAGE_NAME+"/";
	//set the export DB filename here:
	public static String externalDBFile = STAT_DATABASE_NAME;
	

	
	
	
	public final static int DATABASE_VERSION = 1;
	public static SimpleDateFormat normalDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat priorDateFormat = new SimpleDateFormat("MM/dd/yyyy");
	public static SimpleDateFormat lastModDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	public static SimpleDateFormat exactDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS", Locale.US);
	public static String LAST_MOD_DATE = "LastModDate";
	public static String internalDBPath = "/data" + Environment.getDataDirectory().getAbsolutePath() + "/" + PACKAGE_NAME + "/databases/";
	
	public static String internalDBPathFile = internalDBPath + internalDBFile;
	public static String externalDBPathFile = externalDBPath + externalDBFile;

	public final static String SYNC_TIME_TABLE = "sync_time";

	public static SimpleDateFormat sqliteFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat serverFormat = new SimpleDateFormat("MM/dd/yyyy");

	public final static String COL_SPLIT = "@c@";
	public final static String ROW_SPLIT = "@r@";
	public final static String PAGE_SPLIT = "@p@";

	public static String[] lastSyncTimeTableParams = new String[] { "sync_time",
			"PhoneID TEXT NOT NULL," + "TableName TEXT DEFAULT NULL," + "LastSyncTime TEXT DEFAULT NULL," + "PRIMARY KEY (PhoneID,TableName)" };
}

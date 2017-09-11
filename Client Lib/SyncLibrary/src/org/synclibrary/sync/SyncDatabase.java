package org.synclibrary.sync;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.synclibrary.schema.SyncSchema;
import org.synclibrary.schema.Table;
import org.synclibrary.util.FileHelper;
import org.synclibrary.util.Util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.telephony.TelephonyManager;

public class SyncDatabase {
	private static final String TAG = "SyncDB";
	// set server address here:
	private static String SERVER_ADDRESS = "http://cityproject.media.mit.edu/sync_test";

	private static String SYNC_URL = SERVER_ADDRESS + "/sync_library/sync_table.php";
	private static String SYNC_DONE_URL = SERVER_ADDRESS + "/sync_library/sync_done.php";
	private static String SYNC_RESET_URL = SERVER_ADDRESS + "/sync_library/sync_reset.php";
	public static String SYNC_SCHEMA_URL = SERVER_ADDRESS + "/sync_library/sync_schema.php";
	public static String SYNC_TABLE_SCHEMA_URL = SERVER_ADDRESS + "/sync_library/sync_table_schema.php";
	public static String TABLE_UPDATE_TIME_URL = SERVER_ADDRESS + "/sync_library/table_update_time.php";
	private static String INTERNAL_NON_INITIAL_SYNC_FLAGFILE = "NON_INITIAL_SYNC_FLAG";
	public static boolean inSyncing = false;
	private static String downsyncstring;
	private static String[][] downSyncArray;
	private static String UN_SYNC_TABLE1 = "sync_time";

	// private static String UN_SYNC_TABLE1 = "android_metadata";
	// private static String UN_SYNC_TABLE2 = "schema_update";

	public static void resetLastSyncTime(TelephonyManager tm, String tablename) {
		String response;
		if (tablename == null)
			tablename = "";
		try {
			response = Util.convertStreamToString(sync_transit(SYNC_RESET_URL, new String[][] { { "phoneID", tm.getDeviceId() },
					{ "tablename", tablename } }));
			if (response == null || !response.equals("reset")) {
				android.util.Log.i(TAG, "Last_Sync_Time on server reset failed");
				return;
			}
			android.util.Log.i(TAG, "Last_Sync_Time on server reset success");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sync_database(Context context) {
		long start = System.currentTimeMillis();
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (!FileHelper.internalFileExists(context, INTERNAL_NON_INITIAL_SYNC_FLAGFILE)) {
			android.util.Log.i(TAG, "INTERNAL_NON_INITIAL_SYNC_FLAGFILE not exist");
			// import backup DB file from SD card to internal memory
			// if (FileHelper.fileExists(DatabaseDictionary.externalDBPathFile))
			// {
			// importLogStatDB(context);
			// android.util.Log.i(TAG, "External database imported");
			// } else {
			resetLastSyncTime(tm, null);
			// }
		}
		SQLiteOpenHelper helper = new SQLiteOpenHelper(context, DatabaseDictionary.STAT_DATABASE_NAME, null, DatabaseDictionary.DATABASE_VERSION) {
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			}

			@Override
			public void onCreate(SQLiteDatabase db) {
			}
		};
		SQLiteDatabase db = helper.getWritableDatabase();
		// Sync start
		try {
			// lock database
			inSyncing = true;
			// sync table schema
			SyncSchema.syncLocalSchemaWithRemote(db, tm);
			List<Table> localTableList = SyncSchema.getLocalSchema(db);
			if (localTableList != null && localTableList.size() > 0) {
				for (Table table : localTableList) {
					if (table.getTable_name().toLowerCase().equals(UN_SYNC_TABLE1))
						continue;
					// sync table
					syncTable(context, db, table.getTable_name(), tm.getDeviceId());
				}
			}
		} catch (Exception e) {
			android.util.Log.i(TAG, e.toString());
		} finally {
			db.close();
			// release database
			inSyncing = false;
		}
		// export internal DB file to SD card
		// exportLogStatDB();

		try {
			FileHelper.appendToInternalFile(context, "1", INTERNAL_NON_INITIAL_SYNC_FLAGFILE, INTERNAL_NON_INITIAL_SYNC_FLAGFILE);
		} catch (Exception e) {
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();
		android.util.Log.i(TAG, "Sync accomplished in " + String.valueOf((end - start) / 1000f) + " seconds");
	}

	private static String getLastSyncTime(Context context, SQLiteDatabase db, String phoneID, String tableName) {
		// Check if table is exist, if not create it
		if (!isTableExist(context, db, DatabaseDictionary.SYNC_TIME_TABLE))
			createTable(db, DatabaseDictionary.SYNC_TIME_TABLE);

		String sql = "select LastSyncTime from " + DatabaseDictionary.SYNC_TIME_TABLE + " where PhoneID='" + phoneID + "' and TableName='"
				+ tableName + "'";
		Cursor cur = db.rawQuery(sql, null);
		String lastSyncTime = null;
		cur.moveToFirst();
		while (!cur.isAfterLast()) {
			lastSyncTime = cur.getString(0);
			cur.moveToNext();
		}
		cur.close();
		android.util.Log.i(TAG, "get LastSyncTime for table[" + tableName + "]: " + lastSyncTime);
		return lastSyncTime;
	}

	private static void syncTable(Context context, SQLiteDatabase db, String tableName, String phoneID) {
		String lastSyncTime = getLastSyncTime(context, db, phoneID, tableName);
		String upsyncstring = prepareTableSyncData(context, db, tableName, phoneID, lastSyncTime);
		try {
			downsyncstring = Util.convertStreamToString(sync_transit(SYNC_URL, new String[][] { { "tablename", tableName }, { "phoneID", phoneID },
					{ "upsyncstring", upsyncstring } }));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (downsyncstring != null && !downsyncstring.equals("")) {
			int page = 0;
			while (downsyncstring.substring(downsyncstring.length() - 3, downsyncstring.length()).equals(DatabaseDictionary.PAGE_SPLIT)) {
				android.util.Log.i(TAG, "DownSyncing " + tableName + ",Page " + page);
				downsyncstring = downsyncstring.substring(0, downsyncstring.length() - 3);
				downSyncArray = parseSyncDataFromServer(downsyncstring);
				for (int i = 0; i < downSyncArray.length; i++) {
					if (i % 100 == 0)
						android.util.Log.i(TAG, "[" + tableName + "]->record:" + (i + 1));
					putRecord(db, tableName, downSyncArray[i], null);
				}
				page++;
				try {
					downsyncstring = Util.convertStreamToString(sync_transit(SYNC_URL, new String[][] { { "tablename", tableName },
							{ "phoneID", phoneID }, { "upsyncstring", "" }, { "page", String.valueOf(page) } }));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			android.util.Log.i(TAG, "DownSyncing " + tableName + ",Page " + page);
			downSyncArray = parseSyncDataFromServer(downsyncstring);
			for (int i = 0; i < downSyncArray.length; i++) {
				if (i % 100 == 0)
					android.util.Log.i(TAG, "[" + tableName + "]->record:" + (i + 1));
				putRecord(db, tableName, downSyncArray[i], null);
			}
		}
		String newLastSyncTime = null;
		try {
			newLastSyncTime = Util.convertStreamToString(sync_transit(SYNC_DONE_URL, new String[][] { { "phoneID", phoneID },
					{ "tableName", tableName } }));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (newLastSyncTime != null && !newLastSyncTime.equals("")) {
			putLastSyncTime(db, phoneID, tableName, newLastSyncTime);
			android.util.Log.i(TAG, "Update SyncTime [" + tableName + "]: " + newLastSyncTime);
		}
	}

	private static String prepareTableSyncData(Context context, SQLiteDatabase db, String tableName, String phoneID, String lastSyncTime) {
		if (!isTableExist(context, db, DatabaseDictionary.SYNC_TIME_TABLE))
			createTable(db, DatabaseDictionary.SYNC_TIME_TABLE);
		String sql;
		if (lastSyncTime == null)
			sql = "select * from " + tableName;
		else
			sql = "select * from " + tableName + " where datetime(LastModDate,'localtime')>datetime('" + lastSyncTime + "','localtime')";
		Cursor cur = db.rawQuery(sql, null);
		ArrayList<String[]> result = new ArrayList<String[]>();
		cur.moveToFirst();
		while (!cur.isAfterLast()) {
			String[] s = new String[cur.getColumnCount()];
			for (int i = 0; i < cur.getColumnCount(); i++)
				s[i] = cur.getString(i);
			result.add(s);
			cur.moveToNext();
		}
		cur.close();
		String syncData = "";
		for (String[] str : result) {
			for (String _str : str)
				syncData += _str + DatabaseDictionary.COL_SPLIT;
			syncData = syncData.substring(0, syncData.length() - DatabaseDictionary.COL_SPLIT.length());
			syncData += DatabaseDictionary.ROW_SPLIT;
		}
		if (!syncData.equals(""))
			syncData = syncData.substring(0, syncData.length() - DatabaseDictionary.ROW_SPLIT.length());
		android.util.Log.i(TAG, "prepare SyncData for server");
		return syncData;
	}

	public static String[][] parseSyncDataFromServer(String db) {
		String[] rows = db.split(DatabaseDictionary.ROW_SPLIT, -1);
		String[] cols = null;
		db = null;
		if (rows.length == 0)
			return null;
		cols = rows[0].split(DatabaseDictionary.COL_SPLIT, -1);
		if (cols.length == 0)
			return null;
		String[][] parsedDB = new String[rows.length][cols.length];
		for (int i = 0; i < rows.length; i++) {
			cols = rows[i].split(DatabaseDictionary.COL_SPLIT, -1);
			for (int j = 0; j < cols.length; j++) {
				parsedDB[i][j] = cols[j];
			}
		}
		rows = null;
		android.util.Log.i(TAG, "SyncData from server parsed");
		return parsedDB;
	}

	private static void putLastSyncTime(SQLiteDatabase db, String phoneID, String tableName, String lastSyncTime) {
		String insertSQL = "insert or replace into " + DatabaseDictionary.SYNC_TIME_TABLE + " values('" + phoneID + "','" + tableName + "','"
				+ lastSyncTime + "');";
		db.execSQL(insertSQL);
	}

	private static void putRecord(SQLiteDatabase db, String tableName, String[] record, int dateCol[]) {
		String values = "";
		for (int j = 0; j < record.length; j++) {
			record[j] = record[j].replace("'", "''");
			if (dateCol != null) {
				for (int k : dateCol) {
					if (j == k && record[j] != null && !record[j].equals("")) {
						try {
							record[j] = DatabaseDictionary.normalDateFormat.format(DatabaseDictionary.priorDateFormat.parse(record[j]));
						} catch (ParseException e) {

						}
					}
				}
			}
			values += "'" + record[j] + "'";
			if (j != record.length - 1)
				values += ",";
		}
		String insertSQL = "insert or replace into " + tableName + " values(" + values + ");";
		db.execSQL(insertSQL);
	}

	public static InputStream sync_transit(String service_url, String[][] params) {
		String service = service_url;
		InputStream in = null;
		try {
			URL url = new URL(service);
			URLConnection connection;
			connection = url.openConnection();
			HttpURLConnection httpConnection = (HttpURLConnection) connection;

			// Set timeout for HttpURLConnection, otherwise the App will wait
			// until
			// App crash.
			httpConnection.setConnectTimeout(8000);
			httpConnection.setReadTimeout(8000);

			// To accept character like & and %, HttpURLConnection should use
			// POST method to pass parameters
			// Setup POST method properties
			httpConnection.setDoInput(true);
			httpConnection.setDoOutput(true);
			httpConnection.setRequestMethod("POST");
			httpConnection.setUseCaches(false);
			httpConnection.setInstanceFollowRedirects(true);
			httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			// Setup POST parameters and post them
			DataOutputStream out = new DataOutputStream(httpConnection.getOutputStream());
			String content = "";
			for (String[] param : params)
				content += param[0] + "=" + URLEncoder.encode(param[1], "utf-8") + "&";
			content = content.substring(0, content.length() - 1);
			out.writeBytes(content);
			out.flush();
			out.close();

			int responseCode = httpConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				in = httpConnection.getInputStream();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return in;
		} catch (IOException e) {
			e.printStackTrace();
			return in;
		}
		return in;
	}

	public static String importLogStatDB(Context context) {
		InputStream myInput;
		try {
			myInput = new FileInputStream(FileHelper.getSDCard().getAbsolutePath() + "/" + DatabaseDictionary.externalDBPathFile);
			OutputStream myOutput = new BufferedOutputStream(new FileOutputStream(DatabaseDictionary.internalDBPathFile, false));
			byte[] buffer = new byte[8192];
			int length;
			while ((length = myInput.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
			}
			myOutput.flush();
			myInput.close();
			myOutput.close();
			return "SDcard database imported to Internal";
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "Can't find SDcard database";
		} catch (IOException e) {
			e.printStackTrace();
			return "Can't read internal database or write SD card";
		} catch (Exception e) {
			e.printStackTrace();
			return "Can't read internal database or write SD card";
		}
	}

	public static String exportLogStatDB() {
		InputStream myInput;
		try {
			myInput = new FileInputStream(DatabaseDictionary.internalDBPathFile);
			FileHelper.createDir(DatabaseDictionary.externalDBPath);
			OutputStream myOutput = FileHelper
					.openFileForWriting(DatabaseDictionary.externalDBPathFile, DatabaseDictionary.externalDBPathFile, false);
			byte[] buffer = new byte[8192];
			int length;
			while ((length = myInput.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
			}
			myOutput.flush();
			myInput.close();
			myOutput.close();
			return "Internal database exported to SDcard";
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "Can't find internal database";
		} catch (IOException e) {
			e.printStackTrace();
			return "Can't read internal database or write SD card";
		} catch (Exception e) {
			e.printStackTrace();
			return "Can't read internal database or write SD card";
		}
	}

	public static void createTable(SQLiteDatabase db, String tableName) {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + DatabaseDictionary.lastSyncTimeTableParams[0] + " ("
				+ DatabaseDictionary.lastSyncTimeTableParams[1] + ")");
	}

	public static boolean isTableExist(Context context, SQLiteDatabase db, String tableName) {
		if (db == null)
			return false;
		String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='" + tableName + "'";
		int count = 0;
		Cursor cur = db.rawQuery(sql, null);
		cur.moveToFirst();
		while (!cur.isAfterLast()) {
			count = cur.getInt(0);
			cur.moveToNext();
		}
		cur.close();
		if (count > 0)
			return true;
		else
			return false;
	}
}

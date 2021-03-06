package org.synclibrary.schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.synclibrary.sync.SyncDatabase;
import org.synclibrary.util.Util;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.TelephonyManager;


public class SyncSchema {
	private static String LOCAL_SCHEMA_TABLE = "schema_update";
	private static String SPACE = " ";
	private static String COMMA = ",";

	public static void syncLocalSchemaWithRemote(SQLiteDatabase db, TelephonyManager tm) {
		List<String> changedTables = getChangedTables(db);
		String dropTableSQL = null;
		String createTableSQL = null;
		String syncSchemaTime = null;
		String syncSchemaTimeSQL = null;
		for (String s : changedTables) {
			Table table = getRemoteTableSchema(s);
			if (table == null)
				continue;
			else {
				if (table.getType().toLowerCase().equals("mysql"))
					table.convertMysqlSchemaToSqlite();
				dropTableSQL = "DROP TABLE IF EXISTS " + s;
				db.execSQL(dropTableSQL);
				createTableSQL = "CREATE TABLE IF NOT EXISTS " + s + " (";
				for (Column c : table.getColumns()) {
					createTableSQL += c.getColumn_name() + SPACE;
					createTableSQL += c.getData_type() + SPACE;
					if (c.getNotnull() != null && c.getNotnull().equals("1"))
						createTableSQL += "NOT NULL" + SPACE;
					if (c.getColumn_default() != null && !c.getColumn_default().equals(""))
						createTableSQL += "DEFAULT " + c.getColumn_default();
					createTableSQL += COMMA;
				}
				String tableKeyString = "";
				for (Column c : table.getColumns()) {
					if (c.getColumn_key() != null && c.getColumn_key().equals("1")) {
						tableKeyString += c.getColumn_name() + COMMA;
					}
				}
				if (!tableKeyString.equals(""))
					createTableSQL += "PRIMARY KEY (" + tableKeyString.substring(0, tableKeyString.length() - 1) + ")";
				else
					createTableSQL = createTableSQL.substring(0, createTableSQL.length() - 1);
				createTableSQL += ");";
				// android.util.Log.i(TAG, createTableSQL);
				db.execSQL(createTableSQL);
				SyncDatabase.resetLastSyncTime(tm, s);
				try {
					syncSchemaTime = Util.convertStreamToString(SyncDatabase.sync_transit(
							SyncDatabase.TABLE_UPDATE_TIME_URL, new String[][] { { "tablename", s } }));
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (syncSchemaTime != null && !syncSchemaTime.equals("")) {
					syncSchemaTimeSQL = "INSERT OR REPLACE INTO " + LOCAL_SCHEMA_TABLE + " VALUES('" + s + "','"
							+ syncSchemaTime + "');";
					// android.util.Log.i(TAG, syncSchemaTimeSQL);
					db.execSQL(syncSchemaTimeSQL);
				}
			}
		}
	}

	public static List<String> getChangedTables(SQLiteDatabase db) {
		String serverTables = null;
		try {
			serverTables = Util.convertStreamToString(SyncDatabase.sync_transit(SyncDatabase.TABLE_UPDATE_TIME_URL,
					new String[][] { { "phoneID", "" } }));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (serverTables == null)
			return null;
		String[][] serverTableList = SyncDatabase.parseSyncDataFromServer(serverTables);
		String sql = "CREATE TABLE IF NOT EXISTS " + LOCAL_SCHEMA_TABLE
				+ " (TableName TEXT NOT NULL,UpdateTime TEXT,PRIMARY KEY (TableName));";
		db.execSQL(sql);
		Cursor cur = db.query(LOCAL_SCHEMA_TABLE, null, null, null, null, null, null);
		ArrayList<String[]> localTables = new ArrayList<String[]>();
		cur.moveToFirst();
		while (!cur.isAfterLast()) {
			String[] s = { cur.getString(0), cur.getString(1) };
			localTables.add(s);
			cur.moveToNext();
		}
		cur.close();
		List<String> changedTables = new ArrayList<String>();
		if (localTables == null || localTables.size() < 1) {
			for (String[] s : serverTableList)
				changedTables.add(s[0]);
		} else {
			boolean localTableExist;
			for (String[] server : serverTableList) {
				localTableExist = false;
				for (String[] local : localTables) {
					if (server[0].equals(local[0])) {
						localTableExist = true;
						if (server[1].compareTo(local[1]) > 0) {
							changedTables.add(server[0]);
						}
					}
				}
				if (!localTableExist)
					changedTables.add(server[0]);
			}
			boolean localTableRemoveable;
			for (String[] local : localTables) {
				localTableRemoveable = true;
				for (String[] server : serverTableList) {
					if (server[0].equals(local[0]))
						localTableRemoveable = false;
				}
				if (localTableRemoveable) {
					String removeTable = "DROP TABLE IF EXISTS " + local[0];
					db.execSQL(removeTable);
					db.delete(LOCAL_SCHEMA_TABLE, "TableName=?", new String[] { local[0] });
				}
			}
		}
		return changedTables;
	}

	public static List<Table> getLocalSchema(SQLiteDatabase db) {
		List<Table> tables = new ArrayList<Table>();
//		String sql = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name";
		String sql = "SELECT TableName FROM "+SyncSchema.LOCAL_SCHEMA_TABLE+" ORDER BY TableName";
		Cursor cur = db.rawQuery(sql, null);
		cur.moveToFirst();
		while (!cur.isAfterLast()) {
			Table t = new Table(cur.getString(0), "sqlite");
			tables.add(t);
			cur.moveToNext();
		}
		for (Table t : tables) {
			sql = "PRAGMA table_info(" + t.getTable_name() + ")";
			cur = db.rawQuery(sql, null);
			cur.moveToFirst();
			while (!cur.isAfterLast()) {
				Column c = new Column(cur.getString(1), cur.getString(4), cur.getString(3), cur.getString(2), cur
						.getString(5));
				t.getColumns().add(c);
				cur.moveToNext();
			}
		}

		return tables;
	}

	public static Table getRemoteTableSchema(String tablename) {
		String is;
		try {
			is = Util.convertStreamToString(SyncDatabase.sync_transit(SyncDatabase.SYNC_TABLE_SCHEMA_URL,
					new String[][] { { "tablename", tablename } }));
			String[][] tableSchemaArray = SyncDatabase.parseSyncDataFromServer(is);
			if (tableSchemaArray != null && tableSchemaArray.length > 0) {
				Table t = new Table(tableSchemaArray[0][0], "mysql");
				for (String[] s : tableSchemaArray) {
					Column c = new Column(s[1], s[2], s[3], s[4], s[5]);
					t.getColumns().add(c);
				}
				return t;
			} else
				return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}
	// public static Table getRemoteTableSchema(String tablename) {
	// InputStream in =
	// SyncDatabase.sync_transit(SyncDatabase.SYNC_TABLE_SCHEMA_URL, new
	// String[][] { { "tablename",
	// tablename } });
	// DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	// Table t = null;
	// try {
	// DocumentBuilder db = dbf.newDocumentBuilder();
	// Document dom = db.parse(in);
	// Element docEle = dom.getDocumentElement();
	// Element table = (Element) docEle.getElementsByTagName("table").item(0);
	// Element table_name = (Element)
	// table.getElementsByTagName("name").item(0);
	// t = new Table(table_name.getTextContent(), "mysql");
	// NodeList cnl = table.getElementsByTagName("column");
	// for (int j = 0; j < cnl.getLength(); j++) {
	// Element column = (Element) cnl.item(j);
	// Element column_name = (Element)
	// column.getElementsByTagName("column_name").item(0);
	// Element column_default = (Element)
	// column.getElementsByTagName("column_default").item(0);
	// Element is_nullable = (Element)
	// column.getElementsByTagName("is_nullable").item(0);
	// Element data_type = (Element)
	// column.getElementsByTagName("data_type").item(0);
	// Element column_key = (Element)
	// column.getElementsByTagName("column_key").item(0);
	// Column c = new Column(column_name.getTextContent(),
	// column_default.getTextContent(), is_nullable
	// .getTextContent(), data_type.getTextContent(),
	// column_key.getTextContent());
	// t.getColumns().add(c);
	// }
	// } catch (ParserConfigurationException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (SAXException e) {
	// e.printStackTrace();
	// }
	// return t;
	// }

	// public static List<Table> getRemoteSchema() {
	// List<Table> tables = new ArrayList<Table>();
	// InputStream in = SyncDatabase.sync_transit(SyncDatabase.SYNC_SCHEMA_URL,
	// new String[][] { { "phoneID", "" } });
	// DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	// try {
	// DocumentBuilder db = dbf.newDocumentBuilder();
	// Document dom = db.parse(in);
	// Element docEle = dom.getDocumentElement();
	// NodeList nl = docEle.getElementsByTagName("table");
	// if (nl != null && nl.getLength() > 0) {
	// for (int i = 0; i < nl.getLength(); i++) {
	// Element table = (Element) nl.item(i);
	// Element table_name = (Element)
	// table.getElementsByTagName("name").item(0);
	// Table t = new Table(table_name.getTextContent(), "mysql");
	// NodeList cnl = table.getElementsByTagName("column");
	// for (int j = 0; j < cnl.getLength(); j++) {
	// Element column = (Element) cnl.item(j);
	// Element column_name = (Element)
	// column.getElementsByTagName("column_name").item(0);
	// Element column_default = (Element)
	// column.getElementsByTagName("column_default").item(0);
	// Element is_nullable = (Element)
	// column.getElementsByTagName("is_nullable").item(0);
	// Element data_type = (Element)
	// column.getElementsByTagName("data_type").item(0);
	// Element column_key = (Element)
	// column.getElementsByTagName("column_key").item(0);
	// Column c = new Column(column_name.getTextContent(),
	// column_default.getTextContent(),
	// is_nullable.getTextContent(), data_type.getTextContent(),
	// column_key.getTextContent());
	// t.getColumns().add(c);
	// }
	// tables.add(t);
	// }
	// }
	//
	// } catch (ParserConfigurationException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (SAXException e) {
	// e.printStackTrace();
	// }
	// return tables;
	// }

}

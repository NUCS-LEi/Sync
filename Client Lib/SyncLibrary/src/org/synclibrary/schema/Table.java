package org.synclibrary.schema;

import java.util.ArrayList;
import java.util.List;

public class Table {
	private String table_name;
	private List<Column> columns;
	private List<Column> keys;
	private String type;

	public Table() {
		super();
		columns = new ArrayList<Column>();
		keys = new ArrayList<Column>();
	}

	public Table(String name, String type) {
		super();
		this.table_name = name;
		this.type = type;
		columns = new ArrayList<Column>();
		keys = new ArrayList<Column>();
	}

	public String getTable_name() {
		return table_name;
	}

	public void setTable_name(String tableName) {
		table_name = tableName;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

	public List<Column> getKeys() {
		return keys;
	}

	public void setKeys(List<Column> keys) {
		this.keys = keys;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	private String convertDataTypeToSqlite(String mySQLDataType) {
		if (mySQLDataType.toLowerCase().equals("char"))
			return "TEXT";
		if (mySQLDataType.toLowerCase().equals("varchar"))
			return "TEXT";
		if (mySQLDataType.toLowerCase().equals("int"))
			return "INTEGER";
		if (mySQLDataType.toLowerCase().equals("text"))
			return "TEXT";
		return mySQLDataType.toLowerCase();
	}

	private String convertNotNullToSqlite(String is_nullable) {
		if (is_nullable.toLowerCase().equals("no"))
			return "1";
		else
			return "0";
	}

	private String convertDefaultValueToSqlite(String columnDefault) {
		return columnDefault;
	}

	private String convertPKToSqlite(String column_key) {
		if (column_key.toLowerCase().equals("pri"))
			return "1";
		else
			return "0";
	}
	public void convertMysqlSchemaToSqlite(){
		for(Column c:columns){
			c.setData_type(convertDataTypeToSqlite(c.getData_type()));
			c.setNotnull(convertNotNullToSqlite(c.getNotnull()));
			c.setColumn_default(convertDefaultValueToSqlite(c.getColumn_default()));
			c.setColumn_key(convertPKToSqlite(c.getColumn_key()));
		}
		this.setType("sqlite");
	}
}

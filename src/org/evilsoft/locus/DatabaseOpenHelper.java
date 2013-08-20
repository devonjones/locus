package org.evilsoft.locus;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 7;
	private static final String DATABASE_NAME = "locus.db";

	DatabaseOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(createMapsTable());
		db.execSQL(createPointsTable());
		db.execSQL(createMeasurementsTable());
	}

	public String createMapsTable() {
		StringBuffer sb = new StringBuffer();
		sb.append("CREATE TABLE maps (");
		sb.append(" _id INTEGER PRIMARY KEY,");
		sb.append(" name TEXT,");
		sb.append(" filename TEXT,");
		sb.append(" unit TEXT");
		sb.append(")");
		return sb.toString();
	}

	public String createPointsTable() {
		StringBuffer sb = new StringBuffer();
		sb.append("CREATE TABLE points (");
		sb.append(" _id INTEGER PRIMARY KEY,");
		sb.append(" map_id INTEGER,");
		sb.append(" x INTEGER,");
		sb.append(" y INTEGER");
		sb.append(")");
		return sb.toString();
	}

	public String createMeasurementsTable() {
		StringBuffer sb = new StringBuffer();
		sb.append("CREATE TABLE measurements (");
		sb.append(" _id INTEGER PRIMARY KEY,");
		sb.append(" point_id INTEGER,");
		sb.append(" bssid TEXT,");
		sb.append(" ssid TEXT,");
		sb.append(" frequency INTEGER,");
		sb.append(" level INTEGER,");
		sb.append(" timestamp REAL,");
		sb.append(" raw TEXT");
		sb.append(")");
		return sb.toString();
	}

	public String createParametersTable() {
		StringBuffer sb = new StringBuffer();
		sb.append("CREATE TABLE parameters (");
		sb.append(" _id INTEGER PRIMARY KEY,");
		sb.append(" map_id INTEGER,");
		sb.append(" bssid TEXT,");
		sb.append(" const REAL,");
		sb.append(" x REAL,");
		sb.append(" xx REAL,");
		sb.append(" y REAL,");
		sb.append(" yy REAL,");
		sb.append(" resid_var REAL");
		sb.append(")");
		return sb.toString();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 7) {
			db.execSQL("DROP TABLE parameters");
			db.execSQL(createParametersTable());
		}
	}
}
package org.evilsoft.locus;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseAdapter {
	private Context context;
	public SQLiteDatabase database;
	private DatabaseOpenHelper dbHelper;
	private boolean closed = true;

	public DatabaseAdapter(Context context) {
		this.context = context;
	}

	public DatabaseAdapter open() throws SQLException {
		dbHelper = new DatabaseOpenHelper(context);
		database = dbHelper.getWritableDatabase();
		closed = false;
		return this;
	}

	public void close() {
		dbHelper.close();
		dbHelper = null;
		database.close();
		database = null;
		closed = true;
	}

	public boolean isClosed() {
		return closed;
	}

	public static String[] toStringArray(List<String> input) {
		String[] retarr = new String[input.size()];
		for (int i = 0; i < input.size(); i++) {
			retarr[i] = input.get(i);
		}
		return retarr;
	}

	public long insertMap(String name, String filename, String unit) {
		ContentValues cv = new ContentValues();
		cv.put("name", name);
		cv.put("filename", filename);
		cv.put("unit", unit);
		return this.database.insert("maps", null, cv);
	}

	public Cursor fetchAllMaps() {
		List<String> args = new ArrayList<String>();
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT _id, name, filename, unit");
		sql.append(" FROM maps");
		return database.rawQuery(sql.toString(), toStringArray(args));
	}

	public long insertPoint(Long mapId, Integer x, Integer y) {
		ContentValues cv = new ContentValues();
		cv.put("map_id", mapId);
		cv.put("x", x);
		cv.put("y", y);
		return this.database.insert("points", null, cv);
	}

	public Cursor fetchAllPoints(Long mapId) {
		List<String> args = new ArrayList<String>();
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT _id, x, y");
		sql.append(" FROM points");
		sql.append("  WHERE map_id = ?");
		args.add(mapId.toString());
		return database.rawQuery(sql.toString(), toStringArray(args));
	}

	public int killPoint(Long pointId) {
		delMeasurements(pointId);
		return delPoint(pointId);
	}

	public int delPoint(Long pointId) {
		List<String> args = new ArrayList<String>();
		args.add(pointId.toString());
		return database.delete("points", "_id = ?", toStringArray(args));
	}

	public long insertMeasurement(Long pointId, String bssid, String ssid,
			Integer frequency, Integer level, Float timestamp, String raw) {
		ContentValues cv = new ContentValues();
		cv.put("point_id", pointId);
		cv.put("bssid", bssid);
		cv.put("ssid", ssid);
		cv.put("frequency", frequency);
		cv.put("level", level);
		cv.put("timestamp", timestamp);
		cv.put("raw", raw);
		return this.database.insert("measurements", null, cv);
	}

	public Cursor fetchAllMeasurements(Long pointId) {
		List<String> args = new ArrayList<String>();
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT _id, bssid, ssid, frequency, level, timestamp");
		sql.append(" FROM measurements");
		sql.append(" WHERE point_id = ?");
		args.add(pointId.toString());
		return database.rawQuery(sql.toString(), toStringArray(args));
	}

	public Cursor fetchBssidMeasurements(Long mapId, String bssid) {
		List<String> args = new ArrayList<String>();
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT m._id, m.bssid, m.level, p.x, p.y");
		sql.append(" FROM measurements m");
		sql.append("  INNER JOIN points p");
		sql.append("   ON p._id = m.point_id");
		sql.append(" WHERE map_id = ?");
		sql.append("  AND bssid = ?");
		args.add(mapId.toString());
		args.add(bssid);
		return database.rawQuery(sql.toString(), toStringArray(args));
	}

	public int delMeasurements(Long pointId) {
		List<String> args = new ArrayList<String>();
		args.add(pointId.toString());
		return database.delete("measurements", "point_id = ?",
				toStringArray(args));
	}

	public long insertParameter(Long mapId, String bssid, Double con, Double x,
			Double x2, Double y, Double y2, Double rss) {
		delParameters(mapId, bssid);
		ContentValues cv = new ContentValues();
		cv.put("map_id", mapId);
		cv.put("bssid", bssid);
		cv.put("const", con);
		cv.put("x", x);
		cv.put("xx", x2);
		cv.put("y", y);
		cv.put("yy", y2);
		cv.put("resid_var", rss);
		return this.database.insert("parameters", null, cv);
	}

	public int delParameters(Long mapId, String bssid) {
		List<String> args = new ArrayList<String>();
		args.add(mapId.toString());
		args.add(bssid);
		return database.delete("parameters", "map_id = ? AND bssid = ?",
				toStringArray(args));
	}
}

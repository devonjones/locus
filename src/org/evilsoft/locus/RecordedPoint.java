package org.evilsoft.locus;

import android.database.Cursor;
import android.graphics.Point;

public class RecordedPoint extends Point {
	public long _id;

	public static RecordedPoint populateRecordedPoint(Cursor cursor) {
		Long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
		Integer x = cursor.getInt(cursor.getColumnIndexOrThrow("x"));
		Integer y = cursor.getInt(cursor.getColumnIndexOrThrow("y"));
		return new RecordedPoint(id, x, y);
	}

	public RecordedPoint(long _id, int x, int y) {
		super(x, y);
		this._id = _id;
	}
}

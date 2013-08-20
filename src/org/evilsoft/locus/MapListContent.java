package org.evilsoft.locus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.Cursor;

public class MapListContent {
	public static List<MapListItem> ITEMS = new ArrayList<MapListItem>();
	public static Map<String, MapListItem> ITEM_MAP = new HashMap<String, MapListItem>();

	public interface MapListItem {
		public Long getId();

		public String toString();
	}

	public static class AddMapItem implements MapListItem {
		public Long id = 0L;
		public String name = "Add new map";

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static class MapItem implements MapListItem {

		public Long id;
		public String name;
		public String filename;
		public String units;

		public static MapItem populateMapItem(Cursor cursor) {
			Long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
			String name = cursor
					.getString(cursor.getColumnIndexOrThrow("name"));
			String filename = cursor.getString(cursor
					.getColumnIndexOrThrow("filename"));
			String units = cursor.getString(cursor
					.getColumnIndexOrThrow("unit"));
			return new MapItem(id, name, filename, units);
		}

		public MapItem(Long id, String name, String filename, String units) {
			this.id = id;
			this.name = name;
			this.filename = filename;
			this.units = units;
		}

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	static {
		addItem(new AddMapItem());
	}

	private static void addItem(MapListItem item) {
		ITEMS.add(item);
		ITEM_MAP.put(item.getId().toString(), item);
	}

	public static void refresh(DatabaseAdapter dbAdapter) {
		ITEMS.clear();
		addItem(new AddMapItem());
		Cursor curs = dbAdapter.fetchAllMaps();
		try {
			boolean hasNext = curs.moveToFirst();
			while (hasNext) {
				addItem(MapItem.populateMapItem(curs));
				hasNext = curs.moveToNext();
			}
		} finally {
			curs.close();
		}
	}
}

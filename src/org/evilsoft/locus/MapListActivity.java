package org.evilsoft.locus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;

public class MapListActivity extends ScanFragmentActivity implements
		MapListFragment.Callbacks {

	private boolean mTwoPane;
	private static final String TAG = "MapListActivity";
	// Stores names of traversed directories
	ArrayList<String> str = new ArrayList<String>();

	// Check if the first level of the directory structure is the one showing
	private Boolean firstLvl = true;
	private Item[] fileList;
	private File path = new File(Environment.getExternalStorageDirectory() + "");
	private String chosenFile;
	private static final int DIALOG_LOAD_FILE = 1000;
	public static final String PREFS_NAME = "locus.prefs";
	public static final String MAP_COUNT = "MapCount";

	ListAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map_list);

		if (findViewById(R.id.map_detail_container) != null) {
			mTwoPane = true;
			((MapListFragment) getSupportFragmentManager().findFragmentById(
					R.id.map_list)).setActivateOnItemClick(true);
		}
	}

	@Override
	public void onItemSelected(String id) {
		if ("0".equals(id)) {
			loadFileList();
			showDialog(DIALOG_LOAD_FILE);
			Log.d(TAG, path.getAbsolutePath());
		} else {
			if (mTwoPane) {
				Bundle arguments = new Bundle();
				arguments.putString(MapDetailFragment.ARG_ITEM_ID, id);
				detailFragment = new MapDetailFragment();
				detailFragment.setArguments(arguments);
				getSupportFragmentManager().beginTransaction()
						.replace(R.id.map_detail_container, detailFragment)
						.commit();

			} else {
				Intent detailIntent = new Intent(this, MapDetailActivity.class);
				detailIntent.putExtra(MapDetailFragment.ARG_ITEM_ID, id);
				startActivity(detailIntent);
			}
		}
	}

	private void loadFileList() {
		try {
			path.mkdirs();
		} catch (SecurityException e) {
			Log.e(TAG, "unable to write on the sd card ");
		}

		// Checks whether path exists
		if (path.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					// Filters based on whether the file is hidden or not & png
					// extension
					if (sel.isFile() && !sel.isHidden()) {
						String extension = filename.substring(
								filename.lastIndexOf(".") + 1,
								filename.length()).toLowerCase(
								Locale.getDefault());
						if ("png".equals(extension)) {
							return true;
						}
					} else if (sel.isDirectory() && !sel.isHidden()) {
						return true;
					}
					return false;
				}
			};

			String[] fList = path.list(filter);
			fileList = new Item[fList.length];
			for (int i = 0; i < fList.length; i++) {
				// Convert into file path
				File sel = new File(path, fList[i]);
				fileList[i] = new Item(fList[i], R.drawable.file_icon,
						sel.isDirectory());

				// Set drawables
				if (sel.isDirectory()) {
					fileList[i].icon = R.drawable.directory_icon;
					Log.d("DIRECTORY", fileList[i].file);
				} else {
					Log.d("FILE", fileList[i].file);
				}
			}

			if (!firstLvl) {
				Item temp[] = new Item[fileList.length + 1];
				for (int i = 0; i < fileList.length; i++) {
					temp[i + 1] = fileList[i];
				}
				temp[0] = new Item("Up", R.drawable.directory_up, true);
				fileList = temp;
			}
		} else {
			Log.e(TAG, "path does not exist");
		}
		Arrays.sort(fileList);
		adapter = new ArrayAdapter<Item>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
				fileList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// creates view
				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view
						.findViewById(android.R.id.text1);

				// put the image on the text view
				textView.setCompoundDrawablesWithIntrinsicBounds(
						fileList[position].icon, 0, 0, 0);

				// add margin between image and text (support various screen
				// densities)
				int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
				textView.setCompoundDrawablePadding(dp5);

				return view;
			}
		};
	}

	private class Item implements Comparable<Item> {
		public String file;
		public int icon;
		public boolean directory;

		public Item(String file, Integer icon, boolean directory) {
			this.file = file;
			this.icon = icon;
			this.directory = directory;
		}

		@Override
		public String toString() {
			return file;
		}

		@Override
		public int compareTo(Item another) {
			if (this.directory && this.file.equals("Up")) {
				return -1;
			} else if (another.directory && another.file.equals("Up")) {
				return 1;
			}
			if (this.directory == another.directory) {
				return this.file.compareToIgnoreCase(another.file);
			} else if (this.directory) {
				return -1;
			} else if (another.directory) {
				return 1;
			}
			return 0;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new Builder(this);

		if (fileList == null) {
			Log.e(TAG, "No files loaded");
			dialog = builder.create();
			return dialog;
		}

		switch (id) {
		case DIALOG_LOAD_FILE:
			builder.setTitle("Choose a png for your map");
			builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					chosenFile = fileList[which].file;
					File sel = new File(path + "/" + chosenFile);
					if (sel.isDirectory()) {
						firstLvl = false;

						// Adds chosen directory to list
						str.add(chosenFile);
						fileList = null;
						path = new File(sel + "");

						loadFileList();

						removeDialog(DIALOG_LOAD_FILE);
						showDialog(DIALOG_LOAD_FILE);
						Log.d(TAG, path.getAbsolutePath());

					}

					// Checks if 'up' was clicked
					else if (chosenFile.equalsIgnoreCase("up") && !sel.exists()) {

						// present directory removed from list
						String s = str.remove(str.size() - 1);

						// path modified to exclude present directory
						path = new File(path.toString().substring(0,
								path.toString().lastIndexOf(s)));
						fileList = null;

						// if there are no more directories in the list, then
						// its the first level
						if (str.isEmpty()) {
							firstLvl = true;
						}
						loadFileList();

						removeDialog(DIALOG_LOAD_FILE);
						showDialog(DIALOG_LOAD_FILE);
						Log.d(TAG, path.getAbsolutePath());

					} else {
						// Perform action with file picked
						selectMap(sel);
					}
				}
			});
			break;
		}
		dialog = builder.show();
		return dialog;
	}

	public void selectMap(final File sel) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Enter Map Name");
		alert.setMessage("Map name");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Editable value = input.getText();
				// Do something with value!
				storeMap(sel, value.toString());
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		alert.show(); // Create dialog to select name
		// Copy file to app directory
	}

	public void storeMap(File sel, String name) {
		File mPath = new File(Environment.getExternalStorageDirectory()
				+ "/locus/");
		try {
			boolean result = mPath.mkdirs();
			if (!result) {
				Log.e(TAG, "Cannot create directory " + mPath.toString());
			}
		} catch (SecurityException e) {
			Log.e(TAG, "unable to write on the sd card " + e.toString());
		}
		if (mPath.exists()) {
			String newFile = mPath.toString() + "/" + sel.getName();
			boolean result = copyFile(sel.toString(), newFile);
			if (result) {
				dbAdapter.insertMap(name, newFile, "cm");
				MapListFragment frag = ((MapListFragment) getSupportFragmentManager()
						.findFragmentById(R.id.map_list));
				frag.refresh(dbAdapter);
			}
		}
	}

	public static boolean copyFile(String sourceLocation, String destLocation) {
		try {
			File sd = Environment.getExternalStorageDirectory();
			if (sd.canWrite()) {
				File source = new File(sourceLocation);
				File dest = new File(destLocation);
				if (!dest.exists()) {
					dest.createNewFile();
				}
				if (source.exists()) {
					InputStream src = new FileInputStream(source);
					OutputStream dst = new FileOutputStream(dest);
					// Copy the bits from instream to outstream
					byte[] buf = new byte[1024];
					int len;
					while ((len = src.read(buf)) > 0) {
						dst.write(buf, 0, len);
					}
					src.close();
					dst.close();
				}
			}
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
}

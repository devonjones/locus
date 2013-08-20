package org.evilsoft.locus;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouch.OnImageViewTouchSingleTapListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;

import org.evilsoft.locus.MapListContent.MapItem;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MapDetailFragment extends Fragment {

	public static final String ARG_ITEM_ID = "item_id";
	private static final String TAG = "MapDetailFragment";
	protected View rootView;
	protected ScanFragmentActivity scanAct;
	private ImageViewTouch iv;
	private Bitmap map;
	private Bitmap locationOverlay;
	private LayerDrawable layerDrawable;
	private int xplus;
	private int xminus;
	private int yplus;
	private int yminus;
	protected Point currentPoint;
	protected RecordedPoint selectedPoint;
	protected int networksSeen = 0;
	MapListContent.MapItem mItem;

	public MapDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments().containsKey(ARG_ITEM_ID)) {
			mItem = (MapItem) MapListContent.ITEM_MAP.get(getArguments()
					.getString(ARG_ITEM_ID));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_map_detail, container,
				false);
		scanAct = (ScanFragmentActivity) this.getActivity();
		Button measure = ((Button) rootView.findViewById(R.id.buttonMeasure));
		measure.setVisibility(View.INVISIBLE);
		Button delete = ((Button) rootView.findViewById(R.id.buttonDelete));
		delete.setVisibility(View.INVISIBLE);
		iv = ((ImageViewTouch) rootView.findViewById(R.id.image));
		setInitialImage();
		measure.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((TextView) rootView.findViewById(R.id.networks_seen))
						.setText("");
				networksSeen = 0;
				ProgressBar progressBar = ((ProgressBar) rootView
						.findViewById(R.id.progressBar));
				progressBar.setVisibility(View.VISIBLE);
				scanAct.recordWifi();
			}
		});
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				scanAct.dbAdapter.killPoint(selectedPoint._id);
				refresh();
			}

		});
		iv.setSingleTapListener(new OnImageViewTouchSingleTapListener() {
			@Override
			public void onSingleTapConfirmed(MotionEvent event) {
				((TextView) rootView.findViewById(R.id.networks_seen))
						.setText("");
				Button delete = ((Button) rootView
						.findViewById(R.id.buttonDelete));
				delete.setVisibility(View.INVISIBLE);
				Button measure = ((Button) rootView
						.findViewById(R.id.buttonMeasure));
				measure.setVisibility(View.INVISIBLE);
				networksSeen = 0;
				Point eventPoint = createPoint(event.getX(), event.getY());
				Point realPoint = getRealPixelLocation(event);
				Log.i(TAG, "event (x, y) = " + eventPoint.x + ", "
						+ eventPoint.y);
				if (realPoint.x < 0 || realPoint.x > map.getWidth()) {
					return;
				}
				if (realPoint.y < 0 || realPoint.y > map.getHeight()) {
					return;
				}
				refresh();
				RecordedPoint near = touchNearPoint(realPoint);
				if (near != null) {
					delete.setVisibility(View.VISIBLE);
					drawPointHighlight(near);
					drawPoints();
					selectedPoint = near;
				} else {
					drawLocation(realPoint);
					drawPoints();
					setText(rootView, realPoint);

					measure.setVisibility(View.VISIBLE);
					currentPoint = realPoint;
					Log.d(TAG, "onSingleTapConfirmed");
				}
			}
		});
		return rootView;
	}

	public RecordedPoint touchNearPoint(Point touchPoint) {
		Cursor curs = scanAct.dbAdapter.fetchAllPoints(mItem.getId());
		try {
			boolean hasNext = curs.moveToFirst();
			while (hasNext) {
				RecordedPoint p = RecordedPoint.populateRecordedPoint(curs);
				double distance = Math.sqrt(Math.pow(p.x - touchPoint.x, 2)
						+ Math.pow(p.y - touchPoint.y, 2));
				if (distance < 20) {
					return p;
				}
				hasNext = curs.moveToNext();
			}
		} finally {
			curs.close();
		}
		return null;
	}

	public void refresh() {
		((TextView) rootView.findViewById(R.id.x)).setText("");
		((TextView) rootView.findViewById(R.id.loc1)).setText("");
		((TextView) rootView.findViewById(R.id.y)).setText("");
		((TextView) rootView.findViewById(R.id.loc2)).setText("");
		((TextView) rootView.findViewById(R.id.xplus)).setText("");
		((TextView) rootView.findViewById(R.id.xminus)).setText("");
		((TextView) rootView.findViewById(R.id.yplus)).setText("");
		((TextView) rootView.findViewById(R.id.yminus)).setText("");
		locationOverlay.setPixels(new int[map.getWidth() * map.getHeight()], 0,
				map.getWidth(), 0, 0, map.getWidth(), map.getHeight());
		drawPoints();
	}

	public void setText(View view, Point point) {
		((TextView) view.findViewById(R.id.x)).setText(" x  ");
		((TextView) view.findViewById(R.id.loc1)).setText(point.x + "");
		((TextView) view.findViewById(R.id.y)).setText(" y ");
		((TextView) view.findViewById(R.id.loc2)).setText(point.y + "");

		TextView xplusView = ((TextView) view.findViewById(R.id.xplus));
		xplusView.setText(xplus + " cm  ");
		xplusView.setTextColor(Color.RED);
		TextView xminusView = ((TextView) view.findViewById(R.id.xminus));
		xminusView.setText(xminus + " cm  ");
		xminusView.setTextColor(Color.BLUE);
		TextView yplusView = ((TextView) view.findViewById(R.id.yplus));
		yplusView.setText(yplus + " cm  ");
		yplusView.setTextColor(Color.GREEN);
		TextView yminusView = ((TextView) view.findViewById(R.id.yminus));
		yminusView.setText(yminus + " cm  ");
		yminusView.setTextColor(Color.DKGRAY);

	}

	public Point createPoint(float x, float y) {
		return new Point((int) Math.round(x), (int) Math.round(y));
	}

	public Point getRealPixelLocation(MotionEvent event) {
		// http://stackoverflow.com/questions/6038867/android-how-to-detect-touch-location-on-imageview-if-the-image-view-is-scaled-b
		float[] touchPoint = new float[] { event.getX(), event.getY() };
		Matrix ivInverse = new Matrix();
		iv.getImageViewMatrix().invert(ivInverse);
		ivInverse.mapPoints(touchPoint);

		Matrix imageMatrix = new Matrix();
		float lw = layerDrawable.getIntrinsicWidth();
		float lh = layerDrawable.getIntrinsicHeight();
		float mw = map.getWidth();
		float mh = map.getHeight();
		float widthScale, heightScale;
		imageMatrix.reset();
		widthScale = mw / lw;
		heightScale = mh / lh;
		float scale = Math.min(widthScale, heightScale);
		imageMatrix.postScale(scale, scale);
		imageMatrix.mapPoints(touchPoint);
		return createPoint(touchPoint[0], touchPoint[1]);
	}

	public void drawPointHighlight(Point p) {
		int dot[] = createPoint(Color.BLUE, 11);
		locationOverlay.setPixels(dot, 0, 11, p.x - 6 < 0 ? 0 : p.x - 6,
				p.y - 6 < 0 ? 0 : p.y - 6, 11, 11);
	}

	public void drawPoints() {
		Cursor curs = scanAct.dbAdapter.fetchAllPoints(mItem.getId());
		int dot[] = createPoint(Color.RED, 7);
		try {
			boolean hasNext = curs.moveToFirst();
			while (hasNext) {
				RecordedPoint p = RecordedPoint.populateRecordedPoint(curs);
				locationOverlay.setPixels(dot, 0, 7, p.x - 4 < 0 ? 0 : p.x - 4,
						p.y - 4 < 0 ? 0 : p.y - 4, 7, 7);
				hasNext = curs.moveToNext();
			}
		} finally {
			curs.close();
		}
	}

	public int[] createPoint(int color, int size) {
		int dot[] = new int[size * size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				dot[i + j * size] = color;
			}
		}
		return dot;
	}

	public void drawLocation(Point loc) {
		int pixelRow[] = new int[map.getWidth()];
		map.getPixels(pixelRow, 0, map.getWidth(), 0, loc.y, map.getWidth(), 1);
		int rowStripe[] = createLocationRow(loc.x, pixelRow);
		locationOverlay.setPixels(rowStripe, 0, map.getWidth(), 0,
				loc.y - 1 < 0 ? 0 : loc.y - 1, map.getWidth(), 3);

		int pixelColumn[] = new int[map.getHeight()];
		map.getPixels(pixelColumn, 0, 1, loc.x, 0, 1, map.getHeight());
		int columnStripe[] = createLocationColumn(loc.y, pixelColumn);
		locationOverlay.setPixels(columnStripe, 0, 3, loc.x - 1 < 0 ? 0
				: loc.x - 1, 0, 3, map.getHeight());
		iv.invalidate();
	}

	public int[] createLocationRow(int loc, int[] pixels) {
		int[] returnRow = new int[pixels.length * 3];
		xplus = 0;
		for (int i = loc; i < pixels.length; i++) {
			if (pixels[i] == Color.BLACK) {
				break;
			} else {
				returnRow[i] = Color.RED;
				returnRow[i + pixels.length] = Color.RED;
				returnRow[i + pixels.length * 2] = Color.RED;
				xplus++;
			}
		}
		xminus = 0;
		for (int i = loc; i >= 0; i--) {
			if (pixels[i] == Color.BLACK) {
				break;
			} else {
				returnRow[i] = Color.BLUE;
				returnRow[i + pixels.length] = Color.BLUE;
				returnRow[i + pixels.length * 2] = Color.BLUE;
				xminus++;
			}
		}
		return returnRow;
	}

	public int[] createLocationColumn(int loc, int[] pixels) {
		int[] returnRow = new int[pixels.length * 3];
		yplus = 0;
		for (int i = loc; i < pixels.length; i++) {
			if (pixels[i] == Color.BLACK) {
				break;
			} else {
				returnRow[i * 3] = Color.GREEN;
				returnRow[i * 3 + 1] = Color.GREEN;
				returnRow[i * 3 + 2] = Color.GREEN;
				yplus++;
			}
		}
		yminus = 0;
		for (int i = loc; i >= 0; i--) {
			if (pixels[i] == Color.BLACK) {
				break;
			} else {
				returnRow[i * 3] = Color.DKGRAY;
				returnRow[i * 3 + 1] = Color.DKGRAY;
				returnRow[i * 3 + 2] = Color.DKGRAY;
				yminus++;
			}
		}
		return returnRow;
	}

	public void setInitialImage() {
		if (mItem != null) {
			map = BitmapFactory.decodeFile(mItem.filename);
			locationOverlay = Bitmap.createBitmap(map.getWidth(),
					map.getHeight(), Bitmap.Config.ARGB_8888);

			if (null != map) {
				iv.setDisplayType(DisplayType.FIT_TO_SCREEN);
				Drawable[] layers = new Drawable[2];
				layers[0] = new BitmapDrawable(map);
				layers[1] = new BitmapDrawable(locationOverlay);
				drawPoints();
				layerDrawable = new LayerDrawable(layers);
				iv.setImageDrawable(layerDrawable);
			} else {
				Toast.makeText(this.getActivity().getApplicationContext(),
						"Failed to load the image", Toast.LENGTH_LONG).show();
			}
		}
	}
}

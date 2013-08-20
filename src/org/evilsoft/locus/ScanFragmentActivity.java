package org.evilsoft.locus;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.WifiProbe;

public class ScanFragmentActivity extends FragmentActivity implements
		Probe.DataListener {
	protected DatabaseAdapter dbAdapter;
	private Long currentMapId;
	private Long currentPointId;
	public static final String PIPELINE_NAME = "default";
	private FunfManager funfManager;
	private BasicPipeline pipeline;
	private WifiProbe wifiProbe;
	private long scantime;
	private Handler handler;
	protected MapDetailFragment detailFragment;
	// private Handler handler;
	private ServiceConnection funfManagerConn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		openDb();
		handler = new Handler();
		funfManagerConn = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				funfManager = ((FunfManager.LocalBinder) service).getManager();

				Gson gson = funfManager.getGson();
				wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
				pipeline = (BasicPipeline) funfManager
						.getRegisteredPipeline(PIPELINE_NAME);
				wifiProbe.registerPassiveListener(ScanFragmentActivity.this);
				/*
				 * funfManager.enablePipeline(PIPELINE_NAME); pipeline =
				 * (BasicPipeline) funfManager
				 * .getRegisteredPipeline(PIPELINE_NAME);
				 */
				funfManager.disablePipeline(PIPELINE_NAME);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				funfManager = null;
			}
		};
		// Bind to the service, to create the connection with FunfManager
		bindService(new Intent(this, FunfManager.class), funfManagerConn,
				Context.BIND_AUTO_CREATE);
	}

	public void recordWifi() {
		Toast.makeText(this.getApplicationContext(), "Recording Signals",
				Toast.LENGTH_LONG).show();
		currentMapId = detailFragment.mItem.id;
		currentPointId = getDb().insertPoint(detailFragment.mItem.id,
				detailFragment.currentPoint.x, detailFragment.currentPoint.y);
		if (!pipeline.isEnabled()) {
			funfManager.enablePipeline(PIPELINE_NAME);
			pipeline = (BasicPipeline) funfManager
					.getRegisteredPipeline(PIPELINE_NAME);
		}
	}

	public void updateModel(String bssid) {
		// get measurements
		Cursor curs = dbAdapter.fetchBssidMeasurements(currentMapId, bssid);
		try {
			// if count(measurements) > 20
			boolean hasNext = curs.moveToFirst();
			if (curs.getCount() > 20) {
				OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
				double[][] x = new double[curs.getCount()][4];
				double[] y = new double[curs.getCount()];
				int i = 0;
				while (hasNext) {
					// m._id, m.bssid, m.level, p.x, p.y
					y[i] = curs.getDouble(2);
					x[i][0] = curs.getDouble(3);
					x[i][1] = curs.getDouble(3) * curs.getDouble(3);
					x[i][2] = curs.getDouble(4);
					x[i][3] = curs.getDouble(4) * curs.getDouble(4);
					i++;
					hasNext = curs.moveToNext();
				}
				ols.newSampleData(y, x);
				// do regression
				double[] coe = ols.estimateRegressionParameters();
				double rss = ols.calculateResidualSumOfSquares();
				double r_var = rss / curs.getCount();
				// store parameters
				dbAdapter.insertParameter(currentMapId, bssid, coe[0], coe[1],
						coe[2], coe[3], coe[4], r_var);
			}
		} finally {
			curs.close();
		}
	}

	// {"BSSID":"e0:46:9a:89:b0:54", "SSID":"evilsoft",
	// "capabilities":"[WPA2-PSK-CCMP][WPS][ESS]", "frequency":2422,
	// "level":-56, "timestamp":1365371628.668, "tsf":1493787253341,
	// "wifiSsid":{ "octets":{
	// "buf":[101,118,105,108,115,111,102,116,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
	// "count":8 }}}
	@Override
	public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
		if (detailFragment != null) {
			scantime = System.currentTimeMillis();
			dbAdapter.insertMeasurement(currentPointId, data.get("BSSID")
					.getAsString(), data.get("SSID").getAsString(),
					data.get("frequency").getAsNumber().intValue(),
					data.get("level").getAsNumber().intValue(),
					data.get("timestamp").getAsFloat(), data.toString());
			updateModel(data.get("BSSID").getAsString());
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					detailFragment.networksSeen++;
					((TextView) detailFragment.rootView
							.findViewById(R.id.networks_seen))
							.setText(detailFragment.networksSeen + " Networks");
				}
			});
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (scantime > 0) {
						long currenttime = System.currentTimeMillis();
						if (currenttime - scantime > 4900) {
							funfManager.disablePipeline(PIPELINE_NAME);
							scantime = 0;
							currentPointId = null;
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									ProgressBar progress = ((ProgressBar) detailFragment.rootView
											.findViewById(R.id.progressBar));
									progress.setVisibility(View.INVISIBLE);
									detailFragment.refresh();
								}
							});
						}
					}
				}
			}, 5000L);
		}
	}

	@Override
	public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
	}

	@Override
	protected void onStop() {
		wifiProbe.unregisterPassiveListener(this);
		super.onStop();
	}

	public DatabaseAdapter getDb() {
		openDb();
		return dbAdapter;
	}

	private void openDb() {
		if (dbAdapter == null) {
			dbAdapter = new DatabaseAdapter(getApplicationContext());
		}
		if (dbAdapter.isClosed()) {
			dbAdapter.open();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (dbAdapter != null) {
			dbAdapter.close();
		}
	}
}

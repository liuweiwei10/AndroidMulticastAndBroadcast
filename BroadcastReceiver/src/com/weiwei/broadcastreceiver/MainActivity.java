package com.weiwei.broadcastreceiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

	private static final String TAG = "BroadcastReceiver";
	private static final String port = "32000"; 
	private TextView tv;
	private Button btnUpdate;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView) findViewById(R.id.tv_logcat);
		btnUpdate = (Button) findViewById(R.id.bt_update);
		
        // update the logcat whenever the update button is pressed
		btnUpdate.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				try {
				    String cmd = String.format("%s -d -v time %s *:S",
				              "logcat",
				              TAG);
					Process process = Runtime.getRuntime().exec(cmd);
					BufferedReader bufferedReader = new BufferedReader(
							new InputStreamReader(process.getInputStream()));

					StringBuilder log = new StringBuilder();
					String line = "";

					while ((line = bufferedReader.readLine()) != null) {
						log.append("\n").append(line);
					}
					//Log.d(TAG, "log:" + log.toString());
					tv.setText(log.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		
		new Thread(new Runnable() {
			public void run() {
				
				// call native method to receive broadcast, see the source code in /jni/native.c
				receiveBroadcast(port);
			}
		}).start();
      
	}

	private native int receiveBroadcast(String portNo);

	static {

		System.loadLibrary("jni");

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}

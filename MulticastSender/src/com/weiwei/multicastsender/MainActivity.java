package com.weiwei.multicastsender;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import android.support.v7.app.ActionBarActivity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
	private static final String TAG = "MulticastSender";
	private Button btnSend;
	private CheckBox cbIsByNative;
	private EditText etMessage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnSend = (Button) findViewById(R.id.btn_send);
		cbIsByNative = (CheckBox) findViewById(R.id.cb_is_native);
		etMessage = (EditText) findViewById(R.id.et_message);

		// send five packets to specified group whenever the send button is clicked
		btnSend.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (etMessage.getText().toString().trim().equals("")) {
					Toast.makeText(getApplicationContext(),
							"please input the message to send",
							Toast.LENGTH_SHORT).show();
				} else {
					//disable the send button
					v.setEnabled(false);

					// get multicast group address, port number and message to
					// send
					MulticastConfig mConfig = new MulticastConfig();
					final String multicastIp = mConfig.multicastIP;
					final String portNo = mConfig.portNo;
					final String message = etMessage.getText().toString();

					//reset the input box
					etMessage.setText("");

					if (cbIsByNative.isChecked()) {
						// send through native method
						new Thread(new Runnable() {
							public void run() {
								// call native method to send multicast, see source code in jni/native.c
								int result = sendMulticast(multicastIp, portNo,
										message);
								if (result == 0) {
									Log.d(TAG, "send multicast:success");
									Message msg = Message.obtain();
									msg.obj = result;
									msg.what = 200;
									handler.sendMessage(msg);

								} else {
									Log.d(TAG, "send multicast: failure");
									Message msg = Message.obtain();
									msg.obj = result;
									msg.what = 201;
									handler.sendMessage(msg);
								}
							}
						}).start();
					} else {
						// send through java method

						WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
						if (wifi != null) {
							WifiManager.MulticastLock lock = wifi
									.createMulticastLock("WifiDevices");
							lock.acquire();
						}
						Thread sendMulticast = new Thread(new MultiCastThread(multicastIp, Integer.parseInt(portNo), message));
						sendMulticast.start();
					}

				}
			}
		});
	}

	private native int sendMulticast(String mulitcastIp, String portNo,
			String message);

	static {

		System.loadLibrary("jni");

	}

	
	/*this is the thread sending multicast by java method*/
	public class MultiCastThread implements Runnable {
		MulticastSocket s;
		DatagramPacket pack;
		String groupAddr;
		int portNo;
		String message;

		public MultiCastThread(String groupAddr, int portNo, String message) {
			try {
				s = new MulticastSocket(portNo);
				s.joinGroup(InetAddress.getByName(groupAddr));
				this.groupAddr = groupAddr;
				this.portNo = portNo;
				this.message = message;
			} catch (Exception e) {
				Log.v("Socket Error: ", e.getMessage());
			}
		}

		@Override
		public void run() {

			try {
				for (int i = 0; i < 5; i++) {
					Log.d(TAG, "sending:" + message);
					pack = new DatagramPacket(message.getBytes(),
							message.getBytes().length,
							InetAddress.getByName(groupAddr), portNo);
					s.setTimeToLive(5);
					s.send(pack);
				    Thread.sleep(3000);
				}
				Message msg = Message.obtain();
				msg.obj = 200;
				msg.what = 200;
				handler.sendMessage(msg);
			} catch (Exception e) {
				Log.d(TAG, "send multicast failure:" + e.getMessage());
				Message msg = Message.obtain();
				msg.obj = 201;
				msg.what = 201;
				handler.sendMessage(msg);
			}
		}
	}

	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 200) {// multicast success
				btnSend.setEnabled(true);
				Toast.makeText(getApplicationContext(),
						"send multicast: success", Toast.LENGTH_SHORT).show();
			} else if (msg.what == 201) {
				btnSend.setEnabled(true);
				Toast.makeText(getApplicationContext(),
						"send multicast: failure", Toast.LENGTH_SHORT).show();
			}
		}
	};

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

package com.weiwei.broadcastsender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import android.support.v7.app.ActionBarActivity;
import android.content.Context;
import android.net.DhcpInfo;
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
	private static final String TAG = "BroadcastSender";
	private static InetAddress broadcastAddr;
	private static final int portNo = 32000;
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
		WifiManager wifi = (WifiManager) this
        .getSystemService(Context.WIFI_SERVICE);
		try {
			broadcastAddr = getBroadcastAddress(wifi);
		} catch (IOException e) {
			e.printStackTrace();
		}
        Log.i(TAG, "broadcast addr: " + broadcastAddr.getHostAddress());

		// send five broadcast packets 
				btnSend.setOnClickListener(new Button.OnClickListener() {
					public void onClick(View v) {
						if (etMessage.getText().toString().trim().equals("")) {
							Toast.makeText(getApplicationContext(),
									"please input the message to send",
									Toast.LENGTH_SHORT).show();
						} else {
							//disable the send button
							v.setEnabled(false);

							// get the message to send
							final String message = etMessage.getText().toString();

							//reset the input box
							etMessage.setText("");

							if (cbIsByNative.isChecked()) {
								// send through native method
								new Thread(new Runnable() {
									public void run() {
										// call native method to send multicast, see source code in jni/native.c
										int result = sendBroadcast(broadcastAddr.getHostAddress(), portNo,
												message);
										if (result == 0) {
											Log.d(TAG, "send broadcast:success");
											Message msg = Message.obtain();
											msg.obj = result;
											msg.what = 200;
											handler.sendMessage(msg);

										} else {
											Log.d(TAG, "send broadcast: failure");
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
								Thread sendMulticast = new Thread(new BroadcastThread(broadcastAddr, portNo, message));
								sendMulticast.start();
							}

						}
					}
				});
			
	}
	
	private native int sendBroadcast(String broadcastIp, int portNo,
			String message);

	static {
		System.loadLibrary("jni");
	}
	
	public class BroadcastThread implements Runnable {
		DatagramSocket s;
		DatagramPacket pack;
		InetAddress addr;
		int portNo;
		String message;

		public BroadcastThread(InetAddress addr, int portNo, String message) {
			try {
				s = new DatagramSocket(portNo);
				this.addr = addr;
				this.portNo = portNo;
				this.message = message;
			} catch (Exception e) {
				Log.v("Socket Error: ", e.getMessage());
			}
		}

		@Override
		public void run() {

			try {				
				for(int i=0; i < 5; i++) {
					Log.d(TAG, "sending: " + message);
				    sendBroadcastMsg(addr, portNo, message, s, pack);
				    Thread.sleep(3000);
				}
				s.close();
				Log.d(TAG, "send broadcast success:");
				Message msg = Message.obtain();
				msg.obj = 200;
				msg.what = 200;
				handler.sendMessage(msg);
			
			}catch (Exception e) {
				Log.d(TAG, "send broadcast failure:" + e.getMessage());
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
			if (msg.what == 200) {// broadcast success
				btnSend.setEnabled(true);
				Toast.makeText(getApplicationContext(),
						"send broadcast: success", Toast.LENGTH_SHORT).show();
			} else if (msg.what == 201) {
				btnSend.setEnabled(true);
				Toast.makeText(getApplicationContext(),
						"send broadcast: failure", Toast.LENGTH_SHORT).show();
			}
		}
	};
	
    private void sendBroadcastMsg(InetAddress broadcastAddr, int port, String message, DatagramSocket socket, DatagramPacket packet) throws IOException {

        socket.setBroadcast(true);
      
        packet = new DatagramPacket(message.getBytes(),
                message.length(), broadcastAddr, port);
        
        socket.send(packet);
/*
        byte[] buffer = new byte[1024];
        DatagramPacket receivedPacket = new DatagramPacket(buffer,
                buffer.length);
        socket.receive(receivedPacket);

        String s = new String(receivedPacket.getData());
        Log.i(TAG, "MSG Received: " + s);*/
    }
    
    private InetAddress getBroadcastAddress(WifiManager wifi)
            throws IOException {
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null) {
            Log.i(TAG, "dhcp is null!");
            return InetAddress.getByName("255.255.255.255");
        }
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; ++k) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
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

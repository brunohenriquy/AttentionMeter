package org.attentionmeter.facedetect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.app.Service;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

public class Server extends Service {
	
	public static final String TAG = "MyServiceTag";
	/**
	 * @uml.property  name="myBinder"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private final IBinder myBinder = new LocalBinder();

	/**
	 * @uml.property  name="info"
	 * @uml.associationEnd  readOnly="true"
	 */
	TextView info;
	/**
	 * @uml.property  name="infoip"
	 * @uml.associationEnd  readOnly="true"
	 */
	TextView infoip;
	/**
	 * @uml.property  name="msg"
	 * @uml.associationEnd  readOnly="true"
	 */
	TextView msg;
	/**
	 * @uml.property  name="message"
	 */
	String message = "";
	/**
	 * @uml.property  name="serverSocket"
	 */
	ServerSocket serverSocket;
	
    public static final String BROADCAST_ACTION = "com.websmithing.broadcasttest.displayevent";
    /**
	 * @uml.property  name="handler"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private final Handler handler = new Handler();
    /**
	 * @uml.property  name="intent"
	 * @uml.associationEnd  
	 */
    Intent intent;
	
	@Override
    public void onCreate() {
        super.onCreate();
        
        Thread socketServerThread = new Thread(new SocketServerThread());
		socketServerThread.start();
        
        intent = new Intent(BROADCAST_ACTION);
    }


    @SuppressWarnings("deprecation")
	public void onStart(Intent intent, int startId){
        super.onStart(intent, startId);
        
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 100); // 100 Milliseconds
        
    }
    
    /**
	 * @uml.property  name="sendUpdatesToUI"
	 */
    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            DisplayLoggingInfo();            
            handler.postDelayed(this, 100); // 100 Milliseconds
        }
    };
    
    private void DisplayLoggingInfo() {
        Log.d(TAG, "entered DisplayLoggingInfo");
 
        if (message != ""){
	        String[] separated = message.split("-");
	        intent.putExtra("ECG", separated[0]);
	        intent.putExtra("GSR", separated[1]);
	        intent.putExtra("EEG", separated[2]);
	        intent.putExtra("EMG", separated[3]);
        }
        
        if (getIpAddress() != ""){
        	intent.putExtra("IP", getIpAddress() + ":" + serverSocket.getLocalPort());
        }
        
        sendBroadcast(intent);
    }

	public class SocketServerThread extends Thread {

		static final int SocketServerPORT = 8080;
		int count = 0;

		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(SocketServerPORT);

				while (true) {
					Socket socket = serverSocket.accept();
					
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
					byte[] buffer = new byte[1024];
					
					int bytesRead;
					InputStream inputStream = socket.getInputStream();
					
					/*
					 * notice:
					 * inputStream.read() will block if no data return
					 */
		            while ((bytesRead = inputStream.read(buffer)) != -1){
		                byteArrayOutputStream.write(buffer, 0, bytesRead);
		                message = byteArrayOutputStream.toString("UTF-8");
		            }

				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private String getIpAddress() {
		String ip = "";
		try {
			Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
					.getNetworkInterfaces();
			while (enumNetworkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = enumNetworkInterfaces
						.nextElement();
				Enumeration<InetAddress> enumInetAddress = networkInterface
						.getInetAddresses();
				while (enumInetAddress.hasMoreElements()) {
					InetAddress inetAddress = enumInetAddress.nextElement();

					if (inetAddress.isSiteLocalAddress()) {
						ip += "SiteLocalAddress: " 
								+ inetAddress.getHostAddress();
					}
					
				}

			}

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ip += "Something Wrong! " + e.toString() + "\n";
		}

		return ip;
	}

	@Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return myBinder;
    }

    public class LocalBinder extends Binder {
        public Server getService() {
            return Server.this;
        }
    }

}

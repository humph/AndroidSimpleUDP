package udp.yarracoders.com;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.OptionsMenu;
import com.googlecode.androidannotations.annotations.OptionsItem;

/*
 * Simple hacky app to send UDP packets used to control power of a remote device
 * Set hostname, port and the three UDP messages as user defined preferences in Settings Menu
 * Select which UDP message to send using the spinner/dropdown on the main screen 
 * The UI thread shows and (bit nasty) also temporarily stores the values of host, port and selected message to send when SEND button hit
 * (That could be improved!)
 * Some error conditions not yet handled well (such as empty values in hostname)
 * Uses Android Annotations https://github.com/excilys/androidannotations
 * Inspired by the code snippet for boxeeremote at http://code.google.com/p/boxeeremote/wiki/AndroidUDP
 * Developed at the Melbourne Android Australia User Group for the Global Android Dev Camp 17-19th Feb 2012.
 * Thanks to Glenn and Guy; James for organizing and Daniel for the venue.  
 */

@EActivity
@OptionsMenu(R.menu.menu)
public class UDPActivity extends Activity implements OnSharedPreferenceChangeListener {
	
	@ViewById
	TextView response;
	
	@ViewById
	TextView hostname;
	
	@ViewById
	TextView port;
	
	@ViewById
	TextView message;
	
	@ViewById
	Spinner spinner;
	
//	@Pref
	//MyPrefs_ myPrefs;	
	
	SharedPreferences prefs;

	/*
	// Set up spinner
	@AfterViews
	protected void init () {
	ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.defined_messages, android.R.layout.simple_spinner_item);
	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	spinner.setAdapter(adapter);
	}
	*/
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		// Populate the display with info on current settings
		hostname.setText(prefs.getString("prefHostname", ""));
		port.setText(prefs.getString("prefPort",""));
		
		//Set up spinner and listener
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.defined_messages, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

	}
	
	// MENU ITEM HANDLING
	@OptionsItem(R.id.menuPrefs)
	void clickPrefs () {
		startActivity(new Intent(this, PrefsActivity.class));
	}
	
	@OptionsItem(R.id.menuHelp)
	void clickHelp() {
		printOutput("No Help Yet");
	}
		
	// Handle Selection of Different Messages via spinner selection
	public class MyOnItemSelectedListener implements OnItemSelectedListener {
		
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {			
			
			String selectedItemString =  parent.getItemAtPosition(pos).toString();
			message.setText(prefs.getString(selectedItemString, ""));
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		// No Action
		}
	}
	
	// Button handler to send the UDP message
	
	@Click(R.id.sendPacketButton)
	void clickSendPacketButton() {
		//Try sending packet
		DatagramSocket socket;
		try {
			int portInt = Integer.parseInt(port.getText().toString());
			if (portInt <1024) {
				printOutput("Can't send to a port below 1024 \n");
				port.setText("1024");
			}
			
			socket=openSocket(portInt);
			
			sendPacket(socket, portInt);
			
		} catch (NumberFormatException e) {
			printOutput("Port must be a number! \n");
		}
		
		//Save message to defined message list
		// TO DO
	}

	DatagramSocket openSocket(int port) {
		try	{
			return new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	// Network interactions in the background on other thread
	@Background
	void sendPacket(DatagramSocket socket, int port) {
		try	{
			InetAddress address = InetAddress.getByName(hostname.getText().toString());
			
			DatagramPacket packet = new DatagramPacket((message.getText().toString()).getBytes(), message.length(), address, port);
			socket.send(packet);
			Log.d("AndroidUDP", "Sent: " + packet);
			printOutput("Packet sent, waiting for response \n");
			
			//Now get response
				byte[] buf = new byte[1024];
				packet = new DatagramPacket(buf, buf.length);
				socket.setSoTimeout(5000);
				socket.receive(packet);
				if (packet.getData().length < 1) {
					printOutput("Timed out with no response \n");
				} else {
					String s = new String(packet.getData(), 0, packet.getLength());
					printOutput(s);
					Log.d("AndroidUDP", "Received: "+ s);
					printOutput(s);
				}
		
		}	catch (UnknownHostException e) {
			e.printStackTrace();
		}	catch (SocketException e) {
			e.printStackTrace();
		}	catch (IOException e) {
			e.printStackTrace();
		}	finally {
			socket.close();
		}
	}
	
	
	// Show Response and other updates on UI
	@UiThread
	void printOutput(String update) {
		response.setText(update);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// Change what's displayed on main display and what is used when sending data
		hostname.setText(prefs.getString("prefHostname", ""));
		port.setText(prefs.getString("prefPort",""));
		
		// Note that changing URL message prefs does not currently change data in field below spinner. Need to fix that bug.
		
	}
}
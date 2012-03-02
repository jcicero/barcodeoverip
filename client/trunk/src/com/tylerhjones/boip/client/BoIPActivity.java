/*
 * 
 * BarcodeOverIP Client (Android < v3.2) Version 0.3.1 Beta
 * Copyright (C) 2012, Tyler H. Jones (me@tylerjones.me)
 * http://boip.tylerjones.me/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Filename: SeverListActivity.java
 * Package Name: com.tylerhjones.boip.client
 * Created By: Tyler H. Jones on Feb 27, 2012 at 7:26:55 PM
 * 
 * Description: Main activity in BoIP Client. Everything starts from here...
 */


package com.tylerhjones.boip.client;

import java.util.ArrayList;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class BoIPActivity extends ListActivity {
	
	private static final String TAG = "BoIPActivity";
	private ProgressDialog ConnectingProgress = null;
	private ArrayList<Server> Servers = new ArrayList<Server>();
	private ServerAdapter theAdapter;
	private Database DB = new Database(this);
	private Server SelectedServer;
	
	// private BoIPClient client = new BoIPClient(this);
	// private Runnable ConnectServer;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		lv("onCreate() called!");

		//runOnUiThread(ConnectResult);
		this.theAdapter = new ServerAdapter(this, R.layout.serverlist_item, Servers);
		setListAdapter(theAdapter);
		UpdateList();
		
		registerForContextMenu(getListView());
		getListView().setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				SelectedServer = Servers.get(position);
				showScanBarcode();
			}
		});
	}
	
	/** App starts anything it needs to start */
	public void onStart() {
		super.onStart();
	}
	
	/** App kills anything it started */
	public void onStop() {
		super.onStop();
	}
	
	/** App starts displaying things */
	public void onResume() {
		super.onResume();
		this.UpdateList();
	}
	
	/** App goes into background */
	public void onPause() {
		super.onPause();
	}

	/*************************************************************************/
	/** Event handler functions ******************************************** */
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == getListView().getId()) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			menu.setHeaderTitle(Servers.get(info.position).getName());
			String[] menuItems = getResources().getStringArray(R.array.cmenu_serverlist);
			for (int i = 0; i < menuItems.length; i++) {
				menu.add(Menu.NONE, i, i, menuItems[i]);
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int menuItemIndex = item.getItemId();
		if (menuItemIndex == 1) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.deleteserver_msg_body)).setTitle(getText(R.string.deleteserver_msg_title)).setCancelable(false)
									.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int id) {
											DB.open();
											DB.deleteServer(Servers.get(info.position));
											DB.close();
											UpdateList();
										}
									}).setNegativeButton("No", new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int id) {
											dialog.cancel();
										}
									});
			AlertDialog adialog = builder.create();
			adialog.show();
			return true;
		} else {
			showServerInfo(Servers.get(info.position));
			return true;
		}
	}

	private void UpdateList() {
		
		DB.open();
		Servers.clear();
		Servers = DB.getAllServers();
		DB.close();
		lv("UpdateList(): Got Servers, clearing adapter...");
		theAdapter.clear();

		lv("UpdateList(): Got servers. Count: " + Servers.size());
		if (Servers != null && Servers.size() > 0) {
			theAdapter.notifyDataSetChanged();
			for (int i = 0; i < Servers.size(); i++) {
				theAdapter.add(Servers.get(i));
			}
		}
	}

	private class ServerAdapter extends ArrayAdapter<Server> {
		
		private ArrayList<Server> items;

		public ServerAdapter(Context context, int textViewResourceId, ArrayList<Server> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.serverlist_item, null);
			}
			Server SVR = items.get(position);
			if (SVR != null) {
				TextView tt = (TextView) v.findViewById(R.id.toptext);
				TextView bt = (TextView) v.findViewById(R.id.bottomtext);
				if (tt != null) {
					tt.setText(SVR.getName());
				}
				if (bt != null) {
					bt.setText(SVR.getHost() + ":" + String.valueOf(SVR.getPort()));
				}
			}
			return v;
		}
	}
	

	/******************************************************************************************/
	/** Send Barcode to Server ****************************************************************/
/*
 * public boolean ValidateConnection(Server s) {
 * 
 * //final BoIPClient client = new BoIPClient(s, this);
 * 
 * Runnable ConnectServer = new Runnable() {
 * 
 * @Override
 * public void run() {
 * client.connect();
 * client.Validate();
 * if(client.CanConnect) {
 * client.sendBarcode(code);
 * }
 * client.close();
 * ConnectingProgress.dismiss();
 * }
 * };
 * 
 * Thread thread = new Thread(null, ConnectServer, "MagentoBackground");
 * thread.start();
 * ConnectingProgress = ProgressDialog.show(BoIPActivity.this, "Please wait.", "Validating client with the server...", true);
 * }
 */

	public void SendBarcode(Server s, final String code) {
		
		Log.v(TAG, "SendBarcode called! Barcode: '" + code + "'");
		final BoIPClient client = new BoIPClient(this, s);
		// client.setServer(s);

		Runnable ConnectServer = new Runnable() {
			
			@Override
			public void run() {
				client.connect();
				client.Validate();
				if (client.CanConnect) {
					client.sendBarcode(code);
				}
				client.close();
				ConnectingProgress.dismiss();
			}
		};
		
		Thread thread = new Thread(null, ConnectServer, "MagentoBackground");
		thread.start();
		ConnectingProgress = ProgressDialog.show(BoIPActivity.this, "Please wait.", "Sending barcode to server...", true);
	}
	

	// ------------------------------------------------------------------------------------
	// --- Setup Menus --------------------------------------------------------------------
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.mnuMainExit:
				this.finish();
				return true;
			case R.id.mnuMainAbout:
				AlertDialog adialog = null;
				adialog = new AlertDialog.Builder(this).create();
				adialog.setCancelable(true); // If 'false' This blocks the 'BACK' button
				adialog.setMessage(getText(R.string.about_msg_body));
				adialog.setTitle(getText(R.string.about_msg_title));
				adialog.setButton("OK", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				adialog.show();
				return true;
			case R.id.mnuMainDonate:
				Uri uri = Uri.parse("http://" + getText(R.string.project_donate_site));
				startActivity(new Intent(Intent.ACTION_VIEW, uri));
				return true;
			case R.id.mnuMainAddServer:
				this.addServer();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/******************************************************************************************/
	/** Launch ServerInfoActivity *************************************************************/
	
	private void showServerInfo(Server s) { // Server object given, edit server
		Intent intent = new Intent();
		intent.setClassName("com.tylerhjones.boip.client", "com.tylerhjones.boip.client.ServerInfoActivity");
		intent.putExtra("com.tylerhjones.boip.client.ServerName", s.getName());
		intent.putExtra("com.tylerhjones.boip.client.Action", Common.EDIT_SREQ);
		startActivityForResult(intent, Common.EDIT_SREQ);
	}
	
	private void addServer() {
		Intent intent = new Intent();
		intent.setClassName("com.tylerhjones.boip.client", "com.tylerhjones.boip.client.ServerInfoActivity");
		intent.putExtra("com.tylerhjones.boip.client.Action", Common.ADD_SREQ);
		startActivityForResult(intent, Common.ADD_SREQ);
	}
	
	public void showScanBarcode() {
		// ---- ZXing Product Lookup Window -------------------------------------
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
		intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
		intent.putExtra("SCAN_WIDTH", 800);
		intent.putExtra("SCAN_HEIGHT", 200);
		intent.putExtra("RESULT_DISPLAY_DURATION_MS", 500L);
		intent.putExtra("PROMPT_MESSAGE", "BarcodeOverIP -  Scan a barcode for transmission to target system");
		startActivityForResult(intent, IntentIntegrator.REQUEST_CODE);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		this.UpdateList();
		if (requestCode == Common.ADD_SREQ) {
			if (resultCode == RESULT_OK) {
				this.UpdateList();
				Toast.makeText(this, "Server(s) updated successfully!", 5);
			} else {
				Toast.makeText(this, "No changes were made.", 3);
			}
		}
		if (requestCode == Common.EDIT_SREQ) {
			if (resultCode == RESULT_OK) {
				this.UpdateList();
				Toast.makeText(this, "Server edited successfully!", 5);
			} else {
				Toast.makeText(this, "No changes were made.", 3);
			}
		}
		if (resultCode == Common.BARCODE_SREQ) {
			if (resultCode == RESULT_OK) {
				IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
				String barcode = result.getContents().toString();
				if (barcode != null) {
					this.SendBarcode(SelectedServer, barcode);
				}
			}
		} else {
			Toast.makeText(this, "No activity called!", 6);
		}
	}
	
	
	/** Logging shortcut functions **************************************************** */
	
	public void ld(String msg) { // Debug message
		Log.d(TAG, msg);
	}

	public void ld(String msg, String val) { // Debug message with one string value passed
		Log.d(TAG, msg + val);
	}
	
	public void ld(String msg, String val1, String val2) { // Debug message with two string values passed
		Log.d(TAG, msg + val1 + " - " + val2);
	}
	
	public void ld(String msg, int val) { // Debug message with one integer value passed
		Log.d(TAG, msg + String.valueOf(val));
	}
	
	public void lv(String msg) { // Verbose message
		Log.v(TAG, msg);
	}
	
	public void lv(String msg, String val) { // Verbose message with one string value passed
		Log.v(TAG, msg + val);
	}
	
	public void lv(String msg, String val1, String val2) { // Verbose message with two string values passed
		Log.v(TAG, msg + val1 + " - " + val2);
	}
	
	public void lv(String msg, int val) { // Verbose message with one integer value passed
		Log.v(TAG, msg + String.valueOf(val));
	}
	
	public void li(String msg) { // Info message
		Log.v(TAG, msg);
	}
	
	public void li(String msg, String val) { // Info message with one string value passed
		Log.v(TAG, msg + val);
	}
}
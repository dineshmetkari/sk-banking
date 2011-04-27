/*
 * Copyright (C) 2010 Nullbyte <http://nullbyte.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jjsan.eu.skbanking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jjsan.eu.skbanking.R;
import jjsan.eu.skbanking.appwidget.AutoRefreshService;
import jjsan.eu.skbanking.banking.Account;
import jjsan.eu.skbanking.banking.Bank;
import jjsan.eu.skbanking.banking.BankFactory;
import jjsan.eu.skbanking.banking.exceptions.BankException;
import jjsan.eu.skbanking.banking.exceptions.LoginException;

import jjsan.eu.skbanking.R;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


public class BankEditActivity extends LockableActivity implements OnClickListener, OnItemSelectedListener {
	private final static String TAG = "AccountActivity";
	private Bank SELECTED_BANK;
	private long BANKID = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bank);
		ArrayList<Bank> items = BankFactory.listBanks(this);
		Collections.sort(items);
		Spinner spnBanks = (Spinner)findViewById(R.id.spnBankeditBanklist);
		BankSpinnerAdapter<Bank> adapter = new BankSpinnerAdapter<Bank>(this, android.R.layout.simple_spinner_item, items);
		spnBanks.setAdapter(adapter);
		spnBanks.setOnItemSelectedListener(this);

		findViewById(R.id.btnSettingsCancel).setOnClickListener(this);
		findViewById(R.id.btnSettingsOk).setOnClickListener(this);
		
		Log.d(TAG, "=================Layout=================");
		Bundle extras = getIntent().getExtras(); 
		if (extras != null) {
			BANKID = extras.getLong("id", -1);
			if (BANKID != -1) {
				Bank bank = BankFactory.bankFromDb(BANKID, this, false);
				if (bank != null) {
					((EditText)findViewById(R.id.edtBankeditUsername)).setText(bank.getUsername());
                    ((EditText)findViewById(R.id.edtBankeditPassword)).setText(bank.getPassword());
                    ((EditText)findViewById(R.id.edtBankeditvubpin)).setText(bank.getvubpin());
                    ((EditText)findViewById(R.id.edtBankeditCustomName)).setText(bank.getCustomName());
                    Log.d(TAG, "================= bank != null =================");
					TextView errorDesc = (TextView)findViewById(R.id.txtErrorDesc);
					if (bank.isDisabled()) {
						errorDesc.setVisibility(View.VISIBLE);
					}
					else {
						errorDesc.setVisibility(View.INVISIBLE);
					}
					SELECTED_BANK = bank;
					for (int i = 0; i < items.size(); i++) {
						if (bank.getBanktypeId() == items.get(i).getBanktypeId()) {
							spnBanks.setSelection(i);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btnSettingsCancel) {
			this.finish();
		}
		else if (v.getId() == R.id.btnSettingsOk){
			SELECTED_BANK.setUsername(((EditText) findViewById(R.id.edtBankeditUsername)).getText().toString().trim());
            SELECTED_BANK.setPassword(((EditText) findViewById(R.id.edtBankeditPassword)).getText().toString().trim());
            SELECTED_BANK.setvubpin(((EditText) findViewById(R.id.edtBankeditvubpin)).getText().toString().trim());
            SELECTED_BANK.setCustomName(((EditText) findViewById(R.id.edtBankeditCustomName)).getText().toString().trim());
            SELECTED_BANK.setDbid(BANKID);
			new DataRetrieverTask(this, SELECTED_BANK).execute();
			
			Log.d(TAG, "=================Finished Setting up name and password=================");
		}

	}

	@Override
	public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
		
		SELECTED_BANK = (Bank)parentView.getItemAtPosition(pos);
		EditText edtUsername = (EditText)findViewById(R.id.edtBankeditUsername);
        EditText edtPassword = (EditText)findViewById(R.id.edtBankeditPassword);
        EditText edtvubpin = (EditText)findViewById(R.id.edtBankeditvubpin);
        TextView txtUsername = (TextView)findViewById(R.id.txtBankeditUsername);
        TextView txtPassword = (TextView)findViewById(R.id.txtBankeditPassword);
        TextView txtvubpin = (TextView)findViewById(R.id.txtBankeditvubpin);
        
		edtUsername.setInputType(SELECTED_BANK.getInputTypeUsername());
		edtUsername.setHint(SELECTED_BANK.getInputHintUsername());
		
		edtPassword.setInputType(SELECTED_BANK.getInputTypePassword());
		edtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
		edtPassword.setTypeface(Typeface.MONOSPACE);

		edtvubpin.setInputType(SELECTED_BANK.getInputTypePassword());
		edtvubpin.setTransformationMethod(PasswordTransformationMethod.getInstance());
		edtvubpin.setTypeface(Typeface.MONOSPACE);
		
        txtUsername.setText(SELECTED_BANK.getInputTitleUsername());
        txtPassword.setText(SELECTED_BANK.getInputTitlePassword());
        txtvubpin.setText(SELECTED_BANK.getInputTitlevubpin());
		
        if (SELECTED_BANK.isInputUsernameHidden()) {
            edtUsername.setVisibility(View.GONE);
            txtUsername.setVisibility(View.GONE);
        }
        else {
            edtUsername.setVisibility(View.VISIBLE);
            txtUsername.setVisibility(View.VISIBLE);
        }
        
        if (SELECTED_BANK.isInputPasswordHidden()) {
            edtPassword.setVisibility(View.GONE);
            txtPassword.setVisibility(View.GONE);
        }
        else {
            edtPassword.setVisibility(View.VISIBLE);
            txtPassword.setVisibility(View.VISIBLE);
        }
        if (SELECTED_BANK.isInputvubpinHidden()) {
        	 edtvubpin.setVisibility(View.GONE);
        	 txtvubpin.setVisibility(View.GONE);
        }
        else {
            edtvubpin.setVisibility(View.VISIBLE);
            txtvubpin.setVisibility(View.VISIBLE);
        }
	
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg) {
	}

	private class BankSpinnerAdapter<T> extends ArrayAdapter<T> {
		private int resource;
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = ((LayoutInflater)super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(resource, parent, false);
			}
			((TextView)convertView).setText(((Bank)getItem(position)).getName());
			return convertView;			
		}

		public BankSpinnerAdapter(Context context, int textViewResourceId, List<T> items) {
			super(context, textViewResourceId, items);
			resource = textViewResourceId;
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			if (convertView == null) {
				convertView = ((LayoutInflater)super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
			}
			((TextView)convertView).setText(((Bank)getItem(position)).getName());
			return convertView;
		}


	}
	private class DataRetrieverTask extends AsyncTask<String, Void, Void> {
		private final ProgressDialog dialog = new ProgressDialog(BankEditActivity.this);
		private Exception exc = null;
		private Bank bank;
		private BankEditActivity context;
		private Resources res;

		public DataRetrieverTask(BankEditActivity context, Bank bank) {
			Log.d(TAG, "=================DataRetrieverTask=================");
			this.context = context;
			this.res = context.getResources();
			this.bank = bank;
		}
		protected void onPreExecute() {
			Log.d(TAG, "=================onPreExecute=================");
			this.dialog.setMessage(res.getText(R.string.logging_in));
			this.dialog.show();
		}

		protected Void doInBackground(final String... args) {
			try {
				Log.d(TAG, "=================doInBackground=================");
				Log.d(TAG, "Updating "+bank);
				bank.update();
				Log.d(TAG, "=================U=================");
				bank.updateAllTransactions();
				bank.closeConnection();
				Log.d(TAG, "Saving "+bank);
				bank.save();
				Log.d(TAG, "Disabled: " + bank.isDisabled());
				// Transactions updated.
				final SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(getBaseContext());
				if (prefs.getBoolean("content_provider_enabled", false)) {
					final ArrayList<Account> accounts = bank.getAccounts();
					for (final Account account : accounts) {
						AutoRefreshService.broadcastTransactionUpdate(
								getBaseContext(), bank.getDbId(),
								account.getId());
					}
				}
			} catch (BankException e) {
				this.exc = e;
			} catch (LoginException e) {
				this.exc = e;
			}
			return null;
		}

		protected void onPostExecute(final Void unused) {
			Log.d(TAG, "=================onPostExecute=================");
			AutoRefreshService.sendWidgetRefresh(context);
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			if (this.exc != null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(BankEditActivity.this);
				builder.setMessage(this.exc.getMessage()).setTitle(res.getText(R.string.could_not_create_account))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
			else {
				context.finish();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
}

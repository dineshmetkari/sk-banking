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
import jjsan.eu.skbanking.R;
import jjsan.eu.skbanking.appwidget.AutoRefreshService;
import jjsan.eu.skbanking.banking.Account;
import jjsan.eu.skbanking.banking.Bank;
import jjsan.eu.skbanking.banking.BankFactory;
import jjsan.eu.skbanking.banking.exceptions.BankException;
import jjsan.eu.skbanking.banking.exceptions.LoginException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;


public class DataRetrieverTask extends AsyncTask<String, String, Void> {
	private final ProgressDialog dialog;
	private ArrayList<String> errors;
	private final MainActivity parent;
	private int bankcount;
	private final Resources res;
	private long bankId = -1;

	public DataRetrieverTask(final MainActivity parent) {
		this.parent = parent;
		this.res = parent.getResources();
		this.dialog = new ProgressDialog(parent);
	}

	public DataRetrieverTask(final MainActivity parent, final long bankId) {
		this(parent);
		this.bankId = bankId;
	}

	@Override
	protected void onPreExecute() {
		this.dialog.setMessage(res.getText(R.string.updating_account_balance)
				+ "\n ");
		this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		this.dialog.setCancelable(false);
		this.dialog.show();
	}

	@Override
	protected Void doInBackground(final String... args) {
		errors = new ArrayList<String>();
		ArrayList<Bank> banks;
		if (bankId != -1) {
			banks = new ArrayList<Bank>();
			banks.add(BankFactory.bankFromDb(bankId, parent, true));
		} else {
			banks = BankFactory.banksFromDb(parent, true);
		}
		bankcount = banks.size();
		this.dialog.setMax(bankcount);
		int i = 0;
		for (final Bank bank : banks) {
			publishProgress(new String[] { new Integer(i).toString(),
					bank.getName() + " (" + bank.getUsername() + ")" });
			if (bank.isDisabled()) {
				Log.d("AA", bank.getName() + " (" + bank.getUsername()
						+ ") is disabled. Skipping refresh.");
				continue;
			}
			Log.d("AA",
					"Refreshing " + bank.getName() + " (" + bank.getUsername()
							+ ").");
			try {
				// Log.d("AA", "bank.update()");
				bank.update();
				// Log.d("AA", "bank.updateAllTransactions()");
				bank.updateAllTransactions();
				// Log.d("AA", "bank.closeConnction()");
				bank.closeConnection();
				// Log.d("AA", "bank.save()");
				bank.save();
				// Log.d("AA", "DONE!");
				i++;
			} catch (final BankException e) {
				this.errors.add(bank.getName() + " (" + bank.getUsername()
						+ ")");
			} catch (final LoginException e) {
				this.errors.add(bank.getName() + " (" + bank.getUsername()
						+ ")");
				bank.disable();
			}

			final SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(parent);
			if (prefs.getBoolean("content_provider_enabled", false)) {
				final ArrayList<Account> accounts = bank.getAccounts();
				for (final Account account : accounts) {
					AutoRefreshService.broadcastTransactionUpdate(parent,
							bank.getDbId(), account.getId());
				}
			}
		}
		publishProgress(new String[] { new Integer(i).toString(), "" });
		return null;
	}

	@Override
	protected void onProgressUpdate(final String... args) {
		this.dialog.setProgress(new Integer(args[0]));
		this.dialog.setMessage(res.getText(R.string.updating_account_balance)
				+ "\n" + args[1]);
	}

	@Override
	protected void onPostExecute(final Void unused) {
		parent.refreshView();
		AutoRefreshService.sendWidgetRefresh(parent);
		if (this.dialog.isShowing()) {
			this.dialog.dismiss();
		}

		if ((this.errors != null) && !this.errors.isEmpty()) {
			final StringBuilder errormsg = new StringBuilder();
			errormsg.append(res.getText(R.string.accounts_were_not_updated)
					+ ":\n");
			for (final String err : errors) {
				errormsg.append(err);
				errormsg.append("\n");
			}
			final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
			builder.setMessage(errormsg.toString())
					.setTitle(res.getText(R.string.errors_when_updating))
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setNeutralButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									dialog.cancel();
								}
							});
			final AlertDialog alert = builder.create();
			alert.show();
		}
	}
}

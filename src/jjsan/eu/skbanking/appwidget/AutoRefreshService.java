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

package jjsan.eu.skbanking.appwidget;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import jjsan.eu.skbanking.Helpers;
import jjsan.eu.skbanking.MainActivity;
import jjsan.eu.skbanking.R;
import jjsan.eu.skbanking.banking.Account;
import jjsan.eu.skbanking.banking.Bank;
import jjsan.eu.skbanking.banking.BankFactory;
import jjsan.eu.skbanking.banking.exceptions.BankException;
import jjsan.eu.skbanking.banking.exceptions.LoginException;
import jjsan.eu.skbanking.db.DBAdapter;

public class AutoRefreshService extends Service {
	private final static String TAG = "AutoRefreshService";
	public final static String BROADCAST_WIDGET_REFRESH = "jjsan.eu.skbanking.WIDGET_REFRESH";
	public final static String BROADCAST_MAIN_REFRESH = "jjsan.eu.skbanking.MAIN_REFRESH";
	public final static String BROADCAST_REMOTE_NOTIFIER = "org.damazio.notifier.service.UserReceiver.USER_MESSAGE";
	public final static String BROADCAST_OPENWATCH_TEXT = "com.smartmadsoft.openwatch.action.TEXT";
	public final static String BROADCAST_OPENWATCH_VIBRATE = "com.smartmadsoft.openwatch.action.VIBRATE";
	public final static String ACTION_MAIN_SHOW_TRANSACTIONS = "jjsan.eu.skbanking.action.MAIN_SHOW_TRANSACTIONS";
	public final static String BROADCAST_TRANSACTIONS_UPDATED = "jjsan.eu.skbanking.action.TRANSACTIONS";

	@Override
	public void onCreate() {
		new DataRetrieverTask().execute();
	}

    @Override
	public void onDestroy() {
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	public static void showNotification(final String text, final int icon,
			final String title, final String bank, Context context) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (!prefs.getBoolean("notify_on_change", true)) {
			return;
		}

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		final Notification notification = new Notification(icon, text,
				System.currentTimeMillis());
		// Remove notification from statusbar when clicked
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		// http://www.freesound.org/samplesViewSingle.php?id=75235
		// http://www.freesound.org/samplesViewSingle.php?id=91924
		Log.d(TAG,
				"Notification sound: "
						+ prefs.getString("notification_sound", "none"));
		if (prefs.getString("notification_sound", null) != null) {
			notification.sound = Uri.parse(prefs.getString(
					"notification_sound", null));
		}
		if (prefs.getBoolean("notify_with_vibration", true)) {
			final long[] vib = { 0, 90, 130, 80, 350, 190, 20, 380 };
			notification.vibrate = vib;
			// notification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		final PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				new Intent(context, MainActivity.class), 0);

		notification.setLatestEventInfo(context, title, text, contentIntent);

		notificationManager.notify(R.id.about, notification);

		// Broadcast to Remote Notifier if enabled
		// http://code.google.com/p/android-notifier/
		if (prefs.getBoolean("notify_remotenotifier", false)) {
			final Intent i = new Intent(BROADCAST_REMOTE_NOTIFIER);
			i.putExtra("title", String.format("%s (%s)", bank, title));
			i.putExtra("description", text);
			context.sendBroadcast(i);
		}

		// Broadcast to OpenWatch if enabled
		// http://forum.xda-developers.com/showthread.php?t=554551
		if (prefs.getBoolean("notify_openwatch", false)) {
			Intent i;
			if (prefs.getBoolean("notify_openwatch_vibrate", false)) {
				i = new Intent(BROADCAST_OPENWATCH_VIBRATE);
			} else {
				i = new Intent(BROADCAST_OPENWATCH_TEXT);
			}
			i.putExtra("line1", String.format("%s (%s)", bank, title));
			i.putExtra("line2", text);
			context.sendBroadcast(i);
		}

	}

	private class DataRetrieverTask extends AsyncTask<String, String, Void> {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(AutoRefreshService.this);
		private ArrayList<String> errors;
		private Resources res;

		public DataRetrieverTask() {
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Void doInBackground(final String... args) {
			errors = new ArrayList<String>();
			Boolean refreshWidgets = false;
			final ArrayList<Bank> banks = BankFactory.banksFromDb(
					AutoRefreshService.this, true);
			if (banks.isEmpty()) {
				return null;
			}
			final DBAdapter db = new DBAdapter(AutoRefreshService.this);
			db.open();
			BigDecimal currentBalance;
			BigDecimal diff;
			final HashMap<String, Account> accounts = new HashMap<String, Account>();

			for (final Bank bank : banks) {
				if (prefs.getBoolean("debug_mode", false)
						&& prefs.getBoolean("debug_only_testbank", false)) {
					Log.d(TAG,
							"Debug::Only_Testbank is ON. Skipping update for "
									+ bank.getName());
					continue;
				}
				if (bank.isDisabled()) {
					Log.d(TAG, bank.getName() + " (" + bank.getDisplayName()
							+ ") is disabled. Skipping refresh.");
					continue;
				}
				Log.d(TAG,
						"Refreshing " + bank.getName() + " ("
								+ bank.getDisplayName() + ").");
				try {
					currentBalance = bank.getBalance();
					accounts.clear();
					for (final Account account : bank.getAccounts()) {
						accounts.put(account.getId(), account);
					}
					bank.update();
					diff = currentBalance.subtract(bank.getBalance());
					if (diff.compareTo(new BigDecimal(0)) != 0) {
						Account oldAccount;
						for (final Account account : bank.getAccounts()) {
							oldAccount = accounts.get(account.getId());
							if (oldAccount != null) {
								if (account.getBalance().compareTo(
										oldAccount.getBalance()) != 0) {
									boolean notify = false;
									switch (account.getType()) {
									case Account.REGULAR:
										notify = prefs.getBoolean(
												"notify_for_deposit", true);
										break;
									case Account.FUNDS:
										notify = prefs.getBoolean(
												"notify_for_funds", false);
										break;
									case Account.LOANS:
										notify = prefs.getBoolean(
												"notify_for_loans", false);
										break;
									case Account.CCARD:
										notify = prefs.getBoolean(
												"notify_for_ccards", true);
										break;
									case Account.OTHER:
										notify = prefs.getBoolean(
												"notify_for_other", false);
										break;
									}
									Log.d(TAG,
											"Account type: "
													+ account.getType()
													+ "; notify: " + notify);
									if (account.isHidden()
											|| !account.isNotify()) {
										notify = false;
									}
									if (notify) {
										diff = account.getBalance().subtract(
												oldAccount.getBalance());
										showNotification(
												account.getName()
														+ ": "
														+ ((diff.compareTo(new BigDecimal(
																0)) == 1) ? "+"
																: "")
														+ Helpers
																.formatBalance(
																		diff,
																		account.getCurrency())
														+ " ("
														+ Helpers
																.formatBalance(
																		account.getBalance(),
																		account.getCurrency())
														+ ")",
												bank.getImageResource(),
												bank.getDisplayName(),
												bank.getName(),
												AutoRefreshService.this);
									}

									refreshWidgets = true;
								}
							}
						}
						if (prefs.getBoolean(
								"autoupdates_transactions_enabled", true)) {
							bank.updateAllTransactions();
						}
					}
					bank.closeConnection();
					db.updateBank(bank);

					// Send update for all accounts since we're overwriting the
					// database transaction history
					if (prefs.getBoolean("content_provider_enabled", false)) {
						for (final Account account : bank.getAccounts()) {
							broadcastTransactionUpdate(getBaseContext(),
									bank.getDbId(), account.getId());
						}
					}
				} catch (final BankException e) {
					// Refresh widgets if an update fails
					Log.d(TAG, "Error while updating bank '" + bank.getDbId()
							+ "'; BankException: " + e.getMessage());
				} catch (final LoginException e) {
					Log.d(TAG, "Error while updating bank '" + bank.getDbId()
							+ "'; LoginException: " + e.getMessage());
					refreshWidgets = true;
					db.disableBank(bank.getDbId());
				}
			}

			if (refreshWidgets) {
				final Intent updateIntent = new Intent(BROADCAST_MAIN_REFRESH);
				sendBroadcast(updateIntent);
				sendWidgetRefresh(AutoRefreshService.this);
			}
			db.close();

			return null;
		}

		@Override
		protected void onProgressUpdate(final String... args) {
		}

		@Override
		protected void onPostExecute(final Void unused) {
			if ((this.errors != null) && !this.errors.isEmpty()) {
				final StringBuilder errormsg = new StringBuilder();
				errormsg.append(res.getText(R.string.accounts_were_not_updated)
						+ ":\n");
				for (final String err : errors) {
					errormsg.append(err);
					errormsg.append("\n");
				}
				Log.d("aa", errormsg.toString());
			}
			AutoRefreshService.this.stopSelf();
		}
	}

	public static void broadcastTransactionUpdate(final Context context,
			final long bankId, final String accountId) {
		final Intent i = new Intent(BROADCAST_TRANSACTIONS_UPDATED);
		i.putExtra("accountId", new Long(bankId).toString() + "_" + accountId);
		context.sendBroadcast(i);
	}

	public static void sendWidgetRefresh(final Context context) {
		// Send intent to BankdroidWidgetProvider
		final Intent updateIntent = new Intent(BROADCAST_WIDGET_REFRESH);
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
				0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		try {
			pendingIntent.send();
		} catch (final CanceledException e) {
			// TODO Auto-generated catch block
			Log.e("", e.getMessage(), e);
		}
	}
}
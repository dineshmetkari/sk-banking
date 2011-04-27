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

package jjsan.eu.skbanking.banking;

import java.math.BigDecimal;
import java.util.ArrayList;

import jjsan.eu.skbanking.banking.banks.O2sk;
import jjsan.eu.skbanking.banking.banks.VubSK;
import jjsan.eu.skbanking.banking.banks.mBank;
import jjsan.eu.skbanking.banking.exceptions.BankException;
import jjsan.eu.skbanking.db.Crypto;
import jjsan.eu.skbanking.db.DBAdapter;
import jjsan.eu.skbanking.provider.IBankTypes;

import net.sf.andhsli.hotspotlogin.SimpleCrypto;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;


public class BankFactory {

	public static Bank fromBanktypeId(int id, Context context) throws BankException {
		switch (id) {
//        case IBankTypes.TESTBANK:
//            return new TestBank(context);
//        case IBankTypes.PAYPAL:
//            return new PayPal(context);
        case IBankTypes.MBANK:
        	return new mBank(context);
        case IBankTypes.VUBSK:
        	return new VubSK(context);
        case IBankTypes.O2KS:
        	return new O2sk(context);


		default:
			throw new BankException("BankType id not found.");
		}
	}
	

	public static ArrayList<Bank> listBanks(Context context) {
		ArrayList<Bank> banks = new ArrayList<Bank>();

		//banks.add(new PayPal(context));
		banks.add(new mBank(context));
    	banks.add(new VubSK(context));
    	banks.add(new O2sk(context));
    	
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("debug_mode", true)) { 
            //banks.add(new TestBank(context));
        }
		return banks;
	}

	public static Bank bankFromDb(long id, Context context, boolean loadAccounts) {
		Bank bank = null;
		DBAdapter db = new DBAdapter(context);
		db.open();
		Cursor c = db.getBank(id);

		if (c != null) {
			try {
				bank = fromBanktypeId(c.getInt(c.getColumnIndex("banktype")), context);
				String password = "";
				String vubpin = "";
				try {
					password = SimpleCrypto.decrypt(Crypto.getKey(), c.getString(c.getColumnIndex("password")));
					vubpin = SimpleCrypto.decrypt(Crypto.getKey(), c.getString(c.getColumnIndex("vubpin")));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				bank.setData(c.getString(c.getColumnIndex("username")),
							 password, vubpin,
							 new BigDecimal(c.getString(c.getColumnIndex("balance"))),
							 (c.getInt(c.getColumnIndex("disabled")) == 0 ? false : true),
							 c.getLong(c.getColumnIndex("_id")),
							 c.getString(c.getColumnIndex("currency")),
							 c.getString(c.getColumnIndex("custname")));
				if (loadAccounts) {
					bank.setAccounts(accountsFromDb(context, bank.getDbId()));
				}
			} catch (BankException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				c.close();
			}
		}
		db.close();
		return bank;
	}

	public static ArrayList<Bank> banksFromDb(Context context, boolean loadAccounts) {
		ArrayList<Bank> banks = new ArrayList<Bank>();
		DBAdapter db = new DBAdapter(context);
		db.open();
		Cursor c = db.fetchBanks();
		if (c == null) {
			db.close();
			return banks;
		}
		while (!c.isLast() && !c.isAfterLast()) {
			c.moveToNext();
			//Log.d("AA", "Refreshing "+c.getString(clmBanktype)+" ("+c.getString(clmUsername)+").");
			try {
				Bank bank = fromBanktypeId(c.getInt(c.getColumnIndex("banktype")), context);
				
	            String password = "";
	            String vubpin = "";
                try {
                    password = SimpleCrypto.decrypt(Crypto.getKey(), c.getString(c.getColumnIndex("password")));
                    vubpin = SimpleCrypto.decrypt(Crypto.getKey(), c.getString(c.getColumnIndex("vubpin")));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                bank.setData(c.getString(c.getColumnIndex("username")),
				             password, vubpin,
				             new BigDecimal(c.getString(c.getColumnIndex("balance"))),
				             (c.getInt(c.getColumnIndex("disabled")) == 0 ? false : true),
				             c.getLong(c.getColumnIndex("_id")),
				             c.getString(c.getColumnIndex("currency")),
				             c.getString(c.getColumnIndex("custname")));
				if (loadAccounts) {
					bank.setAccounts(accountsFromDb(context, bank.getDbId()));
				}
				banks.add(bank);
			} catch (BankException e) {
				//e.printStackTrace();
			}
		}
		c.close();
		db.close();
		return banks;
	}
	
	public static Account accountFromDb(Context context, String accountId, boolean loadTransactions, boolean loadPartners) {
		DBAdapter db = new DBAdapter(context);
		db.open();
		Cursor c = db.getAccount(accountId);
       
		if (c == null || c.isClosed() || (c.isBeforeFirst() && c.isAfterLast())) {
			db.close();
			return null;
		}

		Account account = new Account(c.getString(c.getColumnIndex("name")),
                                      new BigDecimal(c.getString(c.getColumnIndex("balance"))),
                                      c.getString(c.getColumnIndex("id")).split("_", 2)[1],
                                      c.getLong(c.getColumnIndex("bankid")),
                                      c.getInt(c.getColumnIndex("acctype")));
        account.setHidden(c.getInt(c.getColumnIndex("hidden")) == 1 ? true : false);
        account.setNotify(c.getInt(c.getColumnIndex("notify")) == 1 ? true : false);
        account.setCurrency(c.getString(c.getColumnIndex("currency")));
		c.close();
		if (loadTransactions) {
			ArrayList<Transaction> transactions = new ArrayList<Transaction>();
			//"transdate", "btransaction", "amount"}			
			c = db.fetchTransactions(accountId);
			if (!(c == null || c.isClosed() || (c.isBeforeFirst() && c.isAfterLast()))) {
				while (!c.isLast() && !c.isAfterLast()) {
					c.moveToNext();
					transactions.add(new Transaction(c.getString(c.getColumnIndex("transdate")),
                                     c.getString(c.getColumnIndex("btransaction")),
                                     new BigDecimal(c.getString(c.getColumnIndex("amount"))),
                                     c.getString(c.getColumnIndex("currency"))));
				}
				c.close();
			}
			account.setTransactions(transactions);
		}
		if (loadPartners) {
			ArrayList<PartnersData> partners = new ArrayList<PartnersData>();
			//"transdate", "btransaction", "amount"}			
			c = db.fetchPartners(accountId);
			if (!(c == null || c.isClosed() || (c.isBeforeFirst() && c.isAfterLast()))) {
				while (!c.isLast() && !c.isAfterLast()) {
					c.moveToNext();
					partners.add(new PartnersData(
							c.getString(c.getColumnIndex("account_number")),
							c.getString(c.getColumnIndex("constant_symbol")),
							c.getString(c.getColumnIndex("bank_code")),
							c.getString(c.getColumnIndex("specificsymbol")),
							c.getString(c.getColumnIndex("name")),
							c.getString(c.getColumnIndex("amount_value")),
							c.getString(c.getColumnIndex("additional_data")),
							c.getString(c.getColumnIndex("variable_symbol")),
							c.getString(c.getColumnIndex("amount_currency_rowid"))
							));
				}
				c.close();
			}
			account.setPartners(partners);
		}
		
		
		db.close();
		return account;
	}
	
	public static ArrayList<Account> accountsFromDb(Context context, long bankId) {
		ArrayList<Account> accounts = new ArrayList<Account>();
		DBAdapter db = new DBAdapter(context);
		db.open();
		Cursor c = db.fetchAccounts(bankId);
		if (c == null) {
			db.close();
			return accounts;
		}
		while (!c.isLast() && !c.isAfterLast()) {
			c.moveToNext();
			try {
    			Account account = new Account(c.getString(c.getColumnIndex("name")),
                                              new BigDecimal(c.getString(c.getColumnIndex("balance"))),
                                              c.getString(c.getColumnIndex("id")).split("_", 2)[1],
                                              c.getLong(c.getColumnIndex("bankid")),
                                              c.getInt(c.getColumnIndex("acctype")));
    	        account.setHidden(c.getInt(c.getColumnIndex("hidden")) == 1 ? true : false);
    	        account.setNotify(c.getInt(c.getColumnIndex("notify")) == 1 ? true : false);			
    	        account.setCurrency(c.getString(c.getColumnIndex("currency")));
    			accounts.add(account);
			}
			catch (ArrayIndexOutOfBoundsException e) {
			    // Attempted to load an account without and ID, probably an old Avanza account.
			}
		}
		c.close();
		db.close();
		return accounts;
	}
	
}

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

package jjsan.eu.skbanking.db;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import jjsan.eu.skbanking.banking.Account;
import jjsan.eu.skbanking.banking.Bank;
import jjsan.eu.skbanking.banking.PartnersData;
import jjsan.eu.skbanking.banking.Transaction;


import net.sf.andhsli.hotspotlogin.SimpleCrypto;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


public class DBAdapter {

    static final String TAG = "DBAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    static final String DATABASE_NAME = "data";
    static final int DATABASE_VERSION = 9;

    private final Context mCtx;

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public DBAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public DBAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
        mDb.close();
    }


    public long createBank(Bank bank) {
    	return updateBank(bank);
    }

    
    /**
     * Delete the bank with the given bankId
     * 
     * @param bankId id of bank to delete
     */
    public int deleteBank(long bankId) {
        int c = mDb.delete("banks", "_id=" + bankId, null);
        c += this.deleteAccounts(bankId);
        return c;
    }
    
    /**
     * Delete the accounts for the given bankIdbank with the given rowId
     * 
     * @param bankId id of bank to delete
     */    
    public int deleteAccounts(long bankId) {
        int c = mDb.delete("accounts", "bankid=" + bankId, null);
        return c;
    }

    
    public int deleteTransactions(String account) {
        int c = mDb.delete("transactions", "account='" + account + "'", null);
        return c;
    }    

    public int deletePartners(String account) {
        int c = mDb.delete("partners", "account='" + account + "'", null);
        return c;
    }    
    
    
    /**
     * Return a Cursor over the list of all banks in the database
     * 
     * @return Cursor over all banks
     */
    public Cursor fetchBanks() {
        return mDb.query("banks", new String[] {"_id", "balance", "banktype", "username", "password", "vubpin","disabled", "custname", "updated", "sortorder", "currency"}, null, null, null, null, "_id asc");
    }

    
    /**
     * Return a Cursor over the list of all accounts belonging to a bank
     * 
     * @return Cursor over all accounts belonging to a bank
     */
    public Cursor fetchAccounts(long bankId) {
        return mDb.query("accounts", new String[] {"bankid", "balance", "name", "id", "acctype", "hidden", "notify", "currency"}, "bankid="+bankId, null, null, null, null);
    }
    
    public Cursor fetchTransactions(String account) {
        return mDb.query("transactions", new String[] {"transdate", "btransaction", "amount", "currency"}, "account='"+account+"'", null, null, null, null);
    }    

    public Cursor fetchPartners(String account) {
        return mDb.query("partners", new String[] {"account_number", "constant_symbol", "bank_code", "specificsymbol" , "name" , "amount_value", "additional_data", "variable_symbol", "amount_currency_rowid"}, "account='"+account+"'", null, null, null, null);
    }    

    public long updateBank(Bank bank) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
    	ContentValues initialValues = new ContentValues();
        initialValues.put("banktype", bank.getBanktypeId());
        initialValues.put("username", bank.getUsername());
        String password = "";
        String vubpin = "";
        try {
            password = SimpleCrypto.encrypt(Crypto.getKey(), bank.getPassword());
            vubpin = SimpleCrypto.encrypt(Crypto.getKey(), bank.getvubpin());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        initialValues.put("password", password);
        initialValues.put("vubpin", vubpin);
        initialValues.put("disabled", 0);
        initialValues.put("balance", bank.getBalance().toPlainString());
        initialValues.put("currency", bank.getCurrency());
        initialValues.put("custname", bank.getCustomName());
        initialValues.put("updated", sdf.format(cal.getTime()));
        
        long bankId = bank.getDbId();
        Log.d(TAG, "Updating bank, DbId: "+bankId);
        if (bankId == -1) {
        	bankId = mDb.insert("banks", null, initialValues);
        }
        else {
        	mDb.update("banks", initialValues, "_id="+bankId, null);
            deleteAccounts(bankId);
        }
        if (bankId != -1) {
	        ArrayList<Account> accounts = bank.getAccounts();
            Log.d(TAG, "Bank accounts: "+bank.getAccounts().size());
	        for(Account acc : accounts) {
	            ContentValues vals = new ContentValues();
	            vals.put("bankid", bankId);
	            vals.put("balance", acc.getBalance().toPlainString());
                vals.put("name", acc.getName());
	            vals.put("id", new Long(bankId).toString()+"_"+acc.getId());
                vals.put("hidden", acc.isHidden() ? 1 : 0);
                vals.put("notify", acc.isNotify() ? 1 : 0);
                vals.put("currency", acc.getCurrency());
                vals.put("acctype", acc.getType());
                Log.d(TAG, "Acctype: "+acc.getType());
	            mDb.insert("accounts", null, vals);
	            ArrayList<Transaction> transactions = acc.getTransactions();
	            if (transactions != null && !transactions.isEmpty()) {
	                deleteTransactions(new Long(bankId).toString()+"_"+acc.getId());
		            for(Transaction transaction : transactions) {
			            ContentValues transvals = new ContentValues();
			            transvals.put("transdate", transaction.getDate());
			            transvals.put("btransaction", transaction.getTransaction());
			            transvals.put("amount", transaction.getAmount().toPlainString());
			            transvals.put("account", new Long(bankId).toString()+"_"+acc.getId());
			            transvals.put("currency", transaction.getCurrency());
			            
			            mDb.insert("transactions", null, transvals);
		            }
	            }
	            ArrayList<PartnersData> partners = acc.getPartners();
	            if (partners != null && !partners.isEmpty()) {
	                deletePartners(new Long(bankId).toString()+"_"+acc.getId());
		            for(PartnersData partner : partners) {
			            ContentValues transvals = new ContentValues();
			            transvals.put("account_number", partner.getaccount_number());
			            transvals.put("constant_symbol", partner.getconstant_symbol());
			            transvals.put("bank_code", partner.getbank_code());
			            transvals.put("specificsymbol", partner.getspecificsymbol());
			            transvals.put("name", partner.getname());
			            transvals.put("amount_value", partner.getamount_value());
			            transvals.put("additional_data", partner.getadditional_data());
			            transvals.put("variable_symbol", partner.getvariable_symbol());
			            transvals.put("amount_currency_rowid", partner.getamount_currency_rowid());
			            transvals.put("account", new Long(bankId).toString()+"_"+acc.getId());
			            
			            mDb.insert("partners", null, transvals);
		            }
	            }	            
	        }
        }
        Log.d(TAG, "Updated bank: "+bankId);
        return bankId;
    }
    
	public void disableBank(long bankId) {
    	if (bankId == -1) return;
        ContentValues initialValues = new ContentValues();
        initialValues.put("disabled", 1);
    	mDb.update("banks", initialValues, "_id="+bankId, null);
    }
    
    public Cursor getBank(String bankId) {
    	Cursor c = mDb.query("banks", new String[] {"_id", "balance", "banktype", "username", "password", "vubpin", "disabled", "custname", "updated", "sortorder", "currency"}, "_id="+bankId, null, null, null, null);
    	if (c != null) {
    		c.moveToFirst();
    	}
		return c;
    }
    
    public Cursor getBank(long bankId) {
    	return getBank(new Long(bankId).toString());
    }

    public Cursor getAccount(String id) {
    	Cursor c = mDb.query("accounts", new String[] {"id", "balance", "name", "bankid", "acctype", "hidden", "notify", "currency"}, "id='"+id+"'", null, null, null, null);
    	if (c != null) {
    		c.moveToFirst();
    	}
		return c;
    }
    

}



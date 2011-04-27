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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 
 * @since 8 jan 2011
 */
final public class DatabaseHelper extends SQLiteOpenHelper {

	public DatabaseHelper(final Context context) {
		super(context, DBAdapter.DATABASE_NAME, null,
				DBAdapter.DATABASE_VERSION);
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		db.execSQL("create table banks (_id integer primary key autoincrement, "
				+ "balance text not null, "
				+ "banktype integer not null, "
				+ "username text not null, "
				+ "password text not null, "
				+ "vubpin text not null, "
				+ "custname text, "
				+ "updated text, "
				+ "sortorder real, "
				+ "currency text, " + "disabled integer);");
		db.execSQL("create table accounts (bankid integer not null, "
				+ "id text not null, " + "balance text not null, "
				+ "acctype integer not null, " + "hidden integer not null, "
				+ "notify integer not null, " + "currency text, "
				+ "name text not null);");
		db.execSQL("create table transactions " +
				"(_id integer primary key autoincrement, "
				+ "transdate text not null, "
				+ "btransaction text not null, "
				+ "amount text not null, "
				+ "currency text, "
				+ "account text not null);");
		db.execSQL("create table partners " +
				"(_id integer primary key autoincrement, "
				+ "account_number text not null, "
				+ "constant_symbol text not null, "
				+ "bank_code text not null, "
				+ "specificsymbol text, " 
				+ "name text not null, " 
				+ "amount_value text not null, "
				+ "additional_data text, "
				+ "variable_symbol text not null, "
				+ "amount_currency_rowid text not null, "
				+ "account text not null);");
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
			final int newVersion) {
		Log.w(DBAdapter.TAG, "Upgrading database from version " + oldVersion
				+ " to " + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS banks;");
		db.execSQL("DROP TABLE IF EXISTS accounts;");
		db.execSQL("DROP TABLE IF EXISTS transactions;");
		db.execSQL("DROP TABLE IF EXISTS partners;");
		onCreate(db);
	}
}
package jjsan.eu.skbanking;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jjsan.eu.skbanking.R;
import jjsan.eu.skbanking.banking.Account;
import jjsan.eu.skbanking.banking.Bank;
import jjsan.eu.skbanking.banking.BankFactory;
import jjsan.eu.skbanking.banking.PartnersData;
import jjsan.eu.skbanking.banking.Transaction;
import jjsan.eu.skbanking.banking.Bank.LoginPackage;
import jjsan.eu.skbanking.banking.banks.VubSK;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import eu.nullbyte.android.urllib.Urllib;

public class BankPaymentActivity extends LockableActivity{
	private final static String TAG = "PaymentActivity";
	Spinner spnPartners;
	private ArrayAdapter<PartnersData> arrayPartners;
	public ArrayList<PartnersData> tmpArray;
	EditText edName;
	EditText edAccountNumber;
	EditText edAmount;
	EditText edCurrency;
	EditText edInfo;
	
    protected Resources res;

	private Pattern reViewState = Pattern.compile("__VIEWSTATE\"\\s+value=\"([^\"]+)\"");
	private Pattern reToken = Pattern.compile("__TOKEN\"\\s+value=\"([^\"]+)\"");
	private Pattern rePartnerData = Pattern.compile("<span\\s*account_number=\"([0-9]+)\"\\s*constantsymbol_rowid=\"" +
			"([0-9]+)\"\\s*account_bankcode_rowid=\"([0-9]+)\"\\s*account_bankcode_code=\"([0-9]+)\"\\s*specificsymbol=\"" +
			"([0-9]+)?\"\\s*constantsymbol_symbol=\"([0-9]+)\"\\s*name=\"([A-Z0-9 ]+)\"\\s*amount_value=\"([0-9,. ]+)\"" +
			"\\s*additionaldata=\"([A-Z0-9 ]+)?\"\\s*rowid=\"([0-9]+)\"\\s*variablesymbol=\"([0-9]+)\"" +
			"\\s*amount_currency_rowid=\"([0-9]+)\"></span>"
			, Pattern.CASE_INSENSITIVE);
	
	
	public void setEditTexts(int pos)
	{
		edName.setText(tmpArray.get(pos).getname());
		edAccountNumber.setText(tmpArray.get(pos).getaccount_number());
		edAmount.setText(tmpArray.get(pos).getamount_value());
		edCurrency.setText(tmpArray.get(pos).getamount_currency_rowid());
		edInfo.setText(tmpArray.get(pos).getadditional_data());
	}
	
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Use HtcListView if available
	    setContentView(R.layout.payment);

	    Bundle extras = getIntent().getExtras();
		//need this
	    
		Bank bank = BankFactory.bankFromDb(extras.getLong("bank"), this, false);
		Log.d("========================================", bank.toString());
		Account account = BankFactory.accountFromDb(this, extras.getLong("bank")+"_"+extras.getString("account"), true, true);
		Log.d("========================================", account.toString());
		edName = (EditText) findViewById(R.id.edtPartnerName);
		edAccountNumber = (EditText) findViewById(R.id.edtPaymentAccount);
		edAmount = (EditText) findViewById(R.id.edtPaymentAmount);
		edCurrency = (EditText) findViewById(R.id.edtPaymentCurrency);
		
		ArrayList<PartnersData> partners = new ArrayList<PartnersData>();
		
		partners = account.getPartners();
		
		Log.d(TAG, "=====================Partners: "+partners.size());
		
		String array_spinner[] = {"None"};
		
		if (partners.size() > 0) {
			//findViewById(R.id.txtTranDesc).setVisibility(View.GONE);
			int i = 1;
			for (PartnersData partner : partners) {
				Log.d(TAG, "-=-=-=" + Integer.toString(i));
				array_spinner[i] =	replaceUnicode(partner.getname());
				i++;
				}
		}
		else
		{
			//array_spinner[1] = "String1";
			//array_spinner[2] = "String2";
		}
		Log.d(TAG, "-=-=-=");
		spnPartners = (Spinner) findViewById(R.id.spnPartner);
		Log.d(TAG, "-=-=-=");
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, array_spinner);
		Log.d(TAG, "-=-=-=");
		spnPartners.setAdapter(adapter);
		Log.d(TAG, "-=-=-=");
		spnPartners.setOnItemSelectedListener(new MyOnItemSelectedListener());
		Log.d(TAG, "Finished");
	}

	public class MyOnItemSelectedListener implements OnItemSelectedListener {

	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {
	        Toast.makeText(parent.getContext(), "The pm is " +  tmpArray.get(pos).getname(), Toast.LENGTH_LONG).show();
	        setEditTexts(pos);
	        
	    }

	    public void onNothingSelected(AdapterView parent) {
	      // Do nothing.
	    }
	}
	
	public String replaceUnicode(String string){
		
		string = string.replaceAll("&#250;", "ú");
		string = string.replaceAll("&#218;", "Ú");
		string = string.replaceAll("&#253;", "ý");
		string = string.replaceAll("&#221;", "Ý");
		string = string.replaceAll("&#243;", "ó");
		string = string.replaceAll("&#211;", "Ó");
		string = string.replaceAll("&#244;", "ô");
		string = string.replaceAll("&#212;", "Ô");
		string = string.replaceAll("&#353;", "š");
		string = string.replaceAll("&#352;", "Š");
		string = string.replaceAll("&#382;", "ž");
		string = string.replaceAll("&#381;", "Ž");
		string = string.replaceAll("&#225;", "á");
		string = string.replaceAll("&#193;", "Á");
		string = string.replaceAll("&#237;", "í");
		string = string.replaceAll("&#205;", "Í");
		string = string.replaceAll("&#233;", "é");
		string = string.replaceAll("&#201;", "É");
		string = string.replaceAll("&#196;", "ä");
		
		return string;

	}
}

/* Copyright (C) 2010 Nullbyte <http://nullbyte.eu>
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

package jjsan.eu.skbanking.banking.banks;

//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import android.os.Environment;
//import java.text.SimpleDateFormat;
//import java.util.Date;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import java.util.regex.PatternSyntaxException;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.net.ParseException;
import android.os.Environment;
import android.text.Html;
import android.text.InputType;
import android.util.Log;

import jjsan.eu.skbanking.Helpers;
import jjsan.eu.skbanking.R;
import jjsan.eu.skbanking.banking.Account;
import jjsan.eu.skbanking.banking.Bank;
import jjsan.eu.skbanking.banking.PartnersData;
import jjsan.eu.skbanking.banking.Transaction;
import jjsan.eu.skbanking.banking.exceptions.BankException;
import jjsan.eu.skbanking.banking.exceptions.LoginException;
import jjsan.eu.skbanking.provider.IBankTypes;

import eu.nullbyte.android.urllib.Urllib;



public class VubSK extends Bank {
	private static final String TAG = "VubSK";
	private static final String NAME = "VubSK";
	private static final String NAME_SHORT = "vubsk";
	private static final String URL = "https://ib.vub.sk/start.aspx";
	private static final int BANKTYPE_ID = IBankTypes.VUBSK;
    //private static final int INPUT_TYPE_USERNAME = InputType.TYPE_CLASS_TEXT | + InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;

    private Pattern reFormAction = Pattern.compile("<form.*?Form1.*?action=\"([^\"]+)\".*?>", Pattern.CASE_INSENSITIVE);
    private Pattern reViewState = Pattern.compile("__VIEWSTATE\"\\s+value=\"([^\"]+)\"");
    private Pattern reToken = Pattern.compile("__TOKEN\"\\s+value=\"([^\"]+)\"");
    private Pattern reMoney = Pattern.compile("<td\\s*width=\"120\"\\s*align=\"right\"><span\\s*class=\"black\">\\D?([A-Z0-9,.&#; ]+)</span></td>", Pattern.CASE_INSENSITIVE);
    private Pattern reAccName = Pattern.compile("<td\\s*width=\"90\">([0-9]+)\\s*</td>", Pattern.CASE_INSENSITIVE);
   
    private Pattern reTransactionDate = Pattern.compile("\\s*<td><span>([0-9][0-9].[0-9][0-9].[0-9][0-9][0-9][0-9])</span></td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private Pattern reTransactionAmount = Pattern.compile("<td\\s*style=\"text-align:right;\"><span\\s*class=\"([(A-Z]+)\">\\D?([A-Z0-9,.&#; ]+)</span></td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private Pattern reTransactionName = Pattern.compile("<span>[0-9]+</span></td>.*?<td><span>([-.+A-Za-z0-9&#;ľščťžýáíéúäňôďĺ, ]+)</span></td>.*?<td style=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	
	//pamyments
	private Pattern rePartnerData = Pattern.compile("<span\\s*account_number=\"([0-9]+)\"\\s*constantsymbol_rowid=\"" +
			"([0-9]+)\"\\s*account_bankcode_rowid=\"([0-9]+)\"\\s*account_bankcode_code=\"([0-9]+)\"\\s*specificsymbol=\"" +
			"([0-9]+)?\"\\s*constantsymbol_symbol=\"([0-9]+)\"\\s*name=\"([A-Z0-9 ]+)\"\\s*amount_value=\"([0-9,. ]+)\"" +
			"\\s*additionaldata=\"([A-Z0-9 ]+)?\"\\s*rowid=\"([0-9]+)\"\\s*variablesymbol=\"([0-9]+)\"" +
			"\\s*amount_currency_rowid=\"([0-9]+)\"></span>"
			, Pattern.CASE_INSENSITIVE);
	
	//private Pattern reBankCode = Pattern.compile("<option value=\"([0-9]+)\">([0-9]{4})\\s*-\\s*([-.+A-Za-z0-9&#;ľščťžýáíéúäňôďĺ, ]+)</option>", Pattern.CASE_INSENSITIVE);
	//private Pattern reCurrencies = Pattern.compile("<span\\s*rowid=\"([0-9]+)\"\\s*rowval=\"([A-Z]{3})\\s*-\\s*([-.+A-Za-z0-9&#;ľščťžýáíéúäňôďĺ, ]+)\"></span>",Pattern.CASE_INSENSITIVE);			
	//private Pattern reConstSymols = Pattern.compile("<option value=\"([0-9]+)\">([0-9]{4})\\s*-\\s*([-.+A-Za-z0-9&#;ľščťžýáíéúäňôďĺ, ]+)</option>", Pattern.CASE_INSENSITIVE);	
	
	private String response = null;

	public VubSK(Context context) {
		super(context);
		super.TAG = TAG;
		super.NAME = NAME;
		super.NAME_SHORT = NAME_SHORT;
		super.BANKTYPE_ID = BANKTYPE_ID;
		super.URL = URL;
		super.INPUT_TYPE_USERNAME = InputType.TYPE_CLASS_PHONE;
		//super.INPUT_TYPE_PASSWORD = InputType.TYPE_CLASS_PHONE;
		super.INPUT_TYPE_PIN = InputType.TYPE_CLASS_PHONE;
				
	}

	public VubSK(String username, String password, String vubpin, Context context) throws BankException, LoginException {
		this(context);
		this.update(username, password, vubpin, customName);
	}

	private void writeToFile(String filename, String whattowrite){
		try{

	        File file = new File(Environment.getExternalStorageDirectory(), "."+filename);
			file.createNewFile();

	        BufferedWriter writer = new BufferedWriter(new FileWriter(file,true));

	        writer.write(whattowrite);
	        writer.newLine();
	        writer.flush();
	        writer.close();
	        }

	        catch (IOException e) {
	        	Log.e("getText", e.getMessage());
	        }
	}
	
	
    @Override
    protected LoginPackage preLogin() throws BankException,
            ClientProtocolException, IOException {
        urlopen = new Urllib(true);
        response = urlopen.open("https://ib.vub.sk/");
        Matcher matcher = reViewState.matcher(response);
        if (!matcher.find()) {
            throw new BankException(res.getText(R.string.unable_to_find).toString()+" ViewState.");
        }
        String strViewState = matcher.group(1);
        
        List <NameValuePair> postData = new ArrayList <NameValuePair>();
        
        postData.add(new BasicNameValuePair("__LANGUAGE", "sk"));
        postData.add(new BasicNameValuePair("__TOKEN_EX", ""));
        postData.add(new BasicNameValuePair("__EVENTTARGET", ""));
        postData.add(new BasicNameValuePair(" __EVENTARGUMENT", ""));
        postData.add(new BasicNameValuePair("__VIEWSTATE", strViewState));
   
        postData.add(new BasicNameValuePair("DView:tblLogin:txtCustomerID", username));
        postData.add(new BasicNameValuePair("DView:tblLogin:txtPIN", vubpin));
        postData.add(new BasicNameValuePair("DView:tblLogin:txtPassword", password));
        postData.add(new BasicNameValuePair("DView:btnSubmit", 	"PotvrdiĹĄ"));
        //Log.d(TAG, "==========================="+username+ " " + password + " " + vubpin);
        return new LoginPackage(urlopen, postData, response, "https://ib.vub.sk/");
    }

    @Override
	public Urllib login() throws LoginException, BankException {
		try {
            LoginPackage lp = preLogin();
			response = urlopen.open(lp.getLoginTarget(), lp.getPostData());

			Matcher matcher = reFormAction.matcher(response);
	        String strPostUrl;
	        
	        if (matcher.find()) {
	            strPostUrl = Html.fromHtml(matcher.group(1)).toString();
	            Log.d(TAG, "Found post url: "+strPostUrl);
	        }
	        else {
	            throw new BankException(res.getText(R.string.unable_to_find).toString()+" post url.");
	        }
			
			matcher = reViewState.matcher(response);
	        if (!matcher.find()) {
	            throw new BankException(res.getText(R.string.unable_to_find).toString()+" ViewState.");
	        }
	        String strViewState = matcher.group(1);
	        
	        //writeToFile("first.txt", response);
	        
	        matcher = reToken.matcher(response);
	        if (!matcher.find()) {
	            throw new BankException(res.getText(R.string.unable_to_find).toString()+" Token.");
	        }
	        String token = matcher.group(1);
	        
	        List <NameValuePair> postData = new ArrayList <NameValuePair>();

	        postData.add(new BasicNameValuePair("__LANGUAGE", "sk"));
	        postData.add(new BasicNameValuePair("__TOKEN_EX", ""));
	        postData.add(new BasicNameValuePair("__TOKEN", token));
	        postData.add(new BasicNameValuePair("__VIEWSTATE", strViewState));        
	        postData.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
	        postData.add(new BasicNameValuePair("DView:hdnSetupCF",""));
	        postData.add(new BasicNameValuePair("viewAdvertising:hdnReportZoneClick",""));
	        postData.add(new BasicNameValuePair("__EVENTTARGET", 	"DView:lbtnNoInterest"));
	        
			LoginPackage lp2 = new LoginPackage(urlopen, postData, response, "https://ib.vub.sk/ib.aspx");
			
			response = urlopen.open(lp2.getLoginTarget(), lp2.getPostData());
	        
		}
		
		catch (ClientProtocolException e) {
			throw new BankException(e.getMessage());
		}
		catch (IOException e) {
			throw new BankException(e.getMessage());
		}
		return urlopen;
	}

	@Override
	public void update() throws BankException, LoginException {
		super.update();
		
		if (username == null || password == null || vubpin == null || username.length() == 0 
				|| password.length() == 0 || vubpin.length() == 0) {
			throw new LoginException(res.getText(R.string.invalid_username_password).toString());
		}
		
		urlopen = login();
		try {
			Matcher matcher;
			matcher = reAccName.matcher(response);

			if (matcher.find()) {
				String accname = matcher.group(1);
				
				matcher = reMoney.matcher(response);
				if (matcher.find()) {
					
					String tmp = matcher.group(1);
					tmp = tmp.replaceAll("&#160;"," ");
					//last three is currency
					String strAmount = tmp.substring(0, tmp.length()-4);
					strAmount = strAmount.replaceAll(" ", "");
					String strCurrency = tmp.substring(tmp.length()-3,tmp.length());
					
					Account account = new Account(accname , Helpers.parseBalance(strAmount), "1");
					account.setCurrency(strCurrency);
	    		    accounts.add(account);
				}
			}

			if (accounts.isEmpty()) {
				throw new BankException(res.getText(R.string.no_accounts_found).toString());
			}

		}		
        finally {
            super.updateComplete();
        }
	}
	
	@Override
	public void updateTransactions(Account account, Urllib urlopen) throws LoginException, BankException {
		super.updateTransactions(account, urlopen);
		
		ArrayList<Transaction> transactions = new ArrayList<Transaction>();
		
		String response = null;
		Matcher matcher, matcher2, matcherN;
		
		
			try {
				response = urlopen.open("https://ib.vub.sk/ib.aspx");
				matcher = reViewState.matcher(response);
		        if (!matcher.find()) {
		            throw new BankException(res.getText(R.string.unable_to_find).toString()+" ViewState.");
		        }
		        String strViewState = matcher.group(1);
		        matcher = reToken.matcher(response);
		        if (!matcher.find()) {
		            throw new BankException(res.getText(R.string.unable_to_find).toString()+" Token.");
		        }
		        String token = matcher.group(1);
		        
		        List <NameValuePair> postData = new ArrayList <NameValuePair>();

		        postData.add(new BasicNameValuePair("__LANGUAGE", "sk"));
		        postData.add(new BasicNameValuePair("__TOKEN_EX", ""));
		        postData.add(new BasicNameValuePair("__TOKEN", token));
		        postData.add(new BasicNameValuePair("__EVENTARGUMENT", "12"));
		        postData.add(new BasicNameValuePair("__VIEWSTATE", strViewState));        
		        postData.add(new BasicNameValuePair("DView_tblDDA_Table_row", "0"));
		        postData.add(new BasicNameValuePair("DView_tblTDA_Table_row", "-1"));
		        postData.add(new BasicNameValuePair("viewAdvertising:hdnReportZoneClick",""));
		        postData.add(new BasicNameValuePair("__EVENTTARGET", "viewMenu:IBMenuControl"));		        
		        
				LoginPackage lp2 = new LoginPackage(urlopen, postData, response, "https://ib.vub.sk/ib.aspx");
				
				response = urlopen.open(lp2.getLoginTarget(), lp2.getPostData());
				
				matcher  = reTransactionDate.matcher(response);
				matcherN = reTransactionName.matcher(response);
				matcher2 = reTransactionAmount.matcher(response);
				
				while (matcher.find()) {

				String strDate = matcher.group(1);
				
				String strTransaction = "+";
				String strAmount = null;
				String strCurrency = "EUR";
				
				if (matcher2.find()) {
					if (matcher2.group(1).equalsIgnoreCase("red"))
						strTransaction = "-";
					String tmp = matcher2.group(2);
					tmp = tmp.replaceAll("&#160;"," ");
					//last three is amount
					strAmount = tmp.substring(0, tmp.length()-4);
					strAmount = strAmount.replaceAll(" ", "");
					strCurrency = tmp.substring(tmp.length()-3,tmp.length());
					//strAmount = matcher2.group(2);
				}

				if (matcher.find()) {
				}

				if (matcher2.find()) {
				}
				
				DecimalFormat format = new DecimalFormat(strAmount);
				Double value = null;
				try {
					value = format.parse(strAmount).doubleValue();
				} catch (java.text.ParseException e1) {
					e1.printStackTrace();
				}
				BigDecimal decAmount = new BigDecimal(value);
				if (strTransaction.equalsIgnoreCase("-"))
				{
					BigDecimal negative = new BigDecimal(-1);
					decAmount = decAmount.multiply(negative);
				}
				strTransaction = "unknown";
				if (matcherN.find())
					strTransaction = matcherN.group(1);
				
	            try {
					transactions.add(new Transaction(strDate,
							replaceUnicode(strTransaction),
                    		decAmount, strCurrency));
                }
                catch (ParseException e) {
                    Log.d(TAG, "Unable to parse date: " + matcher.group(1).trim());
                }
				}
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			ArrayList<Transaction> tmpArray = new ArrayList<Transaction>();
			for (int i=1;i<=transactions.size();i++)
			{
				tmpArray.add(transactions.get(transactions.size()-i));
			}

			account.setTransactions(tmpArray);
			account.setPartners(getPartners(account, urlopen));
			
			}	

	public ArrayList<PartnersData> getPartners(Account account, Urllib urlopen) throws LoginException, BankException {

		//ArrayList<Transaction> PartnersData = new ArrayList<Transaction>();
		ArrayList<PartnersData> PartnerData = new ArrayList<PartnersData>();
		String response = null;
		Matcher matcher; //, matcher2, matcherN;
		
		Log.d(TAG, "==============PAYMENT");
		
			try {
				response = urlopen.open("https://ib.vub.sk/ib.aspx");
				matcher = reViewState.matcher(response);
				
				if (!matcher.find()) {
		            throw new BankException(res.getText(R.string.unable_to_find).toString()+" ViewState.");
		        }
		        String strViewState = matcher.group(1);
		        matcher = reToken.matcher(response);
		        if (!matcher.find()) {
		            throw new BankException(res.getText(R.string.unable_to_find).toString()+" Token.");
		        }
		        String token = matcher.group(1);
		        List <NameValuePair> postData = new ArrayList <NameValuePair>();
		        postData.add(new BasicNameValuePair("__LANGUAGE", "sk"));
		        postData.add(new BasicNameValuePair("__TOKEN_EX", ""));
		        postData.add(new BasicNameValuePair("__TOKEN", token));
		        postData.add(new BasicNameValuePair("__EVENTARGUMENT", "31"));
		        postData.add(new BasicNameValuePair("__VIEWSTATE", strViewState));        
		        postData.add(new BasicNameValuePair("DView_tblDDA_Table_row", "0"));
		        postData.add(new BasicNameValuePair("DView_tblTDA_Table_row", "-1"));
		        postData.add(new BasicNameValuePair("viewAdvertising:hdnReportZoneClick",""));
		        postData.add(new BasicNameValuePair("__EVENTTARGET", "viewMenu:IBMenuControl"));		        
		        
				LoginPackage lp2 = new LoginPackage(urlopen, postData, response, "https://ib.vub.sk/ib.aspx");
				response = urlopen.open(lp2.getLoginTarget(), lp2.getPostData());
				
				writeToFile("payment.txt", response);

				/*String account_number,String constant_symbol_rowid, String bank_code,
			String specificsymbol, String name,String amount_value, String additional_data,
			String variable_symbol,String amount_currency_rowid*/
				
				matcher = rePartnerData.matcher(response);
				while (matcher.find()) {
					PartnerData.add(new PartnersData(matcher.group(1),matcher.group(6),matcher.group(4),matcher.group(5),
							matcher.group(7),matcher.group(8),matcher.group(9),
							matcher.group(11),matcher.group(12)));
				}
				
				/*matcher = reBankCode.matcher(response);
				while (matcher.find()) {
				if (matcher.group(2).equalsIgnoreCase("0308")) break;
				Log.d(TAG,"Bank = "+matcher.group(2)+" - "+matcher.group(3));
				}

				matcher = reCurrencies.matcher(response);
				while (matcher.find()) {
				Log.d(TAG,"Currency = "+matcher.group(2)+" - "+matcher.group(3));
				}

				matcher = reConstSymols.matcher(response);
				Boolean show = false;
				while (matcher.find()) {
					if (matcher.group(2).equalsIgnoreCase("0308")) show = true;
				if (show) Log.d(TAG,"Konstantny = "+matcher.group(2)+" - "+matcher.group(3));
				}
				*/
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			return PartnerData;
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
			string = string.replaceAll("&#228;", "ä");
			
			return string;

		}
	
}
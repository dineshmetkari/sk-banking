package jjsan.eu.skbanking.banking.banks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.os.Environment;
import android.text.Html;
import android.text.InputType;
import android.util.Log;

import jjsan.eu.skbanking.Helpers;
import jjsan.eu.skbanking.R;
import jjsan.eu.skbanking.banking.Account;
import jjsan.eu.skbanking.banking.Bank;
import jjsan.eu.skbanking.banking.exceptions.BankException;
import jjsan.eu.skbanking.banking.exceptions.LoginException;
import jjsan.eu.skbanking.provider.IBankTypes;

import eu.nullbyte.android.urllib.Urllib;

public class O2sk extends Bank {

	private static final String TAG = "O2SK";
	private static final String NAME = "O2sk";
	private static final String NAME_SHORT = "o2sk";
	private static final String URL = "https://www.sk.o2.com/prihlasenie";
	private static final int BANKTYPE_ID = IBankTypes.O2KS;
    private static final int INPUT_TYPE_USERNAME = InputType.TYPE_CLASS_TEXT;
    private static final String INPUT_HINT_USERNAME = "";
    
    private Pattern reFormAction = Pattern.compile("<form\\s*name=\"actionLogin\"\\s*autocomplete=\"off\"\\s*action=\"([^\"]+)\"\\s*method=\"post\"\\s*formId=\"actionLogin\">", Pattern.CASE_INSENSITIVE);
    private Pattern reformTimeStamp = Pattern.compile("formTimestamp\"\\s+value=\"([^\"]+)\"");
    private Pattern reBalance = Pattern.compile("<b>\\s*([0-9,. ]+)\\s*&euro;</b>");
    private Pattern rePhoneNumber = Pattern.compile("slo:\\s*([0-9,. ]+)\\s*([0-9,. ]+)</legend>");
    private Pattern reDate = Pattern.compile("<p\\s*class=\"normal\"\\s*>\\s*Informácia\\s*je\\s*platná\\s*k\\s*([0-9,. ]+)\\s*</p>",Pattern.CASE_INSENSITIVE);
    
	private String response = null;
	
	public O2sk(Context context) {
		super(context);
		super.TAG = TAG;
		super.NAME = NAME;
		super.NAME_SHORT = NAME_SHORT;
		super.BANKTYPE_ID = BANKTYPE_ID;
		super.URL = URL;
        super.INPUT_TYPE_USERNAME = INPUT_TYPE_USERNAME;
        super.INPUT_HINT_USERNAME = INPUT_HINT_USERNAME;
        super.INPUT_HIDDEN_vubpin = true;
	}
	

	public O2sk(String username, String password, Context context) throws BankException, LoginException {
		this(context);
		this.update(username, password, "", customName);
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
	        //Get cookies and url to post to
	        response = urlopen.open("https://www.sk.o2.com/prihlasenie");
	        Matcher matcher = reFormAction.matcher(response);
	        String strPostUrl;
	        Log.d(TAG,"==== POST ====");
	        
	        if (matcher.find()) {
	            strPostUrl = Html.fromHtml(matcher.group(1)).toString();
	            Log.d(TAG, "Found post url: "+strPostUrl);
	        }
	        else {
	            throw new BankException(res.getText(R.string.unable_to_find).toString()+" post url.");
	        }
	        
	        matcher = reformTimeStamp.matcher(response);
	        if (!matcher.find()) {
	            throw new BankException(res.getText(R.string.unable_to_find).toString()+" formTimestamp.");
	        }
	        String strformTimestamp = matcher.group(1);
	        List <NameValuePair> postData = new ArrayList <NameValuePair>();
	        postData.add(new BasicNameValuePair("redirect", "/moje-o2/spotreba-a-balicky"));
	        postData.add(new BasicNameValuePair("login", username));
	        postData.add(new BasicNameValuePair("password", password));
	        postData.add(new BasicNameValuePair("formTimestamp", strformTimestamp));
	        
	        return new LoginPackage(urlopen, postData, response, "https://www.sk.o2.com/prihlasenie?p_p_id=login_WAR_liferayportlets&p_p_lifecycle=1&p_p_state=normal&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&_login_WAR_liferayportlets_action=actionLogin");
	    }

	   @Override
		public Urllib login() throws LoginException, BankException {

		    try {
		        LoginPackage lp = preLogin();
		        String response = urlopen.open(lp.getLoginTarget(), lp.getPostData());
				
		        if (response.contains("Vyskytla sa chyba")) {
					throw new LoginException(res.getText(R.string.invalid_username_password).toString());
				}
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
			if (username == null || password == null || username.length() == 0 || password.length() == 0) {
				throw new LoginException(res.getText(R.string.invalid_username_password).toString());
			}
			urlopen = login();
			String response = null;
			Matcher matcher;
			String strPhoneNumber = "";
			String strDate = "";
			
			try {
					response = urlopen.open("https://www.sk.o2.com/moje-o2/spotreba-a-balicky");
					writeToFile("o2.txt", response);
					//get balance for whole account
					matcher = reBalance.matcher(response);
		            if (matcher.find()) {
		                balance = Helpers.parseBalance(matcher.group(1));
		            }
		            
					matcher = rePhoneNumber.matcher(response);
		            if (matcher.find()) {
		                strPhoneNumber = (matcher.group(1))+" "+matcher.group(2).substring(0,3)+" "+matcher.group(2).substring(3,6) ;
		            }
		            
					matcher = reDate.matcher(response);
		            if (matcher.find()) {
		                strDate = matcher.group(1).replaceAll(" ","");
		            }
		            
		            Log.d(TAG, "----"+strPhoneNumber+" "+strDate+" "+balance.toString());
		            Account account = new Account(strPhoneNumber+" - "+strDate, balance, ""+"1");
		    		account.setCurrency("EUR");
	    		    accounts.add(account);
	    		     if (accounts.isEmpty()) 
	    		    	 	throw new BankException(res.getText(R.string.no_accounts_found).toString());
			}
			catch (IOException e) {
				throw new BankException(e.getMessage());
			}
			finally {
				super.updateComplete();
			}
		}

	   
}

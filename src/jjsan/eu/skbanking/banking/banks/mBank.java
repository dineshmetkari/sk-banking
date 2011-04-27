package jjsan.eu.skbanking.banking.banks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
//import jjsan.eu.skbanking.banking.Bank.LoginPackage;

import eu.nullbyte.android.urllib.Urllib;

public class mBank extends Bank {
	private static final String TAG = "mBank";
	private static final String NAME = "mBank";
	private static final String NAME_SHORT = "mbank";
	private static final String URL = "https://sk.mbank.eu/";
	private static final int BANKTYPE_ID = IBankTypes.MBANK;
    private static final int INPUT_TYPE_USERNAME = InputType.TYPE_CLASS_PHONE;
    private static final String INPUT_HINT_USERNAME = "";
    
    private Pattern reFormAction = Pattern.compile("<form.*?MainForm.*?action=\"([^\"]+)\".*?>", Pattern.CASE_INSENSITIVE);
    private Pattern reBalance = Pattern.compile("span\\s*class=\"text\\s*amount\\s*strong\">?[^0-9,.-]*([0-9,. ]+)([A-Z]+)\\s*?</span>", Pattern.CASE_INSENSITIVE);
    private Pattern reNameAccounts = Pattern.compile("href=\"#\">\\s*([A-Z]+\\s*[-]\\s*\\D+)", Pattern.CASE_INSENSITIVE);
    private Pattern reAccounts = Pattern.compile("AccountsGrid_grid_ctl[0-9][0-9]_MLabel_[0-9]_[0-9]\">([0-9,. ]+)\\s*([A-Z]+)\\s*</span>", Pattern.CASE_INSENSITIVE);
  
    private Pattern reLastTransactions = Pattern.compile("title=\"Zobraz\\s*posledné\\s*operácie\"\\s*onclick=\"doSubmit[('/]+account_oper_list.aspx[',POST]+'([^']+)'", Pattern.CASE_INSENSITIVE);
    private Pattern reTransactionName = Pattern.compile("</span><span>([^<]+)</span></p><p\\s*class=\"Amount\"><span\\s*id=\"account_operations_grid_ctl[0-9]+_MLabel[0-9_]+\"\\s*[class=\"negative\"]*?\\s*>([-0-9, ]+)\\s*EUR</span></p><p\\s*class=\"Amount\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    //private Pattern reTransactionAmount = Pattern.compile("<p\\s*class=\"Amount\"><span\\s*id=\"account_operations_grid_ctl[0-9]+_MLabel[0-9_]+\"\\s*[class=\"negative\"]*?\\s*>([-0-9, ]+)\\s*EUR</span></p><p\\s*class=\"Amount\">",Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private Pattern reTransactionDate = Pattern.compile("<p\\s*class=\"Date\"><span\\s*id=\"account_operations_grid_ctl[0-9]+_MLabel[0-9_]+\">([-0-9]+)</span>",Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    
    
 //   private Pattern reParamaters = Pattern.compile("__PARAMETERS\"\\s+value=\"([^\"]+)\"");
    private Pattern reState = Pattern.compile("__STATE\"\\s+value=\"([^\"]+)\"");
   // private Pattern reViewState = Pattern.compile("__VIEWSTATE\"\\s+value=\"([^\"]+)\"");
    private Pattern reEventValidation = Pattern.compile("__EVENTVALIDATION\"\\s+value=\"([^\"]+)\"");
    private Pattern reSeed = Pattern.compile("seed\"\\s+value=\"([^\"]+)\"");
    private Pattern rePartner = Pattern.compile("'/defined_transfers_list_czsk.aspx','','POST','([+A-Z0-9=/]+)'", Pattern.CASE_INSENSITIVE);
    private Pattern rePartnerName = Pattern.compile("href=\"#\">([-A-Z0-9 ]+)</a></p><p class=\"RecipientName\">",Pattern.CASE_INSENSITIVE  | Pattern.DOTALL);
    private String response = null;
	
	public mBank(Context context) {
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

	public mBank(String username, String password, Context context) throws BankException, LoginException {
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
	        response = urlopen.open("https://sk.mbank.eu/");
	        Matcher matcher = reFormAction.matcher(response);
	        String strPostUrl;
	        Log.d(TAG,"==== POST ====");
	        //writeToFile("first.txt", response);
	        
	        if (matcher.find()) {
	            strPostUrl = Html.fromHtml(matcher.group(1)).toString();
	            Log.d(TAG, "Found post url: "+strPostUrl);
	        }
	        else {
	            throw new BankException(res.getText(R.string.unable_to_find).toString()+" post url.");
	        }
	        
	        matcher = reState.matcher(response);
	        if (!matcher.find()) {
	            throw new BankException(res.getText(R.string.unable_to_find).toString()+" State.");
	        }
	        String strViewState = matcher.group(1);
	        
	        matcher = reSeed.matcher(response);
	        if (!matcher.find()) {
	            throw new BankException(res.getText(R.string.unable_to_find).toString()+" seed.");
	        }
	        String strSeed = matcher.group(1);

	        matcher = reEventValidation.matcher(response);
	        if (!matcher.find()) {
	            throw new BankException(res.getText(R.string.unable_to_find).toString()+" EventValidation.");
	        }
	        String strEventValidation = matcher.group(1);

	        List <NameValuePair> postData = new ArrayList <NameValuePair>();
	        
	        SimpleDateFormat formatter = new SimpleDateFormat("dd. M. yyyy hh:mm:ss");;
			String currentTime = formatter.format(new Date());
	        
	        postData.add(new BasicNameValuePair("seed", strSeed));
	        postData.add(new BasicNameValuePair("localDT", currentTime));
	        postData.add(new BasicNameValuePair("__PARAMETERS", ""));
	        postData.add(new BasicNameValuePair("__STATE", strViewState));
	        postData.add(new BasicNameValuePair("__VIEWSTATE", ""));
	        postData.add(new BasicNameValuePair("__EVENTVALIDATION", strEventValidation));
	        
	        postData.add(new BasicNameValuePair("customer",  username));
	        postData.add(new BasicNameValuePair("password", password));
	        
	        return new LoginPackage(urlopen, postData, response, "https://sk.mbank.eu/logon.aspx");
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
		
			try {
				response = urlopen.open("https://sk.mbank.eu/accounts_list.aspx");
				//writeToFile("accm.txt", response);
				//count accounts
				String temp = response;
				int numAccounts = 0;
				String tmpsubstr = "MLabel";
				while (temp.indexOf(tmpsubstr)!=-1)
				{
					temp = temp.substring(temp.indexOf(tmpsubstr)+tmpsubstr.length(), temp.length());
					numAccounts++;
				}
				
				//get names of accounts
				int i = 0;
				String[] Names = new String[numAccounts]; 
				matcher = reNameAccounts.matcher(response);
				while (matcher.find()){
					Names[i] = matcher.group(1);
					i++;
				}
				
				
				//get balance for whole account
				matcher = reBalance.matcher(response);
	            if (matcher.find()) {
	                balance = Helpers.parseBalance(matcher.group(1));
	                currency = matcher.group(2).trim();
	            }
	            
	    		//for (int i=1;i<=numAccounts;i++)
	    		//{
	        	matcher = reAccounts.matcher(response);
	        	int accId = 1;
	            
	            while (matcher.find()){
	    		Account account = new Account(Names[accId-1], Helpers.parseBalance(matcher.group(1).replace(",", ".")), ""+accId);
	    		account.setCurrency(matcher.group(2).trim());
    		    accounts.add(account);
    		    accId++;
    			//	}
	            }
	            
	            //ArrayList<PartnersData> tmp = getPartners();
	            
	            if (accounts.isEmpty()) {
				throw new BankException(res.getText(R.string.no_accounts_found).toString());
			}
		}
		catch (ClientProtocolException e) {
			throw new BankException(e.getMessage());
		}
		catch (IOException e) {
			throw new BankException(e.getMessage());
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
		Matcher matcher, matcherN;
		
					try {
					response = urlopen.open("https://sk.mbank.eu/accounts_list.aspx");
					
				   matcher = reState.matcher(response);
				        if (!matcher.find()) {
				            throw new BankException(res.getText(R.string.unable_to_find).toString()+" State.");
				        }
				        String strState = matcher.group(1);

					Log.d(TAG,"---Transactions---");
					matcher = reLastTransactions.matcher(response);
					//int i = 1;
					while (matcher.find()) {
					    	//we got and account
						Log.d("Transactions---",matcher.group(1));
						
				        List <NameValuePair> postData = new ArrayList <NameValuePair>();
						
				        postData.add(new BasicNameValuePair("__PARAMETERS", matcher.group(1)));
				        postData.add(new BasicNameValuePair("__STATE", strState));
				        postData.add(new BasicNameValuePair("__VIEWSTATE", ""));
						
				        LoginPackage lp2 = new LoginPackage(urlopen, postData, response, "https://sk.mbank.eu/account_oper_list.aspx");
						
						response = urlopen.open(lp2.getLoginTarget(), lp2.getPostData());

						matcher = reTransactionName.matcher(response);
						while(matcher.find())
						{
							Log.d("Trans: ", matcher.group(1)+" Amount: "+matcher.group(2));
							String strName = matcher.group(1);
												
						/*matcher2 = reTransactionAmount.matcher(response);
						while(matcher2.find())
						{
							Log.d("Amount:", matcher2.group(1));
						}
						 */
						String strAmount = matcher.group(2);

						DecimalFormat format = new DecimalFormat(strAmount);
						Double value = null;
						Log.d("Trans: ", "DECIMAL");
						try {
							value = format.parse(strAmount).doubleValue();
						} catch (java.text.ParseException e1) {
							e1.printStackTrace();
						}
						Log.d("Trans: ", "DECIMAL");
						BigDecimal decAmount = new BigDecimal(value);
						if (strAmount.startsWith("-"))
						{
							BigDecimal negative = new BigDecimal(-1);
							decAmount = decAmount.multiply(negative);
						}
						Log.d("Trans: ", "DECIMAL");
						matcherN = reTransactionDate.matcher(response);
						String strDate = "01.01.2011";
						if (matcherN.find()) strDate = matcherN.group(1); 
						
						//writeToFile("llist"+Integer.toString(i)+".txt", response);
						try {
							//Log.d("Trans: ", strDate);
							Log.d("Trans: ", strName);
							Log.d("Trans: ", decAmount.toString());
							transactions.add(new Transaction(strDate.replaceAll("-", "."),
									strName, decAmount, "EUR" ));
					        }
						catch (ParseException e) {
		                    Log.d(TAG, "Unable to parse date: " + matcher.group(1).trim());
		                }
						Log.d("Trans: ", "DECIMAL");
						
						}
						Log.d("Trans: ", "DECIMAL");
					} 
					}
					catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					account.setTransactions(transactions);
					//account.setPartners(getPartners(account, urlopen));
					
}
	
	public ArrayList<PartnersData> getPartners() throws LoginException, BankException {

		super.update();
		if (username == null || password == null || username.length() == 0 || password.length() == 0) {
			throw new LoginException(res.getText(R.string.invalid_username_password).toString());
		}
		
		urlopen = login();
		String response = null;
		Matcher matcher;
		
		try {
			response = urlopen.open("https://sk.mbank.eu/accounts_list.aspx");
			writeToFile("mbank2", response);
			matcher = rePartner.matcher(response);
			while (matcher.find()) {
			Log.d(TAG,"Partners ------ matched");
			Log.d(TAG,matcher.group(1));
			String strParamaters = matcher.group(1);
					//matcher.group(1)+" "+matcher.group(2)+" "+matcher.group(3)+" "+matcher.group(4)+" "+matcher.group(5));
			
			
			matcher = reState.matcher(response);
	        if (!matcher.find()) {
	            throw new BankException(res.getText(R.string.unable_to_find).toString()+" State.");
	        }
	        String strState = matcher.group(1);
	        
	        List <NameValuePair> postData = new ArrayList <NameValuePair>();
	        
	       // SimpleDateFormat formatter = new SimpleDateFormat("dd. M. yyyy hh:mm:ss");;
			//String currentTime = formatter.format(new Date());
			
			//function doSubmit(addr, target, method, parameters, causesvalidation, resetform, isAction, funct2call)
			///defined_transfers_list_czsk.aspx','','POST','iAUB8kiobjxh5UtlzF7QgrSQoe6qBtR3BIuJJV68Cd4RKDFoUn/QwrwumZ+zoH3ONmmqSH/rmkw8XAC47POH0II1fbnj/I+P9YgKVTXvxmsRzUZd/U+USMD8fS0uk3E5JIrR/TOOUK+Dw1tUfJFPzKC7v/X3mRqJuhmkOxwQhLYecI+CkPw62glY95JZ9YyQDUJwO3DRSzOHzaFFDk5mTp+Cgab5+yhi9jrP91+yglEVV/tegAD2IjoCFJDLh8tM1fHINARCKCoK2iQmajk2bg==',false,false,false,null
			postData.add(new BasicNameValuePair("__PARAMETERS", strParamaters));
	        postData.add(new BasicNameValuePair("__STATE", strState));
	        postData.add(new BasicNameValuePair("__VIEWSTATE", ""));
	        
	        LoginPackage lp2 = new LoginPackage(urlopen, postData, response, "https://sk.mbank.eu/defined_transfers_list_czsk.aspx");
			response = urlopen.open(lp2.getLoginTarget(), lp2.getPostData());
			writeToFile("000ttest.txt", response);
			matcher = rePartnerName.matcher(response);
	        while (matcher.find()) {
	            Log.d("PARTNER: ",matcher.group(1));

	            /*public PartnersData(String account_number,String constant_symbol, String bank_code,
	        			String specificsymbol, String name,String amount_value, String additional_data,
	        			String variable_symbol,String amount_currency_rowid)*/
	            partners.add((new PartnersData("","","","",matcher.group(1),"","","","")));
	        }
	        
			//writeToFile("mbank2.txt", response);
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
        return partners;
	}

}
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;

import jjsan.eu.skbanking.Helpers;
import jjsan.eu.skbanking.R;
import jjsan.eu.skbanking.banking.exceptions.BankException;
import jjsan.eu.skbanking.banking.exceptions.LoginException;
import jjsan.eu.skbanking.db.DBAdapter;
import jjsan.eu.skbanking.provider.IBankTypes;
import jjsan.eu.skbanking.provider.IPartnersData;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.text.InputType;
import android.util.Log;

import eu.nullbyte.android.urllib.Urllib;

public abstract class Bank implements Comparable<Bank>, IBankTypes, IPartnersData {
    protected String TAG = "Bank";
	protected String NAME = "Bank";
	protected String NAME_SHORT = "bank";
	protected int BANKTYPE_ID = 0;
	protected String URL;
    protected int INPUT_TYPE_USERNAME = InputType.TYPE_CLASS_TEXT;
    protected int INPUT_TYPE_PIN = InputType.TYPE_CLASS_PHONE;
    protected int INPUT_TYPE_PASSWORD = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    protected String INPUT_HINT_USERNAME = null;
    protected boolean INPUT_HIDDEN_USERNAME = false;
    protected boolean INPUT_HIDDEN_PASSWORD = false;
    protected boolean INPUT_HIDDEN_vubpin = false;
    protected int INPUT_TITLETEXT_USERNAME = R.string.username;
    protected int INPUT_TITLETEXT_PASSWORD = R.string.password;
    protected int INPUT_TITLETEXT_vubpin = R.string.vubpin;
    protected boolean STATIC_BALANCE = false;
    protected boolean BROKEN = false;

    protected Context context;
    protected Resources res;

    protected String username;
    protected String password;
    protected String vubpin;

    protected ArrayList<Account> accounts = new ArrayList<Account>();
    protected ArrayList<PartnersData> partners = new ArrayList<PartnersData>();
    
    protected HashMap<String, Account> oldAccounts;
    protected BigDecimal balance = new BigDecimal(0);
    protected boolean disabled = false;
    protected long dbid = -1;
    protected Urllib urlopen = null;
    protected String customName;
    protected String currency = "EUR";

    public Urllib getUrlopen() {
        return urlopen;
    }

    public void setUrlopen(Urllib urlopen) {
        this.urlopen = urlopen;
    }

    public void setDbid(long dbid) {
        this.dbid = dbid;
    }

    public Bank(Context context) {
        this.context = context;
        this.res = this.context.getResources();
    }

    public void update(String username, String password, String vubpin, String customName) throws BankException, LoginException {
        this.username = username;
        this.password = password;
        this.vubpin = vubpin;
        this.customName = customName;
        this.update();
    }

    public void update() throws BankException, LoginException {
        balance = new BigDecimal(0);
        oldAccounts = new HashMap<String, Account>();
        for(Account account: accounts) {
            oldAccounts.put(account.getId(), account);
        }
        accounts = new ArrayList<Account>();
    }

    public void updateTransactions(Account account, Urllib urlopen) throws LoginException, BankException {
    }

    public void updateAllTransactions() throws LoginException, BankException {
        if (urlopen == null) {
            urlopen = login();
        }
        for (Account account: accounts) {
            updateTransactions(account, urlopen);
        }
        if (urlopen != null) {
            urlopen.close();
        }

    }
    
    public void updatePartners(Account account, Urllib urlopen) throws LoginException, BankException {
    }

    public void updateAllPartners() throws LoginException, BankException {
        if (urlopen == null) {
            urlopen = login();
        }
        for (Account account: accounts) {
            updatePartners(account, urlopen);
        }
        if (urlopen != null) {
            urlopen.close();
        }

    }

    public Urllib login() throws LoginException, BankException {
        return null;
    }

    public void closeConnection() {
        if (urlopen != null) {
            urlopen.close();
        }
    }

    public ArrayList<Account> getAccounts() {
        return this.accounts;
    }

    public void setAccounts(ArrayList<Account> accounts) {
        this.accounts = accounts;
        for (Account a : accounts) {
            a.setBank(this);
        }
    }

    public String getPassword() {
        return password;
    }

    public String getvubpin() {
        return vubpin;
    }
    
    public String getUsername() {
        return username;
    }

    public String getCustomname() {
        return customName;
    }

    public BigDecimal getBalance() {
        if (STATIC_BALANCE) {
            return balance;
        }
        else {
            BigDecimal bal = new BigDecimal(0); 
            for (Account account : accounts) {
                if (account.getType() == Account.REGULAR || account.getType() == Account.CCARD) {
                    if (!account.isHidden()) {
                        bal = bal.add(account.getBalance());
                    }
                }
            }
            return bal;
        }
    }

    public int getBanktypeId() {
        return BANKTYPE_ID;
    }

    public String getName() {
        return NAME;
    }

    public String getDisplayName() {
        if (customName != null && customName.length() > 0) return customName;
        return username;
    }


    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getShortName() {
        return NAME_SHORT;
    }

    public void setData(String username, String password, String vubpin, BigDecimal balance,
            boolean disabled, long dbid, String currency, String customName) {
        this.username = username;
        this.password = password;
        this.vubpin = vubpin;
        this.balance = balance;
        this.disabled = disabled;
        this.dbid = dbid;
        this.currency = currency;
        this.customName = customName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getDbId() {
        return dbid;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void disable() {
        DBAdapter db = new DBAdapter(context);
        db.open();
        db.disableBank(dbid);
        db.close();
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }


    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setvubpin(String vubpin) {
        this.vubpin = vubpin;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setCustomname(String customname) {
        this.customName = customname;
    }
    
    public void save() {
        DBAdapter db = new DBAdapter(context);
        db.open();
        dbid = db.updateBank(this); // Update ID on insert as well.
        db.close();
    }

    public String getURL() {
        return URL;
    }

    public int getInputTypeUsername() {
        return INPUT_TYPE_USERNAME;
    }

    public int getInputTypePassword() {
        return INPUT_TYPE_PASSWORD;
    }

    public String getInputHintUsername() {
        return INPUT_HINT_USERNAME;
    }

    public boolean isInputUsernameHidden() {
        return INPUT_HIDDEN_USERNAME;
    }

    public boolean isInputPasswordHidden() {
        return INPUT_HIDDEN_PASSWORD;
    }
    
    public boolean isInputvubpinHidden() {
        return INPUT_HIDDEN_vubpin;
    }

    public int getInputTitleUsername() {
        return INPUT_TITLETEXT_USERNAME;
    }

    public int getInputTitlePassword() {
        return INPUT_TITLETEXT_PASSWORD;
    }
    
    public int getInputTitlevubpin() {
        return INPUT_TITLETEXT_vubpin;
    }

    // Returns true if the current implementation of this bank is broken.
    public boolean isBroken() {
        return BROKEN;
    }

    public int getImageResource() {
        return res.getIdentifier("logo_"+NAME_SHORT, "drawable", context.getPackageName());	
    }

    public int compareTo(Bank another) {
        return this.toString().compareToIgnoreCase(another.toString());
    }

    public void updateComplete() {
        for (Account a : this.accounts) {
            //Preserve hidden and notify settings from old accounts
            if (oldAccounts != null) {
                Account oa = oldAccounts.get(a.getId());
                if (oa != null) {
                    a.setHidden(oa.isHidden());
                    a.setNotify(oa.isNotify());
                    a.setCurrency(oa.getCurrency());
                }
            }
            a.setBank(this);
        }
    }

    public SessionPackage getSessionPackage(Context context) {
        String preloader = "Error...";
        try { 
            preloader = IOUtils.toString(context.getResources().openRawResource(R.raw.loading));
        }
        catch (NotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            LoginPackage lp = preLogin();
            if (lp == null) {
                throw new BankException("No automatic login for this bank. preLogin() is not implemented or has failed.");
            }
            String html = String.format(preloader,
                    "function go(){document.getElementById('submitform').submit(); }", // Javascript function
                    Helpers.renderForm(lp.getLoginTarget(), lp.getPostData())+"<script type=\"text/javascript\">setTimeout('go()', 1000);</script>" // HTML
            );        

            CookieStore cookies = urlopen.getHttpclient().getCookieStore();
            return new SessionPackage(html, cookies);
        }
        catch (ClientProtocolException e) {
            Log.d(TAG, e.getMessage());
        }
        catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
        catch (BankException e) {
            Log.d(TAG, e.getMessage());
        }
        String html = String.format(preloader,
                String.format("function go(){window.location=\"%s\" }", this.URL), // Javascript function
                "<script type=\"text/javascript\">setTimeout('go()', 1000);</script>" // HTML
        );          
        return new SessionPackage(html, null);
    }

    protected LoginPackage preLogin() throws BankException, ClientProtocolException, IOException {
        return null;
    }

    public static class SessionPackage {
        private String html;
        private CookieStore cookiestore;
        public SessionPackage(String html, CookieStore cookiestore) {
            this.html = html;
            this.cookiestore = cookiestore;
        }
        public String getHtml() {
            return html;
        }
        public CookieStore getCookiestore() {
            return cookiestore;
        }
    }    

    public static class LoginPackage {
        private String response;
        private Urllib urllib;
        private List<NameValuePair> postData;
        private String loginTarget;
        public LoginPackage(Urllib urllib, List<NameValuePair> postData, String response, String loginTarget) {
            this.urllib = urllib;
            this.postData = postData;
            this.response = response;
            this.loginTarget = loginTarget;
        }
        public String getResponse() {
            return response;
        }
        public Urllib getUrllib() {
            return urllib;
        }
        public List<NameValuePair> getPostData() {
            return postData;
        }
        public String getLoginTarget() {
            return loginTarget;
        }

        
    }    

}
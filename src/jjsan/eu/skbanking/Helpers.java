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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import jjsan.eu.skbanking.R;

import org.apache.http.NameValuePair;

import android.app.Activity;
import android.util.Log;

public class Helpers {
    private final static String[] currencies = {"AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD",
        "AWG", "AZN", "BAM", "BBD", "BDT", "BGN", "BHD", "BIF",
        "BMD", "BND", "BOB", "BRL", "BSD", "BTN", "BWP", "BYR",
        "BZD", "CAD", "CDF", "CHF", "CLP", "CNY", "COP", "CRC",
        "CUP", "CVE", "CYP", "CZK", "DJF", "DKK", "DOP", "DZD",
        "EEK", "EGP", "ERN", "ETB", "EUR", "FJD", "FKP", "GBP",
        "GEL", "GGP", "GHS", "GIP", "GMD", "GNF", "GTQ", "GYD",
        "HKD", "HNL", "HRK", "HTG", "HUF", "IDR", "ILS", "IMP",
        "INR", "IQD", "IRR", "ISK", "JEP", "JMD", "JOD", "JPY",
        "KES", "KGS", "KHR", "KMF", "KPW", "KRW", "KWD", "KYD",
        "KZT", "LAK", "LBP", "LKR", "LRD", "LSL", "LTL", "LVL",
        "LYD", "MAD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP",
        "MRO", "MTL", "MUR", "MVR", "MWK", "MXN", "MYR", "MZN",
        "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB",
        "PEN", "PGK", "PHP", "PKR", "PLN", "PYG", "QAR", "RON",
        "RSD", "RUB", "RWF", "SAR", "SBD", "SCR", "SDG", "SEK",
        "SGD", "SHP", "SLL", "SOS", "SPL", "SRD", "STD", "SVC",
        "SYP", "SZL", "THB", "TJS", "TMM", "TND", "TOP", "TRY",
        "TTD", "TVD", "TWD", "TZS", "UAH", "UGX", "USD", "UYU",
        "UZS", "VEB", "VEF", "VND", "VUV", "WST", "XAF", "XAG",
        "XAU", "XCD", "XDR", "XOF", "XPD", "XPF", "XPT", "YER",
        "ZAR", "ZMK", "ZWD"};

    private final static String[][] symMappings = {{"$U", "UYU"}, {"$b", "BOB"}, {"BZ$", "BZD"},
        {"C$", "NIO"}, {"J$", "JMD"}, {"NT$", "TWD"},
        {"R$", "BRL"}, {"RD$", "DOP"}, {"TT$", "TTD"},
        {"Z$", "ZWD"}, {"$", "USD"}, {"B/.", "PAB"},
        {"Bs", "VEF"}, {"Ft", "HUF"}, {"Gs", "PYG"},
        {"KM", "BAM"}, {"Kč", "CZK"}, {"Lek", "ALL"},
        {"S/.", "PEN"}, {"Ls", "LVL"}, {"Lt", "LTL"},
        {"MT", "MZN"}, {"Php", "PHP"}, {"RM", "MYR"},
        {"Rp", "IDR"}, {"TL", "TRY"}, {"kn", "HRK"},
        {"kr", "SEK"}, {"lei", "RON"}, {"p.", "BYR"},
        {"L", "HNL"}, {"S", "SOS"}, {"P", "BWP"},
        {"Q", "GTQ"}, {"R", "ZAR"}, {"zł", "PLN"},
        {"¢", "GHC"}, {"£", "GBP"}, {"¥", "JPY"},
        {"ƒ", "ANG"}, {"Дин.", "RSD"}, {"ден", "MKD"},
        {"лв", "BGN"}, {"ман", "AZN"}, {"руб", "RUB"},
        {"؋", "AFN"}, {"฿", "THB"}, {"៛", "KHR"},
        {"₡", "CRC"}, {"₤", "TRL"}, {"₦", "NGN"},
        {"₨", "PKR"}, {"₩", "KRW"}, {"₪", "ILS"},
        {"₫", "VND"}, {"€", "EUR"}, {"₭", "LAK"},
        {"₮", "MNT"}, {"₱", "CUP"}, {"₴", "UAH"},
        {"﷼", "SAR"}, 
    }; 

    public static BigDecimal parseBalance(String balance) {
        balance = balance.replaceAll("[^0-9,.-]*", "");
        balance = balance.replace(",", ".");
        if (balance.indexOf(".") != balance.lastIndexOf(".")) {
            String b = balance.substring(balance.lastIndexOf("."));
            balance = balance.substring(0, balance.lastIndexOf("."));
            balance = balance.replace(".", "");
            balance = balance+b;
        }
        BigDecimal ret;
        try {
            ret = new BigDecimal(balance);
        }
        catch (NumberFormatException e) {
            Log.d("parseBalance", "Unable to parse: "+balance);
            ret = new BigDecimal(0);
        }
       
        return ret;
    }
    public static String formatBalance(BigDecimal balance, String curr, boolean round) {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator(',');
        dfs.setGroupingSeparator(' ');
        DecimalFormat currency;
        if (!round) {
            currency = new DecimalFormat("#,##0.00 ");
        }
        else {
            currency = new DecimalFormat("#,##0 ");  
        }
        currency.setDecimalFormatSymbols(dfs);
        return currency.format(balance.doubleValue())+curr;
    }
    public static String formatBalance(BigDecimal balance, String curr) {
        return formatBalance(balance, curr, false);
    }
    public static String formatBalance(Double balance, String curr) {
        return formatBalance(new BigDecimal(balance), curr);
    }

    public static void slowDebug(String TAG, String text) {
        slowDebug(TAG, text, 100);
    }
    public static void slowDebug(String TAG, String text, int sleep) {
        for (String s : text.split("\n")) {
            Log.d(TAG, s);
            try {
                Thread.sleep(sleep);
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    static public void setActivityAnimation(Activity activity, int in, int out) {
        try {
            Method method = Activity.class.getMethod("overridePendingTransition", new Class[]{int.class, int.class});
            method.invoke(activity, in, out);
        } catch (Exception e) {
            // Can't change animation, so do nothing
        }
    }

    public static String parseCurrency(String text, String def) {
        for (String currency : currencies) {
            if (text.contains(currency)) return currency;
        }
        for (String[] symCur : symMappings) {
            if (text.contains(symCur[0])) return symCur[1];
        }
        return def;
    }
    
    public static String renderForm(String action, List <NameValuePair> postData) {
        StringBuilder form = new StringBuilder();
        form.append("<form id=\"submitform\" method=\"POST\" action=\"")
        .append(action)
        .append("\">");
        for (NameValuePair p : postData) {
            form.append("<input type=\"hidden\" name=\"")
            .append(p.getName())
            .append("\" value=\"")
            .append(p.getValue())
            .append("\" />");
        }
        form.append("</form>");
        return form.toString();
        
    }
    

    /**
     * Determines what year a transaction belongs to.
     * 
     * If the given <code>day</code> of the given <code>month</code> for the current year
     * is in the future the transaction is probably from last year.
     * 
     * @param month     The month, where January is 1.
     * @param day       The day of the month, starting from 1.
     * @return          An ISO 8601 formatted date.
     */
    public static String getTransactionDate(String month, String day) {
        return getTransactionDate(Integer.parseInt(month), Integer.parseInt(day));
    }

    /**
     * Determines what year a transaction belongs to.
     * 
     * If the given <code>day</code> of the given <code>month</code> for the current year
     * is in the future the transaction is probably from last year.
     * 
     * @param month     The month, where January is 1.
     * @param day       The day of the month, starting from 1.
     * @return          An ISO 8601 formatted date.
     */
    public static String getTransactionDate(int month, int day) {
        month--; // Java-months start at 0
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance(); 
        int currentYear = cal.get(Calendar.YEAR);
        cal.set(currentYear, month, day, 0, 0);
        if (cal.getTime().after(Calendar.getInstance().getTime())) {
            //If the transaction is in the future the year is probably of by +1.
            cal.add(Calendar.YEAR, -1);
        }
        return sdf.format(cal.getTime());
    }

}

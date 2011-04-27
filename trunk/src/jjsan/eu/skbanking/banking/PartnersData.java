package jjsan.eu.skbanking.banking;

import java.util.ArrayList;

import jjsan.eu.skbanking.provider.IPartnersData;


public class PartnersData implements Comparable<PartnersData>, IPartnersData  {
	String account_number,
		   payment_constant_symbol,
		   payment_rowid1,
		   account_bankcode_rowid,
		   bank_code,
		   specificsymbol,
		   constant_symbol,
		   name,
		   amount_value,
		   additional_data,
		   payment_rowid2,
		   variable_symbol,
		   amount_currency_rowid;
	
	private ArrayList<PartnersData> partners;

	public PartnersData(String account_number,String constant_symbol, String bank_code,
			String specificsymbol, String name,String amount_value, String additional_data,
			String variable_symbol,String amount_currency_rowid)
	{
		
		this.account_number = account_number; 
		this.constant_symbol = constant_symbol; 
		this.bank_code = bank_code;
		this.specificsymbol = specificsymbol;
		this.name = name;
		this.amount_value = amount_value;
		this.additional_data = additional_data;
		this.variable_symbol = variable_symbol;
		this.amount_currency_rowid = amount_currency_rowid;
		
	}

	/* name */
	public String getname()
	{
		return this.name;
	}

	public void setname(String name)
	{
		this.name = name;
	}
	/* account number */
	public String getaccount_number()
	{
		return this.account_number;
	}

	public void setaccount_number(String account_number)
	{
		this.account_number = account_number;
	}
	/* amount currency */
	public String getamount_currency_rowid()
	{
		return this.amount_currency_rowid;
	}
	
	public void setamount_currency_rowid(String amount_currency_rowid)
	{
		this.amount_currency_rowid = amount_currency_rowid;
	}
	/* bank code */
	public String getbank_code() {
		return this.bank_code;
	}

	public void setbank_code(String bank_code) {
		this.bank_code = bank_code;
	}
	/* constant_sysbol_rowid */
	public String getconstant_symbol() {
		return this.constant_symbol;
	}

	public void setconstant_sysbol_rowid(String constant_symbol) {
		this.constant_symbol = constant_symbol;
	}
	/* specificsymbol */
	public String getspecificsymbol() {
		return this.specificsymbol;
	}

	public void setspecificsymbol(String specificsymbol) {
		this.specificsymbol = specificsymbol;
	}
	/* additional data */
	public String getadditional_data() {
		return this.additional_data;
	}
	
	public void setadditional_data(String additional_data) {
		this.additional_data = additional_data;
	}
	/* amount value */
	public String getamount_value() {
		return this.amount_value;
	}
	
	public void setamount_value(String amount_value) {
		this.amount_value = amount_value;
	}
	/* variable symbol */
	public String getvariable_symbol() {
		return this.variable_symbol;
	}

	public void setvariable_symbol(String variable_symbol) {
		this.variable_symbol = variable_symbol;
	}
	
	@Override
	public int compareTo(PartnersData another) {
		return 0;
	}
	
	public ArrayList<PartnersData> getPartners() {
		return partners;
	}
	
	
}

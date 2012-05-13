/**
 * 
 */
package com.bityarn.mmreceipt;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Muniu Kariuki - muniu@bityarn.co.ke
 * 
 */
public class MPESAMesageParser {

	private String message = null;

	protected static final String AMOUNT_PATTERN = "Ksh[,|.|\\d]+";
	protected static final String AMOUNT_PATTERN_WITHOUT_DOT = "Ksh[,\\d]+";
	protected static final String SENT_TO = " sent to";
	protected static final String DATETIME_PATTERN = "on d/M/yy at hh:mm a";
	protected static final String CONFIRMATION_CODE_PATTERN = "[A-Z0-9]+ Confirmed.";
	protected static final String PAID_BY_PATTERN = "([A-Za-z ]+)";
	protected static final String SENT_ACCOUNT_NAME = "sent to ([A-Za-z ]+)";
	protected static final String RECEIVED_FROM = "received";

	/**
	 * 
	 */
	public MPESAMesageParser(String message) {
		this.message = message;
	}

	/**
	 * Utility function
	 * 
	 * @param textString
	 * @param searchPattern
	 * @return
	 */
	private String getFirstMatch(String textString, String searchPattern) {
		try {
			Pattern pattern = Pattern.compile(searchPattern);
			Matcher matcher = pattern.matcher(textString);
			matcher.find();
			return matcher.group(0);
		} catch (Exception e) {
			return "0";
		}
	}

	/**
	 * MPESA Transaction Date
	 * 
	 * @param message
	 * @return
	 */
	public String getTimestamp() {
		final String firstMatch = getFirstMatch(message, DATETIME_PATTERN);
		return firstMatch;
	}

	/**
	 * MPESA Transaction/Confirmation Code
	 * 
	 * @param message
	 * @return
	 */
	public String getConfirmationCode() {
		final String firstMatch = getFirstMatch(message,
				CONFIRMATION_CODE_PATTERN);
		return firstMatch.replace(" Confirmed.", "").trim();
	}

	/**
	 * MPESA Amount
	 * 
	 * @param message
	 * @return
	 */
	public BigDecimal getAmount() {
		final String amountWithKsh = getFirstMatch(message, AMOUNT_PATTERN);
		String amountWithKshMinusZero = getFirstMatch(amountWithKsh,
				AMOUNT_PATTERN_WITHOUT_DOT);
		return new BigDecimal(amountWithKshMinusZero.substring(3).replaceAll(
				",", ""));
	}

	/**
	 * MPESA Account balance
	 * 
	 * @param message
	 * @return
	 */
	public BigDecimal getBalance() {
		try {
			final String balance_part = getFirstMatch(message,
					"balance is Ksh[,|.|\\d]+");
			String amountWithKsh = balance_part.split("balance is ")[1];
			if (amountWithKsh.endsWith(".")) {
				amountWithKsh = amountWithKsh.substring(0,
						amountWithKsh.length() - 2);
			}
			return new BigDecimal(amountWithKsh.substring(3)
					.replaceAll(",", ""));
		} catch (final ArrayIndexOutOfBoundsException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	public String getAccountName() {
		String str;
		try {
			str = getFirstMatch(message, SENT_ACCOUNT_NAME).substring(
					"sent to ".length());
		} catch (IllegalStateException ex) {
			str = "";
		}
		return str;
	}

}

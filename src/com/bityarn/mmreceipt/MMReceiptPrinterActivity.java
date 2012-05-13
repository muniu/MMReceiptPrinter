package com.bityarn.mmreceipt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.bityarn.mpesareceipt.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import co.ke.bityarn.pdf.PDFWriter;
import co.ke.bityarn.pdf.PaperSize;
import co.ke.bityarn.pdf.StandardFonts;

/**
 * 
 * @author Muniu Kariuki - muniu@bityarn.co.ke
 * 
 */
public class MMReceiptPrinterActivity extends Activity {

	private String[] knownSenders = { "MPESA", "", "+254713778804" };

	private String[] sentKeywords = { "sent to" };
	private String[] receivedKeywords = { "received" };

	protected final String OUTBOUND_PAYMENT_TYPE = "Outbound";
	protected final String INBOUND_PAYMENT_TYPE = "Inbound";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		sweepSMSInbox();
	}

	private void sweepSMSInbox() {
		ContentResolver contentResolver = getContentResolver();
		final String[] fields = new String[] { "*" };
		Uri uri = Uri.parse("content://sms/inbox");
		Cursor queryCursor = contentResolver.query(uri, fields, null, null,
				"date asc");

		startManagingCursor(queryCursor);

		int mesgCount = queryCursor.getCount();

		Log.i("MM_RECEIPT", "Message Count = " + mesgCount);

		if (queryCursor.moveToFirst()) {
			for (int i = 0; i < mesgCount; i++) {

				String fromAddress = queryCursor.getString(
						queryCursor.getColumnIndexOrThrow("address"))
						.toString();
				String body = queryCursor.getString(
						queryCursor.getColumnIndexOrThrow("body")).toString();

				if (isFrom(fromAddress.toLowerCase(), knownSenders)
						&& hasKeywords(body.toLowerCase(), receivedKeywords)) {
					// This is an incoming payment

				} else if (isFrom(fromAddress.toLowerCase(), knownSenders)
						&& hasKeywords(body.toLowerCase(), sentKeywords)) {

					MPESAMesageParser messageParser = new MPESAMesageParser(
							body);

					// This is an outgoing payment

					String confirmationCode = messageParser
							.getConfirmationCode();
					String amount = messageParser.getAmount().toString();
					String balance = messageParser.getBalance().toString();
					String accountName = messageParser.getAccountName();
					String timestamp = messageParser.getTimestamp();

					Log.i("MM_RECEIPT", "Payment Type - Outbound");

					Log.i("MM_RECEIPT", "==================================");

					// Proceed to Generate and Save a PDF receipt for our
					// message
					String pdfContent = makeReceipt(OUTBOUND_PAYMENT_TYPE,
							confirmationCode, accountName, timestamp, amount);

					outputToFile(confirmationCode + "_Receipt.pdf", pdfContent,
							"ISO-8859-1");
				}

				queryCursor.moveToNext();
			}

		}
		queryCursor.close();
	}

	/**
	 * 
	 * Filter the @param messageSource through a list of known @param addresses
	 * 
	 * @return a boolean true is matches correctly or false if it doesn't
	 */
	public boolean isFrom(String messageSource, String[] addresses) {
		for (int i = 0; i < addresses.length; i++) {
			if (messageSource.equalsIgnoreCase(addresses[i])) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 
	 * Filter the @param message through a list of known @param keywords
	 * 
	 * @return
	 */
	public boolean hasKeywords(String message, String[] keywords) {
		for (int i = 0; i < keywords.length; i++) {
			if (message.contains(keywords[i].toLowerCase())) {
				return true;
			}
		}

		return false;
	}

	private String makeReceipt(String paymentType, String transactionCode,
			String name, String timeStamp, String amount) {
		PDFWriter mPDFWriter = new PDFWriter(PaperSize.FOLIO_WIDTH,
				PaperSize.FOLIO_HEIGHT);

		// note that to make this images snippet work
		// you have to uncompress the assets.zip file
		// included into your project assets folder
		AssetManager mngr = getAssets();
		try {
			Bitmap xoiPNG = BitmapFactory.decodeStream(mngr
					.open("mpesa_mpesa.jpg"));
			// Transformation.DEGREES_315_ROTATION
			mPDFWriter.addImage(200, 600, xoiPNG);

		} catch (IOException e) {
			e.printStackTrace();
		}

		mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_BOLD);
		mPDFWriter.addText(85, 565, 20, paymentType);

		// Disclaimer yada yada
		mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ITALIC);
		mPDFWriter.addText(85, 550, 15,
				"**This is not a purchase or sales Receipt**");
		mPDFWriter
				.addText(85, 535, 15,
						"**It is a printed receipt from an MPESA confirmation message**");

		mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ROMAN);

		mPDFWriter
				.addText(85, 475, 18, "Transaction Code : " + transactionCode);
		mPDFWriter.addText(85, 450, 18, "Name : " + name);
		mPDFWriter.addText(85, 425, 18, "Timestamp : " + timeStamp);
		mPDFWriter.addText(85, 400, 18, "Amount : " + amount);

		mPDFWriter.addText(85, 130, 12,
				"(Recipient Signature or Petty Cash Voucher Number)");
		mPDFWriter.addLine(85, 145, 335, 145);

		String s = mPDFWriter.asString();
		return s;

	}

	private void outputToFile(String fileName, String pdfContent,
			String encoding) {
		File newFile = new File(Environment.getExternalStorageDirectory() + "/"
				+ fileName);

		try {
			// newFile.createNewFile();
			Log.i("MM_RECEIPT",
					"Generated " + fileName + " " + newFile.createNewFile());
			try {
				FileOutputStream pdfFile = new FileOutputStream(newFile);
				pdfFile.write(pdfContent.getBytes(encoding));
				pdfFile.close();
			} catch (FileNotFoundException e) {
				Log.e("MM_RECEIPT", "Error " + e.getMessage());
			}
		} catch (IOException e) {
			Log.e("MM_RECEIPT", "Error " + e.getMessage());
		}
	}
}
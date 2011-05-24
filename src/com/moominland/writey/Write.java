package com.moominland.writey;

import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class Write extends Activity {
    private static final String TAG = "Writey";
	/** Called when the activity is first created. */
	private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private String[][] techLists;
	private IntentFilter[] intentFilters;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
        	ndef.addDataType("text/*");
        } catch (MalformedMimeTypeException e) { }
        
        IntentFilter techFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        intentFilters = new IntentFilter[] {ndef, techFilter};
        
        techLists = new String[][] { 
        		{ Ndef.class.getName() },
				{ NdefFormatable.class.getName() } };
    }
    
    @Override
    public void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists);
        String action = getIntent().getAction();
        Log.i(TAG, action);
        //I'm sure I was going to do something here
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
        	Log.i(TAG, "Action detected");
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    	Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    	v.vibrate(100);
    	Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    	EditText edit = (EditText) findViewById(R.id.writeTag);
    	
        byte[] textBytes = edit.getText().toString().getBytes();
        
        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/chariot".getBytes(),
                new byte[] {}, textBytes);
        NdefMessage message = new NdefMessage(new NdefRecord[] {
            textRecord
        });
    	
    	if (edit.getText().length() > 0) {
    		writeTag(message, tag);    		
    		hideKeyboard();
    	} else {
    		toast("Not writing an empty tag - silly!");
    		hideKeyboard();
    	}
    }
    
    private void toast(String text) {
    	Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
    
    private void hideKeyboard() {
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	EditText edit = (EditText) findViewById(R.id.writeTag);
    	imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
    }
    
    private boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    toast("Tag is read-only.");
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    toast("Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size
                            + " bytes.");
                    return false;
                }

                ndef.writeNdefMessage(message);
                toast("Wrote message to pre-formatted tag.");
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        toast("Formatted tag and wrote message");
                        return true;
                    } catch (IOException e) {
                        toast("Failed to format tag.");
                        return false;
                    }
                } else {
                    toast("Tag doesn't support NDEF.");
                    return false;
                }
            }
        } catch (Exception e) {
            toast("Failed to write tag");
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }
}
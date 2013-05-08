package com.danielkao.poweroff;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

public class ToggleAutoOnOffActivity extends Activity {

	@SuppressLint("CommitPrefEdits")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		boolean mIsAutoOn;
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_toggle_auto_on_off);
		
        SharedPreferences sp = getSharedPreferences(ConstantValues.PREF, Activity.MODE_PRIVATE);
        mIsAutoOn = sp.getBoolean(ConstantValues.IS_AUTO_ON, false);
        Editor editor = sp.edit();
        editor.putBoolean(ConstantValues.IS_AUTO_ON, !mIsAutoOn);
        editor.commit();
        
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(ConstantValues.TOGGLE_AUTO, !mIsAutoOn);
        startActivity(intent);
        finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.toggle_auto_on_off, menu);
		return true;
	}

}

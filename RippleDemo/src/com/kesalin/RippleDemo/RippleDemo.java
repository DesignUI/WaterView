package com.kesalin.RippleDemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class RippleDemo extends Activity {
	RippleView m_rippleView;
	LinearLayout main;

	/** Called when the activity is first created. */
	@SuppressLint("InlinedApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);


		setContentView(R.layout.main);
		main = (LinearLayout) findViewById(R.id.main);


		m_rippleView = new RippleView(this);

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);

		m_rippleView.setLayoutParams(lp);
		m_rippleView.setScaleType(ScaleType.FIT_XY);
		
		main.addView(m_rippleView);


	}

	public boolean onTouchEvent(MotionEvent event) {
		m_rippleView.processTouchEvent(event);

		return super.onTouchEvent(event);
	}


	protected void onStop() {
		m_rippleView.stop();

		super.onStop();
	}

	protected void onResume() {
		m_rippleView.resume();

		super.onResume();
	}

	protected void onDestroy() {
		m_rippleView.destroy();
		m_rippleView = null;

		super.onDestroy();
	}
}
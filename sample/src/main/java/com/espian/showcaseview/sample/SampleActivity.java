package com.espian.showcaseview.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.espian.showcaseview.ShowcaseView;

public class SampleActivity extends Activity implements View.OnClickListener,
		ShowcaseView.OnShowcaseEventListener {

	private ShowcaseView mShowcaseView;
	private Button mButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mShowcaseView = (ShowcaseView) findViewById(R.id.showcase);
		mShowcaseView.setShowcaseView(findViewById(R.id.button));
		mShowcaseView.setOnShowcaseEventListener(this);
		mButton = (Button) findViewById(R.id.button);
		mButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		if (mShowcaseView.isShown()) {
			mShowcaseView.hide();
		} else {
			mShowcaseView.show();
		}
	}

	@Override
	public void onShowcaseViewHide(ShowcaseView showcaseView) {
		mButton.setText(R.string.button_show);
	}

	@Override
	public void onShowcaseViewShow(ShowcaseView showcaseView) {
		mButton.setText(R.string.button_hide);
	}
}

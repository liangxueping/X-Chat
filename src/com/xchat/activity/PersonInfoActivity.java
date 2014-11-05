package com.xchat.activity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.xchat.swipeback.SwipeBackActivity;

public class PersonInfoActivity extends SwipeBackActivity implements OnClickListener{

	private TextView mTitleNameView;// 标题栏
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_personal_data);
		initView();// 初始化view
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
	}


	private void initView() {
		mTitleNameView = (TextView) findViewById(R.id.ivTitleName);
		mTitleNameView.setText("查看信息");
		ImageButton mLeftBtn = ((ImageButton) findViewById(R.id.show_left_fragment_btn));
		mLeftBtn.setVisibility(View.VISIBLE);
		mLeftBtn.setOnClickListener(this);
		mLeftBtn.setBackgroundResource(R.drawable.show_arrow_left_selector);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.show_left_fragment_btn:
			finish();
			break;
		default:
			break;
		}
	}
}

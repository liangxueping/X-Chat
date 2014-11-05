package com.xchat.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;

import com.xchat.adapter.MyFragmentPagerAdapter;
import com.xchat.fragment.Fragment3;
import com.xchat.fragment.FriendsFragment;
import com.xchat.fragment.RecentChatFragment;
import com.xchat.fragment.SettingsFragment;
import com.xchat.service.XChatService;
import com.xchat.util.L;
import com.xchat.util.PreferenceConstants;
import com.xchat.util.PreferenceUtils;
import com.xchat.util.T;

public class MainActivity extends BaseActivity implements OnClickListener,
		OnPageChangeListener, FragmentCallBack {

	private XChatService mXxService;

	private ViewPager pager;

	private List<String> titleList;
	private List<Fragment> fragList;
	public RecentChatFragment recentChatFragment;
	public FriendsFragment friendsFragment;
	public Fragment3 fragment3;
	public SettingsFragment settingsFragment;

	private View tabRecentChat, tabFriendsFragment, tabFragment3, tabSettingsFragment, currSelectedTab;

	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mXxService = ((XChatService.XXBinder) service).getService();
			mXxService.registerConnectionStatusCallback(friendsFragment);
			// 开始连接xmpp服务器
			if (!mXxService.isAuthenticated()) {
				String usr = PreferenceUtils.getPrefString(MainActivity.this,
						PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(
						MainActivity.this, PreferenceConstants.PASSWORD, "");
				mXxService.Login(usr, password);
			} else {
				friendsFragment.hasUpdateTitle = true;
				friendsFragment.updateTitle();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mXxService.unRegisterConnectionStatusCallback();
			mXxService = null;
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_center_layout);

		initViews();

		startService(new Intent(MainActivity.this, XChatService.class));
	}

	/**
	 * 连续按两次返回键就退出
	 */
	private long firstTime;

	@Override
	public void onBackPressed() {
		if (System.currentTimeMillis() - firstTime < 3000) {
			finish();
		} else {
			firstTime = System.currentTimeMillis();
			T.showShort(this, R.string.press_again_backrun);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		bindXMPPService();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindXMPPService();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
			L.i(LoginActivity.class, "[SERVICE] 绑定！");
		} catch (IllegalArgumentException e) {
			L.e(LoginActivity.class, "Service 绑定失败！");
		}
	}

	private void bindXMPPService() {
		L.i(LoginActivity.class, "[SERVICE] 解除绑定");
		bindService(new Intent(MainActivity.this, XChatService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE
						+ Context.BIND_DEBUG_UNBIND);
	}

	private void initViews() {
		tabFriendsFragment = findViewById(R.id.tabFriendsFragment);
		tabFragment3 = findViewById(R.id.tabFragment3);
		tabRecentChat = findViewById(R.id.tabRecentChat);
		tabSettingsFragment = findViewById(R.id.tabSettingsFragment);
		tabFriendsFragment.setOnClickListener(this);
		tabFragment3.setOnClickListener(this);
		tabRecentChat.setOnClickListener(this);
		tabSettingsFragment.setOnClickListener(this);
		pager = (ViewPager) findViewById(R.id.pager);

		titleList = new ArrayList<String>();
		titleList.add("第一页");
		titleList.add("第二页");
		titleList.add("第三页");
		titleList.add("第四页");

		recentChatFragment = new RecentChatFragment();
		friendsFragment = new FriendsFragment();
		fragment3 = new Fragment3();
		settingsFragment = new SettingsFragment();

		fragList = new ArrayList<Fragment>();
		fragList.add(recentChatFragment);
		fragList.add(friendsFragment);
		fragList.add(fragment3);
		fragList.add(settingsFragment);
		// 可以控制页面自动消亡
		MyFragmentPagerAdapter adapter = new MyFragmentPagerAdapter(
				getSupportFragmentManager(), fragList, titleList);

		pager.setAdapter(adapter);
		// 设置当前页面
		pager.setCurrentItem(1);
		// 设置默认加载页面数量（默认为1）
		pager.setOffscreenPageLimit(5);
		selectView(tabFriendsFragment);
		pager.setOnPageChangeListener(this);
	}

	@Override
	public void onClick(View v) {
		if (currSelectedTab == v) {
			return;
		}
		selectView(v);

		switch (v.getId()) {
		case R.id.tabRecentChat:
			pager.setCurrentItem(0);
			break;

		case R.id.tabFriendsFragment:
			pager.setCurrentItem(1);
			break;

		case R.id.tabFragment3:
			pager.setCurrentItem(2);
			break;
		case R.id.tabSettingsFragment:
			pager.setCurrentItem(3);
			break;
		}
	}

	private void selectView(View v) {
		if (currSelectedTab != null) {
			currSelectedTab.setSelected(false);
		}
		v.setSelected(true);
		currSelectedTab = v;
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageSelected(int arg0) {
		switch (arg0) {
		case 0:
			selectView(tabRecentChat);
			break;
		case 1:
			selectView(tabFriendsFragment);
			break;
		case 2:
			selectView(tabFragment3);
			break;
		case 3:
//			moreFragment.onFragmentSelected();
			selectView(tabSettingsFragment);
			break;
		}
	}

	@Override
	public XChatService getService() {
		return mXxService;
	}

	@Override
	public MainActivity getMainActivity() {
		return this;
	}

	public void updateRoster() {
		// mRosterAdapter.requery();
	}
}

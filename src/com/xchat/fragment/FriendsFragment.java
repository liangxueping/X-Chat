package com.xchat.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.xchat.activity.ChatActivity;
import com.xchat.activity.FragmentCallBack;
import com.xchat.activity.MainActivity;
import com.xchat.activity.R;
import com.xchat.adapter.RosterAdapter;
import com.xchat.app.XChatBroadcastReceiver;
import com.xchat.app.XChatBroadcastReceiver.EventHandler;
import com.xchat.db.RosterProvider;
import com.xchat.db.RosterProvider.RosterConstants;
import com.xchat.iphonetreeview.IphoneTreeView;
import com.xchat.pulltorefresh.PullToRefreshBase;
import com.xchat.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.xchat.pulltorefresh.PullToRefreshScrollView;
import com.xchat.quickaction.ActionItem;
import com.xchat.quickaction.QuickAction;
import com.xchat.quickaction.QuickAction.OnActionItemClickListener;
import com.xchat.service.IConnectionStatusCallback;
import com.xchat.service.XChatService;
import com.xchat.util.L;
import com.xchat.util.NetUtil;
import com.xchat.util.PreferenceConstants;
import com.xchat.util.PreferenceUtils;
import com.xchat.util.T;
import com.xchat.util.XMPPHelper;
import com.xchat.view.AddRosterItemDialog;
import com.xchat.view.CustomDialog;
import com.xchat.view.GroupNameView;

public class FriendsFragment extends Fragment implements OnClickListener, IConnectionStatusCallback, EventHandler {

	private View mView;
	private Context mContext;
	
	private static final int ID_CHAT = 0;
	private static final int ID_AVAILABLE = 1;
	private static final int ID_AWAY = 2;
	private static final int ID_XA = 3;
	private static final int ID_DND = 4;
	public static HashMap<String, Integer> mStatusMap;
	static {
		mStatusMap = new HashMap<String, Integer>();
		mStatusMap.put(PreferenceConstants.OFFLINE, -1);
		mStatusMap.put(PreferenceConstants.DND, R.drawable.status_shield);
		mStatusMap.put(PreferenceConstants.XA, R.drawable.status_invisible);
		mStatusMap.put(PreferenceConstants.AWAY, R.drawable.status_leave);
		mStatusMap.put(PreferenceConstants.AVAILABLE, R.drawable.status_online);
		mStatusMap.put(PreferenceConstants.CHAT, R.drawable.status_qme);
	}
	private Handler mainHandler = new Handler();
	private View mNetErrorView;
	private TextView mTitleNameView;
	private ImageView mTitleStatusView;
	private ProgressBar mTitleProgressBar;
	private PullToRefreshScrollView mPullRefreshScrollView;
	private IphoneTreeView mIphoneTreeView;
	private RosterAdapter mRosterAdapter;
	private ContentObserver mRosterObserver = new RosterObserver();
	private int mLongPressGroupId, mLongPressChildId;
	private FragmentCallBack mFragmentCallBack;
	public boolean hasUpdateTitle = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mFragmentCallBack = (FragmentCallBack) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnHeadlineSelectedListener");
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.friends_layout, container, false);
		mContext = mView.getContext();
		initViews(inflater);
		registerListAdapter();
		return mView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mFragmentCallBack.getMainActivity().getContentResolver().registerContentObserver(RosterProvider.CONTENT_URI, true, mRosterObserver);
		setStatusImage(isConnected());
		mRosterAdapter.requery();
		XChatBroadcastReceiver.mListeners.add(this);
		if (NetUtil.getNetworkState(mContext) == NetUtil.NETWORN_NONE)
			mNetErrorView.setVisibility(View.VISIBLE);
		else
			mNetErrorView.setVisibility(View.GONE);
	}

	@Override
	public void onPause() {
		super.onPause();
		mFragmentCallBack.getMainActivity().getContentResolver().unregisterContentObserver(mRosterObserver);
		XChatBroadcastReceiver.mListeners.remove(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	};
	
	private void initViews(LayoutInflater inflater) {
		mNetErrorView = mView.findViewById(R.id.net_status_bar_top);

		mTitleNameView = (TextView) mView.findViewById(R.id.ivTitleName);
		mTitleProgressBar = (ProgressBar) mView.findViewById(R.id.ivTitleProgress);
		mTitleStatusView = (ImageView) mView.findViewById(R.id.ivTitleStatus);
		mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils.getPrefString(mContext, PreferenceConstants.ACCOUNT, "")));
		mTitleNameView.setOnClickListener(this);

		mPullRefreshScrollView = (PullToRefreshScrollView) mView.findViewById(R.id.pull_refresh_scrollview);
		// mPullRefreshScrollView.setMode(Mode.DISABLED);
		// mPullRefreshScrollView.getLoadingLayoutProxy().setLastUpdatedLabel(
		// "最近更新：刚刚");
		mPullRefreshScrollView.setOnRefreshListener(new OnRefreshListener<ScrollView>() {
			@Override
			public void onRefresh(
					PullToRefreshBase<ScrollView> refreshView) {
				new GetDataTask().execute();
			}
		});
		mIphoneTreeView = (IphoneTreeView) mView.findViewById(R.id.iphone_tree_view);
		mIphoneTreeView.setHeaderView(inflater.inflate(R.layout.contact_buddy_list_group, mIphoneTreeView, false));
		mIphoneTreeView.setEmptyView(mView.findViewById(R.id.empty));
		mIphoneTreeView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				int groupPos = (Integer) view.getTag(R.id.xxx01); // 参数值是在setTag时使用的对应资源id号
				int childPos = (Integer) view.getTag(R.id.xxx02);
				mLongPressGroupId = groupPos;
				mLongPressChildId = childPos;
				if (childPos == -1) {
					// 长按的是父项
					// 根据groupPos判断你长按的是哪个父项，做相应处理（弹框等）
					showGroupQuickActionBar(view.findViewById(R.id.group_name));
					// T.showShort(mContext,
					// "LongPress group position = " + groupPos);
				} else {
					// 根据groupPos及childPos判断你长按的是哪个父项下的哪个子项，然后做相应处理。
					// T.showShort(mContext,
					// "onClick child position = " + groupPos
					// + ":" + childPos);
					showChildQuickActionBar(view.findViewById(R.id.icon));

				}
				return false;
			}
		});
		mIphoneTreeView.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				String userJid = mRosterAdapter.getChild(groupPosition, childPosition).getJid();
				String userName = mRosterAdapter.getChild(groupPosition, childPosition).getAlias();
				startChatActivity(userJid, userName);
				return false;
			}
		});
		updateTitle();
	}

	private void registerListAdapter() {
		mRosterAdapter = new RosterAdapter(mContext, mIphoneTreeView, mPullRefreshScrollView);
		mIphoneTreeView.setAdapter(mRosterAdapter);
		mRosterAdapter.requery();
	}

	private void showGroupQuickActionBar(View view) {
		QuickAction quickAction = new QuickAction(mContext, QuickAction.HORIZONTAL);
		quickAction.addActionItem(new ActionItem(0, getString(R.string.rename)));
		quickAction.addActionItem(new ActionItem(1, getString(R.string.add_friend)));
		quickAction.setOnActionItemClickListener(new OnActionItemClickListener() {
			@Override
			public void onItemClick(QuickAction source, int pos,
					int actionId) {
				// 如果没有连接直接返回
				if (!isConnected()) {
					T.showShort(mContext, R.string.conversation_net_error_label);
					return;
				}
				switch (actionId) {
				case 0:
					String groupName = mRosterAdapter.getGroup(mLongPressGroupId).getGroupName();
					if (TextUtils.isEmpty(groupName)) {// 系统默认分组不允许重命名
						T.showShort(mContext, R.string.roster_group_rename_failed);
						return;
					}
					renameRosterGroupDialog(mRosterAdapter.getGroup(mLongPressGroupId).getGroupName());
					break;
				case 1:

					new AddRosterItemDialog(mFragmentCallBack.getMainActivity(), mFragmentCallBack.getService()).show();// 添加联系人
					break;
				default:
					break;
				}
			}
		});
		quickAction.show(view);
		quickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);
	}

	private void showChildQuickActionBar(View view) {
		QuickAction quickAction = new QuickAction(mContext, QuickAction.HORIZONTAL);
		quickAction.addActionItem(new ActionItem(0, getString(R.string.open)));
		quickAction.addActionItem(new ActionItem(1, getString(R.string.rename)));
		quickAction.addActionItem(new ActionItem(2, getString(R.string.move)));
		quickAction.addActionItem(new ActionItem(3, getString(R.string.delete)));
		quickAction.setOnActionItemClickListener(new OnActionItemClickListener() {

			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				String userJid = mRosterAdapter.getChild(mLongPressGroupId, mLongPressChildId).getJid();
				String userName = mRosterAdapter.getChild(mLongPressGroupId, mLongPressChildId).getAlias();
				switch (actionId) {
				case 0:
					startChatActivity(userJid, userName);
					break;
				case 1:
					if (!isConnected()) {
						T.showShort(mContext, R.string.conversation_net_error_label);
						break;
					}
					renameRosterItemDialog(userJid, userName);
					break;
				case 2:
					if (!isConnected()) {
						T.showShort(mContext, R.string.conversation_net_error_label);
						break;
					}
					moveRosterItemToGroupDialog(userJid);
					break;
				case 3:
					if (!isConnected()) {
						T.showShort(mContext, R.string.conversation_net_error_label);
						break;
					}
					removeRosterItemDialog(userJid, userName);
					break;
				default:
					break;
				}
			}
		});
		quickAction.show(view);
	}

	void removeRosterItemDialog(final String JID, final String userName) {
		new CustomDialog.Builder(mFragmentCallBack.getMainActivity())
				.setTitle(R.string.deleteRosterItem_title)
				.setMessage(getString(R.string.deleteRosterItem_text, userName, JID))
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								mFragmentCallBack.getService().removeRosterItem(JID);
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						}).create().show();
	}
	@SuppressWarnings("static-access")
	void moveRosterItemToGroupDialog(final String jabberID) {
		LayoutInflater inflater = (LayoutInflater) mFragmentCallBack.getMainActivity().getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
		View group = inflater
				.inflate(R.layout.moverosterentrytogroupview, null);
		final GroupNameView gv = (GroupNameView) group
				.findViewById(R.id.moverosterentrytogroupview_gv);
		gv.setGroupList(getRosterGroups());
		new CustomDialog.Builder(mFragmentCallBack.getMainActivity())
				.setTitle(R.string.MoveRosterEntryToGroupDialog_title)
				.setView(group)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						L.d("new group: " + gv.getGroupName());
						if (isConnected()){
							mFragmentCallBack.getService().moveRosterItemToGroup(jabberID, gv.getGroupName());
						}
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				}).create().show();
	}
	void renameRosterItemDialog(final String JID, final String userName) {
		editTextDialog(R.string.RenameEntry_title,
				getString(R.string.RenameEntry_summ, userName, JID), userName,
				new EditOk() {
					public void ok(String result) {
						if (mFragmentCallBack.getService() != null)
							mFragmentCallBack.getService().renameRosterItem(JID, result);
					}
				});
	}
	
	void renameRosterGroupDialog(final String groupName) {
		editTextDialog(R.string.RenameGroup_title,
				getString(R.string.RenameGroup_summ, groupName), groupName,
				new EditOk() {
					public void ok(String result) {
						if (mFragmentCallBack.getService() != null)
							mFragmentCallBack.getService().renameRosterGroup(groupName, result);
					}
				});
	}

	@SuppressWarnings("static-access")
	private void editTextDialog(int titleId, CharSequence message, String text, final EditOk ok) {
		LayoutInflater inflater = (LayoutInflater) mFragmentCallBack.getMainActivity().getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.edittext_dialog, null);

		TextView messageView = (TextView) layout.findViewById(R.id.text);
		messageView.setText(message);
		final EditText input = (EditText) layout.findViewById(R.id.editText);
		input.setTransformationMethod(android.text.method.SingleLineTransformationMethod.getInstance());
		input.setText(text);
		new CustomDialog.Builder(mFragmentCallBack.getMainActivity())
				.setTitle(titleId)
				.setView(layout)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								String newName = input.getText().toString();
								if (newName.length() != 0)
									ok.ok(newName);
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						}).create().show();
	}


	private static final String[] GROUPS_QUERY = new String[] { 
		RosterConstants._ID, 
		RosterConstants.GROUP, 
		};
	private static final String[] ROSTER_QUERY = new String[] { 
		RosterConstants._ID, 
		RosterConstants.JID, 
		RosterConstants.ALIAS, 
		RosterConstants.STATUS_MODE, 
		RosterConstants.STATUS_MESSAGE, 
		};

	public List<String> getRosterGroups() {
		// we want all, online and offline
		List<String> list = new ArrayList<String>();
		Cursor cursor = mFragmentCallBack.getMainActivity().getContentResolver().query(RosterProvider.GROUPS_URI, GROUPS_QUERY, null, null, RosterConstants.GROUP);
		int idx = cursor.getColumnIndex(RosterConstants.GROUP);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			list.add(cursor.getString(idx));
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	public List<String[]> getRosterContacts() {
		// we want all, online and offline
		List<String[]> list = new ArrayList<String[]>();
		Cursor cursor = mFragmentCallBack.getMainActivity().getContentResolver().query(RosterProvider.CONTENT_URI, ROSTER_QUERY, null, null, RosterConstants.ALIAS);
		int JIDIdx = cursor.getColumnIndex(RosterConstants.JID);
		int aliasIdx = cursor.getColumnIndex(RosterConstants.ALIAS);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String jid = cursor.getString(JIDIdx);
			String alias = cursor.getString(aliasIdx);
			if ((alias == null) || (alias.length() == 0))
				alias = jid;
			list.add(new String[] { jid, alias });
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	public Context getContext(){
		return mContext;
	}
	
	public void updateTitle(){
		if(hasUpdateTitle && mTitleNameView != null){
			mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils.getPrefString(mContext, PreferenceConstants.ACCOUNT,"")));
			setStatusImage(true);
			mTitleProgressBar.setVisibility(View.GONE);
			hasUpdateTitle = false;
		}
	}

	private void startChatActivity(String userJid, String userName) {
		Intent chatIntent = new Intent(mContext, ChatActivity.class);
		Uri userNameUri = Uri.parse(userJid);
		chatIntent.setData(userNameUri);
		chatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME, userName);
		startActivity(chatIntent);
	}

	private void setStatusImage(boolean isConnected) {
		if (!isConnected) {
			mTitleStatusView.setVisibility(View.GONE);
			return;
		}
		String statusMode = PreferenceUtils.getPrefString(mContext, PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
		int statusId = mStatusMap.get(statusMode);
		if (statusId == -1) {
			mTitleStatusView.setVisibility(View.GONE);
		} else {
			mTitleStatusView.setVisibility(View.VISIBLE);
			mTitleStatusView.setImageResource(statusId);
		}
	}

	private boolean isConnected() {
		return mFragmentCallBack.getService() != null && mFragmentCallBack.getService().isAuthenticated();
	}
	
	@Override
	public void onNetChange() {
		if (NetUtil.getNetworkState(mContext) == NetUtil.NETWORN_NONE) {
			T.showShort(mContext, R.string.net_error_tip);
			mNetErrorView.setVisibility(View.VISIBLE);
		} else {
			mNetErrorView.setVisibility(View.GONE);
		}
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		switch (connectedState) {
		case XChatService.CONNECTED:
			mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils.getPrefString(mContext, PreferenceConstants.ACCOUNT, "")));
			mTitleProgressBar.setVisibility(View.GONE);
			// mTitleStatusView.setVisibility(View.GONE);
			setStatusImage(true);
			break;
		case XChatService.CONNECTING:
			mTitleNameView.setText(R.string.login_prompt_msg);
			mTitleProgressBar.setVisibility(View.VISIBLE);
			mTitleStatusView.setVisibility(View.GONE);
			break;
		case XChatService.DISCONNECTED:
			mTitleNameView.setText(R.string.login_prompt_no);
			mTitleProgressBar.setVisibility(View.GONE);
			mTitleStatusView.setVisibility(View.GONE);
			T.showLong(mContext, reason);
			break;

		default:
			break;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ivTitleName:
			if (isConnected())
				showStatusQuickAction(v);
			break;
		default:
			break;
		}
	}

	private void showStatusQuickAction(View v) {
		QuickAction quickAction = new QuickAction(mContext, QuickAction.VERTICAL);
		quickAction.addActionItem(new ActionItem(ID_CHAT, getString(R.string.status_chat), getResources().getDrawable(R.drawable.status_qme)));
		quickAction.addActionItem(new ActionItem(ID_AVAILABLE, getString(R.string.status_available), getResources().getDrawable(R.drawable.status_online)));
		quickAction.addActionItem(new ActionItem(ID_AWAY, getString(R.string.status_away), getResources().getDrawable( R.drawable.status_leave)));
		quickAction.addActionItem(new ActionItem(ID_XA, getString(R.string.status_xa), getResources().getDrawable(R.drawable.status_invisible)));
		quickAction.addActionItem(new ActionItem(ID_DND, getString(R.string.status_dnd), getResources().getDrawable(R.drawable.status_shield)));
		quickAction.setOnActionItemClickListener(new OnActionItemClickListener() {
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				if (!isConnected()) {
					T.showShort(mContext, R.string.conversation_net_error_label);
					return;
				}
				switch (actionId) {
				case ID_CHAT:
					mTitleStatusView.setImageResource(R.drawable.status_qme);
					PreferenceUtils.setPrefString(mContext, PreferenceConstants.STATUS_MODE, PreferenceConstants.CHAT);
					PreferenceUtils.setPrefString(mContext, PreferenceConstants.STATUS_MESSAGE, getString(R.string.status_chat));
					break;
				case ID_AVAILABLE:
					mTitleStatusView.setImageResource(R.drawable.status_online);
					PreferenceUtils.setPrefString(mContext, PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
					PreferenceUtils.setPrefString(mContext, PreferenceConstants.STATUS_MESSAGE, getString(R.string.status_available));
					break;
				case ID_AWAY:
					mTitleStatusView.setImageResource(R.drawable.status_leave);
					PreferenceUtils.setPrefString(mContext, PreferenceConstants.STATUS_MODE, PreferenceConstants.AWAY);
					PreferenceUtils.setPrefString(mContext, PreferenceConstants.STATUS_MESSAGE, getString(R.string.status_away));
					break;
				case ID_XA:
					mTitleStatusView.setImageResource(R.drawable.status_invisible);
					PreferenceUtils.setPrefString(mContext, PreferenceConstants.STATUS_MODE, PreferenceConstants.XA);
					PreferenceUtils.setPrefString(mContext, PreferenceConstants.STATUS_MESSAGE, getString(R.string.status_xa));
					break;
				case ID_DND:
					mTitleStatusView.setImageResource(R.drawable.status_shield);
					PreferenceUtils.setPrefString(mContext, PreferenceConstants.STATUS_MODE, PreferenceConstants.DND);
					PreferenceUtils.setPrefString(mContext, PreferenceConstants.STATUS_MESSAGE, getString(R.string.status_dnd));
					break;
				default:
					break;
				}
				mFragmentCallBack.getService().setStatusFromConfig();
				mFragmentCallBack.getMainActivity().settingsFragment.readData();
			}
		});
		quickAction.show(v);
	}
	public void updateRoster() {
		mRosterAdapter.requery();
	}
	
	private class RosterObserver extends ContentObserver {
		public RosterObserver() {
			super(mainHandler);
		}

		public void onChange(boolean selfChange) {
			L.d(MainActivity.class, "RosterObserver.onChange: " + selfChange);
			if (mRosterAdapter != null)
				mainHandler.postDelayed(new Runnable() {
					public void run() {
						updateRoster();
					}
				}, 100);
		}
	}

	private class GetDataTask extends AsyncTask<Void, Void, String[]> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// if (mPullRefreshScrollView.getState() != State.REFRESHING)
			// mPullRefreshScrollView.setState(State.REFRESHING, true);
		}

		@Override
		protected String[] doInBackground(Void... params) {
			// Simulates a background job.
			if (!isConnected()) {// 如果没有连接重新连接
				String usr = PreferenceUtils.getPrefString(mContext, PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(mContext, PreferenceConstants.PASSWORD, "");
				mFragmentCallBack.getService().Login(usr, password);
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			return null;
		}

		@Override
		protected void onPostExecute(String[] result) {
			mRosterAdapter.requery();// 重新查询一下数据库
			mPullRefreshScrollView.onRefreshComplete();
			T.showShort(mContext, "刷新成功!");
			super.onPostExecute(result);
		}
	}
	public abstract class EditOk {
		abstract public void ok(String result);
	}
}

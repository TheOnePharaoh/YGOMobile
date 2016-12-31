package cn.garymb.ygomobile.model.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.garymb.ygomobile.common.Constants;
import cn.garymb.ygomobile.data.wrapper.BaseRequestJob;
import cn.garymb.ygomobile.data.wrapper.RoomRequestJob;
import cn.garymb.ygomobile.data.wrapper.ServerRequestJob;
import cn.garymb.ygomobile.ygo.YGORoomInfo;
import cn.garymb.ygomobile.ygo.YGOServerInfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

public class DataStore {

	public static final int MODIFIABLE_SERVER_INFO_START = 0xFFD;
	public static final int EXTERNAL_SERVER_1 = 0xFFE;
	public static final int EXTERNAL_SERVER_2 = 0xFFF;
	public static final int MYCARD_PRIVATE_SERVER = 0xFFD;
	public static final int MYCARD_SERVER = 0x1000;
	public static final int MODIFIABLE_SERVER_CHECKMATE_SERVER = 0x1001;
	public static final int USER_DEFINE_SERVER_INFO_START = 0x1002;

	private static final String DEFAULT_USER_NAME = "player";
	private static final String DEFAULT_CHECKMATE_SERVER_NAME = "checkmate";
	private static final String DEFAULT_CHECKMATE_SERVER_ADDR = "173.224.211.158";
	private static final int DEFAULT_CHECKMATE_SERVER_PORT = 21001;

	private static final String EXTERNAL_SERVER_1_NAME = "external_1";
	private static final String EXTERNAL_SERVER_1_ADDR = "122.0.65.73";
	private static final int EXTERNAL_SERVER_1_PORT = 233;

	private static final String EXTERNAL_SERVER_2_NAME = "external_2(with AI)";
	private static final String EXTERNAL_SERVER_2_ADDR = "117.21.174.109";
	private static final int EXTERNAL_SERVER_2_PORT = 8910;

	private static final String MYCARD_PRIVATE_SERVER_NAME = "紫毛の幻想乡";
	private static final String MYCARD_PRIVATE_SERVER_ADDR = "122.0.65.73";
	private static final int MYCARD_PRIVATE_SERVER_PORT = 1998;

	private static final String TAG = "DataStore";

	private SparseArray<YGOServerInfo> mServers;
	private Map<String, YGORoomInfo> mRooms;

	private CardImageUrlInfo mCardImageUrlInfo;

	private Context mContext;

	public DataStore(Context context) {
		mContext = context;
		mServers = new SparseArray<YGOServerInfo>();
		LoadModifiableServers();
		mRooms = new HashMap<String, YGORoomInfo>();
	}

	private void LoadModifiableServers() {
		SharedPreferences sp = mContext.getSharedPreferences(Constants.PREF_FILE_SERVER_LIST, Context.MODE_PRIVATE);
		// add default server.
		loadNewServer(sp, MODIFIABLE_SERVER_CHECKMATE_SERVER, DEFAULT_CHECKMATE_SERVER_NAME,
				DEFAULT_CHECKMATE_SERVER_ADDR, DEFAULT_CHECKMATE_SERVER_PORT);
		loadNewServer(sp, MYCARD_SERVER, ResourcesConstants.DEFAULT_MC_SERVER_NAME,
				ResourcesConstants.DEFAULT_MC_SERVER_ADDR, ResourcesConstants.DEFAULT_MC_SERVER_PORT);
		loadNewServer(sp, MYCARD_PRIVATE_SERVER, MYCARD_PRIVATE_SERVER_NAME,
				MYCARD_PRIVATE_SERVER_ADDR, MYCARD_PRIVATE_SERVER_PORT);
		loadNewServer(sp, EXTERNAL_SERVER_1, EXTERNAL_SERVER_1_NAME, EXTERNAL_SERVER_1_ADDR,
				EXTERNAL_SERVER_1_PORT);
		loadNewServer(sp, EXTERNAL_SERVER_2, EXTERNAL_SERVER_2_NAME, EXTERNAL_SERVER_2_ADDR,
				EXTERNAL_SERVER_2_PORT);
		int size = sp.getInt(Constants.PREF_KEY_USER_DEF_SERVER_SIZE, 0);
		// add user define server.
		for (int i = 0; i < size; i++) {
			loadNewServer(sp, MODIFIABLE_SERVER_INFO_START + i, "", "", 0);
		}
	}

	public void removeServer(int groupId) {
		mServers.remove(groupId);
		SharedPreferences.Editor editor = mContext
				.getSharedPreferences(Constants.PREF_FILE_SERVER_LIST, Context.MODE_PRIVATE).edit();
		editor.putInt(Constants.PREF_KEY_USER_DEF_SERVER_SIZE, mServers.size());
		editor.remove(Constants.PREF_KEY_SERVER_NAME + groupId);
		editor.remove(Constants.PREF_KEY_USER_NAME + groupId);
		editor.remove(Constants.PREF_KEY_SERVER_ADDR + groupId);
		editor.remove(Constants.PREF_KEY_SERVER_PORT + groupId);
		editor.remove(Constants.PREF_KEY_SERVER_INFO + groupId);
		editor.commit();
	}

	public void addNewServer(YGOServerInfo info) {
		mServers.put(Integer.parseInt(info.id), info);
		SharedPreferences.Editor editor = mContext
				.getSharedPreferences(Constants.PREF_FILE_SERVER_LIST, Context.MODE_PRIVATE).edit();
		editor.putInt(Constants.PREF_KEY_USER_DEF_SERVER_SIZE, mServers.size());
		editor.putString(Constants.PREF_KEY_USER_NAME + info.id, info.userName);
		editor.putString(Constants.PREF_KEY_SERVER_NAME + info.id, info.name);
		editor.putString(Constants.PREF_KEY_SERVER_ADDR + info.id, info.ipAddrString);
		editor.putString(Constants.PREF_KEY_SERVER_INFO + info.id, info.serverInfoString);
		editor.putInt(Constants.PREF_KEY_SERVER_PORT + info.id, info.port);
		editor.commit();
	}

	private boolean loadNewServer(SharedPreferences sp, int index, String defname, String defAddr, int defPort) {
		if (index < MODIFIABLE_SERVER_INFO_START) {
			Log.w(TAG, "can not add a server index less than " + MODIFIABLE_SERVER_INFO_START);
		}
		String server = sp.getString(Constants.PREF_KEY_SERVER_ADDR + index, defAddr);
		String name = sp.getString(Constants.PREF_KEY_SERVER_NAME + index, defname);
		String user = sp.getString(Constants.PREF_KEY_USER_NAME + index, DEFAULT_USER_NAME);
		String serverInfo = sp.getString(Constants.PREF_KEY_SERVER_INFO + index, "");
		int port = sp.getInt(Constants.PREF_KEY_SERVER_PORT + index, defPort);
		YGOServerInfo info = new YGOServerInfo(index + "", user, name, server, port);
		info.serverInfoString = serverInfo;
		mServers.put(index, info);
		boolean isServerNotExist = TextUtils.isEmpty(sp.getString(Constants.PREF_KEY_SERVER_ADDR + index, ""));
		if (isServerNotExist) {
			addNewServer(info);
		}
		return isServerNotExist;
	}

	public synchronized List<YGORoomInfo> getRooms() {
		List<YGORoomInfo> rooms = new ArrayList<YGORoomInfo>();
		for (YGORoomInfo info : mRooms.values()) {
			rooms.add(info.clone());
		}
		return rooms;
	}

	public synchronized YGOServerInfo getMyCardServer() {
		// try to set default server addr
		if (mServers.get(0) == null) {
			mServers.put(0, new YGOServerInfo("0", "player", ResourcesConstants.DEFAULT_MC_SERVER_NAME,
					ResourcesConstants.DEFAULT_MC_SERVER_ADDR, ResourcesConstants.DEFAULT_MC_SERVER_PORT));
		}
		return mServers.get(0);
	}

	public synchronized void updateData(BaseRequestJob wrapper) {
		if (wrapper instanceof ServerRequestJob) {
			int size = ((ServerRequestJob) wrapper).size();
			for (int i = 0; i < size; i++) {
				mServers.put(i, (YGOServerInfo) ((ServerRequestJob) wrapper).getItem(i));
			}
		} else if (wrapper instanceof RoomRequestJob) {
			int size = ((RoomRequestJob) wrapper).size();
			for (int i = 0; i < size; i++) {
				YGORoomInfo info = (YGORoomInfo) ((RoomRequestJob) wrapper).getItem(i);
				if (info.deleted) {
					mRooms.remove(info.id);
				} else {
					mRooms.put(info.id, info);
				}
			}
		}
	}

	public synchronized SparseArray<YGOServerInfo> getServers() {
		if (mServers.get(0) == null) {
		}
		return mServers;
	}

	public void updateCardImageURL(CardImageUrlInfo info) {
		if (info != null) {
			mCardImageUrlInfo = info;
			ImageItemInfoHelper.init(info.mEnImgLQUrl);
		}
	}

}

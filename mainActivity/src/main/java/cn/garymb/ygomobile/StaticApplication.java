package cn.garymb.ygomobile;

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_NAME;
import static org.acra.ReportField.BUILD;
import static org.acra.ReportField.CUSTOM_DATA;
import static org.acra.ReportField.DEVICE_FEATURES;
import static org.acra.ReportField.DISPLAY;
import static org.acra.ReportField.DUMPSYS_MEMINFO;
import static org.acra.ReportField.ENVIRONMENT;
import static org.acra.ReportField.LOGCAT;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.TOTAL_MEM_SIZE;
import static org.acra.ReportField.USER_CRASH_DATE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.HC4.impl.nio.client.CloseableHttpPipeliningClient;
import org.apache.http.HC4.impl.nio.client.HttpAsyncClients;

import cn.garymb.ygomobile.common.Constants;
import cn.garymb.ygomobile.controller.Controller;
import cn.garymb.ygomobile.core.CrashSender;
import cn.garymb.ygomobile.setting.Settings;

import com.github.nativehandler.NativeCrashHandler;
import okhttp3.OkHttpClient;
import com.umeng.fb.FeedbackAgent;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.Display;
import android.view.WindowManager;

@ReportsCrashes(formKey = "", // will not be used
customReportContent = { APP_VERSION_NAME, ANDROID_VERSION, PHONE_MODEL,
		CUSTOM_DATA, STACK_TRACE, USER_CRASH_DATE, LOGCAT, BUILD,
		TOTAL_MEM_SIZE, DISPLAY, DUMPSYS_MEMINFO, DEVICE_FEATURES, ENVIRONMENT }, mailTo = "garymabin@gmail.com", includeDropBoxSystemTags = true, mode = ReportingInteractionMode.DIALOG, resDialogText = R.string.crashed, resDialogIcon = android.R.drawable.ic_dialog_info, // optional.
// default
// is
// a
// warning
// sign
resDialogTitle = R.string.crash_title, // optional. default is your application
// name
resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when
// defined, adds
// a user text
// field input
// with this
// text resource
// as a label
resDialogOkToast = R.string.crash_dialog_ok_toast)
public class StaticApplication extends Application {

	private static final String TAG = "StaticApplication";

	public static Pair<String, String> sRootPair;

	private static StaticApplication INSTANCE;

	private SharedPreferences mSettingsPref;

	private String mDataBasePath;

	private float mScreenWidth;

	private float mScreenHeight;

	private float mDensity;

	private int mVersionCode;

	private String mVersionName;

	private SoundPool mSoundEffectPool;

	private ArrayList<String> mFontsPath = new ArrayList<String>();

	private Map<String, Integer> mSoundIdMap;
	private String mCoreConfigVersion;
	
	private FeedbackAgent mFeedbackAgent;

	static {
		System.loadLibrary("YGOMobile");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		INSTANCE = this;
		new NativeCrashHandler().registerForNativeCrash(this);
		ACRA.init(this);
		CrashSender sender = new CrashSender(this);
		ACRA.getErrorReporter().setReportSender(sender);
		Controller.peekInstance();
		sRootPair = Pair
				.create(getResources().getString(R.string.root_dir), "/"/* "Environment.getExternalStorageDirectory().getPath()" */);
		mSettingsPref = PreferenceManager.getDefaultSharedPreferences(this);
		if (android.os.Build.VERSION.SDK_INT >= 17) {
			mDataBasePath = getApplicationInfo().dataDir + "/databases/";
		} else {
			mDataBasePath = "/data/data/" + getPackageName() + "/databases/";
		}
		try {
			PackageInfo info = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			mVersionCode = info.versionCode;
			mVersionName = info.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		Controller.peekInstance();
		initSoundEffectPool();
		Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		Point screenResolution = new Point();
		display.getRealSize(screenResolution);
		mScreenWidth = screenResolution.x;
		mScreenHeight = screenResolution.y;
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		mSoundEffectPool.release();
	}

	private void initSoundEffectPool() {
		mSoundEffectPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
		AssetManager am = getAssets();
		String[] sounds;
		mSoundIdMap = new HashMap<String, Integer>();
		try {
			sounds = am.list("sound");
			for (String sound : sounds) {
				String path = "sound" + File.separator + sound;
				mSoundIdMap
						.put(path, mSoundEffectPool.load(am.openFd(path), 1));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void playSoundEffect(String path) {
		Integer id = mSoundIdMap.get(path);
		if (id != null) {
			mSoundEffectPool.play(id, 0.5f, 0.5f, 2, 0, 1.0f);
		}
	}


	public byte[] getSignInfo() {
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), PackageManager.GET_SIGNATURES);
			Signature[] signs = pi.signatures;
			Signature sign = signs[0];
			return parseSignature(sign.toByteArray());
		} catch (Exception e) {
		}
		return null;
	}

	private byte[] parseSignature(byte[] signature) {
		try {
			CertificateFactory certFactory = CertificateFactory
					.getInstance("X.509");
			X509Certificate cert = (X509Certificate) certFactory
					.generateCertificate(new ByteArrayInputStream(signature));
			byte[] buffer = cert.getEncoded();
			return Arrays.copyOf(buffer, 16);
		} catch (Exception e) {
		}
		return null;
	}

	public OkHttpClient getOkHttpClient() {
		return new OkHttpClient();
	}

	public CloseableHttpPipeliningClient getPipelinlingHttpClient() {
		return HttpAsyncClients.createPipelining();
	}

	public static StaticApplication peekInstance() {
		return INSTANCE;
	}

	public String getDefaultImageCacheRootPath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath()
				+ Constants.WORKING_DIRECTORY + Constants.CARD_IMAGE_DIRECTORY;
	}

	public String getCoreSkinPath() {
		return getCacheDir() + File.separator
				+ Constants.CORE_SKIN_PATH;
	}
	
	public String getCoreConfigVersion() {
		return mCoreConfigVersion;
	}
	
	public void setCoreConfigVersion(String ver) {
		mCoreConfigVersion = ver;
	}

	public String getDefaultFontName() {
		return Constants.DEFAULT_FONT_NAME;
	}

	public String getDefaultResPath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath()
				+ Constants.WORKING_DIRECTORY;
	}

	public String getResourcePath() {
		SharedPreferences sp = getSharedPreferences(Constants.PREF_FILE,
				Context.MODE_PRIVATE);
		return sp.getString(Constants.RESOURCE_PATH, getDefaultResPath());
	}

	public ArrayList<String> getFontList() {
		return mFontsPath;
	}
	
	public void setFontList(Collection<? extends String> list) {
		mFontsPath.addAll(list);
	}

	public void setResourcePath(String path) {
		SharedPreferences sp = getSharedPreferences(Constants.PREF_FILE,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(Constants.RESOURCE_PATH, path);
		editor.commit();
	}

	public String getCardImagePath() {
		return getResourcePath() + Constants.CARD_IMAGE_DIRECTORY;
	}
	
	public SharedPreferences getApplicationSettings() {
		return mSettingsPref;
	}

	public boolean getMobileNetworkPref() {
		return mSettingsPref.getBoolean(
				Settings.KEY_PREF_COMMON_NOT_DOWNLOAD_VIA_GPRS, true);
	}

	public int getGameScreenOritation() {
		boolean lockScreen = mSettingsPref.getBoolean(
				Settings.KEY_PREF_GAME_SCREEN_ORIENTATION, true);
		if (lockScreen) {
			return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		} else {
			return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
		}
	}

	public String getDataBasePath() {
		return mDataBasePath;
	}
	
	public String getCompatExternalFilesDir() {
		File path = getExternalFilesDir(null);
		if (path != null) {
			String prefix = Environment.getExternalStorageDirectory().getPath();
			return path.toString().replace(prefix, "/mnt/sdcard");
		} else {
			return "/mnt/sdcard/android/data/cn.garymb.ygomobile/files/";
		}
	}

	public void setLastCheckTime(long time) {
		SharedPreferences sp = getSharedPreferences(Constants.PREF_FILE_COMMON,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putLong(Constants.PREF_KEY_UPDATE_CHECK, time);
		editor.commit();
	}

	public long getLastCheckTime() {
		SharedPreferences sp = getSharedPreferences(Constants.PREF_FILE_COMMON,
				Context.MODE_PRIVATE);
		return sp.getLong(Constants.PREF_KEY_UPDATE_CHECK, 0);
	}

	public String getFontPath() {
		return mSettingsPref.getString(Settings.KEY_PREF_GAME_FONT_NAME, Constants.SYSTEM_FONT_DIR  + Constants.DEFAULT_FONT_NAME);
	}

	public String getLastDeck() {
		SharedPreferences sp = getSharedPreferences(Constants.PREF_FILE_COMMON,
				Context.MODE_PRIVATE);
		return sp.getString(Constants.PREF_KEY_LAST_DECK,
				Constants.DEFAULT_DECK_NAME);
	}

	public void setLastDeck(String name) {
		SharedPreferences sp = getSharedPreferences(Constants.PREF_FILE_COMMON,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(Constants.PREF_KEY_LAST_DECK, name);
		editor.commit();
	}

	public float getScreenHeight() {
		return mScreenHeight;
	}

	public float getScreenWidth() {
		return mScreenWidth;
	}

	public float getSmallerSize() {
		return mScreenHeight < mScreenWidth ? mScreenHeight : mScreenWidth;
	}

	public float getXScale() {
		return (mScreenHeight > mScreenWidth ? mScreenHeight : mScreenWidth) / 1024.0f;
	}

	public float getYScale() {
		return (mScreenHeight > mScreenWidth ? mScreenWidth : mScreenHeight) / 640.0f;
	}

	public float getDensity() {
		return mDensity;
	}

	public int getVersionCode() {
		return mVersionCode;
	}

	public String getVersionName() {
		return mVersionName;
	}
}

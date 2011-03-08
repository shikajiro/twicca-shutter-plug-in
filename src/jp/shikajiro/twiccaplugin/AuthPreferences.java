package jp.shikajiro.twiccaplugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * SharedPreferencesに保存されている認証情報
 * @author shikajiro
 *
 */
public class AuthPreferences {
	private static final String AUTH_KEY = "AUTH_KEY";
	private static final String AUTH_STATUS = "AUTH_STATUS";
	
	/**
	 * Preferencesから認証状態を取得する。
	 * @return
	 */
	public static int getPrefAuthStatus(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return preferences.getInt(AUTH_STATUS, AuthState.NOT_KEY);
	}
	
	/**
	 * Preferencesから認証キーを取得する。
	 * @return
	 */
	public static String getPrefAuthKey(Context context){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return preferences.getString(AUTH_KEY, "");
	}
	
	/**
	 * 現在の認証状態を保存します。
	 * @param status
	 */
	public static void storeStatus(Context context,int status) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = preferences.edit();
		editor.putInt(AUTH_STATUS, status);
		editor.commit();
	}

	/**
	 * 認証キーを保存します。
	 * @param key
	 */
	public static void storeKey(Context context,String key) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = preferences.edit();
		editor.putString(AUTH_KEY, key);
		editor.commit();
	}

}

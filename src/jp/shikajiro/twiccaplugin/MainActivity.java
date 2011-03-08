package jp.shikajiro.twiccaplugin;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * shutter plugin for twicca
 * <ul>
 * <li>twiccaで画像または動画を添付し送信する。</li>
 * <li>shutter.jpログイン画面に遷移する。</li>
 * <li>twitter認証でログインする。</li>
 * <li>twiccaに戻り、認証キー発行画面に遷移する。</li>
 * <li>認証キーをコピー(またはメモ)する。</li>
 * <li>twiccaに戻り、認証キーを入力する。</li>
 * <li>認証後、短縮URLが発行される。</li>
 * <li>認証後は2~6の流れは省略される。</li>
 * </ul>
 * @author shikajiro
 * 
 */
public class MainActivity extends Activity {

	private static final String START_TAG = "START_TAG";
	private static final String ACTIVITY_RESULT_TAG = "ACTIVITY_RESULT_TAG";
	private static final String STATE_TAG = "STATE_TAG";
	private static final String RESPONSE_TAG = "RESPONSE_TAG";

	private static final String KEY_URL = "http://shtt.jp/users/key";
	private static final String UPLOAD_URL = "http://shtt.jp/api/update_v1.json";
	// private static final String UPLOAD_URL = "http://shikajiro.appspot.com/"; //debug server

	@Override
	protected void onStart() {
		super.onStart();

		// ログイン状態の取得
		int loginStatus = AuthPreferences.getPrefAuthStatus(this);
		Log.d(STATE_TAG, "state:"+loginStatus);

		/*
		 * 1. 始めてログインする場合、ログイン画面へ遷移する。 
		 * 2. ログイン状態でkeyを保持していない場合、key取得画面へ遷移する。 
		 * 3. key取得後でkey未入力の場合、key入力画面へ遷移する。 
		 * 4. key入力後はメディア送信処理を行う。
		 */
		switch (loginStatus) {
		case AuthState.NOT_KEY:
			Log.d(START_TAG, "NOT_KEY");
			// 始めてkey認証する状態
			Uri key_uri = Uri.parse(KEY_URL);
			startActivityForResult(new Intent(Intent.ACTION_VIEW, key_uri),
					AuthState.NOT_KEY);
			//TODO webブラウザ側で「keyをコピーして戻るボタン戻ってください」みたいなメッセージを書いてもらう。
			break;
		case AuthState.NOT_KEY_INPUT:
			Log.d(START_TAG, "NOT_KEY_INPUT");
			// key認証から帰ってきた直後の状態
			startActivityForResult(new Intent(this, KeyActivity.class),
					AuthState.NOT_KEY_INPUT);
			break;
		case AuthState.KEY_COMPLETE:
			Log.d(START_TAG, "KEY_COMPLETE");
			// keyの認証設定が終わった状態
			final ProgressDialog dialog = ProgressDialog.show(this, "", "uploading now...", true);
			new Thread(new Runnable() {
				public void run() {
					String key = AuthPreferences.getPrefAuthKey(getApplicationContext());
					Log.d("onStart", "PrefAuthKey:"+key);
					changeMedia2Url(key);
					dialog.dismiss();
					finish();
				}
			}).start();

			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d("onActivityResult", "requestCode:"+requestCode);
		/*
		 * 1. ログイン処理から戻った場合、key未取得状態にする。 
		 * 2. key取得から戻った場合、key発行状態にする。 
		 * 3. key入力から戻った場合、短縮URL生成処理を実行する。
		 */
		switch (requestCode) {
		case AuthState.NOT_KEY:
			Log.d(ACTIVITY_RESULT_TAG, "NOT_KEY");
			AuthPreferences.storeStatus(this, AuthState.NOT_KEY_INPUT);
			break;
		case AuthState.NOT_KEY_INPUT:
			Log.d(ACTIVITY_RESULT_TAG, "NOT_KEY_INPUT");
			int loginStatus = AuthPreferences.getPrefAuthStatus(this);
			if(loginStatus == AuthState.NOT_KEY){
				onStart();
			}else if(loginStatus == AuthState.KEY_COMPLETE){
				final ProgressDialog dialog = ProgressDialog.show(this, "", "uploading now...", true);
				new Thread(new Runnable() {
					public void run() {
						String key = AuthPreferences.getPrefAuthKey(getApplicationContext());
						Log.d("onActivityResult", "PrefAuthKey:"+key);
						changeMedia2Url(key);
						dialog.dismiss();
						finish();
					}
				}).start();
			}

			
			break;
		default:
			break;
		}
	}

	/**
	 * メディアデータをshtt.jpへ送信して短縮URLを取得する。
	 * 
	 * @param key:認証キー
	 */
	private void changeMedia2Url(String key) {
		Log.d("changeMedia2Url", "key:"+key);

		// twiccaから渡されたメディアデータを取得
		InputStream openInputStream = this.getTwiccaMedia();

		// keyを使用してshutter.jpにメディアを送信
		String url = this.generateUrl(key, openInputStream);
		Log.d("changeMedia2Url", "url:"+url);

		// twiccaに短縮URLを設定する。
		this.setTwiccaResult(url);
	}

	/**
	 * twiccaに返却値を設定する。
	 * 
	 * @param url
	 */
	private void setTwiccaResult(String url) {
		// url取得失敗などで空文字の場合、twiccaへは返却しない。(twicca仕様)
		if (url == null || url.length() <= 0) {
			return;
		}
		Intent result = new Intent();
		result.setData(Uri.parse(url));
		setResult(RESULT_OK, result);
	}

	/**
	 * shtt.jpにメディアデータを送信し、短縮URLを取得する。
	 * 
	 * @param key:認証キー
	 * @param openInputStream:メディアデータ
	 * @return 短縮URL
	 */
	private String generateUrl(String key, InputStream inputStream) {
		String url = "";

		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(UPLOAD_URL);
			MultipartEntity entity = new MultipartEntity(
					HttpMultipartMode.BROWSER_COMPATIBLE);
			entity.addPart("key", new StringBody(key, Charset.forName("UTF-8")));
			// TODO メッセージに何を送る？
			entity.addPart("message", new StringBody(
					"shutter plugin sample message", Charset.forName("UTF-8")));
			String type = getIntent().getType();
			// TODO ファイル名をどうする？
			InputStreamBody streamBody = new InputStreamBody(inputStream, type,
					"shutter_plugin_sample.jpg");
			entity.addPart("media", streamBody);
			httppost.setEntity(entity);
			HttpResponse response = httpclient.execute(httppost);

			if (response != null
					&& response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				//responseからjsonを取得し、そこから短縮URLを取得する。
				InputStream is = response.getEntity().getContent();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));
				String responsJson = "";
				String line;
				while ((line = br.readLine()) != null) {
					responsJson += line;
				}
				JSONObject jsonObject = new JSONObject(responsJson);
				url = jsonObject.getJSONObject("image").getString("url");

			} else {
				Log.d(RESPONSE_TAG, "response NG code:"
								+ response.getStatusLine().getStatusCode()
								+ " reason:"
								+ response.getStatusLine().getReasonPhrase());
			}
		} catch (JSONException e) {
			Log.d(RESPONSE_TAG, "JSONException:" + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.d(RESPONSE_TAG, "IOException:" + e.getMessage());
			e.printStackTrace();
		}

		return url;
	}

	/**
	 * twiccaから添付されたメディアデータ（画像or動画）を取得する。
	 * 
	 * @return メディアデータのストリーム
	 */
	private InputStream getTwiccaMedia() {
		InputStream openInputStream = null;
		try {
			Intent receivedIntent = getIntent();
			Uri uri = receivedIntent.getData();
			openInputStream = getContentResolver().openInputStream(uri);
		} catch (FileNotFoundException e) {
			Log.d(RESPONSE_TAG, "FileNotFoundException:" + e.getMessage());
			e.printStackTrace();
		}

		return openInputStream;
	}

}
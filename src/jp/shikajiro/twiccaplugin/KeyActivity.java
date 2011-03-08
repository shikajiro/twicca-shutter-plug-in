package jp.shikajiro.twiccaplugin;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * 認証キー入力画面
 * TODO 背景が透明になってしまっている。
 * @author shikajiro
 *
 */
public class KeyActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.key_input);
		
		View pinButton = findViewById(R.id.PinButton);
		View retryButton = findViewById(R.id.retryButton);
		pinButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//入力された認証キーを保存する。
				TextView text = (TextView)findViewById(R.id.PinText);
				String str = text.getText().toString();
				AuthPreferences.storeKey(getApplicationContext(), str);
				AuthPreferences.storeStatus(getApplicationContext(), AuthState.KEY_COMPLETE);
				finish();
			}
		});
		retryButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//keyを再取得する。
				AuthPreferences.storeStatus(getApplicationContext(), AuthState.NOT_KEY);
				finish();
			}
		});
	}
}

package jp.shikajiro.twiccaplugin;

public class AuthState {
	public static final int NOT_LOGIN = 1; //ログインされていない状態
	public static final int NOT_KEY = 2; //認証キーを発行していない状態
	public static final int NOT_KEY_INPUT = 3; //認証キーを入力していない状態
	public static final int KEY_COMPLETE = 4; //認証キーが完了した状態
}

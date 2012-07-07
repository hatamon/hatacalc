package jp.hatamonkensetsu.software.android.calclib;

public class WordToken extends Token {
	public final String m_String;

	public WordToken(Tags i_Tags, String i_String) {
		super(i_Tags);
		m_String = new String(i_String);
	}
}

package jp.hatamonkensetsu.software.android.calclib;

public class StringStream implements Stream {
	private StringBuffer m_String = new StringBuffer();
//	@Override
	public char read() {
		char result = 0;
		if ( m_String.length() > 0 ) {
			result = m_String.charAt(0);
			m_String.delete(0,1);
		}
		return result;
	}

	public void append(String i_String) {
		m_String.append(i_String);
	}

	public StringBuffer getCopyOfBuffer() {
		return new StringBuffer(m_String);
	}
}

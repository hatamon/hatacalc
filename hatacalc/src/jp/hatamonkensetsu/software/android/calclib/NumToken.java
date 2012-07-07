package jp.hatamonkensetsu.software.android.calclib;

import java.math.BigDecimal;

public class NumToken extends Token {
	public final BigDecimal m_Value;

	public NumToken(BigDecimal i_Value) {
		super(Tags.NUM);
		m_Value = i_Value;
	}
}

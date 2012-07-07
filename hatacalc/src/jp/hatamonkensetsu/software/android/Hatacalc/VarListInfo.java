package jp.hatamonkensetsu.software.android.Hatacalc;

import java.math.BigDecimal;

public class VarListInfo {
	private String m_VarName = "";
	private BigDecimal m_Value;

	public VarListInfo(String i_VarName, BigDecimal i_Value) {
		setVarName(i_VarName);
		setValue(i_Value);
	}
	
	public void setVarName(String i_VarName) {
		this.m_VarName = i_VarName;
	}
	public String getVarName() {
		return m_VarName;
	}

	public void setValue(BigDecimal i_Value) {
		this.m_Value = i_Value;
	}
	public BigDecimal getValue() {
		return m_Value;
	}
}

package jp.hatamonkensetsu.software.android.Hatacalc;

public class FuncListInfo {
	private String m_FuncArgs = "";
	private String m_FuncName = "";
	private String m_FuncExpression = "";

	public FuncListInfo(String i_FuncName, String i_FuncArgs, String i_FuncExpression) {
		setFuncArgs(i_FuncArgs);
		setFuncName(i_FuncName);
		setFuncExpression(i_FuncExpression);
	}
	
	public void setFuncArgs(String i_FuncArgs) {
		this.m_FuncArgs = i_FuncArgs;
	}
	public String getFuncArgs() {
		return m_FuncArgs;
	}

	public void setFuncName(String i_FuncName) {
		this.m_FuncName = i_FuncName;
	}
	public String getFuncName() {
		return m_FuncName;
	}

	public void setFuncExpression(String i_FuncExpression) {
		this.m_FuncExpression = i_FuncExpression;
	}
	public String getFuncExpression() {
		return m_FuncExpression;
	}
}

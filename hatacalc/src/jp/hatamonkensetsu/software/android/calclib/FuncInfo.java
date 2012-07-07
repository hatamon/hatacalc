package jp.hatamonkensetsu.software.android.calclib;

import java.util.ArrayList;

public class FuncInfo {
	public ArrayList<String> m_ArgNames = new ArrayList<String>();
	public IUserFunc m_Executor;
	public String m_ExecExpression = new String();
	public boolean m_IsBuiltIn;
	public FuncInfo() {
		m_ArgNames.clear();
		m_IsBuiltIn = false;
	}
}

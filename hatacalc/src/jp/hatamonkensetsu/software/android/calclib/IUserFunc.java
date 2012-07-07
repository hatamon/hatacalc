package jp.hatamonkensetsu.software.android.calclib;

import java.math.BigDecimal;
import java.util.ArrayList;

public interface IUserFunc {
	public class Param {
		public static final int NUMVALUE = 0;
		public static final int STRINGVALUE = 1;
		public int m_Type = NUMVALUE;
		public BigDecimal m_NumValue;
		public String m_StringValue;
	}
	
	public class Result {
		public BigDecimal value;
		public boolean isOk;
		public String errorMessage;
	}

	public Result exec(String i_FuncName, ArrayList<String> i_ArgNames, ArrayList<Param> i_Params, String i_ExecExpression);
}

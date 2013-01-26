package jp.hatamonkensetsu.software.android.calclib;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.math.BigDecimal;

import jp.hatamonkensetsu.software.android.Hatacalc.FuncListInfo;
import jp.hatamonkensetsu.software.android.Hatacalc.VarListInfo;

public class Parser {
	public class UserDefFuncExecutor implements IUserFunc {

		public Result exec(String i_FuncName, ArrayList<String> i_ArgNames, ArrayList<Param> i_Params, String i_ExecExpression) {
			final Result result = new Result();
			result.value = new BigDecimal("0.0");
			String dispFuncName = i_FuncName;

			if(!m_CallHist.containsKey(i_FuncName)) m_CallHist.put(i_FuncName, 0);

			if(m_CallHist.get(i_FuncName) != 0) {
				result.errorMessage = new String(dispFuncName + "() error: Recursive call");
				result.isOk = false;
				return result;
			}

			m_CallHist.put(i_FuncName, m_CallHist.get(i_FuncName) + 1);

			
			if(i_Params.size() != i_ArgNames.size()) {
				result.errorMessage = new String(dispFuncName + "() : number of arg error");
				result.isOk = false;
				m_CallHist.put(i_FuncName, m_CallHist.get(i_FuncName) - 1);
				return result;
			}
			
			for(int i = 0; i < i_Params.size(); i++) {
				if(i_Params.get(i).m_Type != IUserFunc.Param.NUMVALUE)
				{
					result.errorMessage = new String(dispFuncName + "() : arg(" + i + ") error");
					result.isOk = false;
					m_CallHist.put(i_FuncName, m_CallHist.get(i_FuncName) - 1);
					return result;
				}
			}

			try {
				if( i_ExecExpression.compareTo("") != 0) {
					Parser parser = new Parser();
					String expression = new String();
					
					for(int i = 0; i < i_Params.size(); i++) {
						expression += i_ArgNames.get(i) + " = " + getDisplayValueString(i_Params.get(i).m_NumValue) + ";";
					}
					expression += i_ExecExpression;

					if(parser.parse(expression)) {
						result.value = parser.getResult();
					}
					else {
						result.errorMessage = parser.getErrorMessage();
						if(result.errorMessage.compareTo("") == 0) {
							result.errorMessage = new String(dispFuncName + "() : exec error");
						}
						result.isOk = false;
						m_CallHist.put(i_FuncName, m_CallHist.get(i_FuncName) - 1);
						return result;
					}
				}
				else {
					result.errorMessage = new String(dispFuncName + "() : no expression");
					result.isOk = false;
					m_CallHist.put(i_FuncName, m_CallHist.get(i_FuncName) - 1);
					return result;
				}
			} catch(Exception e) {
				result.errorMessage = new String(dispFuncName + "() : error");
				result.isOk = false;
				m_CallHist.put(i_FuncName, m_CallHist.get(i_FuncName) - 1);
				return result;
			}
			
			result.isOk = true;

			m_CallHist.put(i_FuncName, m_CallHist.get(i_FuncName) - 1);
			return result;
		}
	}

	private HashMap<String, BigDecimal> m_IdValues = new HashMap<String, BigDecimal>();
	private Lex m_Lex = null;
	private Lex m_MainLex = new Lex();
	private String m_originalExpression;
	private Token m_Token = null;
	private ArrayList<BigDecimal> m_Stack = new ArrayList<BigDecimal>();
	private String m_ErrorMessage = null;
	private boolean m_ParseOK = false;
	private static HashMap<String, FuncInfo> m_mapUserFunc = new HashMap<String, FuncInfo>();
	private static HashMap<String, String> m_mapKigoChangeTable = new HashMap<String, String>();
	private BigDecimal m_Result = null;
	private UserDefFuncExecutor m_Executor = new UserDefFuncExecutor();
	private static HashMap<String, Integer> m_CallHist = new HashMap<String, Integer>();
	
	public Parser() {
		m_ParseOK = false;
		m_Result = new BigDecimal("0.0");
		m_Lex = m_MainLex;
	}

	public BigDecimal getResult() {
		return m_Result;
	}
	
	public void clearResult() {
		m_Result = new BigDecimal("0.0");
	}

	public void setKigoChange(String i_Kigo, String i_Change) {
		m_mapKigoChangeTable.put(i_Kigo, i_Change);
	}

	private String changeKigo(String i_String) {
		String result = new String(i_String);
		for(Iterator<String> it = m_mapKigoChangeTable.keySet().iterator(); it.hasNext(); ) {
			String kigo = it.next();
			result = result.replaceAll(kigo, m_mapKigoChangeTable.get(kigo));
		}
		return result;
	}
	
	public boolean parse(String i_String) {
		boolean parseOK = true;
		m_originalExpression = i_String;
		String expression = changeKigo(m_originalExpression);
		m_Stack.clear();
		
		parseOK = lowparse(expression);

//for(int i = 0; i < m_Stack.size(); i++) {
//	System.out.print("-- " + pop().toString());
//}
		if(!parseOK) clearResult();
		
		return parseOK;
	}

	public boolean lowparse(String i_String) {
		boolean parseOK = true;
		StringStream stream = new StringStream();
		stream.append(i_String);
		m_Lex.init(stream);

		if(i_String.trim().compareTo("") != 0) {
			m_Token = m_Lex.scan();
			while(parseOK && (m_Token.m_Tag != Tags.END) && (m_Token.m_Tag != Tags.UNKNOWN))
			{
				m_ErrorMessage = null;
				parseOK = parseS();
				if(parseOK) {
					m_Result = pop();
					setIdValue("Ans", getResult());
				}
				if(m_Token.m_Tag == Tags.DELIMITER) {
					while(m_Token.m_Tag == Tags.DELIMITER) {
						m_Token = m_Lex.scan();
					}
				}
				else {
					if(m_Token.m_Tag != Tags.END) {
						parseOK = false;
					}
				}
			}
		}		
		m_ParseOK = parseOK;
		return parseOK;
	}

	public boolean hasError() {
		return !m_ParseOK;
	}

	public String getErrorMessage() {
		if(m_ErrorMessage != null) {
			return m_ErrorMessage;
		}
		else {
			return "Error!";
		}
	}

	public void setUserFunc(String i_Name, FuncInfo i_FuncInfo) {
		m_Lex.addUserFunc(i_Name);
		m_mapUserFunc.put(i_Name, i_FuncInfo);
	}

	public boolean isBuiltInFunc(String i_Name) {
		if(hasUserFunc(i_Name)) return m_mapUserFunc.get(i_Name).m_IsBuiltIn;
		else return false;
	}

	public boolean hasUserFunc(String i_Name) {
		return m_mapUserFunc.containsKey(i_Name);
	}

	public void delUserFunc(String i_Name) {
		if(hasUserFunc(i_Name)) {
			m_Lex.delUserFunc(i_Name);
			m_mapUserFunc.remove(i_Name);
		}
	}

	private void push(BigDecimal i_Value) {
		m_Stack.add( i_Value );
	}
	private BigDecimal pop() {
		int pos = m_Stack.size() - 1;
		BigDecimal ret = m_Stack.get(pos);
		m_Stack.remove(pos);
		return ret;
	}
	private BigDecimal peek() {
		int pos = m_Stack.size() - 1;
		return m_Stack.get(pos);
	}

	private boolean parseS() {
		boolean parseOK = true;

		if( (m_Token.m_Tag == Tags.ID) && m_Lex.isNext(Tags.ASSIGN)) {
			String id = new String(((WordToken)m_Token).m_String);
			m_Lex.scan(); // ASSIGN “Ç‚Ý”ò‚Î‚µ
			m_Token = m_Lex.scan();
			parseOK = parseS();
			if(parseOK) {
				setIdValue(id, peek());
			}
		}
		else if( m_Token.m_Tag == Tags.FUNCDEF ) {
			m_Token = m_Lex.scan();
			parseOK = parseFuncDef();
		}
		else if( m_Token.m_Tag == Tags.FUNCUNDEF ) {
			m_Token = m_Lex.scan();
			parseOK = parseFuncUndef();
		}
		else {
			parseOK = parseE0();
		}
		
		return parseOK;
	}

	public void setIdValue(String i_ID, BigDecimal i_Val) {
		m_IdValues.put(i_ID, i_Val);
	}

	public void delIdValue(String i_ID) {
		if(m_IdValues.containsKey(i_ID)) m_IdValues.remove(i_ID);
	}

	boolean parseFuncDef() {
		ArrayList<String> argnames = new ArrayList<String>();
		if((m_Token.m_Tag == Tags.ID) || (m_Token.m_Tag == Tags.USERFUNC)) {
			String funcName = new String(((WordToken)m_Token).m_String);
			if(m_IdValues.containsKey(funcName)) {
				m_ErrorMessage = new String(funcName + " is variable");
				return false;
			}
			if(isBuiltInFunc(funcName)) {
				m_ErrorMessage = new String(funcName + " : Built-in func");
				return false;
			}
			m_Token = m_Lex.scan();
			if(m_Token.m_Tag == Tags.LPAREN) {
				m_Token = m_Lex.scan();
				if(m_Token.m_Tag == Tags.ID) {
					argnames.add(new String(((WordToken)m_Token).m_String));
					m_Token = m_Lex.scan();
					while(m_Token.m_Tag == Tags.COMMA) {
						m_Token = m_Lex.scan();
						if(m_Token.m_Tag == Tags.ID) {
							argnames.add(new String(((WordToken)m_Token).m_String));
							m_Token = m_Lex.scan();
						}
						else {
							break;
						}
					}
					for(int i = argnames.size()-1; i > 0; i--) {
						if(argnames.indexOf(argnames.get(i)) != i) {
							m_ErrorMessage = new String(argnames.get(i) + " : duplicate name");
							return false;
						}
					}
					if(argnames.indexOf(funcName) != -1) {
						m_ErrorMessage = new String(funcName + " : duplicate name");
						return false;
					}
				}
				if(m_Token.m_Tag == Tags.RPAREN) {
					m_Token = m_Lex.scan();
					if(m_Token.m_Tag == Tags.ASSIGN) {
						String expression = new String(m_originalExpression.substring(m_originalExpression.indexOf('=')+1));
						m_Token = m_Lex.scan();
						if(expression.compareTo("") != 0) {
							FuncInfo info = new FuncInfo();
							info.m_Executor = m_Executor;
							info.m_ArgNames = argnames;
							info.m_ExecExpression = expression;
							setUserFunc(funcName, info);
							m_ErrorMessage = new String("OK");
						}
					}
				}
				else {
					m_ErrorMessage = new String("invalid argname");
					return false;
				}
			}
		}
		else {
			m_ErrorMessage = new String("invalid funcname");
			return false;
		}

		return false;
	}

	boolean parseFuncUndef() {
		boolean parseOK = false;
		if(m_Token.m_Tag == Tags.USERFUNC) {
			if(isBuiltInFunc(((WordToken)m_Token).m_String)) {
				m_ErrorMessage = new String(((WordToken)m_Token).m_String + " : Built-in func");
				return false;
			}
			delUserFunc(((WordToken)m_Token).m_String);
			m_ErrorMessage = new String("OK");
		}
		else {
			m_ErrorMessage = new String(((WordToken)m_Token).m_String + " : not func");
		}

		return parseOK;
	}

	boolean parseE0() {
		boolean parseOK = true;
		parseOK = parseE1();
		if(parseOK) parseOK = parseE0a();
		return parseOK;
	}

	boolean parseE0a() {
		boolean parseOK = true;
		while ( parseOK && (m_Token.m_Tag == Tags.L_OR) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE1();
			if(parseOK) {
				long d2 = pop().longValue();
				long d1 = pop().longValue();
				switch ( prevTag ) {
					case	L_OR:
						push(new BigDecimal( ((d1 != 0) || (d2 != 0)) ? 1 : 0 ));
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE1() {
		boolean parseOK = true;
		parseOK = parseE2();
		if(parseOK) parseOK = parseE1a();
		return parseOK;
	}

	boolean parseE1a() {
		boolean parseOK = true;
		while ( parseOK && (m_Token.m_Tag == Tags.L_AND) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE2();
			if(parseOK) {
				long d2 = pop().longValue();
				long d1 = pop().longValue();
				switch ( prevTag ) {
					case	L_AND:
						push(new BigDecimal( ((d1 != 0) && (d2 != 0)) ? 1 : 0 ));
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE2() {
		boolean parseOK = true;
		parseOK = parseE3();
		if(parseOK) parseOK = parseE2a();
		return parseOK;
	}

	boolean parseE2a() {
		boolean parseOK = true;
		while ( parseOK && (m_Token.m_Tag == Tags.OR) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE3();
			if(parseOK) {
				long d2 = pop().longValue();
				long d1 = pop().longValue();
				switch ( prevTag ) {
					case	OR:
						push(new BigDecimal( d1 | d2));
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE3() {
		boolean parseOK = true;
		parseOK = parseE4();
		if(parseOK) parseOK = parseE3a();
		return parseOK;
	}

	boolean parseE3a() {
		boolean parseOK = true;
		while ( parseOK && (m_Token.m_Tag == Tags.AND) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE4();
			if(parseOK) {
				long d2 = pop().longValue();
				long d1 = pop().longValue();
				switch ( prevTag ) {
					case	AND:
						push(new BigDecimal( d1 & d2));
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE4() {
		boolean parseOK = true;
		parseOK = parseE5();
		if(parseOK) parseOK = parseE4a();
		return parseOK;
	}

	boolean parseE4a() {
		boolean parseOK = true;
		while ( parseOK && (m_Token.m_Tag == Tags.XOR) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE5();
			if(parseOK) {
				long d2 = pop().longValue();
				long d1 = pop().longValue();
				switch ( prevTag ) {
					case	XOR:
						push(new BigDecimal( d1 ^ d2));
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE5() {
		boolean parseOK = true;
		parseOK = parseE6();
		if(parseOK) parseOK = parseE5a();
		return parseOK;
	}

	boolean parseE5a() {
		boolean parseOK = true;
		while ( parseOK && ( (m_Token.m_Tag == Tags.EQ) || (m_Token.m_Tag == Tags.NE) ) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE6();
			if(parseOK) {
				BigDecimal d2 = pop();
				BigDecimal d1 = pop();
				switch ( prevTag ) {
					case	EQ:
						push( new BigDecimal((d1.compareTo(d2) == 0)?1:0) );
						break;
					case	NE:
						push( new BigDecimal((d1.compareTo(d2) != 0)?1:0) );
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE6() {
		boolean parseOK = true;
		parseOK = parseE7();
		if(parseOK) parseOK = parseE6a();
		return parseOK;
	}

	boolean parseE6a() {
		boolean parseOK = true;
		while ( parseOK && ( (m_Token.m_Tag == Tags.LT) || (m_Token.m_Tag == Tags.GT) || (m_Token.m_Tag == Tags.LE) || (m_Token.m_Tag == Tags.GE) ) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE7();
			if(parseOK) {
				BigDecimal d2 = pop();
				BigDecimal d1 = pop();
				switch ( prevTag ) {
					case	LT:
						push( new BigDecimal((d1.compareTo(d2) < 0)?1:0) );
						break;
					case	LE:
						push( new BigDecimal((d1.compareTo(d2) <= 0)?1:0) );
						break;
					case	GE:
						push( new BigDecimal((d1.compareTo(d2) >= 0)?1:0) );
						break;
					case	GT:
						push( new BigDecimal((d1.compareTo(d2) > 0)?1:0) );
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE7() {
		boolean parseOK = true;
		parseOK = parseE8();
		if(parseOK) parseOK = parseE7a();
		return parseOK;
	}

	boolean parseE7a() {
		boolean parseOK = true;
		while ( parseOK && ( (m_Token.m_Tag == Tags.LSHIFT) || (m_Token.m_Tag == Tags.RSHIFT) ) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE8();
			if(parseOK) {
				long d2 = pop().longValue();
				long d1 = pop().longValue();
				switch ( prevTag ) {
					case	LSHIFT:
						push(new BigDecimal(d1 << d2));
						break;
					case	RSHIFT:
						push(new BigDecimal(d1 >> d2));
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE8() {
		boolean parseOK = true;
		parseOK = parseE9();
		if(parseOK) parseOK = parseE8a();
		return parseOK;
	}

	boolean parseE8a() {
		boolean parseOK = true;
		while ( parseOK && ( (m_Token.m_Tag == Tags.ADD) || (m_Token.m_Tag == Tags.SUB) ) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE9();
			if(parseOK) {
				BigDecimal d2 = pop();
				BigDecimal d1 = pop();
				switch ( prevTag ) {
					case	ADD:
						push( d1.add(d2) );
						break;
					case	SUB:
						push( d1.subtract(d2) );
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE9() {
		boolean parseOK = true;
		parseOK = parseE10();
		if(parseOK) parseOK = parseE9a();
		return parseOK;
	}

	boolean parseE9a() {
		boolean parseOK = true;
		while ( parseOK && ( (m_Token.m_Tag == Tags.MUL) || (m_Token.m_Tag == Tags.DIV) || (m_Token.m_Tag == Tags.MOD) || (m_Token.m_Tag == Tags.ID) || (m_Token.m_Tag == Tags.LPAREN) || (m_Token.m_Tag == Tags.PI) || (m_Token.m_Tag == Tags.E) || (m_Token.m_Tag == Tags.USERFUNC) ) ) {
			Tags prevTag = m_Token.m_Tag;
			if ( (m_Token.m_Tag == Tags.MUL) || (m_Token.m_Tag == Tags.DIV) || (m_Token.m_Tag == Tags.MOD) ) {
				m_Token = m_Lex.scan();
				parseOK = parseE10();
			}
			else
			{
				parseOK = parseElast();
			}
			if(parseOK) {
				BigDecimal d2 = pop();
				BigDecimal d1 = pop();
				switch ( prevTag ) {
					case	MUL:
					case	ID:
					case	LPAREN:
					case	PI:
					case	E:
					case	USERFUNC:
						push( d1.multiply(d2) );
						break;
					case	DIV:
						if(d2.doubleValue() == 0.0) {
							m_ErrorMessage = new String("divided by zero!");
							parseOK = false;
						}
						else
						{
							push( d1.divide(d2, 20, BigDecimal.ROUND_HALF_UP) );
						}
						break;
					case	MOD:
						if(d2.doubleValue() == 0.0) {
							m_ErrorMessage = new String("divided by zero!");
							parseOK = false;
						}
						else
						{
							push( d1.remainder(d2) );
						}
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE10() {
		boolean parseOK = true;
		parseOK = parseE11();
		if(parseOK) parseOK = parseE10a();
		return parseOK;
	}

	boolean parseE10a() {
		boolean parseOK = true;
		while ( parseOK && ( (m_Token.m_Tag == Tags.PERMUTATION) || (m_Token.m_Tag == Tags.COMBINATION) ) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE11();
			if(parseOK) {
				BigDecimal d2 = pop();
				BigDecimal d1 = pop();
				switch ( prevTag ) {
					case	PERMUTATION:
						if(d1.subtract(d2).doubleValue() < 0.0) {
							push(new BigDecimal("0"));
						}
						else {
							push( factorial(d1).divide(factorial(d1.subtract(d2))) );
						}
						break;
					case	COMBINATION:
						if(d1.subtract(d2).doubleValue() < 0.0) {
							push(new BigDecimal("0"));
						}
						else {
							push( factorial(d1).divide(factorial(d2).multiply(factorial(d1.subtract(d2)))) );
						}
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE11() {
		boolean parseOK = true;
		parseOK = parseE12();
		if(parseOK) parseOK = parseE11a();
		return parseOK;
	}

	boolean parseE11a() {
		boolean parseOK = true;
		while ( parseOK && ( (m_Token.m_Tag == Tags.POWER) ) ) {
			Tags prevTag = m_Token.m_Tag;
			m_Token = m_Lex.scan();
			parseOK = parseE12();
			if(parseOK) {
				BigDecimal d2 = pop();
				BigDecimal d1 = pop();
				switch ( prevTag ) {
					case	POWER:
						push( new BigDecimal(Math.pow(d1.doubleValue(), d2.doubleValue())));
						break;
					default:
						parseOK = false;
						break;
				}
			}
		}
		return parseOK;
	}

	boolean parseE12() {
		boolean parseOK = true;
		parseOK = parseElast();
		if(parseOK) parseOK = parseE12a();
		return parseOK;
	}

	boolean parseE12a() {
		boolean parseOK = true;
		while ( parseOK && ( (m_Token.m_Tag == Tags.FACTORIAL) ) ) {
			BigDecimal d1 = pop();
			switch ( m_Token.m_Tag ) {
				case	FACTORIAL:
					if(d1.doubleValue() < 0.0) {
						m_ErrorMessage = new String("n! : Illegal value");
						parseOK = false;
					}
					else {
						push( factorial(d1) );
						m_Token = m_Lex.scan();
					}
					break;
				default:
					parseOK = false;
					break;
			}
		}
		return parseOK;
	}

	boolean parseElast() {
		boolean parseOK = true;
		switch(m_Token.m_Tag) {
			case	ID:
				{
					String id = ((WordToken)m_Token).m_String;
					BigDecimal value = m_IdValues.get(id);
					if(value != null) {
						push(value);
						m_Token = m_Lex.scan();
					}
					else {
						m_ErrorMessage = new String(id + " : unknown variable");
						parseOK = false;
					}
				}
				break;
			case	NUM:
				push(((NumToken)m_Token).m_Value);
				m_Token = m_Lex.scan();
				break;
			case	LPAREN:
				m_Token = m_Lex.scan();
				parseOK = parseS();
				if(parseOK) {
					if(m_Token.m_Tag != Tags.RPAREN) {
						parseOK = false;
					}
					else
					{
						m_Token = m_Lex.scan();
					}
				}
				break;
			case	ADD:
				m_Token = m_Lex.scan();
				parseOK = parseElast();
				if(parseOK) {
					push(pop().plus());
				}
				break;
			case	SUB:
				m_Token = m_Lex.scan();
				parseOK = parseElast();
				if(parseOK) {
					push(pop().negate());
				}
				break;
			case	TRUE:
				push(new BigDecimal(1.0));
				m_Token = m_Lex.scan();
				break;
			case	FALSE:
				push(new BigDecimal(0.0));
				m_Token = m_Lex.scan();
				break;
			case	PI:
				push(new BigDecimal(Math.PI));
				m_Token = m_Lex.scan();
				break;
			case	E:
				push(new BigDecimal(Math.E));
				m_Token = m_Lex.scan();
				break;
			case	NOT:
				m_Token = m_Lex.scan();
				parseOK = parseElast();
				if(parseOK) {
					push(new BigDecimal((~(pop().longValue()))));
				}
				break;
			case	L_NOT:
				m_Token = m_Lex.scan();
				parseOK = parseS();
				if(parseOK) {
					push(new BigDecimal((pop().doubleValue() != 0.0)?0:1));
				}
				break;
			case	USERFUNC:
			{
				ArrayList<IUserFunc.Param> aParams = new ArrayList<IUserFunc.Param>();
				String userfuncname = ((WordToken)m_Token).m_String;
				m_Token = m_Lex.scan();

				if(m_Token.m_Tag == Tags.LPAREN)	m_Token = m_Lex.scan();
				else								parseOK = false;

				if(parseOK) {
					if(m_Token.m_Tag == Tags.RPAREN) {
						m_Token = m_Lex.scan();
					}
					else {
						while(true) {
							if(m_Token.m_Tag == Tags.STRING) {
								IUserFunc.Param param = new IUserFunc.Param();
								param.m_Type = IUserFunc.Param.STRINGVALUE;
								param.m_StringValue = ((WordToken)m_Token).m_String;
								aParams.add(param);
								m_Token = m_Lex.scan();
							}
							else {
								parseOK = parseS();
								if(parseOK) {
									IUserFunc.Param param = new IUserFunc.Param();
									param.m_Type = IUserFunc.Param.NUMVALUE;
									param.m_NumValue = pop();
									aParams.add(param);
								}
							}
							if(parseOK) {
								if(m_Token.m_Tag == Tags.COMMA) {
									m_Token = m_Lex.scan();
									continue;
								}
								else if(m_Token.m_Tag == Tags.RPAREN) {
									m_Token = m_Lex.scan();
									break;
								}
								else {
									parseOK = false;
									break;
								}
							}
							else {
								break;
							}
						}
					}
				}
				if(parseOK) {
					BigDecimal retValue = new BigDecimal("0.0");
					IUserFunc executor = m_mapUserFunc.get(userfuncname).m_Executor;
					if(executor != null) {
						IUserFunc.Result result;
						result = executor.exec(userfuncname, m_mapUserFunc.get(userfuncname).m_ArgNames, aParams, m_mapUserFunc.get(userfuncname).m_ExecExpression);
						retValue = result.value;
						m_ErrorMessage = result.errorMessage;
						parseOK = result.isOk;
					}
					if(parseOK) {
						push(retValue);
					}
				}
				break;
			}
			case	LOOP:
			{
				String expression = "";
				long loopcount = 0;
				m_Token = m_Lex.scan();

				if(m_Token.m_Tag == Tags.LPAREN)	m_Token = m_Lex.scan();
				else								parseOK = false;

				if(parseOK) {
					parseOK = parseS();
				}
				if(parseOK) {
					if(m_Token.m_Tag != Tags.COMMA) {
						parseOK = false;
					}
					else
					{
						loopcount = pop().longValue();
						m_Token = m_Lex.scan();
					}
				}
				if(parseOK) {
					if(m_Token.m_Tag != Tags.STRING) {
						parseOK = false;
					}
					else
					{
						expression = ((WordToken)m_Token).m_String;
						m_Token = m_Lex.scan();
					}
				}
				if(parseOK) {
					if(m_Token.m_Tag != Tags.RPAREN) {
						parseOK = false;
					}
					else
					{
						m_Token = m_Lex.scan();
					}
				}
				if(parseOK) {
					Lex lex = new Lex();
					Lex prevlex = m_Lex;
					Token prevtoken = m_Token;
					Iterator<String> it = m_mapUserFunc.keySet().iterator();
					while(it.hasNext()) {
						lex.addUserFunc(it.next());
					}
					m_Lex = lex;
					for(long i = 0; parseOK && (i < loopcount); i++) {
						parseOK = lowparse(expression);
					}
					m_Lex = prevlex;
					m_Token = prevtoken;
				}
				if(parseOK) {
					push(getResult());
				}
				break;
			}
			case	IF:
			{
				String expression1 = "";
				String expression2 = "";
				boolean condition = false;
				m_Token = m_Lex.scan();

				if(m_Token.m_Tag == Tags.LPAREN)	m_Token = m_Lex.scan();
				else								parseOK = false;

				if(parseOK) {
					parseOK = parseS();
				}
				if(parseOK) {
					if(m_Token.m_Tag != Tags.COMMA) {
						parseOK = false;
					}
					else
					{
						condition = (pop().longValue() != 0.0);
						m_Token = m_Lex.scan();
					}
				}
				if(parseOK) {
					if(m_Token.m_Tag != Tags.STRING) {
						parseOK = false;
					}
					else
					{
						expression1 = ((WordToken)m_Token).m_String;
						m_Token = m_Lex.scan();
					}
				}
				if(parseOK) {
					if(m_Token.m_Tag == Tags.COMMA) {
						m_Token = m_Lex.scan();
						if(m_Token.m_Tag != Tags.STRING) {
							parseOK = false;
						}
						else
						{
							expression2 = ((WordToken)m_Token).m_String;
							m_Token = m_Lex.scan();
						}
					}
				}
				if(parseOK) {
					if(m_Token.m_Tag != Tags.RPAREN) {
						parseOK = false;
					}
					else
					{
						m_Token = m_Lex.scan();
					}
				}
				if(parseOK) {
					Lex lex = new Lex();
					Lex prevlex = m_Lex;
					Token prevtoken = m_Token;
					Iterator<String> it = m_mapUserFunc.keySet().iterator();
					while(it.hasNext()) {
						lex.addUserFunc(it.next());
					}
					m_Lex = lex;
					if(condition) {
						parseOK = lowparse(expression1);
					}
					else {
						if(expression2 != "") {
							parseOK = lowparse(expression2);
						}
						else {
							parseOK = lowparse("0");
						}
					}
					m_Lex = prevlex;
					m_Token = prevtoken;
				}
				if(parseOK) {
					push(getResult());
				}
				break;
			}
			default:
				parseOK = false;
				break;
		}
		return parseOK;
	}
	
	BigDecimal factorial(BigDecimal i_Value) {
		long val = i_Value.longValue();
		BigDecimal result = new BigDecimal("1");
		BigDecimal zero = new BigDecimal("0");
		BigDecimal one = new BigDecimal("1");
		if(val != 1) {
			BigDecimal i;;
			for(i = new BigDecimal(val); i.compareTo(zero) > 0; i = i.subtract(one)) {
				result = result.multiply(i);
			}
		}
		return result;
	}
	
	public HashMap<String, BigDecimal> getIdValuesList() {
		return m_IdValues;
	}

	public HashMap<String, FuncInfo> getUserFuncList() {
		return m_mapUserFunc;
	}
	
	public ArrayList<FuncListInfo> getUserFuncInfoList() {
		HashMap<String, FuncInfo> funcs = getUserFuncList();
		ArrayList<FuncListInfo> userFuncs = new ArrayList<FuncListInfo>();
		for(Iterator<String> it = funcs.keySet().iterator(); it.hasNext(); ) {
			String funcname = it.next();
			String args = "(";
			
			FuncInfo info = funcs.get(funcname);
			if(info.m_IsBuiltIn) continue;
			
			if(funcs.get(funcname).m_ArgNames.size() > 0) args += funcs.get(funcname).m_ArgNames.get(0);
			for(int arg = 1; arg < funcs.get(funcname).m_ArgNames.size(); arg++) {
				args += "," + funcs.get(funcname).m_ArgNames.get(arg);
			}
			args += ")";
			userFuncs.add(new FuncListInfo(funcname, args, funcs.get(funcname).m_ExecExpression));
		}
		return userFuncs;
	}

	public ArrayList<VarListInfo> getVarInfoList() {
		HashMap<String, BigDecimal> idValuesList = getIdValuesList();
		ArrayList<VarListInfo> vars = new ArrayList<VarListInfo>();
		for(Iterator<String> it = idValuesList.keySet().iterator(); it.hasNext(); ) {
			String id = it.next();
			BigDecimal val = idValuesList.get(id);
			
			vars.add(new VarListInfo(id, val));
		}
		return vars;
	}

	public static String getDisplayValueString(BigDecimal i_value) {
		String numstring = "";
		try{
			DecimalFormat df1 = new DecimalFormat("######.####################");
			numstring = df1.format(i_value.doubleValue());
		}catch(Exception e){}
		return numstring;
	}

	public static String getDisplayValueStringWithSeparator(BigDecimal i_value) {
		String numstring = "";
		try{
			DecimalFormat df1 = new DecimalFormat("###,###.####################");
			numstring = df1.format(i_value.doubleValue());
		}catch(Exception e){}
		return numstring;
	}
}

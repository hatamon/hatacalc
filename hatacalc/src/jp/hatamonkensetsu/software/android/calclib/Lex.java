package jp.hatamonkensetsu.software.android.calclib;

import java.util.HashMap;
import java.math.BigDecimal;
import java.math.BigInteger;

public class Lex {
	private char m_Peek = ' ';
	private int m_LineNum;
	private static HashMap<String, WordToken> m_Words = new HashMap<String, WordToken>();
	private int m_CharClassTable[] = new int[256];
	private static final int OTHER = 0;
	private static final int LETTER = 1;
	private static final int NUM = 2;
	private static final int SPACE = 3;
	private static final int CR = 4;
	private Stream m_Stream;
	private Token m_CurToken = null;
	private Token m_NextToken = null;
	
	private void reserve(WordToken i_Word) {
		m_Words.put(i_Word.m_String, i_Word);
	}

	public Lex() {
		makeCharClassTable();

		reserve(new WordToken(Tags.TRUE, "true"));
		reserve(new WordToken(Tags.FALSE, "false"));
		reserve(new WordToken(Tags.LOOP, "loop"));
		reserve(new WordToken(Tags.IF, "if"));
		reserve(new WordToken(Tags.PI, "pi"));
		reserve(new WordToken(Tags.E, "e"));
		reserve(new WordToken(Tags.PERMUTATION, "P"));
		reserve(new WordToken(Tags.COMBINATION, "C"));
		reserve(new WordToken(Tags.MOD, "mod"));
		reserve(new WordToken(Tags.FUNCDEF, "def"));
		reserve(new WordToken(Tags.FUNCUNDEF, "undef"));
	}

	public void addUserFunc(String i_Name) {
		reserve(new WordToken(Tags.USERFUNC, new String(i_Name)));
	}

	public void delUserFunc(String i_Name) {
		m_Words.remove(i_Name);
	}

	public void init(Stream i_Stream) {
		m_Peek = ' ';
		m_Stream = i_Stream;
		m_LineNum = 0;
		m_CurToken = null;
		m_NextToken = null;
	}

	public boolean isNext(Tags i_Tag) {
		if(m_NextToken == null) return false;
		else if(m_NextToken.m_Tag == i_Tag) return true;
		else return false;
	}
	
	public Token scan() {
		if(m_NextToken == null) {
			m_CurToken = lowscan();
		}
		else {
			m_CurToken = m_NextToken;
		}

		m_NextToken = lowscan();

		return m_CurToken;
	}

	private Token lowscan() {
		while( isSpace(m_Peek) || isCr(m_Peek) ) {
			if ( isCr(m_Peek) ) {
				m_LineNum += 1;
			}

			m_Peek = m_Stream.read();
		}

		if( isNum(m_Peek) ) {
			StringBuffer value = new StringBuffer();
			boolean hasPeriod = false;
			final int decValue = 0;
			final int hexValue = 1;
			final int binValue = 2;
			int valueType = decValue;
			BigDecimal engMul = new BigDecimal("1.0");
			
			if(m_Peek == '0') {
				value.append(m_Peek);
				m_Peek = m_Stream.read();
				if(m_Peek == 'x') {
					m_Peek = m_Stream.read();
					valueType = hexValue;
					value.setLength(0);
				}
				else if(m_Peek == 'b') {
					m_Peek = m_Stream.read();
					valueType = binValue;
					value.setLength(0);
				}
				else {
					valueType = decValue;
				}
			}
			
			switch(valueType) {
			case decValue:
				while( true ) {
					if(isNum(m_Peek) ) {
						value.append(m_Peek);
					}
					else if(m_Peek == '.')
					{
						if(hasPeriod) {
							break;
						}
						else {
							hasPeriod = true;
							value.append(m_Peek);
						}
					}
					else
					{
						break;
					}
					m_Peek = m_Stream.read();
				}
				switch(m_Peek) {
					case	'K': engMul = new BigDecimal("1000.0");		m_Peek = m_Stream.read(); break;
					case	'M': engMul = new BigDecimal("1000000.0");	m_Peek = m_Stream.read(); break;
					case	'G': engMul = new BigDecimal("1000000000.0");	m_Peek = m_Stream.read(); break;
					case	'T': engMul = new BigDecimal("1000000000000.0");	m_Peek = m_Stream.read(); break;
					case	'm': engMul = new BigDecimal("0.001");		m_Peek = m_Stream.read(); break;
					case	'u': engMul = new BigDecimal("0.000001");		m_Peek = m_Stream.read(); break;
					case	'n': engMul = new BigDecimal("0.000000001");	m_Peek = m_Stream.read(); break;
					case	'p': engMul = new BigDecimal("0.000000000001");	m_Peek = m_Stream.read(); break;
					default: break;
				}
				break;
			case hexValue:
				while( true ) {
					if(isNum(m_Peek) ) {
						value.append(m_Peek);
					}
					else if( ('a' <= Character.toLowerCase(m_Peek)) && (Character.toLowerCase(m_Peek) <= 'f')) {
						value.append(m_Peek);
					}
					else
					{
						BigInteger val = new BigInteger("0");
						if(value.length() > 0) val = new BigInteger(value.toString(), 16);
						value.setLength(0);
						value.append(val.toString());
						break;
					}
					m_Peek = m_Stream.read();
				}
				break;
			case binValue:
				while( true ) {
					if( (m_Peek == '0') || (m_Peek == '1') ) {
						value.append(m_Peek);
					}
					else
					{
						BigInteger val = new BigInteger("0");
						if(value.length() > 0) val = new BigInteger(value.toString(), 2);
						value.setLength(0);
						value.append(val.toString());
						break;
					}
					m_Peek = m_Stream.read();
				}
				break;
			default:
				break;
			}
			return new NumToken(new BigDecimal(value.toString()).multiply(engMul));
		}
		else if( isLetter(m_Peek) ) {
			StringBuffer strBuffer = new StringBuffer();

			while( isLetter(m_Peek) || isNum(m_Peek) ) {
				strBuffer.append(m_Peek);
				m_Peek = m_Stream.read();
			}
			String str = new String(strBuffer);
			WordToken wordToken = m_Words.get(str);
			if ( wordToken == null ) {
				wordToken = new WordToken(Tags.ID, str);
				m_Words.put(str, wordToken);
			}
			return wordToken;
		}
		else
		{
			switch(m_Peek) {
				case	0: m_Peek = ' '; return new Token(Tags.END);
				case	';': m_Peek = ' '; return new Token(Tags.DELIMITER);
				case	'+': m_Peek = ' '; return new Token(Tags.ADD);
				case	'-': m_Peek = ' '; return new Token(Tags.SUB);
				case	'/': m_Peek = ' '; return new Token(Tags.DIV);
				case	'(': m_Peek = ' '; return new Token(Tags.LPAREN);
				case	')': m_Peek = ' '; return new Token(Tags.RPAREN);
				case	',': m_Peek = ' '; return new Token(Tags.COMMA);
				case	'.': m_Peek = ' '; return new Token(Tags.PERIOD);
				case	'*':
					m_Peek = m_Stream.read();
					switch(m_Peek) {
						case	'*':	m_Peek = ' '; return new Token(Tags.POWER);
						default:		return new Token(Tags.MUL);
					}
				case	'\"':
					{
						StringBuffer valstr = new StringBuffer();
						m_Peek = m_Stream.read();
						while(m_Peek != '\"') {
							if(isCr(m_Peek) || (m_Peek == 0)) {
								m_Peek = ' '; return new Token(Tags.UNKNOWN);
							}
							if(m_Peek == '\\')
							{
								m_Peek = m_Stream.read();
								switch(m_Peek)
								{
								case	'\\': m_Peek = '\\'; break;
								case	'\"': m_Peek = '\"'; break;
								case	't': m_Peek = '\t'; break;
								case	'r': m_Peek = '\r'; break;
								case	'n': m_Peek = '\n'; break;
								default: m_Peek = ' '; return new Token(Tags.UNKNOWN);
								}
							}
							valstr.append(m_Peek);
							m_Peek = m_Stream.read();
						}
						m_Peek = m_Stream.read();
						return new WordToken(Tags.STRING, new String(valstr.toString()));
					}
					// unreachable break;
				case	'{':
				{
					StringBuffer valstr = new StringBuffer();
					int stack = 1;
					m_Peek = m_Stream.read();
					while(true) {
						if(m_Peek == '{') {
							stack += 1;
						}
						if(m_Peek == '}') {
							stack -= 1;
							if(stack == 0) {
								break;
							}
						}
						if(isCr(m_Peek) || (m_Peek == 0)) {
							m_Peek = ' '; return new Token(Tags.UNKNOWN);
						}
						if(m_Peek == '\\')
						{
							m_Peek = m_Stream.read();
							switch(m_Peek)
							{
							case	'\\': m_Peek = '\\'; break;
							case	'\"': m_Peek = '\"'; break;
							case	't': m_Peek = '\t'; break;
							case	'r': m_Peek = '\r'; break;
							case	'n': m_Peek = '\n'; break;
							default: m_Peek = ' '; return new Token(Tags.UNKNOWN);
							}
						}
						valstr.append(m_Peek);
						m_Peek = m_Stream.read();
					}
					m_Peek = m_Stream.read();
					return new WordToken(Tags.STRING, new String(valstr.toString()));
				}
				// unreachable break;
				case	'=':
					m_Peek = m_Stream.read();
					switch(m_Peek) {
						case	'=':	m_Peek = ' '; return new Token(Tags.EQ);
						default:		return new Token(Tags.ASSIGN);
					}
				case	'<':
					m_Peek = m_Stream.read();
					switch(m_Peek) {
						case	'<':	m_Peek = ' '; return new Token(Tags.LSHIFT);
						case	'=':	m_Peek = ' '; return new Token(Tags.LE);
						default:		return new Token(Tags.LT);
					}
				case	'>':
					m_Peek = m_Stream.read();
					switch(m_Peek) {
						case	'>':	m_Peek = ' '; return new Token(Tags.RSHIFT);
						case	'=':	m_Peek = ' '; return new Token(Tags.GE);
						default:		return new Token(Tags.GT);
					}
				case	'&':
					m_Peek = m_Stream.read();
					switch(m_Peek) {
						case	'&':	m_Peek = ' '; return new Token(Tags.L_AND);
						default:		return new Token(Tags.AND);
					}
				case	'|':
					m_Peek = m_Stream.read();
					switch(m_Peek) {
						case	'|':	m_Peek = ' '; return new Token(Tags.L_OR);
						default:		return new Token(Tags.OR);
					}
				case	'^': m_Peek = ' '; return new Token(Tags.XOR);
				case	'~': m_Peek = ' '; return new Token(Tags.NOT);
				case	'%': m_Peek = ' '; return new Token(Tags.PERCENT);
				case	'!':
					m_Peek = m_Stream.read();
					switch(m_Peek) {
						case	'=':	m_Peek = ' '; return new Token(Tags.NE);
						default:		return new Token(Tags.FACTORIAL);
					}
				default: m_Peek = ' '; return new Token(Tags.UNKNOWN);
			}
		}
	}

	private void makeCharClassTable() {
		for(int i = 0; i < m_CharClassTable.length; i++) {
			m_CharClassTable[i] = OTHER;
		}
		for(int i = '0'; i <= '9'; i++) {
			m_CharClassTable[i] = NUM;
		}
		for(int i = 'a'; i <= 'z'; i++) {
			m_CharClassTable[i] = LETTER;
		}
		for(int i = 'A'; i <= 'Z'; i++) {
			m_CharClassTable[i] = LETTER;
		}
		m_CharClassTable['_'] = LETTER;
		m_CharClassTable[' '] = SPACE;
		m_CharClassTable['\t'] = SPACE;
		m_CharClassTable['\n'] = CR;
	}

	private boolean isOther(char ch) {
		if(ch >= 256)	return false;
		else			return m_CharClassTable[ch] == OTHER;
	}
	private boolean isLetter(char ch) {
		if(ch >= 256) {
			return true;
		}
		else {
			return m_CharClassTable[ch] == LETTER;
		}
	}
	private boolean isNum(char ch) {
		if(ch >= 256)	return false;
		else			return m_CharClassTable[ch] == NUM;
	}
	private boolean isSpace(char ch) {
		if(ch >= 256)	return false;
		else			return m_CharClassTable[ch] == SPACE;
	}
	private boolean isCr(char ch) {
		if(ch >= 256)	return false;
		else			return m_CharClassTable[ch] == CR;
	}
}

package jp.hatamonkensetsu.software.android.Hatacalc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import jp.hatamonkensetsu.software.android.calclib.FuncInfo;
import jp.hatamonkensetsu.software.android.calclib.IUserFunc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import jp.hatamonkensetsu.software.android.calclib.Parser;

import jp.hatamonkensetsu.software.android.Hatacalc.R;

public class Hatacalc extends Activity implements IUserFunc, View.OnLongClickListener, View.OnTouchListener {

	private EditText m_edtExpression;
	private String m_ExpressionStr;
	private int m_ExpressionCurPos;
	private EditText m_edtAnswer;
	private Parser m_Parser;
	private static final String STATE_CURRENT_VIEW = "state-current-view";

	private final int decResult = 0;
	private final int hexResult = 1;
	private final int binResult = 2;
	private int m_ResultType = decResult;
	
	private final int deg = 0;
	private final int rad = 1;
	private int m_degradType = deg;

	private boolean m_IsAutoCalc = false;
	
    private MyPagerAdapter mPagerAdapter1;
    private MyPagerAdapter mPagerAdapter2;
    private ViewPager mViewPager1;
    private ViewPager mViewPager2;
    private int m_CurrentPanelId1 = 0;
    private int m_CurrentPanelId2 = 0;
    
    private static final int MENU_ITEM_TOGGLE_AUTO_CALC = 0;
    private static final int MENU_ITEM_VAR_LIST = 1;
    private static final int MENU_ITEM_FUNC_LIST = 2;
    
    private int m_Direction = 0;
    private final int m_Speed = 100;
    private boolean m_LongClicked = false;

    private final Handler m_Handler = new Handler();
    private final Runnable m_Runnable = new Runnable() {
        public void run() {
            if (m_Direction != 0) {
            	if(m_Direction == -101)		backSpace();
            	else						moveCursorByOffset(m_Direction);
                m_Handler.postDelayed(this, m_Speed);
            }
        }
    };
    
    private ArrayList<String> m_Ids;
    private ArrayList<FuncListInfo> m_UserFuncs;
    
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Disable IME for this application
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.main);

		m_Parser = ((HatacalcApp)getApplication()).getParser();
		
		m_edtExpression = (EditText)this.findViewById(R.id.EditTextExpression);
        m_edtExpression.setBackgroundDrawable(null);
		m_edtExpression.requestFocus();
		m_edtAnswer = (EditText)this.findViewById(R.id.EditTextAnswer);
        m_edtAnswer.setBackgroundDrawable(null);

        mPagerAdapter1 = new MyPagerAdapter();
        mPagerAdapter1.addResId(R.layout.panel1_main);
        mPagerAdapter1.addResId(R.layout.panel1_if);
        mPagerAdapter1.addResId(R.layout.panel1_loop);
        mPagerAdapter1.addResId(R.layout.panel1_math1);
        mPagerAdapter1.addResId(R.layout.panel1_math2);
        mPagerAdapter1.addResId(R.layout.panel1_bin);
        mPagerAdapter1.addResId(R.layout.panel1_eng);
        mPagerAdapter1.addResId(R.layout.panel1_func);
        mPagerAdapter1.addResId(R.layout.panel1_var);
        mViewPager1 = (ViewPager) findViewById(R.id.viewPager1);
        mViewPager1.setAdapter(mPagerAdapter1);
        mViewPager1.setCurrentItem(savedInstanceState==null ? 0 : savedInstanceState.getInt(STATE_CURRENT_VIEW, 0));

        mPagerAdapter2 = new MyPagerAdapter();
        mPagerAdapter2.addResId(R.layout.panel2_main);
        mPagerAdapter2.addResId(R.layout.panel2_key);
        mViewPager2 = (ViewPager) findViewById(R.id.viewPager2);
        mViewPager2.setAdapter(mPagerAdapter2);
        mViewPager2.setCurrentItem(savedInstanceState==null ? 0 : savedInstanceState.getInt(STATE_CURRENT_VIEW, 0));

		{
			FuncInfo info = new FuncInfo();
			info.m_Executor = this;
			info.m_ArgNames.add("x");
			info.m_IsBuiltIn = true;
			m_Parser.setUserFunc("sind", info);
			m_Parser.setUserFunc("cosd", info);
			m_Parser.setUserFunc("tand", info);
			m_Parser.setUserFunc("sinr", info);
			m_Parser.setUserFunc("cosr", info);
			m_Parser.setUserFunc("tanr", info);
			m_Parser.setUserFunc("asind", info);
			m_Parser.setUserFunc("acosd", info);
			m_Parser.setUserFunc("atand", info);
			m_Parser.setUserFunc("asinr", info);
			m_Parser.setUserFunc("acosr", info);
			m_Parser.setUserFunc("atanr", info);
			m_Parser.setUserFunc("ln", info);
			m_Parser.setUserFunc("log", info);
			m_Parser.setUserFunc("sqrt", info);
			m_Parser.setUserFunc("sum", info);
			m_Parser.setUserFunc("ave", info);
		}
		m_Parser.setKigoChange("\u0435", " e ");
		m_Parser.setKigoChange("\u03c0", " pi ");
		m_Parser.setKigoChange("\u2212", "-");
		m_Parser.setKigoChange("\u00d7", "*");
		m_Parser.setKigoChange("\u00f7", "/");
		m_Parser.setKigoChange("\uff0e", ".");
		m_Parser.setKigoChange("\uff0c", ",");
		m_Parser.setKigoChange("\uff08", "(");
		m_Parser.setKigoChange("\uff09", ")");
		m_Parser.setKigoChange("\uff1d", "=");
		m_Parser.setKigoChange("\u221a", "sqrt");

        ((Button)findViewById(R.id.ButtonChange)).setText(R.string.Dec);
        ((Button)findViewById(R.id.ButtonCalc)).setOnLongClickListener(this);
        ((Button)findViewById(R.id.ButtonClear)).setOnLongClickListener(this);
        ((Button)findViewById(R.id.ButtonLeft)).setOnLongClickListener(this);
        ((Button)findViewById(R.id.ButtonRight)).setOnLongClickListener(this);
        ((Button)findViewById(R.id.ButtonBS)).setOnLongClickListener(this);
        ((Button)findViewById(R.id.ButtonLeft)).setOnTouchListener(this);
        ((Button)findViewById(R.id.ButtonRight)).setOnTouchListener(this);
        ((Button)findViewById(R.id.ButtonBS)).setOnTouchListener(this);

        loadPref();
        restoreSetting();
	}

	@Override
	protected void onStart() {
		super.onStart();
        if(m_IsAutoCalc) calc();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
        savePref();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ITEM_VAR_LIST, 0, R.string.VarList);
		menu.add(0, MENU_ITEM_FUNC_LIST, 0, R.string.FuncList);
		menu.add(0, MENU_ITEM_TOGGLE_AUTO_CALC, 0, R.string.ToggleAutoCalc);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		switch(item.getItemId()) {
		case	MENU_ITEM_VAR_LIST:
			startVarListActivity();
			break;
		case	MENU_ITEM_FUNC_LIST:
			startFuncListActivity();
			break;
		case	MENU_ITEM_TOGGLE_AUTO_CALC:
			toggleAutoCalc();
			break;
		}
		return true;
	}
	
	public boolean onLongClick(View v) {
	    int id = v.getId();
	    switch (id) {
	    case R.id.ButtonCalc:
	    	toggleAutoCalc();
            break;
	    case R.id.ButtonClear:
            clearAnswer();
            clearExpression();
            break;
	    case R.id.ButtonLeft:
	    	m_LongClicked = true;
	    	m_Direction = -1;
            m_Handler.post(m_Runnable);
            break;
	    case R.id.ButtonRight:
	    	m_LongClicked = true;
            m_Direction = 1;
            m_Handler.post(m_Runnable);
            break;
	    case R.id.ButtonBS:
	    	m_LongClicked = true;
            m_Direction = -101;
            m_Handler.post(m_Runnable);
            break;
	    }
		return false;
	}

	public boolean onTouch(View v, MotionEvent event) {
		switch(v.getId())
		{
		case	R.id.ButtonLeft:
		case	R.id.ButtonRight:
		case	R.id.ButtonBS:
			if (	(event.getAction() == MotionEvent.ACTION_CANCEL)
				||	(event.getAction() == MotionEvent.ACTION_UP)
				) {
	        	m_Direction = 0;
	        }
			break;
		}
		return false;
	}
	
	public void onClick(View v) {
        int id = v.getId();
        switch (id) {
        case R.id.ButtonBS:
	    	if( ! m_LongClicked )	backSpace();
	    	m_LongClicked = false;
            break;

        case R.id.ButtonClear:
            clearExpression();
            break;

        case R.id.ButtonCalc:
        	if( ! m_IsAutoCalc)		calc();
            break;

        case R.id.ButtonLeft:
	    	if( ! m_LongClicked )	moveCursorByOffset(-1);
	    	m_LongClicked = false;
            break;

        case R.id.ButtonRight:
	    	if( ! m_LongClicked )	moveCursorByOffset(1);
	    	m_LongClicked = false;
            break;

        case R.id.ButtonChange:
        	toggleResultType();
            break;

        case R.id.ButtonDegRad:
        	toggleDegRad();
            break;
            
        case R.id.ButtonKilo: insertText(getString(R.string.KiloIns)); break;
        case R.id.ButtonMega: insertText(getString(R.string.MegaIns)); break;
        case R.id.ButtonGiga: insertText(getString(R.string.GigaIns)); break;
        case R.id.ButtonTera: insertText(getString(R.string.TeraIns)); break;
        case R.id.ButtonMilli: insertText(getString(R.string.MilliIns)); break;
        case R.id.ButtonMicro: insertText(getString(R.string.MicroIns)); break;
        case R.id.ButtonNano: insertText(getString(R.string.NanoIns)); break;
        case R.id.ButtonPico: insertText(getString(R.string.PicoIns)); break;
        case R.id.ButtonLoop: insertText(getString(R.string.LoopIns)); break;
        case R.id.ButtonIf: insertText(getString(R.string.IfIns)); break;
        case R.id.ButtonPower: insertText(getString(R.string.PowerIns)); break;
        case R.id.ButtonFactorial: insertText(getString(R.string.FactorialIns)); break;
        case R.id.ButtonPermutation: insertText(" "+getString(R.string.PermutationIns)+" "); break;
        case R.id.ButtonCombination: insertText(" "+getString(R.string.CombinationIns)+" "); break;
        case R.id.ButtonSin: if(m_degradType == deg) insertText(getString(R.string.SindIns)); else insertText(getString(R.string.SinrIns)); break;
        case R.id.ButtonCos: if(m_degradType == deg) insertText(getString(R.string.CosdIns)); else insertText(getString(R.string.CosrIns)); break;
        case R.id.ButtonTan: if(m_degradType == deg) insertText(getString(R.string.TandIns)); else insertText(getString(R.string.TanrIns)); break;
        case R.id.ButtonAsin: if(m_degradType == deg) insertText(getString(R.string.AsindIns)); else insertText(getString(R.string.AsinrIns)); break;
        case R.id.ButtonAcos: if(m_degradType == deg) insertText(getString(R.string.AcosdIns)); else insertText(getString(R.string.AcosrIns)); break;
        case R.id.ButtonAtan: if(m_degradType == deg) insertText(getString(R.string.AtandIns)); else insertText(getString(R.string.AtanrIns)); break;
        case R.id.ButtonSum: insertText(getString(R.string.SumIns)); break;
        case R.id.ButtonAve: insertText(getString(R.string.AveIns)); break;
        case R.id.ButtonLn: insertText(getString(R.string.LnIns)); break;
        case R.id.ButtonLog: insertText(getString(R.string.LogIns)); break;
        case R.id.ButtonSqrt: insertText(getString(R.string.SqrtIns)); break;
        case R.id.ButtonMod: insertText(" "+getString(R.string.ModIns)+" "); break;
        case R.id.ButtonFuncDef: insertText(getString(R.string.FuncDefIns)+" "); break;
        case R.id.ButtonFunc1: insertText(getString(R.string.Func1Ins)); break;
        case R.id.ButtonFunc2: insertText(getString(R.string.Func2Ins)); break;
        case R.id.ButtonFunc3: insertText(getString(R.string.Func3Ins)); break;
        case R.id.ButtonBack: mViewPager1.setCurrentItem(0); break;
        case R.id.ButtonChangeIf: mViewPager1.setCurrentItem(1); break;
        case R.id.ButtonChangeLoop: mViewPager1.setCurrentItem(2); break;
        case R.id.ButtonChangeMath1: mViewPager1.setCurrentItem(3); break;
        case R.id.ButtonChangeMath2: mViewPager1.setCurrentItem(4); break;
        case R.id.ButtonChangeHex: mViewPager1.setCurrentItem(5); break;
        case R.id.ButtonChangeEng: mViewPager1.setCurrentItem(6); break;
        case R.id.ButtonChangeFunc: mViewPager1.setCurrentItem(7); break;
        case R.id.ButtonChangeVar: mViewPager1.setCurrentItem(8); break;
        case R.id.ButtonVarListBtn: startVarListActivity();; break;
        case R.id.ButtonFuncListBtn: startFuncListActivity();; break;
        default:
            if (v instanceof Button) {
                String text = ((Button) v).getText().toString();
                insertText(text);
            }
        }
	}

	private void startVarListActivity() {
		Intent intent = new Intent(this, VarListActivity.class);  
		
		startActivityForResult(intent, MENU_ITEM_VAR_LIST);
	}
	
	private void startFuncListActivity() {
		Intent intent = new Intent(this, FuncListActivity.class);  
		
		startActivityForResult(intent, MENU_ITEM_FUNC_LIST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			if(extras != null) {
				switch(requestCode)
				{
				case	MENU_ITEM_VAR_LIST:
					insertText(extras.getString("var"));
					break;
				case	MENU_ITEM_FUNC_LIST:
					insertText(extras.getString("func"));
					break;
				}
			}
		}
	}

	public Result exec(String i_FuncName, ArrayList<String> i_ArgNames, ArrayList<Param> i_Params, String i_ExecExpression) {
		final Result result = new Result();
		result.value = new BigDecimal("0.0");
		String funcName = i_FuncName.toLowerCase(Locale.getDefault());
		Boolean isVariableArgSize = false;

		if(i_FuncName.compareToIgnoreCase("sum") == 0) isVariableArgSize = true;
		if(i_FuncName.compareToIgnoreCase("ave") == 0) isVariableArgSize = true;

		if(!isVariableArgSize) {
			if(i_Params.size() != i_ArgNames.size()) {
				result.errorMessage = funcName + "() : number of arg error";
				result.isOk = false;
				return result;
			}
			
			for(int i = 0; i < i_Params.size(); i++) {
				if(i_Params.get(i).m_Type != IUserFunc.Param.NUMVALUE)
				{
					result.errorMessage = funcName + "() : arg(" + i + ") error";
					result.isOk = false;
					return result;
				}
			}
		}
		
		try {
			if( i_ExecExpression.compareTo("") != 0) {
				Parser parser = new Parser();
				String expression = new String();
				
				for(int i = 0; i < i_Params.size(); i++) {
					expression += i_ArgNames.get(i) + " = " + i_Params.get(i).m_NumValue + ";";
				}
				expression += i_ExecExpression;

				if(parser.parse(expression)) {
					result.value = parser.getResult();
				}
				else {
					result.errorMessage = funcName + "() : exec error";
					result.isOk = false;
					return result;
				}
			}
			else {
				if(		(funcName.compareTo("sind") == 0)
					||	(funcName.compareTo("sinr") == 0)
					||	(funcName.compareTo("cosd") == 0)
					||	(funcName.compareTo("cosr") == 0)
					||	(funcName.compareTo("tand") == 0)
					||	(funcName.compareTo("tanr") == 0)
				) {
					double val;
					if(funcName.charAt(3) == 'd') val = Math.toRadians(i_Params.get(0).m_NumValue.doubleValue());
					else val = i_Params.get(0).m_NumValue.doubleValue();
					if(funcName.regionMatches(0, "sin", 0, 3)) result.value = new BigDecimal(Math.sin(val));
					else if(funcName.regionMatches(0, "cos", 0, 3)) result.value = new BigDecimal(Math.cos(val));
					else if(funcName.regionMatches(0, "tan", 0, 3)) result.value = new BigDecimal(Math.tan(val));
					else {
						result.errorMessage = funcName + "() : app error";
						result.isOk = false;
						return result;						
					}
					result.value = result.value.setScale(15, BigDecimal.ROUND_HALF_UP);
				}
				else if(	(funcName.compareTo("asind") == 0)
						||	(funcName.compareTo("asinr") == 0)
						||	(funcName.compareTo("acosd") == 0)
						||	(funcName.compareTo("acosr") == 0)
						||	(funcName.compareTo("atand") == 0)
						||	(funcName.compareTo("atanr") == 0)
				) {
					if(funcName.regionMatches(0, "asin", 0, 4)) result.value = new BigDecimal(Math.asin(i_Params.get(0).m_NumValue.doubleValue()));
					else if(funcName.regionMatches(0, "acos", 0, 4)) result.value = new BigDecimal(Math.acos(i_Params.get(0).m_NumValue.doubleValue()));
					else if(funcName.regionMatches(0, "atan", 0, 4)) result.value = new BigDecimal(Math.atan(i_Params.get(0).m_NumValue.doubleValue()));
					else {
						result.errorMessage = funcName + "() : app error";
						result.isOk = false;
						return result;						
					}
					if(funcName.charAt(4) == 'd') result.value = new BigDecimal(Math.toDegrees(result.value.doubleValue()));
				}
				else if(i_FuncName.compareToIgnoreCase("ln") == 0) {
					result.value = new BigDecimal(Math.log(i_Params.get(0).m_NumValue.doubleValue()));
				}
				else if(i_FuncName.compareToIgnoreCase("log") == 0) {
					result.value = new BigDecimal(Math.log10(i_Params.get(0).m_NumValue.doubleValue()));
				}
				else if(i_FuncName.compareToIgnoreCase("sqrt") == 0) {
					result.value = new BigDecimal(Math.sqrt(i_Params.get(0).m_NumValue.doubleValue()));
				}
				else if(i_FuncName.compareToIgnoreCase("sum") == 0) {
					result.value = new BigDecimal(0);
					for(int i=0; i < i_Params.size(); i++) {
						result.value = result.value.add(i_Params.get(i).m_NumValue);
					}
				}
				else if(i_FuncName.compareToIgnoreCase("ave") == 0) {
					result.value = new BigDecimal(0);
					if(i_Params.size() > 0) {
						for(int i=0; i < i_Params.size(); i++) {
							result.value = result.value.add(i_Params.get(i).m_NumValue);
						}
						result.value = result.value.divide(new BigDecimal(i_Params.size()), 20, BigDecimal.ROUND_HALF_UP); 
					}
				}
				else {
					result.errorMessage = funcName + "() : unknown command";
					result.isOk = false;
					return result;
				}
			}
		} catch(Exception e) {
			result.errorMessage = funcName + "() : error";
			result.isOk = false;
			return result;
		}
		
		result.isOk = true;
		return result;
	}

	private void clearExpression() {
        m_edtExpression.setText("");
        if(m_IsAutoCalc) calc();
	}
	
	private void clearAnswer() {
        m_edtAnswer.setText("");
        m_Parser.clearResult();
	}
	
	private void backSpace() {
        int cursor = m_edtExpression.getSelectionStart();
		if(cursor > 0) {
        	m_edtExpression.getText().delete(cursor-1, cursor);
            if(m_IsAutoCalc) calc();
        }
		else {
			if(m_edtExpression.getText().length() > 0) {
		       	m_edtExpression.getText().delete(cursor, cursor+1);  				
			}
		}
	}

	private void moveCursorByOffset(int i_Offset) {
        int cursor = m_edtExpression.getSelectionStart() + i_Offset;
        if((cursor >= 0) && (cursor <= m_edtExpression.length())) m_edtExpression.setSelection(cursor);
	}

	private void moveCursorTo(int i_pos) {
        int cursor = i_pos;
        if(cursor < 0) cursor = 0;
        if(cursor > m_edtExpression.length()) cursor = m_edtExpression.length();
        
        m_edtExpression.setSelection(cursor);
	}

	private void insertText(String i_Text) {
        int cursor = m_edtExpression.getSelectionStart();
        m_edtExpression.getText().insert(cursor, i_Text);
        if(m_IsAutoCalc) calc();
	}

	private void updateAnswer() {
		if(m_Parser.hasError()) {
			m_edtAnswer.setText(m_Parser.getErrorMessage());
		}
		else {
			String result = "";
			BigDecimal roundVal = m_Parser.getResult().round(new MathContext(20));
			switch(m_ResultType) {
			case	decResult:
				result = Parser.getDisplayValueStringWithSeparator(roundVal);
				break;
			case	hexResult:
				result = "0x" + divide(Long.toHexString(roundVal.longValue()).toUpperCase(Locale.getDefault()),4);
				break;
			case	binResult:
				result = "0b" + divide(Long.toBinaryString(roundVal.longValue()), 4);
				break;
			}
			m_edtAnswer.setText(result);
		}
	}

	private void calc() {
		String expression = m_edtExpression.getText().toString();
		
		m_Parser.parse(expression);
		updateAnswer();
	}
	
	String divide(String i_Str, int i_Keta) {
		StringBuffer result = new StringBuffer();
		int count = 0;
		for(int i = i_Str.length(); i > 0; i--) {
			if(count >= i_Keta) {
				result.insert(0, ' ');
				count = 0;
			}
			result.insert(0, i_Str.charAt(i-1));
			count += 1;
		}
		return result.toString();
	}
	
	private void toggleResultType() {
		changeResultTypeTo(m_ResultType+1);
		updateAnswer();
	}

	private void toggleAutoCalc() {
    	changeAutoCalcTo(!m_IsAutoCalc);
    	if(m_IsAutoCalc) calc();
	}

	private void toggleDegRad() {
    	changeDegRadTo(m_degradType + 1);
	}

	private void changeResultTypeTo(int i_resultType) {
		m_ResultType = i_resultType;
		if(m_ResultType > binResult) {
			m_ResultType = decResult;
		}
		switch(m_ResultType) {
		case	decResult:
			((Button)findViewById(R.id.ButtonChange)).setText(R.string.Dec);
			break;
		case	hexResult:
			((Button)findViewById(R.id.ButtonChange)).setText(R.string.Hex);
			break;
		case	binResult:
			((Button)findViewById(R.id.ButtonChange)).setText(R.string.Bin);
			break;
		}
	}

	private void changeAutoCalcTo(boolean i_isAutoCalc) {
    	m_IsAutoCalc = i_isAutoCalc;
    	if(m_IsAutoCalc) {
    		((Button)(findViewById(R.id.ButtonCalc))).setText(R.string.AutoCalc);
    	}
    	else {
    		((Button)(findViewById(R.id.ButtonCalc))).setText(R.string.Calc);
    	}
	}

	private void changeDegRadTo(int i_degrad) {
    	m_degradType = i_degrad;
    	if(m_degradType > rad) {
    		m_degradType = deg;
    	}
    	switch(m_degradType) {
		case	deg:
			((Button)findViewById(R.id.ButtonDegRad)).setText(R.string.Deg);
			break;
		case	rad:
			((Button)findViewById(R.id.ButtonDegRad)).setText(R.string.Rad);
			break;
    	}
	}
	
	@SuppressLint("WorldReadableFiles")
	private void loadPref() {
		SharedPreferences pref = getSharedPreferences("hatacalc.pref", MODE_WORLD_READABLE);
		m_ExpressionStr = pref.getString("LastExpression", "");
		m_ExpressionCurPos = pref.getInt("CursorPos", 0);
		m_ResultType = pref.getInt("ResultType", decResult);
		m_IsAutoCalc = pref.getBoolean("IsAutoCalc", false);
		m_degradType = pref.getInt("DegRadType", deg);
		m_CurrentPanelId1 = pref.getInt("Panel1Id", 0);
		m_CurrentPanelId2 = pref.getInt("Panel2Id", 0);
		
		{
			int idCount = pref.getInt("IdCount", 0);
			m_Ids = new ArrayList<String>();
			for(int i = 0; i < idCount; i++) {
				m_Ids.add(pref.getString("Id"+i, ""));
			}
		}
		{
			int funcCount = pref.getInt("UserFuncCount", 0);
			m_UserFuncs = new ArrayList<FuncListInfo>();
			for(int i = 0; i < funcCount; i++) {
				String all = pref.getString("UserFunc"+i, "");
				String name = "";
				String args = "";
				String expression = "";
				int indexLParen = all.indexOf('(');
				int indexAssign = all.indexOf('=');
				if(indexLParen >= 1) {
					name = all.substring(0, indexLParen);
					if(indexAssign > indexLParen) {
						args = all.substring(indexLParen, indexAssign);
						expression = all.substring(indexAssign+1);
					}
				}
				m_UserFuncs.add(new FuncListInfo(name, args, expression));
			}
		}
	}
	
	@SuppressLint("WorldReadableFiles")
	private void savePref() {
		SharedPreferences pref = getSharedPreferences("hatacalc.pref", MODE_WORLD_READABLE);
		Editor e = pref.edit();
		m_ExpressionStr = m_edtExpression.getText().toString();
		m_ExpressionCurPos = m_edtExpression.getSelectionStart();
		e.putString("LastExpression", m_ExpressionStr);
		e.putInt("CursorPos", m_ExpressionCurPos);
		e.putInt("ResultType", m_ResultType);
		e.putBoolean("IsAutoCalc", m_IsAutoCalc);
		e.putInt("DegRadType", m_degradType);

		m_CurrentPanelId1 = mViewPager1.getCurrentItem();
		e.putInt("Panel1Id", m_CurrentPanelId1);
		m_CurrentPanelId2 = mViewPager2.getCurrentItem();
		e.putInt("Panel2Id", m_CurrentPanelId2);
		
		HashMap<String, BigDecimal> ids = m_Parser.getIdValuesList();
		m_Ids = new ArrayList<String>();
		for(Iterator<String> it = ids.keySet().iterator(); it.hasNext(); ) {
			String id = it.next();
			String val = id + "=" + Parser.getDisplayValueString(ids.get(id));
			m_Ids.add(val);
		}
		e.putInt("IdCount", m_Ids.size());
		for(int i = 0; i < m_Ids.size(); i++) {
			e.putString("Id"+i, m_Ids.get(i));
		}

		m_UserFuncs = m_Parser.getUserFuncInfoList();
		e.putInt("UserFuncCount", m_UserFuncs.size());
		for(int i = 0; i < m_UserFuncs.size(); i++) {
			e.putString("UserFunc"+i, m_UserFuncs.get(i).getFuncName() + m_UserFuncs.get(i).getFuncArgs() + "=" + m_UserFuncs.get(i).getFuncExpression());
		}

		e.commit();
	}
	
	private void restoreSetting() {
		m_edtExpression.setText(m_ExpressionStr);
		moveCursorTo(m_ExpressionCurPos);
		changeResultTypeTo(m_ResultType);
		changeAutoCalcTo(m_IsAutoCalc);
		mViewPager1.setCurrentItem(m_CurrentPanelId1);
		mViewPager1.setCurrentItem(m_CurrentPanelId2);
		for(int i = 0; i < m_Ids.size(); i++) {
			m_Parser.parse(m_Ids.get(i));
		}
		for(int i = 0; i < m_UserFuncs.size(); i++) {
			m_Parser.parse("def " + m_UserFuncs.get(i).getFuncName() + m_UserFuncs.get(i).getFuncArgs() + "=" + m_UserFuncs.get(i).getFuncExpression());
		}
		calc();
	}
	
	private class MyPagerAdapter extends PagerAdapter {
		private ArrayList<Integer> mResIds = new ArrayList<Integer>();

		public void addResId(int id) {
			mResIds.add(id);
		}
		
		@Override
		public int getCount() {
			return mResIds.size();
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			int resid = mResIds.get(position);
			View view = getLayoutInflater().inflate(resid, null);
			container.addView(view);

			// 生成したView内のViewにアクセスする場合は、以下で操作します
			switch(resid) {
			case	R.layout.panel1_math1:
				changeDegRadTo(m_degradType);
				break;
			}
			return view;
		}
		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == (View)object;
		}
	}
}

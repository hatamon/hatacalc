package jp.hatamonkensetsu.software.android.Hatacalc;

import jp.hatamonkensetsu.software.android.calclib.Parser;
import android.app.Application;

public class HatacalcApp extends Application {
	Parser m_Parser = new Parser();
	
	@Override
    public void onCreate() {
        super.onCreate();
    }
    
    public Parser getParser() {
    	return m_Parser;
    }
}

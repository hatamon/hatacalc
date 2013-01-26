package jp.hatamonkensetsu.software.android.Hatacalc;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import jp.hatamonkensetsu.software.android.Hatacalc.R;
import jp.hatamonkensetsu.software.android.calclib.Parser;

public class FuncListActivity extends ListActivity implements AdapterView.OnItemLongClickListener {
	public class FuncListAdapter extends ArrayAdapter<FuncListInfo> {
		private LayoutInflater m_Inflater;
		private TextView m_Name;
		private TextView m_Expression;
		
		public FuncListAdapter(Context context, List<FuncListInfo> objects) {
			super(context, 0, objects);
			m_Inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public View getView(final int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = m_Inflater.inflate(R.layout.func_list_row, null);
			}
			final FuncListInfo item = this.getItem(position);
			if(item != null) {
				m_Name = (TextView)convertView.findViewById(R.id.FuncName);
				m_Name.setText(item.getFuncName() + item.getFuncArgs());
				m_Expression = (TextView)convertView.findViewById(R.id.FuncExpression);
				m_Expression.setText(item.getFuncExpression());
			}
			
			return convertView;
		}
	}

	private Parser m_Parser;
	private ArrayList<FuncListInfo> m_UserFuncs;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        setContentView(R.layout.func_list_activity);

		m_Parser = ((HatacalcApp)getApplication()).getParser();
		m_UserFuncs = m_Parser.getUserFuncInfoList();
        
        FuncListAdapter adapter = new FuncListAdapter(getApplicationContext(), m_UserFuncs);
		setListAdapter(adapter);
		
		getListView().setOnItemLongClickListener(this);
	}
	
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long id) {
		final String[] ITEM = new String[]{getString(R.string.PasteFuncDef), getString(R.string.Remove)};
		new AlertDialog.Builder(this)
				.setTitle(m_UserFuncs.get((int)id).getFuncName())
				.setItems(ITEM, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch(which) {
						case	0:
							Intent intent = new Intent();
							intent.putExtra("func", getString(R.string.FuncDefIns) + " " + m_UserFuncs.get((int)id).getFuncName() + m_UserFuncs.get((int)id).getFuncArgs() + "=" + m_UserFuncs.get((int)id).getFuncExpression());
							setResult(RESULT_OK, intent);
							
							finish();
							break;
						case	1:
							{
								String funcName = m_UserFuncs.get((int)id).getFuncName();
								Toast.makeText(FuncListActivity.this, funcName + " : " + getString(R.string.Removed), Toast.LENGTH_LONG).show();
								m_Parser.delUserFunc(funcName);
								((FuncListAdapter)(getListView().getAdapter())).remove(m_UserFuncs.get((int)id));
								break;
							}
						}
					}
				})
        		.create()
        		.show();
		return true;
	}

	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);
		Intent intent = new Intent();
		intent.putExtra("func", m_UserFuncs.get((int)id).getFuncName() + "(");
		setResult(RESULT_OK, intent);
		
		finish();
	}	
}

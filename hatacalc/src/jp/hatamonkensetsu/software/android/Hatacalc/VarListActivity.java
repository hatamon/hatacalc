package jp.hatamonkensetsu.software.android.Hatacalc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import jp.hatamonkensetsu.software.android.Hatacalc.VarListActivity.VarListAdapter;
import jp.hatamonkensetsu.software.android.calclib.Parser;
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

public class VarListActivity extends ListActivity  implements AdapterView.OnItemLongClickListener {
	public class VarListAdapter extends ArrayAdapter<VarListInfo> {
		private LayoutInflater m_Inflater;
		private TextView m_Name;
		private TextView m_Value;
		
		public VarListAdapter(Context context, List<VarListInfo> objects) {
			super(context, 0, objects);
			m_Inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public View getView(final int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = m_Inflater.inflate(R.layout.var_list_row, null);
			}
			final VarListInfo item = this.getItem(position);
			if(item != null) {
				m_Name = (TextView)convertView.findViewById(R.id.VarName);
				m_Name.setText(item.getVarName());
				m_Value = (TextView)convertView.findViewById(R.id.Value);
				{
					String result = "";
					BigDecimal roundVal = item.getValue().round(new MathContext(20));
					try{
						double number = roundVal.doubleValue();
						DecimalFormat df1 = new DecimalFormat("###,###.####################");
						result = df1.format(number);
					}catch(Exception e){}
					m_Value.setText(result);
				}
			}
			
			return convertView;
		}
	}

	private Parser m_Parser;
	private ArrayList<VarListInfo> m_Vars;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        setContentView(R.layout.var_list_activity);

		m_Parser = ((HatacalcApp)getApplication()).getParser();
		m_Vars = m_Parser.getVarInfoList();
        
        VarListAdapter adapter = new VarListAdapter(getApplicationContext(), m_Vars);
		setListAdapter(adapter);
		
		getListView().setOnItemLongClickListener(this);
	}
	
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long id) {
		final String[] ITEM = new String[]{getString(R.string.Remove)};
		new AlertDialog.Builder(this)
				.setTitle(m_Vars.get((int)id).getVarName())
				.setItems(ITEM, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch(which) {
						case	0:
							{
								String varName = m_Vars.get((int)id).getVarName();
								Toast.makeText(VarListActivity.this, varName + " : " + getString(R.string.Removed), Toast.LENGTH_LONG).show();
								m_Parser.delIdValue(varName);
								((VarListAdapter)(getListView().getAdapter())).remove(m_Vars.get((int)id));
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
		intent.putExtra("var", m_Vars.get((int)id).getVarName());
		setResult(RESULT_OK, intent);
		
		finish();
	}	
}

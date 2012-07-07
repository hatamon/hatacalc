package jp.hatamonkensetsu.software.android.Hatacalc;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class HatacalcView extends View {
	// コンストラクタ
	public HatacalcView(Context context) {
		super(context);
		setBackgroundColor(Color.WHITE);
	}
	
	// 描画
//	@Override
//	protected void onDraw(Canvas canvas) {
//		canvas.drawText("Hello World!", 0, 12, new Paint());
//	}
}

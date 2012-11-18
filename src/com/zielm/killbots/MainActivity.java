package com.zielm.killbots;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final GameView v = (GameView)findViewById(R.id.gameView1);
		v.label = (TextView)findViewById(R.id.textView1);
		v.activity = this;
		v.init();
		v.newGame();
		((Button)findViewById(R.id.newGame)).setOnClickListener(new OnClickListener() {
			
			public void onClick(View view) {
				v.newGame();
			}
		});
		((Button)findViewById(R.id.teleport)).setOnClickListener(new OnClickListener() {
			
			public void onClick(View view) {
				if(v.teleport())
					v.nextRound();
			}
		});
	}
	
}

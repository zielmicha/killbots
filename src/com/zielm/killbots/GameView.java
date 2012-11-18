package com.zielm.killbots;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class GameView extends SurfaceView {

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				movePlayerRel(event.getX(), event.getY());
				return false;
			}
		});
	}
	
	Paint bgPaint = new Paint();
	{
		bgPaint.setARGB(0xff, 0xff, 0xff, 0xff);
	}
	
	Rect rects[] = new Rect[] { 
							new Rect(53, 12, 53 + 56, 12 + 56), // empty
							new Rect(109, 68, 109 + 56, 68 + 56), // wreck 
							new Rect(164, 12, 164 + 56, 12 + 56), // enemy
							new Rect(53, 68, 53 + 56, 68 + 56)}; // player
	
	byte EMPTY = 0;
	byte WRECK = 1;
	byte ENEMY = 2;
	byte PLAYER = 3;
	byte UNREACHABLE = 4;
	int playerX = 5, playerY = 5;
	
	@Override
	protected void onDraw(Canvas c) {
		c.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
		Rect r = new Rect();
		for(int x=0; x<mw; x++) {
			for(int y=0; y<mh; y++) {
				r.left = x * size;
				r.top = y * size;
				r.right = x * size + size;
				r.bottom = y * size + size;
				c.drawBitmap(mBitmap, rects[map[x][y]], r, null);
			}
		}
	}
	
	void init() {
		(new Thread() {
			public void run() {
				try{
					while(true) {
						Thread.sleep(1000);
						myRedraw();
					}
				} catch(Exception ex) {}
			}
		}).start();
		mBitmap = Bitmap.createScaledBitmap(
				BitmapFactory.decodeResource(activity.getResources(), R.drawable.robotkill),
				240, 140, true);
		labelText = label.getText() + " ";
	}
	String labelText;
	int energy = 5;
	int level = 1;
	
	int size = 56;
	byte[][] map;
	int mw, mh;
	Bitmap mBitmap;
	Activity activity;
	TextView label;
	
	void myRedraw() {
		Canvas c = getHolder().lockCanvas();
		if(c != null) {
			onDraw(c);
			getHolder().unlockCanvasAndPost(c);
		}
	}

	void movePlayerRel(float x, float y) {
		x -= getWidth() / 2;
		y -= getHeight() / 2;
		double angle = Math.atan2(y, x);
		double d45 = Math.PI / 4;
		angle = Math.round(angle / d45) * d45;
		int rdx = (int)Math.round(Math.cos(angle));
		int rdy = (int)Math.round(Math.sin(angle));
		movePlayerRelCorrected(rdx, rdy);
	}
	
	void movePlayerRelCorrected(int x, int y) {
		if(movePlayer(playerX + x, playerY + y)) nextRound();
	}
	
	Random rand = new Random();
	public void newGame() {
		updateLabel();
		mw = getWidth() / size;
		mh = getHeight() / size;
		map = new byte[mw][mh];
		if(mw == 0 || mh == 0) {
			myRedraw();
			return;
		}
		playerX = playerY = -1;
		generateBots(5 + level * 2);
		teleport();
		energy ++;
		myRedraw();
	}
	public void nextLevel() {
		level ++;
		energy += level;
		newGame();
	}

	void updateLabel() {
		if(energy == 0 && !(rcanMovePlayer(0, 1) || rcanMovePlayer(0, -1) || rcanMovePlayer(1, -1) || rcanMovePlayer(1, 1)
				|| rcanMovePlayer(1, 0) || rcanMovePlayer(-1, 0) || rcanMovePlayer(-1, 1) || rcanMovePlayer(-1, -1))) {
			label.setText("No more possible moves :( This is the end.\n" + labelText);
		} else {
			label.setText("Energy: " + energy + "       Level: " + level + "\n" + labelText);
		}
	}
	
	public boolean teleport() {
		if(mw == 0 || mh == 0) return false;
		if(energy == 0) return false;
		energy --;
		updateLabel();
		int x = -1, y = -1;
		while(!movePlayer(x, y)) {
			x = rand.nextInt(mw);
			y = rand.nextInt(mh);
		}
		myRedraw();
		return true;
	}
	void nextRound() {
		boolean hasBots = false;
		int[][] counts = new int[mw][mh];
		for(int x=0; x<mw; x++) {
			for(int y=0; y<mh; y++) {
				if(map[x][y] == ENEMY) {
					int dx = playerX - x;
					int dy = playerY - y;
					double angle = Math.atan2(dy, dx);
					double d45 = Math.PI / 4;
					angle = Math.round(angle / d45) * d45;
					int rdx = (int)Math.round(Math.cos(angle));
					int rdy = (int)Math.round(Math.sin(angle));
					int nx = Math.min(mw - 1, Math.max(0, x + rdx)), ny =  Math.min(mh - 1, Math.max(0, y + rdy));
					counts[nx][ny] += 1;
					map[x][y] = EMPTY;
				}
			}
		}
		for(int x=0; x<mw; x++) {
			for(int y=0; y<mh; y++) {
				if(counts[x][y] != 0 && map[x][y] == EMPTY) {
					map[x][y] = counts[x][y] == 1? ENEMY: WRECK;
					if(counts[x][y] == 1) hasBots = true;
				}
			}
		}
		if(!hasBots) nextLevel();
		updateLabel();
		
	}
	byte getAt(int x, int y){
		if(x < 0 || y < 0 || x >= mw || y >= mh) return UNREACHABLE;
		return map[x][y];
	}
	
	void generateBots(int count) {
		count = Math.min(count, mw * mh / 10);
		for(int i=0; i<count; i++) {
			map[rand.nextInt(mw)][rand.nextInt(mh)] = ENEMY;
		}
	}
	
	public boolean movePlayer(int x, int y) {
		if(!canMovePlayer(x, y)) return false;
		if(getAt(playerX, playerY) != UNREACHABLE) map[playerX][playerY] = EMPTY;
		map[x][y] = PLAYER;
		playerX = x;
		playerY = y;
		return true;
	}
	public boolean canMovePlayer(int x, int y) {
		if(getAt(x, y) != EMPTY) return false;
		if(getAt(x-1, y-1) == ENEMY || getAt(x-1, y) == ENEMY || getAt(x-1, y+1) == ENEMY ||
		   getAt(x, y-1) == ENEMY || getAt(x, y) == ENEMY || getAt(x, y+1) == ENEMY ||
		   getAt(x+1, y-1) == ENEMY || getAt(x+1, y) == ENEMY || getAt(x+1, y+1) == ENEMY)
			return false;
		return true;
	}
	public boolean rcanMovePlayer(int x, int y) {
		return canMovePlayer(playerX + x, playerY + y);
	}
}

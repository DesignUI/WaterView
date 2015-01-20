package com.kesalin.RippleDemo;

import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.widget.ImageView;

public class RippleView extends ImageView implements Runnable {

	boolean m_isRunning = false;
	boolean m_isRain = true;

	int m_width;
	int m_height;

	// 双缓冲
	short[] m_buf1;// 用来保存水面上每一个点的前一时刻和后一时刻波幅数据，因为波幅也就代表了波的能量
	short[] m_buf2;// 所以在后面我们称这两个数组为波能缓冲区

	int[] m_bitmap1;
	int[] m_bitmap2;

	Thread m_thread;

	int m_preX;
	int m_preY;

	Random random;
	private int screenW;
	private int screenH;

	public RippleView(Context context) {
		super(context);

		DisplayMetrics dm = new DisplayMetrics();
		((RippleDemo) context).getWindowManager().getDefaultDisplay()
				.getMetrics(dm);
		screenW = dm.widthPixels;
		screenH = dm.heightPixels;

		random = new Random();

		Bitmap image = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.b);

		m_width = image.getWidth();
		m_height = image.getHeight();

		// leave 1 extra up, low border for boundary condition
		m_buf1 = new short[m_width * (m_height)];
		m_buf2 = new short[m_width * (m_height)];

		m_bitmap1 = new int[m_width * m_height];
		m_bitmap2 = new int[m_width * m_height];

		image.getPixels(m_bitmap1, 0, m_width, 0, 0, m_width, m_height);//拷贝像素数组到m_bitmap1

		start();
	}

	@SuppressLint("DrawAllocation")
	protected void onDraw(Canvas canvas) {
		Bitmap bit = Bitmap.createBitmap(m_bitmap2, 0, m_width, m_width,
				m_height, Bitmap.Config.RGB_565);
		RectF rectF = new RectF(0, 0, screenW, screenH); // w和h分别是屏幕的宽和高，也就是你想让图片显示的宽和高
		canvas.drawBitmap(bit, null, rectF, null);
	}

	

	public void processTouchEvent(MotionEvent event) {
		int action = event.getAction();
		int x = (int) (event.getX()) * m_width / screenW;// 还原比例
		int y = (int) (event.getY()) * m_height / screenH;

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			dropStone(x, y, 8, 50);
			m_preX = x;
			m_preY = y;
			break;

		case MotionEvent.ACTION_MOVE:
			bresenhamDrop(m_preX, m_preY, x, y, 4, 40);
			m_preX = x;
			m_preY = y;
			break;
		}
	}

	public void start() {
		m_isRunning = true;
		m_thread = new Thread(this);
		m_thread.start();
	}

	public void stop() {
		m_isRunning = false;
	}

	public void resume() {
		m_isRunning = true;
	}

	public void destroy() {
		stop();

		m_thread.interrupt();
	}

	public void run() {

		while (m_isRunning) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
			;

			if (m_isRain) {
				int x = 10 + random.nextInt() % (m_width - 20);
				int y = 10 + random.nextInt() % (m_height - 20);
				int size = 4 + random.nextInt() % 4;
				dropStone(x, y, size, 80);
			}

			rippleSpread();

			rippleRender();

			postInvalidate();
		}
	}

	// 某点下一时刻的波幅算法为：上下左右四点的波幅和的一半减去当前波幅，即
	// X0' =（X1 + X2 + X3 + X4）/ 2 - X0
	// +----x3----+
	// + | +
	// + | +
	// x1---x0----x2
	// + | +
	// + | +
	// +----x4----+
	//
	void rippleSpread() {
		int pixels = m_width * (m_height - 1);
		for (int i = m_width; i < pixels; ++i) {
			// 波能扩散:上下左右四点的波幅和的一半减去当前波幅
			// X0' =（X1 + X2 + X3 + X4）/ 2 - X0
			// 1/2可以用移位运算符 “>>”
			m_buf2[i] = (short) (((m_buf1[i - 1] + m_buf1[i + 1]
					+ m_buf1[i - m_width] + m_buf1[i + m_width]) >> 1) - m_buf2[i]);

			// 波能衰减 1/32改成衰减1/2
			// m_buf2[i] -= m_buf2[i] >>5;
			m_buf2[i] -= m_buf2[i] >> 2;
		}

		// 交换波能数据缓冲区
		short[] temp = m_buf1;
		m_buf1 = m_buf2;
		m_buf2 = temp;
	}

	void rippleRender() {
		int offset;
		int i = m_width;
		int length = m_width * m_height;
		for (int y = 1; y < m_height - 1; ++y) {
			for (int x = 0; x < m_width; ++x, ++i) {
				// 计算出偏移象素和原始象素的内存地址偏移量 : offset = width * yoffset + xoffset
				offset = (m_width * (m_buf1[i - m_width] - m_buf1[i + m_width]))//Y方向
						+ (m_buf1[i - 1] - m_buf1[i + 1]);

				// 判断坐标是否在窗口范围内
				if (i + offset > 0 && i + offset < length) {
					m_bitmap2[i] = m_bitmap1[i + offset];
				} else {
					m_bitmap2[i] = m_bitmap1[i];
				}
			}
		}
	}

	// 为了形成水波，我们必须在水池中加入波源，你可以想象成向水中投入石头，
	// 形成的波源的大小和能量与石头的半径和扔石头的力量都有关系。
	// 我们只要修改波能数据缓冲区 buf，让它在石头入水的地点来一个负的"尖脉冲"，
	// 即让 buf[x, y] = -n。经过实验，n 的范围在（32 ~ 128）之间比较合适。
	// stoneSize : 波源半径
	// stoneWeight : 波源能量
	//
	void dropStone(int x, int y, int stoneSize, int stoneWeight) {
		// 判断坐标是否在屏幕范围内
		if ((x + stoneSize) > m_width || (y + stoneSize) > m_height
				|| (x - stoneSize) < 0 || (y - stoneSize) < 0) {
			return;
		}

		int value = stoneSize * stoneSize;
		short weight = (short) -stoneWeight;
		for (int posx = x - stoneSize; posx < x + stoneSize; ++posx) {
			for (int posy = y - stoneSize; posy < y + stoneSize; ++posy) {
				if ((posx - x) * (posx - x) + (posy - y) * (posy - y) < value) {
					m_buf1[m_width * posy + posx] = weight;
				}
			}
		}

		resume();
	}

	void dropStoneLine(int x, int y, int stoneSize, int stoneWeight) {
		// 判断坐标是否在屏幕范围内
		if ((x + stoneSize) > m_width || (y + stoneSize) > m_height
				|| (x - stoneSize) < 0 || (y - stoneSize) < 0) {
			return;
		}

		for (int posx = x - stoneSize; posx < x + stoneSize; ++posx) {
			for (int posy = y - stoneSize; posy < y + stoneSize; ++posy) {
				m_buf1[m_width * posy + posx] = -40;
			}
		}

		resume();
	}

	// xs, ys : 起始点，xe, ye : 终止点，size : 波源半径，weight : 波源能量
	void bresenhamDrop(int xs, int ys, int xe, int ye, int size, int weight) {
		int dx = xe - xs;
		int dy = ye - ys;
		dx = (dx >= 0) ? dx : -dx;
		dy = (dy >= 0) ? dy : -dy;

		if (dx == 0 && dy == 0) {
			dropStoneLine(xs, ys, size, weight);
		} else if (dx == 0) {
			int yinc = (ye - ys != 0) ? 1 : -1;
			for (int i = 0; i < dy; ++i) {
				dropStoneLine(xs, ys, size, weight);
				ys += yinc;
			}
		} else if (dy == 0) {
			int xinc = (xe - xs != 0) ? 1 : -1;
			for (int i = 0; i < dx; ++i) {
				dropStoneLine(xs, ys, size, weight);
				xs += xinc;
			}
		} else if (dx > dy) {
			int p = (dy << 1) - dx;
			int inc1 = (dy << 1);
			int inc2 = ((dy - dx) << 1);
			int xinc = (xe - xs != 0) ? 1 : -1;
			int yinc = (ye - ys != 0) ? 1 : -1;

			for (int i = 0; i < dx; ++i) {
				dropStoneLine(xs, ys, size, weight);
				xs += xinc;
				if (p < 0) {
					p += inc1;
				} else {
					ys += yinc;
					p += inc2;
				}
			}
		} else {
			int p = (dx << 1) - dy;
			int inc1 = (dx << 1);
			int inc2 = ((dx - dy) << 1);
			int xinc = (xe - xs != 0) ? 1 : -1;
			int yinc = (ye - ys != 0) ? 1 : -1;

			for (int i = 0; i < dy; ++i) {
				dropStoneLine(xs, ys, size, weight);
				ys += yinc;
				if (p < 0) {
					p += inc1;
				} else {
					xs += xinc;
					p += inc2;
				}
			}
		}

		resume();
	}
}

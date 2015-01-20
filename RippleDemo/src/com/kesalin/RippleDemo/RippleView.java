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

	// ˫����
	short[] m_buf1;// ��������ˮ����ÿһ�����ǰһʱ�̺ͺ�һʱ�̲������ݣ���Ϊ����Ҳ�ʹ����˲�������
	short[] m_buf2;// �����ں������ǳ�����������Ϊ���ܻ�����

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

		image.getPixels(m_bitmap1, 0, m_width, 0, 0, m_width, m_height);//�����������鵽m_bitmap1

		start();
	}

	@SuppressLint("DrawAllocation")
	protected void onDraw(Canvas canvas) {
		Bitmap bit = Bitmap.createBitmap(m_bitmap2, 0, m_width, m_width,
				m_height, Bitmap.Config.RGB_565);
		RectF rectF = new RectF(0, 0, screenW, screenH); // w��h�ֱ�����Ļ�Ŀ�͸ߣ�Ҳ����������ͼƬ��ʾ�Ŀ�͸�
		canvas.drawBitmap(bit, null, rectF, null);
	}

	

	public void processTouchEvent(MotionEvent event) {
		int action = event.getAction();
		int x = (int) (event.getX()) * m_width / screenW;// ��ԭ����
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

	// ĳ����һʱ�̵Ĳ����㷨Ϊ�����������ĵ�Ĳ����͵�һ���ȥ��ǰ��������
	// X0' =��X1 + X2 + X3 + X4��/ 2 - X0
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
			// ������ɢ:���������ĵ�Ĳ����͵�һ���ȥ��ǰ����
			// X0' =��X1 + X2 + X3 + X4��/ 2 - X0
			// 1/2��������λ����� ��>>��
			m_buf2[i] = (short) (((m_buf1[i - 1] + m_buf1[i + 1]
					+ m_buf1[i - m_width] + m_buf1[i + m_width]) >> 1) - m_buf2[i]);

			// ����˥�� 1/32�ĳ�˥��1/2
			// m_buf2[i] -= m_buf2[i] >>5;
			m_buf2[i] -= m_buf2[i] >> 2;
		}

		// �����������ݻ�����
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
				// �����ƫ�����غ�ԭʼ���ص��ڴ��ַƫ���� : offset = width * yoffset + xoffset
				offset = (m_width * (m_buf1[i - m_width] - m_buf1[i + m_width]))//Y����
						+ (m_buf1[i - 1] - m_buf1[i + 1]);

				// �ж������Ƿ��ڴ��ڷ�Χ��
				if (i + offset > 0 && i + offset < length) {
					m_bitmap2[i] = m_bitmap1[i + offset];
				} else {
					m_bitmap2[i] = m_bitmap1[i];
				}
			}
		}
	}

	// Ϊ���γ�ˮ�������Ǳ�����ˮ���м��벨Դ��������������ˮ��Ͷ��ʯͷ��
	// �γɵĲ�Դ�Ĵ�С��������ʯͷ�İ뾶����ʯͷ���������й�ϵ��
	// ����ֻҪ�޸Ĳ������ݻ����� buf��������ʯͷ��ˮ�ĵص���һ������"������"��
	// ���� buf[x, y] = -n������ʵ�飬n �ķ�Χ�ڣ�32 ~ 128��֮��ȽϺ��ʡ�
	// stoneSize : ��Դ�뾶
	// stoneWeight : ��Դ����
	//
	void dropStone(int x, int y, int stoneSize, int stoneWeight) {
		// �ж������Ƿ�����Ļ��Χ��
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
		// �ж������Ƿ�����Ļ��Χ��
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

	// xs, ys : ��ʼ�㣬xe, ye : ��ֹ�㣬size : ��Դ�뾶��weight : ��Դ����
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

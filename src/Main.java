import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Main {

	private static int MAX_ITERATIONS = 20;
	private static final int ORBIT_LIMIT_SQUARED = 10;

	// size is 2x drawing size because this increases the resolution on my retina display.
	private static final int SIZE = 1024;

	private static double camX = 0.0;
	private static double camY = 0.0;
	private static double camSize = SIZE;
	private static double camOrigX = (double)SIZE * 2.0 / 3.0;
	private static double camOrigY = (double)SIZE / 2.0;

	private static JFrame frame;

	private static boolean isMouseThreadRunning = false;
	private static boolean doRepaint = true;
	private static boolean drawingRect = false;
	private static Point rectZoomPointInitial;
	private static Point rectZoomPointFinal;

	private static BufferedImage mandelOut = null;

	public static void main(String[] args) throws UnsupportedLookAndFeelException {
		setupWindow();
		setupRenderingThreads();
	}

	private static void applyDrawnRect() {
		camX += 2 * rectZoomPointInitial.x * camSize / (double)SIZE;
		camY += 2 * rectZoomPointInitial.y * camSize / (double)SIZE;
		camSize = Math.abs(rectZoomPointFinal.x - rectZoomPointInitial.x) * 2 * camSize / (double)SIZE;
	}

	private static void setupRenderingThreads() {
		renderingTask = new RenderingTask(ORBIT_LIMIT_SQUARED);
	}

	private static RenderingTask renderingTask;

	private static void drawMandelbrot(Graphics g) {
		if (!drawingRect) {

			ImageWriter ww = new ImageWriter(SIZE, SIZE);

			renderingTask.setCameraInfo(SIZE, camSize, camX, camY, camOrigX, camOrigY);
			renderingTask.setIterations(MAX_ITERATIONS);

			ArrayList<RenderingTask> tasks = new ArrayList<>();
			ArrayList<Thread> threads = new ArrayList<>();

			final int THREADS_X = 4;
			final int THREADS_Y = 4;
			final int THREAD_WIDTH = SIZE / THREADS_X;
			final int THREAD_HEIGHT = SIZE / THREADS_Y;

			// make all tasks
			for (int i = 0; i < THREADS_Y; i++) {
				for (int j = 0; j < THREADS_X; j++) {
					tasks.add(new RenderingTask(renderingTask).setBounds(THREAD_WIDTH * j, THREAD_HEIGHT * i, THREAD_WIDTH * (j + 1), THREAD_HEIGHT * (i + 1)));
				}
			}

			// generate all threads
			for (var t : tasks) {
				threads.add(new Thread(t));
			}

			// start them all!
			for (var t : threads) {
				t.start();
			}

			// join them
			try {
				for (var t : threads) {
					t.join();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			// merge all thread data
			for (var t : tasks) {
				int x = t.getOriginX();
				int y = t.getOriginY();
				int w = t.getWidth();
				int h = t.getHeight();
				var d = t.getFinalizedData();
				for (int i = 0; i < h; i++) {
					for (int j = 0; j < w; j++) {
						int xp = x + j;
						int yp = y + i;
						int cc = d[(i * w) + j];
						ww.writePixel(xp, yp, cc);
					}
				}
			}

			// finalize mandel out data
			mandelOut = ww.getBufferedImage();
		}

		g.drawImage(mandelOut, 0, 0, (int)((double)SIZE / 2.0), (int)((double)SIZE / 2.0), null);
	}

	private static void drawZoomRectangle(Graphics g) {
		if (rectZoomPointInitial == null || rectZoomPointFinal == null) {
			return;
		}
		g.drawRect(
			Math.min(rectZoomPointInitial.x, rectZoomPointFinal.x),
			Math.min(rectZoomPointInitial.y, rectZoomPointFinal.y),
			Math.abs(rectZoomPointFinal.x - rectZoomPointInitial.x),
			Math.abs(rectZoomPointFinal.x - rectZoomPointInitial.x)
		);
	}

	private static void setupWindow() throws UnsupportedLookAndFeelException {

		UIManager.setLookAndFeel(new NimbusLookAndFeel());

		// initialize
		frame = new JFrame("Mandelbrot Set");
		frame.setVisible(true);
		frame.setFocusable(true);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize((int)((double)SIZE / 2.0), (int)((double)SIZE / 2.0));

		// centre window
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (int)((dimension.getWidth() - frame.getWidth()) / 2);
		int y = (int)((dimension.getHeight() - frame.getHeight()) / 2);
		frame.setLocation(x, y);

		frame.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!drawingRect) {
					rectZoomPointInitial = frame.getMousePosition();
					rectZoomPointInitial.y -= 30.0;
					drawingRect = true;
				}
				initMouseRectThread();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				drawingRect = false;
				applyDrawnRect();
			}
		});

		var content = new JComponent() {
			@Override
			public void paintComponent(Graphics g) {
				drawMandelbrot(g);
				if (drawingRect) {
					drawZoomRectangle(g);
				}
			}
		};

		JButton decBtn = new JButton("-");
		decBtn.addActionListener(a -> new Thread(() -> {
			doRepaint = false;
			MAX_ITERATIONS -= 5;
			frame.repaint();
		}).start());
		content.add(decBtn);

		JButton resetBtn = new JButton("Reset");
		resetBtn.addActionListener(a -> new Thread(() -> {
			camX = 0.0;
			camY = 0.0;
			camSize = (double)SIZE;
			frame.repaint();
		}).start());
		content.add(resetBtn);

		JButton incBtn = new JButton("+");
		incBtn.addActionListener(a -> new Thread(() -> {
			doRepaint = false;
			MAX_ITERATIONS += 5;
			frame.repaint();
		}).start());
		content.add(incBtn);

		content.setLayout(new FlowLayout());

		frame.setContentPane(content);
		frame.repaint();
	}

	private static boolean checkAndMarkMouseRectThread() {
		if (isMouseThreadRunning) {
			return false;
		}
		isMouseThreadRunning = true;
		return true;
	}

	private static void initMouseRectThread() {
		if (checkAndMarkMouseRectThread()) {
			new Thread(() -> {
				do {
					rectZoomPointFinal = frame.getMousePosition();
					rectZoomPointFinal.y -= 30.0;
					frame.repaint();
				} while(drawingRect);
				isMouseThreadRunning = false;
			}).start();
		}
	}
}

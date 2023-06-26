import java.awt.*;

public class RenderingTask implements Runnable {

	private int x0, y0, x1, y1;
	private int iterations;
	private double size, camSize, camX, camY, camOrigX, camOrigY, orbitLimitSqr;
	private int[] colourData;

	public RenderingTask(double orbitLimitSqr) {
		this.orbitLimitSqr = orbitLimitSqr;
	}

	public RenderingTask(RenderingTask other) {
		this.x0 = other.x0;
		this.y0 = other.y0;
		this.x1 = other.x1;
		this.y1 = other.y1;
		this.iterations = other.iterations;
		this.size = other.size;
		this.camSize = other.camSize;
		this.camX = other.camX;
		this.camY = other.camY;
		this.camOrigX = other.camOrigX;
		this.camOrigY = other.camOrigY;
		this.orbitLimitSqr = other.orbitLimitSqr;
	}

	public RenderingTask setBounds(int x0, int y0, int x1, int y1) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		this.colourData = new int[(y1 - y0) * (x1 - x0)];
		return this;
	}

	public int getWidth() {
		return x1 - x0;
	}

	public int getHeight() {
		return y1 - y0;
	}

	public int getOriginX() {
		return x0;
	}

	public int getOriginY() {
		return y0;
	}

	public void setCameraInfo(double size, double camSize, double camX, double camY, double camOrigX, double camOrigY) {
		this.size = size;
		this.camSize = camSize;
		this.camX = camX;
		this.camY = camY;
		this.camOrigX = camOrigX;
		this.camOrigY = camOrigY;
	}

	public void setIterations(int iter) {
		this.iterations = iter;
	}

	private double baseChannel(double t) {
		return Math.max(0, 1.0 - (5.0 * t * t));
	}

	private Color gradient(double t) {
		return new Color(
			(int)(255.0 * baseChannel(t - 0.0)),
			(int)(255.0 * baseChannel(t - 0.8)),
			(int)(255.0 * baseChannel(t - 0.5))
		);
	}

	public int[] getFinalizedData() {
		return colourData;
	}

	@Override
	public void run() {
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				double rc = (double)((x / size * camSize) + camX - camOrigX) / (size * 0.3);
				double ic = (double)((y / size * camSize) + camY - camOrigY) / (size * 0.3);
				double rz = 0.0;
				double iz = 0.0;

				int i = 0;
				for (; i < iterations; i++) {
					double rzz = rc + ((rz * rz) - (iz * iz));
					double izz = ic + (2.0 * rz * iz);
					rz = rzz;
					iz = izz;
					if (((rz * rz) + (iz * iz)) > orbitLimitSqr) {
						break;
					}
				}

				final double imod = Math.sqrt((rz * rz) + (iz * iz));

				// note: this slows down performance pretty heavily, if you want better performance
				//       just comment out the part after i, aka: (Math.log(...))
				final double ismooth = (double)i - (Math.log(Math.max(1.0, Math.log(imod) / Math.log(2.0))) / Math.log(2.0));

				Color colour = Color.BLACK;

				if (i + 1 < iterations) {
					colour = gradient((double)ismooth / (double)iterations);
				}

				colourData[((y - y0) * getWidth()) + (x - x0)] = colour.getRGB();
			}
		}
	}
}

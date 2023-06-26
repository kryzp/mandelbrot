import java.awt.image.BufferedImage;

public class ImageWriter {
	private final BufferedImage image;

	public ImageWriter(int width, int height) {
		this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}

	public synchronized void writePixel(int x, int y, int rgb) {
		this.image.setRGB(x, y, rgb);
	}

	public BufferedImage getBufferedImage() {
		return this.image;
	}
}

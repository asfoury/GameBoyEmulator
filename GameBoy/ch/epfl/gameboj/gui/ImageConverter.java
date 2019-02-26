package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public abstract class ImageConverter {
	private static final int[] COLOR_MAP = new int[] { 0xFF_FF_FF_FF, 0xFF_D3_D3_D3, 0xFF_A9_A9_A9, 0xFF_00_00_00 };
    /**
     * converts a LcdImage format image to javafx image format.
     * @param lcdImage
     * @return javafx image
     */
	public static Image convert(LcdImage lcdImage) {
		WritableImage image = new WritableImage(lcdImage.width(), lcdImage.height());
        PixelWriter p = image.getPixelWriter();
		for (int y = 0; y < lcdImage.height(); ++y) {
			for (int x = 0; x < lcdImage.width(); ++x) {
				p.setArgb(x, y, COLOR_MAP[lcdImage.get(x, y)]);
			}
		}

		return image;

	}
}

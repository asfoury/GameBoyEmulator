package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;

/**
 * a Game Boy image.
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public final class LcdImage {

	private final List<LcdImageLine> lines;
	private final int height, width;

	/**
	 * constructs an image of Game Boy.
	 * 
	 * @param width
	 *            of the Image.
	 * @param height
	 *            of the Image.
	 * @param lines
	 *            of pixels of the image.
	 */
	public LcdImage(int width, int height, List<LcdImageLine> lines) {
		Preconditions.checkArgument(width > 0 && height > 0 && width % 32 == 0);
		this.lines = Collections.unmodifiableList(new ArrayList<LcdImageLine>(lines));
		this.width = width;
		this.height = height;
	}

	/**
	 * gets the height of this.
	 */
	public int height() {
		return height; // Integers are immutable
	}

	/**
	 * gets the width of this.
	 */
	public int width() {
		return width; // integers are immutable
	}

	/**
	 * gets the color of a given pixel
	 * 
	 * @param x
	 *            coordinates of the pixel
	 * @param y
	 *            coordinates of the pixel
	 * @return integer between 0 and 3
	 */
	public int get(int x, int y) {

		return Bits.set(Bits.set(0, 0, lines.get(y).lsb().testBit(x)), 1, lines.get(y).msb().testBit(x));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object that) {
		if (that.getClass() != this.getClass())
			return false;
		LcdImage b = (LcdImage) that;
		return lines.equals(b.lines);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return lines.hashCode();
	}

	/**
	 * an Image Builder.
	 *
	 * @author Karim Sabaa (269647)
	 * @author Mohamed Elasfoury (289473)
	 */
	public final static class Builder {

		List<LcdImageLine> lines;
		private int height;
		private int width;

		/**
		 * Constructs a LcdImage of a certain size.
		 * 
		 * @param width
		 *            of the image.
		 * @param height
		 *            of the image
		 * @return
		 * @throws IllegalStateException
		 *             if the width is't a multiple of 32 or if height or width are
		 *             negative.
		 */
		public Builder(int width, int height) throws IllegalArgumentException {
			Preconditions.checkArgument(width % Integer.SIZE == 0 && width > 0 && height > 0);
			lines = Arrays.asList(new LcdImageLine[height]);
			this.height = height;
			this.width = width;
		}

		/**
		 * sets a line in the image
		 * 
		 * @param index
		 *            of the line
		 * @param line
		 *            to be added
		 * @return this
		 * @throws IllegalStateException
		 *             if it has already been build
		 * @throws IllegalArgumentException
		 *             if index isn't right or the line isn't correct.
		 */
		public Builder setLine(int index, LcdImageLine line) throws IllegalStateException, IllegalArgumentException {
			if (lines == null)
				throw new IllegalStateException();
			Preconditions.checkArgument(index >= 0 && index < height);
			Preconditions.checkArgument(line != null && line.size() == width);
			lines.set(index, line);
			return this;
		}

		/**
		 * 
		 * @return {@link LcdImage}
		 */
		public LcdImage build() throws IllegalStateException {

			return new LcdImage(width, height, lines);

		}
	}
}

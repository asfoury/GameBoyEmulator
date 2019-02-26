package ch.epfl.gameboj.component.lcd;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

/**
 * a line of the Image of Game Boy.
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public final class LcdImageLine {

	private final BitVector msb, lsb, opacity;

	/**
	 * 
	 * @param msb
	 *            ,most significant bit vector.
	 * @param lsb
	 *            , least significant bit vector.
	 * @param opacity
	 *            vector.
	 * @throws IllegalArgumentException
	 *             if the given BitVector aren't the same size.
	 */
	public LcdImageLine(BitVector msb, BitVector lsb, BitVector opacity) throws IllegalArgumentException {
		Objects.requireNonNull(msb);
		Objects.requireNonNull(lsb);
		Objects.requireNonNull(opacity);
		Preconditions.checkArgument(
				msb.size() == lsb.size() && msb.size() == opacity.size() && lsb.size() == opacity.size());
		this.msb = msb;
		this.lsb = lsb;
		this.opacity = opacity;
	}

	/**
	 * returns the size in pixels of this line.
	 * 
	 */
	public int size() {
		return msb.size(); // msb size = lsb size = opacity size
	}

	/**
	 * returns the (most significant bit) vector of this line.
	 */
	public BitVector msb() {

		return msb; // BitVector is immutable
	}

	/**
	 * returns the (least significant bit) vector of this line.
	 */
	public BitVector lsb() {
		return lsb; // BitVector is immutable
	}

	/**
	 * returns the (Opacity) vector of this line.
	 */
	public BitVector opacity() {
		return opacity; // BitVector is immutable
	}

	/**
	 * shifts this line a given number of pixels, it remains the same size if the
	 * pixels number is positive the shift is for the left, else for the right
	 * 
	 */
	public LcdImageLine shift(int numberPixels) {

		return new LcdImageLine(msb.shift(numberPixels), lsb.shift(numberPixels), opacity.shift(numberPixels));
	}

	/**
	 * 
	 * 
	 * @param index
	 *            , of the start pixel
	 * @param size
	 *            of the the extraction
	 * @return Wrapped extraction
	 * @throws IllegalArgumentException
	 *             if the pixel's index is out of bounds or if the size isn't a
	 *             multiple of 32.
	 */
	public LcdImageLine extractWrapped(int size, int index) throws IllegalArgumentException {
		return new LcdImageLine(msb.extractWrapped(size, index), lsb.extractWrapped(size, index),
				opacity.extractWrapped(size, index));
	}

	/**
	 * transforms the colors of this line depending of the given palet.
	 * 
	 * @param palette
	 */
	public LcdImageLine mapColors(int palette) { // a revoir
		if (palette == 0b11_10_01_00)
			return this;
		BitVector newColor3Msb = (msb.and(lsb)).and(new BitVector(size(), Bits.test(palette, 7)));
		BitVector newColor3Lsb = (msb.and(lsb)).and(new BitVector(size(), Bits.test(palette, 6)));

		BitVector newColor2Msb = (((msb.and(lsb)).not()).and(msb)).and(new BitVector(size(), Bits.test(palette, 5)));
		BitVector newColor2Lsb = (((msb.and(lsb)).not()).and(msb)).and(new BitVector(size(), Bits.test(palette, 4)));

		BitVector newColor1Msb = (((msb.and(lsb)).not()).and(lsb)).and(new BitVector(size(), Bits.test(palette, 3)));
		BitVector newColor1Lsb = (((msb.and(lsb)).not()).and(lsb)).and(new BitVector(size(), Bits.test(palette, 2)));

		BitVector newColor0Msb = ((msb.or(lsb)).not()).and(new BitVector(size(), Bits.test(palette, 1)));
		BitVector newColor0Lsb = ((msb.or(lsb)).not()).and(new BitVector(size(), Bits.test(palette, 0)));

		BitVector newMsb = ((newColor3Msb.or(newColor2Msb)).or(newColor1Msb)).or(newColor0Msb);
		BitVector newLsb = ((newColor3Lsb.or(newColor2Lsb)).or(newColor1Lsb)).or(newColor0Lsb);
		return new LcdImageLine(newMsb, newLsb, opacity);
	}

	/**
	 * returns the result of the combination of this line and a given line, using
	 * the opacity of the given line.
	 * 
	 * @param that
	 *            , the other line.
	 */
	public LcdImageLine below(LcdImageLine that) {
		return below(that, that.opacity);
	}

	/**
	 * returns the result of the combination of this line and a given line, using
	 * the given opacity.
	 * 
	 * @param that,
	 *            the other line.
	 * @param opcaity
	 *            used for the operation.
	 * @return LcdImageLine
	 */
	public LcdImageLine below(LcdImageLine that, BitVector givenOpacity) {
		Preconditions.checkArgument(that.size() == opacity.size());
		BitVector newMsb = (that.msb.and(givenOpacity)).or(this.msb.and(givenOpacity.not()));
		BitVector newLsb = (that.lsb.and(givenOpacity)).or(this.lsb.and(givenOpacity.not()));
		BitVector newOpacity = this.opacity.or(givenOpacity);
		return new LcdImageLine(newMsb, newLsb, newOpacity);
	}

	/**
	 * Does the same as the method {@link #below(LcdImageLine)} except starting from
	 * a given index.
	 * 
	 * @param that,
	 *            the other LcdImageLine
	 * @param index,
	 *            of the starting pixel @return, the joined line.
	 */
	public LcdImageLine join(LcdImageLine that, int index) {
		Preconditions.checkArgument(that.size() == opacity.size() && index >= 0 && index < size());
		BitVector b1 = new BitVector(size(), true).shift(index);
		LcdImageLine l = below(new LcdImageLine(that.msb, that.lsb, b1));
        BitVector newOpacity = (that.opacity().and(b1)).or(this.opacity.and(b1.not()));

		return new LcdImageLine(l.msb, l.lsb, newOpacity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object that) { // not sure

		if (that.getClass() != this.getClass())
			return false; // not sure, instanceof
		LcdImageLine thatLine = (LcdImageLine) that;
		BitVector[] tempoThis = { this.msb, this.lsb, this.opacity };
		BitVector[] tempoThat = { thatLine.msb, thatLine.lsb, thatLine.opacity };
		return Arrays.equals(tempoThis, tempoThat);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(new BitVector[] { this.msb, this.lsb, this.opacity }); // not sure
	}

	/**
	 * a line Builder.
	 *
	 * @author Karim Sabaa (269647)
	 * @author Mohamed Elasfoury (289473)
	 */
	public final static class Builder {
		private BitVector.Builder msbBuilding, lsbBuilding;
        private int size;
		/**
		 * Constructs a LcdImageLine of a certain size with all it's pixels equal to 0.
		 * 
		 * @param size
		 *            of the Line.
		 * @return
		 * @throws IllegalArgumentException
		 *             if the size is't a multiple of 32 or negative.
		 */
		public Builder(int size) throws IllegalArgumentException {
            Preconditions.checkArgument(size%Integer.SIZE==0);
			msbBuilding = new BitVector.Builder(size);
			lsbBuilding = new BitVector.Builder(size);
		    this.size = size;
		}

		/**
		 * define the value of the octet at the given index
		 *
		 *
		 * @param index
		 *            of the octet
		 * @throws IndexOutOfBoundsException
		 *             if the index of Octet! isn't correct.
		 * @throws IllegalArgumentException
		 *             if the given int isn't an octet.
		 */
		public Builder setBytes(int index, int msbOctet, int lsbOctet)
				throws IndexOutOfBoundsException, IllegalArgumentException {
			Preconditions.checkArgument(index>=0 && index < size/Byte.SIZE);
			Preconditions.checkBits8(msbOctet);
			Preconditions.checkBits8(lsbOctet);
			msbBuilding.setByte(index, Bits.reverse8(msbOctet));
			lsbBuilding.setByte(index, Bits.reverse8(lsbOctet));
			
			return this;
		}

		/**
		 * Builds the Line
		 */
		public LcdImageLine build() {
			BitVector msb = msbBuilding.build();
			BitVector lsb = lsbBuilding.build();
			return new LcdImageLine(msb, lsb,msb.or(lsb));
		}
	}

}

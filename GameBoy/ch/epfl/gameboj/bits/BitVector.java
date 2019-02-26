package ch.epfl.gameboj.bits;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

/**
 * Vector of bits, it's size is a multiple of 32.
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
 
public final class BitVector {
 
	public static void main(String[] args) {
		
	}
    /**
     * Constructs a BitVector of a certain size with a certain value in all of it's
     * bits.
     * 
     * @param size
     *            of the bits vector.
     * @param initialValue
     *            of all the bits of the vector.
     * @throws IllegalArgumentException
     *             if the size is't a multiple of 32 or negative.
     */
    public BitVector(int size, boolean initialValue) throws IllegalArgumentException {
 
        this(BitVectorUtile(size, initialValue));
 
    }
 
    /**
     * Constructs a BitVector of a certain size with all it's bits equal to 0.
     * 
     * @param size
     *            of the bits vector.
     * @throws IllegalArgumentException
     *             if the size is't a multiple of 32 or negative.
     */
    public BitVector(int size) throws IllegalArgumentException {
        this(size, false);
    }
 
    /**
     * 
     * @return size of the Vector
     */
    public int size() {
        int size = vector.length * Integer.SIZE;
        return size;
    }
 
    /**
     * test the given index's bit of the Vector
     * 
     * @param index
     * @return Value of bit.
     */
    public boolean testBit(int index) throws IllegalArgumentException {
        Preconditions.checkArgument(index >= 0 && index < size());
        return Bits.test(vector[indexTableau(index)], indexInt(index));
    }
 
    /**
     * 
     * @return Complement of the Vector
     */
    public BitVector not()  {
        return logicOperator(null, logicOperation.NOT);
    }
 
    /**
     * Does the logic and operation with the given BitVector.
     * 
     * @param that
     *            , the other BitVector.
     * @return result of operation.
     * @throws IllegalArgumentException
     *              if their sizes are different
     * @throws NullPointerException
     *              if parameter is null.  
     */
    public BitVector and(BitVector that)throws IllegalArgumentException, NullPointerException  {
        return logicOperator(that, logicOperation.AND);
    }
 
    /**
     * Does the logic operation or with the given BitVector
     * 
     * @param that
     *            , the other BitVector.
     * @return result of the operation.
     *  @throws IllegalArgumentException
     *              if their sizes are different
     * @throws NullPointerException
     *              if parameter is null.  
     */
    public BitVector or(BitVector that)throws IllegalArgumentException, NullPointerException {
        return logicOperator(that, logicOperation.OR);
 
    }
 
    /**
     * shifts this a given distance to the left if distance positive, else to the
     * right.
     * 
     * @param distance
     * @return this
     */
    public BitVector shift(int distance) {
        return extractZeroExtended(size(), -distance);
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
        return Arrays.equals(this.vector, ((BitVector) that).vector);
    }
 
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(vector);
    }
 
    /**
     * Returns the extraction of the zero-extended Vector version.
     * 
     * @param size
     *            of the extraction.
     * @param startIndex
     *            of the extraction
     * @return extraction result
     */
    public BitVector extractZeroExtended(int size, int startIndex) {
        return extract(size, startIndex, extension.ZERO_EXTENSION);
    }
 
    /**
     * Returns the extraction of the wrapped vector version.
     * 
     * @param size
     *            of the extraction
     * @param startIndex
     *            of the extraction
     * @return extraction result
     */
    public BitVector extractWrapped(int size, int startIndex) {
        return extract(size, startIndex, extension.WRAPE_EXTENSION);
    }
 
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < tableSize(); ++i) {
            b.append(Integer.toBinaryString(vector[i]));
        }
        return b.toString();
    }
   /*
    * (non-Javadoc)
    * @see java.lang.Object#clone()
    */
    @Override
    public BitVector clone() {
    	int [] tempo = new int [tableSize()];
    	for(int i =0; i< tableSize(); i++) {
    		tempo[i] = vector[i];
    	}
    	return new BitVector(tempo);
    }
   
    /**
     * BitVector builder. It's principal purpose is to permit the construction of
     * the vector each octet at a time.
     *
     * @author Karim Sabaa (269647)
     * @author Mohamed Elasfoury (289473)
     */
    public final static class Builder {
         
         
         private int[] tempo;
          
         /**
         * Constructs a BitVector of a certain size with all it's bits equal to 0.
         * 
         * @param size
         *            of the bits vector.
         * @return
         * @throws IllegalArgumentException
         *             if the size is't a multiple of 32 or negative.
         */
        public Builder(int size) throws IllegalArgumentException {
            Preconditions.checkArgument(size>=0 && size%Integer.SIZE ==0);
            tempo = new int[Math.floorDiv(size, Integer.SIZE)];
 
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
        public Builder setByte(int index, int that) throws IndexOutOfBoundsException, IllegalArgumentException {
            if(tempo==null) throw new IllegalStateException();
            Preconditions.checkBits8(that);
            if (!(index >= 0 && index * Byte.SIZE < tempo.length*Integer.SIZE))
                throw new IndexOutOfBoundsException();
             
            int indexTable = Math.floorDiv(index * Byte.SIZE, Integer.SIZE);
            for (int i = 0; i < Byte.SIZE; ++i) 
                tempo[indexTable] = Bits.set(tempo[indexTable], Math.floorMod(index * Byte.SIZE, Integer.SIZE) + i,
                        Bits.test(that, i));
            return this;
        }
 
        /**
         * Build the BitVector
         */
        public BitVector build() {
            if(tempo==null) throw new IllegalStateException();
            try {
            return new BitVector(tempo);
            }finally{
                tempo = null;
            }
        }
    }
    ///////////////////////////////////////////////End of Public interface/////////////////////////////////////////////////
 
     
     
     
     
     
     
     
    private int[] vector;
     
    private enum extension {
        ZERO_EXTENSION, WRAPE_EXTENSION
    }
 
    private enum logicOperation {
        NOT, OR, AND
    }
 
     
     
     
    private BitVector logicOperator(BitVector that, logicOperation l)  {
        if (l != logicOperation.NOT  ) {
            Objects.requireNonNull(that);
            Preconditions.checkArgument(that.size()== size());
            }
        int[] tempo = new int[tableSize()];
        for (int i = 0; i < tableSize(); ++i)
            switch (l) {
            case NOT:
                tempo[i] = vector[i] ^ -1;
                break;
            case OR:
                tempo[i] = vector[i] | that.vector[i];
                break;
            case AND:
                tempo[i] = vector[i] & that.vector[i];
                break;
            }
        return new BitVector(tempo);
    }
 
     
    public BitVector(int[] vectorElements) {
        vector = vectorElements;
    }
 
     
    private int tableSize() {
        return vector.length;
    }
 
     
    private int indexTableau(int i) {
        return Math.floorDiv(Math.floorMod(i, size()), Integer.SIZE);
 
    }
 
     
    private int indexInt(int i) {
        return Math.floorMod(i, Integer.SIZE);
    }
 
     
     
     
    private BitVector extract(int size, int startIndex, extension e) {
        Preconditions.checkArgument(size%Integer.SIZE==0);
    	int sizeTable =Math.floorDiv(size, Integer.SIZE);
        int[] tempo = new int[sizeTable];
        for (int i = 0; i < sizeTable; ++i)
            tempo[i] = intExtentionCalculator(size - Integer.SIZE * i, startIndex + i * Integer.SIZE, e);
        return new BitVector(tempo);
    }
 
     
     
     
     
    private int intExtentionCalculator(int size, int startIndex, extension e) {
        int result = 0;
        for (int i = 0; i < Math.min(size, Integer.SIZE); ++i)
            result = Bits.set(result, i, valueWithExtension(startIndex + i, e));
        return result;
    }
 
     
     
     
     
     
    private boolean valueWithExtension(int index, extension e) {
        if (index >= 0 && index < size())
            return Bits.test(vector[indexTableau(index)], indexInt(index));
        else {
            if (e == extension.ZERO_EXTENSION)
                return false;
            return Bits.test(vector[indexTableau(index)], indexInt(index));
        }
    }
 
     
     
     
     
    private static int[] BitVectorUtile(int size, boolean initialValue) throws IllegalArgumentException {
        Preconditions.checkArgument(size > 0 && size % Integer.SIZE == 0);
        int[] vectorElements = new int[Math.floorDiv(size, Integer.SIZE)];
        if (initialValue)
            for (int i = 0; i < Math.floorDiv(size, Integer.SIZE); ++i)
                vectorElements[i] = -1;
        return vectorElements;
    }
 
   
}
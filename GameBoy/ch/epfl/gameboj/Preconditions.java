package ch.epfl.gameboj;

public interface Preconditions {

    /**
     * Preconditions.
     *
     * @author Asfoury.(289473)
     * @autor Karim.(269647)
     * 
     */

    /**
     * 
     * @param the
     *            expressions to be checked
     * @throws IllegalArgumentException
     *             if the expression is false
     * 
     */
    public static void checkArgument(boolean b)
            throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException();
        }

    }

    /**
     * 
     * @param the
     *            int to be checked
     * @throws IllegalArgumentException
     *             if the number is larger than FF(base16)
     * @return the int if it is equal to or less than FF
     */
    public static int checkBits8(int v) throws IllegalArgumentException {
    	
    	checkArgument(v <= 0xFF && v >= 0);
        return v;
    }

    /**
     * 
     * @param the
     *            int to be checked
     * @throws IllegalArgumentException
     *             if the number is larger than FFFF(base16)
     * @return the int if it is equal to or less than FFFF
     */
    public static int checkBits16(int v) throws IllegalArgumentException {
        
    	checkArgument(!(v > 0xFFFF || v < 0x0));

        return v;
    }

}

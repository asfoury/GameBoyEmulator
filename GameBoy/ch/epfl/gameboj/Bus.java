package ch.epfl.gameboj;

import java.util.ArrayList;
import java.util.Objects;

import ch.epfl.gameboj.component.Component;

/**
 * Bus. the wires between components
 * 
 * @author Asfoury.(289473)
 * @autor Karim.(269647)
 * 
 **/
public final class Bus {

    private final ArrayList<Component> component = new ArrayList<Component>();

    /**
     * Attaches the component to this bus .
     *
     * @param the
     *            component that will get attached .
     * @throws NullPointerException
     *             if the component is null
     * 
     *             *
     */
    public void attach(Component component) throws NullPointerException {
        Objects.requireNonNull(component);
        this.component.add(component);

    }

    /**
     *Reads the data at the given address 
     * 
     *
     * @param the address of the byte.
     * @throws IllegalArgumentException
     *             if address is not a 16 bits value.
     */
    public int read(int address) {
        Preconditions.checkBits16(address);
       
        for(Component c : component) {
        	 int addressRead = c.read(address);
            if(addressRead != Component.NO_DATA)
                return addressRead ;
        }
        return 0xFF;
    }

    /**
     * Writes data at the given address
     *
     * @param data to be written and the address where it will be written
     *        
     * @throws IllegalArgumentException if the data or the address are not in the correct sizes(16bits and 8bits resp.)
     * 
     *             *
     */
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        
        for(Component c:component) {
           c.write(address, data);
        }

    }
}

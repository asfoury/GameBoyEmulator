package ch.epfl.gameboj.component.memory;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;

public final class RamController implements Component 
{
    

    /**
     * RamController class
     *
     * @author Asfoury.(289473)
     * @autor Karim.(269647)
     * 
     */
	
	 private final Ram ram;
	 private final int startAddress;
	 private final int endAddress;

	 /**
	     * Constructs the controller
	     * 
	     *
	     * @param the memory, the starting accessible address and end address.
	     */
	public RamController(Ram ram, int startAddress, int endAddress) throws IllegalArgumentException
	{
		Objects.requireNonNull(ram);
		Preconditions.checkBits16(startAddress);
		Preconditions.checkBits16(endAddress-1);
		Preconditions.checkArgument(!(endAddress - startAddress < 0 ||  endAddress - startAddress > ram.size()));
	
		this.ram = ram;
		this.startAddress = startAddress;
		this.endAddress = endAddress ;
		
	}
	 /**
	     * Constructs the controller
	     * 
	     *
	     * @param the memory, the starting accessible address and it goes till the end of memory.
	     */
	 public RamController(Ram ram, int startAddress)
		{
			 this(ram, startAddress, ram.size() + startAddress);
		}
	
	 /**
	     * Gets the value in the memory at a specific address( only if it's accessible).
	     * 
	     *
	     * @param the address of the value searched.
	     * @return the value searched.
	     */
	public int read(int address) 
	{
	    Preconditions.checkBits16(address);
		
	    if(address>= startAddress && address < endAddress)
		{
		   return ram.read(address - startAddress);
		}
		   else {
			   return Component.NO_DATA;
		   }
		  
	}

	 /**
     * Writes a new value in the memory at a specific address( only if it's accessible).
     * 
     *
     * @param the value and the address where it will be written.
     */
	public void write(int address, int data) 
	{
		Preconditions.checkBits16(address);
	    Preconditions.checkBits8(data);
		if(address>= startAddress && address < endAddress)
		{
	
	    ram.write(address - startAddress, data); 
	  
		}
	}
}

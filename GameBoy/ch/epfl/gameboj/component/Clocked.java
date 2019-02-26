package ch.epfl.gameboj.component;

/**
 * This interface represents a component piloted by the system's clock.
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public interface Clocked 
{
    /**
     * Demand the component to progress while executing 
     * all the operations that it must do during the given cycle. 
     * @param cycle
     */
	void cycle (long cycle);
	
}

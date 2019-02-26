package ch.epfl.gameboj.component;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

/**
 * Represents a component connected to the bus and piloted by the system's
 * clock.
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public final class Timer implements Component, Clocked {

	private Cpu cpu;
	private int DIV, TIMA, TMA, TAC; // DIV is the Principal Counter
    private int maxTIMA = 0xFF;
	/**
	 * Constructs a Timer associated to the given Cpu.
	 * 
	 * @param Cpu
	 * @throws NullPointerException
	 *             if the Cpu is null
	 */
	public Timer(Cpu cpu) throws NullPointerException {
		if (cpu == null)
			throw new NullPointerException("The given Cpu is Null");
		this.cpu = cpu;
		this.DIV = 0;
		this.TIMA = 0;
		this.TMA = 0;
		this.TAC = 0;

	}

	/**
	 * Increments the principal counter by four every cycle. Reset it back to zero
	 * when it hits it's maximum value (0xFFFF).
	 */
	@Override
	public void cycle(long cycle) {
		boolean s0 = state();
		this.DIV = Bits.clip(16, this.DIV + 4);
		incIfChange(s0);
	}

	/**
	 * Reads from registers. In case of the principal counter, it reads only the 8
	 * most significant bits.
	 * 
	 * @return register value or NO_DATA.
	 * @throws IllegalArgumentException
	 *             if the address isn't 16 bits.
	 */
	@Override
	public int read(int address) throws IllegalArgumentException {
		Preconditions.checkBits16(address);
		switch (address) {
		case AddressMap.REG_DIV:
			return Bits.extract(this.DIV, 8, 8);
		case AddressMap.REG_TIMA:
			return this.TIMA;
		case AddressMap.REG_TMA:
			return this.TMA;
		case AddressMap.REG_TAC:
			return this.TAC;
		}
		return Component.NO_DATA;
	}

	/**
	 * Writes in the Registers. In case of the principal counter, whatever the data
	 * is, it will reset DIV to zero.
	 * 
	 * @throws IllegalArgumentException
	 *             if address isn't 16 bits or data isn't 8 bits.
	 */
	@Override
	public void write(int address, int data) throws IllegalArgumentException {
		Preconditions.checkBits16(address);
		Preconditions.checkBits8(data);
		boolean s0 = state();
		switch (address) {
		case AddressMap.REG_DIV: {
			this.DIV = 0;
		}
			break;
		case AddressMap.REG_TIMA: {
			this.TIMA = data;
		}
			break;
		case AddressMap.REG_TMA: {
			this.TMA = data;
		}
			break;
		case AddressMap.REG_TAC: {
			this.TAC = data;
		}
			break;
		}
		incIfChange(s0);
	}

	/**
	 * returns the state of the Timer, which is a logic operation between a given
	 * bit in the principal counter ( indexed by the 2 least significant bits of the
	 * register TAC and a bijective map) and the bit 2 of the register TAC
	 * 
	 * @return state a boolean representing the state of the timer.
	 */
	private boolean state() {
		boolean bit2TAC = Bits.test(this.TAC, 2);
		int index = Bits.clip(2, this.TAC);
		switch (index) {
		case 0b00: {

			return bit2TAC && Bits.test(this.DIV, 9);
		}

		case 0b01: {

			return bit2TAC && Bits.test(this.DIV, 3);
		}

		case 0b10: {

			return bit2TAC && Bits.test(this.DIV, 5);
		}

		case 0b11: {

			return bit2TAC && Bits.test(this.DIV, 7);
		}

		default:

			return false;
		}
	}
     /**
      * If the state of this timer passes from true to false 
      * it increments the secondary counter by 1 . Until it reaches 
      * it's maximum value (0xFF) and it does the following:
      * 1- resets TIMA to the value in the register TMA
      * 2- requests a Timer Interruption.
      * @param etat represents the last state of the Timer
      */
	private void incIfChange(boolean etat) {

		if (etat == true && state() == false) {
			if (this.TIMA == this.maxTIMA) {
				this.TIMA = this.TMA;
				cpu.requestInterrupt(Interrupt.TIMER);

			} else {

				this.TIMA += 1;
			}
		}
	}
}

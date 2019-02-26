package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Alu.RotDir;
import ch.epfl.gameboj.component.cpu.Opcode.Kind;
import ch.epfl.gameboj.component.memory.Ram;

/**
 * Represents the processor of the gameBoy.
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public final class Cpu implements Component, Clocked {

	/// ***/// Public interface beginning ///***///

	public enum Interrupt implements Bit {
		VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD
	}

	/**
	 * 
	 * @return table containing the Value of the following registers in order:PC,
	 *         SP, A, F, B, C, D, E, H and L.
	 */
	public int[] _testGetPcSpAFBCDEHL() {

		int values[] = { PC, SP, registerFile.get(Reg.A), registerFile.get(Reg.F), registerFile.get(Reg.B),
				registerFile.get(Reg.C), registerFile.get(Reg.D), registerFile.get(Reg.E), registerFile.get(Reg.H),
				registerFile.get(Reg.L)

		};
		return values;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Clocked#cycle(long)
	 */
	@Override
	public void cycle(long cycle) {
		int tempo = Alu.unpackValue(Alu.and(IF, IE));
		if ((nextNonIdleCycle == Long.MAX_VALUE) && (Integer.lowestOneBit(tempo) != 0)) {
			this.nextNonIdleCycle = cycle;
		}

		if (cycle < nextNonIdleCycle)
			return;
		this.reallyCycle();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#attachTo(ch.epfl.gameboj.Bus)
	 */
	@Override
	public void attachTo(Bus bus) {
		this.bus = bus;
		bus.attach(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#read(int)
	 */
	@Override
	public int read(int address) throws IllegalArgumentException {
		Preconditions.checkBits16(address);
		if (address == AddressMap.REG_IF)
			return this.IF;
		if (address == AddressMap.REG_IE)
			return this.IE;
		if (address >= AddressMap.HIGH_RAM_START && address < AddressMap.HIGH_RAM_END)
			return this.highRam.read(address - AddressMap.HIGH_RAM_START);

		return NO_DATA;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#write(int, int)
	 */
	@Override
	public void write(int address, int data) {

		Preconditions.checkBits16(address);
		Preconditions.checkBits8(data);
		if (address == AddressMap.REG_IF)
			this.IF = data;

		if (address == AddressMap.REG_IE)
			this.IE = data;

		if (address >= AddressMap.HIGH_RAM_START && address < AddressMap.HIGH_RAM_END)
			this.highRam.write(address - AddressMap.HIGH_RAM_START, data);

	}

	/**
	 * Throw an interruption by putting to 1 the corresponding interruption one the
	 * register IF.
	 * 
	 * @param Interruption
	 */
	public void requestInterrupt(Interrupt interruption) {
		this.IF = Bits.set(this.IF, interruption.index(), true);

	}

	/// ***/// Public interface end ///***///

	private enum Direction {
		LEFT, RIGHT
	}

	private enum Reg implements Register {
		A, F, B, C, D, E, H, L
	}

	private enum Reg16 implements Register {
		AF, BC, DE, HL
	}

	private enum FlagSrc {
		V0, V1, ALU, CPU
	}

	private static final Opcode[] DIRECT_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.DIRECT);
	private static final Opcode[] PREFIXED_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.PREFIXED);
	private RegisterFile<Reg> registerFile = new RegisterFile<>(Reg.values());

	private long nextNonIdleCycle = 0;
	private int PC, SP, IE, IF = 0;

	private boolean IME = false;

	private Ram highRam = new Ram(AddressMap.HIGH_RAM_SIZE);

	private Bus bus;

	private void reallyCycle() {
		int tempo = Alu.unpackValue(Alu.and(IF, IE));
		if (this.IME && Integer.lowestOneBit(tempo) != 0) {

			this.IME = false;

			int indexInterruption = Integer.SIZE - Integer.numberOfLeadingZeros(Integer.lowestOneBit(tempo)) - 1;

			this.IF = Bits.set(this.IF, indexInterruption, false);
			this.push16(PC);

			this.PC = AddressMap.INTERRUPTS[indexInterruption];

			this.nextNonIdleCycle += 5;

		} else {
			this.dispatch(this.bus.read(Bits.clip(16, this.PC)));
		}
	}

	private void dispatch(int instruction) {

		Opcode opcode = (instruction == 0xCB) ? PREFIXED_OPCODE_TABLE[read8AfterOpcode()]
				: DIRECT_OPCODE_TABLE[instruction];

		int PCprime = Bits.clip(16, this.PC + opcode.totalBytes);

		switch (opcode.family) {
		case NOP: {
		}
			break;
		case LD_R8_HLR: {
			Reg r8 = extractReg(opcode, 3);
			registerFile.set(r8, read8AtHl());
		}
			break;
		case LD_A_HLRU: {
			registerFile.set(Reg.A, read8AtHl());
			setReg16(Reg16.HL, reg16(Reg16.HL) + extractHlIncrement(opcode));
		}
			break;
		case LD_A_N8R: {
			int n8 = read8AfterOpcode();
			registerFile.set(Reg.A, read8(Bits.clip(16, AddressMap.REGS_START + n8)));
		}
			break;
		case LD_A_CR: {
			registerFile.set(Reg.A, read8(Bits.clip(16, AddressMap.REGS_START + registerFile.get(Reg.C))));
		}
			break;
		case LD_A_N16R: {
			int n16 = read16AfterOpcode();
			registerFile.set(Reg.A, read8(Bits.clip(16, n16)));
		}
			break;
		case LD_A_BCR: {
			registerFile.set(Reg.A, read8(reg16(Reg16.BC)));
		}
			break;
		case LD_A_DER: {
			registerFile.set(Reg.A, read8(reg16(Reg16.DE)));
		}
			break;
		case LD_R8_N8: {

			int n8 = read8AfterOpcode();
			Reg r8 = extractReg(opcode, 3);
			registerFile.set(r8, n8);
		}
			break;
		case LD_R16SP_N16: {
			Reg16 r16 = extractReg16(opcode);
			int n16 = read16AfterOpcode();
			setReg16SP(r16, n16);
		}
			break;
		case POP_R16: {
			Reg16 r16 = extractReg16(opcode);
			setReg16(r16, pop16());
		}
			break;
		case LD_HLR_R8: {
			Reg r8 = extractReg(opcode, 0);
			write8AtHl(registerFile.get(r8));
		}
			break;
		case LD_HLRU_A: {
			write8AtHl(registerFile.get(Reg.A));
			setReg16(Reg16.HL, reg16(Reg16.HL) + extractHlIncrement(opcode));
		}
			break;
		case LD_N8R_A: {
			write8(AddressMap.REGS_START + read8AfterOpcode(), registerFile.get(Reg.A));
		}
			break;
		case LD_CR_A: {
			write8(AddressMap.REGS_START + registerFile.get(Reg.C), registerFile.get(Reg.A));
		}
			break;
		case LD_N16R_A: {
			write8(read16AfterOpcode(), registerFile.get(Reg.A));
		}
			break;
		case LD_BCR_A: {
			write8(reg16(Reg16.BC), registerFile.get(Reg.A));
		}
			break;
		case LD_DER_A: {
			write8(reg16(Reg16.DE), registerFile.get(Reg.A));
		}
			break;
		case LD_HLR_N8: {
			write8AtHl(read8AfterOpcode());
		}
			break;
		case LD_N16R_SP: {
			write16(read16AfterOpcode(), SP);
		}
			break;
		case LD_R8_R8: {
			registerFile.set(extractReg(opcode, 3), registerFile.get(extractReg(opcode, 0)));
		}
			break;
		case LD_SP_HL: {
			SP = reg16(Reg16.HL);
		}
			break;
		case PUSH_R16: {
			push16(reg16(extractReg16(opcode)));
		}
			break;
		// Add
		case ADD_A_R8: {
			Reg reg = this.extractReg(opcode, 0);
			int sum = Alu.add(registerFile.get(reg), registerFile.get(Reg.A), getCarry(opcode));
			this.setRegFromAlu(Reg.A, sum);
			this.combineAluFlags(sum, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;
		case ADD_A_N8: {
			int n8 = this.read8AfterOpcode();
			int sum = Alu.add(registerFile.get(Reg.A), n8, getCarry(opcode));
			this.setRegFromAlu(Reg.A, sum);
			this.combineAluFlags(sum, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;
		case ADD_A_HLR: {

			int sum = Alu.add(this.read8AtHl(), registerFile.get(Reg.A), getCarry(opcode));
			this.setRegFromAlu(Reg.A, sum);
			this.combineAluFlags(sum, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;
		case INC_R8: {

			Reg reg = this.extractReg(opcode, 3);
			int sum = Alu.add(registerFile.get(reg), 1);
			this.setRegFromAlu(reg, sum);
			this.combineAluFlags(sum, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);

		}
			break;
		case INC_HLR: {

			int valueFlags = Alu.add(this.read8AtHl(), 1);
			this.write8AtHl(Alu.unpackValue(valueFlags));
			this.combineAluFlags(valueFlags, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);

		}
			break;
		case INC_R16SP: {

			Reg16 r16 = this.extractReg16(opcode);
			if (r16 == Reg16.AF) {
				int valueFlags = Alu.add16L(SP, 1);
				this.setReg16SP(r16, Alu.unpackValue(valueFlags));
			} else {
				int valueFlags = Alu.add16L(reg16(r16), 1);
				this.setReg16SP(r16, Alu.unpackValue(valueFlags));
			}

		}
			break;
		case ADD_HL_R16SP: {

			Reg16 r16 = this.extractReg16(opcode);
			if (r16 == Reg16.AF) {
				int valueFlags = Alu.add16H(SP, reg16(Reg16.HL));
				this.setReg16(Reg16.HL, Alu.unpackValue(valueFlags));
				this.combineAluFlags(valueFlags, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
			} else {
				int valueFlags = Alu.add16H(reg16(r16), reg16(Reg16.HL));
				this.setReg16(Reg16.HL, Alu.unpackValue(valueFlags));
				this.combineAluFlags(valueFlags, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
			}
		}
			break;
		case LD_HLSP_S8: {

			int e8 = Bits.clip(16, Bits.signExtend8(this.read8AfterOpcode()));
			int sum = Alu.add16L(SP, e8);

			if (Bits.test(opcode.encoding, 4)) {
				this.setReg16(Reg16.HL, Alu.unpackValue(sum));
			} else {
				SP = Alu.unpackValue(sum);
			}

			combineAluFlags(sum, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;

		// Subtract
		case SUB_A_R8: {
			Reg r8 = this.extractReg(opcode, 0);
			int result = Alu.sub(registerFile.get(Reg.A), registerFile.get(r8), getCarry(opcode));
			registerFile.set(Reg.A, Alu.unpackValue(result));
			this.combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);
		}
			break;
		case SUB_A_N8: {
			int n8 = this.read8AfterOpcode();
			int result = Alu.sub(registerFile.get(Reg.A), n8, getCarry(opcode));
			registerFile.set(Reg.A, Alu.unpackValue(result));

			this.combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);
		}
			break;
		case SUB_A_HLR: {
			int busHL = this.read8AtHl();
			int result = Alu.sub(registerFile.get(Reg.A), busHL, getCarry(opcode));

			registerFile.set(Reg.A, Alu.unpackValue(result));
			this.combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);
		}
			break;
		case DEC_R8: {

			Reg r8 = this.extractReg(opcode, 3);
			int result = Alu.sub(registerFile.get(r8), 1);
			registerFile.set(r8, Alu.unpackValue(result));
			this.combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);

		}
			break;
		case DEC_HLR: {

			int busHL = this.read8AtHl();
			int result = Alu.sub(busHL, 1);
			this.write8AtHl(Alu.unpackValue(result));
			this.combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);

		}
			break;
		case CP_A_R8: {
			Reg r8 = this.extractReg(opcode, 0);
			int result = Alu.sub(registerFile.get(Reg.A), registerFile.get(r8));
			this.combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;
		case CP_A_N8: {
			int n8 = this.read8AfterOpcode();
			int result = Alu.sub(registerFile.get(Reg.A), n8);
			this.combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);

		}
			break;
		case CP_A_HLR: {

			int busHL = this.read8AtHl();
			int result = Alu.sub(registerFile.get(Reg.A), busHL);
			this.combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);
		}
			break;
		case DEC_R16SP: {

			Reg16 r16 = this.extractReg16(opcode);

			if (r16 == Reg16.AF) {
				int result = Alu.add16L(SP, Bits.clip(16, -1));

				this.setReg16SP(r16, Alu.unpackValue(result));
			} else {
				int result = Alu.add16L(reg16(r16), Bits.clip(16, -1));

				this.setReg16SP(r16, Alu.unpackValue(result));
			}

		}
			break;

		// And, or, xor, complement
		case AND_A_N8: {
			int A = registerFile.get(Reg.A);
			int n = this.read8AfterOpcode();
			int valueFlag = Alu.and(A, n);
			this.setRegFromAlu(Reg.A, valueFlag);
			this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.V0);

		}
			break;
		case AND_A_R8: {
			int A = registerFile.get(Reg.A);
			Reg r8 = this.extractReg(opcode, 0);
			int valueR8 = registerFile.get(r8);
			int valueFlag = Alu.and(A, valueR8);
			this.setRegFromAlu(Reg.A, valueFlag);
			this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.V0);

		}
			break;
		case AND_A_HLR: {
			int A = registerFile.get(Reg.A);
			int busHL = this.read8AtHl();
			int valueFlag = Alu.and(A, busHL);
			this.setRegFromAlu(Reg.A, valueFlag);
			this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.V0);
		}
			break;
		case OR_A_R8: {
			int A = registerFile.get(Reg.A);
			Reg r8 = this.extractReg(opcode, 0);
			int valueR8 = registerFile.get(r8);
			int valueFlag = Alu.or(A, valueR8);
			this.setRegFromAlu(Reg.A, valueFlag);
			this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
		}
			break;
		case OR_A_N8: {
			int A = registerFile.get(Reg.A);
			int n = this.read8AfterOpcode();
			int valueFlag = Alu.or(A, n);
			this.setRegFromAlu(Reg.A, valueFlag);
			this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);

		}
			break;
		case OR_A_HLR: {
			int A = registerFile.get(Reg.A);
			int busHL = this.read8AtHl();
			int valueFlag = Alu.or(A, busHL);
			this.setRegFromAlu(Reg.A, valueFlag);
			this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
		}
			break;
		case XOR_A_R8: {
			int A = registerFile.get(Reg.A);
			Reg r8 = this.extractReg(opcode, 0);
			int valueR8 = registerFile.get(r8);
			int valueFlag = Alu.xor(A, valueR8);
			this.setRegFromAlu(Reg.A, valueFlag);
			this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
		}
			break;
		case XOR_A_N8: {
			int A = registerFile.get(Reg.A);
			int n = this.read8AfterOpcode();
			int valueFlag = Alu.xor(A, n);
			this.setRegFromAlu(Reg.A, valueFlag);
			this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
		}
			break;
		case XOR_A_HLR: {
			int A = registerFile.get(Reg.A);
			int busHL = this.read8AtHl();
			int valueFlag = Alu.xor(A, busHL);
			this.setRegFromAlu(Reg.A, valueFlag);
			this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
		}
			break;
		case CPL: {
			int A = registerFile.get(Reg.A);
			int cplA = Bits.complement8(A);
			registerFile.set(Reg.A, cplA);
			this.combineAluFlags(cplA, FlagSrc.CPU, FlagSrc.V1, FlagSrc.V1, FlagSrc.CPU);
		}
			break;

		// Rotate, shift
		case ROTCA: {
			int A = registerFile.get(Reg.A);
			if (this.directionRot(opcode).equals(Direction.LEFT)) {
				int valueFlag = Alu.rotate(RotDir.LEFT, A);
				this.setRegFromAlu(Reg.A, valueFlag);
				this.combineAluFlags(valueFlag, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			} else {
				int valueFlag = Alu.rotate(RotDir.RIGHT, A);
				this.setRegFromAlu(Reg.A, valueFlag);
				this.combineAluFlags(valueFlag, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			}
		}
			break;
		case ROTA: {
			int A = registerFile.get(Reg.A);
			boolean carry = Bits.test(registerFile.get(Reg.F), 4);
			if (this.directionRot(opcode).equals(Direction.LEFT)) {
				int valueFlag = Alu.rotate(RotDir.LEFT, A, carry);
				this.setRegFromAlu(Reg.A, valueFlag);
				this.combineAluFlags(valueFlag, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			} else {
				int valueFlag = Alu.rotate(RotDir.RIGHT, A, carry);
				this.setRegFromAlu(Reg.A, valueFlag);
				this.combineAluFlags(valueFlag, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			}
		}
			break;
		case ROTC_R8: {
			Reg r = this.extractReg(opcode, 0);
			int valueR = registerFile.get(r);
			if (this.directionRot(opcode).equals(Direction.LEFT)) {
				int valueFlag = Alu.rotate(RotDir.LEFT, valueR);
				this.setRegFromAlu(r, valueFlag);
				this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			} else {
				int valueFlag = Alu.rotate(RotDir.RIGHT, valueR);
				this.setRegFromAlu(r, valueFlag);
				this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			}

		}
			break;
		case ROT_R8: {
			Reg r = this.extractReg(opcode, 0);
			int valueR = registerFile.get(r);
			boolean carry = Bits.test(registerFile.get(Reg.F), 4);
			if (this.directionRot(opcode).equals(Direction.LEFT)) {
				int valueFlag = Alu.rotate(RotDir.LEFT, valueR, carry);
				this.setRegFromAlu(r, valueFlag);
				this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			} else {
				int valueFlag = Alu.rotate(RotDir.RIGHT, valueR, carry);
				this.setRegFromAlu(r, valueFlag);
				this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			}
		}
			break;
		case ROTC_HLR: {
			int busHL = this.read8AtHl();
			if (this.directionRot(opcode).equals(Direction.LEFT)) {
				int valueFlag = Alu.rotate(RotDir.LEFT, busHL);
				int value = Alu.unpackValue(valueFlag);
				this.write8AtHl(value);
				this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			} else {
				int valueFlag = Alu.rotate(RotDir.RIGHT, busHL);
				int value = Alu.unpackValue(valueFlag);
				this.write8AtHl(value);
				this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			}
		}
			break;
		case ROT_HLR: {
			int busHL = this.read8AtHl();
			boolean carry = Bits.test(registerFile.get(Reg.F), 4);
			if (this.directionRot(opcode).equals(Direction.LEFT)) {
				int valueFlag = Alu.rotate(RotDir.LEFT, busHL, carry);
				int value = Alu.unpackValue(valueFlag);
				this.write8AtHl(value);
				this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			} else {
				int valueFlag = Alu.rotate(RotDir.RIGHT, busHL, carry);
				int value = Alu.unpackValue(valueFlag);
				this.write8AtHl(value);
				this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			}
		}
			break;
		case SWAP_R8: {
			Reg r8 = this.extractReg(opcode, 0);
			int valueR8 = registerFile.get(r8);
			int valueFlag = Alu.swap(valueR8);
			registerFile.set(r8, Alu.unpackValue(valueFlag));
			this.combineAluFlags(valueFlag, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);

		}
			break;
		case SWAP_HLR: {
			int busHL = this.read8AtHl();
			int valueFlags = Alu.swap(busHL);
			int value = Alu.unpackValue(valueFlags);
			this.write8AtHl(value);
			this.combineAluFlags(valueFlags, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);

		}
			break;
		case SLA_R8: {
			Reg r8 = this.extractReg(opcode, 0);
			int valueR8 = registerFile.get(r8);
			int valueFlags = Alu.shiftLeft(valueR8);
			int value = Alu.unpackValue(valueFlags);
			registerFile.set(r8, value);
			this.combineAluFlags(valueFlags, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
		}
			break;
		case SRA_R8: {
			Reg r8 = this.extractReg(opcode, 0);
			int valueR8 = registerFile.get(r8);
			int valueFlags = Alu.shiftRightA(valueR8);
			int value = Alu.unpackValue(valueFlags);
			registerFile.set(r8, value);
			this.combineAluFlags(valueFlags, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);

		}
			break;
		case SRL_R8: {
			Reg r8 = this.extractReg(opcode, 0);
			int valueR8 = registerFile.get(r8);
			int valueFlags = Alu.shiftRightL(valueR8);
			int value = Alu.unpackValue(valueFlags);
			registerFile.set(r8, value);
			this.combineAluFlags(valueFlags, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
		}
			break;
		case SLA_HLR: {
			int busHL = this.read8AtHl();
			int valueFlags = Alu.shiftLeft(busHL);
			int value = Alu.unpackValue(valueFlags);
			this.write8AtHl(value);
			this.combineAluFlags(valueFlags, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
		}
			break;
		case SRA_HLR: {
			int busHL = this.read8AtHl();
			int valueFlags = Alu.shiftRightA(busHL);
			int value = Alu.unpackValue(valueFlags);
			this.write8AtHl(value);
			this.combineAluFlags(valueFlags, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
		}
			break;
		case SRL_HLR: {
			int busHL = this.read8AtHl();
			int valueFlags = Alu.shiftRightL(busHL);
			int value = Alu.unpackValue(valueFlags);
			this.write8AtHl(value);
			this.combineAluFlags(valueFlags, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
		}
			break;

		// Bit test and set
		case BIT_U3_R8: {
			Reg r8 = this.extractReg(opcode, 0);
			int valueR8 = registerFile.get(r8);
			int index = this.bitIndexToBeTestedModified(opcode);
			int valueFlagZ = Alu.testBit(valueR8, index);
			this.combineAluFlags(valueFlagZ, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);
		}
			break;
		case BIT_U3_HLR: {
			int busHL = this.read8AtHl();
			int index = this.bitIndexToBeTestedModified(opcode);
			int valueFlagZ = Alu.testBit(busHL, index);
			this.combineAluFlags(valueFlagZ, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);
		}
			break;
		case CHG_U3_R8: {
			Reg r8 = this.extractReg(opcode, 0);
			int valueR8 = registerFile.get(r8);
			int n = this.bitIndexToBeTestedModified(opcode);
			if (Bits.test(opcode.encoding, 6)) {
				int valueFlags = Alu.or(valueR8, 1 << n);
				int value = Alu.unpackValue(valueFlags);
				registerFile.set(r8, value);
			} else {
				int valueFlags = Alu.and(valueR8, Bits.complement8(1 << n));
				int value = Alu.unpackValue(valueFlags);
				registerFile.set(r8, value);
			}
		}
			break;
		case CHG_U3_HLR: {
			int busHL = this.read8AtHl();
			int n = this.bitIndexToBeTestedModified(opcode);
			if (Bits.test(opcode.encoding, 6)) {
				int valueFlags = Alu.or(busHL, 1 << n);
				int value = Alu.unpackValue(valueFlags);
				this.write8AtHl(value);
			} else {
				int valueFlags = Alu.and(busHL, Bits.complement8(1 << n));
				int value = Alu.unpackValue(valueFlags);
				this.write8AtHl(value);
			}
		}
			break;

		// Misc. ALU
		case DAA: {
			int A = registerFile.get(Reg.A);
			int F = registerFile.get(Reg.F);
			boolean n = Bits.test(F, Alu.Flag.N);
			boolean h = Bits.test(F, Alu.Flag.H);
			boolean c = Bits.test(F, Alu.Flag.C);
			int valueFlags = Alu.bcdAdjust(A, n, h, c);
			registerFile.set(Reg.A, Alu.unpackValue(valueFlags));
			this.combineAluFlags(valueFlags, FlagSrc.ALU, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU);
		}
			break;
		case SCCF: {
			if (Bits.test(opcode.encoding, 3) && Bits.test(registerFile.get(Reg.F), Alu.Flag.C)) {

				this.combineAluFlags(0, FlagSrc.CPU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
			} else {
				this.combineAluFlags(0, FlagSrc.CPU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V1);
			}
		}
			break;
		// Jumps
		case JP_HL: {
			PCprime = this.reg16(Reg16.HL);
		}
			break;
		case JP_N16: {
			PCprime = this.read16AfterOpcode();
		}
			break;
		case JP_CC_N16: {
			if (this.determineCondition(opcode)) {
				PCprime = this.read16AfterOpcode();
				this.nextNonIdleCycle += opcode.additionalCycles;
			}
		}
			break;
		case JR_E8: {
			PCprime = this.PC + opcode.totalBytes + Bits.signExtend8(this.read8AfterOpcode());
		}
			break;
		case JR_CC_E8: {
			if (this.determineCondition(opcode)) {
				PCprime = this.PC + opcode.totalBytes + Bits.signExtend8(this.read8AfterOpcode());

				this.nextNonIdleCycle += opcode.additionalCycles;
			}
		}
			break;

		// Calls and returns
		case CALL_N16: {
			push16(this.PC + opcode.totalBytes);
			PCprime = this.read16AfterOpcode();
		}
			break;
		case CALL_CC_N16: {
			if (this.determineCondition(opcode)) {
				this.push16(this.PC + opcode.totalBytes);
				PCprime = this.read16AfterOpcode();
				this.nextNonIdleCycle += opcode.additionalCycles;
			}

		}
			break;
		case RST_U3: {
			push16(PCprime);

			PCprime = AddressMap.RESETS[this.bitIndexToBeTestedModified(opcode)];
		}
			break;
		case RET: {
			PCprime = this.pop16();

		}
			break;
		case RET_CC: {
			if (this.determineCondition(opcode)) {
				PCprime = this.pop16();
				this.nextNonIdleCycle += opcode.additionalCycles;
			}
		}
			break;

		// Interrupts
		case EDI: {

			if (Bits.test(opcode.encoding, 3)) {
				this.IME = true;

			} else {
				this.IME = false;

			}

		}
			break;
		case RETI: {
			this.IME = true;
			PCprime = this.pop16();

		}
			break;

		// Misc control
		case HALT: {
			this.nextNonIdleCycle = Long.MAX_VALUE;

		}
			break;
		case STOP:
			throw new Error("STOP is not implemented");
		default:
			throw new IllegalArgumentException();
		}

		nextNonIdleCycle += opcode.cycles;
		PC = PCprime;

	}

	private int read8(int address) {
		return this.bus.read(address);
	}

	private int read8AtHl() {
		return this.bus.read(reg16(Reg16.HL));
	}

	private int read8AfterOpcode() {
		return this.bus.read(Bits.clip(16, PC + 1));
	}

	private int read16(int address) {
		return Bits.make16(read8(address + 1), read8(address));
	}

	private int read16AfterOpcode() {
		return read16(Bits.clip(16, PC + 1));
	}

	private void write8(int address, int v) {

		this.bus.write(address, v);
	}

	private void write16(int address, int v) {
		int first8 = Bits.clip(8, v);
		int second8 = Bits.extract(v, 8, 8);

		this.bus.write(address, first8);
		this.bus.write(address + 1, second8);
	}

	private void write8AtHl(int v) {
		this.bus.write(reg16(Reg16.HL), v);
	}

	private void push16(int v) {
		SP = Bits.clip(16, SP - 2);
		write16(SP, v);
	}

	private int pop16() {
		int oldSp = SP;
		this.SP = Bits.clip(16, SP + 2);

		return (read16(oldSp));
	}

	private int reg16(Reg16 r) {

		switch (r) {
		case HL:
			return (Bits.make16(registerFile.get(Reg.H), registerFile.get(Reg.L)));

		case DE:
			return (Bits.make16(registerFile.get(Reg.D), registerFile.get(Reg.E)));
		case BC:
			return Bits.make16(registerFile.get(Reg.B), registerFile.get(Reg.C));
		case AF:
			return Bits.make16(registerFile.get(Reg.A), registerFile.get(Reg.F));

		default:
			throw new IllegalArgumentException();
		}

	}

	private void setReg16(Reg16 r, int newV) {

		int first8 = Bits.clip(8, newV);
		int second8 = Bits.extract(newV, 8, 8);

		switch (r) {
		case HL: {
			registerFile.set(Reg.H, second8);
			registerFile.set(Reg.L, first8);
		}
			break;
		case DE: {
			registerFile.set(Reg.D, second8);
			registerFile.set(Reg.E, first8);
		}
			break;
		case BC: {
			registerFile.set(Reg.B, second8);
			registerFile.set(Reg.C, first8);
		}
			break;
		case AF: {

			int i = 0;
			while (i < 4) {
				first8 = Bits.set(first8, i, false);
				i++;
			}

			registerFile.set(Reg.A, second8);
			registerFile.set(Reg.F, first8);
		}
			break;

		default:
			throw new IllegalArgumentException();
		}
	}

	private void setReg16SP(Reg16 r, int newV) {
		int first8 = Bits.clip(8, newV);
		int second8 = Bits.extract(newV, 8, 8);

		switch (r) {
		case HL: {
			registerFile.set(Reg.H, second8);
			registerFile.set(Reg.L, first8);
		}
			break;
		case DE: {
			registerFile.set(Reg.D, second8);
			registerFile.set(Reg.E, first8);
		}
			break;
		case BC: {
			registerFile.set(Reg.B, second8);
			registerFile.set(Reg.C, first8);
		}
			break;
		case AF: {
			SP = newV;
		}
			break;

		default:
			throw new IllegalArgumentException();
		}

	}

	private Reg extractReg(Opcode opcode, int startBit) throws IllegalArgumentException {
		if (startBit < 0 || startBit > 7) {
			throw new IllegalArgumentException();
		}
		int index = Bits.extract(opcode.encoding, startBit, 3);
		Reg registers[] = { Reg.B, Reg.C, Reg.D, Reg.E, Reg.H, Reg.L, null, Reg.A };

		return registers[index];
	}

	private Reg16 extractReg16(Opcode opcode) {

		int index = Bits.extract(opcode.encoding, 4, 2);
		Reg16 registers[] = { Reg16.BC, Reg16.DE, Reg16.HL, Reg16.AF };

		return registers[index];
	}

	private int extractHlIncrement(Opcode opcode) {
		return Bits.test(opcode.encoding, 4) ? -1 : 1;
	}

	private void setRegFromAlu(Reg r, int vf) {

		int value = Alu.unpackValue(vf);
		registerFile.set(r, value);

	}

	private void setFlags(int valueFlags) {
		int flags = Alu.unpackFlags(valueFlags);
		registerFile.set(Reg.F, flags);
	}

	private void combineAluFlags(int vf, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {

		int v1 = createVector(FlagSrc.V1, z, n, h, c);
		int alu = createVector(FlagSrc.ALU, z, n, h, c);
		int cpu = createVector(FlagSrc.CPU, z, n, h, c);

		setFlags((registerFile.get(Reg.F) & cpu) | (vf & alu) | v1);

	}

	private int createVector(FlagSrc f, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {

		return Alu.maskZNHC(f == z, f == n, f == h, f == c);
	}

	private boolean getCarry(Opcode o) {
		return Bits.test(registerFile.get(Reg.F), 4) && Bits.test(o.encoding, 3);
	}

	private int bitIndexToBeTestedModified(Opcode opcode) {

		int encoding = opcode.encoding;
		int bitIndex = Bits.extract(encoding, 3, 3);
		return bitIndex;
	}

	private Direction directionRot(Opcode opcode) {

		int encoding = opcode.encoding;
		return (Bits.test(encoding, 3)) ? Direction.RIGHT : Direction.LEFT;

	}

	private boolean determineCondition(Opcode opcode) {
		switch (Bits.extract(opcode.encoding, 3, 2)) {
		case 0b00: {
			return (Bits.test(registerFile.get(Reg.F), Alu.Flag.Z)) ? false : true;
		}
		case 0b01: {
			return (Bits.test(registerFile.get(Reg.F), Alu.Flag.Z)) ? true : false;
		}
		case 0b10: {
			return (Bits.test(registerFile.get(Reg.F), Alu.Flag.C)) ? false : true;
		}
		case 0b11: {
			return (Bits.test(registerFile.get(Reg.F), Alu.Flag.C)) ? true : false;
		}

		}
		return false;
	}

	private static Opcode[] buildOpcodeTable(Kind kind) {
		Opcode[] opcodeTable = new Opcode[256];
		for (Opcode o : Opcode.values()) {
			if (o.kind == kind) {
				opcodeTable[o.encoding] = o;

			}
		}

		return opcodeTable;
	}
}
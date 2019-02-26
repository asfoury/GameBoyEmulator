package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;

public final class LcdController implements Component, Clocked {

	public final static int LCD_WIDTH = 160;
	public final static int LCD_HEIGHT = 144;

	/**
	 * constructs a LcdController
	 * 
	 * @param cpu
	 *            of the gameboy
	 * @throws NullPointerException
	 *             if the cpu is null;
	 */
	public LcdController(Cpu cpu) throws NullPointerException {
		Objects.requireNonNull(cpu);
		registerFile = new RegisterFile<>(register.values());
		this.cpu = cpu;
		currentCycle = 0;
		nextNonIdleCycle = 0;
		VIDEO_RAM = new Ram(AddressMap.VIDEO_RAM_SIZE);
		OAM_RAM = new Ram(AddressMap.OAM_RAM_SIZE);
		startCopying = false;
		zeroBitVector = new BitVector(LCD_WIDTH);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Clocked#cycle(long)
	 */
	@Override
	public void cycle(long cycle) {
		if (counter == AddressMap.OAM_RAM_SIZE) {
			startCopying = false;
			counter = 0;
		}
		if (startCopying) {
			int address = Bits.make16(registerFile.get(register.DMA), counter);
			OAM_RAM.write(counter, bus.read(address));
			++counter;
		}

		if (nextNonIdleCycle == Long.MAX_VALUE && registerFile.testBit(register.LCDC, LCDC.LCD_STATUS)) {
			lcdOnCycle = cycle;
			nextNonIdleCycle = cycle;

		}

		WX_7 = registerFile.get(register.WX) - 7;
		currentCycle = cycle;
		if (cycle == nextNonIdleCycle)
			reallyCycle();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#read(int)
	 */
	@Override
	public int read(int address) {

		Preconditions.checkBits16(address);

		if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END)
			return registerFile.get(register.values()[address - AddressMap.REGS_LCDC_START]);

		if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END)
			return OAM_RAM.read(address - AddressMap.OAM_START);

		if (address >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END)
			return VIDEO_RAM.read(address - AddressMap.VIDEO_RAM_START);

		return Component.NO_DATA;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#write(int, int)
	 */
	@Override
	public void write(int address, int data) {
		Preconditions.checkBits8(data);
		Preconditions.checkBits16(address);

		if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {

			register reg = register.values()[address - AddressMap.REGS_LCDC_START];
			switch (reg) {
			case LCDC: {
				if (!Bits.test(data, LCDC.LCD_STATUS)) {
					setMode(mode.ZERO);
					set_LY_LYC(register.LY, 0);
					nextNonIdleCycle = Long.MAX_VALUE;
					if (registerFile.testBit(register.STAT, STAT.INT_MODE0))
						cpu.requestInterrupt(Interrupt.LCD_STAT);
				}
				registerFile.set(reg, data);
			}
				break;
			case STAT: {
				int lsb3Stat = registerFile.get(register.STAT) & 0b00000111;
				int msb5Data = data & 0b11111000;
				registerFile.set(register.STAT, msb5Data | lsb3Stat);
			}
				break;
			case LY:
				break;
			case LYC: {
				set_LY_LYC(reg, data);
			}
				break;
			case DMA: {
				counter = 0;
				registerFile.set(reg, data);
				startCopying = true;
			}
				break;
			default: {
				registerFile.set(reg, data);
			}
				break;
			}
		}
		if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END)
			OAM_RAM.write(address - AddressMap.OAM_START, data);

		if (address >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END)
			VIDEO_RAM.write(address - AddressMap.VIDEO_RAM_START, data);

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
	 * gets the image currently displayed or a blank image.
	 */
	public LcdImage currentImage() {
		List<LcdImageLine> lines = new ArrayList<>();
		BitVector zeroLine = new BitVector(LCD_WIDTH);
		for (int i = 0; i < LCD_HEIGHT; ++i)
			lines.add(new LcdImageLine(zeroLine, zeroLine, zeroLine));
		return (currentImage == null) ? new LcdImage(LCD_WIDTH, LCD_HEIGHT, lines) : currentImage;
	}

	/////////////////// End of Public Interface//////////////////

	private enum LCDC implements Bit {
		BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS
	}

	private enum register implements Register {
		LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX
	}

	private enum SPRITE implements Bit {
		notUsed0, notUsed1, notUsed2, notUsed3, PALETTE, FLIP_H, FLIP_V, BEHIND_BG
	}

	private enum STAT implements Bit {
		MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC
	}

	private enum mode {
		ZERO, ONE, TWO, THREE
	}

	private enum plan {
		FRONT, BACK
	}

	private enum image {
		BACKGROUND, WINDOW
	}

	private final int BACKGROUND_SIZE = 256;
	private RegisterFile<register> registerFile;
	private int winY, counter, WX_7;
	private Ram VIDEO_RAM, OAM_RAM;
	private Cpu cpu;
	private boolean startCopying;
	private Bus bus;
	private BitVector zeroBitVector;
	private LcdImage.Builder nextImageBuilder;
	private LcdImage currentImage;
	private long nextNonIdleCycle, lcdOnCycle, currentCycle;

	private final int TILE_SIZE = 8;

	int octetsPerTile = TILE_SIZE * 2;

	private void reallyCycle() {

		final int cyclesPerLine = 114;
		final int cyclesMode2 = 20;
		final int cyclesMode3 = 43;
		final int cyclesMode0 = 51;
		final int linesMode1 = 10;
		int LY = registerFile.get(register.LY);

		if (currentCycle == lcdOnCycle) {
			setMode(mode.TWO);
			winY = 0;
			nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
		}
		switch (getMode()) {
		case ZERO: {
			if (LY < LCD_HEIGHT - 1) {
				setMode(mode.TWO);
			} else {
				setMode(mode.ONE);
				currentImage = nextImageBuilder.build();
				cpu.requestInterrupt(Interrupt.VBLANK);
			}
			nextNonIdleCycle += cyclesMode0;
			if (registerFile.testBit(register.STAT, STAT.INT_MODE0))
				cpu.requestInterrupt(Interrupt.LCD_STAT);
		}
			break;
		case ONE: {

			if (LY < (LCD_HEIGHT - 1) + linesMode1) {
				set_LY_LYC(register.LY, LY + 1);

			} else {
				setMode(mode.TWO);
				set_LY_LYC(register.LY, 0);
				winY = 0;
			}
			nextNonIdleCycle += cyclesPerLine;
			if (registerFile.testBit(register.STAT, STAT.INT_MODE1))
				cpu.requestInterrupt(Interrupt.LCD_STAT);
		}
			break;
		case TWO: {
			set_LY_LYC(register.LY, currentCycle == lcdOnCycle ? 0 : LY + 1);
			nextNonIdleCycle += cyclesMode2;
			if (registerFile.testBit(register.STAT, STAT.INT_MODE2))
				cpu.requestInterrupt(Interrupt.LCD_STAT);
			setMode(mode.THREE);

		}
			break;
		case THREE: {
			this.nextNonIdleCycle += cyclesMode3;
			nextImageBuilder.setLine(registerFile.get(register.LY), computeLine(LY));
			setMode(mode.ZERO);

		}
			break;
		}
	}

	private void set_LY_LYC(register reg, int value) {
		Preconditions.checkArgument(reg == register.LY || reg == register.LYC);
		registerFile.set(reg, value);
		if (registerFile.get(register.LY) == registerFile.get(register.LYC)) {
			registerFile.setBit(register.STAT, STAT.LYC_EQ_LY, true);
			if (registerFile.testBit(register.STAT, STAT.INT_LYC))
				cpu.requestInterrupt(Interrupt.LCD_STAT);

		} else {
			registerFile.setBit(register.STAT, STAT.LYC_EQ_LY, false);
		}
	}

	private void setMode(mode m) {

		registerFile.set(register.STAT, registerFile.get(register.STAT) & 0b11111100
				| (Bits.test(m.ordinal(), 1) ? 1 << 1 : 0) | (Bits.test(m.ordinal(), 0) ? 1 : 0));

	}

	private mode getMode() {

		Boolean mode0 = registerFile.testBit(register.STAT, STAT.MODE0);
		Boolean mode1 = registerFile.testBit(register.STAT, STAT.MODE1);

		return mode.values()[(mode1 ? (1 << 1) : 0) + (mode0 ? 1 : 0)];
	}

	private LcdImageLine computeLine(int lineIndex) { // finish

		Boolean windowON = registerFile.testBit(register.LCDC, LCDC.WIN) && WX_7 < LCD_WIDTH && WX_7 >= 0;
		Boolean spriteON = registerFile.testBit(register.LCDC, LCDC.OBJ);
		Boolean backGroundOn = registerFile.testBit(register.LCDC, LCDC.BG);

		LcdImageLine backGroundLine = (backGroundOn) ? backGroundLine(lineIndex)
				: new LcdImageLine(zeroBitVector, zeroBitVector, zeroBitVector);

		LcdImageLine backGroundAndWindowLine = (lineIndex < registerFile.get(register.WY) || !windowON) ? backGroundLine
				: backGroundLine.join(windowLine(), WX_7);

		if (spriteON) {
			BitVector opacity = backGroundAndWindowLine.opacity().or(spritesLine(lineIndex, plan.BACK).opacity().not());

			return spritesLine(lineIndex, plan.BACK).below(backGroundAndWindowLine, opacity)
					.below(spritesLine(lineIndex, plan.FRONT));
		} else
			return backGroundAndWindowLine;

	}

	private LcdImageLine backGroundLine(int lineIndex) { // finish

		return line(lineIndex, image.BACKGROUND).build().extractWrapped(LCD_WIDTH, registerFile.get(register.SCX))
				.mapColors(registerFile.get(register.BGP));

	}

	private LcdImageLine windowLine() { // finish

		return line(winY, image.WINDOW).build().extractWrapped(LCD_WIDTH, 0).shift(WX_7)
				.mapColors(registerFile.get(register.BGP));
	}

	private LcdImageLine.Builder line(int y, image pt) { /////////////////////////////////// still
		LcdImageLine.Builder builder = new LcdImageLine.Builder(BACKGROUND_SIZE);
		int tilesPerLine = 32;
		int yInsideImage = (pt == image.WINDOW) ? winY : (y + registerFile.get(register.SCY)) % BACKGROUND_SIZE;

		for (int x = 0; x < tilesPerLine; x++) {

			int tileIndex = read((yInsideImage / TILE_SIZE) * tilesPerLine + x + AddressMap.BG_DISPLAY_DATA[registerFile
					.testBit(register.LCDC, (pt == image.BACKGROUND) ? LCDC.BG_AREA : LCDC.WIN_AREA) ? 1 : 0]);
			
			int lsbAddressOfLine = tileBeginningAddress(tileIndex) + 2 * (yInsideImage % TILE_SIZE);
			int msbAddressOfLine = lsbAddressOfLine + 1;
			builder.setBytes(x, read(msbAddressOfLine), read(lsbAddressOfLine));
		}
		if (pt == image.WINDOW)
			++winY;
		return builder;
	}

	private int tileBeginningAddress(int tileIndex) {
		int tileBeginningAddress;
		if (registerFile.testBit(register.LCDC, LCDC.TILE_SOURCE))
			tileBeginningAddress = AddressMap.TILE_SOURCE[1] + tileIndex * octetsPerTile;
		else
			tileBeginningAddress = (tileIndex >= 0x80) ? AddressMap.TILE_SOURCE[0] + (tileIndex - 0x80) * octetsPerTile
					: 0x9000 + tileIndex * octetsPerTile;
		return tileBeginningAddress;
	}

	private int[] spritesIntersectingLine(int line) { // finish
		int maxSpritesPerLine = 10;
		int maxSpritesInGameBoy = 40;
		int counter = 0;
		int spriteHeight = (registerFile.testBit(register.LCDC, LCDC.OBJ_SIZE) ? 16 : 8);

		int xCoordinateAndIndex[] = new int[maxSpritesPerLine];

		for (int i = 0; i < maxSpritesInGameBoy; i++) {
			int firstYcoordinateOfSprite = OAM_RAM.read(4 * i) - 16;

			Boolean lineIntersectSprite = line >= firstYcoordinateOfSprite
					&& line < firstYcoordinateOfSprite + spriteHeight;

			if (lineIntersectSprite && counter < maxSpritesPerLine) {
				xCoordinateAndIndex[counter] = Bits.make16(OAM_RAM.read(4 * i + 1), i);
				++counter;
			}
		}

		int index[] = new int[counter];

		Arrays.sort(xCoordinateAndIndex, 0, counter);

		for (int i = 0; i < counter; i++)
			index[i] = Bits.clip(8, xCoordinateAndIndex[i]);
		return index;
	}

	private LcdImageLine oneSpriteLine(int indexOfSprite, int lineIndex) {

		boolean flipH = Bits.test(OAM_RAM.read(4 * indexOfSprite + 3), SPRITE.FLIP_H);
		boolean flipV = Bits.test(OAM_RAM.read(4 * indexOfSprite + 3), SPRITE.FLIP_V);

		int sizeSprite = (registerFile.testBit(register.LCDC, LCDC.OBJ_SIZE) ? 16 : 8);
		int firstYcoordinateOfSprite = OAM_RAM.read(4 * indexOfSprite) - 16;
		int firstXcoordinateofSprite = OAM_RAM.read(4 * indexOfSprite + 1) - 8;
		int palette = registerFile
				.get(Bits.test(OAM_RAM.read(4 * indexOfSprite + 3), SPRITE.PALETTE) ? register.OBP1 : register.OBP0);
		int lineInsideSprite = (flipV) ? (sizeSprite - 1) - (lineIndex - firstYcoordinateOfSprite)
				: lineIndex - firstYcoordinateOfSprite;
		int tileIndex = OAM_RAM.read(4 * indexOfSprite + 2);
		int msb = VIDEO_RAM.read(octetsPerTile * tileIndex + 2 * (lineInsideSprite % octetsPerTile) + 1);
		int lsb = VIDEO_RAM.read(octetsPerTile * tileIndex + 2 * (lineInsideSprite % octetsPerTile));

		LcdImageLine.Builder builder = new LcdImageLine.Builder(LCD_WIDTH);
		builder.setBytes(0, flipH ? Bits.reverse8(msb) : msb, flipH ? Bits.reverse8(lsb) : lsb);

		return builder.build().shift(firstXcoordinateofSprite).mapColors(palette);

	}

	private LcdImageLine spritesLine(int line, plan planType) {
		LcdImageLine spritesLine = new LcdImageLine(zeroBitVector, zeroBitVector, zeroBitVector);

		for (int indexSprite : spritesIntersectingLine(line)) {
			boolean BehindBackGround = Bits.test(OAM_RAM.read(indexSprite * 4 + 3), SPRITE.BEHIND_BG);

			if (planType == plan.BACK && BehindBackGround || planType == plan.FRONT && !BehindBackGround)
				spritesLine = spritesLine.below(oneSpriteLine(indexSprite, line));

		}
		return spritesLine;
	}

}

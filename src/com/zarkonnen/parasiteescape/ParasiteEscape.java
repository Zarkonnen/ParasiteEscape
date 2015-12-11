package com.zarkonnen.parasiteescape;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.JFrame;

/**
 * A Java4k game about escaping a space base through parasitic possession.
 * Copyright David Stark 2015.
 *
 * @author David Stark
 */
public class ParasiteEscape extends JFrame implements Runnable, KeyListener, MouseListener, MouseMotionListener {

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent me) {
	}

	@Override
	public void mouseMoved(MouseEvent me) {
		my = me.getY();
		mx = me.getX();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		click = true;
		btn = e.getButton();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		key[((KeyEvent) e).getKeyCode()] = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		key[((KeyEvent) e).getKeyCode()] = false;
	}

	boolean key[] = new boolean[65535];
	boolean click = false;
	int btn;
	int my, mx;
	BufferStrategy strategy;

	public void init() {
		setIgnoreRepaint(true);
		Canvas canvas = new Canvas();
		add(canvas);
		canvas.setBounds(0, 0, 800, 600);
		canvas.createBufferStrategy(2);
		strategy = canvas.getBufferStrategy();
		canvas.addKeyListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		new Thread(this).start();
	}

	private static final int UNARMED = -1;
	private static final int GROUP_DIV = 4;
	private static final int PEOPLE_GROUP = 1;
	private static final int BULLET_GROUP = 2;
	private static final int PARTICLE_GROUP = 3;
	private static final int DUDE = 4;
	private static final int GRUNT = 5;
	private static final int OFFICER = 6;
	private static final int ARMOR = 7;
	private static final int BULLET = 8;
	private static final int PSI_BULLET = 9;
	private static final int LOOK = 10;
	private static final int BLOOD = 12;
	private static final int ICHOR = 13;
	private static final int NUM_UNITS = 100;
	private static final int T_STRIDE = 3;
	private static final int W = 0;
	private static final int H = 1;
	private static final int LOOK_FREQ = 2;
	private static final int I_STRIDE = 5;
	private static final int TYPE = 0;
	private static final int FLOOR = 1;
	private static final int RELOAD = 2;
	private static final int LOOK_STATUS = 3;
	private static final int AUTH = 4;
	private static final int F_STRIDE = 4;
	private static final int X = 0;
	private static final int Y = 1;
	private static final int DX = 2;
	private static final int DY = 3;

	private static final int SOLIDITY_BOUNDARY = 0xa;
	private static final int PRE_DOOR = 1;
	private static final int LVL_DOOR = 2;
	private static final int SPACE_WINDOW = 3;
	private static final int V_PIPE = 4;
	private static final int RED_CONSOLE = 5;
	private static final int IN_PORTAL_1 = 6;
	private static final int OUT_PORTAL_1 = 7;
	private static final int IN_PORTAL_2 = 8;
	private static final int OUT_PORTAL_2 = 9;
	private static final int WALL = 0xa;
	private static final int BOX = 0xb;
	private static final int RED_DOOR = 0xc;
	private static final int TANK = 0xf;

	private static final double GRAVITY = 0.025f;
	private static final int U_PER_L = 6;
	private static final int NUM_LEVELS = 6;

	private int[] types = {
		0, 0, 0,// 0
		0, 0, 0,// 1
		0, 0, 0,// 2
		0, 0, 0,// 3
		12, 22, UNARMED,// dude 4
		12, 22, 180,// grunt 5
		12, 22, 55,// officer 6
		12, 22, 80,// battle armor
		1, 1, 0,// bullet 8
		1, 1, 0,// psiblast 9
		1, 1, 0,// look 10
		0, 0, 0,// 11
		2, 2, 0, // blood 12
		2, 2, 0 // ichor 13
	};

	private int controlledUnit = 0;
	private int[] unit_i = new int[I_STRIDE * NUM_UNITS];
	private double[] unit_f = new double[F_STRIDE * NUM_UNITS];
	private int[][] map = new int[15][20];
	private int[][] anim;
	private boolean victory;
	private int difficulty = 1;
	Random r = new Random();

	private void play(int start, int end, int samps, float rate, int vol) {
		byte[] sample = new byte[samps];
		try {
			int i = 0;
			lp:
			while (true) {
				for (int x = start; x < end; x++) {
					i++;
					if (i == samps) {
						break lp;
					}
					sample[i] = (byte) (levelMaps.charAt(x) * vol / 4);
					if (i > samps - 200) {
						sample[i] = (byte) (sample[i] * (samps - i) / 200);
					}
				}
			}
			// Initialise Sound System
			AudioFormat audioFormat = new AudioFormat(rate, 8, 1, true, true);
			DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
			Clip snd = (Clip) AudioSystem.getLine(info);
			snd.open(audioFormat, sample, 0, sample.length);
			snd.start();
		} catch (Exception e) {
		}
	}

	private int findEmpty() {
		for (int i = 0; i < NUM_UNITS; i++) {
			if (unit_i[i * I_STRIDE + TYPE] == 0) {
				return i;
			}
		}
		return 99;
	}

	private void splash(double x, double y, int type) {
		for (int p = 0; p < 20; p++) {
			int i = findEmpty();
			unit_i[i * I_STRIDE + TYPE] = type;
			unit_i[i * I_STRIDE + FLOOR] = 1;
			unit_f[i * F_STRIDE + X] = x;
			unit_f[i * F_STRIDE + Y] = y;
			unit_f[i * F_STRIDE + DX] = r.nextDouble() * 2 - 1;
			unit_f[i * F_STRIDE + DY] = r.nextDouble() * 2 - 1;
		}
	}

	private void shoot(int u, double x, double y, int type, int reload, boolean aim) {
		int i = findEmpty();
		double dx = x - unit_f[u * F_STRIDE + X];
		double dy = y - unit_f[u * F_STRIDE + Y];

		if (aim) {
			dy -= GRAVITY * dx * dx * difficulty / 400;
		}
		double dist = Math.sqrt(dx * dx + dy * dy + 1);
		unit_i[i * I_STRIDE + TYPE] = type;
		unit_i[i * I_STRIDE + FLOOR] = 1;
		unit_f[i * F_STRIDE + X] = unit_f[u * F_STRIDE + X] + dx * 16 / dist;
		unit_f[i * F_STRIDE + Y] = unit_f[u * F_STRIDE + Y] + dy * 16 / dist;
		unit_f[i * F_STRIDE + DX] = dx * 9.0f / dist;
		unit_f[i * F_STRIDE + DY] = dy * 9.0f / dist + unit_f[u * F_STRIDE + DY];
		unit_i[u * I_STRIDE + RELOAD] = reload;
		unit_i[i * I_STRIDE + LOOK_STATUS] = u;
		if (type == BULLET) {
			play(100, 600, 3000, 32000f, 3);
		}
		if (type == PSI_BULLET) {
			play(99, 980, 300, 800f, 15);
		}
	}

	private boolean animate(int type) {
		for (int y = 0; y < 15; y++) {
			for (int x = 0; x < 20; x++) {
				if (map[y][x] == type && anim[y][x] == 0) {
					anim[y][x] = 1;
					return true;
				}
			}
		}
		return false;
	}

	String levelMaps
			= "aaaaaaaaaaaaaaaaaaaa"
			+ "a000000040040000004a"
			+ "a000000040040000004a"
			+ "a000000040040000204a"
			+ "a0aaa0004004000aaaaa"
			+ "a000000040040000004a"
			+ "a000000040040000004a"
			+ "aaaa000aaaaaa000aaaa"
			+ "aaaa000040040000aaaa"
			+ "aaaa000040040000aaaa"
			+ "aaaa000040f40000004a"
			+ "aaaaaaaaaaaaaaaaaa4a"
			+ "a000000000000000004a"
			+ "a0f0f0f0f0f0f0f0f04a"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "a000000000000000040a"
			+ "a050000333333000040a"
			+ "aaaaaa0333333000040a"
			+ "a000000333333000040a"
			+ "a0000003333330aaaaaa"
			+ "a000000333333000040a"
			+ "a000000333333000040a"
			+ "aaaaaa00000000aaaaaa"
			+ "a000000000000000040a"
			+ "a0b0000000000000040a"
			+ "aaaaaa00000000aaaaaa"
			+ "a0000a00000000a0000a"
			+ "a1000000000000c0002a"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "a000000000000000000a"
			+ "a10000000000f00000ba"
			+ "aaa00000000aaaaaaaaa"
			+ "a000600000000009000a"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "a000004000400000400a"
			+ "a500004000470000480a"
			+ "aaaaa04000aaaaaaaaaa"
			+ "a000004000400000400a"
			+ "a000004000403330400a"
			+ "a0b0b04000403330400a"
			+ "aaaaa04000403330400a"
			+ "a200c040004000004b0a"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "a04440000a000004440a"
			+ "a04440000a000004440a"
			+ "a04440700a060004441a"
			+ "a0aaaaaaaaaaaaaaaaaa"
			+ "a044400000000004440a"
			+ "a044400000000904440a"
			+ "a044400330000004440a"
			+ "a044400330000004440a"
			+ "a04440aaaa000004440a"
			+ "a044400000000004440a"
			+ "a044400000000004440a"
			+ "a044400000000004440a"
			+ "a8444000b0020004440a"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "a000000444000000400a"
			+ "a000000444000000400a"
			+ "a000000aaa000000400a"
			+ "a033330aaa0000b0400a"
			+ "a033330aaa00aaaaaaaa"
			+ "a033330aaa000033400a"
			+ "a033330aaa000533400a"
			+ "a000000aaa00aaaaa00a"
			+ "a000000aaa000033400a"
			+ "a000000aaa000033400a"
			+ "a00aaaaaaa00aaaaa00a"
			+ "a00a000aaa000000400a"
			+ "a000010a2c000b00400a"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "aaaaaaaaaaaaaaaaaaaa"
			+ "a000000440000000000a"
			+ "a03330044f006003330a"
			+ "a033300aaaaaa003330a"
			+ "a033300004400003330a"
			+ "a100000004400000000a"
			+ "aaaaaa00044000aaaaaa"
			+ "a000000004400000000a"
			+ "a0000000aaaa0000000a"
			+ "abb00000a5000000000a"
			+ "abb00bf0aaaa0000000a"
			+ "aaaaaaaaaaaa0000700a"
			+ "a3333300a004000aaaaa"
			+ "a3323300c0040004444a"
			+ "aaaaaaaaaaaaaaaaaaaa";

	@Override
	public void run() {
		int level = 0;
		String levelUnits
				= "43704100" + "46704100" + "56802500" + "46801300" + "00000000" + "00000000"
				+ "50905300" + "50602950" + "57201750" + "60900901" + "52000900" + "00000000"
				+ "50500900" + "53001700" + "60502901" + "50905300" + "53905300" + "00000000"
				+ "46001200" + "61001200" + "61601200" + "62205300" + "65605300" + "00000000"
				+ "41905300" + "52504150" + "51504150" + "66202800" + "66204001" + "47005300"
				+ "40502100" + "56202100" + "77002100" + "44203701" + "65005300" + "71005300";
		game:
		while (true) {
			boolean paused = true;
			controlledUnit = 0;
			unit_i = new int[I_STRIDE * NUM_UNITS];
			unit_f = new double[F_STRIDE * NUM_UNITS];
			anim = new int[15][20];
			for (int n = 0; n < U_PER_L; n++) {
				unit_i[n * I_STRIDE + TYPE] = Integer.parseInt(levelUnits.substring(U_PER_L * 8 * level + n * 8, U_PER_L * 8 * level + n * 8 + 1));
				unit_f[n * F_STRIDE + X] = Double.parseDouble(levelUnits.substring(U_PER_L * 8 * level + n * 8 + 1, U_PER_L * 8 * level + n * 8 + 4));
				unit_f[n * F_STRIDE + Y] = Double.parseDouble(levelUnits.substring(U_PER_L * 8 * level + n * 8 + 4, U_PER_L * 8 * level + n * 8 + 7));
				unit_i[n * I_STRIDE + AUTH] = Integer.parseInt(levelUnits.substring(U_PER_L * 8 * level + n * 8 + 7, U_PER_L * 8 * level + n * 8 + 8));
			}
			for (int y = 0; y < 15; y++) {
				for (int x = 0; x < 20; x++) {
					map[y][x] = Integer.parseInt(levelMaps.substring(15 * 20 * level + y * 20 + x, 15 * 20 * level + y * 20 + x + 1), 36);
				}
			}
			int tick = 0;
			while (true) {
				tick++;
				// Music
				if (tick % 32 == 16) {
					play(0, 40, 1000 * (tick % (2 + level) + 1), 3000f * (tick % (2 + level) + 1), 1);
				}
				if (tick % 32 == 0) {
					play(0, 40, 2000 * (tick % (17 % (level + 2) + 2) + 2), 6000f * (tick % (17 % (level + 2) + 2) + 2), 1);
				}

				if (paused && click) {
					paused = false;
					click = false;
					victory = false;
				}
				if (unit_i[controlledUnit * I_STRIDE + TYPE] == 0) {
					continue game;
				}

				for (int ph = 0; ph < 3 && !paused; ph++) {
					// Controls
					if (unit_i[controlledUnit * I_STRIDE + FLOOR] < 30) {
						unit_f[controlledUnit * F_STRIDE + DX] = 0;
						if (key[KeyEvent.VK_A] || key[KeyEvent.VK_LEFT]) {
							unit_f[controlledUnit * F_STRIDE + DX] = -.625f;
						}
						if (key[KeyEvent.VK_D] || key[KeyEvent.VK_RIGHT]) {
							unit_f[controlledUnit * F_STRIDE + DX] = .625f;
						}
						if (key[KeyEvent.VK_W] || key[KeyEvent.VK_UP]) {
							unit_f[controlledUnit * F_STRIDE + DY] = -1.1f;
						}
					}
					if ((types[unit_i[controlledUnit * I_STRIDE + TYPE] * T_STRIDE + LOOK_FREQ] != UNARMED || btn == 3 || key[KeyEvent.VK_E]) && (click || key[KeyEvent.VK_E]) && unit_i[controlledUnit * I_STRIDE + RELOAD] <= 0) {
						shoot(controlledUnit, mx, my, (btn == 3 || key[KeyEvent.VK_E]) ? PSI_BULLET : BULLET, 75, false);
					}
					click = false;

					// Physics
					us:
					for (int u = 0; u < NUM_UNITS; u++) {
						if (unit_i[u * I_STRIDE + TYPE] != 0) {
							unit_i[u * I_STRIDE + RELOAD]--;

							// Y-move & gravity
							unit_f[u * F_STRIDE + Y] += unit_f[u * F_STRIDE + DY];
							unit_f[u * F_STRIDE + DY] += GRAVITY;
							int left = (int) Math.floor(unit_f[u * F_STRIDE + X] / 40);
							int right = (int) Math.floor((unit_f[u * F_STRIDE + X] + types[unit_i[u * I_STRIDE + TYPE] * T_STRIDE + W]) / 40);
							int top = (int) Math.floor(unit_f[u * F_STRIDE + Y] / 40);
							int bottom = (int) Math.floor((unit_f[u * F_STRIDE + Y] + types[unit_i[u * I_STRIDE + TYPE] * T_STRIDE + H]) / 40);
							unit_i[u * I_STRIDE + FLOOR]++;
							// Floor
							if (unit_f[u * F_STRIDE + DY] > 0 && (map[bottom][left] >= SOLIDITY_BOUNDARY || map[bottom][right] >= SOLIDITY_BOUNDARY)) {
								if (unit_f[u * F_STRIDE + DY] > 3) {
									if (unit_i[u * I_STRIDE + TYPE] / GROUP_DIV == PEOPLE_GROUP) {
										play(953, 1382, 1000, 2000f, 12);
										splash(unit_f[u * F_STRIDE + X] + 6, unit_f[u * F_STRIDE + Y] + 11, BLOOD);
									}
									unit_i[u * I_STRIDE + TYPE] = 0;
									continue us;
								}
								unit_f[u * F_STRIDE + DY] = 0;
								unit_f[u * F_STRIDE + Y] = bottom * 40 - types[unit_i[u * I_STRIDE + TYPE] * T_STRIDE + H] - 1;
								unit_i[u * I_STRIDE + FLOOR] = 0;
							}
							// Ceiling
							if (unit_f[u * F_STRIDE + DY] < 0 && (map[top][left] >= SOLIDITY_BOUNDARY || map[top][right] >= SOLIDITY_BOUNDARY)) {
								unit_f[u * F_STRIDE + DY] = 0;
								unit_f[u * F_STRIDE + Y] = top * 40 + 41;
							}

							// X-move
							unit_f[u * F_STRIDE + X] += unit_f[u * F_STRIDE + DX];
							left = (int) Math.floor(unit_f[u * F_STRIDE + X] / 40);
							right = (int) Math.floor((unit_f[u * F_STRIDE + X] + types[unit_i[u * I_STRIDE + TYPE] * T_STRIDE + W]) / 40);
							top = (int) Math.floor(unit_f[u * F_STRIDE + Y] / 40);
							bottom = (int) Math.floor((unit_f[u * F_STRIDE + Y] + types[unit_i[u * I_STRIDE + TYPE] * T_STRIDE + H]) / 40);

							// Brrains
							if (u != controlledUnit && unit_i[u * I_STRIDE + TYPE] / GROUP_DIV == PEOPLE_GROUP && unit_i[u * I_STRIDE + FLOOR] == 0) {
								unit_i[u * I_STRIDE + LOOK_STATUS]--;
								if (map[top + 1][right] < SOLIDITY_BOUNDARY || map[top][right + 1] >= SOLIDITY_BOUNDARY) {
									unit_f[u * F_STRIDE + DX] = -0.5;
								} else if (map[top + 1][left] < SOLIDITY_BOUNDARY || map[top][left - 1] >= SOLIDITY_BOUNDARY || Math.abs(unit_f[u * F_STRIDE + DX]) < 0.5) {
									unit_f[u * F_STRIDE + DX] = 0.5;
								}

								// Shootings!
								if (types[unit_i[u * I_STRIDE + TYPE] * T_STRIDE + LOOK_FREQ] != UNARMED && (unit_i[u * I_STRIDE + RELOAD] <= 0 || (unit_i[u * I_STRIDE + LOOK_STATUS] <= 0 && tick % (types[unit_i[u * I_STRIDE + TYPE] * T_STRIDE + LOOK_FREQ] / difficulty / difficulty) == 0))) {
									r.setSeed(tick);
									shoot(u, unit_f[controlledUnit * F_STRIDE + X], unit_f[controlledUnit * F_STRIDE + Y], unit_i[u * I_STRIDE + LOOK_STATUS] > 0 ? BULLET : LOOK, unit_i[u * I_STRIDE + LOOK_STATUS] > 0 ? 75 : types[unit_i[u * I_STRIDE + TYPE] * T_STRIDE + LOOK_FREQ], true);
								}
							}

							// Walls
							if (map[top][left] >= SOLIDITY_BOUNDARY || map[bottom][left] >= SOLIDITY_BOUNDARY) {
								unit_f[u * F_STRIDE + DX] = 0;
								unit_f[u * F_STRIDE + X] = left * 40 + 41;
								unit_i[u * I_STRIDE + FLOOR] = 0;
							} else if (map[top][right] >= SOLIDITY_BOUNDARY || map[bottom][right] >= SOLIDITY_BOUNDARY) {
								unit_f[u * F_STRIDE + DX] = 0;
								unit_f[u * F_STRIDE + X] = right * 40 - types[unit_i[u * I_STRIDE + TYPE] * T_STRIDE + W] - 1;
								unit_i[u * I_STRIDE + FLOOR] = 0;
							}

							// Escaep!
							if (u == controlledUnit && map[bottom][right] == LVL_DOOR) {
								level++;
								if (level >= NUM_LEVELS) {
									level = 0;
									difficulty = 2;
									victory = true;
								}
								continue game;
							}

							// Teleporting!
							if (map[bottom][right] == IN_PORTAL_1 || map[bottom][right] == IN_PORTAL_2) {
								for (int y = 0; y < 15; y++) {
									for (int x = 0; x < 20; x++) {
										if (map[y][x] == (map[bottom][right] == IN_PORTAL_1 ? OUT_PORTAL_1 : OUT_PORTAL_2)) {
											unit_f[u * F_STRIDE + X] = x * 40 + unit_f[u * F_STRIDE + X] - right * 40;
											unit_f[u * F_STRIDE + Y] = y * 40 + unit_f[u * F_STRIDE + Y] - bottom * 40;
										}
									}
								}
							}

							// Unlocking
							if (u == controlledUnit && unit_i[u * I_STRIDE + AUTH] > 0) {
								if (map[bottom][right] == RED_CONSOLE && animate(RED_DOOR)) {
									play(0, 300, 6000, 4000f, 12);
								}
							}
						}
					}

					// Terrible, terrible damage.
					for (int u = 0; u < NUM_UNITS; u++) {
						if (unit_i[u * I_STRIDE + TYPE] / GROUP_DIV > PEOPLE_GROUP) {
							if (unit_i[u * I_STRIDE + FLOOR] == 0) {
								unit_i[u * I_STRIDE + TYPE] = 0;
							}
						}
						if (unit_i[u * I_STRIDE + TYPE] / GROUP_DIV == BULLET_GROUP) {
							for (int u2 = 0; u2 < NUM_UNITS; u2++) {
								if (unit_i[u2 * I_STRIDE + TYPE] / GROUP_DIV == PEOPLE_GROUP) {
									if (unit_f[u * F_STRIDE + X] + 5 > unit_f[u2 * F_STRIDE + X]
											&& unit_f[u * F_STRIDE + X] < unit_f[u2 * F_STRIDE + X] + 12
											&& unit_f[u * F_STRIDE + Y] + 5 > unit_f[u2 * F_STRIDE + Y]
											&& unit_f[u * F_STRIDE + Y] < unit_f[u2 * F_STRIDE + Y] + 22) {
										switch (unit_i[u * I_STRIDE + TYPE]) {
											case BULLET:
												splash(unit_f[u2 * F_STRIDE + X] + 6, unit_f[u2 * F_STRIDE + Y] + 11, BLOOD);
												unit_i[u2 * I_STRIDE + TYPE] = 0;
												play(953, 1382, 1000, 2000f, 12);
												break;
											case PSI_BULLET:
												play(99, 980, 300, 800f, 12);
												splash(unit_f[u2 * F_STRIDE + X] + 6, unit_f[u2 * F_STRIDE + Y] + 11, ICHOR);
												if (unit_i[u2 * I_STRIDE + TYPE] != ARMOR) {
													controlledUnit = u2;
													for (int z = 0; z < NUM_UNITS; z++) {
														unit_i[z * I_STRIDE + LOOK_STATUS] = 0;
													}
												}
												break;
											case LOOK:
												if (controlledUnit == u2) {
													unit_i[unit_i[u * I_STRIDE + LOOK_STATUS] * I_STRIDE + LOOK_STATUS] = 100;
												}
												break;
										}
										unit_i[u * I_STRIDE + TYPE] = 0;
									}
								}
							}
						}
					}
				}

				// Gfx
				Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
				g.setColor(Color.DARK_GRAY);
				g.fillRect(0, 0, 800, 600);
				for (int y = 0; y < 15; y++) {
					for (int x = 0; x < 20; x++) {
						int tx = x * 40;
						int ty = y * 40;
						switch (map[y][x]) {
							case WALL:
								g.setColor(Color.GRAY);
								g.fillRect(tx + 3, ty, 34, 40);
								// Left
								if (x > 0 && map[y][x - 1] != WALL) {
									g.setColor(new Color(30, 30, 30));
									g.fillRect(tx + 3, ty, 3, 40);
									for (int l = 0; l < 40; l += 8) {
										g.fillOval(tx - 2, ty + l, 6, 6);
									}
								} else {
									g.setColor(Color.GRAY);
									g.fillRect(tx, ty, 5, 40);
								}
								// Right
								if (x < 19 && map[y][x + 1] != WALL) {
									g.setColor(new Color(30, 30, 30));
									g.fillRect(tx + 35, ty, 3, 40);
									for (int l = 0; l < 40; l += 8) {
										g.fillOval(tx + 35, ty + l, 6, 6);
									}
								} else {
									g.setColor(Color.GRAY);
									g.fillRect(tx + 35, ty, 5, 40);
								}
								break;
							case PRE_DOOR:
								g.setColor(Color.WHITE);
								g.fillRoundRect(tx + 1, ty + 1, 38, 38, 10, 10);
								g.setColor(new Color(50, 50, 50));
								g.fillRoundRect(tx + 4, ty + 4, 32, 32, 10, 10);
								break;
							case LVL_DOOR:
								g.setColor(Color.WHITE);
								g.fillRoundRect(tx + 1, ty + 1, 38, 38, 10, 10);
								g.setColor(Color.LIGHT_GRAY);
								g.fillRoundRect(tx + 4, ty + 4, 32, 32, 10, 10);
								g.setColor(Color.DARK_GRAY);
								g.drawLine(tx + 20, ty + 4, tx + 20, ty + 14);
								g.drawLine(tx + 20, ty + 14, tx + 24, ty + 17);
								g.drawLine(tx + 24, ty + 17, tx + 16, ty + 23);
								g.drawLine(tx + 16, ty + 23, tx + 20, ty + 26);
								g.drawLine(tx + 20, ty + 26, tx + 20, ty + 35);
								g.drawString("" + (level + 1), tx + 7, ty + 18);
								break;
							case SPACE_WINDOW:
								g.setColor(new Color(35, 35, 35));
								g.fillRect(tx, ty, 40, 40);
								g.setColor(Color.BLACK);
								g.fillRoundRect(tx + 1, ty + 1, 38, 38, 10, 10);
								r.setSeed(x + y * y);
								for (int s = 0; s < 40; s++) {
									g.setColor(r.nextBoolean() ? Color.LIGHT_GRAY : Color.GRAY);
									g.fillRect(tx + 2 + r.nextInt(36), ty + 2 + r.nextInt(36), 1, 1);
								}
								break;
							case V_PIPE:
								g.setColor(Color.BLACK);
								g.fillRect(tx + 10, ty, 20, 40);
								g.setColor(Color.DARK_GRAY);
								for (int p = 0; p < 40; p += 4) {
									g.fillRect(tx + 11, ty + p, 18, 1);
								}
								g.setColor(new Color(255, 255, 255, 30));
								g.fillRect(tx + 14, ty, 5, 40);
								g.fillRect(tx + 15, ty, 2, 40);
								break;
							case BOX:
								g.setColor(new Color(62, 50, 28));
								g.fillRoundRect(tx, ty, 40, 40, 10, 10);
								g.setColor(new Color(99, 83, 56));
								g.fillRoundRect(tx + 2, ty + 2, 36, 36, 10, 10);
								g.setColor(new Color(62, 50, 28));
								for (int b = 1; b < 6; b++) {
									g.fillRect(tx + 2, ty + 2 + b * 6, 36, 1);
								}
								break;
							case RED_CONSOLE:
								g.setColor(Color.WHITE);
								g.fillRect(tx + 2, ty + 4, 26, 23);
								g.fillRect(tx + 2, ty + 30, 20, 5);
								g.setColor(Color.BLACK);
								g.fillRect(tx + 4, ty + 8, 22, 18);
								g.setColor(Color.RED);
								g.fillRect(tx + 6, ty + 10, 9, 1);
								g.fillRect(tx + 6, ty + 12, 5, 1);
								g.fillRect(tx + 6, ty + 14, ((tick / 50)) % 2, 1);
								if (((tick / 50)) % 2 == 0) {
									g.drawOval(tx + 17, ty + 11, 6, 4);
									g.fillRect(tx + 19, ty + 15, 2, 8);
									g.fillRect(tx + 19, ty + 20, 4, 1);
									g.fillRect(tx + 19, ty + 22, 5, 1);
								}
								break;
							case RED_DOOR:
								g.setColor(Color.LIGHT_GRAY);
								g.fillRect(tx + 10, ty, 20, 19 - anim[y][x] / 2);
								g.fillRect(tx + 10, ty + 21 + anim[y][x] / 2, 20, 19 - anim[y][x] / 2);
								if (anim[y][x] > 0 && ++anim[y][x] == 38) {
									map[y][x] = 0;
								}
								g.setColor(new Color(180, 50, 50));
								g.fillRect(tx + 5, ty, 30, 3);
								g.fillRect(tx + 5, ty + 37, 30, 3);
								break;
							case IN_PORTAL_1:
							case IN_PORTAL_2:
							case OUT_PORTAL_1:
							case OUT_PORTAL_2:
								switch (map[y][x]) {
									case IN_PORTAL_1:
									case OUT_PORTAL_1:
										g.setColor(Color.GREEN);
										break;
									case IN_PORTAL_2:
									case OUT_PORTAL_2:
										g.setColor(Color.ORANGE);
										break;
								}
								g.fillOval(tx, ty, 40, 40);
								g.setColor(Color.WHITE);
								int q = (tick / 4) % 5;
								switch (map[y][x]) {
									case IN_PORTAL_1:
									case IN_PORTAL_2:
										q = 5 - q;
								}
								for (; q < 20; q += 5) {
									g.drawOval(tx + 20 - q, ty + 20 - q, q * 2, q * 2);
								}
								break;
							case TANK:
								g.setColor(Color.LIGHT_GRAY);
								g.fillRect(tx + 8, ty, 24, 40);
								g.setColor(new Color(70, 70, 90));
								g.fillRect(tx + 10, ty + 2, 20, 30);
								g.setColor(new Color(70, 170, 200));
								g.fillRect(tx + 10, ty + 6, 20, 26);
								g.setColor(new Color(56, 112, 120));
								// Head
								g.fillOval(tx + 17, ty + 8 + (tick / 50) % 2, 6, 6);
								// Torso
								g.fillRect(tx + 17, ty + 14 + (tick / 50) % 2, 6, 8);
								// Arms
								g.fillRect(tx + 14, ty + 14 + (tick / 50) % 2, 2, 7);
								g.fillRect(tx + 24, ty + 14 + (tick / 50) % 2, 2, 7);
								// Legs
								g.fillRect(tx + 17, ty + 22 + (tick / 50) % 2, 2, 8);
								g.fillRect(tx + 20, ty + 22 + (tick / 50) % 2, 2, 8);
								g.setColor(new Color(255, 255, 255, 30));
								g.fillRect(tx + 14, ty, 5, 40);
								g.fillRect(tx + 15, ty, 2, 40);
								break;
						}
					}
				}

				for (int u = 0; u < NUM_UNITS; u++) {
					int ux = (int) unit_f[u * F_STRIDE + X];
					int uy = (int) unit_f[u * F_STRIDE + Y];
					switch (unit_i[u * I_STRIDE + TYPE] / GROUP_DIV) {
						case PEOPLE_GROUP:
							g.setColor(new Color(208, 169, 130));
							g.fillOval(ux + 3, uy, 6, 6);
							switch (unit_i[u * I_STRIDE + TYPE]) {
								case DUDE:
									g.setColor(Color.WHITE);
									break;
								case GRUNT:
									g.setColor(new Color(99, 121, 77));
									break;
								case OFFICER:
									g.setColor(Color.BLACK);
									break;
								case ARMOR:
									g.setColor(Color.GRAY);
									break;
							}
							// Torso
							g.fillRect(ux + 3, uy + 6, 6, 8);
							// Arms
							g.fillRect(ux, uy + 6, 2, 7);
							g.fillRect(ux + 10, uy + 6, 2, 7);
							// Legs!
							if (unit_f[u * F_STRIDE + DX] != 0) {
								g.fillRect(ux + 3, uy + 14, 2, 6 + ((tick / 10) % 2) * 2);
								g.fillRect(ux + 6, uy + 14, 2, 6 + ((tick / 10 + 1) % 2) * 2);
							} else {
								g.fillRect(ux + 3, uy + 14, 2, 8);
								g.fillRect(ux + 6, uy + 14, 2, 8);
							}
							if (unit_i[u * I_STRIDE + AUTH] > 0) {
								g.setColor(Color.RED);
								g.fillRect(ux + 7, uy + 6, 2, 3);
							}
							if (u == controlledUnit) {
								g.setColor(Color.GREEN);
								g.fillOval(ux + 5, uy + 7, 3 + ((tick / 7) % 3), 8);
							}
							if (unit_i[u * I_STRIDE + LOOK_STATUS] > 0) {
								g.setColor(Color.YELLOW);
								g.fillRect(ux + 4, uy - 20, 3, 14);
								g.fillRect(ux + 4, uy - 5, 3, 3);
							}
							break;
						case BULLET_GROUP:
							switch (unit_i[u * I_STRIDE + TYPE]) {
								case BULLET:
									g.setColor(Color.YELLOW);
									break;
								case PSI_BULLET:
									g.setColor(Color.GREEN);
									break;
								case LOOK:
									g.setColor(new Color(0, 0, 0, 0));
									break;
							}
							g.fillRect(ux - 2, uy - 2, 5, 5);
							break;
						case PARTICLE_GROUP:
							switch (unit_i[u * I_STRIDE + TYPE]) {
								case BLOOD:
									g.setColor(Color.RED);
									break;
								case ICHOR:
									g.setColor(Color.GREEN);
									break;
							}
							g.fillRect(ux, uy, 2, 2);
							break;
					}
				}

				g.setColor(Color.RED);
				g.fillRect(mx, my, 2, 2);

				if (paused) {
					g.setColor(new Color(0, 0, 0, 127));
					g.fillRect(0, 0, 800, 600);
					if (victory) {
						g.setColor(Color.WHITE);
						g.scale(8.0, 8.0);
						g.drawString("VICTORY", 2, 30);
						g.scale(0.125, 0.125);
						g.drawString("Hard mode enabled!", 16, 300);
					}
				}

				strategy.show();
				try {
					Thread.sleep(25);
				} catch (Exception e) {
				}
			}
		}
	}
}

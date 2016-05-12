package ncl.cs.prime.archon.arch.modules.arm;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Mmu extends ArmModule {

	public static final long TIME_READ = 10;
	public static final long TIME_WRITE = 120;
	
	private static final String[] CONFIG_NAMES = {"read", "write"};

	public static int[] sharedMemory = new int[1024*1024];
	public static JPanel memView = null;
	public static Thread memViewRefresher = null;
	public static long sharedTime = 0L;
	public static boolean criticalRead = false;
	public static boolean criticalWrite = true;

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"n", "addr"};
		d.outputNames = new String[] {"d"};
		d.configNames = CONFIG_NAMES;
		return d;
	}
	
	public static JPanel createView() {
		if(memView!=null)
			return memView;
		
		memView = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				Graphics2D g2 = (Graphics2D) g;
				for(int y=0; y<256*2; y++)
					for(int x = 0; x<256; x++) {
						int c = sharedMemory[y*256+x+65536];
						if(c<0) c = 0;
						if(c>255) c = 255;
						g2.setColor(new Color(c, c, c));
						g2.drawLine(x, y, x, y);
					}
			}
		};
		memView.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				memView.repaint();
			}
		});
		
		memView.setPreferredSize(new Dimension(256, 512));
		memView.setMinimumSize(new Dimension(256, 512));
		return memView;
	}
	
	public static void initMatrix(int[] matrix) {
		if(matrix==null)
			matrix = new int[] {0, 1, 0, 1, -4, 1, 0, 1, 0};
		for(int n=0; n<matrix.length; n++)
			sharedMemory[n] = matrix[n];
	}
	
	public static void initData(String imagePath, int[] matrix, boolean reset) {
		if(reset)
			Arrays.fill(sharedMemory, 0);
			
		initMatrix(matrix);
		try {
			BufferedImage img = ImageIO.read(new File(imagePath));
			for(int y=0; y<256; y++)
				for(int x = 0; x<256; x++) {
					sharedMemory[y*256+x+65536] = img.getRGB(x, y) & 255;
				}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		sharedTime = 0L;
	}
	
	protected InPort<Integer> in = new InPort<>(this);
	protected InPort<Integer> addr = new InPort<>(this);
	protected OutPort<Integer> out = new OutPort<Integer>(this, -1);
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {in, addr};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {out};
	}
	
	@Override
	protected String getResourceName() {
		return "mmu-"+CONFIG_NAMES[config];
	}
	
	@Override
	public long getDuration() {
		return config==0 ? TIME_READ : TIME_WRITE;
	}
	
	@Override
	protected void update() {
		int a = addr.getValue();
		if(a%4>0) {
			throw new UnsupportedOperationException("Memory address not aligned ("+a+").");
		}
		a >>= 2;
		if (config==0) { // read mode
			out.value = sharedMemory[a];
		}
		else { // write mode
			sharedMemory[a] = in.getValue();
			out.value = in.getValue();
		}
		if(config==0 && criticalRead || config==1 && criticalWrite) {
			syncTime(sharedTime);
			super.update();
			sharedTime = getTime();
		}
		else {
			super.update();
		}
	}

}

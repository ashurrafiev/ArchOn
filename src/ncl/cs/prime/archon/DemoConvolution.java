package ncl.cs.prime.archon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import ncl.cs.prime.archon.arch.modules.arm.ArmEstimation;
import ncl.cs.prime.archon.arch.modules.arm.Mmu;
import ncl.cs.prime.archon.bytecode.CodeExecutor;
import ncl.cs.prime.archon.bytecode.CodeExecutor.ExecMode;
import ncl.cs.prime.archon.parse.ProgramParserBytecode;

public class DemoConvolution extends JFrame {

	public static final String IMAGE_PATH = "examples/sample.png";
	public static final int MAX_CORES = 64;
	
	private int[] matrix = {0, -1, 0, -1, 4, -1, 0, -1, 0};
	private int numCores = 1;
	private double voltage = 0.8;
	private double powerLimit = 0;
	private double freqLimit = 0;
	private boolean printTime = true;
	private boolean useScaling = false;

	private File program;
	private int threadCounter;
	
	private JPanel controls;
	private JTextField[] txtMatrix = new JTextField[9];
	private JTextField txtCores, txtVoltage, txtPowerLimit, txtFreqLimit, txtTimeLimit;
	private JLabel labFreq;
	private JCheckBox chkPrintTime;
	private JCheckBox chkUseScaling;
	private JCheckBox chkCSMemRead;
	private JCheckBox chkCSMemWrite;
	private JButton btnRun;
	private JButton btnReset;
	private JTextPane txtInfo;
	
	private Thread painter;
	private long simTime;
	
	private static float fontSize = 11f;
	
	private static void setFontSize(JComponent component, float size) {
		if(component.getFont()!=null) {
			component.setFont(component.getFont().deriveFont(size));
		}
		for(Component c : component.getComponents()) {
			setFontSize((JComponent) c, size);
		}
	}
	
	private static int scaled(int x) {
		return (int) Math.ceil((float) x * fontSize / 11f);
	}
	
	private double timeFromFreq(double freq) {
		return 26607635.0 / freq;
	}
	
	private void updateControls() {
		voltage = PrimeModel.model.setVoltage(voltage);
		
		txtCores.setText(Integer.toString(numCores));
		txtVoltage.setText(formatDouble(voltage, 2));
		labFreq.setText("Freq: "+formatDouble(PrimeModel.model.getF(PrimeModel.model.getActive()) / 1000000.0, 2)+"MHz");
		txtPowerLimit.setText(powerLimit > 0.0 ? formatDouble(powerLimit, 7) : "");
		txtFreqLimit.setText(freqLimit > 0.0 ? formatDouble(freqLimit / 1000000.0, 2) : "");
		txtTimeLimit.setText(freqLimit > 0.0 ? formatDouble(timeFromFreq(freqLimit) * 1000.0, 3) : "");
		for(int i=0; i<matrix.length; i++)
			txtMatrix[i].setText(Integer.toString(matrix[i]));
		chkPrintTime.setSelected(printTime);
		chkCSMemWrite.setSelected(Mmu.criticalWrite);
		chkCSMemRead.setSelected(Mmu.criticalRead);
		chkUseScaling.setSelected(useScaling);
		
		repaint();
	}
	
	private static String formatDouble(double x, int prec) {
		return String.format("%."+prec+"f", x);
	}
	
	private static int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch(NumberFormatException e) {
			System.err.println("Bad number format: "+e.getMessage());
			return 0;
		}
	}

	private static double parseDouble(String s) {
		try {
			return Double.parseDouble(s);
		} catch(NumberFormatException e) {
			System.err.println("Bad number format: "+e.getMessage());
			return 0;
		}
	}

	private void readSettings() {
		numCores = parseInt(txtCores.getText());
		if(numCores<1) numCores = 1;
		if(numCores>MAX_CORES) numCores = MAX_CORES;
		PrimeModel.model.setNCores(numCores);
		
		voltage = PrimeModel.model.setVoltage(parseDouble(txtVoltage.getText()));
		powerLimit = txtPowerLimit.getText().isEmpty() ? 0.0 : parseDouble(txtPowerLimit.getText());
		if(txtFreqLimit.getText().isEmpty()) {
			if(txtTimeLimit.getText().isEmpty())
				freqLimit = 0.0;
			else
				freqLimit = timeFromFreq(parseDouble(txtTimeLimit.getText()) / 1000.0);
		}
		else
			freqLimit =parseDouble(txtFreqLimit.getText()) * 1000000.0;
		
		printTime = chkPrintTime.isSelected();
		Mmu.criticalWrite = chkCSMemWrite.isSelected();
		Mmu.criticalRead = chkCSMemRead.isSelected();
		useScaling = chkUseScaling.isSelected();
		
		for(int i=0; i<matrix.length; i++)
			matrix[i] = parseInt(txtMatrix[i].getText());
		Mmu.initMatrix(matrix);
		
		updateControls();
	}
	
	private JPanel createMatrixEditor() {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets.bottom = scaled(1);
		c.insets.left = scaled(1);
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		for(int x=0; x<3; x++) {
			c.gridx = x;
			for(int y=0; y<3; y++) {
				c.gridy = y;
				int i = x+y*3;
				txtMatrix[i] = new JTextField();
				p.add(txtMatrix[i], c);
			}
		}
		
		return p;
	}
	
	private JPanel createCSEditor() {
		JPanel p = new JPanel(new GridLayout(2, 1));
		p.setBorder(BorderFactory.createTitledBorder("Critical section"));
		p.add(chkCSMemWrite = new JCheckBox("Memory write"));
		p.add(chkCSMemRead = new JCheckBox("Memory read"));
		return p;
	}
	
	private JPanel createLimitEditor(JTextField edit, ActionListener actionListener) {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(edit, c);
		c.gridx = 1;
		c.weightx = 0;
		JButton btnApply = new JButton("Apply");
		btnApply.addActionListener(actionListener);
		p.add(btnApply, c);
		return p;
	}
	
	private JPanel createControls() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createEmptyBorder(scaled(16), scaled(16), scaled(16), scaled(16)));
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.insets.bottom = scaled(2);
		c.insets.left = scaled(2);
		c.weightx = 1;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		p.add(new JLabel("Matrix:"), c);

		c.gridy++;
		c.insets.bottom = scaled(20);
		p.add(createMatrixEditor(), c);

		c.gridy++;
		c.insets.bottom = scaled(2);
		p.add(new JLabel("Power upper limit (W):"), c);
		
		c.gridy++;
		txtPowerLimit = new JTextField();
		p.add(createLimitEditor(txtPowerLimit, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				applyPowerLimit();
			}
		}), c);

		c.gridy++;
		c.insets.bottom = scaled(2);
		p.add(new JLabel("Troughput lower limit (MHz):"), c);
		
		c.gridy++;
		txtFreqLimit = new JTextField();
		p.add(createLimitEditor(txtFreqLimit, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				applyFreqLimit();
			}
		}), c);

		c.gridy++;
		c.insets.bottom = scaled(2);
		p.add(new JLabel("Time limit (ms):"), c);
		
		c.gridy++;
		txtTimeLimit = new JTextField();
		p.add(createLimitEditor(txtTimeLimit, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				txtFreqLimit.setText("");
				applyFreqLimit();
			}
		}), c);
		
		c.gridy++;
		chkUseScaling = new JCheckBox("Use many-core scaling profile");
		p.add(chkUseScaling, c);

		
		c.gridy++;
		c.insets.bottom = scaled(2);
		p.add(new JLabel("Cores:"), c);
		
		c.gridy++;
		txtCores = new JTextField();
		p.add(txtCores, c);

		c.gridy++;
		p.add(new JLabel("Voltage:"), c);
		
		c.gridy++;
		txtVoltage = new JTextField();
		p.add(txtVoltage, c);

		c.gridy++;
		labFreq = new JLabel();
		p.add(labFreq, c);
		
		c.gridy++;
		chkPrintTime = new JCheckBox("Report timing");
		p.add(chkPrintTime, c);
		
		c.gridy++;
		c.insets.bottom = scaled(5);
		p.add(createCSEditor(), c);
		
		c.gridy++;
		c.gridwidth = 1;
		c.insets.top = scaled(10);
		btnRun = new JButton("Run");
		btnRun.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				btnReset.setEnabled(false);
				btnRun.setEnabled(false);
				readSettings();
				runProgramRoundRobin();
			}
		});
		p.add(btnRun, c);
		
		c.gridx++;
		btnReset = new JButton("Reset memory");
		btnReset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				resetMemory();
				repaint();
			}
		});
		p.add(btnReset, c);
		
		updateControls();
		resetMemory();
		return p;
	}
	
	private JPopupMenu createPopup() {
		JPopupMenu menu = new JPopupMenu();
		JMenuItem menuSize = new JMenuItem("Font Size...");
		menuSize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					int size = Integer.parseInt(JOptionPane.showInputDialog(null, "Font size"));
					fontSize = size;
					setFontSize((JComponent) DemoConvolution.this.getContentPane(), (float) size);
					DemoConvolution.this.invalidate();
				}
				catch(Exception e) {
				}
			}
		});
		menu.add(menuSize);
		return menu;
	}
	
	private void showPopup(MouseEvent e) {
		if(e.isPopupTrigger()) {
			createPopup().show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	public DemoConvolution(File program) {
		this.program = program;
//		setMinimumSize(new Dimension(760, 560));
		
		setTitle("ArchOn Demo: Convolution");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		JPanel cp = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0;
		c.weighty = 1;
		
		controls = createControls();
		controls.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				showPopup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				showPopup(e);
			}
		});
		JScrollPane sp = new JScrollPane(controls, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setMinimumSize(new Dimension(controls.getPreferredSize().width + sp.getVerticalScrollBar().getPreferredSize().width, controls.getPreferredSize().height));
		cp.add(sp, c);
		
		c.weightx = 1;
		c.gridx++;
		
		JTabbedPane tabs = new JTabbedPane();
		cp.add(tabs, c);
		
		JPanel simTab = new JPanel(new BorderLayout());
		simTab.setBackground(Color.WHITE);
		simTab.add(Mmu.createView(), BorderLayout.WEST);
//		txtInfo = new JLabel();
		txtInfo = new JTextPane();
		txtInfo.setContentType("text/html");
		txtInfo.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		txtInfo.setBorder(BorderFactory.createEmptyBorder(scaled(8), scaled(8), scaled(8), scaled(8)));
		txtInfo.setPreferredSize(new Dimension(300, 200));
//		txtInfo.setVerticalAlignment(JLabel.NORTH);
		txtInfo.setText("<html>Press <b>Run</b> to start simulation...<br></html>");
		
		simTab.add(txtInfo, BorderLayout.CENTER);
		tabs.addTab("Simulation", new JScrollPane(simTab));
		
		JPanel gp = new JPanel() {
			@Override
			public void paint(Graphics g) {
				new Plot().plotModel((Graphics2D) g, getWidth(), getHeight());
			}
		};
		gp.setPreferredSize(new Dimension(512, 512));
		gp.setBackground(Color.WHITE);
		tabs.addTab("PEAR model", gp);
		
		setContentPane(cp);
		pack();
		setSize(640, 480);
		setVisible(true);
	}
	
	private int getScalingType() {
		if(Mmu.criticalRead && Mmu.criticalWrite)
			return 2;
		if(!Mmu.criticalRead && Mmu.criticalWrite)
			return 1;
		if(!Mmu.criticalRead && !Mmu.criticalWrite)
			return 0;
		System.err.println("No scaling profile for this setup!");
		return 0; 
	}
	
	public void applyPowerLimit() {
		readSettings();
		if(powerLimit<=0)
			return;
		freqLimit = 0;
		boolean success = useScaling ? PrimeModel.model.setPowerLimitScaled(powerLimit, getScalingType()) : PrimeModel.model.setPowerLimit(powerLimit);
		txtPowerLimit.setForeground(success ? Color.BLACK : Color.RED);
		numCores = PrimeModel.model.getNCores();
		voltage = PrimeModel.model.getV(PrimeModel.model.getActive());
		updateControls();
	}

	public void applyFreqLimit() {
		readSettings();
		if(freqLimit<=0)
			return;
		powerLimit = 0;
		boolean success = useScaling ? PrimeModel.model.setFreqLimitScaled(freqLimit, getScalingType()) : PrimeModel.model.setFreqLimit(freqLimit);
		txtFreqLimit.setForeground(success ? Color.BLACK : Color.RED);
		numCores = PrimeModel.model.getNCores();
		voltage = PrimeModel.model.getV(PrimeModel.model.getActive());
		updateControls();
	}

	private long infoCyclesMin, infoCyclesMax;
	
	public void simulationFinished() {
		long t = (System.nanoTime() - simTime)/1000000L;
		System.out.println("----- Simulation stopped after "+t+"ms. -----\n\n");
		String estim = "(Unavailable)";
		if(printTime) {
			double freq = PrimeModel.model.getF(PrimeModel.model.getActive());
			estim = "Min time (cycles): <b>" + infoCyclesMin + "</b><br>" +
					"&nbsp;= " + formatDouble(infoCyclesMin / freq * 1000.0, 3) + "ms at " + formatDouble(freq / 1000000.0, 2) + "MHz<br>" +
					"Max time (cycles): <b>" + infoCyclesMax + "</b><br>" +
					"&nbsp;= " + formatDouble(infoCyclesMax / freq * 1000.0, 3) + "ms at " + formatDouble(freq / 1000000.0, 2) + "MHz<br>";
		}
		txtInfo.setText("<html><i>Simulation stopped after "+t+"ms.</i><br>" +
			"&nbsp;<br>" +
			"<u>Estimated parameters:</u><br>" +
			estim +
			ArmEstimation.estimDump +
			"</html>");
		
		btnReset.setEnabled(true);
		btnRun.setEnabled(true);
		painter.interrupt();
		repaint();
	}
	
	public void resetMemory() {
		Mmu.initData(IMAGE_PATH, null, true);
	}
	
	private void startPainter() {
		System.out.println("----- Starting simulation... -----\n");
		txtInfo.setText("<html><i>Simulation started...</i></html>");
		infoCyclesMin = Long.MAX_VALUE;
		infoCyclesMax = Long.MIN_VALUE;
		ArmEstimation.estimDump = "";
		
		painter = new Thread() {
			@Override
			public void run() {
				try {
					for(;;) {
						Thread.sleep(250);
						Mmu.memView.repaint();
					}
				}
				catch(InterruptedException e) {}
			}
		};
		painter.start();
		simTime = System.nanoTime();
	}
	
	public void runProgramParallel() {
		startPainter();
		threadCounter = numCores;
		final int span = (int) Math.ceil(256f/(float)numCores);
		for(int i=0; i<numCores; i++) {
			final CodeExecutor exec = new CodeExecutor();
			final int index = i;
			new Thread() {
				public void run() {
					try {
						exec.getIP().loadCode(program);
						exec.execute(new int[] {(span*index)*1024, span});
						if(printTime)
							System.out.printf("Core %d stopped at platform time %d\n", index, exec.getArch().syncTime());
						threadCounter--;
						if(threadCounter<=0) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									simulationFinished();
								}
							});
						}
					} catch (IOException e) {
						System.err.println(e.getMessage());
					}
				};
			}.start();
		}
	}
	
	public void runProgramRoundRobin() {
		startPainter();
		Mmu.sharedTime = 0L;
		new Thread() {
			public void run() {
				int span = (int) Math.ceil(256f/(float)numCores);
				CodeExecutor[] exec = new CodeExecutor[numCores];
				for(int i=0; i<numCores; i++) {
					try {
						exec[i] = new CodeExecutor();
//						exec[i] = new FaultyCodeExecutor(500000, 100000000);
						exec[i].getIP().loadCode(program);
						if(i==0)
							exec[i].setEst(new ArmEstimation());
						exec[i].executeFirst(new int[] {(span*i)*1024, span});
					} catch (IOException e) {
						System.err.println(e.getMessage());
						exec[i] = null;
					}
				}
				boolean running;
				do {
					running = false;
					for(int i=0; i<numCores; i++) {
						if(exec[i]==null)
							continue;
						if(exec[i].executeNext(ExecMode.normal))
							running = true;
						else {
							if(printTime) {
								long t = exec[i].getArch().syncTime();
								if(t<infoCyclesMin) infoCyclesMin = t;
								if(t>infoCyclesMax) infoCyclesMax = t;
								System.out.printf("Core %d stopped at platform time %d\n", i, t);
							}
							if(i==0)
								exec[i].getEst().dump();
							exec[i] = null;
						}
					}
				} while(running);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						simulationFinished();
					}
				});
			}
		}.start();
	}
	

	
	public static void main(String[] args) {
		SilverOceanTheme.enable();
		new DemoConvolution(ProgramParserBytecode.compileFile("examples/convo_arm.sim", true));
	}

}

package ncl.cs.prime.archon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import ncl.cs.prime.archon.arch.modules.arm.ArmEstimation;
import ncl.cs.prime.archon.arch.modules.arm.Mmu;
import ncl.cs.prime.archon.bytecode.FaultyCodeExecutor;
import ncl.cs.prime.archon.bytecode.CodeExecutor.ExecMode;
import ncl.cs.prime.archon.parse.ArmInstructionMapper;
import ncl.cs.prime.archon.parse.ProgramParserBytecode;
import ncl.cs.prime.archon.parse.ArmInstructionMapper.ArmLine;

public class DemoFaults extends JFrame {

	public static final byte[] NAME_IDS = {-1, -2, -3, -4, -5, -8, -9, 0, 1, 2, 3, 8, 16, 17, 24, 32, 33, 34, 48, 49, 64, 65, 112};
	public static final String[] NAMES = {"debug:print", "debug:printstr", "debug:println", "{D}init", "{D}param", "debug:estim",
		"debug:estprint", "{D}unlink", "{D}link", "{D}link flag", "{D}link nflag", "{D}unlink all", "{D}config", "{J}jump", "nop", "{X}exec", "{X}exec n", "{X}stop", "{J}condition", "{J}ncondition", "assign", "free", "aliases"};

	public static final String SRC_PATH = "convo_arm.sim";
	public static final String IMAGE_PATH = "sample.png";
	public static final int MAX_CORES = 64;
	public static final int MAX_TEST_RUNS = 1000;
	
	private int[] matrix = {0, -1, 0, -1, 5, -1, 0, -1, 0};
	private int testRuns = 500;
	private int numCores = 1;
	private double faultRate = 1.0;
	private boolean printTime = true;

	private JTextField[] txtMatrix = new JTextField[9];
	private JTextField txtCores, txtFaultRate, txtRuns;
	private JButton btnRun, btnRunStats;
	private JTextPane txtInfo;
	private JLabel txtStatInfo;
	private JTextPane txtFaults;
	private JProgressBar progStats;
	private ZoomMemView memView;

	public static DemoStatistics stats = null;
	
	private File program;
	private ArmInstructionMapper mapper;

	private FaultyCodeExecutor[] exec = null;
	private Thread painter;
	private long simTime;

	private static void setFontSize(JComponent component, float size) {
		if(component.getFont()!=null) {
			component.setFont(component.getFont().deriveFont(size));
		}
		for(Component c : component.getComponents()) {
			setFontSize((JComponent) c, size);
		}
	}
	
	private static String cmdName(byte cmd) {
		for(int i=0; i<NAMES.length; i++)
			if(NAME_IDS[i]==cmd)
				return NAMES[i];
		return "?";
	}

	private void updateControls() {
		txtRuns.setText(Integer.toString(testRuns));
		txtCores.setText(Integer.toString(numCores));
		txtFaultRate.setText(formatDouble(faultRate, 2));
		for(int i=0; i<matrix.length; i++)
			txtMatrix[i].setText(Integer.toString(matrix[i]));
		
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

		testRuns = parseInt(txtRuns.getText());
		if(testRuns<1) testRuns = 1;
		if(testRuns>MAX_TEST_RUNS) testRuns = MAX_TEST_RUNS;

		PrimeModel.model.setVoltage(1.0);

		faultRate = parseDouble(txtFaultRate.getText());
		if(faultRate<0.01) faultRate = 0.01;

		Mmu.criticalWrite = true;
		Mmu.criticalRead = false;
		
		int[] oldMatrix = matrix;
		boolean invalidateGolden = false;
		matrix = new int[9];
		for(int i=0; i<matrix.length; i++) {
			matrix[i] = parseInt(txtMatrix[i].getText());
			if(oldMatrix[i]!=matrix[i])
				invalidateGolden = true;
		}
		if(invalidateGolden || stats==null) {
			stats = new DemoStatistics(program, matrix);
			resetMemory();
			Mmu.initMatrix(matrix);
		}	
		else
			Mmu.initMatrix(matrix);
		
		updateControls();
	}


	private JPanel createSourceList() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBackground(Color.WHITE);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.weighty = 0;
		c.insets.top = 2;
		c.insets.bottom = 2;
		c.insets.left = 0;
		c.insets.right = 0;
		
		for(final ArmLine line : mapper.lines()) {
			if(line.arm==null)
				continue;

			c.gridx = 0;
			c.weightx = 0;
			c.insets.left = 10;
			c.weightx = 0;
			p.add(new JLabel(Integer.toString(line.armIndex)), c);

			c.insets.left = 10;
			c.gridx++;
			final JCheckBox chD = new JCheckBox("D");
			chD.setBackground(Color.WHITE);
			chD.setEnabled((line.faultType & ArmInstructionMapper.FAULT_TYPE_LINK)!=0);
			chD.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if(chD.isSelected())
						line.reinforceType |= ArmInstructionMapper.FAULT_TYPE_LINK;  
					else
						line.reinforceType &= ~ArmInstructionMapper.FAULT_TYPE_LINK;  
				}
			});
			p.add(chD, c);

			c.insets.left = 0;
			c.insets.right = 20;
			c.gridx++;
			final JCheckBox chX = new JCheckBox("X");
			chX.setBackground(Color.WHITE);
			chX.setEnabled((line.faultType & ArmInstructionMapper.FAULT_TYPE_EXEC)!=0);
			chX.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if(chX.isSelected())
						line.reinforceType |= ArmInstructionMapper.FAULT_TYPE_EXEC;  
					else
						line.reinforceType &= ~ArmInstructionMapper.FAULT_TYPE_EXEC;  
				}
			});
			p.add(chX, c);

			c.gridx++;
			c.insets.right = 0;
			c.weightx = 1;
			p.add(new JLabel(line.arm), c);
			c.gridy++;
		}
		c.gridx = 0;
		c.gridwidth = 4;
		c.weighty = 1;
		p.add(new JPanel(), c);
//		p.setPreferredSize(new Dimension(300, c.gridy*24));
		return p;
	}
	
	private JPanel createMainControls() {
		JPanel p = new JPanel(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.weightx = 1;
		c.weighty = 1;
		c.insets.top = 10;
		c.insets.left = 10;
		c.insets.right = 10;
		
		c.insets.bottom = 2;
		p.add(new JLabel("Faults (per 1M cycles):"), c);
		
		c.gridy++;
		c.insets.top = 0;
		c.insets.bottom = 10;
		txtFaultRate = new JTextField();
		p.add(txtFaultRate, c);

		c.gridx++;
		c.gridy = 0;
		c.gridheight = 2;
		c.insets.top = 10;
		c.insets.bottom = 10;
		c.fill = GridBagConstraints.BOTH;
		btnRun = new JButton("Run");
		btnRun.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				btnRunStats.setEnabled(false);
				btnRun.setEnabled(false);
				resetMemory();
				readSettings();
				repaint();
				runProgramRoundRobin();
			}
		});
		p.add(btnRun, c);
		return p;
	}
	
	private JPanel createMatrixEditor() {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets.bottom = 1;
		c.insets.left = 1;
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

	private JPanel createAdvancedControls() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.insets.bottom = 2;
		c.insets.left = 2;
		c.weightx = 1;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		p.add(new JLabel("Matrix:"), c);

		c.gridy++;
		c.insets.bottom = 20;
		p.add(createMatrixEditor(), c);

		c.gridy++;
		c.insets.bottom = 2;
		p.add(new JLabel("Cores:"), c);
		
		c.gridy++;
		txtCores = new JTextField();
		p.add(txtCores, c);

		return p;
	}

	private JPanel createStatControls() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.insets.bottom = 2;
		c.insets.left = 2;
		c.weightx = 1;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		c.gridy++;
		c.insets.bottom = 2;
		p.add(new JLabel("Test runs:"), c);
		
		c.gridy++;
		c.insets.bottom = 10;
		txtRuns = new JTextField();
		p.add(txtRuns, c);

		c.gridy++;
		c.insets.bottom = 20;
		progStats = new JProgressBar();
		p.add(progStats, c);

		c.gridy++;
		c.insets.bottom = 20;
		txtStatInfo = new JLabel("-");
		p.add(txtStatInfo, c);

		c.gridy++;
		c.gridwidth = 1;
		btnRunStats = new JButton("Run stats");
		btnRunStats.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				btnRunStats.setEnabled(false);
				btnRun.setEnabled(false);
				readSettings();
				runStats();
			}
		});
		p.add(btnRunStats, c);

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
					setFontSize((JComponent) DemoFaults.this.getContentPane(), (float) size);
					DemoFaults.this.invalidate();
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

	public DemoFaults(File program, ArmInstructionMapper mapper) {
		this.program = program;
		this.mapper = mapper;
//		setMinimumSize(new Dimension(760, 560));
		
		setTitle("ArchOn Demo: Convolution");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		JPanel cp = new JPanel(new GridBagLayout());
		cp.setPreferredSize(new Dimension(800, 600));
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridheight = 2;

		cp.add(new JScrollPane(createSourceList(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), c);

		cp.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				showPopup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				showPopup(e);
			}
		});

		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.gridheight = 1;
		c.weightx = 1;
		c.weighty = 0;
		cp.add(createMainControls(), c);

		c.gridx = 1;
		c.gridy = 1;
		c.weighty = 1;
		JTabbedPane tabs = new JTabbedPane();
		cp.add(tabs, c);

		JPanel simTab = new JPanel(new BorderLayout());
		simTab.setBackground(Color.WHITE);
		simTab.add(memView = new ZoomMemView(), BorderLayout.CENTER);
		JPanel viewControls = new JPanel(new FlowLayout());
		final JCheckBox chShowGolden = new JCheckBox("Show golden result");
		chShowGolden.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				memView.showGolden = chShowGolden.isSelected();
				repaint();
			}
		});
		viewControls.add(chShowGolden);
		final JCheckBox chShowDiff = new JCheckBox("Highlight difference");
		chShowDiff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				memView.showDiff = chShowDiff.isSelected();
				repaint();
			}
		});
		viewControls.add(chShowDiff);
		simTab.add(viewControls, BorderLayout.NORTH);
		tabs.addTab("Simulation", new JScrollPane(simTab));

		JPanel advTab = createAdvancedControls();
		tabs.addTab("Advanced", new JScrollPane(advTab));

		JPanel statTab = createStatControls();
		tabs.addTab("Statistics", new JScrollPane(statTab));

		txtFaults = new JTextPane();
		txtFaults.setContentType("text/html");
		txtFaults.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		txtFaults.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		txtFaults.setPreferredSize(new Dimension(300, 200));
		txtFaults.setText("<html>Press <b>Run</b> to start simulation...<br></html>");
		tabs.addTab("Faults", new JScrollPane(txtFaults));

		txtInfo = new JTextPane();
		txtInfo.setContentType("text/html");
		txtInfo.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		txtInfo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		txtInfo.setPreferredSize(new Dimension(300, 200));
		txtInfo.setText("<html>Press <b>Run</b> to start simulation...<br></html>");
		tabs.addTab("Estimates", new JScrollPane(txtInfo));
		
		updateControls();
		
		setContentPane(cp);
		pack();
		setVisible(true);
		
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
		
		btnRunStats.setEnabled(true);
		btnRun.setEnabled(true);
		painter.interrupt();
		updateFaultList();
		repaint();
	}

	public void resetMemory() {
		Mmu.initData(IMAGE_PATH, null, true);
	}

	private void updateFaultList() {
		if(exec==null)
			return;
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		for(int i=0; i<numCores; i++) {
			if(exec==null || exec[i]==null || exec[i].injections==null)
				continue;
			String errMax = exec[i].errors.size()>=FaultyCodeExecutor.MAX_ERRORS ? " (max!)" : "";
			sb.append("<u>Core <b>"+i+"</b></u><br>&nbsp; total sim cycles: "+exec[i].counter+",<br>&nbsp; faults: <b>"+exec[i].injections.size()+"</b>,<br>&nbsp; exceptions: <b>"+exec[i].errors.size()+errMax+"</b><br>");
		}
		sb.append("<hr><table boder=\"1\" cellpadding=\"4\" cellspacing=\"0\">");
		sb.append("<tr><th>#</th><th>Core</th><th>Cycle</th><th>Addr</th><th>Cmd</th><th>Line</th></tr>");
		int n = 0;
		for(int i=0; i<numCores; i++) {
			if(exec==null || exec[i]==null || exec[i].injections==null)
				continue;
			for(FaultyCodeExecutor.InjectionInfo f : exec[i].injections) {
				int line = mapper.findArmIndexForAddr(f.addr);
				sb.append("<tr><td>"+n+"</td><td>"+i+"</td><td>"+f.counter+"</td><td>"+f.addr+"</td><td>"+cmdName(f.cmd)+"</td><td>"+line+"</td></tr>");
				n++;
			}
		}
		sb.append("</table>");
		sb.append("</html>");
		txtFaults.setText(sb.toString());
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
						memView.repaint();
					}
				}
				catch(InterruptedException e) {}
			}
		};
		painter.start();
		simTime = System.nanoTime();
	}

	public void runProgramRoundRobin() {
		startPainter();
		Mmu.sharedTime = 0L;
		new Thread() {
			public void run() {
				int span = (int) Math.ceil(256f/(float)numCores);
				exec = new FaultyCodeExecutor[numCores];
				boolean finished[] = new boolean[numCores];
				byte[] code = mapper.compile();
				for(int i=0; i<numCores; i++) {
					try {
						exec[i] = new FaultyCodeExecutor((int) (1000000.0 / faultRate), 1000000000);
						exec[i].getIP().setCode(code);
						if(i==0)
							exec[i].setEst(new ArmEstimation());
						exec[i].executeFirst(new int[] {(span*i)*1024, span});
						finished[i] = false;
					} catch (Exception e) {
						System.err.println(e.getMessage());
						exec[i] = null;
					}
				}
				boolean running;
				do {
					running = false;
					for(int i=0; i<numCores; i++) {
						if(finished[i])
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
							finished[i] = true;
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

	public void runStats() {
		progStats.setMaximum(testRuns);
		progStats.setValue(0);
		new Thread() {
			@Override
			public void run() {
				byte[] code = mapper.compile();
//				final long errors = stats.run(testRuns, code, (int) (1000000.0 * (double)DemoStatistics.SPAN / 256.0 / faultRate), matrix, progStats);
				final long errors = stats.run(testRuns, code, (int) (1000000.0 / faultRate), matrix, progStats);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						txtStatInfo.setText("<html>"+errors+" total pixels errors, <b>("+formatDouble(100.0 * (double)errors / 256.0 / (double)DemoStatistics.SPAN / (double)testRuns, 1)+"%)</b></html>");
						btnRun.setEnabled(true);
						btnRunStats.setEnabled(true);
						repaint();
					}
				});
			}
		}.start();
	}
	
	public static void main(String[] args) {
		SilverOceanTheme.enable();
		new DemoFaults(
			ProgramParserBytecode.compileFile(SRC_PATH, false),
			ArmInstructionMapper.mapFile(SRC_PATH)
		);
	}

}

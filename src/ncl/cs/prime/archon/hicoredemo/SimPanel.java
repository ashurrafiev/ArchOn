package ncl.cs.prime.archon.hicoredemo;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import ncl.cs.prime.archon.bytecode.CodeExecutor;
import ncl.cs.prime.archon.bytecode.CodeExecutor.ExecMode;
import ncl.cs.prime.archon.parse.ProgramParserBytecode;

public class SimPanel extends JPanel {

	public static int totalWork = 81920;
	public static boolean splitWork = true;
	public static int cacheMissPercent = 50;
	
	private JTextField txtWorkload;
	private JTextField txtMissRate;
	private JCheckBox chkSplitWork;
	
	private JButton btnStart, btnStop;
	private JTextArea info;
	private JTextArea console;
	private List<Long> results = new LinkedList<>();
	
	private Thread simThread = null;
	private long simStarted = 0L;

	public SimPanel() {
		setPreferredSize(new Dimension(350, 600));
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createEtchedBorder());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0;
		c.insets.top = 8;
		c.insets.left = 4;
		c.insets.right = 4;
		
		add(new JLabel("Total workload:"), c);
		c.gridy++;
		c.insets.top = 2;
		txtWorkload = new JTextField(Integer.toString(totalWork));
		txtWorkload.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				int v = totalWork;
				try {
					v = Integer.parseInt(txtWorkload.getText());
					if(v<1000) v = 1000;
					if(v>1000000) v = 1000000;
				}
				catch(NumberFormatException ex) {
				}
				txtWorkload.setText(Integer.toString(v));
				totalWork = v;
			}
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		add(txtWorkload, c);
		c.gridy++;

		chkSplitWork = new JCheckBox("Split workload between cores");
		chkSplitWork.setSelected(splitWork);
		chkSplitWork.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				splitWork = chkSplitWork.isSelected();
			}
		});
		add(chkSplitWork, c);
		c.gridy++;
		
		add(new JLabel("Cache miss rate (%):"), c);
		c.gridy++;
		c.insets.top = 2;
		txtMissRate = new JTextField(Integer.toString(cacheMissPercent));
		txtMissRate.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				int v = cacheMissPercent;
				try {
					v = Integer.parseInt(txtMissRate.getText());
					if(v<0) v = 0;
					if(v>100) v = 100;
				}
				catch(NumberFormatException ex) {
				}
				txtMissRate.setText(Integer.toString(v));
				cacheMissPercent = v;
			}
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		add(txtMissRate, c);
		c.insets.top = 8;
		c.gridy++;
		
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
		btnStart = new JButton("Start simulation");
		btnStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startSimulation();
			}
		});
		controls.add(btnStart);
		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(simThread!=null) {
					simThread.interrupt();
					stopSimulation();
				}
			}
		});
		btnStop.setEnabled(false);
		controls.add(btnStop);
		add(controls, c);
		
		c.gridy++;
		c.weighty = 1;
		c.insets.top = 16;
		info = new JTextArea();
		add(new JScrollPane(info,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), c);

		c.gridy++;
		c.weighty = 3;
		c.insets.top = 8;
		c.insets.bottom = 16;
		console = new JTextArea();
		console.setEditable(false);
		add(new JScrollPane(console,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), c);
	}
	
	private void updateInfo() {
		if(results.size()==0)
			return;
		double mean = 0;
		for(Long t : results)
			mean += (double) t;
		mean /= (double)results.size();
		double var = 0;
		for(Long t : results)
			var += ((double)t - mean)*((double)t - mean);
		var /= (double)results.size();
		var = Math.sqrt(var);
		
		double elapsed = (System.currentTimeMillis()-simStarted)/1000.0;
		info.setText(String.format(
				"# Simulations: %d\n\n"
				+ "Estimated platform time:\n"
				+ "    - Average: %.1f\n"
				+ "    - SqrtVar: %.1f (%.2f%%)\n"
				+ "\nElapsed: %.3fs (%.3fs per sim)",
				results.size(),
				mean, var, var*100.0/Math.abs(mean),
				elapsed, elapsed / (double) results.size()
			));
	}
	
	private class Simulator implements Runnable {
		private File f;
		public Simulator(File f) {
			this.f = f;
		}
		@Override
		public void run() {
			simStarted = System.currentTimeMillis();
			try {
				for(int i=0; simThread!=null; i++) {
					CodeExecutor exec = new CodeExecutor();
					try {
						exec.getIP().loadCode(f);
						try {
							exec.execute(null, ExecMode.normal);
							if(exec.interrupted) {
								stopSimulation();
								return;
							}
						}
						catch(Exception e) {
							console.append(String.format(" Sim #%d\tERROR: %s\n", i, e.getMessage()));
							stopSimulation();
							return;
						}
						long t = exec.getArch().syncTime();
						results.add(t);
						updateInfo();
						console.append(String.format(" Sim #%d\t%d\n", i, t));
						console.setCaretPosition(console.getDocument().getLength());
					} catch (IOException e) {
						stopSimulation();
						return;
					}
					Thread.sleep(1);
				}
			}
			catch(InterruptedException e) {
			}
			stopSimulation();
		}
	}
	
	public void startSimulation() {
		if(simThread!=null)
			return;
		console.setText("");
		info.setText("");
		results.clear();
		
		File f = new File("noc_demo.sim");
		f.deleteOnExit();	
		try {
			NocDesignPanel.builder.writeCode(f);
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(null, "Cannot write temporary files.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ProgramParserBytecode p = new ProgramParserBytecode();
		if(p.compile(f, false)==null) {
			JOptionPane.showMessageDialog(null, "Unable to start simulation due to compile error.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		f = new File(f.getAbsolutePath()+".bin");
		f.deleteOnExit();
		
		simThread = new Thread(new Simulator(f));
		simThread.start();
		btnStart.setEnabled(false);
		btnStop.setEnabled(true);
	}
	
	public void stopSimulation() {
		simThread = null;
		btnStart.setEnabled(true);
		btnStop.setEnabled(false);
	}
	
}

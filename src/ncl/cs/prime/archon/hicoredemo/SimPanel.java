package ncl.cs.prime.archon.hicoredemo;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import ncl.cs.prime.archon.arch.modules.hicore.HiEstimation;
import ncl.cs.prime.archon.bytecode.CodeExecutor;

public class SimPanel extends JPanel implements Simulator.Listener {

	private JTextField txtWorkload;
	private JTextField txtMissRate;
	private JCheckBox chkSplitWork;
	
	private JButton btnStart, btnStop;
	private JTextArea info;
	private JTextArea console;
	private List<Long> results = new LinkedList<>();
	
	private long simStarted = 0L;

	public SimPanel(final Simulator sim) {
		sim.setListener(this);
		
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
		final JComboBox<String> cmbApp = new JComboBox<>(new String[] {"Convolution filter", "Computation only", "Memory only"});
		cmbApp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Simulator.appSetup = Simulator.APP_SETUP[cmbApp.getSelectedIndex()];
			}
		});
		add(cmbApp, c);
		
		c.gridy++;
		c.insets.top = 8;
		add(new JLabel("Total workload:"), c);
		c.gridy++;
		c.insets.top = 2;
		txtWorkload = new JTextField(Integer.toString(Simulator.totalWork));
		txtWorkload.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				int v = Simulator.totalWork;
				try {
					v = Integer.parseInt(txtWorkload.getText());
					if(v<1000) v = 1000;
					if(v>1000000) v = 1000000;
				}
				catch(NumberFormatException ex) {
				}
				txtWorkload.setText(Integer.toString(v));
				Simulator.totalWork = v;
			}
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		add(txtWorkload, c);
		c.gridy++;

		chkSplitWork = new JCheckBox("Split workload between cores");
		chkSplitWork.setSelected(Simulator.splitWork);
		chkSplitWork.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Simulator.splitWork = chkSplitWork.isSelected();
			}
		});
		add(chkSplitWork, c);
		c.gridy++;
		
		add(new JLabel("Cache miss rate (%):"), c);
		c.gridy++;
		c.insets.top = 2;
		txtMissRate = new JTextField(Integer.toString(Simulator.cacheMissPercent));
		txtMissRate.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				int v = Simulator.cacheMissPercent;
				try {
					v = Integer.parseInt(txtMissRate.getText());
					if(v<0) v = 0;
					if(v>100) v = 100;
				}
				catch(NumberFormatException ex) {
				}
				txtMissRate.setText(Integer.toString(v));
				Simulator.cacheMissPercent = v;
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
				sim.startSimulation();
			}
		});
		controls.add(btnStart);
		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sim.interrupt();
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
	
	@Override
	public void simulationStarted() {
		simStarted = System.currentTimeMillis();
		console.setText("");
		console.append("#\ttime\ttotalPower\tidlePower\tmemWait\n");
		info.setText("");
		results.clear();
		btnStart.setEnabled(false);
		btnStop.setEnabled(true);
	}
	
	@Override
	public void simulationStopped() {
		btnStart.setEnabled(true);
		btnStop.setEnabled(false);
	}
	
	public void updateInfo(int simId, CodeExecutor exec) {
		HiEstimation est = (HiEstimation)exec.getEst();
		est.finish();
		long res = exec.getArch().syncTime();
		results.add(res);
		
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
		
		console.append(String.format("%d\t%d\t%.3f\t%.3f\t%.1f\n", simId, res, est.totalPower(), est.leakage, est.averageMemWait()));
		console.setCaretPosition(console.getDocument().getLength());
	}
	
	@Override
	public void error(int simId, Exception e) {
		console.append(String.format("ERROR in %d: %s\n", simId, e.getMessage()));
	}
	
}

package ncl.cs.prime.archon;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ncl.cs.prime.archon.hicoredemo.BusDesignPanel;
import ncl.cs.prime.archon.hicoredemo.BusNodeListPanel;
import ncl.cs.prime.archon.hicoredemo.NocDesignPanel;
import ncl.cs.prime.archon.hicoredemo.NocNodeListPanel;
import ncl.cs.prime.archon.hicoredemo.SimPanel;
import ncl.cs.prime.archon.hicoredemo.Simulator;

public class HiCoreDemo {

	public static void main(String[] args) {
		SilverOceanTheme.enable();

		boolean noc = false;
		for(String arg : args) {
			if("-noc".equals(arg))
				noc = true;
		}
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel cp = new JPanel(new BorderLayout());
		
		if(noc) {
			cp.add(new NocDesignPanel(), BorderLayout.CENTER);
			cp.add(new JScrollPane(new NocNodeListPanel(),
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
					BorderLayout.WEST);
			cp.add(new SimPanel(new Simulator(true) {
				protected void writeCode(java.io.File f) throws java.io.IOException {
					NocDesignPanel.builder.writeCode(f);
				};
			}), BorderLayout.EAST);
		}
		else {
			cp.add(new BusDesignPanel(), BorderLayout.CENTER);
			cp.add(new JScrollPane(new BusNodeListPanel(),
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
					BorderLayout.WEST);
			cp.add(new SimPanel(new Simulator(true) {
				protected void writeCode(java.io.File f) throws java.io.IOException {
					BusDesignPanel.builder.writeCode(f);
				};
			}), BorderLayout.EAST);
		}
		
		frame.setContentPane(cp);
		
		frame.pack();
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
	}
}

package ncl.cs.prime.archon;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ncl.cs.prime.archon.hicoredemo.NocDesignPanel;
import ncl.cs.prime.archon.hicoredemo.NodeListPanel;
import ncl.cs.prime.archon.hicoredemo.SimPanel;

public class HiCoreDemo {

	public static void main(String[] args) {
		SilverOceanTheme.enable();
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel cp = new JPanel(new BorderLayout());
		cp.add(new NocDesignPanel(), BorderLayout.CENTER);
		cp.add(new JScrollPane(new NodeListPanel(),
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
				BorderLayout.WEST);
		cp.add(new SimPanel(), BorderLayout.EAST);
		
		frame.setContentPane(cp);
		
		frame.pack();
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
	}
}

package ncl.cs.prime.archon.hicoredemo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import ncl.cs.prime.archon.hicoredemo.NocBuilder.NodeType;

public class NodeListPanel extends JPanel {
	
	public static final int WIDTH = NocDesignPanel.MESH_STEP+100;
	public static final int ITEM_HEIGHT = NocDesignPanel.MESH_STEP+30;
	
	private static final Font FONT = new Font("Verdana", Font.PLAIN, 13);
	
	private static final Color BG_COLOR = new Color(0xeeeeee);
	private static final Color SELECTION_COLOR = new Color(0xffffdddd);

	public static NodeType activeType = NodeType.coreA7cache;
	
	public NodeListPanel() {
		setPreferredSize(new Dimension(WIDTH, ITEM_HEIGHT*NodeType.values().length));
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int y = e.getY() / ITEM_HEIGHT;
				if(y>=0 && y<NodeType.values().length)
					activeType = NodeType.values()[y];
				repaint();
			}
		});
	}
	
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(BG_COLOR);
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		int cy = 0;
		for(NodeType t : NodeType.values()) {
			if(t==activeType) {
				g2.setColor(SELECTION_COLOR);
				g2.fillRect(0, cy, WIDTH, ITEM_HEIGHT);
			}
			g2.setColor(Color.BLACK);
			g2.setFont(FONT);
			g2.drawString(t.name()+": "+NocDesignPanel.builder.countNodes(t), 10, cy+15);
			NocDesignPanel.paintNode(g2, WIDTH-NocDesignPanel.MESH_STEP/2-20, cy + NocDesignPanel.MESH_STEP/2 + 20, t);
			cy += ITEM_HEIGHT;
			g2.setColor(Color.GRAY);
			g2.drawLine(0, cy-1, WIDTH, cy-1);
		}
	}
}

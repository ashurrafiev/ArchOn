package ncl.cs.prime.archon.hicoredemo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import ncl.cs.prime.archon.hicoredemo.BusBuilder.NodeType;

public class BusNodeListPanel extends JPanel {
	public static final int WIDTH = NocDesignPanel.MESH_STEP+100;
	public static final int ITEM_HEIGHT = NocDesignPanel.MESH_STEP+30;
	
	private static final Font FONT = new Font("Verdana", Font.PLAIN, 13);
	
	private static final Color BG_COLOR = new Color(0xeeeeee);
	private static final Color DISABLE_COLOR = new Color(0x55999999, true);
	private static final Color SELECTION_COLOR = new Color(0xffffdddd);

	public static NodeType activeType = null;
	
	public static BusNodeListPanel instance;
	
	public BusNodeListPanel() {
		instance = this;
		setPreferredSize(new Dimension(WIDTH, ITEM_HEIGHT*NodeType.values().length));
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int y = e.getY() / ITEM_HEIGHT;
				if(y>=0 && y<NodeType.values().length) {
					NodeType t = NodeType.values()[y];
					if(e.getButton()==MouseEvent.BUTTON1 && acceptType(t)) {
						replace(t);
					}
					repaint();
					if(e.getButton()==MouseEvent.BUTTON3 && t.setup!=null) {
						String s = JOptionPane.showInputDialog(t.name()+" characterisation parameters:", t.setup);
						if(s!=null)
							t.setup = s;
					}
				}
			}
		});
	}
	
	public void updateSelection(BusBuilder.Node node) {
		activeType = node==null ? null : node.type;
		repaint();
	}
	
	public boolean acceptType(NodeType t) {
		if(BusDesignPanel.instance!=null && BusDesignPanel.instance.selectionNode!=null)
			return BusDesignPanel.instance.selectionNode.parent.acceptChild(t);
		else
			return false;
	}
	
	public void replace(NodeType t) {
		if(BusDesignPanel.instance!=null && BusDesignPanel.instance.selectionNode!=null) {
			BusDesignPanel.instance.selectionNode = BusDesignPanel.instance.selectionNode.replace(t);
			activeType = t;
			BusDesignPanel.instance.repaint();
		}
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
			g2.drawString(t.name()/*+": "+NocDesignPanel.builder.countNodes(t)*/, 10, cy+15);
			BusDesignPanel.paintNode(g2, WIDTH-NocDesignPanel.MESH_STEP/2-20, cy + NocDesignPanel.MESH_STEP/2 + 20, 1, 5, t);
			
			if(!acceptType(t)) {
				g2.setColor(DISABLE_COLOR);
				g2.fillRect(0, cy, WIDTH, ITEM_HEIGHT);
			}
			
			cy += ITEM_HEIGHT;
			g2.setColor(Color.GRAY);
			g2.drawLine(0, cy-1, WIDTH, cy-1);
		}
	}
}

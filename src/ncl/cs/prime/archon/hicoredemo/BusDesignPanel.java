package ncl.cs.prime.archon.hicoredemo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import javax.swing.JPanel;

import ncl.cs.prime.archon.hicoredemo.BusBuilder.EmptyNode;
import ncl.cs.prime.archon.hicoredemo.BusBuilder.NodeType;

public class BusDesignPanel extends JPanel {
	
	public static final int MESH_STEP = 64;
	
	private static final Font FONT = new Font("Times New Roman", Font.BOLD, 17);
	
	private static final Color SELECTION_COLOR = new Color(0xffffdddd);
	private static final Color CURSOR_COLOR = new Color(0xffff7777);

	public static BusBuilder builder = new BusBuilder();
	private int builderWidth, builderHeight;
	
	private Point mouse = null;
	private BusBuilder.Node mouseNode = null;
	public double scale = 2;

	private Point selection = null;
	public BusBuilder.Node selectionNode = null;

	public static BusDesignPanel instance;
	
	public BusDesignPanel() {
		instance = this;
		setPreferredSize(new Dimension(800, 800));
		setFocusable(true);
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode()==KeyEvent.VK_DELETE) {
					if(BusNodeListPanel.instance!=null)
						BusNodeListPanel.instance.replace(NodeType.empty);
				}
				else if(e.getKeyCode()==KeyEvent.VK_Z && e.isControlDown()) {
					selectionNode = builder.undo();
					if(BusNodeListPanel.instance!=null) {
						BusNodeListPanel.instance.updateSelection(selectionNode);
					}
					repaint();
				}
			}
		});
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocus();
				if(mouse!=null && mouseNode!=null) {
					selection = new Point(mouse);
					selectionNode = mouseNode;
					if(BusNodeListPanel.instance!=null) {
						BusNodeListPanel.instance.updateSelection(selectionNode);
					}
					repaint();
				}
				else {
					selection = null;
					selectionNode = null;
					repaint();
				}
			}
		});
		
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				mouse = toMeshPoint(e.getPoint());
				mouseNode = null;
				repaint();
			}
		});
	}
	
	private int calcx(int x) {
		return x*MESH_STEP - builderWidth*MESH_STEP/2 + MESH_STEP/2;
	}
	
	private int calcy(int y) {
		return -y*MESH_STEP + builderHeight*MESH_STEP/2 - MESH_STEP/2;
	}
	
	public Point toMeshPoint(Point p) {
		Point m = new Point();
		m.x = (int)((p.x-getWidth()/2)/scale);
		m.y = (int)((p.y-getHeight()/2)/scale);
	
		int cx, cy;
		for(int x = 0; x<=builderWidth; x++)
			for(int y = 0; y<=builderHeight; y++) {
				cx = calcx(x);
				cy = calcy(y);
				if(m.x>=cx-MESH_STEP/2 && m.x<cx+MESH_STEP/2 &&
						m.y>=cy-MESH_STEP/2 && m.y<cy+MESH_STEP/2)
					return new Point(x, y);
			}
		return null;
	}
	
	private void paintNode(Graphics2D g2, int x, int y, BusBuilder.Node node) {
		int cx = calcx(x);
		int cy = calcy(y);
		
		int sx = x;
		for(BusBuilder.Node s : node.subs) {
			if(!(s instanceof EmptyNode)) {
				int scx = calcx(sx);
				g2.setColor(Color.GRAY);
				g2.setStroke(new BasicStroke(2f));
				g2.drawLine(scx, cy, scx, cy-MESH_STEP);
			}
			
			paintNode(g2, sx, y+1, s);
			sx += s.getWidth();
		}
		
		int w = 1;
		if(node.type==NodeType.bus) {
			w = node.getWidth();
		}
		int in = w>1 ? 10 : 5;
		
		if(selection!=null && selectionNode!=null && node==selectionNode) { //selection.x>=x && selection.x<x+w && selection.y==y) {
			g2.setColor(SELECTION_COLOR);
			g2.fillRect(cx-MESH_STEP/2, cy-MESH_STEP/2+in-5, w*MESH_STEP, MESH_STEP-in*2+10);
			g2.setStroke(new BasicStroke(1f));
		}
		
		paintNode(g2, cx, cy, w, in, node.type);
		
		if(mouse!=null && node!=builder.memory && mouse.x>=x && mouse.x<x+w && mouse.y==y) {
			g2.setStroke(new BasicStroke(3f));
			g2.setColor(CURSOR_COLOR);
			g2.drawRect(cx-MESH_STEP/2, cy-MESH_STEP/2+in-5, w*MESH_STEP, MESH_STEP-in*2+10);
			g2.setStroke(new BasicStroke(1f));
			mouseNode = node;
		}
	}
	
	@Override
	public void paint(Graphics g) {
		builderWidth = builder.memory.getWidth();
		builderHeight = builder.memory.getHeight();
		scale = Math.min(
				getWidth() / (double)((builderWidth+2)*MESH_STEP),
				getHeight() / (double)((builderHeight+2)*MESH_STEP)
			);

		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		AffineTransform t = AffineTransform.getTranslateInstance(getWidth()/2, getHeight()/2);
		t.scale(scale, scale);
		g2.setTransform(t);
		
		paintNode(g2, 0, 0, builder.memory);
	}
	
	public static int drawStringCentered(Graphics2D g2, String str, int x, int y) {
		FontMetrics fm = g2.getFontMetrics();
		int w = fm.stringWidth(str);
		int h = fm.getHeight();
		g2.drawString(str, x - w/2, y + h/2);
		return y + h;
	}
	
	public static void paintNode(Graphics2D g2, int cx, int cy, int w, int in, NodeType type) {
		if(type==NodeType.empty) {
			g2.setColor(Color.LIGHT_GRAY);
			g2.setStroke(new BasicStroke(1f));
			g2.drawLine(cx-MESH_STEP/5, cy, cx+MESH_STEP/5, cy);
			g2.drawLine(cx, cy+MESH_STEP/5, cx, cy-MESH_STEP/5);
		}
		else {
			g2.setColor(type.color);
			g2.setStroke(new BasicStroke(1f));
			g2.fillRect(cx-MESH_STEP/2+5, cy-MESH_STEP/2+in, w*MESH_STEP-10, MESH_STEP-in*2);
			g2.setColor(Color.BLACK);
			g2.drawRect(cx-MESH_STEP/2+5, cy-MESH_STEP/2+in, w*MESH_STEP-10, MESH_STEP-in*2);
			g2.setFont(FONT);
			drawStringCentered(g2, type.label, cx, cy-2);
		}

	}
}

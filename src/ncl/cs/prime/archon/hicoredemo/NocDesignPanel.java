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
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import ncl.cs.prime.archon.hicoredemo.NocBuilder.NodeType;

public class NocDesignPanel extends JPanel {

	public static final int MESH_STEP = 64;
	
	private static final Font FONT = new Font("Times New Roman", Font.BOLD, 17);
	
	private static final Color SELECTION_COLOR = new Color(0xffffdddd);
	private static final Color CURSOR_COLOR = new Color(0xffff7777);

	public static NocBuilder builder = new NocBuilder();
	
	private Point mouse = null;
	public double scale = 2;
	
	private enum Selection {
		none, col, row
	}
	
	private Selection selection = Selection.none;
	private int startSel, endSel;
	private boolean selecting = false;
		
	public NocDesignPanel() {
		setPreferredSize(new Dimension(800, 800));
		setFocusable(true);
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode()==KeyEvent.VK_SPACE) {
					if(selection==Selection.row) {
						for(int y=Math.min(startSel, endSel); y<=Math.max(startSel, endSel); y++)
							for(int x=0; x<builder.width; x++)
								builder.mesh[x][y] = NocNodeListPanel.activeType;
					}
					else if(selection==Selection.col) {
						for(int x=Math.min(startSel, endSel); x<=Math.max(startSel, endSel); x++)
							for(int y=0; y<builder.height; y++)
								builder.mesh[x][y] = NocNodeListPanel.activeType;
					}
					selection = Selection.none;
					repaint();
				}
				else if(e.getKeyCode()==KeyEvent.VK_DELETE) {
					if(selection==Selection.row) {
						builder.deleteRows(Math.min(startSel, endSel), Math.max(startSel, endSel));
					}
					else if(selection==Selection.col) {
						builder.deleteCols(Math.min(startSel, endSel), Math.max(startSel, endSel));
					}
					selection = Selection.none;
					repaint();
				}
				else if(e.getKeyCode()==KeyEvent.VK_S && e.isControlDown()) {
					File f = new File("nocbuilder.sim");
					try {
						builder.writeCode(f);
						JOptionPane.showMessageDialog(null, "Source script saved to "+f.getName());
					}
					catch(IOException ex) {
						JOptionPane.showMessageDialog(null, "Cannot write to "+f.getName(), "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
				else {
					int w = (int) Math.sqrt(builder.countCoreNodes());
					NodeType core = NocNodeListPanel.activeType.isCore ? NocNodeListPanel.activeType : NodeType.coreA7cache;
					switch(e.getKeyCode()) {
						case KeyEvent.VK_F1:
							builder = NocBuilder.createSingleMemNoc(w, w, core);
							repaint();
							break;
						case KeyEvent.VK_F2:
							builder = NocBuilder.create4MemNoc(w, w, core);
							repaint();
							break;
						case KeyEvent.VK_F3:
							builder = NocBuilder.createMemRowNoc(w, w, core);
							repaint();
							break;
						case KeyEvent.VK_F4:
							builder = NocBuilder.createMemBoxNoc(w, w, core);
							repaint();
							break;
					}
				}
			}
		});
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocus();
				mouse = toMeshPoint(e.getPoint());
				if(mouse!=null) {
					if((mouse.x==-2 || mouse.x==builder.width+1) && mouse.y>=0 && mouse.y<builder.height) {
						startSel = endSel = mouse.y;
						selection = Selection.row;
						selecting = true;
					}
					if((mouse.y==-2 || mouse.y==builder.height+1) && mouse.x>=0 && mouse.x<builder.width) {
						startSel = endSel = mouse.x;
						selection = Selection.col;
						selecting = true;
					}
					
					int resx = 0;
					int resy = 0;
					int dx = 0;
					int dy = 0;
					boolean resize = false;
					if(mouse.x==-1 && mouse.y>=-1 && mouse.y<=builder.height) {
						resx = 1;
						dx = 1;
						resize = true;
					}
					if(mouse.x==builder.width && mouse.y>=-1 && mouse.y<=builder.height) {
						resx = 1;
						resize = true;
					}
					if(mouse.y==-1 && mouse.x>=-1 && mouse.x<=builder.width) {
						resy = 1;
						dy = 1;
						resize = true;
					}
					if(mouse.y==builder.height && mouse.x>=-1 && mouse.x<=builder.width) {
						resy = 1;
						resize = true;
					}
					if(resize) {
						builder.resizeMesh(resx, resy, dx, dy);
						mouse.x += dx;
						mouse.y += dy;
					}
					editNode();
				}
				if(!selecting)
					selection = Selection.none;
				repaint();
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				selecting = false;
			}
		});
		
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				mouse = toMeshPoint(e.getPoint());
				repaint();
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				mouse = toMeshPoint(e.getPoint());
				if(selecting && mouse!=null) {
					if(selection==Selection.row && mouse.y>=0 && mouse.y<builder.height) {
						endSel = mouse.y;
					}
					if(selection==Selection.col && mouse.x>=0 && mouse.x<builder.width) {
						endSel = mouse.x;
					}
				}
				else
					editNode();
				repaint();
			}
		});
	}
	
	private void editNode() {
		if(mouse!=null && mouse.x>=0 && mouse.x<builder.width && mouse.y>=0 && mouse.y<builder.height)
			builder.mesh[mouse.x][mouse.y] = NocNodeListPanel.activeType;
	}
	
	private int calcx(int x) {
		return x*MESH_STEP - builder.width*MESH_STEP/2 + MESH_STEP/2;
	}
	
	private int calcy(int y) {
		return y*MESH_STEP - builder.height*MESH_STEP/2 + MESH_STEP/2;
	}
	
	public Point toMeshPoint(Point p) {
		Point m = new Point();
		m.x = (int)((p.x-getWidth()/2)/scale);
		m.y = (int)((p.y-getHeight()/2)/scale);
		
		// m.x = (m.x - MESH_STEP/2 + builder.width*MESH_STEP/2)/MESH_STEP;
		// m.y = (m.y - MESH_STEP/2 + builder.height*MESH_STEP/2)/MESH_STEP;
		// return m;
		
		// I hate doing coordinate conversion! I fucking hate it!!!
		// You solve a very simple linear equation and it doesn't fucking work
		// because it's a fucking integer division and you have a changing sign.
		// Yeah, I know you can "if" here and there, but fuck this!
		// I am doing brute-force.
	
		int cx, cy;
		for(int x = -2; x<=builder.width+1; x++)
			for(int y = -2; y<=builder.height+1; y++) {
				cx = calcx(x);
				cy = calcy(y);
				if(m.x>=cx-MESH_STEP/2 && m.x<cx+MESH_STEP/2 &&
						m.y>=cy-MESH_STEP/2 && m.y<cy+MESH_STEP/2)
					return new Point(x, y);
			}
		return null;
	}
	
	@Override
	public void paint(Graphics g) {
		scale = Math.min(
				getWidth() / (double)((builder.width+4)*MESH_STEP),
				getHeight() / (double)((builder.height+4)*MESH_STEP)
			);

		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		AffineTransform t = AffineTransform.getTranslateInstance(getWidth()/2, getHeight()/2);
		t.scale(scale, scale);
		g2.setTransform(t);
		
		int cx, cy;
		for(int x = 0; x<builder.width; x++) {
			cx = calcx(x);
			int cy0 = calcy(-2);
			int cy1 = calcy(builder.height+1);
			if(selection==Selection.col && x>=Math.min(startSel, endSel) && x<=Math.max(startSel, endSel)) {
				g2.setColor(SELECTION_COLOR);
				g2.fillRect(cx-MESH_STEP/2, cy0+MESH_STEP/2, MESH_STEP, cy1-cy0-MESH_STEP);
				g2.setColor(CURSOR_COLOR);
			}
			else
				g2.setColor(Color.LIGHT_GRAY);
			g2.fillPolygon(new int[] {cx-10, cx, cx+10}, new int[] {cy0, cy0+10, cy0}, 3);
			g2.fillPolygon(new int[] {cx-10, cx, cx+10}, new int[] {cy1, cy1-10, cy1}, 3);
		}
		for(int y = 0; y<builder.height; y++) {
			cy = calcy(y);
			int cx0 = calcx(-2);
			int cx1 = calcx(builder.width+1);
			if(selection==Selection.row && y>=Math.min(startSel, endSel) && y<=Math.max(startSel, endSel)) {
				g2.setColor(SELECTION_COLOR);
				g2.fillRect(cx0+MESH_STEP/2, cy-MESH_STEP/2, cx1-cx0-MESH_STEP, MESH_STEP);
				g2.setColor(CURSOR_COLOR);
			}
			else
				g2.setColor(Color.LIGHT_GRAY);
			g2.fillPolygon(new int[] {cx0, cx0+10, cx0}, new int[] {cy-10, cy, cy+10}, 3);
			g2.fillPolygon(new int[] {cx1, cx1-10, cx1}, new int[] {cy-10, cy, cy+10}, 3);
		}
		
		
		g2.setColor(Color.GRAY);
		g2.setStroke(new BasicStroke(2f));
		for(int x = 0; x<builder.width; x++)
			for(int y = 0; y<builder.height; y++) {
				if(builder.mesh[x][y]==NodeType.blocked)
					continue;
				cx = calcx(x);
				cy = calcy(y);
				if(x<builder.width-1 && builder.mesh[x+1][y]!=NodeType.blocked)
					g2.drawLine(cx, cy, cx+MESH_STEP, cy);
				if(y<builder.height-1 && builder.mesh[x][y+1]!=NodeType.blocked)
					g2.drawLine(cx, cy, cx, cy+MESH_STEP);
			}
		g2.setStroke(new BasicStroke(1f));
		for(int x = -1; x<=builder.width; x++)
			for(int y = -1; y<=builder.height; y++) {
				cx = calcx(x);
				cy = calcy(y);
				if(x>=0 && x<builder.width && y>=0 && y<builder.height)
					paintNode(g2, cx, cy, builder.mesh[x][y]);
				else {
					g2.setColor(Color.LIGHT_GRAY);
					g2.drawLine(cx-MESH_STEP/5, cy, cx+MESH_STEP/5, cy);
					g2.drawLine(cx, cy+MESH_STEP/5, cx, cy-MESH_STEP/5);
				}
				
				if(mouse!=null && mouse.x==x && mouse.y==y) {
					g2.setStroke(new BasicStroke(3f));
					g2.setColor(CURSOR_COLOR);
					g2.drawRect(cx-MESH_STEP/2, cy-MESH_STEP/2, MESH_STEP, MESH_STEP);
					g2.setStroke(new BasicStroke(1f));
				}
			}
	}
	
	public static int drawStringCentered(Graphics2D g2, String str, int x, int y) {
		FontMetrics fm = g2.getFontMetrics();
		int w = fm.stringWidth(str);
		int h = fm.getHeight();
		g2.drawString(str, x - w/2, y + h/2);
		return y + h;
	}
	
	public static void paintNode(Graphics2D g2, int cx, int cy, NodeType node) {
		switch(node) {
			case blocked:
				g2.setColor(Color.RED);
				g2.setStroke(new BasicStroke(2f));
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.drawLine(cx-MESH_STEP/5, cy-MESH_STEP/5, cx+MESH_STEP/5, cy+MESH_STEP/5);
				g2.drawLine(cx-MESH_STEP/5, cy+MESH_STEP/5, cx+MESH_STEP/5, cy-MESH_STEP/5);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				g2.setStroke(new BasicStroke(1f));
				break;
			case onlyRouter:
				g2.setColor(Color.GRAY);
				g2.fillPolygon(new int[] {cx-10, cx, cx+10, cx}, new int[] {cy, cy-10, cy, cy+10}, 4);
				break;
			default:
				g2.setColor(node.color);
				g2.setFont(FONT);
				g2.fillRect(cx-MESH_STEP/2+5, cy-MESH_STEP/2+5, MESH_STEP-10, MESH_STEP-10);
				g2.setColor(Color.BLACK);
				g2.drawRect(cx-MESH_STEP/2+5, cy-MESH_STEP/2+5, MESH_STEP-10, MESH_STEP-10);
				drawStringCentered(g2, node.label, cx, cy-2);
				break;
		}
	}
	
}

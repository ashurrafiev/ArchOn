package ncl.cs.prime.archon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import ncl.cs.prime.archon.arch.modules.arm.Mmu;

public class ZoomMemView extends JPanel {

	private Point focus = new Point(128, 384);
	
	public boolean showGolden = false;
	public boolean showDiff = false;
	
	public ZoomMemView() {
		setPreferredSize(new Dimension(512, 512));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				focus = e.getPoint();
				repaint();
			}
		});
	}
	
	private Color getColor(int x, int y) {
		if(x<0 || y<0 || x>255 || y>511)
			return getBackground();
		if((showGolden || showDiff) && DemoFaults.stats==null)
			return getBackground();
		int offs = y*256+x+65536;
		if(showDiff && DemoFaults.stats.goldenMemory[offs]!=Mmu.sharedMemory[offs])
			return Color.RED;
		int c = showGolden ? DemoFaults.stats.goldenMemory[offs] : Mmu.sharedMemory[offs];
		if(c<0) c = 0;
		if(c>255) c = 255;
		return new Color(c, c, c);
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;
		for(int y=0; y<256*2; y++)
			for(int x = 0; x<256; x++) {
				g2.setColor(getColor(x, y));
				g2.drawLine(x, y, x, y);
			}
		for(int y = 0; y<16; y++)
			for(int x = 0; x<16; x++) {
				g2.setColor(getColor(focus.x+x-8, focus.y+y-8));
				g2.fillRect(x*16+256, y*16+256, 16, 16);
			}
	}
}

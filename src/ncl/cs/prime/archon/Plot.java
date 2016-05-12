package ncl.cs.prime.archon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Plot {

	private Graphics2D g2;
	private int x0, y0, w, h, n;
	private double xmin, xmax, ymax;
	
	private interface Value {
		public double get(int i);
	}
	
	private int calcx(int i) {
		return x0 + (int)(w*(PrimeModel.model.getV(i)-xmin)/(xmax-xmin));
	}
	
	private void plot(Value value) {
		int prevx = -1;
		int prevy = -1;
		for(int i=0; i<n; i++) {
			int x = calcx(i);
			double v = value.get(i);
			int y = (int)(h*v/ymax);
			if(prevy>=0) {
				g2.drawLine(prevx, y0-prevy, x, y0-y);
			}
			prevx = x;
			prevy = y;
		}
	}
	
	public void plotModel(Graphics2D g2, int width, int height) {
		this.g2 = g2;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, width, height);
		
		x0 = 30;
		y0 = height - 30;
		w = width - 40;
		h = height - 60;
		
		n = PrimeModel.model.range();
		xmin = PrimeModel.model.minV();
		xmax = PrimeModel.model.maxV();
		ymax = PrimeModel.model.maxF();

		for(int i=0; i<n; i++) {
			g2.setColor((PrimeModel.model.getActive()==i) ? Color.BLUE : new Color(0xdddddd));
			int x = calcx(i);
			g2.drawLine(x, y0-h, x, y0);
		}
		for(int i=0; i<n; i++) {
			g2.setColor((PrimeModel.model.getActive()==i) ? Color.BLUE : Color.BLACK);
			int x = calcx(i);
			g2.drawString(Double.toString(PrimeModel.model.getV(i)), x, y0+15);
		}

		g2.setColor(Color.BLACK);
		g2.drawString(Double.toString(ymax / 1000000.0), x0, 15);

		if(ymax>0) {
			g2.setClip(x0, y0-h, w, h);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			g2.setStroke(new BasicStroke(1f));
			Color lightLightGray = new Color(0xdddddd);
			for(int ncores = 1, nmajor = 1; ncores<=PrimeModel.MAX_CORES; ncores++) {
				if(ncores==1)
					g2.setColor(Color.BLACK);
				else if(ncores==nmajor)
					g2.setColor(Color.LIGHT_GRAY);
				else
					g2.setColor(lightLightGray);
				if(ncores==nmajor)
					nmajor = nmajor << 1;
				if(ncores==PrimeModel.model.getNCores())
					continue;
				final int finalNCores = ncores;
				plot(new Value() {
					@Override
					public double get(int i) {
						return PrimeModel.model.getFn(i, finalNCores);
					}
				});
			}
			
			g2.setStroke(new BasicStroke(2f));
			g2.setColor(Color.BLUE);
			plot(new Value() {
				@Override
				public double get(int i) {
					return PrimeModel.model.getFn(i, PrimeModel.model.getNCores());
				}
			});
			
			if(PrimeModel.model.hasLim()) {
				g2.setColor(Color.RED);
				plot(new Value() {
					@Override
					public double get(int i) {
						return PrimeModel.model.getLim(i);
					}
				});
			}

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

			if(PrimeModel.model.getActive()>=0) {
				g2.setStroke(new BasicStroke(1f));
				int x = calcx(PrimeModel.model.getActive()); //x0 + w*PrimeModel.model.getActive()/n;
				double v = PrimeModel.model.getFn(PrimeModel.model.getActive(), PrimeModel.model.getNCores());
				int y = (int)(h*v/ymax);
				g2.setColor(Color.BLUE);
				g2.fillRect(x-2, y0-y-2, 4, 4);
				g2.setColor(Color.WHITE);
				g2.drawRect(x-2, y0-y-2, 4, 4);
			}

			g2.setClip(0, 0, width, height);
		}
		
		g2.setStroke(new BasicStroke(1f));
		g2.setColor(Color.BLACK);
		g2.drawLine(x0, y0, x0+w, y0);
		g2.drawLine(x0, y0-h, x0, y0);
	}

}

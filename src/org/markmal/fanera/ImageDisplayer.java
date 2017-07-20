package org.markmal.fanera;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.PNGEncodeParam;

class ImageDisplayer extends JFrame implements ActionListener {
	PrinterJob printJob;
	PageFormat pageFormat;

	BufferedImage bImage;
	int pixelsPerMeter;

	// the printable sizes, in 1/72nds of an inch
	double imageableWidth = 0, imageableHeight = 0;

	double scale = 0.10;
	int indentX = 0;
	int indentY = 0;

	int fragmentX, fragmentY, fragmentWidth, fragmentHeight;
	static final int SELECTION_MODE_PAGES = 0;
	static final int SELECTION_MODE_FRAGMENT = 1;

	int selectionMode = SELECTION_MODE_PAGES;

	ImagePanel imagePanel;

	private class PrintedPage {
		boolean isSelected = false;
		int gridX, gridY;
		int x, y, width, height;
		BufferedImage bImage;

		public PrintedPage(int gridX, int gridY, int x, int y, int width, int height) {
			this.gridX = gridX;
			this.gridY = gridY;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		int getScreenX() {
			return (int) Math.round(scale * x);
		}

		int getScreenY() {
			return (int) Math.round(scale * y);
		}

		int getScreenWidth() {
			return (int) Math.round(scale * width);
		}

		int getScreenHeight() {
			return (int) Math.round(scale * height);
		}

		boolean isHit(int screenX, int screenY) {
			int sx = (int) Math.round(screenX / scale);
			int sy = (int) Math.round(screenY / scale);
			return (((sx >= this.x) && (sx <= (this.x + this.width)))
					&& ((sy >= this.y) && (sy <= (this.y + this.height))));
		}
	}

	ArrayList<PrintedPage> printedPages = new ArrayList<PrintedPage>();

	int getSelectedPrintedPagesCount() {
		int c = 0;
		for (int p = 0; p < printedPages.size(); p++) {
			PrintedPage pg = printedPages.get(p);
				if (pg.isSelected) c++;
			}
		return c;
	}

	
	private class ImagePanel extends JPanel implements MouseListener, MouseWheelListener, MouseMotionListener {
		private static final long serialVersionUID = 1L;

		private ImagePanel() {
			setPreferredSize(new Dimension((int) Math.round(bImage.getWidth() * scale),
					(int) Math.round(bImage.getHeight() * scale)));
			this.addMouseWheelListener(this);
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
		}

		public void paint(Graphics g) {
			g.setColor(Color.black);
			int w = getSize().width;
			int h = getSize().height;
			int bw = bImage.getWidth();
			int bh = bImage.getHeight();
			int sw = (int) Math.round(scale * bw);
			int sh = (int) Math.round(scale * bh);
			int six = (int) Math.round(scale * indentX);
			int siy = (int) Math.round(scale * indentY);

			g.fillRect(0, 0, w, h);
			// g.drawImage(bImage, 0, 0, this);
			g.drawImage(bImage, six, siy, sw, sh, this);

			if (selectionMode == SELECTION_MODE_PAGES) {
				if (imageableWidth > 0)
					drawPrintGrid(g);
			}
			if (selectionMode == SELECTION_MODE_FRAGMENT) {
				drawFragment(g);
			}

		}

		private void drawFragment(Graphics g) {
			g.setColor(Color.GREEN);
			g.drawRect(
					(int) Math.round(scale * fragmentX), 
					(int) Math.round(scale * fragmentY),
					(int) Math.round(scale * fragmentWidth),
					(int) Math.round(scale * fragmentHeight)
					);
		}

		private void drawPrintGrid(Graphics g) {
			for (int p = 0; p < printedPages.size(); p++) {
				PrintedPage pg = printedPages.get(p);
				g.setColor(Color.BLUE);
				g.drawRect(pg.getScreenX(), pg.getScreenY(), pg.getScreenWidth(), pg.getScreenHeight());
			}
			for (int p = 0; p < printedPages.size(); p++) {
				PrintedPage pg = printedPages.get(p);
				g.setColor(Color.RED);
				if (pg.isSelected)
					g.drawRect(pg.getScreenX(), pg.getScreenY(), pg.getScreenWidth(), pg.getScreenHeight());
			}
		}

		private void makePageGrid() {

			// the imageableWidth, in 1/72nds of an inch
			double pgWm = imageableWidth * 0.0254 / 72;
			double pgHm = imageableHeight * 0.0254 / 72;
			int W = (int) Math.ceil(bImage.getWidth() + indentX);
			int H = (int) Math.ceil(bImage.getHeight() + indentY);
			double Wm = 1d * W / pixelsPerMeter;
			double Hm = 1d * H / pixelsPerMeter;
			int horSheetCount = (int) Math.ceil(Wm / pgWm);
			int verSheetCount = (int) Math.ceil(Hm / pgHm);

			// screen grid width/height for printed page
			int sw = (int) (Math.round(pgWm * pixelsPerMeter));
			int sh = (int) (Math.round(pgHm * pixelsPerMeter));

			printedPages.clear();

			for (int h = 0; h < horSheetCount; h++) {
				int x = (int) (Math.round(h * pgWm * pixelsPerMeter));
				for (int v = 0; v < verSheetCount; v++) {
					int y = (int) (Math.round(v * pgHm * pixelsPerMeter));
					PrintedPage pg = new PrintedPage(h, v, x, y, sw, sh);
					System.out.printf("PrintedPage: %d,%d, %d,%d\n", x, y, sw, sh);
					printedPages.add(pg);
				}
			}

		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int notches = e.getWheelRotation();
			scale += notches * 0.01;
			scaleLabel.setText(String.format("Scale: %7.5f", scale));
			System.out.printf("Scale:%f\n", scale);
			this.invalidate();
			this.repaint();

		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// toggle a page to be printed
			int x = e.getX();
			int y = e.getY();
			System.out.printf("Click: %d,%d\n", x, y);

			
			for (int p = 0; p < printedPages.size(); p++) {
				PrintedPage pg = printedPages.get(p);

				if (pg.isHit(x, y)) {
					pg.isSelected = !(pg.isSelected);
					repaint();
					System.out.printf("Hit grid: %d,%d\n", pg.gridX, pg.gridY);
				}
			}
			enableButtons();
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
		}

		int mousePressedButton, mouseStartDragX, mouseStartDragY, indentX0, indentY0, mouseStartFragmentX,
				mouseStartFragmentY;

		@Override
		public void mousePressed(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			mousePressedButton = e.getButton();
			System.out.printf("Mouse Pressed: %d,%d %d\n", x, y, mousePressedButton);

			if ((mousePressedButton == MouseEvent.BUTTON1) && (selectionMode == SELECTION_MODE_FRAGMENT)) {
				mouseStartFragmentX = x;
				mouseStartFragmentY = y;
				fragmentX = (int) Math.round(x / scale);
				fragmentY = (int) Math.round(y / scale);
			}

			if (mousePressedButton == MouseEvent.BUTTON3) {
				// start adjusting/shifting image
				mouseStartDragX = x;
				mouseStartDragY = y;
				indentX0 = indentX;
				indentY0 = indentY;
			}

		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			enableButtons();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			System.out.printf("Mouse Draggeded: %d,%d %d\n", x, y, e.getButton());

			if ((mousePressedButton == MouseEvent.BUTTON1) && (selectionMode == SELECTION_MODE_FRAGMENT)) {
				// drawing fragment rect
				fragmentWidth = (int) Math.round((x - mouseStartFragmentX) / scale);
				fragmentHeight = (int) Math.round((y - mouseStartFragmentY) / scale);
				repaint();
			}

			if (mousePressedButton == MouseEvent.BUTTON3) { // shift image
				// adjusting/shifting image
				indentX = indentX0 + (int) Math.round((x - mouseStartDragX) / scale);
				indentY = indentY0 + (int) Math.round((y - mouseStartDragY) / scale);
				repaint();
			}
			mouseXunit.setText(String.format("X: %7.5f m", e.getX() / scale / pixelsPerMeter));
			mouseYunit.setText(String.format("Y: %7.5f m", e.getY() / scale / pixelsPerMeter));
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			// System.out.printf("Mouse Moved: %d,%d %d\n",e.getX(), e.getY(),
			// e.getButton());
			mouseXunit.setText(String.format("X: %7.5f m", e.getX() / scale / pixelsPerMeter));
			mouseYunit.setText(String.format("Y: %7.5f m", e.getY() / scale / pixelsPerMeter));
		}
	}

	
	void printSelectedPages() {
		ArrayList<BufferedImage> pages = new ArrayList<BufferedImage>();
		for (int p = 0; p < printedPages.size(); p++) {
			PrintedPage pg = printedPages.get(p);

			if (pg.isSelected) {
				BufferedImage pbi = new BufferedImage(pg.width, pg.height, BufferedImage.TYPE_INT_ARGB);
				Graphics g = pbi.getGraphics();
				g.drawImage(bImage, 0, 0, pg.width - 1, pg.height - 1, pg.x - indentX, pg.y - indentY,
						pg.x - indentX + pg.width - 1, pg.y - indentY + pg.height - 1, null);
				pages.add(pbi);

				// saveToPNG(pbi,5000, "print_"+(new Integer(p).toString()));

				System.out.printf("Print: %d,%d\n", pg.gridX, pg.gridY);
			}
		}

		ImagePrinter imagePrinter = new ImagePrinter(pages, printJob, pageFormat);
		imagePrinter.print();
	}

	String savePageImageFileName = "page_";
	void saveSelectedPages() {
		int i = 1;
		for (int p = 0; p < printedPages.size(); p++) {
			PrintedPage pg = printedPages.get(p);

			if (pg.isSelected) {
				BufferedImage pbi = new BufferedImage(pg.width, pg.height, BufferedImage.TYPE_INT_ARGB);
				Graphics g = pbi.getGraphics();
				g.drawImage(bImage, 0, 0, pg.width - 1, pg.height - 1, pg.x - indentX, pg.y - indentY,
						pg.x - indentX + pg.width - 1, pg.y - indentY + pg.height - 1, null);

				System.out.printf("Save: %d,%d\n", pg.gridX, pg.gridY);
				saveToPNG(pbi, pixelsPerMeter , String.format(savePageImageFileName+"_%02d", i) );
				i++;
			}
		}
	}
	
	String saveFragmentImageFileName = "fragment";
	void saveSelectedFragment() {
				BufferedImage pbi = new BufferedImage(fragmentWidth, fragmentHeight, BufferedImage.TYPE_INT_ARGB);
				Graphics g = pbi.getGraphics();
				g.drawImage(bImage, 0, 0, 
						fragmentWidth-1, fragmentHeight-1, 
						fragmentX, fragmentY,
						fragmentX+fragmentWidth-1, fragmentY+fragmentHeight-1,
						null);

				System.out.printf("Save: %s %d,%d %d,%d\n", saveFragmentImageFileName,
						fragmentX, fragmentY,
						fragmentWidth, fragmentHeight);
				saveToPNG(pbi, pixelsPerMeter , saveFragmentImageFileName);
	}

	/*
	 * private JMenuItem printItem; private JMenuItem selectPrinterItem; private
	 * JMenuItem closeItem; private JMenuItem helpItem;
	 */

	public void actionPerformed(ActionEvent event) {
		Object target = event.getSource();

		if (target == selectPrinterButton) {
			selectPrinter();
		}

		if (target == printButton) {
			// new ImagePrinter(bImage).print();
			printSelectedPages();
		}
		if (target == selectFragmentButton) {
			selectionMode = SELECTION_MODE_FRAGMENT;
			selectFragmentButton.setEnabled(false);
			selectPagesButton.setEnabled(true);
			imagePanel.repaint();

		}
		if (target == selectPagesButton) {
			selectionMode = SELECTION_MODE_PAGES;
			selectFragmentButton.setEnabled(true);
			selectPagesButton.setEnabled(false);
			imagePanel.repaint();
		}
		if (target == this.saveAsImagesButton) {
			if (selectionMode == SELECTION_MODE_PAGES) {
				this.saveSelectedPages();
			}
			if (selectionMode == SELECTION_MODE_FRAGMENT) {
				this.saveSelectedFragment();
			}
		}

		if (target == closeButton) {
			this.removeAll();
			this.setVisible(false);
			bImage = null;
		}
		if (target == helpButton) {
			// new ImagePrinter(bImage).print();
			JOptionPane.showMessageDialog(this, "1. Select printer first.\n" + "2. adjust image to fit page(s).\n"
					+ "3. Select pages to print (they will be highlighted red).\n" + "4. Print.");
		}


	}

	void useDefaultPrinter() {
		printJob = PrinterJob.getPrinterJob();
		pageFormat = printJob.defaultPage();
		imageableWidth = pageFormat.getImageableWidth();
		imageableHeight = pageFormat.getImageableHeight();
		System.out.printf("Default imageableWidth:%f\n", imageableWidth);
		System.out.printf("Default imageableHeight:%f\n", imageableHeight);
		this.imagePanel.makePageGrid();
		printButton.setEnabled(true);
	}

	void selectPrinter() {
		printJob = PrinterJob.getPrinterJob();
		if (printJob.printDialog()) {
			pageFormat = printJob.pageDialog(printJob.defaultPage());
			imageableWidth = pageFormat.getImageableWidth();
			imageableHeight = pageFormat.getImageableHeight();
			System.out.printf("imageableWidth:%f\n", imageableWidth);
			System.out.printf("imageableHeight:%f\n", imageableHeight);
			this.imagePanel.makePageGrid();
			printButton.setEnabled(true);
			printButton.setToolTipText("This will print to the selected printer");
		}
		repaint();
	}

	/*
	 * private JMenuBar createMenuBar() {
	 * this.setTitle("Multipage Print Preview and Selection"); JMenuBar menuBar =
	 * new JMenuBar(); JMenu fileMenu = new JMenu("File"); printItem = new
	 * JMenuItem("Print..."); printItem.addActionListener(this);
	 * printItem.setEnabled(false);
	 * printItem.setToolTipText("Select printer first");
	 * 
	 * selectPrinterItem = new JMenuItem("Select Printer");
	 * selectPrinterItem.addActionListener(this); selectPrinterItem.
	 * setToolTipText("This does not actually print. Only selects a printer and page properties."
	 * );
	 * 
	 * helpItem = new JMenuItem("Help"); helpItem.addActionListener(this);
	 * 
	 * closeItem = new JMenuItem("Close"); closeItem.addActionListener(this);
	 * 
	 * fileMenu.add(selectPrinterItem); fileMenu.add(printItem); fileMenu.add(new
	 * JSeparator()); fileMenu.add(closeItem); menuBar.add(fileMenu);
	 * menuBar.add(helpItem); return menuBar; }
	 */

	ImageDisplayer(BufferedImage bImage, int pixelsPerMeter) {
		this.bImage = bImage;
		this.pixelsPerMeter = pixelsPerMeter;
		this.setTitle("Off-screen Canvas3D Snapshot");

		this.addUI();

		this.pack();
		useDefaultPrinter();
		this.setVisible(true);
	}

	protected void saveToPNG(BufferedImage bufferedImage, int pixelsPerMeter, String fileName) {
		PNGEncodeParam param = PNGEncodeParam.getDefaultEncodeParam(bufferedImage);
		param.setPhysicalDimension(pixelsPerMeter, pixelsPerMeter, 1); // set pixel size to 0.1mm
		FileOutputStream bufferedOutputStream;
		try {
			bufferedOutputStream = new FileOutputStream(fileName + ".png");
			ImageEncoder encoder = ImageCodec.createImageEncoder("PNG", bufferedOutputStream, param);
			try {
				encoder.encode(bufferedImage);
				bufferedOutputStream.flush();
				bufferedOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	protected JButton selectPrinterButton = new JButton("Select Printer");
	protected JButton selectPagesButton = new JButton("Select Pages");
	protected JButton printButton = new JButton("Print");

	protected JButton selectFragmentButton = new JButton("Select Fragment");
	protected JButton saveAsImagesButton = new JButton("Save as Images");

	protected JButton closeButton = new JButton("Close");
	protected JButton helpButton = new JButton("Help");

	protected JLabel mouseXunit = new JLabel("X:");
	protected JLabel mouseYunit = new JLabel("Y:");
	protected JLabel scaleLabel = new JLabel("Scale:");

	protected void addUI() {
		setLayout(new BorderLayout());
		JPanel topPanel = new JPanel();
		JPanel bottomPanel = new JPanel();
		add(BorderLayout.NORTH, topPanel);
		add(BorderLayout.SOUTH, bottomPanel);

		// Create and initialize menu bar
		// this.setJMenuBar(createMenuBar());

		imagePanel = new ImagePanel();
		JScrollPane scrollPane = new JScrollPane(imagePanel);
		scrollPane.getViewport().setPreferredSize(new Dimension(700, 700));
		// this.getContentPane().add(scrollPane);
		add(BorderLayout.CENTER, scrollPane);

		topPanel.add(selectPrinterButton);
		topPanel.add(printButton);
		topPanel.add(selectPagesButton);
		topPanel.add(new JSeparator());
		topPanel.add(selectFragmentButton);
		topPanel.add(saveAsImagesButton);
		topPanel.add(new JSeparator());
		topPanel.add(closeButton);
		topPanel.add(new JSeparator());
		topPanel.add(new JSeparator());
		topPanel.add(helpButton);

		selectPrinterButton.addActionListener(this);
		selectPrinterButton.setToolTipText("Select printer and page properties");

		selectPagesButton.addActionListener(this);
		selectPagesButton.setToolTipText("Select pages for printing or saving");
		selectPagesButton.setEnabled(false);

		printButton.addActionListener(this);
		printButton.setToolTipText("Print selected pages");
		printButton.setEnabled(false);

		selectFragmentButton.addActionListener(this);
		selectFragmentButton.setToolTipText("Select arbitrary fragment for saving");

		saveAsImagesButton.addActionListener(this);
		saveAsImagesButton.setToolTipText("Save selected pages or fragment as images");
		saveAsImagesButton.setEnabled(false);

		closeButton.addActionListener(this);
		closeButton.setToolTipText("Close this dialog");

		helpButton.addActionListener(this);

		mouseXunit.setPreferredSize(new Dimension(100, 20));
		mouseYunit.setPreferredSize(new Dimension(100, 20));
		scaleLabel.setPreferredSize(new Dimension(120, 20));
		bottomPanel.add(mouseXunit);
		bottomPanel.add(mouseYunit);
		bottomPanel.add(new JSeparator());
		bottomPanel.add(scaleLabel);
		scaleLabel.setText(String.format("Scale: %7.5f", scale));
		
		enableButtons();

	}

	protected void enableButtons() {
		if (selectionMode == SELECTION_MODE_PAGES) {
			boolean b = (getSelectedPrintedPagesCount() > 0);
			printButton.setEnabled(b);
			saveAsImagesButton.setEnabled(b);
		}
		if (selectionMode == SELECTION_MODE_FRAGMENT) {
			boolean b = (fragmentWidth>0) && (fragmentHeight>0);
			printButton.setEnabled(b);
			saveAsImagesButton.setEnabled(b);
		}

	}
	
}
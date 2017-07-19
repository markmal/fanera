import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
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

import javax.media.j3d.Canvas3D;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.PNGEncodeParam;

class ImagePrinter implements Printable, ImageObserver {
  
	//BufferedImage bImage;
	ArrayList<BufferedImage> pages;

    PrinterJob printJob;
    PageFormat pageFormat;
	
  public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {

    if (pi >= pages.size() ) {
      return Printable.NO_SUCH_PAGE;
    }
    BufferedImage pgBI = pages.get(pi);
    saveToPNG(pgBI,5000, "print_"+(new Integer(pi).toString()));
    
    Graphics2D g2d = (Graphics2D) g;
    //g2d.translate(pf.getImageableX(), pf.getImageableY());
    AffineTransform t2d = new AffineTransform();
    t2d.translate(pf.getImageableX(), pf.getImageableY());
    double xscale = pf.getImageableWidth() / (double) pgBI.getWidth();
    double yscale = pf.getImageableHeight() / (double) pgBI.getHeight();
    double scale = Math.min(xscale, yscale);
    t2d.scale(scale, scale);
    try {
      g2d.drawImage(pgBI, t2d, this);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Printable.NO_SUCH_PAGE;
    }
    return Printable.PAGE_EXISTS;
  }

  void print() {
    //pageFormat.setOrientation(PageFormat.LANDSCAPE);
    pageFormat = printJob.validatePage(pageFormat);
    printJob.setPrintable(this, pageFormat);
    //if (printJob.printDialog()) {
      try {
        printJob.print();
      } catch (PrinterException ex) {
        ex.printStackTrace();
      }
    //}
  }

  public boolean imageUpdate(Image img, int infoflags, int x, int y,
      int width, int height) {
    return false;
  }

  public float[] getImageableDimension() {
	    PrinterJob printJob = PrinterJob.getPrinterJob();
	    PageFormat pageFormat = printJob.defaultPage();
	    return new float[]{
	    		(float)pageFormat.getImageableWidth(), 
	    		(float)pageFormat.getImageableHeight()
	    		};
  }
  
  ImagePrinter(BufferedImage bImage) {
	 if (pages == null)
	   pages = new ArrayList<BufferedImage>();
	 pages.add(bImage);
  }

public ImagePrinter(ArrayList<BufferedImage> pages,
		PrinterJob printJob, PageFormat pageFormat) {
	this.pages = pages;
	this.printJob = printJob;
	this.pageFormat = pageFormat;
}

protected void saveToPNG(BufferedImage bufferedImage, int pixelsPerMeter, String fileName){
	PNGEncodeParam  param = PNGEncodeParam.getDefaultEncodeParam(bufferedImage);
	param.setPhysicalDimension(pixelsPerMeter, pixelsPerMeter, 1); //set pixel size to 0.1mm 
	FileOutputStream bufferedOutputStream;
	try {
		bufferedOutputStream = new FileOutputStream(fileName+".png");
    	ImageEncoder encoder= ImageCodec.createImageEncoder("PNG", 
    			bufferedOutputStream, param); 
    	
    	try {
			encoder.encode(bufferedImage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}


}
package org.markmal.fanera;

/**
 * This class captures offscreen 3D canvas to 2D image for further printing / saving.
 * 
 * @license GNU LGPL (LGPL.txt):
 * 
 * @author Mark Malakanov
 * @version 1.2.2.10
 * 
 **/

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;

class OffScreenCanvas3D extends Canvas3D {
  OffScreenCanvas3D(GraphicsConfiguration graphicsConfiguration,
      boolean offScreen) {
    super(graphicsConfiguration, offScreen);
  }

  BufferedImage doRender(int width, int height) {
	//double PhysicalHeight = this.getPhysicalHeight();
	//double PhysicalWidth = this.getPhysicalWidth();
    BufferedImage bImage = new BufferedImage(width, height,
        BufferedImage.TYPE_INT_ARGB);
    ImageComponent2D buffer = new ImageComponent2D(
        ImageComponent.FORMAT_RGBA,
        bImage);

    setBackground(Color.WHITE);
    setOffScreenBuffer(buffer);
    renderOffScreenBuffer();
    waitForOffScreenRendering();
    bImage = getOffScreenBuffer().getImage();
    return bImage;
  }

  public void postSwap() {
    // No-op since we always wait for off-screen rendering to complete
  }
}

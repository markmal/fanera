package org.markmal.fanera;

/**
 * This program loads an STL file (for example a model of wing exported from XFLR5) 
 * and fills it with plywood texture.
 * So it looks like the model is carved out of plywood.
 * Then it can be printed in natural sizes.
 * 
 * @license GNU LGPL (LGPL.txt):
 * 
 * @author Mark Malakanov
 * 
 **/

import com.sun.j3d.loaders.Scene;
import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate; 
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom; 
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent; 
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;

import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.PNGEncodeParam;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.ImageComponent3D;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture3D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.BranchGroup;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent; 
import java.awt.event.MouseWheelListener; 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere; 
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileFilter;
import javax.media.j3d.GraphicsContext3D;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4d;
import javax.vecmath.Vector4f;

import org.j3d.loaders.stl.STLFileReader;
import org.j3d.ui.CapturedImageObserver;
import org.markmal.fanera.GitHubReleaseChecker.GitHubRelease;

import javax.imageio.*;

public class Fanera extends Frame 
	implements WindowListener, ActionListener,
		MouseListener, MouseMotionListener, MouseWheelListener, 
		CapturedImageObserver, MouseBehaviorCallback 
{	    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static final String RELEASE="1.3.2.18";
	
    BranchGroup sceneGroup; 
    BranchGroup objRoot;
    View view;
    Canvas3D canvas3D;
    OffScreenCanvas3D offScreenCanvas3D;
    TransformGroup transformGroup ; 
	SimpleUniverse simpleU;
    
    MouseWheelZoom myMouseZoom;
    MouseRotate rotate;
    MouseTranslate translate; 

    //TODO - add as program params
	/* Russian Birch plywood 8 plies, 12mm, glue is 1/7 of wood */
    float plyThickness = 0.0014f; // thickness of one ply in meters
    int glueRatio = 7; // how many times glue thickness is less than ply thickness. Here 1/7;
    
	int texSz_r = 100; // texture size 
	int texSz_t = 100; // texture size
	int texSz_s = 100; // texture size

    float texelToReal_r = 5000f/texSz_r; // every 10mm
    //float texelToReal_t = 5000f/texSz_s; // every 10mm
    //float texelToReal_s = 5000f/texSz_s; // every 10mm
    float texelToReal_t = 100f; // every 10mm
    float texelToReal_s = 100f; // every 10mm
    
	//TODO - add as program params
	boolean sceneAntialiasingEnable = true;
	boolean lightsEnable = true;
	Color3f backgroundColor3f = new Color3f(0.75f,0.75f,0.75f); //gray
	
	private int ColorARGB8(int A, int R, int G, int B){
		return ( A << 24) | (R << 16) | (G << 8) | B ;
	}
	
    public Appearance createTextureAppearancePlywood(){    
	    Appearance ap = new Appearance();
	    
	    int N=100; // number of plys in texture
	    texSz_r = (int)(glueRatio*N); //number of texels in texture by R-axis
	    texelToReal_r = 1f/(plyThickness*N)  ; // real size of texture block by R-axis 
	    
	    BufferedImage[] img = new BufferedImage[texSz_r];
	    
	    int wood = ColorARGB8(0,191,191,75);
	    int glue = ColorARGB8(0,63,63,15);
	    int grid = ColorARGB8(0,181,100,0);
	    
	    BufferedImage lyr_wood = new BufferedImage(texSz_s,texSz_t, BufferedImage.TYPE_INT_ARGB);
	    BufferedImage lyr_glue = new BufferedImage(texSz_s,texSz_t, BufferedImage.TYPE_INT_ARGB);

		  for (int t = 0; t < texSz_t; t++) {
		    for (int s = 0; s < texSz_s; s++) {
		      lyr_wood.setRGB(s, t, wood);  
		      lyr_glue.setRGB(s, t, glue);  
			  
		      if ( ((t % 100) == 0) || ((s % 100) == 0)
		    	  || ((t % 100) == 1) || ((s % 100) == 1)) 	  
				  lyr_wood.setRGB(s, t, grid); 
		    }
		  }

		  for (int r = 0; r < texSz_r; r++) {
			if ( (r % glueRatio) == 0 )  
			  img[r] = lyr_glue;
			else 
			  img[r] = lyr_wood;
		  }
		
	    ImageComponent3D image = new ImageComponent3D(ImageComponent3D.FORMAT_RGBA, img);
	    Texture3D texture = new Texture3D(Texture.BASE_LEVEL, Texture.RGBA,
	    		image.getWidth(), image.getHeight(), image.getDepth());
	    texture.setImage(0, image);
	    texture.setEnable(true);
	    //texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
	    texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);

	    TextureAttributes texAtt = new TextureAttributes();
	    texAtt.setTextureMode(TextureAttributes.MODULATE);
	    //texAtt.setTextureMode(TextureAttributes.DECAL );
	    //ap.setTextureAttributes(texAtt);

	    TransparencyAttributes ta = new TransparencyAttributes();
	    ta.setTransparencyMode( TransparencyAttributes.NICEST );
	    ta.setTransparency(0.5f);
	    //ap.setTransparencyAttributes(ta);
	    
	    ap.setTexture(texture);
	    
        // show both sides of polygons because STL does not tell which one faces outside
        //PolygonAttributes polAttr = new PolygonAttributes(); 
        //polAttr.setPolygonMode(PolygonAttributes.POLYGON_FILL);
        //polAttr.setPolygonMode(PolygonAttributes.POLYGON_LINE);
        //polAttr.setCullFace(PolygonAttributes.CULL_NONE);
        //appearance.setPolygonAttributes(polAttr);
        
        TexCoordGeneration tcGen = new TexCoordGeneration(
        		TexCoordGeneration.OBJECT_LINEAR,
        		TexCoordGeneration.TEXTURE_COORDINATE_3 );
        
        // set v-ratio to bring texel size to real ply thickness
        //float v = glueRatio * texelSize / plyThickness;

        tcGen.setPlaneS(new Vector4f(texelToReal_s, 0, 0, 0));
        tcGen.setPlaneT(new Vector4f(0, texelToReal_t, 0, 0));
        tcGen.setPlaneR(new Vector4f(0, 0, texelToReal_r, 0));
        ap.setTexCoordGeneration(tcGen);
        
		if (lightsEnable)
			ap.setMaterial(buildMaterial());

	    
	    return ap;
	  }
     
    
    public Fanera(String filename) 
    { 
        super("Fanera "+RELEASE); 
        this.addWindowListener(this); 
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        
        setLayout(new BorderLayout()); 
        
        canvas3D = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        canvas3D.setBackground(Color.WHITE);
        add(canvas3D); 
        
        // create Print Canvas
        int pgWidth = 5000;  
        int pgHeight = 5000;
        offScreenCanvas3D = new OffScreenCanvas3D(SimpleUniverse.getPreferredConfiguration(), true);
        offScreenCanvas3D.setSize(new Dimension(pgWidth, pgHeight));
        offScreenCanvas3D.getScreen3D().setSize(new Dimension(pgWidth, pgHeight));
        offScreenCanvas3D.getScreen3D().setPhysicalScreenWidth(1); // in meters
        offScreenCanvas3D.getScreen3D().setPhysicalScreenHeight(1); // in meters
        
	    simpleU = new SimpleUniverse(canvas3D);
	    simpleU.getViewer().getView().addCanvas3D(offScreenCanvas3D);
	    
	    canvas3D.addMouseWheelListener(this);
	    objRoot = new BranchGroup();         
	    
	    addBackground(backgroundColor3f);
	    
		if (lightsEnable)
		  addLights(objRoot);
	
	    transformGroup = new TransformGroup();
	    transformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
	    transformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);

	    objRoot.addChild(transformGroup);         
		objRoot.setCapability(BranchGroup.ALLOW_DETACH);
	
	    setMouseControls();
	    
	    simpleU.getViewingPlatform().setNominalViewingTransform();
	    
	    view = simpleU.getViewer().getView();
	    view.setSceneAntialiasingEnable(sceneAntialiasingEnable);
	    view.setBackClipDistance(1000);
	    view.setFrontClipDistance(0);
	    view.setProjectionPolicy(View.PARALLEL_PROJECTION);
		view.setScreenScalePolicy(View.SCALE_EXPLICIT);
		//view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
		view.setWindowResizePolicy(View.PHYSICAL_WORLD);
		view.setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);

        rotate.setFactor(0.005);
        translate.setFactor(0.001);

		simpleU.addBranchGraph(objRoot); 

	    //canvas3D.getScreen3D().setPhysicalScreenHeight(1);
	    //canvas3D.getScreen3D().setPhysicalScreenWidth(2);

        if (filename != null)
        	loadSTL(filename);
		
	    addUI();
	    
	    canvas3D.repaint(); 
	}	
    
    Scene scene;
    Appearance appearance;
    
    public boolean loadSTL(String filename){ 
	    try{ 
	        File file = new File(filename); 
	        final STLFileReader reader = new STLFileReader(file); 
	        STLLoader loader = new STLLoader(); 
	        
	        if (sceneGroup != null) {
	        	sceneGroup.detach();
	    	    //transformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
	        	transformGroup.removeChild(sceneGroup);
	        }

	        scene = loader.createScene(reader);
	        sceneGroup = scene.getSceneGroup();
	        sceneGroup.setCapability(BranchGroup.ALLOW_DETACH);
	        
	        if (appearance == null)
	        	appearance = createTextureAppearancePlywood();
	        
	        Shape3D shape = (Shape3D) sceneGroup.getChild(0);
	        shape.setCapability(Shape3D.ALLOW_APPEARANCE_OVERRIDE_WRITE);
	        shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
	        shape.setAppearance(appearance);

	        transformGroup.addChild(sceneGroup);
	        this.setTitle("Fanera "+RELEASE+" file:"+filename);
	        return true;
	    }catch (IOException ex){ 
	        System.out.println(ex); 
	        return false;
	    } 
    }

    protected void addLights(BranchGroup b) {
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0),
            100.0);
        Color3f ambLightColour = new Color3f(0.5f, 0.5f, 0.5f);
        AmbientLight ambLight = new AmbientLight(ambLightColour);
        ambLight.setInfluencingBounds(bounds);
        Color3f dirLightColour = new Color3f(0.75f, 0.75f, 0.75f);
        Vector3f dirLightDir = new Vector3f(-1.0f, -1.0f, -1.0f);
        DirectionalLight dirLight = new DirectionalLight(dirLightColour,
            dirLightDir);
        dirLight.setInfluencingBounds(bounds);
        b.addChild(ambLight);
        b.addChild(dirLight);
      }

    Background background;
    protected void addBackground(Color3f bgColor3f) {
	    background = new Background(bgColor3f);
	    background.setCapability(Background.ALLOW_COLOR_WRITE);
	    BoundingSphere sphere = new BoundingSphere(new Point3d(0,0,0), 100.0);
	    background.setApplicationBounds(sphere);
	    objRoot.addChild(background);
    }

    protected void changeBackground(Color3f bgColor3f) {
	    background.setColor(bgColor3f);
    }
    
    protected Material buildMaterial() {
        Color3f ambientColour = new Color3f(1.0f, 0.0f, 0.0f);
        Color3f emissiveColour = new Color3f(0.0f, 0.0f, 0.0f);
        Color3f specularColour = new Color3f(0.75f, 0.75f, 0.75f);
        Color3f diffuseColour = new Color3f(1.0f, 0.0f, 0.0f);
        float shininess = 10.0f;
        return (new Material(ambientColour, emissiveColour,
            diffuseColour, specularColour, shininess));
    }
    
    protected void setMouseControls() {
	    transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); // -----------(1) 
	    transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ); //--------------(2) 
	    
	    myMouseZoom = new MouseWheelZoom(); 
	    myMouseZoom.setTransformGroup(transformGroup);  // ---------------(7) 
	    myMouseZoom.setSchedulingBounds(new BoundingSphere());  // ---------------(8) 
	    myMouseZoom.setFactor(0.01);
	    myMouseZoom.setupCallback(this);
	
	    rotate = new MouseRotate(transformGroup); 
	    rotate.setSchedulingBounds(new BoundingSphere());
	    rotate.setFactor(0.01);
	    
	    translate = new MouseTranslate(transformGroup); 
	    translate.setSchedulingBounds(new BoundingSphere()); 
	    translate.setFactor(0.01);

	    objRoot.addChild(myMouseZoom); 
	    objRoot.addChild(rotate); 
	    objRoot.addChild(translate);
	}    

    
    protected JButton openButton = new JButton("Open");
    protected JButton plywoodButton = new JButton("Plywood");
    protected JButton exitButton = new JButton("Exit");

    /*
    protected JButton leftButton = new JButton("<-");
    protected JButton rightButton = new JButton("->");
    protected JButton upButton = new JButton("^");
    protected JButton downButton = new JButton("v");
	*/
    
    protected JButton frontButton = new JButton("Front");
    protected JButton sideButton = new JButton("Side");
    protected JButton backButton = new JButton("Back");
    
    protected JButton printPreviewButton = new JButton("Print Preview");
    protected JButton toggleProjectionButton = new JButton("Projection");
    
    public void rotateLeft(){
        //Create a temporary transform
        Transform3D temp = new Transform3D();
        //Read the transform from the shape
        transformGroup.getTransform(temp);
        //Create a rotation that will be applied
        Transform3D tempDelta = new Transform3D();
        tempDelta.rotY(-0.3);
        //Apply the rotation
        temp.mul(tempDelta);
        //Write the value back into the scene graph
        transformGroup.setTransform(temp);
    }
    
    public void rotateRight(){
        //Do the same for the right rotation
        Transform3D temp = new Transform3D();
        transformGroup.getTransform(temp);
        Transform3D tempDelta = new Transform3D();
        tempDelta.rotY(0.3);
        temp.mul(tempDelta);
        transformGroup.setTransform(temp);
    }

    public void rotateUp(){
        Transform3D temp = new Transform3D();
        transformGroup.getTransform(temp);
        Transform3D tempDelta = new Transform3D();
        tempDelta.rotX(-0.3);
        temp.mul(tempDelta);
        transformGroup.setTransform(temp);
    }
    
    public void rotateDown(){
        Transform3D temp = new Transform3D();
        transformGroup.getTransform(temp);
        Transform3D tempDelta = new Transform3D();
        tempDelta.rotX(0.3);
        temp.mul(tempDelta);
        transformGroup.setTransform(temp);
    }

    public void rotateFront(){
        Transform3D temp = new Transform3D();
        transformGroup.getTransform(temp);
        AxisAngle4f a = new AxisAngle4f();
        a.angle = (float)Math.toRadians(0);
        a.x = 1f;
        a.y = 1f;
        a.z = 1f;
        temp.setRotation(a);
        transformGroup.setTransform(temp);
    }

    public void rotateBack(){
        Transform3D temp = new Transform3D();
        transformGroup.getTransform(temp);
        AxisAngle4f a = new AxisAngle4f();
        a.angle = (float)Math.toRadians(180);
        a.x = 1f;
        a.y = 0f;
        a.z = 0f;
        temp.setRotation(a);
        transformGroup.setTransform(temp);
    }

    public void rotateSide(){
        Transform3D temp = new Transform3D();
        transformGroup.getTransform(temp);
        AxisAngle4f a = new AxisAngle4f();
        a.angle = (float)Math.toRadians(90);
        a.x = -1f;
        a.y = 0f;
        a.z = 0f;
        temp.setRotation(a);
        transformGroup.setTransform(temp);
    }

    public void toggleProjection(){
    	if (view.getProjectionPolicy() == View.PERSPECTIVE_PROJECTION){
    		view.setProjectionPolicy(View.PARALLEL_PROJECTION);
    		view.setScreenScalePolicy(View.SCALE_EXPLICIT);
    		//System.out.printf("switched to PARALLEL_PROJECTION\n");
    		this.printPreviewButton.setEnabled(true);
    		printPreviewButton.setToolTipText("Open Print Multipage Preview and Selection");
    	    toggleProjectionButton.setToolTipText("Toggle to Perspective projection");
    		projectionLabel.setText("Projection: Parallel");
    	}
    	else {
    		view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
    		view.setScreenScalePolicy(View.SCALE_SCREEN_SIZE);
   		 	//System.out.printf("switched to PERSPECTIVE_PROJECTION\n");
   		 	printPreviewButton.setEnabled(false);
   		 	printPreviewButton.setToolTipText("Change projection to Parallel to enable printing");
   		 	toggleProjectionButton.setToolTipText("Toggle to Parallel projection");
    		projectionLabel.setText("Projection: Perspective");
    	}
    	
    }
    
    void onPlywoodButton(){
    	PlywoodDialog cd = new PlywoodDialog(this,"aWord",this);
    	cd.setPlyThickness(this.plyThickness * 1000f); // m to mm
    	cd.setGlueRatio(this.glueRatio);
    	cd.pack();
    	cd.setVisible(true);
    	cd.setModal(true);

		if (cd.result) {
		    plyThickness = cd.getPlyThickness() * 0.001f; // from mm to m
		    glueRatio = cd.getGlueRatio();
		    
		    appearance = createTextureAppearancePlywood();
		    if (sceneGroup != null) {
		    	Shape3D shape = (Shape3D) sceneGroup.getChild(0);
		    	shape.setAppearance(appearance);
		    }
	
		}
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == exitButton) {
          dispose();
          System.exit(0);
        } else if (e.getSource() == openButton) {
        	openFile();
        } else if (e.getSource() == plywoodButton) {
        	onPlywoodButton();
        } else if (e.getSource() == frontButton) {
			rotateFront();
		} else if (e.getSource() == backButton) {
			rotateBack();
		} else if (e.getSource() == sideButton) {
			rotateSide();
		} else if (e.getSource() == printPreviewButton) {
			this.printImage();
		} else if (e.getSource() == toggleProjectionButton) {
			toggleProjection();
		}
        
	}
    @Override 
    public void windowActivated(WindowEvent e){} 
    @Override 
    public void windowClosed(WindowEvent e){} 
    @Override 
    public void windowDeactivated(WindowEvent e){} 
    @Override 
    public void windowDeiconified(WindowEvent e){} 
    @Override 
    public void windowIconified(WindowEvent e){} 
    @Override 
    public void windowOpened(WindowEvent e){} 
    @Override 
    public void windowClosing(WindowEvent e){ 
            System.exit(1); 
    } 

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}
	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}


	@Override
	public void mousePressed(MouseEvent mouseEvent) {
		// TODO Auto-generated method stub
		Point slp = mouseEvent.getLocationOnScreen();
		Component comp = canvas3D.getComponentAt(slp);
		System.out.printf("Comp name:%s\n", comp.getName());
	}


	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseMoved(MouseEvent me) {
    	/*System.out.printf("mouseMoved:%d, %d\n", 
    			me.getPoint().x, me.getPoint().y );*/
	}
	
	double parallelProjectionScale = 1;
	double perspectiveProjectionScale = 1;
    double transformZoomFactor = 0; 

    @Override 
    public void mouseWheelMoved(MouseWheelEvent e){ 
    	double s = view.getScreenScale();
    	//System.out.printf("Scale:%f \n", s );
		//scaleLabel.setText(String.format("Persp Scale: %7.5f", s));
    	if (view.getProjectionPolicy() == View.PARALLEL_PROJECTION){
    	 if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL ){
    		int notches = e.getWheelRotation();
    		parallelProjectionScale = view.getScreenScale();
    		parallelProjectionScale = parallelProjectionScale + 0.01*notches; 
    		view.setScreenScale(parallelProjectionScale);
   		 	//System.out.printf("Scale PARALLEL_PROJECTION: %f\n",parallelProjectionScale);
    		//scaleLabel.setText(String.format("Par Scale: %7.5f", parallelProjectionScale));
    	 }
    	}
   	}

    
	@Override
	public void transformChanged(int type, Transform3D transform) {
		// TODO Auto-generated method stub
		if ( type == MouseBehaviorCallback.ZOOM ) {

			//System.out.printf("RF=%f TF=%f\n",rotate.getXFactor(),translate.getXFactor());
			
			
			Vector3d sv = new Vector3d();
			transform.getScale(sv);
			Matrix4d m4d = new Matrix4d(); 
			transform.get(m4d);
			transformZoomFactor = m4d.m23; // Mouse Zoom Factor is here
			perspectiveProjectionScale = 1 + m4d.m23;  
	        double rotationFactor    = 0.005 ;//* (1-transformZoomFactor);  
	        double translationFactor = 0.001 ;//* (1-transformZoomFactor);  
			
	        if (view.getProjectionPolicy() == View.PARALLEL_PROJECTION){
				double tzf = parallelProjectionScale - 1f;
				rotationFactor    = 0.005 - (0.005*tzf);
				translationFactor = 0.001 - (0.001*tzf);
				//System.out.printf("Paral Zoom Factor:%f \n", tzf);
	    		scaleLabel.setText(String.format("Par Scale: %f", parallelProjectionScale));
			}else {
				//System.out.printf("Persp Zoom Factor:%f \n", transformZoomFactor);
	    		scaleLabel.setText(String.format("Per Scale: %f", perspectiveProjectionScale));
			}
	        rotate.setFactor(rotationFactor);
	        translate.setFactor(translationFactor);
		}
	}

    
    public void printImage(){
    	Point loc = canvas3D.getLocationOnScreen();
        offScreenCanvas3D.setOffScreenLocation(loc);
        Dimension dim = offScreenCanvas3D.getSize();
        
		double ss = view.getScreenScale();
		int pixelsPerMeter = (int)Math.round( dim.width * ss);
		System.out.printf("ScreenScale:%f pixelsPerMeter:%d\n",ss,pixelsPerMeter);

		
		changeBackground(new Color3f(1f,1f,1f)); //white
		
		BufferedImage bImage = offScreenCanvas3D.doRender(dim.width, dim.height);

        new ImageDisplayer(bImage, pixelsPerMeter);
        
        changeBackground(backgroundColor3f);
        
    }
	
    protected void saveToPNG(BufferedImage bufferedImage, int pixelsPerMeter){
    	PNGEncodeParam  param = PNGEncodeParam.getDefaultEncodeParam(bufferedImage);
    	param.setPhysicalDimension(pixelsPerMeter, pixelsPerMeter, 1); //set pixel size to 0.1mm 
    	FileOutputStream bufferedOutputStream;
		try {
			bufferedOutputStream = new FileOutputStream("out.png");
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
    
    
    protected void addUI(){
        setLayout(new BorderLayout());
        JPanel bottomPanel = new JPanel();
        JPanel topPanel = new JPanel();
        topPanel.add(openButton);
        topPanel.add(plywoodButton);
        /*topPanel.add(downButton);
        topPanel.add(leftButton);
        topPanel.add(rightButton);
        topPanel.add(upButton);
        topPanel.add(downButton);
        */
        topPanel.add(frontButton);
        topPanel.add(backButton);
        topPanel.add(sideButton);
        topPanel.add(printPreviewButton);
        topPanel.add(toggleProjectionButton);
        
        topPanel.add(exitButton);
        
        add(BorderLayout.NORTH, topPanel);
        add(BorderLayout.CENTER, canvas3D);
        
        openButton.addActionListener(this);
        openButton.setToolTipText("Open STL file");
        plywoodButton.addActionListener(this);
        plywoodButton.setToolTipText("Define Plywood");
        exitButton.addActionListener(this);
        /*leftButton.addActionListener(this);
        rightButton.addActionListener(this);
        upButton.addActionListener(this);
        downButton.addActionListener(this);*/
        frontButton.addActionListener(this);
        backButton.addActionListener(this);
        sideButton.addActionListener(this);
        printPreviewButton.addActionListener(this);
        toggleProjectionButton.addActionListener(this);
        toggleProjectionButton.setToolTipText("Toggle to Perspective projection");

		add(BorderLayout.SOUTH, bottomPanel);
		projectionLabel.setPreferredSize(new Dimension(200, 20));
		scaleLabel.setPreferredSize(new Dimension(200, 20));
		bottomPanel.add(projectionLabel);
		bottomPanel.add(new JSeparator());
		bottomPanel.add(scaleLabel);

		projectionLabel.setText("Projection: "
				+((view.getProjectionPolicy()==View.PARALLEL_PROJECTION)?"Parallel":"Perspective"));
		scaleLabel.setText(String.format("Scale: %7.5f", 1.0));

    }
    JLabel scaleLabel = new JLabel(); 
    JLabel projectionLabel = new JLabel(); 


	@Override
	public void canvasImageCaptured(BufferedImage arg0) {
		// TODO Auto-generated method stub
		
	}

	public void openFile(){
		FileDialog openFileDialog = new FileDialog(
				new Frame(), "Open", FileDialog.LOAD);
		openFileDialog.setFilenameFilter(
				(File dir, String name) -> name.endsWith(".stl"));
		//openFileDialog.setFile("*.stl");
		openFileDialog.setDirectory( System.getProperty("user.dir") );
		openFileDialog.setVisible(true);
		String fileName = openFileDialog.getFile();
		loadSTL(fileName);
	}
	
	public static void checkNewRelease() {
		String ghrUrl = "https://api.github.com/repos/markmal/fanera/releases";
		GitHubReleaseChecker ghrc = new GitHubReleaseChecker(ghrUrl);
		if (ghrc.releasesCollection == null) return;
		GitHubRelease ghr = ghrc.findLatestRelease();
		
		if (ghrc.compareReleaseStrings(RELEASE, ghr.tag_name) == 1) {
			System.out.printf("New Release html_url:%s\n",ghr.html_url);
			int n = JOptionPane.showConfirmDialog(null,
				    "New version "+ghr.tag_name+" has been released.\n"
				    +"Do you want to open it in browser for download?\n"+
				    ghr.html_url,
				    "New version",
				    JOptionPane.YES_NO_OPTION);
			if(n == 0) {
				System.out.printf("yes \n");
				if(Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
				{
				  try {
					Desktop.getDesktop().browse(new URI(ghr.html_url));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}
			}
		}
	}
	
	public static void main(String args[]) 
    {
			checkNewRelease();
		
            Fanera myApp=new Fanera("MainWing_H105_D07.stl"); 
            //myApp.setLocationRelativeTo(null); 
            myApp.setLocation(800,10); 
            myApp.setSize(1004,1024); 
            myApp.setVisible(true); 
    }
      
}
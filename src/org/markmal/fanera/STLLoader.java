package org.markmal.fanera;

/*****************************************************************************

 * STLLoader.java
 * Java Source
 *
 * This source is licensed under the GNU LGPL v2.1.
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information.
 *
 * Copyright (c) 2001, 2002 Dipl. Ing. P. Szawlowski
 * University of Vienna, Dept. of Medical Computer Sciences
 ****************************************************************************/

//package org.j3d.renderer.java3d.loaders;

// Local imports
import java.io.*;

import javax.media.j3d.*;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.TexCoord3f;
import javax.vecmath.Vector3f;

import java.net.URL;
import java.net.MalformedURLException;
import java.awt.Component;

import com.sun.j3d.loaders.*;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;

// Application specific imports
import org.j3d.loaders.stl.*;

/**
 * Class to load objects from a STL (Stereolithography) file into Java3D.<p>
 * In case that the file uses the binary STL format, no check can be done to
 * assure that the file is in STL format. A wrong format will only be
 * recognized if an invalid amount of data is contained in the file.<p>
 * @author  Dipl. Ing. Paul Szawlowski -
 *          University of Vienna, Dept. of Medical Computer Sciences
 * 
 * @author mark
 * @version $Revision: 1.3 $
 * Set actual normals of triangles using original STL normals just for a general direction.
 * Because XFLR5 exports to STL with all normals to North/South/West/East
 */
public class STLLoader extends LoaderBase
{
    private final Component itsParentComponent;
    private boolean         itsShowProgress = false;

    /**
     * Creates a STLLoader object.
     */
    public STLLoader( )
    {
        itsParentComponent = null;
    }

    /**
     * Creates a STLLoader object which shows the progress of reading. Effects
     * only {@link #load( URL )} and {@link #load( String )}.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *      Use <code>null</code> if there is no parent.
     */
    public STLLoader( final Component parentComponent )
    {
        itsParentComponent = parentComponent;
        itsShowProgress = true;
    }

    /**
     * Loads a STL file from a file. The data may be in ASCII or binary
     * format.<p>
     * The <code>getNamedObjects</code> method of the <code>Scene</code> object
     * will return <code>Shape3D</code> objects with no <code>Appearance</code>
     * set.
     * @return <code>Scene object</code> of the content of <code>fileName</code>
     *      or <code>null</code> if user cancelled loading (only possible if
     *      progress monitoring is enabled).
     */
    public Scene load( String fileName ) throws FileNotFoundException,
    IncorrectFormatException, ParsingErrorException
    {
        try
        {
            return load( new URL( fileName ) );
        }
        catch( MalformedURLException e )
        {
            throw new FileNotFoundException( );
        }
    }

    /**
     * Loads a STL file from an URL. The data may be in ASCII or binary
     * format.<p>
     * The <code>getNamedObjects</code> method of the <code>Scene</code> object
     * will return <code>Shape3D</code> objects with no <code>Appearance</code>
     * set.
     * @return <code>Scene object</code> of the content of <code>url</code> or
     *      <code>null</code> if user cancelled loading (only possible if
     *      progress monitoring is enabled).
     */
    public Scene load( URL url ) throws FileNotFoundException,
    IncorrectFormatException, ParsingErrorException
    {
        STLFileReader reader = null;
        try
        {
            if( itsShowProgress )
            {
                reader = new STLFileReader( url, itsParentComponent );
            }
            else
            {
                reader = new STLFileReader( url );
            }
            return createScene( reader );
        }
        catch( InterruptedIOException ie )
        {
            // user cancelled loading
            return null;
        }
        catch( IOException e )
        {
            throw new IncorrectFormatException( e.toString( ) );
        }
    }

    /**
     * Loading from a <code>Reader</code> object not supported.
     * @return <code>null</code>
     */
    public Scene load( Reader reader ) throws FileNotFoundException,
    IncorrectFormatException, ParsingErrorException
    {
        /** @todo loading from Reader object */
        return null;
    }

    static Vector3f[] genNormals(TriangleArray triangle){
    	final GeometryInfo gi = new GeometryInfo(triangle);
        final NormalGenerator normalGenerator = new NormalGenerator();
        normalGenerator.generateNormals(gi);
        return gi.getNormals();
    }

    /*
     * returns true if both points to similar direction
     */
    static boolean compareNormals(Vector3f n1, Vector3f n2){
    	float a = (float) Math.toRadians(89f);
    	return (Math.abs(n1.angle(n2)) < a);
    }
    
    /**
     * Creates a <code>Scene</code> object with the contents of the STL file.
     * Closes the reader after finishing reading.
     * @param reader <code>STLFileReader</code> object for reading the STL file.
     */
    public static Scene createScene( final STLFileReader reader )
    throws IncorrectFormatException, ParsingErrorException
    {
        try
        {
            final SceneBase scene = new SceneBase( );
            final BranchGroup bg = new BranchGroup( );
            final int numOfObjects = reader.getNumOfObjects( );
            final int[ ] numOfFacets = reader.getNumOfFacets( );
            final String[ ] names = reader.getObjectNames( );

            System.out.printf("numOfObjects:%d\n", numOfObjects);
            
            final double[ ] normal = new double[ 3 ];
            final float[ ] fNormal = new float[ 3 ];
            final double[ ][ ] vertices = new double[ 3 ][ 3 ];
            final double[ ][ ] xvertices = new double[ 3 ][ 3 ];
            for( int i = 0; i < numOfObjects; i++ )
            {
                final TriangleArray geometry = new TriangleArray
                (
                    3 * numOfFacets[ i ],
                    TriangleArray.NORMALS | TriangleArray.COORDINATES
                    | TriangleArray.TEXTURE_COORDINATE_3
                    | TriangleArray.COLOR_4
                    | TriangleArray.ALLOW_COLOR_READ | TriangleArray.ALLOW_COLOR_WRITE
                    //12 //3 * numOfFacets[ i ]
                );
                
                System.out.printf("Object:%d; facets:%d\n", i, numOfFacets[i]);
                		
                int index = 0;
                for( int j = 0; j < numOfFacets[ i ]; j ++ )
                {
                    final boolean ok = reader.getNextFacet( normal, vertices );
                    if( ok )
                    {
                        fNormal[ 0 ] = ( float ) normal[ 0 ];
                        fNormal[ 1 ] = ( float ) normal[ 1 ];
                        fNormal[ 2 ] = ( float ) normal[ 2 ];
                        
                        //bottom side
                        xvertices[0] = vertices[2]; 
                        xvertices[1] = vertices[1]; 
                        xvertices[2] = vertices[0]; 

                    	TriangleArray tria = new TriangleArray(3, TriangleArray.NORMALS | TriangleArray.COORDINATES); 
                    	TriangleArray xtria = new TriangleArray(3, TriangleArray.NORMALS | TriangleArray.COORDINATES); 
                        // calculate normal to compare with normal from STL,
                        // if they are opposite, use rearranged vertices 
                        for( int k = 0; k < 3; k++ )
                        {
                        	tria.setCoordinates(k, vertices[k]);
                        	xtria.setCoordinates(k, xvertices[k]);
                        }
                    	Vector3f[] triaNormal = genNormals(tria);
                    	Vector3f[] xtriaNormal = genNormals(xtria);

                    	boolean sameDirection = compareNormals(triaNormal[0], new Vector3f(fNormal));
                        //System.out.printf("Facet:%d\n", j);
                        
                        for( int k = 0; k < 3; k++ )
                        {
                            //System.out.printf("vertex p:%d [%f,%f,%f]\n", 
                           	//	k, vertices[k][0],vertices[k][1],vertices[k][2]);
                        	
                            //geometry.setNormal(index, fNormal );
                            
                            if (sameDirection){ 
                            	geometry.setCoordinate( index, vertices[k] );
                            	geometry.setNormal(index, triaNormal[0]);
                            }
                            else {
                            	geometry.setCoordinate( index, xvertices[k] );
                            	geometry.setNormal(index, xtriaNormal[0]);
                            }
                            /*
                            if ( normal[2] > 0.1d ){
                            	geometry.setCoordinate( index, vertices[k] );
                    			geometry.setColor(index, new Color4f(0.0f, 0.0f, 0.7f, 1.0f));
                            }
                            else if ( normal[2] < -0.1d ) // flip triangle to face outside
                            	geometry.setCoordinate( index, xvertices[k] );
                            else  
                            	if (normal[1] >= 0d){
                            		geometry.setCoordinate( index, vertices[k] );
                            		geometry.setColor(index, new Color4f(0.7f, 0.0f, 0.0f, 1.0f));;
                            	}
                            	else{
                                	geometry.setCoordinate( index, xvertices[k] );
                            		geometry.setColor(index, new Color4f(0.0f, 0.8f, 0.0f, 1.0f));;
                            	}
                            */
                            index++;
                        }
                    }
                    else
                    {
                        throw new ParsingErrorException( );
                    }
                }
                final Shape3D shape = new Shape3D( geometry );
                bg.addChild( shape );
                String name = names[ i ];
                if( name == null )
                {
                    name = new String( "Unknown_" + i );
                }
                scene.addNamedObject( name, shape );
                System.out.printf("Shape:"+name);
            }
            scene.setSceneGroup( bg );
            return scene;
        }
        catch( InterruptedIOException ie )
        {
            // user cancelled loading
            return null;
        }
        catch( IOException e )
        {
            throw new ParsingErrorException( e.toString( ) );
        }
        finally
        {
            try
            {
                reader.close( );
            }
            catch( IOException e )
            {
                e.printStackTrace( );
            }
        }
    }
}
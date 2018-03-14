/**
 * 
 */
package com.hifiremote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import javax.swing.JOptionPane;

/**
 * @author Greg
 */
public class LibraryLoader
{
  public static void loadLibrary( File folder, String libraryName ) throws UnsatisfiedLinkError
  {
    String osName = System.getProperty( "os.name" );
    if ( osName.startsWith( "Windows" ) )
    {
      osName = "Windows";
    }
    if ( libraryFolder == null )
    {
      String arch = System.getProperty( "os.arch" ).toLowerCase();
      String folderName = osName + '-' + arch;
      libraryFolder = new File( folder, folderName );
      System.err.println( "libraryFolder=" + libraryFolder.getAbsolutePath() );
      if ( osName.equals( "Windows" ) )
      {
        hidIndex = arch.equals( "amd64" ) ? 4 : 5;
      }
      else if ( osName.equals( "Linux" ) )
      {
        hidIndex = arch.equals( "amd64" ) ? 0 : 1;
      }
      else if ( osName.equals( "Mac OS X" ) )
      {
        hidIndex = arch.equals( "x86_64" ) ? 2 : 3;
      }
    }

    if ( libraries.get( libraryName ) == null )
    {
      System.err.println( "LibraryLoader: Java version '" + System.getProperty( "java.version" ) + "' from '" + System.getProperty( "java.home" ) + "' running on '" + System.getProperty( "os.name" ) + "' (" + System.getProperty( "os.arch" ) + ")" );
      String mappedName = System.mapLibraryName( libraryName );
      if ( osName.equalsIgnoreCase( "Mac OS X" ) )
      {
        int dot = mappedName.indexOf( '.' );
        if ( dot >= 0 )
        {
          String base = mappedName.substring( 0, dot );
          String extn = mappedName.substring( dot );
          if ( extn.equalsIgnoreCase( ".dylib" ) )
          {
            mappedName = base + ".jnilib"; 
          }
        }
      }
      File libraryFile = new File( libraryFolder, mappedName );
      if ( libraryName.equals( "hidapi" ) && !libraryFile.exists() )
      {
        System.err.println( "LibraryLoader: Attempting to copy hidapi library to library folder" );
        boolean success = copyHIDLibrary( libraryFile );
        System.err.println( "LibraryLoader: Attempt to copy hidapi library " + ( success ? "succeeded" : "failed" ) );
        if ( !success )
        {
          String title = "Setup error";
          String message = "RMIR was unable to set up the library required for USB HID communication.\n"
            + "This may mean that you have installed RMIR in a read-only folder.  If so,\n"
            + "some features of RMIR will not work correctly.  You are strongly advised\n"
            + "to reinstall it in a folder that is not read-only.";
          JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
        }
      }
      System.err.println( "LibraryLoader: Attempting to load '" + libraryName + "' from '" + libraryFile.getAbsolutePath() + "'..." );
      try
      {
        if ( osName.equals( "Windows" ) && libraryName.equalsIgnoreCase( "jp1parallel" ) )
        {
          String arch = System.getProperty( "os.arch" ).toLowerCase();
          String inpoutName = arch.equals( "amd64" ) ? "inpoutx64.dll" : "inpout32.dll" ;
          File inpoutFile = new File( folder, inpoutName );
          if ( inpoutFile.exists() )
          {
            System.load( inpoutFile.getAbsolutePath() );
          }
        }
        System.load( libraryFile.getAbsolutePath() );
        System.err.println( "LibraryLoader: Loaded '" + libraryName + "' successfully from '" + libraryFile.getAbsolutePath() + "'" );
        libraries.put( libraryName, mappedName );
      }
      catch ( UnsatisfiedLinkError ule )
      {
        System.err.println( "LibraryLoader: Failed to load '" + libraryName + "' from '" + libraryFile.getAbsolutePath() + "'" );
        // second try just from standard library locations
        loadLibrary( libraryName );
      }
    }
  }

  public static void loadLibrary( String libraryName ) throws UnsatisfiedLinkError
  {
    if ( libraries.get( libraryName ) == null )
    {
      System.err.println( "LibraryLoader: Java version '" + System.getProperty( "java.version" ) + "' from '" + System.getProperty( "java.home" ) + "' running on '" + System.getProperty( "os.name" ) + "' (" + System.getProperty( "os.arch" ) + ")" );
      System.err.println( "LibraryLoader: Attempting to load '" + libraryName + "' from java library path..." );
      System.err.println( "LibraryLoader: Java library path is '" + System.getProperty( "java.library.path" ) + "'" );
      System.loadLibrary( libraryName );
      System.err.println( "LibraryLoader: Loaded '" + libraryName + "' successfully from somewhere in java library path.");
      libraries.put( libraryName, libraryName );
    }
  }
  
  private static boolean copyHIDLibrary( File libFile )
  {
    // Based on com.codeminders.hidapi.ClassPathLibraryLoader, modified to
    // load required library to RMIR library folder
    if ( hidIndex < 0 )
    {
      return false;
    }
    String path = HID_LIB_NAMES[ hidIndex ];
    try {
      // have to use a stream
      InputStream in = LibraryLoader.class.getResourceAsStream( path );
      if ( in != null ) 
      {
        try 
        {
          OutputStream out = new FileOutputStream( libFile );
          byte[] buf = new byte[ 1024 ];
          int len;
          while ( ( len = in.read( buf ) ) > 0 )
          {            
            out.write( buf, 0, len );
          }
          out.close();
        } 
        finally 
        {
          in.close();
        }
      }                 
    } 
    catch ( Exception e ) { return false; }
    catch ( UnsatisfiedLinkError e ) { return false; }
    return true;
  }

  public static String getLibraryFolder()
  {
    return libraryFolder.getAbsolutePath();
  }

  protected static HashMap< String, String > libraries = new HashMap< String, String >();
  protected static File libraryFolder = null;
  protected static int hidIndex = -1;
  
  private static final String[] HID_LIB_NAMES = 
  {
    "/native/linux/libhidapi-jni-64.so",
    "/native/linux/libhidapi-jni-32.so",
    "/native/mac/libhidapi-jni-64.jnilib",
    "/native/mac/libhidapi-jni-32.jnilib",
    "/native/win/hidapi-jni-64.dll",
    "/native/win/hidapi-jni-32.dll"
  };
}

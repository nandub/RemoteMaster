package com.hifiremote.jp1;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class DynamicURLClassLoader extends URLClassLoader
{
  protected static DynamicURLClassLoader instance;

  protected DynamicURLClassLoader( URL[] urls, ClassLoader parent )
  {
    super( urls, parent );
  }

  public static DynamicURLClassLoader getInstance() {
    if (instance == null) {
      instance = new DynamicURLClassLoader( new URL[0], ClassLoader.getSystemClassLoader() );
    }
    return instance;
  }

  /**
   * Adds the url.
   *
   * @param url the url
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void addURLs( URL url ) throws IOException
  {
    try
    {
      super.addURL( url );
    }
    catch ( Throwable t )
    {
      t.printStackTrace( System.err );
      throw new IOException( "Error, could not add URL to system classloader" );
    }
  }

  /**
   * Adds the file.
   *
   * @param name the name
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void addFile( String name ) throws IOException
  {
    addFile( new File( name ) );
  }

  /**
   * Adds the file.
   *
   * @param file the file
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void addFile( File file )
      throws IOException
  {
    addURLs( file.toURI().toURL() );
  }

  /**
   * Adds the files.
   *
   * @param files the files
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void addFiles( File[] files )
      throws IOException
  {
    URL[] urls = new URL[ files.length ];
    for ( int i = 0; i < files.length; ++i )
      urls[ i ] = files[ i ].toURI().toURL();
    addURLs( urls );
  }

  /**
   * Adds the ur ls.
   *
   * @param urls the urls
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void addURLs( URL[] urls )
      throws IOException
  {
    for (URL url : urls) {
      addURLs( url );
    }
  }
}

package com.hifiremote.jp1.io;

import java.io.File;
import com.hifiremote.jp1.Hex;

// TODO: Auto-generated Javadoc
/**
 * The Class JP12Serial.
 */
public class JP12Serial
  extends IO
{
  
  /* (non-Javadoc)
   * @see com.hifiremote.jp1.io.IO#getInterfaceName()
   */
  public native String getInterfaceName();
  
  /* (non-Javadoc)
   * @see com.hifiremote.jp1.io.IO#getInterfaceVersion()
   */
  public native String getInterfaceVersion();
  
  /* (non-Javadoc)
   * @see com.hifiremote.jp1.io.IO#getPortNames()
   */
  public native String[] getPortNames();
  
  /* (non-Javadoc)
   * @see com.hifiremote.jp1.io.IO#openRemote(java.lang.String)
   */
  public native String openRemote( String portName );
  
  /* (non-Javadoc)
   * @see com.hifiremote.jp1.io.IO#closeRemote()
   */
  public native void closeRemote();
  
  /* (non-Javadoc)
   * @see com.hifiremote.jp1.io.IO#getRemoteSignature()
   */
  public native String getRemoteSignature();
  
  /* (non-Javadoc)
   * @see com.hifiremote.jp1.io.IO#getRemoteEepromAddress()
   */
  public native int getRemoteEepromAddress();
  
  /* (non-Javadoc)
   * @see com.hifiremote.jp1.io.IO#getRemoteEepromSize()
   */
  public native int getRemoteEepromSize();
  
  /* (non-Javadoc)
   * @see com.hifiremote.jp1.io.IO#readRemote(int, byte[], int)
   */
  public native int readRemote( int address, byte[] buffer, int length );
  
  /* (non-Javadoc)
   * @see com.hifiremote.jp1.io.IO#writeRemote(int, byte[], int)
   */
  public native int writeRemote( int address, byte[] buffer, int length );
  
  /** The is loaded. */
  private static boolean isLoaded = false;

  /**
   * Instantiates a new j p12 serial.
   * 
   * @throws UnsatisfiedLinkError the unsatisfied link error
   */
  public JP12Serial()
    throws UnsatisfiedLinkError
  {
    if ( !isLoaded )
    {
      System.loadLibrary( "jp12serial" );
      isLoaded = true;
    }
  }

  /**
   * Instantiates a new j p12 serial.
   * 
   * @param folder the folder
   * 
   * @throws UnsatisfiedLinkError the unsatisfied link error
   */
  public JP12Serial( File folder )
    throws UnsatisfiedLinkError
  {
    if ( !isLoaded )
    {
      File file = new File( folder, System.mapLibraryName( "jp12serial" ));
      System.load( file.getAbsolutePath());
      isLoaded = true;
    }
  }


  /**
   * The main method.
   * 
   * @param args the arguments
   */
  public static void main( String[] args )
  {
    JP12Serial test = new JP12Serial();
    String portName = null;
    for ( int i = 0; i < args.length; ++i )
    {
      String arg = args[ i ];
      if ( arg.equals( "-port" ) && (( i + 1 ) < args.length ))
      {
        portName = args[ ++i ];
        System.err.println( "Using port " + portName );
      }
    }
    portName = test.openRemote( portName );

    if ( portName != null )
    {
      System.err.println( "Found remote on port " + portName );
      System.err.println( "signature=" + test.getRemoteSignature());
      int address = test.getRemoteEepromAddress();
      System.err.println( "address=" + Integer.toHexString( address ).toUpperCase());
      int size = test.getRemoteEepromSize();
      System.err.println( "size=" + size );
      short[] buffer = new short[ 0x20 ];
      int len = test.readRemote( address, buffer );
      if ( len < 0 )
      {
        System.err.println( "Error reading from remote!" );
      }
      else
      {
        System.err.println( "Start of EEPROM:" );
        System.err.print( ' '  );
        System.err.println( Hex.toString( buffer, 16 ));
      }
      test.closeRemote();
    }
    else
    {
      System.err.println( "No JP1.2 compatible remote found!" );
    }
  }
}

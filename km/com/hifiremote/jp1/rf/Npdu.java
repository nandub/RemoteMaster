package com.hifiremote.jp1.rf;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.rf.Mpdu.MACAddrData;

public class Npdu extends Hex
{
  public static enum NSDUType
  { 
    DATA, COMMAND, VDATA 
  };
  
  public static enum NSDUCommand
  {
    DISCOVERY_REQ, DISCOVERY_RSP, PAIR_REQ, PAIR_RSP,
    UNPAIR_REQ, KEYSEED, PING_REQ, PING_RSP
  }
  
  public static enum NSDUDirection
  {
    IN, OUT
  }

  /**
   * Network Service Primitive
   */
  public static class NSPrimitive
  {
    public int frameCtl = 0;
    public Hex frameCtrHex = null;
    public MACAddrData addrData = null;
    public Hex profileID  = null;
    public Hex vendorID = null;
    public NSDUType type = null;
    public NSDUCommand cmd = null;
    public NSDUDirection direction = null;
    public boolean valid = true;
    public boolean secured = false;
    public int channelDesignator = 0;
    public Hex nsdu = null;
    public Hex rawNsdu = null;  // encrypted value when NPDU is secured
    public Hex authData = null; // message integrity field for authentication
    public int rptCount = 0;
    public RfRemote rfRemote = null;
    private String message = null;
     
    public void setError( String message )
    {
      valid = false;
      this.message = message;
    }
    
    public String getErrorMessage()
    {
      return message;
    }

    public void setCommand()
    {
      if ( type == NSDUType.COMMAND )
      {
        int cmdCode = nsdu.getData()[ 0 ];
        if ( cmdCode < 1 || cmdCode > 8 )
        {
          setError( "Unknown network command" );
          return;
        }
        cmd = NSDUCommand.values()[ cmdCode - 1 ];
      }
      else
      {
        cmd = null;
      }
    }
    
    public void process( Rf4ceAuthenticator decrypter )
    {
      decrypter.decrypt( this );
      setCommand();
      if ( valid )
      {
        if ( type == NSDUType.COMMAND )
        {
          decrypter.processCommand( this );
        }
        rfRemote = decrypter.getRfRemote();
      }
      return;
    }
  }
  
  public Npdu( Hex hex )
  {
    super( hex );
  }
  
  public Npdu( Hex hex, int offset, int length )
  {
    super( hex, offset, length );
  }
  
  public Npdu( Hex hex, int offset )
  {
    super( hex, offset, hex.length() - offset );
  }
  
  public NSPrimitive parse()
  {
    NSPrimitive nsPrim = new NSPrimitive();
    nsPrim.frameCtl = getData()[ 0 ];
    nsPrim.channelDesignator = nsPrim.frameCtl >> 6;
    int frameType = nsPrim.frameCtl & 3;
    if ( frameType == 0 )
    {
      nsPrim.setError( "RF4CE frame has illegal reserved type" );
      return nsPrim;
    }
    nsPrim.type = NSDUType.values()[ frameType - 1 ];
    nsPrim.secured = ( nsPrim.frameCtl & 4 ) == 4;
    boolean hasProfileId = frameType != 2;
    boolean hasVendorId = frameType == 3;
    int rf4ceHdrSize = 5 + ( hasProfileId ? 1 : 0 ) + ( hasVendorId ? 2 : 0 );
    int rf4ceFtrSize = nsPrim.secured ? 4 : 0;
    int nsduSize = length() - rf4ceHdrSize - rf4ceFtrSize;     
    nsPrim.frameCtrHex = subHex( 1, 4 );
    if ( hasProfileId )
    {
      nsPrim.profileID = subHex( 5, 1 );
    }
    if ( hasVendorId )
    {
      nsPrim.vendorID = subHex( 6, 2 ); 
    }
    // Now extract the nsdu, but possibly encrypted and with a 4-byte message integrity field
    nsPrim.nsdu = subHex( rf4ceHdrSize, nsduSize );
    nsPrim.authData = nsPrim.secured ? subHex( rf4ceHdrSize + nsduSize, 4 ) : null;
    return nsPrim;
  }
  
}

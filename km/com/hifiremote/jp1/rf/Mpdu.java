package com.hifiremote.jp1.rf;

import com.hifiremote.jp1.Hex;

public class Mpdu extends Hex
{
  /**
   * MAC address fields
   */
  public static class MACAddrData
  {
    public Hex srcPAN = null;
    public Hex srcAddr = null;
    public Hex destPAN = null;
    public Hex destAddr = null;
  }
  
  /**
   * MAC Service Primitive
   */
  public static class MSPrimitive
  {
    public MACAddrData addrData = null;
    public Npdu npdu = null;
    private String error = null;
    
    public MSPrimitive()
    {
      addrData = new MACAddrData();
    }
    
    public void setError( String error )
    {
      npdu = null;
      this.error = error;
    }

    public String getError()
    {
      return error;
    }
  }
  
  public Mpdu( Hex hex )
  {
    super( hex );
  }
  
  public Mpdu( Hex hex, int offset, int length )
  {
    super( hex, offset, length );
  }
  
  public Mpdu( Hex hex, int offset )
  {
    super( hex, offset, hex.length() - offset );
  }
  
  public MSPrimitive parse()
  {
    MSPrimitive prim = new MSPrimitive();
    MACAddrData addrData = prim.addrData;

    int macFrameCtl = getData()[ 0 ] + ( getData()[ 1 ] << 8 );
    int macFrameType = macFrameCtl & 7;
    int macDestAddrMode = ( macFrameCtl >> 10 ) & 3;
    int macSrcAddrMode = ( macFrameCtl >> 14 ) & 3;
    boolean macIntraPAN = ( macFrameCtl & 0x40 ) == 0x40;
    if ( macFrameType != 1 )
    {
      prim.setError( "MAC frame is not a data frame" );
      return prim;
    }
    if ( macSrcAddrMode == 1 || macDestAddrMode == 1 )
    {
      prim.setError( "Unknown MAC address mode" );
      return prim;
    }        
    if ( macSrcAddrMode == 0 || macDestAddrMode == 0 )
    {
      prim.setError( "MAC address mode of None is not supported" );
      return prim;
    }
    int destAddrSize =  macDestAddrMode == 3 ? 8 : 2;
    int srcAddrSize =  macSrcAddrMode == 3 ? 8 : 2;
    int srcPANSize = macIntraPAN ? 0 : 2;
    addrData.destPAN = subHex( 3, 2 );
    int addrFieldSize = 2;
    addrData.destAddr = subHex( 3 + addrFieldSize, destAddrSize );
    addrFieldSize += destAddrSize;
    addrData.srcPAN = macIntraPAN ? addrData.destPAN : subHex( 3 + addrFieldSize, srcPANSize );
    addrFieldSize += srcPANSize;
    addrData.srcAddr = subHex( 3 + addrFieldSize, srcAddrSize );
    addrFieldSize += srcAddrSize;
    int npduOffset = 3 + addrFieldSize;
    int npduSize = length() - npduOffset - 2;  // 2 is size of mpdu frame check sequence
    prim.npdu = new Npdu( this, npduOffset, npduSize );
    return prim;
  }
}

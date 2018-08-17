package com.hifiremote.jp1;

import java.util.LinkedHashMap;

import com.hifiremote.jp1.AssemblerOpCode.AddressMode;

public class TI2541Processor extends LittleEndianProcessor
{
  public TI2541Processor( String name )
  {
    super( name );
    setPageSize( 0x800 );
  }
  
  @Override
  public String getEquivalentName()
  {
    return "TI2541";
  }
  
  @Override
  public AddressMode disasmModify( AddressMode mode, Object[] obj )
  {
    switch ( mode.modifier )
    {
      case 1:
        LinkedHashMap< String, AddressMode > modes = getAddressModes();
        int op1 = ( Integer )obj[ 0 ];
        if ( op1 < 0x70 )
        {
          mode = modes.get( "Fun1" );
        }
        else if ( op1 == 0x73 )
        {
          // 0x73 not available in MAXQ executors
          mode = modes.get( "Fun1Z" );
        }
        else if ( op1 < 0x80 )
        {
          mode = modes.get( "Fun1B" );
        }
        break;
    }
    return mode;
  }
  
  @Override
  public int[] getCarrierData( Hex hex )
  {
    short[] data = hex.getData();
    int carrier = getInt( data, 1 );
    int duty = data[ 3 ] * 10;
    return new int[]{ carrier, duty };
  }
}

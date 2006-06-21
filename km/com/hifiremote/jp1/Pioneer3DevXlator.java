package com.hifiremote.jp1;

import javax.swing.*;

public class Pioneer3DevXlator
  extends Translator
{
  private static int devIndex = 0;
  private static int obcIndex = 1;
  private static int obc2Index = 2;

  public Pioneer3DevXlator( String[] textParms )
  {
    super( textParms );
  }

  private int getDevice( Hex hex )
  {
    int temp = extract( hex, 8, 3 );
    if ( temp == 4 )
      return 0;
    if ( temp == 2 )
      return 1;
    return 2;
  }

  private int getDevice( Value[] parms )
  {
    if (( parms[ devIndex ] == null ) ||
        ( parms[ devIndex ].getValue() == null ))
      return 0;
    return (( Number )parms[ devIndex ].getValue()).intValue();
  }

  private void setDevice( int device, Hex hex )
  {
    int temp = 1 << ( 2 - device );
    insert( hex, 8, 3, temp );
  }

  private int getObc( Hex hex )
  {
    return reverse( extract( hex, 0, 8 ));
  }

  private int getObc( Value[] parms )
  {
    return (( Number )parms[ obcIndex ].getValue()).intValue();
  }

  private void setObc( int obc, Hex hex )
  {
    insert( hex, 0, 8, reverse( obc ));
  }

  private int adjust( int obc, int obc2 )
  {
    if (( obc & 0x80 ) != 0 )
      obc2 += 0x80;
    if (( obc & 0x40 ) == 0 )
      obc2 += 0x40;
    return obc2;
  }

  private Integer getObc2( Hex hex )
  {
    int obc2 = reverse( extract( hex, 11, 5 ), 5 );
    if ( obc2 == 0 )
      return null;
    else
    {
      int obc = getObc( hex );
      return new Integer( adjust( obc, obc2 ));
    }
  }

  private Integer getObc2( Value[] parms )
  {
    if (( parms[ obc2Index ] == null ) ||
        ( parms[ obc2Index ].getValue() == null ))
      return null;
    return ( Integer )parms[ obc2Index ].getValue();
  }

  private void setObc2( Integer obc2, Hex hex )
  {
    if ( obc2 != null )
    {
      int val = obc2.intValue();
      insert( hex, 11, 5, reverse( val, 5 ));
      Integer temp = getObc2( hex );
      if ( !obc2.equals( temp ))
//        JOptionPane.showMessageDialog( KeyMapMaster.getKeyMapMaster(),
        throw new IllegalArgumentException(
                                         "OBC=" + getObc( hex ) + " and OBC2=" + val + " can not be sent using Pioneer 3DEV. Use Pioneer 4DEV instead." );
//                                       "Value not supported.",
//                                       JOptionPane.ERROR_MESSAGE );
    }
    else
      insert( hex, 11, 5, 0 );
  }

  public void in( Value[] parms, Hex hex, DeviceParameter[] devParms, int onlyIndex )
  {
    boolean doAll = ( onlyIndex < 0 );
    if (( onlyIndex == devIndex ) || doAll )
    {
      setDevice( getDevice( parms ), hex );
    }
    if (( onlyIndex == obcIndex ) || doAll )
    {
      int obc = getObc( parms );
      setObc( obc, hex );
    }
    if (( onlyIndex == obc2Index ) || doAll )
    {
      setObc2( getObc2( parms ), hex );
    }
  }

  public void out( Hex hex, Value[] parms, DeviceParameter[] devParms )
  {
    parms[ devIndex ] = new Value( new Integer( getDevice( hex )));
    int obc = getObc( hex );
    parms[ obcIndex ] = new Value( new Integer( obc ));
    Integer obc2 = getObc2( hex );
    parms[ obc2Index ] = new Value( obc2 );
  }
}

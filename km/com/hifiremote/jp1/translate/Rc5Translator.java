package com.hifiremote.jp1.translate;

import com.hifiremote.jp1.DeviceParameter;
import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.Value;

// TODO: Auto-generated Javadoc
/**
 * The Class Rc5Translator.
 */
public class Rc5Translator extends Translate
{
  protected int devOffset = 0;
  protected int devCount = 3;

  /**
   * Instantiates a new rc5 translator.
   * 
   * @param textParms
   *          the text parms
   */
  public Rc5Translator( String[] textParms )
  {
    super( textParms );
    int parmIndex = 0;
    for ( int i = 0; i < textParms.length; i++ )
    {
      String text = textParms[ i ];
      int val = Integer.parseInt( text );
      switch ( parmIndex )
      {
        case 0:
          devOffset = val;
          break;
        case 1:
          devCount = val;
          break;
        default:
          break;
      }
      parmIndex++ ;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.Translate#in(com.hifiremote.jp1.Value[], com.hifiremote.jp1.Hex,
   * com.hifiremote.jp1.DeviceParameter[], int)
   */
  @Override
  public void in( Value[] parms, Hex hexData, DeviceParameter[] devParms, int onlyIndex )
  {
    int select = 0;
    int obc = 0;

    if ( onlyIndex == 0 )
    {
      // User has specified which device to use.
      select = ( ( Number )parms[ 0 ].getValue() ).intValue();
      insert( hexData, 6, 2, select );
    }
    else if ( onlyIndex == 1 )
    {
      // User is trying to set the OBC. so get the value
      obc = ( ( Number )parms[ 1 ].getValue() ).intValue();

      // We need to know which device is selected so get that.
      select = hexData.getData()[ 0 ] & 3;
      if ( select >= devCount )
      {
        select = 0;
      }
      // We need to know if the device supports OBC less than or greater than 64
      int flag = ( ( Number )devParms[ 2 * select + devOffset + 1 ].getValue() ).intValue();

      // if the flag and obc value aren't compatible
      if ( flag == 0 && obc > 63 || flag == 1 && obc < 64 )
      {
        // get the device number, since we need to match it.
        int device = ( ( Number )devParms[ 2 * select + devOffset ].getValue() ).intValue();
        // cycle through the device parms
        for ( int i = 0; i < devCount; i++ )
        {
          // don't try to match the one that was being used.
          if ( i == select )
          {
            continue;
          }

          int index = 2 * i + devOffset;
          // make sure a device number has been entered.
          if ( devParms[ index ] == null || devParms[ index ].getValue() == null )
          {
            continue;
          }
          // extract the device number
          int tempDevice = ( ( Number )devParms[ index ].getValue() ).intValue();
          // extract the flag
          int tempFlag = ( ( Number )devParms[ index + 1 ].getValue() ).intValue();
          // if they will work for the new OBC value
          if ( tempDevice == device && tempFlag != flag )
          {
            // store the new index
            insert( hexData, 6, 2, i );
          }
        }
      }
      // store the obc
      insert( hexData, 0, 6, complement( obc & 0x3f, 6 ) );
    }
    else
    {
      if ( parms[ 0 ] == null || parms[ 0 ].getValue() == null )
      {
        return;
      }
      select = ( ( Number )parms[ 0 ].getValue() ).intValue();
      insert( hexData, 6, 2, select );
      if ( parms[ 1 ] == null || parms[ 1 ].getValue() == null )
      {
        return;
      }
      obc = ( ( Number )parms[ 1 ].getValue() ).intValue();
      insert( hexData, 0, 6, complement( obc, 6 ) );
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.Translate#out(com.hifiremote.jp1.Hex, com.hifiremote.jp1.Value[],
   * com.hifiremote.jp1.DeviceParameter[])
   */
  @Override
  public void out( Hex hex, Value[] parms, DeviceParameter[] devParms )
  {
    // first do the device selector
    int select = hex.getData()[ 0 ] & 3;
    if ( select >= devCount )
    {
      select = 0;
    }
    parms[ 0 ] = new Value( new Integer( select ) );

    // Now do the OBC
    int obc = complement( extract( hex, 0, 6 ), 6 );
    while ( select >= 0 )
    {
      int index = 2 * select + devOffset;
      if ( devParms[ index ] != null && devParms[ index ].getValue() != null )
      {
        int flag = ( ( Number )devParms[ index + 1 ].getValue() ).intValue();
        if ( flag != 0 )
        {
          obc |= 64;
        }
        break;
      }
      select-- ;
    }
    parms[ 1 ] = new Value( new Integer( obc ) );
  }
}

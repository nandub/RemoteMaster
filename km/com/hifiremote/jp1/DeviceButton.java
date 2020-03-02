package com.hifiremote.jp1;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;

import com.hifiremote.jp1.Remote.RFSelector;

// TODO: Auto-generated Javadoc
/**
 * The Class DeviceButton.
 */
public class DeviceButton extends Highlight
{
  /**
   * Instantiates a new device button.
   * 
   * @param name
   *          the name
   * @param hiAddr
   *          the hi addr
   * @param lowAddr
   *          the low addr
   * @param typeAddr
   *          the type addr
   * @param setupCode
   *          the default setup code
   * @param maxSetupCode
   *          the maximum allowed setup code
   */
  public DeviceButton( String name, int hiAddr, int lowAddr, int typeAddr, int setupCode, int index, int offset  )
  {
    this.name = name;
    defaultName = name;
    highAddress = hiAddr;
    lowAddress = lowAddr;
    typeAddress = typeAddr;
    defaultSetupCode = setupCode + offset;
    deviceCodeOffset = offset;
    buttonIndex = index;
  }
  
  public static final DeviceButton noButton = new DeviceButton( "<none>", 0, 0, 0, 0, -1, 0 );

  public void setName( String name )
  {
    this.name = name;
  }

  public void setDefaultName()
  {
    name = defaultName;
  }

  /**
   * Gets the default setup code.
   * 
   * @return the default setup code
   */
  public int getDefaultSetupCode()
  {
    return defaultSetupCode;
  }

  public boolean isRf()
  {
    return rf;
  }

  public void setRf( boolean rf )
  {
    this.rf = rf;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    return name;
  }

  /**
   * Gets the device type index.
   * 
   * @param data
   *          the data
   * @return the device type index
   */
  public int getDeviceTypeIndex( short[] data )
  {
    return highAddress > 0 ? data[ highAddress ] >> 4 : data[ 2 ];
  }
  
  /**
   * Sets the device type index.
   * 
   * @param index
   *          the index
   * @param data
   *          the data
   */
  public void setDeviceTypeIndex( short index, short[] data )
  {
    if ( highAddress > 0 )
    {
      if ( index == 0xFF )
      {
        data[ highAddress ] = 0xFF;
        data[ lowAddress ] = 0xFF;
      }
      else
      {
        data[ highAddress ] &= 0x0F;
        index <<= 4;
        data[ highAddress ] |= index;
      }
    }
    else
    {
      data[ 2 ] = index;
      if ( index == 0xFF )
      {
        data[ 3 ] = 0xFF;  // setup code high
        data[ 4 ] = 0xFF;  // setup code low
        data[ 5 ] = 0xFF;  // setup code lock OFF
        volumePT = noButton;
        transportPT = noButton;
        channelPT = noButton;
        xPT = noButton;
        inputPT = noButton;
        zPT = noButton;
      }
    }
  }
  
  public int getDeviceGroup( short[] data )
  {
    if ( typeAddress > 0 ) return data[ typeAddress ];
    else return -1;
  }
  
  public void setDeviceGroup( short group, short[] data )
  {
    if ( typeAddress > 0 )
    {
      data[ typeAddress ] = group;
    }
  }

  /**
   * Gets the setup code.
   * 
   * @param data
   *          the data
   * @return the setup code
   */
  public short getSetupCode( short[] data )
  {
    short setupCode = highAddress > 0 ? data[ highAddress ] : data[ 3 ];
    if ( highAddress > 0 )
    {
      // Remotes without segments, that use high address byte to encode device
      // type index and, for remotes with 1-byte PID, also bit 8 of PID.
      int mask = SetupCode.getMax() >> 8;
      setupCode &= mask;
    }
    setupCode <<= 8;
    setupCode |= lowAddress > 0 ? data[ lowAddress ] : data[ 4 ];
    setupCode += deviceCodeOffset;
    return setupCode;
  }
  
  public boolean getSetupLock( short[] data )
  {
    return ( data[ 5 ] & 0x80 ) == 0;
  }

  /**
   * Sets the setup code.
   * 
   * @param setupCode
   *          the setup code
   * @param data
   *          the data
   */
  public void setSetupCode( short setupCode, short[] data )
  {
    if ( highAddress > 0 )
    {
      if ( setupCode > SetupCode.getMax() )
      {
        throw new NumberFormatException( "Setup codes must be between 0 and " + SetupCode.getMax() );
      }
      int val = setupCode - deviceCodeOffset;
      short temp = ( short )val;
      temp >>= 8;
      int mask = 0xF8;
      if ( SetupCode.getMax() > 2047 )
        mask = 0xF0;
      data[ highAddress ] &= mask;
      data[ highAddress ] |= temp;
      data[ lowAddress ] = ( short )( val & 0xFF );
    }
    else
    {
      Hex.put( ( int )setupCode, data, 3 );
    }
  }
  
  public void setSetupLock( boolean lock, short[] data )
  {
    data[ 5 ] = ( short )( lock ? data[ 5 ] & 0x7F : data[ 5 ] | 0x80 );
    
  }
  
  public int getDeviceSlot( short[] data )
  {
    if ( highAddress == 0 )
    {
      // this returns the device type in its high byte and the high byte of the setup code
      // in its low byte, which will return 0xFFFF only if both the device type and setup
      // code are unset
      return ( data[ 2 ] << 8 ) | data[ 3 ];
    }
    else
    {
      return ( data[ highAddress ] << 8 ) | data[ lowAddress ];
    }
  }
  
  public void setDeviceSlot( int value, short[] data )
  {
    data[ highAddress ] = ( short )( value >> 8 );
    data[ lowAddress ] = ( short )( value & 0xFF );
  }
  
  public void zeroDeviceSlot( short[] data )
  {
    if ( highAddress > 0 )
    {
      data[ highAddress ] = 0;
      data[ lowAddress ] = 0;
    }
    else
    {
      data[ 3 ] = 0;
      data[ 4 ] = 0;
    }
  }
  
  public int getButtonIndex()
  {
    return buttonIndex;
  }

  public short[] getPTdefaults()
  {
    return ptDefaults;
  }

  public void setPTdefaults( short[] ptDefaults )
  {
    this.ptDefaults = ptDefaults;
  }

  public DeviceButton getVolumePT()
  {
    return volumePT;
  }

  public void setVolumePT( DeviceButton volumePT )
  {
    this.volumePT = volumePT;
  }

  public DeviceButton getTransportPT()
  {
    return transportPT;
  }

  public void setTransportPT( DeviceButton transportPT )
  {
    this.transportPT = transportPT;
  }

  public DeviceButton getChannelPT()
  {
    return channelPT;
  }

  public void setChannelPT( DeviceButton channelPT )
  {
    this.channelPT = channelPT;
  }
  
  public DeviceButton getxPT()
  {
    return xPT;
  }

  public void setxPT( DeviceButton xPT )
  {
    this.xPT = xPT;
  }

  public DeviceButton getInputPT()
  {
    return inputPT;
  }

  public void setInputPT( DeviceButton inputPT )
  {
    this.inputPT = inputPT;
  }

  public DeviceButton getzPT()
  {
    return zPT;
  }

  public void setzPT( DeviceButton zPT )
  {
    this.zPT = zPT;
  }

  private String defaultName = null;

  /** The high address. */
  private int highAddress = 0;

  /** The low address. */
  private int lowAddress = 0;

  /** The type address. */
  private int typeAddress = 0;

  /** The default setup code */
  private int defaultSetupCode = 0;
  private int deviceCodeOffset = 0;
  
  private int buttonIndex = 0;
  private int colorIndex = -1;  // Default -1 is white, non-editable
  private int[] colorParams = null;  // Only set for devices with editable led colors
  private DeviceButton volumePT = noButton;
  private DeviceButton transportPT = noButton;
  private DeviceButton channelPT = noButton;
  private DeviceButton xPT = noButton;
  private DeviceButton inputPT = noButton;
  private DeviceButton zPT = noButton;
  private int favoriteWidth = 0;
  private int vpt = 0;
  private boolean constructed = false;
  private short[] ptDefaults = null;
  private boolean rf = false;
  
  private HashMap< Button, String > softButtonNames = null;
  private HashMap< Button, String > softFunctionNames = null;
  private RFSelector[] rfSelectors = null;
  
  public boolean isConstructed()
  {
    return constructed;
  }

  public void setConstructed( boolean constructed )
  {
    this.constructed = constructed;
  }

  /** 
   *   Used only for XSight remotes, where there is a direct correspondence between device
   *   buttons and device upgrades even for built-in setup codes
   */
  private DeviceUpgrade upgrade = null;

  public DeviceUpgrade getUpgrade()
  {
    return upgrade;
  }

  public void setUpgrade( DeviceUpgrade upgrade )
  {
    this.upgrade = upgrade;
  }

  public HashMap< Button, String > getSoftButtonNames()
  {
    return softButtonNames;
  }

  public void setSoftButtonNames( HashMap< Button, String > softButtonNames )
  {
    this.softButtonNames = softButtonNames;
  }

  public HashMap< Button, String > getSoftFunctionNames()
  {
    return softFunctionNames;
  }

  public void setSoftFunctionNames( HashMap< Button, String > softFunctionNames )
  {
    this.softFunctionNames = softFunctionNames;
  }

  public int getFavoriteWidth()
  {
    return favoriteWidth;
  }

  public void setFavoritewidth( int favoriteWidth )
  {
    this.favoriteWidth = favoriteWidth;
  }
  
  /** 0 = Off, 1 = On, 2 = Master */
  public int getVpt()
  {
    return vpt;
  }

  /** 0 = Off, 1 = On, 2 = Master */
  public void setVpt( int vpt )
  {
    this.vpt = vpt;
  }

  public int getColorIndex()
  {
    return colorIndex;
  }

  public void setColorIndex( int colorIndex )
  {
    this.colorIndex = colorIndex;
  }

  public int[] getColorParams()
  {
    return colorParams;
  }

  public void setColorParams( int[] colorParams )
  {
    this.colorParams = colorParams;
  }

  public RFSelector[] getRfSelectors()
  {
    return rfSelectors;
  }

  public void setRfSelectors( RFSelector[] rfSelectors )
  {
    this.rfSelectors = rfSelectors;
  }

  public void doHighlight( Color[] highlight )
  {
    if ( highAddress > 0 )
    {
      highlight[ highAddress ] = getHighlight();
      highlight[ lowAddress ] = getHighlight();
      if ( typeAddress > 0 )
      {
        highlight[ typeAddress ] = getHighlight();
      }
      setMemoryUsage( ( typeAddress > 0 ) ? 3 : 2 );
    }
    else if ( getSegment() != null )
    {
      int address = getSegment().getAddress();
      int size = getSegment().getHex().length() + 4;
      for ( int i = 3; i < size; i++ )
      {
        highlight[ address + i ] = getHighlight();
      }
      setMemoryUsage( size - 3 );
    }
  }
  
  public void store( Remote remote )
  {
    Hex hex = null;    
    if ( getSegment() != null )
    {
      hex = getSegment().getHex();
    }
    else if ( remote.getSegmentTypes() != null )
    {
      // Create unset segment for this device button
      hex = new Hex( 12 );
      Arrays.fill( hex.getData(), ( short )0xFF );
      hex.set( ( short )buttonIndex, 0 );
      hex.set( ( short )0, 1 );
      setSegment( new Segment( 0, 0xFF, hex ) );
    }
    else
    {
      // Remote does not have segments
      return;
    }
    
    int ptDefLen = ptDefaults == null ? 0 : ptDefaults.length;
    
    // Set punchthrough defaults.  When there are no explicit defaults, use 0 for VTC and
    // 0xFF for XYZ for compatibility with previous behaviour for remotes that do not use XYZ.
    for ( int i = 0; i < 6; i++ )
    {
      hex.set( ( short )( i < ptDefLen ? ptDefaults[ i ] : i < 3 ? 0 : 0xFF ), i + 6 );
    }

    String pt = remote.getPunchThru();
    // Set device buttons for those PTs specified in RDF, when these are explicitly set.
    // When set to <none>, i.e. no button, the defaults set above apply.
    if ( pt.indexOf( 'V' ) >= 0 && volumePT != DeviceButton.noButton )
    {  
      hex.set( ( short )( hex.getData()[ 2 ] == 0xFF ? 0xFF : volumePT.getButtonIndex() ), 6 );         
    }
    if ( pt.indexOf( 'T' ) >= 0 && transportPT != DeviceButton.noButton )
    {  
      hex.set( ( short )( hex.getData()[ 2 ] == 0xFF ? 0xFF : transportPT.getButtonIndex() ), 7 );         
    }
    if ( pt.indexOf( 'C' ) >= 0 && channelPT != DeviceButton.noButton )
    {  
      hex.set( ( short )( hex.getData()[ 2 ] == 0xFF ? 0xFF : channelPT.getButtonIndex() ), 8 );         
    }
    if ( pt.indexOf( 'X' ) >= 0 && xPT != DeviceButton.noButton )
    {  
      hex.set( ( short )( hex.getData()[ 2 ] == 0xFF ? 0xFF : xPT.getButtonIndex() ), 9 );         
    }
    if ( pt.indexOf( 'Y' ) >= 0 && inputPT != DeviceButton.noButton )
    {  
      hex.set( ( short )( hex.getData()[ 2 ] == 0xFF ? 0xFF : inputPT.getButtonIndex() ), 10 );         
    }
    if ( pt.indexOf( 'Z' ) >= 0 && zPT != DeviceButton.noButton )
    {  
      hex.set( ( short )( hex.getData()[ 2 ] == 0xFF ? 0xFF : zPT.getButtonIndex() ), 11 );         
    }
  }
  
  public void store( PropertyWriter pw )
  {
    if ( rfSelectors == null || rfSelectors.length == 0 )
      return;    
    pw.print( "DeviceIndex", buttonIndex );
    for ( int i = 0; i < rfSelectors.length; i++ )
    {
      RFSelector rfSel = rfSelectors[ i ];
      String value = rfSel.btn.getKeyCode() + "|" + rfSel.irDevType.getName() + "|" 
          + rfSel.irCode + "|" + rfSel.rfDevType.getName() + "|" + rfSel.rfCode;
      pw.print( "Selector." + i, value );
    }
  }
}

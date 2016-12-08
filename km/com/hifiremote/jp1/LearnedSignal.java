package com.hifiremote.jp1;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.hifiremote.decodeir.DecodeIRCaller;

// TODO: Auto-generated Javadoc
/**
 * The Class LearnedSignal.
 */
public class LearnedSignal extends Highlight
{

  /**
   * Instantiates a new learned signal.
   * 
   * @param keyCode
   *          the key code
   * @param deviceButtonIndex
   *          the device button index
   * @param data
   *          the data
   * @param notes
   *          the notes
   */
  public LearnedSignal( int keyCode, int deviceButtonIndex, int format, Hex data, String notes )
  {
    this.keyCode = keyCode;
    this.deviceButtonIndex = deviceButtonIndex;
    this.format = format;
    this.data = data;
    this.notes = notes;
  }

  public LearnedSignal( LearnedSignal signal )
  {
    keyCode = signal.keyCode;
    deviceButtonIndex = signal.deviceButtonIndex;
    format = signal.format;
    header = signal.header == null ? null : new Hex( signal.header );
    name = signal.name;
    data = new Hex( signal.data );
    notes = signal.notes;
    setSegmentFlags( signal.getSegmentFlags() );

    unpackLearned = signal.unpackLearned;
    if ( signal.decodes != null )
    {
      List< LearnedSignalDecode > signalDecodes = signal.getDecodes();
      decodes = new ArrayList< LearnedSignalDecode >( signalDecodes.size() );
      for ( LearnedSignalDecode decode : signalDecodes )
      {
        decodes.add( new LearnedSignalDecode( decode ) );
      }
    }

    timingAnalyzer = signal.getTimingAnalyzer();
  }

  public static LearnedSignal read( HexReader reader, Remote remote )
  {
    // Only used with format=0
    if ( reader.peek() == remote.getSectionTerminator() )
    {
      return null;
    }
    if ( reader.available() < 4 )
    {
      return null;
    }
    int keyCode = reader.read();
    int type = reader.read();
    int deviceButtonIndex = 0;
    if ( remote.hasDeviceSelection() )
    {
      if ( remote.getLearnedDevBtnSwapped() )
      {
        deviceButtonIndex = type & 0x0F;
      }
      else
      {
        deviceButtonIndex = type >> 4;
      }
    }
    int length = reader.read();
    short[] data = reader.read( length );

    return new LearnedSignal( keyCode, deviceButtonIndex, 0, new Hex( data ), null );
  }

  /**
   * Instantiates a new learned signal.
   * 
   * @param properties
   *          the properties
   */
  public LearnedSignal( Properties properties )
  {
    super( properties );
    keyCode = Integer.parseInt( properties.getProperty( "KeyCode" ) );
    deviceButtonIndex = Integer.parseInt( properties.getProperty( "DeviceButtonIndex" ) );
    format = Integer.parseInt( properties.getProperty( "Format", "0" ) );
    String temp = properties.getProperty( "Header" );
    if ( temp != null )
    {
      header = new Hex( temp );
    }
  }
  
  public String getSignalHexText()
  {
    // If format != 0, convert to format 0.  Return the format 0 data, without any header
    Hex hex = new Hex( data );
    if ( format > 0 )
    {
      ProntoSignal ps = new ProntoSignal( this );
      if ( ps.error == null )
      {
        hex = ps.makeLearned( 0 ).getData();
      }
      if ( ps.error != null )
      {
        return "This signal cannot be displayed in standard UEI learned signal form.  "
            + "Select Pronto format to clone or copy this signal.";
      }
    }
    return hex.toString();
  }

  public int getSize()
  {
    return data.length() + 3; // keyCode, deviceButtonIndex, length, data
  }

  /**
   * Store.
   * 
   * @param pw
   *          the pw
   */
  public void store( PropertyWriter pw )
  {
    super.store( pw );
    pw.print( "KeyCode", keyCode );
    pw.print( "DeviceButtonIndex", deviceButtonIndex );
    if ( format > 0 )
    {
      pw.print( "Format", format);
    }
    if ( header != null )
    {
      pw.print( "Header", header );
    }
  }

  /** The key code. */
  private int keyCode;

  /**
   * Gets the key code.
   * 
   * @return the key code
   */
  public int getKeyCode()
  {
    return keyCode;
  }

  /**
   * Sets the key code.
   * 
   * @param code
   *          the new key code
   */
  public void setKeyCode( int code )
  {
    keyCode = code;
  }
  
  public void setName( String name )
  {
    this.name = name;
  }
  
  public void setNotes( String notes )
  {
    if ( notes != this.notes && ( notes == null || !notes.equals( this.notes ) ) )
    {
      this.notes = notes;
    }
  }
  
  public String getSignalName( Remote remote )
  {
    String sName = name == null || name.isEmpty() ? notes : name;
    return sName == null || sName.isEmpty() ? remote.getButton( keyCode ).getName() : sName;
  }
  
  public int getFormat()
  {
    return format;
  }

  public void setFormat( int format )
  {
    this.format = format;
  }
  
  /** The 7-byte header of an XSight Touch learned signal,
   * not yet understood.
   */
  private Hex header = null;

  public Hex getHeader()
  {
    return header;
  }

  public void setHeader( Hex header )
  {
    this.header = header;
  }

  /**
   * Format is 0 for original learned signal format,
   * 1 for the format used by remotes with Maxim processors
   * 2 for the format used in TI CC2541 processors
   */
  private int format = 0;

  public int store( short[] buffer, int offset, Remote remote )
  {
    // Only used when remote does not have segments
    buffer[ offset++ ] = ( short )keyCode;
    if ( remote.getLearnedDevBtnSwapped() )
    {
      buffer[ offset ] = ( short )( 0xFF & ( deviceButtonIndex | 0x20 ) );
    }
    else
    {
      buffer[ offset ] = ( short )( 0xFF & ( deviceButtonIndex << 4 | 2 ) );
    }
    ++offset;
    int dataLength = data.length();
    buffer[ offset++ ] = ( short )dataLength;
    Hex.put( data, buffer, offset );

    return offset + dataLength;
  }

  /** The unpack learned. */
  private UnpackLearned unpackLearned = null;

  /**
   * Gets the unpack learned.
   * 
   * @return the unpack learned
   */
  public UnpackLearned getUnpackLearned()
  {
    if ( unpackLearned == null )
    {
      unpackLearned = new UnpackLearned( data, format );
    }
    return unpackLearned;
  }

  /** The decodes. */
  private ArrayList< LearnedSignalDecode > decodes = null;

  /**
   * Gets the decodes.
   * 
   * @return the decodes
   */
  public ArrayList< LearnedSignalDecode > getDecodes()
  {
    if ( decodes == null )
    {
      UnpackLearned ul = getUnpackLearned();
      if ( !ul.ok )
      {
        return null;
      }
      getDecodeIR();
      decodeIR.setBursts( ul.durations, ul.repeat, ul.extra );
      decodeIR.setFrequency( ul.frequency );
      decodeIR.initDecoder();
      decodes = new ArrayList< LearnedSignalDecode >();
      while ( decodeIR.decode() )
      {
        decodes.add( new LearnedSignalDecode( decodeIR ) );
      }
    }
    return decodes;
  }

  private LearnedSignalTimingAnalyzer timingAnalyzer = null;
  public LearnedSignalTimingAnalyzer getTimingAnalyzer()
  {
    if ( timingAnalyzer == null )
      timingAnalyzer = new LearnedSignalTimingAnalyzer( getUnpackLearned() );
    return timingAnalyzer;
  }
  public void clearTimingAnalyzer()
  {
    timingAnalyzer = null;
    unpackLearned = null;
    decodes = null;
  }

  public static boolean hasDecodeIR()
  {
    if ( hasDecodeIR == 0 )
      getDecodeIR();
    return ( hasDecodeIR == 2 );
  }
  public static String getDecodeIRVersion()
  {
    return ( hasDecodeIR() ? getDecodeIR().getVersion() : null );
  }

  /**
   * Gets the decode ir.
   * 
   * @return the decode ir
   */
  private static DecodeIRCaller getDecodeIR()
  {
    if ( decodeIR == null )
    {
      try
      {
        decodeIR = new DecodeIRCaller( RemoteMaster.getWorkDir() );
        hasDecodeIR = 2; // yes
      }
      catch ( UnsatisfiedLinkError ule )
      {
        System.err.println( "Failed to load DecodeIR JNI interface!" );
        hasDecodeIR = 1; // no
      }
    }

    return decodeIR;
  }

  /** The decode ir. */
  private static DecodeIRCaller decodeIR = null;
  private static int hasDecodeIR = 0;
}

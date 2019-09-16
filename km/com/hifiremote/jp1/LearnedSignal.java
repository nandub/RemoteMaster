package com.hifiremote.jp1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.harctoolbox.analyze.Analyzer;
import org.harctoolbox.analyze.Cleaner;
import org.harctoolbox.analyze.NoDecoderMatchException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.Decoder.Decode;
import org.harctoolbox.irp.Decoder.DecodeTree;
import org.harctoolbox.irp.Decoder.DecoderParameters;
import org.harctoolbox.irp.Decoder.TrunkDecodeTree;
import org.harctoolbox.irp.Expression;
import org.harctoolbox.irp.InvalidNameException;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.NameEngine;
import org.harctoolbox.irp.NameUnassignedException;

import com.hifiremote.decodeir.DecodeIRCaller;
import com.hifiremote.jp1.LearnedSignalDecode.Executor;
import com.hifiremote.jp1.LearnedSignalDecode.Executor.Selector;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

// TODO: Auto-generated Javadoc
/**
 * The Class LearnedSignal.
 */
public class LearnedSignal extends Highlight
{
/*
  public static void main(String[] args) { 
    try 
    { 
        String s = "  Cat_Dog  Mouse-123*%$_Rat( Sausage)dog";
        System.out.println( LearnedSignalDecode.getMatchName( s ));
      
        s = "DXT:U? 1 2 AB Cd_>";
        System.out.println( LearnedSignalDecode.Executor.getSelector( s ) );
        
        Executor ex = new Executor();
        ex.parms.devParms = Arrays.asList( "D:2? 2 AX", "X", "Y:V?5", "C4?2BB", "CC?3A", "CD?4BB" );
        ex.setSelectors();
        for ( String key : ex.selectorList.keySet() )
        {
          List< Integer > l = ex.selectorList.get( key );
          System.out.print( key + ": " );
          for ( Integer i : l) System.out.print( "" + i + " " );
          System.out.println();
        }
       
        String expr = "D: 3 (? ?B B) 7 ??BB? 2 AX";
        System.out.println( "Processed expression: " + ex.preprocess( expr, 1 ));
        
      
        IrSignal irSignal = Pronto.parse(
            "0000 006D 001A 0000 0157 00AC 0013 0055 0013 00AC 0013 0055 0013 00AC 0013 00AC 0013 0055 0013 0055 0013 0055 0013 0055 0013 0055 0013 0055 0013 0055 0013 00AC 0013 0055 0013 00AC 0013 0055 0013 0498 0157 0055 0013 0D24 0157 0055 0013 0D24 0157 0055 0013 0D24 0157 0055 0013 1365"
            );
        IrpDatabase irpDatabase = new IrpDatabase((String) null); 
        System.out.println("We have " + irpDatabase.size() + " protocols in the data base!"); 
        System.out.println("PID: " + irpDatabase.getFirstProperty( "Aiwa", "uei-executor" ));
        
        Decoder decoder = new Decoder(irpDatabase); 
        Map<String, Decode> sigDecodes = decoder.decodeIrSignal(irSignal); 
        for (Decode decode :  sigDecodes.values()) 
            System.out.println(decode); 
    } catch (Exception ex) { 
        ex.printStackTrace(); 
    } 
} 
*/
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
    Hex hex = new Hex( data );
    // If format not 0 or 4, convert to format 0.  Return the format 0 data, without any header.
    boolean convert = format > 0 && format < 4;
    if ( !convert )
    {
      // Formats 0 and 4.  Convert these if they use ROM burst table entries beyond map 17,
      // as not all remotes support these high frequency entries.  Formats 0 and 4 are identical
      // except for signals that use these entries.  Conversion replaces ROM burst table data
      // by burst table embedded in signal hex.
      int burstNum = hex.getData()[ 2 ];
      if ( ( burstNum & 0x80 ) != 0 && ( burstNum & 0x1F ) > 17 )
        convert = true;
    }
    
    if ( convert )
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
   * 3 for the new format used in the Inteset 422-3
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
  private LearnedSignalDecode preferredLSDecode = null;
  private boolean usingDecodeIR = false;

  public LearnedSignalDecode getPreferredLSDecode()
  {
    return preferredLSDecode;
  }

  public void setPreferredLSDecode( LearnedSignalDecode preferredLSDecode )
  {
    this.preferredLSDecode = preferredLSDecode;
  }

  /**
   * Gets the decodes.
   * 
   * @return the decodes
   * @throws InvalidNameException 
   */
  public ArrayList< LearnedSignalDecode > getDecodes()
  {
    boolean nowUsingDecodeIR = LearnedSignal.hasDecodeIR()
        && Boolean.parseBoolean( RemoteMaster.getProperties().getProperty( "UseDecodeIR", "false" ) );
    boolean decoderChanged = decodes == null ? false : usingDecodeIR != nowUsingDecodeIR;
    usingDecodeIR = nowUsingDecodeIR;
      
    if ( decodes == null || decoderChanged )
    {
      UnpackLearned ul = getUnpackLearned();
      if ( !ul.ok )
      {
        return null;
      }
      
      decodes = new ArrayList< LearnedSignalDecode >();

      if ( usingDecodeIR )
      {
        getDecodeIR();
        decodeIR.setBursts( ul.durations, ul.repeat, ul.extra );
        decodeIR.setFrequency( ul.frequency );
        decodeIR.initDecoder();

        while ( decodeIR.decode() )
        {
          decodes.add( new LearnedSignalDecode( decodeIR ) );
        }
      }     
      else
      {
        try
        {
          if ( ( tmDecoder = getTmDecoder() ) == null )
          {
            return null;
          }

          IrSignal irSignal = new IrSignal( ul.durations, ul.oneTime, ul.repeat, ul.frequency );
          Map<String, Decode> sigDecodes = tmDecoder.decodeIrSignal( irSignal, tmDecoderParams );
          
          if ( sigDecodes == null || sigDecodes.size() == 0 )
          {
            int[] dur = Arrays.copyOfRange( ul.durations, ul.oneTime, ul.durations.length);            
            IrSignal irSignal2 = new IrSignal( dur, 0, ul.repeat, ul.frequency);
            sigDecodes = tmDecoder.decodeIrSignal( irSignal2, tmDecoderParams );
          }
          
          if ( ( sigDecodes == null || sigDecodes.size() == 0 ) 
              && ( irSignal.introOnly() || irSignal.repeatOnly() ) )
          { 
            ModulatedIrSequence sequence = irSignal.toModulatedIrSequence();
            //sequence = Cleaner.clean(sequence, IrCoreUtils.DEFAULT_ABSOLUTE_TOLERANCE, IrCoreUtils.DEFAULT_RELATIVE_TOLERANCE);
            
            //DecodeTree sDecodes =  tmDecoder.decode( sequence, tmDecoderParams );
            Iterator<  TrunkDecodeTree > it = tmDecoder.decode( sequence, tmDecoderParams ).iterator();
            sigDecodes = new Hashtable< String, Decode >();
            while ( it.hasNext() )
            {
              TrunkDecodeTree tree = it.next();
              sigDecodes.put( tree.getName(), tree.getTrunk() );
//              System.err.println( tree.toString() );
            }            
          }

          for ( Decode dc : sigDecodes.values() )
          {
            //System.out.println( dc );
            LearnedSignalDecode lsd = new LearnedSignalDecode( dc );
            if ( lsd.decode != null )
              decodes.add( lsd );
          };

        }
        catch ( InvalidArgumentException e )
        {
          System.err.println( "*** Error: Invalid argument in IrSignal" );
          return null;
        }
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
      System.err.println( "Using DecodeIR to decode Learned Signals" );
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
  
  public static Decoder getTmDecoder()
  {
    if ( tmDecoder == null )
    {
      try
      {  
        //tmDatabase = new IrpDatabase( ( String )null ); 
        tmDatabase = new IrpDatabase( new File( RemoteMaster.getWorkDir(), "IrpProtocols.xml" ) );
        tmDecoder = new Decoder( tmDatabase ); 
        tmDecoderParams = new Decoder.DecoderParameters(); 
        tmDecoderParams.setRemoveDefaultedParameters( false );
        tmDecoderParams.setIgnoreLeadingGarbage( true );
      }
      catch ( IOException ioe )
      {
        System.err.println( "*** Error: Unable to open protocol database" );
        return null;
      }
      catch ( IrpParseException ipe )
      {
        System.err.println( "*** Error: Unable to parse protocol database" );
        return null;
      }
    }
    return tmDecoder;
  }

  public static IrpDatabase getTmDatabase()
  {
    return tmDatabase;
  }

  /** The decode ir. */
  private static DecodeIRCaller decodeIR = null;
  private static int hasDecodeIR = 0;
  private static Decoder tmDecoder = null;
  private static IrpDatabase tmDatabase = null;
  private static DecoderParameters tmDecoderParams = null;
  //private IrSignal irSignal = null;
}

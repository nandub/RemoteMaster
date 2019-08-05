package com.hifiremote.jp1.extinstall;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import com.hifiremote.jp1.AddressRange;
import com.hifiremote.jp1.CheckSum;
import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.ProgressUpdater;
import com.hifiremote.jp1.Remote;
import com.hifiremote.jp1.RemoteConfiguration;
import com.hifiremote.jp1.RemoteManager;
import com.hifiremote.jp1.XorCheckSum;

public class RMWavConverter
{
  private int bitNum = 0;
  private int bitValue = 1;
  private byte byteValue = 0;
  private int byteNum = 0;
  private int byteCount = 2048;
  private int numBytes = 0;
  private short[] data = null;
  private int filePos = 0;
  private int lastPhasePos = 1;
  private int lowPos = 46;
  private int highPos = 56;
  private WavePeak wp = WavePeak.WPNONE;
  private RemoteConfiguration config = null;
  private Remote remote = null;
  private List< Remote > remotes = null;
  private String sigString = null;
  private IrHexConfig wavConfig = null;  
  private String progressName = null;
  private ProgressUpdater progressUpdater = null;
  
  private enum WavePeak
  {
    WPLOW, WPNONE, WPHIGH
  }
  
  public RMWavConverter( RemoteConfiguration config )
  {
    this.config = config;
    remote = config != null ? config.getRemote() : null;
  }
  
  public void exportWav( File file, AddressRange range )
  {
    int baseAddr = remote.getBaseAddress();
    int offset = range.getStart();
    short[] data = config.getData();
    int len = data.length;
    int[] img = new int[ len ];
    for ( int i = 0; i < len; i++ )
      img[ i ] = data[ i ];
    boolean isHCS08 = remote.getProcessor().getName().equals( "HCS08" );
    if ( isHCS08 && needsCheckSum( range, img ) )
      offset -= 2;
    int upStart = baseAddr + offset;
    int upSize = range.getFreeStart() - offset;
    String sig = remote.getSignature().substring( 0, 4 );
    IRToWav irToWav = new IRToWav( sig, upStart, upSize, offset, isHCS08, img );
    irToWav.setProgressUpdater( progressUpdater );
    progressName = "SAVING WAV SOUND FILE:";
    irToWav.generate( file );
  }
  
  private boolean needsCheckSum( AddressRange range, int[] img )
  {
    // If range for Wav file is preceded by a checksum, calculate the
    // checksum required when remainder of checksum range filled with FF
    for ( CheckSum ckSum : remote.getCheckSums() )
    {
      AddressRange ckRange = ckSum.getAddressRange();
      int addr = ckSum.getCheckSumAddress();
      if ( ckRange.getStart() == range.getStart() 
          && ckRange.getStart() == addr + 2  && ( ckSum instanceof XorCheckSum ) )
      {
        short val = ( ( XorCheckSum )ckSum ).calculateCheckSum( config.getData(), 
            range.getStart(), range.getFreeStart() - 1 );
        // If odd number of bytes left in range, include one FF byte.  Note that the
        // end address is inclusive, so test is for 0 not 1.
        if ( ( ( range.getEnd() - range.getFreeStart() ) % 2 ) == 0 )
          val ^= 0xFF;
        img[ addr ] = val;
        img[ addr + 1 ] = val ^ 0xFF;
        return true;
      }
    }
    return false;
        
  }
  
  public void importWav( File file )
  {
    int baseAddress = 0;
    BufferedInputStream in = null;
    progressName = "IMPORTING WAV FILE:";
    try
    {
      data = new short[ byteCount ];
      in = new BufferedInputStream( new FileInputStream( file ) );

      // Read and interpret the WAV file into data byte array
      in.skip( 45 );            // Skip over header in Wav
      int val = 0;
      int prog = 0;
      int newProg = 0;
      int incr = ( int )( file.length() / 100 + 1 );
      for ( filePos = 45; filePos < file.length(); filePos++ )
      {
        if ( progressUpdater != null )
        {
          newProg = filePos / incr;
          if ( newProg > prog )
          {
            progressUpdater.updateProgress( newProg );
            prog = newProg;
          }
        }
        
        val = in.read();
        if ( val > 0xF0 )
          wp = WavePeak.WPHIGH; // Wave peak
        else if ( val < 0x10 )
          wp = WavePeak.WPLOW;  // Wave pit
        else
        {
          if ( wp == WavePeak.WPHIGH )
          {
            if ( filePos - highPos < 6 )
              phaseChange();
            highPos = filePos;
          }
          else if ( wp == WavePeak.WPLOW )
          {
            if ( filePos - lowPos < 6 )
              phaseChange();
            lowPos = filePos;
          }
          wp = WavePeak.WPNONE;
        }
      }
      setBit(); // Store last byte
    }
    catch ( Exception e )
    {
      System.err.println( "Error interpreting WAV file " + file.getAbsolutePath() );
    }
    
    try
    {
      in.close();
    }
    catch ( IOException e ) {}
    
    // Decode the data as an ExtInstall .hex string
    progressName = "DECODING WAV IMPORT:";
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter( sw );
    boolean isJP12 = false;
    String message = null;
    String title = "WAV import";
    int prog = 0;
    int newProg = 0;
    while ( byteNum < numBytes )
    {
      if ( progressUpdater != null )
      {
        newProg = ( byteNum * 100 ) / numBytes;
        if ( newProg > prog )
        {
          progressUpdater.updateProgress( newProg );
          prog = newProg;
        }
      }

      while ( data[ byteNum++ ] == 0xA8 ) {}    // Skip $A8 separators and following $00
      int blockLen = data[ byteNum++ ];
      int val = ( int )data[ byteNum ];
      switch ( val )
      {
        case 0xFF:
        case 0x00:
          break;      // Lead-in blocks
        case 0x45:
          break;      // Terminator
        case 0x43:
          isJP12 = true;
          break;
        case 0x56:    // Header block
          char[] ch = new char[ 4 ];
          for ( int i = 0; i < 4; i++ )
            ch[ i ] = ( char )data[ byteNum + 2 + i ];
          sigString = new String( ch );

          if ( ( remote != null && !remote.getSignature().substring( 0, 4 ).equals( sigString ) )
              || remote == null )
          {
            if ( remote != null )
            {
              message = "The data being merged comes from a different model of\n " +
                  "remote.  Depending on how different that remote is,\n" +
                  "merging may cause the remote to operate incorrectly\n" +
                  "or not at all.  Are you sure you want to do this?";
              if ( message != null && JOptionPane.showConfirmDialog( null, message, title,
                  JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) != JOptionPane.YES_OPTION )
                return;
              
              // The base address for the import must be that for the signature of the
              // imported data, so clear the current, incorrect, remote.
              remote = null;
            }
          
            // At this point either a new image is requested or there is a mismatch
            // of signature between the existing image and the imported data.  If the
            // imported data does not contain a signature then we can merge into it if
            // the user has accepted the mismatch.  But if the imported data contains the
            // mismatched signature then it must be treated as a new image.  At present
            // we do not know if a signature is present or not, so create a list of
            // remotes for this signature in any case.
            remotes = new ArrayList< Remote >();
            RemoteManager rm = RemoteManager.getRemoteManager();
            for ( Remote r : rm.getRemotes() )
            {
              if ( r.getSignature().substring( 0, 4 ).equals( sigString ) && r.supportWaveUpgrade() )
              {
                remotes.add( r );
              }
            }

            if ( remotes.isEmpty() )
            {
              message = "No remote found for signature starting " + sigString + " from WAV file.";
              JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
              return;
            }
     
            remote = remotes.get( 0 );
          }
          // In all cases, remote is now one with the signature for the import.
          baseAddress = remote.getBaseAddress();
          break;
        case 0x53:    // Data block
          int addr = ( data[ byteNum + 2 ] << 8 ) + data[ byteNum + 3 ];
          if ( isJP12 ) addr -= baseAddress;
          String s = String.format( "%04X: ", addr ) + ( new Hex( Arrays.copyOfRange( data, byteNum + 4, byteNum + data[byteNum + 1 ] + 1 )));
          pw.println( s );
          break;
        default:
          message = "Unknown block type in WAV file";
          JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
          return;
      }
      byteNum += blockLen - 1;
    }
    
    CrudeErrorLogger erl = new CrudeErrorLogger();
    wavConfig = new IrHexConfig();
    StringReader sr = new StringReader( sw.toString() );
    BufferedReader bsr = new BufferedReader( sr );
    try
    {
      wavConfig.Load( erl, bsr );
    }
    catch ( IOException e )
    {
      e.printStackTrace();
    }
    
    String importedSig = getImportedSignature();
    if ( importedSig != null && !importedSig.substring( 0, 4 ).equals( sigString ) )
    {
      message = "The imported data contains inconsistent signatures.";
      JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
      return;
    }
  }
  
  
  private void setBit()
  {
    if ( bitNum < 8 )
    {
      // Data bit
      if ( bitValue != 0 )
        byteValue |= bitValue << bitNum;
      bitNum++;
    }
    else
    {
      // Parity bit
      if ( numBytes >= byteCount )
      {
        byteCount += 2048;
        data = Arrays.copyOf( data, byteCount );
      }
      data[ numBytes++ ] = ( short )( byteValue & 0xFF );
      byteValue = 0;
      bitNum = 0;
    }
  }
  
  private void phaseChange()
  {
    // Find how many no-phase changes occurred since last time
    int phaseBits = ( filePos - lastPhasePos ) / 70;
    lastPhasePos = filePos;
    if ( phaseBits > 0 )
    {
      for ( int i = 0; i < phaseBits - 1; i++ )
        setBit();
      bitValue ^= 1;
      setBit();
    }
  }

  public void mergeData( short[] data )
  {
    for ( int i = 0; i < data.length && i < wavConfig.size(); i++ )
    {
      IrHex irh = wavConfig.get( i );
      if ( irh != null && irh.isValid() )
        data[ i ] = irh.get();
    }
  }
  
  public String getImportedSignature()
  {
    int[] vals = new int[ 10 ];
    for ( int i = 0; i < 10; i++ )
    {
      vals[ i ] = wavConfig.get( i ) == null ? -1 : wavConfig.get( i ).get() & 0xFF;
    }
    int start = vals[ 0 ] == -1 || vals[ 1 ] == -1 || vals[ 0 ] + vals[ 1 ] == 0xFF ? 2 : 0;
    short[] sigData = new short[ 8 ];
    for ( int i = 0; i < 8; i++ )
    {
      if ( vals[ start + i ] == -1 )
        return null;
      else
        sigData[ i ] = ( short )vals[ start + i ];
    }
    return Hex.getRemoteSignature( sigData );
  }
  
  public Remote getRemote()
  {
    if ( remotes != null )
    {
      remote = RemoteConfiguration.filterRemotes( remotes, sigString, wavConfig.size(), null, null, true );
      remotes = null;
    }
    return remote;
  }
  
  public void setProgressUpdater( ProgressUpdater progressUpdater )
  {
    this.progressUpdater = progressUpdater;
  }  
  
  public String getProgressName()
  {
    return progressName;
  }
}

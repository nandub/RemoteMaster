package com.hifiremote.jp1;

// TODO: Auto-generated Javadoc
/**
 * The Class UnpackLearned.
 */
public class UnpackLearned
{

  /** The ok. */
  public boolean ok;

  /** The error. */
  public String error;

  /** The frequency. */
  public int frequency;

  /** The bursts. */
  public int[] bursts;

  /** The durations. */
  public int[] durations;

  /** The one time. */
  public int oneTime;

  /** The repeat. */
  public int repeat;

  /** The extra. */
  public int extra;
  /** The parts. */
  public int[] parts;

  /** The part types. */
  public boolean[] partTypes;

  /**
   * Instantiates a new unpack learned.
   * 
   * @param hex
   *          the hex
   */
  public UnpackLearned( Hex hex, int format )
  {
    /*
     * There are 5 formats (0-4) used for the hex data of learned signals.  They differ in
     * the format used for burst tables, in the unit used for carrier period and in one case
     * also in the location of the carrier period within the hex data.
     * 
     * In all cases a burst value takes 4 bytes (8 nibbles), but in formats 0 and 4 the ON and OFF
     * durations are each 4 nibbles while in formats 1-3 the ON duration is 3 nibbles and OFF
     * duration is 5 nibbles.  The only distinction between formats 0 and 4 is that they use a
     * different burst table in ROM.
     * 
     * Units used are as follows:
     *   Formats 0, 4:  Carrier period in units of the period of an 8MHz clock, ON and OFF durations
     *      both in units of 2us (= 16 * 8MHz clock period).
     *   Format 1:  Carrier period in units of the period of a 12MHz clock, ON durations in 
     *      units of carrier period, OFF durations in units of 4/3 us (= 16 * 12MHz clock period).
     *   Format 2:  Carrier period in units of the period of a 4MHz clock, ON and OFF durations
     *      both in units of carrier period.
     *   Format 3:  Carrier period in units of the period of a 10MHz clock, ON durations in 
     *      units of carrier period, OFF durations in units of 2us.
     *      
     * There is an exception to this for unmodulated signals.  In these cases format 1
     * uses 4/3us as unit for both ON and OFF durations, other formats all use 2us for both
     * durations.  The period is usually given as 0 but a small nonzero value has also been seen
     * which makes the unit for a modulated signal of that period be the correct unit for an 
     * unmodulated signal. 
     * 
     * A further difference between formats is in the bit that selects whether the burst table is
     * embedded in the signal or is in a table in ROM.  Format 3 uses bit 6 to flag the ROM table,
     * other formats use bit 7.
     */
    
    if ( format > 4 )
    {
      ok = false;
      error = "Format=" + format + " not supported";
      return;
    }
    ok = true;
    error = "";
    if ( hex == null || hex.length() < 5 )
    {
      ok = false;
      error = "hex learned signal too short to unpack";
      return;
    }
    
    int period = hex.get( format == 3 ? 1 : 0 );
    frequency = period == 0 || period == zeroPeriods[ format ] ? 0 : (int)( ( clockMHz[ format ] * 1000000.0 ) / period + 0.5 );
    
    int offset = loadBurstTable( hex, format );
    if ( ok )
    {
      loadDurations( hex, offset );
    }
  }

  public String toString()
  {
    return durationsToString( bursts, "" );
  }

  public static String durationsToString( int[] data, String sep )
  {
    StringBuilder str = new StringBuilder();
    if ( data != null && data.length != 0 )
    {
      boolean isSigned = false;
      for ( int d: data )
        if ( d < 0 )
        {
          isSigned = true;
          break;
        }

      for ( int i = 0; i < data.length; i++ )
      {
        if ( i > 0 )
          str.append( ' ' );
        if ( !isSigned )
          str.append( ( i & 1 ) == 0 ? "+" : "-" );
        else if ( data[i] > 0 )
            str.append( '+' );
        str.append( data[i] );
        if ( i > 0 && ( i % 2 ) == 1 )
          str.append( sep );
      }
    }
    if ( str.length() == 0 )
      return "** No signal **";

    return str.toString();
  }
  
  /**
   * Returns the units in microseconds for ON and OFF bursts for given format 
   * and carrier frequency.
   */
  public static double[] getBurstUnits( int format, int freq )
  {
    if ( freq == 0 )
    {
      double mult = format == 1 ? 4.0 / 3.0 : 2.0;
      return new double[]{ mult, mult };
    }

    double multOn = format == 0 || format == 4 ? 2.0 : 1000000.0 / freq;
    double multOff = ( new double[]{ 2.0, 4.0/3.0, multOn, 2.0, 2.0 } )[ format ];
    return new double[]{ multOn, multOff };
  }

  private int[] getBurstTimes( int[] val, int format )
  {
    int[] times = new int[ 2 ];

    boolean split35 = format == 1 || format == 2 || format == 3;
    double[] mult = getBurstUnits( format, frequency );
    int on = val[ 0 ] >> ( split35 ? 4 : 0 );
    times[ 0 ] = (int)( (double)on * mult[ 0 ] + 0.5 );
    int off =  val[ 1 ] + ( split35 ? ( val[ 0 ] & 0x0F ) << 16 : 0 );
    times[ 1 ] = (int)( (double)off * mult[ 1 ] + 0.5 );
    return times;
  }
  
  /**
   * Load burst table.
   * Returns the offset in hex data to byte beyond burst table.
   */
  private int loadBurstTable( Hex hex, int format )
  {
    int burstNum = hex.getData()[ format == 3 ? 0 : 2 ];
    // Code in URC-7781 indicates that bits 5-7 of burstNum are potential flags, bits 0-4
    // are the number of bursts if burst table is embedded in signal or are a map number
    // if burst table is in ROM.  This is consistent with the newer burst table actually
    // only using 0-31 for map numbers despite having 46 entries.  Maps 18-31 are used for
    // learning but the corresponding entry in 32-45 is used for sending.
    int mask = format == 3 ? 0x40 : 0x80;  // Mask for bit that selects ROM burst table
    int result;
    if ( ( burstNum & mask ) != 0 )
    {
      result = 3;
      int[] romBursts = format == 0 ? romBurstsA : romBurstsB;
      int[] romIndex = format == 0 ? romIndexA : romIndexB;
      int[] romPeriods = format == 0 ? romPeriodsA : romPeriodsB;
      burstNum &= 0x1F;
      // Map numbers > 17 are used for high frequency learned signals (around 400MHz or more)
      // where a different map is used for sending than for learning.  The offset is 3 for
      // format 0 where the ROM table has 24 entries and is 14 for the other formats where
      // the ROM table has 46 entries.
      if ( format == 0 && burstNum >= 21 )
      {
        // In format 0, map numbers 21-23 are used for sending but do not occur in signals.
        // For other formats, valid map numbers are 0-31 and due to the mask, burstNum cannot
        // exceed this.
        ok = false;
        error = "ROM burst index out of range";
        return 0;
      }
      if ( burstNum >= 18 )
      {
        frequency = (int)( 8000000.0 / romPeriods[ burstNum - 18 ] + 0.5 );
        burstNum += ( format == 0 ? 3 : 14 );
      }
      burstNum = romIndex[ burstNum ];
      int count = romBursts.length - burstNum;
      if ( count > 32 )
        count = 32;
      bursts = new int[ count ];
      while ( count > 1 )
      {
        count -= 2;
        int[] val = new int[]{ romBursts[ count + burstNum ], romBursts[ count + burstNum + 1 ] };
        int[] times = getBurstTimes( val, romBursts == romBurstsA ? 0 : 3 );
        bursts[ count ] = times[ 0 ];
        bursts[ count + 1 ] = times[ 1 ];
      }
    }
    else if ( ( burstNum & 0xE0 ) != 0 )
    {
      ok = false;
      error = "burst number byte has unexpected flag";
      return 0;
    }
    else if ( burstNum != 0 )
    {
      result = burstNum * 4 + 3;
      if ( result >= hex.length() )
      {
        ok = false;
        error = "burst table extends beyond end of hex";
        return 0;
      }
      bursts = new int[ burstNum * 2 ];
      for ( int i = 0; i < burstNum; i++ )
      {
        int[] val = new int[]{ hex.get( 4 * i + 3 ), hex.get( 4 * i + 5 ) };
        int[] times = getBurstTimes( val, format );
        bursts[ 2 * i ] = times[ 0 ];
        bursts[ 2 * i + 1 ] = times[ 1 ];
      }
    }
    else
    {
      ok = false;
      error = "00 found where burst table expected";
      return 0;
    }
    return result;
  }

  /**
   * Load durations.
   * 
   * @param hex
   *          the hex
   * @param offset
   *          the offset
   */
  private void loadDurations( Hex hex, int offset )
  {
    int partNdx = 0;
    int total = 0;
    for ( int ndx = offset; ndx != hex.length(); ++partNdx )
    {
      int count = ( hex.getData()[ ndx ] & 0x7F );
      if ( count == 0 )
      {
        ok = false;
        error = "burst index count is zero";
        return;
      }
      total += count * 2;
      ndx += ( count + 3 ) >> 1;
      if ( ndx > hex.length() )
      {
        ok = false;
        error = "duration list extends beyonds hex data";
        return;
      }
    }
    durations = new int[ total ];
    parts = new int[ partNdx ];
    partTypes = new boolean[ partNdx ];
    total = 0;
    partNdx = 0;
    for ( int ndx = offset; ndx != hex.length(); ++partNdx )
    {
      int count = hex.getData()[ ndx ];
      partTypes[ partNdx ] = ( count & 0x80 ) != 0;
      count &= 0x7F;
      parts[ partNdx ] = count;
      ++ndx;
      for ( int n = 0; n < count; ++n )
      {
        int x = hex.getData()[ ( n >> 1 ) + ndx ];
        x = ( ( ( n & 1 ) == 0 ) ? ( x >> 4 ) : ( x & 0xF ) ) * 2;
        if ( x >= bursts.length )
        {
//          ok = false;
          // Non-fatal error
          error = "burst index out of range";
//          return;
          durations[ total++ ] = 0;
          durations[ total++ ] = 0;
        }
        else
        {
          durations[ total++ ] = bursts[ x ];
          durations[ total++ ] = bursts[ x + 1 ];
          //System.err.println( "Durations[ " + (total-2) + "..." + (total-1) + " ] = " + durations[total-2] + " " + durations[total-1] );

        }
      }
      ndx += ( count + 1 ) >> 1;
    }
    repeat = 0;
    extra = 0;

    for ( int n = 0; n < partNdx; ++n )
    {
      if ( partTypes[ n ] && repeat == 0 )
      {
        repeat = 2 * parts[ n ];
      }
      else if ( repeat > 0 )
      {
        extra += 2 * parts[ n ];
      }
    }

    oneTime = total - repeat - extra;
  }

  private int roundTo(int value, int r)
  {
    return ((int) Math.round( (double)value / (double)r )) * r;
  }

  public int[] getBursts()
  {
    return getBursts(1);
  }
  public int[] getBursts(int r)
  {
    int[] temp = new int[bursts.length];
    for ( int i = 0; i < bursts.length; i++ )
      temp[i] = roundTo( bursts[i], r );
    return temp;
  }

  public int[] getDurations( int r, boolean signed )
  {
    return ( durations == null ? new int[0] : getDurations( 0, durations.length, r, signed ) );
  }
  public int[] getOneTimeDurations(int r, boolean signed)
  {
    return getDurations( 0, oneTime, r, signed );
  }
  public int[] getRepeatDurations(int r, boolean signed)
  {
    return getDurations( oneTime, oneTime + repeat, r, signed );
  }
  public int[] getExtraDurations(int r, boolean signed)
  {
    return getDurations( oneTime + repeat, oneTime + repeat + extra, r, signed );
  }
  private int[] getDurations( int start, int end, int r, boolean signed )
  {
    int[] temp = new int[end - start];
    int t = 0;
    for ( int i = start; i < end; i++ )
      temp[t++] = ( signed && i % 2 == 1 ? -1 : 1 ) * roundTo( durations[i], r );
    return temp;
  }

  /** The ROM bursts for learned format 0, in the data format of learned format 0 */
  private final int[] romBurstsA =
  {
      0x01A3, 0x4A81, 0x068F, 0x0690, 0x01A3, 0x04F6, 0x01A3, 0x01A4, // 0
      0x00D2, 0xAF0C, 0x00D2, 0x4507, 0x0277, 0x00D3, 0x00D2, 0x0278, // 8
      0x0083, 0x589B, 0x0083, 0x039B, 0x0083, 0x0189, // 16
      0x0083, 0x5D6F, 0x0083, 0x5527, 0x0083, 0x039B, 0x0083, 0x0189, // 22
      0x0009, 0xFCC0, 0x0009, 0x008C, 0x0009, 0x005A, 0x0009, 0x0028, // 30
      0x270F, 0x07D0, 0x01F3, 0x0FA0, 0x07CF, 0x07D0, 0x00F9, 0x03E8, 0x00F9, 0x01F4, // 38
      0x0118, 0x60C9, 0x08C9, 0x08CA, 0x0118, 0x034D, 0x0118, 0x0119, // 48
      0x0118, 0x4F1F, 0x1193, 0x08CA, 0x0118, 0x034D, 0x0118, 0x0119, // 56
      0x0118, 0xBE1D, 0x0118, 0x5C64, 0x08C9, 0x08CA, 0x0118, 0x034D, 0x0118, 0x0119, // 64
      0x0118, 0xBCE6, 0x0118, 0x4F21, 0x1193, 0x08CA, 0x1193, 0x0465, 0x0118, 0x034D, 0x0118, 0x0119, // 74
      0x0118, 0xBCE6, 0x0118, 0x57EE, 0x1193, 0x08CA, 0x1193, 0x0465, 0x0118, 0x034D, 0x0118, 0x0119, // 86
      0x0118, 0xB895, 0x0118, 0x2E8C, 0x1193, 0x08CA, 0x0118, 0x034D, 0x0118, 0x0119, // 98
      0x010A, 0xEE3E, 0x010A, 0x283A, 0x010A, 0x0537, 0x010A, 0x0216, 0x010A, 0x010B, // 108
      0x010A, 0xEE3E, 0x010A, 0x283A, 0x010A, 0x0537, 0x0215, 0x0216, 0x010A, 0x0216, 0x010A, 0x010B, // 118
      0x010A, 0xEE3E, 0x010A, 0x283A, 0x00F2, 0x0537, 0x0215, 0x0216, 0x0215, 0x010B, 0x010A, 0x0216, 0x010A, 0x010B, // 130
      0x01BC, 0xB0FF, 0x0379, 0x01BD, 0x01BC, 0x01BD, // 144
      0x01BC, 0xB0FF, 0x0379, 0x01BD, 0x01BC, 0x037A, 0x01BC, 0x01BD, // 150
      0x01BC, 0xB0FF, 0x0379, 0x037A, 0x0379, 0x01BD, 0x01BC, 0x037A, 0x01BC, 0x01BD, // 158
      0x0009, 0xFFFF, // 168
      0x0009, 0x1E61, 0x0009, 0x184E, 0x0009, 0x1238, 0x0009, 0x0C22, 0x0009, 0x060C, // 170
      0x0009, 0x7B65, 0x0009, 0x10CC, 0x0009, 0x0B2C, // 180
      0x006B, 0xFFFF, // 186
      0x006B, 0x1DFF, 0x006B, 0x17EC, 0x006B, 0x11D6, 0x006B, 0x0BC0, 0x006B, 0x05AA, // 188
      0x0013, 0x7B5B, 0x0013, 0x10C2, 0x0013, 0x0B22  // 198
  };

  /** The index for romBurstsA. */
  private final int[] romIndexA =
  {
      48, 56, 64, 74, 86, 8, 98, 16, 22, 30, 144, 150, 158, 108, 118, 130, 0, 38, 168, 170, 180, 186, 188, 198
  };
  
  /**
   *   Periods for maps 21-23 in units of period of 8MHz clock
   */
  private final int[] romPeriodsA =
  {
      0x0012, 0x0012, 0x0014
  };
  
  // The table for ROM bursts type B is taken from the Inteset INT422-3, the first remote
  // to use this data that we came across.  As a result, it is in learned format 3.  The
  // corresponding tables in remotes with learned formats 1, 2 and 4 are in the those formats
  // but are equivalent in their data.  The type B periods table is from the URC-7960 for
  // simplicity, to keep the same 8MHz clock as the type A table.
  
  /** The ROM bursts for learned formats 1-4, in the data format of learned format 3 */
  private final int[] romBurstsB =
  {
      0x0160, 0x60C9, 0x0AB0, 0x08CA, 0x0160, 0x034D, 0x0160, 0x0119, // 0
      0x0160, 0x4F1F, 0x1560, 0x08CA, 0x0160, 0x034D, 0x0160, 0x0119, // 8
      0x0160, 0xBE1D, 0x0160, 0x5C64, 0x0AB0, 0x08CA, 0x0160, 0x034D, 0x0160, 0x0119, // 16
      0x0160, 0xBCE6, 0x0160, 0x4F21, 0x1560, 0x08CA, 0x1560, 0x0465, 0x0160, 0x034D, 0x0160, 0x0119, // 26
      0x0160, 0xBCE6, 0x0160, 0x57EE, 0x1560, 0x08CA, 0x1560, 0x0465, 0x0160, 0x034D, 0x0160, 0x0119, // 38
      0x0100, 0xAF0C, 0x0100, 0x4507, 0x0300, 0x00D3, 0x0100, 0x0278, // 50
      0x0150, 0xB895, 0x0150, 0x2E8C, 0x1550, 0x08CA, 0x0150, 0x034D, 0x0150, 0x0119, // 58
      0x00A0, 0x589B, 0x00A0, 0x039B, 0x00A0, 0x0189, // 68
      0x00A0, 0x5D6F, 0x00A0, 0x5527, 0x00A0, 0x039B, 0x00A0, 0x0189, // 74
      0x00A0, 0xFCC0, 0x00A0, 0x008C, 0x00A0, 0x005A, 0x00A0, 0x0028, // 82
      0x0200, 0xB0FF, 0x0400, 0x01BD, 0x0200, 0x01BD, // 90
      0x0200, 0xB0FF, 0x0400, 0x01BD, 0x0200, 0x037A, 0x0200, 0x01BD, // 96
      0x0200, 0xB0FF, 0x0400, 0x037A, 0x0400, 0x01BD, 0x0200, 0x037A, 0x0200, 0x01BD, // 104
      0x0100, 0xEE3E, 0x0100, 0x283A, 0x0100, 0x0537, 0x0100, 0x0216, 0x0100, 0x010B, // 114
      0x0100, 0xEE3E, 0x0100, 0x283A, 0x0100, 0x0537, 0x0200, 0x0216, 0x0100, 0x0216, 0x0100, 0x010B, // 124
      0x0100, 0xEE3E, 0x0100, 0x283A, 0x0100, 0x0537, 0x0200, 0x0216, 0x0200, 0x010B, 0x0100, 0x0216, 0x0100, 0x010B, // 136
      0x0300, 0x4A81, 0x0C00, 0x0690, 0x0300, 0x04F6, 0x0300, 0x01A4, // 150
      0x4710, 0x07D0, 0x0380, 0x0FA0, 0x0E30, 0x07D0, 0x01C0, 0x03E8, 0x01C0, 0x01F4, // 158
      0x00A0, 0xACB9, 0x00A0, 0x0A6E, 0x00A0, 0x06F9, // 168
      0x00A0, 0xBDF5, 0x00A0, 0x5030, 0x00A0, 0x1A54, 0x00A0, 0x15EF, 0x00A0, 0x045C, 0x00A0, 0x0228, // 174
      0x00A0, 0xFFFF, 0x00A0, 0x1E61, 0x00A0, 0x184E, 0x00A0, 0x1238, 0x00A0, 0x0C22, 0x00A0, 0x060C, // 186
      0x00A0, 0x1E61, 0x00A0, 0x184E, 0x00A0, 0x1238, 0x00A0, 0x0C22, 0x00A0, 0x060C, // 198
      0x00A0, 0x7B65, 0x00A0, 0x10CC, 0x00A0, 0x0B2C, // 208
      0x00A0, 0x5218, 0x00A0, 0x0057, 0x00A0, 0x0046, 0x00A0, 0x0034, 0x00A0, 0x0023, // 214
      0x00A0, 0x4FE0, 0x00A0, 0x0057, 0x00A0, 0x0044, 0x00A0, 0x0034, 0x00A0, 0x0022, // 224
      0x00A0, 0x3E95, 0x00A0, 0x0EC0, 0x00A0, 0x09D3, // 234
      0x00A0, 0x3EEF, 0x00A0, 0x01F3, 0x00A0, 0x00F6, // 240
      0x00A0, 0x3263, 0x00A0, 0x18E5, 0x00A0, 0x041E, 0x00A0, 0x020B, // 246
      0x00A0, 0xFFFF, 0x00A0, 0xF42C, 0x00A0, 0x01FE, 0x00A0, 0x01A3, 0x00A0, 0x014A, 0x00A0, 0x00F3, // 254
      0x00A0, 0xF42C, 0x00A0, 0x01FE, 0x00A0, 0x01A3, 0x00A0, 0x014A, 0x00A0, 0x00F3, // 266
      0x00A0, 0x42C2, 0x00A0, 0x1E7A, 0x00A0, 0x1860, 0x00A0, 0x1245, 0x00A0, 0x0C2B, 0x00A0, 0x0610, // 276
      0x00A0, 0x61C4, 0x00A0, 0x1E7A, 0x00A0, 0x1860, 0x00A0, 0x1245, 0x00A0, 0x0C2B, 0x00A0, 0x0610, // 288
      0x0100, 0xACBF, 0x0100, 0x0A64, 0x0100, 0x06EF, // 300
      0x1000, 0xBCE6, 0x1000, 0x4F21, 0xF110, 0x08CA, 0xF110, 0x0465, 0x1000, 0x034D, 0x1000, 0x0119, // 306
      0x0641, 0x4A0F, 0x0640, 0x1DFF, 0x0640, 0x17EC, 0x0640, 0x11D6, 0x0640, 0x0BC0, 0x0640, 0x05AA, // 318
      0x0640, 0x1DFF, 0x0640, 0x17EC, 0x0640, 0x11D6, 0x0640, 0x0BC0, 0x0640, 0x05AA, // 330
      0x00A0, 0x7B5B, 0x00A0, 0x10C2, 0x00A0, 0x0B22, // 340
      0x00D0, 0x5213, 0x00D0, 0x0052, 0x00D0, 0x0041, 0x00D0, 0x002F, 0x00D0, 0x001E, // 346
      0x0110, 0x4FD8, 0x0110, 0x004F, 0x0110, 0x003C, 0x0110, 0x002C, 0x0110, 0x001A, // 356
      0x00B0, 0x3E93, 0x00B0, 0x0EBE, 0x00B0, 0x09D1, // 366
      0x05F0, 0x3E93, 0x05F0, 0x0197, 0x05F0, 0x009A, // 372
      0x2150, 0x3163, 0xFFF0, 0x084F, 0x2150, 0x031E, 0x2150, 0x010B, // 378
      0x0106, 0x37CC, 0x0100, 0xF424, 0x0100, 0x01F6, 0x0100, 0x019B, 0x0100, 0x0142, 0x0100, 0x00E9, // 386
      0x0100, 0xF424, 0x0100, 0x01F6, 0x0100, 0x019B, 0x0100, 0x0142, 0x0100, 0x00E9, // 398
      0x05C0, 0x4268, 0x05C0, 0x1E20, 0x05C0, 0x1806, 0x05C0, 0x11EB, 0x05C0, 0x0BD1, 0x05C0, 0x05B6, // 408
      0x07C0, 0x616A, 0x05C0, 0x1E20, 0x05C0, 0x1806, 0x05C0, 0x11EB, 0x05C0, 0x0BD1, 0x05C0, 0x05B6  // 420
  };
      
  /** The index for romBurstsB. */
  private final int[] romIndexB = 
  {
      0, 8, 16, 26, 38, 50, 58, 68, 74, 82, 90, 96, 104, 114, 124, 136, 
      150, 158, 168, 174, 186, 198, 208, 214, 224, 234, 240, 246, 254, 266, 276, 288, 
      300, 306, 318, 330, 340, 346, 356, 366, 372, 378, 386, 398, 408, 420
  };
  
  /**
   *   Periods for maps 32-45 in units of period of 8MHz clock (from URC-7960)
   */
  private final int[] romPeriodsB = 
  {
      0x0014, 0x0012, 0x0012, 0x0012, 0x0014, 0x0012, 0x0012, 0x0012, 
      0x0012, 0x0008, 0x0012, 0x0012, 0x0012, 0x0012
  };
  
  /**
   * The clock rate in MHz whose period is used as unit for carrier periods in each format
   */
  public final static int[] clockMHz = new int[] { 8, 12, 4, 10, 8 };
  
  /**
   * Period values that can signify an unmodulated signal for each format.  The nonzero values
   * are those that, interpreted as an actual period, give the burst On and Off units
   * used for unmodulated signals in that format (4/3us for format 1, 2us for formats 2, 3).
   */
  public final static int[] zeroPeriods = new int[] { 0, 16, 8, 20, 0 };
  
}

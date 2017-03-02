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
  
  private int period;

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
    if ( format > 3 )
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
    period = hex.get( format == 3 ? 1 : 0 );
    if ( period == 0 )
    {
      frequency = 0;
    }
    else if ( format == 2 )
    {
      frequency = 4000000 / period;
    }
    else if ( format == 1 )
    {
      frequency = 12000000 / period;
    }
    else if ( format == 3 )
    {
      frequency = 10000000 / period;
    }
    else
    {
      frequency = 8000000 / period;
    }
    if ( frequency > 400000 )
    {
      // Some remotes, eg JP2 & JP3, use a small but nonzero period for zero frequency
      frequency = 0;
    }
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
   * Load burst table.
   * 
   * @param hex
   *          the hex
   * @return the int
   */
  private int loadBurstTable( Hex hex, int format )
  {
    int burstNum = hex.getData()[ format == 3 ? 0 : 2 ];
    int result;
    if ( ( burstNum & 0xC0 ) != 0 )
    {
      result = 3;
      int[] romBursts = ( burstNum & 0x80 ) > 0 ? romBursts7 : romBursts6;
      int[] romIndex = ( burstNum & 0x80 ) > 0 ? romIndex7 : romIndex6;
      burstNum &= 0x3F;
      if ( burstNum >= romIndex.length )
      {
        ok = false;
        error = "ROM burst index out of range";
        return 0;
      }
      burstNum = romIndex[ burstNum ];
      int count = romBursts.length - burstNum;
      if ( count > 32 )
        count = 32;
      bursts = new int[ count ];
      while ( count > 1 )
      {
        count -= 2;
        bursts[ count ] = ( int )( ( romBursts == romBursts7 ? 2.0 : 1.6 ) * romBursts[ count + burstNum ] + 0.5 );
        bursts[ count + 1 ] = 2 * romBursts[ count + 1 + burstNum ];
      }
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
      
      /**
       *   A burst is stored as two 16-bit values, but in learned formats 1 and 2 this is
       *   interpreted as a 12-bit (3 nibble) ON value and a 20-bit (5 nibble) OFF value.
       */
      boolean split35 = format == 1 || format == 2;
      
      // period == 0 needs to be covered but has not actually been seen, as a small
      // nonzero value seems to be used for unmodulated signals to make these timing
      // calculations be correct.  2us is used here.
      double multOn, multOff;
      if ( format == 0 )
      {
        multOn = multOff = 2.0;
      }
      else if ( format == 1 )
      {
        multOn = ( period == 0 ) ? 2.0 : period / 12.0;
        multOff = 4.0 / 3.0;
      }
      else if ( format == 2 )
      {
        multOn = ( period == 0 ) ? 2.0 : period / 4.0;
        multOff = multOn;
      }
      else  // format == 3
          {
        multOn = 1.6;
        multOff = 2.0;
          }
      for ( int ndx = 0; ndx < burstNum * 2; ndx += 2 )
      {
        int val = hex.get( ndx * 2 + 3 ) >> ( split35 ? 4 : 0 );
      bursts[ ndx ] = (int)( (double)val * multOn + 0.5 );
      }
      for ( int ndx = 1; ndx < burstNum * 2; ndx += 2 )
      {
        int val =  hex.get( ndx * 2 + 3 ) + ( split35 ? ( hex.getData()[ ndx * 2 + 2 ] & 0x0F ) * 0x10000 : 0 );
        bursts[ ndx ] = (int)( (double)val * multOff + 0.5 );
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

  /** The rom bursts when bit 7 of burstNum is set */
  private final int[] romBursts7 =
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

  /** The index for romBursts7. */
  private final int[] romIndex7 =
  {
      48, 56, 64, 74, 86, 8, 98, 16, 22, 30, 144, 150, 158, 108, 118, 130, 0, 38, 168, 170, 180, 186, 188, 198
  };
  
  /** The rom bursts when bit 6 of burstNum is set */
  private final int[] romBursts6 =
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
      
  /** The index for romBursts6. */
  private final int[] romIndex6 = 
  {
      0, 8, 16, 26, 38, 50, 58, 68, 74, 82, 90, 96, 104, 114, 124, 136, 
      150, 158, 168, 174, 186, 198, 208, 214, 224, 234, 240, 246, 254, 266, 276, 288, 
      300, 306, 318, 330, 340, 346, 356, 366, 372, 378, 386, 398, 408, 420
  };
}

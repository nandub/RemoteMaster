package com.hifiremote.jp1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProntoSignal
{
  /* The structure of Pronto signals and the tabular data here are taken from
   *   http://www.hifi-remote.com/infrared/prontoirformats.pdf
   * by Eigeny Oulianov.
   * 
   * 
   * A Pronto signal consists of a sequence of 16-bit words that describe a two-part
   * IR signal.  The first part of the IR signal is sent once when a key is depressed, 
   * the second part is sent repeatedly while the key remains depressed.  The Pronto 
   * format does not handle IR protocols that have a third part sent once when the key
   * is released, though the UEI learned signal format does support such protocols.
   * 
   * Each part of the IR signal consists of a sequence of IR pulses described by a 
   * sequence of burst pairs, a pair consisting of a MARK burst for which an IR pulse 
   * is sent followed by a SPACE burst that is the gap before the next pulse is sent.
   * A signal is constructed as a sequence of individual bursts in which there may be
   * two MARKs or two SPACEs following one another, but these are not separately
   * distinguishable so their durations are added.  The resulting signal always appears
   * to be a sequence of MARK/SPACE burst pairs, however it may be constructed.
   * 
   * The Pronto signal determines a carrier frequency and the MARK and SPACE times are 
   * specified as a number of cycles of this frequency. The first 4 words of any Pronto 
   * signal are a header, the rest is data.  The number of words in the data section is 
   * always even, given by twice the sum of the 3rd and 4th header words.  Any further 
   * words beyond the data section should be ignored, as not part of the signal. The 
   * first header word is a format identifier that determines the interpretation of the
   * data section and any further interpretation of the header.  In a modulated signal 
   * the MARKs are pulses of a square wave at the carrier frequency, but there are also
   * unmodulated signals where the MARKs are IR continuously "on".  These still specify
   * a carrier frequency in the Pronto header but it is used purely as a unit for 
   * describing the MARK and SPACE durations.
   * 
   * There are three distinct types of format: raw formats, template-based formats and 
   * database-linked formats.  In raw formats the 2nd word of the header determines the 
   * carrier frequency and the 3rd and 4th words are the numbers of burst pairs in the
   * once-only and the repeated parts of the IR signal.  The data section of the Pronto
   * signal consists of the burst pairs of the two parts concatenated.
   * 
   * Template-based formats provide a more concise description of certain common IR protocols
   * than do raw formats and they are interpreted by an algorithm specifying how to use the 
   * template to convert the signal into an equivalent raw format. The conversion is via an 
   * intermediate "string code" consisting of a string of alphanumeric characters together 
   * with exactly one vertical bar "|" that acts as a separator to split the string code into 
   * the two parts that describe the once-only and the repeated parts of the IR signal. A
   * template is specified as a character string known as a zTemplate that describes which 
   * characters are permitted values in each position in the string code.
   *  
   * Each alphanumeric character in the string code represents a sequence of bursts, which
   * are alternating MARK and SPACE durations but need not start with a MARK or end with a
   * SPACE.  Each format provides a burst sequence table that maps each character into a 
   * corresponding sequence.  To distinguish SPACEs from MARKs, MARK durations are given as
   * positive values and SPACE durations as negative ones.  In the representation used below,
   * the string code is specified as a sequence of integers, with non-negative values being
   * an index into the table of burst pairs and with -1 representing the "|" separator.  The
   * character corresponding to each non-negative value is given as a comment in the tables
   * below.  The carrier frequency for the burst pairs is specified as part of the format, not
   * by the Pronto signal header.
   * 
   * The template-based formats further subdivide into two types, the fixed-size and 
   * variable-size formats.  The algorithm for constructing the string code from the zTemplate 
   * and the data section of the Pronto signal differs for these two types.
   * 
   * Each position in a string code is of one of three types, the type for each position being 
   * determined by the template.  Fixed positions hold a character that is determined by the 
   * template alone, specified in the zTemplate by that character.  These include the separator
   * "|".  Data positions hold one from a list of characters, the list being given in the 
   * zTemplate enclosed in [].  Finally there are toggle positions that alternate between two 
   * characters on successive key presses.  These characters are given in the zTemplate enclosed
   * in {}.  If there is more than one toggle position then they each hold the same characters.
   * A zTemplate may also contain non-alphanumeric characters that do not convert to any
   * entry in the string code, as the prime purpose of the template is to provide sufficient
   * information to enable the different protocols to be distinguished during the learning of an
   * IR signal. Consequently not all the information in a template is needed for the algorithms
   * for converting a template-based Pronto signal to an equivalent raw format.
   *  
   * In a fixed-size template-based format the data positions all have the template entry [01],
   * where '0' and '1' are adjacent character entries in the burst sequence table for the format.
   * In the format specification below, the index values of these entries in the table are
   * described by an "offset" parameter.  This is the index of the '0' character, as a positive
   * value if the '1' character follows '0' and as a negative value if the '1' precedes the '0'.
   * There may be up to four data words in the Pronto signal, each treated as a sequence of
   * 16 bits.  These bits determine whether it is the '0' or '1' character that should be
   * selected in each data position of the string code.  The mapping of bits to data positions
   * is specified by a "zMask" consisting of up to 4 sections, one for each data word.  There
   * is an abbreviated format for specifying zMasks that is expanded below into a list of 16
   * integers for each section.  A zero value means that the corresponding bit in the data word
   * is not used.  A positive value is one more than the index of the corresponding position in
   * the string code.  A negative value is similar, but the bit value must be complemented.
   * In the format specification below, the "zMask" parameter is a list of up to 4 integers, 
   * one for each section of the zMask, these being index values into a table of zMask sections.
   * Toggle positions are specified below by a Toggle structure that gives the burst sequence 
   * index of the two characters that alternate, together with a location parameter that is a
   * list of the index values of the positions in the string code where the toggle appears.  
   * Finally the values of the fixed positions, which include the separator "|", are given 
   * below by the "cData" parameter which lists the fixed values sequentially for the positions 
   * not specified by the zMask and Toggle parameters.
   * 
   * In a variable-size template-based format there are two additional header words, one giving
   * the length of the string code and the other being a subformat identifier.  This extended
   * header is followed by one data word for each string code position.  In contrast to the
   * fixed-size formats, these data words are interpreted as hexadecimal integers.  Data word
   * 0010 represents the separator "|" character.  Values smaller than this are index values
   * into the burst sequence table for the subformat.  Values greater than 0010 correspond to
   * toggle positions.  The value is 0x14 more than the index into the zTemplate of the toggle 
   * character for odd keypresses, the index for even keypresses being one greater.  The Toggle
   * structure used below is as for fixed-size formats, except that the location parameter lists
   * the index values into the zTemplate rather than into the string code.  
   */
  
  private int[] pronto = null;      // Pronto signal
  
  // Next 4 items are as for learned signal in UnpackLearned class
  private int[] durations = null;   // Durations of each MARK and SPACE in microseconds
  private int oneTime = 0;          // Number of durations in initial part of signal sent once only
  private int repeat = 0;           // Number of durations in repeating part of signal
  private int extra = 0;            // Number of durations in final part of signal sent once only
  private int frequency = 0;        // Carrier frequency in Hz
  
  public String error = null;
  
  public ProntoSignal( String str )
  {
    short[] p = Hex.parseHex( str );
    pronto = new int[ p.length ];
    for ( int i = 0; i < p.length; i++ )
    {
      pronto[ i ] = p[ i ] + ( p[ i ] < 0 ? 0x10000 : 0 );
    }
  }
  
  public ProntoSignal( LearnedSignal ls )
  {
    UnpackLearned ul = ls.getUnpackLearned();
    if ( ul != null && ul.ok )
    {
      oneTime = ul.oneTime;
      repeat = ul.repeat;
      extra = ul.extra;
      frequency = ul.frequency;
      durations = ul.durations.clone();
    }
  }
  
  public int[] getDurations()
  {
    return durations;
  }

  public void makePronto()
  {
    int unit = 0;
    if ( frequency > 0 )
    {
      unit = ( int )Math.floor( 4145146. / frequency + 0.5 ); 
    }
    else
    {
      int shortest = durations[ 0 ];
      for ( int val : durations )
      {
        shortest = Math.min( val, shortest );
      }
      unit = ( int )Math.floor( 4.145146 * 0.125 * shortest + 0.5 );
      unit &= -2;
    }
    if ( unit <= 0 ) 
    {
      unit = 1;
    }

    pronto = new int[ durations.length + 4 ];
    pronto[ 0 ] = frequency > 0 ? 0 : 0x0100;
    pronto[ 1 ] = unit;
    pronto[ 2 ] = oneTime / 2;
    pronto[ 3 ] = repeat / 2;

    for ( int i = 0; i < durations.length; i += 2 )
    {
      int v1 = ( int )Math.floor( durations[ i ]*4.145146/unit + 0.5 );
      if ( v1 == 0 )
      {
        v1 = 1;
      }
      if ( v1 > 0xFFFF )
      {
        v1 = 0xFFFF;
      }
      int v2 = ( int )Math.floor( ( durations[ i ]+durations[i + 1] )*4.145146/unit + 0.5 ) - v1;
      if ( v2 == 0 )
      {
        v2 = 1;
      }
      if ( v2 > 0xFFFF )
      {
        v2 = 0xFFFF;
      }
      pronto[ i + 4 ] = v1;
      pronto[ i + 5 ] = v2;
    } 
  }
  
  public static class ProntoFormat
  {
    private int fmtID = 0;          // main format id
    private int subID = 0;          // subformat id
    private int freqDiv = 0;
    private short[] zMask = null;
    private int offset = 0;         // value in String Code for template character '0'
    private int[][] bursts = null;
    private short[] cData = null;
    private Toggle toggle = new Toggle();
    
    public ProntoFormat() {};

    /**
     *  Constructor for variable-length template-based formats
     */
    public ProntoFormat(int fmtID, int subID, int freqDiv, int[][] bursts, Toggle... toggle )
    {
      this.fmtID = fmtID;
      this.subID = subID;
      this.freqDiv = freqDiv;
      this.bursts = bursts;
      if ( toggle != null && toggle.length > 0 )
      {
        this.toggle = toggle[ 0 ];
      }
    };
    
    /**
     *  Constructor for fixed-length template-based formats
     */
    public ProntoFormat(int fmtID, int subID, int freqDiv, int[][] bursts, int offset, 
        short[] cData, short[] zMask, Toggle... toggle )
    {
      this(fmtID, subID, freqDiv, bursts, toggle );
      this.offset = offset;
      this.cData = cData;
      this.zMask = zMask;     
    };
  }
  
  /**
   *  Toggle is an extract from a zTemplate.
   *    chars is an array of two integers, the index values in the burst sequence table of the
   *      toggle characters used for odd and even keypresses.
   *    locations is an array giving the positions of the toggle characters.  For variable-length
   *      formats this is the index position of the odd-keypress character in the zTemplate, the
   *      location in the string code being variable and determined by the pronto data.  For 
   *      fixed-length formats it is directly the index position in the string code.
   */
  private static class Toggle
  {
    private int[] chars = null;
    private int[] locations = new int[ 0 ];
    
    public Toggle( int... vals )
    {
      if ( vals != null && vals.length > 2 )
      {
        chars = Arrays.copyOf( vals, 2 );
        locations = Arrays.copyOfRange( vals, 2, vals.length );
      }
    }
    
    public boolean atLocation( int pos )
    {
      for ( int loc : locations )
      {
        if ( pos == loc )
        {
          return true;
        }
      }
      return false;
    }
  }

  public void unpack( int togNum )
  {
    error = null;
    
    // Test Pronto length is valid, allowing for extra junk at end
    if ( pronto.length < 6 || pronto.length < 2*( pronto[ 2 ] + pronto[ 3 ] ) + 4 ) 
    {
      error = "Pronto signal has invalid length";
      return;
    }
    
    // Set length of valid data
    int prontoSize = 2*( pronto[ 2 ] + pronto[ 3 ] ) + 4;
    
    /* Identify template-based formats.  There are several cases where there is more than one
     * format with the same fmtID.  These need a further test for identification as follows:
     *   Format 7000 is a variable-length format where pronto[ 4 ] is the format subID; 
     *   Format 6001 is a fixed-length format with two cases distinguished by the highest bit
     *     of pronto[ 4 ], a bit not otherwise used;
     *   Formats >= 900A each have two fixed-length formats, differing in freqDiv, the frequency
     *     divider value.  In raw formats, pronto[ 1 ] is freqDiv but in template-based formats
     *     pronto[ 1 ] is normally a dummy value.  In these cases pronto[ 1 ] appears to be set
     *     to freqDiv to serve as an identifier.  The two freqDiv values are 0x6D and 0x68, with
     *     0x6D being the more usual one.  So here, freqDiv is taken as 0x6D unless 
     *     pronto[ 1 ] == 0x68, when this is set as freqDiv.
     */ 
    ProntoFormat pfm = null;
    int[] pData = null;
    for ( ProntoFormat pfTest : pfList ) 
    {
      if ( pfTest.fmtID == pronto[ 0 ] 
          && ( pfTest.fmtID != 0x6001 || pfTest.subID == ( pronto[ 4 ] < 0x8000 ? 0x40 : 0x41 ) )
          && ( pfTest.fmtID != 0x7000 || pfTest.subID == pronto[ 4 ] ) 
          && ( pfTest.fmtID < 0x900A || pronto[ 1 ] != 0x68 || pfTest.freqDiv == 0x68 ))
      {
        pfm = pfTest;
        break;
      }
    }
    
    if ( pfm == null )
    {
      // Not template-based, so test for raw formats
      if ( pronto[ 0 ] == 0x0000 || pronto[ 0 ] == 0x0100 )
      {
        pfm = new ProntoFormat();
        pfm.fmtID = pronto[ 0 ];
        pfm.freqDiv = pronto[ 1 ];
        oneTime = 2 * pronto[ 2 ];
        repeat = 2 * pronto[ 3 ];
        extra = 0;  // Pronto does not support an extra section
        pData = Arrays.copyOfRange( pronto, 4, pronto.length );
      }
      else
      {
        error = "Pronto format " + String.format( "%04X", pronto[ 0 ] )
            + ( pronto[ 0 ] == 0x8000 ? " is not supported" : " is invalid" );
        return;
      }
    }
    
    // Construct string code for template-based formats, with different algorithms
    // for fixed-length and variable-length formats.
    short[] stringCode = null;
    int scSize = 0;   // length of stringCode
    if ( pfm.fmtID == 0x7000 ) 
    {
      // Variable length format.  These formats have a 6-word header and
      // then one word for each character of the string code.  If string code is of odd
      // length, there is one dummy word to make the overall length be even.
      
      // Check length of string code given by pronto[ 5 ] for consistency.
      scSize = pronto[ 5 ];
      if ( scSize + ( scSize & 1 ) + 6 != prontoSize )
      {
        error = "Invalid length data for pronto format 7000";
        return;
      }

      /* Test Pronto data for invalid values.
       *   Value < 0x0010 is index into burst sequence table for the format.
       *   Value == 0x0010 encodes separator "|", so there must be exactly one such value.
       *   Value > 0x0010 represents a toggle, and is 0x14 more than the index into the
       *     zTemplate of the toggle character for an odd key press.  Valid values of
       *     this index are given by "locations" item of format toggle parameter.
       */
      
      int barCount = 0;
      for ( int i = 0; i < scSize; i++ ) 
      {
        if ( pronto[ i+6 ] == 0x0010 )
        {
          barCount++;
        }
        if ( pronto[ i+6 ] > 0x0010 && !pfm.toggle.atLocation( pronto[ i+6 ] - 0x14 )
            || pronto[ i+6 ] < 0x0010 && pronto[ i+6 ] >= pfm.bursts.length
            || i == scSize - 1 && barCount != 1 )
        {   
          error = "Invalid values in pronto format 7000";
          return;
        }
      }

      // Construct string code from pronto data
      stringCode = new short[ scSize ];
      for ( int i = 0; i < scSize; i++ ) 
      {
        stringCode[ i ] = pronto[ i+6 ] < 0x0010 ? ( short )pronto[ i+6 ] : 
          pronto[ i+6 ] == 0x0010 ? -1 : 
            ( short )pfm.toggle.chars[ togNum & 1 ];
      } 
    }
    else if ( pfm.fmtID > 0x1000 )
    {
      // Fixed length template-based formats.
      
      // Except for format 9001, there is one data word for each section of the zMask.
      // A final dummy word is added when the number of sections is odd.  For format 
      // 9001 there are 4 zMask sections but only two data words, each being used twice.  
      int dataSize = pfm.fmtID == 0x9001 ? 2 : pfm.zMask.length + ( pfm.zMask.length & 1 );
      if ( dataSize != prontoSize - 4 )
      {
        error = "Invalid data in Pronto signal";
        return;
      }
      
      stringCode = new short[ 70 ];  // Maximum size for fixed-length formats
      Arrays.fill( stringCode, ( short ) -2 );  // Unset
      
      // Set stringCode values from Pronto data, starting with data positions;
      int ndx = 0;
      for ( short i : pfm.zMask )
      {
        for ( int j = 15; j >= 0; j-- ) 
        {
          short z = zMaskWords[ i ][ j ];
          if ( z == 0 ) 
          {
            break;
          }
          if ( Math.abs( z ) >= stringCode.length ) 
          {
            error = "Error in internal Pronto tables";
            return; 
          }
          // offset and calc value are -ve when the order of 0 and 1 in the table is reversed
          stringCode[ Math.abs( z ) ] = ( short )Math.abs( ( ( pronto[ ndx + 4 ] >> (15-j) & 1 ) ^ ( z<0 ? 1 : 0 ) ) + pfm.offset );
          scSize++;
        }
        ndx = ( ndx + 1 ) & ( dataSize - 1 );
      }
      
      // Next set toggle positions
      for ( int i : pfm.toggle.locations )
      {
        stringCode[ i ] = ( short )pfm.toggle.chars[ togNum & 1 ];
        scSize++;
      }
      
      // Finally set remaining stringCode values from template
      for ( int i = 0, j = 0; i < stringCode.length; i++ ) 
      {
        if ( stringCode[ i ] == -2 ) 
        {
          if ( j == pfm.cData.length )
          {
            // All cData has been written
            if ( i < scSize )
            {
              // There is still an unset string code position before its calculated end,
              // so something has been written past that end point
              error = "Invalid value in Pronto data";
              return;
            }
            else
            {
              break;
            }
          }
          stringCode[ i ] = pfm.cData[ j++ ];
          scSize++;
        }
      }
      
      // Truncate string code to actual size
      stringCode = Arrays.copyOf( stringCode, scSize );
    }

    if ( scSize > 0 ) 
    {
      int carry = 0;  // initial OFF of repeat section that concatenates with any preceding OFF
      int count = 0;  // current size of oneTime or repeat section
      int val = 0;    // current value being built
      List< Integer > burstList = new ArrayList< Integer >();

      for ( short ndx : stringCode ) 
      {
        if ( ndx < 0 ) 
        {
          // Separator, so finish the "once" section
          if ( val > 0 )
          {
            burstList.add( val );
            val = 0;
            count++;
          }
          if ( ( count & 1 ) == 1 )
          {
            burstList.add( 0 );
            count++;
          }
          oneTime = count;
          count = 0;  // reset the count for the "repeat" section
          carry = 0;  // ignore any carry from a oneTime section
        }
        else for ( int n : pfm.bursts[ ndx ] ) 
        {
          if ( count == 0 && val == 0 && n < 0 )
          {
            // First n of section is an OFF, so it is carried over
            carry += -n;
          }
          else if ( ( count & 1 ) == ( n < 0 ? 0 : 1 ) )
          {
            // Here val is for an ON burst and n is an OFF value or vice versa,
            // so add val to list and start building a new value
            burstList.add( val );
            val = Math.abs( n );
            count++;
          }
          else
          {
            val += Math.abs( n );
          }
        }
      }
      
      // Now finalize the repeat section by posting final value to list and adding in any carry
      if ( ( count & 1 ) == 1 )
      {
        // Currently building an OFF value
        val += carry;
        burstList.add( val );
        count++;
      }
      else if ( val > 0 )
      {
        // Currently building a non-zero ON value
        burstList.add( val );
        burstList.add( carry );
        count += 2;
      }
      else if ( carry > 0 && count > 0 )
      {
        // No new ON value so add carry to last OFF
        val = burstList.get( count - 1 );
        burstList.set( count - 1, val + carry );
      }
      repeat = count;
      extra = 0;
      
      // The carry has been handled for the repeat section, but still needs adding to any oneTime section
      if ( carry > 0 && oneTime > 0 )
      {
        val = burstList.get( oneTime - 1 );
        burstList.set( oneTime - 1, val + carry );
      }
      
      pData = new int[ burstList.size() ];
      for ( int i = 0; i < burstList.size(); i++ )
      {
        pData[ i ] = burstList.get( i );
      }
    }
    
    int unit = pfm.freqDiv;
    if ( pfm.fmtID != 0x0100 && ( pfm.fmtID != 0x0000 || unit != 1 ) ) 
    {
      frequency = ( int )Math.floor( 4145146. / unit + 0.5 );
    }

    durations = new int[ oneTime + repeat ];
    for (int i=0; i < oneTime + repeat; i += 2 )
    {
      durations[ i ] = ( int )Math.floor( ( pData[ i ] * unit)/4.145146 + 0.5 );
      durations[ i+1 ] = (int)Math.floor( ( ( pData[ i ]+ pData[ i + 1 ])  *unit)/4.145146 + 0.5 ) - durations[ i ];
    }
    
    return;
  }
  
  private boolean equalTimes( int[] t1, int[] t2, int errLimit )
  {
    for ( int i = 0; i < t1.length && i < t2.length; i++ )
    {
      if ( Math.abs( t2[i] ) < Math.floor( 0.975*Math.abs( t1[i] ) ) && Math.abs( t2[i] ) < Math.abs( t1[i] ) - errLimit
              || Math.abs( t2[i] ) > Math.ceil( 1.025*Math.abs( t1[i] ) ) && Math.abs( t2[i] ) > Math.abs( t1[i] ) + errLimit )
      {
        return false;
      }
    }
    return true;
  }
  
  private void makeLearnBursts( List< int[] > bursts, int[] data )
  {
    int errLimit = frequency > 0 ? ( int )Math.round( 2500000./frequency ) : 25;
    
    for ( int i = 0; i < oneTime + repeat; i += 2 )
    {
      boolean found = false;
      int[] next = new int[]{ durations[ i ], durations[ i + 1 ] };
      for ( int j = 0; j < bursts.size(); j++ )
      {
        if ( equalTimes( bursts.get( j ), next, errLimit ) )
        {
          data[ i/2 ] = j;
          found = true;
          break;
        }
      }
      if ( !found )
      {
        data[ i/2 ] = bursts.size();
        bursts.add( next );
      }
    }
  }

  public LearnedSignal makeLearned( int format )
  {
    error = null;
    if ( format > 2 )
    {
      error = "Format value " + format + " is not supported";
      return null;
    }
    int[] data = new int[ ( oneTime + repeat )/2 ];
    List< int[] > bursts = new ArrayList< int[] >();
    
    makeLearnBursts( bursts, data );
    
    if ( bursts.size() > 16 )
    {
      error = "There are " + bursts.size() + " bursts, maximum permitted is 16";
      return null;
    }

    int length;
    length = 3 + 4*bursts.size();
    if ( oneTime > 0 ) 
    {  
      length += ( oneTime/2 + 1 )/2 + 1;
    }
    if ( repeat > 0 )
    {
      length += ( repeat/2 + 1 )/2 + 1;
    }
    if ( extra > 0 )
    {
      length += ( extra/2 + 1 )/2 + 1;
    }
    
    if ( length + 3 > 0x80 )  // Max length including 3-byte header is 0x80
    {
      error = "Total length is " + ( length + 3 ) + " bytes, maximum permitted is 128";
      return null;
    }
    
    Hex hex = new Hex( length );
    // Get carrier period in units of system clock period, from corresponding frequencies in Hz
    double clock = format == 0 ? 8000000.0 : format== 1 ? 12000000.0 : 4000000.0;
    int period = frequency > 0 ? (int)Math.floor( clock / frequency + 0.5 ) : 0;
    if ( period > 0x7FFF )
    {
      error = "Nonzero frequency is below supported lower limit";
      return null;
    }
    double[] units = new double[ 2 ];   // units in us for burst ON and OFF times
    if ( format == 0 )
    {
      units[ 0 ] = units[ 1 ] = 2.0;
    }
    if ( format == 1 )
    {
      units[ 0 ] = ( period == 0 ) ? 2.0 : period / 12.0;
      units[ 1 ] = 4.0 / 3.0;
    }
    else if ( format == 2 )
    {
      units[ 0 ] = units[ 1 ] = ( period == 0 ) ? 2.0 : period / 4.0;
    }

    hex.put( period, 0 ); 
    hex.set( ( short )bursts.size(), 2 );
    int ndx = 3;

    for ( int i = 0; i < bursts.size(); i++ )
    {
      int tOn  = ( int )Math.round( bursts.get( i )[ 0 ] / units[ 0 ] );
      int tOff = ( int )Math.round( bursts.get( i )[ 1 ] / units[ 1 ] );
      if ( format > 0 )
      {
        if ( tOn > 0xFFF )
        {
          tOn = 0xFFF;
          error = "Burst MARK duration out of range for format " + format;
        }
        if ( tOff > 0xFFFFF ) 
        {
          tOff = 0xFFFFF;
          error = "Burst SPACE duration out of range for format " + format;
        }
        tOn = ( tOn << 4 ) | ( tOff >> 16 );  // 3 nibbles for On, 5 for Off
        tOff &= 0xFFFF;
      }
      else
      {
        if ( tOn > 0xFFFF )
        {
          tOn = 0xFFFF;
          error = "Burst MARK duration out of range for format 0";
        }
        if ( tOff > 0xFFFF )
        {
          tOff = 0xFFFF;
          error = "Burst SPACE duration out of range for format 0";
        }
      }
      hex.put( tOn, ndx );
      ndx += 2;
      hex.put( tOff, ndx );
      ndx += 2;
    }
    if ( oneTime > 0 )
    {
      hex.set( ( short )( oneTime/2 ), ndx++ );
      for ( int i = 0; i < oneTime/2; )
      {
        int x = data[ i++ ]<<4;
        if ( i < oneTime/2 ) 
        {
          x |= data[ i++ ];
        }
        hex.set( ( short )x, ndx++ );
      }
    }
    if ( repeat > 0 )
    {
      hex.set( ( short )( repeat/2 | 0x80 ), ndx++ );
      for ( int i = oneTime/2; i < ( oneTime + repeat )/2; )
      {
        int x = data[ i++ ]<<4;
        if ( i < ( oneTime + repeat )/2 ) 
        {
          x |= data[ i++ ];
        }
        hex.set( ( short )x, ndx++ );
      }
    }
    if ( extra > 0 )
    {
      hex.set( ( short )( extra/2 ), ndx++ );
      for ( int i = ( oneTime + repeat )/2; i < ( oneTime + repeat + extra )/2; )
      {
        int x = data[ i++ ]<<4;
        if ( i < ( oneTime + repeat + extra )/2 ) 
        {
          x |= data[ i++ ];
        }
        hex.set( ( short )x, ndx++ );
      }
    }
    return new LearnedSignal( 0, 0, format, hex, null );
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    if ( pronto != null && pronto.length > 0 )
    {
      for ( int val : pronto )
      {
        if ( sb.length() > 0 )
        {
          sb.append( " " );
        }
        sb.append( String.format( "%04X", val ) );
      }
    }
    return sb.length() > 0 ? sb.toString() :  "** No pronto signal **";
  }
  
  // Expanded sections for zMasks

  private static short[][] zMaskWords = {
      { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 5, 6, 7, 8},  // {4-8}          //  0
      { 0, 0, 0, 0, 0, 0, 0, 0, 0,-2, 9,10,11,12,13,14},  // {!2,9-14}      //  1 
      { 0, 0, 0, 0, 0, 0, 0, 0, 7, 8, 9,10,11,12,13,14},  // {7-14}         //  2 
      { 0, 0, 0, 0, 0, 0, 0, 0,15,16,17,18,19,20,21,22},  // {15-22)        //  3
      { 0, 0, 0, 0, 0, 0, 0, 0, 0,-2,10,11,12,13,14,15},  // {-2,10-15}     //  4
      { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,17,18,19,20,21},  // {16-21}        //  5
      { 8, 7, 6, 5, 4, 3, 2, 1,16,15,14,13,12,11,10, 9},  // {8-1,16-9}     //  6
      {24,23,22,21,20,19,18,17,32,31,30,29,28,27,26,25},  // {24-17,32-25}  //  7
      { 9, 8, 7, 6, 5, 4, 3, 2,17,16,15,14,13,12,11,10},  // {9-2,17-10}    //  8
      {25,24,23,22,21,20,19,18,33,32,31,30,29,28,27,26},  // {25-18,33-26}  //  9
      {42,41,40,39,38,37,36,35,50,49,48,47,46,45,44,43},  // {42-35,50-43}  // 10
      {58,57,56,55,54,53,52,51,66,65,64,63,62,61,60,59},  // {58-51,66-59}  // 11
      {43,42,41,40,39,38,37,36,51,50,49,48,47,46,45,44},  // {43-36,51-44}  // 12
      {59,58,57,56,55,54,53,52,67,66,65,64,63,62,61,60},  // {59-52,67-60}  // 13
      { 0, 0, 0, 0, 0, 0, 0, 0, 8, 7, 6, 5, 4, 3, 2, 1},  // {8-1}          // 14
      { 0, 0, 0, 0, 0, 0, 0, 0,16,15,14,13,12,11,10, 9},  // {16-9}         // 15
      { 0, 0, 0, 0, 0, 0, 0, 0,24,23,22,21,20,19,18,17},  // {24-17}        // 16
      { 0, 0, 0, 0, 0, 0, 0, 0,32,31,30,29,28,27,26,25},  // {32-25}        // 17
      { 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 9,10,11,12,13,14},  // {8-14}         // 18
      { 0, 0, 0, 0, 0, 0, 0, 0,23,24,25,26,27,28,29,30},  // {23-30}        // 19
      { 0, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22},  // {8-22}         // 20
      { 0, 0, 0, 0, 0, 0, 0, 0,31,32,33,34,35,36,37,38},  // {31-38}        // 21
      { 0, 0, 0, 0, 0, 0, 0, 0,-16,-15,-14,-13,-12,-11,-10, -9},// {!16-!9} // 22
      { 0, 0, 0, 0, 0, 0, 0, 0,-32,-31,-30,-29,-28,-27,-26,-25} // {!32-!25}// 23
    };

  // Fixed values in string codes for fixed length template-based formats
  
  private static short[] tm00 = {-1, 2, 0 };
  private static short[] tm01 = {-1, 0, 4, 3, 3, 3, 5 };
  private static short[] tm02 = {-1, 3, 1, 0 };
  private static short[] tm13 = { 0, 1,-1, 4 };
  private static short[] tm14 = {-1, 0, 1 };
  private static short[] tm15 = { 0, 1, 0, 1,-1, 4 };
  private static short[] tm16 = { 0, 1,-1, 0, 1 };
  private static short[] tm17 = {-1, 0, 1, 0, 1 };
  private static short[] tm40 = {-1, 0, 4, 4, 4, 3, 3, 5 };
  private static short[] tm41 = {-1, 0, 4, 4, 4, 3, 4, 5 };
  
  // Burst sequence table entries
  
  private static int[][] bst00 = {
    { -0x0CA0 },          // R
    {  0x0020,-0x0020 },  // 0
    { -0x0020, 0x0020 }   // 1
  };

  private static int[][] bst01 = {
    {  0x0060,-0x0020 },  // H
    {  0x0020,-0x0020 },  // T
    { -0x0020, 0x0020 },  // t
    { -0x0010, 0x0010 },  // 0
    {  0x0010,-0x0010 },  // 1
    { -0x0BC0 }           // R
  };

  private static int[][] bst02 = {
    { -0x0AA0 },          // R
    { -0x0080 },          // S
    {  0x0020,-0x0020 },  // 0
    { -0x0020, 0x0020 }   // 1
  };

  private static int[][] bst03 = {
    {  0x005B,-0xC422 },  // R
    {  0x005B,-0x1BC0 },  // 5
    {  0x005B,-0x1622 },  // 4
    {  0x005B,-0x1083 },  // 3
    {  0x005B,-0x0AE4 },  // 2
    {  0x005B,-0x0546 }   // 1
  };

  private static int[][] bst04 = {
    {  0x0103,-0xAD01 },  // p
    {  0x0103,-0x47D5 },  // e
    {  0x0103,-0x1745 },  // s
    {  0x1010,-0x040C },  // r
    {  0x0103,-0x0309 },  // 0
    {  0x0103,-0x0103 },  // 1
    {  0x1010,-0x081E }   // s$
  };

  private static int[][] bst05 = {
    {  0x227B,-0x1160 },  // r
    {  0x222C,-0x8FFA },  // R
    {  0x222C,-0x6108 },  // S
    {  0x222C,-0x4165 },  // T
    {  0x222C,-0x31AF },  // o
    {  0x222C,-0x2C13 },  // U
    {  0x222C,-0x0684 },  // 0
    {  0x222C,-0x222C },  // 1
    {  0x227B,-0x1160 }   // o$
  };

  private static int[][] bst06 = {
    {  0x00FC,-0x347B },  // r
    {  0x00FC,-0x00FC },  // a
    {  0x00FC,-0x007E },  // e
    {  0x007E,-0x347B },  // t
    {  0x007E,-0x0274 },  // s
    {  0x007E,-0x017A },  // b
    {  0x007E,-0x00FC },  // c
    {  0x007E,-0x007E }   // d
  };

  private static int[][] bst08 = {
    {  0x0019,-0x005A, 0x0028 },          // P
    { -0x0044, 0x0024 },                  // a
    { -0x0036, 0x0012,-0x000E, 0x0012 },  // b
    { -0x0022, 0x0012,-0x0022, 0x0012 },  // c
    { -0x000E, 0x0012,-0x0036, 0x0012 },  // d
    { -0x07FC }                           // r
  };

  private static int[][] bst0A = {
    {  0x0006,-0x0857 },  // R
    {  0x0006,-0x011A },  // 2
    {  0x0006,-0x00BB }   // 1
  };

  private static int[][] bst0B = {
    { 0x0010,-0x0857 },   // R
    { 0x0010,-0x0642 },   // S
    { 0x0010,-0x0112 },   // 2
    { 0x0010,-0x00B1 }    // 1
  };

  private static int[][] bst0C = {
    { 0x0010,-0x059D },   // R
    { 0x0010,-0x0417 },   // S
    { 0x0010,-0x02FB },   // T
    { 0x0010,-0x0099 },   // 2
    { 0x0010,-0x0044 }    // 1
  };

  private static int[][] bst0D = {
    { 0x0008,-0x4B7B },   // R
    { 0x0008,-0x3A74 },   // S
    { 0x0008,-0x0A2B },   // 0
    { 0x0008,-0x06C3 },   // 1
    { 0x0008,-0x0515 }    // s
  };

  private static int[][] bst0E = {
    { 0x0008,-0x2BAE },   // R
    { 0x0008,-0x2104 },   // S
    { 0x0008,-0x05CA },   // 0
    { 0x0008,-0x03D9 }    // 1
  };

  private static int[][] bst0F = {
    {  0x0019,-0x024C },  // R
    {  0x0019,-0x010A },  // 0
    {  0x0019,-0x00AF }   // 1
  };

  private static int[][] bst10 = {
    { 0x0009,-0x74F3 },   // R
    { 0x0009,-0x58D2 },   // S
    { 0x0009,-0x0F3E },   // 0
    { 0x0009,-0x0A2A }    // 1
  };

  private static int[][] bst11 = {
    {  0x0006,-0x0841 },  // R
    {  0x0006,-0x065B },  // S
    {  0x0006,-0x011A },  // 2
    {  0x0006,-0x00BB }   // 1
  };

  private static int[][] bst13 = {
    { 0x0157,-0x00AB },   // I
    { 0x0016,-0x05E7 },   // F
    { 0x0016,-0x0040 },   // 1
    { 0x0016,-0x0015 },   // 0
    { 0x0157,-0x0055, 0x0016,-0x0E3B }   // R
  };

  private static int[][] bst14 = {
    { 0x0157,-0x00AB },   // I
    { 0x0016,-0x05E7 },   // F
    { 0x0016,-0x0040 },   // 1
    { 0x0016,-0x0015 }    // 0
  };

  private static int[][] bst40 = {
    {  0x0070,-0x0020 },  // H
    {  0x0020,-0x0020 },  // T
    { -0x0020, 0x0020 },  // t
    { -0x0010, 0x0010 },  // 0
    {  0x0010,-0x0010 },  // 1
    {  0x0AB0 }           // R
  };

  private static int[][] bst41 = {
    {  0x0070,-0x0020 },  // H
    {  0x0020,-0x0020 },  // T
    { -0x0020, 0x0020 },  // t
    { -0x0010, 0x0010 },  // 0
    {  0x0010,-0x0010 },  // 1
    {  0x09B0 }           // R
  };
  
  // Main pronto format table
  
  private static ProntoFormat[] pfList = {
    new ProntoFormat(0x5000, 0x00, 0x73, bst00, 1, tm00, new short[]{ 0, 1 }, new Toggle( 1, 2, 3 ) ),
    new ProntoFormat(0x6000, 0x01, 0x73, bst01, 3, tm01, new short[]{ 2, 3 }, new Toggle( 2, 1, 6 ) ),
    new ProntoFormat(0x5001, 0x02, 0x73, bst02, 2, tm02, new short[]{ 0, 4, 5 }, new Toggle( 2, 3, 3 ) ),
    new ProntoFormat(0x7000, 0x03, 0x09, bst03 ),
    new ProntoFormat(0x7000, 0x04, 0x09, bst04 ),
    new ProntoFormat(0x7000, 0x05, 0x04, bst05 ),
    new ProntoFormat(0x7000, 0x06, 0x0C, bst06 ),
    new ProntoFormat(0x7000, 0x07, 0x0C, bst06 ),
    new ProntoFormat(0x7000, 0x08, 0x88, bst08, new Toggle( 1, 3, 3 ) ),
    new ProntoFormat(0x7000, 0x09, 0x88, bst08, new Toggle( 2, 4, 3 ) ),
    new ProntoFormat(0x7000, 0x0A, 0x7C, bst0A, new Toggle( 2, 1, 3 ) ),
    new ProntoFormat(0x7000, 0x0B, 0x7C, bst0B, new Toggle( 3, 2, 2, 6 ) ),
    new ProntoFormat(0x7000, 0x0C, 0x7C, bst0C, new Toggle( 4, 3, 8 ) ),
    new ProntoFormat(0x7000, 0x0D, 0x0C, bst0D, new Toggle( 2, 3, 5 ) ),
    new ProntoFormat(0x7000, 0x0E, 0x18, bst0E, new Toggle( 2, 3, 2, 6 ) ),
    new ProntoFormat(0x7000, 0x0F, 0x42, bst0F ),
    new ProntoFormat(0x7000, 0x10, 0x08, bst10, new Toggle( 2, 3, 2, 6 ) ),
    new ProntoFormat(0x7000, 0x11, 0x6D, bst11, new Toggle( 3, 2, 3 ) ),
    new ProntoFormat(0x7000, 0x12, 0x63, bst11, new Toggle( 3, 2, 3 ) ),
    new ProntoFormat(0x900A, 0x13, 0x6D, bst13, -3, tm13, new short[]{ 6, 7 } ),
    new ProntoFormat(0x900B, 0x14, 0x6D, bst14, -3, tm14, new short[]{ 8, 9 } ),
    new ProntoFormat(0x900C, 0x15, 0x6D, bst13, -3, tm15, new short[]{ 6, 7,10,11 } ),
    new ProntoFormat(0x900D, 0x16, 0x6D, bst14, -3, tm16, new short[]{ 6, 7,12,13 } ),
    new ProntoFormat(0x900E, 0x17, 0x6D, bst14, -3, tm17, new short[]{ 8, 9,12,13 } ),
    new ProntoFormat(0x900A, 0x18, 0x68, bst13, -3, tm13, new short[]{ 6, 7 } ),
    new ProntoFormat(0x900B, 0x19, 0x68, bst14, -3, tm14, new short[]{ 8, 9 } ),
    new ProntoFormat(0x900C, 0x1A, 0x68, bst13, -3, tm15, new short[]{ 6, 7,10,11 } ),
    new ProntoFormat(0x900D, 0x1B, 0x68, bst14, -3, tm16, new short[]{ 6, 7,12,13 } ),
    new ProntoFormat(0x900E, 0x1C, 0x68, bst14, -3, tm17, new short[]{ 8, 9,12,13 } ),
    // id values 0x40 - 0x42 represent codes documented as *, **, ***.
    new ProntoFormat(0x9000, 0x1D, 0x6D, bst13, -3, tm13, new short[]{ 14,15,16,17 } ),
    new ProntoFormat(0x6001, 0x40, 0x73, bst40,  3, tm40, new short[]{ 18, 3,19 }, new Toggle( 2, 1, 6 ) ), // These two are distinguished
    new ProntoFormat(0x6001, 0x41, 0x73, bst41,  3, tm41, new short[]{ 20,19,21 }, new Toggle( 2, 1, 6 ) ),  // by value of aiPronto[4].
    new ProntoFormat(0x9001, 0x42, 0x6D, bst13, -3, tm13, new short[]{ 14,16,22,23 } )
  };


}


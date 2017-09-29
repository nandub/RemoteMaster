package com.hifiremote.jp1;

public class LearnedSignalTimingAnalysis
{
  private String _Name;
  private String _Message;
  private int[] _Bursts;
  private int[][] _OneTimeDurations;
  private int[][] _RepeatDurations;
  private int[][] _ExtraDurations;
  private String _Separator;
  private int _SeparatorFirst;
  private int _SeparatorInterval;

  public String getName() { return _Name; }
  public String getMessage() { return _Message; }
  public int[] getBursts() { return _Bursts; }
  public int[][] getOneTimeDurations() { return _OneTimeDurations; }
  public int[][] getRepeatDurations() { return _RepeatDurations; }
  public int[][] getExtraDurations() { return _ExtraDurations; }

  public LearnedSignalTimingAnalysis( String name, int[] bursts, int[][] oneTime, int[][] repeat, int[][] extra, String sep, int sepFirst, int sepInterval, String message )
  {
    _Name = name;
    _Bursts = bursts;
    _OneTimeDurations = oneTime;
    _RepeatDurations = repeat;
    _ExtraDurations = extra;
    _Separator = sep;
    _SeparatorFirst = sepFirst;
    _SeparatorInterval = sepInterval;
    _Message = message;
  }

  private String[] makeDurationStringList( int[][] durations )
  {
    if ( durations == null || durations.length == 0 )
    {
      return new String[ 0 ];
    }
    int r = 0;
    String[] results = new String[durations.length];
    for ( int[] d: durations )
      results[r++] = durationsToString( d, _Separator, _SeparatorFirst, _SeparatorInterval );
    return results;
  }
  public String[] getOneTimeDurationStringList()
  {
    return makeDurationStringList( getOneTimeDurations() );
  }
  public String[] getRepeatDurationStringList()
  {
    return makeDurationStringList( getRepeatDurations() );
  }
  public String[] getExtraDurationStringList()
  {
    return makeDurationStringList( getExtraDurations() );
  }

  public String getBurstString()
  {
    return durationsToString( getBursts(), _Separator, -2, 2 );
  }
  public String getOneTimeDurationString()
  {
    return joinedDurationsToString( getOneTimeDurations(), _Separator, _SeparatorFirst, _SeparatorInterval );
  }
  public String getRepeatDurationString()
  {
    return joinedDurationsToString( getRepeatDurations(), _Separator, _SeparatorFirst, _SeparatorInterval );
  }
  public String getExtraDurationString()
  {
    return joinedDurationsToString( getExtraDurations(), _Separator, _SeparatorFirst, _SeparatorInterval );
  }
  
  /**
   *  To handle the case where data[] is incomplete, with its lead-in missing, this now
   *  by default overrides the given value of sepFirst for such bi-phase analyses. To
   *  prevent overriding, give sepFirst as a negative value or zero.
   */
  public String durationsToString( int[] data, String sep, int sepFirst, int sepInterval )
  {
    StringBuilder str = new StringBuilder();
    if ( data != null && data.length != 0 )
    {
      int unit = 0;
      if ( _Name.startsWith( "LI" ) && sepFirst > 0 )
      {
        // Analysis is bi-phase and overriding is permitted.  Get the unit.
        int ndx = _Message.lastIndexOf( ' ' );
        String u = _Message.substring( ndx + 1, _Message.length() - 1 );
        unit = Integer.parseInt( u );
        if ( ( data[ 0 ] == unit || data[ 0 ] == 2 * unit ) && ( data[ 1 ] == - unit || data[ 1 ] == - 2 * unit ) )
        {
          // Lead-in missing so override sepFirst
          for ( int i = 0; i < data.length - 1; i++ )
          {
            if ( data[ i ] == data[ i + 1 ] )
            {
              sepFirst = data[ i ] > 0 ? 1 : 2;
              break;
            }
          }
        }
      }
      
      sepFirst = Math.abs( sepFirst );
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
        if ( (i+1) == sepFirst || ( (i+1-sepFirst) % sepInterval ) == 0 )
          str.append( sep );
      }
    }
    if ( str.length() == 0 )
      return "** No signal **";

    return str.toString();
  }
  
  /**
   *  To handle the case where data sections are incomplete, with lead-in missing, this
   *  by default overrides the given value of sepFirst for bi-phase sections with this issue. To
   *  prevent overriding, give sepFirst as a negative value or zero.  The return value is the
   *  concatenation of the strings corresponding to the individual sections of the data.
   */
  public String joinedDurationsToString( int[][] data, String sep, int sepFirst, int sepInterval )
  {
    if ( data == null )
      return "** No signal **";
    StringBuilder sb = new StringBuilder();
    for ( int[] d : data )
    {
      if ( sb.length() > 0 )
        sb.append( ' ' );
      sb.append( durationsToString( d, sep, sepFirst, sepInterval ) );
    }
    return sb.toString();
  }
}

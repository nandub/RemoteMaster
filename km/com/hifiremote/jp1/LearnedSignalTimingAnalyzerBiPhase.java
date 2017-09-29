package com.hifiremote.jp1;

import java.util.ArrayList;
import java.util.HashMap;

public class LearnedSignalTimingAnalyzerBiPhase extends LearnedSignalTimingAnalyzerBase
{
  private int _Unit;
  private String _PreferredName;

  public LearnedSignalTimingAnalyzerBiPhase( UnpackLearned u )
  {
    super( u );
  }

  @Override
  public String getName()
  {
    return "Bi-Phase";
  }

  private HashMap<Integer,Integer> getDurationHistogram( int roundTo )
  {
    int[] durations = getUnpacked().getDurations( roundTo, true );
    HashMap<Integer,Integer> hist = new HashMap<Integer,Integer>();
    
    // Determine the unit with this rounding, in order to be able to test whether a lead-out test
    // is real or is a result of a missing lead-in.
    int unit = Integer.MAX_VALUE;
    for ( int d: durations )
      if ( Math.abs( d ) < unit )
        unit = Math.abs( d );

    int leadIn1 = durations[0];
    int leadIn2 = durations[1];
    boolean leadInSpurious = ( leadIn1 == unit || leadIn1 == 2 * unit ) && ( leadIn2 == - unit || leadIn2 == - 2 * unit );
    for ( int i = 2; i < durations.length - 2; i++ )
    {
      int value = durations[i];
      int absValue = Math.abs( value );
      if ( !hist.containsKey( absValue ) )
      {
        // Check for a lead out.  If true lead-in is missing then any SPACE longer than two units
        // is a potential lead-out.
        if ( value < 0 && ( !leadInSpurious && durations[i+1] == leadIn1 && durations[i+2] == leadIn2 
            || leadInSpurious && absValue > 2 * unit ) )
        {
          i += 2;
          continue;
        }
        hist.put( absValue, 1 );
      }
      else
        hist.put( absValue, hist.get( absValue ) + 1 );
    }

    return hist;
  }
  
  private int[][] getDurationSplit( int[] durations )
  {
    if ( durations == null || durations.length < 4 )
      return null;
    
    boolean leadInMissing = ( durations[ 0 ] == _Unit || durations[ 0 ] == 2 * _Unit ) && ( durations[ 1 ] == - _Unit || durations[ 1 ] == - 2 * _Unit );
    if ( !leadInMissing )
    {
      return splitDurationsBeforeLeadIn( durations );
    }
    else
    {
      // Split durations after each candidate lead-out.
      // First identify these candidates (as in getDurationHistogram(...).
      ArrayList< Integer > list = new ArrayList< Integer >();
      for ( int d : durations )
      {
        int dAbs = Math.abs( d );
        if ( d < 0 && dAbs > 2 * _Unit )
        {
          list.add( d );
        }
      }
      // Create a separator for splitDurations(...) from these values
      int[][] sep = new int[ list.size() ][];
      for ( int i = 0; i < list.size(); i++ )
      {
        sep[ i ] = new int[]{ list.get( i ) };
      }
      // Create the split.
      return splitDurations( durations, sep, true );
    }
  }

  @Override
  protected int calcAutoRoundTo()
  {
    HashMap<Integer,Integer> hist = getDurationHistogram( 1 );

    int min = Integer.MAX_VALUE;
    for ( int k: hist.keySet() )
      if ( k < min )
        min = k;

    int limit = min + (int) ( Math.pow( 10, Math.floor( Math.log10( min ) ) ) / 2 );

    int roundTo = 0;
    while ( roundTo < limit )
    {
      roundTo += 10;
      if ( checkCandidacy( roundTo ) ) // this will trigger an analyze for biphase
        // found a working one, return it
        return roundTo;
    }

    return 0;
  }

  @Override
  protected int checkCandidacyImpl( int roundTo )
  {
    HashMap<Integer,Integer> hist = getDurationHistogram( roundTo );

    int min = Integer.MAX_VALUE;
    for ( int d: hist.keySet() )
      if ( d < min )
        min = d;

    if ( min <= 0 )
      return 0; // obviously no good

    for ( int d: hist.keySet() )
      if ( d % min != 0 || d / min > 2 )
        return 0;

    // so we might good...but we dunno until we try...
    return 1;
  }

  private int _SavedUnit;
  @Override
  public void saveState()
  {
    _SavedUnit = _Unit;
    super.saveState();
  }
  @Override
  public void restoreState()
  {
    super.restoreState();
    _Unit = _SavedUnit;
  }

  @Override
  protected void analyzeImpl()
  {
    HashMap<Integer,Integer> hist = getDurationHistogram( getRoundTo() );
    _Unit = Integer.MAX_VALUE;
    for ( int d: hist.keySet() )
      if ( d < _Unit )
        _Unit = d;

    System.err.println( "BiPhaseAnalyzer: (" + this.hashCode() +") Analyze beginning with rounding of " + getRoundTo() + " yielding unit size of " + _Unit + "..." );

    HashMap<String,int[][]> oneTime = AnalyzeDurationSet( getUnpacked().getOneTimeDurations( getRoundTo(), true ) );
    HashMap<String,int[][]> repeat = AnalyzeDurationSet( getUnpacked().getRepeatDurations( getRoundTo(), true ) );
    HashMap<String,int[][]> extra = AnalyzeDurationSet( getUnpacked().getExtraDurations( getRoundTo(), true ) );

    HashMap<String,Integer> codes = new HashMap<String,Integer>();
    if ( oneTime != null )
      for ( String k: oneTime.keySet() )
        codes.put( k, 0 );
    if ( repeat != null )
      for ( String k: repeat.keySet() )
        codes.put( k, 0 );
    if ( extra != null )
      for ( String k: extra.keySet() )
        codes.put( k, 0 );

    String preferredCode = null;
    String preferredName = null;
    
    boolean allAlt = true;
    for ( String code: codes.keySet() )
    {
      if ( !code.startsWith( "?" ) )
      {
        allAlt = false;
        break;
      }
    }

    // codes.keySet() is all the unique analysis codes
    for ( String code: codes.keySet() )
    {
      if ( !allAlt && code.startsWith( "?" ) )
        continue;
      String altCode = "?,?" + code.substring( code.lastIndexOf( ',' ) );
      boolean valid = ( oneTime == null || oneTime.containsKey( code ) || oneTime.containsKey( altCode ) );
      valid = valid && ( repeat == null || repeat.containsKey( code ) || repeat.containsKey( altCode ) );
      valid = valid && ( extra == null || extra.containsKey( code ) || extra.containsKey( altCode ) );

      if ( valid )
      {
        int[][] tempOneTime = ( oneTime == null ? null : oneTime.get( code ) != null ? oneTime.get( code ) : oneTime.get( altCode ) );
        int[][] tempRepeat = ( repeat == null ? null : repeat.get( code ) != null ? repeat.get( code ) : repeat.get( altCode ) );
        int[][] tempExtra = ( extra == null ? null : extra.get( code ) != null ? extra.get( code ) : extra.get( altCode ) );

        String[] codeSplit = code.split( "," );

        String msg = "Bi-Phase unit size is " + _Unit + ".";
        String name = "LI " + codeSplit[0] + " LO " + codeSplit[2] + " " + ( codeSplit[1].equals("1") ? "ODD" : "EVEN" );

        addAnalysis( new LearnedSignalTimingAnalysis( name, getUnpacked().getBursts( getRoundTo() ), tempOneTime, tempRepeat, tempExtra, ";", ( codeSplit[1].equals("1") ? 1 : 2 ), 2, msg ) );

        if ( preferredCode == null || code.compareTo( preferredCode ) < 0 )
        {
          preferredCode = code;
          preferredName = name;
        }
      }
    }

    _PreferredName = preferredName;
    System.err.println( "BiPhaseAnalyzer: analyzeImpl complete yielding " + getAnalyses().size() + " analyses preferring '" + _PreferredName + "'." );    
  }

  /** return is dictionary of analysis codes to a set of analyzed durations from the split signal
   *  analysis codes are of form "i,s,o":
   *   i = number of units taken from lead in off time to produce first pair
   *   s = ( separateOdd ? 1 : 0 )
   *   o has following meaning:
   *     0 = analysis ended in complete pairs, lead out unchanged
   *     1 = off time for final pair was taken from lead out
   *     2 = final on time was used as part of lead out
   *  Where a lead-in is missing, the true i and s values are indeterminate, so are replaced with '?'.
   *  Note that if the duration list has a missing lead-in then any code starting with ? can only
   *  have one component and codes not starting with ? will have first component null. 
   */ 
  private HashMap<String,int[][]> AnalyzeDurationSet( int[] durations )
  {
    /*
    if ( durations == null || durations.length == 0 )
      System.err.println( "BiPhaseAnalyzer: AnalyzeDurationSet with " + ( durations == null ? "null" : "empty" ) + " set." );
    else if ( durations.length > 3 )
      System.err.println( "BiPhaseAnalyzer: AnalyzeDurationSet with set of " + durations.length + " durations... ( " + durations[0] + " " + durations[1] + " " + durations[2] + " " + durations[3] + " ... )" );
    else
      System.err.println( "BiPhaseAnalyzer: AnalyzeDurationSet with set of " + durations.length + " durations..." );
    */

    int[][] temp = getDurationSplit( durations );
    if ( temp == null )
      return null;
    
    boolean leadInMissing = ( durations[ 0 ] == _Unit || durations[ 0 ] == 2 * _Unit ) && ( durations[ 1 ] == - _Unit || durations[ 1 ] == - 2 * _Unit );
    HashMap<String,int[][]> results = new HashMap<String,int[][]>();

    int i = 0;
    HashMap<String,int[]> tempResults = null;
    for ( int[] t: temp )
    {
      tempResults = AnalyzeDurations( t );
      // if we got no results for this split component, why bother with the rest
      if ( tempResults == null || tempResults.size() == 0 )
      {
        results.clear();
        return results;
      }
        
      for ( String k: tempResults.keySet() )
      {
        String m = k;
        // Only the first split component can actually have a missing lead-in
        if ( i == 0 && leadInMissing )
        {
          // i and s parts of code are unreliable
          m = "?,?" + k.substring( k.lastIndexOf( ',' ) );
        }
        if ( !results.containsKey( m ) )
          results.put( m, new int[temp.length][] );
        results.get( m )[i] = tempResults.get( k );
      }
      i++;
    }
    
    ArrayList< String > codes = new ArrayList< String>();
    for ( String k: results.keySet() )
    {
      codes.add( k );
    }
    
    for ( String code : codes )
    {
      if ( code.startsWith( "?" ) )
        continue;
      String altCode = "?,?" + code.substring( code.lastIndexOf( ',' ) );
      if ( results.get( altCode ) != null )
      {
        results.get( code )[ 0 ] = results.get( altCode )[ 0 ];
      }
    }

    return results;
  }

  /** Input is a list of durations ending with a lead-out, or at least a value to be treated as such.
   *  This is one component from a possible multi-component list of durations split at possible lead-outs.
   *  Return is dictionary of analysis codes to durations for a single split component of the signal.
   *  Analysis codes are of form "i,s,o":
   *  i = number of units taken from lead in off time to produce first pair
   *  s = ( separateOdd ? 1 : 0 )
   *  o has following meaning:
   *     0 = analysis ended in complete pairs, lead out unchanged
   *     1 = off time for final pair was taken from lead out
   *     2 = final on time was used as part of lead out
   */     
  private HashMap<String,int[]> AnalyzeDurations( int[] durations )
  {
    /*
    if ( durations == null || durations.length == 0 )
      System.err.println( "BiPhaseAnalyzer: AnalyzeDurations with " + ( durations == null ? "null" : "empty" ) + " set." );
    else if ( durations.length > 3 )
      System.err.println( "BiPhaseAnalyzer: AnalyzeDurations with set of " + durations.length + " durations... ( " + durations[0] + " " + durations[1] + " " + durations[2] + " " + durations[3] + " ... )");
    else
      System.err.println( "BiPhaseAnalyzer: AnalyzeDurations with set of " + durations.length + " durations...");
    */

    if ( durations == null || durations.length < 4 )
      return null;

    int[] leadIn = new int[2];
    leadIn[0] = durations[0];
    leadIn[1] = durations[1];

    int leadOut = durations[durations.length -1];

    // setup temp array used for analysis
    // we leave 0th spot blank to hold partial lead in off time later
    int[] temp = new int[durations.length - 2];
    for ( int i = 2; i < durations.length - 1; i++ )
      temp[i-1] = durations[i];

    HashMap<String,int[]> results = new HashMap<String,int[]>();
    HashMap<String,ArrayList<int[]>> tempResults = null;

    // we're going to try all possibilities for dividing up the off time of the lead in
    // first, let's find out how many iterations that will take
    int num = Math.abs( leadIn[1] ) / _Unit;
    if ( num > Math.abs( temp[1] ) / _Unit )
      num = Math.abs( temp[1] ) / _Unit;

    // now try them all
    for ( int n = 0; n <= num; n++ )
    {
      temp[0] = -1 * _Unit * n;
      tempResults = analyzeSignalData( temp, leadOut );
      if ( tempResults != null )
      {
        int leadIn1 = leadIn[1] + ( _Unit * n );
        // we analyzed successfully with at least 1 result, so append to results
        for ( String k: tempResults.keySet() )
        {
          String code = Integer.toString( n ) + "," + ( leadIn1 == 0 ? 1 : 0 ) + "," + k;
          results.put( code, mergeAnalysisResult( leadIn[0], leadIn1, tempResults.get( k ), k ) );
        }
      }
    }

    return results;
  }

  private int[] mergeAnalysisResult( int leadIn0, int leadIn1, ArrayList<int[]> pairs, String code )
  {
    int[] data = new int[pairs.size() * 2 + ( code == "2" ? 2 : 1 )];
    int i = 0;
    data[i++] = leadIn0;
    if ( leadIn1 != 0 )
      data[i++] = leadIn1;
    for ( int[] r: pairs )
    {
      if ( r[0] != 0 )
        data[i++] = r[0];
      if ( r[1] != 0 )
        data[i++] = r[1];
    }
    return data;
  }

  // return is dictionary of 'o' part of analysis codes to list of logical pairs for a single split component of the signal
  // analysis codes are of form "i,s,o":
  //  i = number of units taken from lead in off time to produce first pair
  //  s = ( separateOdd ? 1 : 0 )
  //  o has following meaning:
  //    0 = analysis ended in complete pairs, lead out unchanged
  //    1 = off time for final pair was taken from lead out
  //    2 = final on time was used as part of lead out
  private HashMap<String,ArrayList<int[]>> analyzeSignalData( int[] durations, int leadOut )
  {
    /*
    if ( durations == null || durations.length == 0 )
      System.err.println( "BiPhaseAnalyzer: analyzeSignalData with " + ( durations == null ? "null" : "empty" ) + " set." );
    else if ( durations.length > 3 )
      System.err.println( "BiPhaseAnalyzer: analyzeSignalData with set of " + durations.length + " durations... ( " + durations[0] + " " + durations[1] + " " + durations[2] + " " + durations[3] + " ... )" );
    else
      System.err.println( "BiPhaseAnalyzer: analyzeSignalData with set of " + durations.length + " durations..." );
    */

    ArrayList<int[]> result = new ArrayList<int[]>();

    int[] p = null;
    for ( int d: durations )
    {
      if ( d== 0 ) continue;
      //if ( p == null )
      //  System.err.println( "CurrentPair = (), Next = " + d );
      //else
      //  System.err.println( "CurrentPair = ( " + p[0] + ", ??? ), Next = " + d );

      // starting a new pair
      if ( p == null )
      {
        if ( Math.abs( d ) != _Unit )
          return null;
        
        p = new int[2];
        p[0] = d;
      }
      // next finishes our current pair
      else if ( p[0] == -d )
      {
        p[1] = d;
        result.add( p );
        //System.err.println( "Adding pair ( " + p[0] + ", " + p[1] + " )" );
        p = null;
      }
      else if ( d == - 2*p[0])
      {
        p[1] = d/2;
        result.add(p);
        p = new int[2];
        p[0] = d/2;
      }
      // error...unable to parse input durations as bi-phase
      else
      {
        return null;
      }
    }

    //System.err.println( "BiPhaseAnalyzer: Found " + result.size() + " result pairs..." );

    // if we ended on a complete pair, just tack on lead out and we're done
    if ( p == null )
    {
      p = new int[2];
      p[0] = leadOut;
      p[1] = 0;
      result.add( p );

      HashMap<String,ArrayList<int[]>> results = new HashMap<String,ArrayList<int[]>>();
      results.put( "0", result );
      return results;
    }

    // we have an unfinished pair, so we have two options:
    //  1) finish last pair from the lead out
    //  2) assume last + is part of lead out

    // clone is OK here because they will always and forever have the same pairs up to this point
    @SuppressWarnings( "unchecked" )
    ArrayList<int[]> result2 = (ArrayList<int[]>)result.clone();

    // result is finish last pair from the lead out
    p[1] = -p[0];
    result.add( p );
    int[] lo = new int[2];
    lo[0] = leadOut + p[0];
    lo[1] = 0;
    result.add( lo );

    // result2 is last + is part of lead out
    int[] lo2 = new int[2];
    lo2[0] = p[0];
    lo2[1] = leadOut;
    result2.add( lo2 );

    HashMap<String,ArrayList<int[]>> results = new HashMap<String,ArrayList<int[]>>();
    results.put( "1", result );
    results.put( "2", result2 );
    return results;
  }

  @Override
  protected String getPreferredAnalysisName()
  {
    return _PreferredName;
  }
}

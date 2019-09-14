package com.hifiremote.jp1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.harctoolbox.irp.Decoder.Decode;
import org.harctoolbox.irp.Expression;
import org.harctoolbox.irp.InvalidNameException;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.NameEngine;
import org.harctoolbox.irp.NameUnassignedException;
import org.harctoolbox.irp.NamedProtocol;

import com.hifiremote.decodeir.DecodeIRCaller;
import com.hifiremote.jp1.ProtocolManager.QualifiedID;

public class LearnedSignalDecode
{
  public static class Executor
  {
    public Protocol protocol = null;
    public Parameters parms = new Parameters();
    
    /**
     * selectorList has keys that are the names of the selectors in parms.devParms, with an
     * un-named selector having an empty string as name.  It is not allowed to have both an
     * un-named selector and named ones, so if there is more than one key then they must
     * all be non-empty strings.  The value corresponding to a name is a list of the distinct
     * selector indices for that name, in the order of their first occurrence in parms.devParms.
     * Note that the same index can be assigned to more than one selector for a given name.
     */
    public LinkedHashMap< String, List< Integer > > selectorList = new LinkedHashMap< String, List<Integer> >();

    /**
     * choiceList is a List< int[] > of arrays, the length of each array being the size
     * of the selectorList keyset, i.e. the number of distinct selector names, with the
     * elements in the array being in the same order as the selector names in nameList.
     * The value of an element is an index into the list of selector indices that is
     * mapped to that name in selectorList.  By default, choiceList is constructed to
     * contain all possible combinations of such index values, its size being the
     * product of the sizes of all the selector index lists in selectorList.  The default
     * can be overridden by setting the sequencer string, which then specifies both the
     * order of the names in nameList and the integer arrays in choiceList.  This override
     * can be used to specify the order of its entries and/or to limit the combinations
     * that occur.
     */
    public List< int[] > choiceList = new ArrayList< int[] >();
    
    /**
     * nameList is an ordered list of the key values in selectorList.  By default the
     * order is unspecified, but it can be specifed by giving a sequencer string.
     */
    public List< String > nameList = new ArrayList< String >();
    
    /**
     * qualifier, if set, is an IRP expression that evaluates, in the context of an
     * IrpTransmogrifier decode, to an integer interpreted as a boolean, zero being
     * false and all other values being true.  The default is true.  It specifies
     * whether the executor is or is not a valid one for that decode.
     */
    public String qualifier = null;
    
    /**
     * Normally left null (see choiceList), values for the sequencer string can have two
     * possible forms.  If the key values in selectorList consist of non-empty names then
     * sequencer starts with a dot-separated list of these names followed by an equals
     * sign.  The nameList is then set to these names in their list order.  If there is
     * just one key value and it is empty (empty and non-empty names cannot both occur)
     * then this prefix part is omitted and nameList is set to contain just the empty
     * string.  There follows a comma-separated list of entries that are used to construct
     * the choiceList.  Each entry consists of a dot-separated list of integers that
     * form the values in the integer array of a choiceList entry.  All dot-separated lists
     * must have the same length as the number of keys in selectorList.
     */
    public String sequencer = null;
    
    /**
     * newParms is a comma-separated list of additional IRP assignments enclosed in braces
     * devParms is a List< String > of IRP expressions that evaluate to protocols.ini
     *   device parameter values, allowing more than one expression to correspond to the
     *   same device parameter.
     * devIndices is a List< Integer > in one-to-one correspondence with devParms, giving
     *   the 0-based index of the corresponding device parameter as in protocols.ini.
     *   Multiple devParms expressions for the same parameter are represented by these
     *   expressions all having the same index.
     * cmdParms is as devParms but for command parameter values, but multiple expressions
     *   for the same parameter are not allowed.  There is no corresponding cmdIndices
     *   list as these are in one-to-one correspondence with the cmdParms.
     */
    public static class Parameters
    {
      public String newParms = null;
      public List< String > devParms = null;
      public List< Integer > devIndices = null;
      public List< String > cmdParms = null;
    }
    
    public static class Selector
    {
      public int index = 0;
      public String name = null;
      
      @Override
      public boolean equals( Object s )
      {
        if ( s == null || !(s instanceof Selector ) )
          return false;
        Selector sel = ( Selector )s;
        return sel.index == index && sel.name.equals( name );
      }
      
      @Override
      public String toString()
      {
        return name + " " + index;
      }
    }
    
    public static Selector getSelector( String parm )
    {
      int ndx = parm.lastIndexOf( "?" );
      if ( ndx < 0 ) return null;
      String selString = parm.substring( ndx + 1 );
      // If there is a colon beyond the question mark then it is either ill-formed
      // or it is a conditional statement, not a selector.
      if ( selString.indexOf( ":" ) >= 0 ) return null;
      
      Selector selector = new Selector();
      Pattern p = Pattern.compile( "([0-9]|\\s)*" );
      Matcher m = p.matcher( selString );
      if ( m.find() )
      {
        String s = selString.substring( 0, m.end() ).replaceAll( "\\s", "" );
        selector.index = Integer.parseInt( s );
      }
      selString = selString.substring( m.end() );
      p = Pattern.compile( "([0-9A-Z]|\\s)*" );
      m = p.matcher( selString );
      if ( m.find() )
      {
        if ( m.end() != selString.length() )
        {
          System.err.println( "Warning: the name of the selector " + selString + " includes "
              + "illegal characters and will be truncated" );
        }
        selector.name = selString.substring( 0, m.end() ).replaceAll( "\\s", "" );
      }

      return selector;
    }
    
    /*
     * setSelectors takes the parms.devParms and sequencer values of the executor and
     * constructs the selectorList, nameList and choiceList for that executor.
     */
    public void setSelectors()
    {
      selectorList.clear();
      if ( parms == null || parms.devParms == null ) 
        return;
      for ( String parm : parms.devParms )
      {
        Selector sel = getSelector( parm );
        if ( sel == null ) continue;
        String name = sel.name == null ? "null" : sel.name;  // null should actually not occur
        List< Integer > list = selectorList.get( name );
        if ( list == null )
        {
          selectorList.put( name, new ArrayList< Integer >() );
          list = selectorList.get( name );
        }
        if ( !list.contains( sel.index ))
          list.add( sel.index );
      }
        
      nameList.clear();
      choiceList.clear();
      boolean andOthers = false;
      List< String > givenChoices = new ArrayList< String >();
      if ( sequencer != null )
      {
        int ndx = sequencer.indexOf( "=" );
        String nameStr = "";
        String valStr = sequencer;
        if ( ndx >= 0 )
        {
          nameStr = sequencer.substring( 0, ndx );
          valStr = sequencer.substring( ndx + 1 );
        }
        
        StringTokenizer st1 = null;
        if ( !nameStr.isEmpty() )
        {
          st1 = new StringTokenizer( nameStr, "." );
          while ( st1.hasMoreTokens() )
          {
            nameList.add( st1.nextToken() );
          }
        }
        else
        {
          nameList.add( "" );
        }
        
        ndx = valStr.indexOf( ".." );
        if ( ndx >= 0 )
        {
          // presence of ".." signifies that after adding the explicitly given choiceList
          // entries, add all other valid entries.  This option enables certain entries to
          // be given priority without needing to write out all the remaining ones whose
          // order is irrelevant.  The ".." ends the given list, anything else is ignored,
          // so more than two dots can be used if desired, or ",.." as StringTokenizer
          // sees no token after the final comma.
          andOthers = true;
          valStr = valStr.substring( 0, ndx );
        }
        st1 = new StringTokenizer( valStr, "," );
        while ( st1.hasMoreTokens() )
        {
          String st1Token = st1.nextToken();
          StringTokenizer st2 = new StringTokenizer( st1Token, "." );
          if ( st2.countTokens() != nameList.size() )
          {
            System.err.println( "*** Error: sequencer has invalid syntax" );
            continue;
          }
          if ( andOthers )
          {
            givenChoices.add( st1Token );
          }
          int n = 0;
          int[] choices = new int[ nameList.size() ];
          while ( st2.hasMoreTokens() )
          {
            choices[ n++ ] = Integer.valueOf( st2.nextToken() );
          }
          choiceList.add( choices );
        }
      }
      
      if ( sequencer == null || andOthers )
      {
        if ( !andOthers )
        {
          for ( String sn : selectorList.keySet() )
            nameList.add( sn );
        }

        int[] sizes = new int[ nameList.size() ];
        long count = 1;
        for ( int i = 0; i < sizes.length; i++ )
        {
          sizes[ i ] = selectorList.get( nameList.get( i )).size();
          count *= sizes[ i ];
        }
        for ( long n = 0; n < count; n++ )
        {
          long m = n;
          int[] choices = new int[ sizes.length ];
          for ( int i = 0; i < sizes.length; i++ )
          {
            choices[ i ] = ( int )( m % sizes[ i ] );
            m = m / sizes[ i ];
          }
          String choiceStr = null;
          if ( andOthers )
          {
            StringBuilder sb = new StringBuilder();
            for ( int i = 0; i < choices.length; i++ )
            {
              if ( i > 0 ) sb.append( "." );
              sb.append( choices[ i ] );
            }
            choiceStr = sb.toString();
          }
          if ( !andOthers || !givenChoices.contains( choiceStr ) )
          {
            choiceList.add( choices );
          }
        }
      }
    }

    /**
     * preprocessParms takes an index into choiceList as argument and returns an
     * Executor.Parameters structure with newParms, devParms and cmdParms elements
     * being those of the Executor with each expression processed by
     * preprocess( expression, index).
     * 
     */
    public Parameters preprocessParms( int index )
    {
      Parameters p = new Parameters();
      p.newParms = preprocess( parms.newParms, index );
      if ( parms.devParms != null )
      {
        p.devParms = new ArrayList< String >();
        for ( String s : parms.devParms )
        {
          p.devParms.add( preprocess( s, index ) );
        }
      } 
      if ( parms.cmdParms != null )
      {
        p.cmdParms = new ArrayList< String >();
        for ( String s : parms.cmdParms )
        {
          p.cmdParms.add( preprocess( s, index ) );
        }
      }
      return p;
    }
    
    /**
     * preprocess takes a string that is an extended IRP expression, or a comma-separated
     * list of such expressions, that may contain selector expressions (of form
     * "? index name" with name being optional) and index expressions (of form 
     * "?? name" again with name being optional), together with an index into choiceList.
     * The name elements are selector names, no name corresponding to an empty string
     * as name. The choice from the choiceList index determines a selector index value
     * for each selector name, mapped by getIndexValue.
     * 
     * The return value is an expression in pure IRP notation, resulting from processing
     * as follows.  Each index expression is replaced by the corresponding selector
     * index value.  Selector expressions are always appended to pure IRP expressions.
     * If the selector index in the expression matches the value mapped from the selector
     * name then the pure IRP expression is returned, otherwise an empty string is returned.  
     */
    public String preprocess( String expr, int index )
    {
      // Locate index expression of form ??<name> and replace by value from map for the name
      if ( expr == null ) return null;
      Pattern p1 = Pattern.compile( "\\?\\s*\\?\\s*" );
      Pattern p2 = Pattern.compile( "\\?\\s*\\?\\s*[A-Z]" );
      Pattern p3 = Pattern.compile( "\\?\\s*\\?([0-9A-Z]|\\s)*" );
      
      Matcher m1 = p1.matcher( expr );
      Matcher m2 = p2.matcher( expr );
      Matcher m3 = p3.matcher( expr );
      int start = 0;
      int end = 0;
      while ( m1.find() )
      {
        start = m1.start();
        end = m1.end();
        if ( m2.find() )
        {
          // expr has name following the question marks
          m3.find();  // must return true;
          end = m3.end();
        }
        String name = expr.substring( start, end ).replaceAll( "\\s", "" ).substring( 2 );
        Integer value = getIndexValue( name, index );
        if ( value == null )
        {
          System.err.println( "*** Error: Unable to replace indexer question marks" );
          return "";
        }
        expr = expr.substring( 0, start ) + value + expr.substring( end );
        m1 = p1.matcher( expr );
        m2 = p2.matcher( expr );
        m3 = p3.matcher( expr );
      }
      
      Selector s = getSelector( expr );
      Integer sValue = null;
      if ( s != null && ( sValue = getIndexValue( s.name, index ) ) != null )
      {
        if ( selectorList.get( s.name ) == null || s.index != sValue )
        {
          // Expression has a non-matching selector, so is deleted
          return "";
        }
        else
        {
          // Expression has matching selector, so delete the selector
          expr = expr.substring( 0, expr.lastIndexOf( "?" ) ).trim();
        }
      }
      return expr;
    }
    
    /**
     * getIndexValue takes a selector name and an index into choiceList and returns
     * the selector index for that name and choice.
     */
    private Integer getIndexValue( String name, int index )
    {
      if ( index >= choiceList.size() ) return null;
      List< Integer > vals = selectorList.get( name );
      int[] choices = choiceList.get( index );
      int nameIndex = nameList.indexOf( name );
      if ( vals == null || choices == null || nameIndex < 0 
          || nameIndex >= choices.length ) return null;
      return vals.get( choices[ nameIndex ] );
    }
  }

  /**
   * Instantiates a new learned signal decode.
   * 
   * @param decodeIRCaller
   *          the decode ir caller
   */
  public LearnedSignalDecode( DecodeIRCaller decodeIRCaller )
  {
    protocolName = decodeIRCaller.getProtocolName();
    device = decodeIRCaller.getDevice();
    subDevice = decodeIRCaller.getSubDevice();
    obc = decodeIRCaller.getOBC();
    int[] temp = decodeIRCaller.getHex();
    int len = 0;
    for ( int i = 0; i < temp.length && temp[ i ] >= 0; ++i )
    {
      ++len;
    }
    hex = new int[ len ];
    System.arraycopy( temp, 0, hex, 0, len );
    miscMessage = decodeIRCaller.getMiscMessage();
    errorMessage = decodeIRCaller.getErrorMessage();
  }
  
  
  /**
   * A decode is considered invalid only if it has an executor with a non-null
   * qualifier and that qualifier evaluates to 0 (= false) for that decode.
   */
  public boolean isValidDecode()
  {
    if ( executor != null && executor.qualifier != null )
    {
      NameEngine engine = new NameEngine( decode.getMap() );
      Expression exp = Expression.newExpression( executor.qualifier );
      long value = 0;
      try
      {
        value = exp.toLong( engine );
      }
      catch ( NameUnassignedException e )
      {
        System.err.println( "*** Error: Unassigned name in qualifier" );
        return true;
      }
      return value != 0;    
    }
    return true;
  }

  public LearnedSignalDecode( Decode decode )
  {
    this( decode, null, null );
  }
  
  public LearnedSignalDecode( Decode decode, Value[] matchDevParms, Executor executor )
  {
    if ( decode == null ) return;
    this.decode = decode;
    Map< String, Long > map = decode.getMap();
    NamedProtocol np = decode.getNamedProtocol();
    protocolName = np.getName();
    if ( executor == null )
    {
      executor = defaultExecutorMap.get( np );
      if ( executor == null )
      {
        executor = getExecutor( np, null );
        if ( executor != null )
          defaultExecutorMap.put( np, executor );
      }
    }
    this.executor = executor;
    miscMessage = "";


    for ( String key : map.keySet() )
    {
      if ( key.equals( "D" ) )
        device = map.get( "D" ).intValue();
      else if ( key.equals( "S" ) )
        subDevice = map.get( "S" ).intValue();
      else if ( key.equals( "F" ) )
        obc = map.get( "F" ).intValue();
      else
      {
        if ( !miscMessage.isEmpty() )
          miscMessage += ", ";
        miscMessage += key + "=" + map.get( key );
      }
    }

    if ( matchDevParms == null )
    {
      hex = new int[ 0 ];
      if ( isValidDecode() )
      {
        evaluate( map, 0 );
        List< String > error = new ArrayList< String >();
        if ( executor.protocol != null )
        {
          short[] data = getProtocolHex( error ).getData();
          hex = new int[ data.length ];
          for ( int i = 0; i < data.length; i++ )
            hex[ i ] = data[ i ];
        }
      }
      errorMessage = "";
      return;
    }

    for ( int i = 0; i <  Math.max( executor.choiceList.size(), 1 ); i++ )
    {
      evaluate( map, i );
      Value[] testParms = getDevParmValues( decode );
      if ( testParms == null )
        continue;   // values were not self-consistent
      boolean ok = true;
      for ( int j = 0; j < matchDevParms.length; j++ )
      {
        if ( j >= testParms.length )
        {
          ok = false;
          break;
        }
        Integer tInt = ( Integer )testParms[ j ].getUserValue();
        Integer mInt = ( Integer )matchDevParms[ j ].getUserValue();
        if ( tInt != null && mInt != null && tInt.intValue() != mInt.intValue() )
        {
          ok = false;
          break;
        }
      }
      if ( !ok ) 
        continue;
      match = true;
      for ( int j = 0; j < matchDevParms.length; j++ )
      {
        Integer tInt = ( Integer )testParms[ j ].getUserValue();
        if ( tInt != null )
          matchDevParms[ j ].setValue( tInt );
      }
      break;
    }
  }

  private Value[] getDevParmValues( Decode decode )
  {
    Protocol protocol = executor.protocol;
    DeviceParameter[] protocolDevParms = protocol.getDeviceParameters();
    Value[] parmValues = new Value[ protocolDevParms.length ];
    for ( int i = 0; i < parmValues.length; i++ )
    {
      parmValues[ i ] = new Value( null, protocol.devParms[ i ].getDefaultValue() );
    }
    if ( devParmList != null )
    {
      for ( int i = 0; i < devParmList.size(); i++ )
      {
        Integer newVal = devParmList.get( i );
        if ( newVal == null )
          continue;
        int index = executor.parms.devIndices.get( i );
        if ( index < parmValues.length )
        {
          Integer val = ( Integer )parmValues[ index ].getUserValue();
          if ( val != null && val.intValue() != newVal.intValue() )
            return null;  // inconsistent new values
          else if ( newVal != null )
            parmValues[ index ].setValue( newVal );
        }
        else
        {
          System.err.println( "*** Error in uei-executor parameters" );
          return null;
        }
      }
    }
    else
    {
      Map< String, Long > map = decode.getMap();
      String devMatch = LearnedSignalDecode.getMatchName( "Device" );
      String subMatch = LearnedSignalDecode.getMatchName( "Subdevice" );
      for ( int i = 0; i < parmValues.length; i++ )
      {
        String parmMatch = LearnedSignalDecode.getMatchName( protocolDevParms[i].getName() );
        if ( parmMatch.equals( devMatch ) )
          parmValues[i].setValue( map.get( "D" ).intValue() );
        else if ( parmMatch.equals( subMatch ) )
          parmValues[i].setValue( map.get( "S" ).intValue() );
        //else
        //  parmValues[i] = new Value( protocolDevParms[i].getValueOrDefault() );
      }
    }
    return parmValues;
  }
  
  /**
   * evaluate takes as input a map from an IrpTransmogrifier decode and an index into
   * choiceList.  The map assigns a numerical value to each NamedProtocol decode
   * parameter.  This method processes the Executor value of this Learned Signal Code
   * to set its devParmList and cmdParmList fields  These are lists of Integers (so 
   * null is a permitted value) in one-to-one correspondence with the devParms and
   * cmdParms fields of the executor parms field.  
   * 
   * Expressions in newParms, devParms and cmdParms are each evaluated in two stages.
   * First they are preprocessed by Executor.preprocessParms( index ) to create a new
   * Executor.Parameters structure whose expressions are all pure IRP notation.  The
   * expressions in the resulting newParms are then evaluated, from left to right,
   * in the context of the numerical assignments in the input map to give additional
   * numerical assignments that are then added to the context for the next evaluation.
   * Finally the expressions in the processed devParms and cmdParms are evaluated
   * in the final context, with the resulting values saved in the devParmsList and
   * cmdParmsList fields, with null values set both for empty expressions and for
   * expressions that evaluate to a negative numerical value.
   */
  public void evaluate( Map< String, Long > map, int index )
  { 
    // Clone map so that new parameters do not affect original
    Map< String, Long > newMap = new HashMap< String, Long >();
    for ( String s : map.keySet() )
      newMap.put( s, map.get( s ) );
    NameEngine currentParameters = new NameEngine( newMap );
    Executor.Parameters eParms = executor.preprocessParms( index );

    if ( eParms.newParms != null )
    {
      NameEngine executorParameters = null;
      try
      {
        executorParameters = new NameEngine( eParms.newParms );
        if ( executorParameters != null ) 
        { 
          for ( Map.Entry< String, Expression > entry : executorParameters )
          { 
            String name = entry.getKey(); 
            Expression exp = entry.getValue(); 
            long value = exp.toLong( currentParameters );
            newMap.put( name, value );
            currentParameters = new NameEngine( newMap );
          } 
        }
      }
      catch ( InvalidNameException | NameUnassignedException e )
      {
        e.printStackTrace();
      }
    }

    if ( eParms.devParms != null )
    {
      devParmList = new ArrayList< Integer >();
      evaluateParms( eParms.devParms, devParmList, currentParameters );
    }
    if ( executor.parms.cmdParms != null )
    {
      cmdParmList = new ArrayList< Integer >();
      evaluateParms( eParms.cmdParms, cmdParmList, currentParameters );
    }
  }
  
  private void evaluateParms( List< String > parms, List< Integer > values, NameEngine engine )
  {
    for ( String parm : parms )
    {
      if ( parm.isEmpty() )
      {
        values.add( null );
        continue;
      }
      
      Expression exp = Expression.newExpression( parm );
      long value = 0;
      try
      {
        value = exp.toLong( engine );
      }
      catch ( NameUnassignedException e )
      {
        System.err.println( "*** Error: Unassigned name" );
        break;
      }
      values.add( value >= 0 ? ( int )value : null );
    }
  }

  public LearnedSignalDecode( LearnedSignalDecode decode )
  {
    protocolName = decode.protocolName;
    device = decode.device;
    subDevice = decode.subDevice;
    obc = decode.obc;
    hex = decode.hex;
    miscMessage = decode.miscMessage;
    errorMessage = decode.errorMessage;
    ignore = decode.ignore;
  }
  
  /**
   * Returns the hex value for this learned signal in the specified protocol,
   * preserving the OBC of the signal.
   */
  public Hex getProtocolHex( List< String > error )
  {
    Protocol protocol = null;
    if ( executor == null || ( protocol = executor.protocol ) == null ) return null;
    Value[] values = new Value[ protocol.cmdParms.length ];
    for ( int i = 0; i < values.length; i++ )
    {
      values[ i ] = new Value( null, protocol.cmdParms[ i ].getDefaultValue() );
    }
    Hex pHex = new Hex( protocol.getDefaultCmdLength() );
    CmdParameter[] parms = protocol.cmdParms;
    if ( cmdParmList != null )
    {
      for ( int i = 0; i < parms.length && i < cmdParmList.size(); i++ )
      {
        Integer listVal = cmdParmList.get( i );
        if (  listVal != null )
        {
          values[ i ] = new Value( listVal );
        }
      }
    }
    else
    {
      for ( int i = 0; i < parms.length; i++ )
      {
        if ( parms[ i ].getName().toUpperCase().startsWith( "OBC" ) )
        {
          values[ i ] = new Value( obc );
          break;
        }
      }
    }
    
    try
    {
      for ( int i = 0; i < protocol.cmdTranslators.length; i++ )
      {
        for ( int j = 0; j < values.length; j++ )
        {
          protocol.cmdTranslators[ i ].in( values, pHex, protocol.devParms, j );
        }
      }
    }
    catch ( IllegalArgumentException ex )
    {
      pHex = null;
      error.add( "" );
      error.add( ex.getMessage() );
    }
    return pHex;
  }
  
/*  
  public Hex getSignalHex()
  {
    if ( hex == null )
    {
      return null;
    }
    Hex sHex = new Hex( hex.length );
    for ( int i = 0; i < hex.length; i++ )
    {
      sHex.set( ( short )hex[ i ], i );
    }
    return sHex;
  }
*/
  
  /**
   * If there is a "(" in the name, it removes it and all that follows.  In the
   * result it removes all characters, including spaces, other than alphanumeric 
   * and returns the result in lower case, to create a simplified name for matching.
   */
  public static String getMatchName( String name )
  {
    int ndx = name.indexOf( "(" );
    if ( ndx >= 0 )
      name = name.substring( 0, ndx );            // remove parenthesis and all that follows
    name = name.replaceAll( "\\W|_|\\s", "" );  // remove all except alphanumeric chars
    return name.toLowerCase();
  }
  
  /**
   * The NamedProtocol input is an IrpTransmogrifier protocol, eString is the
   * value of a uei-executor parameter for that protocol (needed as there may
   * be more than one uei-executor parameter entry for a single protocol).  If
   * eString is null then it is set to the value of the first uei-executor entry.
   * 
   * The output is an Executor structure.  Initially the following fields are set:
   * protocol, qualifier, sequencer and all four fields of parms.  Note that protocol
   * is set to an RMIR protocol that is actually an executor for the NamedProtocol.
   * The setSelectors method is then used to set the remaining Executor fields,
   * which are selectorList, nameList and choiceList.
   */
  public static Executor getExecutor( NamedProtocol np, String eString )
  {
    IrpDatabase tmDatabase = LearnedSignal.getTmDatabase();
    if ( np == null || np.getName() == null || tmDatabase == null ) return null;
    String npName = np.getName();
    if ( eString == null )
    {
      eString = tmDatabase.getFirstProperty( npName, "uei-executor" );
    }
    if ( eString == null ) return null;
    
    String parmString = null;
    Executor executor = new Executor();
    eString = eString.trim();
    int ndx1 = eString.indexOf( "[" );
    int ndx2 = eString.lastIndexOf( "]" );
    if ( ndx1 >= 0 && ndx2 > ndx1 )
    {
      parmString = eString.substring( ndx1 + 1, ndx2 ).trim();
      eString = eString.substring( 0, ndx1 ).trim();
    }
    
    ndx1 = eString.indexOf( "{" );
    ndx2 = eString.lastIndexOf( "}" );
    if ( ndx1 >= 0 && ndx2 > ndx1 )
    {
      // Include the braces in the string
      executor.parms.newParms = eString.substring( ndx1, ndx2 + 1 ).trim();
      eString = eString.substring( 0, ndx1 ).trim();
    }

    ndx1 = eString.indexOf( "(" );
    ndx2 = eString.lastIndexOf( ")" );
    String nString = "";
    if ( ndx1 >= 0 && ndx2 > ndx1 )
    {
      nString = eString.substring( ndx1 + 1, ndx2 ).trim();
      eString = eString.substring( 0, ndx1 ).trim();
    }
    
    ndx1 = nString.indexOf( ";" );
    String qString = "";
    if ( ndx1 >= 0 )
    {
      qString = nString.substring( ndx1 + 1 ).trim();
      nString = nString.substring( 0, ndx1 ).trim();     
    }
    
    if ( !nString.isEmpty() )
      npName = nString;
    
    ndx1 = qString.indexOf( ";" );
    String sString = "";
    if ( ndx1 >= 0 )
    {
      sString = qString.substring( ndx1 + 1 ).trim();
      qString = qString.substring( 0, ndx1 ).trim(); 
    }
    
    if ( !qString.isEmpty() )
      executor.qualifier = qString;
    if ( !sString.isEmpty() )
      executor.sequencer = sString.replaceAll( "\\s", "" );
    
    if ( parmString != null )
    {
      String devParmString = parmString;
      String cmdParmString = null;
      int ndx = parmString.indexOf( ";" );
      if ( ndx >= 0 )
      {
        devParmString = parmString.substring( 0, ndx ).replaceAll( "\\s", "" );
        cmdParmString = parmString.substring( ndx + 1 ).replaceAll( "\\s", "" );
      }
      if ( devParmString != null && !devParmString.isEmpty() )
      {
        executor.parms.devParms = new ArrayList< String >();
        executor.parms.devIndices = new ArrayList< Integer >();
        parseParameterString( devParmString, executor.parms.devParms, executor.parms.devIndices );
      }
      if ( cmdParmString != null && !cmdParmString.isEmpty() )
      {
        executor.parms.cmdParms = new ArrayList< String >();
        parseParameterString( cmdParmString, executor.parms.cmdParms, null );
      }
    }
    
    QualifiedID qid = new QualifiedID( eString );
    Hashtable< Hex, List< Protocol >> byPid = ProtocolManager.getProtocolManager().getByPID();
    List< Protocol > protList = new ArrayList< Protocol >();
    String matchName = getMatchName( npName );
    Protocol protocol = null;
    for ( Protocol test : byPid.get( qid.pid ) )
    {
      if ( test.getVariantName().equals( qid.variantName ) )
      {
        protList.add( test );
      }
    }
    if ( protList.size() > 1 )
    {
      for ( Protocol test : protList )
      {
        if ( getMatchName( test.getName() ).equals( matchName ) )
        {
          protocol = test;
          break;
        }
      }
    }
    else if ( protList.size() == 1 )
    {
      protocol = protList.get( 0 );
    }
    protocol.reset();
    executor.protocol = protocol;
    executor.setSelectors();
    return executor;
  }
  
  public static void parseParameterString( String parmString, List< String > parmList, List< Integer > indexList )
  {
    if ( parmString == null || parmList == null )
      return;

    // Parse, returning separator as token so that two consecutive separators can be treated
    // as separated by an empty string.
    StringTokenizer st = new StringTokenizer( parmString, ",", true );
    String parm = "";
    int index = 0;
    String lastToken = ",";
    while ( st.hasMoreTokens() )
    {
      String token = st.nextToken();
      if ( token.equals( "," ) )
      {
        if ( lastToken.equals( "," ) )
        {
          parmList.add( "" );
          if ( indexList != null )
            indexList.add( index );
          index++;
        }
      }
      else
      {
        parm = token;
        int ndx = -1;
        while ( ( ndx = parm.indexOf( "|||" ) ) >= 0 )
        {
          parmList.add( parm.substring( 0, ndx ) );
          if ( indexList != null )
            indexList.add( index );
          parm = parm.substring( ndx + 3 );
        }
        parmList.add( parm );
        if ( indexList != null )
          indexList.add( index );
        index++;
      }
      lastToken = token;
    }
  }
  
  public static boolean displayErrors( String protocolName, List< List< String > > failedToConvert )
  {
    String message = "<html>The following learned signals could not be converted for use with the " + protocolName
        + " protocol.<p>If you need help figuring out what to do about this, please post<br>"
        + "a question in the JP1 Forums at http://www.hifi-remote.com/forums</html>";

    JPanel panel = Protocol.getErrorPanel( message, failedToConvert );
    String[] buttonText =
      {
        "Continue conversion", "Abort conversion"
      };
    int rc = JOptionPane.showOptionDialog( null, panel, "Protocol Conversion Error", JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE, null, buttonText, buttonText[ 0 ] );
    return rc == JOptionPane.YES_OPTION;
  }

  public boolean isMatch()
  {
    return match;
  }

  /** The protocol name. */
  public String protocolName = null;

  /** The device. */
  public int device = 0;

  /** The sub device. */
  public int subDevice = -1;  // denotes null

  /** The obc. */
  public int obc = 0;

  /** The hex. */
  public int[] hex;

  public List< Integer > devParmList = null;
  public List< Integer > cmdParmList = null;
  public Executor executor = null;
  public Decode decode = null;
  public static HashMap< NamedProtocol, Executor > defaultExecutorMap = new HashMap< NamedProtocol, LearnedSignalDecode.Executor >();
  
  /** The misc message. */
  public String miscMessage = null;

  /** The error message. */
  public String errorMessage = null;

  public boolean ignore = false;
  public boolean match = false;
}

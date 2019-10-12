package com.hifiremote.jp1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
import com.hifiremote.jp1.Executor.ExecutorWrapper;
import com.hifiremote.jp1.Executor.ExecutorWrapper.BracketData;
import com.hifiremote.jp1.ProtocolManager.QualifiedID;

public class LearnedSignalDecode
{
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
    NameEngine engine = new NameEngine( map );
    
    if ( executor == null )
    {
      executor = defaultExecutorMap.get( np );
      if ( executor == null )
      {
        executor = getExecutor( np, null );
        if ( executor != null )
          defaultExecutorMap.put( np, executor );
        else
          return;
      }
    }
    this.executor = executor;
    
    String commentItem = null;
    if ( executor.wrapper.assignments != null )
    {
      try
      {
        decode = executor.wrapper.fixDecode(protocolName, np, engine);
        map = decode.getMap();
      }
      catch ( Exception e )
      {
        e.printStackTrace();
      }
      if ( executor.wrapper.protocolName != null )
        protocolName = executor.wrapper.protocolName;
      if ( executor.wrapper.commentItem != null )
        commentItem = executor.wrapper.commentItem;
    }

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
    if ( commentItem != null )
    {
      if ( !miscMessage.isEmpty() )
        miscMessage += ", ";
      miscMessage += commentItem;
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

    // The entire choiceList is run through, with the preferred choice being the one,
    // or the first if more than one, in which fewest empty device parameter values
    // would be set.
    int preferredChoice = 0;
    int countOfNewValues = matchDevParms.length + 1;
    for ( int i = 0; i <  Math.max( executor.choiceList.size(), 1 ); i++ )
    {
      evaluate( map, i );
      Value[] testParms = getDevParmValues( decode );
      if ( testParms == null )
        continue;   // values were not self-consistent
      boolean ok = true;
      int count = 0;
      for ( int j = 0; j < matchDevParms.length; j++ )
      {
        if ( j >= testParms.length )
        {
          // this case should not occur, as both lists should be same length
          ok = false;  
          break;
        }
        Integer tInt = ( Integer )testParms[ j ].getUserValue();
        Integer mInt = ( Integer )matchDevParms[ j ].getUserValue();
        if ( tInt != null && mInt != null && tInt.intValue() != mInt.intValue())
        {
          ok = false;
          break;
        }
        if ( tInt != null && mInt == null )
        {
          count++;
        }
      }
      if ( !ok ) 
        continue;
      match = true;
      if ( count < countOfNewValues )
      {
        countOfNewValues = count;
        preferredChoice = i;
      }
      if ( countOfNewValues == 0 )
      {
        // No point in looking further, as no choice can give less than this
        break;
      }
    }
    evaluate( map, preferredChoice );
    Value[] testParms = getDevParmValues( decode );
    for ( int j = 0; j < matchDevParms.length; j++ )
    {
      Integer tInt = ( Integer )testParms[ j ].getUserValue();
      if ( tInt != null )
        matchDevParms[ j ].setValue( tInt );
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
      String obcMatch = LearnedSignalDecode.getMatchName( "OBC" );
      for ( int i = 0; i < parms.length; i++ )
      {
        String parmMatch = LearnedSignalDecode.getMatchName( parms[i].getName() );
        if ( parmMatch.equals( obcMatch ) )
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
   * If there is an opening bracket in the name, this removes it and all that follows.
   * In the result it removes all characters, including spaces, other than alphanumeric 
   * and returns the result in lower case, to create a simplified name for matching.
   */
  public static String getMatchName( String name )
  {
    String brackets = "([{";
    for ( int pos = 0; pos < name.length(); pos++ )
    {
      char ch = name.charAt( pos );
      int ndx = brackets.indexOf( ch );
      if ( ndx >= 0 )
      {
        // remove bracket and all that follows
        name = name.substring( 0, pos );
        break;
      }
    }           
    name = name.replaceAll( "\\W|_|\\s", "" );  // remove all except alphanumeric chars
    return name.toLowerCase();
  }
  
  /**
   * The NamedProtocol input is an IrpTransmogrifier protocol, wrapper is an
   * ExecutorWrapper for that protocol (needed as there may be more than one
   * ExecutorWrapper for a single protocol).  If wrapper is null then it is 
   * set to the wrapper from the first uei-executor entry.
   * 
   * The output is an Executor structure.  Initially the following fields are set:
   * protocol, qualifier, sequencer and all four fields of parms.  Note that protocol
   * is set to an RMIR protocol that is actually an executor for the NamedProtocol.
   * The setSelectors method is then used to set the remaining Executor fields,
   * which are selectorList, nameList and choiceList.
   */
  public static Executor getExecutor( NamedProtocol np, ExecutorWrapper wrapper )
  {
    IrpDatabase tmDatabase = LearnedSignal.getTmDatabase();
    if ( np == null || np.getName() == null || tmDatabase == null ) return null;
    String npName = np.getName();
    if ( wrapper == null )
    {
      List< ExecutorWrapper > wrappers = LearnedSignal.getExecutorWrappers( np );
      if ( wrappers != null && wrappers.size() > 0 )
        wrapper = wrappers.get( 0 );
    }
    if ( wrapper == null ) return null;
    
    Executor executor = new Executor();
    executor.wrapper = wrapper;
    int start = 0;
    int qidEnd = -1;
    int ndx = 0;
    BracketData bd = null;
    while ( ( bd  = wrapper.getBrackettedData( start ) ) != null )
    {
      if ( qidEnd < 0 )
        qidEnd = bd.start;
      switch ( bd.type )
      {
        case 0:  // Parentheses ()      
          ndx = bd.text.indexOf( ";" );
          if ( ndx >= 0 )
          {
            executor.name = bd.text.substring( 0, ndx ).trim();  
            bd.text = bd.text.substring( ndx + 1 ).trim();
            ndx = bd.text.indexOf( ";" );
            if ( ndx >= 0 )
            {
              executor.qualifier = bd.text.substring( 0, ndx ).replaceAll( "\\s", "" );
              executor.sequencer = bd.text.substring( ndx + 1 ).replaceAll( "\\s", "" );
            }
            else
            {
              executor.qualifier = bd.text.replaceAll( "\\s", "" );
            }
          }
          else
          {
            executor.name = bd.text.trim();
          }
          if ( executor.name != null && executor.name.isEmpty() )
            executor.name = null;
          if ( executor.qualifier != null && executor.qualifier.isEmpty() )
            executor.qualifier = null;
          if ( executor.sequencer != null && executor.sequencer.isEmpty() )
            executor.sequencer = null;
          break;
        case 1:  // Brackets []
          String devParmString = null;
          String cmdParmString = null;
          ndx = bd.text.indexOf( ";" );
          if ( ndx >= 0 )
          {
            devParmString = bd.text.substring( 0, ndx ).replaceAll( "\\s", "" );
            cmdParmString = bd.text.substring( ndx + 1 ).replaceAll( "\\s", "" );
          }
          else
          {
            devParmString = bd.text.replaceAll( "\\s", "" );
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
          break;
        case 2:  // Braces {}
          executor.parms.newParms = "{" + bd.text.replaceAll( "\\s", "" ) + "}";
          break;
      }
      start = bd.end;
    }
    
    String qidString = null;
    if ( qidEnd >= 0 )
      qidString = wrapper.executorDescriptor.substring( 0, qidEnd ).replaceAll( "\\s", "" );
    else
      qidString = wrapper.executorDescriptor.replaceAll( "\\s", "" );
    
    QualifiedID qid = new QualifiedID( qidString );
    Hashtable< Hex, List< Protocol >> byPid = ProtocolManager.getProtocolManager().getByPID();
    List< Protocol > protList = new ArrayList< Protocol >();
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
        if ( executor.name != null && test.getName().equals( executor.name )
            || executor.name == null && getMatchName( test.getName() ).equals( getMatchName( npName  ) ) )
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
  public int device = -1;     // denotes null

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
  public static HashMap< NamedProtocol, Executor > defaultExecutorMap = new HashMap< NamedProtocol, Executor >();
  
  /** The misc message. */
  public String miscMessage = null;

  /** The error message. */
  public String errorMessage = null;

  public boolean ignore = false;
  public boolean match = false;
}

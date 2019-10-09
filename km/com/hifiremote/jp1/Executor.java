package com.hifiremote.jp1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.harctoolbox.irp.Expression;
import org.harctoolbox.irp.InvalidNameException;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpInvalidArgumentException;
import org.harctoolbox.irp.NameEngine;
import org.harctoolbox.irp.NameUnassignedException;
import org.harctoolbox.irp.NamedProtocol;
import org.harctoolbox.irp.UnsupportedRepeatException;
import org.harctoolbox.irp.Decoder.Decode;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Executor
{
  public Protocol protocol = null;
  public ExecutorWrapper wrapper = null;
  public Parameters parms = new Parameters();
  public String name = null;
  
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
   * order is unspecified, but it can be specified by giving a sequencer string.
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
  
  public static class ExecutorWrapper
  {
    private static final String RM_NAMESPACE = "https://sourceforge.net/projects/controlremote/files/RemoteMaster"; 

    public static class BracketData
    {
      public int type = 0;
      public int start = 0;
      public int end = 0;
      public String text = null;
    }
    
    public String protocolName = null;
    public String executorDescriptor = null;
    public String commentItem = null;
    public final Map<String, Expression> assignments;
    
    public BracketData getBrackettedData( int start )
    {
      String brackets = "()[]{}";
      BracketData bracketID = null;
      if ( executorDescriptor == null || start >= executorDescriptor.length() )
        return null;
      while ( true )
      {
        char ch = executorDescriptor.charAt( start );
        int ndx = brackets.indexOf( ch );
        if ( ndx >= 0 && ( ndx & 1 ) == 0 )
        {
          bracketID = new BracketData();
          bracketID.type = ndx >> 1;
          bracketID.start = start;
          break;
        }
        if ( ++start == executorDescriptor.length() )
        {
          // No opening bracket found
          return null;
        }
      }
      int count = 0;
      while ( true )
      {
        char ch = executorDescriptor.charAt( start );
        int ndx = brackets.indexOf( ch );
        if ( ndx >= 0 && ( ndx >> 1 ) == bracketID.type )
        {
          // Add +1/-1 to count for opening/closing bracket
          count += 1 - 2*( ndx & 1 );
        }
        if ( count == 0 )
        {
          bracketID.end = start;
          bracketID.text = executorDescriptor.substring( bracketID.start + 1, bracketID.end );
          return bracketID;
        }
        if ( ++start == executorDescriptor.length() )
        {
          // No matching closing bracket found
          return null;
        }
      }
    }
    
    public ExecutorWrapper( String executorDescriptor )
    {
      this.executorDescriptor = executorDescriptor;
      assignments = null;
    }

    private ExecutorWrapper(DocumentFragment fragment) 
    { 
      assignments = new LinkedHashMap<>(4); // Important: keeps the order things are put in. 
      protocolName = null; 
      NodeList children = fragment.getChildNodes(); 

      // Finding the deployment element 
      Element deployment = null; 
      for (int i = 0; i < children.getLength(); i++) { 
        Node node = children.item(i); 
        if (node.getNodeType() != Node.ELEMENT_NODE) 
          continue; 
        Element e = (Element) node; 
        if (e.getLocalName().equals("deployment")) 
        { 
          deployment = e;
          break; 
        } 
      } 
      if (deployment == null) 
        return; 

      // Getting the assignments 
      NodeList assignmentNodes = deployment.getElementsByTagNameNS(RM_NAMESPACE, "assignment"); 
      for (int i = 0; i < assignmentNodes.getLength(); i++) { 
        Element e = (Element) assignmentNodes.item(i); 

        // Get and parse the Expression contained therein 
        Expression exp = Expression.newExpression(e.getTextContent()); 

        // and stuff it into the map 
        String paramName = e.getAttribute("target"); 
        assignments.put(paramName, exp); 
      } 

      executorDescriptor = deployment.getAttribute( "executor" );
      NodeList nl = deployment.getElementsByTagNameNS(RM_NAMESPACE, "protocolName"); 
      if (nl.getLength() > 0) 
        protocolName = nl.item(0).getTextContent();
      nl = deployment.getElementsByTagNameNS(RM_NAMESPACE, "commentItem"); 
      if (nl.getLength() > 0) 
        commentItem = nl.item(0).getTextContent();
      
    } 

    private void fixParameters(NameEngine nameEngine) 
    { 
      for ( Entry< String, Expression > assgnmnt : assignments.entrySet() )
      {
        try 
        { 
          String paramName = assgnmnt.getKey(); 
          long value = assgnmnt.getValue().toLong(nameEngine); 
          nameEngine.define(paramName, value); 
        } 
        catch (NameUnassignedException | InvalidNameException ex) 
        { 
          System.err.println( "*** Error: exception in ExecutorWrapper.fixParameters" );
        } 
      }; 
    }

    public Decode fixDecode(String protocolName, NamedProtocol namedProtocol, NameEngine nameEngine) 
        throws InvalidNameException, UnsupportedRepeatException, NameUnassignedException, 
        IrpInvalidArgumentException 
    { 
      if (this.protocolName != null) 
        protocolName = this.protocolName; 
      fixParameters(nameEngine); 
      NamedProtocol np = new NamedProtocol(protocolName, namedProtocol.getIrp(), null); // inefficient... 
      Decode fixedDecode = new Decode(np, nameEngine.toMap(), -1, -1, 0); 
      return fixedDecode; 
    }
  }

  public static class ExecutorWrapperDatabase 
  { 
    private final Map<String, List<ExecutorWrapper>> map;
    
    private static List<ExecutorWrapper> parseList(List<DocumentFragment> exec) 
    { 
      List<ExecutorWrapper> result = new ArrayList<>(16);
      for ( DocumentFragment fragment : exec )
      { 
        result.add(new ExecutorWrapper(fragment)); 
      }; 
      return result; 
    } 

    ExecutorWrapperDatabase(IrpDatabase irpDatabase) 
    { 
      map = new HashMap<>(16);
      for ( String protName : irpDatabase.getKeys())
      {
        List<DocumentFragment> exec = irpDatabase.getXmlProperties(protName, "uei-executor"); 
        if (exec != null) 
        {           
          List<ExecutorWrapper> lies = parseList(exec); 
          map.put(protName, lies); 
        } 
      }; 
    } 

    List<ExecutorWrapper> get(String name) 
    { 
      return map.get(name.toLowerCase(Locale.US)); 
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


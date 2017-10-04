package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class LearnedSignalTimingSummaryDialog extends JDialog implements ActionListener
{
  private final static Class< ? >[] colClasses =
  {
    BurstPair.class, String.class
  };
  
  private final static String[] colPrototypeNames =
    {
          "Burst Pair_____", "Coding________"
    };
  
  private final static String[] colNames =
  {
        "Burst Pair", "Coding"
  };

  public class TranslationTableModel extends AbstractTableModel
  { 
    @Override
    public Class< ? > getColumnClass( int col )
    {
      if ( col < colClasses.length )
      {
        return colClasses[ col ];
      }
      else
      {
        return null;
      }
    }
    
    @Override
    public String getColumnName( int col )
    {
      if ( col < colNames.length )
      {
        return colNames[ col ];
      }
      else
      {
        return null;
      }
    }
    
    @Override
    public boolean isCellEditable( int row, int col )
    {
      return col == 1 && display < 2;
    }
    
    @Override
    public int getRowCount()
    {
      return burstList.size();
    }

    @Override
    public int getColumnCount()
    {
      return 2;
    }

    @Override
    public Object getValueAt( int row, int column )
    {
      BurstPair key = row < burstList.size() ? burstList.get( row ) : null;
      switch ( column )
      {
        case 0:
          return key;
        case 1:          
          return key == null ? null : translation[ display & 1 ].get( key );
        default:
          return null;
      }
    }
    
    @Override
    public void setValueAt( Object value, int row, int column )
    {
      BurstPair key = burstList.get( row );
      switch ( column )
      {
        case 1:
          String s = ( ( String )value ).trim();
          if ( s == null || s.isEmpty() )
          {
            translation[ display & 1 ].remove( key );
          }
          else
          {
            translation[ display & 1 ].put( key, s );
          }
          break;
      }
      fireTableCellUpdated( row, column );
      return;
    }
    
    public void set()
    {
      burstRoundBox.setEnabled( display < 2 );
      parityBox.setEnabled( display < 2 );
      Collections.sort( burstList, new Comparator< BurstPair >()
      {
        @Override
        public int compare( BurstPair bp1, BurstPair bp2 )
        {
          if ( Math.abs( bp1.b1 ) < Math.abs(  bp2.b1 ) )
            return -1;
          else if ( Math.abs( bp1.b1 ) > Math.abs( bp2.b1 ) )
            return 1;
          else if ( Math.abs( bp1.b2 ) < Math.abs(  bp2.b2 ) )
            return -1;
          else if ( Math.abs( bp1.b2 ) > Math.abs(  bp2.b2 ) )
            return 1;
          else if ( bp1.b1 < bp2.b1 )
            return -1;
          else if ( bp1.b1 > bp2.b1 )
            return 1;
          else
            return 0;        
        }
      } );
      fireTableDataChanged();
    }
  }

  private class BurstPair
  {
    public int b1;
    public int b2;
    
    public BurstPair( int b1, int b2 )
    {
      this.b1 = b1;
      this.b2 = b2;
    }
    
    @Override
    public boolean equals( Object obj )
    {
      if ( obj == null || !( obj instanceof BurstPair ) )
      {
        return false;
      }
      BurstPair bp = ( BurstPair )obj;
      return ( b1 == bp.b1 && b2 == bp.b2 );
    }
    
    @Override
    public int hashCode()
    {
      return Objects.hash( b1, b2 );
    }
    
    private String signed( int b )
    {
      String sign = b > 0 ? "+" : "";
      return sign + b;
    }
    
    @Override
    public String toString()
    {
      return signed( b1 ) + " " + signed( b2 );
    }
  }
  
  public static void showDialog( Component locationComp, RemoteConfiguration config )
  {
    if ( dialog == null )
      dialog = new LearnedSignalTimingSummaryDialog( locationComp );
    dialog.config = config;
    dialog.display = 0;
    dialog.translation[ 0 ].clear();
    dialog.translation[ 1 ].clear();
    
    dialog.paritySettings = new int[]{ 0, 0 };    // TESTING  comment out these four
    dialog.roundSettings = new String[]{ null, null };
    dialog.burstRoundBox.setText( null );
    dialog.parityBox.setSelectedIndex( 0 );
    
    dialog.rawButton.setSelected( true );
    dialog.notePanel.setVisible( !RemoteMaster.suppressTimingSummaryInfo.isSelected() );
    dialog.generateSummary();
    dialog.pack();
    dialog.setLocationRelativeTo( locationComp );
    if ( bounds != null )
    {
      dialog.setBounds( bounds );
    }
    dialog.setVisible( true );
  }

  public static void reset()
  {
    if ( dialog != null )
    {
      dialog.dispose();
      dialog = null;
    }
  }

  private LearnedSignalTimingSummaryDialog( Component c )
  {
    super( ( JFrame )SwingUtilities.getRoot( c ) );
    setTitle( "Learned Signal Timing Summary" );
    setModal( true );

    JComponent contentPane = ( JComponent )getContentPane();
    contentPane.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

    notePanel = new JPanel( new BorderLayout() );
    notePanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ),
        BorderFactory.createLineBorder( Color.GRAY ) ) );
    JTextArea noteTextArea = new JTextArea();
    noteTextArea.setEditable( false );
    noteTextArea.setLineWrap( true );
    noteTextArea.setWrapStyleWord( true );
    noteTextArea.setFont( ( new JLabel() ).getFont() );
    noteTextArea.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    noteTextArea.setText( 
        "There are two displays, Raw and Analyzed, and two modes, Data and Coded, selected together "
      + "by the radio buttons. The Raw display shows the burst pair timings as learned, the Analyzed "
      + "display splits bursts to display as bi-phase pairs when this interpretation is possible.  "
      + "The Data mode shows the burst timings, the Coded mode replaces burst pairs by coding strings "
      + "set in the Coding table.  Uncoded burst pairs remain shown as timings.  The Burst Pair "
      + "column in the Coding table gives an ordered list of all distinct burst pairs in the display.\n"
      + "The Parity and Round To boxes override the default settings but are overridden for individual "
      + "signals by the Advanced Details editor if rounding has been set there.  These boxes, and the Coding "
      + "table, can be edited only in Data mode.\n" 
      + "Use the \"Options > Suppress messages\" menu to suppress this message in future.");
    notePanel.add( noteTextArea, BorderLayout.CENTER );
    contentPane.add( notePanel, BorderLayout.PAGE_START );
    
    int rows = RemoteMaster.suppressTimingSummaryInfo.isSelected() ? 25 : 21;
    summaryTextArea = new JTextArea( rows, 80 );
    summaryTextArea.setEditable( false );
    summaryTextArea.setLineWrap( false );
    summaryScrollPane = new JScrollPane( summaryTextArea );
    summaryScrollPane.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Timing Summary" ), summaryScrollPane.getBorder() ) );
    contentPane.add( summaryScrollPane, BorderLayout.CENTER );

    Box bottomBox = Box.createVerticalBox();
    contentPane.add( bottomBox, BorderLayout.PAGE_END );

    // Add the action buttons
    JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
    bottomBox.add( buttonPanel );

    Box notes = Box.createVerticalBox();
    notes.add( new JLabel( "Notes: Parity and rounding here does not override selected analysis settings." ) );
    notes.add( new JLabel( "Also analyzed signals will only change if the new settings yields a valid analysis." ) );

    paritySettings = new int[]{ 0, 0 };
    roundSettings = new String[]{ null, null };
    
    parityBox = new JComboBox< String >( parityList );
    buttonPanel.add( notes );
    buttonPanel.add( new JLabel( "  Parity:" ) );
    buttonPanel.add( parityBox );
    buttonPanel.add( new JLabel( "  Round To: " ) );
    buttonPanel.add( burstRoundBox );
    buttonPanel.add( new JLabel( "   ") );
    parityBox.addActionListener( this );
    burstRoundBox.setColumns( 4 );
    burstRoundBox.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent e) {
        generateSummary();
      }
      public void removeUpdate(DocumentEvent e) {
        generateSummary();
      }
      public void insertUpdate(DocumentEvent e) {
        generateSummary();
      }
    });

    ButtonGroup group = new ButtonGroup();
    group.add( rawButton );
    group.add( analyzedButton );
    group.add( rawCodedButton );
    group.add( analyzedCodedButton );
    rawButton.addActionListener( this );
    rawButton.setToolTipText( "Display raw data for each signal" );
    rawButton.setSelected( true );
    rawCodedButton.addActionListener( this );
    rawCodedButton.setToolTipText( "Display raw data with burst pairs converted to codes" );
    analyzedButton.addActionListener( this );
    analyzedButton.setToolTipText( "Display the selected analysis for each signal" );
    analyzedCodedButton.addActionListener( this );
    analyzedCodedButton.setToolTipText( "Display analysis with burst pairs converted to codes" );
    saveButton.addActionListener( this );
    saveButton.setToolTipText( "Save Summary as .csv file" );
    buttonPanel.add( saveButton );
    okButton.addActionListener( this );
    okButton.setToolTipText( "Close the Summary" );
    buttonPanel.add( okButton );
    
    model = new TranslationTableModel();
    codingTable = new JTableX( model );
    JPanel codePanel = new JPanel( new BorderLayout() );
    codePanel.setBorder( BorderFactory.createTitledBorder( "Set burst codes:" ) );
    JScrollPane scrollPane = new JScrollPane( codingTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
    codePanel.add( scrollPane, BorderLayout.CENTER );
    contentPane.add( codePanel, BorderLayout.LINE_START );
    Box box = Box.createVerticalBox();
    box.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 5, 0, 0, 0 ),
        BorderFactory.createTitledBorder( "Select display:" ) ) );
    codePanel.add( box, BorderLayout.PAGE_END );
    box.add( rawButton );
    box.add( rawCodedButton );
    box.add( Box.createVerticalStrut( 5 ) );
    box.add( analyzedButton );
    box.add( analyzedCodedButton );
    TableColumnModel columnModel = codingTable.getColumnModel();
    JLabel l = new JLabel();
    int width = 0;
    for ( int i = 0; i < colPrototypeNames.length; i++ )
    {
      l.setText( colPrototypeNames[ i ]);
      width =  Math.max( width, l.getPreferredSize().width );
      TableColumn column = columnModel.getColumn( i );
      column.setMinWidth( width );
      column.setMaxWidth( width );
      column.setPreferredWidth( width );
    }

//    int sbWidth = scrollPane.getVerticalScrollBar().getPreferredSize().width;
    Dimension d = codingTable.getPreferredSize();
    d.width -= scrollPane.getVerticalScrollBar().getPreferredSize().width;
    codingTable.setPreferredScrollableViewportSize( d );
    translation[ 0 ] = new LinkedHashMap< BurstPair, String >();
    translation[ 1 ] = new LinkedHashMap< BurstPair, String >();
  }

  private void appendDurations( StringBuilder summary, String[] durationStrings, boolean autoCode, String intro )
  {
    boolean first = true;
    for ( String d: durationStrings )
    {
      if ( first )
      {
        first = false;
        summary.append( intro );
      }
      else
      {
        summary.append( "\n\t\t\t\t\t\tMore:\t" );
      }
      if ( display < 2 )
      {
        summary.append( d );
        addTranslationKeys( d, autoCode );
      }
      else
      {
        summary.append( durationsToCoding( d ) );
      }
    }
    summary.append( '\n' );
  }

  private void generateSummary()
  {
    int r = 1;
    String roundText = burstRoundBox.getText();
    roundSettings[ display & 1 ] = roundText;
    int parity = paritySettings[ display & 1 ];
    boolean roundingSet = false;
    if ( roundText != null && !roundText.isEmpty() )
    {
      try
      {
        r = Integer.parseInt( roundText );
        roundingSet = true;
      }
      catch (NumberFormatException e)
      {
        r = 1;
      }
    }
    if ( codingTable.getCellEditor() != null )
    {
      codingTable.getCellEditor().stopCellEditing();
    }

    List<LearnedSignal> signals = this.config.getLearnedSignals();
    Remote remote = this.config.getRemote();
    if ( display < 2 )
    {
      burstList.clear();
    }

    StringBuilder summary = new StringBuilder();
    summary.append( "LEARNED SIGNALS:\n" );
    summary.append( display == 0 ? "RAW TIMING DATA:\n" :
      display == 1 ? "SELECTED DATA ANALYSES:\n" :
        display == 2 ? "CODED RAW DATA:\n" :
        "CODED ANALYSES:\n" );
    summary.append( "#\tName\tDevice\tKey\tNotes\tFreq\t" );
    summary.append( display == 0 ? "Raw Timing Data\n" :
      display == 1 ? "Analyzed Timing Data\n" :
        display == 2 ? "Raw Timing Data with Coding\n" :
        "Analyzed Timing Data with Coding\n" );
    int i = 1;
    for ( LearnedSignal s: signals )
    {
      UnpackLearned ul = s.getUnpackLearned();
      summary.append( i++ );
      summary.append( '\t' );
      summary.append( s.getName() != null ? s.getName() : "" );
      summary.append( '\t' );
      summary.append( remote.getDeviceButton( s.getDeviceButtonIndex() ).getName() );
      summary.append( '\t' );
      summary.append( remote.getButtonName( s.getKeyCode() ) );
      summary.append( '\t' );
      summary.append( (s.getNotes() == null ? "" : s.getNotes()) );
      if ( ul.ok )
      {
        summary.append( '\t' );
        summary.append( ul.frequency );
        summary.append( '\t' );

        LearnedSignalTimingAnalyzer lsta = s.getTimingAnalyzer();
        LearnedSignalTimingAnalyzerBase analyzer = ( display & 1 ) == 0 ? lsta.getAnalyzer( "Raw Data" )
            : lsta.getSelectedAnalyzer();
        LearnedSignalTimingAnalysis analysis = null;
        boolean autoCode = analyzer.getName().equals( "Bi-Phase" );
     
        if ( ( roundingSet || parity > 0 ) && !analyzer.getIsRoundingLocked() )
        {
          // Either or both of rounding and parity is being forced.  We obey these settings only if
          // rounding has not been set through Advanced Details.
          analyzer.saveState();
          analyzer.setRoundTo( r );
          String displayAnalysisName = lsta.getSelectedAnalysisName();
          String[] analysisNames = analyzer.getAnalysisNames();
          if ( parity > 0 && ( ( display & 1 ) == 0 || !displayAnalysisName.toUpperCase().contains( parityList[ parity ].toUpperCase() ) ) )
          {
            // When parity is set, this covers all cases except that where the analyzer is the selected one
            // but the selected analysis has correct parity, when we can simply display the selected analysis
            // in the selected analyzer.
            if ( analyzer.getName().equals( "Raw Data" ) )
            {
              displayAnalysisName = parityList[ parity ];
            }
            else
            {
              displayAnalysisName = null;
              for ( String name : analysisNames )
              {
                if ( name.toUpperCase().contains( parityList[ parity ].toUpperCase() )  )
                {
                  displayAnalysisName = name;
                  break;
                }
              }
            }
          }
          else if ( parity == 0 && ( display & 1 ) == 0 
              && !lsta.getSelectedAnalyzer().getName().equals( "Raw Data" ) )
          {
            // When parity is default, the case not covered here is when the analyzer is the selected
            // one and this is the raw one.  In this case we can display the selected analysis.
            displayAnalysisName = "Even";
          }
          analysis = analyzer.getAnalysis( displayAnalysisName );
          if ( analysis == null )
          {
            analysis = analyzer.getPreferredAnalysis();
          }
          analyzer.restoreState();
        }
        else
        {
          // Neither rounding nor parity is being forced, but we have to obey the raw/analyzed choice.
          // If raw is chosen but this is not the selected one, we use Even parity.
          String displayAnalysisName = lsta.getSelectedAnalysisName();
          if ( ( display & 1 ) == 0  && !lsta.getSelectedAnalyzer().getName().equals( "Raw Data" ) )
          {
            displayAnalysisName = "Even";
          }
          analysis = analyzer.getAnalysis( displayAnalysisName );
          if ( analysis == null )
          {
            analysis = analyzer.getPreferredAnalysis();
          }
        }
        if ( analysis != null && ul.oneTime > 0 && ul.extra > 0 && ul.repeat == 0 )
        {
          appendDurations( summary, analysis.getOneTimeDurationStringList(), autoCode, "Once:\t" );
          appendDurations( summary, analysis.getExtraDurationStringList(), autoCode, "\t\t\t\t\t\tMore:\t" );
        }
        else if ( analysis != null )
        {
          String prefix= "";
          if ( ul.oneTime > 0 )
          {
            appendDurations( summary, analysis.getOneTimeDurationStringList(), autoCode, prefix+"Once:\t" );
            prefix = "\t\t\t\t\t\t";
          }
          if ( ul.repeat > 0 )
          {
            appendDurations( summary, analysis.getRepeatDurationStringList(), autoCode, prefix+"Repeat:\t" );
            prefix = "\t\t\t\t\t\t";
          }
          if ( ul.extra > 0 )
            appendDurations( summary, analysis.getExtraDurationStringList(), autoCode, prefix+"Extra:\t" );
        }
        else
          summary.append( "** No valid analysis **\n" );
      }
      else
        summary.append( "** No signal **\n" );
    }
    model.set();
    summaryText = summary.toString();
    summaryTextArea.setText( summaryText );
    dialog.validate();
    javax.swing.SwingUtilities.invokeLater( new Runnable()
    {
      public void run()
      {
        summaryScrollPane.getVerticalScrollBar().setValue( 0 );
      }
    } );
  }
  
  private void save( File file )
  {
    PrintWriter out;
    try
    {
      out = new PrintWriter( new BufferedWriter( new FileWriter( file ) ) );
      String temp = summaryText.substring( summaryText.indexOf( '#' ) );
      temp = temp.replaceAll( "\"", "\"\"" );
      temp = temp.replaceAll( "\t", "\",\"" );
      temp = temp.replaceAll( "\n", "\"\n\"" );
      out.print( "\"" + temp + "\"" );
      out.close();
    }
    catch ( IOException e )
    {
      e.printStackTrace();
    }
  }

  public void actionPerformed( ActionEvent event )
  {
    Object source = event.getSource();
    if ( source == okButton )
    {
      bounds = getBounds();
      setVisible( false );
    }
    else if ( source == saveButton )
    {
      String[] endings = { ".csv" };
      PropertyFile properties = JP1Frame.getProperties();
      File dir = properties.getFileProperty( "IRPath" );       
      RemoteMaster rm = ( RemoteMaster )SwingUtilities.getAncestorOfClass( RemoteMaster.class, this );

      if ( fileChooser == null )
      {
        fileChooser = new RMFileChooser( dir );
        fileChooser.setFileFilter( new EndingFileFilter( "Comma-separated values (*.csv)", endings ) );
      }
      int returnVal = fileChooser.showSaveDialog( rm );
      if ( returnVal == RMFileChooser.APPROVE_OPTION )
      {
        String name = fileChooser.getSelectedFile().getAbsolutePath();
        if ( !name.toLowerCase().endsWith( ".csv" ) )
        {
          name = name + ".csv";
        }
        File file = new File( name );
        dir = file.getParentFile();
        properties.setProperty( "IRPath", dir );
        int rc = JOptionPane.YES_OPTION;
        if ( file.exists() )
        {
          rc = JOptionPane.showConfirmDialog( rm, file.getName() + " already exists.  Do you want to replace it?",
              "Replace existing file?", JOptionPane.YES_NO_OPTION );
        }
        if ( rc == JOptionPane.YES_OPTION )
        {
          save( file );
        }
      }
    }
    else if ( source == rawButton && rawButton.isSelected() )
    {
      display = 0;
      burstRoundBox.setText( roundSettings[ 0 ] );
      parityBox.setSelectedIndex( paritySettings[ 0 ] );
      generateSummary();
    }
    else if ( source == analyzedButton && analyzedButton.isSelected() )
    {
      display = 1;
      burstRoundBox.setText( roundSettings[ 1 ] );
      parityBox.setSelectedIndex( paritySettings[ 1 ] );
      generateSummary();
    }
    else if ( source == rawCodedButton && rawCodedButton.isSelected() )
    {
      display = 2;
      burstRoundBox.setText( roundSettings[ 0 ] );
      parityBox.setSelectedIndex( paritySettings[ 0 ] );
      generateSummary();
    }
    else if ( source == analyzedCodedButton && analyzedCodedButton.isSelected() )
    {
      display = 3;
      burstRoundBox.setText( roundSettings[ 1 ] );
      parityBox.setSelectedIndex( paritySettings[ 1 ] );
      generateSummary();
    }
    else if ( source == parityBox )
    {
      paritySettings[ display & 1 ] = parityBox.getSelectedIndex();
      generateSummary();
    }
  }
  
  private void addTranslationKeys( String durationString, boolean autoCode )
  {
    StringTokenizer st = new StringTokenizer( durationString, ";" );
    while ( st.hasMoreTokens() )
    {
      String token = st.nextToken().trim();
      int pos = token.indexOf( ' ' );
      int p1 = 0;
      int p2 = 0;
      if ( pos >= 0 )
      {
        if ( token.contains( "No signal" ) )
        {
          continue;
        }
        else try
        {
          p1 = Integer.parseInt( token.substring( 0, pos ) );
          p2 = Integer.parseInt( token.substring( pos + 1 ).trim() );
          BurstPair bp = new BurstPair( p1, p2 );
          if ( !burstList.contains( bp ) )
          {
            burstList.add( bp );
          }
          if ( autoCode && p2 == -p1 && translation[ display & 1 ].get( bp ) == null )
          {
            translation[ display & 1 ].put( bp, p1 < 0 ? "0" : "1" );
          }
        }
        catch ( Exception e )
        {
          System.err.println( "Bad burst pair in string " + durationString );
        }
      }
    }
  }
  
  private String durationsToCoding( String durationString )
  {
    StringBuilder sb = new StringBuilder();
    StringTokenizer st = new StringTokenizer( durationString, ";" );
    BurstPair bp = null;
    int count = st.countTokens();
    String lastCode = null;
    for ( int i = 0; i < count; i++ )
    {
      String token = st.nextToken().trim();
      String code = null;
      int pos = token.indexOf( ' ' );
      int p1 = 0;
      int p2 = 0;
      if ( pos >= 0 )
      {
        if ( token.contains( "No signal" ) )
        {
          continue;
        }
        else try
        {
        p1 = Integer.parseInt( token.substring( 0, pos ) );
        p2 = Integer.parseInt( token.substring( pos + 1 ).trim() );
        bp = new BurstPair( p1, p2 );
        code = ( String )translation[ display & 1 ].get( bp );
        }
        catch ( Exception e )
        {
          System.err.println( "Bad burst pair in string " + durationString );
        }
      }
      if ( i > 0 && ( lastCode == null && code != null ))
      {
        sb.append( "; " + code );
      }
      else if ( i > 0 && code == null )
      {
        sb.append( "; " + token );
      }
      else if ( code != null )
      {
        sb.append( code );
      }
      else
      {
        sb.append( token );
      }
      lastCode = code;
    }
    return sb.toString();
  }

  private RemoteConfiguration config = null;

  private JTextField burstRoundBox = new JTextField();
  private JComboBox< String > parityBox = null;
  private JRadioButton rawButton = new JRadioButton( "Raw Data" );
  private JRadioButton analyzedButton = new JRadioButton( "Analyzed Data" );
  private JRadioButton rawCodedButton = new JRadioButton( "Raw Coded" );
  private JRadioButton analyzedCodedButton = new JRadioButton( "Analyzed Coded" );
  private int display = 0;
  @SuppressWarnings( "unchecked" )
  private LinkedHashMap< BurstPair, String >[] translation = ( LinkedHashMap< BurstPair, String >[] ) new LinkedHashMap[ 2 ];
  private List< BurstPair > burstList = new ArrayList< BurstPair >();
  private TranslationTableModel model = null;
  private JTableX codingTable = null;
  private JScrollPane summaryScrollPane = null;
  private JPanel notePanel = null;
  /** The ok button. */
  private JButton okButton = new JButton( "OK" );
  private JButton saveButton = new JButton ( "Save" );
  private String[] roundSettings = null;
  private int[] paritySettings = null;
  private String summaryText = null;

  private JTextArea summaryTextArea = null;
  private RMFileChooser fileChooser = null;

  /** The dialog. */
  private static LearnedSignalTimingSummaryDialog dialog = null;
  private static String[] parityList = { "", "Even", "Odd" };
  private static Rectangle bounds = null;

}

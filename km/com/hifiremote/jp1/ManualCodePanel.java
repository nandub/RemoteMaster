package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.hifiremote.jp1.ProtocolDataPanel.DisplayArea;

public class ManualCodePanel extends JPanel
{
  public class CodeTableModel extends AbstractTableModel
  {
    public int getRowCount()
    {
      return procs.length;
    }

    public int getColumnCount()
    {
      return colNames.length;
    }

    public String getColumnName( int col )
    {
      return colNames[ col ];
    }

    public Class< ? > getColumnClass( int col )
    {
      return classes[ col ];
    }

    @Override
    public boolean isCellEditable( int row, int col )
    {
      if ( displayProcessor != null
          && !procs[ row ].getEquivalentName().equals( displayProcessor.getEquivalentName() ) )
      {
        return false;
      }
      return ( col == 1 );
    }

    public Object getValueAt( int row, int col )
    {
      Hex hex = protocol.getCode( procs[ row ] );
      // We want dispHex to be the official code, not custom code when that is present.
      // Edit 13 Nov 2017: Don't know the reason for the above statement, which is
      // not consistent with the code that the disassemble button accesses, so I've
      // changed it for consistency to use custom code when present.
//      Hex dispHex = ( displayProtocol == null ) ? null : displayProtocol.code.get( procs[ row ].getEquivalentName() );
      Hex dispHex = ( displayProtocol == null ) ? null : displayProtocol.getCode( procs[ row ] );
      if ( dispHex == null )
      {
        dispHex = new Hex();
      }
      switch ( col )
      {
        case 0:
          return procs[ row ];
        case 1:
          return ( hex == null || hex.length() == 0 ) ? dispHex : hex;
//        case 2:
//          return itemLists[ row ];
        default:
          // There are no other columns but value 4 is used by cell renderer
          return hex == null || hex.length() == 0 || isEmpty[ row ] && hex.equals( dispHex );
      }
    }
    
    public void setValueAt( Object value, int row, int col )
    {  
      switch ( col )
      {
        case 1:
          Hex newCode = ( Hex )value;
          String procName = procs[ row ].getEquivalentName();
          if ( ( newCode != null ) && ( newCode.length() != 0 ) )
          {
            if ( !protocol.hasAnyCode() || protocol.getCode().size() == 1 && protocol.getCode().keySet().contains( procName ) )
            {
              owner.getDevicePanel().update( procName, newCode );
            }
          }
          else if ( codeWhenNull != null )
          {
            String title = "Code deletion";
            String message = "This protocol is not built in to the remote.  Do you want to restore\n"
                + "the code to the standard code for this protocol?\n\n"
                + "If you select NO then the protocol upgrade for this device upgrade\n"
                + "will be deleted.  The device upgrade will not function until you\n"
                + "restore the protocol upgrade, which you may do by deleting this\n"
                + "null entry and answering this question again.";
            boolean restore = ( JOptionPane.showConfirmDialog( null, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE ) == JOptionPane.YES_OPTION );
            if ( restore )
            {
              newCode = codeWhenNull;
            }
            else
            {
              newCode = new Hex();
            }
          }

          Processor proc = procs[ row ];
          protocol.setCode( newCode, proc );
//          CodeTableModel model = ( CodeTableModel )tablePanel.getCodeTable().getModel();
//          model.fireTableRowsUpdated( row, row );
          fireTableRowsUpdated( row, row );
          ProtocolDataPanel pdp = owner.getProtDataPanel();
          owner.getDevicePanel().enableButtons();
          if ( owner.getAssemblerPanel().isAssembled() )
          {
            pdp.pfMainPanel.setPFData( proc, newCode );
          }
          else
          {
            owner.getAssemblerModel().disassemble( newCode, proc );
          }

          procBox.setSelectedItem( proc );
          if ( proc instanceof S3C80Processor
              && ( ( S3C80Processor )proc ).testCode( newCode ) == S3C80Processor.CodeType.NEW )
          {
            proc = ProcessorManager.getProcessor( "S3F80" ); // S3C8+ code
          }
          owner.interpretPFPD();
          pdp.pfMainPanel.set();
          pdp.pdMainPanel.set();
          pdp.fnMainPanel.set();
          break;
        case 2:
          ManualSettingsPanel.getItemLists()[ row ] = ( List< AssemblerItem > )value;
          break;
      }
    }
    
    
    public void setCodeWhenNull( Hex codeWhenNull )
    {
      this.codeWhenNull = codeWhenNull;
    }

    private Hex codeWhenNull = null;
  }
  
  public class CodeCellRenderer extends DefaultTableCellRenderer
  {
    @Override
    public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus,
        int row, int col )
    {
      Component c = super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, col );
      CodeTableModel model = ( CodeTableModel )table.getModel();
      if ( isSelected )
      {
        c.setForeground( ( Boolean )model.getValueAt( row, 4 ) ? Color.YELLOW : Color.WHITE );
      }
      else
      {
        c.setForeground( ( Boolean )model.getValueAt( row, 4 ) ? Color.GRAY : Color.BLACK );
      }
      return c;
    }
  }
  
  public ManualCodePanel( ManualSettingsPanel owner )
  {
    this.owner = owner;
    setLayout( new BorderLayout() );
    codeModel = new CodeTableModel();
    codeTable = new JTableX( codeModel );
    codeTable.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    codeTable.getSelectionModel().addListSelectionListener( owner );
    JPanel panel = new JPanel( new BorderLayout() );

    JScrollPane scrollPane = new JScrollPane( codeTable );
    JPanel codePanel = new JPanel( new BorderLayout() );
    codePanel.setBorder( BorderFactory.createTitledBorder( "Protocol code" ) );
    codePanel.add( scrollPane, BorderLayout.CENTER );
    panel.add( codePanel, BorderLayout.CENTER );
    add( panel, BorderLayout.CENTER );
    noteArea = new DisplayArea( null, null );

    noteArea.setBorder( BorderFactory.createTitledBorder( "Notes: " ));   
    panel.add( noteArea, BorderLayout.PAGE_END );

    DefaultTableCellRenderer r = ( DefaultTableCellRenderer )codeTable.getDefaultRenderer( String.class );
    r.setHorizontalAlignment( SwingConstants.CENTER );
    codeTable.setDefaultEditor( Hex.class, new HexCodeEditor() );   
    //  codeTable.getSelectionModel().addListSelectionListener( this );
    
    codeTable.setLongToolTipTimeout();

    TableColumnModel columnModel = codeTable.getColumnModel();
    TableColumn column = columnModel.getColumn( 0 );
    JLabel l = ( JLabel )codeTable.getTableHeader().getDefaultRenderer()
        .getTableCellRendererComponent( codeTable, colNames[ 0 ], false, false, 0, 0 );
    int width = l.getPreferredSize().width;

    setProcs();

    for ( int i = 0; i < procs.length; i++ )
    {
      l.setText( procs[ i ].getFullName() );
      width = Math.max( width, l.getPreferredSize().width );
    }
    for ( int i = 0; i < procs.length; i++ )
    {
      column.setMinWidth( width );
      column.setMaxWidth( width );
      column.setPreferredWidth( width );
    }

    columnModel.getColumn( 1 ).setCellRenderer( new CodeCellRenderer() );

    codeTable.doLayout();
  }
  
  private void setProcs()
  {
    procs = ProcessorManager.getProcessors();
    int count = 0;
    for ( int i = 0; i < procs.length; i++ )
    {
      Processor proc = procs[ i ];
      if ( proc.getEquivalentName().equals( proc.getFullName() ) )
        ++count;
    }
    Processor[] uProcs = new Processor[ count ];
    count = 0;
    for ( int i = 0; i < procs.length; i++ )
    {
      Processor proc = procs[ i ];
      if ( proc.getEquivalentName().equals( proc.getFullName() ) )
        uProcs[ count++ ] = proc;
    }
    procs = uProcs;
    List< Processor > asmProcs = new ArrayList< Processor >(  );
    for ( Processor proc : procs )
    {
      if ( proc.getDataStyle() >= 0 )
      {
        // dataStyle == -1 for processors with MAXQ-format protocols
        asmProcs.add( proc );
        procBox.setModel( new DefaultComboBoxModel< Processor >( asmProcs.toArray( new Processor[ 0 ] ) ) );
      }
    }
  }
  
  public void setProtocol( ManualProtocol protocol )
  {
    this.protocol = protocol;
    isEmpty = new boolean[ procs.length ];
    for ( int i = 0; i < procs.length; i++ )
    {
      Hex hex = displayProtocol != null ? displayProtocol.getCode( procs[ i ] ) : null;
      isEmpty[ i ] = ( hex == null || hex.length() == 0 );
    }
  }
  
  public ManualProtocol getProtocol()
  {
    return protocol;
  }

  public String[] importProtocolCode( String string, boolean idOnly )
  {
    StringTokenizer st = new StringTokenizer( string, "\n" );
    String text = null;
    String processor = null;
    String name = null;
    String variantName = null;
    String pidStr = null;
    String procStr = null;
    while ( st.hasMoreTokens() )
    {
      while ( st.hasMoreTokens() )
      {
        text = st.nextToken();
        System.err.println( "got '" + text );
        if ( text.toUpperCase().startsWith( "UPGRADE PROTOCOL 0 =" ) )
        {
          StringTokenizer st2 = new StringTokenizer( text, "()=" );
          st2.nextToken(); // discard everything before the =
          pidStr = st2.nextToken().toUpperCase().trim();
          System.err.println( "Imported pid is " + pidStr );
          processor = st2.nextToken().toUpperCase().trim();
          System.err.println( "processorName is " + processor );
          if ( processor.startsWith( "S3C8" ) )
            processor = "S3C80";
          else if ( processor.startsWith( "S3F8" ) )
            processor = "S3F80";
          if ( st2.hasMoreTokens() )
          {
            name = st2.nextToken().trim();
            System.err.println( "importedName is " + name );
            int ndx = name.indexOf(  ':' );
            if ( ndx > 0 )
            {
              variantName = name.substring( ndx + 1 ).trim();
              name = name.substring( 0, ndx ).trim();
            }
          }
          break;
        }
      }
      if ( idOnly )
      {
        break;
      }
      if ( st.hasMoreTokens() )
      {
        text = st.nextToken(); // 1st line of code
        while ( st.hasMoreTokens() )
        {
          String temp = st.nextToken();
          if ( temp.trim().equals( "End" ) )
            break;
          text = text + ' ' + temp;
        }
        System.err.println( "getting processor with name " + processor );
        Processor p = ProcessorManager.getProcessor( processor );
        if ( p != null )
        {
          // processor = p.getFullName();
          processor = p.getEquivalentName();
          p = ProcessorManager.getProcessor( processor );
          if ( owner instanceof ManualSettingsPanel )
          {
            ManualSettingsPanel msp = ( ManualSettingsPanel )owner;
            msp.selectProcessor( p );
//          procBox.setSelectedItem( p );
//          pid.setValue( new Hex( pidStr ) );
//          name.setText( importedName );
          }
        }
        if ( procStr == null )
        {
          procStr = processor;
        }
        System.err.println( "Adding code for processor " + processor );
        System.err.println( "Code is " + text );
        for ( int i = 0; i < procs.length; i++ )
        {
          if ( procs[ i ] == p )
          {
            codeModel.setValueAt( new Hex( text ), i, 1 );
//            deviceText.setText( getProtocolText( true, true ) );
          }
        }
      }
    }
    return new String[]{ name, variantName, pidStr, procStr };
  }
  
  

  public void setDisplayProtocol( Protocol displayProtocol )
  {
    this.displayProtocol = displayProtocol;
  }
  
  public void setDisplayProcessor( Processor displayProcessor )
  {
    this.displayProcessor = displayProcessor;
  }

  public JTableX getCodeTable()
  {
    return codeTable;
  }

  private final static String[] colNames =
  {
      "Processor", "Protocol Code"
  };

  private final static Class< ? >[] classes =
  {
      Processor.class, Hex.class, List.class
  };
  
  private static Processor[] procs = new Processor[ 0 ];
  
  public static Processor[] getProcs()
  {
    return procs;
  }
  
  private static JComboBox< Processor > procBox = new JComboBox< Processor >();
  
  public static JComboBox< Processor > getProcBox()
  {
    return procBox;
  }

  public CodeTableModel getCodeModel()
  {
    return codeModel;
  }

  public DisplayArea getNoteArea()
  {
    return noteArea;
  }

  private ManualSettingsPanel owner = null;
  private JTableX codeTable = null;
  private CodeTableModel codeModel = null;
  private ManualProtocol protocol = null;
  private Protocol displayProtocol = null;
  private Processor displayProcessor = null;
  private DisplayArea noteArea = null;
  private boolean isEmpty[] = null;
}

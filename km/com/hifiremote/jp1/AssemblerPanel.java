package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.hifiremote.jp1.AssemblerOpCode.OpArg;
import com.hifiremote.jp1.AssemblerOpCode.Token;
import com.hifiremote.jp1.AssemblerTableModel.DisasmState;
import com.hifiremote.jp1.ProtocolDataPanel.DisplayArea;
import com.hifiremote.jp1.ProtocolDataPanel.Mode;
import com.hifiremote.jp1.RMProtocolBuilder.PBAction;
import com.hifiremote.jp1.assembler.CommonData;

public class AssemblerPanel extends JPanel implements ListSelectionListener, ItemListener
{
  public class EditorPanel extends JPanel implements ActionListener
  {
    public EditorPanel()
    {
      setLayout( new GridLayout( 3, 5 ) );
      setMinimumSize( new Dimension( 10, 10 ) );
      add( insert );
      add( copy );
      add( new JLabel() );
      add( assemble );
      add( delete );
      add( cut );
      add( new JLabel() );;
      add( build );
      add( selectAll );
      add( paste );
      add( new JLabel() );;
      add( update );
      moveUp.addActionListener( this );
      moveDown.addActionListener( this );
      insert.addActionListener( this );
      delete.addActionListener( this );
      cut.addActionListener( this );
      copy.addActionListener( this );
      paste.addActionListener( this );
      paste.setEnabled( false );
      build.addActionListener( this );
      update.addActionListener( this );
      selectAll.addActionListener( this );
      assemble.addActionListener( this );
      toAssemblerButton.setToolTipText( "Import hex code of selected processors to Assembler for editing" );
      toAssemblerButton.addActionListener( this );

      insert.setToolTipText( "Inserts above selection a number of rows equal to the number selected." );
      delete.setToolTipText( "Deletes the rows containing selected cells." );
      copy.setToolTipText( "Copies to clipboard the rows containing selected cells." );
      cut.setToolTipText( "Copies to clipboard the rows containing selected cells, then deletes these rows." );
      paste.setToolTipText( "Inserts rows from clipboard above current selection." );
      selectAll.setToolTipText( "Selects all the rows of the currently selected listing." );
      assemble.setToolTipText( "Assembles binary code from assembler listing and updates protocol code with result." );
      build.setToolTipText( "Builds complete assembler listing for a protocol from data in Protocol Data tab." );
      update.setToolTipText( "Updates the data section of an assembler listing from Protocol Data tab." );
      JP1Frame frame = RemoteMaster.getFrame();
      cutItems = ( frame instanceof RemoteMaster ) ? ( ( RemoteMaster )frame ).getClipBoardItems() : 
        ( frame instanceof KeyMapMaster ) ? ( ( KeyMapMaster )frame ).getClipBoardItems() : new ArrayList< AssemblerItem >();
    }
    
    @Override
    public void actionPerformed( ActionEvent e )
    {
      Object source = e.getSource();
      if ( Arrays.asList( insert, delete, moveUp, moveDown, copy, cut, paste ).contains( source ) )
      {
        if ( assemblerTable.getCellEditor() != null )
        {
          assemblerTable.getCellEditor().stopCellEditing();
        }
        List< AssemblerItem > itemList = assemblerModel.getItemList();
        int row = assemblerTable.getSelectedRow();
        int col = assemblerTable.getSelectedColumn();
        int rowCount = assemblerTable.getSelectedRowCount();
        int colCount = assemblerTable.getSelectedColumnCount();
        if ( source == insert )
        {
          for ( int i = 0; i < rowCount; i++ )
            itemList.add( row, new AssemblerItem() );
          assemblerModel.fireTableDataChanged();
          assemblerTable.changeSelection( row + rowCount, col, false, false );
          assemblerTable.changeSelection( row + 2 * rowCount - 1, col + colCount - 1, false, true );
        }
        else if ( source == paste )
        {
          itemList.addAll( row, cutItems );
          assemblerModel.fireTableDataChanged();
          assemblerTable.changeSelection( row, col, false, false );
          assemblerTable.changeSelection( row + cutItems.size() - 1, col + colCount - 1, false, true );
        }
        else if ( source == delete || source == cut || source == copy )
        {
          if ( !assemblerModel.testBuildMode( processor ) )
          {
            for ( int i = 0; i < rowCount; i++ )
            {
              String op = ( String )assemblerModel.getValueAt( row + i, 3 );
              if ( op.equalsIgnoreCase( "ORG" ) )
              {
                String title = "Assembler edit";
                String message = "You cannot include an ORG instruction in this operation unless\n"
                    + "in Build mode, which is when the Assemble and Update buttons\n"
                    + "are disabled.";        
                JOptionPane.showMessageDialog( settingsPanel.getAssemblerPanel(), message, title, JOptionPane.WARNING_MESSAGE );
                return;
              }
            }
          }
          
          if ( source == cut || source == copy )
          {
            cutItems.clear();
            cutItems.addAll( itemList.subList( row, row + rowCount ) );
            paste.setEnabled( true );
          }
          if ( source == delete || source == cut )
          {
            for ( int i = 0; i < rowCount; i++ )
              itemList.remove( row );
            if ( itemList.size() == 0 ) itemList.add( new AssemblerItem() );
            assemblerModel.fireTableDataChanged();
            assemblerTable.changeSelection( row, col, false, false );
            assemblerTable.changeSelection( row, col + colCount - 1, false, true );
          }
        }
        setAssemblerButtons( false );
      }
      else if ( source == selectAll )
      {
        assemblerTable.changeSelection( 0, 0, false, false );
        assemblerTable.changeSelection( assemblerModel.getItemList().size() - 2, assemblerTable.getColumnCount() - 1, false, true );
      }
      else if ( source == assemble )
      {
        int row = ManualCodePanel.getProcBox().getSelectedIndex();
        Processor proc = procs[ row ];
        if ( assemblerTable.getCellEditor() != null )
        {
          assemblerTable.getCellEditor().stopCellEditing();
        }
        Hex hex = assemblerModel.assemble( proc );
        assemblerModel.fireTableDataChanged();
        if ( hex != null )
        {
          if ( codePanel.getCodeModel().isCellEditable( row, 1 ) )
          {
            assembled = true;
            codePanel.getCodeModel().setValueAt( hex, row, 1 );
            assembled = false;
          }
          else
          {
            String title = "Assemble";
            String message = "The code for this processor is not editable, so the assembled\n" + 
                             "hex code will not be saved in the .rmdu/.rmir file.  You may,\n" +
                             "however, use the Save button to save the assembly source as a\n" +
                             "separate file.";
            JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
          }
        }
      }
      else if ( source == toAssemblerButton )
      {
        ManualProtocol protocol = codePanel.getProtocol();
        if ( protocol != null )
        {
          int[] rows = codeTable.getSelectedRows();
          int firstRow = -1;
          for ( int row : rows )
          {
            Processor proc = procs[ row ];
            if ( proc.getDataStyle() < 0 )
            {
              continue;
            }
            if ( firstRow < 0 ) firstRow = row;
            Hex hex = ( Hex )codePanel.getCodeModel().getValueAt( row, 1 );
            if ( hex != null && hex.length() > 0 )
            {
              ManualCodePanel.getProcBox().setSelectedItem( proc );
              settingsPanel.setMode( Mode.ASM );
              DisasmState state = null;
              state = new DisasmState();
              state.useFunctionConstants = settingsPanel.useFunctionConstants.isSelected();
              state.useRegisterConstants = settingsPanel.useRegisterConstants.isSelected();
              assemblerModel.disassemble( hex, proc, state );
              protocol.setCode( hex, proc );
            }
          }
          if ( firstRow >= 0 )
          {
            ManualCodePanel.getProcBox().setSelectedItem( procs[ firstRow ] );    
          }
          setAssemblerButtons( false );
          codePanel.getCodeTable().repaint();
        }
      }
      else if ( source == update || source == build )
      {
        stopEditing();
        if ( source == build )
        {
          Iterator< AssemblerItem > it = assemblerModel.getItemList().iterator();
          while ( it.hasNext() )
          {
            if ( !it.next().getOperation().equalsIgnoreCase( "ORG" ) ) it.remove();
          }
        }
        if ( processor instanceof S3C80Processor && assemblerModel.testBuildMode( processor ) )
        {
          processor = ProcessorManager.getProcessor( "S3C80" );
        }
        int ramAddress = processor.getRAMAddress();
        int i = 0;
        boolean hasOrg = false;
        AssemblerItem startItem = null;
        AssemblerItem endItem = null;
        List< AssemblerItem > newItemList = new ArrayList< AssemblerItem >();
        protDataPanel.setShowMessages( false );
        protDataPanel.setAssemblerData( !protDataPanel.pfMainPanel.isActive() && !protDataPanel.pdMainPanel.isActive() );

        int length = 0;
        int endDirectives = 0;
        int end = 2;  // length of JR / BRA instruction
        i = 0;
        for ( i = 0; i < assemblerModel.getItemList().size(); i++ )
        {
          AssemblerItem item = assemblerModel.getItemList().get( i );
          if ( item.isCommentedOut() ) continue;
          if ( item.getOperation().equalsIgnoreCase( "ORG" ) )
          {
            hasOrg = true;
            for ( Token t : OpArg.getArgs( item.getArgumentText(), null, null ) ) ramAddress = t.value;
            if ( processor instanceof S3C80Processor )
            {
              processor = ProcessorManager.getProcessor( ( ramAddress & 0xC000 ) == 0xC000 ? "S3F80" : "S3C80" );
              settingsPanel.setProcessor( processor );
              ramAddress = processor.getRAMAddress();
            }
          }
          if ( length == processor.getStartOffset() )
          {
            String op = item.getOperation();
            if ( op.equals( "JR" ) || op.equals( "BRA" ) )
            {
              end += length;
              startItem = item;
              int j = i + 1;
              for ( ; j < assemblerModel.getItemList().size(); j++ )
              {
                // search for JR / BRA destination label
                AssemblerItem item2 = assemblerModel.getItemList().get( j );
                if ( item2.isCommentedOut() ) continue;
                if ( Arrays.asList( item.getArgumentText(), item.getArgumentText() + ":" ).contains( item2.getLabel() ) )
                  break;
                end += item2.getLength();
              }
              if ( j == assemblerModel.getItemList().size() )
              {
                // label not found, so base end on hex data
                end += item.getHex().getData()[ 1 ];
              }
            }
          }
          length += item.getLength();
          if ( length == 0 && !( item.getOperation().trim().isEmpty() && item.getArgumentText().trim().isEmpty() && item.getComments().trim().isEmpty() ) )
          {
            newItemList.add( item );
            endDirectives = newItemList.size();
          }
          if ( length >= 5 ) break;
        }
        if ( startItem == null )
        {
          if ( length > processor.getStartOffset() )
          {
            String message = "Cannot get data as code is not a valid protocol format.\n\n"
              + "Get Data will only get data from the Protocol Data etc tabs into a protocol that is\n"
              + "already properly structured, so if you enter code into the listing grid, assemble it and\n"
              + "THEN try to get the data bytes into it, it won't work. Begin with a basic Build. You can\n"
              + "press Build right at the start. You don't need to put the proper, or even any, values\n"
              + "into the Protocol Data tab, it will work with just the defaults and you can change the\n"
              + "data later. The basic build will give you only one assembler instruction following the\n"
              + "data block, JP XmitIR or the equivalent for other processors. Replace this with whatever\n"
              + "code you want.\n\n"
              + "If you forget to do the initial build and have already entered assembler code, use Cut and\n"
              + "Paste. Select it all, Cut, then press Build with the empty assembler grid that results, and\n"
              + "finally Paste the cut data at the end of the build. After that, you can change the data and\n"
              + "use Get Data as required.";
            String title = "Update Data";
            JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
            return;
          }
          else
          {
            String op = ( processor instanceof S3C80Processor ) ? "JR" : "BRA";
            startItem = new AssemblerItem( ramAddress + processor.getStartOffset(), op, "L0" );
            op = ( processor instanceof S3C80Processor ) ? "JP" : "JMP";
            endItem = new AssemblerItem( 0, op, "XmitIR" );
          }
        }

        DisasmState state = new DisasmState();
        i = assemblerModel.getPfCount() + assemblerModel.getPdCount();
        List< AssemblerItem > oldItemList = assemblerModel.getData();
        assemblerModel.setItemList( newItemList );
        assemblerModel.dbOut( 0, processor.getStartOffset(), ramAddress, 0, processor );
        newItemList.add( startItem );
        assemblerModel.dbOut( processor.getStartOffset() + 2, i + 5, ramAddress, 0, processor );
        if ( endItem == null )
        {
          // Update mode
          startItem.getHex().set( ( short )( i + 3 - processor.getStartOffset() ), 1 );
          length = 0;
          i += ramAddress + 5;
          for ( AssemblerItem item : oldItemList )
          {
            if ( length >= end && item.getOpCode() != null )
            {
              item.setAddress( i );
              newItemList.add( item );
              i += item.getLength();
            }
            length += item.getLength();
          }
        }
        else
        {
          // Build mode
          LinkedHashMap< String, String > labels = processor.getAsmLabels();
          labels.put( "L0", String.format( "%04XH", ramAddress + i + 5 ) );
          LinkedHashMap< Integer, String > rptLabels = new LinkedHashMap< Integer, String >();
          rptLabels.put( ramAddress + i + 5, "L0" );
          int rptVal = assemblerModel.getForcedRptCount();
          Hex hx = new Hex( CommonData.forcedRptCode[ protDataPanel.getDataStyle() ] );
          if ( protDataPanel.getRptType() == 0 && rptVal > 0 )
          {
            short rpt = ( short )( long )rptVal;
            switch ( protDataPanel.getDataStyle() )
            {
              case 0:
                hx.set( rpt, 2 );
                break;
              case 1:
              case 2:
                hx.set( rpt, 1 );
                break;
              case 3:
                int op = hx.getData()[ 0 ] * 0x100;
                hx = new Hex( 2 * ( rpt - 1 ) );
                for ( int j = 0; j < rpt - 1; j++ )
                {
                  hx.put( op + +2 * ( rpt - j - 2 ), 2 * j );
                }
                labels.put( "L1", String.format( "%04XH", ramAddress + i + 5 + hx.length() ) );
                rptLabels.put( ramAddress + i + 5 + hx.length(), "L1" );
                break;
              case 4:
                hx.set( ( short )( rpt - 1 ), 1 );
                break;
            }
            while ( hx.length() > 0 )
            {
              AssemblerItem item = new AssemblerItem( ramAddress + i + 5, hx );
              int opLen = item.disassemble( processor, rptLabels, state );
              String lbl = rptLabels.get( item.getAddress() );
              if ( lbl != null )
                item.setLabel( lbl + ":" );
              newItemList.add( item );
              hx = hx.subHex( opLen );
              i += opLen;
            }
            if ( protDataPanel.getDataStyle() != 3 ) state.zeroUsed.add( processor.getZeroAddresses().get( "RPT" ) );
          }
          endItem.setAddress( ramAddress + i + 5 );
          String lbl = rptLabels.get( endItem.getAddress() );
          if ( lbl != null )
            endItem.setLabel( lbl + ":" );
          if ( protDataPanel.getBurstMidFrame() == 1 )
            endItem.setArgumentText( "XmitSplitIR" );
          state.absUsed.add( processor.getAbsAddresses().get( endItem.getArgumentText() ) );
          newItemList.add( endItem );

          startItem.assemble( processor, labels, true );
          endItem.assemble( processor, labels, true );
          assemblerModel.insertEQU( endDirectives, processor, state );
          if ( !hasOrg ) assemblerModel.insertORG( endDirectives, ramAddress, processor );
        }
        newItemList.add( new AssemblerItem() );
        assemblerModel.getData().clear();
        assemblerModel.getData().addAll( newItemList );
        assemblerModel.setItemList( assemblerModel.getData() );
        assemblerModel.setChanged( true );
        setAssemblerButtons( false );
        
        protDataPanel.interpretPFPD( false );
        protDataPanel.pfMainPanel.set();
        protDataPanel.pdMainPanel.set();
        protDataPanel.fnMainPanel.set();
        protDataPanel.setShowMessages( true );
        assemblerModel.fireTableDataChanged();
        
      }
    }
    
    public void setAssemblerButtons( boolean retitle )
    {
      boolean valid = codeTable != null;
      boolean asm = valid && settingsPanel.getMode() == Mode.ASM;
      boolean sel = valid;
      assemble.setEnabled( asm && !assemblerModel.testBuildMode( processor ) );
      insert.setEnabled( asm && sel );
      delete.setEnabled( asm && sel );
      build.setEnabled( asm );
      update.setEnabled( asm && !assemblerModel.testBuildMode( processor ) );
      cut.setEnabled( asm && sel );
      paste.setEnabled( asm && sel && cutItems.size() > 0 );
      copy.setEnabled( sel );
      selectAll.setEnabled( valid );
      if ( rmpbSaveAsAction != null )
      {
        rmpbSaveAsAction.setEnabled( valid && !assemblerModel.testBuildMode( processor ) );
      }
      if ( dialogSaveButton != null )
      {
        dialogSaveButton.setEnabled( valid && !assemblerModel.testBuildMode( processor ) );
      }
      if ( retitle )
      {
        String title = asm ? "Assembler listing (editable)" : "Disassembler listing (not editable)";
        asmBorder.setTitle( title );
        noteArea.setVisible( asm );
        editorPanel.setVisible( asm );
        repaint();
      }
    }
    
    public JButton getAssemble()
    {
      return assemble;
    }

    private JButton moveUp = new JButton( "Up" );
    private JButton moveDown = new JButton( "Down" );
    private JButton insert = new JButton( "Insert" );
    private JButton delete = new JButton( "Delete" );
    private JButton selectAll = new JButton( "Select All" );
    private JButton copy = new JButton( "Copy" );
    private JButton cut = new JButton( "Cut" );
    private JButton paste = new JButton( "Paste" );
    private JButton assemble = new JButton( "Assemble" );
    private List< AssemblerItem > cutItems = null;
    
    public JButton build = new JButton( "Build" );
    public JButton update = new JButton( "Update" );
  }
  
  public AssemblerPanel( ManualSettingsPanel settingsPanel )
  {
    this.settingsPanel = settingsPanel;
    this.assemblerModel = settingsPanel.getAssemblerModel();
    this.codePanel = settingsPanel.getTablePanel();
    optionButtons[ 5 ] = settingsPanel.useRegisterConstants;
    optionButtons[ 6 ] = settingsPanel.useFunctionConstants;
    codeTable = codePanel.getCodeTable();
    procs = ManualCodePanel.getProcs();
    
    setLayout( new BorderLayout() );
    setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    assemblerTable = new JP1Table( assemblerModel );
    assemblerTable.initColumns( assemblerModel );
    assemblerTable.setSelectionMode( ListSelectionModel.SINGLE_INTERVAL_SELECTION );
    assemblerTable.getSelectionModel().addListSelectionListener( this );

    String noteText = "To build for an S3F80 processor, select S3C80 and add an instruction "
        + "ORG FF00H in the Assembler listing before pressing the Build button.";
    noteArea = new DisplayArea( noteText, null );
    noteArea.setBorder( BorderFactory.createEmptyBorder( 5,5,10,5 ) );
    add( noteArea, BorderLayout.PAGE_START );
    
    JScrollPane scrollPane = new JScrollPane( assemblerTable );
    asmBorder = BorderFactory.createTitledBorder( "" ); // Title added by setAssemblerButtons()
    scrollPane.setBorder( asmBorder );
    add( scrollPane, BorderLayout.CENTER );
    assemblerTable.setLongToolTipTimeout();
    
    // Disassembly options
    JPanel lowerRightPanel = new JPanel();
    lowerRightPanel.setLayout( new BoxLayout( lowerRightPanel, BoxLayout.PAGE_AXIS ) );
    optionsPanel = new JPanel();
    optionsPanel.setLayout( new BoxLayout( optionsPanel, BoxLayout.PAGE_AXIS ) );
    optionsPanel.setBorder( BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder( "Disassembly options" ), BorderFactory.createEmptyBorder( 0, 10, 0, 10 ) ) );
    lowerRightPanel.add( optionsPanel );
    add( lowerRightPanel, BorderLayout.PAGE_END );
    JPanel optionPanel = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
    useRegisterConstants.addItemListener( this );
    useFunctionConstants.addItemListener( this );
    optionPanel.add( new JLabel( "Use predefined constants for: " ) );
    optionPanel.add( useRegisterConstants );
    optionPanel.add( useFunctionConstants );
    optionsPanel.add( optionPanel );
    optionPanel = new JPanel( new GridLayout( 1, 4 ) );
    asCodeButton.addItemListener( this );
    rcButton.addItemListener( this );
    wButton.addItemListener( this );
    ButtonGroup grp = new ButtonGroup();
    grp.add( asCodeButton );
    grp.add( rcButton );
    grp.add( wButton );
    setOptionButtons();
    optionPanel.add( new JLabel( "S3C80 only:" ) );
    optionPanel.add( asCodeButton );
    optionPanel.add( rcButton );
    optionPanel.add( wButton );
    optionsPanel.add( optionPanel );
    optionsPanel.setVisible( false );
    
    editorPanel = new EditorPanel();
    lowerRightPanel.add( editorPanel );
    editorPanel.setAssemblerButtons( true );
  }
  
  public void saveOptionButtons()
  {
    int opt = 0;
    for ( int i = 0; i < optionButtons.length; i++ )
    {
      opt |= optionButtons[ i ].isSelected() ? 1 << i : 0;
    }
    // To preserve 7 as the default option now that there are two extra buttons
    // whose default is selected, XOR with 0x60
    opt ^= 0x60;
    if ( opt == 7 )
    {
      properties.remove( "AssemblerOptions" );
    }
    else
    {
      properties.setProperty( "AssemblerOptions", "" + opt );
    }
  }

  private void setOptionButtons()
  {
    int opt = 7;
    try
    {
      opt = Integer.parseInt( properties.getProperty( "AssemblerOptions", "7" ) );
    }
    catch ( NumberFormatException e )
    {
      e.printStackTrace();
    }
    opt ^= 0x60;

    for ( int i = 0; i < optionButtons.length; i++ )
    {
      optionButtons[ i ].setSelected( ( ( opt >> i ) & 1 ) == 1 );
    }
  }
  
  public void stopEditing()
  {
    if ( assemblerTable.getCellEditor() != null )
    {
      assemblerTable.getCellEditor().stopCellEditing();
    }
  }
  
  public boolean isAssembled()
  {
    return assembled;
  }

  public void setAssembled( boolean assembled )
  {
    this.assembled = assembled;
  }

  public Processor getProcessor()
  {
    return processor;
  }

  public void setProcessor( Processor processor )
  {
    this.processor = processor;
  }

  public EditorPanel getEditorPanel()
  {
    return editorPanel;
  }

  public void setProtDataPanel( ProtocolDataPanel protDataPanel )
  {
    this.protDataPanel = protDataPanel;
  }

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if ( e.getSource() == assemblerTable.getSelectionModel() )
    {
      editorPanel.setAssemblerButtons( false );
      return;
    }
    
  }
  
  @Override
  public void itemStateChanged( ItemEvent e )
  {
    // Disassembler options changed
    saveOptionButtons();
    ManualProtocol protocol = codePanel.getProtocol();
    if ( protocol != null && codeTable != null )
    {
      int row = ManualCodePanel.getProcBox().getSelectedIndex();
      Processor proc = procs[ row ];
      Hex hex = ( Hex )codePanel.getCodeModel().getValueAt( row, 1 );
      assemblerModel.disassemble( hex, proc );
    }
  }
  
  public void setRmpbSaveAsAction( PBAction rmpbSaveAsAction )
  {
    this.rmpbSaveAsAction = rmpbSaveAsAction;
  }

  public void setDialogSaveButton( JButton dialogSaveButton )
  {
    this.dialogSaveButton = dialogSaveButton;
  }

  private ManualSettingsPanel settingsPanel = null;
  private ManualCodePanel codePanel = null;
  private AssemblerTableModel assemblerModel = null;
  private JP1Table assemblerTable = null;
  private TitledBorder asmBorder = null;
  private DisplayArea noteArea = null;
  private PBAction rmpbSaveAsAction = null;
  private JButton dialogSaveButton = null;
  public JPanel optionsPanel = null;
  public JCheckBox useRegisterConstants = new JCheckBox( "Registers" );
  public JCheckBox useFunctionConstants = new JCheckBox( "Functions" );
  public JRadioButton asCodeButton = new JRadioButton( "As code" );
  public JRadioButton rcButton = new JRadioButton( "Force RCn" );
  public JRadioButton wButton = new JRadioButton( "Force Wn" );
  public JButton toAssemblerButton = new JButton( "Import to Assembler");
  
  private JToggleButton[] optionButtons =
    {
        useRegisterConstants, useFunctionConstants, asCodeButton, rcButton, wButton,
        null, null
    };
  private boolean assembled = false;
  private Processor processor = null;
  private EditorPanel editorPanel = null;
  private JTableX codeTable = null;
  private Processor[] procs = null;
  private PropertyFile properties = JP1Frame.getProperties();
  private ProtocolDataPanel protDataPanel = null;
}

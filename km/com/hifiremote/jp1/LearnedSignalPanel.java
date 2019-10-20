package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;

import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.NamedProtocol;

import com.hifiremote.jp1.Executor;
import com.hifiremote.jp1.Executor.ExecutorWrapper;

/**
 * The Class LearnedSignalPanel.
 */
public class LearnedSignalPanel extends RMTablePanel< LearnedSignal >
{
  private static class ExecutorData
  {
    public List< Protocol > protocols = null;
    public LinkedHashMap< LearnedSignal, LinkedHashMap< Protocol, Executor > > map;
  }
  
  /**
   * Instantiates a new learned signal panel.
   */
  public LearnedSignalPanel()
  {
    super( new LearnedSignalTableModel() );

    kit = Toolkit.getDefaultToolkit();
    clipboard = kit.getSystemClipboard();
    
    TransferHandler th = new TransferHandler()
    {
      protected Transferable createTransferable( JComponent c )
      {
        return new LocalObjectTransferable( new Integer( table.getSelectedRow() ) );
      }

      public int getSourceActions( JComponent c )
      {
        return TransferHandler.COPY;
      }

      public void exportToClipboard( JComponent comp, Clipboard clip, int action )
      {
        JTable table = ( JTable )comp;
        int[] selectedRows = table.getSelectedRows();
        int[] selectedCols = table.getSelectedColumns();
        StringBuilder buff = new StringBuilder( 200 );
        for ( int rowNum = 0; rowNum < selectedRows.length; rowNum++ )
        {
          if ( rowNum != 0 )
            buff.append( "\n" );
          for ( int colNum = 0; colNum < selectedCols.length; colNum++ )
          {
            if ( colNum != 0 )
              buff.append( "\t" );
            int selRow = selectedRows[ rowNum ];
            // int convertedRow = sorter.convertRowIndexToModel( selRow );
            int selCol = selectedCols[ colNum ];
            int convertedCol = table.convertColumnIndexToModel( selCol );
            Object value = table.getValueAt( selRow, selCol );
            if ( value != null )
            {
              DefaultTableCellRenderer cellRenderer = ( DefaultTableCellRenderer )table.getColumnModel()
                  .getColumn( selCol ).getCellRenderer();
              if ( cellRenderer != null )
              {
                cellRenderer.getTableCellRendererComponent( table, value, false, false, selRow, convertedCol );
                value = cellRenderer.getText();
              }
              buff.append( value.toString() );
            }
          }
        }
        StringSelection data = new StringSelection( buff.toString() );
        clipboard.setContents( data, data );
      }
    };
    table.setTransferHandler( th );
    
    deleteAllButton.setVisible( true );
    
    copyButton = new JButton( "Copy" );
    copyButton.addActionListener( this );
    copyButton.setToolTipText( "Copy to clipboard for pasting to Functions tab of a device upgrade" );
    copyButton.setEnabled( false );
    buttonPanel.add( copyButton );
    
    convertToUpgradeButton = new JButton( "Convert to Device Upgrade" );
    convertToUpgradeButton.addActionListener( this );
    convertToUpgradeButton.setToolTipText( "Convert the selected set of consecutive items to a Device Upgrade." );
    buttonPanel.add( convertToUpgradeButton );

    timingSummaryButton = new JButton( "Timing Summary" );
    timingSummaryButton.addActionListener( this );
    timingSummaryButton.setToolTipText( "View the Timing Summary for all of the Learned Signals." );
    timingSummaryButton.setEnabled( true );
//    timingSummaryButton.setVisible( Boolean.parseBoolean( RemoteMaster.getProperties().getProperty( "LearnedSignalTimingAnalysis", "false" ) ) );
    buttonPanel.add( timingSummaryButton );
    
    refresh();
  }

  @Override
  protected void refresh()
  {
    convertToUpgradeButton.setEnabled( !Boolean.parseBoolean( RemoteMaster.getProperties().getProperty( "UseDecodeIR", "false" ) ) );    
    timingSummaryButton.setVisible( Boolean.parseBoolean( RemoteMaster.getProperties().getProperty( "LearnedSignalTimingAnalysis", "false" ) ) );
  }

  /**
   * Sets the.
   * 
   * @param remoteConfig
   *          the remote config
   */
  @Override
  public void set( RemoteConfiguration remoteConfig )
  {
    this.remoteConfig = remoteConfig;
    ( ( LearnedSignalTableModel )model ).set( remoteConfig );
    table.initColumns( model );
    newButton.setEnabled( remoteConfig != null );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.RMTablePanel#createRowObject(java.lang.Object)
   */
  @Override
  public LearnedSignal createRowObject( LearnedSignal learnedSignal )
  {
    LearnedSignal newSignal = null;
    if ( learnedSignal != null )
    {
      newSignal = new LearnedSignal( learnedSignal );
    }
    return LearnedSignalDialog.showDialog( SwingUtilities.getRoot( this ), newSignal, remoteConfig );
  }
  
  @Override
  protected void editRowObject( int row )
  {
    Remote remote = remoteConfig.getRemote();
    List< LearnedSignal > lsList = remoteConfig.getLearnedSignals();
    LearnedSignal baseLS = getRowObject( row );
    int ndx = lsList.indexOf( baseLS );
    LearnedSignal newLS = createRowObject( baseLS );
    if ( newLS != null )
    {
      lsList.remove( ndx );
      lsList.add( ndx, newLS );
      if ( remote.usesEZRC() )
      {
        DeviceButton db = remote.getDeviceButton( baseLS.deviceButtonIndex );
        Button btn = remote.getButton( baseLS.getKeyCode() );
        LinkedHashMap< Integer, LearnedSignal > lsMap = null;
        if ( db != null && db.getUpgrade() != null && ( lsMap = db.getUpgrade().getLearnedMap() ) != null )
        {
          lsMap.remove( ( int )btn.getKeyCode() );
          Function oldFn = null;
          if ( ( oldFn = db.getUpgrade().getAssignments().getAssignment( btn ) ) != null )
          {
            oldFn.addReference( db, btn );
          }

          db = remote.getDeviceButton( newLS.deviceButtonIndex );
          if ( db != null && db.getUpgrade() != null && ( lsMap = db.getUpgrade().getLearnedMap() ) != null )
          {
            lsMap.put( newLS.getKeyCode(), newLS );
            newLS.addReference( db, remote.getButton( newLS.getKeyCode() ) );
          }
        }
      }
      model.setRow( sorter.modelIndex( row ), newLS );
    }
  }

  
  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    if ( source == convertToUpgradeButton )
    {
      finishEditing();
      if ( table.getRowCount() == 0 )
        return;
      int[] rows = table.getSelectedRows();
      ArrayList<LearnedSignal> signals = new ArrayList<LearnedSignal>();
      for ( int i =0; i < rows.length; i++ )
      {
        LearnedSignal s = getRowObject(rows[i]);
        if ( !s.getDecodes().isEmpty() )
          signals.add(s);
      }
      if ( !signals.isEmpty() )
        convertToDeviceUpgrade( signals.toArray(new LearnedSignal[signals.size()]) );
    }
    else if ( source == timingSummaryButton )
    {
      LearnedSignalTimingSummaryDialog.showDialog( SwingUtilities.getRoot( this ), remoteConfig );
    }
    else if ( source == copyButton )
    {
      table.getTransferHandler().exportToClipboard( table, clipboard, TransferHandler.COPY );
    }
    else
      super.actionPerformed( e );
  }
  
  private ExecutorData getConsistentProtocols( LearnedSignal[] signals )
  {
    if ( signals == null || signals.length == 0 )
      return null;
    ExecutorData execData = new ExecutorData();
    execData.map = new LinkedHashMap< LearnedSignal, LinkedHashMap<Protocol, Executor> >();
    execData.protocols = new ArrayList< Protocol >();
    
    for ( LearnedSignal ls : signals )
    {
      execData.map.put( ls, getProtocolMap( ls ) );
    }
    for ( Protocol p : execData.map.get( signals[ 0 ] ).keySet() )
    {
      // Initial protocol list is that of first signal
      execData.protocols.add( p );
    }

    // Now delete protocols that are not present in lists of all other signals
    ListIterator< Protocol > it = execData.protocols.listIterator();
    while ( it.hasNext() )
    {
      Protocol p = it.next();
      for ( LearnedSignal ls : signals )
      {
        Set< Protocol > pSet = execData.map.get( ls ).keySet();
        if ( !pSet.contains( p ) )
        {
          it.remove();
          break;
        }
      }
    }
    return execData;
  }
  
  private LinkedHashMap< Protocol, Executor > getProtocolMap( LearnedSignal ls )
  {
    IrpDatabase tmDatabase = LearnedSignal.getTmDatabase();
    LinkedHashMap< Protocol, Executor > pMap = new LinkedHashMap< Protocol, Executor >();
    if ( tmDatabase == null )
      return pMap;
    Remote remote = remoteConfig.getRemote();
    List< Protocol > protocols = ProtocolManager.getProtocolManager().getProtocolsForRemote( remote );
    for ( LearnedSignalDecode lsd : ls.getDecodes() )
    {
      NamedProtocol np = lsd.decode.getNamedProtocol();
      List< ExecutorWrapper > wrappers = LearnedSignal.getExecutorWrappers( np );
      for ( ExecutorWrapper wrapper : wrappers )
      {
        Executor e = LearnedSignalDecode.getExecutor( np, wrapper );
        if ( e == null ) continue;
        Protocol p = e.protocol;
        if ( p != null && protocols.contains( p ) )
        {
          pMap.put( p, e );
        }
      }
    }
    return pMap;
  }
/*  
  private JList< Protocol > getProtocolList( List< Protocol > protocols )
  {
    DefaultListModel< Protocol > protocolModel = new DefaultListModel< Protocol >();
    for ( Protocol p : protocols )
      protocolModel.addElement( p );
    JList< Protocol > protocolList = new JList< Protocol >( protocolModel );
    protocolList.setSelectedIndex( 0 );
    protocolList.setCellRenderer( new DefaultListCellRenderer()
    {
      @Override
      public Component getListCellRendererComponent(JList< ? > list, Object value,
          int index, boolean isSelected, boolean cellHasFocus)
      {
        Protocol p = ( Protocol )value;
        String varName = p.getVariantName();
        String text = p.getName() + " (" + p.id.toString().replace( " ", "" )
            + ( varName == null || varName.isEmpty() ? "" : ":" + varName ) + ")";
        return super.getListCellRendererComponent( list, text, index, 
            isSelected, cellHasFocus );
        
      }
    });
    return protocolList;
  }
*/
  
  private < E > JList< E > getJList( List< E > list )
  {
    DefaultListModel< E > eModel = new DefaultListModel< E >();
    for ( E item : list )
      eModel.addElement( item );
    JList< E > eList = new JList< E >( eModel );
    eList.setSelectedIndex( 0 );
    eList.setCellRenderer( new DefaultListCellRenderer()
    {
      @Override
      public Component getListCellRendererComponent(JList< ? > list, Object value,
          int index, boolean isSelected, boolean cellHasFocus)
      {
        String text = null;
        if ( value instanceof Protocol )
        {
          Protocol p = ( Protocol )value;
          String varName = p.getVariantName();
          text = p.getName() + " (" + p.id.toString().replace( " ", "" )
              + ( varName == null || varName.isEmpty() ? "" : ":" + varName ) 
              + ( p.needsCode( remoteConfig.getRemote() ) ? "*" : "" ) + ")";
        }
        else if ( value instanceof DeviceUpgrade )
        {
          DeviceUpgrade du = ( DeviceUpgrade )value;
          int setupInt = du.getSetupCode();
          text = du.getDeviceTypeAliasName();
          if ( setupInt >= 0 )
          {
            text += "/" + ( new SetupCode( setupInt ) );
          }
        }
        else
        {
          text = value.toString();
        }
        return super.getListCellRendererComponent( list, text, index, 
            isSelected, cellHasFocus );
        
      }
    });
    return eList;
  }
  
  
  private < E > JPanel getOptionPanel( String message, JList< ? > optionList )
  {
    JPanel textPanel = new JPanel( new BorderLayout() );
    JPanel panel = new JPanel( new BorderLayout() );
    JPanel protPanel = new JPanel( new BorderLayout() );
    JTextArea txtArea = new JTextArea( message );
    txtArea.setFont( ( new JLabel() ).getFont() );
    txtArea.setBackground( panel.getBackground() );
    
    textPanel.add( txtArea, BorderLayout.CENTER );
    textPanel.setBorder( BorderFactory.createEmptyBorder( 5,5,5,5 ) );
    protPanel.setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
    protPanel.add( optionList, BorderLayout.CENTER );
    panel.add( textPanel, BorderLayout.PAGE_START );
    panel.add( protPanel, BorderLayout.CENTER );
    return panel;
  }
  
  private void convertToDeviceUpgrade( LearnedSignal[] signals )
  {
    ExecutorData execData = getConsistentProtocols( signals );
    if ( execData == null )
    {
      showMessage( "There are no signals to convert.", JOptionPane.ERROR_MESSAGE );
      return;
    }
    if ( execData.protocols.isEmpty() )
    {
      showMessage( "There is no protocol that is compatible with all the\n"
          + "selected signals.  Conversion aborting.", JOptionPane.ERROR_MESSAGE );
      return;
    }
    Protocol protocol = execData.protocols.get( 0 );
    if ( execData.protocols.size() > 1 )
    {
      // Let user choose the protocol to use
      String message = 
          "There is more than one executor that is compatible both with\n"
        + "your remote and with the learned signals you have selected.\n"
        + "The compatible ones are listed below.  A * after the PID means\n"
        + "that the executor is not built in to the remote, so a new device\n"
        + "upgrade will include a protocol upgrade.\n\n"
        + "Please choose an executor from the list and press Convert, or\n"
        + "press Cancel to cancel the conversion.";
      String[] options = { "Convert", "Cancel" };
      JList< Protocol > pList = getJList( execData.protocols );
      int result = JOptionPane.showOptionDialog( null, getOptionPanel( message, pList ), "Protocol chooser", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
          null, options, options[ 0 ] );
      protocol = result == 0 ? pList.getSelectedValue() : null;
    }
    if ( protocol == null )
    {
      //showMessage( "No protocol chosen, conversion aborted.", JOptionPane.INFORMATION_MESSAGE );
      return;
    }
    
    LinkedHashMap< LearnedSignal, Executor > lsMap = new LinkedHashMap< LearnedSignal, Executor >();
    for ( LearnedSignal ls : signals )
    {
      lsMap.put( ls, execData.map.get( ls ).get( protocol) );
    }
    
    System.err.println("Checking if we can append to an existing learned signal conversion" );
    DeviceUpgrade appendUpgrade = null;
    List< DeviceUpgrade > upgrades = new ArrayList< DeviceUpgrade >();
    for ( DeviceUpgrade du: remoteConfig.getDeviceUpgrades() )
    {
      if ( du.protocol == protocol )
      {
        upgrades.add( du );
      }
    }
    if ( upgrades.size() > 0 )
    {
      String message =
          "The selected learned signals can be appended to an existing\n"
              + "device upgrade as the upgrades listed below use the same\n"
              + "executor.  The compatible existing upgrades are listed below.\n\n"
              + "If you want to append the signals, select the required existing\n"
              + "upgrade and press Append.  To create a new upgrade for the\n"
              + "signals, press New, or to cancel the conversion, press Cancel.";
      String[] options = { "Append", "New", "Cancel" };
      JList< DeviceUpgrade > pList = getJList( upgrades );
      int result = JOptionPane.showOptionDialog( null, getOptionPanel( message, pList ), "Upgrade chooser", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
          null, options, options[ 1 ] );
      if ( result == 0 )
      {
        appendUpgrade = pList.getSelectedValue();
      }
      else if ( result != 1 )
      {
        return;
      }
    }

    Remote remote = remoteConfig.getRemote();
    List< List< String >> failedToConvert = new ArrayList< List< String > >();
    if ( appendUpgrade == null )
    {
      DeviceUpgrade upgrade = null;
      upgrade = new DeviceUpgrade( signals, remoteConfig, lsMap, null, failedToConvert );
      String msg = null;
      if ( upgrade.protocol == null )     
      {
        // User has chosen not to continue the append process
        msg = "The conversion of Learned Signals to a new device upgrade\nhas been aborted.";
      }
      else
      {
        DeviceButton defaultDev = null;
        if ( remote.hasDeviceDependentUpgrades() == 1 )
        {
          // This is the case for remotes that use EZ-RC.
          for ( DeviceButton db : remote.getDeviceButtons() )
          {
            if ( db.getSegment() == null || db.getDeviceSlot( db.getSegment().getHex().getData() ) == 0xFFFF )
            {
              String message = "The new upgrade that has been created will be assigned automatically\n" +
                  "to an unassigned device.  What name do you want to give to this\n" +
                  "device?";
              String name = JOptionPane.showInputDialog( RemoteMaster.getFrame(), message, "New device" );
              if ( name == null )
              {
                protocol = null;
              }
              else
              {
                defaultDev = db;
                if ( db.getSegment() == null )
                {
                  db.setSegment( new Segment( 0, 0xFF, new Hex( 15 ) ) );
                }
                defaultDev.setName( name );
                defaultDev.setUpgrade( upgrade );
                upgrade.setButtonRestriction( defaultDev );
                upgrade.setButtonIndependent( false );
                short[] data = defaultDev.getSegment().getHex().getData();
                DeviceType devType = remote.getDeviceTypeByAliasName( upgrade.getDeviceTypeAliasName() );
                defaultDev.setSetupCode( ( short )upgrade.getSetupCode(), data );
                defaultDev.setDeviceTypeIndex( ( short )devType.getNumber(), data );
                defaultDev.setDeviceGroup( ( short )devType.getGroup(), data );
                if ( !remoteConfig.getDeviceButtonList().contains( defaultDev ) )
                {
                  remoteConfig.getDeviceButtonList().add( defaultDev );
                }
              }
              break;
            }
          }
          if ( defaultDev == null && upgrade != null && protocol != null )
          {
            msg = "You already have the maximum number of assigned devices.  You\n"
                +  "cannot add the new upgrade as there is no device to which it can\n"
                +  "be assigned.  The conversion is aborted.";
            upgrade = null;
          }
        }
      }
      if ( msg == null )
      {
        if ( remote.usesEZRC() )
        {
          Function[] assignments = upgrade.getAssignments().getAssignedFunctions();
          for ( int keyCode = 0; keyCode < assignments.length; keyCode++ )
          {
            Function f = assignments[ keyCode ];
            if ( f != null )
            {
              Button b = remote.getButton( keyCode );
              f.removeReferences();
              f.addReference( upgrade.getButtonRestriction(), b );
            }
          }
        }
        remoteConfig.getDeviceUpgrades().add( upgrade );
        remoteConfig.getOwner().getDeviceUpgradePanel().model.fireTableDataChanged();
        if ( signals.length == upgrade.getFunctions().size() )
        {
          msg = "The " + signals.length + " selected Learned Signals" ;
        }
        else
        {
          msg = "Of the " + signals.length + " selected Learned Signals, " + upgrade.getFunctions().size();
        }
        msg += " have been converted\ninto a new Device Upgrade of type CBL\nwith the Setup Code " 
            + upgrade.getSetupCode() + ".\n\nSwitch to the Devices tab to view/edit/etc this new Upgrade.";
      }
      remoteConfig.getOwner().setChanged( true );
      JOptionPane.showMessageDialog( RemoteMaster.getFrame(), msg, "Learned Signals converted to New Device Upgrade", JOptionPane.PLAIN_MESSAGE );
    }
    else
    {
      // Append the Learned Signals to the selected existing upgrade
      ArrayList<String> existingFunctions = new ArrayList<String>();
      ArrayList<String> renamedFunctions = new ArrayList<String>();
      ArrayList<String> shiftedFunctions = new ArrayList<String>();
      ArrayList<String> unassignedFunctions = new ArrayList<String>();

      DeviceUpgrade upgrade = new DeviceUpgrade( signals, remoteConfig, lsMap, appendUpgrade, failedToConvert );

      String msg = null;
      if ( upgrade.protocol == null )     
      {
        // User has chosen not to continue the append process
        msg = "The appending of Learned Signals to an existing device upgrade\nhas been aborted.";
      }
      else
      {
        appendUpgrade.protocol.setDeviceParms( upgrade.getParmValues() );
        appendUpgrade.setParmValues( upgrade.getParmValues() );
        for ( Function fu : upgrade.getFunctions() )
        {
          String origName = fu.getName();
          Function fo = appendUpgrade.getFunction( fu.getHex() );
          if ( fo != null )
          {
            existingFunctions.add( origName );
          }
          else
          {
            int i = 1;
            String name = origName;
            while ( appendUpgrade.getFunction( name ) != null )
            {
              i++;
              name = origName + "_" + i;
            }
            if ( i > 1 )
            {
              renamedFunctions.add( origName );
              fu.setName( name );
            }
            fu.setUpgrade( appendUpgrade );
            appendUpgrade.getFunctions().add( fu );
            
            int keyCode = Arrays.asList( upgrade.getAssignments().getAssignedFunctions() ).indexOf( fu );
            Button b = remote.getButton( keyCode );

            if ( appendUpgrade.getFunction( b, Button.NORMAL_STATE ) == null )
              appendUpgrade.setFunction( b, fu, Button.NORMAL_STATE );
            else if ( remote.getShiftEnabled() && b.allowsKeyMove( Button.SHIFTED_STATE ) && appendUpgrade.getFunction( b, Button.SHIFTED_STATE ) == null )
            {
              appendUpgrade.setFunction( b, fu, Button.SHIFTED_STATE );
              shiftedFunctions.add( origName );
            }
            else
              unassignedFunctions.add( origName );
          }
        }
        if ( failedToConvert.isEmpty() )
        {
          msg = "The " + signals.length + " selected Learned Signals";
        }
        else
        {
          msg = "Of the " + signals.length + " selected Learned Signals, " + ( signals.length - failedToConvert.size() );
        }
        msg += " were appended to existing\n"
            + "Device Upgrade (" + appendUpgrade.getDescription() + ") with protocol " + appendUpgrade.getProtocol().getName();
        msg += ".\n";

        boolean comma;
        if ( !existingFunctions.isEmpty() )
        {
          msg = msg + "\nThe following functions were already present in the upgrade:\n   ";
          comma = false;
          for (String n: existingFunctions)
            if (comma)
              msg = msg + ", " + n;
            else
            {
              msg = msg + n;
              comma = true;
            }
          msg = msg + "\n";
        }
        if ( !renamedFunctions.isEmpty() )
        {
          msg = msg + "The following Functions were renamed to prevent duplicates:\n   ";
          comma = false;
          for (String n: renamedFunctions)
            if (comma)
              msg = msg + ", " + n;
            else
            {
              msg = msg + n;
              comma = true;
            }
          msg = msg + "\n";
        }
        if ( !shiftedFunctions.isEmpty() )
        {
          msg = msg + "\nThe following were assigned to shifted keys to prevent duplicates:\n   ";
          comma = false;
          for (String n: shiftedFunctions)
            if (comma)
              msg = msg + ", " + n;
            else
            {
              msg = msg + n;
              comma = true;
            }
          msg = msg + "\n";
        }
        if ( !unassignedFunctions.isEmpty() )
        {
          msg = msg + "\nThe following could not be assigned to a key due to duplicates:\n   ";
          comma = false;
          for (String n: unassignedFunctions)
            if (comma)
              msg = msg + ", " + n;
            else
            {
              msg = msg + n;
              comma = true;
            }
        }
      }
      remoteConfig.getOwner().setChanged( true );
      JOptionPane.showMessageDialog( RemoteMaster.getFrame(), msg, "Learned Signals appended to Existing Device Upgrade", JOptionPane.PLAIN_MESSAGE );
    }
  }
  
  private void showMessage( String message, int msgType )
  {
    String title = "Learned to Upgrade Conversion";
    JOptionPane.showMessageDialog( remoteConfig.getOwner(), message, title, msgType );
  }
  
  public void valueChanged( ListSelectionEvent e )
  {
    super.valueChanged( e );
    if ( !e.getValueIsAdjusting() )
    {
//      convertToUpgradeButton.setEnabled( table.getSelectedRowCount() >= 1 );
      copyButton.setEnabled( table.getSelectedRowCount() >= 1 );
    }
  }

  private RemoteConfiguration remoteConfig = null;
  
  private JButton convertToUpgradeButton = null;
  private JButton timingSummaryButton = null;
  
  private JButton copyButton = null;
  private Clipboard clipboard = null;
  private Toolkit kit = null;
}

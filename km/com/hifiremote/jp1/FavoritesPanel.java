package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.NumberFormatter;

public class FavoritesPanel extends RMPanel implements ActionListener, ListSelectionListener
{
  public FavoritesPanel()
  {
    super();
    deviceBoxPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
    deviceButtonBox = new JComboBox();
    deviceButtonBox.addActionListener( this );
    Dimension d = deviceButtonBox.getPreferredSize();
    d.width = 100;
    deviceButtonBox.setPreferredSize( d );
    deviceBoxPanel.add( new JLabel( "Channel change device: " ) );
    deviceBoxPanel.add( deviceButtonBox );
    
    NumberFormatter formatter = new NumberFormatter( new DecimalFormat( "0.0" ) );
    formatter.setValueClass( Float.class );
    duration = new JFormattedTextField( formatter ){
      @Override
      protected void processFocusEvent( FocusEvent e ) 
      {
        super.processFocusEvent( e );
        if ( e.getID() == FocusEvent.FOCUS_GAINED )
        {  
          selectAll();
        }  
      }
    };
//    duration.setFocusLostBehavior( JFormattedTextField.PERSIST );
    duration.setColumns( 4 );
    duration.addActionListener( this );
    deviceBoxPanel.add( Box.createHorizontalStrut( 20 ) );
    deviceBoxPanel.add( new JLabel( "Interdigit pause: " ) );
    deviceBoxPanel.add( duration );
    deviceBoxPanel.add( new JLabel( " secs") );
    
    addFinal = new JCheckBox( "Send final key?" );
    addFinal.addActionListener( this );
    addFinal.setToolTipText( "Send a key such as Enter or OK after each macro?" );
    deviceBoxPanel.add( Box.createHorizontalStrut( 20 ) );
    deviceBoxPanel.add( addFinal );
    
    finalKey = new JTextField( 12 );
    finalKeyLabel = new JLabel( "Key: " );
    finalKey.setEditable( false );
    finalKey.setToolTipText( "Double-click to edit." );
    deviceBoxPanel.add( Box.createHorizontalStrut( 5 ) );
    deviceBoxPanel.add( finalKeyLabel );
    deviceBoxPanel.add( finalKey );
    
    
    JPanel panel = new JPanel( new BorderLayout() );
    panel.setBorder( BorderFactory.createTitledBorder( "Favorites Macros" ) );
    panel.add( deviceBoxPanel, BorderLayout.PAGE_START );
    favTable = new JP1Table( favModel );
    favTable.setSelectionMode( ListSelectionModel.SINGLE_INTERVAL_SELECTION );
    JScrollPane scrollPane = new JScrollPane( favTable );
    panel.add( scrollPane, BorderLayout.CENTER );
    
    JPanel buttonPanel = new JPanel();

    editButton = new JButton( "Edit" );
    editButton.addActionListener( this );
    editButton.setToolTipText( "Edit the selected item." );
    editButton.setEnabled( false );
    buttonPanel.add( editButton );

    newButton = new JButton( "New" );
    newButton.addActionListener( this );
    newButton.setToolTipText( "Add a new item." );
    buttonPanel.add( newButton );

    cloneButton = new JButton( "Clone" );
    cloneButton.addActionListener( this );
    cloneButton.setToolTipText( "Add a copy of the selected item." );
    cloneButton.setEnabled( false );
    buttonPanel.add( cloneButton );

    deleteButton = new JButton( "Delete" );
    deleteButton.addActionListener( this );
    deleteButton.setToolTipText( "Delete the selected item." );
    deleteButton.setEnabled( false );
    buttonPanel.add( deleteButton );

    upButton = new JButton( "Up" );
    upButton.addActionListener( this );
    upButton.setToolTipText( "Move the selected item up in the list." );
    upButton.setEnabled( false );
    buttonPanel.add( upButton );

    downButton = new JButton( "Down" );
    downButton.addActionListener( this );
    downButton.setToolTipText( "Move the selected item down in the list." );
    downButton.setEnabled( false );
    buttonPanel.add( downButton );

    panel.add( buttonPanel, BorderLayout.PAGE_END );
    add( panel, BorderLayout.PAGE_START ); 
    
    panel = new JPanel( new BorderLayout() );
    panel.setBorder( BorderFactory.createTitledBorder( "Favorites Group Assignments" ) );
    activityGroupTable = new JP1Table( activityGroupModel );
    activityGroupTable.setCellEditorModel( activityGroupModel );
    activityGroupTable.setSelectionMode( ListSelectionModel.SINGLE_INTERVAL_SELECTION );
    scrollPane = new JScrollPane( activityGroupTable );
    panel.add( scrollPane, BorderLayout.CENTER );
    add( panel, BorderLayout.CENTER );
    
    d = favTable.getPreferredSize();
    d.height = 12 * favTable.getRowHeight();
    favTable.setPreferredScrollableViewportSize( d );
    favTable.getSelectionModel().addListSelectionListener( this );
    favTable.addFocusListener( new FocusAdapter()
    {
      @Override
      public void focusGained( FocusEvent e )
      {
        activeTable = favTable;
        setHighlightAction( favTable );
      }
    } );
    activityGroupTable.addFocusListener( new FocusAdapter()
    {
      @Override
      public void focusGained( FocusEvent e )
      {
        activeTable = activityGroupTable;
        setHighlightAction( activityGroupTable );
      }
    } );
    
    duration.addFocusListener( new FocusAdapter()
    {
      @Override
      public void focusLost( FocusEvent e )
      {
        float f = ( Float )duration.getValue();
        try
        {
          duration.commitEdit();
        }
        catch ( ParseException e1 )
        {
          duration.setValue( f );
          return;
        }
        // minimum duration is 0.1 secs
        f = ( Float )duration.getValue();
        int val = Math.max( ( int )( 10.0 * f + 0.5 ), 1 );
        remoteConfig.setFavPause( val );
        duration.setValue( val/10.0 );
        try
        {
          duration.commitEdit();
        }
        catch ( ParseException e1 ) {}
        propertyChangeSupport.firePropertyChange( "data", null, null );
      }
    } );
    
    openEditor = new MouseAdapter()
    {
      @Override
      public void mouseClicked( MouseEvent e )
      {
        if ( e.getClickCount() != 2 )
        {
          return;
        }
        Component source = e.getComponent();
        if ( source == finalKey )
        {
          Remote remote = remoteConfig.getRemote();
          Button btn = remote.getButton( finalKey.getText() );
          if ( btn == null )
          {
            btn = remote.getButtons().get( 0 );
          }
          Integer result = KeyChooser.showDialog( finalKey, remote, ( int )btn.getKeyCode() );
          if ( result != null )
          {
            btn = remote.getButton( result );
            finalKey.setText( btn.getName() );
            remoteConfig.setFavFinalKey( btn );
            propertyChangeSupport.firePropertyChange( "data", null, null );
          }
          return;
        }
        int row = favTable.getSelectedRow();
        if ( row == -1 )
        {
          return;
        }
        if ( !favTable.isCellEditable( row, favTable.columnAtPoint( e.getPoint() ) ) )
        {
          editRowObject( row );
        }
      }
    };
    favTable.addMouseListener( openEditor );
    finalKey.addMouseListener( openEditor );
    
    activeTable = favTable;
  }

  @Override
  public void addPropertyChangeListener( PropertyChangeListener listener )
  {
    if ( listener != null )
    {
      if ( favModel != null )
      {
        favModel.addPropertyChangeListener( listener );
      }
      if ( activityGroupModel != null )
      {
        activityGroupModel.addPropertyChangeListener( listener );
      }
      propertyChangeSupport.addPropertyChangeListener( listener );
    }
  }

  @Override
  public void set( RemoteConfiguration remoteConfig )
  {
    this.remoteConfig = remoteConfig;
    Remote remote = remoteConfig.getRemote();
    DefaultComboBoxModel comboModel = new DefaultComboBoxModel( remote.getDeviceButtons() );
    favModel.set( remoteConfig );
    favTable.initColumns( favModel );
    activityGroupTable.setVisible( false );
    favBtn = remote.getButtonByStandardName( "Favorites" );
    newButton.setEnabled( favBtn != null );
    duration.setValue( new Float( remoteConfig.getFavPause() / 10.0 ) );
    if ( favBtn != null )
    {
      activityGroupModel.set( favBtn, remoteConfig );
      activityGroupTable.initColumns( activityGroupModel );
      deviceButtonBox.setModel( comboModel );
      deviceButtonBox.setSelectedItem( remoteConfig.getFavKeyDevButton() ); 
    }
    Button favFinalKey = remoteConfig.getFavFinalKey();
    boolean showFinal = favFinalKey != null;
    addFinal.setSelected( showFinal );
    finalKey.setVisible( showFinal );
    finalKeyLabel.setVisible( showFinal );
    if ( showFinal )
    {
      finalKey.setText( favFinalKey.getName() );
    }
    else
    {
      finalKey.setText( remote.getUpgradeButtons()[ 0 ].getName() );
    }
  }
  
  public void finishEditing()
  {
    if ( favTable.getCellEditor() != null )
    {
      favTable.getCellEditor().stopCellEditing();
    }
    if ( activityGroupTable.getCellEditor() != null )
    {
      activityGroupTable.getCellEditor().stopCellEditing();
    }
  }
  
  private void setHighlightAction( JP1Table table )
  {
    remoteConfig.getOwner().highlightAction.setEnabled( table.getSelectedRowCount() > 0 );
  }
  
  public JP1Table getActiveTable()
  {
    return activeTable;
  }
  
  @Override
  public void actionPerformed( ActionEvent event )
  {
    Object source = event.getSource();
    List< FavScan > favScans = remoteConfig.getFavScans();
    finishEditing();
    int row = 0;
    Remote remote = remoteConfig.getRemote();
    if ( source.getClass() == JButton.class )
    {
      row = favTable.getSelectedRow();
    }

    if ( source == deviceButtonBox )
    {
      DeviceButton deviceButton = ( DeviceButton )deviceButtonBox.getSelectedItem();
      if ( deviceButton != remoteConfig.getFavKeyDevButton() )
      {
        remoteConfig.setFavKeyDevButton( deviceButton );
      }
      propertyChangeSupport.firePropertyChange( "data", null, null );
    }
    else if ( source == addFinal )
    {
      boolean checked = addFinal.isSelected();
      finalKey.setVisible( checked );
      finalKeyLabel.setVisible( checked );
      Button btn = checked ? remote.getButton( finalKey.getText() ) : null;
      remoteConfig.setFavFinalKey( btn );
      propertyChangeSupport.firePropertyChange( "data", null, null );
    }
    else if ( source == upButton || source == downButton )
    {
      FavScan favScan = favScans.get( row );
      favScans.remove( row );
      int toRow = ( source == upButton ) ? row - 1 : row + 1;
      favScans.add( toRow, favScan );      
      favModel.fireTableRowsUpdated( Math.min( row, toRow ), Math.max( row, toRow ));
      favTable.setRowSelectionInterval( toRow, toRow );
    }
    else if ( source == deleteButton )
    {
      int[] rows = favTable.getSelectedRows();
      Arrays.sort( rows );
      for ( int i = rows.length - 1; i >= 0; i-- )
      {
        favScans.remove( rows[ i ] );
      }
      favModel.fireTableRowsDeleted( rows[ 0 ], rows[ rows.length - 1 ] );
    }
    else if ( source == newButton )
    {
      newRowObject();

    }
    else if ( source == cloneButton )
    {
      FavScan orig = favScans.get( row );
      FavScan favScan = new FavScan( orig );
      favScan.setSegmentFlags( orig.getSegmentFlags() );
      favScans.add( favScan );
      row = favScans.size() - 1;
      favModel.fireTableRowsInserted( row, row );
      favTable.setRowSelectionInterval( row, row );
    }
    else if ( source == editButton )
    {
      editRowObject( row );
    }
    activityGroupTable.setVisible( favTable.getModel().getRowCount() > 0 );
  }
  
  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if ( !e.getValueIsAdjusting() )
    {
      if ( favTable.getSelectedRowCount() == 1 )
      {
        int row = favTable.getSelectedRow();
        boolean selected = row != -1;
        upButton.setEnabled( row > 0 );
        downButton.setEnabled( selected && row < favTable.getRowCount() - 1 );
        cloneButton.setEnabled( true );
        editButton.setEnabled( true );
      }
      else
      {
        upButton.setEnabled( false );
        downButton.setEnabled( false );
        cloneButton.setEnabled( false );
        editButton.setEnabled( false );
      }
      deleteButton.setEnabled( favTable.getSelectedRowCount() > 0 );
    }
  }
  
  private void editRowObject( int row )
  {
    List< FavScan > favScans = remoteConfig.getFavScans();
    FavScan favScan = FavScanDialog.showDialog( this, favScans.get( row ), remoteConfig );
    if ( favScan != null )
    {
      favScans.set( row, favScan );
      favModel.fireTableRowsUpdated( row, row );
      propertyChangeSupport.firePropertyChange( "data", null, null );
    }
  }
  
  private void newRowObject()
  {
    List< FavScan > favScans = remoteConfig.getFavScans();
    FavScan favScan = FavScanDialog.showDialog( this, null, remoteConfig );
    if ( favScan != null )
    {
      favScan.setSegmentFlags( 0xFF );
      favScans.add( favScan );
      int row = favScans.size() - 1;
      if ( favTable.getSelectedRowCount() == 0 )
      {
        favTable.setColumnSelectionInterval( 1, 1 );
      }
      favModel.fireTableRowsInserted( row, row );
      favTable.setRowSelectionInterval( row, row );
      favTable.requestFocusInWindow();
    }
  }
  
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport( this );
  private RemoteConfiguration remoteConfig = null;
  public MouseListener openEditor = null;
  private JP1Table favTable = null;
  private JP1Table activeTable = null;
  private FavScanTableModel favModel = new FavScanTableModel();
  private JP1Table activityGroupTable = null;
  private ActivityGroupTableModel activityGroupModel = new ActivityGroupTableModel();
  private Button favBtn = null;
  private JPanel deviceBoxPanel = null;
  private JFormattedTextField duration = null;
  private JComboBox deviceButtonBox = null;
  private JButton editButton = null;
  private JButton newButton = null;
  private JButton cloneButton = null;
  private JButton deleteButton = null;
  private JButton upButton = null;
  private JButton downButton = null;
  private JCheckBox addFinal = null;
  private JTextField finalKey = null;
  private JLabel finalKeyLabel = null;
}

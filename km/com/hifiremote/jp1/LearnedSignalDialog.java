package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

// TODO: Auto-generated Javadoc
/**
 * The Class LearnedSignalDialog.
 */
public class LearnedSignalDialog extends JDialog implements ActionListener, DocumentListener,
  ItemListener
{

  /**
   * Show dialog.
   * 
   * @param locationComp
   *          the location comp
   * @param learnedSignal
   *          the learned signal
   * @return the learned signal
   */
  public static LearnedSignal showDialog( Component locationComp, LearnedSignal learnedSignal,
      RemoteConfiguration config )
  {
    if ( dialog == null )
    {
      dialog = new LearnedSignalDialog( locationComp );
    }

    dialog.setRemoteConfiguration( config );
    dialog.setLearnedSignal( learnedSignal, false );
    
    // Set preferred size of advanced button to that for the wider 
    // of the two possible button captions
    dialog.setAdvancedButtonText( false );
    dialog.pack();
    dialog.advancedButton.setPreferredSize( dialog.advancedButton.getSize() );
    dialog.setAdvancedButtonText( dialog.advancedArea.isVisible() );
    dialog.applyButton.setEnabled( false );
    dialog.pack();

    dialog.setLocationRelativeTo( locationComp );
    dialog.setVisible( true );
    return dialog.learnedSignal;
  }
  
  public static void reset()
  {
    if ( dialog != null )
    {
      dialog.dispose();
      dialog = null;
    }
  }

  /**
   * Instantiates a new learned signal dialog.
   * 
   * @param c
   *          the c
   */
  private LearnedSignalDialog( Component c )
  {
    super( ( JFrame )SwingUtilities.getRoot( c ) );
    setTitle( "Learned Signal Details" );
    setModal( true );

    JComponent contentPane = ( JComponent )getContentPane();
    contentPane.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

    // Create box to hold panels that always show
    Box box = Box.createVerticalBox();
    contentPane.add( box, BorderLayout.PAGE_START );
    
    JPanel topPanel = new JPanel( new BorderLayout() );
    box.add( topPanel );
 
    // Add the signal name (when used) and the bound device and key controls
    JPanel idPanel = new JPanel( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
    namePanel = new JPanel( new FlowLayout( FlowLayout.LEFT, 5, 1 ) );
    namePanel.setBorder( BorderFactory.createTitledBorder( "Signal Name" ) );
    namePanel.add( new JLabel( "Name:" ) );
    namePanel.add( nameField );
    namePanel.setVisible( false );
    nameStrut.setVisible( false );
    idPanel.add( namePanel );
    idPanel.add( nameStrut );
    
    JPanel panel = new JPanel( new FlowLayout( FlowLayout.LEFT, 5, 0 ) );
    panel.setAlignmentX( Component.LEFT_ALIGNMENT );
    panel.setBorder( BorderFactory.createTitledBorder( "Bound Key" ) );
    panel.add( new JLabel( "Device:" ) );
    panel.add( boundDevice );
    panel.add( Box.createHorizontalStrut( 5 ) );
    panel.add( new JLabel( "Key:" ) );
    panel.add( boundKey );
    panel.add( shift );
    panel.add( xShift );     
    idPanel.add( panel );
    
    topPanel.add( idPanel, BorderLayout.LINE_START );
    boundKey.addActionListener( this );
    shift.addActionListener( this );
    xShift.addActionListener( this );
    
    topPanel.add( advancedButton, BorderLayout.LINE_END );
    advancedButton.setToolTipText( "Shows or hides the signal timing details" );
    advancedButton.addActionListener( this );
        
    signalTextArea.setEditable( true );
    signalTextArea.setLineWrap( true );
    signalTextArea.setWrapStyleWord( true );
    signalTextArea.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent e) {
          signalTextChanged();
      }
      public void removeUpdate(DocumentEvent e) {
          signalTextChanged();
      }
      public void insertUpdate(DocumentEvent e) {
          signalTextChanged();
      }
    });
    signalTextArea.setToolTipText( "Edits to Signal Data do not take effect until you press Apply or OK" );
    JScrollPane scrollPane = new JScrollPane( signalTextArea );

    panel = new JPanel( new BorderLayout() );
    panel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Signal Data" ), panel.getBorder() ) );
    
    ButtonGroup bg1 = new ButtonGroup();
    bg1.add( learnButton );
    bg1.add( prontoButton );
    learnButton.setSelected( true );
    learnButton.addItemListener( this );
    prontoButton.addItemListener( this );
    
    ButtonGroup bg2 = new ButtonGroup();
    bg2.add( oddButton );
    bg2.add( evenButton );
    oddButton.setSelected( true );
    oddButton.setEnabled( false );
    evenButton.setEnabled( false );
    keypressLabel.setEnabled( false );
    
    JPanel formatPanel = new JPanel( new FlowLayout() );
    JLabel formatLabel = new JLabel( "Data format:" );
    formatLabel.setToolTipText(
        "<html>When the Apply button is disabled, the Signal Data area displays its<br>"
            + "data in the selected format.  The display will switch between the two<br>"
            + "formats when the selection is changed.  When the Apply button is enabled,<br>"
            + "the data in the Signal Data area will be interpreted as being in the<br>"
            + "selected format when Apply or OK is pressed.  Changing the selected format<br>"
            + "will have no effect on the displayed data.  Make sure that the selected<br>"
            + "format is correct before pressing Apply or OK.</html>" );
    formatPanel.add( formatLabel );
    formatPanel.add( learnButton );
    learnButton.setToolTipText(
        "<html>UEI Learned format represents an IR signal in the original internal format<br>"
            + "used by UEI remotes.  Remotes with a Maxim (MAXQ) or Texas Instruments (TI)<br>"
            + "processor use a newer format.  RMIR converts those formats to and from the<br>"
            + "original one as required, to provide a consistent display across all remotes.</html>" );        
    formatPanel.add( prontoButton );
    prontoButton.setToolTipText(
        "<html>For conversion to UEI Learned, Pronto format accepts all formats other than those<br>"
            + "beginning with 8000.  Those are an index into an internal database to which RMIR<br>"
            + "has no access.  On conversion from UEI Learned, Pronto format provides only the<br>"
            + "Pronto raw formats, those beginning 0000 or 0100.</html>");
    keypressLabel.setToolTipText( 
        "<html>Predefined Pronto formats support protocols that toggle.  These are<br>"
            + "protocols that alternate between two distinct signals on succesive<br>"
            + "keypresses.  UEI learned format does not support toggles, so you can<br>"
            + "here select whether the Pronto should be converted to the learned signal<br>"
            + "for an odd-numbered or even-numbered keypress.</html>");
    formatPanel.add( new JLabel( "        " ) );
    formatPanel.add( keypressLabel );
    formatPanel.add( oddButton );
    formatPanel.add( evenButton );
    oddButton.setToolTipText( 
        "<html>When selected, Pronto signals for protocols that toggle will be<br>"
        + "interpreted as sent on the 1st, 3rd, 5th etc. keypress</html>" );
    evenButton.setToolTipText( 
        "<html>When selected, Pronto signals for protocols that toggle will be<br>"
        + "interpreted as sent on the 2nd, 4th, 6th etc. keypress</html>" );
    panel.add( formatPanel, BorderLayout.PAGE_START );
    panel.add( scrollPane, BorderLayout.CENTER );
    topPanel.add( panel, BorderLayout.PAGE_END );
    
    ItemListener i = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if ( e.getStateChange() == ItemEvent.SELECTED )
          applyButton.setEnabled( true );
      }
    };
    oddButton.addItemListener( i );
    evenButton.addItemListener( i );
    
    table = new JP1Table( model );
    table.setCellSelectionEnabled( false );
    table.setRowSelectionAllowed( true );
    Dimension d = table.getPreferredScrollableViewportSize();
    d.width = table.getPreferredSize().width;
    d.height = 3 * table.getRowHeight();
    table.setPreferredScrollableViewportSize( d );
    table.initColumns( model );
    scrollPane = new JScrollPane( table );
    scrollPane.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Decodes" ), scrollPane.getBorder() ) );
    box.add( scrollPane );
    
    // End of box for panels that always show.
    // Now create advanced area that shows only when requested.  The duration panels
    // in this need to be able to expand, with scroll bars as required, when the dialog
    // is expanded or contracted.
    advancedArea = new JPanel( new BorderLayout() );
    advancedArea.setBorder( BorderFactory.createTitledBorder( "Advanced Details" ) );
    Dimension dim = advancedArea.getPreferredSize();
    dim.height = 300;
    advancedArea.setPreferredSize( dim );
    contentPane.add( advancedArea, BorderLayout.CENTER);

    // add panel with rounding/analysis controls
    advancedAreaControls = new JPanel( new FlowLayout( FlowLayout.LEFT, 1, 1 ) );
    advancedAreaControls.add( new JLabel( " Round To: " ) );
    advancedAreaControls.add( burstRoundBox );
    advancedAreaControls.add( new JLabel( "  Analyzer: ") );
    advancedAreaControls.add( analyzerBox );
    advancedAreaControls.add( new JLabel( "  Analysis: ") );
    advancedAreaControls.add( analysisBox );
    advancedAreaControls.add( new JLabel( "  ") );
    advancedAreaControls.add( analysisMessageLabel );
    advancedArea.add( advancedAreaControls, BorderLayout.PAGE_START );

    // setup analyzer/analysis boxes and message label
    analysisMessageLabel.setText( null );
    i = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          if ( e.getStateChange() == ItemEvent.SELECTED )
            onAnalysisChange();
        }
    };
    analyzerBox.addItemListener( i );
    analysisBox.addItemListener( i );

    // setup round to box
    burstRoundBox.setColumns( 4 );
    burstRoundBox.getDocument().addDocumentListener( dl );
    
    // End of advanced area controls.  Now create box to hold the duration panels
    box = Box.createVerticalBox();
    advancedArea.add( box, BorderLayout.CENTER );
    
    burstTextArea.setEditable( false );
    burstTextArea.setLineWrap( true );
    burstTextArea.setWrapStyleWord( true );
    scrollPane = new JScrollPane( burstTextArea );
    scrollPane.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Bursts" ), scrollPane.getBorder() ) );
    box.add( scrollPane );
    // temporarily hiding bursts...may remove entirely
    burstTextArea.getParent().getParent().setVisible( false );

    onceDurationTextArea.setEditable( false );
    onceDurationTextArea.setLineWrap( true );
    onceDurationTextArea.setWrapStyleWord( true );
    scrollPane = new JScrollPane( onceDurationTextArea );
    scrollPane.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Sent Once" ), scrollPane.getBorder() ) );
    box.add( scrollPane );

    repeatDurationTextArea.setEditable( false );
    repeatDurationTextArea.setLineWrap( true );
    repeatDurationTextArea.setWrapStyleWord( true );
    scrollPane = new JScrollPane( repeatDurationTextArea );
    scrollPane.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Sent Repeatedly" ), scrollPane.getBorder() ) );
    box.add( scrollPane );

    extraDurationTextArea.setEditable( false );
    extraDurationTextArea.setLineWrap( true );
    extraDurationTextArea.setWrapStyleWord( true );
    scrollPane = new JScrollPane( extraDurationTextArea );
    scrollPane.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Sent on Release" ), scrollPane.getBorder() ) );
    box.add( scrollPane );
    
    // End of duration panels. Now add the action buttons.
    JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
    advancedArea.add( buttonPanel, BorderLayout.PAGE_END );
    
    unlockButton.addActionListener( this );
    unlockButton.setEnabled( false );
    unlockButton.setToolTipText( "<html>Release lock that makes Timing summary use rounding set here for<br>"
        + "this signal and analysis.  When button is disabled, rounding is unlocked.</html>" );
    buttonPanel.add( unlockButton );
    
    applyButton.addActionListener( this );
    applyButton.setEnabled( false );
    applyButton.setToolTipText( "<html>Apply edits made in the Signal Data panel without closing dialog.<br>"
        + "Save the current analysis settings.</html>" );
    buttonPanel.add( applyButton );

    okButton.addActionListener( this );
    okButton.setToolTipText( "Apply all Signal Data edits, save current analysis settings and exit dialog." );
    buttonPanel.add( okButton );

    cancelButton.addActionListener( this );
    cancelButton.setToolTipText( "Abandon all edits since they were last applied, then exit dialog." );
    buttonPanel.add( cancelButton );
    
    advancedArea.setVisible( false );
    setAdvancedButtonText( advancedArea.isVisible() );
  }
  
  DocumentListener dl = new DocumentListener() {
    public void changedUpdate(DocumentEvent e) {
      roundingChanged();
    }
    public void removeUpdate(DocumentEvent e) {
      roundingChanged();
    }
    public void insertUpdate(DocumentEvent e) {
      roundingChanged();
    }
  };
  
  private void roundingChanged()
  {
//    signalTextChanged();
    setAdvancedAreaTextFields();
    applyButton.setEnabled( true );
  }

  /**
   * Sets the learned signal.
   * 
   * @param learnedSignal
   *          the new learned signal
   */
  private void setLearnedSignal( LearnedSignal learnedSignal, boolean applyOnly )
  {
    if ( !applyOnly )
    {
      table.initColumns( model );
      if ( learnedSignal == null )
      {
        this.learnedSignal = new LearnedSignal( 0, 0, 0, new Hex(), null );
        nameField.setText( null );
        boundKey.setSelectedIndex( 0 );
        shift.setSelected( false );
        xShift.setSelected( false );
        model.set( this.learnedSignal );
        signalTextArea.setText( null );
        burstRoundBox.getDocument().removeDocumentListener( dl );
        burstRoundBox.setText( null );
        burstRoundBox.getDocument().addDocumentListener( dl );
        burstTextArea.setText( null );
        onceDurationTextArea.setText( null );
        repeatDurationTextArea.setText( null );
        extraDurationTextArea.setText( null );
        analyzerBox.setModel( new DefaultComboBoxModel( new String[] { "..." } ) );
        analysisBox.setModel( new DefaultComboBoxModel( new String[] { "..." } ) );
        return;
      }
      this.learnedSignal = learnedSignal;
      Remote remote = config.getRemote();
      nameField.setText( learnedSignal.getName() );
      boundDevice.setSelectedItem( remote.getDeviceButton( learnedSignal.getDeviceButtonIndex() ) );
      setButton( learnedSignal.getKeyCode(), boundKey, shift, xShift );
      model.set( learnedSignal );
      signalTextLock = true;
      signalTextArea.setText( learnedSignal.getSignalHexText() );
      signalTextLock = false;
      learnButton.setSelected( true );
    }

    LearnedSignalTimingAnalyzer timingAnalyzer = this.learnedSignal.getTimingAnalyzer();
    if ( !timingAnalyzer.getIsValid() )
    {
      burstRoundBox.setText( null );
      analysisMessageLabel.setText( null );
      burstTextArea.setText( "Unable to unpack learned signal data...analysis not possible." );
      onceDurationTextArea.setText( null );
      repeatDurationTextArea.setText( null );
      extraDurationTextArea.setText( null );
      burstTextArea.setRows( 1 );
      onceDurationTextArea.setRows( 1 );
      repeatDurationTextArea.setRows( 1 );
      extraDurationTextArea.setRows( 1 );
      onceDurationTextArea.getParent().getParent().setVisible( false );
      repeatDurationTextArea.getParent().getParent().setVisible( false );
      extraDurationTextArea.getParent().getParent().setVisible( false );
      analyzerBox.setModel( new DefaultComboBoxModel( new String[] { "..." } ) );
      analysisBox.setModel( new DefaultComboBoxModel( new String[] { "..." } ) );
      pack();
    }
    else
    {
      analysisUpdating = true;
      advancedAreaUpdating = true;

      analyzerBox.setModel( new DefaultComboBoxModel( timingAnalyzer.getAnalyzerNames() ) );
      analyzerBox.setSelectedItem( timingAnalyzer.getSelectedAnalyzer().getName() );
      analysisBox.setModel( new DefaultComboBoxModel( timingAnalyzer.getSelectedAnalyzer().getAnalysisNames() ) );
      analysisBox.setSelectedItem( timingAnalyzer.getSelectedAnalysisName() );
      analysisMessageLabel.setText( timingAnalyzer.getSelectedAnalysis() != null ? timingAnalyzer.getSelectedAnalysis().getMessage() : "No valid analysis" );
      burstRoundBox.setText( Integer.toString( timingAnalyzer.getSelectedAnalyzer().getRoundTo() ) );

      // the accesses above will have initialized the timing analyzer to last selcted or preferred analyzer/analysis, so we save the state
      timingAnalyzer.saveState();
      // we'll back out any changes with restoreState if the user clicks cancel

      advancedAreaUpdating = false;
      analysisUpdating = false;

      setAdvancedAreaTextFields();
    }
  }

  private void onAnalysisChange()
  {
    if ( analysisUpdating )
      return;
    analysisUpdating = true;

    applyButton.setEnabled( true );

    LearnedSignalTimingAnalyzer timingAnalyzer = this.learnedSignal.getTimingAnalyzer();
    if ( !timingAnalyzer.getSelectedAnalyzer().getName().equals( analyzerBox.getSelectedItem().toString() ) )
    {
      timingAnalyzer.setSelectedAnalyzer( analyzerBox.getSelectedItem().toString() ); // will auto select preferred analysis
      analysisBox.setModel( new DefaultComboBoxModel( timingAnalyzer.getSelectedAnalyzer().getAnalysisNames() ) );
      analysisBox.setSelectedItem( timingAnalyzer.getSelectedAnalysisName() );
      burstRoundBox.getDocument().removeDocumentListener( dl );
      burstRoundBox.setText( Integer.toString( timingAnalyzer.getSelectedAnalyzer().getRoundTo() ) );
      burstRoundBox.getDocument().addDocumentListener( dl );
      setAdvancedAreaTextFields();
    }
    else
    {
      timingAnalyzer.setSelectedAnalysisName( analysisBox.getSelectedItem().toString() );
      setAdvancedAreaTextFields();
    }
    analysisUpdating = false;
  }

  private void setAdvancedAreaTextFields()
  {
    if ( advancedAreaUpdating )
      return;
    advancedAreaUpdating = true;

    LearnedSignalTimingAnalysis analysis;

    if ( Boolean.parseBoolean( RemoteMaster.getProperties().getProperty( "LearnedSignalTimingAnalysis", "false" ) ) )
    {
      advancedAreaControls.setVisible( true );

      int r = 1;
      String roundText = burstRoundBox.getText();
      if ( roundText != null && !roundText.isEmpty() )
        try { r = Integer.parseInt( roundText ); }
        catch (NumberFormatException e) { r = 1; }

      LearnedSignalTimingAnalyzerBase analyzer = this.learnedSignal.getTimingAnalyzer().getSelectedAnalyzer();
      unlockButton.setEnabled( analyzer.getIsRoundingLocked() );
      if ( r != analyzer.getRoundTo() )
      {
        analyzer.unlockRounding();
        analyzer.setRoundTo( r );
        analyzer.lockRounding();
        unlockButton.setEnabled( true );
      }

      analysis = this.learnedSignal.getTimingAnalyzer().getSelectedAnalysis();
      if ( analysis == null )
      {
        LearnedSignalTimingAnalyzer timingAnalyzer = this.learnedSignal.getTimingAnalyzer();
        analysis = timingAnalyzer.getSelectedAnalyzer().getPreferredAnalysis();
        analysisBox.setModel( new DefaultComboBoxModel( timingAnalyzer.getSelectedAnalyzer().getAnalysisNames() ) );
        if ( analysis != null )
        {
          analysisBox.setSelectedItem( analysis.getName() );
        }
      }
      analysisMessageLabel.setText( analysis != null ? analysis.getMessage() : "No valid analysis" );
    }
    else
    {
      advancedAreaControls.setVisible( false );
      analysis = this.learnedSignal.getTimingAnalyzer().getAnalyzer( "Raw Data" ).getAnalysis( "Even" );
    }

    String temp = analysis != null ? analysis.getBurstString() : "** No signal **";
    burstTextArea.setText( temp );
    burstTextArea.setRows( (int)Math.ceil( (double)temp.length() / 75.0 ) );

    temp = analysis != null ? analysis.getOneTimeDurationString() : "** No signal **";
    onceDurationTextArea.setText( temp );
    onceDurationTextArea.setRows( (int)Math.ceil( (double)temp.length() / 75.0 ) );
    onceDurationTextArea.getParent().getParent().setVisible( !temp.equals( "** No signal **" ) );

    temp = analysis != null ? analysis.getRepeatDurationString() : "** No signal **";
    repeatDurationTextArea.setText( temp );
    repeatDurationTextArea.setRows( (int)Math.ceil( (double)temp.length() / 75.0 ) );
    repeatDurationTextArea.getParent().getParent().setVisible( !temp.equals( "** No signal **" ) );

    temp = analysis != null ? analysis.getExtraDurationString() : "** No signal **";
    extraDurationTextArea.setText( temp );
    extraDurationTextArea.setRows( (int)Math.ceil( (double)temp.length() / 75.0 ) );
    extraDurationTextArea.getParent().getParent().setVisible( !temp.equals( "** No signal **" ) );

    pack();
    advancedAreaUpdating = false;
  }

  private void setRemoteConfiguration( RemoteConfiguration config )
  {
    this.config = config;
    Remote remote = config.getRemote();
    shift.setText( remote.getShiftLabel() );
    xShift.setText( remote.getXShiftLabel() );
    xShift.setVisible( remote.getXShiftEnabled() );
    boundDevice.setModel( new DefaultComboBoxModel( remote.getDeviceButtons() ) );
    boundKey.setModel( new DefaultComboBoxModel( remote.getLearnButtons() ) );
    namePanel.setVisible( remote.usesEZRC() );
    nameStrut.setVisible( remote.usesEZRC() );
  }

  private void setButton( int code, JComboBox comboBox, JCheckBox shiftBox, JCheckBox xShiftBox )
  {
    Remote remote = config.getRemote();
    Button b = remote.getButton( code );
    if ( !Arrays.asList( remote.getLearnButtons() ).contains( b ) )
    {
      b = null;
    }
    
    if ( b == null )
    {
      int base = code & 0x3F;
      if ( base != 0 )
      {
        b = remote.getButton( base );
        if ( ( base | remote.getShiftMask() ) == code )
        {
          shiftBox.setEnabled( b.allowsShiftedMacro() );
          shiftBox.setSelected( true );
          comboBox.setSelectedItem( b );
          return;
        }
        if ( remote.getXShiftEnabled() && ( base | remote.getXShiftMask() ) == code )
        {
          xShiftBox.setEnabled( remote.getXShiftEnabled() & b.allowsXShiftedMacro() );
          xShiftBox.setSelected( true );
          comboBox.setSelectedItem( b );
          return;
        }
      }
      b = remote.getButton( code & ~remote.getShiftMask() );
      if ( b != null )
      {
        shiftBox.setSelected( true );
      }
      else if ( remote.getXShiftEnabled() )
      {
        b = remote.getButton( code ^ ~remote.getXShiftMask() );
        if ( b != null )
        {
          xShiftBox.setSelected( true );
        }
      }
    }

    shiftBox.setEnabled( b.allowsShiftedKeyMove() );
    xShiftBox.setEnabled( b.allowsXShiftedKeyMove() );

    if ( b.getIsXShifted() )
    {
      xShiftBox.setSelected( true );
    }
    else if ( b.getIsShifted() )
    {
      shiftBox.setSelected( true );
    }

    comboBox.removeActionListener( this );
    comboBox.setSelectedItem( b );
    comboBox.addActionListener( this );
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed( ActionEvent event )
  {
    Object source = event.getSource();
    Remote remote = config.getRemote();
    Button b = ( Button )boundKey.getSelectedItem();
    boolean ok = true;
    
    if ( source == applyButton || source == okButton )
    {
      // Assumes data is in original format (format=0)
      String notes = learnedSignal.getNotes();
      int deviceIndex = ( ( DeviceButton )boundDevice.getSelectedItem() ).getButtonIndex();
      int keyCode = getKeyCode( boundKey, shift, xShift );
      learnedSignal.setDeviceButtonIndex( deviceIndex );
      learnedSignal.setKeyCode( keyCode );
      learnedSignal.setName( nameField.getText() );
      
      if ( signalTextHasChanged )
      {
        int format = remote.getLearnedFormat();
        ProntoSignal ps = null;
        if ( learnButton.isSelected() )
        {
          // Signal text area should hold learned signal data in format 0 without any header
          Hex data = new Hex( Hex.parseHex( signalTextArea.getText() ) );
          learnedSignal.setData( data );
          learnedSignal.setFormat( 0 );
        }
        else if ( prontoButton.isSelected() )
        {
          ps = new ProntoSignal( signalTextArea.getText() );
          ps.unpack( oddButton.isSelected() ? 0 : 1 );
          if ( ps.error == null )
          {
            LearnedSignal ls = ps.makeLearned( format );
            if ( ps.error == null )
            {
              learnedSignal.setData( ls.getData() ); 
              learnedSignal.setFormat( format );
            }
          }
        }
        
        learnedSignal.clearTimingAnalyzer();
        if ( config.hasSegments() )
        {
          // set default value
          learnedSignal.setSegmentFlags( 0xFF );
        }
        
        if ( remote.isSSD() && learnedSignal.getHeader() == null )
        {
          // Only two header values have been seen, this one and 10 00 00 18 60 00 00.  Tests
          // seem to show that it makes no difference which is used.
          learnedSignal.setHeader( new Hex( new short[]{ 0x10, 00, 00, 0x18, 0x20, 00, 00 } ) );
        }

        UnpackLearned ul = learnedSignal.getUnpackLearned();
        
        if ( ps != null && ps.error != null )
        {
          ok = false;
          String message = "Malformed Pronto signal: " + ps.error;
          String title = "Pronto Signal Error";
          JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
        }
        else if ( ! ul.ok )
        {
          ok = false;
          String message = "Malformed learned signal: " + ul.error;
          String title = "Learned Signal Error";
          JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
        }
        else if ( format != learnedSignal.getFormat() )
        {
          // This conversion will be from format 0 to format 1 or 2, so should not cause an error
          ps = new ProntoSignal( learnedSignal );
          learnedSignal.setData( ps.makeLearned( format ).getData() );
          learnedSignal.setFormat( format );
        }
      }
      else
      {
        // re-save any changes on apply or ok...in this else
        // since signal data changes clears out the analyzer anyway
        learnedSignal.getTimingAnalyzer().saveState();
      }
    }
    
    if ( source == applyButton && ok )
    {
      setLearnedSignal( learnedSignal, true );
      model.set( learnedSignal );
      applyButton.setEnabled( false );
    }
    else if ( source == okButton && ok )
    {
      setVisible( false );
    }
    else if ( source == cancelButton )
    {
      // back out any timing analysis changes
      learnedSignal.getTimingAnalyzer().restoreState();
      learnedSignal = null;
      setVisible( false );
    }
    else if ( source == unlockButton )
    {
      learnedSignal.getTimingAnalyzer().getSelectedAnalyzer().unlockRounding();
      unlockButton.setEnabled( false );
    }
    else if ( source == advancedButton )
    {
      advancedArea.setVisible( ! advancedArea.isVisible() );
      setAdvancedButtonText( advancedArea.isVisible() );
      pack(); 
    }
    else if ( source == shift )
    {
      if ( shift.isSelected() )
      {
        xShift.setSelected( false );
      }
      else if ( b != null && remote.getXShiftEnabled() )
      {       
        xShift.setSelected( b.needsShift( Button.LEARN_BIND ) );
      }
    }
    else if ( source == xShift )
    {
      if ( xShift.isSelected() )
      {
        shift.setSelected( false );
      }
      else if ( b != null )
      {
        shift.setSelected( b.needsShift( Button.LEARN_BIND ) );
      }
    }    
    else if ( source == boundKey )
    {
      if ( b != null )
      {
        b.setShiftBoxes( Button.LEARN_BIND, shift, xShift );
      }
    }
  }
  
  private void setAdvancedButtonText( boolean hide )
  {
    String text = "<html><center>";
    text += hide ? "Hide " : "Show ";
    text += "Advanced<br>Details</center></html>";
    advancedButton.setText( text );
  }
  
  private int getKeyCode( JComboBox comboBox, JCheckBox shiftBox, JCheckBox xShiftBox )
  {
    int keyCode = ( ( Button )comboBox.getSelectedItem() ).getKeyCode();
    if ( shiftBox.isSelected() )
    {
      keyCode |= config.getRemote().getShiftMask();
    }
    else if ( xShiftBox.isSelected() )
    {
      keyCode |= config.getRemote().getXShiftMask();
    }
    return keyCode;
  }
  
  private boolean signalTextHasChanged = false;
  private boolean signalTextLock = false;
  private void signalTextChanged()
  {
    if ( signalTextLock )
      return;
    signalTextHasChanged = true;
    applyButton.setEnabled( true );
  }

  private void documentChanged( DocumentEvent e )
  {
    applyButton.setEnabled( true );
  }

  private RemoteConfiguration config = null;

  /** The bound device. */
  private JComboBox boundDevice = new JComboBox();

  /** The bound key. */
  private JComboBox boundKey = new JComboBox();

  /** The shift. */
  private JCheckBox shift = new JCheckBox();

  /** The x shift. */
  private JCheckBox xShift = new JCheckBox();

  /** The ok button. */
  private JButton okButton = new JButton( "OK" );

  private JButton cancelButton = new JButton( "Cancel" );
  
  private JButton applyButton = new JButton( "Apply" );
  private JButton unlockButton = new JButton( "Unlock" );
  private JButton advancedButton = new JButton();
  
  private JPanel advancedArea = null;

  private boolean advancedAreaUpdating = false;
  private boolean analysisUpdating = false;

  private JPanel namePanel = null;
  private JTextField nameField = new JTextField( 10 );
  private Component nameStrut = Box.createHorizontalStrut( 5 );
  
  // panel holding advanced area controls
  private JPanel advancedAreaControls = new JPanel();
  // text box to enter rounding of times
  private JTextField burstRoundBox = new JTextField();
  // drop down to pick timing analyzer
  private JComboBox analyzerBox = new JComboBox();
  // drop down to pick timing analysis
  private JComboBox analysisBox = new JComboBox();
  // label to hold analysis result message
  private JLabel analysisMessageLabel = new JLabel();

  /** The burst text area. */
  private JTextArea burstTextArea = new JTextArea( 4, 70 );

  /** The duration text area. */
  private JTextArea onceDurationTextArea = new JTextArea( 8, 70 );

  /** The duration text area. */
  private JTextArea repeatDurationTextArea = new JTextArea( 8, 70 );

  /** The duration text area. */
  private JTextArea extraDurationTextArea = new JTextArea( 8, 70 );

  /** The duration text area. */
  //private JTextArea durationTextArea = new JTextArea( 8, 70 );
  
  private JTextArea signalTextArea = new JTextArea( 6, 70 );
  
  private JRadioButton learnButton = new JRadioButton( "UEI Learned" );
  private JRadioButton prontoButton = new JRadioButton( "Pronto" );
  private JRadioButton oddButton = new JRadioButton( "Odd" );
  private JRadioButton evenButton = new JRadioButton( "Even" );
  private JLabel keypressLabel = new JLabel( "Keypress:" );

  /** The learned signal. */
  private LearnedSignal learnedSignal = null;

  /** The model. */
  private JP1Table table = null;
  private DecodeTableModel model = new DecodeTableModel();

  /** The dialog. */
  private static LearnedSignalDialog dialog = null;

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  @Override
  public void insertUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  @Override
  public void removeUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  @Override
  public void itemStateChanged( ItemEvent e )
  {
    if ( e.getStateChange() == ItemEvent.SELECTED )
    {
      oddButton.setEnabled( prontoButton.isSelected() );
      evenButton.setEnabled( prontoButton.isSelected() );
      keypressLabel.setEnabled( prontoButton.isSelected() );
      if ( applyButton.isEnabled() )
      {
        return;
      }
      signalTextLock = true;
      if ( learnButton.isSelected() )
      {
        signalTextArea.setText( learnedSignal.getSignalHexText() );
      }
      else if ( prontoButton.isSelected() && learnedSignal != null && learnedSignal.getData().length() > 0 )
      {
        ProntoSignal ps = new ProntoSignal( learnedSignal );
        ps.makePronto();
        signalTextArea.setText( ps.toString() );
      }
      signalTextLock = false;
    }
  }
}

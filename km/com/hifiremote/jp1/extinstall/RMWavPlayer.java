package com.hifiremote.jp1.extinstall;

import java.io.File;
import java.io.IOException;
 

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.hifiremote.jp1.RemoteMaster.TimingTask;
 
/**
 * This is based on a demonstration program of how to play back an audio file
 * using the Clip in Java Sound API.
 * @author www.codejava.net
 *
 * Modified by Graham Dixon (mathdon) for use in RMIR.
 */

public class RMWavPlayer implements LineListener 
{
  public void open( File audioFile )
  {
    try
    {
      AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
      AudioFormat format = audioStream.getFormat();
      DataLine.Info info = new DataLine.Info(Clip.class, format);
      audioClip = (Clip) AudioSystem.getLine(info);
      audioClip.addLineListener(this);
      audioClip.open(audioStream);
    }
    catch (UnsupportedAudioFileException ex) 
    {
      System.err.println("The specified audio file is not supported.");
      ex.printStackTrace();
    }
    catch (LineUnavailableException ex) 
    {
      System.err.println("Audio line for playing back is unavailable.");
      ex.printStackTrace();
    } 
    catch (IOException ex) 
    {
      System.err.println("Error playing the audio file.");
      ex.printStackTrace();
    }
  }

  /**
   * Returns duration in tenths of a second
   */
  public int getDuration()
  {
    long duration = audioClip.getMicrosecondLength();   // length in microseconds
    return ( int )( duration / 100000 );
  }

  public void play()
  {
    if ( timer != null )
      timer.execute();
    audioClip.start();

    if ( consoleApp )
    {
      while ( !playCompleted ) 
      {
        // wait for the playback completes
        try 
        {
          Thread.sleep(1000);
        } 
        catch (InterruptedException ex) 
        {
          ex.printStackTrace();
        }
      }
    }
  }

  /**
   * Listens to the START and STOP events of the audio line.
   */
  @Override
  public void update(LineEvent event) 
  {
    LineEvent.Type type = event.getType();

    if (type == LineEvent.Type.START) 
    {
      System.err.println("Playback started.");
    } 
    else if (type == LineEvent.Type.STOP) 
    { 
      if ( !playCompleted )
        System.err.println("Playback completed.");
      playCompleted = true;
      close();
      if ( timer != null )
        timer.setCancelled( true );
    }
  }

  public void close()
  {
    if ( !playCompleted )
      System.err.println("Playback cancelled by user.");
    playCompleted = true;
    audioClip.close();
  }

  public TimingTask getTimer()
  {
    return timer;
  }

  public void setTimer( TimingTask timer )
  {
    this.timer = timer;
  }

  public static void main(String[] args) 
  {
    consoleApp = true;
    File audioFile = new File(args[0]);
    RMWavPlayer player = new RMWavPlayer();
    player.open(audioFile);
    player.play();
  }

  private Clip audioClip = null;
  private boolean playCompleted = false;
  private static boolean consoleApp = false;
  private TimingTask timer = null;

}


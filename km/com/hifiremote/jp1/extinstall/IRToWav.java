package com.hifiremote.jp1.extinstall;

/**
 * @author Alain Berguerand (aberguerand@yahoo.com)
 */

// IRToWav.java
// V2.0     28.07.2019 restructured by Graham Dixon (mathdon) for use in RMIR
// V1.9     01.02.2007
// V1.8     12.01.2007
// V1.7     08.04.2006
// V1.6     12.08.2005
// V1.5     16.12.2003
// V1.3     03.06.2003
// V1.2     12.01.2003
// V1.1     09.01.2003
// V1.0     25.12.2002

import java.io.* ;

import com.hifiremote.jp1.ProgressUpdater;

class IRToWav
// Open an IR generated image of the upgrade area and output a corresponding .wav file.
{
  static String  nameWav = "Upgrade.WAV" ; // Default output wave file name
  private String  sig = "" ; // Default signature
  private int maxSize = 4096 ; // Max size of handled IR file
  private int [] img = new int [0x10000] ; // Stores upgrade data
  private int word = 9 ;         // Number of bits in the bit stream to make a byte
  private int [] res = new int [10* (maxSize + 2000)] ; // Stores bit stream
  private int [] wk = new int [0x10000] ; // Stores byte stream
  private int upStart = -1 ;
  private int upSize = 0;
  private int offset = 0;

  private int cur = 0 ;
  private int blockSize = 40 ; // Maximum size of data block
  private int pageSize = 512 ; // Size of page for clear page block
  private int [] riff = { 0x52, 0x49, 0x46, 0x46 } ; // 'RIFF'
  private int [] wavHead = {                        0x57, 0x41, 0x56, 0x45, 0x66, 0x6d, 0x74, 0x20, 0x10, 0x00, 0x00,
   0x00, 0x01, 0x00, 0x01, 0x00, 0x22, 0x56, 0x00, 0x00, 0x22, 0x56, 0x00, 0x00, 0x01, 0x00, 0x08, 0x00, 0x64, 0x61,
   0x74, 0x61 } ;

  private int samplefreq = 22050 ; // Hz
  private int carrierfreq = 2400 ; // Hz
  private double sampling = 3.141592654 * 2.0 * carrierfreq / samplefreq ; // Interval between output samples, radians
  private boolean isHCS08 = false ;
  private ProgressUpdater progressUpdater = null;

  public IRToWav( String sig, int upStart, int upSize, int offset, boolean isHCS08, int[] img )
  {
    this.sig = sig;
    this.upStart = upStart;
    this.upSize = upSize;
    this.offset = offset;
    this.isHCS08 = isHCS08;
    this.img = img;
  }

  public static void main(String[] args)
  {
    String  nameImg = "" ; // Default image file name
    String  sig = "" ; // Default signature
    boolean isHCS08 = false ;
    int startAdr = -1 ;
    int endAdr = -1;
    int[] img = new int [0x10000];
    
    FileReader image ;
    int upSize = 0 ; // Size of upgrade area
    int upStart = -1 ; // Starting address of upgrade area
    int offset = 0 ; // Offset from start of upgrade
    boolean inAdr ;

    int readByte, curData;
    int i;
    byte sigBytes [] = new byte [4] ;

    // Get img file name
    for (i=0; i < args.length; i++)
    {
       switch (i)
       {
         case 0 : // IR output file name
            nameImg = args[i] ;
            break ;
         case 1 : // Signature
            sig = args[i] ;
            if  (sig.length() ==5)
              if (sig.charAt(4) == '+')
              {
                isHCS08=true ;
                sig = sig.substring (0,4);
              }
            break ;
         case 2 : // Start Address
            startAdr = Integer.parseInt(args[i], 16) ;
            break ;
         case 3 : // End Address
            endAdr = Integer.parseInt(args[i], 16) ;
            break ;

         default :
            break ;
       }
    }
    if ((endAdr >=0 ) && (endAdr < startAdr))
    {
      System.err.println("Starting address " +startAdr + " must be smaller than ending address " + endAdr +" !");
      System.exit (0) ;
    }
    if ((sig.length() !=4) & (sig !="")  )
    {
      System.err.println("Invalid signature #"+ sig +"#. It must be exactly 4 characters long !");
      System.exit (0) ;
    }


    try
    {
      // Open and read IR file
      image = new FileReader (nameImg) ;
      readByte = image.read () ;
      i = 0 ;
      inAdr = true ;
      curData = 0 ;

      // Copy file in memory
      // Code is clumsy but works
      while ( readByte > -1)
      {
        if (inAdr) // We are in the address part, construct it
        {
          if ((readByte >= '0') & (readByte <= '9'))
          {
            curData = curData * 0x10 + readByte - '0' ;
          }
          else
          if ((readByte >= 'A') & (readByte <= 'F'))
          {
             curData = curData * 0x10 + readByte - 'A' + 0xA ;
          }
          else
          if ((readByte >= 'a') & (readByte <= 'f'))
          {
             curData = curData * 0x10 + readByte - 'a' + 0xA ;
          }
          else// End of address part
          if (readByte == ':')
          {
            if (upStart == -1)
              upStart = curData ;
            inAdr = false ;
            curData = -1 ;
          }
        }
        else // We are in the data part
        {
          if ((readByte >= '0') & (readByte <= '9'))
          {
            if (curData == -1)
              curData = readByte - '0' ;
            else
              curData = curData * 0x10 + readByte - '0' ;
          }
          else
          if ((readByte >= 'A') & (readByte <= 'F'))
          {
            if (curData == -1)
              curData = readByte - 'A' + 0xA ;
            else
              curData = curData * 0x10 + readByte - 'A' + 0xA ;
          }
          else
          if ((readByte >= 'a') & (readByte <= 'f'))
          {
            if (curData == -1)
              curData = readByte - 'a' + 0xA ;
            else
              curData = curData * 0x10 + readByte - 'a' + 0xA ;
          }
          else
          if (readByte == ' ')
          {
            if (curData != -1)
            {
              img[i] = curData ;
              curData = -1 ;
              i++ ;
            }
          }
          else // End of line
          {
             if (curData != -1)
             {
               img[i] = curData ;
               i++ ;
             }
             inAdr = true ;
             curData = 0;
          }
        }
        readByte = image.read () ;
      }
      img[i] = curData ;
      i++ ;
      upSize = i;
      if (endAdr == -1)
        endAdr = upStart + upSize ;
      image.close();
    }
    catch(FileNotFoundException e)
    {
      System.err.println("Could not open IR file");
    }
    catch(IOException e)
    {
      System.err.println("Could not close IR file");
    }
    
    // Get signature
    if ( (upStart <= 0x02) & ((upStart + upSize) >= 0x06) &
        ((sig.compareTo("none")==0) | (sig ==""))  )
    {
      sigBytes[0] = (byte) img[2 - upStart] ;
      sigBytes[1] = (byte) img[3 - upStart] ;
      sigBytes[2] = (byte) img[4 - upStart] ;
      sigBytes[3] = (byte) img[5 - upStart] ;
      sig =  new String(sigBytes) ;
    }
    if (sig == "")
      sig = "EBV0" ;
    System.err.print("Signature " + sig);
    offset = 0 ;

    // Handle start and end address
    if ( (endAdr < (upStart + upSize)) & (endAdr > upStart) )
      upSize = endAdr - upStart + 1 ;
    if ( (startAdr > upStart) & (startAdr <(upStart + upSize)) )
    {
      upSize = upSize - startAdr + upStart ;
      offset = startAdr-upStart ;
      upStart = startAdr ;
    } ;

    IRToWav irToWav = new IRToWav( sig, upStart, upSize, offset, isHCS08, img );
    irToWav.generate( new File( nameWav ) );
  }

  void generate( File wavFile )
  {
    FileOutputStream wav ;
    int i, j, k; 
    // Variables used to prepare the byte stream
    int dataStart, dataEnd, dataPos, blockCount, blockLen;
    // Variables used to extract the phase changes
    int maxcount, maxwk, pageCount, curPage ;
    long lcounter, smpcount;
    int flip ; // Value of the next bit, will be inverted after a phase change
    int samplecount ;
    double sine ;
    // Variables used to prepare the bit stream
    int curbyte, curbit, prevbit, pos ;

    if ( progressUpdater != null )
      progressUpdater.updateProgress( 0 );
    
    try
    {
      // Prepare byte stream
      cur = 0 ;
      /* Synchro blocks */
      for (j=0; j<4 ;j++)
      {
        //Block header
        for (i=0; i<8;i++)
          addByte (0xA8);
        addByte (0x00) ;
        // Block length
        addByte (0x03) ;
        // Block type
        addByte (0xFF) ;
        // Block trailer
        addByte (0xFD) ;
      }

      /* Last synchro block */
      //Block header
      for (i=0; i<8;i++)
        addByte (0xA8);
      addByte (0x00) ;
      // Block length
      addByte (0x05) ;
      // Block type
      addByte (0x00) ;
      // Block data
      addByte (0x00) ;
      addByte (0xFF) ;
      // Block trailer
      addByte (0xFB) ;


      /* Header block */
      //Block header
      for (i=0; i<8;i++)
        addByte (0xA8);
      addByte (0x00) ;
      // Block length
      dataStart = addByte (0x0B) ; // Remember index in order to compute checksum
      // Block type
      addByte (0x56) ;
      // Var data length
      addByte (0x06) ;
      // Signature
      addByte (sig.charAt(0)) ;
      addByte (sig.charAt(1)) ;
      addByte (sig.charAt(2)) ;
      addByte (sig.charAt(3)) ;
      // Number of data blocks
      blockCount = upSize / (blockSize-3)  +1 ;
      dataEnd = addByte ( blockCount) ;
      // Block trailer
      // First check sum
      i = sum (dataStart+1, dataEnd) ;
      dataEnd = addByte (i % 0x100) ;
      // Second check sum
      i = sum (dataStart-1, dataEnd) ;
      i = 0x10000 - i ;
      addWord (i);

      /* Clear page block */
      if (isHCS08)
        pageCount = ( (upSize-1) / pageSize) +1;
      else
        pageCount = 0 ;
      //      pageCount=3;
      for (curPage=0; curPage < pageCount; curPage++)
      {
        //Block header
        for (i=0; i<8;i++)
          addByte (0xA8);
        addByte (0x00) ;
        // Block length
        dataStart = addByte (0x09) ; // Remember index in order to compute checksum
        // Block type
        addByte (0x43) ;
        // Page address
        addWord (upStart + curPage * pageSize) ;
        // Page count
        addByte (0) ;
        dataEnd = addByte (1) ;
        // Block trailer
        // First check sum
        i = sum (dataStart+1, dataEnd) ;
        dataEnd = addByte (i % 0x100) ;
        // Second check sum
        i = sum (dataStart-1, dataEnd) ;
        i = 0x10000 - i ;
        addWord (i);
      } ;


      /* Data blocks */
      dataPos = 0 ;
      for (j=0; j<blockCount; j++)
      {
        //Block header
        for (i=0; i<8;i++)
          addByte (0xA8);
        addByte (0x00) ;
        // Block length
        if (j < (blockCount -1))
          blockLen = blockSize ;
        else
          blockLen = upSize % (blockSize - 3) + 3 ;
        dataStart = addByte (blockLen + 5) ; // Remember index in order to compute checksum
        // Block type
        addByte (0x53) ;
        // Var data length
        addByte (blockLen) ;
        // Address
        addWord (upStart + dataPos);
        // Data bytes
        for (k=0; k< (blockLen-3);k++)
        {
          dataEnd = addByte (img[dataPos+offset]);
          dataPos++;
        }
        // Block trailer
        // First check sum
        i = sum (dataStart+1, dataEnd) ;
        dataEnd = addByte (i % 0x100) ;
        // Second check sum
        i = sum (dataStart-1, dataEnd) ;
        i = 0x10000 - i ;
        addWord (i) ;
      }

      /* Trailer block */
      //Block header
      for (k=0; k<8;k++)
        addByte (0xA8);
      addByte (0x00) ;
      // Block length
      addByte (0x04) ;
      // Block type
      addByte (0x45) ;
      // Block data
      addByte (0xFF) ;
      // Block trailer
      addByte (0xB7) ;
      maxwk = cur ;

      /* Output raw byte stream, for debugging purposes */
      cur = 0 ;
      while ( (cur <= maxwk) & false )
      {
        System.err.print(toHex2(wk[cur])+" ");
        cur++ ;
      }

      // Prepare bit stream
      cur = 0 ;
      prevbit = 1 ;
      // For each byte
      for (pos = 0; pos < maxwk; pos++)
      {  
        curbyte = wk[pos];

        // First bits
        curbit = curbyte % 2 ;
        curbyte = curbyte / 2 ;
        // Obscure, but correctly generates the first two bits
        if (curbit == prevbit)
          res[cur] = 1 ;
        else
          res[cur]=0;
        cur++ ;
        res[cur]=1;
        cur++;
        prevbit = curbit;
        // Remaining bits
        for (j=0; j<(word-2);j++)
        {
          curbit = curbyte % 2 ;
          curbyte = curbyte / 2 ;
          if (curbit == prevbit)
            res[cur] = 0 ;
          else
            res [cur] = 1 ;
          cur++ ;
          prevbit = curbit ;
        }

      }
      maxcount = cur ;

      /* Output bit stream, for debugging pruposes */
      System.err.println();
      for (i=0;((i< maxcount) & false);i++)
      {
        switch (res[i])
        {
          case -1 :
            System.err.print("-");
            break ;
          case 0 :
            System.err.print("0");
            break ;
          case 1 :
            System.err.print("+");
            break ;
        }
        if (((i+1) % word) == 0)
          System.err.print(" ");
      }

      // Prepare wav file
      wav = new FileOutputStream (wavFile);
      // Write header
      // Output RIFF header
      for (i=0; i<riff.length;i++)
        wav.write (riff[i]);
      // Output data size
      samplecount = (maxcount * 147) / 2 + 1 ; // 147 = 73.5 * 2, avoiding floating point computations
      lcounter = samplecount + wavHead.length + 4  ;
      wav.write ((int) (lcounter % 0x100));
      lcounter = lcounter / 0x100 ;
      wav.write ((int) (lcounter % 0x100));
      lcounter = lcounter / 0x100 ;
      wav.write ((int) (lcounter % 0x100));
      lcounter = lcounter / 0x100 ;
      wav.write ((int) (lcounter % 0x100));
      // Output rest of header
      for (i=0; i<wavHead.length;i++)
        wav.write (wavHead[i]);
      // Output chunk data size
      lcounter = samplecount;
      wav.write ((int) (lcounter % 0x100));
      lcounter = lcounter / 0x100 ;
      wav.write ((int) (lcounter % 0x100));
      lcounter = lcounter / 0x100 ;
      wav.write ((int) (lcounter % 0x100));
      lcounter = lcounter / 0x100 ;
      wav.write ((int) (lcounter % 0x100));
      // Output bit stream
      wav.write (0x00); // UIEC files have this initial 0x00 sample
      smpcount = 0 ;
      flip = 0 ;
      int prog = 0;
      int newProg = 0;
      int incr = maxcount / 100 + 1;
      for (j=0;j<=maxcount;j++)
      {
        if ( progressUpdater != null )
        {
          newProg = j / incr;
          if ( newProg > prog )
          {
            progressUpdater.updateProgress( newProg );
            prog = newProg;
          }
        }
        
        // Output 8 oscillations
        if (res[j] == 1)
          flip = 1 - flip ;
        do
        {
          sine = java.lang.Math.sin((double) (smpcount) * sampling );
          if (flip != 0)
            sine =  - sine ;
          sine = ( sine + 1.0) * 126.0 + 2.0 ; // Calibrate to 0x02 - 0xFD range
          wav.write ((int) sine);
          smpcount++ ;
        }
        while ( ((10 * smpcount) % 735) >= 9 ) ; // 735 = 10 * 73.5, to avoid floating point calculations

      }
      // Pad file, if necessary, windows media player chokes when the announced size is not correct
      for (lcounter= smpcount + 1; lcounter < samplecount; lcounter++)
        wav.write (0x80);
      // Close file
      wav.close();
    }
    catch(FileNotFoundException e)
    {
      System.err.println("Could not open .wav file");
    }
    catch(IOException e)
    {
      System.err.println("Could not close .wav file");
    }
  }


 // class Main

/*--------------------------------------------------------------------*/
  // Misc utilities
/*--------------------------------------------------------------------*/

  public String toHex4 (int value)
  // Returns an hexadecimal string representation with 4 digits and leading 0s
  {
    return ( Integer.toHexString( 0x10000 | value).substring(1).toUpperCase() ) ;
  }

  public String toHex3 (int value)
  // Returns an hexadecimal string representation with 3 digits and leading 0s
  {
    return ( Integer.toHexString( 0x1000 | value).substring(1).toUpperCase() ) ;
  }

  public String toHex32(int value)
  // Returns an hexadecimal string representation with 2 digits and leading 0s
  {
    return ( Integer.toHexString( 0x100 | value).substring(1).toUpperCase() ) ;
  }

  public void skipSep ()
  {
     while (wk[cur] == 0xa8)
      cur++ ;
  }

  public int addByte (int value)
  // Add a byte to the byte stream
  {
    wk[cur] = value;
    cur++ ;
    return ( cur) ;
  }

  public int addWord (int value)
  // Add a word to the byte stream
  {
    wk[cur] = value / 0x100 ;
    wk[cur+1] = value % 0x100 ;
    cur = cur+2 ;
    return ( cur) ;
  }

  public int sum (int start, int end)
  // Computes the sum of a sub-array - Used to compute check sums
  {
    int i, j ;
    i = 0 ;
    for (j = start; j < end; j++)
      i = i + wk[j];
    return (i) ;
  }

  public void setProgressUpdater( ProgressUpdater progressUpdater )
  {
    this.progressUpdater = progressUpdater;
  }

  public static String toHex2 (int value)
  // Returns an hexadecimal string representation with 2 digits and leading 0s
  {
    return ( Integer.toHexString( 0x100 | value).substring(1).toUpperCase() ) ;
  }


} // class IRToWav
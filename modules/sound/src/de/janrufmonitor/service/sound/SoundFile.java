package de.janrufmonitor.service.sound;

import java.io.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.sound.sampled.*;

import javazoom.jl.player.Player;

import de.janrufmonitor.framework.IJAMConst;

public class SoundFile {
    
    String filename;
    AudioFormat audioFormat;
    Logger m_logger;
	private AudioInputStream audioInputStream;
    
    public SoundFile(String filename) {
    	this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
    	this.filename = filename;
    }
    
    public void setSoundFile(String filename) {
        this.filename = filename;
    }
    
    public void play(){
        if (this.filename!=null){
            try {
            	InputStream is = new FileInputStream(this.filename);
            	
            	if (this.filename.toLowerCase().endsWith(".mp3")) {
                	Player p = new Player(is);
                	this.m_logger.info("playing audio file "+this.filename);
                	p.play();
            	} else {
                    byte tempBuffer[] = new byte[10000];
                    int cnt;

                    AudioFormat audioFormat =  getAudioFormat();
                    audioInputStream = new AudioInputStream(is,
					                     audioFormat,
					                     is.available()/audioFormat.
					                     getFrameSize());
                    DataLine.Info dataLineInfo =
                        new DataLine.Info(SourceDataLine.class,
                                          audioFormat);
                    
                    SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                    
                    sourceDataLine.open(audioFormat);
                    sourceDataLine.start();
                    
                    //Keep looping until the input
                    // read method returns -1 for
                    // empty stream.
                    while((cnt = audioInputStream.
                    read(tempBuffer, 0,
                    tempBuffer.length)) != -1){
                        if(cnt > 0){
                            
                            //Write data to the internal
                            // buffer of the data line
                            // where it will be delivered
                            // to the speaker.
                            sourceDataLine.write(
                            tempBuffer, 0, cnt);
                        }
                    }
                    
                    //Block and wait for internal
                    // buffer of the data line to
                    // empty.
                    sourceDataLine.drain();
                    sourceDataLine.close();
            	}
            }catch (Exception e) {
            	this.m_logger.warning(e.toString()+" : "+e.getMessage());
            }
        }
    }
    
    public void setAudioFormat(float sampleRate, int sampleSize, int channels, boolean signed, boolean endian) {
        this.audioFormat = new AudioFormat(sampleRate, sampleSize, channels, signed, endian);
    }
    
    private AudioFormat getAudioFormat(){
        if (this.audioFormat==null) {
            float sampleRate = 22050.0F;
            //8000,11025,16000,22050,44100
            int sampleSizeInBits = 8;
            //8,16
            int channels = 1;
            //1,2
            boolean signed = false;
            //true,false
            boolean bigEndian = false;
            //true,false
            return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        }
        return this.audioFormat;
    }
    
}
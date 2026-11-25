import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

public class Sound
{
    public static void play(String file, int volume)
    {
        if (volume <= 0)
            return;
        
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File(file));
            final Clip clip = AudioSystem.getClip();
            clip.open(stream);
            
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float decibels = -30 + volume * 3;
                if (decibels > gain.getMaximum())
                    decibels = gain.getMaximum();
                gain.setValue(decibels);
            }
            
            clip.addLineListener(new LineListener() {
                public void update(LineEvent event)
                {
                    if (event.getType() == LineEvent.Type.STOP)
                        clip.close();
                }
            });
            clip.start();
        } catch (Exception e) {
            System.out.println("Couldn't play " + file);
        }
    }
}

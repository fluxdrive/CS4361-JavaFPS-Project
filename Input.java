
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/*
 * Does input.
 */
public class Input implements KeyListener
{
    public boolean[] keyboard = new boolean[KeyEvent.KEY_LAST + 1];
    
    @Override
    public void keyPressed(KeyEvent e)
    {
        int keyCode = e.getKeyCode();
        if (keyCode > 0 && keyCode < keyboard.length) {
            keyboard[keyCode] = true;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e)
    {
        /* wtf why??
        for (int i = 0; i < key.length; i++) {
            key[i] = false;
        }
        */
        int keyCode = e.getKeyCode();
        if (keyCode > 0 && keyCode < keyboard.length ) {
            keyboard[keyCode] = false;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e)
    {
        
    }
}
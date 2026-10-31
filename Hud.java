import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Hud
{
    private Font font = new Font("Monospaced", Font.BOLD, 12);
    
    private String[] heart = {
        ".XX...XX.",
        "XXXX.XXXX",
        "XXXXXXXXX",
        "XXXXXXXXX",
        ".XXXXXXX.",
        "..XXXXX..",
        "...XXX...",
        "....X...."
    };
    
    public void draw(BufferedImage screen, Player player)
    {
        Graphics2D g = screen.createGraphics();
        g.setFont(font);
        
        int width = screen.getWidth();
        int height = screen.getHeight();
        int bar_top = 0;
        
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, bar_top, width, 22);
        
        drawHearts(g, player, 6, bar_top + 7);
        
        g.setColor(Color.WHITE);
        g.drawString("AMMO " + player.ammo, 180, bar_top + 15);
        g.drawString("KEYS " + player.keys, 255, bar_top + 15);
        
        g.dispose();
    }
    
    private void drawHearts(Graphics2D g, Player player, int start_x, int start_y)
    {
        int hearts = player.max_health / 10;
        for (int i = 0; i < hearts; i++) {
            int heart_health = player.health - i * 10;
            int fill = 0;
            if (heart_health >= 10)
                fill = 2;
            else if (heart_health > 0)
                fill = 1;
            drawHeart(g, start_x + i * 10, start_y, fill);
        }
    }
    
    private void drawHeart(Graphics2D g, int x, int y, int fill)
    {
        for (int row = 0; row < heart.length; row++) {
            for (int col = 0; col < heart[row].length(); col++) {
                if (heart[row].charAt(col) != 'X')
                    continue;
                
                boolean full = fill == 2 || (fill == 1 && col < 5);
                if (full)
                    g.setColor(Color.RED);
                else
                    g.setColor(Color.DARK_GRAY);
                g.fillRect(x + col, y + row, 1, 1);
            }
        }
    }
    
    public void drawPause(BufferedImage screen)
    {
        Graphics2D g = screen.createGraphics();
        g.setFont(font);
        
        int width = screen.getWidth();
        int height = screen.getHeight();
        
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, width, height);
        
        g.setColor(Color.WHITE);
        drawCentered(g, "PAUSED", width, height / 2 - 10);
        drawCentered(g, "Press ESC to return to game", width, height / 2 + 12);
        
        g.dispose();
    }
    
    private void drawCentered(Graphics2D g, String text, int width, int y)
    {
        FontMetrics metrics = g.getFontMetrics();
        int x = (width - metrics.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }
}

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Hud
{
    private double scale = 0;
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

    private int s(int n)
    {
        return (int)(n * scale);
    }

    private void setScale(int width)
    {
        if (scale == width / 320.0)
            return;
        scale = width / 320.0;
        font = new Font("Monospaced", Font.BOLD, s(12));
    }

    public void draw(BufferedImage screen, Player player)
    {
        int width = screen.getWidth();
        setScale(width);

        Graphics2D g = screen.createGraphics();
        g.setFont(font);

        int bar_top = 0;

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, bar_top, width, s(22));

        drawHearts(g, player, s(6), bar_top + s(7));

        g.setColor(Color.WHITE);
        g.drawString("AMMO " + player.ammo, s(180), bar_top + s(15));
        g.drawString("KEYS " + player.keys, s(255), bar_top + s(15));

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
            drawHeart(g, start_x + i * s(10), start_y, fill);
        }
    }

    private void drawHeart(Graphics2D g, int x, int y, int fill)
    {
        int pixel = s(1);

        for (int row = 0; row < heart.length; row++) {
            for (int col = 0; col < heart[row].length(); col++) {
                if (heart[row].charAt(col) != 'X')
                    continue;

                boolean whole_heart = fill == 2;
                boolean left_half = fill == 1 && col < 5;
                if (whole_heart || left_half)
                    g.setColor(Color.RED);
                else
                    g.setColor(Color.DARK_GRAY);
                g.fillRect(x + col * pixel, y + row * pixel, pixel, pixel);
            }
        }
    }

    public void drawPause(BufferedImage screen)
    {
        int width = screen.getWidth();
        int height = screen.getHeight();
        setScale(width);

        Graphics2D g = screen.createGraphics();
        g.setFont(font);

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, width, height);

        g.setColor(Color.WHITE);
        drawCentered(g, "PAUSED", width, height / 2 - s(10));
        drawCentered(g, "Press ESC to return to game", width, height / 2 + s(12));

        g.dispose();
    }

    public void drawMessage(BufferedImage screen, String text)
    {
        int width = screen.getWidth();
        setScale(width);

        Graphics2D g = screen.createGraphics();
        g.setFont(font);

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, s(28), width, s(18));

        g.setColor(Color.WHITE);
        drawCentered(g, text, width, s(41));

        g.dispose();
    }

    public void drawEnd(BufferedImage screen, String title, String hint)
    {
        int width = screen.getWidth();
        int height = screen.getHeight();
        setScale(width);

        Graphics2D g = screen.createGraphics();
        g.setFont(font);

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, width, height);

        g.setColor(Color.WHITE);
        drawCentered(g, title, width, height / 2 - s(10));
        drawCentered(g, hint, width, height / 2 + s(12));

        g.dispose();
    }

    private void drawCentered(Graphics2D g, String text, int width, int y)
    {
        FontMetrics metrics = g.getFontMetrics();
        int x = (width - metrics.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }
}

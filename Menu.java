import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class Menu
{
    public static final int NONE = 0;
    public static final int START = 1;
    public static final int QUIT = 2;
    
    private static final int MAIN = 0;
    private static final int SETTINGS = 1;
    private static final int HELP = 2;
    
    public int volume = 5;
    
    private String title = "JAVA FPS";
    private String[] main_options = {"START NEW GAME", "HELP", "SETTINGS", "QUIT"};
    private String[] settings_options = {"VOLUME", "BACK"};
    private String[] help_keys = {"W", "S", "A", "D", "LEFT / RIGHT", "ESC"};
    private String[] help_actions = {"Move forward", "Move backward", "Move left", "Move right", "Look around", "Pause"};
    
    private int[] tree_shift = {3, 7, 1, 6, 4, 0, 5, 2, 7, 1, 5, 3};
    private int[] tree_size = {70, 90, 80, 100, 75, 95, 85, 70, 100, 80, 90, 75};
    
    private int screen = MAIN;
    private int selected = 0;
    private int settings_selected = 0;
    private boolean[] was_down = new boolean[KeyEvent.KEY_LAST + 1];
    
    private double scale = 0;
    private Font title_font;
    private Font option_font;
    private Font small_font;
    
    private int s(int number)
    {
        return (int)(number * scale);
    }
    
    private boolean pressed(Input input, int key)
    {
        return input.keyboard[key] && was_down[key] == false;
    }
    
    public int update(Input input)
    {
        int result = NONE;
        
        if (screen == MAIN)
            result = updateMain(input);
        else if (screen == SETTINGS)
            updateSettings(input);
        else if (screen == HELP)
            updateHelp(input);
        
        System.arraycopy(input.keyboard, 0, was_down, 0, was_down.length);
        return result;
    }
    
    private int updateMain(Input input)
    {
        if (pressed(input, KeyEvent.VK_UP) || pressed(input, KeyEvent.VK_W))
            selected -= 1;
        if (pressed(input, KeyEvent.VK_DOWN) || pressed(input, KeyEvent.VK_S))
            selected += 1;
        
        if (selected < 0)
            selected = main_options.length - 1;
        if (selected >= main_options.length)
            selected = 0;
        
        if (pressed(input, KeyEvent.VK_ENTER)) {
            if (selected == 0)
                return START;
            else if (selected == 1)
                screen = HELP;
            else if (selected == 2)
                screen = SETTINGS;
            else if (selected == 3)
                return QUIT;
        }
        
        return NONE;
    }
    
    private void updateSettings(Input input)
    {
        if (pressed(input, KeyEvent.VK_UP) || pressed(input, KeyEvent.VK_W))
            settings_selected -= 1;
        if (pressed(input, KeyEvent.VK_DOWN) || pressed(input, KeyEvent.VK_S))
            settings_selected += 1;
        
        if (settings_selected < 0)
            settings_selected = settings_options.length - 1;
        if (settings_selected >= settings_options.length)
            settings_selected = 0;
        
        if (settings_selected == 0) {
            if (pressed(input, KeyEvent.VK_LEFT) || pressed(input, KeyEvent.VK_A))
                volume -= 1;
            if (pressed(input, KeyEvent.VK_RIGHT) || pressed(input, KeyEvent.VK_D))
                volume += 1;
            
            if (volume < 0)
                volume = 0;
            if (volume > 10)
                volume = 10;
        }
        
        if (pressed(input, KeyEvent.VK_ENTER) && settings_selected == 1)
            screen = MAIN;
        if (pressed(input, KeyEvent.VK_ESCAPE))
            screen = MAIN;
    }
    
    private void updateHelp(Input input)
    {
        if (pressed(input, KeyEvent.VK_ENTER) || pressed(input, KeyEvent.VK_ESCAPE))
            screen = MAIN;
    }
    
    public void draw(BufferedImage image, double time)
    {
        Graphics2D g = image.createGraphics();
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        if (scale != width / 320.0) {
            scale = width / 320.0;
            title_font = new Font("Monospaced", Font.BOLD, s(32));
            option_font = new Font("Monospaced", Font.BOLD, s(12));
            small_font = new Font("Monospaced", Font.BOLD, s(10));
        }
        
        drawBackground(g, width, height, time);
        
        g.setFont(title_font);
        g.setColor(Color.BLACK);
        String heading = title;
        if (screen == HELP)
            heading = "CONTROLS";
        drawCentered(g, heading, width, height / 5 + s(2));
        g.setColor(Color.RED);
        drawCentered(g, heading, width, height / 5);
        
        g.setFont(option_font);
        if (screen == MAIN)
            drawOptions(g, width, height, main_options, selected);
        else if (screen == SETTINGS)
            drawOptions(g, width, height, settings_options, settings_selected);
        else if (screen == HELP)
            drawHelp(g, width, height);
        
        int footer_y = height * 2 / 5 + s(26) * 3 + s(36);
        if (screen == SETTINGS)
            footer_y = height * 2 / 5 + s(26) + s(36);
        if (screen == HELP)
            footer_y = height * 2 / 5 + s(18) * 5 + s(36);
        
        g.setFont(small_font);
        g.setColor(Color.LIGHT_GRAY);
        if (screen == MAIN)
            drawCentered(g, "UP/DOWN: choose   ENTER: select", width, footer_y);
        else if (screen == SETTINGS)
            drawCentered(g, "UP/DOWN: choose   LEFT/RIGHT: volume   ESC: back", width, footer_y);
        else if (screen == HELP)
            drawCentered(g, "ENTER or ESC: back", width, footer_y);
        
        g.dispose();
    }
    
    private void drawOptions(Graphics2D g, int width, int height, String[] options, int chosen)
    {
        drawPanel(g, width, height, options.length, s(26));
        
        for (int i = 0; i < options.length; i++) {
            String text = options[i];
            
            if (screen == SETTINGS && i == 0)
                text = "VOLUME [" + volumeBar() + "]";
            
            if (i == chosen) {
                g.setColor(Color.YELLOW);
                text = "> " + text + " <";
            } else {
                g.setColor(Color.WHITE);
            }
            
            drawCentered(g, text, width, height * 2 / 5 + i * s(26));
        }
    }
    
    private void drawHelp(Graphics2D g, int width, int height)
    {
        drawPanel(g, width, height, help_keys.length, s(18));
        
        FontMetrics metrics = g.getFontMetrics();
        for (int i = 0; i < help_keys.length; i++) {
            int y = height * 2 / 5 + i * s(18);
            
            g.setColor(Color.YELLOW);
            g.drawString(help_keys[i], width / 2 - s(8) - metrics.stringWidth(help_keys[i]), y);
            
            g.setColor(Color.WHITE);
            g.drawString(help_actions[i], width / 2 + s(8), y);
        }
    }
    
    private void drawPanel(Graphics2D g, int width, int height, int rows, int step)
    {
        int top = height * 2 / 5 - s(24);
        int bottom = height * 2 / 5 + (rows - 1) * step + s(20);
        
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(s(40), top, width - s(80), bottom - top);
    }
    
    private void drawBackground(Graphics2D g, int width, int height, double time)
    {
        for (int y = 0; y < height; y++) {
            double amount = (double)y / height;
            int red, green, blue;
            if (amount < 0.6) {
                double part = amount / 0.6;
                red = (int)(15 + 105 * part);
                green = (int)(5 + 30 * part);
                blue = (int)(50 + 70 * part);
            } else {
                double part = (amount - 0.6) / 0.4;
                red = (int)(120 + 120 * part);
                green = (int)(35 + 75 * part);
                blue = (int)(120 - 50 * part);
            }
            g.setColor(new Color(red, green, blue));
            g.fillRect(0, y, width, 1);
        }
        
        for (int i = 0; i < 60; i++) {
            int brightness = 170 + (int)(80 * Math.sin(time / 400.0 + i));
            g.setColor(new Color(brightness, brightness, brightness));
            g.fillRect((i * 97) % width, (i * 53) % (height / 2), s(1), s(1));
        }
        
        int moon_x = width * 88 / 100;
        int moon_y = height * 30 / 100;
        int radius = height / 10;
        
        g.setPaint(new RadialGradientPaint(new Point2D.Float(moon_x, moon_y), radius * 3,
            new float[] {0f, 0.35f, 1f},
            new Color[] {new Color(255, 170, 90, 190), new Color(220, 70, 150, 80), new Color(0, 0, 0, 0)}));
        g.fillRect(moon_x - radius * 3, moon_y - radius * 3, radius * 6, radius * 6);
        
        g.setPaint(new RadialGradientPaint(new Point2D.Float(moon_x, moon_y), radius,
            new float[] {0f, 1f},
            new Color[] {new Color(255, 245, 190), new Color(255, 160, 70)}));
        g.fillOval(moon_x - radius, moon_y - radius, radius * 2, radius * 2);
        
        g.setColor(new Color(230, 120, 50, 110));
        g.fillOval(moon_x - radius / 2, moon_y - radius / 3, radius / 3, radius / 3);
        g.fillOval(moon_x + radius / 6, moon_y + radius / 6, radius / 2, radius / 2);
        g.fillOval(moon_x + radius / 4, moon_y - radius / 2, radius / 4, radius / 4);
        
        g.setColor(new Color(40, 10, 45));
        for (int i = 0; i * s(17) < width + s(17); i++)
            drawPine(g, i * s(17) + s(tree_shift[i % tree_shift.length]), height * 8 / 10, tree_size[i % tree_size.length] * height / 320);
        
        g.setColor(new Color(255, 170, 200, 50));
        g.fillRect(0, height * 62 / 100, width, height * 14 / 100);
        
        g.setColor(new Color(8, 3, 12));
        for (int i = 0; i * s(27) < width + s(27); i++)
            drawPine(g, i * s(27) + s(tree_shift[(i + 3) % tree_shift.length]), height + s(6), tree_size[(i + 5) % tree_size.length] * height / 220 + height / 8);
    }
    
    private void drawPine(Graphics2D g, int x, int base, int size)
    {
        for (int i = 0; i < 3; i++) {
            int top = base - size + i * size / 4;
            int bottom = base - size / 3 + i * size / 6;
            int half = size / 6 + i * size / 14;
            g.fillPolygon(new int[] {x, x - half, x + half}, new int[] {top, bottom, bottom}, 3);
        }
        g.fillRect(x - s(2), base - size / 5, s(4), size / 5);
    }
    
    private String volumeBar()
    {
        String bar = "";
        for (int i = 0; i < 10; i++) {
            if (i < volume)
                bar += "#";
            else
                bar += "-";
        }
        return bar;
    }
    
    private void drawCentered(Graphics2D g, String text, int width, int y)
    {
        FontMetrics metrics = g.getFontMetrics();
        int x = (width - metrics.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }
}

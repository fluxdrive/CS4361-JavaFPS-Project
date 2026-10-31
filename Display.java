import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Display extends Canvas implements Runnable
{
    // Guaranteed to fit within the ancient 1360x768 screens anyone may still have.
    public static final int WIDTH = 960;
    public static final int HEIGHT = 960;
    
    private Thread thread;
    private boolean running = false;
    private Renderer renderer;
    private Input input;
    private Random random;
    
    // Levels have to be loaded upside down. Sorry.
    private int[][] map = {
        {1,1,1,1,1,1,1},
        {1,0,0,0,0,0,1},
        {1,0,0,0,1,0,1},
        {1,0,0,0,0,0,1},
        {1,0,0,0,0,0,1},
        {1,0,0,0,0,0,1},
        {1,1,1,1,1,1,1}
    };
    
    // Levels have to be loaded upside down. Sorry.
    private int[][] map2 = {
        {1,1,1,1,1,1,1,1,1,3,1,1,1},
        {1,0,0,0,0,0,1,1,0,0,0,1,1},
        {1,0,2,0,2,0,1,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,3},
        {1,0,2,0,2,0,1,0,0,0,0,0,1},
        {1,0,0,0,0,0,1,1,0,0,0,1,1},
        {1,1,1,0,1,1,1,1,1,0,1,1,1},
        {1,1,1,0,0,0,0,0,0,0,1,1,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1}
    };
    
    private Player player = new Player(1.5, 3.5, 0.0);
    private Hud hud = new Hud();
    private GameState state = GameState.PLAYING;
    private boolean escape_was_down = false;
    
    private double imp_x = 9.5;
    private double imp_y = 3.5;
    public BufferedImage[] imp_textures;
    
    private double dopefish_x = 10.5;
    private double dopefish_y = 4.5;
    public BufferedImage dopefish_texture;
    
    private double time_current;
    private double time_last;
    private double time_delta;
    
    public Display() {
        Dimension size = new Dimension(WIDTH, HEIGHT);
        
        // BufferStrategy flips out if dimensions change without its knowledge.
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        
        renderer = new Renderer();
        renderer.walls = map2;
        
        try {
            renderer.wall_textures = new BufferedImage[] {ImageIO.read(new File("wolf3d_wall.png")), ImageIO.read(new File("mandril.png")), ImageIO.read(new File("framed_mona_lisa_2.png"))};
            renderer.floor_texture = ImageIO.read(new File("sand.png"));
            renderer.ceiling_texture = ImageIO.read(new File("sky.png"));
            imp_textures = new BufferedImage[] {
                ImageIO.read(new File("imp0.png")),
                ImageIO.read(new File("imp1.png")),
                ImageIO.read(new File("imp2.png")),
                ImageIO.read(new File("imp3.png")),
                ImageIO.read(new File("imp4.png")),
                ImageIO.read(new File("imp5.png")),
                ImageIO.read(new File("imp6.png")),
                ImageIO.read(new File("imp7.png"))
            };
            dopefish_texture = ImageIO.read(new File("dopefish.png"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        input = new Input();
        addKeyListener(input);
        random = new Random();
    }
    
    private void start()
    {
        if (running) {
            return;
        }
        
        running = true;
        thread = new Thread(this);
        thread.start();
        
        System.out.println("Working!");
    }
    
    private void stop()
    {
        if (!running) {
            return;
        }
        
        running = false;
        
        try { 
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    // If race conditions happen, make this method synchronized.
    public void run()
    {
        while (running) {
            tick();
            render();
        }
    }
    
    private void tick()
    {
        time_last = time_current;
        time_current = System.currentTimeMillis();
        time_delta = time_current - time_last;
        
        boolean escape_is_down = input.keyboard[KeyEvent.VK_ESCAPE];
        if (escape_is_down && escape_was_down == false) {
            if (state == GameState.PLAYING)
                state = GameState.PAUSED;
            else if (state == GameState.PAUSED)
                state = GameState.PLAYING;
        }
        escape_was_down = escape_is_down;
        
        if (state == GameState.PLAYING) {
            player.update(time_delta, input, renderer.walls);
            
            if (player.health <= 0)
                state = GameState.LOST;
        }
    }
    
    private void render()
    {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(3);
            return;
        }
        
        renderer.clean();
        
        renderer.setCamera(player.x, player.y, player.angle);
        
        renderer.drawFlats();
        renderer.drawWalls();
        renderer.drawThing(imp_x, imp_y, 1 * Math.PI, imp_textures);
        renderer.drawSprite(dopefish_x, dopefish_y, dopefish_texture);
        
        hud.draw(renderer.device, player);
        if (state == GameState.PAUSED)
            hud.drawPause(renderer.device);
        
        Graphics g = bs.getDrawGraphics();
        g.drawImage(renderer.device, 0, 0, WIDTH, HEIGHT, null); // Does this cause a race condition? I hope not... Supposedly this function *can* be asynchronous.
        g.dispose();
        bs.show();
    }
    
    public static void main(String[] args)
    {
        Display game = new Display();
        JFrame frame = new JFrame();
        frame.add(game);
        frame.pack();
        frame.setTitle("Basic Raycaster");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        
        System.out.println("Running!");
        System.out.println(System.getProperty("user.dir"));
        
        game.start();
    }
}
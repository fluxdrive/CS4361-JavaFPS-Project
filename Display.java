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
    
    // Level 1, abandoned castle. 1 stone, 2 mossy stone, 3 torch, 4 banner, 5 window, 6 door. Door (6) is at row 5, column 22.
    private int[][] level1 = {
        {1,1,1,1,1,1,1,1,1,3,1,5,1,1,1,1,1,1,4,4,4,4,4,4,4,4},
        {1,0,0,0,0,0,1,1,1,0,0,0,0,0,1,1,1,1,4,0,0,0,0,0,0,4},
        {1,0,0,0,0,0,3,1,1,0,0,0,0,0,1,1,1,1,4,0,0,0,0,0,0,4},
        {3,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,4,0,0,0,0,0,0,4},
        {1,0,0,0,0,0,1,1,1,0,0,1,0,0,1,1,1,1,4,0,0,0,0,0,0,4},
        {1,0,0,0,0,0,3,1,1,0,0,0,0,0,5,1,1,1,4,4,4,4,6,4,4,4},
        {1,1,1,1,1,1,1,1,1,3,1,0,0,1,1,1,1,1,0,0,0,0,0,0,0,1},
        {2,2,2,2,2,2,2,2,2,1,1,0,0,5,1,1,1,1,0,0,0,0,1,0,0,1},
        {2,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,1},
        {2,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,1,1,0,0,1,0,0,5,0,1},
        {2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,1,0,1},
        {2,0,0,2,0,2,0,0,2,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,1},
        {2,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,5,1,0,0,0,1,0,0,0,1},
        {2,0,0,0,0,0,0,0,2,1,5,1,3,1,1,1,1,1,0,0,0,0,0,0,0,1},
        {2,0,0,0,0,0,0,0,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,3,1},
        {2,2,2,2,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };
    
    private Player player = new Player(2.5, 2.5, 0.0);
    private Hud hud = new Hud();
    private Menu menu = new Menu();
    private GameState state = GameState.MENU;
    private boolean escape_was_down = false;
    private boolean enter_was_down = false;
    private int current_level = 1;
    
    private Item[] items;
    private BufferedImage[] item_textures;
    private BufferedImage exit_texture;
    private double exit_x = 23.5;
    private double exit_y = 2.5;
    private int door_row = 5;
    private int door_col = 22;
    private String message = "";
    private double message_time = 0;
    
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
        renderer.walls = level1;
        
        try {
            renderer.wall_textures = new BufferedImage[] {ImageIO.read(new File("castle_wall.png")), ImageIO.read(new File("castle_wall_mossy.png")), ImageIO.read(new File("castle_torch.png")), ImageIO.read(new File("castle_banner.png")), ImageIO.read(new File("castle_window.png")), ImageIO.read(new File("castle_door.png"))};
            renderer.floor_texture = ImageIO.read(new File("castle_floor.png"));
            renderer.ceiling_texture = ImageIO.read(new File("castle_ceiling.png"));
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
            item_textures = new BufferedImage[] {
                ImageIO.read(new File("key.png")),
                ImageIO.read(new File("ammo.png")),
                ImageIO.read(new File("health.png")),
                ImageIO.read(new File("gun.png"))
            };
            exit_texture = ImageIO.read(new File("castle_exit.png"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        input = new Input();
        addKeyListener(input);
        random = new Random();
        items = makeItems();
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
            else if (state == GameState.WON || state == GameState.LOST) {
                menu.showMain(input);
                state = GameState.MENU;
            }
        }
        escape_was_down = escape_is_down;
        
        if (state == GameState.MENU) {
            int choice = menu.update(input);
            if (choice == Menu.START)
                startLevel(menu.level);
            if (choice == Menu.QUIT)
                System.exit(0);
        }
        
        if (state == GameState.PLAYING) {
            player.update(time_delta, input, renderer.walls);
            
            checkItems();
            checkDoor();
            checkExit();
            
            if (player.health <= 0)
                state = GameState.LOST;
        }
        
        boolean enter_is_down = input.keyboard[KeyEvent.VK_ENTER];
        if (enter_is_down && enter_was_down == false) {
            if (state == GameState.WON) {
                menu.showLevels(input);
                state = GameState.MENU;
            } else if (state == GameState.LOST) {
                newGame();
            }
        }
        enter_was_down = enter_is_down;
    }
    
    private void startLevel(int number)
    {
        if (number == 1) {
            current_level = 1;
            newGame();
        } else {
            menu.note = "LEVEL " + number + " IS NOT BUILT YET";
        }
    }
    
    private void newGame()
    {
        player = new Player(2.5, 2.5, 0.0);
        renderer.walls[door_row][door_col] = 6;
        items = makeItems();
        state = GameState.PLAYING;
    }
    
    private Item[] makeItems()
    {
        return new Item[] {
            new Item(2.5, 13.5, Item.KEY),
            new Item(11.5, 2.5, Item.AMMO),
            new Item(12.5, 10.5, Item.GUN),
            new Item(24.5, 12.5, Item.HEALTH)
        };
    }
    
    private void showMessage(String text)
    {
        message = text;
        message_time = time_current;
    }
    
    private void checkItems()
    {
        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            if (item.taken)
                continue;
            
            double dx = item.x - player.x;
            double dy = item.y - player.y;
            if (dx * dx + dy * dy > 0.5 * 0.5)
                continue;
            
            item.taken = true;
            Sound.play("pickup.wav", menu.volume);
            if (item.type == Item.KEY) {
                player.keys += 1;
                showMessage("GOT THE KEY");
            }
            if (item.type == Item.AMMO) {
                player.ammo += 10;
                showMessage("GOT 10 AMMO");
            }
            if (item.type == Item.HEALTH) {
                player.health = Math.min(player.health + 30, player.max_health);
                showMessage("GOT SOME HEALTH");
            }
            if (item.type == Item.GUN) {
                player.has_gun = true;
                showMessage("GOT THE GUN");
            }
        }
    }
    
    private void checkDoor()
    {
        if (renderer.walls[door_row][door_col] == 0)
            return;
        
        double dx = Math.abs(door_col + 0.5 - player.x);
        double dy = Math.abs(door_row + 0.5 - player.y);
        
        boolean lined_up_x = dx < 0.5;
        boolean lined_up_y = dy < 0.5;
        boolean touching = false;
        if (lined_up_x && dy < 0.8)
            touching = true;
        if (lined_up_y && dx < 0.8)
            touching = true;
        if (touching == false)
            return;
        
        if (player.keys > 0) {
            player.keys -= 1;
            renderer.walls[door_row][door_col] = 0;
            showMessage("DOOR UNLOCKED");
            Sound.play("door_open.wav", menu.volume);
        } else {
            showMessage("LOCKED. FIND THE KEY");
        }
    }
    
    private void checkExit()
    {
        double dx = exit_x - player.x;
        double dy = exit_y - player.y;
        if (dx * dx + dy * dy < 0.6 * 0.6) {
            state = GameState.WON;
            if (current_level == menu.unlocked && menu.unlocked < 3)
                menu.unlocked += 1;
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
        renderer.drawSprite(exit_x, exit_y, exit_texture);
        for (int i = 0; i < items.length; i++) {
            if (items[i].taken == false)
                renderer.drawSprite(items[i].x, items[i].y, item_textures[items[i].type]);
        }
        
        if (state == GameState.MENU) {
            menu.draw(renderer.device, time_current);
        } else {
            hud.draw(renderer.device, player);
            if (state == GameState.PAUSED)
                hud.drawPause(renderer.device);
            if (state == GameState.WON)
                hud.drawEnd(renderer.device, "YOU GOT OUT!", "ENTER: level menu   ESC: main menu");
            if (state == GameState.LOST)
                hud.drawEnd(renderer.device, "YOU DIED", "ENTER: try again   ESC: main menu");
            if (time_current - message_time < 2000)
                hud.drawMessage(renderer.device, message);
        }
        
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
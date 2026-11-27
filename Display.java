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
    public static final int HEIGHT = 720;
    
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

    // Enemy positions from map, same x, y tile coordinates as the items (unused until enemies are added)
    private double[][] level1_enemies = {
        {5.5, 12.5}, {21.5, 11.5}, {19.5, 8.5}, {24.5, 8.5}
    };

    // Level 2, underground lab. 1 concrete panel, 2 pipe drop with valve, 3 caged lamp, 4 steel blast wall,
    // 5 vent grille, 6 door, 7 access panel, 8 hazard sign. Blast walls (4) surround the exit area.
    // Door (6) is at row 4, column 25 (into the exit area).
    private int[][] level2 = {
        {7,1,1,7,5,7,2,1,1,1,1,7,5,1,1,1,1,1,1,1,1,2,4,4,4,4,4,4,2},
        {8,0,0,0,0,0,1,7,1,0,0,0,0,0,1,1,2,2,1,7,2,4,0,0,0,0,0,0,4},
        {1,0,0,0,0,0,0,0,0,0,1,0,0,0,8,1,1,2,7,1,1,4,0,0,0,0,0,0,4},
        {7,0,0,0,0,0,1,3,1,0,0,0,0,0,1,1,2,1,2,1,7,4,0,0,0,0,0,0,4},
        {1,0,0,0,0,0,7,1,1,0,0,0,0,0,1,1,1,1,1,1,1,1,4,4,4,6,4,4,1},
        {1,5,1,0,7,3,1,1,2,7,2,0,2,1,1,7,1,2,3,1,1,2,4,4,4,0,4,4,7},
        {1,2,1,0,1,1,2,1,8,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,2,7,0,0,0,0,0,0,0,0,0,0,0,3,1,0,0,0,0,0,0,3},
        {7,0,0,0,0,0,0,0,0,0,0,0,0,0,1,2,1,0,0,0,2,1,0,0,0,0,0,0,7},
        {3,0,0,0,0,0,0,7,7,0,0,7,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,1},
        {1,0,0,0,0,0,0,3,1,0,0,2,0,0,0,0,0,0,1,0,1,7,0,0,7,0,1,0,1},
        {7,1,1,7,1,1,1,7,7,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,7},
        {7,0,0,0,0,0,0,0,1,7,1,7,2,7,1,0,2,3,1,2,1,1,0,0,0,0,0,0,3},
        {2,0,0,0,0,0,0,0,3,2,1,1,5,1,2,0,1,7,1,1,7,3,1,1,0,1,1,1,1},
        {3,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,1},
        {1,0,0,1,2,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {2,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,1,1,0,0,0,2,0,0,0,1},
        {1,0,0,0,0,0,0,0,1,7,0,0,0,0,0,0,0,0,0,7,1,0,0,0,0,0,0,0,5},
        {2,7,1,8,1,1,1,1,1,7,5,2,7,1,8,1,7,7,3,1,2,1,2,8,2,1,1,1,1}
    };
    
    // Enemy positions from map, same x, y tile coordinates as the items (unused until enemies are added)
    private double[][] level2_enemies = {
        {11.5, 16.5}, {23.5, 15.5}, {5.5, 14.5}, {23.5, 11.5}, {17.5, 10.5}, {3.5, 9.5}, {12.5, 8.5}, {26.5, 7.5}
    };

    // Level 3, frozen caverns. 1, 2 and 7 cave rock, 3, 8 and 9 ice crystal clusters, 4 icicles,
    // 5 ice pocket with a frozen skull, 6 door. Icicle walls (4) surround the boss room and the exit wing.
    // Doors (6) are at row 17, column 24 (into the boss room) and row 8, column 28 (into the exit room).
    private int[][] level3 = {
        {1,1,2,9,1,2,2,2,1,1,2,7,1,1,2,7,1,7,1,1,1,1,2,1,2,4,4,4,4,4,4,4,1},
        {7,0,0,0,0,0,2,1,1,0,0,0,0,0,0,3,2,1,2,1,1,1,2,1,4,0,0,0,0,0,0,0,4},
        {7,0,0,0,0,0,1,3,1,0,0,0,0,0,0,7,1,1,1,1,2,1,1,7,4,0,0,0,0,0,0,0,4},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,4,0,4,0,0,0,4,0,4},
        {8,0,0,0,0,0,7,1,2,0,0,0,0,1,0,1,7,1,1,5,1,2,0,7,4,0,0,0,0,0,0,0,4},
        {1,0,0,0,0,0,2,1,1,0,7,0,0,0,0,2,7,1,2,2,2,2,0,2,4,0,4,0,0,0,4,0,4},
        {7,2,7,0,2,9,1,2,7,0,0,0,0,0,0,7,2,2,7,1,7,0,0,0,4,0,0,0,0,0,0,0,4},
        {1,1,2,0,2,1,2,7,1,2,1,0,7,2,9,2,2,2,2,1,1,0,0,0,4,0,0,0,0,0,0,0,4},
        {2,7,1,0,1,1,1,7,1,8,1,0,1,1,1,2,7,1,1,1,2,0,0,0,4,4,4,4,6,4,4,4,7},
        {1,0,0,0,0,0,0,7,1,0,0,0,0,0,0,0,0,0,7,1,7,0,0,0,4,4,0,0,0,0,0,4,7},
        {5,0,0,0,0,1,0,1,1,0,7,0,0,0,0,7,0,0,5,1,7,0,0,0,4,4,0,0,0,0,0,4,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,2,7,7,0,0,0,4,4,4,4,0,4,4,4,1},
        {2,0,2,0,0,0,0,2,1,0,0,0,0,0,0,0,0,0,7,1,1,0,0,1,4,0,0,0,0,0,0,0,4},
        {1,0,0,0,0,0,0,1,1,0,0,0,1,1,2,0,0,0,7,1,2,0,0,0,4,0,0,0,0,0,0,0,4},
        {1,2,1,1,1,1,2,1,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,4,0,4,0,0,4},
        {2,1,1,7,2,3,1,7,1,0,1,7,0,0,0,7,7,0,7,5,1,0,0,0,4,0,0,0,0,0,0,0,4},
        {7,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1,1,1,1,0,0,4,0,0,0,0,0,0,0,4},
        {8,0,0,0,0,0,0,0,1,2,2,1,2,0,1,7,2,7,2,7,1,0,0,0,6,0,0,0,0,0,0,0,4},
        {2,0,0,0,0,0,0,0,2,5,1,0,0,0,0,0,0,0,1,2,1,0,0,0,4,0,0,0,0,0,0,0,4},
        {1,0,0,1,1,2,0,0,0,0,0,0,0,0,0,0,0,0,2,7,1,2,1,3,4,0,0,4,0,4,0,0,4},
        {1,0,0,0,0,0,0,0,1,7,1,0,0,0,0,0,0,0,1,7,7,2,2,7,4,0,0,0,0,0,0,0,4},
        {7,0,0,0,0,0,0,0,7,1,2,1,1,7,7,1,1,8,2,1,1,7,1,1,4,0,0,0,0,0,0,0,4},
        {1,2,7,9,1,1,1,1,1,1,1,7,1,7,7,7,7,7,1,7,1,7,1,1,2,4,4,4,4,4,4,4,7}
    };
    
    // Enemy positions from map, same x, y tile coordinates as the items (unused until enemies are added)
    private double[][] level3_enemies = {
        {6.5, 20.5}, {26.5, 20.5}, {12.5, 19.5}, {5.5, 17.5}, {11.5, 14.5}, {21.5, 14.5}, {16.5, 13.5},
        {30.5, 13.5}, {5.5, 12.5}, {3.5, 11.5}, {12.5, 10.5}, {22.5, 9.5}, {21.5, 7.5}, {13.5, 5.5}
    };
    // Level 3 Boss spawn, same as enemies for x, y, unused until boss is implemented
    private double[] level3_boss = {28.5, 16.5};
    
    private Player player = new Player(2.5, 2.5, 0.0);
    private Hud hud = new Hud();
    private Menu menu = new Menu();
    private GameState state = GameState.MENU;
    private boolean escape_was_down = false;
    private boolean enter_was_down = false;
    private int current_level = 1;
    private int[][] current_map;
    
    private Item[] items;
    private Renderer.Texture[] item_textures;
    private Renderer.Texture exit_texture;
    private double exit_x = 23.5;
    private double exit_y = 2.5;
    private int door_row = 5;
    private int door_col = 22;
    
    // Level 3's exit room door opens when the final boss is defeated, not with a key (openBossDoor).
    // -1 means the level has no boss door (levels 1 and 2)
    private int boss_door_row = -1;
    private int boss_door_col = -1;
    
    // Texture sets for each level, loaded once at startup.
    private Renderer.Texture[] castle_walls;
    private Renderer.Texture castle_floor;
    private Renderer.Texture castle_ceiling;
    private Renderer.Texture castle_exit;
    private Renderer.Texture[] ice_walls;
    private Renderer.Texture ice_floor;
    private Renderer.Texture ice_ceiling;
    private Renderer.Texture ice_exit;
    private Renderer.Texture[] lab_walls;
    private Renderer.Texture lab_floor;
    private Renderer.Texture lab_ceiling;
    private Renderer.Texture lab_exit;
    private String message = "";
    private double message_time = 0;
    
    private double imp_x = 9.5;
    private double imp_y = 3.5;
    public Renderer.Texture[] imp_textures;
    
    private double dopefish_x = 10.5;
    private double dopefish_y = 4.5;
    public Renderer.Texture dopefish_texture;
    
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
        try {
            castle_walls = Renderer.Texture.read(new BufferedImage[] {
                ImageIO.read(new File("castle_wall.png")),
                ImageIO.read(new File("castle_wall_mossy.png")),
                ImageIO.read(new File("castle_torch.png")),
                ImageIO.read(new File("castle_banner.png")),
                ImageIO.read(new File("castle_window.png")),
                ImageIO.read(new File("castle_door.png"))
            });
            castle_floor = new Renderer.Texture(ImageIO.read(new File("castle_floor.png")));
            castle_ceiling = new Renderer.Texture(ImageIO.read(new File("castle_ceiling.png")));
            castle_exit = new Renderer.Texture(ImageIO.read(new File("castle_exit.png")));
            // Level 3 wall numbers: the Nth image here is wall N in the level3 map.
            ice_walls = Renderer.Texture.read(new BufferedImage[] {
                ImageIO.read(new File("ice_wall.png")),      // 1 cave rock
                ImageIO.read(new File("ice_rock2.png")),     // 2 cave rock
                ImageIO.read(new File("ice_crystal.png")),   // 3 crystal cluster
                ImageIO.read(new File("ice_icicles.png")),   // 4 icicles
                ImageIO.read(new File("ice_window.png")),    // 5 frozen skull
                ImageIO.read(new File("ice_door.png")),      // 6 door
                ImageIO.read(new File("ice_rock3.png")),     // 7 cave rock
                ImageIO.read(new File("ice_crystal2.png")),  // 8 crystal cluster
                ImageIO.read(new File("ice_crystal3.png"))   // 9 crystal cluster
            });
            ice_floor = new Renderer.Texture(ImageIO.read(new File("ice_floor.png")));
            ice_ceiling = new Renderer.Texture(ImageIO.read(new File("ice_ceiling.png")));
            ice_exit = new Renderer.Texture(ImageIO.read(new File("ice_exit.png")));
            
            // Level 2 wall numbers: the Nth image here is wall N in the level2 map.
            lab_walls = Renderer.Texture.read(new BufferedImage[] {
                ImageIO.read(new File("lab_wall.png")),    // 1 concrete panel
                ImageIO.read(new File("lab_pipes.png")),   // 2 pipe drop with valve
                ImageIO.read(new File("lab_light.png")),   // 3 caged lamp
                ImageIO.read(new File("lab_secure.png")),  // 4 steel blast wall
                ImageIO.read(new File("lab_vent.png")),    // 5 vent grille
                ImageIO.read(new File("lab_door.png")),    // 6 door
                ImageIO.read(new File("lab_panel.png")),   // 7 access panel
                ImageIO.read(new File("lab_sign.png"))     // 8 hazard sign
            });
            lab_floor = new Renderer.Texture(ImageIO.read(new File("lab_floor.png")));
            lab_ceiling = new Renderer.Texture(ImageIO.read(new File("lab_ceiling.png")));
            lab_exit = new Renderer.Texture(ImageIO.read(new File("lab_exit.png")));
            imp_textures = Renderer.Texture.read(new BufferedImage[] {
                ImageIO.read(new File("imp0.png")),
                ImageIO.read(new File("imp1.png")),
                ImageIO.read(new File("imp2.png")),
                ImageIO.read(new File("imp3.png")),
                ImageIO.read(new File("imp4.png")),
                ImageIO.read(new File("imp5.png")),
                ImageIO.read(new File("imp6.png")),
                ImageIO.read(new File("imp7.png"))
            });
            dopefish_texture = new Renderer.Texture(ImageIO.read(new File("dopefish.png")));
            item_textures = Renderer.Texture.read(new BufferedImage[] {
                ImageIO.read(new File("key.png")),
                ImageIO.read(new File("ammo.png")),
                ImageIO.read(new File("health.png")),
                ImageIO.read(new File("gun.png"))
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        loadLevel(1);
        
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
            player.update(time_delta, input, current_map);
            
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
        if (loadLevel(number)) {
            current_level = number;
            newGame();
        } else {
            menu.note = "LEVEL " + number + " IS NOT BUILT YET";
        }
    }
    
    // Sets up the map, textures, items, doors and exit for a level.
    // Returns false if that level doesn't exist yet.
    private boolean loadLevel(int number)
    {
        if (number == 1) {
            current_map = copyMap(level1);
            renderer.setWallTextures(castle_walls);
            renderer.setFloorTexture(castle_floor);
            renderer.setCeilingTexture(castle_ceiling);
            exit_texture = castle_exit;
            exit_x = 23.5;
            exit_y = 2.5;
            door_row = 5;
            door_col = 22;
            boss_door_row = -1;
            boss_door_col = -1;
            items = makeItems();
        } else if (number == 2) {
            current_map = copyMap(level2);
            renderer.setWallTextures(lab_walls);
            renderer.setFloorTexture(lab_floor);
            renderer.setCeilingTexture(lab_ceiling);
            exit_texture = lab_exit;
            exit_x = 26.5;
            exit_y = 2.5;
            door_row = 4;
            door_col = 25;
            boss_door_row = -1;
            boss_door_col = -1;
            items = makeLevel2Items();
        } else if (number == 3) {
            current_map = copyMap(level3);
            renderer.setWallTextures(ice_walls);
            renderer.setFloorTexture(ice_floor);
            renderer.setCeilingTexture(ice_ceiling);
            exit_texture = ice_exit;
            exit_x = 28.5;
            exit_y = 2.5;
            door_row = 17;
            door_col = 24;
            boss_door_row = 8;
            boss_door_col = 28;
            items = makeLevel3Items();
        } else {
            return false;
        }
        
        renderer.setWalls(current_map);
        return true;
    }
    
    // Works on a copy so opened doors are closed again when the level restarts.
    private int[][] copyMap(int[][] map)
    {
        int[][] copy = new int[map.length][];
        for (int i = 0; i < map.length; i++)
            copy[i] = map[i].clone();
        return copy;
    }

    private void newGame()
    {
        loadLevel(current_level);
        player = new Player(2.5, 2.5, 0.0);
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
    
    private Item[] makeLevel2Items()
    {
        return new Item[] {
            new Item(2.5, 16.5, Item.KEY),
            new Item(17.5, 7.5, Item.GUN),
            new Item(10.5, 3.5, Item.AMMO),
            new Item(26.5, 16.5, Item.HEALTH)
        };
    }

    private Item[] makeLevel3Items()
    {
        return new Item[] {
            new Item(2.5, 20.5, Item.KEY),
            new Item(16.5, 10.5, Item.GUN),
            new Item(17.5, 12.5, Item.AMMO),
            new Item(10.5, 2.5, Item.AMMO),
            new Item(15.5, 20.5, Item.HEALTH),
            new Item(22.5, 5.5, Item.HEALTH)
        };
    }

    // Opens the boss door (level 3) call when the boss is killed
    private void openBossDoor()
    {
        if (boss_door_row < 0 || current_map[boss_door_row][boss_door_col] == 0)
            return;
        
        current_map[boss_door_row][boss_door_col] = 0;
        showMessage("THE WAY OUT IS OPEN");
        Sound.play("door_open.wav", menu.volume);
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
        if (current_map[door_row][door_col] == 0)
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
            current_map[door_row][door_col] = 0;
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
        if (dx * dx + dy * dy < 0.6 * 0.6)
            state = GameState.WON;
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
        
        if (current_level == 1) {
            renderer.putThing(imp_x, imp_y, 1 * Math.PI, imp_textures);
            renderer.putSprite(dopefish_x, dopefish_y, dopefish_texture);
        }
        renderer.putSprite(exit_x, exit_y, exit_texture);
        for (int i = 0; i < items.length; i++) {
            if (items[i].taken == false)
                renderer.putSprite(items[i].x, items[i].y, item_textures[items[i].type]);
        }
        
        renderer.draw();
        
        BufferedImage device = renderer.getRender();
        
        if (state == GameState.MENU) {
            menu.draw(device, time_current);
        } else {
            hud.draw(device, player);
            if (state == GameState.PAUSED)
                hud.drawPause(device);
            if (state == GameState.WON)
                hud.drawEnd(device, "YOU GOT OUT!", "ENTER: level menu   ESC: main menu");
            if (state == GameState.LOST)
                hud.drawEnd(device, "YOU DIED", "ENTER: try again   ESC: main menu");
            if (time_current - message_time < 2000)
                hud.drawMessage(device, message);
        }
        
        Graphics g = bs.getDrawGraphics();
        g.drawImage(device, 0, 0, WIDTH, HEIGHT, null); // Does this cause a race condition? I hope not... Supposedly this function *can* be asynchronous.
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
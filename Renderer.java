import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/* 
 * 2.5D Renderer.
 * Geometry is drawn via raycasting. Sprites are drawn via Z-buffer.
 */
public class Renderer
{
    // Good enough for DOOM, good enough for us. I apologize for hardcoding these numbers...
    public static final int DEVICE_WIDTH = 320;
    public static final int DEVICE_HEIGHT = 320;
    
    public BufferedImage device;   // You *may* touch this, but please don't unless you *have* to. (Drawing UI is a good reason to touch this).
    public int[] device_pixels;    // Reference to device's pixels. Is it faster than device.setRGB()? A tutorial I saw suggests this so I presume it's good...
    private double depth_buffer[]; // Sprite drawing requires this. No touchy.
    
    // Use doubles to avoid any potential perils of floating point imprecision!
    private double camera_x;
    private double camera_y;
    private double camera_angle;
    private double camera_cos;
    private double camera_sin;
    private double camera_cosn;
    private double camera_sinn;
    
    // 2D array of blocks on a square grid. These are the world's walls.
    // Zero values indicate air. Integers >0 indicate a solid wall (this integer minus 1 is the index of its wall texture).
    public int[][] walls;
    
    // Textures
    public BufferedImage[] wall_textures = null;
    public BufferedImage floor_texture = null;
    public BufferedImage ceiling_texture = null;
    
    public Renderer()
    {
        device = new BufferedImage(DEVICE_WIDTH, DEVICE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        device_pixels = ((DataBufferInt)device.getRaster().getDataBuffer()).getData();
        depth_buffer = new double[DEVICE_WIDTH * DEVICE_WIDTH];
        clean();
    }
    
    // Wipe with black.
    public void clean()
    {
        clean(0x000000);
    }
    
    // Wipe device and depth buffer. Draw over the whole thing with a given color.
    public void clean(int color)
    {
        for (int i = 0; i < DEVICE_WIDTH * DEVICE_HEIGHT; i++) {
            device_pixels[i] = color;
            depth_buffer[i] = Double.POSITIVE_INFINITY;
        }
    }
    
    // X and Y are world coordinates. Angle is in radians: 0 faces +X, PI/2 faces +Y, PI faces -X, 3PI/2 faces -Y.
    public void setCamera(double world_x, double world_y, double world_angle)
    {
        camera_x = world_x;
        camera_y = world_y;
        camera_angle = normalize_angle(world_angle);
        camera_sin = Math.sin(world_angle);
        camera_cos = Math.cos(world_angle);
        camera_sinn = Math.sin(-world_angle);
        camera_cosn = Math.cos(-world_angle);
    }
    
    public void drawWalls()
    {
        for (int x = 0; x < DEVICE_WIDTH; x++)
        {
            drawRay(x);
        }
    }
    
    // Given 8 directional sprites, pick the best one and draw it.
    public void drawThing(double world_x, double world_y, double world_angle, BufferedImage[] textures)
    {
        double net_angle = normalize_angle(-Math.atan2(world_y - camera_y, world_x - camera_x) + world_angle);
        
        BufferedImage texture = null;
        
        // Ugh.
        if (net_angle < 0.392)
            texture = textures[0];
        else if (net_angle < 1.178)
            texture = textures[1];
        else if (net_angle < 1.963)
            texture = textures[2];
        else if (net_angle < 2.748)
            texture = textures[3];
        else if (net_angle < 3.534)
            texture = textures[4];
        else if (net_angle < 4.319)
            texture = textures[5];
        else if (net_angle < 5.105)
            texture = textures[6];
        else if (net_angle < 5.890)
            texture = textures[7];
        else
            texture = textures[0];
        
        drawSprite(world_x, world_y, texture);
    }
    
    // Draw a sprite. Always faces the camera.
    public void drawSprite(double world_x, double world_y, BufferedImage texture)
    {
        // Convert world coords to view coords.
        world_x -= camera_x;
        world_y -= camera_y;
        double view_x = world_x*camera_cosn - world_y*camera_sinn;
        double view_y = world_x*camera_sinn + world_y*camera_cosn;
        
        // Don't draw if behind camera.
        if (view_x < 0.1)
            return;
        
        // Sprite faces camera.
        double screen_x_left  = -(view_y + 0.5) / view_x;
        double screen_x_right = -(view_y - 0.5) / view_x;
        double screen_y_bottom = -0.5  / view_x;
        double screen_y_top    = 0.5 / view_x;
        
        // Don't draw if off screen.
        if (screen_x_right < -1.0)
            return;
        if (screen_x_left > 1.0)
            return;
        
        // Ugh...
        int device_x_left  = (int)(160.0 * screen_x_left + 160.0);
        int device_x_right = (int)(160.0 * screen_x_right + 160.0);
        int device_y_bottom = (int)(160.0 - 160.0 * screen_y_bottom);
        int device_y_top    = (int)(160.0 - 160.0 * screen_y_top);
        
        // Don't even attempt clever tricks.
        for (int device_y = Math.min(Math.max(0, device_y_top), DEVICE_HEIGHT - 1);
             device_y <= Math.min(Math.max(0, device_y_bottom), DEVICE_HEIGHT - 1);
             device_y++
        ) {
            for (int device_x = Math.min(Math.max(0, device_x_left), DEVICE_WIDTH - 1);
                 device_x <= Math.min(Math.max(0, device_x_right), DEVICE_WIDTH - 1);
                 device_x++
            ) { 
                int i = device_y * DEVICE_WIDTH + device_x;
                if (view_x >= depth_buffer[i])
                    continue;
                
                double screen_x = ((double)device_x + 0.5) / 160.0 - 1.0;
                double screen_y = ((double)device_y + 0.5) / 160.0 - 1.0;
                int texture_x = (int)((screen_x - screen_x_left) * texture.getWidth() / (screen_x_right - screen_x_left));
                int texture_y = (int)((screen_y - screen_y_bottom) * texture.getWidth() / (screen_y_top - screen_y_bottom));
                texture_x = Math.min(Math.max(0, texture_x), texture.getWidth() - 1);
                texture_y = Math.min(Math.max(0, texture_y), texture.getHeight() - 1);
                
                int color = texture.getRGB(texture_x, texture_y);
                
                if ((color & 0xFF000000) != 0xFF000000)
                    continue;
                
                device_pixels[i] = color;
                depth_buffer[i] = view_x;
            }
        }
    }
    
    // Draw floor and ceiling.
    public void drawFlats()
    {
        for (int device_y = 0; device_y < DEVICE_HEIGHT / 2; device_y++) { // We can mirror this, so we only need to do half.
            // Calculate view coordinates from pixel coordinates. We can do this because of same-height floors/ceilings.
            double view_x = 80.0 / (160.0 - (double)device_y + 0.5);
            for (int device_x = 0; device_x < DEVICE_WIDTH; device_x++) {
                double view_y = view_x * (1.0 - ((double)device_x + 0.5) / 160.0); // I could incrementalize this but... ugh... not unless there's performance problems.
                // Calculate world coordinates from view coordinates.
                double world_x = view_x*camera_cos - view_y*camera_sin + camera_x;
                double world_y = view_x*camera_sin + view_y*camera_cos + camera_y;
                // Get texture coordinates from world coordinates.
                int ceiling_texture_x = Math.min(Math.max(0, (int)((double)ceiling_texture.getWidth() * (world_x % 1.0))), ceiling_texture.getWidth() - 1);
                int ceiling_texture_y = Math.min(Math.max(0, (int)((double)ceiling_texture.getWidth() * (world_y % 1.0))), ceiling_texture.getWidth() - 1);
                int floor_texture_x = Math.min(Math.max(0, (int)((double)floor_texture.getWidth() * (world_x % 1.0))), ceiling_texture.getWidth() - 1);
                int floor_texture_y = Math.min(Math.max(0, (int)((double)floor_texture.getWidth() * (world_y % 1.0))), ceiling_texture.getWidth() - 1);
                // Draw pixels
                device_pixels[device_y * DEVICE_WIDTH + device_x] = ceiling_texture.getRGB(ceiling_texture_x, ceiling_texture_y);
                device_pixels[(319 - device_y) * DEVICE_WIDTH + device_x] = floor_texture.getRGB(floor_texture_x, floor_texture_y);
            }
        }
    }
    
    /*
     * Walk an advancing ray across the map, stopping at the first hit we meet.
     * Hits can only ever happen on a gridline, so we only need to check hits at points where our ray and a gridline intersect.
     * THIS CODE IS NASTY AS !#$% BUT IT WORKS. NO TOUCHY UNLESS IT'S BROKEN OR IT'S A REFACTOR FOR ADVANCED RAYCASTER!
     */
    private void drawRay(int device_x)
    {
        // Get y of (1, y) in view space from device space x. Offset by 0.5 to cast ray from pixel's center.
        double view_y = 1.0 - ((double)device_x + 0.5) / 160.0;
        
        // What tile is the camera standing in right now?
        int tile_x = (int)camera_x;
        int tile_y = (int)camera_y;
        
        // Don't bother rendering if we're stuck inside a wall.
        if (walls[tile_y][tile_x] != 0)
            return;
        
        // Our ray will move across the tiles in these directions. We don't know which directions yet.
        int tile_step_x = 0;
        int tile_step_y = 0;
        
        // Our ray is a vector, calculate it like one.
        // Modulusing our ray to be within the x:[0,1] y:[0,1] square lets me simplify the algorithm a bit.
        double ray_x0 = camera_x % 1.f;
        double ray_y0 = camera_y % 1.f;
        double ray_x1 = camera_cos - view_y*camera_sin + ray_x0;
        double ray_y1 = camera_sin + view_y*camera_cos + ray_y0;
        
        // How close is the nearest vertical/horizontal gridline to our marching ray?
        double x_dist; 
        double y_dist;
        
        // Distance to the next vertical/horizontal gridline?
        double delta_x_dist;
        double delta_y_dist;
        
        // It's okay and good to interpret our finite line segments as infinite lines here.
        double intersection[];
        if (ray_x1 - ray_x0 >= 0.0) {
            // Ray faces right (+X)
            tile_step_x = 1;
            intersection = line_intersect(ray_x0, ray_y0, ray_x1, ray_y1, 1.0, 0.0, 1.0, 1.0);
        } else {
            // Ray faces left (-X)
            tile_step_x = -1;
            intersection = line_intersect(ray_x0, ray_y0, ray_x1, ray_y1, 0.0, 0.0, 0.0, 1.0);
        }
        x_dist = distance_between_points(ray_x0, ray_y0, intersection[0], intersection[1]);
        if (ray_y1 - ray_y0 >= 0.0) {
            // Ray faces up (+Y)
            tile_step_y = 1;
            intersection = line_intersect(ray_x0, ray_y0, ray_x1, ray_y1, 0.0, 1.0, 1.0, 1.0);
        } else {
            // Ray faces down (-Y)
            tile_step_y = -1;
            intersection = line_intersect(ray_x0, ray_y0, ray_x1, ray_y1, 0.0, 0.0, 1.0, 0.0);
        }
        y_dist = distance_between_points(ray_x0, ray_y0, intersection[0], intersection[1]);
        
        // More calculating.
        double slope = (ray_y1 - ray_y0)/(ray_x1 - ray_x0);
        double slope_inverse = 1 / slope;
        delta_x_dist = Math.sqrt(1.0 + slope*slope);
        delta_y_dist = Math.sqrt(1.0 + slope_inverse*slope_inverse);
        
        // March rays
        boolean side = true;
        while (true) {
            if (x_dist < y_dist) {
                side = true;
                tile_x += tile_step_x;
                if (walls[tile_y][tile_x] != 0)
                    break;
                x_dist += delta_x_dist;
            } else {
                side = false;
                tile_y += tile_step_y;
                if (walls[tile_y][tile_x] != 0)
                    break;
                y_dist += delta_y_dist;
            }
        }
        
        // Undo incidental mirroring of textures by flipping them.
        boolean flip;
        if (side) {
            flip = tile_step_x > 0;
        } else {
            flip = tile_step_y < 0;
        }
        
        BufferedImage texture = wall_textures[walls[tile_y][tile_x] - 1];
        
        // The right triangle formed by the final ray has the same proportions as the right triangle formed by the initial ray, just scaled up.
        // The view_x of the world space ray is always 1. So to get the view_x of *_dist we can use the scaling factor * 1.0;
        double scale_factor = (side ? x_dist : y_dist) / distance_between_points(ray_x0, ray_y0, ray_x1, ray_y1);
        double view_x = scale_factor;
        double screen_y = 0.5 / view_x;
        
        // Convert logical "screen" cordinates to device "pixel" coordinates.
        // Magic number 160 is half of DEVICE_HEIGHT. Not bothering to make it pretty right now.
        int device_y_top = (int)(-160.0*screen_y + 160.0);
        //device_y_top = Math.min(Math.max(0, device_y_top), DEVICE_HEIGHT - 1);
        int device_y_bottom = (int)(160.0*screen_y + 160.0);
        //device_y_bottom = Math.min(Math.max(0, device_y_bottom), DEVICE_HEIGHT - 1);
        
        // Just in case
        int color = side ? 0xFF0000 : 0x0000FF;
        
        // Texture mapping, pixel drawing
        double world_x = scale_factor * (camera_cos - view_y*camera_sin) + camera_x;
        double world_y = scale_factor * (camera_sin + view_y*camera_cos) + camera_y;
        int texture_x = Math.min(Math.max(0, (int)((double)texture.getWidth() * ((Math.max(0, side ? world_y : world_x)) % 1.0))), texture.getWidth() - 1);
        if (flip) {
            texture_x = texture.getWidth() - 1 - texture_x;
        }
        double delta_texture_y = (double)texture.getHeight() / (device_y_bottom - device_y_top); // Incrementalize this !#$%.
        int device_y_begin = Math.min(Math.max(0, device_y_top), DEVICE_HEIGHT - 1);
        double texture_y = (double)(device_y_begin - device_y_top) * delta_texture_y; // Wind the Incremental backwards if we're getting clipped...
        for (int device_y = device_y_begin;
             device_y <= Math.min(Math.max(0, device_y_bottom), DEVICE_HEIGHT - 1);
             device_y++
        ) {
            color = texture.getRGB(texture_x, Math.min(Math.max(0, (int)texture_y), texture.getHeight() - 1));
            texture_y += delta_texture_y;
            int i = device_y * DEVICE_WIDTH + device_x;
            device_pixels[i] = color;
            depth_buffer[i] = view_x;
        }
    }
    
    private double normalize_angle(double angle)
    {
        angle = angle % (2.0 * Math.PI);
        if (angle < 0)
            angle += 2.0 * Math.PI;
        return angle;
    }
    
    private void testing_draw_squares()
    {
        for (int y = 0; y < DEVICE_HEIGHT; y++)
            for (int x = 0; x < DEVICE_WIDTH; x++) {
                int color = 0x000000;
                if (y > DEVICE_HEIGHT / 2) color += 0x000000FF;
                if (x > DEVICE_WIDTH / 2) color += 0xFF0000;
                device_pixels[y * DEVICE_WIDTH + x] = color;
            }
    }
    
    private void testing_draw_columns()
    {
        for (int x = 0; x < DEVICE_WIDTH; x++)
            for (int y = 0; y <= x % DEVICE_HEIGHT; y++) {
                device_pixels[y * DEVICE_WIDTH + x] = 0xFF00FF;
            }
    }
    
    private void testing_draw_diagonal()
    {
        for (int x = 0; x < DEVICE_WIDTH; x++)
            for (int y = 0; y < DEVICE_HEIGHT; y++) {
                double fx = (double)x / (double)DEVICE_WIDTH;
                double fy = (double)y / (double)DEVICE_HEIGHT;
                int color = 0x000000;
                if (fx > fy) color = 0xFF00FF;
                device_pixels[y * DEVICE_WIDTH + x] = color;
            }
    }
    
    // Can't do it like I would in C because Java can't pass pointers to primitives.
    private static double[] line_intersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4)
    {
        double out[] = new double[2];
        double denominator = (x1-x2)*(y3-y4)-(y1-y2)*(x3-x4);
        out[0] = ((x1*y2-y1*x2)*(x3-x4)-(x1-x2)*(x3*y4-y3*x4)) / denominator;
        out[1] = ((x1*y2-y1*x2)*(y3-y4)-(y1-y2)*(x3*y4-y3*x4)) / denominator;
        return out;
    }
    
    private static double distance_between_points(double x1, double y1, double x2, double y2)
    {
        double delta_x = x2 - x1;
        double delta_y = y2 - y1;
        return Math.sqrt(delta_x*delta_x + delta_y*delta_y);
    }
}
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.ArrayList;

/* 
 * 2.5D Renderer.
 */
public class Renderer
{
    // 320x240 is good enough for DOOM, good enough for us.
    public static final int DEVICE_WIDTH = 960;
    public static final int HALF_DEVICE_WIDTH = DEVICE_WIDTH / 2;
    public static final int DEVICE_HEIGHT = 720;
    public static final int HALF_DEVICE_HEIGHT = DEVICE_HEIGHT / 2;
    public static final double ASPECT_RATIO = (double)DEVICE_WIDTH / (double)DEVICE_HEIGHT;
    public static final double INVERSE_ASPECT_RATIO = (double)DEVICE_HEIGHT / (double)DEVICE_WIDTH; // This often gets used as screen-space Ymax
    
    // Buffers
    private BufferedImage device;    // Framebuffer
    private int[] device_pixels;     // Reference to device's pixels. Is it faster than device.setRGB()? A tutorial I saw suggests this so I presume it's good...
    private double depth_buffer_x[]; // Sprite drawing requires this.
    private int wall_device_y[];     // Flat drawing requires this.
    
    // Use doubles to avoid any potential perils of floating point imprecision!
    // Replacing these with floats doesn't seem to significantly improve performance.
    private double camera_x;
    private double camera_y;
    private double camera_angle;
    private double camera_cos;
    private double camera_sin;
    private double camera_cosn;
    private double camera_sinn;
    
    // Textures
    private Texture[] wall_textures = null;
    private Texture floor_texture = null;
    private Texture ceiling_texture = null;
    
    // Sprites
    private ArrayList<Sprite> sprites;
    
    // 2D array of blocks on a square grid. These are the world's walls.
    // Zero values indicate air. Integers >0 indicate a solid wall (this integer minus 1 is the index of its wall texture).
    // Levels have to be loaded upside down.
    private int[][] walls;
    
    public Renderer()
    {
        device = new BufferedImage(DEVICE_WIDTH, DEVICE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        device_pixels = ((DataBufferInt)device.getRaster().getDataBuffer()).getData();
        depth_buffer_x = new double[DEVICE_WIDTH];
        wall_device_y = new int[DEVICE_WIDTH];
        sprites = new ArrayList<Sprite>();
        clean();
    }
    
    public void setWalls(int[][] map)
    {
        walls = map;
    }
    
    public void setWallTextures(Texture[] textures)
    {
        wall_textures = textures;
    }
    
    public void setFloorTexture(Texture texture)
    {
        floor_texture = texture;
    }
    
    public void setCeilingTexture(Texture texture)
    {
        ceiling_texture = texture;
    }
    
    public BufferedImage getRender()
    {
        return device;
    }
    
    // Wipe the sprite buffer.
    // This used to wipe the depth buffer, but we don't have that anymore!
    public void clean()
    {
        sprites.clear();
    }
    
    // Wipe device and depth buffer. Draw over the whole thing with a given color.
    // You should only need to do this if you're not drawing Flats.
    public void clean(int color)
    {
        Arrays.fill(device_pixels, color);
        clean();
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
    
    // Given 8 directional sprites, pick the best one and draw it.
    public void putThing(double world_x, double world_y, double world_angle, Texture[] textures)
    {
        double net_angle = normalize_angle(world_angle - Math.atan2(world_y - camera_y, world_x - camera_x));
        
        Texture texture = null;
        
        // Determine which 8th of the circle the net angle lies within.
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
        
        putSprite(world_x, world_y, texture);
    }
    
    public void putSprite(double world_x, double world_y, Texture texture)
    {
        // Convert world coords to view coords.
        world_x -= camera_x;
        world_y -= camera_y;
        double view_x = world_x*camera_cosn - world_y*camera_sinn;
        double view_y = world_x*camera_sinn + world_y*camera_cosn;
        
        // Don't draw if behind camera
        if (view_x < 0.01) // Use an epsilon
            return;
        
        sprites.add(new Sprite(view_x, view_y, texture));
    }
    
    public void draw()
    {
        drawWalls();
        drawFlats();
        drawSprites();
    }
    
    // Painter's Algorithm
    private void drawSprites()
    {
        sprites.sort((a, b) -> Double.compare(b.view_x, a.view_x));
        for (Sprite s : sprites)
        {
            drawSprite(s.view_x, s.view_y, s.texture);
        }
    }
    
    // Draw a sprite. Always faces the camera.
    private void drawSprite(double view_x, double view_y, Texture texture)
    {
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
        
        // Pixel coordinates
        int device_x_left = device_to_pixel(screen_x_to_device_x(screen_x_left));
        int device_x_right = device_to_pixel(screen_x_to_device_x(screen_x_right));
        int device_y_bottom = device_to_pixel(screen_y_to_device_y(screen_y_bottom));
        int device_y_top = device_to_pixel(screen_y_to_device_y(screen_y_top));
        
        // Fast rejects
        int device_x_min = clamp(device_x_left, 0, DEVICE_WIDTH - 1);
        int device_x_max = clamp(device_x_right, 0, DEVICE_WIDTH - 1);
        for (; device_x_min < DEVICE_WIDTH && view_x >= depth_buffer_x[device_x_min]; device_x_min++);
        for (; device_x_max >= 0 && view_x >= depth_buffer_x[device_x_max]; device_x_max--);
        
        // Just in case. Haven't seen any issues here. I just don't want unpredictable behavior from the TextureMappers.
        if (device_x_min > device_x_max || device_x_min > DEVICE_WIDTH - 1 || device_x_max < 0)
            return;
        
        // Attempt clever tricks. Incremental algorithm on both X and Y.
        TextureMapper mapperY = new TextureMapper(device_y_top, device_y_bottom, 0, DEVICE_HEIGHT - 1, texture.getHeight());
        while (!mapperY.done()) {
            int device_y = mapperY.getIV();
            int texture_y = mapperY.getDV();
            TextureMapper mapperX = new TextureMapper(device_x_left, device_x_right, device_x_min, device_x_max, texture.getWidth());
            while (!mapperX.done()) {
                int device_x = mapperX.getIV();
                int texture_x = mapperX.getDV();
                mapperX.next();
                
                int color = texture.getRGB(texture_x, texture_y);
                if ((color & 0xFF000000) != 0xFF000000) { // Reject transparency
                    continue;
                }
                
                int i = device_y * DEVICE_WIDTH + device_x;
                device_pixels[i] = color;
            }
            mapperY.next();
        }
    }
    
    private class Sprite
    {
        double view_x;
        double view_y;
        Texture texture;
        
        public Sprite(double view_x, double view_y, Texture texture)
        {
            this.view_x = view_x;
            this.view_y = view_y;
            this.texture = texture;
        }
    }
    
    private void drawFlats()
    {
        double floor_width = (double)floor_texture.getWidth();
        double floor_height = (double)floor_texture.getHeight();
        
        for (int device_y = 0; device_y < HALF_DEVICE_HEIGHT; device_y++) {
            double view_x = screen_y_to_view_x(device_y_to_screen_y(pixel_to_device(device_y)));
            
            // Only allowed to do this because of fixed 90 degree FOV.
            double view_y0 = view_x;
            double view_y1 = -view_x;
            
            // Transform view space line segment (x,y0)->(x,y1) into a world space line segment.
            double world_x0 = view_x*camera_cos - view_y0*camera_sin + camera_x;
            double world_y0 = view_x*camera_sin + view_y0*camera_cos + camera_y;
            double world_x1 = view_x*camera_cos - view_y1*camera_sin + camera_x;
            double world_y1 = view_x*camera_sin + view_y1*camera_cos + camera_y;
            
            // Interpolate this line in an incremental fashion.
            FastMapper floor_x_mapper = new FastMapper(0, 0, DEVICE_WIDTH - 1, floor_width * world_x0, floor_width * world_x1);
            FastMapper floor_y_mapper = new FastMapper(0, 0, DEVICE_WIDTH - 1, floor_height * world_y0, floor_height * world_y1);
            
            // Now draw
            for (int device_x = 0; device_x < DEVICE_WIDTH; device_x++) {
                if (device_y >= wall_device_y[device_x]) { // Don't draw flat that will be occluded by wall
                    floor_x_mapper.next();
                    floor_y_mapper.next();
                    continue;
                }
                int floor_texture_x = floor_x_mapper.next() % floor_texture.width;
                int floor_texture_y = floor_y_mapper.next() % floor_texture.height;
                device_pixels[device_y * DEVICE_WIDTH + device_x] = ceiling_texture.getRGB(floor_texture_x, floor_texture_y);
                device_pixels[(DEVICE_HEIGHT - 1 - device_y) * DEVICE_WIDTH + device_x] = floor_texture.getRGB(floor_texture_x, floor_texture_y);
            }
        }
    }
    
    // Draw walls
    public void drawWalls()
    {
        for (int x = 0; x < DEVICE_WIDTH; x++)
        {
            drawRay(x);
        }
    }
    
    // BufferedImages are slow for some reason so we avoid them as much as possible with our own Texture class.
    public static class Texture
    {
        private int width;
        private int height;
        private int[] pixels;
        
        public Texture(BufferedImage img)
        {
            this.width = img.getWidth();
            this.height = img.getHeight();
            this.pixels = img.getRGB(0, 0, width, height, null, 0, height).clone();
        }
        
        public static Texture[] read(BufferedImage[] imgs)
        {
            int length = imgs.length;
            Texture[] textures = new Texture[length];
            for (int i = 0; i < length; i++) {
                textures[i] = new Texture(imgs[i]);
            }
            return textures;
        }
        
        public int getRGB(int x, int y)
        {
            return pixels[y * width + x];
        }
        
        public int getRGB(int i)
        {
            return pixels[i];
        }
        
        public int getWidth()
        {
            return width;
        }
        
        public int getHeight()
        {
            return height;
        }
    }
    
    // Raycasting
    private void drawRay(int device_x)
    {
        // What tile is the camera standing in right now?
        int tile_x = (int)camera_x;
        int tile_y = (int)camera_y;
        
        // Don't bother rendering if we're stuck inside a wall.
        if (walls[tile_y][tile_x] != 0)
            return;
        
        // Get y of (1, y) in view space from device space x. Offset by 0.5 to cast ray from pixel's center.
        double view_y = 1.0 - ((double)device_x + 0.5) / (double)HALF_DEVICE_WIDTH;
        
        // Our ray will move across the tiles in these directions. We don't know which directions yet.
        int tile_step_x = 0;
        int tile_step_y = 0;
        
        // Our ray is a line segment.
        // Modulusing our ray to be within the (0 <= x <= 1, 0 <= y <= 1) square lets me simplify the algorithm a bit.
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
        double slope_inverse = 1.0 / slope;
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
        
        Texture texture = wall_textures[walls[tile_y][tile_x] - 1];
        
        // The right triangle formed by the final ray has the same proportions as the right triangle formed by the initial ray, just scaled up.
        // The view_x of the world space ray is always 1. So to get the view_x of *_dist we can use the scaling factor * 1.0;
        double scale_factor = (side ? x_dist : y_dist) / distance_between_points(ray_x0, ray_y0, ray_x1, ray_y1);
        double view_x = scale_factor;
        depth_buffer_x[device_x] = view_x;
        // Near clipping. Required or program crashes!
        if (view_x < 0.01) {
            return;
        }
        double screen_y = 0.5 / view_x;
        
        // Convert logical "screen" cordinates to device "pixel" coordinates.
        int device_y_top = device_to_pixel(screen_y_to_device_y(screen_y));
        int device_y_bottom = device_to_pixel(screen_y_to_device_y(-screen_y));
        wall_device_y[device_x] = device_y_top;
        
        // Texture mapping, pixel drawing
        double world_x = scale_factor * (camera_cos - view_y*camera_sin) + camera_x;
        double world_y = scale_factor * (camera_sin + view_y*camera_cos) + camera_y;
        int texture_x = clamp((int)((double)texture.getWidth() * ((Math.max(0, side ? world_y : world_x)) % 1.0)), 0, texture.getWidth() - 1);
        if (flip) {
            texture_x = texture.getWidth() - 1 - texture_x;
        }
        
        TextureMapper mapper = new TextureMapper(device_y_top, device_y_bottom, 0, DEVICE_HEIGHT - 1, texture.getHeight());
        
        while (!mapper.done()) {
            device_pixels[mapper.getIV() * DEVICE_WIDTH + device_x] = texture.getRGB(texture_x, mapper.getDV());
            mapper.next();
        }
    }
    
    private static double normalize_angle(double angle)
    {
        angle = angle % (2.0 * Math.PI);
        if (angle < 0)
            angle += 2.0 * Math.PI;
        return angle;
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
    
    private static double clamp(double x, double min, double max)
    {
        return Math.clamp(x, min, max);
    }
    
    private static int clamp(int x, int min, int max)
    {
        return Math.clamp(x, min, max);
    }
    
    // Convert integer device-space pixel coordinate to floating-point device-space coordinate.
    private static double pixel_to_device(int n)
    {
        return (double)n + 0.5;
    }
    
    // Convert floating-point device-space coordinate to integer device-space pixel coordinate.
    private static int device_to_pixel(double n)
    {
        return (int)(Math.floor(n));
    }
    
    private static double device_x_to_screen_x(double device_x)
    {
        return map(device_x, 0.0, (double)DEVICE_WIDTH, -1.0, 1.0);
    }
    
    private static double device_y_to_screen_y(double device_y)
    {
        return map(device_y, 0.0, (double)DEVICE_HEIGHT, INVERSE_ASPECT_RATIO, -INVERSE_ASPECT_RATIO);
    }
    
    private static double screen_x_to_device_x(double screen_x)
    {
        return map(screen_x, -1.0, 1.0, 0.0, (double)DEVICE_WIDTH);
    }
    
    private static double screen_y_to_device_y(double screen_y)
    {
        return map(screen_y, INVERSE_ASPECT_RATIO, -INVERSE_ASPECT_RATIO, 0.0, (double)DEVICE_HEIGHT);
    }
    
    private static double screen_x_view_x_to_view_y(double screen_x, double view_x)
    {
        return -screen_x * view_x;
    }
    
    private static double screen_y_to_view_x(double screen_y)
    {
        return 0.5 / screen_y;
    }
    
    private static double view_x_to_screen_y(double view_x)
    {
        return 0.5 / view_x;
    }
    
    private static double view_x_y_to_screen_x(double view_x, double view_y)
    {
        return -view_y / view_x;
    }
    
    // Maps a single value from one interval to another interval.
    private static double map(double x, double from_0, double from_1, double to_0, double to_1)
    {
        return (x - from_0)*(to_1 - to_0)/(from_1 - from_0) + to_0;
    }
    
    // Fixed point multiply
    private static int fmul(long a, long b)
    {
        return (int)((a * b) >> 16);
    }
    
    // Fixed point divide
    private static int fdiv(long a, long b)
    {
        return (int)((a << 16) / b);
    }
    
    // Floor it
    private static int fixed_to_int_floor(int n)
    {
        if ((n < 0) && ((n & 0x0000FFFF) != 0)) // Check for sign and existence of fractional component
            return (n >> 16) - 1;
        else
            return n >> 16;
    }
    
    private static int int_to_fixed(int n)
    {
        return n << 16;
    }
    
    private static int double_to_fixed(double n)
    {
        return (int)(n * 65536.0);
    }
    
    // SLOW. Uses doubles though which is useful!
    // This class aids in mapping a sequence of values from one interval to another in an incremental way.
    // It does not have an Iterator interface because I permit this class to iterate indefinitely (it'll go as far as it's asked to)
    private static class Mapper
    {
        public double x;
        public double delta;
        
        // Map from this interval...
        public double from_0;
        public double from_1;
        
        // ... to this interval
        public double to_0;
        public double to_1;
        
        public Mapper(double start, double from_0, double from_1, double to_0, double to_1)
        {
            delta = (to_1 - to_0) / (from_1 - from_0);
            x = (start - from_0) * delta + to_0; // Wind fowards/backwards as necessary.
        }
        
        // Advance by 1.
        public double next()
        {
            double out = x;
            x += delta;
            return out;
        }
        
        // Adance by this many.
        public double next(double units)
        {
            double out = x;
            x += units * delta;
            return out;
        }
    }
    
    // TextureMapper seems to be faster than FastMapper, but FastMapper is definitely faster than Mapper.
    // TextureMapper created rendering artifacts when I attempted to use it for drawing Flats but Mapper was too slow, so we need this.
    // Uses fixed-point integer arithmetic.
    // This class aids in mapping a sequence of values from one interval to another in an incremental way.
    private static class FastMapper
    {
        public int x;
        public int delta;
        
        // Map from interval [in_0,in_1] to [out_0,out_1]
        public FastMapper(double start, double in_0, double in_1, int out_0, int out_1)
        {
            this.delta = fdiv(int_to_fixed(out_1 - out_0), double_to_fixed(in_1 - in_0));
            this.x = fmul(double_to_fixed(start - in_0), delta) + int_to_fixed(out_0); // Wind fowards/backwards as necessary.
        }
        
        public FastMapper(int start, int in_0, int in_1, int out_0, int out_1)
        {
            delta = fdiv(out_1 - out_0, in_1 - in_0);
            if (start == in_0)
                x = int_to_fixed(out_0);
            else
                x = fmul(int_to_fixed(start - in_0), delta) + int_to_fixed(out_0); // Wind forwards/backwards as necessary.
        }
        
        public FastMapper(int start, int in_0, int in_1, double out_0, double out_1)
        {
            delta = fdiv(double_to_fixed(out_1 - out_0), int_to_fixed(in_1 - in_0));
            if (start == in_0)
                x = double_to_fixed(out_0);
            else
                x = fmul(int_to_fixed(start - in_0), delta) + double_to_fixed(out_0);
        }
        
        // Advance by 1.
        public int next()
        {
            int out = x >> 16;
            x += delta;
            return out;
        }
    }
    
    // Heavily modified bresenham algorithm from textbook.
    // In this case, x is merely the independent variable and y is the dependent variable.
    private static class TextureMapper
    {
        private int x, y, d, dx, c, m;
        private int end;
        
        // Our independent variable moves from from to to, clamped by min and max.
        // Our dependent variable moves from 0 to length - 1.
        public TextureMapper(int from, int to, int min, int max, int length)
        {
            x = from;
            y = 0;
            d = 0;
            dx = to - from;
            c = 2 * dx;
            m = 2 * (length - 1);
            end = Math.min(max, to);
            
            // Skip past clipped area
            if (from < min) {
                int x_offset = min - from;
                x = min;
                d += x_offset * m;
                
                int y_offset = (d - dx)/c;
                y += y_offset;
                d -= y_offset * c;
            }
        }
        
        // In rare cases, y can sometimes go out of bounds. I have no idea how.
        // This extra check is necessitated.
        public boolean done()
        {
            return (x > end);
        }
        
        // Get independent variable
        public int getIV()
        {
            return x;
        }
        
        // Get dependent variable
        public int getDV()
        {
            return y;
        }
        
        // Advance mapper
        public void next()
        {
            x++;
            d += m;
            while (d >= dx) {
                y++;
                d -= c;
            }
            // Don't try to accelerate this with the y_offset trick, it ends up slower.
        }
    }
}
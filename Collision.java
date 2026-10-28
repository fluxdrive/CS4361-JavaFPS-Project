public class Collision
{
    public static final double RADIUS = 0.2;
    
    public static boolean isSolid(int[][] walls, double x, double y)
    {
        if (x < 0 || y < 0)
            return true;
        
        int tile_x = (int)x;
        int tile_y = (int)y;
        
        if (tile_y >= walls.length)
            return true;
        if (tile_x >= walls[tile_y].length)
            return true;
        if (walls[tile_y][tile_x] != 0)
            return true;
        
        return false;
    }
    
    public static boolean isBlocked(int[][] walls, double x, double y)
    {
        if (isSolid(walls, x - RADIUS, y - RADIUS))
            return true;
        if (isSolid(walls, x + RADIUS, y - RADIUS))
            return true;
        if (isSolid(walls, x - RADIUS, y + RADIUS))
            return true;
        if (isSolid(walls, x + RADIUS, y + RADIUS))
            return true;
        
        return false;
    }
}

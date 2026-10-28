import java.awt.event.KeyEvent;

public class Player
{
    public double x;
    public double y;
    public double angle;
    
    public Player(double x, double y, double angle)
    {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }
    
    public void update(double time_delta, Input input, int[][] walls)
    {
        double motion_x = 0;
        double motion_y = 0;
        if (input.keyboard[KeyEvent.VK_W])
            motion_x += 0.002 * time_delta;
        if (input.keyboard[KeyEvent.VK_S])
            motion_x -= 0.002 * time_delta;
        if (input.keyboard[KeyEvent.VK_A])
            motion_y += 0.002 * time_delta;
        if (input.keyboard[KeyEvent.VK_D])
            motion_y -= 0.002 * time_delta;
        if (input.keyboard[KeyEvent.VK_LEFT])
            angle += 0.00157 * time_delta;
        if (input.keyboard[KeyEvent.VK_RIGHT])
            angle -= 0.00157 * time_delta;
        
        double move_x = motion_x * Math.cos(angle) - motion_y * Math.sin(angle);
        double move_y = motion_x * Math.sin(angle) + motion_y * Math.cos(angle);
        
        if (Collision.isBlocked(walls, x + move_x, y) == false)
            x = x + move_x;
        if (Collision.isBlocked(walls, x, y + move_y) == false)
            y = y + move_y;
    }
}

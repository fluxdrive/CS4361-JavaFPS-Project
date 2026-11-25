public class Item
{
    public static final int KEY = 0;
    public static final int AMMO = 1;
    public static final int HEALTH = 2;
    public static final int GUN = 3;
    
    public double x;
    public double y;
    public int type;
    public boolean taken = false;
    
    public Item(double x, double y, int type)
    {
        this.x = x;
        this.y = y;
        this.type = type;
    }
}

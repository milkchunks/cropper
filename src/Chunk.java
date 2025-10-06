public class Chunk {
    static final int WIDTH = 200;
    static final int HEIGHT = 200;
    int x;
    int y;
    int integerRGB;

    Chunk(int x, int y, int integerRGB) {
        this.x = x;
        this.y = y;
        this.integerRGB = integerRGB;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "x=" + x +
                ", y=" + y +
                ", integerRGB=" + integerRGB +
                '}';
    }
}

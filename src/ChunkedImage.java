import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ChunkedImage {
    ArrayList<ArrayList<Chunk>> map;
    //width in chunks
    int cw;
    //height in chunks
    int ch;

    ChunkedImage(ArrayList<ArrayList<Chunk>> map, int cw, int ch) {
        this.map = map;
        this.cw = cw;
        this.ch = ch;
    }
}

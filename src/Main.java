import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static final int SECTOR_WIDTH = 200;
    public static final int SECTOR_HEIGHT = 200;
    public static void main(String[] args) {
        BufferedImage img = null;

        try
        {
            img = ImageIO.read(new File("C:\\Users\\catti\\Downloads\\IMG_4204.jpg"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        ChunkedImage cimg = chunk(img);
        writePhoto(crop(cimg, img), "cropped");
    }


    public void copyDir(File dir) {
        for (int i = 0; i < dir.listFiles().length; i++) {
            File elm = dir.listFiles()[i];
            if (elm.isDirectory()) {
                copyDir(elm);
            }
        }
    }

    /**
     * Lowers the resolution of an image by taking the average color of SECTOR_HEIGHT x SECTOR_WIDTH squares, then displaying these instead of a full res image.
     * @param input - Photo to be 'chunked'
     * @return A much lower resolution image.
     */
    public static ChunkedImage chunk(BufferedImage input) {
        BufferedImage output = copy(input);
        ArrayList<ArrayList<Chunk>> chunkMap = new ArrayList<>();
        int chunkX = 0;
        int chunkY = 0;
        for (int i = 0; i < output.getWidth(); i += SECTOR_WIDTH) {
            //add new column
            chunkMap.add(new ArrayList<>());
            for (int j = 0; j < output.getHeight(); j += SECTOR_HEIGHT) {
                int[] sector = input.getRGB(i, j, SECTOR_WIDTH, SECTOR_HEIGHT, null, 0, SECTOR_WIDTH);
                //3 byte bgr
                int bsum = 0;
                int gsum = 0;
                int rsum = 0;
                for (int k = 0; k < sector.length; k++) {
                    int b = new Color(sector[k]).getBlue();
                    bsum += b;
                    int g = new Color(sector[k]).getGreen();
                    gsum += g;
                    int r = new Color(sector[k]).getRed();
                    rsum += r;
                }
                int avgb = bsum / (SECTOR_HEIGHT * SECTOR_WIDTH);
                int avgg = gsum / (SECTOR_HEIGHT * SECTOR_WIDTH);
                int avgr = rsum / (SECTOR_HEIGHT * SECTOR_WIDTH);

                int[] outputRGB = new int[sector.length];
                int integerAvg = rgbToInt(avgr, avgg, avgb);
                Arrays.fill(outputRGB, integerAvg);
                //add new cell to column (add row)
                chunkMap.get(chunkX).add(new Chunk(chunkX, chunkY, integerAvg));
                output.setRGB(i, j, SECTOR_WIDTH, SECTOR_HEIGHT, outputRGB, 0, SECTOR_WIDTH);
                chunkY++;
            }
            chunkX++;
            chunkY = 0;
        }
        writePhoto(output, "save");
        return new ChunkedImage(chunkMap, chunkMap.size(), chunkMap.get(0).size());
    }

    //todo: decide which chunks are different enough to be the crop edge
    public static BufferedImage crop(ChunkedImage input, BufferedImage original) {
        //pick from center, go outwards vertically from center chunk until you find top and bottom margins
        //then go left until you find the top and bottom left corners
        //then go right to find top and bottom right corners
        //compare top corners to find highest, then compare bottom corners to find lowest. if the crop chunk is the very edge, do not crop
        //15 truncates to 7

        //set all edges to the center
        Chunk sectionTop = input.map.get(input.cw / 2).get(input.ch / 2);
        Chunk sectionBottom = input.map.get(input.cw / 2).get(input.ch / 2);
        Chunk sectionLeft = input.map.get(input.cw / 2).get(input.ch / 2);
        Chunk sectionRight = input.map.get(input.cw / 2).get(input.ch / 2);

        Chunk prevUp = input.map.get(input.cw / 2).get(input.ch / 2);
        Chunk prevDown = input.map.get(input.cw / 2).get(input.ch / 2);
        Chunk prevL = input.map.get(input.cw / 2).get(input.ch / 2);
        Chunk prevR = input.map.get(input.cw / 2).get(input.ch / 2);

        for (int u = prevUp.y + 1; u < input.ch; u++) {
            Chunk nextUpChunk = input.map.get(input.cw / 2).get(input.ch - u);
            //check all r g and b
            Color nextUpChunkColor = new Color(nextUpChunk.integerRGB);
            Color thisUpChunkColor = new Color(prevUp.integerRGB);

            if (meetsThreshold(thisUpChunkColor, nextUpChunkColor)) {
                break;
            } else {
                sectionTop = nextUpChunk;
            }
            prevUp = nextUpChunk;
        }

        for (int d = prevDown.y + 1; d < input.ch; d++) {
            Chunk nextDownChunk = input.map.get(input.cw / 2).get(d);
            Color nextDownChunkColor = new Color(nextDownChunk.integerRGB);
            Color thisDownChunkColor = new Color(prevDown.integerRGB);

            if (meetsThreshold(thisDownChunkColor, nextDownChunkColor)) {
                break;
            } else {
                sectionBottom = nextDownChunk;
            }
            prevDown = nextDownChunk;
        }

        //go left
        for (int l = prevL.x + 1; l < input.cw; l++) {
            Chunk nextLChunk = input.map.get(input.cw - l).get(input.ch / 2);
            //check all r g and b
            Color nextLChunkColor = new Color(nextLChunk.integerRGB);
            Color thisLChunkColor = new Color(prevL.integerRGB);

            if (meetsThreshold(thisLChunkColor, nextLChunkColor)) {
                break;
            } else {
                sectionLeft = nextLChunk;
            }
            prevL = nextLChunk;
        }

        for (int r = prevR.x + 1; r < input.cw; r++) {
            Chunk nextRChunk = input.map.get(r).get(input.ch / 2);
            Color nextRChunkColor = new Color(nextRChunk.integerRGB);
            Color thisRChunkColor = new Color(prevR.integerRGB);
            if (meetsThreshold(thisRChunkColor, nextRChunkColor)) {
                break;
            } else {
                sectionRight = nextRChunk;
            }
            prevR = nextRChunk;
        }

        //now crop based on the outer edges
        //x = x of leftmost point
        Chunk topLeft = new Chunk(sectionLeft.x, sectionTop.y, input.map.get(sectionLeft.x).get(sectionTop.y).integerRGB);
        Chunk bottomRight = new Chunk(sectionRight.x, sectionBottom.y, input.map.get(sectionRight.x).get(sectionBottom.y).integerRGB);

        int x1 = topLeft.x * SECTOR_WIDTH;
        int y1 = topLeft.y * SECTOR_HEIGHT;
        int w = (bottomRight.x + 1) * SECTOR_WIDTH;
        int h = (bottomRight.y + 1) * SECTOR_HEIGHT;
        if (bottomRight.x + 1 == input.map.size()) w = original.getWidth() - x1;
        if (bottomRight.y + 1 == input.map.get(0).size()) h = original.getHeight() - y1;
        return original.getSubimage(x1, y1, w, h);
    }

    public static boolean meetsThreshold(Color a, Color b) {
        final int THRESHOLD = 75;
        return //Math.abs(a.getRed() - b.getRed()) > THRESHOLD ||
                Math.abs(a.getGreen() - b.getGreen()) > THRESHOLD; //||
                //Math.abs(a.getBlue() - b.getBlue()) > THRESHOLD;
    }

    public static void writePhoto(BufferedImage pic, String name) {
        try {
            ImageIO.write(pic, "jpg", new File("C:\\Users\\catti\\Downloads\\" + name + ".jpg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int rgbToInt(int r, int g, int b) {
        int rgb = r;
        rgb = (rgb << 8) + g;
        rgb = (rgb << 8) + b;
        return rgb;
    }


    public static BufferedImage copy(BufferedImage toCopy) {
        int tw = toCopy.getWidth() - toCopy.getWidth() % SECTOR_WIDTH;
        int th = toCopy.getHeight() - toCopy.getHeight() % SECTOR_HEIGHT;
        int[] rgb = toCopy.getRGB(0, 0, tw, th, null, 0,tw);
        BufferedImage output = new BufferedImage(tw, th, toCopy.getType());
        output.setRGB(0, 0, tw, th, rgb, 0 ,tw);
        return output;
    }
}
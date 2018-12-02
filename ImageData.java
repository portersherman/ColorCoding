import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/*----------------------------------------------------------------------------*/
/*
/* File: ImageData.java
/*
/* ImageData objects store a two-dimensional array of Pixel objects, allowing
/* for more straightforward filtering operations on the pixel data than provided
/* by the BufferedImage data type.
/*
/* Author: Porter Sherman
/*
/*----------------------------------------------------------------------------*/

public class ImageData {

    // pixel array
    private Pixel[][] data;
    // width of image
    private int width;
    // height of iamge
    private int height;
    // presence of alpha channel
    private boolean hasAlphaChannel;

    // constructor for implementing deep copy
    public ImageData(int height, int width, Pixel[][] data, boolean hasAlphaChannel) {
        this.height = height;
        this.width = width;
        this.hasAlphaChannel = hasAlphaChannel;
        this.data = new Pixel[height][width];
        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
                // duplicate all pixels
                this.data[j][i] = data[j][i].copy();
            }
        }
    }

    // constructor for creating blank images
    public ImageData(int height, int width, boolean hasAlphaChannel) {
        this.height = height;
        this.width = width;
        this.hasAlphaChannel = hasAlphaChannel;
        this.data = new Pixel[height][width];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // initialize all pixels to transparent black
                data[j][i] = new Pixel(0, 0, 0, 0);
            }
        }
    }

    // constructor that marshalls BufferedImage object to ImageData object
    public ImageData(BufferedImage img) {
        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        this.width = img.getWidth();
        this.height = img.getHeight();
        this.hasAlphaChannel = img.getAlphaRaster() != null;
        this.data = new Pixel[height][width];
        // marshall differently depending on presence of alpha data
        if (hasAlphaChannel) {
            final int pixelLength = 4;
            for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
                // marshalling from byte to Pixel object
                data[row][col] = new Pixel(
                    (int) pixels[pixel + 3] & 0xff, // red
                    (int) pixels[pixel + 2] & 0xff, // green
                    (int) pixels[pixel + 1] & 0xff, // blue
                    (int) pixels[pixel] & 0xff // alpha
                );
                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        } else {
            final int pixelLength = 3;
            for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
                // marshalling from byte to Pixel object
                data[row][col] = new Pixel(
                    (int) pixels[pixel + 2] & 0xff, // red
                    (int) pixels[pixel + 1] & 0xff, // green
                    (int) pixels[pixel] & 0xff, // blue
                    0xff // alpha
                );
                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        }
    }

    // getter for width
    public int getWidth() {
        return this.width;
    }

    // getter for height
    public int getHeight() {
        return this.height;
    }

    // getter for alpha channel
    public boolean getHasAlphaChannel() {
        return this.hasAlphaChannel;
    }

    // getter for pixel array
    public Pixel[][] getData() {
        return this.data;
    }

    // utility function to marshall ImageData object to BufferedImage object
    public void toImage(BufferedImage img) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                img.setRGB(i, j, data[j][i].toInt());
            }
        }
    }

    // utility function to prevent out of bounds errors
    public int clamp(float x, int floor, int ceil) {
        return (x > ceil) ? ceil : (x < floor) ? floor : (int) x;
    }

    // transform one-dimensional index to two-dimensional index
    public int[] index2D(int index1D) {
        return new int[]{ (index1D / this.width), (index1D % this.width) };
    }

    // transform two-dimensional idnex to one-dimensional index
    public int index1D(int index2DY, int index2DX) {
        return (index2DY * this.width) + index2DX;
    }

    // interface for map that operates on pixels uniformly
    public interface Map {
        void apply(Pixel p);
    }

    // interface for map that allows for mapping from src pixel to dest pixel
    public interface CoarseMap {
        void apply(Pixel dest, Pixel src, int index);
    }

    // interface for map that provide a index for spatial filters
    public interface IndexedMap {
        void apply(Pixel p, int index);
    }

    // interface for map that applies kernel to matrix of neighboring pixels
    public interface KernelMap {
        void apply(Pixel[] pixels, int index);
    }

    // apply map to every pixel in ImageData object
    public void applyMap(Map map) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                map.apply(data[j][i]);
            }
        }
    }

    // apply coarse map to every pixel in ImageData object
    public void applyCoarseMap(CoarseMap map, int size) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // calculate one-dimensional index
                int index = (i + j * width);
                // src pixel selected from pixellation area using point sampling
                map.apply(
                    data[j][i],
                    data[clamp((int) Math.floor(j / size) * size + size/2, 0, height - 1)][clamp((int) Math.floor(i / size) * size + size/2, 0, width - 1)],
                    index
                );
            }
        }
    }

    // apply coarse map to every pixel in ImageData object using averaging
    public void applyCoarseMapWithAveraging(CoarseMap map, int size) {
        // store averages for each pixellation area
        Pixel[][] averages = new Pixel[height / size + 1][width / size + 1];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // if average for this pixellatino area hasn't yet been
                // calculated, calculate
                if (averages[j / size][i / size] == null) {
                    Pixel average = new Pixel(0, 255);
                    int weight = 0;
                    // sum pixels in pixellation area
                    for (int k = i; k < i + size; k++) {
                        for (int l = j; l < j + size; l++) {
                            if (k < 0 || k > width - 1 || l < 0 || l > height - 1) {
                                continue;
                            } else {
                                average.setPixel(average.add(data[clamp(l, 0, height - 1)][clamp(k, 0, width - 1)]));
                                weight++;
                            }
                        }
                    }
                    // normalize
                    averages[j / size][i / size] = average.multScalar(1 / (double) weight);
                }
                // calculate one-dimensional index
                int index = (i + j * width);
                // src pixel calculated from averaging operation
                map.apply(
                    data[j][clamp((i + width) % width, 0, width - 1)],
                    averages[j / size][i / size],
                    index
                );
            }
        }
    }

    // apply coarse map to every pixel in ImageData object with rows offset
    public int[] applyOffsetCoarseMap(CoarseMap map, int size) {
        // keep track of row offsets
        int[] offsets = new int[height / size + 1];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // if offset for this row hasn't been calculated yet, calculate
                while (offsets[j / size] == 0) {
                    offsets[j / size] = (int) (Math.random() * size) - size/2;
                }
                // calculate one-dimensional index
                int index = (i + j * width);
                // src pixel selected from pixellation area using point sampling
                map.apply(
                    data[j][clamp((i + offsets[j / size] + width) % width, 0, width - 1)],
                    data[clamp((int) Math.floor(j / size) * size + size/2, 0, height - 1)][clamp((int) (Math.floor(i / size) * size + size/2 + offsets[j / size] + width) % width, 0, width - 1)],
                    index
                );
            }
        }
        // return offsets used for subsequent filters
        return offsets;
    }

    // apply coarse map to every pixel in ImageData object using averaging with
    // rows offset
    public int[] applyOffsetCoarseMapWithAveraging(CoarseMap map, int size) {
        // store averages for each pixellation area
        Pixel[][] averages = new Pixel[height / size + 1][width / size + 1];
        // keep track of row offsets
        int[] offsets = new int[height / size + 1];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // if offset for this row hasn't been calculated yet, calculate
                while (offsets[j / size] == 0) {
                    offsets[j / size] = (int) (Math.random() * size) - size/2;
                }
                // if average for this pixellatino area hasn't yet been
                // calculated, calculate
                if (averages[j / size][i / size] == null) {
                    Pixel average = new Pixel(0, 255);
                    int weight = 0;
                    // sum pixels in pixellation area
                    for (int k = i + offsets[j / size]; k < i + size + offsets[j / size]; k++) {
                        for (int l = j; l < j + size; l++) {
                            average.setPixel(average.add(data[clamp(l, 0, height - 1)][clamp((k + width) % width, 0, width - 1)]));
                            weight++;
                        }
                    }
                    // normalize
                    averages[j / size][i / size] = average.multScalar(1 / (double) weight);
                }
                // calculate one-dimensional index
                int index = (i + j * width);
                // src pixel calculated from averaging operation
                map.apply(
                    data[j][clamp((i + offsets[j / size] + width) % width, 0, width - 1)],
                    averages[j / size][i / size],
                    index
                );
            }
        }
        // return offsets used for subsequent filters
        return offsets;
    }

    // apply indexed map to every pixel in ImageData object
    public void applyIndexedMap(IndexedMap map) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // calculate one-dimensional index
                int index = (i + j * width);
                map.apply(data[j][i], index);
            }
        }
    }

    // apply indexed map to every pixel in ImageData object with rows offset
    public void applyIndexedMapWithOffsets(IndexedMap map, int size, int[] offsets) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // calculate one-dimensional index
                int index = (i + j * width);
                // select pixel after offsetting and wrapping on image borders
                map.apply(data[j][clamp((i + offsets[j / size] + width) % width, 0, width - 1)], index);
            }
        }
    }

    // apply filter kernel to area according to dimension supplied
    public void applyKernelMap(KernelMap map, int dimension) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // calculate one-dimensional index
                int index = (i + j * width);
                // create new one-dimensional array to collect pixels in kernel
                // area
                Pixel[] res = new Pixel[dimension * dimension];
                for (int k = j - dimension/2; k < j + dimension/2 + 1; k++) {
                    for (int l = i - dimension/2; l < i + dimension/2 + 1; l++) {
                        res[((k - j + dimension/2) * dimension) + (l - i + dimension/2)] = getData()[k][l];
                    }
                }
                map.apply(res, index);
            }
        }
    }
}

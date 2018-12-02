import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/*----------------------------------------------------------------------------*/
/*
/* File: Comp.java
/*
/* Comp implements an iterative image filter based on the decomposition and
/* reconstruction of the imput image. The effect makes use of a number of
/* transformations. At progressively finer resolutions, the input image is
/* pixellated, then separated out into RGB and CMY channels. Then, each
/* pixellation in each channel is radiused according to the luminosity of the
/* pixellation, and offset from center. These circles are then overlaid,
/* creating the familiar patterns that connote color spaces. The overlaying is
/* performed with either a lighten or darken blend mode, depending on the color
/* space. After all levels have been computed, the results are composited
/* normally.
/*
/* Author: Porter Sherman
/*
/*----------------------------------------------------------------------------*/

// used to determine how to calculate pixel intensity during radiusing
enum Mode { RGB, CMY };

public class Comp {

    // prime numbers used to mitigate interference in composited image
    private static int[] randomLevels = { 1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37 };

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("please supply filename as first argument and ____ as second argument");
            System.out.println("alternatively, type 'help' for more information");
            return;
        }

        if (args[0].equals("help")) {
            System.out.println("* first argument: filename");
            System.out.println("* second argument: resolution depth");
            System.out.println("* third argument: version");
            return;
        }

        try {
            // read input image into BuffereImage Object
            BufferedImage inImg = ImageIO.read(new File(args[0]));
            // marshall buffere image object into ImageData object
            ImageData imgData = new ImageData(inImg);
            // run CMY and RGB color space filters sequentially
            compCMY(imgData, Integer.parseInt(args[1]), args[0].substring(0, args[0].indexOf('.', 0)), (args[2] != null) ? args[2] : "");
            compRGB(imgData, Integer.parseInt(args[1]), args[0].substring(0, args[0].indexOf('.', 0)), (args[2] != null) ? args[2] : "");
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static void compCMY(ImageData imgData, int levels, String fileName, String version) {
        long start = System.currentTimeMillis();

        // create results array for future compositing
        ImageData[] results = new ImageData[levels];

        // iterate through levels
        for (int i = 0; i < levels; i++) {

            // too many levels of depth requested, avoid out of bounds error
            if (i > randomLevels.length) {
                break;
            }

            // size of pixel post-pixellation
            int size = (int) ((double) imgData.getWidth() / Math.pow(randomLevels[i], 2));

            // max resolution for efficiency and fidelity
            if (size < 7) {
                break;
            }

            // deep copy input ImageData
            ImageData imgDataCopy = new ImageData(
                imgData.getHeight(),
                imgData.getWidth(),
                imgData.getData(),
                imgData.getHasAlphaChannel()
            );

            // pixellate input image and return random offsets used
            int[] offsets = pixellateOffsetAverage(imgDataCopy, size);

            // construct new ImageData objects from deep copy
            ImageData imgDataC = new ImageData(
                imgDataCopy.getHeight(),
                imgDataCopy.getWidth(),
                imgDataCopy.getData(),
                imgDataCopy.getHasAlphaChannel()
            );
            ImageData imgDataM = new ImageData(
                imgDataCopy.getHeight(),
                imgDataCopy.getWidth(),
                imgDataCopy.getData(),
                imgDataCopy.getHasAlphaChannel()
            );
            ImageData imgDataY = new ImageData(
                imgDataCopy.getHeight(),
                imgDataCopy.getWidth(),
                imgDataCopy.getData(),
                imgDataCopy.getHasAlphaChannel()
            );

            // separate each copy of the pixellated ImageData into channels
            // by performing component-wise multiplication and addition to each
            // pixel (arguments one and two)
            separate(new Pixel(255, 0, 0), new Pixel(0, 255, 255), imgDataC);
            separate(new Pixel(0, 255, 0), new Pixel(255, 0, 255), imgDataM);
            separate(new Pixel(0, 0, 255), new Pixel(255, 255, 0), imgDataY);

            // apply radiusing filter to each ImageData object and offset
            // depending on which function is called
            circleCenterOffset(imgDataCopy, size, offsets, Mode.CMY);
            circleTopOffset(imgDataC, size, offsets, Mode.CMY);
            circleLeftOffset(imgDataM, size, offsets, Mode.CMY);
            circleRightOffset(imgDataY, size, offsets, Mode.CMY);

            try {
                write(imgDataC, fileName + "-" + version + "-tri-" + i + "-C.png");
                write(imgDataM, fileName + "-" + version + "-tri-" + i + "-M.png");
                write(imgDataY, fileName + "-" + version + "-tri-" + i + "-Y.png");
                write(imgDataCopy, fileName + "-" + version + "-tri-" + i + "-CMY-all.png");
            } catch (IOException e) {
                System.out.println(e);
            }

            // composite three channels and radiused original image using
            // darken blend mode
            results[i] = compositeDarken(new ImageData[]{ imgDataCopy, imgDataC, imgDataM, imgDataY });

            try {
                write(results[i], fileName + "-" + version + "-tri-" + i + "-darken.png");
            } catch (IOException e) {
                System.out.println(e);
            }
        }

        // alpha composite (using implicity increasing z-indices) results from
        // each level of resolution
        ImageData imgDataRes = compositeNormal(results);

        try {
            write(imgDataRes, fileName + "-" + version + "-tri-" + levels + "-CMY-normal.png");
        } catch (IOException e) {
            System.out.println(e);
        }

        System.out.println("compCMY finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // iteratively calls appropriate filters on imgData object, outputs all
    // channels and compositing results
    public static void compRGB(ImageData imgData, int levels, String fileName, String version) {
        long start = System.currentTimeMillis();

        // create results array for future compositing
        ImageData[] results = new ImageData[levels];

        // iterate through results
        for (int i = 0; i < levels; i++) {

            // too many levels of depth requested, avoid out of bounds error
            if (i > randomLevels.length) {
                break;
            }

            // size of pixel post-pixellation
            int size = (int) ((double) imgData.getWidth() / Math.pow(randomLevels[i], 2));

            // max resolution for efficiency and fidelity
            if (size < 7) {
                break;
            }

            // deep copy input ImageData
            ImageData imgDataCopy = new ImageData(
                imgData.getHeight(),
                imgData.getWidth(),
                imgData.getData(),
                imgData.getHasAlphaChannel()
            );

            // pixellate input image and return random offsets used
            int[] offsets = pixellateOffsetAverage(imgDataCopy, size);

            // construct new ImageData objects from deep copy
            ImageData imgDataR = new ImageData(
                imgDataCopy.getHeight(),
                imgDataCopy.getWidth(),
                imgDataCopy.getData(),
                imgDataCopy.getHasAlphaChannel()
            );
            ImageData imgDataG = new ImageData(
                imgDataCopy.getHeight(),
                imgDataCopy.getWidth(),
                imgDataCopy.getData(),
                imgDataCopy.getHasAlphaChannel()
            );
            ImageData imgDataB = new ImageData(
                imgDataCopy.getHeight(),
                imgDataCopy.getWidth(),
                imgDataCopy.getData(),
                imgDataCopy.getHasAlphaChannel()
            );

            // separate each copy of the pixellated ImageData into channels
            // by performing component-wise multiplication and addition to each
            // pixel (arguments one and two)
            separate(new Pixel(255, 0, 0), new Pixel(0, 255), imgDataR);
            separate(new Pixel(0, 255, 0), new Pixel(0, 255), imgDataG);
            separate(new Pixel(0, 0, 255), new Pixel(0, 255), imgDataB);

            // apply radiusing filter to each ImageData object and offset
            // depending on which function is called
            circleCenterOffset(imgDataCopy, size, offsets, Mode.RGB);
            circleTopOffset(imgDataR, size, offsets, Mode.RGB);
            circleLeftOffset(imgDataG, size, offsets, Mode.RGB);
            circleRightOffset(imgDataB, size, offsets, Mode.RGB);

            try {
                write(imgDataR, fileName + "-" + version + "-tri-" + i + "-R.png");
                write(imgDataG, fileName + "-" + version + "-tri-" + i + "-G.png");
                write(imgDataB, fileName + "-" + version + "-tri-" + i + "-B.png");
                write(imgDataCopy, fileName + "-" + version + "-tri-" + i + "-RGB-all.png");
            } catch (IOException e) {
                System.out.println(e);
            }

            // composite three channels and radiused original image using
            // lighten blend mode
            results[i] = compositeLighten(new ImageData[]{ imgDataCopy, imgDataR, imgDataG, imgDataB });

            try {
                write(results[i], fileName + "-" + version + "-tri-" + i + "-lighten.png");
            } catch (IOException e) {
                System.out.println(e);
            }

            imgDataR = null;
            imgDataG = null;
            imgDataB = null;
            imgDataCopy = null;
        }

        // alpha composite (using implicity increasing z-indices) results from
        // each level of resolution
        ImageData imgDataRes = compositeNormal(results);

        try {
            write(imgDataRes, fileName + "-" + version + "-tri-" + levels + "-RGB-normal.png");
        } catch (IOException e) {
            System.out.println(e);
        }

        System.out.println("compRGB finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // separates ImageData into channels using component-wise mult and add
    // pixels operations
    public static void separate(Pixel mult, Pixel add, ImageData imgData) {
        long start = System.currentTimeMillis();
        // map to apply componsnt-wise pixel operations on every pixel in
        // ImageData
        ImageData.Map map = (p) -> {
            p.setPixel(p.mult(mult).add(add));
        };
        imgData.applyMap(map);
        System.out.println("separate finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // pixellation filter that uses point sampling
    public static void pixellate(ImageData imgData, int size) {
        long start = System.currentTimeMillis();
        // map to pixellate ImageData according to size supplied
        ImageData.CoarseMap map = (dest, src, index) -> {
            dest.setPixel(src);
        };
        imgData.applyCoarseMap(map, size);
        System.out.println("pixellate finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // pixellation filter that uses uniformly weighted averaging over
    // pixellation area
    public static void pixellateAverage(ImageData imgData, int size) {
        long start = System.currentTimeMillis();
        // map to pixellate ImageData according to size supplied
        ImageData.CoarseMap map = (dest, src, index) -> {
            dest.setPixel(src);
        };
        imgData.applyCoarseMapWithAveraging(map, size);
        System.out.println("pixellate average finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // pixellation filter that uses point sampling and row offsetting
    public static int[] pixellateOffset(ImageData imgData, int size) {
        long start = System.currentTimeMillis();
        // map to pixellate ImageData according to size supplied
        ImageData.CoarseMap map = (dest, src, index) -> {
            dest.setPixel(src);
        };
        int[] offsets = imgData.applyOffsetCoarseMap(map, size);
        System.out.println("pixellate offset finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
        return offsets;
    }

    // pixellation filter that uses uniformly weighted averaging over
    // pixellation area and row offsetting
    public static int[] pixellateOffsetAverage(ImageData imgData, int size) {
        long start = System.currentTimeMillis();
        // map to pixellate ImageData according to size supplied
        ImageData.CoarseMap map = (dest, src, index) -> {
            dest.setPixel(src);
        };
        int[] offsets = imgData.applyOffsetCoarseMapWithAveraging(map, size);
        System.out.println("pixellate offset average finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
        return offsets;
    }

    // radiusing filter relative to center
    public static void circleCenter(ImageData imgData, int size, Mode mode) {
        long start = System.currentTimeMillis();
        // map that uses one-dimensional index to decide whether to set pixel
        // transparent
        ImageData.IndexedMap circleMap = (p, index) -> {
            // calculate x and y coordinates from one-dimensional index
            int[] index2D = imgData.index2D(index);
            // calculate position within pixellated area
            int xLoc = index2D[1] % size;
            int yLoc = index2D[0] % size;
            // calculate radius based on intensity of pixel and channel mode
            int radius = (int) (((double) size / 4) + ((double) size / 4) * ((mode == Mode.CMY) ? (1 - (double) p.getMin() / 255) : (double) p.getMax() / 255));
            if (Math.sqrt(Math.abs(xLoc - (double) size/2) * Math.abs(xLoc - (double) size/2) + Math.abs(yLoc - (double) size/2) * Math.abs(yLoc - (double) size/2)) > (double) radius) {
                // outside of radius, set to transparent
                p.setOpacity(0.0);
            } else if (Math.sqrt(Math.abs(xLoc - (double) size/2) * Math.abs(xLoc - (double) size/2) + Math.abs(yLoc - (double) size/2) * Math.abs(yLoc - (double) size/2)) > (double) radius - 1) {
                // outside of radius - 1, set to half opacity for anti-aliasing
                p.setOpacity(0.5);
            }
        };
        imgData.applyIndexedMap(circleMap);
        System.out.println("circle finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // radiusing filter relative to top center
    public static void circleTop(ImageData imgData, int size, Mode mode) {
        long start = System.currentTimeMillis();
        // map that uses one-dimensional index to decide whether to set pixel
        // transparent
        ImageData.IndexedMap circleMap = (p, index) -> {
            // calculate x and y coordinates from one-dimensional index
            int[] index2D = imgData.index2D(index);
            // calculate position within pixellated area
            int xLoc = index2D[1] % size;
            int yLoc = index2D[0] % size;
            // calculate radius based on intensity of pixel and channel mode
            int radius = (int) (((double) size / 4) + ((double) size / 4) * ((mode == Mode.CMY) ? (1 - (double) p.getMin() / 255) : (double) p.getMax() / 255));
            if (Math.sqrt(Math.abs(xLoc - (double) size/2) * Math.abs(xLoc - (double) size/2) + Math.abs(yLoc - radius) * Math.abs(yLoc - radius)) > (double) radius) {
                // outside of radius, set to transparent
                p.setOpacity(0.0);
            } else if (Math.sqrt(Math.abs(xLoc - (double) size/2) * Math.abs(xLoc - (double) size/2) + Math.abs(yLoc - radius) * Math.abs(yLoc - radius)) > (double) radius - 1) {
                // outside of radius - 1, set to half opacity for anti-aliasing
                p.setOpacity(0.5);
            }
        };
        imgData.applyIndexedMap(circleMap);
        System.out.println("circle finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // radiusing filter relative to bottom left
    public static void circleLeft(ImageData imgData, int size, Mode mode) {
        long start = System.currentTimeMillis();
        // map that uses one-dimensional index to decide whether to set pixel
        // transparent
        ImageData.IndexedMap circleMap = (p, index) -> {
            // calculate x and y coordinates from one-dimensional index
            int[] index2D = imgData.index2D(index);
            // calculate position within pixellated area
            int xLoc = index2D[1] % size;
            int yLoc = index2D[0] % size;
            // calculate radius based on intensity of pixel and channel mode
            int radius = (int) (((double) size / 4) + ((double) size / 4) * ((mode == Mode.CMY) ? (1 - (double) p.getMin() / 255) : (double) p.getMax() / 255));
            if (Math.sqrt(Math.abs(xLoc - radius) * Math.abs(xLoc - radius) + Math.abs(yLoc - (size - radius)) * Math.abs(yLoc - (size - radius))) > (double) radius) {
                // outside of radius, set to transparent
                p.setOpacity(0.0);
            } else if (Math.sqrt(Math.abs(xLoc - radius) * Math.abs(xLoc - radius) + Math.abs(yLoc - (size - radius)) * Math.abs(yLoc - (size - radius))) > (double) radius - 1) {
                // outside of radius - 1, set to half opacity for anti-aliasing
                p.setOpacity(0.5);
            }
        };
        imgData.applyIndexedMap(circleMap);
        System.out.println("circle finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // radiusing filter relative to bottom right
    public static void circleRight(ImageData imgData, int size, Mode mode) {
        long start = System.currentTimeMillis();
        // map that uses one-dimensional index to decide whether to set pixel
        // transparent
        ImageData.IndexedMap circleMap = (p, index) -> {
            // calculate x and y coordinates from one-dimensional index
            int[] index2D = imgData.index2D(index);
            // calculate position within pixellated area
            int xLoc = index2D[1] % size;
            int yLoc = index2D[0] % size;
            // calculate radius based on intensity of pixel and channel mode
            int radius = (int) (((double) size / 4) + ((double) size / 4) * ((mode == Mode.CMY) ? (1 - (double) p.getMin() / 255) : (double) p.getMax() / 255));
            if (Math.sqrt(Math.abs(xLoc - (size - radius)) * Math.abs(xLoc - (size - radius)) + Math.abs(yLoc - (size - radius)) * Math.abs(yLoc - (size - radius))) > (double) radius) {
                // outside of radius, set to transparent
                p.setOpacity(0.0);
            } else if (Math.sqrt(Math.abs(xLoc - (size - radius)) * Math.abs(xLoc - (size - radius)) + Math.abs(yLoc - (size - radius)) * Math.abs(yLoc - (size - radius))) > (double) radius - 1) {
                // outside of radius - 1, set to half opacity for anti-aliasing
                p.setOpacity(0.5);
            }
        };
        imgData.applyIndexedMap(circleMap);
        System.out.println("circle finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // offset radiusing filter relative to center
    public static void circleCenterOffset(ImageData imgData, int size, int[] offsets, Mode mode) {
        long start = System.currentTimeMillis();
        // map that uses one-dimensional index to decide whether to set pixel
        // transparent
        ImageData.IndexedMap circleMap = (p, index) -> {
            // calculate x and y coordinates from one-dimensional index
            int[] index2D = imgData.index2D(index);
            // calculate position within pixellated area
            int xLoc = index2D[1] % size;
            int yLoc = index2D[0] % size;
            // calculate radius based on intensity of pixel and channel mode
            int radius = (int) (((double) size / 4) + ((double) size / 4) * ((mode == Mode.CMY) ? (1 - (double) p.getMin() / 255) : (double) p.getMax() / 255));
            if (Math.sqrt(Math.abs(xLoc - (double) size/2) * Math.abs(xLoc - (double) size/2) + Math.abs(yLoc - (double) size/2) * Math.abs(yLoc - (double) size/2)) > (double) radius) {
                // outside of radius, set to transparent
                p.setOpacity(0.0);
            } else if (Math.sqrt(Math.abs(xLoc - (double) size/2) * Math.abs(xLoc - (double) size/2) + Math.abs(yLoc - (double) size/2) * Math.abs(yLoc - (double) size/2)) > (double) radius - 1) {
                // outside of radius - 1, set to half opacity for anti-aliasing
                p.setOpacity(0.5);
            }
        };
        imgData.applyIndexedMapWithOffsets(circleMap, size, offsets);
        System.out.println("circle finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // offset radiusing filter relative to top center
    public static void circleTopOffset(ImageData imgData, int size, int[] offsets, Mode mode) {
        long start = System.currentTimeMillis();
        // map that uses one-dimensional index to decide whether to set pixel
        // transparent
        ImageData.IndexedMap circleMap = (p, index) -> {
            // calculate x and y coordinates from one-dimensional index
            int[] index2D = imgData.index2D(index);
            // calculate position within pixellated area
            int xLoc = index2D[1] % size;
            int yLoc = index2D[0] % size;
            // calculate radius based on intensity of pixel and channel mode
            int radius = (int) (((double) size / 4) + ((double) size / 4) * ((mode == Mode.CMY) ? (1 - (double) p.getMin() / 255) : (double) p.getMax() / 255));
            if (Math.sqrt(Math.abs(xLoc - (double) size/2) * Math.abs(xLoc - (double) size/2) + Math.abs(yLoc - radius) * Math.abs(yLoc - radius)) > (double) radius) {
                // outside of radius, set to transparent
                p.setOpacity(0.0);
            } else if (Math.sqrt(Math.abs(xLoc - (double) size/2) * Math.abs(xLoc - (double) size/2) + Math.abs(yLoc - radius) * Math.abs(yLoc - radius)) > (double) radius - 1) {
                // outside of radius - 1, set to half opacity for anti-aliasing
                p.setOpacity(0.5);
            }
        };
        imgData.applyIndexedMapWithOffsets(circleMap, size, offsets);
        System.out.println("circle finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // offset radiusing filter relative to bottom left
    public static void circleLeftOffset(ImageData imgData, int size, int[] offsets, Mode mode) {
        long start = System.currentTimeMillis();
        // map that uses one-dimensional index to decide whether to set pixel
        // transparent
        ImageData.IndexedMap circleMap = (p, index) -> {
            // calculate x and y coordinates from one-dimensional index
            int[] index2D = imgData.index2D(index);
            // calculate position within pixellated area
            int xLoc = index2D[1] % size;
            int yLoc = index2D[0] % size;
            // calculate radius based on intensity of pixel and channel mode
            int radius = (int) (((double) size / 4) + ((double) size / 4) * ((mode == Mode.CMY) ? (1 - (double) p.getMin() / 255) : (double) p.getMax() / 255));
            if (Math.sqrt(Math.abs(xLoc - radius) * Math.abs(xLoc - radius) + Math.abs(yLoc - (size - radius)) * Math.abs(yLoc - (size - radius))) > (double) radius) {
                // outside of radius, set to transparent
                p.setOpacity(0.0);
            } else if (Math.sqrt(Math.abs(xLoc - radius) * Math.abs(xLoc - radius) + Math.abs(yLoc - (size - radius)) * Math.abs(yLoc - (size - radius))) > (double) radius - 1) {
                // outside of radius - 1, set to half opacity for anti-aliasing
                p.setOpacity(0.5);
            }
        };
        imgData.applyIndexedMapWithOffsets(circleMap, size, offsets);
        System.out.println("circle finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // offset radiusing filter relative to bottom right
    public static void circleRightOffset(ImageData imgData, int size, int[] offsets, Mode mode) {
        long start = System.currentTimeMillis();
        // map that uses one-dimensional index to decide whether to set pixel
        // transparent
        ImageData.IndexedMap circleMap = (p, index) -> {
            // calculate x and y coordinates from one-dimensional index
            int[] index2D = imgData.index2D(index);
            // calculate position within pixellated area
            int xLoc = index2D[1] % size;
            int yLoc = index2D[0] % size;
            // calculate radius based on intensity of pixel and channel mode
            int radius = (int) (((double) size / 4) + ((double) size / 4) * ((mode == Mode.CMY) ? (1 - (double) p.getMin() / 255) : (double) p.getMax() / 255));
            if (Math.sqrt(Math.abs(xLoc - (size - radius)) * Math.abs(xLoc - (size - radius)) + Math.abs(yLoc - (size - radius)) * Math.abs(yLoc - (size - radius))) > (double) radius) {
                // outside of radius, set to transparent
                p.setOpacity(0.0);
            } else if (Math.sqrt(Math.abs(xLoc - (size - radius)) * Math.abs(xLoc - (size - radius)) + Math.abs(yLoc - (size - radius)) * Math.abs(yLoc - (size - radius))) > (double) radius - 1) {
                // outside of radius - 1, set to half opacity for anti-aliasing
                p.setOpacity(0.5);
            }
        };
        imgData.applyIndexedMapWithOffsets(circleMap, size, offsets);
        System.out.println("circle finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }

    // compositing filter using darken blend mode
    public static ImageData compositeDarken(ImageData[] imgData) {
        long start = System.currentTimeMillis();
        // create empty ImageData object as destination for compositing
        ImageData imgDataRes = new ImageData(
            imgData[0].getHeight(),
            imgData[0].getWidth(),
            imgData[0].getHasAlphaChannel()
        );
        // map that composites images, storing result in destination ImageData
        // created above
        ImageData.IndexedMap map = (p, index) -> {
            // calculate two-dimensional index to retreive corresponding pixel
            // from other image
            int[] index2D = imgDataRes.index2D(index);
            // composite pixels at index from each ImageData object
            for (int i = 0; i < imgData.length; i++) {
                Pixel otherPixel = imgData[i].getData()[index2D[0]][index2D[1]];
                if (i == 0) {
                    // copy first pixel over
                    p.setPixel(otherPixel);
                } else {
                    // composite other pixels with the pixel in destination
                    // ImageData object
                    p.setPixel(p.blendDarken(otherPixel));
                }
            }
        };
        imgDataRes.applyIndexedMap(map);

        System.out.println("composite darken finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
        return imgDataRes;
    }

    // compositing filter using lighten blend mode
    public static ImageData compositeLighten(ImageData[] imgData) {
        long start = System.currentTimeMillis();
        // create empty ImageData object as destination for compositing
        ImageData imgDataRes = new ImageData(
            imgData[0].getHeight(),
            imgData[0].getWidth(),
            imgData[0].getHasAlphaChannel()
        );
        // map that composites images, storing result in destination ImageData
        // created above
        ImageData.IndexedMap map = (p, index) -> {
            // calculate two-dimensional index to retreive corresponding pixel
            // from other image
            int[] index2D = imgDataRes.index2D(index);
            // composite pixels at index from each ImageData object
            for (int i = 0; i < imgData.length; i++) {
                Pixel otherPixel = imgData[i].getData()[index2D[0]][index2D[1]];
                if (i == 0) {
                    // copy first pixel over
                    p.setPixel(otherPixel);
                } else {
                    // composite other pixels with the pixel in destination
                    // ImageData object
                    p.setPixel(p.blendLighten(otherPixel));
                }
            }
        };
        imgDataRes.applyIndexedMap(map);

        System.out.println("composite lighten finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
        return imgDataRes;
    }

    // compositing filter using normal blend mode
    public static ImageData compositeNormal(ImageData[] imgData) {
        long start = System.currentTimeMillis();
        // create empty ImageData object as destination for compositing
        ImageData imgDataRes = new ImageData(
            imgData[0].getHeight(),
            imgData[0].getWidth(),
            imgData[0].getHasAlphaChannel()
        );
        // map that composites images, storing result in destination ImageData
        // created above
        ImageData.IndexedMap map = (p, index) -> {
            // calculate two-dimensional index to retreive corresponding pixel
            // from other image
            int[] index2D = imgDataRes.index2D(index);
            for (int i = 0; i < imgData.length; i++) {
                if (imgData[i] != null) {
                    Pixel otherPixel = imgData[i].getData()[index2D[0]][index2D[1]];
                    if (i == 0) {
                        // copy first pixel over
                        p.setPixel(otherPixel);
                    } else {
                        // composite other pixels with the pixel in destination
                        // ImageData object
                        p.setPixel(p.blendNormal(otherPixel));
                    }
                }
            }
        };
        imgDataRes.applyIndexedMap(map);

        System.out.println("composite normal finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
        return imgDataRes;
    }

    // utility function for writing images with descriptive filenames
    public static void write(ImageData imgData, String fileName) throws IOException {
        long start = System.currentTimeMillis();
        // create BufferedImage object as destiantion
        BufferedImage outImg = new BufferedImage(
            imgData.getWidth(),
            imgData.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        // marshall image object to buffered iamge object
        imgData.toImage(outImg);
        // write file
        File outFile = new File(fileName);
        ImageIO.write(outImg, "png", outFile);
        System.out.println("write finished: " + (((double) System.currentTimeMillis() - start) / 1000) + "s");
    }
}

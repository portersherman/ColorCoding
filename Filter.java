import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Filter {

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("please supply filename as first argument and filter as second argument");
            System.out.println("alternatively, type 'help' as first argument for a list of available filters");
            return;
        }

        if (args[0].equals("help")) {
            System.out.println(" - SeparateRGB: decompose image into RGB channels");
            System.out.println(" - SeparateCMY: decompose image into CMY channels");
            System.out.println(" - Checker: checker with white pixels");
            System.out.println(" - Pixellate <size>: pixellate image with coarseness proportional to size");
            return;
        }

        BufferedImage inImg = ImageIO.read(new File(args[0]));

        if (args[1].equals("SeparateRGB")) {
            separateRGB(inImg);
            return;
        }

        if (args[1].equals("SeparateCMY")) {
            separateCMY(inImg);
            return;
        }

        if (args[1].equals("Checker")) {
            checker(inImg);
            return;
        }

        if (args[1].equals("Pixellate")) {
            pixellate(inImg, Integer.parseInt(args[2]));
            return;
        }
    }

    public static void checker(BufferedImage inImg) throws IOException {
        ImageData imgData = new ImageData(inImg);

        ImageData.IndexedMap indexedMap = (p, x, y) -> p.add(((x + y) % 2 == 0) ? new Pixel(255, 255, 255, 255) : new Pixel(0, 0, 0, 255));
        imgData.applyIndexedMap(indexedMap);

        BufferedImage outImg = new BufferedImage(
            inImg.getWidth(),
            inImg.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        imgData.toImage(outImg);
        File outFile = new File("outChecker.png");
        ImageIO.write(outImg, "png", outFile);
    }

    public static void pixellate(BufferedImage inImg, int size) throws IOException {
        ImageData imgData = new ImageData(inImg);

        ImageData.CoarseMap map = (dest, src) -> dest.setPixel(src);
        imgData.applyCoarseMap(map, size);

        BufferedImage outImg = new BufferedImage(
            inImg.getWidth(),
            inImg.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        imgData.toImage(outImg);
        File outFile = new File("outPixellate.png");
        ImageIO.write(outImg, "png", outFile);
    }

    public static void separateRGB(BufferedImage inImg) throws IOException {
        ImageData imgDataR = new ImageData(inImg);
        ImageData imgDataG = new ImageData(inImg);
        ImageData imgDataB = new ImageData(inImg);

        ImageData.Map mapR = p -> p.mult(new Pixel(255, 0, 0, 255));
        imgDataR.applyMap(mapR);

        ImageData.Map mapG = p -> p.mult(new Pixel(0, 255, 0, 255));
        imgDataG.applyMap(mapG);

        ImageData.Map mapB = p -> p.mult(new Pixel(0, 0, 255, 255));
        imgDataB.applyMap(mapB);

        BufferedImage outImgR = new BufferedImage(
            inImg.getWidth(),
            inImg.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        BufferedImage outImgG = new BufferedImage(
            inImg.getWidth(),
            inImg.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        BufferedImage outImgB = new BufferedImage(
            inImg.getWidth(),
            inImg.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        imgDataR.toImage(outImgR);
        imgDataG.toImage(outImgG);
        imgDataB.toImage(outImgB);

        File outFileR = new File("outR.png");
        File outFileG = new File("outG.png");
        File outFileB = new File("outB.png");
        ImageIO.write(outImgR, "png", outFileR);
        ImageIO.write(outImgG, "png", outFileG);
        ImageIO.write(outImgB, "png", outFileB);
    }

    public static void separateCMY(BufferedImage inImg) throws IOException {
        ImageData imgDataC = new ImageData(inImg);
        ImageData imgDataM = new ImageData(inImg);
        ImageData imgDataY = new ImageData(inImg);

        ImageData.Map mapC = p -> p.mult(new Pixel(0, 255, 255, 255));
        imgDataC.applyMap(mapC);

        ImageData.Map mapM = p -> p.mult(new Pixel(255, 0, 255, 255));
        imgDataM.applyMap(mapM);

        ImageData.Map mapY = p -> p.mult(new Pixel(255, 255, 0, 255));
        imgDataY.applyMap(mapY);

        BufferedImage outImgC = new BufferedImage(
            inImg.getWidth(),
            inImg.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        BufferedImage outImgM = new BufferedImage(
            inImg.getWidth(),
            inImg.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        BufferedImage outImgY = new BufferedImage(
            inImg.getWidth(),
            inImg.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        imgDataC.toImage(outImgC);
        imgDataM.toImage(outImgM);
        imgDataY.toImage(outImgY);

        File outFileC = new File("outC.png");
        File outFileM = new File("outM.png");
        File outFileY = new File("outY.png");
        ImageIO.write(outImgC, "png", outFileC);
        ImageIO.write(outImgM, "png", outFileM);
        ImageIO.write(outImgY, "png", outFileY);
    }
}

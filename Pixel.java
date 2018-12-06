/*----------------------------------------------------------------------------*/
/*
/* File: Pixel.java
/*
/* Pixel objects store RGBA values and provide methods to implement useful
/* operations.
/*
/* Author: Porter Sherman
/*
/*----------------------------------------------------------------------------*/

public class Pixel{

    // RGBA values
    public int r;
    public int g;
    public int b;
    public int a;

    // constructor
    public Pixel(int red, int green, int blue, int alpha) {
        r = red;
        g = green;
        b = blue;
        a = alpha;
    }

    // constructor that defaults alpha to 255
    public Pixel(int red, int green, int blue) {
        r = red;
        g = green;
        b = blue;
        a = 255;
    }

    // constructor of grayscale pixel
    public Pixel(int k, int alpha) {
        r = k;
        g = k;
        b = k;
        a = alpha;
    }

    // constructor of grayscale pixel that defaults alpha to 255
    public Pixel(int k) {
        r = k;
        g = k;
        b = k;
        a = 255;
    }

    // deep copy of pixel
    public Pixel copy() {
        return new Pixel(this.r, this.g, this.b, this.a);
    }

    // getter for r
    public int getR() {
        return this.r;
    }

    // getter for g
    public int getG() {
        return this.g;
    }

    // getter for b
    public int getB() {
        return this.b;
    }

    // get minimum value of RGB components
    public int getMin() {
        return Math.min(this.r, Math.min(this.g, this.b));
    }

    // get maximum value of RGB components
    public int getMax() {
        return Math.max(this.r, Math.max(this.g, this.b));
    }

    // calculate saturation of pixel according to saturation formula
    public double calcSaturation() {
        int max = Math.max(this.r, Math.max(this.g, this.b));
        int min = Math.min(this.r, Math.min(this.g, this.b));
        if (max == min) {
            return 0.0;
        } else if ((double) (max + min) / (2 * 255) > 0.5) {
            return (double) (max - min) / (2 * 255 - max - min);
        } else {
            return (double) (max - min) / (max + min);
        }
    }

    // component-wise add
    public Pixel add(Pixel p) {
        int r = this.r + p.r;
        int g = this.g + p.g;
        int b = this.b + p.b;
        return new Pixel(r, g, b, this.a);
    }

    // component-wise multiplication with normalization
    public Pixel mult(Pixel p) {
        int r = (this.r * p.r) / 255;
        int g = (this.g * p.g) / 255;
        int b = (this.b * p.b) / 255;
        return new Pixel(r, g, b, this.a);
    }

    // multiplication of every value with scalar
    public Pixel multScalar(int scl) {
        int r = this.r * scl;
        int g = this.g * scl;
        int b = this.b * scl;
        return new Pixel(r, g, b, this.a);
    }

    // multiplication of every value with scalar
    public Pixel multScalar(double scl) {
        int r = (int) (this.r * scl);
        int g = (int) (this.g * scl);
        int b = (int) (this.b * scl);
        return new Pixel(r, g, b, this.a);
    }

    // blend according to darken blend mode
    public Pixel blendDarken(Pixel p) {
        // if either pixel is transparent, return other pixel
        if (p.a == 0) {
            return new Pixel(this.r, this.g, this.b, this.a);
        }
        if (this.a == 0) {
            return new Pixel(p.r, p.g, p.b, p.a);
        }
        // choose minumum of every pixel and return resulting pixel
        int r = Math.min(this.r, p.r);
        int g = Math.min(this.g, p.g);
        int b = Math.min(this.b, p.b);
        Pixel newPixel = new Pixel(r, g, b, (this.a + p.a) / 2);
        newPixel.clamp();
        return newPixel;
    }

    // blend according to lighten blend mode
    public Pixel blendLighten(Pixel p) {
        // if either pixel is transparent, return other pixel
        if (p.a == 0) {
            return new Pixel(this.r, this.g, this.b, this.a);
        }
        if (this.a == 0) {
            return new Pixel(p.r, p.g, p.b, p.a);
        }
        // choose maxiumum of every pixel and return resulting pixel
        int r = Math.max(this.r, p.r);
        int g = Math.max(this.g, p.g);
        int b = Math.max(this.b, p.b);
        Pixel newPixel = new Pixel(r, g, b, (this.a + p.a) / 2);
        newPixel.clamp();
        return newPixel;
    }

    // OUTDATED - blend according to darken blend mode
    public Pixel blendDarkenAll(Pixel p) {
        // choose minumum of every pixel and return resulting pixel
        int r = Math.min(this.r, p.r);
        int g = Math.min(this.g, p.g);
        int b = Math.min(this.b, p.b);
        Pixel newPixel = new Pixel(r, g, b, (this.a + p.a) / 2);
        newPixel.clamp();
        return newPixel;
    }

    // OUTDATED - blend that returns pixel walled to most intense value
    public Pixel blendSelect(Pixel p) {
        int thisMin = Math.min(this.r, Math.min(this.g, this.b));
        int pMin = Math.min(p.r, Math.min(p.g, p.b));
        Pixel newPixel = null;
        if (thisMin < pMin) {
            if (this.r < this.g && this.r < this.b) {
                newPixel = new Pixel(0, 255, 255, (this.a + p.a) / 2);
            } else if (this.g < this.r && this.g < this.b) {
                newPixel = new Pixel(255, 0, 255, (this.a + p.a) / 2);
            } else if (this.b < this.r && this.b < this.g) {
                newPixel = new Pixel(255, 255, 0, (this.a + p.a) / 2);
            }
        } else {
            if (p.r < p.g && p.r < p.b) {
                newPixel = new Pixel(0, 255, 255, (this.a + p.a) / 2);
            } else if (p.g < p.r && p.g < p.b) {
                newPixel = new Pixel(255, 0, 255, (this.a + p.a) / 2);
            } else if (p.b < p.r && p.b < p.g) {
                newPixel = new Pixel(255, 255, 0, (this.a + p.a) / 2);
            }
        }
        if (newPixel != null) {
            newPixel.clamp();
            return newPixel;
        }

        return new Pixel(0, 0, 0, (this.a + p.a) / 2);
    }

    // blend accordign to z-index and alpha (argument given higher z)
    public Pixel blendNormal(Pixel p) {
        int r = (int) (p.r * (double) p.a / 255 + this.r * (1 - (double) p.a / 255) * (double) this.a / 255);
        int g = (int) (p.g * (double) p.a / 255 + this.g * (1 - (double) p.a / 255) * (double) this.a / 255);
        int b = (int) (p.b * (double) p.a / 255 + this.b * (1 - (double) p.a / 255) * (double) this.a / 255);
        Pixel newPixel = new Pixel(r, g, b, (int) (p.a + this.a * (1 - (double) p.a / 255)));
        newPixel.clamp();
        return newPixel;
    }

    // OUTDATED - wall pixel to most intense RGB value
    public Pixel maxRGB() {
        int r = (this.r > this.g) && (this.r > this.b) ? 255 : 0;
        int g = (this.g > this.r) && (this.g > this.b) ? 255 : 0;
        int b = (this.b > this.r) && (this.b > this.g) ? 255 : 0;
        return new Pixel(r, g, b, this.a);
    }

    // OUTDATED - wall pixel to most intense CMY value
    public Pixel maxCMY() {
        int r = ((this.r < this.g) && (this.r < this.b)) ? 0 : 255;
        int g = ((this.g < this.r) && (this.g < this.b)) ? 0 : 255;
        int b = ((this.b < this.r) && (this.b < this.g)) ? 0 : 255;
        return new Pixel(r, g, b, this.a);
    }

    // calculate luminance
    public int getLum() {
        return (int) Math.floor(0.2126 * this.r + 0.7152 * this.g + 0.0722 * this.b);
    }

    // setter for RGBA values
    public void setPixel(Pixel src) {
        this.r = src.r;
        this.g = src.g;
        this.b = src.b;
        this.a = src.a;
    }

    // setter for alpha value
    public void setOpacity(double opacity) {
        this.a = (int) (this.a * opacity);
    }

    // utility stringifying function
    public String toString() {
        return "( " + this.r + ", " + this.g + ", " + this.b + ", " + (int) this.a + " )";
    }

    // utility function to convert to int for marshalling
    public int toInt() {
        return (this.a << 24) | (this.r << 16) | (this.g << 8) | this.b;
    }

    // utility function to convert to int without alpha for marshalling
    public int toIntNoAlpha() {
        return (this.r << 16) | (this.g << 8) | this.b;
    }

    // clamp function to keep RGBA values in gamut
    public void clamp() {
        this.r = (this.r > 255) ? 255 : (this.r < 0) ? 0 : this.r;
        this.g = (this.g > 255) ? 255 : (this.g < 0) ? 0 : this.g;
        this.b = (this.b > 255) ? 255 : (this.b < 0) ? 0 : this.b;
        this.a = (this.a > 255) ? 255 : (this.a < 0) ? 0 : this.a;
    }
}

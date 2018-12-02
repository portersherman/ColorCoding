# Color Coding

## Comp.java

Comp implements an iterative image filter based on the decomposition and
reconstruction of the input image. The effect makes use of a number of
transformations. At progressively finer resolutions, the input image is
pixellated, then separated out into RGB and CMY channels. Then, each
pixellation in each channel is radiused according to the luminosity of the
pixellation, and offset from center. These circles are then overlaid,
creating the familiar patterns that connote color spaces. The overlaying is
performed with either a lighten or darken blend mode, depending on the color
space. After all levels have been computed, the results are composited
normally.

## ImageData.java

ImageData objects store a two-dimensional array of Pixel objects, allowing
for more straightforward filtering operations on the pixel data than provided
by the BufferedImage data type.

## Pixel.java

Pixel objects store RGBA values and provide methods to implement useful
operations.

## Building

Build from CLI using command: `javac Comp.java`
Tested using Java 1.8

## Running

Run from CLI using command: `java Comp <filename> <depth> <version string>`

Author: Porter Sherman

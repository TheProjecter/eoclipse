/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.metaaps.eoclipse.imagereaders.utils;

import com.vividsolutions.jts.geom.Geometry;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Interface that handles the methods to mask part of the raster
 * @author thoorfr
 */
public interface IMask {

    public boolean intersects(int x, int y, int width, int height);
    public boolean contains(int x, int y);
    public boolean includes(int x, int y, int width, int height);
    public BufferedImage rasterize(Rectangle rect, int offsetX, int offsetY, double scalingFactor);
    public Area getShape();
    public void buffer(double bufferingDistance);
    public IMask createBufferedMask(double bufferingDistance);
    public List<Geometry> getGeometries();

}

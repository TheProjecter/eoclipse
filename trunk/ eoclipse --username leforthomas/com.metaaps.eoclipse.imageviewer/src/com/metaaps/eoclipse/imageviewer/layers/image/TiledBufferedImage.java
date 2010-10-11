/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.metaaps.eoclipse.imageviewer.layers.image;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.metaaps.eoclipse.common.datasets.DataContent;
import com.metaaps.eoclipse.common.datasets.GCP;
import com.metaaps.eoclipse.common.datasets.IGeoRaster;
import com.metaaps.eoclipse.common.datasets.IGeoTransform;


/**
 * In order to save RAM, this Object manage the raster you may change or do whatever with it
 * @author thoorfr
 */
public class TiledBufferedImage extends DataContent implements IGeoRaster {

    private int[] preloadedInterval;
    private final int xSize;
    private final int ySize;
    private int[] preloadedData;
    private final Rectangle bounds;
    private final File root;
    private static int tilesize = 512;
    private HashMap<String, File> tiles = new HashMap<String, File>();
    private final int nXTiles;
    private final int nYTiles;
    private IGeoRaster gir;
    private MappedByteBuffer writingTile = null;
    private File writingFile = null;
    private RandomAccessFile writingFis;

    public TiledBufferedImage(File rootDirectory, IGeoRaster gir) {
        this.xSize = gir.getWidth();
        this.ySize = gir.getHeight();
        this.bounds = new Rectangle(0, 0, xSize, ySize);
        this.root = rootDirectory;
        this.root.mkdirs();
        this.nXTiles = xSize / tilesize + 1;
        this.nYTiles = ySize / tilesize + 1;
        this.gir = gir;
        preloadedInterval = new int[]{-1, -1};
        mapExistingTiles();
    }

    public int[] readTile(int x, int y, int width, int height) {
        return readTile(x, y, width, height, new int[width * height]);
    }

    public int[] readTile(int x, int y, int width, int height, int[] tile) {
        Rectangle rect = new Rectangle(x, y, width, height);
        rect = rect.intersection(bounds);
        if (rect.isEmpty()) {
            return tile;
        }
        if (rect.y != preloadedInterval[0] || rect.y + rect.height != preloadedInterval[1]) {
            preloadLineTile(rect.y, rect.height);
        }
        int xinit = rect.x - x;
        int yinit = rect.y - y;
        for (int i = 0; i < rect.height; i++) {
            for (int j = 0; j < rect.width; j++) {
                int temp = i * xSize + j + rect.x;
                tile[(i + yinit) * width + j + xinit] = preloadedData[temp];
            }
        }
        return tile;
    }

    private ByteBuffer writeTileFile(int xx, int yy, int[] data) {
        FileOutputStream fos = null;
        ByteBuffer out = null;
        try {
            File f = new File(root, gir.getBand() + "_" + xx + "_" + yy);
            f.createNewFile();
            fos = new FileOutputStream(f);
            out = ByteBuffer.allocate(4 * data.length);
            IntBuffer ib = out.asIntBuffer();
            ib.put(data);
            fos.getChannel().write(out);
            tiles.put(gir.getBand() + "_" + xx + "_" + yy, f);
        } catch (Exception ex) {
            Logger.getLogger(TiledBufferedImage.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(TiledBufferedImage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        out.rewind();
        return out;
    }

    public void writeTile(int x, int y, int width, int height, int[] data) throws IOException {
        for (int j = y; j < y + height; j++) {
            for (int i = x; i < x + width; i++) {
                write(i, j, data[j * width + i]);
            }
        }
    }

    public void applyScaleFactor(double scale, double offset) {
//        File f = null;
        RandomAccessFile fis = null;
        MappedByteBuffer tile = null;
        for (int y = 0; y < gir.getHeight() / tilesize; y++) {
            for (int x = 0; x < gir.getHeight() / tilesize; x++) {
                try {
//                    f = tiles.get(getBand() + "_" + x + "_" + y);
                    fis = new RandomAccessFile(writingFile, "rwd");
                    tile = fis.getChannel().map(MapMode.READ_WRITE, 0, writingFile.length()).load();
                    for (int yt = 0; yt < tilesize; yt++) {
                        for (int xt = 0; xt < tilesize; xt++) {
                            tile.mark();
                            int val=(int) (scale * tile.getInt() + offset);
                            tile.flip();
                            tile.putInt(val);
                        }
                    }
                    tile.force();
                    fis.close();
                } catch (Exception ex) {
                    Logger.getLogger(TiledBufferedImage.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void write(int x, int y, int value) throws IOException {
        int xx = x / tilesize;
        int yy = y / tilesize;
        if (writingFile == null || !writingFile.getAbsolutePath().endsWith(getBand() + "_" + xx + "_" + yy)) {
            if (writingTile != null) {
                writingTile.force();
                writingFis.close();
            }
            writingFile = tiles.get(getBand() + "_" + xx + "_" + yy);
            if (writingFile == null) {
                writeTileFile(xx, yy, gir.readTile(xx * tilesize, yy * tilesize, tilesize, tilesize));
                writingFile = tiles.get(getBand() + "_" + xx + "_" + yy);
                if (writingFile == null) {
                    throw new IOException("can't write the tile");
                }
            }
            writingFis = new RandomAccessFile(writingFile, "rwd");
            writingTile = writingFis.getChannel().map(MapMode.READ_WRITE, 0, writingFile.length()).load();
            writingTile.limit((int) writingFile.length());
        }
        int position = 4 * ((x % tilesize) + (y % tilesize) * tilesize);
        //System.out.println(writingTile.limit());
        writingTile.putInt(position, value);
    }

    public void flush() {
        if (writingTile != null) {
            writingTile.force();
        }
    }

    public void preloadLineTile(int y, int height) {
        preloadedInterval = new int[]{y, y + height};
        preloadedData = new int[xSize * height];
        int[] tile = new int[tilesize * tilesize];
        MappedByteBuffer bb;
        try {
            int col = 0;
            for (int yy = y / tilesize; yy < ((y + height) / tilesize) + 1; yy++) {
                if (yy < 0) {
                    continue;
                }
                for (int xx = 0; xx < nXTiles; xx++) {
                    File f = tiles.get(getBand() + "_" + xx + "_" + yy);
                    if (f == null) {
                        int[] data = gir.readTile(xx * tilesize, yy * tilesize, tilesize, tilesize);
                        writeTileFile(xx, yy, data);
                        f = tiles.get(getBand() + "_" + xx + "_" + yy);
                    }

                    //System.out.println(f.getAbsolutePath());

                    if (col == 0) {
                        FileInputStream fis = new FileInputStream(f);
                        long startpointer = (y % tilesize) * tilesize * 4;
                        bb = fis.getChannel().map(MapMode.READ_ONLY, startpointer, f.length() - startpointer).load();
                        fis.close();
                        bb.rewind();
                        int aa = 0;
                        while (bb.hasRemaining()) {
                            tile[aa] = bb.getInt();
                            aa++;
                        }
                        for (int j = 0; j < tilesize - y % tilesize; j++) {
                            int temp = (col + j) * xSize + xx * tilesize;
                            for (int i = 0; i < tilesize; i++) {
                                try {
                                    preloadedData[temp + i] = tile[j * tilesize + i];
                                } catch (Exception e) {
                                    //System.out.println(e);
                                }
                            }
                        }
                    } else {
                        FileInputStream fis = new FileInputStream(f);
                        long endpointer = ((y + height) % tilesize) * tilesize * 4;
                        bb = fis.getChannel().map(MapMode.READ_ONLY, 0, endpointer).load();
                        fis.close();
                        bb.rewind();
                        int aa = 0;
                        while (bb.hasRemaining()) {
                            tile[aa] = bb.getInt();
                            aa++;
                        }
                        for (int j = 0; j < (y + height) % tilesize; j++) {
                            int temp = (col + j - (y % tilesize)) * xSize + xx * tilesize;
                            for (int i = 0; i < tilesize; i++) {
                                try {
                                    preloadedData[temp + i] = tile[j * tilesize + i];
                                } catch (Exception e) {
                                    //System.out.println(e);
                                }
                            }
                        }
                    }
                }
                col += tilesize;
            }
        } catch (IOException e) {
            Logger.getLogger(IGeoRaster.class.getName()).log(Level.SEVERE, "cannot preload the line tile", e);
        }

    }

    private void mapExistingTiles() {
        for (int yy = 0; yy < nYTiles; yy++) {
            for (int xx = 0; xx < nXTiles; xx++) {
                for (int b = 0; b < getNBand(); b++) {
                    File f = new File(root, b + "_" + xx + "_" + yy);
                    if (f.exists()) {
                        tiles.put(b + "_" + xx + "_" + yy, f);
                    }
                }
            }
        }
    }

    public int[] readAndDecimateTile(int x, int y, int width, int height, int outWidth, int outHeight, boolean filter) {
        if (height < 257) {
            int[] outData = new int[outWidth * outHeight];
            int[] data = readTile(x, y, width, height);
            int decX = Math.round(width / (1f * outWidth));
            int decY = Math.round(height / (1f * outHeight));
            if (data != null) {
                int index = 0;
                for (int j = 0; j < outHeight; j++) {
                    int temp = (int) (j * decY) * width;
                    for (int i = 0; i < outWidth; i++) {
                        if (filter) {
                            for (int h = 0; h < decY; h++) {
                                for (int w = 0; w < decX; w++) {
                                    outData[index] += data[temp + h * width + (int) (i * decX + w)];
                                }
                            }
                            if (decX > 1) {
                                outData[index] /= (int) decX;
                            }
                            if (decY > 1) {
                                outData[index] /= (int) decY;
                            }
                        } else {
                            outData[index] = data[temp + (int) (i * decX)];
                        }
                        index++;
                    }
                }
            }
            return outData;
        } else {
            float incy = height / 256f;
            int[] outData = new int[outWidth * outHeight];
            float decY = height / (1f * outHeight);
            int index = 0;
            for (int i = 0; i < Math.ceil(incy); i++) {
                int tileHeight = (int) Math.min(256, height - i * 256);
                if (tileHeight > decY) {
                    int[] temp = readAndDecimateTile(x, y + i * 256, width, tileHeight, outWidth, Math.round(tileHeight / decY), filter);
                    if (temp != null) {
                        for (int j = 0; j < temp.length; j++) {
                            if (index < outData.length) {
                                outData[index++] = temp[j];
                            }
                        }
                    } else {
                        index += outWidth * (int) (256 / decY);
                    }
                }
            }
            return outData;
        }
    }

    public void setBand(int band) {
        gir.setBand(band);
        preloadedInterval = new int[]{-1, -1};
    }

    public int getBand() {
        return gir.getBand();
    }

    public List<double[]> getFrameLatLon() {
        return gir.getFrameLatLon();
    }

    public String getName() {
        return gir.getName();
    }

    public int getWidth() {
        return gir.getWidth();
    }

    public int getHeight() {
        return gir.getHeight();
    }

    public int getNBand() {
        return gir.getNBand();
    }

    public String getFormat() {
        return gir.getFormat();
    }

    public String getDescription() {
        return "" + this.hashCode();
    }

    public int getNumberOfBytes() {
        return gir.getNumberOfBytes();
    }

    public int getType(boolean oneBand) {
        return gir.getType(oneBand);
    }

    public IGeoTransform getGeoTransform() {
        return gir.getGeoTransform();
    }

    public List<GCP> getGcps() {
        return gir.getGcps();
    }

    public String getAccessRights() {
        return gir.getAccessRights();
    }

    public boolean initialise() {
        return gir.initialise();
    }

    public int read(int x, int y) {
        return readTile(x, y, 1, 1)[0];
    }

    public String getBandName(int band) {
        return gir.getBandName(band);
    }

    public void dispose() {
        gir.dispose();
        gir = null;
        root.delete();
    }

    public int[] readAndDecimateTile(int x, int y, int width, int height, double scalingFactor, boolean filter) {
        return gir.readAndDecimateTile(x, y, width, height, scalingFactor, filter);
    }

    @Override
    public IGeoRaster clone() {
        return new TiledBufferedImage(root, gir.clone());
    }

    @Override
    public HashMap<String, Object> getProperties() {
    	// TODO Auto-generated method stub
    	return null;
    }

	@Override
	public String getDataFormat() {
		// TODO Auto-generated method stub
		return "TIFF_ALL_ALL_ALL";
	}
    
}

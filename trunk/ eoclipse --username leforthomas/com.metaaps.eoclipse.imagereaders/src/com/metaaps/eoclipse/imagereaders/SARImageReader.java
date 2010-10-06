/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.metaaps.eoclipse.imagereaders;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.referencing.GeodeticCalculator;

import com.metaaps.eoclipse.common.datasets.DataContent;
import com.metaaps.eoclipse.common.datasets.GCP;
import com.metaaps.eoclipse.common.datasets.IGeoRaster;
import com.metaaps.eoclipse.common.datasets.ISatelliteMetadata;
import com.metaaps.eoclipse.common.datasets.IGeoTransform;
import com.metaaps.eoclipse.common.datasets.ISourceDataContent;
import com.metaaps.eoclipse.common.datasets.ISARMetadata;

/**
 * this is a class that implememts default method to access raster data.
 * Your own reader should extends this class in most cases:
 * class MySarReader extends SARImageReader { ... }
 * @author leforth
 */
public abstract class SARImageReader extends DataContent implements IGeoRaster, ISatelliteMetadata, ISARMetadata, ISourceDataContent {

    protected static int MAXTILESIZE = 16 * 1024 * 1024;
    protected int xSize = -1;
    protected int ySize = -1;
    protected String name = "";
    protected List<GCP> gcps;
    private HashMap<String, Object> metadata = new HashMap<String, Object>();
    protected IGeoTransform geotransform;
    protected int band = 0;

    public int getBand() {
        return this.band;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return xSize;
    }

    public int getHeight() {
        return ySize;
    }

    public HashMap<String, Object> getMetadata() {
        return (HashMap<String, Object>) metadata.clone();
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public List<GCP> getGcps() {
        return gcps;
    }

    public IGeoTransform getGeoTransform() {
        return geotransform;
    }

    public int[] readAndDecimateTile(int x, int y, int width, int height, int outWidth, int outHeight, boolean filter) {
        if (x + width < 0 || y + height < 0 || x > xSize || y > ySize) {
            return new int[outWidth * outHeight];
        }

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
                    temp = null;
                    //System.gc();
                }
            }
            return outData;
        }
    }

    public int[] readAndDecimateTile(int x, int y, int width, int height, double scalingFactor, boolean filter) {
        System.out.println("readAndDecimateTile(" + x + ", " + y + ", " + width + ", " + height + ")");
        int outWidth = (int) (width * scalingFactor);
        int outHeight = (int) (height * scalingFactor);
        double deltaPixelsX = (double) width / outWidth;
        double deltaPixelsY = (double) height / outHeight;
        double tileHeight = height / (((double) (width * height) / MAXTILESIZE));
        int[] outData = new int[outWidth * outHeight];
        if (height / outHeight > 4) {
            double a = width * 1.0 / outWidth;
            double b = height * 1.0 / outHeight;
            System.out.println(a + "--" + b);
            for (int i = 0; i < outHeight; i++) {
                //System.out.println(i);
                for (int j = 0; j < outWidth; j++) {
                    try {
                        outData[i * outWidth + j] = readTile((int) (x + j * a), (int) (y + i * b), 1, 1)[0];
                    } catch (Exception e) {
                    }
                }
            }
            return outData;
        }
        // load first tile
        int currentY = 0;
        int[] tile = readTile(0, currentY, width, (int) Math.ceil(tileHeight));
//        if (progressbar != null) {
//            progressbar.setMaximum(outHeight / 100);
//        // start going through the image one Tile at a time
//        }
        double posY = 0.0;
        for (int j = 0; j < outHeight; j++, posY += deltaPixelsY) {
//            // update progress bar
//            if (j / 100 - Math.floor(j / 100) == 0) {
//                if (progressbar != null) {
//                    progressbar.setCurrent(j / 100);
//                // check if Tile needs loading
//                }
//            }
            if (posY > (int) Math.ceil(tileHeight)) {
                tile = readTile(0, currentY + (int) Math.ceil(tileHeight), width, (int) Math.ceil(tileHeight));
                posY -= (int) Math.ceil(tileHeight);
                currentY += (int) Math.ceil(tileHeight);

            }

            double posX = 0.0;
            for (int i = 0; i < outWidth; i++, posX += deltaPixelsX) {
                //System.out.println("i = " + i + ", j = " + j + ", posX = " + posX + ", posY = " + posY);
                outData[i + j * outWidth] = tile[(int) posX * (int) posY];
            }
            //System.gc();
        }

        return outData;
    }

    public double getImageAzimuth() {
        double az = 0;

        //compute the azimuth considering the two left corners of the image
        //azimuth angle in degrees between 0 and +180
        double[] endingPoint = getGeoTransform().getGeoFromPixel(getWidth() / 2, 0, "EPSG:4326");
        double[] startingPoint = getGeoTransform().getGeoFromPixel(getWidth() / 2, getHeight() - 1, "EPSG:4326");
        GeodeticCalculator gc = new GeodeticCalculator();
        gc.setStartingGeographicPoint(startingPoint[0], startingPoint[1]);
        gc.setDestinationGeographicPoint(endingPoint[0], endingPoint[1]);
        az = gc.getAzimuth();
        return az;
    }

    public void dispose() {
    }

    public String getDescription() {
        String description = "Image Acquisition and Generation Parameters:\n";
        description += "--------------------\n\n";
        description += "Satellite and Instrument: ";
        description += "\n" + getMetadata(SATELLITE) + "  " + getMetadata(SENSOR);
        description += "\nProduct: ";
        description += getMetadata(PRODUCT);
        description += "\nMode: ";
        description += getMetadata(MODE);
        description += "\nBeam: ";
        description += getMetadata(BEAM);
        description += "\nPolarisations: ";
        description += getMetadata(POLARISATION);
        description += "\nHeading Angle: ";
        description += getMetadata(HEADING_ANGLE);
        description += "\nOrbit Direction: ";
        description += getMetadata(ORBIT_DIRECTION);
        description += "\nImage Dimensions:\n";
        description += "\tWidth:" + getMetadata(WIDTH);
        description += "\n\tHeight:" + getMetadata(HEIGHT);
        description += "\nImage Acquisition Time:\n";
        description += "\tStart:" + getMetadata(TIMESTAMP_START);
        description += "\n\tStop:" + getMetadata(TIMESTAMP_STOP);
        description += "\nImage Pixel Spacing:\n";
        description += "\tAzimuth:" + getMetadata(AZIMUTH_SPACING);
        description += "\n\tRange:" + getMetadata(RANGE_SPACING);
        description += "\nImage Processor and Algorithm: ";
        description += getMetadata(PROCESSOR);
        description += "\nImage ENL: ";
        description += getMetadata(ENL);
        description += "\nSatellite Altitude (m): ";
        description += getMetadata(SATELLITE_ALTITUDE);
        description += "\nSatellite Speed (m/s): ";
        description += getMetadata(SATELLITE_SPEED);
        description += "\nIncidence Angles (degrees):\n";
        description += "\tNear: " + getMetadata(INCIDENCE_NEAR);
        description += "\n\tFar: " + getMetadata(INCIDENCE_FAR);

        return description;
    }

    public void geoCorrect() {
        String imagepath = this.getFilesList()[0];
        try {
            FileReader stream = new FileReader(imagepath + "sumoXML.xml");
            char[] filestream = new char[500];
            stream.read(filestream);
            String filestring = new String(filestream);
            // look for the sumo xml header
            if (filestring.contains("<!-- XML document for SUMO purposes -->")) {
                // look for the offset fields
                double longitudeshift = 0.0;
                double latitudeshift = 0.0;
                longitudeshift = Double.parseDouble(filestring.substring(filestring.indexOf("<longitude>") + 11, filestring.indexOf("</longitude>")));
                latitudeshift = Double.parseDouble(filestring.substring(filestring.indexOf("<latitude>") + 10, filestring.indexOf("</latitude>")));
                // translate the image with the file values converted back into pixels
                double[] originlatlon = getGeoTransform().getGeoFromPixel(0.0, 0.0, "EPSG:4326");
                double[] pixelshift = getGeoTransform().getPixelFromGeo(originlatlon[0] + latitudeshift, originlatlon[1] + longitudeshift, "EPSG:4326");
                System.out.println(pixelshift[0]);
                getGeoTransform().setTransformTranslation((int) pixelshift[0], (int) pixelshift[1]);
            }
        } catch (Exception ex) {
            Logger.getLogger(SARImageReader.class.getName()).log(Level.WARNING, "GeoCorrection file not found");
        }
    }

    public int[] getAmbiguityCorrection(int xPos) {

        double temp, deltaAzimuth, deltaRange;
        int position = xPos;
        int[] output = new int[2];

        try {

            double slantRange = getSlantRange(position);
            // already in radian
            double incidenceAngle = getIncidence(position);
            double satelliteSpeed = getSatelliteSpeed();
            double radarWavelength = Double.parseDouble((String) getMetadata(RADAR_WAVELENGTH));
            double prf = getPRF(position); //Double.parseDouble((String) getMetadata(PRF));
            double orbitInclination = Math.toRadians(Double.parseDouble((String) getMetadata(SATELLITE_ORBITINCLINATION)));
            double revolutionsPerDay = Double.parseDouble((String) getMetadata(REVOLUTIONS_PERDAY));
            double sampleDistAzim = getGeoTransform().getPixelSize()[0];
            double sampleDistRange = getGeoTransform().getPixelSize()[1];

            temp = (radarWavelength * slantRange * prf) /
                    (2 * satelliteSpeed * (1 - Math.cos(orbitInclination) / revolutionsPerDay));

            //azimuth and delta in number of pixels
            deltaAzimuth = temp / sampleDistAzim;
            deltaRange = (temp * temp) / (2 * slantRange * sampleDistRange * Math.sin(incidenceAngle));

            output[0] = (int) Math.floor(deltaAzimuth);
            output[1] = (int) Math.floor(deltaRange);

        } catch (Exception ex) {
        }
        return output;

    }

    public double getIncidence(int position) {
        double incidenceangle = 0.0;
        // estimation of incidence angle based on near and range distance values
        double nearincidence = Math.toRadians(Double.parseDouble((String) getMetadata(INCIDENCE_NEAR)));
        double sataltitude = Double.parseDouble((String) getMetadata(SATELLITE_ALTITUDE));
        double distancerange = sataltitude * Math.tan(nearincidence) + position * getGeoTransform().getPixelSize()[1];
        incidenceangle = Math.atan(distancerange / sataltitude);
        return incidenceangle;
    }

    private double getSatelliteSpeed() {
        // calculate satellite speed
/*
        double seconds = ((double)(getTimestamp(GeoImageReaderBasic.TIMESTAMP_STOP).getTime() - getTimestamp(GeoImageReaderBasic.TIMESTAMP_START).getTime())) / 1000;
        // calculate satellite speed in azimuth pixels / seconds
        double azimuthpixelspeed = ((double)getHeight() * (6400000 + sataltitude) / 6400000) / seconds;
        return azimuthpixelspeed * getGeoTransform().getPixelSize()[0];
         */
        double satellite_speed = 0.0;

        // check if satellite speed has been calculated
        if (getMetadata(SATELLITE_SPEED) != null) {
            satellite_speed = Double.valueOf((String) getMetadata(SATELLITE_SPEED));
        } else {
            // Ephemeris --> R + H
            //Approaching the orbit as circular V=SQRT(GM/(R+H))
            double sataltitude = Double.parseDouble((String) getMetadata(SATELLITE_ALTITUDE));
            satellite_speed = Math.pow(3.986005e14 / (6371000 + sataltitude), 0.5);
            setMetadata(SATELLITE_SPEED, String.valueOf(satellite_speed));
        }

        return satellite_speed;
    }

    public double getSlantRange(int position) {
        double slantrange = 0.0;
        double incidenceangle = getIncidence(position);
        double sataltitude = Double.parseDouble((String) getMetadata(SATELLITE_ALTITUDE));
        // calculate slant range
        if (Math.cos(incidenceangle) != 0.0) {
            slantrange = sataltitude / Math.cos(incidenceangle);
        }
        return slantrange;
    }

    public double getPRF(int position) {
        double prf = 0;
        //check if is the case of TSX ScanSAR 
        if (getMetadata(SATELLITE).equals("TerraSAR-X") && getMetadata(MODE).equals("SC")) {
            int bound1 = new Integer((String) getMetadata(STRIPBOUND1)).intValue();
            int bound2 = new Integer((String) getMetadata(STRIPBOUND2)).intValue();
            int bound3 = new Integer((String) getMetadata(STRIPBOUND3)).intValue();
            //return the different PRF depending by the strip
            if (position >= 0 && position < bound1) {
                return Double.parseDouble((String) getMetadata(PRF1));
            }
            if (position < bound2) {
                return Double.parseDouble((String) getMetadata(PRF2));
            }
            if (position < bound3) {
                return Double.parseDouble((String) getMetadata(PRF3));
            }

            return Double.parseDouble((String) getMetadata(PRF4));
        }

        //for all the other cases with only one PRF
        return Double.parseDouble((String) getMetadata(PRF));


    }

    public double getBetaNought(int x, double DN) {
        double Kvalue = Double.parseDouble((String) getMetadata(K));

        return Math.pow(DN, 2) / Kvalue;
    }

    public double getBetaNoughtDb(int x, double DN) {
        double betaNoughtDb;
        double betaNought;

        betaNought = this.getBetaNought(x, DN);
        if (betaNought > 0) {
            betaNoughtDb = 10 * Math.log(betaNought);
        } else {
            betaNoughtDb = -1;
        }

        // System.out.println("beta nought in db:" +betaNoughtDb );
        return betaNoughtDb;
    }

    public double getSigmaNoughtDb(int[] pixel, double value,
            double incidence_angle) {
        double sigmaNoughtDb;
        double betaNoughtDb;

        betaNoughtDb = getBetaNoughtDb(pixel[0], value);

        if (betaNoughtDb != -1) {
            sigmaNoughtDb = betaNoughtDb + 10 * Math.log(Math.sin(incidence_angle));
        } else {
            sigmaNoughtDb = -1;
        }
        return sigmaNoughtDb;
    }

    public double getSigmaNoughtDb(double betaNoughtDb, double incidence_angle) {
        double sigmaNoughtDb;

        sigmaNoughtDb = betaNoughtDb + 10 * Math.log(Math.sin(incidence_angle));
        return sigmaNoughtDb;
    }

    /**
     * @param file
     *            The file in which it reads
     * @param pointer
     *            Location in the file
     * @param nbBytes
     * @return
     * @throws IOException
     */
    protected float getFloatValue(RandomAccessFile file, int pointer, int nbBytes) throws IOException {
        String convert = "";
        Float temp = null;
        float data = 0;
        int i;
        for (i = 0; i < nbBytes; i++) {
            convert += getCharValue(file, pointer++);
        }
        System.out.println("getFloatValue: " + convert);
        convert = convert.trim();
        if (convert.equalsIgnoreCase("")) {
            System.out.println("getFloatValue : nothing at this place");
            data = -1;
        } else {
            temp = new Float(convert);
            data = temp.floatValue();
        }
        return data;
    }

    protected char getCharValue(RandomAccessFile file, int pointer) throws IOException {
        file.seek(pointer);
        return (char) file.readByte();
    }

    public List<double[]> getFrameLatLon() {
        if (geotransform != null) {
            ArrayList<double[]> latlonframe = new ArrayList<double[]>();

            // use the four image corners to define the image frame
            latlonframe.add(geotransform.getGeoFromPixel(0, 0, "EPSG:4326"));
            latlonframe.add(geotransform.getGeoFromPixel(xSize, 0, "EPSG:4326"));
            latlonframe.add(geotransform.getGeoFromPixel(xSize, ySize, "EPSG:4326"));
            latlonframe.add(geotransform.getGeoFromPixel(0, ySize, "EPSG:4326"));

            return latlonframe;
        }

        return null;
    }

    @Override
    public IGeoRaster clone(){
        return this;
    }
    
    @Override
    public HashMap<String, Object> getProperties() {
    	// TODO Auto-generated method stub
    	return getMetadata();
    }
    
    @Override
    public void save(File file) {
    	// TODO Auto-generated method stub
    	
    }
    
}

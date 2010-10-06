/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.metaaps.eoclipse.imageviewer.api;

import com.metaaps.eoclipse.common.Attributes;
import com.vividsolutions.jts.geom.Geometry;

import java.util.List;
import java.util.Vector;

/**
 * This is THE class model for all Vector Data
 * @author thoorfr
 */
public class GeometricLayer {
    
    
    public final static String POINT = "point";
    public final static String POLYGON = "polygon";
    public final static String LINESTRING = "linestring";
    /**
     * please use at your own risk...
     */
    public final static String MIXED = "mixed";
    

    private List<Geometry> geoms;
    private List<Attributes> atts;
    private String type;
    private String name;
    private String projection;

    public GeometricLayer(String type) {
        geoms = new Vector<Geometry>();
        atts = new Vector<Attributes>();
        this.type=type;
    }
    
    /**
     * perform a deep copy of the Geometric Layer
     * @return
     */
    @Override
    public GeometricLayer clone(){
        GeometricLayer out=new GeometricLayer(type);
        out.name=name;
        for(int i=0;i<geoms.size();i++){
            out.geoms.add(i,(Geometry)geoms.get(i).clone());
            out.atts.add(i,atts.get(i).clone());
        }
        return out;
    }

    /**
     * Clears all the data but keep schema and geometric types
     */
    public void clear(){
        geoms.clear();
        atts.clear();
    }
    
    /**
     * retrun the atributes associated with the geometry
     * @param geom
     * @return
     */
    public Attributes getAttributes(Geometry geom) {
        int i = geoms.indexOf(geom);
        if(i<0) return null;
        return atts.get(i);
    }

    /**
     *
     * @return a SHALLOW COPY of the attributes for Thread safe use
     */
    public List<Attributes> getAttributes() {
       return new Vector<Attributes>(atts);
    }

    /**
     * 
     * @return the list of geometries (NOT COPIED so NOT THREAD-SAFE)
     */
    public List<Geometry> getGeometries() {
        return geoms;
    }
    
    /**
     * 
     * @return the type of the geometry, one of the static field of the class
     */
    public String getGeometryType(){
        return this.type;
    }

    /**
     * return the schema example: getSchema(':')="name:age:position"
     * @param separator
     * @return
     */
    public String getSchema(char separator) {
        String out = "";
        for (String att : getSchema()) {
            out += att + separator;
        }
        if (out.equals("")) {
            return out;
        } else {
            return out.substring(0, out.length() - 1);
        }
    }

    /**
     * PLEASE DO NOT USE, AT YOUR OWN RISKS
     * @param name
     * @param type
     * @return
     */
    public boolean addColumn(String name, String type){
        for(Attributes att:atts){
            att.addColumn(name, type);
        }
        return true;
    }
    
    /**
     * 
     * @return the types of the schema. @see Attributes
     */
    public String[] getSchemaTypes(){
          if (atts.size() > 0) {
            return atts.get(0).getTypes();
        } else {
            return new String[]{};
        }
    }

    /**
     * The types of the schema. @see Attributes
     * @param separator
     * @return
     */
    public String getSchemaTypes(char separator) {
        String out = "";
        for (String att : getSchemaTypes()) {
            out += att + separator;
        }
        if (out.equals("")) {
            return out;
        } else {
            return out.substring(0, out.length() - 1);
        }
    }

    /**
     * Adds a new geometry with attributes to the layer. NOTE THAT NEITHER 
     * THE SCHEMA NOR the GEOMETRY TYPE ARE CHECKED
     * so you can use it in whatever way you want, at your own risks of course
     * @param geom
     * @param att
     */
    public void put(Geometry geom, Attributes att) {
        geoms.add(geom);
        atts.add(att);
    }

    /**
     * Adds a geometry, with default Attributes
     * NO GEOMETRY TYPE CHECK
     * @param geo
     */
    public void put(Geometry geo){
        this.put(geo, Attributes.createAttributes(getSchema(), getSchemaTypes()));
    }

    /**
     * Removes one geometry. The Attributes will be removed accordingly
     * @param geom
     */
    public void remove(Geometry geom) {
        if (!geoms.contains(geom)) {
            return;
        }
        int i = geoms.indexOf(geom);
        geoms.remove(i);
        atts.remove(i);
    }

    /**
     * replace the geometry "oldGeometry" with "newGeometry"
     * @param oldGeometry
     * @param newGeometry
     */
    public void replace(Geometry oldGeometry, Geometry newGeometry) {
        geoms.set(geoms.indexOf(oldGeometry), newGeometry);
    }

    public void setAttribute(Geometry geom, String att, Object value) {
        if (!geoms.contains(geom)) {
            return;
        }
        int i = geoms.indexOf(geom);
        atts.get(i).set(att, value);
    }

    public String[] getSchema() {
        if (atts.size() > 0) {
            return atts.get(0).getSchema();
        } else {
            return new String[]{};
        }
    }

    public String getName() {
        return name;
    }

    public void setGeometryType(String type) {
        this.type=type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjection() {
        return projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }
}

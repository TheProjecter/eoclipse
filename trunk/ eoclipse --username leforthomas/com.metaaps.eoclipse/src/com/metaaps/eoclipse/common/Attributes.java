/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.metaaps.eoclipse.common;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * This class deals wit attributes of GeometricLayer.
 * For optimisation purpose, the geometric data (ie to be displayed)
 * and the associated attributes have been separated.
 * This class should be used in a synchronised way, ie, only one thread
 * can access one instantiated class
 * @author thoorfr
 */
public class Attributes {

    private Map<String, Object> attributes;
    private String[] schema;
    private String[] types;

    /**
     * create an instance of the class providing a schema. For instance a schema like:
     * ["name", "length", "width", "releaseDate"] will have the following types:
     * ["java.lang.String", "java.lang.Double", "java.lang.Double", "java.lang.Date"]. Only "Double", String", "Integer", "Long" and "Date"
     * are supported so far. Respect Case.
     * @param schema ordered
     * @param types ordered
     * @return
     */
    public static Attributes createAttributes(String[] schema, String[] types) {
        Attributes att = new Attributes();
        att.schema = schema;
        att.types = types;
        att.attributes = new HashMap<String, Object>();
        for (int i = 0; i < schema.length; i++) {
            if (types[i].contains("Double")) {
                att.attributes.put(schema[i], Double.NaN);
            }
            if (types[i].contains("String")) {
                att.attributes.put(schema[i], new String());
            }
            if (types[i].contains("Integer")) {
                att.attributes.put(schema[i], Integer.MIN_VALUE);
            }
            if (types[i].contains("Long")) {
                att.attributes.put(schema[i], Long.MIN_VALUE);
            }
            if (types[i].contains("Date")) {
                att.attributes.put(schema[i], new Timestamp(System.currentTimeMillis()));
            } else {
                att.attributes.put(schema[i], null);
            }
        }
        return att;
    }
    /**
     * Deep copy of the attributes data
     * @return a new instance of Attributes with full copy of data
     */
    @Override
    public Attributes clone(){
        Attributes out=Attributes.createAttributes(schema, types);
        out.attributes.putAll(attributes);
        return out;
    }

    /**
     *
     * @return the values of the Attributes in the schema order (getSchema())
     */
    public Object[] getValues() {
        Object[] out = new Object[schema.length];
        int i = 0;
        for (String s : schema) {
            out[i++] = attributes.get(s);
        }
        return out;
    }

    /**
     * Set the attributes value giving the schema name.
     * @param att
     * @param value
     * @return true if successfully added, false if not or ignored
     */
    public boolean set(String att, Object value) {
        if (!attributes.containsKey(att)) {
            return false;
        } else {
            attributes.put(att, value);
            return true;
        }
    }

    /**
     * 
     * @param att
     * @return the value corresponding to the schema key "att"
     */
    public Object get(String att) {
        return attributes.get(att);
    }

    /**
     * 
     * @return the schema of the Attributes instance
     */
    public String[] getSchema() {
        return schema;
    }

    /**
     *
     * @return the types of the Atributes in the same order of the schema
     */
    public String[] getTypes() {
        return types;
    }

    /**
     * add a new column at the end of the schema. THIS IS NOT RECOMMENDED. Prefer
     * the use aof a new Attributes instance with the new schema and copy all the values
     * form the former instance
     * @param name
     * @param type
     * @return
     */
    public boolean addColumn(String name, String type){
        String[] temp=new String[schema.length+1];
        System.arraycopy(schema, 0, temp, 0, schema.length);
        schema=temp;
        schema[schema.length-1]=name;
        temp=new String[types.length+1];
        System.arraycopy(types, 0, temp, 0, types.length);
        types=temp;
        types[types.length-1]=type;
        return true;
    }
    
    /**
     * 
     * @return a String of the form "key1=value1\nkey2=bvalue2" etc....
     */
    @Override
    public String toString(){
        String out="";
        for(String key:schema){
            out+=key+"="+attributes.get(key)+"\n";
        }
        
        return out;
    }
}

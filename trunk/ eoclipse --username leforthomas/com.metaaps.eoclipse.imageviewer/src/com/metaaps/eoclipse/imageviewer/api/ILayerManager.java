/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.metaaps.eoclipse.imageviewer.api;

import java.util.List;

/**
 * Interface that codes the behavior of a Layer Manager which consists of a set of layers.
 * @author thoorfr
 */
public interface ILayerManager extends ILayer {

    public void addLayer(ILayer layer);
    public void removeLayer(ILayer layer);
    public List<ILayer> getLayers();

}

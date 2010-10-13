/*******************************************************************************
 * Copyright (c) 2010 METAAPS SRL(U).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     METAAPS SRL(U) - created by Thomas Lefort - initial API and implementation
 ******************************************************************************/
package com.metaaps.eoclipse.common;

import org.jdom.Element;

/**
 * @author leforthomas
 * 
 * methods for serialisation of the workflow elements
 * 
 */
public interface ISerialize {
	
	void fillDOMElement(Element element);
	
	void readDOMElement(Element element);

}

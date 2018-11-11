/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.pma2020.xform_test;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/* Taken from: https://www.mkyong.com/java/how-to-modify-xml-file-in-java-dom-parser/ */
class XmlModifier {
    private String newFilePath;
    private Document xmlDom;

    XmlModifier(String filePath) {
        newFilePath = filePath.substring(0, filePath.length() -4) + "-modified" + ".xml";
        xmlDom = createXmlDom(filePath);
    }

    private Document createXmlDom(String filePath) {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        Document doc = null;
        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.parse(filePath);
        } catch (ParserConfigurationException | IOException | SAXException exc) {
            exc.printStackTrace();
        }
        return doc;
    }

    void writeToFile() {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(xmlDom);
            StreamResult result = new StreamResult(new File(newFilePath));
            transformer.transform(source, result);
        } catch (TransformerException exc) {
            exc.printStackTrace();
        }
    }

    String getnewFilePath() {
      return newFilePath;
    }

    /** Originally, I wanted a separate methods to get a list of nodes and then manipulate nodes. This would also allow
    * me to optionally print warnings outside of this method. To save time, this has not yet been implemented. -jef,
    * 2018/08/14 */
    boolean modifyNodeAttributesByFindReplace(String attributeName, String find, String replace) {
        boolean modificationMade = false;
        NodeList bindElements = xmlDom.getElementsByTagName("bind");

        for (int i = 0; i < bindElements.getLength(); i++) {
            Node childNode = bindElements.item(i);
            NamedNodeMap attributes = childNode.getAttributes();
            Node calculateAttr = attributes.getNamedItem(attributeName);
            if (calculateAttr != null) {
                if (calculateAttr.getNodeValue().contains(find)) {
                    calculateAttr.setNodeValue(replace);
                    modificationMade = true;
                }
            }
        }

        return modificationMade;
    }
}

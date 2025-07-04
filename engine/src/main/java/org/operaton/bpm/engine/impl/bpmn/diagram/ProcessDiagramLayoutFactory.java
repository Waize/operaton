/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.bpmn.diagram;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParser;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.repository.DiagramElement;
import org.operaton.bpm.engine.repository.DiagramLayout;
import org.operaton.bpm.engine.repository.DiagramNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Provides positions and dimensions of elements in a process diagram as
 * provided by {@link RepositoryService#getProcessDiagram(String)}.
 *
 * @author Falko Menge
 */
public class ProcessDiagramLayoutFactory {

  private static final int GREY_THRESHOLD = 175;

  // Parser features and their values needed to disable XXE Parsing
  private static final Map<String, Boolean> XXE_FEATURES = Map.of(
          "http://apache.org/xml/features/disallow-doctype-decl", true,
          "http://xml.org/sax/features/external-general-entities", false,
          "http://xml.org/sax/features/external-parameter-entities", false,
          "http://apache.org/xml/features/nonvalidating/load-external-dtd", false
  );

  /**
   * Provides positions and dimensions of elements in a process diagram as
   * provided by {@link RepositoryService#getProcessDiagram(String)}.
   *
   * Currently, it only supports BPMN 2.0 models.
   *
   * @param bpmnXmlStream
   *          BPMN 2.0 XML file
   * @param imageStream
   *          BPMN 2.0 diagram in PNG format (JPEG and other formats supported
   *          by {@link ImageIO} may also work)
   * @return Layout of the process diagram. Will return {@code null} when parameter imageStream is {@code null}.
   */
  public DiagramLayout getProcessDiagramLayout(InputStream bpmnXmlStream, InputStream imageStream) {
    Document bpmnModel = parseXml(bpmnXmlStream);
    return getBpmnProcessDiagramLayout(bpmnModel, imageStream);
  }

  /**
   * Provides positions and dimensions of elements in a BPMN process diagram as
   * provided by {@link RepositoryService#getProcessDiagram(String)}.
   *
   * @param bpmnModel
   *          BPMN 2.0 XML document
   * @param imageStream
   *          BPMN 2.0 diagram in PNG format (JPEG and other formats supported
   *          by {@link ImageIO} may also work)
   * @return Layout of the process. Will return {@code null} when parameter imageStream is {@code null}.
   */
  public DiagramLayout getBpmnProcessDiagramLayout(Document bpmnModel, InputStream imageStream) {
    if (imageStream == null) {
      return null;
    }
    DiagramNode diagramBoundsXml = getDiagramBoundsFromBpmnDi(bpmnModel);
    DiagramNode diagramBoundsImage;
    if (isExportedFromAdonis50(bpmnModel)) {
      int offsetTop = 29; // Adonis header
      int offsetBottom = 61; // Adonis footer
      diagramBoundsImage = getDiagramBoundsFromImage(imageStream, offsetTop, offsetBottom);
    } else {
      diagramBoundsImage = getDiagramBoundsFromImage(imageStream);
    }
        
    Map<String, DiagramNode> listOfBounds = new HashMap<>();
    listOfBounds.put(diagramBoundsXml.getId(), diagramBoundsXml);
//    listOfBounds.putAll(getElementBoundsFromBpmnDi(bpmnModel));
    listOfBounds.putAll(fixFlowNodePositionsIfModelFromAdonis(bpmnModel, getElementBoundsFromBpmnDi(bpmnModel)));

    Map<String, DiagramElement> listOfBoundsForImage = transformBoundsForImage(diagramBoundsImage, diagramBoundsXml, listOfBounds);
    return new DiagramLayout(listOfBoundsForImage);
  }
  
  protected Document parseXml(InputStream bpmnXmlStream) {
    // Initiate DocumentBuilderFactory
    DocumentBuilderFactory factory = getConfiguredDocumentBuilderFactory();
    DocumentBuilder builder;
    Document bpmnModel;
    try {
      // Get DocumentBuilder
      builder = factory.newDocumentBuilder();
      // Parse and load the Document into memory
      bpmnModel = builder.parse(bpmnXmlStream);
    } catch (Exception e) {
      throw new ProcessEngineException("Error while parsing BPMN model.", e);
    }
    return bpmnModel;
  }

  protected DiagramNode getDiagramBoundsFromBpmnDi(Document bpmnModel) {
    Double minX = null;
    Double minY = null;
    Double maxX = null;
    Double maxY = null;
  
    // Node positions and dimensions
    NodeList setOfBounds = bpmnModel.getElementsByTagNameNS(BpmnParser.BPMN_DC_NS, "Bounds");
    for (int i = 0; i < setOfBounds.getLength(); i++) {
      Element element = (Element) setOfBounds.item(i);
      Double x = Double.valueOf(element.getAttribute("x"));
      Double y = Double.valueOf(element.getAttribute("y"));
      Double width = Double.valueOf(element.getAttribute("width"));
      Double height = Double.valueOf(element.getAttribute("height"));
  
      if (x == 0.0 && y == 0.0 && width == 0.0 && height == 0.0) {
        // Ignore empty labels like the ones produced by Yaoqiang:
        // <bpmndi:BPMNLabel><dc:Bounds height="0.0" width="0.0" x="0.0" y="0.0"/></bpmndi:BPMNLabel>
      } else {
        if (minX == null || x < minX) {
          minX = x;
        }
        if (minY == null || y < minY) {
          minY = y;
        }
        if (maxX == null || maxX < (x + width)) {
          maxX = (x + width);
        }
        if (maxY == null || maxY < (y + height)) {
          maxY = (y + height);
        }
      }
    }
  
    // Edge bend points
    NodeList waypoints = bpmnModel.getElementsByTagNameNS(BpmnParser.OMG_DI_NS, "waypoint");
    for (int i = 0; i < waypoints.getLength(); i++) {
      Element waypoint = (Element) waypoints.item(i);
      Double x = Double.valueOf(waypoint.getAttribute("x"));
      Double y = Double.valueOf(waypoint.getAttribute("y"));
  
      if (minX == null || x < minX) {
        minX = x;
      }
      if (minY == null || y < minY) {
        minY = y;
      }
      if (maxX == null || maxX < x) {
        maxX = x;
      }
      if (maxY == null || maxY < y) {
        maxY = y;
      }
    }
  
    DiagramNode diagramBounds = new DiagramNode("BPMNDiagram");
    diagramBounds.setX(minX);
    diagramBounds.setY(minY);
    diagramBounds.setWidth(maxX - minX);
    diagramBounds.setHeight(maxY - minY);
    return diagramBounds;
  }

  protected DiagramNode getDiagramBoundsFromImage(InputStream imageStream) {
    return getDiagramBoundsFromImage(imageStream, 0, 0);
  }

  protected DiagramNode getDiagramBoundsFromImage(InputStream imageStream, int offsetTop, int offsetBottom) {
    BufferedImage image;
    try {
      image = ImageIO.read(imageStream);
    } catch (IOException e) {
      throw new ProcessEngineException("Error while reading process diagram image.", e);
    }
    return getDiagramBoundsFromImage(image, offsetTop, offsetBottom);
  }

  protected DiagramNode getDiagramBoundsFromImage(BufferedImage image, int offsetTop, int offsetBottom) {
    int width = image.getWidth();
    int height = image.getHeight();
    
    Map<Integer, Boolean> rowIsWhite = new TreeMap<>();
    Map<Integer, Boolean> columnIsWhite = new TreeMap<>();
    
    for (int row = 0; row < height; row++) {
      if (!rowIsWhite.containsKey(row)) {
        rowIsWhite.put(row, true);
      }
      if (row <= offsetTop || row > image.getHeight() - offsetBottom) {
        rowIsWhite.put(row, true);
      } else {
        for (int column = 0; column < width; column++) {
          if (!columnIsWhite.containsKey(column)) {
            columnIsWhite.put(column, true);
          }
          int pixel = image.getRGB(column, row);
          int alpha = (pixel >> 24) & 0xff;
          int red   = (pixel >> 16) & 0xff;
          int green = (pixel >>  8) & 0xff;
          int blue  = (pixel >>  0) & 0xff;
          if (!(alpha == 0 || (red >= GREY_THRESHOLD && green >= GREY_THRESHOLD && blue >= GREY_THRESHOLD))) {
            rowIsWhite.put(row, false);
            columnIsWhite.put(column, false);
          }
        }
      }
    }
  
    int marginTop = 0;
    for (int row = 0; row < height; row++) {
      if (rowIsWhite.get(row)) {
        ++marginTop;
      } else {
        // Margin Top Found
        break;
      }
    }
    
    int marginLeft = 0;
    for (int column = 0; column < width; column++) {
      if (columnIsWhite.get(column)) {
        ++marginLeft;
      } else {
        // Margin Left Found
        break;
      }
    }
    
    int marginRight = 0;
    for (int column = width - 1; column >= 0; column--) {
      if (columnIsWhite.get(column)) {
        ++marginRight;
      } else {
        // Margin Right Found
        break;
      }
    }
    
    int marginBottom = 0;
    for (int row = height -1; row >= 0; row--) {
      if (rowIsWhite.get(row)) {
        ++marginBottom;
      } else {
        // Margin Bottom Found
        break;
      }
    }
    
    DiagramNode diagramBoundsImage = new DiagramNode();
    diagramBoundsImage.setX((double) marginLeft);
    diagramBoundsImage.setY((double) marginTop);
    diagramBoundsImage.setWidth((double) (width - marginRight - marginLeft));
    diagramBoundsImage.setHeight((double) (height - marginBottom - marginTop));
    return diagramBoundsImage;
  }

  protected Map<String, DiagramNode> getElementBoundsFromBpmnDi(Document bpmnModel) {
    Map<String, DiagramNode> listOfBounds = new HashMap<>();
    // iterate over all DI shapes
    NodeList shapes = bpmnModel.getElementsByTagNameNS(BpmnParser.BPMN_DI_NS, "BPMNShape");
    for (int i = 0; i < shapes.getLength(); i++) {
      Element shape = (Element) shapes.item(i);
      String bpmnElementId = shape.getAttribute("bpmnElement");
      // get bounds of shape
      NodeList childNodes = shape.getChildNodes();
      for (int j = 0; j < childNodes.getLength(); j++) {
        Node childNode = childNodes.item(j);
        if (childNode instanceof Element element
                && BpmnParser.BPMN_DC_NS.equals(childNode.getNamespaceURI())
                && "Bounds".equals(childNode.getLocalName())) {
          DiagramNode bounds = parseBounds(element);
          bounds.setId(bpmnElementId);
          listOfBounds.put(bpmnElementId, bounds);
          break;
        }
      }
    }
    return listOfBounds;
  }

  protected DiagramNode parseBounds(Element boundsElement) {
    DiagramNode bounds = new DiagramNode();
    bounds.setX(Double.valueOf(boundsElement.getAttribute("x")));
    bounds.setY(Double.valueOf(boundsElement.getAttribute("y")));
    bounds.setWidth(Double.valueOf(boundsElement.getAttribute("width")));
    bounds.setHeight(Double.valueOf(boundsElement.getAttribute("height")));
    return bounds;
  }

  protected Map<String, DiagramElement> transformBoundsForImage(DiagramNode diagramBoundsImage, DiagramNode diagramBoundsXml, Map<String, DiagramNode> listOfBounds) {
    Map<String, DiagramElement> listOfBoundsForImage = new HashMap<>();
    for (Entry<String, DiagramNode> bounds : listOfBounds.entrySet()) {
      listOfBoundsForImage.put(bounds.getKey(), transformBoundsForImage(diagramBoundsImage, diagramBoundsXml, bounds.getValue()));
    }
    return listOfBoundsForImage;
  }

  protected DiagramNode transformBoundsForImage(DiagramNode diagramBoundsImage, DiagramNode diagramBoundsXml, DiagramNode elementBounds) {
    double scalingFactorX = diagramBoundsImage.getWidth() / diagramBoundsXml.getWidth();
    double scalingFactorY = diagramBoundsImage.getWidth() / diagramBoundsXml.getWidth();

    DiagramNode elementBoundsForImage = new DiagramNode(elementBounds.getId());
    elementBoundsForImage.setX((double) Math.round((elementBounds.getX() - diagramBoundsXml.getX()) * scalingFactorX + diagramBoundsImage.getX()));
    elementBoundsForImage.setY((double) Math.round((elementBounds.getY() - diagramBoundsXml.getY()) * scalingFactorY + diagramBoundsImage.getY()));
    elementBoundsForImage.setWidth((double) Math.round(elementBounds.getWidth() * scalingFactorX));
    elementBoundsForImage.setHeight((double) Math.round(elementBounds.getHeight() * scalingFactorY));
    return elementBoundsForImage;
  }

  protected Map<String, DiagramNode> fixFlowNodePositionsIfModelFromAdonis(Document bpmnModel, Map<String, DiagramNode> elementBoundsFromBpmnDi) {
    if (isExportedFromAdonis50(bpmnModel)) {
      Map<String, DiagramNode> mapOfFixedBounds = new HashMap<>();
      XPathFactory xPathFactory = XPathFactory.newInstance();
      XPath xPath = xPathFactory.newXPath();
      xPath.setNamespaceContext(new Bpmn20NamespaceContext());
      for (Entry<String, DiagramNode> entry : elementBoundsFromBpmnDi.entrySet()) {
        String elementId = entry.getKey();
        DiagramNode elementBounds = entry.getValue();
        String expression = "local-name(//bpmn:*[@id = '" + elementId + "'])";
        try {
          XPathExpression xPathExpression = xPath.compile(expression);
          String elementLocalName = xPathExpression.evaluate(bpmnModel);
          if (!"participant".equals(elementLocalName) 
                  && !"lane".equals(elementLocalName)
                  && !"textAnnotation".equals(elementLocalName)
                  && !"group".equals(elementLocalName)) {
            elementBounds.setX(elementBounds.getX() - elementBounds.getWidth()/2);
            elementBounds.setY(elementBounds.getY() - elementBounds.getHeight()/2);
          }
        } catch (XPathExpressionException e) {
          throw new ProcessEngineException("Error while evaluating the following XPath expression on a BPMN XML document: '" + expression + "'.", e);
        }
        mapOfFixedBounds.put(elementId, elementBounds);
      }
      return mapOfFixedBounds;
    } else {
      return elementBoundsFromBpmnDi;
    }
  }

  protected boolean isExportedFromAdonis50(Document bpmnModel) {
    return "ADONIS".equals(bpmnModel.getDocumentElement().getAttribute("exporter"))
            && "5.0".equals(bpmnModel.getDocumentElement().getAttribute("exporterVersion"));
  }

  protected DocumentBuilderFactory getConfiguredDocumentBuilderFactory() {

    boolean isXxeParsingEnabled = Context.getProcessEngineConfiguration().isEnableXxeProcessing();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    // Configure XXE Processing
    try {
      for (Map.Entry<String, Boolean> feature : XXE_FEATURES.entrySet()) {
        factory.setFeature(feature.getKey(), isXxeParsingEnabled ^ feature.getValue());
      }
    } catch (Exception e) {
      throw new ProcessEngineException("Error while configuring BPMN parser.", e);
    }
    factory.setXIncludeAware(isXxeParsingEnabled);
    factory.setExpandEntityReferences(isXxeParsingEnabled);

    // Get a factory that understands namespaces
    factory.setNamespaceAware(true);

    return factory;
  }
}

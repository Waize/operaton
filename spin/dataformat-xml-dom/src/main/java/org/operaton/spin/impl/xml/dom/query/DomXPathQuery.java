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
package org.operaton.spin.impl.xml.dom.query;

import static org.operaton.spin.impl.xml.dom.util.DomXmlEnsure.ensureNotDocumentRootExpression;
import static org.operaton.spin.impl.xml.dom.util.DomXmlEnsure.ensureXPathNotEmpty;
import static org.operaton.spin.impl.xml.dom.util.DomXmlEnsure.ensureXPathNotNull;

import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.operaton.spin.SpinList;
import org.operaton.spin.impl.SpinListImpl;
import org.operaton.spin.impl.xml.dom.DomXmlAttributeIterable;
import org.operaton.spin.impl.xml.dom.DomXmlElement;
import org.operaton.spin.impl.xml.dom.DomXmlElementIterable;
import org.operaton.spin.impl.xml.dom.DomXmlLogger;
import org.operaton.spin.impl.xml.dom.format.DomXmlDataFormat;
import org.operaton.spin.xml.SpinXPathQuery;
import org.operaton.spin.xml.SpinXmlAttribute;
import org.operaton.spin.xml.SpinXmlElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Sebastian Menski
 */
public class DomXPathQuery extends SpinXPathQuery {

  private static final DomXmlLogger LOG = DomXmlLogger.XML_DOM_LOGGER;
  protected final DomXmlElement domElement;
  protected final XPath query;
  protected final String expression;
  protected final DomXmlDataFormat dataFormat;
  protected DomXPathNamespaceResolver resolver;

  public DomXPathQuery(DomXmlElement domElement, XPath query, String expression, DomXmlDataFormat dataFormat) {
    this.domElement = domElement;
    this.query = query;
    this.expression = expression;
    this.dataFormat = dataFormat;
    this.resolver = new DomXPathNamespaceResolver(this.domElement);

    this.query.setNamespaceContext(this.resolver);
  }

  @Override
  public SpinXmlElement element() {
    try {
      ensureNotDocumentRootExpression(expression);
      Element element = (Element) query.evaluate(expression, domElement.unwrap(), XPathConstants.NODE);
      ensureXPathNotNull(element, expression);
      return dataFormat.createElementWrapper(element);
    } catch (XPathExpressionException e) {
      throw LOG.unableToEvaluateXPathExpressionOnElement(domElement, e);
    } catch (ClassCastException e) {
      throw LOG.unableToCastXPathResultTo(Element.class, e);
    }
  }

  @Override
  public SpinList<SpinXmlElement> elementList() {
    try {
      ensureNotDocumentRootExpression(expression);
      NodeList nodeList = (NodeList) query.evaluate(expression, domElement.unwrap(), XPathConstants.NODESET);
      ensureXPathNotEmpty(nodeList, expression);
      return new SpinListImpl<>(new DomXmlElementIterable(nodeList, dataFormat));
    } catch (XPathExpressionException e) {
      throw LOG.unableToEvaluateXPathExpressionOnElement(domElement, e);
    } catch (ClassCastException e) {
      throw LOG.unableToCastXPathResultTo(NodeList.class, e);
    }
  }

  @Override
  public SpinXmlAttribute attribute() {
    try {
      ensureNotDocumentRootExpression(expression);
      Attr attribute = (Attr) query.evaluate(expression, domElement.unwrap(), XPathConstants.NODE);
      ensureXPathNotNull(attribute, expression);
      return dataFormat.createAttributeWrapper(attribute);
    } catch (XPathExpressionException e) {
      throw LOG.unableToEvaluateXPathExpressionOnElement(domElement, e);
    } catch (ClassCastException e) {
      throw LOG.unableToCastXPathResultTo(Attr.class, e);
    }
  }

  @Override
  public SpinList<SpinXmlAttribute> attributeList() {
    try {
      ensureNotDocumentRootExpression(expression);
      NodeList nodeList = (NodeList) query.evaluate(expression, domElement.unwrap(), XPathConstants.NODESET);
      ensureXPathNotEmpty(nodeList, expression);
      return new SpinListImpl<>(new DomXmlAttributeIterable(nodeList, dataFormat));
    } catch (XPathExpressionException e) {
      throw LOG.unableToEvaluateXPathExpressionOnElement(domElement, e);
    } catch (ClassCastException e) {
      throw LOG.unableToCastXPathResultTo(NodeList.class, e);
    }
  }

  @Override
  public String string() {
    try {
      ensureNotDocumentRootExpression(expression);
      return (String) query.evaluate(expression, domElement.unwrap(), XPathConstants.STRING);
    } catch (XPathExpressionException e) {
      throw LOG.unableToEvaluateXPathExpressionOnElement(domElement, e);
    } catch (ClassCastException e) {
      throw LOG.unableToCastXPathResultTo(String.class, e);
    }
  }

  @Override
  public Double number() {
    try {
      ensureNotDocumentRootExpression(expression);
      return (Double) query.evaluate(expression, domElement.unwrap(), XPathConstants.NUMBER);
    } catch (XPathExpressionException e) {
      throw LOG.unableToEvaluateXPathExpressionOnElement(domElement, e);
    } catch (ClassCastException e) {
      throw LOG.unableToCastXPathResultTo(Double.class, e);
    }
  }

  @Override
  public Boolean bool() {
    try {
      ensureNotDocumentRootExpression(expression);
      return (Boolean) query.evaluate(expression, domElement.unwrap(), XPathConstants.BOOLEAN);
    } catch (XPathExpressionException e) {
      throw LOG.unableToEvaluateXPathExpressionOnElement(domElement, e);
    } catch (ClassCastException e) {
      throw LOG.unableToCastXPathResultTo(Boolean.class, e);
    }
  }

  @Override
  public SpinXPathQuery ns(String prefix, String namespace) {
    resolver.setNamespace(prefix, namespace);
    query.setNamespaceContext(resolver);
    return this;
  }

  @Override
  public SpinXPathQuery ns(Map<String, String> namespaces) {
    resolver.setNamespaces(namespaces);
    query.setNamespaceContext(resolver);
    return this;
  }

}

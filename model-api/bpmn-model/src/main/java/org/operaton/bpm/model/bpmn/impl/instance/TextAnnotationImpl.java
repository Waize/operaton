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
package org.operaton.bpm.model.bpmn.impl.instance;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TEXT_FORMAT;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_TEXT_ANNOTATION;

import org.operaton.bpm.model.bpmn.instance.Artifact;
import org.operaton.bpm.model.bpmn.instance.Text;
import org.operaton.bpm.model.bpmn.instance.TextAnnotation;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN 2.0 textAnnotation element
 *
 * @author Filip Hrisafov
 */
public class TextAnnotationImpl extends ArtifactImpl implements TextAnnotation {

  protected static Attribute<String> textFormatAttribute;
  protected static ChildElement<Text> textChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder
      .defineType(TextAnnotation.class, BPMN_ELEMENT_TEXT_ANNOTATION).namespaceUri(BPMN20_NS)
      .extendsType(Artifact.class)
      .instanceProvider(TextAnnotationImpl::new);

    textFormatAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TEXT_FORMAT)
      .defaultValue("text/plain")
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    textChild = sequenceBuilder.element(Text.class)
      .build();

    typeBuilder.build();
  }

  public TextAnnotationImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public String getTextFormat() {
    return textFormatAttribute.getValue(this);
  }

  @Override
  public void setTextFormat(String textFormat) {
    textFormatAttribute.setValue(this, textFormat);
  }

  @Override
  public Text getText() {
    return textChild.getChild(this);
  }

  @Override
  public void setText(Text text) {
    textChild.setChild(this, text);
  }
}

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
package org.operaton.bpm.container.impl.deployment.jobexecutor;

import static org.operaton.bpm.container.impl.deployment.Attachments.PROCESS_APPLICATION;

import java.util.Map;

import org.operaton.bpm.application.AbstractProcessApplication;
import org.operaton.bpm.container.impl.ContainerIntegrationLogger;
import org.operaton.bpm.container.impl.jmx.services.JmxManagedJobExecutor;
import org.operaton.bpm.container.impl.metadata.PropertyHelper;
import org.operaton.bpm.container.impl.metadata.spi.JobAcquisitionXml;
import org.operaton.bpm.container.impl.spi.PlatformServiceContainer;
import org.operaton.bpm.container.impl.spi.DeploymentOperation;
import org.operaton.bpm.container.impl.spi.DeploymentOperationStep;
import org.operaton.bpm.container.impl.spi.ServiceTypes;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.RuntimeContainerJobExecutor;

/**
 * <p>Deployment operation step responsible for starting a JobEexecutor</p>
 *
 * @author Daniel Meyer
 *
 */
public class StartJobAcquisitionStep extends DeploymentOperationStep {

  protected static final ContainerIntegrationLogger LOG = ProcessEngineLogger.CONTAINER_INTEGRATION_LOGGER;

  protected final JobAcquisitionXml jobAcquisitionXml;

  public StartJobAcquisitionStep(JobAcquisitionXml jobAcquisitionXml) {
    this.jobAcquisitionXml = jobAcquisitionXml;

  }

  @Override
  public String getName() {
    return "Start job acquisition '"+jobAcquisitionXml.getName()+"'";
  }

  @Override
  public void performOperationStep(DeploymentOperation operationContext) {

    final PlatformServiceContainer serviceContainer = operationContext.getServiceContainer();
    final AbstractProcessApplication processApplication = operationContext.getAttachment(PROCESS_APPLICATION);

    ClassLoader configurationClassloader = null;

    if(processApplication != null) {
      configurationClassloader = processApplication.getProcessApplicationClassloader();
    } else {
      configurationClassloader = ProcessEngineConfiguration.class.getClassLoader();
    }

    String configurationClassName = jobAcquisitionXml.getJobExecutorClassName();

    if(configurationClassName == null || configurationClassName.isEmpty()) {
      configurationClassName = RuntimeContainerJobExecutor.class.getName();
    }

    // create & instantiate the job executor class
    Class<? extends JobExecutor> jobExecutorClass = loadJobExecutorClass(configurationClassloader, configurationClassName);
    JobExecutor jobExecutor = instantiateJobExecutor(jobExecutorClass);

    // apply properties
    Map<String, String> properties = jobAcquisitionXml.getProperties();
    PropertyHelper.applyProperties(jobExecutor, properties);

    // construct service for job executor
    JmxManagedJobExecutor jmxManagedJobExecutor = new JmxManagedJobExecutor(jobExecutor);

    // deploy the job executor service into the container
    serviceContainer.startService(ServiceTypes.JOB_EXECUTOR, jobAcquisitionXml.getName(), jmxManagedJobExecutor);
  }


  protected JobExecutor instantiateJobExecutor(Class<? extends JobExecutor> configurationClass) {
    try {
      return configurationClass.getDeclaredConstructor().newInstance();
    }
    catch (Exception e) {
      throw LOG.couldNotInstantiateJobExecutorClass(e);
    }
  }

  @SuppressWarnings("unchecked")
  protected Class<? extends JobExecutor> loadJobExecutorClass(ClassLoader processApplicationClassloader, String jobExecutorClassname) {
    try {
      return (Class<? extends JobExecutor>) processApplicationClassloader.loadClass(jobExecutorClassname);
    }
    catch (ClassNotFoundException e) {
      throw LOG.couldNotLoadJobExecutorClass(e);
    }
  }

}

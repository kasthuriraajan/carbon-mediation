/*
 *
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  you may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 * /
 */
package org.wso2.carbon.mediator.dsscall;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.OMNamespaceImpl;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.MessageHelper;
import org.wso2.carbon.dataservices.core.DataServiceFault;
import org.wso2.carbon.dataservices.core.DataServiceProcessor;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * DSSCall mediator
 */
public class DSSCallMediator extends AbstractMediator {

    private String DSName;
    private String operation;
    private Map<String, String> params = new HashMap<String, String>();
    private String targetType;
    private String propertyName;


    public boolean mediate(MessageContext messageContext) {

        SynapseLog synLog = getLog(messageContext);
        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : DSSCall mediator");
        }

        String serviceName = getDSName();
        String serviceRequest = getOperation();
        String targetTypeName = getTargetType();
        String propertyName = getPropertyName();
        String payloadNameSpaceURI = "http://ws.wso2.org/dataservicesample";
        String payloadPrefix = "axis2ns";
        String type = "property";

        if (synLog.isTraceOrDebugEnabled())
        {
            synLog.traceOrDebug("The data service name : "+getDSName());
            synLog.traceOrDebug("The operation name : "+getOperation());
            synLog.traceOrDebug("The target type : "+getTargetType());
        }


        try {
            /*clone the message context to append payloads to invoke dss*/
            MessageContext cloneMessageContext = MessageHelper.cloneMessageContext(messageContext);

			/*Casting the synapse message context to axis2 message context*/
            org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) cloneMessageContext)
                    .getAxis2MessageContext();

			/*Get the Axis service name of the give DS name*/
            AxisService axisService = axis2MessageContext.getConfigurationContext().getAxisConfiguration()
                    .getService(serviceName);
            if (axisService!=null) {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("The axisService :" + axisService.getName());
                    synLog.traceOrDebug("The service name space URI : " + axisService.getTargetNamespace());
                }

			/*Set the axis service into the axis2 message context*/
                axis2MessageContext.setAxisService(axisService);

			/*Set the operation into the axis2 message context*/
                QName qName = new QName(serviceRequest);
                axis2MessageContext.getAxisMessage().getAxisOperation().setName(qName);

            /*Create the pay load using the parameter values given through xml config.*/
                OMFactory fac = OMAbstractFactory.getOMFactory();
                OMNamespace omNamespace = new OMNamespaceImpl(payloadNameSpaceURI, payloadPrefix);
                OMElement payload = fac.createOMElement(operation, omNamespace);

                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Parameter payload NameSpace URI : " + payload.getNamespace().getNamespaceURI());
                    synLog.traceOrDebug("Parameter payload prefix : " + payload.getNamespace().getPrefix());
                }

                for (Map.Entry<String, String> entry : params.entrySet()) {
                    OMElement omElement = fac.createOMElement(entry.getKey(), omNamespace);
                    omElement.setText(entry.getValue());
                    payload.addChild(omElement);
                }

                if (axis2MessageContext.getEnvelope().getBody().getFirstElement() != null) {
                    axis2MessageContext.getEnvelope().getBody().getFirstElement().detach();
                }

                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("The parameter payload : " + payload);
                }
                axis2MessageContext.getEnvelope().getBody().addChild(payload);

			/*access the dss using DataServiceProcessor*/
                try {

                    OMElement omElement = DataServiceProcessor.dispatch(axis2MessageContext);

                    if (omElement != null) {
                        if (synLog.isTraceOrDebugEnabled()) {
                            synLog.traceOrDebug("The result OMElement : " + omElement);
                        }

					/*	set the result payload as property according to the target type*/
                        if (type.equals(targetTypeName)) {
                            messageContext.setProperty(propertyName, omElement);
                            if (synLog.isTraceOrDebugEnabled()) {
                                synLog.traceOrDebug("The result property : " + messageContext.getProperty(propertyName));
                            }
                        }

					/*	set the result payload as envelope in to message context according to the target type*/
                        else {
                            messageContext.getEnvelope().getBody().addChild(omElement);
                        }
                    }
                } catch (DataServiceFault dataServiceFault) {
                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug(dataServiceFault.getMessage());
                    }
                    synLog.error(dataServiceFault.getMessage());
                    handleException("DataService exception occurred while access the DSS to do the operation"
                            , dataServiceFault, messageContext);

                }
            }
            else
            {
                handleException("The data service,  named '"+serviceName+"' does not exist. ",messageContext);
            }

        } catch (AxisFault axisFault) {
            if (synLog.isTraceOrDebugEnabled())
            {
                synLog.traceOrDebug(axisFault.getMessage());
            }
            synLog.error(axisFault.getMessage());
            handleException("AxisFault occurred.",axisFault,messageContext);
        }
        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("End : DSSCall mediator");
        }
        return true;
    }

    void setDSName(String DSName) {

        this.DSName = DSName;
    }

    String getDSName() {
        return DSName;
    }

    String getOperation() {
        return operation;
    }

    void setOperation(String operation) {
        this.operation = operation;
    }

    void setParam(String name, String value) {
        this.params.put(name, value);
    }

    Map<String, String> getParams() {
        return params;
    }


    String getPropertyName() {
        return propertyName;
    }

    void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    String getTargetType() {
        return targetType;
    }

    void setTargetType(String targetType) {
        this.targetType = targetType;
    }


}
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

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.AbstractMediatorSerializer;

import java.util.Map;


/**
 * Serializer for {@link DSSCallMediator} instances.
 *
 * @see DSSCallMediatorSerializer
 */
public class DSSCallMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeSpecificMediator(Mediator m) {

        if (!(m instanceof DSSCallMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        DSSCallMediator mediator = (DSSCallMediator) m;
        OMElement dssCall = fac.createOMElement("dsscall", synNS);
        OMElement dsname = fac.createOMElement("dsname", synNS);
        OMElement operation = fac.createOMElement("operation", synNS);
        OMElement params = fac.createOMElement("params", synNS);
        OMElement target = fac.createOMElement("target", synNS);
        dsname.setText(mediator.getDSName());
        operation.setText(mediator.getOperation());
        if (mediator.getParams() != null) {
            Map<String, String> paramsMap = mediator.getParams();

            for (Map.Entry s : paramsMap.entrySet()) {
                OMElement param = fac.createOMElement("param", synNS);
                param.addAttribute("name", s.getKey().toString(), nullNS);
                param.setText(s.getValue().toString());
                params.addChild(param);
            }
        }
        dssCall.addChild(dsname);
        dssCall.addChild(operation);
        dssCall.addChild(params);

        target.addAttribute("type", mediator.getTargetType(), nullNS);
        if (mediator.getTargetType().equals("property")) {
            target.addAttribute("name", mediator.getPropertyName(), nullNS);
        }
        dssCall.addChild(target);
        saveTracingState(dssCall, mediator);

        return dssCall;
    }

    public String getMediatorClassName() {
        return DSSCallMediator.class.getName();
    }
}
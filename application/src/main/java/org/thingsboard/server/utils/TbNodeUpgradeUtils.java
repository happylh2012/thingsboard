/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.service.component.RuleNodeClassInfo;

public class TbNodeUpgradeUtils {

    public static void upgradeConfigurationAndVersion(RuleNode node, RuleNodeClassInfo nodeInfo) throws Exception {
        JsonNode oldConfiguration = node.getConfiguration();
        var configClass = nodeInfo.getAnnotation().configClazz();

        if (oldConfiguration == null || !oldConfiguration.isObject()) {
            node.setConfiguration(JacksonUtil.valueToTree(configClass.getDeclaredConstructor().newInstance().defaultConfiguration()));
        } else {
            var tbVersionedNode = (TbNode) nodeInfo.getClazz().getDeclaredConstructor().newInstance();
            try {
                TbPair<Boolean, JsonNode> upgradeResult = tbVersionedNode.upgrade(node.getConfigurationVersion(), oldConfiguration);
                if (upgradeResult.getFirst()) {
                    node.setConfiguration(upgradeResult.getSecond());
                }
            } catch (TbNodeException e) {
                if (!isValidConfig(oldConfiguration, configClass)) {
                    throw e;
                }
            }
        }
        node.setConfigurationVersion(nodeInfo.getCurrentVersion());
    }

    private static boolean isValidConfig(JsonNode oldConfiguration, Class<? extends NodeConfiguration> configClass) {
        try {
            JacksonUtil.treeToValue(oldConfiguration, configClass);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.messaging.activemq.ha;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.ALLOW_FAILBACK;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.CLUSTER_NAME;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.GROUP_NAME;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.INITIAL_REPLICATION_SYNC_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.RESTART_BACKUP;
import static org.wildfly.extension.messaging.activemq.ha.ManagementHelper.createAddOperation;
import static org.wildfly.extension.messaging.activemq.ha.ScaleDownAttributes.addScaleDownConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.activemq.artemis.core.config.HAPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.ReplicaPolicyConfiguration;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.ActiveMQReloadRequiredHandlers;
import org.wildfly.extension.messaging.activemq.MessagingExtension;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class ReplicationSlaveDefinition extends PersistentResourceDefinition {

    public static final Collection<AttributeDefinition> ATTRIBUTES;

    static {
        Collection<AttributeDefinition> attributes = new ArrayList<>();
        attributes.add(CLUSTER_NAME);
        attributes.add(GROUP_NAME);
        attributes.add(ALLOW_FAILBACK);
        attributes.add(INITIAL_REPLICATION_SYNC_TIMEOUT);
        attributes.add(MAX_SAVED_REPLICATED_JOURNAL_SIZE);
        attributes.add(RESTART_BACKUP);

        attributes.addAll(ScaleDownAttributes.SCALE_DOWN_ATTRIBUTES);

        ATTRIBUTES = Collections.unmodifiableCollection(attributes);
    }

    public static final ReplicationSlaveDefinition INSTANCE = new ReplicationSlaveDefinition(MessagingExtension.REPLICATION_SLAVE_PATH, false);
    public static final ReplicationSlaveDefinition CONFIGURATION_INSTANCE = new ReplicationSlaveDefinition(MessagingExtension.CONFIGURATION_SLAVE_PATH, true);

    private ReplicationSlaveDefinition(PathElement path, boolean allowSibling) {
        super(path,
                MessagingExtension.getResourceDescriptionResolver(HA_POLICY),
                createAddOperation(path.getKey(), allowSibling, ATTRIBUTES),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        AbstractWriteAttributeHandler writeAttribute = new ActiveMQReloadRequiredHandlers.WriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, writeAttribute);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static HAPolicyConfiguration buildConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ReplicaPolicyConfiguration haPolicyConfiguration = new ReplicaPolicyConfiguration()
                .setAllowFailBack(ALLOW_FAILBACK.resolveModelAttribute(context, model).asBoolean())
                .setInitialReplicationSyncTimeout(INITIAL_REPLICATION_SYNC_TIMEOUT.resolveModelAttribute(context, model).asLong())
                .setMaxSavedReplicatedJournalsSize(MAX_SAVED_REPLICATED_JOURNAL_SIZE.resolveModelAttribute(context, model).asInt())
                .setScaleDownConfiguration(addScaleDownConfiguration(context, model))
                .setRestartBackup(RESTART_BACKUP.resolveModelAttribute(context, model).asBoolean());

        ModelNode clusterName = CLUSTER_NAME.resolveModelAttribute(context, model);
        if (clusterName.isDefined()) {
            haPolicyConfiguration.setClusterName(clusterName.asString());
        }
        ModelNode groupName = GROUP_NAME.resolveModelAttribute(context, model);
        if (groupName.isDefined()) {
            haPolicyConfiguration.setGroupName(groupName.asString());
        }

        return haPolicyConfiguration;
    }
}
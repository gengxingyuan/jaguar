/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.ipam.listener;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;

import org.future.k8snet.ipam.DefaultIpManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.K8sNodesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodesBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeListener implements DataTreeChangeListener<K8sNodes> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeListener.class);

    DefaultIpManager defaultIpManager;
    ListenerRegistration<NodeListener> listenerReg;
    public static final String DEFAULT_IP_POOL = "10.0.0.0/8";
    private final DataBroker dataBroker;

    public NodeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        InstanceIdentifier<IpamConfig> id = InstanceIdentifier.create(IpamConfig.class);
        listenerReg = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, id), this);
    }

    public void close() {
        listenerReg.close();
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<K8sNodes>> changes) {
        for (DataTreeModification<K8sNodes> change : changes) {
            final DataObjectModification<K8sNodes> mod = change.getRootNode();

            switch (mod.getModificationType()) {
                case DELETE:
                    break;
                case SUBTREE_MODIFIED:
                    break;
                case WRITE:
                    if (this.defaultIpManager == null) {
                        initDefaultIpManager(dataBroker);
                    }
                    if (mod.getDataBefore() == null) {
                        addIpBlockToNode(mod.getDataAfter(), dataBroker);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Add ip blocks according to its max number of pod.
     * @param nodeNew, k8s new node
     * @param dataBroker,DataBroker
     */
    private void addIpBlockToNode(K8sNodes nodeNew, DataBroker dataBroker) {
        InstanceIdentifier<K8sNodes> nodeId = InstanceIdentifier.builder(K8sNodesInfo.class)
                .child(K8sNodes.class, nodeNew.getKey()).build();
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        K8sNodesBuilder builder = new K8sNodesBuilder();
        builder.setUid(nodeNew.getUid());
        String ipBlock = defaultIpManager.distributeIp(Integer.valueOf(nodeNew.getMaxPodNum()));
        builder.setPodCidr(ipBlock);
        writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, nodeId, builder.build());
    }

    private void initDefaultIpManager(DataBroker dataBroker) {
        InstanceIdentifier<IpamConfig> path = InstanceIdentifier.create(IpamConfig.class);
        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
        try {
            Optional<IpamConfig> optional = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, path).get();

            // If the user didn't set ip pool use default
            if (!optional.isPresent()) {
                this.defaultIpManager = new DefaultIpManager(DEFAULT_IP_POOL);
            } else {
                IpamConfig ipamConfig = optional.get();
                this.defaultIpManager = new DefaultIpManager(ipamConfig.getNetwork());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("read datastore fail:",e);
        }
    }

}

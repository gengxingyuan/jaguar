/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.ipam.listener;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
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
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
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
        InstanceIdentifier<K8sNodes> id = InstanceIdentifier.builder(K8sNodesInfo.class).child(K8sNodes.class).build();
        listenerReg = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, id), NodeListener.this);
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
       * @param nodeNew,
       *          k8s new node
       * @param dataBroker,DataBroker
       */
    private void addIpBlockToNode(K8sNodes nodeNew, DataBroker dataBroker) {
        InstanceIdentifier<K8sNodes> nodeId = InstanceIdentifier.builder(K8sNodesInfo.class)
                .child(K8sNodes.class, nodeNew.getKey()).build();
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        K8sNodesBuilder builder = new K8sNodesBuilder(nodeNew);
        String ipBlock = defaultIpManager.distributeIp(Integer.valueOf(nodeNew.getMaxPodNum()));
        builder.setPodCidr(ipBlock);
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodeId, builder.build());
        CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();

        Futures.addCallback(submit, new FutureCallback<Void>() {

            @Override
            public void onSuccess(final Void result) {
            // Committed successfully
                LOG.debug("Add ip block to {} -- Committedsuccessfully ", nodeNew.getHostName());
            }

            @Override
            public void onFailure(final Throwable throwable) {
            // Transaction failed

                if (throwable instanceof OptimisticLockFailedException) {
                // Failed because of concurrent transaction modifying same
                // data
                    LOG.error("Add ip block -- Failed because of concurrent transaction modifying same data");
                } else {
                   // Some other type of TransactionCommitFailedException
                    LOG.error("Add ip block -- Some other type of TransactionCommitFailedException", throwable);
                }
            }
            }, MoreExecutors.directExecutor());
    }

    private void initDefaultIpManager(DataBroker dataBroker) {
        InstanceIdentifier<IpamConfig> ipamConfig_path = InstanceIdentifier.create(IpamConfig.class);
        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
        try {
            Optional<IpamConfig> optional = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, ipamConfig_path).get();
            // If the user didn't set ip pool use default
            if (!optional.isPresent()) {
                this.defaultIpManager = new DefaultIpManager(DEFAULT_IP_POOL);
            } else {
                this.defaultIpManager = new DefaultIpManager(optional.get().getPodCidr());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("read datastore fail:", e);
        }
    }

}

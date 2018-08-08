/*
 * Copyright (c) 2017 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.watcher;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.future.k8snet.util.DbUtil;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.K8sNodesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(NodeWatcher.class);

    private final DataBroker dataBroker;
    private final KubernetesClient client;

    public NodeWatcher(final DataBroker dataBroker,KubernetesClient client) {
        this.dataBroker = dataBroker;
        this.client = client;
        this.client.nodes().watch(new Watcher<Node>() {
            public void eventReceived(Action action, Node node) {
                boolean errorFlag = false;
                LOG.debug(action + ",node" + node);
                InstanceIdentifier<K8sNodes> id = buildK8sNodeInstanceIdentifier(node);
                WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
                switch (action) {
                    case ADDED:
                        tx.put(LogicalDatastoreType.CONFIGURATION,id,buildK8sNodes(node));
                        break;
                    case MODIFIED:
                        tx.merge(LogicalDatastoreType.CONFIGURATION,id,buildK8sNodes(node));
                        break;
                    case DELETED:
                        tx.delete(LogicalDatastoreType.CONFIGURATION,id);
                        break;
                    case ERROR:
                        LOG.debug("action is ERROR");
                        break;
                    default :
                        LOG.debug("DBOper is unknown!");
                }
                DbUtil.doSubmit(tx);

            }

            @Override
            public void onClose(KubernetesClientException clientExcept) {
                LOG.info("client.nodes().watch OnClose:" + clientExcept);
            }
        });
        LOG.info("new NodeWatcher");
    }

    private K8sNodes buildK8sNodes(Node node) {
        K8sNodesBuilder k8sNodesBuilder = new K8sNodesBuilder();
        for (NodeAddress nodeAddress:node.getStatus().getAddresses()) {
            if (nodeAddress.getType().equals("InternalIP")) {
                k8sNodesBuilder.setInternalIpAddress(new IpAddress(new Ipv4Address(nodeAddress.getAddress())));
            }
            if (nodeAddress.getType().equals("ExternalIp")) {
                k8sNodesBuilder.setExternalIpAddress(new IpAddress(new Ipv4Address(nodeAddress.getAddress())));
            }
        }
        k8sNodesBuilder.setUid(new Uuid(node.getStatus().getNodeInfo().getSystemUUID()))
                .setHostName(node.getMetadata().getName());
        return k8sNodesBuilder.build();
    }

    private InstanceIdentifier<K8sNodes> buildK8sNodeInstanceIdentifier(Node node) {
        return InstanceIdentifier.create(K8sNodesInfo.class)
                .child(K8sNodes.class,new K8sNodesKey(new Uuid(node.getMetadata().getUid())));
    }

    public void close() {
        LOG.info("NodeWatcher close!");
    }
}

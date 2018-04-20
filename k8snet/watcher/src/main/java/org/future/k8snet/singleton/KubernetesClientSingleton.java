/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.singleton;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.future.k8snet.util.DbUtil;
import org.future.k8snet.watcher.NodeWatcher;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.org.future.k8s.apiserver.config.rev180307.K8sApiserverConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.K8sNodesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.K8sNodesInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class KubernetesClientSingleton implements ClusterSingletonService {
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesClientSingleton.class);
    private final ServiceGroupIdentifier serviceGroupIdent = ServiceGroupIdentifier.create("KubernetesClient");

    private final DataBroker dataBroker;
    final ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private K8sApiserverConfig k8sApiserverConfig;
    private KubernetesClient client;
    private NodeWatcher nodeWatcher;

    public KubernetesClientSingleton(final K8sApiserverConfig k8sApiserverConfig,
                                     final DataBroker dataBroker,
                                     final ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        this.k8sApiserverConfig = k8sApiserverConfig;
        this.dataBroker = dataBroker;
        this.clusterSingletonServiceProvider = clusterSingletonServiceProvider;
        this.clusterSingletonServiceProvider.registerClusterSingletonService(this);
    }

    public void close() {

    }

    public void instantiateServiceInstance() {
        Config config = new ConfigBuilder().withMasterUrl(k8sApiserverConfig.getScheme()
                + "://"+k8sApiserverConfig.getMasterIp().getIpv4Address().getValue()
                + ":"+k8sApiserverConfig.getPort()
                + "/").build();
                //new ConfigBuilder()
                //.withMasterUrl("https://" + cluster.getEndpoint())
                //.withCaCertData(cluster.getMasterAuth().getClusterCaCertificate())
                //.withClientCertData(cluster.getMasterAuth().getClientCertificate())
                //.withClientKeyData(cluster.getMasterAuth().getClientKey())
                //.build();
        client = new DefaultKubernetesClient(config);
        k8sNodeSync();
        nodeWatcher = new NodeWatcher(dataBroker, client);
        LOG.info("k8s node sync instantiateServiceInstance");
    }

    public ListenableFuture<Void> closeServiceInstance() {
        return Futures.immediateCheckedFuture(null);
    }

    @Nonnull
    public ServiceGroupIdentifier getIdentifier() {
        return serviceGroupIdent;
    }

    private void k8sNodeSync() {
        K8sNodesInfoBuilder k8sNodesInfoBuilder = new K8sNodesInfoBuilder();
        k8sNodesInfoBuilder.setId("Nodes");

        List<Node> nodeList = client.nodes().list().getItems();
        List<K8sNodes> k8sNodesList = new ArrayList<K8sNodes>();
        for (Node node:nodeList ) {
            K8sNodesBuilder k8sNodesBuilder = new K8sNodesBuilder();
            for (NodeAddress nodeAddress:node.getStatus().getAddresses()) {
                if( nodeAddress.getType().equals("InternalIP")) {
                    k8sNodesBuilder.setInternalIpAddress(new IpAddress(new Ipv4Address(nodeAddress.getAddress())));
                }
                if( nodeAddress.getType().equals("ExternalIp")) {
                    k8sNodesBuilder.setExternalIpAddress(new IpAddress(new Ipv4Address(nodeAddress.getAddress())));
                }
            }
            k8sNodesBuilder.setUid(new Uuid(node.getStatus().getNodeInfo().getSystemUUID()))
            .setHostName(node.getMetadata().getName());
            k8sNodesList.add(k8sNodesBuilder.build());
        }
        k8sNodesInfoBuilder.setK8sNodes(k8sNodesList);

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<K8sNodesInfo> id = InstanceIdentifier.create(K8sNodesInfo.class);
        tx.merge(LogicalDatastoreType.CONFIGURATION,id,k8sNodesInfoBuilder.build());

        DbUtil.doSubmit(tx);

    }
}

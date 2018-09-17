/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.future.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;

import org.future.util.OperationProcessor;
import org.future.util.OvsdbUtil;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.K8sNodesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeStandalone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeListener implements DataTreeChangeListener<K8sNodes> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeListener.class);
    public static final String BRIDGE_NAME = "br0";
    public static final int OVSDB_PORT = 6641;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private OperationProcessor dbProcessor;
    private ListenerRegistration<NodeListener> listenerRegistration;
    private final DataBroker dataBroker;
    private static MdsalUtils mdsalUtils = null;
    private SouthboundUtils southboundUtils = null;
    private Map<String,ConnectionInfo> nodeConntionMap;

    InstanceIdentifier<Topology> path =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(SouthboundUtils.OVSDB_TOPOLOGY_ID));

    public NodeListener(final DataBroker dataBroker) {
        registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
        this.dataBroker = dataBroker;
        mdsalUtils = new MdsalUtils(dataBroker);
        southboundUtils = new SouthboundUtils(mdsalUtils);
        nodeConntionMap = new ConcurrentHashMap();
        dbProcessor = new OperationProcessor(dataBroker);
        dbProcessor.start();
    }

    protected InstanceIdentifier<K8sNodes> getWildCardPath() {
        return InstanceIdentifier.create(K8sNodesInfo.class).child(K8sNodes.class);
    }

    public void registerListener(LogicalDatastoreType dsType, final DataBroker db) {
        final DataTreeIdentifier<K8sNodes> treeId = new DataTreeIdentifier<>(dsType, getWildCardPath());
        listenerRegistration = db.registerDataTreeChangeListener(treeId, NodeListener.this);
    }

    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } finally {
                listenerRegistration = null;
            }
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<K8sNodes>> changes) {
        for (DataTreeModification<K8sNodes> change : changes) {
            final DataObjectModification<K8sNodes> mod = change.getRootNode();

            switch (mod.getModificationType()) {
                case DELETE:
                    delete(mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                    break;
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        add(mod.getDataAfter());
                    }
                    break;
                default:
                    LOG.error("Unhandled modification type " + mod.getModificationType());
                    break;
            }
        }
    }

    private synchronized void add(K8sNodes nodeNew) {
        LOG.debug("k8sNode added - ovsdb node connecting!" + nodeNew);
        ConnectionInfo connectionInfo = OvsdbUtil.getConnectionInfo(
                String.valueOf(nodeNew.getInternalIpAddress().getValue()),OVSDB_PORT);
        Node node = southboundUtils.createNode(connectionInfo);
        southboundUtils.connectOvsdbNode(connectionInfo,2000);
        NodeId nodeId = southboundUtils.createNodeId(connectionInfo.getRemoteIp(),connectionInfo.getRemotePort());

        try {
            southboundUtils.addBridge(connectionInfo, southboundUtils.createInstanceIdentifier(connectionInfo),
                    BRIDGE_NAME, nodeId, true, OvsdbFailModeStandalone.class,
                    false, DatapathTypeSystem.class, null, null,
                    null, null, 0);
        } catch (InterruptedException e) {
            LOG.info("addBridge failed" + e);
        }
        addTermininationPointForCurNode(connectionInfo);
        nodeConntionMap.put(nodeId.getValue(),connectionInfo);

        LOG.info("add node :" + nodeNew);
    }

    private void update(K8sNodes nodeOld, K8sNodes nodeNew) {
        LOG.info("k8sNode updated - ovsdb node!" + nodeNew);
    }

    private synchronized void delete(K8sNodes nodeOld) {
        LOG.info("k8sNode deleted !" + nodeOld);
        ConnectionInfo connectionInfo = OvsdbUtil.getConnectionInfo(
                nodeOld.getInternalIpAddress().getIpv4Address().getValue(),OVSDB_PORT);
        Node node = southboundUtils.createNode(connectionInfo);
        NodeId nodeId = southboundUtils.createNodeId(connectionInfo.getRemoteIp(),connectionInfo.getRemotePort());
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid);
        nodeConntionMap.remove(nodeId.getValue());

        delTermininationPointForCurNode(connectionInfo);
    }

    private void addTermininationPointForCurNode(ConnectionInfo curConnectionInfo) {
        Node curNode = southboundUtils.createNode(curConnectionInfo);
        String curNodeIp = curConnectionInfo.getRemoteIp().getIpv4Address().getValue();
        String tunInterface1 = curNodeIp.replaceAll(".","-");
        Map<String,String> options1 = new HashMap<>();
        options1.put("remote_ip",curNodeIp);
        for (ConnectionInfo otherConn:nodeConntionMap.values()) {
            Node otherNode = southboundUtils.createNode(otherConn);
            String remoteIp = otherConn.getRemoteIp().getIpv4Address().getValue();
            String tunInterface = remoteIp.replaceAll(".","-");
            Map<String,String> options = new HashMap<>();
            options.put("remote_ip",remoteIp);
            dbProcessor.enqueueOperation(manager ->  {
                InstanceIdentifier<TerminationPoint> tpIid =
                    southboundUtils.createTerminationPointInstanceIdentifier(curNode, tunInterface);
                TerminationPoint tp = createTerminationPoint(tunInterface,options,tpIid);
                manager.mergeToTransaction(LogicalDatastoreType.CONFIGURATION,tpIid,tp,false);
            });
            dbProcessor.enqueueOperation(manager ->  {
                InstanceIdentifier<TerminationPoint> tpIid =
                    southboundUtils.createTerminationPointInstanceIdentifier(otherNode, tunInterface1);
                TerminationPoint tp = createTerminationPoint(tunInterface1,options1,tpIid);
                manager.mergeToTransaction(LogicalDatastoreType.CONFIGURATION,tpIid,tp,false);
            });
        }
    }

    private TerminationPoint createTerminationPoint(String portName,Map<String,String> options,
                                                    InstanceIdentifier<TerminationPoint> tpIid) {

        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        tpAugmentationBuilder.setInterfaceType(InterfaceTypeVxlan.class);

        List<Options> optionsList = new ArrayList<>();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            OptionsBuilder optionsBuilder = new OptionsBuilder();
            optionsBuilder.setKey(new OptionsKey(entry.getKey()));
            optionsBuilder.setOption(entry.getKey());
            optionsBuilder.setValue(entry.getValue());
            optionsList.add(optionsBuilder.build());
        }
        tpAugmentationBuilder.setOptions(optionsList);

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        return tpBuilder.build();
    }

    private void delTermininationPointForCurNode(ConnectionInfo curConnectionInfo) {
        Node delNode = southboundUtils.createNode(curConnectionInfo);
        String remoteIp = curConnectionInfo.getRemoteIp().getIpv4Address().getValue();
        String tunInterface = remoteIp.replaceAll(".","-");
        for (ConnectionInfo otherConn:nodeConntionMap.values()) {
            Node otherNode = southboundUtils.createNode(otherConn);

            dbProcessor.enqueueOperation(manager -> {
                InstanceIdentifier<TerminationPoint> tpId =
                     southboundUtils.createTerminationPointInstanceIdentifier(otherNode,tunInterface);
                manager.addDeleteOperationToTxChain(LogicalDatastoreType.CONFIGURATION,tpId);
            });
        }
    }
}

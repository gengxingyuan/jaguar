/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.ipam;

//import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamConfig;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamService;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestNodeIpBlockInput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestNodeIpBlockOutput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestNodeIpBlockOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.K8sNodesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IpamProvider implements IpamService {
    private static final Logger LOG = LoggerFactory.getLogger(IpamProvider.class);

    private final DataBroker dataBroker;
    private String global_Block = "10.0.0.0/8";

    public IpamProvider(String global_cidr, final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        global_Block = global_cidr;
    }

    public void init() {
        LOG.info("IpamProvider initialized");
    }

    public void close() {
        LOG.info("IpamProvider closed");
    }


    @Override
    public Future<RpcResult<RequestNodeIpBlockOutput>> requestNodeIpBlock(RequestNodeIpBlockInput input) {
        String name = input.getNodeName();
        RequestNodeIpBlockOutputBuilder blockOutputBuilder = new RequestNodeIpBlockOutputBuilder();
        InstanceIdentifier<K8sNodesInfo> path = InstanceIdentifier.builder(K8sNodesInfo.class).build();
        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
        CheckedFuture<com.google.common.base.Optional<K8sNodesInfo>, ReadFailedException> future = tx
                .read(LogicalDatastoreType.CONFIGURATION, path);
        String ipBlock = null;
        try {
            com.google.common.base.Optional<K8sNodesInfo> data = future.get();
            if (data.isPresent()) {
                K8sNodesInfo nodesInfo = data.get();
                Optional<K8sNodes> optionalNodes = nodesInfo.getK8sNodes().stream()
                        .filter(node->node.getHostName().equals(name)).findFirst();
                if(optionalNodes.isPresent()) {
                    ipBlock = optionalNodes.get().getPodCidr();
                }
            } else {
                return RpcResultBuilder.<RequestNodeIpBlockOutput>failed().buildFuture();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.toString());
        }
        if (ipBlock != null) {
            blockOutputBuilder.setGlobalCidr(global_Block);
            blockOutputBuilder.setNodeCidr(ipBlock);
        } else {
            return RpcResultBuilder.<RequestNodeIpBlockOutput>failed().buildFuture();
        }
        return RpcResultBuilder.success(blockOutputBuilder.build()).buildFuture();
    }

  /**
   * Check if the ip block is legal.
   * @param ipb,ip
   *          block
   * @return boolean
   */
    private boolean checkIpBlocks(String ipb) {
        if (ipb != null && !ipb.isEmpty()) {
            String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}"
                    + "|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2"
                    + "[0-4]\\d|25[0-5]|[1-9]\\d|\\d)/([1-9]|[1-2]\\d|30|31)$";
            if (ipb.matches(regex)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}

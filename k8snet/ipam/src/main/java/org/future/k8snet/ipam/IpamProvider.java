/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.ipam;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.future.k8snet.ipam.util.IpamData;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.ConfigIpPoolInput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.ConfigIpPoolOutput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.ConfigIpPoolOutputBuilder;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.GetCurrentSettingsOutput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.GetCurrentSettingsOutputBuilder;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamAttr.SchemeType;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamService;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestAddressInput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestAddressOutput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestNodeIpBlockInput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestNodeIpBlockOutput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestNodeIpBlockOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.K8sNodesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IpamProvider implements IpamService {
    private static final Logger LOG = LoggerFactory.getLogger(IpamProvider.class);

    private final DataBroker dataBroker;
    private IpamData ipamData;

    public IpamProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        ipamData = new IpamData(this.dataBroker);
    }

    public void init() {
        LOG.info("IpamProvider initialized");
    }

    public void close() {
        LOG.info("IpamProvider closed");
    }

    @Override
    public Future<RpcResult<ConfigIpPoolOutput>> configIpPool(ConfigIpPoolInput input) {
        ConfigIpPoolOutputBuilder poolOutputBuilder = new ConfigIpPoolOutputBuilder();
        switch (input.getSchemeType()) {
            case DEFAULT:
                String ipBlock = input.getNetwork();
                if (checkIpBlocks(ipBlock)) {
                    // Write the ip tool into datastore
                    ipamData.addScheme(SchemeType.DEFAULT, ipBlock);
                    poolOutputBuilder.setResult("Success");
                } else {
                    poolOutputBuilder.setResult("Please input ip pool in the correct format like '10.0.0.0/8' ");
                }
                break;
            case OTHER:
                break;
            default:
                break;
        }
        return RpcResultBuilder.success(poolOutputBuilder.build()).buildFuture();
    }

    @Override
    public Future<RpcResult<RequestAddressOutput>> requestAddress(RequestAddressInput input) {
      // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<RequestNodeIpBlockOutput>> requestNodeIpBlock(RequestNodeIpBlockInput input) {
        String uid = input.getNodeUid();
        RequestNodeIpBlockOutputBuilder blockOutputBuilder = new RequestNodeIpBlockOutputBuilder();
        InstanceIdentifier<K8sNodes> path = InstanceIdentifier.builder(K8sNodesInfo.class)
                .child(K8sNodes.class, new K8sNodesKey(new Uuid(uid))).build();
        ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<K8sNodes>, ReadFailedException> future = read
                .read(LogicalDatastoreType.CONFIGURATION, path);
        String ipBlock = null;
        try {
            Optional<K8sNodes> data = future.get();
            if (data.isPresent()) {
                K8sNodes k8sNode = data.get();
                ipBlock = k8sNode.getPodCidr();
            } else {
                return RpcResultBuilder.success(blockOutputBuilder.setIpBlock("No such node").build()).buildFuture();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.toString());
        }
        if (ipBlock != null) {
            blockOutputBuilder.setIpBlock(ipBlock);
        } else {
            blockOutputBuilder.setIpBlock("Not distributed yet");
        }
        return RpcResultBuilder.success(blockOutputBuilder.build()).buildFuture();
    }


    @Override
    public Future<RpcResult<GetCurrentSettingsOutput>> getCurrentSettings() {
        GetCurrentSettingsOutputBuilder builder = new GetCurrentSettingsOutputBuilder();
        builder.setIpSchemes(ipamData.getSchemes());
        return RpcResultBuilder.success(builder.build()).buildFuture();
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

/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.ipam;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.ConfigIpPoolInput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.ConfigIpPoolOutput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.ConfigIpPoolOutputBuilder;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamConfig;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamConfigBuilder;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamService;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestAddressInput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestAddressOutput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpamProvider implements IpamService {
    private static final Logger LOG = LoggerFactory.getLogger(IpamProvider.class);

    private final DataBroker dataBroker;

    public IpamProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void close() {

    }

    @Override
    public Future<RpcResult<ConfigIpPoolOutput>> configIpPool(ConfigIpPoolInput input) {
        ConfigIpPoolOutputBuilder poolOutputBuilder = new ConfigIpPoolOutputBuilder();
        switch (input.getType()) {
        case DEFAULT:
            String ipBlock = input.getNetwork();
            if (checkIpBlocks(ipBlock)) {
                // Write the ip tool into datastore
                InstanceIdentifier<IpamConfig> id = InstanceIdentifier.create(IpamConfig.class);
                WriteTransaction write = dataBroker.newWriteOnlyTransaction();
                IpamConfigBuilder configBuilder = new IpamConfigBuilder();
                configBuilder.setNetwork(ipBlock);
                configBuilder.setType(input.getType());
                write.put(LogicalDatastoreType.CONFIGURATION,id, configBuilder.build());
                poolOutputBuilder.setResult("Success");
            } else {
                poolOutputBuilder.setResult("Please input ip pool in the correct format like '10.0.0.0/8' ");
            }
            break;
        case OTHER:
            break;
        }
        return RpcResultBuilder.success(poolOutputBuilder.build()).buildFuture();
    }

    @Override
    public Future<RpcResult<RequestAddressOutput>> requestAddress(RequestAddressInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Check if the ip block is legal
     * @param text
     * @return
     */
    private boolean checkIpBlocks(String text) {
        if (text != null && !text.isEmpty()) {
            String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)/([1-9]|[1-2]\\d|30|31)$";
            if (text.matches(regex)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}

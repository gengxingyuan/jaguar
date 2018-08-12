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
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamService;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestAddressInput;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.RequestAddressOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpamProvider implements IpamService {
    private static final Logger LOG = LoggerFactory.getLogger(IpamProvider.class);

    private final DataBroker dataBroker;

    public IpamProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public Future<RpcResult<RequestAddressOutput>> requestAddress(RequestAddressInput input) {

        return null;
    }

    public void close() {

    }
}

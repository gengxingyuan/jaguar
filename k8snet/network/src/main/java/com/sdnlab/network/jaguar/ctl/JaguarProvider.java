/*
 * Copyright © 2017 Future Network and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.sdnlab.network.jaguar.ctl;

import com.sdnlab.network.jaguar.ctl.listener.NodeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaguarProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JaguarProvider.class);

    private final DataBroker dataBroker;
    private NodeListener nodeListener;

    public JaguarProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        nodeListener = new NodeListener(dataBroker);
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("JaguarProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("JaguarProvider Closed");
    }
}
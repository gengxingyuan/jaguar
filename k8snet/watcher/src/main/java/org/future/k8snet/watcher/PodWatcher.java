/*
 * Copyright (c) 2017 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.watcher;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PodWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(PodWatcher.class);

    private final DataBroker dataBroker;

    public PodWatcher(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        LOG.info("new NodeWatcher");
    }

    public void close() {
    }
}

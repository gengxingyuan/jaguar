/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.sdnlab.k8snet.ipam;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DefaultIpManagerTest {

    @Test
    public void testDistributedIp() {
        DefaultIpManager ipManager = new DefaultIpManager("10.0.0.0/8");
        String ipBlock = ipManager.distributeIp(200);
        assertEquals(ipBlock,"10.0.0.0/24");
        ipBlock = ipManager.distributeIp(200);
        assertEquals(ipBlock,"10.0.1.0/24");
        ipBlock = ipManager.distributeIp(100);
        assertEquals(ipBlock,"10.0.2.0/25");
        ipBlock = ipManager.distributeIp(100);
        assertEquals(ipBlock,"10.0.2.128/25");
    }
}

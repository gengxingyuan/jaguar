/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.ipam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.future.k8snet.ipam.util.IpDistributor;

/**
 * Default IP address manager.
 * The default IP address manager allocates IP address blocks according to the needs of each Node
 * to improve address utilization.
 * @author Zhi Yi Fang
 *
 */
public class DefaultIpManager {
    private List<String> availableIpBlocks;
    private IpDistributor ipDistributor;

    /**
     * Get the available Ip blocks.
     * @return list of ip blocks
     */
    public List<String> getAvailableIpBlocks() {
        return availableIpBlocks;
    }

    public DefaultIpManager(String ipBlock) {
        this.availableIpBlocks = new ArrayList<>();
        availableIpBlocks.add(ipBlock);
    }

    /**
     * Find the most suitable ip blocks according to the demand.
     * @return cidr ip string
     */
    public String distributeIp(int demand) {
        int demandBit = (int) Math.ceil((Math.log(demand + 2) / Math.log(2)));
        sortIpBlocks(availableIpBlocks);
        String tmpBlock = "";
        for (String block:availableIpBlocks) {
            int host = 32 - Integer.valueOf(block.split("/")[1]);
            if (demandBit == host) {
                availableIpBlocks.remove(block);
                return block;
            } else if (host > demandBit) {
                tmpBlock = block;
                availableIpBlocks.remove(block);
                break;
            }
        }
        if (!tmpBlock.equals("")) {
            ipDistributor = new IpDistributor(tmpBlock);
            String result = ipDistributor.distributeIP(demandBit);
            availableIpBlocks.addAll(ipDistributor.getAvailableIpBlocks());
            sortIpBlocks(availableIpBlocks);

            return result;
        } else {
            return "No Space";
        }
    }

    private void sortIpBlocks(List<String> availableIpBlocks) {
        Collections.sort(availableIpBlocks,new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                Integer obj1 = Integer.valueOf(o1.split("/")[1]);
                Integer obj2 = Integer.valueOf(o2.split("/")[1]);

                return obj2.compareTo(obj1);
            }

        });
    }

}

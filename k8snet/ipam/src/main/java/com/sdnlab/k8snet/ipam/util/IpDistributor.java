/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.sdnlab.k8snet.ipam.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * ip distribute implmentation.
 * @author Zhi Yi Fang
 */
public class IpDistributor {

    private String ipWithMask;
    private String ip;
    private int mask;
    private Node root;

    private class Node {
        private int val;
        private int hostBit;
        private Node left;
        private Node right;
        private Node root;

        private boolean distributed;

        Node(int val, int hostBit) {
            this.val = val;
            this.hostBit = hostBit;
        }
    }

    public String getIpWithMask() {
        return ipWithMask;
    }

    public void setIpWithMask(String ipWithMask) {
        this.ipWithMask = ipWithMask;
    }

    public IpDistributor(String ipWithMask) {
        this.ipWithMask = ipWithMask;
        String[] ipAndMask = ipWithMask.split("/");
        this.ip = ipAndMask[0];
        this.mask = Integer.valueOf(ipAndMask[1]);
        int hostBit = 32 - mask;
        root = new Node(2, hostBit--);
        root.left = new Node(0, hostBit);
        root.right = new Node(1, hostBit);
        root.left.root = root;
        root.right.root = root;
    }

    /**
     * Distribute Ip according to the host bit.
     * @param hostBit - bit count
     * @return ip string
     */
    public String distributeIP(int hostBit) {
        int result = allocateIP(root, hostBit);
        return getDistributedResult(result, hostBit);
    }

    /**
     * Get the available ip blocks in this tree.
     * @return ip blocks
     */
    public List<String> getAvailableIpBlocks() {
        List<String> ipBlocks = new ArrayList<String>();
        searchTree(root, ipBlocks);
        return ipBlocks;
    }

    private void searchTree(Node root, List<String> ipBlocks) {
        while (!root.distributed) {
            Node right = root.right;
            root = root.left;
            int vals = 0;
            int hostB = right.hostBit;
            while (right.root != null) {
                vals += right.val * Math.pow(2, right.hostBit);
                right = right.root;
            }
            ipBlocks.add(getDistributedResult(vals, hostB));
        }

    }


    private String getDistributedResult(int result, int hostBit) {
        String[] temp = this.ip.split("\\.");
        int[] ipValue = intToIP(result);
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            ipValue[i] += Integer.valueOf(temp[i]);
            buffer.append(String.valueOf(ipValue[i]));
            buffer.append(".");
        }
        buffer.deleteCharAt(buffer.length() - 1);
        buffer.append("/").append(String.valueOf(32 - hostBit));
        return buffer.toString();
    }

    private int[] intToIP(int result) {
        int[] ipValue = new int[4];
        Stack<Integer> stack = new Stack<>();
        while (result > 0) {
            stack.add(result % 256);
            result /= 256;
        }
        while (stack.size() < 4) {
            stack.add(0);
        }
        for (int i = 0; i < 4; i++) {
            ipValue[i] = stack.pop();
        }
        return ipValue;
    }

    private int allocateIP(Node node, int hostBit) {
        if (node == null) {
            return -1;
        }

        if (node.hostBit > hostBit) {
            int childBit = node.hostBit - 1;
            node.left = new Node(0, childBit);
            node.right = new Node(1, childBit);
            node.left.root = node;
            node.right.root = node;
            return allocateIP(node.left, hostBit);
        } else {
            node.distributed = true;
            int vals = 0;
            while (node.root != null) {
                vals += node.val * Math.pow(2, node.hostBit);
                node = node.root;
            }
            return vals;
        }
    }
}

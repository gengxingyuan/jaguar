/*
 * Copyright (c) 2017 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.watcher;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.pod.rev170611.Coe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.pod.rev170611.coe.Pods;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.pod.rev170611.coe.PodsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.pod.rev170611.coe.PodsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PodWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(PodWatcher.class);

    private final DataBroker dataBroker;
    private final KubernetesClient client;

    public PodWatcher(final DataBroker dataBroker,KubernetesClient client) {
        this.dataBroker = dataBroker;
        LOG.info("new PodWatcher");
        this.client = client;
        this.client.pods().watch(new Watcher<Pod>() {
            public void eventReceived(Action action, Pod pod) {
                boolean errorFlag = false;
                LOG.debug(action + ",pod" + pod);
                InstanceIdentifier<Pods> id = buildK8sPodsInstanceIdentifier(pod);
                WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
                switch (action) {
                    case ADDED:
                        tx.put(LogicalDatastoreType.CONFIGURATION,id,buildK8sPods(pod));
                        break;
                    case MODIFIED:
                        tx.merge(LogicalDatastoreType.CONFIGURATION,id,buildK8sPods(pod));
                        break;
                    case DELETED:
                        tx.delete(LogicalDatastoreType.CONFIGURATION,id);
                        break;
                    case ERROR:
                        LOG.debug("action is ERROR");
                        break;
                    default :
                        LOG.debug("DBOper is unknown!");
                }
            }

            @Override
            public void onClose(KubernetesClientException clientExcept) {
                LOG.info("client.pods().watch OnClose:" + clientExcept);
            }
        });
    }

    private Pods buildK8sPods(Pod pod) {
        PodsBuilder podsBuilder = new PodsBuilder();
        podsBuilder.setName(pod.getMetadata().getName());
        podsBuilder.setUid(new Uuid(pod.getMetadata().getUid()));
        podsBuilder.setNetworkNS(pod.getMetadata().getNamespace());
        //podsBuilder.setHostIpAddress(new IpAddress(new Ipv4Address(pod.getStatus().getHostIP())));
        //podsBuilder.setPortMacAddress(pod.getStatus().);
        //podsBuilder.setInterface();

        return podsBuilder.build();
    }

    private InstanceIdentifier<Pods> buildK8sPodsInstanceIdentifier(Pod pod) {
        return InstanceIdentifier.create(Coe.class).child(Pods.class,new PodsKey(new Uuid(pod.getMetadata().getUid())));
    }

    public void close() {
    }
}

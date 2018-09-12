/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.singleton;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import org.future.k8snet.util.DbUtil;
import org.future.k8snet.watcher.NodeWatcher;
import org.future.k8snet.watcher.PodWatcher;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.org.future.k8s.apiserver.config.rev180307.K8sApiserverConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.K8sNodesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.K8sNodesInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.coe.northbound.k8s.node.rev170829.k8s.nodes.info.K8sNodesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesClientSingleton implements ClusterSingletonService {
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesClientSingleton.class);
    private final ServiceGroupIdentifier serviceGroupIdent = ServiceGroupIdentifier.create("KubernetesClient");

    private final DataBroker dataBroker;
    final ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private K8sApiserverConfig k8sApiserverConfig;
    private KubernetesClient client;
    private NodeWatcher nodeWatcher;
    private PodWatcher podWatcher;

    public KubernetesClientSingleton(final K8sApiserverConfig k8sApiserverConfig,
                                     final DataBroker dataBroker,
                                     final ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        this.k8sApiserverConfig = k8sApiserverConfig;
        this.dataBroker = dataBroker;
        this.clusterSingletonServiceProvider = clusterSingletonServiceProvider;
        this.clusterSingletonServiceProvider.registerClusterSingletonService(this);
    }

    public void close() {
        LOG.info("KubernetesClientSingleton closed!");
    }

    public void instantiateServiceInstance() {
        Config config = new ConfigBuilder().withMasterUrl(k8sApiserverConfig.getScheme()
                + "://" + k8sApiserverConfig.getMasterIp().getIpv4Address().getValue()
                + ":" + k8sApiserverConfig.getPort()
                + "/")
                .withOauthToken("y62iwm.mpy644pu377yi56f")
                .withTrustCerts(true)
  //              .withCaCertFile("/root/pki/ca.crt")
//                .withClientCertFile("/root/pki/apiserver.crt")
//                .withClientKeyFile("/root/pki/apiserver.key")
//                .withUsername("kubernetes")
//                .withPassword("kubernetes")
                .withOauthToken("y62iwm.mpy644pu377yi56f")
                .withCaCertData("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUN5RENDQWJDZ0F3SUJBZ0lCQURBTkJna3Foa2lHOXcwQkFRc0ZBREFWTVJNd0VRWURWUVFERXdwcmRXSmwKY201bGRHVnpNQjRYRFRFNE1EZ3dOakl6TVRjeU1sb1hEVEk0TURnd016SXpNVGN5TWxvd0ZURVRNQkVHQTFVRQpBeE1LYTNWaVpYSnVaWFJsY3pDQ0FTSXdEUVlKS29aSWh2Y05BUUVCQlFBRGdnRVBBRENDQVFvQ2dnRUJBTEdtCkdyZ2tmZCsxTkxaSXF6bitrSVQ0b2dWdXh3ZDN2cWlQVWtKRC9NbjhTUy9KRFhmUlRjL0xiWDB3QkNUako3cVYKa1Q2OWtFUUZicWpnRjRka2JyckRHV3JXZ3Y1Vk1rRkQ1Y3FkUVlmWjM4ZC9OTEpySnJoSE9wVjRUTzFEL2FZLwpzRk9Sa0pXMG12eXpNRTJuMkxXNWNxeHVBSlMramtaRkRIRitIZVpLVEpVcHlLdWVQZmNUbnA0UW5wOGFydWtWCjlnY0hScmFFZTlNaGViZmZFRmdDM25hOXBCWlhXdVpUcndlUU5sbkljUjRtRHdVblo5c3FlVkRreUVlSXJ0Qi8KVmhFRjY3N1RSQnA0clJNRFF5U1RtS0I4SytMQXdRUUJoRXZTK2tKN0I1NkFtOXJqQUtIWXVvbHhvUHFjcXJzdwpEU1BvNmx2Z0dFd0xScjZEZU8wQ0F3RUFBYU1qTUNFd0RnWURWUjBQQVFIL0JBUURBZ0trTUE4R0ExVWRFd0VCCi93UUZNQU1CQWY4d0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFFV1FNQ0QxSGp5WDc0UjZDRUhUY09UM3BoN0gKQ0ViMXJuQ053cEgzaHNkdEpkcTY2Y1JJR01aU2xMSjVFaFJsUVp0dWFDcGc3SkIxaVVnZFhrZXF0UStHUjJBVQpyb3ltY1NKZ0ZCd2VHM3dBM00rOU5QZnZETE00dXhkOWtwV3RBTlI5OEVMOTZiT1RCaFlhMUxQUDVmWW4zSVd2CmVhUFVMeGhYR2ZKWXd4TVVKWnVab3cvazQrbFl1em9CbkFrVW1PRkVzRzE4SEJPakNWaFZhSVJDdEQrR21memUKbjE2dEZ0UDBJcDlUQzBReGRQZjZFRW5ZbUdVV25VS0l4K2VLajlqa08vZEZJT2VxaTdqeTFtdHNwclBGUUt0TApGMk1RSmplaWpUejV6VGphZTZ2eDBRVjFVOGRIVThjYTNVMnRjampDeWVFRWtYbVUvaTA0Mld5YXVUND0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=")
                .withClientCertData("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUM4akNDQWRxZ0F3SUJBZ0lJYWhxb0o3aGhOTjB3RFFZSktvWklodmNOQVFFTEJRQXdGVEVUTUJFR0ExVUUKQXhNS2EzVmlaWEp1WlhSbGN6QWVGdzB4T0RBNE1EWXlNekUzTWpKYUZ3MHhPVEE0TURZeU16RTNNamRhTURReApGekFWQmdOVkJBb1REbk41YzNSbGJUcHRZWE4wWlhKek1Sa3dGd1lEVlFRREV4QnJkV0psY201bGRHVnpMV0ZrCmJXbHVNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4QU1JSUJDZ0tDQVFFQTFqVGdlMmk2dlNadlBpTnkKZWpGQS9jdThnRE9xdy9rcEJmSkZNVmRaekw5OS83RytDTWpVVUxTTGZJUFRqb2NyVTFhNWxnRkdhMWJvR1VkSQp0dUFyQ0JGNVZsZHMyczlQNUFlRUpmeG9laG9FL1ZDcXVRTEJiRzQweVRyVWFjN2g3V3BvTHBya3lNSHhmVHhWCnk1ODVzVWM5M0w5NXc3ZEI2QkVrRXhLcDRUcE1wdGRoYVVnYWhEUFZFSDVxUmNybkE1eHJzdXpGazhQVFhSdi8KUFY3Z082R0xEU3VaeTh6STFoRjlaNFl5dXRtcDhsMFlCTmxEZjRweGZ2OFR5OVE0cFJhQlNscUNEWmlmdTZNagp6a29Qc3N4MlNoaVZSZmJLYlg0WWREdWJhcGJEVnlGbXB1ZzlDWWI4R0h2S2h1UEZzTkhaQWhJNkQ2eW96ZTdBCi9od2VSUUlEQVFBQm95Y3dKVEFPQmdOVkhROEJBZjhFQkFNQ0JhQXdFd1lEVlIwbEJBd3dDZ1lJS3dZQkJRVUgKQXdJd0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFEaWxLbFcyUUtvQzJZUk9zSHppRDJBYlUyL1ZaallGUlk3aQp6aWxLYys4aURDVmJOTytUT2puZ0JBZkV0d1FtRUlpQXFPRDF4a3ZJQTJmUHgxTnZUY3k1Z3J2THFHck1XV290CnAyK2RnTFFTMUkzQXhZeGJUVnpCaDVyRXpGd1Yzb2N2QlZsbHA5VkZMdVhBZjNGdUdlM0swYTRocndtdGdMZ0MKRlZsbmVqa1k2ejZKajF0U1JvK1lVSVBsRy9PVmVtTTRmOGdyZWNTVTdITzBwQVlyaFFCRE0wZWtud3hvVUtjVQozRGd0anF2bXBaR0FSTGtVamVCcktSVWt1QWc1TGdobGhhM0VOY2F2TFRGNGlKRkw5cmdUcDhHTmpBRWNLR3BXCllQREtWV1BySDNGQlJka25KL0FUajcrcjBtVUFxZ1lvQTByc2tDVmJKcnU4Qi9kK1liND0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=")
                .withClientKeyData("LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFcFFJQkFBS0NBUUVBMWpUZ2UyaTZ2U1p2UGlOeWVqRkEvY3U4Z0RPcXcva3BCZkpGTVZkWnpMOTkvN0crCkNNalVVTFNMZklQVGpvY3JVMWE1bGdGR2ExYm9HVWRJdHVBckNCRjVWbGRzMnM5UDVBZUVKZnhvZWhvRS9WQ3EKdVFMQmJHNDB5VHJVYWM3aDdXcG9McHJreU1IeGZUeFZ5NTg1c1VjOTNMOTV3N2RCNkJFa0V4S3A0VHBNcHRkaAphVWdhaERQVkVINXFSY3JuQTV4cnN1ekZrOFBUWFJ2L1BWN2dPNkdMRFN1Wnk4ekkxaEY5WjRZeXV0bXA4bDBZCkJObERmNHB4ZnY4VHk5UTRwUmFCU2xxQ0RaaWZ1Nk1qemtvUHNzeDJTaGlWUmZiS2JYNFlkRHViYXBiRFZ5Rm0KcHVnOUNZYjhHSHZLaHVQRnNOSFpBaEk2RDZ5b3plN0EvaHdlUlFJREFRQUJBb0lCQVFDUDBRYWF0TkVUcDlkdQpiOXd2WUJ4VGlkTndlNy8rUGE0bVdPZk4zZWpqeC9tdmo4V1lIa1kyUFZHZGN1QzZROVI1NnJORm5HdU1LOWcvCkNKWXdla2RKN25sNTE4NjFCaFdNY3VOdG9ZNy8wN0pmNVNZS2UyZ2tCY1laQ1ErT1RxRnZoazZXWTlhUlYyMXMKZVBMZGVIVUNxbU5GYjZVd0d6ejdzMjUyQThUYytzMytnbDBGVEgwNWYwV05uWkpvdGFtczgvV0g5NkRHQ1VmcwpYZUVyaVFJaXhtcVR1NjlaTi84SHY4ME9oVUp2TTNNUXV3SlREb2g3TGhINTJKR3BPeitORmlMWEN5UzVTNy9sCnlCM21tTUpJQWg3RHBLRFVTRnNuRG9SYjUvaDdqem92aUdEazFManRHcnlzcXRrZW5la2xUaE5aWmNEUDVHa1UKMFFOWUZDZkJBb0dCQU9laDZ6Q0hUR1hHMUxGZG13bjdLUUczWmwvY3hyMEpkNkRsR25oelVtNTFJWFdPZVcyTQoyY1M5c2poa0JYRVZ3THRFUXpvY0RYYmFCcHduc3EyQlJtM2NYMHZFV1o0L25iS0dpTHhkVDFvVUNPazNUUjNiCmszM3BrWlBrSkNUZVFJZm04clZhREdBd0xjVmxqVjRhbXpPZVJlK3pVa2I5UWJKR0l3QzdIc0pWQW9HQkFPeTkKcVR2WEVNc2NOdSs2RDZWQnpsWm9uSFBtQ1dkOHAyeWxQNDR4ZHFHdXRvS0FRbS9Nd2FTQUZISGM5OW9uVjBicQpZeUlhTXE3Q3ljYnNvNGNac2NXdmZBNnhWNXYzY3Mvc0RyU3djZEU0N0NJZklIUXJiSU1jQ0d5eDI1SGZXelZGCndQOGdod3h2bEF3TTRCYk93L01VMDI5RmNqYWNKREsrOWt3bitUd3hBb0dCQUwwd0t3Q0FBWTQvVUFsdUF1dXMKRUZvdWlaZFNvNkJTMHpxKzVWZnNHUy9PeTMxUTJTejVGZ1R6UzFWem5GNDQvaW4ySDFLZkJ3QkVJNUgzZXFEcwpMYklkaEZoR0QweUplU3dQWmk4enAxUlRlTlBETDJGcnJwUHc2YzU1VUd3c2UxaUU4VWZlUEsxenJHN0YreWtaCllocE84NkJNUHE5c3V2UDVCUzNicDh2aEFvR0JBTnh1Wkl0aFh6T2ZtODl2TVc0d2JjMnJMaklFT1RGd3NmdkoKaWZuK09IV25WaTBBKzl0WmpkeDduWTIvcjlBaTNYTWNmeDJid3lGU0ljcmxRQVpsZUUrWGJDM0tGc2NVaW9UcgpTVzRZOUlGWlBSVXdZbW1JblVzZTZRRThRalA3QXRRQmxRaDQ0d1pEUmxoS0RNVnYxS2diOWhzZlVJOWtwNWZRCmswaXAxN21oQW9HQU5rYWc0dHVVMnJvK0VlNzlkL0RmMjZPVVdLZVd3UHZBODdtaThZUm1MNk5SNDJObWsxSTkKYWZnZ1dud0ZINGZvRVRZa1VrNlFZakI0ZjVpQVdRRE85amJnajZ4dE5aZVo5bHkrUDUvUVcwSWhIZ0dSMmx0bwpWak5OdHpNUTlHSGwrMEZwU3daMlFSVjB6cTRtREM5bWdGWFJFWnJ2M1RPR3ZDeFUzMHpCMUtzPQotLS0tLUVORCBSU0EgUFJJVkFURSBLRVktLS0tLQo=")
                .build();
                //new ConfigBuilder()
                //.withMasterUrl("https://" + cluster.getEndpoint())
                //.withCaCertData(cluster.getMasterAuth().getClusterCaCertificate())
                //.withClientCertData(cluster.getMasterAuth().getClientCertificate())
                //.withClientKeyData(cluster.getMasterAuth().getClientKey())
                //.build();
        client = new DefaultKubernetesClient(config);
        k8sNodeSync();
        nodeWatcher = new NodeWatcher(dataBroker, client);
        podWatcher = new PodWatcher(dataBroker,client);
        LOG.info("k8s node sync instantiateServiceInstance");
    }

    public ListenableFuture<Void> closeServiceInstance() {
        return Futures.immediateCheckedFuture(null);
    }

    @Nonnull
    public ServiceGroupIdentifier getIdentifier() {
        return serviceGroupIdent;
    }

    private void k8sNodeSync() {
        K8sNodesInfoBuilder k8sNodesInfoBuilder = new K8sNodesInfoBuilder();
        k8sNodesInfoBuilder.setId("Nodes");

        List<Node> nodeList = client.nodes().list().getItems();
        List<K8sNodes> k8sNodesList = new ArrayList<K8sNodes>();
        for (Node node:nodeList) {
            K8sNodesBuilder k8sNodesBuilder = new K8sNodesBuilder();
            for (NodeAddress nodeAddress:node.getStatus().getAddresses()) {
                if (nodeAddress.getType().equals("InternalIP")) {
                    k8sNodesBuilder.setInternalIpAddress(new IpAddress(new Ipv4Address(nodeAddress.getAddress())));
                }
                if (nodeAddress.getType().equals("ExternalIp")) {
                    k8sNodesBuilder.setExternalIpAddress(new IpAddress(new Ipv4Address(nodeAddress.getAddress())));
                }
            }
            k8sNodesBuilder.setUid(new Uuid(node.getMetadata().getUid()))
                .setHostName(node.getMetadata().getName());
            k8sNodesBuilder.setMaxPodNum(node.getStatus().getCapacity().get("pods").getAmount());
            k8sNodesList.add(k8sNodesBuilder.build());
        }
        k8sNodesInfoBuilder.setK8sNodes(k8sNodesList);

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<K8sNodesInfo> id = InstanceIdentifier.create(K8sNodesInfo.class);
        tx.merge(LogicalDatastoreType.CONFIGURATION,id,k8sNodesInfoBuilder.build());

        DbUtil.doSubmit(tx);

    }
}

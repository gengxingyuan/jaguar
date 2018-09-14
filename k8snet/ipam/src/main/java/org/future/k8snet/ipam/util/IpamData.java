/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.ipam.util;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamAttr.SchemeType;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.IpamConfig;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.ip.schemes.IpSchemes;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.ip.schemes.IpSchemesBuilder;
import org.opendaylight.yang.gen.v1.org.future.ipam.rev180807.ip.schemes.IpSchemesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class IpamData {

    private DataBroker dataBroker;
    private InstanceIdentifier<IpamConfig> root;
    private static final Logger LOG = LoggerFactory.getLogger(IpamData.class);

    public IpamData(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        root = InstanceIdentifier.builder(IpamConfig.class).build();
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void addScheme(SchemeType schemeType, String ipBlock) {
        IpSchemesBuilder builder = new IpSchemesBuilder();
        builder.setKey(new IpSchemesKey(schemeType));
        builder.setSchemeType(schemeType);
        builder.setNetwork(ipBlock);
        WriteTransaction write = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<IpSchemes> id = root.child(IpSchemes.class, new IpSchemesKey(schemeType));
        write.put(LogicalDatastoreType.CONFIGURATION, id, builder.build());
        CheckedFuture<Void, TransactionCommitFailedException> future = write.submit();

        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(final Void result) {
            // Committed successfully
                LOG.debug("Add ip distribution scheme -- Committedsuccessfully ");
            }

            @Override
            public void onFailure(final Throwable throwable) {
            // Transaction failed

                if (throwable instanceof OptimisticLockFailedException) {
                // Failed because of concurrent transaction modifying same
                // data
                    LOG.error(
                            "Add ip distribution scheme -- Failed because of concurrent transaction modifying same data"
                    );
                } else {
                    // Some other type of TransactionCommitFailedException
                    LOG.error(
                            "Add ip distribution scheme -- Some other type of TransactionCommitFailedException",
                            throwable);
                }
            }
            }, MoreExecutors.directExecutor());
    }

    public List<IpSchemes> getSchemes() {
        ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<IpamConfig>, ReadFailedException> future = read
                .read(LogicalDatastoreType.CONFIGURATION, root);
        List<IpSchemes> ipSchemes = null;
        try {
            Optional<IpamConfig> result = future.get();
            if (result.isPresent()) {
                ipSchemes = result.get().getIpSchemes();
            }
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
        return ipSchemes;
    }

}

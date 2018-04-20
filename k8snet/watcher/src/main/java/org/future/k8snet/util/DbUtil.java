/*
 * Copyright (c) 2018 Future Network. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.k8snet.util;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DbUtil.class);

    public static void doSubmit(WriteTransaction tx) {
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();

        Futures.addCallback(submitFuture, new FutureCallback<Void>() {

            @Override
            public void onSuccess(final Void result) {
                // Committed successfully
                LOG.debug("K8s Node -- Committedsuccessfully ");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                // Transaction failed

                if (throwable instanceof OptimisticLockFailedException) {
                    // Failed because of concurrent transaction modifying same
                    // data
                    LOG.error("K8s Node -- Failed because of concurrent transaction modifying same data");
                } else {
                    // Some other type of TransactionCommitFailedException
                    LOG.error("K8s Node -- Some other type of TransactionCommitFailedException", throwable);
                }
            }
        }, MoreExecutors.directExecutor());
    }
}

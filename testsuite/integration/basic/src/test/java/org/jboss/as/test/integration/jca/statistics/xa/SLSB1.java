/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.jca.statistics.xa;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;

/**
 * @author dsimko@redhat.com
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class SLSB1 implements SLSB {

    @PersistenceContext(unitName = "testxapu")
    private EntityManager em;



    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Resource
    private UserTransaction tx;

    @Inject
    private TransactionCheckerSingleton checker;

    @Override
    public void commit() throws Exception {
        tx.begin();
        TxTestUtil.enlistTestXAResource(tm.getTransaction(), checker);
        em.persist(new TestEntity());
        tx.commit();
    }

    @Override
    public void rollback() throws Exception {

        tx.begin();
        TxTestUtil.enlistTestXAResource(tm.getTransaction(), checker);

        em.persist(new TestEntity());
        em.flush();
        tx.rollback();
    }

}

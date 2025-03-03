/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.singleton.service;

import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactoryProvider;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum SingletonDefaultCacheRequirement implements UnaryServiceNameFactoryProvider {
    @Deprecated SINGLETON_SERVICE_BUILDER_FACTORY(org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY),
    SINGLETON_SERVICE_CONFIGURATOR_FACTORY(org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY),
    ;
    private final UnaryServiceNameFactory factory;

    SingletonDefaultCacheRequirement(UnaryRequirement requirement) {
        this.factory = new UnaryRequirementServiceNameFactory(requirement);
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}

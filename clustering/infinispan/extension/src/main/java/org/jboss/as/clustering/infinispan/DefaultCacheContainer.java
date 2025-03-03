/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import java.util.Collections;
import java.util.List;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.EmbeddedCacheManagerAdmin;
import org.infinispan.manager.impl.AbstractDelegatingEmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.LocalModeAddress;

/**
 * EmbeddedCacheManager decorator that overrides the default cache semantics of a cache manager.
 * @author Paul Ferraro
 */
public class DefaultCacheContainer extends AbstractDelegatingEmbeddedCacheManager {

    private final EmbeddedCacheManagerAdmin administrator;

    public DefaultCacheContainer(EmbeddedCacheManager container) {
        super(container);
        this.administrator = new DefaultCacheContainerAdmin(this);
    }

    @Override
    public Address getAddress() {
        Address address = super.getAddress();
        return (address != null) ? address : LocalModeAddress.INSTANCE;
    }

    @Override
    public Address getCoordinator() {
        Address coordinator = super.getCoordinator();
        return (coordinator != null) ? coordinator : LocalModeAddress.INSTANCE;
    }

    @Override
    public List<Address> getMembers() {
        List<Address> members = super.getMembers();
        return (members != null) ? members : Collections.singletonList(LocalModeAddress.INSTANCE);
    }

    @Override
    public void start() {
        // No-op.  Lifecycle is managed by container
    }

    @Override
    public void stop() {
        // No-op.  Lifecycle is managed by container
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.wrap(this.cm.<K, V>getCache());
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return this.wrap(this.cm.<K, V>getCache(cacheName));
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, boolean createIfAbsent) {
        Cache<K, V> cache = this.cm.<K, V>getCache(cacheName, createIfAbsent);
        return (cache != null) ? this.wrap(cache) : null;
    }

    private <K, V> Cache<K, V> wrap(Cache<K, V> cache) {
        return new DefaultCache<>(this, cache.getAdvancedCache());
    }

    @Override
    public EmbeddedCacheManager startCaches(String... cacheNames) {
        super.startCaches(cacheNames);
        return this;
    }

    @Override
    public EmbeddedCacheManagerAdmin administration() {
        return this.administrator;
    }

    @Override
    public synchronized <K, V> Cache<K, V> createCache(String name, Configuration configuration) {
        return this.administrator.createCache(name, configuration);
    }

    @Override
    public void undefineConfiguration(String configurationName) {
        super.undefineConfiguration(configurationName);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EmbeddedCacheManager)) return false;
        return this.toString().equals(object.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return this.getCacheManagerConfiguration().cacheManagerName();
    }
}

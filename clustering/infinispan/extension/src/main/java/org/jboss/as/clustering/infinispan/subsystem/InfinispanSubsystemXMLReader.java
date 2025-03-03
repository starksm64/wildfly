/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.InfinispanLogger.ROOT_LOGGER;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.ColumnAttribute;
import org.jboss.as.clustering.infinispan.subsystem.remote.ConnectionPoolResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteClusterResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.SecurityResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * XML reader for the Infinispan subsystem.
 *
 * @author Paul Ferraro
 */
public class InfinispanSubsystemXMLReader implements XMLElementReader<List<ModelNode>> {

    private final InfinispanSchema schema;

    InfinispanSubsystemXMLReader(InfinispanSchema schema) {
        this.schema = schema;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> result) throws XMLStreamException {

        Map<PathAddress, ModelNode> operations = new LinkedHashMap<>();

        PathAddress address = PathAddress.pathAddress(InfinispanSubsystemResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case CACHE_CONTAINER: {
                    this.parseContainer(reader, address, operations);
                    break;
                }
                case REMOTE_CACHE_CONTAINER: {
                    this.parseRemoteContainer(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        result.addAll(operations.values());
    }

    @SuppressWarnings("deprecation")
    private void parseContainer(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = subsystemAddress.append(CacheContainerResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case DEFAULT_CACHE: {
                    readAttribute(reader, i, operation, CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE);
                    break;
                }
                case ALIASES: {
                    readAttribute(reader, i, operation, CacheContainerResourceDefinition.ListAttribute.ALIASES);
                    break;
                }
                case MODULE: {
                    if (this.schema.since(InfinispanSchema.VERSION_12_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    readAttribute(reader, i, operation, CacheContainerResourceDefinition.ListAttribute.MODULES);
                    break;
                }
                case STATISTICS_ENABLED: {
                    readAttribute(reader, i, operation, CacheContainerResourceDefinition.Attribute.STATISTICS_ENABLED);
                    break;
                }
                case MODULES: {
                    if (this.schema.since(InfinispanSchema.VERSION_12_0)) {
                        readAttribute(reader, i, operation, CacheContainerResourceDefinition.ListAttribute.MODULES);
                        break;
                    }
                }
                case MARSHALLER: {
                    if (this.schema.since(InfinispanSchema.VERSION_13_0)) {
                        readAttribute(reader, i, operation, CacheContainerResourceDefinition.Attribute.MARSHALLER);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case TRANSPORT: {
                    this.parseTransport(reader, address, operations);
                    break;
                }
                case LOCAL_CACHE: {
                    this.parseLocalCache(reader, address, operations);
                    break;
                }
                case INVALIDATION_CACHE: {
                    this.parseInvalidationCache(reader, address, operations);
                    break;
                }
                case REPLICATED_CACHE: {
                    this.parseReplicatedCache(reader, address, operations);
                    break;
                }
                case DISTRIBUTED_CACHE: {
                    this.parseDistributedCache(reader, address, operations);
                    break;
                }
                case EXPIRATION_THREAD_POOL: {
                    this.parseScheduledThreadPool(ScheduledThreadPoolResourceDefinition.EXPIRATION, reader, address, operations);
                    break;
                }
                case LISTENER_THREAD_POOL: {
                    this.parseThreadPool(ThreadPoolResourceDefinition.LISTENER, reader, address, operations);
                    break;
                }
                case ASYNC_OPERATIONS_THREAD_POOL:
                case PERSISTENCE_THREAD_POOL:
                case REMOTE_COMMAND_THREAD_POOL:
                case STATE_TRANSFER_THREAD_POOL:
                case TRANSPORT_THREAD_POOL: {
                    if (this.schema.since(InfinispanSchema.VERSION_14_0)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    ROOT_LOGGER.elementIgnored(element.getLocalName());
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case SCATTERED_CACHE: {
                    this.parseScatteredCache(reader, address, operations);
                    break;
                }
                case BLOCKING_THREAD_POOL: {
                    if (this.schema.since(InfinispanSchema.VERSION_11_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.BLOCKING, reader, address, operations);
                        break;
                    }
                }
                case NON_BLOCKING_THREAD_POOL: {
                    if (this.schema.since(InfinispanSchema.VERSION_11_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.NON_BLOCKING, reader, address, operations);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    @SuppressWarnings("static-method")
    private void parseTransport(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = containerAddress.append(JGroupsTransportResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(containerAddress.append(TransportResourceDefinition.WILDCARD_PATH), operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case LOCK_TIMEOUT: {
                    readAttribute(reader, i, operation, JGroupsTransportResourceDefinition.Attribute.LOCK_TIMEOUT);
                    break;
                }
                case CHANNEL: {
                    readAttribute(reader, i, operation, JGroupsTransportResourceDefinition.Attribute.CHANNEL);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        ParseUtils.requireNoContent(reader);
    }

    private void parseLocalCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = containerAddress.append(LocalCacheResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseCacheAttribute(reader, i, address, operations);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseCacheElement(reader, address, operations);
        }
    }

    private void parseReplicatedCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = containerAddress.append(ReplicatedCacheResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseClusteredCacheAttribute(reader, i, address, operations);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseSharedStateCacheElement(reader, address, operations);
        }
    }

    private void parseScatteredCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = containerAddress.append(ScatteredCacheResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case BIAS_LIFESPAN: {
                    readAttribute(reader, i, operation, ScatteredCacheResourceDefinition.Attribute.BIAS_LIFESPAN);
                    break;
                }
                case INVALIDATION_BATCH_SIZE: {
                    readAttribute(reader, i, operation, ScatteredCacheResourceDefinition.Attribute.INVALIDATION_BATCH_SIZE);
                    break;
                }
                default: {
                    this.parseSegmentedCacheAttribute(reader, i, address, operations);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseSharedStateCacheElement(reader, address, operations);
        }
    }

    private void parseDistributedCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = containerAddress.append(DistributedCacheResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OWNERS: {
                    readAttribute(reader, i, operation, DistributedCacheResourceDefinition.Attribute.OWNERS);
                    break;
                }
                case L1_LIFESPAN: {
                    readAttribute(reader, i, operation, DistributedCacheResourceDefinition.Attribute.L1_LIFESPAN);
                    break;
                }
                case CAPACITY_FACTOR: {
                    readAttribute(reader, i, operation, DistributedCacheResourceDefinition.Attribute.CAPACITY_FACTOR);
                    break;
                }
                default: {
                    this.parseSegmentedCacheAttribute(reader, i, address, operations);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseSharedStateCacheElement(reader, address, operations);
        }
    }

    private void parseInvalidationCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = containerAddress.append(InvalidationCacheResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseClusteredCacheAttribute(reader, i, address, operations);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseCacheElement(reader, address, operations);
        }
    }

    @SuppressWarnings("deprecation")
    private void parseCacheAttribute(XMLExtendedStreamReader reader, int index, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case NAME: {
                // Already read
                break;
            }
            case MODULE: {
                if (this.schema.since(InfinispanSchema.VERSION_12_0)) {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                readAttribute(reader, index, operation, CacheResourceDefinition.ListAttribute.MODULES);
                break;
            }
            case STATISTICS_ENABLED: {
                readAttribute(reader, index, operation, CacheResourceDefinition.Attribute.STATISTICS_ENABLED);
                break;
            }
            case MODULES: {
                if (this.schema.since(InfinispanSchema.VERSION_12_0)) {
                    readAttribute(reader, index, operation, CacheResourceDefinition.ListAttribute.MODULES);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void parseSegmentedCacheAttribute(XMLExtendedStreamReader reader, int index, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case SEGMENTS: {
                readAttribute(reader, index, operation, SegmentedCacheResourceDefinition.Attribute.SEGMENTS);
                break;
            }
            case CONSISTENT_HASH_STRATEGY: {
                if (this.schema.since(InfinispanSchema.VERSION_14_0)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
                ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                break;
            }
            default: {
                this.parseClusteredCacheAttribute(reader, index, address, operations);
            }
        }
    }

    private void parseClusteredCacheAttribute(XMLExtendedStreamReader reader, int index, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case REMOTE_TIMEOUT: {
                readAttribute(reader, index, operation, ClusteredCacheResourceDefinition.Attribute.REMOTE_TIMEOUT);
                break;
            }
            default: {
                this.parseCacheAttribute(reader, index, address, operations);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void parseCacheElement(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        XMLElement element = XMLElement.forName(reader.getLocalName());
        switch (element) {
            case EXPIRATION: {
                this.parseExpiration(reader, cacheAddress, operations);
                break;
            }
            case LOCKING: {
                this.parseLocking(reader, cacheAddress, operations);
                break;
            }
            case TRANSACTION: {
                this.parseTransaction(reader, cacheAddress, operations);
                break;
            }
            case STORE: {
                this.parseCustomStore(reader, cacheAddress, operations);
                break;
            }
            case FILE_STORE: {
                this.parseFileStore(reader, cacheAddress, operations);
                break;
            }
            case REMOTE_STORE: {
                this.parseRemoteStore(reader, cacheAddress, operations);
                break;
            }
            case HOTROD_STORE: {
                this.parseHotRodStore(reader, cacheAddress, operations);
                break;
            }
            case JDBC_STORE: {
                this.parseJDBCStore(reader, cacheAddress, operations);
                break;
            }
            case BINARY_KEYED_JDBC_STORE: {
                if (this.schema.since(InfinispanSchema.VERSION_14_0)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
                ROOT_LOGGER.elementIgnored(element.getLocalName());
                this.parseBinaryKeyedJDBCStore(reader, cacheAddress, operations);
                break;
            }
            case MIXED_KEYED_JDBC_STORE: {
                if (this.schema.since(InfinispanSchema.VERSION_14_0)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
                this.parseMixedKeyedJDBCStore(reader, cacheAddress, operations);
                break;
            }
            case OBJECT_MEMORY: {
                if (this.schema.since(InfinispanSchema.VERSION_11_0)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
                this.parseHeapMemory(reader, cacheAddress, operations);
                break;
            }
            case BINARY_MEMORY: {
                if (this.schema.since(InfinispanSchema.VERSION_11_0)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
                this.parseBinaryMemory(reader, cacheAddress, operations);
                break;
            }
            case OFF_HEAP_MEMORY: {
                this.parseOffHeapMemory(reader, cacheAddress, operations);
                break;
            }
            case HEAP_MEMORY: {
                if (this.schema.since(InfinispanSchema.VERSION_11_0)) {
                    this.parseHeapMemory(reader, cacheAddress, operations);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseSharedStateCacheElement(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        XMLElement element = XMLElement.forName(reader.getLocalName());
        switch (element) {
            case STATE_TRANSFER: {
                this.parseStateTransfer(reader, address, operations);
                break;
            }
            case BACKUPS: {
                this.parseBackups(reader, address, operations);
                break;
            }
            case PARTITION_HANDLING: {
                this.parsePartitionHandling(reader, address, operations);
                break;
            }
            default: {
                this.parseCacheElement(reader, address, operations);
            }
        }
    }

    @SuppressWarnings("static-method")
    private void parsePartitionHandling(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(PartitionHandlingResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    readAttribute(reader, i, operation, PartitionHandlingResourceDefinition.Attribute.ENABLED);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    @SuppressWarnings("static-method")
    private void parseStateTransfer(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(StateTransferResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TIMEOUT: {
                    readAttribute(reader, i, operation, StateTransferResourceDefinition.Attribute.TIMEOUT);
                    break;
                }
                case CHUNK_SIZE: {
                    readAttribute(reader, i, operation, StateTransferResourceDefinition.Attribute.CHUNK_SIZE);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseBackups(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(BackupsResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case BACKUP: {
                    this.parseBackup(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    @SuppressWarnings("static-method")
    private void parseBackup(XMLExtendedStreamReader reader, PathAddress backupsAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String site = require(reader, XMLAttribute.SITE);
        PathAddress address = backupsAddress.append(BackupResourceDefinition.pathElement(site));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SITE: {
                    // Already parsed
                    break;
                }
                case STRATEGY: {
                    readAttribute(reader, i, operation, BackupResourceDefinition.Attribute.STRATEGY);
                    break;
                }
                case BACKUP_FAILURE_POLICY: {
                    readAttribute(reader, i, operation, BackupResourceDefinition.Attribute.FAILURE_POLICY);
                    break;
                }
                case TIMEOUT: {
                    readAttribute(reader, i, operation, BackupResourceDefinition.Attribute.TIMEOUT);
                    break;
                }
                case ENABLED: {
                    readAttribute(reader, i, operation, BackupResourceDefinition.Attribute.ENABLED);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case TAKE_OFFLINE: {
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case TAKE_OFFLINE_AFTER_FAILURES: {
                                readAttribute(reader, i, operation, BackupResourceDefinition.TakeOfflineAttribute.AFTER_FAILURES);
                                break;
                            }
                            case TAKE_OFFLINE_MIN_WAIT: {
                                readAttribute(reader, i, operation, BackupResourceDefinition.TakeOfflineAttribute.MIN_WAIT);
                                break;
                            }
                            default: {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                        }
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    @SuppressWarnings("static-method")
    private void parseLocking(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(LockingResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ISOLATION: {
                    readAttribute(reader, i, operation, LockingResourceDefinition.Attribute.ISOLATION);
                    break;
                }
                case STRIPING: {
                    readAttribute(reader, i, operation, LockingResourceDefinition.Attribute.STRIPING);
                    break;
                }
                case ACQUIRE_TIMEOUT: {
                    readAttribute(reader, i, operation, LockingResourceDefinition.Attribute.ACQUIRE_TIMEOUT);
                    break;
                }
                case CONCURRENCY_LEVEL: {
                    readAttribute(reader, i, operation, LockingResourceDefinition.Attribute.CONCURRENCY);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseTransaction(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(TransactionResourceDefinition.PATH);
        ModelNode operation = operations.get(address);
        if (operation == null) {
            operation = Util.createAddOperation(address);
            operations.put(address, operation);
        }

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STOP_TIMEOUT: {
                    readAttribute(reader, i, operation, TransactionResourceDefinition.Attribute.STOP_TIMEOUT);
                    break;
                }
                case MODE: {
                    readAttribute(reader, i, operation, TransactionResourceDefinition.Attribute.MODE);
                    break;
                }
                case LOCKING: {
                    readAttribute(reader, i, operation, TransactionResourceDefinition.Attribute.LOCKING);
                    break;
                }
                case COMPLETE_TIMEOUT: {
                    if (this.schema.since(InfinispanSchema.VERSION_13_0)) {
                        readAttribute(reader, i, operation, TransactionResourceDefinition.Attribute.COMPLETE_TIMEOUT);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    @SuppressWarnings("static-method")
    private void parseExpiration(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(ExpirationResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_IDLE: {
                    readAttribute(reader, i, operation, ExpirationResourceDefinition.Attribute.MAX_IDLE);
                    break;
                }
                case LIFESPAN: {
                    readAttribute(reader, i, operation, ExpirationResourceDefinition.Attribute.LIFESPAN);
                    break;
                }
                case INTERVAL: {
                    readAttribute(reader, i, operation, ExpirationResourceDefinition.Attribute.INTERVAL);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseHeapMemory(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(HeapMemoryResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SIZE_UNIT: {
                    if (this.schema.since(InfinispanSchema.VERSION_11_0)) {
                        readAttribute(reader, i, operation, HeapMemoryResourceDefinition.Attribute.SIZE_UNIT);
                        break;
                    }
                }
                default: {
                    this.parseMemoryAttribute(reader, i, operation);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseBinaryMemory(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(OffHeapMemoryResourceDefinition.BINARY_PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseBinaryMemoryAttribute(reader, i, operation);
        }
        ParseUtils.requireNoContent(reader);
    }

    @SuppressWarnings("deprecation")
    private void parseOffHeapMemory(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(OffHeapMemoryResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CAPACITY: {
                    if (this.schema.since(InfinispanSchema.VERSION_14_0)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case SIZE_UNIT: {
                    if (this.schema.since(InfinispanSchema.VERSION_11_0)) {
                        readAttribute(reader, i, operation, OffHeapMemoryResourceDefinition.Attribute.SIZE_UNIT);
                        break;
                    }
                }
                default: {
                    this.parseBinaryMemoryAttribute(reader, i, operation);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    @SuppressWarnings("deprecation")
    private void parseBinaryMemoryAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {

        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case EVICTION_TYPE: {
                ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                break;
            }
            default: {
                this.parseMemoryAttribute(reader, index, operation);
            }
        }
    }

    @SuppressWarnings("static-method")
    private void parseMemoryAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {

        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case SIZE: {
                readAttribute(reader, index, operation, MemoryResourceDefinition.Attribute.SIZE);
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseCustomStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(CustomStoreResourceDefinition.PATH);
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLASS: {
                    readAttribute(reader, i, operation, CustomStoreResourceDefinition.Attribute.CLASS);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        if (!operation.hasDefined(CustomStoreResourceDefinition.Attribute.CLASS.getName())) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(XMLAttribute.CLASS));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseStoreElement(reader, address, operations);
        }
    }

    private void parseFileStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(FileStoreResourceDefinition.PATH);
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case RELATIVE_TO: {
                    readAttribute(reader, i, operation, FileStoreResourceDefinition.Attribute.RELATIVE_TO);
                    break;
                }
                case PATH: {
                    readAttribute(reader, i, operation, FileStoreResourceDefinition.Attribute.RELATIVE_PATH);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseStoreElement(reader, address, operations);
        }
    }

    @SuppressWarnings("deprecation")
    private void parseRemoteStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(RemoteStoreResourceDefinition.PATH);
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CACHE: {
                    readAttribute(reader, i, operation, RemoteStoreResourceDefinition.Attribute.CACHE);
                    break;
                }
                case SOCKET_TIMEOUT: {
                    readAttribute(reader, i, operation, RemoteStoreResourceDefinition.Attribute.SOCKET_TIMEOUT);
                    break;
                }
                case TCP_NO_DELAY: {
                    readAttribute(reader, i, operation, RemoteStoreResourceDefinition.Attribute.TCP_NO_DELAY);
                    break;
                }
                case REMOTE_SERVERS: {
                    readAttribute(reader, i, operation, RemoteStoreResourceDefinition.Attribute.SOCKET_BINDINGS);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseStoreElement(reader, address, operations);
        }
    }

    private void parseHotRodStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = cacheAddress.append(HotRodStoreResourceDefinition.PATH);
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CACHE_CONFIGURATION: {
                    readAttribute(reader, i, operation, HotRodStoreResourceDefinition.Attribute.CACHE_CONFIGURATION);
                    break;
                }
                case REMOTE_CACHE_CONTAINER: {
                    readAttribute(reader, i, operation, HotRodStoreResourceDefinition.Attribute.REMOTE_CACHE_CONTAINER);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseStoreElement(reader, address, operations);
        }
    }

    private void parseJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(JDBCStoreResourceDefinition.PATH);
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case TABLE: {
                    this.parseJDBCStoreStringTable(reader, address, operations);
                    break;
                }
                default: {
                    this.parseStoreElement(reader, address, operations);
                }
            }
        }
    }

    private void parseBinaryKeyedJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(JDBCStoreResourceDefinition.PATH);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case BINARY_KEYED_TABLE: {
                    this.skipJDBCStoreTableElements(reader, null);
                    break;
                }
                default: {
                    this.parseStoreElement(reader, address, operations);
                }
            }
        }
    }

    private void parseMixedKeyedJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(JDBCStoreResourceDefinition.PATH);
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case BINARY_KEYED_TABLE: {
                    ROOT_LOGGER.elementIgnored(element.getLocalName());
                    this.skipJDBCStoreTableElements(reader, null);
                    break;
                }
                case STRING_KEYED_TABLE: {
                    this.parseJDBCStoreStringTable(reader, address, operations);
                    break;
                }
                default: {
                    this.parseStoreElement(reader, address, operations);
                }
            }
        }
    }

    private void parseJDBCStoreAttributes(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DIALECT: {
                    readAttribute(reader, i, operation, JDBCStoreResourceDefinition.Attribute.DIALECT);
                    break;
                }
                case DATA_SOURCE: {
                    readAttribute(reader, i, operation, JDBCStoreResourceDefinition.Attribute.DATA_SOURCE);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }
    }

    private void parseJDBCStoreStringTable(XMLExtendedStreamReader reader, PathAddress storeAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = storeAddress.append(StringTableResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(storeAddress.getParent().append(StoreResourceDefinition.WILDCARD_PATH).append(StringTableResourceDefinition.PATH), operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PREFIX: {
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                default: {
                    this.parseJDBCStoreTableAttribute(reader, i, operation);
                }
            }
        }

        this.parseJDBCStoreTableElements(reader, operation);
    }

    private void parseJDBCStoreTableAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {

        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case FETCH_SIZE: {
                readAttribute(reader, index, operation, TableResourceDefinition.Attribute.FETCH_SIZE);
                break;
            }
            case CREATE_ON_START: {
                if (this.schema.since(InfinispanSchema.VERSION_9_0)) {
                    readAttribute(reader, index, operation, TableResourceDefinition.Attribute.CREATE_ON_START);
                    break;
                }
            }
            case DROP_ON_STOP: {
                if (this.schema.since(InfinispanSchema.VERSION_9_0)) {
                    readAttribute(reader, index, operation, TableResourceDefinition.Attribute.DROP_ON_STOP);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseJDBCStoreTableElements(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case ID_COLUMN: {
                    this.parseJDBCStoreColumn(reader, ColumnAttribute.ID, operation.get(TableResourceDefinition.ColumnAttribute.ID.getName()).setEmptyObject());
                    break;
                }
                case DATA_COLUMN: {
                    this.parseJDBCStoreColumn(reader, ColumnAttribute.DATA, operation.get(TableResourceDefinition.ColumnAttribute.DATA.getName()).setEmptyObject());
                    break;
                }
                case TIMESTAMP_COLUMN: {
                    this.parseJDBCStoreColumn(reader, ColumnAttribute.TIMESTAMP, operation.get(TableResourceDefinition.ColumnAttribute.TIMESTAMP.getName()).setEmptyObject());
                    break;
                }
                case SEGMENT_COLUMN: {
                    if (this.schema.since(InfinispanSchema.VERSION_10_0)) {
                        this.parseJDBCStoreColumn(reader, ColumnAttribute.SEGMENT, operation.get(TableResourceDefinition.ColumnAttribute.SEGMENT.getName()).setEmptyObject());
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void skipJDBCStoreTableElements(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case ID_COLUMN: {
                    ROOT_LOGGER.elementIgnored(element.getLocalName());
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case DATA_COLUMN: {
                    ROOT_LOGGER.elementIgnored(element.getLocalName());
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case TIMESTAMP_COLUMN: {
                    ROOT_LOGGER.elementIgnored(element.getLocalName());
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case SEGMENT_COLUMN: {
                    if (this.schema.since(InfinispanSchema.VERSION_10_0)) {
                        ROOT_LOGGER.elementIgnored(element.getLocalName());
                        ParseUtils.requireNoContent(reader);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    @SuppressWarnings("static-method")
    private void parseJDBCStoreColumn(XMLExtendedStreamReader reader, ColumnAttribute columnAttribute, ModelNode column) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    readAttribute(reader, i, column, columnAttribute.getColumnName());
                    break;
                }
                case TYPE: {
                    readAttribute(reader, i, column, columnAttribute.getColumnType());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    @SuppressWarnings("deprecation")
    private void parseStoreAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case SHARED: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.SHARED);
                break;
            }
            case PRELOAD: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.PRELOAD);
                break;
            }
            case PASSIVATION: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.PASSIVATION);
                break;
            }
            case FETCH_STATE: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.FETCH_STATE);
                break;
            }
            case PURGE: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.PURGE);
                break;
            }
            case SINGLETON: {
                if (this.schema.since(InfinispanSchema.VERSION_14_0)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
                ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                break;
            }
            case MAX_BATCH_SIZE: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.MAX_BATCH_SIZE);
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseStoreElement(XMLExtendedStreamReader reader, PathAddress storeAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(storeAddress.getParent().append(StoreResourceDefinition.WILDCARD_PATH));

        XMLElement element = XMLElement.forName(reader.getLocalName());
        switch (element) {
            case PROPERTY: {
                ParseUtils.requireSingleAttribute(reader, XMLAttribute.NAME.getLocalName());
                readElement(reader, operation, StoreResourceDefinition.Attribute.PROPERTIES);
                break;
            }
            case WRITE_BEHIND: {
                this.parseStoreWriteBehind(reader, storeAddress, operations);
                break;
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void parseStoreWriteBehind(XMLExtendedStreamReader reader, PathAddress storeAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = storeAddress.append(StoreWriteBehindResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(storeAddress.append(StoreWriteResourceDefinition.WILDCARD_PATH), operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MODIFICATION_QUEUE_SIZE: {
                    readAttribute(reader, i, operation, StoreWriteBehindResourceDefinition.Attribute.MODIFICATION_QUEUE_SIZE);
                    break;
                }
                case THREAD_POOL_SIZE: {
                    if (this.schema.since(InfinispanSchema.VERSION_11_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    @SuppressWarnings("static-method")
    private <P extends ThreadPoolDefinition & ResourceDefinitionProvider> void parseThreadPool(P pool, XMLExtendedStreamReader reader, PathAddress parentAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = parentAddress.append(pool.getPathElement());
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MIN_THREADS: {
                    if (pool.getMinThreads() != null) {
                        readAttribute(reader, i, operation, pool.getMinThreads());
                    }
                    break;
                }
                case MAX_THREADS: {
                    readAttribute(reader, i, operation, pool.getMaxThreads());
                    break;
                }
                case QUEUE_LENGTH: {
                    if (pool.getQueueLength() != null) {
                        readAttribute(reader, i, operation, pool.getQueueLength());
                    }
                    break;
                }
                case KEEPALIVE_TIME: {
                    readAttribute(reader, i, operation, pool.getKeepAliveTime());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        ParseUtils.requireNoContent(reader);
    }

    private <P extends ScheduledThreadPoolDefinition & ResourceDefinitionProvider> void parseScheduledThreadPool(P pool, XMLExtendedStreamReader reader, PathAddress parentAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = parentAddress.append(pool.getPathElement());
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_THREADS: {
                    if (this.schema.since(InfinispanSchema.VERSION_10_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case KEEPALIVE_TIME: {
                    readAttribute(reader, i, operation, pool.getKeepAliveTime());
                    break;
                }
                case MIN_THREADS: {
                    if (this.schema.since(InfinispanSchema.VERSION_10_0)) {
                        readAttribute(reader, i, operation, pool.getMinThreads());
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        ParseUtils.requireNoContent(reader);
    }

    @SuppressWarnings("deprecation")
    private void parseRemoteContainer(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = subsystemAddress.append(RemoteCacheContainerResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case CONNECTION_TIMEOUT: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.CONNECTION_TIMEOUT);
                    break;
                }
                case DEFAULT_REMOTE_CLUSTER: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.DEFAULT_REMOTE_CLUSTER);
                    break;
                }
                case KEY_SIZE_ESTIMATE: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.DeprecatedAttribute.KEY_SIZE_ESTIMATE);
                    break;
                }
                case MAX_RETRIES: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.MAX_RETRIES);
                    break;
                }
                case MODULE: {
                    if (this.schema.since(InfinispanSchema.VERSION_12_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.ListAttribute.MODULES);
                    break;
                }
                case PROTOCOL_VERSION: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.PROTOCOL_VERSION);
                    break;
                }
                case SOCKET_TIMEOUT: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.SOCKET_TIMEOUT);
                    break;
                }
                case TCP_NO_DELAY: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.TCP_NO_DELAY);
                    break;
                }
                case TCP_KEEP_ALIVE: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.TCP_KEEP_ALIVE);
                    break;
                }
                case VALUE_SIZE_ESTIMATE: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.DeprecatedAttribute.VALUE_SIZE_ESTIMATE);
                    break;
                }
                case STATISTICS_ENABLED: {
                    if (this.schema.since(InfinispanSchema.VERSION_9_0)) {
                        readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.STATISTICS_ENABLED);
                        break;
                    }
                }
                case MODULES: {
                    if (this.schema.since(InfinispanSchema.VERSION_12_0)) {
                        readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.ListAttribute.MODULES);
                        break;
                    }
                }
                case MARSHALLER: {
                    if (this.schema.since(InfinispanSchema.VERSION_13_0)) {
                        readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.MARSHALLER);
                        break;
                    }
                }
                case TRANSACTION_TIMEOUT: {
                    if (this.schema.since(InfinispanSchema.VERSION_13_0)) {
                        readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.TRANSACTION_TIMEOUT);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case ASYNC_THREAD_POOL: {
                    this.parseThreadPool(ThreadPoolResourceDefinition.CLIENT, reader, address, operations);
                    break;
                }
                case CONNECTION_POOL: {
                    this.parseConnectionPool(reader, address, operations);
                    break;
                }
                case INVALIDATION_NEAR_CACHE: {
                    if (this.schema.since(InfinispanSchema.VERSION_14_0)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    ROOT_LOGGER.elementIgnored(element.getLocalName());
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case REMOTE_CLUSTERS: {
                    this.parseRemoteClusters(reader, address, operations);
                    break;
                }
                case SECURITY: {
                    this.parseRemoteCacheContainerSecurity(reader, address, operations);
                    break;
                }
                case TRANSACTION: {
                    if (this.schema.since(InfinispanSchema.VERSION_14_0)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    ROOT_LOGGER.elementIgnored(element.getLocalName());
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case PROPERTY: {
                    if (this.schema.since(InfinispanSchema.VERSION_11_0) || (this.schema.since(InfinispanSchema.VERSION_9_1) && !this.schema.since(InfinispanSchema.VERSION_10_0))) {
                        ParseUtils.requireSingleAttribute(reader, XMLAttribute.NAME.getLocalName());
                        readElement(reader, operation, RemoteCacheContainerResourceDefinition.Attribute.PROPERTIES);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    @SuppressWarnings("static-method")
    private void parseConnectionPool(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = cacheAddress.append(ConnectionPoolResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case EXHAUSTED_ACTION: {
                    readAttribute(reader, i, operation, ConnectionPoolResourceDefinition.Attribute.EXHAUSTED_ACTION);
                    break;
                }
                case MAX_ACTIVE: {
                    readAttribute(reader, i, operation, ConnectionPoolResourceDefinition.Attribute.MAX_ACTIVE);
                    break;
                }
                case MAX_WAIT: {
                    readAttribute(reader, i, operation, ConnectionPoolResourceDefinition.Attribute.MAX_WAIT);
                    break;
                }
                case MIN_EVICTABLE_IDLE_TIME: {
                    readAttribute(reader, i, operation, ConnectionPoolResourceDefinition.Attribute.MIN_EVICTABLE_IDLE_TIME);
                    break;
                }
                case MIN_IDLE: {
                    readAttribute(reader, i, operation, ConnectionPoolResourceDefinition.Attribute.MIN_IDLE);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseRemoteClusters(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case REMOTE_CLUSTER: {
                    this.parseRemoteCluster(reader, containerAddress, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    @SuppressWarnings("static-method")
    private void parseRemoteCluster(XMLExtendedStreamReader reader, PathAddress clustersAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String remoteCluster = require(reader, XMLAttribute.NAME);
        PathAddress address = clustersAddress.append(RemoteClusterResourceDefinition.pathElement(remoteCluster));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case SOCKET_BINDINGS: {
                    readAttribute(reader, i, operation, RemoteClusterResourceDefinition.Attribute.SOCKET_BINDINGS);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    @SuppressWarnings("static-method")
    private void parseRemoteCacheContainerSecurity(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = containerAddress.append(SecurityResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SSL_CONTEXT: {
                    readAttribute(reader, i, operation, SecurityResourceDefinition.Attribute.SSL_CONTEXT);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private static String require(XMLExtendedStreamReader reader, XMLAttribute attribute) throws XMLStreamException {
        String value = reader.getAttributeValue(null, attribute.getLocalName());
        if (value == null) {
            throw ParseUtils.missingRequired(reader, attribute.getLocalName());
        }
        return value;
    }

    private static void readAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation, Attribute attribute) throws XMLStreamException {
        setAttribute(reader, reader.getAttributeValue(index), operation, attribute);
    }

    private static void setAttribute(XMLExtendedStreamReader reader, String value, ModelNode operation, Attribute attribute) throws XMLStreamException {
        AttributeDefinition definition = attribute.getDefinition();
        definition.getParser().parseAndSetParameter(definition, value, operation, reader);
    }

    private static void readElement(XMLExtendedStreamReader reader, ModelNode operation, Attribute attribute) throws XMLStreamException {
        AttributeDefinition definition = attribute.getDefinition();
        AttributeParser parser = definition.getParser();
        if (parser.isParseAsElement()) {
            parser.parseElement(definition, reader, operation);
        } else {
            parser.parseAndSetParameter(definition, reader.getElementText(), operation, reader);
        }
    }
}

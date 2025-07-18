/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.hc.client5.http;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.net.NamedEndpoint;
import org.apache.hc.core5.util.Args;
import org.apache.hc.core5.util.LangUtils;

/**
 * Connection route definition for HTTP requests.
 *
 * @since 4.0
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public final class HttpRoute implements RouteInfo, Cloneable {

    /** The target host to connect to. */
    private final HttpHost targetHost;

    /** The target name, if different from the target host, {@code null} otherwise. */
    private final NamedEndpoint targetName;

    /**
     * The local address to connect from.
     * {@code null} indicates that the default should be used.
     */
    private final InetAddress localAddress;

    /** The proxy servers, if any. Never null. */
    private final List<HttpHost> proxyChain;

    /** The path to the UDS to connect to, if any. */
    private final Path unixDomainSocket;

    /** Whether the the route is tunnelled through the proxy. */
    private final TunnelType tunnelled;

    /** Whether the route is layered. */
    private final LayerType layered;

    /** Whether the route is (supposed to be) secure. */
    private final boolean secure;

    HttpRoute(final HttpHost targetHost,
              final NamedEndpoint targetName,
              final InetAddress local,
              final List<HttpHost> proxies,
              final Path unixDomainSocket,
              final boolean secure,
              final TunnelType tunnelled,
              final LayerType layered) {
        Args.notNull(targetHost, "Target host");
        Args.notNegative(targetHost.getPort(), "Target port");
        this.targetName = targetName;
        this.targetHost = targetHost;
        this.localAddress = local;
        this.unixDomainSocket = unixDomainSocket;
        if (proxies != null && !proxies.isEmpty()) {
            this.proxyChain = new ArrayList<>(proxies);
        } else {
            this.proxyChain = null;
        }
        if (tunnelled == TunnelType.TUNNELLED) {
            Args.check(this.proxyChain != null, "Proxy required if tunnelled");
        }
        this.secure = secure;
        this.tunnelled = tunnelled != null ? tunnelled : TunnelType.PLAIN;
        this.layered = layered != null ? layered : LayerType.PLAIN;
        if (this.unixDomainSocket != null) {
            validateUdsArguments();
        }
    }

    private void validateUdsArguments() {
        if (this.localAddress != null) {
            throw new UnsupportedOperationException("A localAddress cannot be specified for a UDS connection");
        } else if (this.proxyChain != null) {
            throw new UnsupportedOperationException("Proxies are not supported over a UDS connection");
        } else if (this.layered != LayerType.PLAIN) {
            throw new UnsupportedOperationException("Layering is not supported over a UDS connection");
        } else if (this.tunnelled != TunnelType.PLAIN) {
            throw new UnsupportedOperationException("Tunnelling is not supported over a UDS connection");
        }
    }

    /**
     * Creates a new route with all attributes specified explicitly.
     *
     * @param target    the host to which to route
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param proxies   the proxy chain to use, or
     *                  {@code null} for a direct route
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     * @param tunnelled the tunnel type of this route
     * @param layered   the layering type of this route
     */
    public HttpRoute(final HttpHost target, final InetAddress local, final HttpHost[] proxies,
                     final boolean secure, final TunnelType tunnelled, final LayerType layered) {
        this(target, null, local, proxies != null ? Arrays.asList(proxies) : null, null,
                secure, tunnelled, layered);
    }

    /**
     * Creates a new route with all attributes specified explicitly.
     *
     * @param target    the host to which to route
     * @param targetName the target targetName if differs from the target host.
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param proxies   the proxy chain to use, or
     *                  {@code null} for a direct route
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     * @param tunnelled the tunnel type of this route
     * @param layered   the layering type of this route
     *
     * @since 5.4
     */
    public HttpRoute(final HttpHost target, final NamedEndpoint targetName, final InetAddress local, final HttpHost[] proxies,
                     final boolean secure, final TunnelType tunnelled, final LayerType layered) {
        this(target, targetName, local, proxies != null ? Arrays.asList(proxies) : null, null,
                secure, tunnelled, layered);
    }

    /**
     * Creates a new route with at most one proxy.
     *
     * @param target    the host to which to route
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param proxy     the proxy to use, or
     *                  {@code null} for a direct route
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     * @param tunnelled {@code true} if the route is (to be) tunnelled
     *                  via the proxy,
     *                  {@code false} otherwise
     * @param layered   {@code true} if the route includes a
     *                  layered protocol,
     *                  {@code false} otherwise
     */
    public HttpRoute(final HttpHost target, final InetAddress local, final HttpHost proxy,
                     final boolean secure, final TunnelType tunnelled, final LayerType layered) {
        this(target, null, local, proxy != null ? Collections.singletonList(proxy) : null, null,
                secure, tunnelled, layered);
    }

    /**
     * Creates a new direct route.
     * That is a route without a proxy.
     *
     * @param target    the host to which to route
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     */
    public HttpRoute(final HttpHost target, final InetAddress local, final boolean secure) {
        this(target, null, local, Collections.emptyList(), null, secure, TunnelType.PLAIN, LayerType.PLAIN);
    }

    /**
     * Creates a new direct route. That is a route without a proxy.
     *
     * @param target    the host to which to route
     * @param targetName the target targetName if differs from the target host.
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     *
     * @since 5.4
     */
    public HttpRoute(final HttpHost target, final NamedEndpoint targetName, final InetAddress local, final boolean secure) {
        this(target, targetName, local, Collections.emptyList(), null, secure, TunnelType.PLAIN, LayerType.PLAIN);
    }

    /**
     * Creates a new direct route that connects over a Unix domain socket rather than TCP.
     *
     * @param target           the host to which to route
     * @param unixDomainSocket the path to the Unix domain socket
     *
     * @since 5.6
     */
    public HttpRoute(final HttpHost target, final Path unixDomainSocket) {
        this(target, false, unixDomainSocket);
    }

    /**
     * Creates a new direct route that connects over a Unix domain socket rather than TCP.
     *
     * @param target           the host to which to route
     * @param secure           {@code true} if the route is (to be) secure,
     *                         {@code false} otherwise
     * @param unixDomainSocket the path to the Unix domain socket
     *
     * @since 5.6
     */
    public HttpRoute(final HttpHost target, final boolean secure, final Path unixDomainSocket) {
        this(target, null, null, Collections.emptyList(), unixDomainSocket, secure,
                TunnelType.PLAIN, LayerType.PLAIN);
    }

    /**
     * Creates a new direct insecure route.
     *
     * @param target    the host to which to route
     */
    public HttpRoute(final HttpHost target) {
        this(target, null, null, Collections.emptyList(), null, false, TunnelType.PLAIN, LayerType.PLAIN);
    }

    /**
     * Creates a new route through a proxy.
     * When using this constructor, the {@code proxy} MUST be given.
     * For convenience, it is assumed that a secure connection will be
     * layered over a tunnel through the proxy.
     *
     * @param target    the host to which to route
     * @param targetName the target targetName if differs from the target host.
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param proxy     the proxy to use
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     *
     * @since 5.4
     */
    public HttpRoute(final HttpHost target, final NamedEndpoint targetName, final InetAddress local,
                     final HttpHost proxy, final boolean secure) {
        this(target, targetName, local, Collections.singletonList(Args.notNull(proxy, "Proxy host")), null, secure,
                secure ? TunnelType.TUNNELLED : TunnelType.PLAIN,
                secure ? LayerType.LAYERED : LayerType.PLAIN);
    }

    /**
     * Creates a new route through a proxy.
     * When using this constructor, the {@code proxy} MUST be given.
     * For convenience, it is assumed that a secure connection will be
     * layered over a tunnel through the proxy.
     *
     * @param target    the host to which to route
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param proxy     the proxy to use
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     */
    public HttpRoute(final HttpHost target, final InetAddress local, final HttpHost proxy,
                     final boolean secure) {
        this(target, null, local, Collections.singletonList(Args.notNull(proxy, "Proxy host")), null, secure,
                secure ? TunnelType.TUNNELLED : TunnelType.PLAIN,
                secure ? LayerType.LAYERED : LayerType.PLAIN);
    }

    /**
     * Creates a new plain route through a proxy.
     *
     * @param target    the host to which to route
     * @param proxy     the proxy to use
     *
     * @since 4.3
     */
    public HttpRoute(final HttpHost target, final HttpHost proxy) {
        this(target, null, proxy, false);
    }

    @Override
    public HttpHost getTargetHost() {
        return this.targetHost;
    }

    /**
     * @since 5.4
     */
    @Override
    public NamedEndpoint getTargetName() {
        return targetName;
    }

    @Override
    public InetAddress getLocalAddress() {
        return this.localAddress;
    }

    public InetSocketAddress getLocalSocketAddress() {
        return this.localAddress != null ? new InetSocketAddress(this.localAddress, 0) : null;
    }

    @Override
    public int getHopCount() {
        return proxyChain != null ? proxyChain.size() + 1 : 1;
    }

    @Override
    public HttpHost getHopTarget(final int hop) {
        Args.notNegative(hop, "Hop index");
        final int hopcount = getHopCount();
        Args.check(hop < hopcount, "Hop index exceeds tracked route length");
        if (hop < hopcount - 1) {
            return this.proxyChain.get(hop);
        }
        return this.targetHost;
    }

    @Override
    public HttpHost getProxyHost() {
        return proxyChain != null && !this.proxyChain.isEmpty() ? this.proxyChain.get(0) : null;
    }

    @Override
    public Path getUnixDomainSocket() {
        return this.unixDomainSocket;
    }

    @Override
    public TunnelType getTunnelType() {
        return this.tunnelled;
    }

    @Override
    public boolean isTunnelled() {
        return this.tunnelled == TunnelType.TUNNELLED;
    }

    @Override
    public LayerType getLayerType() {
        return this.layered;
    }

    @Override
    public boolean isLayered() {
        return this.layered == LayerType.LAYERED;
    }

    @Override
    public boolean isSecure() {
        return this.secure;
    }

    /**
     * Compares this route to another.
     *
     * @param obj         the object to compare with
     *
     * @return  {@code true} if the argument is the same route,
     *          {@code false}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof HttpRoute) {
            final HttpRoute that = (HttpRoute) obj;
            return
                    // Do the cheapest tests first
                    this.secure == that.secure &&
                            this.tunnelled == that.tunnelled &&
                            this.layered == that.layered &&
                            Objects.equals(this.targetHost, that.targetHost) &&
                            Objects.equals(this.targetName, that.targetName) &&
                            Objects.equals(this.localAddress, that.localAddress) &&
                            Objects.equals(this.proxyChain, that.proxyChain) &&
                            Objects.equals(this.unixDomainSocket, that.unixDomainSocket);
        }
        return false;
    }


    /**
     * Generates a hash code for this route.
     *
     * @return  the hash code
     */
    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.targetHost);
        hash = LangUtils.hashCode(hash, this.targetName);
        hash = LangUtils.hashCode(hash, this.localAddress);
        if (this.proxyChain != null) {
            for (final HttpHost element : this.proxyChain) {
                hash = LangUtils.hashCode(hash, element);
            }
        }
        if (this.unixDomainSocket != null) {
            hash = LangUtils.hashCode(hash, unixDomainSocket);
        }
        hash = LangUtils.hashCode(hash, this.secure);
        hash = LangUtils.hashCode(hash, this.tunnelled);
        hash = LangUtils.hashCode(hash, this.layered);
        return hash;
    }

    /**
     * Obtains a description of this route.
     *
     * @return  a human-readable representation of this route
     */
    @Override
    public String toString() {
        final StringBuilder cab = new StringBuilder(50 + getHopCount() * 30);
        if (this.localAddress != null) {
            cab.append(this.localAddress);
            cab.append("->");
        } else if (unixDomainSocket != null) {
            cab.append(unixDomainSocket).append("->");
        }
        cab.append('{');
        if (this.tunnelled == TunnelType.TUNNELLED) {
            cab.append('t');
        }
        if (this.layered == LayerType.LAYERED) {
            cab.append('l');
        }
        if (this.secure) {
            cab.append('s');
        }
        cab.append("}->");
        if (this.proxyChain != null) {
            for (final HttpHost aProxyChain : this.proxyChain) {
                cab.append(aProxyChain);
                cab.append("->");
            }
        }
        if (this.targetName != null) {
            cab.append(this.targetName);
            cab.append("/");
        }
        cab.append("[");
        cab.append(this.targetHost);
        cab.append("]");
        return cab.toString();
    }

    // default implementation of clone() is sufficient
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}

/*
 * Copyright 2017 Steffen Folman Sørensen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openhab.binding.snapcast.internal.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.binding.snapcast.internal.rpc.JsonRpcEventClient;
import org.openhab.binding.snapcast.internal.types.Client;
import org.openhab.binding.snapcast.internal.types.Group;
import org.openhab.binding.snapcast.internal.types.ServerStatus;
import org.openhab.binding.snapcast.internal.types.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnapcastController {
    private Logger logger = LoggerFactory.getLogger(JsonRpcEventClient.class);
    private final JsonRpcEventClient client;
    private final Map<String, Client> clientMap = new HashMap<>();
    private final Map<String, Stream> streamMap = new HashMap<>();
    private final Map<String, Group> groupMap = new HashMap<>();
    private final List<SnapclientUpdateListener> snapclientUpdateListeners = new ArrayList<>();
    private final List<SnapgroupUpdateListener> snapgroupUpdateListeners = new ArrayList<>();
    private ServerStatus serverStatus;

    public SnapcastController(JsonRpcEventClient client) {
        this.client = client;
    }

    public synchronized void connect() throws IOException, InterruptedException {
        client.addNotificationHandler(new SnapcastMethodClientOnConnect(clientMap, this));
        client.addNotificationHandler(new SnapcastMethodClientOnDisconnect(clientMap, this));
        client.addNotificationHandler(new SnapcastMethodClientOnUpdate(clientMap, this));
        client.connect();

        serverStatus = client.sendRequestAndReadResponse("Server.GetStatus", null, ServerStatus.class);
        serverStatus.getServer();
        serverStatus.getGroups().forEach(g -> {
            groupMap.put(g.getId(), g);
            g.getClients().forEach(c -> {
                clientMap.put(c.getHost().getMac(), c);
                logger.info("Found snapcast client: {}", c.getHost());
            });
            logger.info("Found snapcast group: {}", g.getName());

        });

        // serverStatus.getGroups().forEach(g -> g.getClients().forEach(c -> clientMap.put(c.getHost().getMac(), c)));
        serverStatus.getStreams().forEach(s -> {
            logger.info("Found snapcast stream: {}", s.getId());
            streamMap.put(s.getId(), s);
        });
    }

    @SuppressWarnings("unused")
    public Collection<Stream> getStreams() {
        return Collections.unmodifiableCollection(streamMap.values());
    }

    public SnapcastClientController getClient(final String mac) {
        return new SnapcastClientController(client, mac, clientMap, snapclientUpdateListeners);
    }

    public SnapcastGroupController getGroup(final String id) {
        return new SnapcastGroupController(client, id, groupMap, snapgroupUpdateListeners);
    }

    public Collection<Client> getClients() {
        return clientMap.values();
    }

    public Collection<Group> getGroups() {
        return groupMap.values();
    }

    /*
     * public Integer getProtocolVersion() {
     * return serverStatus.getServer().getSnapserver().getControlProtocolVersion();
     * }
     */
    @SuppressWarnings("unused")
    public void addClientUpdateListener(final SnapclientUpdateListener updateListener) {
        snapclientUpdateListeners.add(updateListener);
    }

    @SuppressWarnings("unused")
    public void addGroupUpdateListener(final SnapgroupUpdateListener updateListener) {
        snapgroupUpdateListeners.add(updateListener);
    }

}

/*
 * Copyright (C) 2024 Alexander Stojanovich <coas91@rocketmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rs.alexanderstojanovich.evgds.net;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import rs.alexanderstojanovich.evgds.main.Game;
import rs.alexanderstojanovich.evgds.main.GameServer;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface RequestIfc extends DSObject {

    /**
     * Guid length of DSynergy request
     */
    public static final int GUID_LENGTH = 16;

    /**
     * Magic bytes of DSynergy request
     */
    public static final byte[] MAGIC_BYTES = {(byte) 0xAB, (byte) 0xCD, (byte) 0x0D, (byte) 0x13}; // 4 Bytes

    /**
     * Allowed request type. To send to DSynergy server.
     */
    public static enum RequestType {
        /**
         * Occurs on server side if invalid request is received
         */
        INVALID,
        /**
         * Send hello request to authenticate game client
         */
        HELLO,
        /**
         * Get ingame time from server
         */
        GET_TIME,
        /**
         * Set ingame time on the server
         */
        SET_TIME,
        /**
         * Get player position & view from the server
         */
        GET_POS,
        /**
         * Set player position & view on the server
         */
        SET_POS,
        /**
         * Send chat message to the server (global)
         */
        SAY,
        /**
         * Ping the server. Trip round-time.
         */
        PING,
        /**
         * Send goodbye-disconnect request to leave the server
         */
        GOODBYE,
        /**
         * Request Download Level (Map)
         */
        DOWNLOAD,
        /**
         * Get (Download) Level (Map) N-Fragment
         */
        GET_FRAGMENT,
        /**
         * Confirm received packets (mandatory for UDP)
         */
        CONFIRM,
        /**
         * Register player (UUID) to game server
         */
        REGISTER,
        /**
         * Get player info from all players on the server as Json array (or
         * list)
         */
        PLAYER_INFO,
        /**
         * Force disconnect to player
         */
        KICK_PLAYER
    }

    /**
     * Get Request Type. One of the {HELLO, UPDATE, HANDLE_INPUT, SYNC_TIME,
     * SAY, LOAD_CHUNK, GOODBYE }.
     *
     * @return Request Type
     */
    public RequestType getRequestType();

    /**
     * Send request to server endpoint.
     *
     * @param client game client
     * @throws java.lang.Exception
     */
    public void send(Game client) throws Exception;

    /**
     * Get client address from this request
     *
     * @return client address
     */
    public InetAddress getClientAddress();

    /**
     * Get client port from this request
     *
     * @return client port
     */
    public int getClientPort();

    /**
     * Receive request from client endpoint.
     *
     * @param server game server
     * @return null if deserialization failed otherwise valid request
     * @throws java.io.IOException if network error
     */
    public static RequestIfc receive(GameServer server) throws Exception {
        final byte[] buff = new byte[BUFF_SIZE];
        DatagramPacket p = new DatagramPacket(buff, buff.length);
        server.getEndpoint().receive(p);

        byte[] dataContent = Arrays.copyOfRange(p.getData(), 0, p.getLength() - Long.BYTES);
        byte[] dataChksum = Arrays.copyOfRange(p.getData(), p.getLength() - Long.BYTES, p.getLength());
        long checksum = new BigInteger(dataChksum).longValue();

        RequestIfc result = (RequestIfc) new Request(p.getAddress(), p.getPort(), checksum).deserialize(dataContent); // new request                

        return result;
    }

    /**
     * Get sender guid
     *
     * @return sender guid (of this request)
     */
    public String getGuid();
}

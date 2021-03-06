/*
 * Copyright 1997 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/* @test
 *
 * @bug 4095393
 *
 * @summary this tests a regression in 1.2, beta 2 and earlier where
 * the DatagramPackets sent the entire buffer, not the buffer length
 * of the packet.
 *
 * @author Benjamin Renaud
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class SendSize {

    static final int clientPort = 8989;
    static final int serverPort = 9999;
    static final int bufferLength = 512;
    static final int packetLength = 256;

    public static void main(String[] args) throws Exception {
        new ServerThread().start();
        new ClientThread().start();
    }


    static class ServerThread extends Thread {

        int port;
        DatagramSocket server;

        ServerThread(int port) throws IOException {
            this.port = port;
            this.server = new DatagramSocket(port);
        }

        ServerThread() throws IOException {
            this(SendSize.serverPort);
        }

        public void run() {
            try {
                System.err.println("started server thread: " + server);
                byte[] buf = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(buf,
                                                                  buf.length);
                server.receive(receivePacket);
                int len = receivePacket.getLength();
                switch(len) {
                case packetLength:
                    System.out.println("receive length is: " + len);
                    break;
                default:
                    throw new RuntimeException(
                        "receive length is: " + len +
                        ", send length: " + packetLength +
                        ", buffer length: " + buf.length);
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("caugth: " + e);
            }
        }
    }

    static class ClientThread extends Thread {

        int port;
        int serverPort;
        int bufferLength;
        int packetLength;

        DatagramSocket client;
        InetAddress host;

        ClientThread(int port, int serverPort,
                     int bufferLength, int packetLength) throws IOException {
            this.port = port;
            this.serverPort = serverPort;
            this.host = InetAddress.getLocalHost();
            this.bufferLength = bufferLength;
            this.packetLength = packetLength;
            this.client = new DatagramSocket(port, host);
        }

        ClientThread() throws IOException {
            this(SendSize.clientPort, SendSize.serverPort,
                 SendSize.bufferLength, SendSize.packetLength);
        }

        public void run() {
            try {
                System.err.println("started client thread: " + client);
                byte[] buf = new byte[bufferLength];
                DatagramPacket sendPacket =
                    new DatagramPacket(buf, packetLength,
                                       host, serverPort);
                for (int i = 0; i < 10; i++) {
                    client.send(sendPacket);
                }
                System.err.println("sent 10 packets");
                return;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("caught: " + e);
            }
        }
    }
}

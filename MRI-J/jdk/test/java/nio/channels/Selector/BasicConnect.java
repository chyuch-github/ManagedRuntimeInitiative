/*
 * Copyright 2001 Sun Microsystems, Inc.  All Rights Reserved.
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
 * @summary Test nonblocking connect and finishConnect
 * @bug 4457776
 * @library ../../../..
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.*;
import java.util.*;


/**
 * Typically there would be more than one channel registered to select
 * on, this test is just a very simple version with only one channel
 * registered for the connectSelector.
 */

public class BasicConnect {

    static final int PORT = 7;          // echo
    static final String HOST = TestEnv.getProperty("host");

    public static void main(String[] args) throws Exception {
        Selector connectSelector =
            SelectorProvider.provider().openSelector();
        InetSocketAddress isa
            = new InetSocketAddress(InetAddress.getByName(HOST), PORT);
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        boolean result = sc.connect(isa);
        while (!result) {
            SelectionKey connectKey = sc.register(connectSelector,
                                                  SelectionKey.OP_CONNECT);
            int keysAdded = connectSelector.select();
            if (keysAdded > 0) {
                Set readyKeys = connectSelector.selectedKeys();
                Iterator i = readyKeys.iterator();
                while (i.hasNext()) {
                    SelectionKey sk = (SelectionKey)i.next();
                    i.remove();
                    SocketChannel nextReady = (SocketChannel)sk.channel();
                    result = nextReady.finishConnect();
                    if (result)
                        sk.cancel();
                }
            }
        }

        byte[] bs = new byte[] { (byte)0xca, (byte)0xfe,
                                 (byte)0xba, (byte)0xbe };
        ByteBuffer bb = ByteBuffer.wrap(bs);
        sc.configureBlocking(true);
        sc.write(bb);
        bb.rewind();

        ByteBuffer bb2 = ByteBuffer.allocateDirect(100);
        int n = sc.read(bb2);
        bb2.flip();
        if (!bb.equals(bb2))
            throw new Exception("Echoed bytes incorrect: Sent "
                                + bb + ", got " + bb2);
        sc.close();
    }

}

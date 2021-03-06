/*
 * Copyright 2000-2001 Sun Microsystems, Inc.  All Rights Reserved.
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
 * @summary Unit test for socket channels
 * @library ../../../..
 */

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;


public class Basic {

    static java.io.PrintStream out = System.out;

    static final int DAYTIME_PORT = 13;
    static final String DAYTIME_HOST = TestEnv.getProperty("host");

    static void test() throws Exception {
        InetSocketAddress isa
            = new InetSocketAddress(InetAddress.getByName(DAYTIME_HOST),
                                    DAYTIME_PORT);
        SocketChannel sc = SocketChannel.open(isa);
        out.println("opened: " + sc);
        /*
        out.println("opts:   " + sc.options());
        ((SocketOpts.IP.TCP)sc.options())
            .noDelay(true)
            .typeOfService(SocketOpts.IP.TOS_THROUGHPUT)
            .broadcast(true)
            .keepAlive(true)
            .linger(42)
            .outOfBandInline(true)
            .receiveBufferSize(128)
            .sendBufferSize(128)
            .reuseAddress(true);
        out.println("        " + sc.options());
        */
        // sc.connect(isa);
        out.println("connected: " + sc);
        ByteBuffer bb = ByteBuffer.allocateDirect(100);
        int n = sc.read(bb);
        bb.position(bb.position() - 2);         // Drop CRLF
        bb.flip();
        CharBuffer cb = Charset.forName("US-ASCII").newDecoder().decode(bb);
        out.println(isa + " says: \"" + cb + "\"");
        sc.socket().shutdownInput();
        out.println("ishut: " + sc);
        sc.socket().shutdownOutput();
        out.println("oshut: " + sc);
        sc.close();
        out.println("closed: " + sc);
    }

    public static void main(String[] args) throws Exception {
        test();
    }

}

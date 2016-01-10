/**
 * Copyright 2013, Landz and its contributors. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.serce.glu.gluvm;

import org.junit.Test;

import java.lang.invoke.MethodHandle;

import static jdk.vm.ci.amd64.AMD64.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static z.znr.MethodHandles.asm;

/**
 */
public class ZNRInvokeTest {
    static {
        System.setProperty("jnr.invoke.compile.dump", "false");
    }

    private static final MethodHandle SUM = asm(
            long.class,
            long.class, long.class, long.class, long.class, long.class,
            a -> {
                // param regs are %rdi, %rsi, %rdx, %rcx, %r8 and %r9
                a.movl(rax, rdx);
                a.addq(rax, rcx);
                a.addq(rax, r8);
                a.addq(rax, r9);
//                a.movl(r9, a.m)
//                a.addq(rax, a.makeAddress(rsp, 8));
                a.ret(0);
            }
    );

    private static long sum(long a, long b, long c, long d, long e) throws Throwable {
        return (long) SUM.invokeExact(a, b, c, d, e);
    }

    //
    private static final MethodHandle SUM2 = asm(
            long.class,
            long.class, long.class, long.class, long.class, int.class,
            a -> {
                // param regs are %rdi, %rsi, %rdx, %rcx, %r8 and %r9
                a.movl(rax, rdx);
                a.addq(rax, rcx);
                a.addq(rax, r8);
                a.addq(rax, r9);
//                a.add(rax, dword_ptr(rsp, 8));
                a.ret(0);
            }
    );

    private static long sum2(long a, long b, long c, long d, int e) throws Throwable {
        return (long) SUM2.invokeExact(a, b, c, d, e);
    }


    @Test
    public void testPassingFiveParametersToASM() throws Throwable {
        long s = sum(1, 2, 3, 4, 5);
        System.out.println(s);

        assertThat(s, is(15L));

        long s2 = sum2(1, 2, 3, 4, 5);
        System.out.println(s);

        assertThat(s, is(15L));
    }
}

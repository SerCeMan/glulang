package ru.serce.glu.gluvm

import org.junit.After
import org.junit.Before
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author serce
 * @since 31.10.15.
 */
class GluVMTest {

    ByteArrayOutputStream result;
    PrintStream out;

    GluVM vm;

    @Before
    void setup() {
        result = new ByteArrayOutputStream()
        out = System.out
        System.setOut(new PrintStream(result))
        vm = new GluVM()
    }

    @After
    void after() {
        result.close()
        System.setOut(out)
    }


    @Test
    void test1() {
        vm.eval('''
        int add(int x, int y) {
            return x + y;
        }

        void main() {
            int a;
            a = 1;
            int b;
            b = 2;
            int c;
            c = add(a, b);
            println(c);
        }
        ''')
        assertLines('3', result)
    }

    @Test
    void test2() {
        vm.eval('''
        void main() {
            int a;
            if(true) {
                a = 1;
            } else {
                a = 2;
            }
            println(a);
        }
        ''')
        assertLines('1', result)
    }

    @Test
    void test3() {
        vm.eval('''
        void main() {
            int a;
            if(false) {
                a = 1;
            } else {
                a = 2;
            }
            println(a);
        }
        ''')
        assertLines('2', result)
    }

    @Test
    void test4() {
        vm.eval('''
        void main() {
            int a;
            a = 4;
            a = 2 + a;
            println(a);
        }
        ''')
        assertLines('6', result)
    }

    @Test
    void test5() {
        vm.eval('''
        void main() {
            int a;
            a = 4;
            println(a);
        }
        ''')
        assertLines('4', result)
    }

    void assertLines(String exp, ByteArrayOutputStream baos) {
        def lines = baos.toString().split('\n').findAll { !it.isEmpty() }.collect { it.trim() }
        def expected = exp.split('\n').findAll { !it.isEmpty() }.collect { it.trim() }
        assertEquals(expected.size(), lines.size())
        for (int i = 0; i < lines.size(); i++) {
            assertEquals(expected[i], lines[i])
        }
    }
}
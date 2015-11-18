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
        int add2(int x, int y) {
            return x + y;
        }

        int add(int x, int y) {
            return add2(x, y);
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

    @Test
    void test6() {
        vm.eval('''
        int add(int a, int b) {
            int c;
            c = a;
            c = c + b;
            return c;
        }

        void main() {
            int c;
            c = 4;
            int a;
            a = c;
            a = a + 2;
            int d;
            d = add(a, a);
            int k;
            k = a + d;
            println(k);
        }
        ''')
        assertLines('18', result)
    }

    @Test
    void test7() {
        vm.eval('''
        void main() {
            int a;
            a = 1;
            while(a<5) {
                a = a + 1;
            }
            println(a);
        }
        ''')
        assertLines('5', result)
    }

    @Test
    void test8() {
        vm.eval('''
        void main() {
            int a;
            a = 1;
            while(a<=5) {
                a = a + 1;
            }
            println(a);
        }
        ''')
        assertLines('6', result)
    }

    @Test
    void test9() {
        vm.eval('''
        void main() {
            int a;
            a = 6;
            while(a > 1) {
                a = a - 1;
            }
            println(a);
        }
        ''')
        assertLines('1', result)
    }


    @Test
    void test10() {
        vm.eval('''
        void main() {
            int a;
            a = 6;
            while(a >= 1) {
                a = a - 1;
            }
            println(a);
        }
        ''')
        assertLines('0', result)
    }

    @Test
    void test11() {
        vm.eval('''
        void main() {
            int a;
            a = 1;
            while(a == 1) {
                a = a - 1;
            }
            println(a);
        }
        ''')
        assertLines('0', result)
    }

    @Test
    void test12() {
        vm.eval('''
        void main() {
            int a;
            a = 2;
            while(a != 1) {
                a = a - 1;
            }
            println(a);
        }
        ''')
        assertLines('1', result)
    }

    @Test
    void test13() {
        vm.eval('''
        void main() {
            int a;
            a = 5;
            println(a * a);
        }
        ''')
        assertLines('25', result)
    }


    @Test
    void test14() {
        vm.eval('''
        void main() {
            int a;
            a = 5;
            println(a / a);
        }
        ''')
        assertLines('1', result)
    }

    @Test
    void test15() {
        vm.eval('''
        void main() {
            println(true || false);
        }
        ''')
        assertLines('1', result)
    }

    @Test
    void test16() {
        vm.eval('''
        void main() {
            println(true && false);
        }
        ''')
        assertLines('0', result)
    }

    @Test
    void test17() {
        vm.eval('''
        void main() {
            println(!false);
        }
        ''')
        assertLines('1', result)
    }

    @Test
    void test18() {
        vm.eval('''
        int add(int x, int y) {
            return x + y;
        }

        void main() {
            int a;
            int b;
            int c;
            a = 1; b = 2;
            c = add(a, b);
            c = add(a, b);
            c = add(a, b);
            c = add(a, b);
            c = add(a, b);
            c = add(a, b);
            println(c);
        }
        ''')
        assertLines('3', result)
    }

    @Test
    void test19() {
        vm.eval('''
        int add(int x, int y) {
            return x + y;
        }

        int sub(int x, int y) {
            return x - y;
        }

        void main() {
            int a;
            int b;
            int c;
            a = 2; b = 1;
            c = sub(a, b);
            c = sub(a, b);
            c = sub(a, b);
            println(c);
        }
        ''')
        assertLines('''1''', result)
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
package ru.serce.glu.gluc;

/**
 * @author serce
 * @since 18.10.15.
 */
public class Test {

    public Test() {
        add(10, 20);
        test(false);
    }

    public int add(int a, int b) {
        return a + b;
    }

    public boolean test(boolean b) {
        return !b;
    }
}

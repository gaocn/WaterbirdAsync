package waterbird.space.birdasync;

import java.lang.reflect.Array;

/**
 * Created by 高文文 on 2016/12/13.
 */

public class ArrayCompat {

    /*
        getComponentType()方法可以取得一个数组的Class对象；
        例如：
            the componentType of the char is :null
            the componentType of the char[] is :char
     */

    public static<T, U> T[] copyOf(U[] src, int newLength, Class<? extends T[]> newType) {
        T[] copy =
                ((Object) newType == (Object)Object[].class)
                ? ((T[]) new Object[newLength])
                : ((T[]) Array.newInstance(newType.getComponentType(), newLength));
        System.arraycopy(src, 0, copy, 0, Math.min(newLength, src.length));
        return copy;
    }

    public static <T> T[] copyOf(T[] src, int newLength) {
        return (T[]) copyOf(src, newLength, src.getClass());
    }
}

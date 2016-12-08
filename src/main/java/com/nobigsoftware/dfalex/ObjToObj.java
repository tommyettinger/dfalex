package com.nobigsoftware.dfalex;

/**
 * Created by Tommy Ettinger on 12/7/2016.
 */
public interface ObjToObj<T, R>
{
    R apply(T t);
}

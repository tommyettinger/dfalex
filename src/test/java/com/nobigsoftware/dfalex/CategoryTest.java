package com.nobigsoftware.dfalex;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Tommy Ettinger on 12/17/2016.
 */
public class CategoryTest {
    @Test
    public void testCategories() throws Exception
    {
        Assert.assertTrue(CharRange.Ll.contains('a'));
        Assert.assertTrue(CharRange.Ll.contains('z'));

        Assert.assertFalse(CharRange.Ll.contains('A'));
        Assert.assertFalse(CharRange.Ll.contains('Z'));

        Assert.assertTrue(CharRange.IdentifierStart.contains('_'));
        Assert.assertFalse(CharRange.IdentifierStart.contains('9'));
    }
}

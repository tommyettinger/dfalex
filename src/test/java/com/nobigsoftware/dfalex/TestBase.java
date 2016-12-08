package com.nobigsoftware.dfalex;

import org.junit.Assert;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedList;

@SuppressWarnings("unchecked")
public class TestBase
{
    final PrettyPrinter m_printer = new PrettyPrinter();

    int _countStates(DfaState<?>... starts)
    {
        final LinkedList<DfaState<?>> togo = new LinkedList<>();
        final HashSet<DfaState<?>> checkSet = new HashSet<>();
        for (DfaState<?> start : starts)
        {
            if (checkSet.add(start))
            {
                togo.add(start);
            }
        }
        while(!togo.isEmpty())
        {
            DfaState<?> scanst = togo.removeFirst();
            scanst.enumerateTransitions(new DfaTransitionConsumer() {
                @Override
                public void acceptTransition(char firstChar, char lastChar, DfaState newstate) {
                    if (checkSet.add(newstate))
                    {
                        togo.add(newstate);
                    }

                }
            });
        }
        return checkSet.size();
    }
    
    void _checkDfa(DfaState<?> start, String resource, boolean doStdout) throws Exception
    {
        String have;
        {
            StringWriter w = new StringWriter();
            m_printer.print(new PrintWriter(w), start);
            have = w.toString();
        }
        if (doStdout)
        {
            System.out.print(have);
            System.out.flush();
        }
        String want = _readResource(resource);
        Assert.assertEquals(want, have);
    }
    
    static String _readResource(String resource) throws Exception
    {
        InputStream instream = TestBase.class.getResourceAsStream("/" + resource);
        try
        {
            return stringifyStream(instream);
        }
        finally
        {
            instream.close();
        }
    }
    /**
     * @param is
     * @return The content of {@©ode is} as a {@link String}.
     */
    static String stringifyStream(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is);
        s.useDelimiter("\\A");
        String nx = s.hasNext() ? s.next() : "";
        s.close();
        return nx;
    }
}

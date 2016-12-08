package com.nobigsoftware.dfalex;

import org.junit.Assert;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.HashMap;

public class PrettyPrinter
{
    private HashMap<DfaState<?>,String> m_names = new HashMap<>();
    private HashMap<DfaState<?>,String> m_transMemo = new HashMap<>();
    private LinkedList<DfaState<?>> m_closureQ = new LinkedList<>();
    private int m_nextStateNum = 0;
    
    public void print(PrintWriter w, DfaState<?> state)
    {
        m_names.clear();
        m_transMemo.clear();
        m_closureQ.clear();
        m_nextStateNum = 0;
        _nameState(state);
        while (!m_closureQ.isEmpty())
        {
            _printState(w, m_closureQ.removeFirst());
        }
    }
    
    private void _printState(PrintWriter w, DfaState<?> state)
    {
        final String stateName = m_names.get(state);
        w.println(stateName);
        String trans = _getTransitionChars(state);
        if (trans.length()<2)
        {
            w.println("    (done)");
            return;
        }
        for (int i=0; i<trans.length()-1; i+=2)
        {
            w.write("    ");
            char cmin = trans.charAt(i);
            char cmax = trans.charAt(i+1);
            DfaState<?> target = state;
            
            for (;;)
            {
                _printChar(w,cmin);
                if (cmin != cmax)
                {
                    w.write("-");
                    _printChar(w,cmax);
                }
                w.write(" -> ");
                
                target = target.getNextState(cmin);
                String nexttrans = _getTransitionChars(target);
                if (nexttrans.length()==2 && target.getMatch() == null)
                {
                    cmin = nexttrans.charAt(0);
                    cmax = nexttrans.charAt(1);
                }
                else
                {
                    w.println(_nameState(target));
                    break;
                }
            }
        }
    }
    
    private String _nameState(DfaState<?> state)
    {
        String ret = m_names.get(state);
        if (ret == null)
        {
            Object accept = state.getMatch();
            if (accept == null)
            {
                ret = "S" + m_nextStateNum++;
                
            }
            else
            {
                ret =  "S" + m_nextStateNum++ + ":" + accept.toString(); 
            }
            m_names.put(state, ret);
            m_closureQ.add(state);
        }
        return ret;
    }
    @SuppressWarnings("unchecked")
    private String _getTransitionChars(final DfaState<?> state)
    {
        String ret = m_transMemo.get(state);
        if (ret == null)
        {
            final StringBuilder stb = new StringBuilder();
            state.enumerateTransitions(new DfaTransitionConsumer() {
                @Override
                public void acceptTransition(char startc, char endc, DfaState newstate) {
                    stb.append(startc);
                    stb.append(endc);
                    Assert.assertEquals(state.getNextState(startc), newstate);
                }
            });
            ret = stb.toString();
            m_transMemo.put(state, ret);
        }
        return ret;
    }
    
    private void _printChar(PrintWriter w,char c)
    {
        if (c>=' ' && c<(char)128)
        {
            w.append(c);
            return;
        }
        switch(c)
        {
            case '\n':
            w.append("\\n");
            break;
            
            case '\r':
            w.append("\\r");
            break;
            
            case '\t':
            w.append("\\t");
            break;
            
            default:
            w.append("$").append(Integer.toHexString(c));
            break;
        }
    }

}

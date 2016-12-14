/*
 * Copyright 2015 Matthew Timmermans
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nobigsoftware.dfalex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SerializableDfa<RESULT> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ArrayList<DfaStatePlaceholder<RESULT>> m_dfaStates;
    private final int[] m_startStateNumbers;

    private transient List<DfaState<RESULT>> m_startStatesMemo;

    public SerializableDfa() {
        m_dfaStates = new ArrayList<>();
        m_startStateNumbers = new int[0];
    }

    public SerializableDfa(RawDfa<RESULT> rawDfa) {
        final List<DfaStateInfo> origStates = rawDfa.getStates();
        final int len = origStates.size();
        m_dfaStates = new ArrayList<>(len);
        m_startStateNumbers = rawDfa.getStartStates();
        while (m_dfaStates.size() < len) {
            m_dfaStates.add(new PackedTreeDfaPlaceholder<>(rawDfa, m_dfaStates.size()));
        }
    }

    public synchronized List<DfaState<RESULT>> getStartStates() {
        if (m_startStatesMemo == null) {
            final int len = m_dfaStates.size();
            for (int i = 0; i < len; ++i) {
                m_dfaStates.get(i).createDelegate(i, m_dfaStates);
            }
            for (int i = 0; i < len; ++i) {
                m_dfaStates.get(i).fixPlaceholderReferences();
            }
            m_startStatesMemo = new ArrayList<>(m_startStateNumbers.length);
            for (int startState : m_startStateNumbers) {
                m_startStatesMemo.add(m_dfaStates.get(startState).resolvePlaceholder());
            }
        }
        return m_startStatesMemo;
    }

    public StringBuilder condense() {
        StringBuilder sb = new StringBuilder(m_dfaStates.size() * 12);
        sb.append(Tools.json.toJson(this, this.getClass()));
        sharknado(sb);
        return sb;
    }

    @SuppressWarnings("unchecked")
    public static <RESULT> SerializableDfa<RESULT> produce(CharSequence text) {
        int len = text.length();
        CharSequence cs = text.subSequence(0, len - 32);
        return (SerializableDfa<RESULT>) Tools.json.fromJson(SerializableDfa.class, cs.toString());
    }

    private static void sharknado(final StringBuilder data) {
        final long c1 = 0x357BD1113171B1F2L ^ 0xC6BC279692B5CC83L,
                c2 = 0xCAFEBEEF1337FECAL ^ 0xC6BC279692B5CC83L,
                c3 = 0xBABE42DEEDBEEFEEL ^ 0xC6BC279692B5CC83L;
        final int len = data.length();
        long z1 = 0x632BE59BD9B4E019L + c1, r1 = 7L,
                z2 = 0x632BE59BD9B4E019L + c2, r2 = 127L,
                z3 = 0x632BE59BD9B4E019L + c3, r3 = 421L,
                d;
        for (int i = 0; i < len; i++) {
            r1 ^= (z1 += ((d = data.charAt(i)) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c1;
            r2 ^= (z2 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c2;
            r3 ^= (z3 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c3;
        }
        r1 ^= Long.rotateLeft((z1 * 0xC6BC279692B5CC83L ^ r1 * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z1 >>> 58));
        r2 ^= Long.rotateLeft((z2 * 0xC6BC279692B5CC83L ^ r2 * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z2 >>> 58));
        r3 ^= Long.rotateLeft((z3 * 0xC6BC279692B5CC83L ^ r3 * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z3 >>> 58));
        data
                .append(base32[(int) r1 & 31])
                .append(base32[(int) (r1 >>>= 5) & 31]).append(base32[(int) (r1 >>>= 5) & 31])
                .append(base32[(int) (r1 >>>= 5) & 31]).append(base32[(int) (r1 >>>= 5) & 31])
                .append(base32[(int) (r1 >>>= 5) & 31]).append(base32[(int) (r1 >>>= 5) & 31])
                .append(base32[(int) (r1 >>>= 5) & 31]).append(base32[(int) (r1 >>>= 5) & 31])
                .append(base32[(int) (r1 >>>= 5) & 31]).append(base32[(int) (r1 >>> 5) & 31])

                .append(base32[(int) r2 & 31])
                .append(base32[(int) (r2 >>>= 5) & 31]).append(base32[(int) (r2 >>>= 5) & 31])
                .append(base32[(int) (r2 >>>= 5) & 31]).append(base32[(int) (r2 >>>= 5) & 31])
                .append(base32[(int) (r2 >>>= 5) & 31]).append(base32[(int) (r2 >>>= 5) & 31])
                .append(base32[(int) (r2 >>>= 5) & 31]).append(base32[(int) (r2 >>>= 5) & 31])
                .append(base32[(int) (r2 >>>= 5) & 31]).append(base32[(int) (r2 >>> 5) & 31])

                .append(base32[(int) (r3 >>>= 5) & 31]).append(base32[(int) (r3 >>>= 5) & 31])
                .append(base32[(int) (r3 >>>= 5) & 31]).append(base32[(int) (r3 >>>= 5) & 31])
                .append(base32[(int) (r3 >>>= 5) & 31]).append(base32[(int) (r3 >>>= 5) & 31])
                .append(base32[(int) (r3 >>>= 5) & 31]).append(base32[(int) (r3 >>>= 5) & 31])
                .append(base32[(int) (r3 >>>= 5) & 31]).append(base32[(int) (r3 >>> 5) & 31]);

    }

    private static final char[] base32 = "0123456789abcdefghijklmnopqrstuv".toCharArray();
}

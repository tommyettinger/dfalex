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

import com.nobigsoftware.util.BuilderCache;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * Builds deterministic finite automata (google phrase) or DFAs that find patterns in strings
 * <p>
 * Given a set of patterns and the desired result of matching each pattern, you can produce a
 * DFA that will simultaneously match a sequence of characters against all of those patterns.
 * <p>
 * You can also build DFAs for multiple sets of patterns simultaneously. The resulting DFAs will
 * be optimized to share states wherever possible.
 * <p>
 * When you build a DFA to match a set of patterns, you get a "start state" (a {@link DfaState}) for
 * that pattern set. Each character of a string can be passed in turn to {@link DfaState#getNextState(char)},
 * which will return a new {@link DfaState}.
 * <p>
 * {@link DfaState#getMatch()} can be called at any time to get the MATCHRESULT (if any) for
 * the patterns that match the characters processed so far.
 * <p>
 * A {@link DfaState} can be used with a {@link StringMatcher} to find instances of patterns in strings,
 * or with other pattern-matching classes.
 * <p>
 * NOTE that building a Dfa is a complex procedure.  You should typically do it only once for each
 * pattern set you want to use.  Usually you would do this in a static initializer.
 * <p>
 * You can provide a cache that can remember and recall built DFAs, which allows you to build DFAs
 * during your build process in various ways, instead of building them at runtime.  Or you can use
 * the cache to store built DFAs on the first run of your program so they don't need to be built
 * the next time...  But this is usually unnecessary, since building DFAs is more than fast enough to
 * do during runtime initialization.
 *
 * @param <MATCHRESULT> The type of result to produce by matching a pattern.  This must be serializable
 *                      to support caching of built DFAs
 */
public class DfaBuilder<MATCHRESULT extends Serializable> {
    //dfa types for cache keys
    private static final int DFATYPE_MATCHER = 0;
    private static final int DFATYPE_REVERSEFINDER = 1;

    private final BuilderCache m_cache;
    private final Map<MATCHRESULT, List<Matchable>> m_patterns = new LinkedHashMap<>();

    /**
     * Create a new DfaBuilder without a {@link BuilderCache}
     */
    public DfaBuilder() {
        m_cache = null;
    }

    /**
     * Create a new DfaBuilder, with a builder cache to bypass recalculation of pre-built DFAs
     *
     * @param cache The BuilderCache to use
     */
    public DfaBuilder(BuilderCache cache) {
        m_cache = cache;
    }

    /**
     * Reset this DFA builder by forgetting all the patterns that have been added
     */
    public void clear() {
        m_patterns.clear();
    }

    public void addPattern(Matchable pat, MATCHRESULT accept) {
        if (!m_patterns.containsKey(accept)) m_patterns.put(accept, new ArrayList<Matchable>());
        List<Matchable> patlist = m_patterns.get(accept);
        patlist.add(pat);
    }

    /**
     * Build DFA for a single language
     * <p>
     * The resulting DFA matches ALL patterns that have been added to this builder
     *
     * @param ambiguityResolver When patterns for multiple results match the same string, this is called to
     *                          combine the multiple results into one.  If this is null, then a DfaAmbiguityException
     *                          will be thrown in that case.
     * @return The start state for a DFA that matches the set of patterns in language
     */
    public DfaState<MATCHRESULT> build(DfaAmbiguityResolver<MATCHRESULT> ambiguityResolver) {
        return build(Collections.singletonList(m_patterns.keySet()), ambiguityResolver).get(0);
    }

    /**
     * Build DFA for a single language
     * <p>
     * The language is specified as a subset of available MATCHRESULTs, and will include patterns
     * for each result in its set.
     *
     * @param language          set defining the languages to build
     * @param ambiguityResolver When patterns for multiple results match the same string, this is called to
     *                          combine the multiple results into one.  If this is null, then a DfaAmbiguityException
     *                          will be thrown in that case.
     * @return The start state for a DFA that matches the set of patterns in language
     */
    public DfaState<MATCHRESULT> build(Set<MATCHRESULT> language, DfaAmbiguityResolver<MATCHRESULT> ambiguityResolver) {
        return build(Collections.singletonList(language), ambiguityResolver).get(0);
    }

    /**
     * Build DFAs for multiple languages simultaneously.
     * <p>
     * Each language is specified as a subset of available MATCHRESULTs, and will include patterns
     * for each result in its set.
     * <p>
     * Languages built simultaneously will be globally minimized and will share as many states as possible.
     *
     * @param languages         sets defining the languages to build
     * @param ambiguityResolver When patterns for multiple results match the same string, this is called to
     *                          combine the multiple results into one.	If this is null, then a DfaAmbiguityException
     *                          will be thrown in that case.
     * @return Start states for DFAs that match the given languages.  This will have the same length as languages, with
     * corresponding start states in corresponding positions.
     */
    @SuppressWarnings("unchecked")
    public List<DfaState<MATCHRESULT>> build(List<Set<MATCHRESULT>> languages, DfaAmbiguityResolver<MATCHRESULT> ambiguityResolver) {
        if (languages.isEmpty()) {
            return Collections.emptyList();
        }

        SerializableDfa<MATCHRESULT> serializableDfa = null;
        if (m_cache == null) {
            serializableDfa = _build(languages, ambiguityResolver);
        } else {
            StringBuilder cacheKey = _getCacheKey(DFATYPE_MATCHER, languages, ambiguityResolver);
            serializableDfa = m_cache.getCachedItem(cacheKey);
            if (serializableDfa == null) {
                serializableDfa = _build(languages, ambiguityResolver);
                m_cache.maybeCacheItem(cacheKey, serializableDfa);
            }
        }
        return serializableDfa.getStartStates();
    }

    /**
     * Build the reverse finder DFA for all patterns that have been added to this builder
     * <p>
     * The "reverse finder DFA" for a set of patterns is applied to a string backwards from the end, and will
     * produce a {@link Boolean#TRUE} result at every position where a non-empty string match for one of the
     * patterns starts. At other positions it will produce null result.
     * <p>
     * For searching through an entire string, using a reverse finder with {@link StringSearcher} is faster than matching
     * with just the DFA for the language, especially for strings that have no matches.
     *
     * @return The start state for the reverse finder DFA
     */
    public DfaState<Boolean> buildReverseFinder() {
        return buildReverseFinders(Collections.singletonList(m_patterns.keySet())).get(0);
    }

    /**
     * Build the reverse finder DFA for a language
     * <p>
     * The language is specified as a subset of available MATCHRESULTs, and will include patterns
     * for each result in its set.
     * <p>
     * The "reverse finder DFA" for a language is applied to a string backwards from the end, and will
     * produce a {@link Boolean#TRUE} result at every position where a non-empty string in the language starts. At
     * other positions it will produce null result.
     * <p>
     * For searching through an entire string, using a reverse finder with {@link StringSearcher} is faster than matching
     * with just the DFA for the language, especially for strings that have no matches.
     *
     * @param language set defining the languages to build
     * @return The start state for the reverse finder DFA
     */
    public DfaState<Boolean> buildReverseFinder(Set<MATCHRESULT> language) {
        return buildReverseFinders(Collections.singletonList(language)).get(0);
    }

    /**
     * Build reverse finder DFAs for multiple languages simultaneously.
     * <p>
     * Each language is specified as a subset of available MATCHRESULTs, and will include patterns
     * for each result in its set.
     * <p>
     * The "reverse finder DFA" for a language is applied to a string backwards from the end, and will
     * produce a {@link Boolean#TRUE} result at every position where a non-empty string in the language starts. At
     * other positions it will produce null result.
     * <p>
     * For searching through an entire string, using a reverse finder with {@link StringSearcher} is faster than matching
     * with just the DFA for the language, especially for strings that have no matches.
     *
     * @param languages sets defining the languages to build
     * @return Start states for reverse finders for the given languages.  This will have the same length as languages, with
     * corresponding start states in corresponding positions.
     */
    @SuppressWarnings("unchecked")
    public List<DfaState<Boolean>> buildReverseFinders(List<Set<MATCHRESULT>> languages) {
        if (languages.isEmpty()) {
            return Collections.emptyList();
        }

        SerializableDfa<Boolean> serializableDfa = null;
        if (m_cache == null) {
            serializableDfa = _buildReverseFinders(languages);
        } else {
            StringBuilder cacheKey = _getCacheKey(DFATYPE_REVERSEFINDER, languages, null);
            serializableDfa = m_cache.getCachedItem(cacheKey);
            if (serializableDfa == null) {
                serializableDfa = _buildReverseFinders(languages);
                m_cache.maybeCacheItem(cacheKey, serializableDfa);
            }
        }
        return serializableDfa.getStartStates();
    }

    /**
     * Build a {@link StringSearcher} for all the patterns that have been added to this builder
     *
     * @param ambiguityResolver When patterns for multiple results match the same string, this is called to
     *                          combine the multiple results into one.  If this is null, then a DfaAmbiguityException
     *                          will be thrown in that case.
     * @return A {@link StringSearcher} for all the patterns in this builder
     */
    public StringSearcher<MATCHRESULT> buildStringSearcher(DfaAmbiguityResolver<MATCHRESULT> ambiguityResolver) {
        return new StringSearcher<>(build(ambiguityResolver), buildReverseFinder());
    }

    private static final char[] base32 = "0123456789abcdefghijklmnopqrstuv".toCharArray();

    /**
     * Build DFAs from a provided NFA
     * <p>
     * This method is used when you want to build the NFA yourself instead of letting
     * this class do it.
     * <p>
     * Languages built simultaneously will be globally minimized and will share as many states as possible.
     *
     * @param nfa               The NFA
     * @param nfaStartStates    The return value will include the DFA states corresponding to these NFA states, in the same order
     * @param ambiguityResolver When patterns for multiple results match the same string, this is called to
     *                          combine the multiple results into one.  If this is null, then a DfaAmbiguityException
     *                          will be thrown in that case.
     * @param cache             If this cache is non-null, it will be checked for a memoized result for this NFA, and will be populated
     *                          with a memoized result when the call is complete.
     * @return DFA start states that are equivalent to the given NFA start states.  This will have the same length as nfaStartStates, with
     * corresponding start states in corresponding positions.
     */
    @SuppressWarnings("unchecked")
    public static <MR> List<DfaState<MR>> buildFromNfa(Nfa<MR> nfa, int[] nfaStartStates, DfaAmbiguityResolver<MR> ambiguityResolver, BuilderCache cache) {
        StringBuilder cacheKey = null;
        SerializableDfa<MR> serializableDfa = null;
        if (cache != null) {

            //generate the cache key by serializing key info into an SHA-like hash

            cacheKey = new StringBuilder(32);
            final long c1 = 0x357BD1113171B1F2L ^ 0xC6BC279692B5CC83L,
                    c2 = 0xCAFEBEEF1337FECAL ^ 0xC6BC279692B5CC83L,
                    c3 = 0xBABE42DEEDBEEFEEL ^ 0xC6BC279692B5CC83L;
            long z1 = 0x632BE59BD9B4E019L + c1, r1 = 7L,
                    z2 = 0x632BE59BD9B4E019L + c2, r2 = 127L,
                    z3 = 0x632BE59BD9B4E019L + c3, r3 = 421L,
                    d;

            if (nfaStartStates != null) {
                for (int i = 0; i < nfaStartStates.length; i++) {
                    r1 ^= (z1 += ((d = nfaStartStates[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c1;
                    r2 ^= (z2 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c2;
                    r3 ^= (z3 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c3;
                }
            }
            if (nfa != null) {
                r1 ^= (z1 += ((d = nfa.hashCode()) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c1;
                r2 ^= (z2 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c2;
                r3 ^= (z3 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c3;

            }

            r1 ^= Long.rotateLeft((z1 * 0xC6BC279692B5CC83L ^ r1 * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z2 >>> 58));
            r2 ^= Long.rotateLeft((z2 * 0xC6BC279692B5CC83L ^ r2 * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z3 >>> 58));
            r3 ^= Long.rotateLeft((z3 * 0xC6BC279692B5CC83L ^ r3 * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z1 >>> 58));
            cacheKey
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
                /*
                SHAOutputStream sha = new SHAOutputStream();
                sha.on(false);
                ObjectOutputStream os = new ObjectOutputStream(sha);
                os.flush();
                sha.on(true);
                os.writeObject(nfaStartStates);
                os.writeObject(nfa);
                os.writeObject(ambiguityResolver);
                os.flush();
                
                cacheKey = sha.getBase32Digest();
                os.close();
                */
            serializableDfa = cache.getCachedItem(cacheKey);
        }
        if (serializableDfa == null) {
            RawDfa<MR> minimalDfa;
            {
                RawDfa<MR> rawDfa = (new DfaFromNfa<MR>(nfa, nfaStartStates, ambiguityResolver)).getDfa();
                minimalDfa = (new DfaMinimizer<MR>(rawDfa)).getMinimizedDfa();
            }
            serializableDfa = new SerializableDfa<>(minimalDfa);
            if (cacheKey != null && cache != null) {
                cache.maybeCacheItem(cacheKey, serializableDfa);
            }
        }
        return serializableDfa.getStartStates();
    }

    private StringBuilder _getCacheKey(final int dfaType, List<Set<MATCHRESULT>> languages, DfaAmbiguityResolver<? super MATCHRESULT> ambiguityResolver) {
        StringBuilder cacheKey;
        //generate the cache key by serializing key info into an SHA hash
        cacheKey = new StringBuilder(32);
        final long c1 = 0x357BD1113171B1F2L ^ 0xC6BC279692B5CC83L,
                c2 = 0xCAFEBEEF1337FECAL ^ 0xC6BC279692B5CC83L,
                c3 = 0xBABE42DEEDBEEFEEL ^ 0xC6BC279692B5CC83L;
        long z1 = 0x632BE59BD9B4E019L + c1, r1 = 7L,
                z2 = 0x632BE59BD9B4E019L + c2, r2 = 127L,
                z3 = 0x632BE59BD9B4E019L + c3, r3 = 421L,
                d;
        r1 ^= (z1 += ((d = dfaType) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c1;
        r2 ^= (z2 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c2;
        r3 ^= (z3 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c3;

        final int numLangs = languages.size();
        r1 ^= (z1 += ((d = numLangs) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c1;
        r2 ^= (z2 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c2;
        r3 ^= (z3 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c3;

        //write key stuff out in an order based on our LinkedHashMap, for deterministic serialization
        for (Entry<MATCHRESULT, List<Matchable>> patEntry : m_patterns.entrySet()) {
            boolean included = false;
            List<Matchable> patList = patEntry.getValue();
            if (patList.isEmpty()) {
                continue;
            }
            for (int i = 0; i < numLangs; ++i) {
                if (!languages.get(i).contains(patEntry.getKey())) {
                    continue;
                }
                included = true;
                break;
            }
            if (!included) {
                continue;
            }

            r1 ^= (z1 += ((d = patList.size()) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c1;
            r2 ^= (z2 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c2;
            r3 ^= (z3 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c3;
            if (numLangs > 1) {
                int bits = languages.get(0).contains(patEntry.getKey()) ? 1 : 0;
                for (int i = 1; i < languages.size(); ++i) {
                    if ((i & 31) == 0) {
                        r1 ^= (z1 += ((d = bits) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c1;
                        r2 ^= (z2 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c2;
                        r3 ^= (z3 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c3;
                        bits = 0;
                    }
                    if (languages.get(i).contains(patEntry.getKey())) {
                        bits |= 1 << (i & 31);
                    }
                }

                r1 ^= (z1 += ((d = bits) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c1;
                r2 ^= (z2 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c2;
                r3 ^= (z3 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c3;
            }
            for (Matchable pat : patList) {

                r1 ^= (z1 += ((d = pat.hashCode()) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c1;
                r2 ^= (z2 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c2;
                r3 ^= (z3 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c3;
            }
            /*
            r1 ^= (z1 += ((d = patEntry.getKey().hashCode()) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c1;
            r2 ^= (z2 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c2;
            r3 ^= (z3 += (d + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * c3;
            */
        }

        r1 ^= Long.rotateLeft((z1 * 0xC6BC279692B5CC83L ^ r1 * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z2 >>> 58));
        r2 ^= Long.rotateLeft((z2 * 0xC6BC279692B5CC83L ^ r2 * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z3 >>> 58));
        r3 ^= Long.rotateLeft((z3 * 0xC6BC279692B5CC83L ^ r3 * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z1 >>> 58));
        cacheKey
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

        /*
                    //generate the cache key by serializing key info into an SHA hash
            SHAOutputStream sha = new SHAOutputStream();
            sha.on(false);
            ObjectOutputStream os = new ObjectOutputStream(sha);
            os.flush();
            sha.on(true);
            os.writeInt(dfaType);
            final int numLangs = languages.size();
            os.writeInt(numLangs);

            //write key stuff out in an order based on our LinkedHashMap, for deterministic serialization
            for (Entry<MATCHRESULT, List<Matchable>> patEntry : m_patterns.entrySet()) {
                boolean included = false;
                List<Matchable> patList = patEntry.getValue();
                if (patList.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < numLangs; ++i) {
                    if (!languages.get(i).contains(patEntry.getKey())) {
                        continue;
                    }
                    included = true;
                    break;
                }
                if (!included) {
                    continue;
                }
                os.writeInt(patList.size());
                if (numLangs > 1) {
                    int bits = languages.get(0).contains(patEntry.getKey()) ? 1 : 0;
                    for (int i = 1; i < languages.size(); ++i) {
                        if ((i & 31) == 0) {
                            os.writeInt(bits);
                            bits = 0;
                        }
                        if (languages.get(i).contains(patEntry.getKey())) {
                            bits |= 1 << (i & 31);
                        }
                    }
                    os.writeInt(bits);
                }
                for (Matchable pat : patList) {
                    os.writeObject(pat);
                }
                os.writeObject(patEntry.getKey());
            }
            os.writeInt(0); //0-size pattern list terminates pattern map
            os.writeObject(ambiguityResolver);
            os.flush();

            cacheKey = sha.getBase32Digest();
            os.close();

         */


        return cacheKey;
    }

    private SerializableDfa<MATCHRESULT> _build(List<Set<MATCHRESULT>> languages, DfaAmbiguityResolver<MATCHRESULT> ambiguityResolver) {
        Nfa<MATCHRESULT> nfa = new Nfa<>();

        int[] nfaStartStates = new int[languages.size()];
        for (int i = 0; i < languages.size(); ++i) {
            nfaStartStates[i] = nfa.addState(null);
        }

        if (ambiguityResolver == null) {
            ambiguityResolver = new DfaAmbiguityResolver<MATCHRESULT>() {
                @Override
                public MATCHRESULT apply(Set<MATCHRESULT> data) {
                    return DfaBuilder.defaultAmbiguityResolver(data);
                }
            };
        }

        for (Entry<MATCHRESULT, List<Matchable>> patEntry : m_patterns.entrySet()) {
            List<Matchable> patList = patEntry.getValue();
            if (patList == null || patList.size() < 1) {
                continue;
            }
            int matchState = -1; //start state for matching this token
            for (int i = 0; i < languages.size(); ++i) {
                if (!languages.get(i).contains(patEntry.getKey())) {
                    continue;
                }
                if (matchState < 0) {
                    int acceptState = nfa.addState(patEntry.getKey()); //final state accepting this token
                    if (patList.size() > 1) {
                        //we have multiple patterns.  Make a union
                        matchState = nfa.addState(null);
                        for (Matchable pat : patList) {
                            nfa.addEpsilon(matchState, pat.addToNFA(nfa, acceptState));
                        }
                    } else {
                        //only one pattern no union necessary
                        matchState = patList.get(0).addToNFA(nfa, acceptState);
                    }
                }
                //language i matches these patterns
                nfa.addEpsilon(nfaStartStates[i], matchState);
            }
        }

        SerializableDfa<MATCHRESULT> serializableDfa;
        {
            RawDfa<MATCHRESULT> minimalDfa;
            {
                RawDfa<MATCHRESULT> rawDfa = (new DfaFromNfa<MATCHRESULT>(nfa, nfaStartStates, ambiguityResolver)).getDfa();
                minimalDfa = (new DfaMinimizer<MATCHRESULT>(rawDfa)).getMinimizedDfa();
            }
            serializableDfa = new SerializableDfa<>(minimalDfa);
        }
        return serializableDfa;
    }

    private SerializableDfa<Boolean> _buildReverseFinders(List<Set<MATCHRESULT>> languages) {
        Nfa<Boolean> nfa = new Nfa<>();

        int startState = nfa.addState(null);
        final int endState = nfa.addState(true);
        final DfaAmbiguityResolver<Boolean> ambiguityResolver = theDefaultAmbiguityResolver;

        //First, make an NFA that matches the reverse of all the patterns
        for (Entry<MATCHRESULT, List<Matchable>> patEntry : m_patterns.entrySet()) {
            List<Matchable> patList = patEntry.getValue();
            if (patList == null || patList.size() < 1) {
                continue;
            }
            for (int i = 0; i < languages.size(); ++i) {
                if (!languages.get(i).contains(patEntry.getKey())) {
                    continue;
                }
                for (Matchable pat : patEntry.getValue()) {
                    int st = pat.getReversed().addToNFA(nfa, endState);
                    nfa.addEpsilon(startState, st);
                }
            }
        }
        //omit the empty string
        startState = nfa.Disemptify(startState);

        //allow anything first
        startState = Pattern.maybeRepeat(CharRange.ALL).addToNFA(nfa, startState);

        //build the DFA
        SerializableDfa<Boolean> serializableDfa;
        {
            RawDfa<Boolean> minimalDfa;
            {
                RawDfa<Boolean> rawDfa = (new DfaFromNfa<Boolean>(nfa, new int[]{startState}, ambiguityResolver)).getDfa();
                minimalDfa = (new DfaMinimizer<Boolean>(rawDfa)).getMinimizedDfa();
            }
            serializableDfa = new SerializableDfa<>(minimalDfa);
        }
        return serializableDfa;
    }

    /*
    private DfaAmbiguityResolver<MATCHRESULT> genericDefaultAmbiguityResolver = new DfaAmbiguityResolver<MATCHRESULT>() {
        @Override
        public MATCHRESULT apply(Set<MATCHRESULT> data) {
            return DfaBuilder.defaultAmbiguityResolver(data);
        }
    };
    */

    private static DfaAmbiguityResolver<Boolean> theDefaultAmbiguityResolver = new DfaAmbiguityResolver<Boolean>() {
        @Override
        public Boolean apply(Set<Boolean> data) {
            return DfaBuilder.defaultAmbiguityResolver(data);
        }
    };

    private static <T> T defaultAmbiguityResolver(Set<T> matches) {
        throw new DfaAmbiguityException(matches);
    }
}

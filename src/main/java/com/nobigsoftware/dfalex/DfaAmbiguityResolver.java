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
import java.util.Set;
import java.util.function.Function;

import com.nobigsoftware.util.BuilderCache;

/**
 * Implementations of this interface are used to resolve ambiguities in {@link DfaBuilder}.
 * <P>
 * When it's possible for a single string to match patterns that produce different results, the
 * ambiguity resolver is called to determine what the result should be.
 * <P>
 * The implementation can throw a {@link DfaAmbiguityException} in this case, or can combine the
 * multiple result objects into a single object if its type (e.g., EnumSet) permits.
 * <P>
 * This interface implements Serializable so that it can be written into the key signature for
 * {@link BuilderCache}.
 */
public interface DfaAmbiguityResolver<MATCHRESULT> extends Serializable
{
    MATCHRESULT apply(Set<MATCHRESULT> data);
}

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
package com.nobigsoftware.util;

import com.nobigsoftware.dfalex.SerializableDfa;

/**
 * Implementations of this interface can cache serializable objects that
 * can be used to bypass expensive building operations by providing
 * pre-built objects
 */
public interface BuilderCache
{
    /**
     * Get a cached item.
     * 
     * @param key  The key used to identify the item.  The key uniquely identifies all
     *  of the source information that will go into building the item if this call fails
     *  to retrieve a cached version.  Typically this will be a cryptographic hash of
     *  the serialized form of that information.
     *
     * @return  the item that was previously cached under the key, or null if no such item
     *  can be retrieved.
     */
    <R> SerializableDfa<R> getCachedItem(CharSequence key);
    
    /**
     * This method may be called when an item is built, providing an opportunity to
     * cache it.
     * 
     * @param item  The item to cache, if desired
     */
    <R> void maybeCacheItem(CharSequence key, SerializableDfa<R> item);
}

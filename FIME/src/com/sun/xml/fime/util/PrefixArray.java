/*
 * FIME (Fast Infoset ME) software ("Software")
 *
 * Copyright, 2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Software is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at:
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations.
 * 
 *    Sun supports and benefits from the global community of open source
 * developers, and thanks the community for its important contributions and
 * open standards-based technology, which Sun has adopted into many of its
 * products.
 * 
 *    Please note that portions of Software may be provided with notices and
 * open source licenses from such communities and third parties that govern the
 * use of those portions, and any licenses granted hereunder do not alter any
 * rights and obligations you may have under such open source licenses,
 * however, the disclaimer of warranty and limitation of liability provisions
 * in this License will apply to all Software in this distribution.
 * 
 *    You acknowledge that the Software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 */ 


package com.sun.xml.fime.util;


import java.util.Enumeration;
import java.util.NoSuchElementException;

import com.sun.xml.fime.EncodingConstants;
import com.sun.xml.fime.jvnet.FastInfosetException;

public class PrefixArray extends ValueArray {
    public static final int PREFIX_MAP_SIZE = 64;
    
    private int _initialCapacity;
    
    public String[] _array;
    
    private PrefixArray _readOnlyArray;

    private static class PrefixEntry {
        private PrefixEntry next;
        private int prefixId;
    }

    private PrefixEntry[] _prefixMap = new PrefixEntry[PREFIX_MAP_SIZE];
    
    private PrefixEntry _prefixPool;
    
    private static class NamespaceEntry {
        private NamespaceEntry next;
        private int declarationId;
        private int namespaceIndex;
        
        private String prefix;
        private String namespaceName;
        private int prefixEntryIndex;
    }

    private NamespaceEntry _namespacePool;
    
    private NamespaceEntry[] _inScopeNamespaces;
    
    public int[] _currentInScope;
    
    public int _declarationId;
    
    public PrefixArray(int initialCapacity, int maximumCapacity) {
        _initialCapacity = initialCapacity;
        _maximumCapacity = maximumCapacity;
        
        _array = new String[initialCapacity];
        // Sizes of _inScopeNamespaces and _currentInScope need to be two
        // greater than _array because 0 represents the empty string and
        // 1 represents the xml prefix
        _inScopeNamespaces = new NamespaceEntry[initialCapacity + 2];
        _currentInScope = new int[initialCapacity + 2];
        
        increaseNamespacePool(initialCapacity);
        increasePrefixPool(initialCapacity);
        
        initializeEntries();
    }

    public PrefixArray() {
        this(DEFAULT_CAPACITY, MAXIMUM_CAPACITY);
    }

    private final void initializeEntries() {
        _inScopeNamespaces[0] = _namespacePool;
        _namespacePool = _namespacePool.next;
        _inScopeNamespaces[0].next = null;        
        _inScopeNamespaces[0].prefix = "";
        _inScopeNamespaces[0].namespaceName = "";
        _inScopeNamespaces[0].namespaceIndex = _currentInScope[0] = 0;
        
        int index = KeyIntMap.indexFor(KeyIntMap.hashHash(_inScopeNamespaces[0].prefix.hashCode()), _prefixMap.length);
        _prefixMap[index] = _prefixPool;
        _prefixPool = _prefixPool.next;
        _prefixMap[index].next = null;
        _prefixMap[index].prefixId = 0;
        
        
        _inScopeNamespaces[1] = _namespacePool;
        _namespacePool = _namespacePool.next;
        _inScopeNamespaces[1].next = null;
        _inScopeNamespaces[1].prefix = EncodingConstants.XML_NAMESPACE_PREFIX;
        _inScopeNamespaces[1].namespaceName = EncodingConstants.XML_NAMESPACE_NAME;
        _inScopeNamespaces[1].namespaceIndex = _currentInScope[1] = 1;
        
        index = KeyIntMap.indexFor(KeyIntMap.hashHash(_inScopeNamespaces[1].prefix.hashCode()), _prefixMap.length);
        if (_prefixMap[index] == null) {
            _prefixMap[index] = _prefixPool;
            _prefixPool = _prefixPool.next;
            _prefixMap[index].next = null;
        } else {
            final PrefixEntry e = _prefixMap[index];
            _prefixMap[index] = _prefixPool;
            _prefixPool = _prefixPool.next;
            _prefixMap[index].next = e;
        }
        _prefixMap[index].prefixId = 1;
    }
    
    private final void increaseNamespacePool(int capacity) {
        if (_namespacePool == null) {
            _namespacePool = new NamespaceEntry();
        }
        
        for (int i = 0; i < capacity; i++) {
            NamespaceEntry ne = new NamespaceEntry();
            ne.next = _namespacePool;
            _namespacePool = ne;
        }
    }
    
    private final void increasePrefixPool(int capacity) {
        if (_prefixPool == null) {
            _prefixPool = new PrefixEntry();
        }
        
        for (int i = 0; i < capacity; i++) {
            PrefixEntry pe = new PrefixEntry();
            pe.next = _prefixPool;
            _prefixPool = pe;
        }
    }

    public int countNamespacePool() {
        int i = 0;
        NamespaceEntry e = _namespacePool;
        while (e != null) {
            i++;
            e = e.next;
        }
        return i;
    }
    
    public int countPrefixPool() {
        int i = 0;
        PrefixEntry e = _prefixPool;
        while (e != null) {
            i++;
            e = e.next;
        }
        return i;
    }
    
    public final void clear() {
        for (int i = _readOnlyArraySize; i < _size; i++) {
            _array[i] = null;
        }
        _size = _readOnlyArraySize;
    }

    public final void clearCompletely() {
        _prefixPool = null;
        _namespacePool = null;
        
        for (int i = 0; i < _size + 2; i++) {
            _currentInScope[i] = 0;
            _inScopeNamespaces[i] = null;            
        }
        
        for (int i = 0; i < _prefixMap.length; i++) {
            _prefixMap[i] = null;
        }
                
        increaseNamespacePool(_initialCapacity);
        increasePrefixPool(_initialCapacity);
        
        initializeEntries();
        
        _declarationId = 0;
        
        clear();
    }
    
    public final String[] getArray() {
        return _array;
    }
    
    public final void setReadOnlyArray(ValueArray readOnlyArray, boolean clear) {
        if (!(readOnlyArray instanceof PrefixArray)) {
            throw new IllegalArgumentException(MessageCenter.
                    getString("message.illegalClass", new Object[]{readOnlyArray}));
        }       
        
        setReadOnlyArray((PrefixArray)readOnlyArray, clear);
    }

    public final void setReadOnlyArray(PrefixArray readOnlyArray, boolean clear) {
        if (readOnlyArray != null) {
            _readOnlyArray = readOnlyArray;
            _readOnlyArraySize = readOnlyArray.getSize();
            
            // Resize according to size of read only arrays
            _inScopeNamespaces = new NamespaceEntry[_readOnlyArraySize + _inScopeNamespaces.length];
            _currentInScope = new int[_readOnlyArraySize + _currentInScope.length];
            // Intialize first two entries
            initializeEntries();
            
            if (clear) {
                clear();
            }

            _array = getCompleteArray();
            _size = _readOnlyArraySize;
        }
    }

    public final String[] getCompleteArray() {
        if (_readOnlyArray == null) {
            return _array;
        } else {
            final String[] ra = _readOnlyArray.getCompleteArray();
            final String[] a = new String[_readOnlyArraySize + _array.length];
            System.arraycopy(ra, 0, a, 0, _readOnlyArraySize);
            return a;
        }
    }
    
    public final String get(int i) {
        return _array[i];
   }
 
    public final int add(String s) {
        if (_size == _array.length) {
            resize();
        }
            
       _array[_size++] = s;
       return _size;
    }
    
    protected final void resize() {
        if (_size == _maximumCapacity) {
            throw new RuntimeException(MessageCenter.getString("message.arrayMaxCapacity"));
        }

        int newSize = _size * 3 / 2 + 1;
        if (newSize > _maximumCapacity) {
            newSize = _maximumCapacity;
        }

        final String[] newArray = new String[newSize];
        System.arraycopy(_array, 0, newArray, 0, _size);
        _array = newArray;

        newSize += _readOnlyArraySize;
        final NamespaceEntry[] newInScopeNamespaces = new NamespaceEntry[newSize + 2];
        System.arraycopy(_inScopeNamespaces, 0, newInScopeNamespaces, 0, _readOnlyArraySize + _size + 2);
        _inScopeNamespaces = newInScopeNamespaces;

        final int[] newCurrentInScope = new int[newSize + 2];
        System.arraycopy(_currentInScope, 0, newCurrentInScope, 0, _readOnlyArraySize + _size + 2);
        _currentInScope = newCurrentInScope;            
    }
    
    public final void clearDeclarationIds() {
        for (int i = 0; i < _size; i++) {
            final NamespaceEntry e = _inScopeNamespaces[i];
            if (e != null) {
                e.declarationId = 0;
            }
        }

        _declarationId = 1;
    }
    
    public final void pushScope(int prefixIndex, int namespaceIndex) throws FastInfosetException {
        if (_namespacePool == null) {
            increaseNamespacePool(16);
        }
        
        final NamespaceEntry e = _namespacePool;
        _namespacePool = e.next;

        final NamespaceEntry current = _inScopeNamespaces[++prefixIndex];
        if (current == null) {
            e.declarationId = _declarationId;
            e.namespaceIndex = _currentInScope[prefixIndex] = ++namespaceIndex;
            e.next = null;
            
            _inScopeNamespaces[prefixIndex] = e;
        } else if (current.declarationId < _declarationId) {
            e.declarationId = _declarationId;
            e.namespaceIndex = _currentInScope[prefixIndex] = ++namespaceIndex;
            e.next = current;
            
            current.declarationId = 0;
            _inScopeNamespaces[prefixIndex] = e;
        } else {
            throw new FastInfosetException(MessageCenter.getString("message.duplicateNamespaceAttribute"));
        }
    }
    
    public final void pushScopeWithPrefixEntry(String prefix, String namespaceName,
            int prefixIndex, int namespaceIndex) throws FastInfosetException {
        if (_namespacePool == null) {
            increaseNamespacePool(16);
        }
        if (_prefixPool == null) {
            increasePrefixPool(16);
        }
        
        final NamespaceEntry e = _namespacePool;
        _namespacePool = e.next;

        final NamespaceEntry current = _inScopeNamespaces[++prefixIndex];
        if (current == null) {
            e.declarationId = _declarationId;
            e.namespaceIndex = _currentInScope[prefixIndex] = ++namespaceIndex;
            e.next = null;
            
            _inScopeNamespaces[prefixIndex] = e;
        } else if (current.declarationId < _declarationId) {
            e.declarationId = _declarationId;
            e.namespaceIndex = _currentInScope[prefixIndex] = ++namespaceIndex;
            e.next = current;
            
            current.declarationId = 0;
            _inScopeNamespaces[prefixIndex] = e;
        } else {
            throw new FastInfosetException(MessageCenter.getString("message.duplicateNamespaceAttribute"));
        }
 
        final PrefixEntry p = _prefixPool;
        _prefixPool = _prefixPool.next;
        p.prefixId = prefixIndex;
                
        e.prefix = prefix;
        e.namespaceName = namespaceName;
        e.prefixEntryIndex = KeyIntMap.indexFor(KeyIntMap.hashHash(prefix.hashCode()), _prefixMap.length);
        
        final PrefixEntry pCurrent = _prefixMap[e.prefixEntryIndex];
        p.next = pCurrent;
        _prefixMap[e.prefixEntryIndex] = p;
    }
    
    public final void popScope(int prefixIndex) {
        final NamespaceEntry e = _inScopeNamespaces[++prefixIndex];
        _inScopeNamespaces[prefixIndex] = e.next;
        _currentInScope[prefixIndex] = (e.next != null) ? e.next.namespaceIndex : 0;
        
        e.next = _namespacePool;
        _namespacePool = e;
    }

    public final void popScopeWithPrefixEntry(int prefixIndex) {
        final NamespaceEntry e = _inScopeNamespaces[++prefixIndex];
        
        _inScopeNamespaces[prefixIndex] = e.next;
        _currentInScope[prefixIndex] = (e.next != null) ? e.next.namespaceIndex : 0;
        
        e.prefix = e.namespaceName = null;
        e.next = _namespacePool;
        _namespacePool = e;
        
        PrefixEntry current = _prefixMap[e.prefixEntryIndex];
        if (current.prefixId == prefixIndex) {
            _prefixMap[e.prefixEntryIndex] = current.next;
            current.next = _prefixPool;
            _prefixPool = current;
        } else {
            PrefixEntry prev = current;
            current = current.next;
            while (current != null) {
                if (current.prefixId == prefixIndex) {
                    prev.next = current.next;
                    current.next = _prefixPool;
                    _prefixPool = current;
                    break;
                }
                prev = current;
                current = current.next;
            }        
        }        
    }
    
    public final String getNamespaceFromPrefix(String prefix) {
        final int index = KeyIntMap.indexFor(KeyIntMap.hashHash(prefix.hashCode()), _prefixMap.length);
        PrefixEntry pe = _prefixMap[index];
        while (pe != null) {
            final NamespaceEntry ne = _inScopeNamespaces[pe.prefixId];
            if (prefix == ne.prefix || prefix.equals(ne.prefix)) {
                return ne.namespaceName;
            }
            pe = pe.next;
        }
        
        return null;
    }
    
    public final String getPrefixFromNamespace(String namespaceName) {
        int position = 0;
        while (++position < _size + 2) {
            final NamespaceEntry ne = _inScopeNamespaces[position];
            if (ne != null && namespaceName.equals(ne.namespaceName)) {
                return ne.prefix;
            }
        }
        
        return null;
    }
    
    public final Enumeration getPrefixes() {
        return new Enumeration() {
            int _position = 1;
            NamespaceEntry _ne = _inScopeNamespaces[_position];
            
            public boolean hasMoreElements() {
                return _ne != null;
            }
            
            public Object nextElement() {
                if (_position == _size + 2) {
                    throw new NoSuchElementException();
                }
                
                final String prefix = _ne.prefix;
                moveToNext();                
                return prefix;
            }
            
            private final void moveToNext() {
                while (++_position < _size + 2) {
                    _ne = _inScopeNamespaces[_position];
                    if (_ne != null) {
                        return;
                    }
                }
                _ne = null;
            }
            
        };        
    }
    
    public final Enumeration getPrefixesFromNamespace(final String namespaceName) {
        return new Enumeration() {
            String _namespaceName = namespaceName;
            int _position = 0;
            NamespaceEntry _ne;
            
            {
                moveToNext();
            }
            
            public boolean hasMoreElements() {
                return _ne != null;
            }
            
            public Object nextElement() {
                if (_position == _size + 2) {
                    throw new NoSuchElementException();
                }
                                
                final String prefix = _ne.prefix;
                moveToNext();
                return prefix;
            }
            
            private final void moveToNext() {
                while (++_position < _size + 2) {
                    _ne = _inScopeNamespaces[_position];
                    if (_ne != null && _namespaceName.equals(_ne.namespaceName)) {
                        return;
                    }
                }
                _ne = null;
            }
        };        
    }
}

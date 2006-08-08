/*
 * Fast Infoset ver. 0.1 software ("Software")
 * 
 * Copyright, 2004-2005 Sun Microsystems, Inc. All Rights Reserved. 
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

package org.jvnet.fastinfoset.stax;

import com.sun.xml.fastinfoset.EncodingConstants;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import org.jvnet.fastinfoset.EncodingAlgorithmIndexes;

/**
 * Low level Fast Infoset stream writer.
 * <p>
 * This interface provides additional stream-based serialization methods for the
 * case where an application is in specific control of the serialization
 * process and has the knowledge to call the LowLevel methods in the required
 * order.
 * <p>
 * For example, the application may be able to perform efficient information 
 * to indexing mapping and to provide certain information in UTF-8 encoded form.
 * <p>
 * These methods may be used in conjuction with {@link javax.xml.stream.XMLStreamWriter}
 * as long as an element fragment written using the efficient streaming methods
 * are self-contained and no sub-fragment is written using methods from 
 * {@link javax.xml.stream.XMLStreamWriter}.
 * <p>
 * The required call sequence is as follows:
 * <pre>
 * CALLSEQUENCE    := {@link #startDocument startDocument} 
 *                    initiateLowLevelWriting ELEMENT 
 *                    {@link #endDocument endDocument}
 *                 |  initiateLowLevelWriting ELEMENT   // for fragment
 *
 * ELEMENT         := writeLowLevelTerminationAndMark
 *                    NAMESPACES?
 *                    ELEMENT_NAME
 *                    ATTRIBUTES? 
 *                    writeLowLevelEndStartElement
 *                    CONTENTS
 *                    writeLowLevelEndElement
 *
 * NAMESPACES      := writeLowLevelStartNamespaces
 *                    writeLowLevelNamespace* 
 *                    writeLowLevelEndNamespaces
 *
 * ELEMENT_NAME    := writeLowLevelStartElementIndexed
 *                 |  writeLowLevelStartNameLiteral
 *                 |  writeLowLevelStartElement
 * 
 * ATTRUBUTES      := writeLowLevelStartAttributes
 *                   (ATTRIBUTE_NAME writeLowLevelAttributeValue)*
 *
 * ATTRIBUTE_NAME  := writeLowLevelAttributeIndexed
 *                 |  writeLowLevelStartNameLiteral
 *                 |  writeLowLevelAttribute
 *       
 *
 * CONTENTS      := (ELEMENT | writeLowLevelText writeLowLevelOctets)*
 * </pre>
 * <p>
 * Some methods defer to the application for the mapping of information
 * to indexes.
 */
public interface LowLevelFastInfosetStreamWriter {
    /**
     * Initiate low level writing of an element fragment.
     * <p>
     * This method must be invoked before other low level method.
     */
    public void initiateLowLevelWriting()
    throws XMLStreamException;
        
    /**
     * Get the next index to apply to an Element Information Item.
     * <p>
     * This will increment the next obtained index such that:
     * <pre>
     * i = w.getNextElementIndex();
     * j = w.getNextElementIndex();
     * i == j + 1;
     * </pre>
     * @return the index.
     */
    public int getNextElementIndex();

    /**
     * Get the next index to apply to an Attribute Information Item.
     * This will increment the next obtained index such that:
     * <pre>
     * i = w.getNextAttributeIndex();
     * j = w.getNextAttributeIndex();
     * i == j + 1;
     * </pre>
     * @return the index.
     */
    public int getNextAttributeIndex();
    
    /**
     * Get the next index to apply to an [local name] of an Element or Attribute
     * Information Item.
     * This will increment the next obtained index such that:
     * <pre>
     * i = w.getNextLocalNameIndex();
     * j = w.getNextLocalNameIndex();
     * i == j + 1;
     * </pre>
     * @return the index.
     */
    public int getNextLocalNameIndex();

    public void writeLowLevelTerminationAndMark()
    throws IOException;

    public void writeLowLevelStartElementIndexed(int type, int index)
    throws IOException;
    
    public void writeLowLevelStartElement(int type, 
            String prefix, String localName, String namespaceURI) 
            throws IOException;
                
    public void writeLowLevelStartNamespaces()
    throws IOException;
    
    public void writeLowLevelNamespace(String prefix, String namespaceName) 
        throws IOException;
    
    public void writeLowLevelEndNamespaces()
    throws IOException;
    
    public void writeLowLevelStartAttributes()
    throws IOException;
        
    public void writeLowLevelAttributeIndexed(int index)
    throws IOException;
    
    public void writeLowLevelAttribute(
            String prefix, String namespaceURI, String localName)
            throws IOException;
                
    public void writeLowLevelAttributeValue(String value)
    throws IOException;
    
    public void writeLowLevelStartNameLiteral(int type, 
            String prefix, byte[] utf8LocalName, String namespaceURI)
            throws IOException;
    
    public void writeLowLevelStartNameLiteral(int type,
            String prefix, int localNameIndex, String namespaceURI)
            throws IOException;
    
    public void writeLowLevelEndStartElement()
    throws IOException;
    
    public void writeLowLevelEndElement()
    throws IOException;
    
    public void writeLowLevelText(char[] text, int length)
    throws IOException;
    
    public void writeLowLevelText(String text)
    throws IOException;
    
    public void writeLowLevelOctets(byte[] octets, int length)
    throws IOException;
}
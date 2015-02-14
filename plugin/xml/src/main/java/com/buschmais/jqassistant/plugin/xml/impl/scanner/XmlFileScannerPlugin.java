package com.buschmais.jqassistant.plugin.xml.impl.scanner;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.*;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.buschmais.jqassistant.plugin.xml.api.model.*;
import com.google.common.base.Strings;

public class XmlFileScannerPlugin extends AbstractScannerPlugin<FileResource, XmlFileDescriptor> {

    private XMLInputFactory inputFactory;

    @Override
    protected void initialize() {
        inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) throws IOException {
        String lowerCase = path.toLowerCase();
        return lowerCase.endsWith(".xml") || lowerCase.endsWith(".xsd");
    }

    @Override
    public XmlFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        Store store = scanner.getContext().getStore();
        XmlElementDescriptor parentElement = null;
        XmlFileDescriptor documentDescriptor = null;
        Map<String, XmlNamespaceDescriptor> namespaceMappings = new HashMap<>();
        try (InputStream stream = item.createStream()) {
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(stream);
            while (streamReader.hasNext()) {
                int eventType = streamReader.getEventType();
                switch (eventType) {
                case XMLStreamConstants.START_DOCUMENT:
                    documentDescriptor = startDocument(streamReader, store);
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    parentElement = startElement(streamReader, documentDescriptor, parentElement, namespaceMappings, store);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    parentElement = endElement(streamReader, parentElement, namespaceMappings);
                    break;
                case XMLStreamConstants.SPACE:
                case XMLStreamConstants.CHARACTERS:
                    characters(streamReader, XmlTextDescriptor.class, parentElement, store);
                    break;
                case XMLStreamConstants.CDATA:
                    characters(streamReader, XmlCDataDescriptor.class, parentElement, store);
                    break;
                }
                streamReader.next();
            }
        } catch (XMLStreamException e) {
            throw new IOException("Cannot read document.", e);
        }
        return documentDescriptor;
    }

    private XmlFileDescriptor startDocument(XMLStreamReader streamReader, Store store) {
        XmlFileDescriptor documentDescriptor;
        documentDescriptor = store.create(XmlFileDescriptor.class);
        documentDescriptor.setVersion(streamReader.getVersion());
        documentDescriptor.setCharacterEncodingScheme(streamReader.getCharacterEncodingScheme());
        documentDescriptor.setStandalone(streamReader.isStandalone());
        return documentDescriptor;
    }

    private XmlElementDescriptor startElement(XMLStreamReader streamReader, XmlFileDescriptor documentDescriptor, XmlElementDescriptor parentElement,
            Map<String, XmlNamespaceDescriptor> namespaceMappings, Store store) {
        XmlElementDescriptor elementDescriptor = store.create(XmlElementDescriptor.class);
        // get namespace declaration
        for (int i = 0; i < streamReader.getNamespaceCount(); i++) {
            XmlNamespaceDescriptor namespaceDescriptor = store.create(XmlNamespaceDescriptor.class);
            String prefix = streamReader.getNamespacePrefix(i);
            String uri = streamReader.getNamespaceURI(i);
            if (!Strings.isNullOrEmpty(prefix)) {
                namespaceDescriptor.setPrefix(prefix);
                namespaceMappings.put(prefix, namespaceDescriptor);
            }
            namespaceDescriptor.setUri(uri);
            elementDescriptor.getDeclaredNamespaces().add(namespaceDescriptor);
        }
        setName(elementDescriptor, streamReader.getLocalName(), streamReader.getPrefix(), namespaceMappings);

        for (int i = 0; i < streamReader.getAttributeCount(); i++) {
            XmlAttributeDescriptor attributeDescriptor = store.create(XmlAttributeDescriptor.class);
            setName(attributeDescriptor, streamReader.getAttributeLocalName(i), streamReader.getAttributePrefix(i), namespaceMappings);
            attributeDescriptor.setValue(streamReader.getAttributeValue(i));
            elementDescriptor.getAttributes().add(attributeDescriptor);
        }

        if (parentElement == null) {
            documentDescriptor.setRootElement(elementDescriptor);
        } else {
            parentElement.getElements().add(elementDescriptor);
        }
        parentElement = elementDescriptor;
        return parentElement;
    }

    private XmlElementDescriptor endElement(XMLStreamReader streamReader, XmlElementDescriptor parentElement,
            Map<String, XmlNamespaceDescriptor> namespaceMappings) {
        parentElement = parentElement.getParent();
        for (int i = 0; i < streamReader.getNamespaceCount(); i++) {
            String prefix = streamReader.getNamespacePrefix(i);
            if (!Strings.isNullOrEmpty(prefix)) {
                namespaceMappings.remove(prefix);
            }
        }
        return parentElement;
    }

    private void characters(XMLStreamReader streamReader, Class<? extends XmlTextDescriptor> type, XmlElementDescriptor parentElement, Store store) {
        if (streamReader.hasText()) {
            int start = streamReader.getTextStart();
            int length = streamReader.getTextLength();
            String text = new String(streamReader.getTextCharacters(), start, length).trim();
            if (!Strings.isNullOrEmpty(text)) {
                XmlTextDescriptor charactersDescriptor = store.create(type);
                charactersDescriptor.setValue(text);
                parentElement.getCharacters().add(charactersDescriptor);
            }
        }
    }

    private void setName(OfNamespaceDescriptor ofNamespaceDescriptor, String localName, String prefix, Map<String, XmlNamespaceDescriptor> namespaceMappings) {
        ofNamespaceDescriptor.setName(localName);
        if (!Strings.isNullOrEmpty(prefix)) {
            XmlNamespaceDescriptor namespaceDescriptor = namespaceMappings.get(prefix);
            ofNamespaceDescriptor.setNamespaceDeclaration(namespaceDescriptor);
        }
    }

}
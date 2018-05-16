/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.expedia.www.haystack.commons.secretDetector;

import io.dataapps.chlorine.finder.FinderEngine;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Finds secrets:
 * <ul>
 *     <li>For a Spans, finds the tag keys and field keys are secrets.</li>
 *     <li>For an XML document, find the names of elements or attributes whose values are secrets.</li>
 * </ul>
 */
@SuppressWarnings("WeakerAccess")
public class XmlDetector extends DetectorBase implements ValueMapper<Document, Iterable<String>> {
    private static final String TEXT_TEMPLATE =
            "Confidential data has been found in an XML BLOB: root [%s] [%s]";

    public XmlDetector(String bucket) {
        this(new FinderEngine(), new SpanS3ConfigFetcher(bucket, "secret-detector/blobWhiteListItems.txt"));
    }

    public XmlDetector(FinderEngine finderEngine, SpanS3ConfigFetcher spanS3ConfigFetcher) {
        super(finderEngine, spanS3ConfigFetcher);
    }

    /**
     * Finds secrets in an XML Document
     * @param document the XML document in which to look for secrets
     * @return keys of the secrets (the names of the attributes or elements whose values are secrets)
     */
    public Map<String, List<String>> findSecrets(Document document) {
        document.normalizeDocument();
        final Map<String, List<String>> mapOfTypeToKeysOfSecrets = new HashMap<>();
        findSecrets(document.getDocumentElement(), mapOfTypeToKeysOfSecrets);
        return mapOfTypeToKeysOfSecrets;
    }

    private void findSecrets(Node node, Map<String,List<String>> mapOfTypeToKeysOfSecrets) {
        findSecretsInNodeValue(node, mapOfTypeToKeysOfSecrets);
        findSecretsInChildNodes(node, mapOfTypeToKeysOfSecrets);
        findSecretsInAttributes(node, mapOfTypeToKeysOfSecrets);
    }

    private void findSecretsInNodeValue(Node node, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        final String nodeValue = node.getNodeValue();
        if(nodeValue != null) {
            final Map<String, List<String>> secrets = finderEngine.findWithType(nodeValue);
            if(!secrets.isEmpty()) {
                final String completeHierarchy = getCompleteHierarchy(node);
                if(completeHierarchy.endsWith("/#text")) { // prevents double detection of attribute values;
                    // couldn't figure out a cleaner way to do this. An attribute with a secret as its value will see
                    // that secret appear in this check directly (because getNodeValue() returns Attr.value, per
                    // https://docs.oracle.com/javase/8/docs/api/index.html?javax/xml/parsers/DocumentBuilderFactory.html)
                    // as well as when the attributes child text node is traversed. This check ensures that only the
                    // latter will be marked as a secret, and has the nice effect of making the keys of all secrets
                    // in XML end in the "/#text" String.
                    putKeysOfSecretsIntoMap(mapOfTypeToKeysOfSecrets, completeHierarchy, secrets);
                }
            }
        }
    }

    private void findSecretsInChildNodes(Node node, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        final NodeList childNodes = node.getChildNodes();
        if(childNodes != null) {
            for (int index = 0; index < childNodes.getLength(); index++) {
                final Node childNode = childNodes.item(index);
                findSecrets(childNode, mapOfTypeToKeysOfSecrets);
            }
        }
    }

    private void findSecretsInAttributes(Node node, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        final NamedNodeMap attributes = node.getAttributes();
        if(attributes != null) {
            for (int index = 0; index < attributes.getLength(); index++) {
                final Node attribute = attributes.item(index);
                findSecrets(attribute, mapOfTypeToKeysOfSecrets);
            }
        }
    }

    private String getCompleteHierarchy(Node leafNode) {
        final LinkedList<String> nodeList = new LinkedList<>();
        Node node = leafNode;
        do {
            nodeList.addFirst(node.getNodeName());
            node = node instanceof Attr ? ((Attr) node).getOwnerElement() : node.getParentNode();
        } while(node != null);
        return String.join("/", nodeList.toArray(new String[0]));
    }

    @Override
    public Iterable<String> apply(Document document) {
        final Map<String, List<String>> mapOfTypeToKeysOfSecrets = findSecrets(document);
        if (mapOfTypeToKeysOfSecrets.isEmpty()) {
            return Collections.emptyList();
        }
        final String emailText = getEmailText(document, mapOfTypeToKeysOfSecrets);
        for (Map.Entry<String, List<String>> finderNameToKeysOfSecrets : mapOfTypeToKeysOfSecrets.entrySet()) {
            final String finderName = finderNameToKeysOfSecrets.getKey();
            final List<String> keysOfSecrets = finderNameToKeysOfSecrets.getValue();
            for (int index = 0; index < keysOfSecrets.size(); index++) {
                // TODO Update SpanS3ConfigFetcher to support XML path strings in whitelist for XML
                /*
                if (spanS3ConfigFetcher.isTagInXmlWhiteList(finderName, keysOfSecrets.get(index))) {
                    keysOfSecrets.remove(index);
                }
                */
            }
        }

        return Collections.singleton(emailText);
    }

    @SuppressWarnings("WeakerAccess")
    public static String getEmailText(Document document, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        return String.format(TEXT_TEMPLATE, document.getDocumentElement().getLocalName(),
                mapOfTypeToKeysOfSecrets.toString());
    }

}
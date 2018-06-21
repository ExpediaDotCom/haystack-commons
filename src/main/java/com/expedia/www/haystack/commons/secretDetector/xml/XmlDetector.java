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
package com.expedia.www.haystack.commons.secretDetector.xml;

import com.expedia.www.haystack.commons.secretDetector.DetectorBase;
import com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcher;
import io.dataapps.chlorine.finder.FinderEngine;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Finds the names of elements or attributes whose values are secrets.
 */
@SuppressWarnings("WeakerAccess")
public class XmlDetector extends DetectorBase {
    private static final String[] ZERO_LENGTH_STRING_ARRAY = new String[0];

    public XmlDetector(String bucket) {
        this(new FinderEngine(), new S3ConfigFetcher(bucket, "secret-detector/xmlWhiteListItems.txt"));
    }

    public XmlDetector(FinderEngine finderEngine, S3ConfigFetcher s3ConfigFetcher) {
        super(finderEngine, s3ConfigFetcher);
    }

    /**
     * Finds secrets in an XML Document
     *
     * @param document the XML document in which to look for secrets
     * @return keys of the secrets (the names of the attributes or elements whose values are secrets)
     */
    public Map<String, List<String>> findSecrets(Document document) {
        document.normalizeDocument();
        final Map<String, List<String>> mapOfTypeToKeysOfSecrets = new HashMap<>();
        findSecrets(document.getDocumentElement(), mapOfTypeToKeysOfSecrets);
        return mapOfTypeToKeysOfSecrets;
    }

    private void findSecrets(Node node, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        findSecretsInNodeValue(node, mapOfTypeToKeysOfSecrets);
        findSecretsInChildNodes(node, mapOfTypeToKeysOfSecrets);
        findSecretsInAttributes(node, mapOfTypeToKeysOfSecrets);
    }

    private void findSecretsInNodeValue(Node node, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        final String nodeValue = node.getNodeValue();
        if (nodeValue != null) {
            final Map<String, List<String>> secrets = finderEngine.findWithType(nodeValue);
            if (!secrets.isEmpty()) {
                final String completeHierarchy = getCompleteHierarchy(node);
                if (completeHierarchy.endsWith("/#text")) { // prevents double detection of attribute values;
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
        if (childNodes != null) {
            for (int index = 0; index < childNodes.getLength(); index++) {
                final Node childNode = childNodes.item(index);
                findSecrets(childNode, mapOfTypeToKeysOfSecrets);
            }
        }
    }

    private void findSecretsInAttributes(Node node, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        final NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int index = 0; index < attributes.getLength(); index++) {
                final Node attribute = attributes.item(index);
                findSecrets(attribute, mapOfTypeToKeysOfSecrets);
            }
        }
    }

    private static String getCompleteHierarchy(Node leafNode) {
        final LinkedList<String> nodeList = new LinkedList<>();
        Node node = leafNode;
        do {
            nodeList.addFirst(node.getNodeName());
            node = (node instanceof Attr) ? ((Attr) node).getOwnerElement() : node.getParentNode();
        } while (node != null);
        return String.join("/", nodeList.toArray(ZERO_LENGTH_STRING_ARRAY));
    }
/*
    @SuppressWarnings("MethodWithMultipleLoops")
    @Override
    public Iterable<String> apply(@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") Document document) {
        final Map<String, List<String>> mapOfTypeToKeysOfSecrets = findSecrets(document);
        if (mapOfTypeToKeysOfSecrets.isEmpty()) {
            return Collections.emptyList();
        }
        final String emailText = getEmailText(mapOfTypeToKeysOfSecrets);
        final Iterator<Map.Entry<String, List<String>>> firstLevelIterator = mapOfTypeToKeysOfSecrets.entrySet().iterator();
        while (firstLevelIterator.hasNext()) {
            final Map.Entry<String, List<String>> finderNameToKeysOfSecrets = firstLevelIterator.next();
            final String finderName = finderNameToKeysOfSecrets.getKey();
            final List<String> xmlPaths = finderNameToKeysOfSecrets.getValue();
            xmlPaths.removeIf(xmlPath -> s3ConfigFetcher.isInWhiteList(finderName, xmlPath));
            if (xmlPaths.isEmpty()) {
                firstLevelIterator.remove();
            }
        }
        return mapOfTypeToKeysOfSecrets.isEmpty() ? Collections.emptyList() : Collections.singleton(emailText);
    }
*/
}
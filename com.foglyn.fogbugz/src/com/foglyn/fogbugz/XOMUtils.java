package com.foglyn.fogbugz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;

public class XOMUtils {
    /**
     * @return Value of selected XPath. If XPath doesn't match anything, empty
     *         string is returned. (Empty string may be returned also in cases
     *         when XPath matches empty node).
     */
    public static String xpathValueOf(Node node, String xpath) {
        return valueOfNodes(node.query(xpath));
    }

    private static String valueOfNodes(Nodes nodes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            sb.append(nodes.get(i).getValue());
        }
        
        return sb.toString();
    }

    /**
     * @return single Element, or <code>null</code>, if element is not found. If
     *         XPath returns more than one node, or node which is not element,
     *         {@link IllegalStateException} is thrown.
     */
    public static Element xpathElement(Node node, String xpath) {
        Nodes nodes = node.query(xpath);
        
        if (nodes.size() == 0) {
            return null;
        }
        
        if (nodes.size() > 1) {
            throw new IllegalStateException("Max one element expected at '" + xpath + "', found: " + nodes.size() + " nodes");
        }
        
        if (!(nodes.get(0) instanceof Element)) {
            throw new IllegalStateException("Element expected at '" + xpath + "', found: " + nodes.get(0).getClass());
        }
        
        return (Element) nodes.get(0);
    }

    /**
     * @return list of elements. If any of nodes found via XPath expression is
     *         not Element, {@link IllegalStateException} is thrown.
     */
    public static List<Element> xpathElements(Node node, String xpath) {
        Nodes nodes = node.query(xpath);
        if (nodes.size() == 0) {
            return Collections.emptyList();
        }
    
        List<Element> result = new ArrayList<Element>();
        for (int i = 0; i<nodes.size(); i++) {
            Node n = nodes.get(i);
            
            if (!(n instanceof Element)) {
                throw new IllegalStateException("Elements expected at '" + xpath + "', found: " + n.getClass());
            }
            
            Element e = (Element) n;
            
            result.add(e);
        }
        
        return result;
    }
}

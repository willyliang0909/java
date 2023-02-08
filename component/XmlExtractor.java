package internal.extractor;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import internal.tn.FormatException;

public class XmlExtractor implements IExtractor {

	DocumentBuilder builder;
	Document xmlDoc;

	// IBizDocType bizDocType;
	// Map<String, String> map = null;
	/*
	 * @Inject public XmlExtractor(IBizDocType bizDocType) { this.bizDocType =
	 * bizDocType; }
	 */
	
	public static void main(String[] args) throws FormatException {
		XmlExtractor e = new XmlExtractor();
		e.extract("");
	}

	@Override
	public Document initContent(String content) throws FormatException {
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(content));
			xmlDoc = builder.parse(is);
			return xmlDoc;
		} catch (Exception e) {
			throw new FormatException(e.getClass().getCanonicalName() + ": " + e.getMessage());
		}
	}

	@Override
	public String getRootTag() {
		Node rootNode = xmlDoc.getDocumentElement();
		String rootTag = rootNode.getNodeName().toString();
		return rootTag;
	}

	@Override
	public String extract(String path) throws FormatException {

		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			// XPathExpression expr = xpath.compile(path);
			// String name = expr.evaluate(xmlDoc);
			Node node = (Node) xpath.evaluate(path, xmlDoc, XPathConstants.NODE);
			if (node == null) {
				return null;
			} else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
				return node.getNodeValue();
			} else {
				return node.getTextContent();
			}

		} catch (Exception e) {
			throw new FormatException("Cannot parse with path: " + path);
		}
	}

	public Document getXmlDoc() {
		return xmlDoc;
	}

	public void setXmlDoc(Document xmlDoc) {
		this.xmlDoc = xmlDoc;
	}

}

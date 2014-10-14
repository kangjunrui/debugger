package debugger.shapes.model;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IFileEditorInput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ModelUtils {
	public static void load(File file, ShapesDiagram diagram) 
			throws IOException, CoreException, ParserConfigurationException, SAXException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(file);
		Element element = document.getDocumentElement();
		Map<String, Shape> shapemap = new HashMap<String, Shape>();
		NodeList nodes = element.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element node = (Element) nodes.item(i);
			if (node.getTagName() == "file") {
				diagram.addFile(node.getAttribute("name"),
						Integer.parseInt(node.getAttribute("id")));
				continue;
			} else if (node.getTagName() == "connection") {
				Connection connection = new Connection(shapemap.get(node
						.getAttribute("source")), shapemap.get(node
						.getAttribute("target")));
				connection.setLineStyle(Integer.parseInt(node
						.getAttribute("lineStyle")));
				String note="";
				NodeList content = node.getChildNodes();
				for (int j = 0; j < content.getLength(); j++) {
					if (content.item(j).getNodeType() == Node.CDATA_SECTION_NODE ){
						note=content.item(j).getNodeValue();
						break;
					}
				}
				connection.setLineNote(note);
				continue;
			} else {
				Shape shape = new Shape();
				if (node.getTagName() == "rectshape") {
					shape.setType(0);
				} else if (node.getTagName() == "shape") {
					shape.setType(1);
				}
				String name = node.getAttribute("name");
				shape.setName(name);
				String comment="";
				NodeList content = node.getChildNodes();
				for (int j = 0; j < content.getLength(); j++) {
					if (content.item(j).getNodeType() == Node.CDATA_SECTION_NODE ){
						comment=content.item(j).getNodeValue();
						break;
					}
				}
				shape.setComment(comment);
				shape.setLocation(new Point(Integer.parseInt(node
						.getAttribute("x")), Integer.parseInt(node
						.getAttribute("y"))));
				String icon = node.getAttribute("icon");
				if (icon!=null && !icon.isEmpty()){
					shape.setIcon(Integer.parseInt(icon));
				}
				String editor = node.getAttribute("editor");
				if (editor.length() != 0) {
					shape.resource = Integer.parseInt(editor);
					diagram.refEditor(shape.resource);
					String line=node.getAttribute("line");
					if (line.isEmpty()){
						try{
							shape.offset = Integer.parseInt(node.getAttribute("offset"));
							shape.length = Integer.parseInt(node.getAttribute("length"));
						}catch(Exception e){
							
						}
					}else{
						shape.line = Integer.parseInt(line);
					}
					String signature = node.getAttribute("signature");
					if (!signature.isEmpty()){
						shape.signature=signature;
					}
				} else {
					shape.resource = -1;
				}
				String state = node.getAttribute("state");
				shape.showfilename = (state != null && state.equals("1")) ? true
						: false;

				String colorstr = node.getAttribute("color");
				if (!colorstr.isEmpty()) {
					String[] values = colorstr.split(",");
					int red = Integer.parseInt(values[0]);
					int green = Integer.parseInt(values[1]);
					int blue = Integer.parseInt(values[2]);
					shape.setColor(new RGB(red, green, blue));
				}
				diagram.addChild(shape);
				shapemap.put(node.getAttribute("id"), shape);
			}
		}// end for i
	}

	public static void save(OutputStream os, ShapesDiagram diagram)
			throws IOException {
		Document doc = null;
		Element root = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.newDocument();
			root = doc.createElement("diagram");
			doc.appendChild(root);
		} catch (Exception e) {
			e.printStackTrace();
			return;// 如果出现异常，则不再往下执行
		}

		Element element;

		Map<String, Integer> fileToNumber = diagram.getFileToNumber();
		for (String filename : fileToNumber.keySet()) {
			element = doc.createElement("file");
			element.setAttribute("id", "" + fileToNumber.get(filename));
			element.setAttribute("name", "" + filename);
			root.appendChild(element);
		}

		Shape[] shapes = diagram.getChildren().toArray(new Shape[0]);
		int id = 0;
		Map<Shape, Integer> shapemap = new HashMap<Shape, Integer>();
		for (Shape shapemodel : shapes) {
			if (shapemodel.getType()==0) {
				element = doc.createElement("rectshape");
			} else {
				element = doc.createElement("shape");
			}
			element.setAttribute("id", "" + id);
			element.setAttribute("name", "" + shapemodel.getName());
			element.appendChild(doc.createCDATASection(shapemodel.getComment()));
			//element.setAttribute("name", "" + shapemodel.getName());
			Point location = shapemodel.getLocation();
			element.setAttribute("x", "" + location.x);
			element.setAttribute("y", "" + location.y);

			if (shapemodel.getIcon()!=-1){
				element.setAttribute("icon", "" + shapemodel.getIcon());
			}
			
			if (shapemodel.resource != -1) {
				element.setAttribute("editor", shapemodel.resource + "");
				if (shapemodel.line!=-1){
					element.setAttribute("line", shapemodel.line + "");
				}else{
					element.setAttribute("offset", shapemodel.offset + "");
					element.setAttribute("length", shapemodel.length + "");
				}
				element.setAttribute("signature", shapemodel.signature!=null?shapemodel.signature:"");
			}
			element.setAttribute("state", ""
					+ (shapemodel.showfilename ? 1 : 0));
			element.setAttribute("color", shapemodel.getColor().red + ","
					+ shapemodel.getColor().green + ","
					+ shapemodel.getColor().blue);

			root.appendChild(element);
			shapemap.put(shapemodel, id);
			id++;
		}
		for (Shape shapemodel : shapes) {
			List<Connection> conns = shapemodel.getSourceConnections();
			for (Connection conn : conns) {
				element = doc.createElement("connection");
				element.setAttribute("lineStyle", conn.getLineStyle() + "");
				element.appendChild(doc.createCDATASection(conn.getLineNote()));
				element.setAttribute("source", shapemap.get(conn.getSource())
						+ "");
				element.setAttribute("target", shapemap.get(conn.getTarget())
						+ "");
				root.appendChild(element);
			}
		}

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			return;
		}
		DOMSource source = new DOMSource(doc);
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");// 设置文档的换行与缩进
		StreamResult result = new StreamResult(os);
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			e.printStackTrace();
			return;
		}
	}

}

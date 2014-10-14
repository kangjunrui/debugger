package debugger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.TextUtilities;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraphLayout;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.gjt.jclasslib.bytecode.AbstractInstruction;
import org.gjt.jclasslib.bytecode.ImmediateShortInstruction;
import org.gjt.jclasslib.bytecode.Opcodes;
import org.gjt.jclasslib.io.ByteCodeReader;
import org.gjt.jclasslib.io.ClassFileReader;
import org.gjt.jclasslib.structures.AttributeInfo;
import org.gjt.jclasslib.structures.CPInfo;
import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.FieldInfo;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.MethodInfo;
import org.gjt.jclasslib.structures.attributes.CodeAttribute;
import org.gjt.jclasslib.structures.attributes.LineNumberTableAttribute;
import org.gjt.jclasslib.structures.attributes.LineNumberTableEntry;
import org.gjt.jclasslib.structures.attributes.SourceFileAttribute;
import org.gjt.jclasslib.structures.constants.ConstantFieldrefInfo;
import org.gjt.jclasslib.structures.constants.ConstantInterfaceMethodrefInfo;
import org.gjt.jclasslib.structures.constants.ConstantMethodrefInfo;
import org.gjt.jclasslib.structures.constants.ConstantNameAndTypeInfo;
import org.gjt.jclasslib.structures.constants.ConstantReference;
import org.gjt.jclasslib.structures.constants.ConstantUtf8Info;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import debugger.shapes.model.Connection;
import debugger.shapes.model.EllipticalShape;
import debugger.shapes.model.ModelUtils;
import debugger.shapes.model.RectangularShape;
import debugger.shapes.model.Shape;
import debugger.shapes.model.ShapesDiagram;

//output one class diagram for all specified +package, not -package
public class genDirShapes {	
	private static String prjname = "";
	private static File prjdir;
	private static Map<File, File> srcfolders;
	private static File globalbin;
	
	public static void main(String[] args)  throws ParserConfigurationException, SAXException, IOException {
		if (args.length<2){
			System.out.println("usage:");
			System.out.println("firstArg \"F:\\ket\\org.eclipse.gef\\bin\\\"; or any dir in prjdir");
			System.out.println("second and more Args \"org.eclipse.gef.ui.palette.FlyoutPaletteComposite\"; name of a source file");
			return;
		}
		
		prjdir = new File(args[0]);

		if (!prjdir.exists()){
			System.out.println("project dir not exist");
			return;
		}
		
		//relative path to absolute. or getParentFile is null.
		try {
			prjdir=prjdir.getCanonicalFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		if (!prjdir.isDirectory()){
			prjdir=prjdir.getParentFile();
		}
		
		File prjfile=null;
		while(prjdir!=null){
			prjfile=new File(prjdir, ".project");
			if (prjfile.exists()){
				break;
			}
			prjdir=prjdir.getParentFile();
		}
		
		if (prjdir==null){
			System.out.println("project dir is not in project");
			return;
		}
		
		File classpathfile=new File(prjdir, ".classpath");
		if (!classpathfile.exists()){
			System.out.println("no .classpath file.");
		}
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		//parse project file, get project name		
		Document document = builder.parse(prjfile);
		Element element = document.getDocumentElement();
		NodeList prjnamenode = element.getElementsByTagName("name");
		
		if (prjnamenode!=null && prjnamenode.getLength()>0){
			prjname = prjnamenode.item(0).getTextContent();
			System.out.println("Project name:"+prjname);
		}
		
		//parse classpath file, get src/bin pairs
		srcfolders = new HashMap<File, File>(); //src, bin
		globalbin = new File(prjdir, "bin");
		
		document = builder.parse(classpathfile);
		element = document.getDocumentElement();
		NodeList nodes = element.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
				continue;
			Element node = (Element) nodes.item(i);
			if (!node.getTagName().equals("classpathentry"))
				continue;
			
			String kindAttribute = node.getAttribute("kind");
			if (kindAttribute!=null){
				if (kindAttribute.equals("src")){
					String outputAttribute = node.getAttribute("output");
					String pathAttribute = node.getAttribute("path");
					if (!pathAttribute.isEmpty()){
						srcfolders.put(new File(prjdir, pathAttribute), (!outputAttribute.isEmpty())?new File(prjdir, outputAttribute):null);
					}
				}else if (kindAttribute.equals("output")){
					String pathAttribute = node.getAttribute("path");
					globalbin = new File(prjdir, pathAttribute);
				}
			}
		}
		
		Display display = new Display();
		font = new Font(display, "Arial", 16, SWT.NORMAL);
		methodColor = new RGB(0xC6, 0xA3, 0x00); // yellow
		fieldColor = new RGB(0xFF, 0x51, 0x51); // red
		newColor = new RGB(0x40, 0x80, 0x80); // green
		derivedColor = new RGB(70, 240, 240); // cyan

		/*int index=testFileName.lastIndexOf(".");
		if (index!=-1)
			className = testFileName.substring(0, index);
		className = className.replace("\\", "/");*/
		//processFile(className);

	}

	//sourceName is some java notation with package name
	//sourceFileNAme is only file name for .java file.
	public static String getSrcPath(String sourceName, String sourceFileName){
		String pkgname = null;
		int index=sourceName.lastIndexOf(".");
		if (index!=-1){
			pkgname = sourceName.substring(0, index);
		}else{
			pkgname=null;
		}
		
		pkgname.replace(".", File.separator);
		String srcPathName = pkgname + sourceFileName;
		File srcPath = null;//search every src dir for f+java extension
		for (File srcFolder : srcfolders.keySet()){
			srcPath = new File(srcFolder, srcPathName);
			if (srcPath.exists()){
				return srcPath.getPath();
			}
		}
		return null;
	}
	
	private static Font font;

	private static RGB methodColor;
	private static RGB fieldColor;
	private static RGB newColor;
	private static RGB derivedColor;

	private static class ClassEdge extends Edge {
		public ClassEdge(ClassNode source, ClassNode target) {
			super(source, target);
			
		}
		RGB edgeColor = null;
	}
	
	private static class ClassNode extends Node {
		static int idCount=0;
		public ClassNode(){
			this.id=idCount++;
		}
		String sourceFileName = null;
		int id=0;
	}
	
	static Map<String, ClassEdge> edges = new HashMap<String, ClassEdge>();
	static Map<String, ClassNode> nodes = new HashMap<String, ClassNode>();
	//sourceFileName is searched from src folders, 
	//which will add folder name in this function.
	private static void createEdge(DirectedGraph graph, String sourceName, String sourceFileName, String targetName, String targetFileName, RGB color)
			throws InvalidByteCodeException {
		ClassNode sourceNode=nodes.get(sourceName);
		ClassNode targetNode=nodes.get(targetName);
		if (sourceNode==null){
			sourceNode=new ClassNode();
			Dimension size = TextUtilities.INSTANCE.getStringExtents(sourceName,
					font);
			sourceNode.width = size.width;
			sourceNode.height = size.height;
			sourceNode.setPadding(new Insets(20, 20, 20, 0));
			nodes.put(sourceName, sourceNode);
			graph.nodes.add(sourceNode);
		}
		if (sourceNode.sourceFileName==null && sourceFileName!=null){
			sourceNode.sourceFileName=getSrcPath(sourceName, sourceFileName);
		}
		
		if (targetNode==null){
			targetNode=new ClassNode();
			Dimension size = TextUtilities.INSTANCE.getStringExtents(targetName,
					font);
			targetNode.width = size.width;
			targetNode.height = size.height;
			targetNode.setPadding(new Insets(20, 20, 20, 0));
			nodes.put(targetName, targetNode);
			graph.nodes.add(targetNode);
		}
		if (targetNode.sourceFileName==null && targetFileName!=null){
			targetNode.sourceFileName=getSrcPath(targetName, targetFileName);
		}
		
		String edgename = sourceNode.id+"-"+targetNode.id;
		ClassEdge e = edges.get(edgename);
		if (e!=null){
			return;
		}else{
			e = new ClassEdge(sourceNode, targetNode);
			e.edgeColor = color;
			graph.edges.add(e);
			edges.put(edgename, e);
		}
	}

	/*
	 push folders in stack

	for folder in folders
	for file in folder
	if is folder push folder
	else process file
	
	class relation is link color not node color
	
	each class only has a name, connection target/source/color
	 */
	private static boolean process(File[] folders, File outputFile) {
		DirectedGraph graph = new DirectedGraph();
		graph.setDirection(PositionConstants.EAST);
		
		Stack<File> folderstack = new Stack<File>();
		for (File folder : folders){
			folderstack.push(folder);
		}
		
		File folder;
		while ((folder=folderstack.pop())!=null){
			File[] files = folder.listFiles(new FileFilter(){

				@Override
				public boolean accept(File pathname) {
					if (pathname.isDirectory() || pathname.getName().endsWith(".class"))
						return true;
					else
						return false;
				}
				
			});
			for (File file:files){
				if (file.isDirectory()){
					folderstack.push(file);
				}else{
					processFile(graph, file);
				}
			}
		}

		new DirectedGraphLayout().visit(graph);

		ShapesDiagram diagram = new ShapesDiagram(null);

		for (String nodename : nodes.keySet()) {
			ClassNode node = nodes.get(nodename);
			if (node.sourceFileName != null)
				diagram.addFile(prjname + ":" + nodename + node.sourceFileName, 0);
			
			Shape shape = new RectangularShape();

			shape.setName(nodename);
			shape.setLocation(new Point(node.x, node.y));
			if (node.sourceFileName != null) {
				shape.resource = 0;
				diagram.refEditor(shape.resource);
				shape.offset = 0;
			} else {
				shape.resource = -1;
			}
			shape.showfilename = false;

			//shape.setColor();
			diagram.addChild(shape);

			node.data = shape;
		}

		for (int i = 0; i < graph.edges.size(); i++) {
			ClassEdge edge = (ClassEdge) graph.edges.get(i);
			ClassNode sourceNode = ((ClassNode) edge.source);
			ClassNode targetNode = ((ClassNode) edge.target);
			Connection connection = new Connection((Shape)sourceNode.data, 
					(Shape)targetNode.data);
			connection.setLineStyle(1);
			//connection.setLineColor(edge.edgeColor);
		}

		try {
			outputFile.createNewFile();
			FileOutputStream out = new FileOutputStream(outputFile);
			try {
				ModelUtils.save(out, diagram);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			out.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return true;
	}

	public static boolean processFile(DirectedGraph graph, File classfile){

		ClassFile classFile = null;
		try {
			classFile = ClassFileReader.readFromFile(classfile);
		} catch (InvalidByteCodeException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		if (classFile == null)
			return false;
		CPInfo[] cpinfos = classFile.getConstantPool();
		
		String className="";
		try {
			className = classFile.getThisClassName();
		} catch (InvalidByteCodeException e1) {
			e1.printStackTrace();
			return false;
		}
		
		String sourceFileName = null;
		AttributeInfo[] infos = classFile.getAttributes();
		for (int i = 0; i < infos.length; i++) {
			if (infos[i] instanceof SourceFileAttribute) {
				CPInfo cpinfo = cpinfos[((SourceFileAttribute) infos[i])
						.getSourcefileIndex()];
				sourceFileName = ((ConstantUtf8Info) cpinfo).getString();
				break;
			}
		}
		
		String superClassName;
		try {
			superClassName = classFile.getSuperClassName();
			createEdge(graph, 
					className, sourceFileName,
					superClassName, null,
					derivedColor
					);
		} catch (InvalidByteCodeException e1) {
			e1.printStackTrace();
			return false;
		}

		//int[] getInterfaces() -> cpinfos
		
		FieldInfo[] fields = classFile.getFields(); 
		for (int i = 0; i < fields.length; i++) { 
			try {
				String[] types=getTypeNames(fields[i].getDescriptor());
				for (String type:types){
					createEdge(graph, 
							className, sourceFileName,
							type, null,
							fieldColor
							);
				}
			} catch (InvalidByteCodeException e) {
				e.printStackTrace();
				return false;
			}
		}

		MethodInfo[] methods = classFile.getMethods();
		for (int i = 0; i < methods.length; i++) {
			try {
				String[] types=getTypeNames(methods[i].getDescriptor());
				for (String type:types){
					createEdge(graph, 
							className, sourceFileName,
							type, null,
							methodColor
							);
				}
			} catch (InvalidByteCodeException e) {
				e.printStackTrace();
				return false;
			}
		}

		for (int i = 0; i < methods.length; i++) {
			try {
				AttributeInfo[] methodAttributes = methods[i].getAttributes();
				for (AttributeInfo method_attribute : methodAttributes) {
					if (method_attribute instanceof CodeAttribute) {
						CodeAttribute codeAttribute = (CodeAttribute) method_attribute;

						byte[] code = codeAttribute.getCode();
						try {
							ArrayList instructions = ByteCodeReader
									.readByteCode(code);
							Iterator it = instructions.iterator();
							AbstractInstruction currentInstruction;
							while (it.hasNext()) {
								currentInstruction = (AbstractInstruction) it
										.next();
								if (currentInstruction instanceof ImmediateShortInstruction) {
									// System.out.print(currentInstruction.getOpcodeVerbose()
									// + ":");
									if (!(currentInstruction.getOpcode() == Opcodes.OPCODE_INVOKEINTERFACE
											|| currentInstruction.getOpcode() == Opcodes.OPCODE_INVOKESTATIC
											|| currentInstruction.getOpcode() == Opcodes.OPCODE_INVOKESPECIAL || currentInstruction
												.getOpcode() == Opcodes.OPCODE_INVOKEVIRTUAL))
										continue;
									CPInfo cpinfo = cpinfos[((ImmediateShortInstruction) currentInstruction)
											.getImmediateShort()];
									if (cpinfo instanceof ConstantInterfaceMethodrefInfo
											|| cpinfo instanceof ConstantMethodrefInfo) {
										ConstantReference info = ((ConstantReference) cpinfo);
										String methodClassName=info.getClassInfo().getName();
										ConstantNameAndTypeInfo methodinfo = info
												.getNameAndTypeInfo();
										String methodName = methodinfo
												.getName();
										if (!methodClassName.equals(className)
												&& methodName.equals("<init>")){
											createEdge(graph, 
													className, sourceFileName,
													methodClassName, null,
													newColor
													);
										}
									}
								} else {
									// System.out.println(currentInstruction.toString());
								}
							}
						} catch (IOException ex) {
							ex.printStackTrace();
						}

						continue;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	public static String[] getTypeNames(String descriptor){
		ArrayList<String> typenames = new ArrayList<String>();
		for (int i=0;i<descriptor.length();i++){
			char c = descriptor.charAt(i);
			switch(c){
			case '[': // one array dimension 
				
			case 'B': //signed byte 
			case 'C': //Unicode character code point in the Basic Multilingual Plane, encoded with UTF-16  
			case 'D': //double-precision floating-point value 
			case 'F': //single-precision floating-point value 
			case 'I': //integer 
			case 'J': //long integer 
			case 'S': //signed short 
			case 'Z': //true or false 
			
			case 'V': //void (only in method return type)
			case '(': //method argument list begin
			case ')': //method argument list end
				break;
				
			case 'L': //Package/Type; reference an instance of class ClassName 
				int end=descriptor.indexOf(";", i);
				typenames.add(descriptor.substring(i, end));
				i=end+1;
			}
		}
		return typenames.toArray(new String[0]);
	}
}


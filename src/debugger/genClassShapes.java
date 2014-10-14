package debugger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

//for each specified source file
//output one function graph in package 
public class genClassShapes {
	private static Font font;
	private static RGB staticColor;
	private static RGB privateColor;
	private static RGB abstractColor;
	private static RGB virtualColor;
	private static RGB derivedColor;

	private static class MemberNode extends Node {
		private static int memberIdCount = 0;
		int lineNumber = -1;
		int memberId = -1;
		String memberName = "";
		String methodDescriptor = "";
		RGB memberColor = null;
		public String displayName;
		
		public MemberNode getCopy(){
			MemberNode node = new MemberNode();
			node.lineNumber=lineNumber;
			node.memberId=memberId;
			node.memberName=memberName;
			node.methodDescriptor=methodDescriptor;
			node.memberColor=memberColor;
			
			return node;
		}
	}

	//if the class is in another, do not use dot as name string seperator
	private static HashMap<String, MemberNode> memberMap;
	
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
		String prjname = null;
		
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
		staticColor = new RGB(0x00, 0x5A, 0xB5); // blue
		privateColor = new RGB(0xC6, 0xA3, 0x00); // yellow
		abstractColor = new RGB(0xFF, 0x51, 0x51); // red
		virtualColor = new RGB(0x40, 0x80, 0x80); // green
		derivedColor = new RGB(70, 240, 240); // cyan
		
		HashSet<String> processedPrefix = new HashSet<String>();
		for (int i=1;i<args.length;i++){
			String prefix = null;
			String pkgname = null;
			int index=args[i].lastIndexOf(".");
			if (index!=-1){
				pkgname = args[i].substring(0, index);
				prefix = args[i].substring(index + 1);
			}else{
				prefix = args[i];
				pkgname=null;
			}
			if (processedPrefix.contains(prefix))
				continue;
			processedPrefix.add(prefix);
			
			String f = args[i];
			f.replace(".", File.separator);
			//get src dir and bin dir
			String srcFile = f + ".java";
			String binFile = f + ".class";
			File srcDir = null;//search every src dir for f+java extension
			File binDir = null;//search every bin dir for f+class extension
			for (File srcFolder : srcfolders.keySet()){
				srcDir = new File(srcFolder, srcFile);
				if (srcDir.exists()){
					File binFolder = srcfolders.get(srcFolder);
					if (binFolder!=null){
						binDir = new File(binFolder, binFile);
						if (!binDir.exists())
							binDir=null;
					}
				}else{
					srcDir = null;
				}
			}
			
			if (binDir == null){
				binDir = new File(globalbin, binFile);
				if (!binDir.exists())
					binDir=null;
			}
			
			if (binDir==null){
				System.out.println("class file for " + args[i] +" doesn't exist.");
				continue;
			}
			
			List<File> files = getGroup(binDir, prefix);
			
			if (!processFiles(binDir, files, pkgname, prefix, prjname, srcFile)){
				System.out.println("class read unsuccessful: " + args[i]);
			}
		}
		
		display.dispose();

	}
	
	private static List<File> getGroup(File bindir, String prefix){
		HashMap<String, List<File>> map=null;
		File[] files;
		final String filePrefix=prefix+".";
		final String innerPrefix=prefix+"$";
		System.out.println("getGroup:"+prefix);
		files = bindir.listFiles(new FileFilter(){
			@Override
			public boolean accept(File pathname) {
				String name = pathname.getName();
				if (name.endsWith(".class") && (name.startsWith(innerPrefix) || name.startsWith(filePrefix)))
					return true;
				else
					return false;
			}
			
		});
		
		ArrayList<File> namelist = new ArrayList<File>();
		for (File f:files){
			namelist.add(f);
		}

		return namelist;
	}
	
	private static MemberNode createMethodNode(MethodInfo info)
			throws InvalidByteCodeException {
		String methodName = info.getName();
		/*
		 * String name = info.getName(); if (name.equals("<init>")) { methodName
		 * = name; } else { methodName = name + info.getDescriptor(); }
		 */
		MemberNode n = new MemberNode();
		Dimension size = TextUtilities.INSTANCE.getStringExtents(methodName,font);
		n.width = size.width;
		n.height = size.height;
		n.setPadding(new Insets(20, 20, 20, 0));
		n.memberId = MemberNode.memberIdCount++;
		n.memberName = methodName;
		n.methodDescriptor = info.getDescriptor();
		if ((info.getAccessFlags() & MethodInfo.ACC_STATIC) != 0) {
			n.memberColor = staticColor;
		} else if ((info.getAccessFlags() & MethodInfo.ACC_PRIVATE) != 0) {
			n.memberColor = privateColor;
		} else if ((info.getAccessFlags() & MethodInfo.ACC_ABSTRACT) != 0) {
			n.memberColor = abstractColor;
		} else {
			n.memberColor = virtualColor;
		}
		
		AttributeInfo[] methodAttributes = info.getAttributes();
		for (AttributeInfo method_attribute : methodAttributes) {
			if (method_attribute instanceof CodeAttribute) {
				CodeAttribute codeAttribute = (CodeAttribute) method_attribute;
				int lineNumber = -1;
				AttributeInfo[] codeAttributes = codeAttribute
						.getAttributes();
				for (AttributeInfo code_attribute : codeAttributes) {
					if (code_attribute instanceof LineNumberTableAttribute) {
						LineNumberTableAttribute lineAttribute = (LineNumberTableAttribute) code_attribute;
						LineNumberTableEntry[] lines = lineAttribute.getLineNumberTable();
						for (LineNumberTableEntry line : lines) {
							if (line.getStartPc() == 0) {
								lineNumber = line.getLineNumber();
								break;
							}
						}
					}
				}
				if (lineNumber != -1) {
					n.lineNumber = lineNumber - 1;//lineNumber is in [1, ..]
				}
			}
		}
		return n;
	}

	private static MemberNode createFieldNode(FieldInfo info)
			throws InvalidByteCodeException {
		String fieldName = info.getName();
		MemberNode n = new MemberNode();
		Dimension size = TextUtilities.INSTANCE.getStringExtents(fieldName,font);
		n.width = size.width;
		n.height = size.height;
		n.setPadding(new Insets(20, 20, 20, 0));
		n.memberId = MemberNode.memberIdCount++;
		n.memberName = fieldName;
		n.methodDescriptor = null;
		if ((info.getAccessFlags() & MethodInfo.ACC_STATIC) != 0) {
			n.memberColor = staticColor;
		} else if ((info.getAccessFlags() & MethodInfo.ACC_PRIVATE) != 0) {
			n.memberColor = privateColor;
		} else if ((info.getAccessFlags() & MethodInfo.ACC_ABSTRACT) != 0) {
			n.memberColor = abstractColor;
		} else {
			n.memberColor = virtualColor;
		}
		return n;
	}
	
	static HashSet<String> knownClasses = new HashSet<String>();
	//add members in class to memberMap
	private static void createClassMembers(String className){
		//use .classpath info to find this class
		//call createMethodNode or create field which do not has parameter signature.
		//use srcfolders and globalbin
		
		if (knownClasses.contains(className)) return;
		
		String f = className;
		f.replace(".", File.separator);
		//get src dir and bin dir
		String srcFile = f + ".java";
		String binFile = f + ".class";
		File srcDir = null;//search every src dir for f+java extension
		File binDir = null;//search every bin dir for f+class extension
		for (File srcFolder : srcfolders.keySet()){
			srcDir = new File(srcFolder, srcFile);
			if (srcDir.exists()){
				File binFolder = srcfolders.get(srcFolder);
				if (binFolder!=null){
					binDir = new File(binFolder, binFile);
					if (!binDir.exists())
						binDir=null;
				}
			}else{
				srcDir = null;
			}
		}
		
		if (binDir == null){
			binDir = new File(globalbin, binFile);
			if (!binDir.exists())
				binDir=null;
		}
		
		if (binDir==null){
			System.out.println("class file for " + className +" doesn't exist.");
			return;
		}
		
		String prefix = null;
		String pkgname = null;
		int index=className.lastIndexOf(".");
		if (index!=-1){
			pkgname = className.substring(0, index);
			prefix = className.substring(index + 1);
		}else{
			prefix = className;
			pkgname=null;
		}
		
		File file = new File(binDir, binFile);
		
		ClassFile classFile = null;
		try {
			classFile = ClassFileReader.readFromFile(file);
		} catch (InvalidByteCodeException e) {
			e.printStackTrace();
			return ;
		} catch (IOException e) {
			e.printStackTrace();
			return ;
		}

		if (classFile == null)
			return ;

		CPInfo[] cpinfos = classFile.getConstantPool();
		
		FieldInfo[] fields = classFile.getFields(); 
		for (int i = 0; i < fields.length; i++) { 
			try { 
				MemberNode n = createFieldNode(fields[i]);
				memberMap.put(pkgname+"."+prefix + "."+ fields[i].getName(), n);
			} catch (Exception e) { 
				e.printStackTrace(); 
			} 
		} 

		MethodInfo[] methods = classFile.getMethods();
		for (int i = 0; i < methods.length; i++) {
			try {
				MemberNode n = createMethodNode(methods[i]);
				memberMap.put(pkgname+"."+prefix + "."+ methods[i].getName() + methods[i].getDescriptor(), n);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		knownClasses.add(className);	
	}
	
	//editor id = 0: prjname:srcDir/prefix.java
	//use binDir and classNames to read class files.
	private static boolean processFiles(File binDir, List<File> files, String pkgname, String prefix, String prjname, String srcFile) {
		DirectedGraph graph = new DirectedGraph();
		graph.setDirection(PositionConstants.EAST);
		
		for (File file : files){
		ClassFile classFile = null;
		try {
			classFile = ClassFileReader.readFromFile(file);
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
		
		FieldInfo[] fields = classFile.getFields(); 
		for (int i = 0; i < fields.length; i++) { 
			try { 
				MemberNode n = createFieldNode(fields[i]);
				memberMap.put(pkgname+"."+prefix + "."+ fields[i].getName(), n);
				graph.nodes.add(n); 
			} catch (Exception e) { 
				e.printStackTrace(); 
			} 
		} 

		MethodInfo[] methods = classFile.getMethods();
		for (int i = 0; i < methods.length; i++) {
			try {
				MemberNode n = createMethodNode(methods[i]);
				memberMap.put(pkgname+"."+prefix + "."+ methods[i].getName() + methods[i].getDescriptor(), n);
				graph.nodes.add(n);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String fileName=file.getName();
		knownClasses.add(pkgname+"."+fileName.substring(0, fileName.length()-5));
		
		for (int i = 0; i < methods.length; i++) {
			try {
				String sourceName = pkgname+"."+prefix + "."+ methods[i].getName() + methods[i].getDescriptor();
				MemberNode sourceNode = memberMap.get(sourceName);

				AttributeInfo[] methodAttributes = methods[i].getAttributes();
				for (AttributeInfo method_attribute : methodAttributes) {
					if (method_attribute instanceof CodeAttribute) {
						CodeAttribute codeAttribute = (CodeAttribute) method_attribute;

						byte[] code = codeAttribute.getCode();
						try {
							ArrayList instructions = ByteCodeReader.readByteCode(code);
							Iterator it = instructions.iterator();
							AbstractInstruction currentInstruction;
							while (it.hasNext()) {
								currentInstruction = (AbstractInstruction) it.next();
								if (currentInstruction instanceof ImmediateShortInstruction) {
									CPInfo cpinfo = cpinfos[((ImmediateShortInstruction) currentInstruction)
															.getImmediateShort()];
									
									if (cpinfo instanceof ConstantInterfaceMethodrefInfo
											|| cpinfo instanceof ConstantMethodrefInfo) {
										if (!(currentInstruction.getOpcode() == Opcodes.OPCODE_INVOKEINTERFACE
												|| currentInstruction.getOpcode() == Opcodes.OPCODE_INVOKESTATIC
												|| currentInstruction.getOpcode() == Opcodes.OPCODE_INVOKESPECIAL 
												|| currentInstruction.getOpcode() == Opcodes.OPCODE_INVOKEVIRTUAL))
											continue;
										
										ConstantReference info = ((ConstantReference) cpinfo);
										String className = info.getClassInfo().getName();
										if (!className.startsWith(pkgname+"."))
											continue;
										
										boolean isThisClass = className.equals(pkgname+"."+prefix);
										
										if (!isThisClass) createClassMembers(className);
										ConstantNameAndTypeInfo methodinfo = info.getNameAndTypeInfo();
										String targetName = className + "."+ methodinfo.getName() + methodinfo.getDescriptor();
										MemberNode targetNode = memberMap.get(targetName);
										if (!isThisClass){
											MemberNode n = targetNode.getCopy();
											graph.nodes.add(n);
											
											if (className.startsWith(pkgname+"."+prefix+"$")){
												//inner class, with $classname. prefix
												n.displayName=className.substring(pkgname.length()+prefix.length()+1) +"." + n.memberName;
											}else{
												//same package, with subpackage name and classname. prefix
												n.displayName=className.substring(pkgname.length()+1) +"." + n.memberName;
											}
											
										}else{
											//same class, no prefix
											MemberNode n = targetNode;
											n.displayName=n.memberName;
										}
										
										if (targetNode != null && !targetName.equals(sourceName)) {
											Edge e = new Edge(null, sourceNode, targetNode);
											e.weight = 2;
											graph.edges.add(e);
										}
									} else if (cpinfo instanceof ConstantFieldrefInfo){
										if (!(currentInstruction.getOpcode() == Opcodes.OPCODE_PUTSTATIC
												|| currentInstruction.getOpcode() == Opcodes.OPCODE_PUTFIELD))
											continue;
										
										ConstantFieldrefInfo info =	((ConstantFieldrefInfo) cpinfo); 
										String className = info.getClassInfo().getName();
										if(!className.startsWith(pkgname+".")) 
											continue; 
										
										boolean isThisClass = className.equals(pkgname+"."+prefix);
										
										if (!isThisClass) createClassMembers(className);
										ConstantNameAndTypeInfo fieldinfo = info.getNameAndTypeInfo();
										String nodeName = className + "."+ fieldinfo.getName()+fieldinfo.getDescriptor();
									    MemberNode node = memberMap.get(nodeName); 
									    if (!isThisClass){
											MemberNode n = node.getCopy();
											graph.nodes.add(n);
											
											if (className.startsWith(pkgname+"."+prefix+"$")){
												//inner class, with $classname. prefix
												n.displayName=className.substring(pkgname.length()+prefix.length()+1) +"." + n.memberName;
											}else{
												//same package, with subpackage name and classname. prefix
												n.displayName=className.substring(pkgname.length()+1) +"." + n.memberName;
											}
											
										}else{
											//same class, no prefix
											MemberNode n = node;
											n.displayName=n.memberName;
										}
										
									    
								    	Edge e = new Edge(null, sourceNode, node);
								    	e.weight = 2; 
								    	graph.edges.add(e); 
									}
									 
								} else {
									// System.out.println(currentInstruction.toString());
									// System.out.print(currentInstruction.getOpcodeVerbose() + ":");
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
		}
		
		new DirectedGraphLayout().visit(graph);

		File outputFile = new File(prjdir, pkgname+"."+prefix+".shapes");
		// for compatibility, use the same format
		ShapesDiagram diagram = new ShapesDiagram(null);
		
		// graph.nodes, graph.edges
		// n.x, n.y;
		
		String sourceFileName = prjname+":"+srcFile;
		diagram.addFile(sourceFileName, 0);

		for (int i = 0; i < graph.nodes.size(); i++) {
			MemberNode node = (MemberNode) graph.nodes.get(i);

			Shape shape = new RectangularShape();

			shape.setName(node.displayName);
			shape.setLocation(new Point(node.x, node.y));
			if (sourceFileName != null) {
				shape.resource = 0;
				diagram.refEditor(shape.resource);
				shape.offset = node.lineNumber;
			} else {
				shape.resource = -1;
			}
			shape.showfilename = false;

			shape.setColor(node.memberColor);
			diagram.addChild(shape);

			node.data = shape;
		}

		for (int i = 0; i < graph.edges.size(); i++) {
			Edge edge = (Edge) graph.edges.get(i);
			MemberNode sourceNode = ((MemberNode) edge.source);
			String sourceName = sourceNode.memberName
					+ (sourceNode.methodDescriptor!=null?sourceNode.methodDescriptor:"");
			MemberNode targetNode = ((MemberNode) edge.target);
			String targetName = targetNode.memberName
					+ (targetNode.methodDescriptor!=null?targetNode.methodDescriptor:"");;
			Connection connection = new Connection(
					((Shape) memberMap.get(sourceName).data),
					((Shape) memberMap.get(targetName).data));
			connection.setLineStyle(1);
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

}

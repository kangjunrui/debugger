package debugger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.internal.resources.refresh.win32.Win32RefreshProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.refresh.IRefreshMonitor;
import org.eclipse.core.resources.refresh.IRefreshResult;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

public class SyncCommandHandler implements IHandler, IResourceChangeListener {
	@SuppressWarnings("restriction")
	public SyncCommandHandler(){
		ResourcesPlugin.getWorkspace().addResourceChangeListener (
				this, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_REFRESH);
		System.out.println("start sync");
	}
	
	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {

	}
	
	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {

	}
	
	@Override
	public void dispose() {

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

	File root = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
	Map<String, File> syncPrjs = new HashMap<String, File>();
	Set<String> otherPrjs = new HashSet<String>();
	public File getProjectBackup(String prjname){
		//some project may be later deleted.
		//TODO
		
		File backup=syncPrjs.get(prjname);
		if (backup==null){
			if (otherPrjs.contains(prjname)){
				return null;
			}else{
				backup=new File(root, prjname);
				backup=new File(backup, ".backup");
				if (backup.exists()){
					try {
						BufferedReader reader = new BufferedReader(new FileReader(backup));
						String line = reader.readLine();
						backup=new File(line);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (backup.exists()){
						syncPrjs.put(prjname, backup);
						return backup;
					}else{
						otherPrjs.add(prjname);
						return null;
					}
				}else{
					otherPrjs.add(prjname);
					return null;
				}
			}
		}else{
			return backup;
		}
	}
	
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		//System.out.println(event.getType());
		Stack<IResourceDelta[]> stack = new Stack<IResourceDelta[]>();
		IResourceDelta[] children = event.getDelta().getAffectedChildren();
		//breadth-first traverse
		stack.push(children);

		while(!stack.empty()){
			children=stack.pop();
			for (IResourceDelta child:children){
				IResourceDelta[] children2=child.getAffectedChildren();
				if (children2==null || children2.length==0){
					sync(child);
				}else{
					stack.push(children2);
				}	
			}
		}
	}

	public void sync(IResourceDelta child){
		String path=child.getFullPath().toString();
		if (path.endsWith(".java") || path.endsWith(".xml")){
			int start=0;
			int end = path.indexOf("/");
			if (end==0){
				start=1;
				end = path.indexOf("/", 1);
			}
			String prjname = path.substring(start, end);
			String filename = path.substring(end+1);
			
			switch(child.getKind()){
			case IResourceDelta.CHANGED:
			case IResourceDelta.ADDED:{
				File backup = getProjectBackup(prjname);
				if (backup!=null){
					File source = new File(root, path);
					File target = new File(backup, filename);
					File targetdir=null;
					if (!source.isDirectory()){
						targetdir=target.getParentFile();
					}else{
						targetdir=target;
					}
					
					if (!targetdir.exists())
						targetdir.mkdirs();
					if (targetdir!=target && source.length()!=target.length())
						fileChannelCopy(source, target);
				}
			}break;
				
			case IResourceDelta.REMOVED:{
				File backup = getProjectBackup(prjname);
				if (backup!=null){
					new File(backup, filename).delete();
				}
			}break;
			}
		}
	}
    public void fileChannelCopy(File s, File t) {
    	//System.out.println("backup file:" + s.toString());
        FileInputStream fi = null;
        FileOutputStream fo = null;
        FileChannel in = null;
        FileChannel out = null;
        try {
            fi = new FileInputStream(s);
            fo = new FileOutputStream(t);
            in = fi.getChannel();//得到对应的文件通道
            out = fo.getChannel();//得到对应的文件通道
            in.transferTo(0, in.size(), out);//连接两个通道，并且从in通道读取，然后写入out通道
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fi.close();
                in.close();
                fo.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

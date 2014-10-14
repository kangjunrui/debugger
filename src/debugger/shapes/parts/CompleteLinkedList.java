package debugger.shapes.parts;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class CompleteLinkedList<T> extends SimpleLinkedList<T>{
	private static class Group{
		Object name;
	}
	
	protected static class GroupNode<T> extends ListNode<T>{
		ListNode prev;
		ListNode next;
		T obj;
		Group group;
	}
	
	SimpleLinkedList<Group> groups = new SimpleLinkedList<Group>();
	//name to group
	Map<Object, Group> groupnames = new HashMap<Object, Group>();
	//group to GroupNode
	Map<Object, GroupNode<T>> groupfirsts = new HashMap<Object, GroupNode<T>>();
	Map<Object, GroupNode<T>> grouplasts = new HashMap<Object, GroupNode<T>>();
	
	public void addLast(Object grouporname, T obj){
		Group group = groupnames.get(grouporname);
		GroupNode<T> groupfirst, grouplast;
		if (group==null){
			groupfirst = groupfirsts.get(grouporname);
		}else{
			groupfirst = groupfirsts.get(group);
		}

		if (groupfirst==null){
			if (grouporname!=null){
				groupfirst=grouplast=new GroupNode();
				groupfirst.obj=obj;
				group = new Group();
				groups.addLast(group);
				groupnames.put(grouporname, group);
				groupfirst.group=group;
				
				if (first==null){
					first=last=groupfirst;
				}else{
					last.next=groupfirst;
					ListNode t=last;
					last=last.next;
					last.prev=t;
					last.obj=obj;
					map.put(obj, last);
				}
				
				groupfirsts.put(group, groupfirst);
				grouplasts.put(group, groupfirst);
			}else{
				
			}
		}else{
			last.next=new ListNode();
			ListNode t=last;
			last=last.next;
			last.prev=t;
			last.obj=obj;
			map.put(obj, last);
		}
	}
	
	public void addFirst(T obj){
		if (first==null){
			first=last=new ListNode();
			first.obj=obj;
		}else{
			first.prev=new ListNode();
			ListNode t=first;
			first=first.prev;
			first.next=t;
			first.obj=obj;
			map.put(obj, first);
		}
	}
	
	public boolean addBefore(T index, T obj){
		ListNode indexnode = map.get(index);
		if (indexnode==null){
			return false;
		}
		
		ListNode t=new ListNode();
		t.next=indexnode;
		t.prev=indexnode.prev;
		indexnode.prev=t;
		if (t.prev!=null)
			t.prev.next=t;
		t.obj=obj;
		return true;
	}
	
	public boolean addAfter(T index, T obj){
		ListNode indexnode = map.get(index);
		if (indexnode==null){
			return false;
		}
		
		ListNode t=new ListNode();
		t.next=indexnode.next;
		t.prev=indexnode;
		indexnode.next=t;
		if (t.next!=null)
			t.next.prev=t;
		t.obj=obj;
		return true;
	}
	
	public T getFirst(){
		if (first==null)
			return null;
		else
			return first.obj;
	}
	
	public T getLast(){
		if (last==null)
			return null;
		else
			return last.obj;
	}
	
	public Iterator<T> getIterator(){
		return new Iterator<T>(){
			ListNode<T> current = first;
			@Override
			public boolean hasNext() {
				return current!=null;
			}

			@Override
			public T next() {
				if (current==null)
					return null;
				T obj = current.obj;
				current=current.next;
				return obj;
			}

			@Override
			public void remove() {
				
			}
			
		};
	}
	
	public Iterator<T> getInverseIterator(){
		return new Iterator<T>(){
			ListNode<T> current = last;
			@Override
			public boolean hasNext() {
				return current!=null;
			}

			@Override
			public T next() {
				if (current==null)
					return null;
				T obj = current.obj;
				current=current.prev;
				return obj;
			}

			@Override
			public void remove() {
				
			}
			
		};
	}
}

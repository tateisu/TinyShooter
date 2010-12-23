package jp.juggler.TinyShooter;

import java.util.ArrayList;

public class ListWithPool<T extends ListWithPool.Item>{
	public static class Item{
		boolean bActive;
	}
	public static interface ItemFactory<T>{
		public T create();
	}
	ItemFactory<T> factory;
	ArrayList<T> pool;
	ArrayList<T> active;
	int active_count =0;

	public ListWithPool(int initial_capacity,ItemFactory<T> factory){
		this.factory = factory;
		this.pool = new ArrayList<T>(initial_capacity);
		this.active = new ArrayList<T>(initial_capacity);
		while(initial_capacity-- > 0 ){
			this.pool.add(factory.create());
		}
	}

	T obtain(){
		T item;
		int n = pool.size(); 
		if( n > 0 ){
			item = pool.remove(n-1);
		}else{
			item = factory.create();
		}
		item.bActive = true;
		active.add(item);
		++active_count;
		return item;
	}
	void unmark(T item){
		item.bActive = false;
		--active_count;
	}
	
	void sweep(){
		int nActive = 0;
		for(int i=0,e=active.size();i<e;++i){
			T item = active.get(i);
			if(item.bActive){
				if(i!=nActive) active.set(nActive,item);
				++nActive;
			}else{
				pool.add(item);
			}
		}
		int n= active.size();
		while(n>nActive) active.remove(--n);
	}
	void clear(){
		int n= active.size();
		while(n>0) pool.add(active.remove(--n));
		active_count = 0;
	}

	int size(){
		return active.size();
	}
	
	T get(int idx){
		T item = active.get(idx);
		return item.bActive ? item : null;
	}
	
}

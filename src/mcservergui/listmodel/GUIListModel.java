/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mcservergui.listmodel;

import java.util.*;


/**
 *
 * @author dumptruckman
 */
public class GUIListModel<T> extends javax.swing.AbstractListModel {

    TreeSet<T> model;

    public GUIListModel() {
        model = new TreeSet<T>();
    }

    public int getSize() {
        return model.size();
    }

    public T getElementAt(int index) {
        T[] a = null;
        return model.toArray(a)[index];
    }

    public void setModel(TreeSet<T> model) {
        this.model = model;
    }

    public TreeSet<T> getModel() {
        return model;
    }

    public void add(T element) {
        if (model.add(element)) {
            fireContentsChanged(this, 0, getSize());
        }
    }
    
    public void addAll(T elements[]) {
        Collection<T> c = Arrays.asList(elements);
        model.addAll(c);
        fireContentsChanged(this, 0, getSize());
    }

    public void clear() {
        model.clear();
        fireContentsChanged(this, 0, getSize());
    }

    public boolean contains(T element) {
        return model.contains(element);
    }

    public Object firstElement() {
        return model.first();
    }

    public Iterator iterator() {
        return model.iterator();
    }

    public Object lastElement() {
        return model.last();
    }

    public boolean removeElement(T element) {
        boolean removed = model.remove(element);
        if (removed) {
            fireContentsChanged(this, 0, getSize());
        }
        return removed;
    }

    public java.util.List<T> toList() {
        java.util.List<T> list = new java.util.ArrayList<T>();
        java.util.Iterator<T> it = this.iterator();
        while (it.hasNext()) {
            list.add((T)it.next());
        }
        return list;
    }
}

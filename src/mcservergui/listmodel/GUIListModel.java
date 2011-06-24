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
public class GUIListModel extends javax.swing.AbstractListModel {

    SortedSet model;

    public GUIListModel() {
        model = new TreeSet();
    }

    @Override public int getSize() {
        return model.size();
    }

    @Override public Object getElementAt(int index) {
        return model.toArray()[index];
    }

    public void add(Object element) {
        if (model.add(element)) {
            fireContentsChanged(this, 0, getSize());
        }
    }

    public void addAll(Object elements[]) {
        Collection c = Arrays.asList(elements);
        model.addAll(c);
        fireContentsChanged(this, 0, getSize());
    }

    public void clear() {
        model.clear();
        fireContentsChanged(this, 0, getSize());
    }

    public boolean contains(Object element) {
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

    public boolean removeElement(Object element) {
        boolean removed = model.remove(element);
        if (removed) {
            fireContentsChanged(this, 0, getSize());
        }
        return removed;
    }

    public java.util.List getList() {
        java.util.List list = new java.util.ArrayList();
        java.util.Iterator it = this.iterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }
}

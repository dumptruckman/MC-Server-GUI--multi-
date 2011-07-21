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

    //TreeSet<T> model;
    List<T> model;

    public GUIListModel() {
        //model = new TreeSet<T>();
        model = new ArrayList<T>();
    }

    public int getSize() {
        return model.size();
    }

    public T getElementAt(int index) {
        return this.toList().get(index);
    }

    public void setModel(List<T> model/*TreeSet<T> model*/) {
        this.model = model;
    }

    public List<T> getModel() {
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

    public T firstElement() throws NoSuchElementException {
        if (model.isEmpty()) {
            throw new NoSuchElementException();
        }
        return model.get(0);
    }

    public Iterator<T> iterator() {
        return model.iterator();
    }

    public T lastElement() throws NoSuchElementException {
        if (model.isEmpty()) {
            throw new NoSuchElementException();
        }
        return model.get(model.size() - 1);
    }

    public boolean removeElement(T element) {
        boolean removed = model.remove(element);
        if (removed) {
            fireContentsChanged(this, 0, getSize());
        }
        return removed;
    }

    public java.util.List<T> toList() {
        return model;
    }
}

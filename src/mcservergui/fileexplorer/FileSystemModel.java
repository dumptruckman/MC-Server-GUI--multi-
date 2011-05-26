package mcservergui.fileexplorer;

import java.io.*;
import javax.swing.event.TreeModelListener;

public class FileSystemModel implements Serializable, javax.swing.tree.TreeModel {

    String root;

    public FileSystemModel() {
        this( System.getProperty( "user.home" ) );
    }

    public FileSystemModel( String startPath ) {
        root = startPath;
    }

    public Object getRoot() {
        return new File( root );
    }
   

    public Object getChild( Object parent, int index ) {
        File directory = (File)parent;
        return new File ( directory, directory.list()[index] );
    }

    public int getChildCount( Object parent ) {
        File fileSysEntity = (File)parent;
        if ( fileSysEntity.isDirectory() ) {
            String[] children = fileSysEntity.list();
            return children.length;
        }
        else {
            return 0;
        }
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public boolean isLeaf( Object node ) {
        return ((File)node).isFile();
    }

    public void valueForPathChanged( javax.swing.tree.TreePath path, Object newValue ) {
    }

    public int getIndexOfChild( Object parent, Object child ) {
        System.out.println(child.toString());
        File directory = (File)parent;
        File fileSysEntity = (File)child;
        String[] children = directory.list();
        int result = -1;

        for ( int i = 0; i < children.length; ++i ) {
            if ( fileSysEntity.getName().equals( children[i] ) ) {
                result = i;
                break;
            }
        }

        return result;
    }

    @Override public void addTreeModelListener( TreeModelListener listener ) {}

    @Override public void removeTreeModelListener( TreeModelListener listener ) {}
}



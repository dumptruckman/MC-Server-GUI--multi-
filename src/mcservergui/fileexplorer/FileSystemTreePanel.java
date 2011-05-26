package mcservergui.fileexplorer;

import java.io.*;

public class FileSystemTreePanel extends javax.swing.JPanel {
    private javax.swing.JTree tree;

    public FileSystemTreePanel() {
        this( new FileSystemModel() );
    }

    public FileSystemTreePanel( String startPath ) {
        this( new FileSystemModel( startPath ) );
    }

    public FileSystemTreePanel( FileSystemModel model ) {
        tree = new javax.swing.JTree( model ) {
            @Override
            public String convertValueToText(Object value, boolean selected,
                                             boolean expanded, boolean leaf, int row,
                                             boolean hasFocus) {
                return ((File)value).getName();
            }
        };

        //tree.setLargeModel( true );        
        tree.setRootVisible( false );
        tree.setShowsRootHandles( true );
        tree.putClientProperty( "JTree.lineStyle", "Angled" );

        setLayout( new java.awt.BorderLayout() );
        add( tree, java.awt.BorderLayout.CENTER );
    }

    public javax.swing.JTree getTree() {
       return tree;
    }
}



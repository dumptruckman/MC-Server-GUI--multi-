package fileexplorer;

public class DirectoryRenderer extends javax.swing.table.DefaultTableCellRenderer {

    @Override
    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        if ( value != null && value instanceof javax.swing.Icon ) {
           super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
           setIcon( (javax.swing.Icon)value );
           setText( "" );
           return this;
        }
        else {
           setIcon( null );
        }

        return super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
    }
}


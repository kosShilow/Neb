package ru.kos.neb_viewer;

import java.applet.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.tree.*;
//import static ru.kos.neb.neb_viewer.GetNodePropertiesForm.interfInformation;

/**
 *
 * @author kos
 */
@SuppressWarnings("serial")
public class FindTreeCellRenderer extends DefaultTreeCellRenderer
{
    Icon icon_leaf=new ImageIcon(getClass().getResource("/images/vlan.png"));
    Icon open_folder=new ImageIcon(getClass().getResource("/images/FolderOpen.png"));
    Icon close_folder=new ImageIcon(getClass().getResource("/images/FolderClose.png"));
    
    /** Creates a new instance of MyTreeCellRenderer */
    public FindTreeCellRenderer()
    {
    }
    @Override
    public Component getTreeCellRendererComponent(JTree tree,Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean hasFocus)
    {
        Component res = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
//        if(node != null && node.getParent() != null) System.out.println("value="+value.toString()+"\tparent="+node.getParent().toString());
        int level = node.getLevel();

        if (level == 1) {
            setToolTipText("");
            if(expanded) setIcon(open_folder);
            else setIcon(close_folder);
        }
        
        if (level == 2) {
            String tooltip = value.toString();
//            String str = tooltip.split("\\(")[0];
//            if(Main.info_from_map != null && Main.info_from_map.size() == 3) {
//                Map<String, String[]> extends_info = (Map<String, String[]>)Main.info_from_map.get(2);
//                String[] ext = extends_info.get(str);
//                if(ext != null) {
//                    tooltip="<html>"+tooltip;
//                    for(String item : ext) {
//                        if(!item.equals("")) tooltip=tooltip+"<br>"+item;
//                    }
//                    tooltip=tooltip+"</html>";
//                }
//            }
            
            setToolTipText(tooltip);            
            setIcon(icon_leaf);
        }        
        
        return this;
    }
    
}

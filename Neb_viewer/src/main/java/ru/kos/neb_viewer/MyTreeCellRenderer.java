package ru.kos.neb_viewer;

import java.applet.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.tree.*;
import static ru.kos.neb_viewer.GetNodePropertiesForm.item_icon;
//import static ru.kos.neb.neb_viewer.GetNodePropertiesForm.interfInformation;

/**
 *
 * @author kos
 */
@SuppressWarnings("serial")
public class MyTreeCellRenderer extends DefaultTreeCellRenderer
{
    Icon iconNode=new ImageIcon(getClass().getResource("/images/node.png"));
    Icon iconGeneral=new ImageIcon(getClass().getResource("/images/general.png"));
    Icon iconInterfaces=new ImageIcon(getClass().getResource("/images/interfaces.png"));
    Icon iconInterf_up=new ImageIcon(getClass().getResource("/images/interf_up.png"));
    Icon iconInterf_down=new ImageIcon(getClass().getResource("/images/interf_down.png"));
    Icon iconInterf_error=new ImageIcon(getClass().getResource("/images/interf_error.png"));
    Icon iconRoutre=new ImageIcon(getClass().getResource("/images/router.png"));
    Icon iconVlans=new ImageIcon(getClass().getResource("/images/vlans.png"));
    Icon iconVlan=new ImageIcon(getClass().getResource("/images/vlan.png"));
    Icon iconClient=new ImageIcon(getClass().getResource("/images/client.png"));
    Icon iconClientOff=new ImageIcon(getClass().getResource("/images/client_off.png"));
    
    /** Creates a new instance of MyTreeCellRenderer */
    public MyTreeCellRenderer()
    {
    }
    @Override
    public Component getTreeCellRendererComponent(JTree tree,Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean hasFocus)
    {
        Component res = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        String name = res.toString();

//        System.out.println("value="+value.toString());
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
//        if(node != null && node.getParent() != null) System.out.println("value="+value.toString()+"\tparent="+node.getParent().toString());
        int level = node.getLevel();
//        System.out.println("level="+level);
        if (level == 0 && value.toString().equals(GetNodePropertiesForm.ip))
        {
            setIcon(iconNode);
        }
        if (level == 1 && value.toString().equals("General"))
        {
            setIcon(iconGeneral);
        }
        if (level == 1 && value.toString().equals("Interfaces"))
        {
            setIcon(iconInterfaces);
        }
        
//        if (level == 2 && GetNodePropertiesForm.interfInformation.get(value.toString()) != null) {
//            if(!GetNodePropertiesForm.run_task_iface) {
//                String iface_name = value.toString();
//                String mode_iface = GetNodePropertiesForm.response_http.get(iface_name);
//                if(mode_iface != null) {
//                    if(mode_iface.equals("1")) setIcon(iconInterf_up);
//                    else if(mode_iface.equals("2")) setIcon(iconInterf_down);
//                    else setIcon(iconInterf_error);
//                } else setIcon(iconInterf_error);
//            } else setIcon(iconInterf_up);
//        }
        
//        int i=0;
//        for (Map.Entry<String, Map<String, String>> entry : GetNodePropertiesForm.interfInformation.entrySet()) {
        if(!GetNodePropertiesForm.run_task_iface) {
            String status = GetNodePropertiesForm.ifacename_status.get(value.toString());
            if (level == 2 && status != null)
            {
                if(status.equals("1")) setIcon(iconInterf_up);
                else setIcon(iconInterf_down);                 
//                if(!GetNodePropertiesForm.run_task_iface) {
    //                if(GetNodePropertiesForm.out_result != null) {
//                        if(GetNodePropertiesForm.out_result[i] != null) {
//                            if(GetNodePropertiesForm.out_result[i].equals("1")) setIcon(iconInterf_up);
//                            else setIcon(iconInterf_down); 
//                        } else setIcon(iconInterf_error);
    //                } else {
    //                    setIcon(iconInterf_error);
    //                }
//                } else {
//                    setIcon(iconInterf_up);
//                }
    //            break;
            }
        }
//            i++;
//        }

        if (level == 1 && value.toString().equals("Route information"))
        {
            setIcon(iconRoutre);
        }
        if (level == 1 && value.toString().equals("Vlans"))
        {
            setIcon(iconVlans);
        }
        if(level == 2 && value.toString().equals("(default)"))
        {
            int kos=1;
        }
        if(GetNodePropertiesForm.vlanInformation != null ) {
            for (Map.Entry<String, String> entry : GetNodePropertiesForm.vlanInformation.entrySet()) {
                 if (level == 2 && value.toString().equals(entry.getKey()+"  ("+entry.getValue()+")"))
                {
                    setIcon(iconVlan);
                    break;
                }
            }
        }
        if (level == 1 && value.toString().equals("Clients information"))
        {
            setIcon(iconClient);
        }

        if (level == 1 && item_icon.get(value.toString()) != null)
        {
            String path_image = Main.path+"/images/"+item_icon.get(value.toString());
            Icon icon=new ImageIcon(path_image);
            setIcon(icon);
        }

//        // adding charts node icon
//        if(level >= 1)
//        {
//            String[] settings = Utils.FindSettings_value_level(GetNodePropertiesForm.settings, value.toString(), level);
//            if(settings != null)
//            {
////                System.out.println("icon="+settings[0]);
//                Icon iconChart=new ImageIcon(settings[0]);
//                setIcon(iconChart);
//            }
//        }
        
        return this;
    }
    
}

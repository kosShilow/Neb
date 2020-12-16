package ru.kos.neb_viewer;

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;

/**
 *
 * @author kos
 */
public class ImageFileView extends FileView
{
    ImageIcon jpgIcon=new ImageIcon(getClass().getResource("/images/jpgIcon.gif"));
    ImageIcon gifIcon=new ImageIcon(getClass().getResource("/images/gifIcon.gif"));
    ImageIcon tiffIcon=new ImageIcon(getClass().getResource("/images/tiffIcon.gif"));
    ImageIcon pngIcon=new ImageIcon(getClass().getResource("/images/pngIcon.png"));
    
    @Override
    public String getName(File f)
    {
        return null; //let the L&F FileView figure this out
    }
    
    @Override
    public String getDescription(File f)
    {
        return null; //let the L&F FileView figure this out
    }
    
    @Override
    public Boolean isTraversable(File f)
    {
        return null; //let the L&F FileView figure this out
    }
    
    @Override
    public String getTypeDescription(File f)
    {
        String extension = Utils.getExtension(f);
        String type = null;
        
        if (extension != null)
        {
            if (extension.equals(Utils.jpeg) ||
                    extension.equals(Utils.jpg))
            {
                type = "JPEG Image";
            }
            else if (extension.equals(Utils.gif))
            {
                type = "GIF Image";
            }
            else if (extension.equals(Utils.tiff) ||
                    extension.equals(Utils.tif))
            {
                type = "TIFF Image";
            }
            else if (extension.equals(Utils.png))
            {
                type = "PNG Image";
            }
        }
        return type;
    }
    @Override
    public Icon getIcon(File f)
    {
        String extension = Utils.getExtension(f);
        Icon icon = null;
        
        if (extension != null)
        {
            if (extension.equals(Utils.jpeg) ||
                    extension.equals(Utils.jpg))
            {
                icon = jpgIcon;
            }
            else if (extension.equals(Utils.gif))
            {
                icon = gifIcon;
            }
            else if (extension.equals(Utils.tiff) ||
                    extension.equals(Utils.tif))
            {
                icon = tiffIcon;
            }
            else if (extension.equals(Utils.png))
            {
                icon = pngIcon;
            }
        }
        return icon;
    }
}
